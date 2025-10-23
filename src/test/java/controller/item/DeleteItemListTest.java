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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.TaskRepository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteItemListTest {
    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    @Mock
    private DynamoDbTable<Task> table;

    @Mock
    private TaskRepository repository;

    private final Gson gson = new Gson();

    private DeleteItemList deleteItemList;

    @BeforeEach
    void setUp(){
        deleteItemList = new DeleteItemList(table,repository);
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    void shouldDeleteItemSuccessfully(){
        String pk = "USER#milena";
        String sk = "LIST#123";

        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        requestEvent.setQueryStringParameters(Map.of("pk", pk,"sk", sk));

        APIGatewayProxyResponseEvent responseEvent = deleteItemList.handleRequest(requestEvent, context);

        assertEquals(204, responseEvent.getStatusCode());

        //Vai pegar o elemento key
        ArgumentCaptor<Key> keyCaptor = ArgumentCaptor.forClass(Key.class);

        verify(table).deleteItem(keyCaptor.capture());

        Key capturedKey = keyCaptor.getValue();
        assertEquals(pk, capturedKey.partitionKeyValue().s());
        assertEquals(sk, capturedKey.sortKeyValue().get().s());

    }
}
