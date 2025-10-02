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

public class UpdateTask implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson gson = new Gson();
    private final DynamoDbTable<Task> table;

    // PK fixo
    private static final String FIXED_PK = "user-id-123";

    public UpdateTask() {
        DynamoDbClient client = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhanced = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();

        String tableName = System.getenv("TASKS_TABLE");
        this.table = enhanced.table(tableName, TableSchema.fromBean(Task.class));
    }

    public UpdateTask(DynamoDbTable<Task> table) {
        this.table = table;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        var log = context.getLogger();
        log.log("Requisição para atualizar task: " + request.getBody());

        try {
            // Captura o SK da URL
            String sk = request.getPathParameters() != null ? request.getPathParameters().get("sk") : null;
            if (sk == null || sk.isBlank()) {
                return ApiResponseBuilder.createErrorResponse(400, "Parâmetro 'sk' é obrigatório na URL");
            }

            // Valida body
            if (request.getBody() == null || request.getBody().isBlank()) {
                return ApiResponseBuilder.createErrorResponse(400, "O corpo da requisição não pode estar vazio");
            }

            JsonObject body = gson.fromJson(request.getBody(), JsonObject.class);
            if (!body.has("description")) {
                return ApiResponseBuilder.createErrorResponse(400, "Campo 'description' é obrigatório");
            }

            String newDescription = body.get("description").getAsString();

            // Busca task existente
            Task existing = table.getItem(
                    Key.builder()
                            .partitionValue(FIXED_PK)
                            .sortValue(sk)
                            .build()
            );

            if (existing == null) {
                return ApiResponseBuilder.createErrorResponse(404, "Task não encontrada");
            }

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
