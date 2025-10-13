package controller.item;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import model.Task;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import util.ApiResponseBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class UpdateItemList implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson gson = new Gson();
    private final DynamoDbTable<Task> table;

    public UpdateItemList(){
        DynamoDbClient client = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient =  DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();

        String tableName = System.getenv("TASKS_TABLE");
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(Task.class));

    }

    public UpdateItemList(DynamoDbTable<Task> table) {
        this.table = table;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        var log = context.getLogger();
        log.log("Requisição para atualizar task: " + requestEvent.getBody());

        try {
            Map<String, String> pathParameters = requestEvent.getPathParameters();

            String encodedPk = pathParameters != null ? pathParameters.get("pk") : null;
            String encodedSk = pathParameters != null ? pathParameters.get("sk") : null;

            String pk = (encodedPk != null) ? URLDecoder.decode(encodedPk, StandardCharsets.UTF_8) : null;
            String sk = (encodedSk != null) ? URLDecoder.decode(encodedSk, StandardCharsets.UTF_8) : null;

            log.log("Parâmetros decodificados: PK=" + pk + ", SK=" + sk);

            if (pk ==  null || pk.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(400, "Parâmetro 'pk' é obrigatório na URL.");
            }
            if (sk == null || sk.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(400, "Parâmetro 'sk' é obrigatorio.");
            }

            if (requestEvent.getBody() == null || requestEvent.getBody().isBlank()) {
                return ApiResponseBuilder.createErrorResponse(400, "O corpo da requisição não pode estar vazio.");
            }

            JsonObject body = gson.fromJson(requestEvent.getBody(), JsonObject.class);

            Task item = table.getItem(
                    Key.builder().partitionValue(pk).sortValue(sk).build()
            );

            if (item == null) {
                return ApiResponseBuilder.createErrorResponse(404, "Item não encontrado.");
            }

            if (!body.has("description")){
                return ApiResponseBuilder.createErrorResponse(400, "Campo 'description' é obrigatório.");
            }

            String newDescription = body.get("description").getAsString();
            item.setDescription(newDescription);
            table.putItem(item);

            log.log("Task atualizada com sucesso! PK=" + item.getPk() + " SK=" + item.getSk());

            return ApiResponseBuilder.createSuccessResponse(200, gson.toJson(item));
        } catch (JsonSyntaxException ex) {
            log.log("Erro de JSON: " + ex.getMessage());
            return ApiResponseBuilder.createErrorResponse(400, "JSON inválido");
        } catch (Exception ex) {
            log.log("Erro inesperado: " + ex.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro interno do servidor");
        }
    }
}
