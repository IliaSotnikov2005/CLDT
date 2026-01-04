package su.softcom.cldt.testing.gtest.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import su.softcom.cldt.core.ILibrary;
import su.softcom.cldt.testing.gtest.core.GTestConstants;
import su.softcom.cldt.testing.gtest.core.model.GTestInstance;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Менеджер экземпляров GTest.
 */
public class GTestInstancesManager {

	private static final String CONFIG_VERSION_FILE_NAME = "GTestConfigVersion.cmake";

	private GTestInstancesManager() {
		// empty
	}

	private static final Pattern VERSION_REGEX = Pattern
			.compile("set\\s*\\(\\s*PACKAGE_VERSION\\s+\"(\\d+\\.\\d+\\.\\d+)\"\\)");

	private static List<GTestInstance> instances = new ArrayList<>();

	private static final List<Path> SYSTEM_CONFIG_PATHS = Arrays.asList(
			// Linux
			Paths.get("/usr/lib/cmake/GTest"), Paths.get("/usr/local/lib/cmake/GTest"),

			// Windows
			Paths.get("C:/Program Files/GTest/lib/cmake/GTest"),
			Paths.get("C:/Program Files (x86)/GTest/lib/cmake/GTest"));

	/**
	 * Получает список экземпляров GTest из настроек.
	 * Проверяет существование директорий сохранённых GTest.
	 * Производит поиск системного GTest.
	 * 
	 * @return список экземпляров GTest
	 */
	public static List<GTestInstance> getRefreshedInstanses() {
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(GTestConstants.GTEST_NODE);
		String currentInstances = preferences.get(GTestConstants.GTEST_INSTANCES_KEY, "");

		List<GTestInstance> updatedInstances = new ArrayList<>();

		boolean hasEmbedded = false;
		if (!currentInstances.isEmpty()) {
			for (GTestInstance instance : GTestInstance.listFromJson(currentInstances)) {
				if (Files.exists(Paths.get(instance.path()))) {
					updatedInstances.add(instance);
				}
				
				if (instance.type().equals(GTestInstance.EMBEDDED_TEXT)) {
					hasEmbedded = true;
				}
			}
		}
		
		if (!hasEmbedded) {
			ILibrary lib = Activator.getLibrary();
			updatedInstances.add(new GTestInstance(lib.includeDir().getParent().toAbsolutePath().toString(),
					lib.version().toString(), GTestInstance.EMBEDDED_TEXT));
		}

		GTestInstance systemInstance = discoverSystemGTest();
		if (systemInstance != null && !updatedInstances.contains(systemInstance)) {
			updatedInstances.add(systemInstance);
		}
		
		instances = updatedInstances;

		return updatedInstances;
	}

	/**
	 * Получает текущий не обновлённый список экземпляров GTest.
	 * 
	 * @return список экземпляров GTest.
	 */
	public static List<GTestInstance> getInstances() {
		return instances;
	}

	private static GTestInstance discoverSystemGTest() {
		for (Path configPath : SYSTEM_CONFIG_PATHS) {
			Path configFile = configPath.resolve(GTestConstants.CONFIG_FILE_NAME);
			if (Files.exists(configFile)) {
				return createSystemGTestInstance(configFile);
			}

		}

		return null;
	}

	private static GTestInstance createSystemGTestInstance(Path configFile) {
		String version = extractVersionFromConfig(configFile);
		if (version == null) {
			version = "неизвестно";
		}

		return new GTestInstance(configFile.toAbsolutePath().toString(), version, GTestInstance.SYSTEM_TEXT,
				"GoogleTest");
	}

	private static String extractVersionFromConfig(Path configFile) {
		try {
			Path versionFile = configFile.getParent().resolve(CONFIG_VERSION_FILE_NAME);
			if (Files.exists(versionFile)) {
				String content = Files.readString(versionFile);
				var matcher = VERSION_REGEX.matcher(content);
				if (matcher.find()) {
					return matcher.group(1);
				}
			}

			return null;
		} catch (IOException e) {
			return null;
		}
	}
}