package su.softcom.cldt.testing.tests.internal.core;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import su.softcom.cldt.core.cmake.CMakeNode;
import su.softcom.cldt.core.cmake.CMakeParser;
import su.softcom.cldt.core.cmake.CMakeRoot;
import su.softcom.cldt.core.cmake.CommandNode;
import su.softcom.cldt.core.cmake.CommentNode;
import su.softcom.cldt.core.cmake.ICMakeProject;
import su.softcom.cldt.testing.gtest.core.GTestConstants;
import su.softcom.cldt.testing.gtest.core.GTestUtils;
import su.softcom.cldt.testing.gtest.core.model.GTestInstance;
import su.softcom.cldt.testing.gtest.internal.core.GTestRootTreeModifier;

@RunWith(MockitoJUnitRunner.class)
public class GTestRootTreeModifierUnitTest {

	@Mock
	private IProject mockProject;

	@Mock
	private ICMakeProject mockCMakeProject;

	@Mock
	private IFolder mockTestsFolder;

	private GTestRootTreeModifier modifier;

	@Test
	public void testModifyTree_WithGTestInstance_AddsSubdirectory() throws Exception {
		String cmakeContent = """
				cmake_minimum_required(VERSION 3.10)
				project(MyProject)

				add_executable(main main.cpp)
				""";

		CMakeParser parser = new CMakeParser(cmakeContent, false);
		CMakeRoot tree = parser.parse();

		GTestInstance gtest = new GTestInstance("path", "1.12.1", GTestInstance.EMBEDDED_TEXT);

		try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
			gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any())).thenReturn(gtest);

			when(mockCMakeProject.getProject()).thenReturn(mockProject);

			modifier = new GTestRootTreeModifier() {
				@Override
				protected IFolder findTestsFolder() {
					when(mockTestsFolder.getName()).thenReturn("tests");

					// нужно, чтобы не запускался вспомогательный модификатор
					IFile mockFile = mock(IFile.class);
					when(mockTestsFolder.getFile(GTestConstants.CMAKELISTS)).thenReturn(mockFile);
					when(mockFile.exists()).thenReturn(false);

					return mockTestsFolder;
				}
			};

			modifier.modifyTree(mockCMakeProject, tree);

			List<CMakeNode> children = tree.getChildren();

			boolean foundStartComment = false;
			boolean foundEndComment = false;
			boolean foundAddSubdirectory = false;
			for (CMakeNode child : children) {
				if (child instanceof CommentNode comment) {
					String text = comment.toText().toString().trim();
					if (text.contains(GTestRootTreeModifier.GENERATED_COMMENT)) {
						foundStartComment = true;
					}
					if (text.contains(GTestRootTreeModifier.END_GENERATED_COMMENT)) {
						foundEndComment = true;
					}
				}
				if (child instanceof CommandNode cmd && "add_subdirectory".equals(cmd.getName())
						&& !foundAddSubdirectory
						&& cmd.getArgs().stream().anyMatch(arg -> "tests".equals(arg.getValue()))) {
					foundAddSubdirectory = true;
				}
			}

			assertTrue("Should have start comment", foundStartComment);
			assertTrue("Should have end comment", foundEndComment);
			assertTrue("Should have add_subdirectory(tests)", foundAddSubdirectory);
		}
	}

	@Test
	public void testModifyTree_WhenNoGTestInstance() throws Exception {
		String cmakeContent = "cmake_minimum_required(VERSION 3.10)\nproject(MyProject)";
		CMakeParser parser = new CMakeParser(cmakeContent, false);
		CMakeRoot tree = parser.parse();

		int initialSize = tree.getChildren().size();

		try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
			gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any())).thenReturn(null);

			modifier = new GTestRootTreeModifier();

			modifier.modifyTree(mockCMakeProject, tree);

			assertEquals("Tree should not be modified when no GTest instance", initialSize, tree.getChildren().size());
		}
	}

	@Test
	public void testModifyTree_WhenTestsFolderDoesNotExist() throws Exception {
		String cmakeContent = "cmake_minimum_required(VERSION 3.10)\nproject(MyProject)";
		CMakeParser parser = new CMakeParser(cmakeContent, false);
		CMakeRoot tree = parser.parse();

		int initialSize = tree.getChildren().size();

		GTestInstance gtest = new GTestInstance("path", "1.12.1", GTestInstance.SYSTEM_TEXT, "Системный");

		try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
			gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any())).thenReturn(gtest);

			when(mockCMakeProject.getProject()).thenReturn(mockProject);

			modifier = new GTestRootTreeModifier() {
				@Override
				protected IFolder findTestsFolder() {
					return null;
				}
			};

			modifier.modifyTree(mockCMakeProject, tree);

			assertEquals("Tree should not be modified when tests folder doesn't exist", initialSize,
					tree.getChildren().size());
		}
	}

	@Test
	public void testModifyTree_WhenGTestPathIsEmpty() throws Exception {
		String cmakeContent = "cmake_minimum_required(VERSION 3.10)\nproject(MyProject)";
		CMakeParser parser = new CMakeParser(cmakeContent, false);
		CMakeRoot tree = parser.parse();

		GTestInstance gtest = new GTestInstance("", "1.12.1", GTestInstance.USER_TEXT);

		try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
			gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any())).thenReturn(gtest);

			when(mockCMakeProject.getProject()).thenReturn(mockProject);

			modifier = new GTestRootTreeModifier() {
				@Override
				protected IFolder findTestsFolder() {
					return mockTestsFolder;
				}
			};

			modifier.modifyTree(mockCMakeProject, tree);

			assertEquals("Tree should not be modified when GTest path is null", 2, tree.getChildren().size());
		}
	}

	@Test
	public void testModifyTree_RemovesExistingGTestSection() throws Exception {
		String cmakeContent = """
				cmake_minimum_required(VERSION 3.10)
				project(MyProject)

				#---------- Сгенерировано GTest-модификатором CMake дерева ------
				add_subdirectory(tests_old)
				#---------- Конец сгенерированного GTest-модификатором ----------

				add_executable(main main.cpp)
				""";

		CMakeParser parser = new CMakeParser(cmakeContent, false);
		CMakeRoot tree = parser.parse();

		GTestInstance gtest = new GTestInstance("path", "1.12.1", GTestInstance.USER_TEXT);

		try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
			gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any())).thenReturn(gtest);

			when(mockCMakeProject.getProject()).thenReturn(mockProject);

			modifier = new GTestRootTreeModifier() {
				@Override
				protected IFolder findTestsFolder() {
					when(mockTestsFolder.getName()).thenReturn("tests_new");

					IFile mockFile = mock(IFile.class);
					when(mockTestsFolder.getFile(GTestConstants.CMAKELISTS)).thenReturn(mockFile);
					when(mockFile.exists()).thenReturn(false);

					return mockTestsFolder;
				}
			};

			modifier.modifyTree(mockCMakeProject, tree);

			List<CMakeNode> children = tree.getChildren();

			int gtestCommentCount = 0;
			int addSubdirCount = 0;

			for (CMakeNode child : children) {
				if (child instanceof CommentNode comment) {
					String text = comment.toText().toString().trim();
					if (text.contains("Сгенерировано GTest-модификатором")
							|| text.contains("Конец сгенерированного GTest-модификатором")) {
						gtestCommentCount++;
					}
				} else if (child instanceof CommandNode cmd && "add_subdirectory".equals(cmd.getName())
						&& cmd.getArgs().stream().anyMatch(arg -> "tests_new".equals(arg.getValue()))) {
					addSubdirCount++;
				}
			}

			assertEquals("Should have exactly 2 GTest comments", 2, gtestCommentCount);
			assertEquals("Should have exactly one add_subdirectory(tests)", 1, addSubdirCount);
		}
	}

	@Test
	public void testModifyTree_DoesNotAddDuplicateSubdirectory() throws Exception {
		String cmakeContent = """
				cmake_minimum_required(VERSION 3.10)
				project(MyProject)

				add_subdirectory(tests)

				add_executable(main main.cpp)
				""";

		CMakeParser parser = new CMakeParser(cmakeContent, false);
		CMakeRoot tree = parser.parse();

		int initialNodeCount = tree.getChildren().size();

		GTestInstance gtest = new GTestInstance("path", "1.12.1", GTestInstance.USER_TEXT);

		try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
			gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any())).thenReturn(gtest);

			when(mockCMakeProject.getProject()).thenReturn(mockProject);

			modifier = new GTestRootTreeModifier() {
				@Override
				protected IFolder findTestsFolder() {
					when(mockTestsFolder.getName()).thenReturn("tests");

					IFile mockFile = mock(IFile.class);
					when(mockTestsFolder.getFile(GTestConstants.CMAKELISTS)).thenReturn(mockFile);
					when(mockFile.exists()).thenReturn(false);

					return mockTestsFolder;
				}
			};

			modifier.modifyTree(mockCMakeProject, tree);

			assertEquals("Should not add duplicate nodes", initialNodeCount, tree.getChildren().size());
		}
	}

	@Test
	public void testModifyTree_InsertionPosition_AfterLastTarget() throws Exception {
		String cmakeContent = """
				cmake_minimum_required(VERSION 3.10)
				project(MyProject)

				add_library(mylib src.cpp)
				add_executable(myapp main.cpp)
				""";

		CMakeParser parser = new CMakeParser(cmakeContent, false);
		CMakeRoot tree = parser.parse();

		GTestInstance gtest = new GTestInstance("path", "1.12.1", GTestInstance.USER_TEXT);

		try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
			gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any())).thenReturn(gtest);

			when(mockCMakeProject.getProject()).thenReturn(mockProject);

			modifier = new GTestRootTreeModifier() {
				@Override
				protected IFolder findTestsFolder() {
					when(mockTestsFolder.getName()).thenReturn("tests");

					IFile mockFile = mock(IFile.class);
					when(mockTestsFolder.getFile(GTestConstants.CMAKELISTS)).thenReturn(mockFile);
					when(mockFile.exists()).thenReturn(false);

					return mockTestsFolder;
				}
			};

			modifier.modifyTree(mockCMakeProject, tree);

			List<CMakeNode> children = tree.getChildren();
			int addSubdirIndex = -1;
			int addExecutableIndex = -1;

			for (int i = 0; i < children.size(); i++) {
				CMakeNode child = children.get(i);
				if (child instanceof CommandNode cmd && "add_subdirectory".equals(cmd.getName())
						&& cmd.getArgs().stream().anyMatch(arg -> "tests".equals(arg.getValue()))) {
					addSubdirIndex = i;
				} else if (child instanceof CommandNode cmd && "add_executable".equals(cmd.getName())
						&& "myapp".equals(cmd.getArgs().get(0).getValue())) {
					addExecutableIndex = i;
				}
			}

			assertTrue("GTest section should be after the last target", addSubdirIndex > addExecutableIndex);
		}
	}

	@Test
	public void testModifyTree_InsertionPosition_AfterProjectWhenNoTargets() throws Exception {
		String cmakeContent = """
				cmake_minimum_required(VERSION 3.10)
				project(MyProject)
				""";

		CMakeParser parser = new CMakeParser(cmakeContent, false);
		CMakeRoot tree = parser.parse();

		GTestInstance gtest = new GTestInstance("path", "1.12.1", GTestInstance.USER_TEXT);

		try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
			gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any())).thenReturn(gtest);

			when(mockCMakeProject.getProject()).thenReturn(mockProject);

			modifier = new GTestRootTreeModifier() {
				@Override
				protected IFolder findTestsFolder() {
					when(mockTestsFolder.getName()).thenReturn("tests");

					IFile mockFile = mock(IFile.class);
					when(mockTestsFolder.getFile(GTestConstants.CMAKELISTS)).thenReturn(mockFile);
					when(mockFile.exists()).thenReturn(false);

					return mockTestsFolder;
				}
			};

			modifier.modifyTree(mockCMakeProject, tree);

			List<CMakeNode> children = tree.getChildren();
			int addSubdirIndex = -1;
			int projectIndex = -1;

			for (int i = 0; i < children.size(); i++) {
				CMakeNode child = children.get(i);
				if (child instanceof CommandNode cmd && "add_subdirectory".equals(cmd.getName())
						&& cmd.getArgs().stream().anyMatch(arg -> "tests".equals(arg.getValue()))) {
					addSubdirIndex = i;
				} else if (child instanceof CommandNode cmd && "project".equals(cmd.getName())) {
					projectIndex = i;
				}
			}

			assertTrue("GTest section should be after project when there are no targets",
					addSubdirIndex > projectIndex);
		}
	}

	@Test
	public void testModifyTree_UsesTestsFolderFromPreferences() throws Exception {
		String cmakeContent = "cmake_minimum_required(VERSION 3.10)\nproject(MyProject)";
		CMakeParser parser = new CMakeParser(cmakeContent, false);
		CMakeRoot tree = parser.parse();

		GTestInstance gtest = new GTestInstance("path", "1.12.1", GTestInstance.USER_TEXT);

		try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
			gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any())).thenReturn(gtest);

			when(mockCMakeProject.getProject()).thenReturn(mockProject);

			modifier = new GTestRootTreeModifier() {
				@Override
				protected IFolder findTestsFolder() {
					IFolder customTestsFolder = mock(IFolder.class);
					when(customTestsFolder.getName()).thenReturn("my_custom_tests");

					IFile mockFile = mock(IFile.class);
					when(customTestsFolder.getFile(GTestConstants.CMAKELISTS)).thenReturn(mockFile);
					when(mockFile.exists()).thenReturn(false);

					return customTestsFolder;
				}
			};

			modifier.modifyTree(mockCMakeProject, tree);

			boolean foundCustomSubdirectory = false;
			for (CMakeNode child : tree.getChildren()) {
				if (child instanceof CommandNode cmd && "add_subdirectory".equals(cmd.getName())
						&& cmd.getArgs().stream().anyMatch(arg -> "my_custom_tests".equals(arg.getValue()))) {
					foundCustomSubdirectory = true;
					break;
				}
			}

			assertTrue("Should use custom tests folder name", foundCustomSubdirectory);
		}
	}
}