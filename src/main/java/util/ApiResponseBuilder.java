package util;


import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Collections;
import java.util.Map;

/**
 * Classe utilitária para construir respostas padronizadas para o API Gateway.
 * Facilita a criação de respostas de sucesso e erro em formato JSON.
 */
public class ApiResponseBuilder {
    // Instância do Gson para serialização de objetos para JSON, com HTML escaping desabilitado.
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();


//    Construtor privado para impedir a instanciação da classe utilitária.
    private ApiResponseBuilder(){}

    /**
     * Cria uma resposta de sucesso HTTP com um corpo (body) e status code definidos.
     * @param statusCode O código de status HTTP da resposta.
     * @param body O objeto a ser serializado como corpo da resposta JSON.
     * @return Um objeto APIGatewayProxyResponseEvent configurado para sucesso.
     */
    public static APIGatewayProxyResponseEvent createSuccessResponse(int statusCode, Object body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Collections.singletonMap("Content-Type", "application/json"))
                .withBody(gson.toJson(body));
    }

    /**
     * Cria uma resposta de erro HTTP com uma mensagem de erro padronizada.
     * @param statusCode O código de status HTTP da resposta de erro.
     * @param errorMessage A mensagem de erro a ser incluída no corpo da resposta.
     * @return Um objeto APIGatewayProxyResponseEvent configurado para erro.
     */
    public static APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String errorMessage) {
        Map<String, String> errorPayload = Collections.singletonMap("erro", errorMessage);

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(gson.toJson(errorPayload))
                .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
    }
}