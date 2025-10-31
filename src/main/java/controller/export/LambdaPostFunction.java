package controller.export;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;
import util.ApiResponseBuilder;

import java.util.Map;

public class LambdaPostFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final SqsClient sqsClient;
    private final String sqsQueueUrl;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public LambdaPostFunction() {
        this.sqsClient = SqsClient.builder().build();
        this.sqsQueueUrl = System.getenv("SQS_QUEUE_URL");
        if (this.sqsQueueUrl == null) {
            System.err.println("Variável de ambiente SQS_QUEUE_URL não definida.");
        }
    }

    public LambdaPostFunction(SqsClient sqsClient, String sqsQueueUrl) {
        this.sqsClient = sqsClient;
        this.sqsQueueUrl = sqsQueueUrl;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Iniciando LambdaPostFunction...");

        try {

            Map<String, String> queryParams = requestEvent.getQueryStringParameters();
            String pk = (queryParams != null) ? queryParams.get("pk") : null;

            if (pk == null || pk.isBlank()) {
                logger.log("Erro: 'pk' não fornecido nos query parameters.");
                return ApiResponseBuilder.createErrorResponse(400, "Query parameter 'pk' é obrigatório");
            }

            String userEmail = getUserEmailFromAuthContext(requestEvent, logger);
            if (userEmail == null || userEmail.isBlank()) {
                logger.log("Erro: Não foi possível obter o e-mail do usuário autenticado. O Authorizer está configurado?");
                return ApiResponseBuilder.createErrorResponse(401, "Não autorizado ou e-mail não encontrado no token.");
            }

            logger.log("Solicitação recebida para pk: " + pk + ", e-mail: " + userEmail);

            var sqsMessageBody = Map.of(
                    "pk", pk,
                    "email", userEmail
            );
            String messageBodyJson = gson.toJson(sqsMessageBody);

            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(sqsQueueUrl)
                    .messageBody(messageBodyJson)
                    .build();

            sqsClient.sendMessage(sendMessageRequest);
            logger.log("Mensagem enviada com sucesso para o SQS. Corpo: " + messageBodyJson);

            return ApiResponseBuilder.createSuccessResponse(200, Map.of("message", "Sua solicitação foi processada com sucesso."));

        } catch (SqsException e) {
            logger.log("Erro ao enviar mensagem para o SQS: " + e.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro ao enfileirar solicitação.");
        } catch (Exception e) {
            logger.log("Erro inesperado na LambdaPostFunction: " + e.getMessage());
            e.printStackTrace();
            return ApiResponseBuilder.createErrorResponse(500, "Erro interno do servidor.");
        }
    }
    @SuppressWarnings("unchecked")
    private String getUserEmailFromAuthContext(APIGatewayProxyRequestEvent requestEvent, LambdaLogger logger) {
        try {
            if (requestEvent.getRequestContext() != null && requestEvent.getRequestContext().getAuthorizer() != null) {

                Map<String, Object> authorizer = requestEvent.getRequestContext().getAuthorizer();
                Object claimsObject = authorizer.get("claims");

                if (claimsObject instanceof Map) {
                    Map<String, Object> claims = (Map<String, Object>) claimsObject;
                    Object emailObject = claims.get("email");

                    if (emailObject != null) {
                        return (String) emailObject;
                    }
                }
            }
        } catch (Exception e) {
            logger.log("Erro ao tentar pegar e-mail do cognito: " + e.getMessage());
            e.printStackTrace();
        }
        logger.log("Auth: claims, ou email não encontrados na requisição.");
        return null;
    }
}
