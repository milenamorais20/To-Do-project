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

import java.util.UUID;

public class CreateItemList implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbTable<Task> table;
    private final TaskRepository repository;
    private final Gson json;

    public CreateItemList(){
        DynamoDbClient client = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhanced = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();

        String tableName = System.getenv("TASKS_TABLE");
        this.table = enhanced.table(tableName, TableSchema.fromBean(Task.class));
        this.json = new Gson();
        this.repository = new TaskRepository(table);
    }

    //Testes Unitários
    public CreateItemList(DynamoDbTable<Task> table, TaskRepository repository, Gson json) {
        this.table = table;
        this.repository = repository;
        this.json = json;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        var log = context.getLogger();
        log.log("Nova requisição recebida: " + request.getBody());

        try {
            String body = request.getBody();

            if (body == null || body.isBlank()){
                log.log("Corpo da requisição inválido");
                return ApiResponseBuilder.createErrorResponse(400, " o corpo da requisição não pode ser vazio.");
            }

            Task item = json.fromJson(body, Task.class);

            String pkList = item.getPk();
            if (pkList == null || pkList.isBlank()){
                return ApiResponseBuilder.createErrorResponse(400, "O campo 'pk' no corpo da requisição deve ser preenchido.");
            }

            String skList = item.getSk();
            if (skList == null || skList.isBlank()){
                return ApiResponseBuilder.createErrorResponse(400, "O campo 'pk' no corpo da requisição deve ser preenchido.");
            }

            boolean skListExists = repository.skListExists(pkList, skList);
            if (!skListExists){
                return ApiResponseBuilder.createErrorResponse(400, "Não existe nenhuma lista com esse sk");
            }

            item.setPk(skList);
            item.setSk(UUID.randomUUID().toString());

            table.putItem(item);

            log.log("Item inserido na lista com sucesso!");

            return ApiResponseBuilder.createSuccessResponse(200, json.toJson(item));

        } catch (JsonSyntaxException ex) {
            log.log("Erro de sintaxe JSON: " + ex.getMessage());
            return ApiResponseBuilder.createErrorResponse(400, "JSON inválido");
        } catch (Exception ex) {
            log.log("Erro inesperado: " + ex.getMessage());
            return ApiResponseBuilder.createErrorResponse(500, "Erro interno do servidor");
        }
    }
}
