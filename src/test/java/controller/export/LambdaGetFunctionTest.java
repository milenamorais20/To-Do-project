package controller.export;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.TaskRepository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LambdaGetFunctionTest {

    @Mock
    private TaskRepository mockRepository;
    @Mock
    private S3Client mockS3Client;
    @Mock
    private SesClient mockSesClient;
    @Mock
    private Context mockContext;
    @Mock
    private LambdaLogger mockLogger;

    private LambdaGetFunction handler;
    private final String FAKE_S3_BUCKET = "test-bucket";
    private final String FAKE_SES_FROM = "sender@example.com";

    @BeforeEach
    void setUp() {
        when(mockContext.getLogger()).thenReturn(mockLogger);
        handler = new LambdaGetFunction(mockRepository, mockS3Client, mockSesClient, FAKE_S3_BUCKET, FAKE_SES_FROM);
    }

    private SQSEvent createSqsEvent(String pk, String email) {
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId("test-msg-id");
        message.setBody("{\"pk\":\"" + pk + "\",\"email\":\"" + email + "\"}");

        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(message));
        return event;
    }

    @Test
    void shouldReturnSuccessfully() throws IOException {
        String pk = "USER#123";
        String email = "milena@test.com";
        SQSEvent event = createSqsEvent(pk, email);

        List<Task> tasks = List.of(
                new Task(pk, "TASK#1", "Comprar pão"),
                new Task(pk, "TASK#2", "Passear com o cachorro")
        );

        when(mockRepository.getTasksByPk(pk)).thenReturn(tasks);

        ArgumentCaptor<PutObjectRequest> s3RequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> s3BodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        ArgumentCaptor<SendRawEmailRequest> sesRequestCaptor = ArgumentCaptor.forClass(SendRawEmailRequest.class);

        handler.handleRequest(event, mockContext);

        verify(mockRepository, times(1)).getTasksByPk(pk);
        verify(mockS3Client, times(1)).putObject(s3RequestCaptor.capture(), s3BodyCaptor.capture());

        PutObjectRequest s3Request = s3RequestCaptor.getValue();
        assertEquals(FAKE_S3_BUCKET, s3Request.bucket());
        assertTrue(s3Request.key().startsWith("exports/USER-123/"));
        assertTrue(s3Request.key().endsWith(".csv"));

        byte[] csvBytes = s3BodyCaptor.getValue().contentStreamProvider().newStream().readAllBytes();
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);

        assertTrue(csvContent.contains("\"pk\",\"sk\",\"description\""));
        assertTrue(csvContent.contains("\"USER#123\",\"TASK#1\",\"Comprar pão\""));
        assertTrue(csvContent.contains("\"USER#123\",\"TASK#2\",\"Passear com o cachorro\""));

        verify(mockSesClient, times(1)).sendRawEmail(any(SendRawEmailRequest.class));
        verify(mockLogger, atLeastOnce()).log(contains("2 tarefas encontradas"));
        verify(mockLogger, atLeastOnce()).log(contains("Arquivo salvo no S3"));
        verify(mockLogger, atLeastOnce()).log(contains("E-mail enviado com sucesso para " + email));
    }

    @Test
    void shouldReturnFailNoTasksFound() {
        String pk = "USER#404";
        String email = "milena@test.com";
        SQSEvent event = createSqsEvent(pk, email);

        when(mockRepository.getTasksByPk(pk)).thenReturn(Collections.emptyList());

        handler.handleRequest(event, mockContext);

        verify(mockRepository, times(1)).getTasksByPk(pk);
        verify(mockS3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(mockSesClient, never()).sendRawEmail(any(SendRawEmailRequest.class));
        verify(mockLogger, atLeastOnce()).log(contains("Nenhuma tarefa encontrada para o pk: " + pk));
    }
}