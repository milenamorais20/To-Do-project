package repository;

import model.Task;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TaskRepository {

    private final DynamoDbTable<Task> table;

    public TaskRepository(DynamoDbTable<Task> table) {
        this.table = table;
    }

    // Busca todas as tarefas de um usuário
    public List<Task> getTasksByPk(String pk) {
        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(pk).build()
        );

        List<Task> result = new ArrayList<>();
        table.query(condition).items().forEach(result::add);
        return result;
    }

    // Busca tarefa específica pelo ID
    public List<Task> getTask(String pk, String sk) {
        Key key = Key.builder()
                .partitionValue(pk)
                .sortValue(sk)
                .build();

        Task item = table.getItem(key);
        return item != null ? Collections.singletonList(item) : Collections.emptyList();
    }

    public boolean skListExists(String pkList, String skList) {
        Key key = Key.builder()
                .partitionValue(pkList)
                .sortValue(skList)
                .build();

        return table.getItem(key) != null;
    }
}
