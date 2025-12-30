package su.softcom.cldt.testing.gtest.core.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Представляет экземпляр GTest.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record GTestInstance(
		String path,
		String version,
		String type,
		String tag) {

	public static final String EMBEDDED_TEXT = "Встроенный";
	public static final String SYSTEM_TEXT = "Системный";
	public static final String USER_TEXT = "Пользовательский";

	private static final ObjectMapper objectMapper = createObjectMapper();
	
	/**
	 * Создаёт новый GTestInstance.
	 * @param path путь к папке/файлу конфигурации
	 * @param version версия
	 * @param type тип: "Встроенный", "Системный", "Пользовательский"
	 * @param tag метка для экземпляра, отображается вместо path в UI
	 */
	public GTestInstance(String path, String version, String type, String tag) {
        this.path = path != null ? path : "";
        this.version = version;
        this.type = type;
        this.tag = tag != null ? tag : "";
    }
	
	/**
	 * Создаёт новый GTestInstance c пустой меткой.
	 * @param path путь к папке/файлу конфигурации
	 * @param version версия
	 * @param type тип: "Встроенный", "Системный", "Пользовательский"
	 */
	public GTestInstance(String path, String version, String type) {
        this(path, version, type, "");
    }


	/**
	 * Получает путь к GTest в виде IPath.
	 * @return путь в виде IPath
	 */
	public Path asPath() {
	    return Paths.get(path);
	}

	/**
	 * Получает получает GTestInstance из json.
	 * @param json json
	 * @return объект GTestInstance
	 */
	public static GTestInstance fromJson(String json) {
		try {
			return objectMapper.readValue(json, GTestInstance.class);
		} catch (JsonProcessingException e) {
			return null;
		}
	}
	
	/**
	 * Получает список GTestInstance из json.
	 * @param json json
	 * @return список GTestInstance
	 */
	public static List<GTestInstance> listFromJson(String json) {
		try {
			return objectMapper.readValue(json, new TypeReference<List<GTestInstance>>() {
			});
		} catch (JsonProcessingException e) {
			return new ArrayList<>();
		}
	}

	/**
	 * Преобразует GTestInstance в json.
	 * @return json-представление GTestInstance
	 * @throws JsonProcessingException при ошибке преобразования
	 */
	public String toJson() throws JsonProcessingException {
		return objectMapper.writeValueAsString(this);
	}
 
	/**
	 * Преобразует список GTestInstance в json.
	 * @return json-представление списка GTestInstance
	 * @throws JsonProcessingException при ошибке преобразования
	 */
	public static String listToJson(List<GTestInstance> instances) throws JsonProcessingException {
		return objectMapper.writeValueAsString(instances);
	}

	private static ObjectMapper createObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
		return mapper;
	}
}