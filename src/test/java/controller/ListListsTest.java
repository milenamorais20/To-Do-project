package controller;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.Task;
import repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ListListsTest {

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    @Mock
    private DynamoDbTable<Task> taskTable;

    @Mock
    private TaskRepository taskRepository;

    private ListLists listTasks ;

    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        listTasks = new ListLists(taskRepository);

        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    void shouldReturnTasksSuccessfullyWhenPkIsValid() {

        String pk = "USER#milena";
        String sk = "LIST#";

        Task task = new Task();
        task.setPk(pk);
        task.setSk(sk);
        task.setDescription("Task 1");
        List<Task> tasks = List.of(task);

        when(taskRepository.getTasksByPk(pk)).thenReturn(tasks);

        APIGatewayProxyRequestEvent request= new APIGatewayProxyRequestEvent();
        request.setQueryStringParameters(Map.of("pk", pk));

        APIGatewayProxyResponseEvent response = listTasks.handleRequest(request, context);

        Type taskListType = new TypeToken<List<Task>>(){}.getType();
        List<Task> responseTasks = gson.fromJson(response.getBody(), taskListType);

        assertEquals(tasks, responseTasks);
        assertEquals(200, response.getStatusCode());

        verify(taskRepository).getTasksByPk(pk);

    }

    @Test
    void shouldReturnErrorByParam() {

        String pk = "milena";

        APIGatewayProxyRequestEvent request= new APIGatewayProxyRequestEvent();
        request.setQueryStringParameters(Map.of("pk", pk));

        APIGatewayProxyResponseEvent response = listTasks.handleRequest(request, context);

        assertEquals(400, response.getStatusCode());
        verify(taskRepository, never()).getTasksByPk(any());



    }

}