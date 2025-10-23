package controller;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class GetListByIdTest {

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    @Mock
    private DynamoDbTable<Task> table;

    @Mock
    private TaskRepository repository;

    private final Gson gson = new Gson();

    private GetListById getListById;

    @BeforeEach
    void setUp(){
        getListById = new GetListById(table, repository);

        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    void shouldGetItemByIdSuccessfully(){
        String pk = "USER#milena";
        String sk = "LIST#";

        Task list = new Task();
        list.setPk(pk);
        list.setSk(sk + "123");
        list.setDescription("test list");

        List<Task> expectedList = List.of(list);

        when(repository.getTask(pk,sk)).thenReturn(expectedList);

        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        requestEvent.setQueryStringParameters(Map.of("pk", pk,"sk", sk));

        APIGatewayProxyResponseEvent responseEvent = getListById.handleRequest(requestEvent, context);

        assertEquals(200, responseEvent.getStatusCode());

        Type taskListType = new TypeToken<List<Task>>(){}.getType();
        List<Task> responseTasks = gson.fromJson(responseEvent.getBody(), taskListType);

        assertNotNull(responseTasks);
        assertEquals(1, responseTasks.size());

        Task expectedTask = expectedList.get(0);
        Task responseTask = responseTasks.get(0);

        assertEquals(expectedTask.getPk(), responseTask.getPk());
        assertEquals(expectedTask.getSk(), responseTask.getSk());
        assertEquals(expectedTask.getDescription(), responseTask.getDescription());

        verify(repository).getTask(pk,sk);
    }
}
