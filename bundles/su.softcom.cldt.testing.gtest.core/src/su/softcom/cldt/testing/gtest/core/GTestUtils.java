package su.softcom.cldt.testing.gtest.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.fasterxml.jackson.core.JsonProcessingException;

import su.softcom.cldt.core.cmake.CMakeParser;
import su.softcom.cldt.core.cmake.CMakeParser.UnexpectedTokenException;
import su.softcom.cldt.core.cmake.CMakeRoot;
import su.softcom.cldt.testing.gtest.core.model.GTestInstance;
import su.softcom.cldt.testing.gtest.internal.core.GTestWizardTreeModifier;

/**
 * Вспомогательный класс для работы с GTest.
 */
public final class GTestUtils {

	private static final String TEMPLATES_BASE_PATH = "resources/templates/";
	private static final Bundle BUNDLE = FrameworkUtil.getBundle(GTestUtils.class);

	private static final VelocityEngine velocityEngine = initVelocityEngine();

	private GTestUtils() {
		// utility class
	}

	private static VelocityEngine initVelocityEngine() {
		try {
			URL templatesUrl = FileLocator.toFileURL(BUNDLE.getEntry(TEMPLATES_BASE_PATH));
			VelocityEngine ve = new VelocityEngine();
			File templatesFolder = new File(templatesUrl.toURI());

			ve.setProperty("file.resource.loader.path", templatesFolder.getAbsolutePath());
			ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
			ve.setProperty(RuntimeConstants.INPUT_ENCODING, StandardCharsets.UTF_8.name());

			ve.init();
			return ve;
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Создаёт папку для тестов в проекте, если такой нет.
	 * 
	 * @param project    проект
	 * @param folderName имя папки для тестов
	 * @return объект папки, null в случае ошибки
	 */
	public static IFolder createTestsFolderIfNotExists(IProject project, String folderName) {
		if (project == null || folderName == null || folderName.trim().isEmpty()) {
			return null;
		}

		folderName = folderName.trim();
		IFolder testsFolder = project.getFolder(folderName);

		if (!testsFolder.exists()) {
			try {
				testsFolder.create(true, true, new NullProgressMonitor());

				createTestsCMakeLists(project, testsFolder);
			} catch (CoreException e) {
				Platform.getLog(BUNDLE).error(e.getMessage());
				return null;
			}
		}

		return testsFolder;
	}

	/**
	 * Создаёт начальное содержимое файла конфигурации CMake для тестов.
	 * @return начальное содержимое файла конфигурации
	 */
	public static String generateCMakeListsContent() {
		return loadTemplate("CMakeLists.vm", new HashMap<>());
	}

	/**
	 * Получает экземпляр GTest для переданного проекта.
	 * 
	 * @param project проект
	 * @return экземпляр GTest, null в случае пустых настроек
	 */
	public static GTestInstance getGTestInstanceForProject(IProject project) {
		IEclipsePreferences preferences = new ProjectScope(project.getProject()).getNode(GTestConstants.GTEST_NODE);
		GTestInstance instance = GTestInstance.fromJson(preferences.get(GTestConstants.GTEST_INSTANCE_KEY, ""));
		if (instance != null) {
			return instance;
		}

		instance = getDefaultGTestInstance();
		if (instance == null) {
			return null;
		}

		try {
			preferences.put(GTestConstants.GTEST_INSTANCE_KEY, instance.toJson());
		} catch (JsonProcessingException e) {
			Platform.getLog(GTestUtils.class).error(e.getMessage());
		}

		return instance;
	}

	/**
	 * Добавляет тест в цель сборки или создает новую цель сборки для тестов.
	 * 
	 * @param testsFolder  папка тестов
	 * @param testFileName имя тестового файла
	 */
	public static void modifyTestsCMakeLists(IFolder testsFolder, String testFileName) {
		IFile cmakeFile = testsFolder.getFile(GTestConstants.CMAKELISTS);

		if (!cmakeFile.exists()) {
			return;
		}

		try {
			String content;
			try (InputStream stream = cmakeFile.getContents()) {
				content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
			}

			CMakeParser parser = new CMakeParser(content, false);
			CMakeRoot root = parser.parse();

			GTestWizardTreeModifier.modifyCMakeTree(root, testFileName);
			root.writeToFile(cmakeFile, new NullProgressMonitor());
		} catch (IOException | CoreException | UnexpectedTokenException e) {
			Platform.getLog(GTestUtils.class).error(e.getMessage());;
		}
	}

	private static void createTestsCMakeLists(IProject project, IFolder testsFolder) throws CoreException {
		IFile cmakeFile = testsFolder.getFile("CMakeLists.txt");
		if (!cmakeFile.exists()) {
			String cmakeContent = generateCMakeListsContent();
			cmakeFile.create(new ByteArrayInputStream(cmakeContent.getBytes(StandardCharsets.UTF_8)), true, null);
		}
	}

	private static GTestInstance getDefaultGTestInstance() {
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(GTestConstants.GTEST_NODE);
		String instancesJson = preferences.get(GTestConstants.GTEST_INSTANCES_KEY, "");
		if (instancesJson.equals("")) {
			return null;
		}

		GTestInstance embedded = null;
		for (GTestInstance instance : GTestInstance.listFromJson(instancesJson)) {
			if (instance.type().equals(GTestInstance.SYSTEM_TEXT)) {
				return instance;
			} else if (instance.path().equals(GTestInstance.EMBEDDED_TEXT)) {
				embedded = instance;
			}
		}

		return embedded;
	}

	private static String loadTemplate(String templateName, Map<String, Object> context) {
		Template template = velocityEngine.getTemplate(templateName, "UTF-8");
		StringWriter writer = new StringWriter();
		template.merge(new VelocityContext(context), writer);

		return writer.toString();
	}
}
