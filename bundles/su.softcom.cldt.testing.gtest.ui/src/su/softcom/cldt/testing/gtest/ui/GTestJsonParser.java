package su.softcom.cldt.testing.gtest.ui;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import su.softcom.cldt.testing.gtest.ui.models.GTestResult;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;

/**
 * Парсер json-отчёта работы GTest.
 */
public class GTestJsonParser {
    private final ObjectMapper objectMapper;

    /**
     * Создаёт новый объект {@link GTestJsonParser}.
     */
    public GTestJsonParser() {
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Парсит json-отчёт работы GTest.
     * @param jsonFile файл отчёта GTest.
     * @return объект {@link GTestResult}.
     * @throws {@link IOException} если json-файл не найден.
     */
    public GTestResult parseJsonFile(IFile jsonFile) throws IOException {
        if (!jsonFile.exists()) {
            throw new IOException("JSON file not found: " + jsonFile.getFullPath().toOSString());
        }
        
        File file = jsonFile.getLocation().toFile();
        return objectMapper.readValue(file, GTestResult.class);
    }
}