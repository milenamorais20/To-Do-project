package controller;

import model.Task;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import com.google.gson.Gson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateTaskTest {

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    @Mock
    private DynamoDbTable<Task> taskTable;

    private CreateTask createTask ;

    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        // Usando o construtor que aceita dependências para injeção de mocks
        createTask = new CreateTask(taskTable, gson);

        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    void testHandleRequest() {

        Task task = new Task();
        task.setPk("USER#milena");
        task.setSk("LIST#");
        task.setDescription("Este é um teste");

        APIGatewayProxyRequestEvent expectredRequest = new APIGatewayProxyRequestEvent();
        expectredRequest.setBody(gson.toJson(task));

        APIGatewayProxyResponseEvent expectedResponse = createTask.handleRequest(expectredRequest, context);
        assertEquals(201, expectedResponse.getStatusCode());

        verify(taskTable).putItem(any(Task.class));
    }
}