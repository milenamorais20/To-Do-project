package util;


import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;

import java.util.Collections;
import java.util.Map;

public class ApiResponseBuilder {
    private static final Gson gson = new Gson();

    private ApiResponseBuilder(){

    }

    public static APIGatewayProxyResponseEvent createSuccessResponse(int statusCode, Object body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Collections.singletonMap("Content-Type", "application/json"))
                .withBody(gson.toJson(body));
    }


    public static APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String errorMessage) {
        Map<String, String> errorPayload = Collections.singletonMap("Erro: ", errorMessage);

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(gson.toJson(errorPayload))
                .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
    }
}