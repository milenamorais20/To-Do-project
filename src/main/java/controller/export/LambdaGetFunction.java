package controller.export;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import model.Task;
import repository.TaskRepository;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;
import util.Csv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class LambdaGetFunction implements RequestHandler<SQSEvent, Void> {

    private final TaskRepository repository;
    private final S3Client s3Client;
    private final SesClient sesClient;
    private final String s3BucketName;
    private final String sesFromEmail;
    private final Gson gson = new Gson();

    public LambdaGetFunction() {
        DynamoDbClient client = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
        String tableName = System.getenv("TASKS_TABLE");
        DynamoDbTable<Task> table = enhancedClient.table(tableName, TableSchema.fromBean(Task.class));

        this.repository = new TaskRepository(table);
        this.s3Client = S3Client.builder().build();
        this.sesClient = SesClient.builder().build();
        this.s3BucketName = System.getenv("S3_BUCKET_NAME");
        this.sesFromEmail = System.getenv("SES_FROM_EMAIL"); // E-mail verificado no SES
    }

    public LambdaGetFunction(TaskRepository repository, S3Client s3Client, SesClient sesClient, String s3BucketName, String sesFromEmail) {
        this.repository = repository;
        this.s3Client = s3Client;
        this.sesClient = sesClient;
        this.s3BucketName = s3BucketName;
        this.sesFromEmail = sesFromEmail;
    }

//    Processa as mensagens da fila SQS.
    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        LambdaLogger logger = context.getLogger();

        for (SQSEvent.SQSMessage msg : sqsEvent.getRecords()) {// sqsEvent.getRecords() obter a lista de mensagens do Amazon SQS contidas no objeto SQSEvent
            String messageId = msg.getMessageId();
            try {
                logger.log("Processando mensagem SQS: " + messageId);

                String body = msg.getBody();
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> bodyContent = gson.fromJson(body, type);

                String pk = bodyContent.get("pk");
                String userEmail = bodyContent.get("email");

                if (pk == null || userEmail == null) {
                    logger.log("Mensagem mal formatada (pk ou email ausente): " + messageId);
                    continue;
                }

                logger.log("Conteúdo da requisição: pk=" + pk + ", email=" + userEmail);

                List<Task> tasks = repository.getTasksByPk(pk);
                if (tasks == null || tasks.isEmpty()) {

                    logger.log("Nenhuma tarefa encontrada para o pk: " + pk + ". E-mail não será enviado.");
                    continue;
                }
                logger.log(tasks.size() + " tarefas encontradas para " + pk);

                byte[] csvBytes = Csv.generateCsv(tasks);
                logger.log("Arquivo CSV gerado com " + csvBytes.length + " bytes.");

                String s3Key = "exports/" + pk.replace("#", "-") + "/" + System.currentTimeMillis() + ".csv";
                uploadToS3(csvBytes, s3Key, logger);
                logger.log("Arquivo salvo no S3 em s3://" + s3BucketName + "/" + s3Key);

                sendEmailWithAttachment(userEmail, csvBytes, s3Key, logger);
                logger.log("E-mail enviado com sucesso para " + userEmail);

            } catch (Exception e) {
                logger.log("Falha ao processar mensagem " + messageId + ": " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Falha ao processar mensagem SQS: " + e.getMessage(), e);
            }
        }
        return null;
    }

    private void uploadToS3(byte[] data, String s3Key, LambdaLogger logger) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key(s3Key)
                    .contentType("text/csv")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(data));
        } catch (S3Exception e) {
            logger.log("Erro no upload para o S3: " + e.getMessage());
            throw e;
        }
    }

    private void sendEmailWithAttachment(String toEmail, byte[] csvBytes, String s3Key, LambdaLogger logger) throws MessagingException, IOException {

        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage mimeMessage = new MimeMessage(session);

        mimeMessage.setFrom(new InternetAddress(sesFromEmail));
        mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        mimeMessage.setSubject("Seu Relatório de Tarefas está Pronto", "UTF-8");

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent(
                "Olá,\n\nSeu relatório de tarefas solicitado está em anexo.\n\n" +
                        "Uma cópia também foi salva em seu bucket S3 com a chave: " + s3Key + "\n\n" +".",
                "text/plain; charset=UTF-8"
        );

//        Cria o anexo do CSV
        MimeBodyPart csvAttachmentPart = new MimeBodyPart();

        ByteArrayDataSource dataSource = new ByteArrayDataSource(csvBytes, "text/csv");
        csvAttachmentPart.setDataHandler(new DataHandler(dataSource));
        csvAttachmentPart.setFileName("relatorio_tarefas.csv");

        // Monta o e-mail com texto e anexo
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(csvAttachmentPart);
        mimeMessage.setContent(multipart);

        // Converte a mensagem para o formato Raw e envia via SES
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mimeMessage.writeTo(outputStream);
        SdkBytes rawMessageBytes = SdkBytes.fromByteArray(outputStream.toByteArray());
        RawMessage rawMessage = RawMessage.builder().data(rawMessageBytes).build();

        SendRawEmailRequest rawEmailRequest = SendRawEmailRequest.builder().rawMessage(rawMessage).build();
        try {
            sesClient.sendRawEmail(rawEmailRequest);
        } catch (SesException ex) {
            logger.log("Erro ao enviar email pelo SES: " + ex.getMessage());
            throw ex;
        }
    }
}