package controller.item;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.TaskRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListItemsListTest {
    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    @Mock
    private DynamoDbTable<Task> table;

    @Mock
    private TaskRepository repository;

    private ListItemsList listItemsList;

    private Gson gson = new Gson();

    @BeforeEach
    void setUp(){
        listItemsList = new ListItemsList(repository);
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    void shouldReturnHTTPStatus200(){
        String pkList = "LIST#123";

        Task item = new Task();
        item.setPk(pkList);

        List<Task> itemsList = List.of(item);
        when(repository.getTasksByPk(pkList)).thenReturn(itemsList);

        APIGatewayProxyRequestEvent requestEvent =  new APIGatewayProxyRequestEvent();
        requestEvent.setQueryStringParameters(Map.of("pk", pkList));

        APIGatewayProxyResponseEvent responseEvent = listItemsList.handleRequest(requestEvent, context);

        Type itemsListType = new TypeToken<List<Task>>(){}.getType();
        List<Task> responseItemsList = gson.fromJson(responseEvent.getBody(), itemsListType);

        assertEquals(itemsList, responseItemsList);
        assertEquals(200, responseEvent.getStatusCode());

        verify(repository).getTasksByPk(pkList);

    }
}

