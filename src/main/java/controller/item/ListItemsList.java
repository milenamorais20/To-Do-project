package controller.item;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import model.Task;
import repository.TaskRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import util.ApiResponseBuilder;

import java.util.List;

public class ListItemsList implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final Gson json = new Gson();
    private final TaskRepository repository;

    public ListItemsList(){
        DynamoDbClient client = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();

        String tableName = System.getenv("TASKS_TABLE");
        DynamoDbTable<Task> table = enhancedClient.table(tableName, TableSchema.fromBean(Task.class));

        this.repository = new TaskRepository(table);
    }

    public ListItemsList(TaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        var log = context.getLogger();
        log.log("Requisição recebida para listar tarefas: " + requestEvent.getBody());

        try {
            String pkList = null;

            if (requestEvent.getQueryStringParameters() != null){
                pkList = requestEvent.getQueryStringParameters().get("pk");
            }

            if (pkList == null || pkList.isBlank() || !pkList.contains("#")){
                log.log("Corpo da requisição inválido");
                return ApiResponseBuilder.createErrorResponse(400, "A 'pk' da lista não pode ser nula.");
            }

            List<Task> itensListByPk = repository.getTasksByPk(pkList);
            return ApiResponseBuilder.createSuccessResponse(200, itensListByPk);
        }catch (JsonSyntaxException ex) {
            log.log("Falha ao processar JSON: " + ex.getMessage());
            return ApiResponseBuilder.createErrorResponse(400, "Requisição inválida");
        } catch (Exception ex) {
            log.log("Erro inesperado ao listar itens: " + ex.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro interno do servidor");
        }
    }
}
