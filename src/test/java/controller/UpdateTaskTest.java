package controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateTaskTest {

    private DynamoDbTable<Task> mockTable;
    private UpdateTask updateTask;
    private Context context;

    @BeforeEach
    void setUp() {
        mockTable = Mockito.mock(DynamoDbTable.class);
        updateTask = new UpdateTask(mockTable);

        // Mock Lambda Context and Logger
        context = Mockito.mock(Context.class);
        LambdaLogger mockLogger = Mockito.mock(LambdaLogger.class);
        when(context.getLogger()).thenReturn(mockLogger);
    }

    @Test
    void shouldUpdateTaskSuccessfully() {
        Task task = new Task("USER#milena", "LIST#1", "old description");
        when(mockTable.getItem(any(Key.class))).thenReturn(task);

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPathParameters(java.util.Map.of("pk", "USER#milena","sk", "LIST#1"))
                .withBody("{\"description\":\"new description\"}");

        APIGatewayProxyResponseEvent response = updateTask.handleRequest(request, context);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("new description"));
        verify(mockTable, times(1)).putItem(any(Task.class));
    }

    @Test
    void shouldReturn404WhenTaskDoesNotExist() {
        when(mockTable.getItem(any(Key.class))).thenReturn(null);

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPathParameters(java.util.Map.of("pk", "USER#milena","sk", "nonexistent-task"))
                .withBody("{\"description\":\"new description\"}");

        APIGatewayProxyResponseEvent response = updateTask.handleRequest(request, context);

        assertEquals(404, response.getStatusCode());
        verify(mockTable, never()).putItem(any(Task.class));
    }

    @Test
    void shouldReturn400WhenBodyIsEmpty() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPathParameters(java.util.Map.of("pk", "","sk", "task-001"))
                .withBody("");

        APIGatewayProxyResponseEvent response = updateTask.handleRequest(request, context);

        assertEquals(400, response.getStatusCode());
        verify(mockTable, never()).putItem(any(Task.class));
    }

    @Test
    void shouldReturn400WhenBodyDoesNotContainDescription() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPathParameters(java.util.Map.of("pk", "","sk", "task-001"))
                .withBody("{\"otherField\":\"test\"}");

        APIGatewayProxyResponseEvent response = updateTask.handleRequest(request, context);

        assertEquals(400, response.getStatusCode());
        verify(mockTable, never()).putItem(any(Task.class));
    }

    @Test
    void shouldReturn400WhenPathParameterSkIsMissing() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withBody("{\"description\":\"new description\"}"); // no sk

        APIGatewayProxyResponseEvent response = updateTask.handleRequest(request, context);

        assertEquals(400, response.getStatusCode());
        verify(mockTable, never()).putItem(any(Task.class));
    }

    @Test
    void shouldReturn400WhenJsonIsInvalid() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPathParameters(java.util.Map.of("sk", "task-001"))
                .withBody("{\"description\":}"); // invalid JSON

        APIGatewayProxyResponseEvent response = updateTask.handleRequest(request, context);

        assertEquals(400, response.getStatusCode());
        verify(mockTable, never()).putItem(any(Task.class));
    }
}
