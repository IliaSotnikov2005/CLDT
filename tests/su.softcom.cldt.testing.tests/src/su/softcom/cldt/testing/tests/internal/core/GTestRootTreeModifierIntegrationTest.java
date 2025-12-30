package su.softcom.cldt.testing.tests.internal.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.prefs.BackingStoreException;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import su.softcom.cldt.core.CMakeProjectNature;
import su.softcom.cldt.core.CMakeUpdateNature;
import su.softcom.cldt.internal.core.builders.CMakeModifier;
import su.softcom.cldt.testing.gtest.core.GTestConstants;
import su.softcom.cldt.testing.gtest.core.model.GTestInstance;
import su.softcom.cldt.testing.gtest.internal.core.GTestRootTreeModifier;

class GTestRootTreeModifierIntegrationTest {

	private static final String PROJECT_NAME = "GTestModifierTestProject";
	private IProject project;

	@BeforeEach
	void createProject() throws CoreException {
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);

		if (project.exists()) {
			project.delete(true, new NullProgressMonitor());
		}

		project.create(new NullProgressMonitor());
		project.open(new NullProgressMonitor());

		IProjectDescription description = project.getDescription();
		List<String> natures = new ArrayList<>();
		natures.add(CMakeProjectNature.ID);
		natures.add(CMakeUpdateNature.ID);
		description.setNatureIds(natures.toArray(new String[0]));
		project.setDescription(description, new NullProgressMonitor());
	}

	@AfterEach
	void cleanupProject() throws CoreException {
		if (project != null && project.exists()) {
			project.delete(true, true, new NullProgressMonitor());
		}
	}

	private void setupGTestConfiguration(GTestInstance instance, String testsFolderName)
			throws BackingStoreException, JsonProcessingException {
		IEclipsePreferences prefs = new ProjectScope(project).getNode(GTestConstants.GTEST_NODE);
		prefs.put(GTestConstants.GTEST_INSTANCE_KEY, instance.toJson());
		prefs.put(GTestConstants.TESTS_FOLDER_KEY, testsFolderName);
		prefs.flush();
	}

	private void createFile(IFile file, String content) throws CoreException {
		if (file.exists()) {
			file.setContents(new ByteArrayInputStream(content.getBytes()), true, false, new NullProgressMonitor());
		} else {
			file.create(new ByteArrayInputStream(content.getBytes()), true, new NullProgressMonitor());
		}
	}

	private String readFile(IFile file) throws CoreException, IOException {
		try (var stream = file.getContents()) {
			return new String(stream.readAllBytes());
		}
	}

	@SuppressWarnings("restriction")
	@Test
	void modifyTree_shouldGenerateCorrectConfiguration_forSystemGTest()
			throws CoreException, BackingStoreException, IOException {
		GTestInstance gtestInstance = new GTestInstance("testPath", "1.12.1", GTestInstance.SYSTEM_TEXT, "Системный");
		setupGTestConfiguration(gtestInstance, "tests");

		String rootCmakeContent = """
				cmake_minimum_required(VERSION 3.20.0)
				project(TestProject C CXX ASM)
				""";
		createFile(project.getFile("CMakeLists.txt"), rootCmakeContent);

		IFolder testsFolder = project.getFolder("tests");
		if (!testsFolder.exists()) {
			testsFolder.create(true, true, new NullProgressMonitor());
		}

		IFile testsCmakeFile = testsFolder.getFile("CMakeLists.txt");
		createFile(testsCmakeFile, "");

		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, CMakeModifier.ID, null, new NullProgressMonitor());

		String updatedContent = readFile(testsCmakeFile);

		assertTrue(updatedContent.contains(GTestRootTreeModifier.GENERATED_COMMENT),
				"Expected start comment");
		assertTrue(updatedContent.contains("find_package(GTest REQUIRED)"), "Expected find_package for GTest");
		assertTrue(updatedContent.contains(GTestRootTreeModifier.END_GENERATED_COMMENT),
				"Expected end comment");
	}

	@SuppressWarnings("restriction")
	@Test
	void modifyTree_shouldAddGTestToExistingTargetLinkLibraries()
			throws CoreException, BackingStoreException, IOException {
		GTestInstance gtestInstance = new GTestInstance("", "1.12.2", GTestInstance.SYSTEM_TEXT, "Системный");
		setupGTestConfiguration(gtestInstance, "tests");

		String rootCmakeContent = """
				cmake_minimum_required(VERSION 3.20.0)
				project(TestProject)
				""";
		createFile(project.getFile("CMakeLists.txt"), rootCmakeContent);

		IFolder testsFolder = project.getFolder("tests");
		if (!testsFolder.exists()) {
			testsFolder.create(true, true, new NullProgressMonitor());
		}

		String testsCmakeContent = """
				add_executable(${PROJECT_NAME}.tests test1.cpp)

				target_link_libraries(${PROJECT_NAME}.tests
					PRIVATE
					some_other_lib
					another_lib GTest::gtest GTest::gtest_main
				)
				""";

		IFile testsCmakeFile = testsFolder.getFile("CMakeLists.txt");
		createFile(testsCmakeFile, testsCmakeContent);

		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, CMakeModifier.ID, null, new NullProgressMonitor());

		String updatedContent = readFile(testsCmakeFile);

		assertTrue(updatedContent.contains("target_link_libraries(${PROJECT_NAME}.tests"),
				"Expected target_link_libraries");
		assertTrue(updatedContent.contains("some_other_lib"), "Expected some_other_lib");
		assertTrue(updatedContent.contains("GTest::gtest"), "Expected GTest::gtest");
		assertTrue(updatedContent.contains("GTest::gtest_main"), "Expected GTest::gtest_main");
	}
	
	@SuppressWarnings("restriction")
	@Test
	void modifyTree_shouldGenerateEmbeddedGTestConfiguration()
			throws CoreException, BackingStoreException, IOException {
		String gtestPath = "/path/to/googletest";
		GTestInstance gtestInstance = new GTestInstance(gtestPath, "1.12.3", GTestInstance.EMBEDDED_TEXT,
				"Встроенный GTest");
		setupGTestConfiguration(gtestInstance, "tests");

		String rootCmakeContent = """
				cmake_minimum_required(VERSION 3.20.0)
				project(EmbeddedTest)
				""";
		createFile(project.getFile("CMakeLists.txt"), rootCmakeContent);

		IFolder testsFolder = project.getFolder("tests");
		if (!testsFolder.exists()) {
			testsFolder.create(true, true, new NullProgressMonitor());
		}

		String testsCmakeContent = """
				add_executable(${PROJECT_NAME}.tests test.cpp)
				""";

		IFile testsCmakeFile = testsFolder.getFile("CMakeLists.txt");
		createFile(testsCmakeFile, testsCmakeContent);

		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, CMakeModifier.ID, null, new NullProgressMonitor());

		String updatedContent = readFile(testsCmakeFile);

		assertTrue(updatedContent.contains("set(CLDT_GTEST_ROOT"),
				"Expected set CLDT_GTEST_ROOT");
		assertTrue(updatedContent.contains(gtestPath), "Expected path to GTest");
		assertTrue(updatedContent.contains("if(NOT EXISTS"), "Expected existence check for GTest");
		assertTrue(updatedContent.contains("add_subdirectory(${CLDT_GTEST_ROOT}"),
				"Expected add_subdirectory for GTest");
		assertTrue(updatedContent.contains("target_link_libraries(${PROJECT_NAME}.tests PRIVATE"),
				"Expected target_link_libraries");
		assertTrue(updatedContent.contains("gtest"), "Expected link to the gtest (not GTest::gtest)");
		assertTrue(updatedContent.contains("gtest_main"), "Expected link to the gtest_main (not GTest::gtest_main)");
	}

	@SuppressWarnings("restriction")
	@Test
	void modifyTree_shouldGenerateUserGTestConfiguration()
			throws CoreException, BackingStoreException, IOException {
		String gtestPath = "/custom/path/to/gtest";
		GTestInstance gtestInstance = new GTestInstance(gtestPath, "1.12.4", GTestInstance.USER_TEXT,
				"Пользовательский GTest");
		setupGTestConfiguration(gtestInstance, "tests");

		String rootCmakeContent = """
				cmake_minimum_required(VERSION 3.20.0)
				project(UserGTestProject)
				""";
		createFile(project.getFile("CMakeLists.txt"), rootCmakeContent);

		IFolder testsFolder = project.getFolder("tests");
		if (!testsFolder.exists()) {
			testsFolder.create(true, true, new NullProgressMonitor());
		}

		IFile testsCmakeFile = testsFolder.getFile("CMakeLists.txt");
		createFile(testsCmakeFile, "add_executable(${PROJECT_NAME}.test test.cpp)");

		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, CMakeModifier.ID, null, new NullProgressMonitor());

		String updatedContent = readFile(testsCmakeFile);

		assertTrue(updatedContent.contains("set(CLDT_GTEST_ROOT"),
				"Expected set CLDT_GTEST_ROOT");
		assertTrue(updatedContent.contains(gtestPath), "Expected gtest path");
		assertTrue(updatedContent.contains("if(NOT EXISTS"), "Expected existence check for the GTest");
	}

	@SuppressWarnings("restriction")
	@Test
	void modifyTree_shouldNotGenerateAnything_whenNoGTestConfiguration()
			throws CoreException, BackingStoreException, IOException {
		IEclipsePreferences prefs = new ProjectScope(project).getNode(GTestConstants.GTEST_NODE);
		prefs.remove(GTestConstants.GTEST_INSTANCE_KEY);
		prefs.flush();

		String rootCmakeContent = """
				cmake_minimum_required(VERSION 3.20.0)
				project(NoGTestProject)
				""";
		createFile(project.getFile("CMakeLists.txt"), rootCmakeContent);

		IFolder testsFolder = project.getFolder("tests");
		if (!testsFolder.exists()) {
			testsFolder.create(true, true, new NullProgressMonitor());
		}

		String originalTestsContent = """
				add_executable(${PROJECT_NAME}.test test.cpp)
				target_link_libraries(${PROJECT_NAME}.test PRIVATE some_lib)
				""";

		IFile testsCmakeFile = testsFolder.getFile("CMakeLists.txt");
		createFile(testsCmakeFile, originalTestsContent);

		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, CMakeModifier.ID, null, new NullProgressMonitor());

		String updatedContent = readFile(testsCmakeFile);

		assertFalse(updatedContent.contains("Сгенерировано GTest-модификатором"),
				"GTest comment is not expected");
		assertFalse(updatedContent.contains("find_package(GTest"), "find_package is not expected");
		assertFalse(updatedContent.contains("CLDT_GTEST_ROOT"), "CLDT_GTEST_ROOT is not expected");

		assertTrue(updatedContent.contains("add_executable(${PROJECT_NAME}.test"),
				"Expected add_executable");
		assertTrue(updatedContent.contains("target_link_libraries(${PROJECT_NAME}.test"),
				"Expected target_link_libraries");
	}

	@SuppressWarnings("restriction")
	@Test
	void modifyTree_shouldNotGenerateAnything_whenTestsFolderDoesNotExist()
			throws CoreException, BackingStoreException, IOException {
		GTestInstance gtestInstance = new GTestInstance("", "1.12.5", GTestInstance.SYSTEM_TEXT, "Системный");
		setupGTestConfiguration(gtestInstance, "non_existing_folder");

		String rootCmakeContent = """
				cmake_minimum_required(VERSION 3.20.0)
				project(NoTestsFolderProject)
				""";
		createFile(project.getFile("CMakeLists.txt"), rootCmakeContent);

		IFolder testsFolder = project.getFolder("non_existing_folder");
		assertFalse(testsFolder.exists(), "Folder must not exist");

		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, CMakeModifier.ID, null, new NullProgressMonitor());

		String rootContent = readFile(project.getFile("CMakeLists.txt"));

		assertFalse(rootContent.contains("add_subdirectory(non_existing_folder)"),
				"add_subdirectory is not expected");
	}

	@SuppressWarnings("restriction")
	@Test
	void modifyTree_shouldGenerateForCustomTestsFolderName()
			throws CoreException, BackingStoreException, IOException {
		GTestInstance gtestInstance = new GTestInstance("last", "1.12.6", GTestInstance.SYSTEM_TEXT, "Системный");
		setupGTestConfiguration(gtestInstance, "unit_tests");

		String rootCmakeContent = """
				cmake_minimum_required(VERSION 3.20.0)
				project(CustomTestsFolderProject)
				""";
		createFile(project.getFile("CMakeLists.txt"), rootCmakeContent);

		IFolder testsFolder = project.getFolder("unit_tests");
		if (!testsFolder.exists()) {
			testsFolder.create(true, true, new NullProgressMonitor());
		}

		IFile testsCmakeFile = testsFolder.getFile("CMakeLists.txt");
		createFile(testsCmakeFile, "");

		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, CMakeModifier.ID, null, new NullProgressMonitor());

		String rootContent = readFile(project.getFile("CMakeLists.txt"));
		String testsContent = readFile(testsCmakeFile);

		assertTrue(rootContent.contains("add_subdirectory(unit_tests)"),
				"Expected add_subdirectory");

		assertTrue(testsContent.contains("Сгенерировано GTest-модификатором"), "Expected GTest section");
		assertTrue(testsContent.contains("find_package(GTest REQUIRED)"), "Expected find_package GTest");
	}
}