package util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import model.Task;

import java.util.List;

public class Csv {

    public static byte[] generateCsv(List<Task> tasks) throws JsonProcessingException {
        CsvMapper csvMapper = new CsvMapper();
        csvMapper.configure(CsvGenerator.Feature.ALWAYS_QUOTE_STRINGS, true);

        CsvSchema schema = csvMapper.schemaFor(Task.class).withHeader();
        ObjectWriter writer = csvMapper.writer(schema);
        String csvString = writer.writeValueAsString(tasks);

        return csvString.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}