package controller.item;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.TaskRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateItemListTest {
    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    @Mock
    private DynamoDbTable<Task> table;

    @Mock
    private TaskRepository repository;

    private CreateItemList createItemList;
    private final Gson gson =  new Gson();

    @BeforeEach
    void setUp(){
        createItemList = new CreateItemList(table,repository,gson);
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    void shouldReturnHTTPStatus200(){
        Task item = new Task();
        item.setPk("LIST#123");
        item.setSk(UUID.randomUUID().toString());
        item.setDescription("Este é um teste");

        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        requestEvent.setBody(gson.toJson(item));

        when(repository.skListExists("LIST#123")).thenReturn(true);

        APIGatewayProxyResponseEvent responseEvent = createItemList.handleRequest(requestEvent,context);

        assertEquals(200, responseEvent.getStatusCode());

        verify(table).putItem(any(Task.class));
    }
}
