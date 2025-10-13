package controller.item;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdateItemListTest {

    @Mock
    private DynamoDbTable<Task> table;
    @Mock
    private Context context;
    @Mock
    private LambdaLogger logger;

    private UpdateItemList updateItemList;

    @BeforeEach
    void setUp(){
        updateItemList = new UpdateItemList(table);
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    void shouldReturnHTTPStatus200(){
        Task item = new Task("LIST#123", "A1B2C3", "old description");
        when(table.getItem(any(Key.class))).thenReturn(item);

        APIGatewayProxyRequestEvent request =  new APIGatewayProxyRequestEvent()
                .withPathParameters(java.util.Map.of("pk","LIST#123", "sk", "A1B2C3"))
                .withBody("{\"description\":\"new description\"}");

        APIGatewayProxyResponseEvent response = updateItemList.handleRequest(request, context);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("new description"));
        verify(table, times(1)).putItem(any(Task.class));

    }
}
