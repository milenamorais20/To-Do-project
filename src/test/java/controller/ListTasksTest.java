package controller;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import model.Task;
import repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListTasksTest {

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    @Mock
    private DynamoDbTable<Task> taskTable;

    @Mock
    private TaskRepository taskRepository;

    private ListTasks listTasks ;

    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        listTasks = new ListTasks(taskRepository);


        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    void testHandleResponse() {

        String pk = "user-id-123";

        Task task = new Task();
        task.setDescription("Task 1");

        List<Task> tasks = List.of(task);

        when(taskRepository.getTasksByPk(pk)).thenReturn(tasks);

        APIGatewayProxyRequestEvent request= new APIGatewayProxyRequestEvent();

        APIGatewayProxyResponseEvent response = listTasks.handleRequest(request, context);
        assertEquals(200, response.getStatusCode());
        assertEquals(gson.toJson(tasks), response.getBody());


        verify(taskRepository).getTasksByPk(pk);

    }

}