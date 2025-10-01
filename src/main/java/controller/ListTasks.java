package controller;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import model.Task;
import repository.TaskRepository;
import util.ApiResponseBuilder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;

public class ListTasks implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson jsonConverter = new Gson();
    private final TaskRepository repository;

    // Construtor padrão usado na Lambda
    public ListTasks() {
        DynamoDbClient client = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhanced = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();

        String tableName = System.getenv("TASKS_TABLE");
        DynamoDbTable<Task> taskTable = enhanced.table(tableName, TableSchema.fromBean(Task.class));

        this.repository = new TaskRepository(taskTable);
    }

    // Construtor para testes
    public ListTasks(TaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        var log = context.getLogger();
        log.log("Requisição recebida para listar tarefas: " + event.getBody());

        try {
            String pk = "user-id-123";
            List<Task> taskList = repository.getTasksByPk(pk);

            return ApiResponseBuilder.createSuccessResponse(200, taskList);
        } catch (JsonSyntaxException ex) {
            log.log("Falha ao processar JSON: " + ex.getMessage());
            return ApiResponseBuilder.createErrorResponse(400, "Requisição inválida");
        } catch (Exception ex) {
            log.log("Erro inesperado ao listar tarefas: " + ex.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro interno do servidor");
        }
    }
}
