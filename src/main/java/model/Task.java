package model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonPropertyOrder({"pk", "sk", "description"})
@DynamoDbBean
public class Task {
    private String pk;
    private String sk;
    private String description;

    public Task() {

    }

    public Task(String pk, String sk, String description) {
        this.pk = pk;
        this.sk = sk;
        this.description = description;

    }

    @DynamoDbPartitionKey
    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    @DynamoDbSortKey
    public String getSk() {
        return sk;
    }

    public void setSk(String sk) {
        this.sk = sk;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(pk, task.pk) && Objects.equals(sk, task.sk) && Objects.equals(description, task.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pk, sk, description);
    }
}