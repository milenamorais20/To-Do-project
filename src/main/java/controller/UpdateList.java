package controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import model.Task;
import util.ApiResponseBuilder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import java.util.Map;

public class UpdateList implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson gson = new Gson();
    private final DynamoDbTable<Task> table;

    public UpdateList() {
        DynamoDbClient client = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhanced = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();

        String tableName = System.getenv("TASKS_TABLE");
        this.table = enhanced.table(tableName, TableSchema.fromBean(Task.class));
    }

    public UpdateList(DynamoDbTable<Task> table) {
        this.table = table;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        var log = context.getLogger();
        log.log("Requisição para atualizar task: " + request.getBody());

        try {
            // Pega pk e sk da URL
            Map<String, String> pathParams = request.getPathParameters();

            // Pega os valores codificados
            String encodedPk = pathParams != null ? pathParams.get("pk") : null;
            String encodedSk = pathParams != null ? pathParams.get("sk") : null;

            // Decodifica os valores para o formato original %23 para #
            String pk = (encodedPk != null) ? URLDecoder.decode(encodedPk, StandardCharsets.UTF_8) : null;
            String sk = (encodedSk != null) ? URLDecoder.decode(encodedSk, StandardCharsets.UTF_8) : null;

            log.log("Parâmetros decodificados: PK=" + pk + ", SK=" + sk);

            if (pk == null || pk.isBlank()) {
                return ApiResponseBuilder.createErrorResponse(400, "Parâmetro 'pk' é obrigatório na URL");
            }
            if (sk == null || sk.isBlank()) {
                return ApiResponseBuilder.createErrorResponse(400, "Parâmetro 'sk' é obrigatório na URL");
            }
            // Valida body
            if (request.getBody() == null || request.getBody().isBlank()) {
                return ApiResponseBuilder.createErrorResponse(400, "O corpo da requisição não pode estar vazio");
            }
            JsonObject body = gson.fromJson(request.getBody(), JsonObject.class);
            // Busca task existente
            Task existing = table.getItem(
                    Key.builder().partitionValue(pk).sortValue(sk).build()
            );
            if (existing == null) {
                return ApiResponseBuilder.createErrorResponse(404, "Task não encontrada");
            }
            // Aplica atualizações
            if (!body.has("description")) {
                return ApiResponseBuilder.createErrorResponse(400, "Campo 'description' é obrigatório");
            }

            String newDescription = body.get("description").getAsString();

            // Atualiza descrição
            existing.setDescription(newDescription);
            table.putItem(existing);

            log.log("Task atualizada com sucesso! PK=" + existing.getPk() + " SK=" + existing.getSk());

            return ApiResponseBuilder.createSuccessResponse(200, gson.toJson(existing));

        } catch (JsonSyntaxException ex) {
            log.log("Erro de JSON: " + ex.getMessage());
            return ApiResponseBuilder.createErrorResponse(400, "JSON inválido");
        } catch (Exception ex) {
            log.log("Erro inesperado: " + ex.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro interno do servidor");
        }
    }
}
