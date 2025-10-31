package controller.export;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder; // Importar GsonBuilder
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LambdaPostFunctionTest {

    @Mock
    private SqsClient mockSqsClient;

    @Mock
    private Context mockContext;

    @Mock
    private LambdaLogger mockLogger;

    private LambdaPostFunction handler;
    private final String FAKE_SQS_URL = "https://sqs.us-east-1.amazonaws.com/123/test-queue";
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create(); // Usar o builder

    @BeforeEach
    void setUp() {
        when(mockContext.getLogger()).thenReturn(mockLogger);
        handler = new LambdaPostFunction(mockSqsClient, FAKE_SQS_URL);
    }

    @Test
    void shouldReturnSuccessfully() {
        String pk = "USER#12345";
        String email = "test@example.com";

        Map<String, Object> claimsMap = Map.of("email", (Object) email);
        Map<String, Object> authorizerMap = Map.of("claims", (Object) claimsMap);

        APIGatewayProxyRequestEvent mockRequest = mock(APIGatewayProxyRequestEvent.class);
        APIGatewayProxyRequestEvent.ProxyRequestContext mockRequestContext =
                mock(APIGatewayProxyRequestEvent.ProxyRequestContext.class);

        when(mockRequest.getRequestContext()).thenReturn(mockRequestContext);
        when(mockRequestContext.getAuthorizer()).thenReturn(authorizerMap);

        when(mockRequest.getQueryStringParameters()).thenReturn(Map.of("pk", pk));

        ArgumentCaptor<SendMessageRequest> sqsRequestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);

        APIGatewayProxyResponseEvent response = handler.handleRequest(mockRequest, mockContext);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Sua solicitação foi processada com sucesso."));
        verify(mockSqsClient, times(1)).sendMessage(sqsRequestCaptor.capture());

        SendMessageRequest sentMessage = sqsRequestCaptor.getValue();
        assertEquals(FAKE_SQS_URL, sentMessage.queueUrl());
        Map<String, String> messageBody = gson.fromJson(sentMessage.messageBody(), Map.class);
        assertEquals(pk, messageBody.get("pk"));
        assertEquals(email, messageBody.get("email"));

        verify(mockLogger, atLeastOnce()).log(contains("Solicitação recebida para pk: " + pk));
    }

    @Test
    void shouldReturnFailNoPk() {
        APIGatewayProxyRequestEvent mockRequest = mock(APIGatewayProxyRequestEvent.class);


        when(mockRequest.getQueryStringParameters()).thenReturn(null);

        APIGatewayProxyResponseEvent response = handler.handleRequest(mockRequest, mockContext);

        assertEquals(400, response.getStatusCode());


        String expectedJsonSubstring = "\"erro\":\"Query parameter 'pk' é obrigatório\"";
        assertTrue(response.getBody().contains(expectedJsonSubstring));

        verify(mockSqsClient, never()).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldReturnFailNoEmail() {
        APIGatewayProxyRequestEvent mockRequest = mock(APIGatewayProxyRequestEvent.class);
        APIGatewayProxyRequestEvent.ProxyRequestContext mockRequestContext =
                mock(APIGatewayProxyRequestEvent.ProxyRequestContext.class);

        when(mockRequest.getQueryStringParameters()).thenReturn(Map.of("pk", "USER#123"));

        when(mockRequest.getRequestContext()).thenReturn(mockRequestContext);
        when(mockRequestContext.getAuthorizer()).thenReturn(null);

        APIGatewayProxyResponseEvent response = handler.handleRequest(mockRequest, mockContext);

        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Não autorizado ou e-mail não encontrado no token."));
        verify(mockSqsClient, never()).sendMessage(any(SendMessageRequest.class));
        verify(mockLogger, atLeastOnce()).log(contains("Contexto do autorizador, 'claims', ou 'email' não encontrados"));
    }
}
