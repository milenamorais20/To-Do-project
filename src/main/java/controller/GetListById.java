package controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import model.Task;
import repository.TaskRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import util.ApiResponseBuilder;

import java.util.List;
import java.util.Map;

public class GetListById implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final Gson gson = new Gson();
    private final DynamoDbTable<Task> table;
    private final TaskRepository repository;

    public GetListById() {
        DynamoDbClient client = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();

        String tableName = System.getenv("TASKS_TABLE");

        this.table = enhancedClient.table(tableName, TableSchema.fromBean(Task.class));
        this.repository = new TaskRepository(table);
    }

    public GetListById(DynamoDbTable<Task> table, TaskRepository repository) {
        this.table = table;
        this.repository = repository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        LambdaLogger logger = context.getLogger();

        Map<String, String> queryParams = requestEvent.getQueryStringParameters();

        try{
            if (queryParams == null){
                return ApiResponseBuilder.createErrorResponse(400, "Os parâmetros 'pk' e 'sk' são obrigatórios.");
            }

            String pk = queryParams.get("pk");
            String sk = queryParams.get("sk");

            logger.log("Processando requisição para pk: " + pk + ", sk: " + sk);

            if (pk == null || pk.isBlank() || !pk.contains("#")){
                return ApiResponseBuilder.createErrorResponse(400, "Parâmetro 'pk' é obrigatório");
            }

            if (sk == null || sk.isBlank() || !sk.contains("#")){
                return ApiResponseBuilder.createErrorResponse(400, "Parâmetro 'sk' é obrigatório");
            }

            List<Task> list = repository.getTask(pk,sk);

            if (list == null) {
                logger.log("Item não encontrado com pk: " + pk + ", sk: " + sk);
                return ApiResponseBuilder.createErrorResponse(404, "Item não encontrado");
            }

            return ApiResponseBuilder.createSuccessResponse(200, list);

        } catch (Exception ex) {
            logger.log("Erro inesperado ao listar itens: " + ex.getMessage());
            logger.log(ex.toString());
            return ApiResponseBuilder.createErrorResponse(500, "Erro interno do servidor");
        }
    }
}
