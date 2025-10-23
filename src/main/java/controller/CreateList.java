package controller;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import model.Task;
import util.ApiResponseBuilder;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.UUID;

public class CreateList implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbTable<Task> table;
    private final Gson json;

    // Construtor padrão usado pela Lambda
    public CreateList() {
        DynamoDbClient client = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhanced = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();

        String tableName = System.getenv("TASKS_TABLE");
        this.table = enhanced.table(tableName, TableSchema.fromBean(Task.class));
        this.json = new Gson();
    }

    // Construtor auxiliar para testes
    public CreateList(DynamoDbTable<Task> table, Gson json) {
        this.table = table;
        this.json = json;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        var log = context.getLogger();
        log.log("Nova requisição recebida: " + request.getBody());

        try {
            String body = request.getBody();

            if (body == null || body.isBlank()) {
                log.log("Requisição sem corpo válido");
                return ApiResponseBuilder.createErrorResponse(400, "O corpo da requisição não pode estar vazio");
            }

            Task task = json.fromJson(body, Task.class);

            String pkPrefix = task.getPk();
            if (pkPrefix.isBlank() || !pkPrefix.startsWith("USER#")){
                return ApiResponseBuilder.createErrorResponse(400, "Preencha o campo 'pk' corretamente.");
            }

            String skPrefix = task.getSk();
            if (skPrefix == null || !skPrefix.equals("LIST#")){
                return ApiResponseBuilder.createErrorResponse(400, "O campo 'sk' no corpo da requisição deve ser 'LIST#'.");
            }

            String newSk = skPrefix + UUID.randomUUID();
            task.setSk(newSk);

            table.putItem(task);

            log.log("Tarefa criada com sucesso!! ID: " + task.getSk());

            return ApiResponseBuilder.createSuccessResponse(201, json.toJson(task));

        } catch (JsonSyntaxException ex) {
            log.log("Erro de sintaxe JSON: " + ex.getMessage());
            return ApiResponseBuilder.createErrorResponse(400, "JSON inválido");
        } catch (Exception ex) {
            log.log("Erro inesperado: " + ex.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro interno do servidor");
        }
    }
}
