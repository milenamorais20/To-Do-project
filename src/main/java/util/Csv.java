package util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import model.Task;

import java.util.List;

/**
 * Classe utilitária para manipulação de arquivos CSV.
 */
public class Csv {

    /**
     * Gera um array de bytes representando um arquivo CSV a partir de uma lista de tarefas.
     * @param tasks A lista de objetos Task a ser convertida.
     * @return Um array de bytes do conteúdo CSV.
     * @throws JsonProcessingException Se ocorrer um erro durante a serialização.
     */
    public static byte[] generateCsv(List<Task> tasks) throws JsonProcessingException {
        CsvMapper csvMapper = new CsvMapper();
        // Configura o CsvMapper para sempre colocar aspas em todos os campos.
        csvMapper.configure(CsvGenerator.Feature.ALWAYS_QUOTE_STRINGS, true);

        // Define o esquema do CSV com base na classe Task, incluindo um cabeçalho.
        CsvSchema schema = csvMapper.schemaFor(Task.class).withHeader();
        ObjectWriter writer = csvMapper.writer(schema);
        String csvString = writer.writeValueAsString(tasks);

        // Converte a string CSV para bytes usando o padrão UTF-8.
        return csvString.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}