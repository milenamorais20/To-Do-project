package controller.item;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import model.Task;
import repository.TaskRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import util.ApiResponseBuilder;

import java.util.Map;

public class DeleteItemList implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbTable<Task> table;
    private final TaskRepository repository;

    public DeleteItemList (){
        DynamoDbClient client = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();

        String tableName = System.getenv("TASKS_TABLE");

        this.table = enhancedClient.table(tableName, TableSchema.fromBean(Task.class));
        this.repository = new TaskRepository(table);
    }

    public DeleteItemList(DynamoDbTable<Task> table, TaskRepository repository) {
        this.table = table;
        this.repository = repository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Iniciando requisição para deletar item.");

        Map<String, String> params = requestEvent.getQueryStringParameters();

        try{
            if (params ==  null || params.isEmpty()){
                return ApiResponseBuilder.createErrorResponse(400, "É preciso preencher os campos 'pk' e 'sk'");
            }

            String pk =  params.get("pk");
            String sk =  params.get("sk");

            if (pk == null || pk.isBlank() || !pk.contains("#")){
               return ApiResponseBuilder.createErrorResponse(400, "Preencha o 'pk' corretamente");
            }
            if (sk == null || sk.isBlank())){
                return ApiResponseBuilder.createErrorResponse(400, "Preencha o 'sk' corretamente");
            }

            Key keyTask = Key.builder().partitionValue(pk).sortValue(sk).build();
            table.deleteItem(keyTask);

            logger.log("Item deletado com sucesso.");

            return ApiResponseBuilder.createSuccessResponse(204, null);

        }catch (DynamoDbException e) {
            return ApiResponseBuilder.createErrorResponse(500, "Erro ao acessar o DynamoDB: " + e.getMessage());
        }catch (Exception e) {
            return ApiResponseBuilder.createErrorResponse(500, "Erro inesperado. " + e.getMessage());
        }

    }
}
