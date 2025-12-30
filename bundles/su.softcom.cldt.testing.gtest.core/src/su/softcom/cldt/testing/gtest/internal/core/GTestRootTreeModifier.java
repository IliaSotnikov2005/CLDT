package su.softcom.cldt.testing.gtest.internal.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import su.softcom.cldt.core.cmake.ArgumentNode;
import su.softcom.cldt.core.cmake.CMakeNode;
import su.softcom.cldt.core.cmake.CMakeParser;
import su.softcom.cldt.core.cmake.CMakeRoot;
import su.softcom.cldt.core.cmake.CommandNode;
import su.softcom.cldt.core.cmake.CommentNode;
import su.softcom.cldt.core.cmake.ConditionCommand;
import su.softcom.cldt.core.cmake.ICMakeProject;
import su.softcom.cldt.core.cmake.ICMakeTreeModifier;
import su.softcom.cldt.testing.gtest.core.GTestConstants;
import su.softcom.cldt.testing.gtest.core.GTestUtils;
import su.softcom.cldt.testing.gtest.core.model.GTestInstance;

/**
 * Модификатор CMake дерева. Добавляет директорию с тестами в корневую конфигурацию CMake.
 * Для конфигурации CMake тестов вызывает дополнительный модификатор.
 * Сгенерированный код помечается комментариями.
 */
public class GTestRootTreeModifier implements ICMakeTreeModifier {
	public static final String GENERATED_COMMENT =     "---------- Сгенерировано GTest-модификатором CMake дерева ------";
	public static final String END_GENERATED_COMMENT = "---------- Конец сгенерированного GTest-модификатором ----------";
	
	IFolder testsFolder;
	CMakeRoot tree;
	ICMakeProject project;
	
	public GTestRootTreeModifier() {
		// empty
	}

	@Override
	public void modifyTree(ICMakeProject project, CMakeRoot root) {
		GTestInstance instance = GTestUtils.getGTestInstanceForProject(project.getProject());

		if (instance == null) {
			return;
		}

		tree = root;
		this.project = project;

		testsFolder = findTestsFolder();
		if (testsFolder == null) {
			return;
		}

		if (!instance.path().isEmpty()) {
			removeGTestSection(root);
			addTestsSubdirectory(root);
			modifyTestsCMakeLists();
		}
	}

	private void modifyTestsCMakeLists() {
		try {
			IFile testsCMakeFile = testsFolder.getFile(GTestConstants.CMAKELISTS);
			if (!testsCMakeFile.exists()) {
				return;
			}

			String content = readFromFile(testsCMakeFile, GTestUtils.generateCMakeListsContent());

			CMakeParser parser = new CMakeParser(content, false);
			CMakeRoot testsTree = parser.parse();

			GTestTestConfigurationTreeModifier modifier = new GTestTestConfigurationTreeModifier();
			modifier.modifyTree(project, testsTree);

			testsTree.writeToFile(testsCMakeFile, new NullProgressMonitor());
		} catch (CoreException | IOException | CMakeParser.UnexpectedTokenException e) {
			Platform.getLog(getClass()).error("Ошибка модификации tests/CMakeLists.txt: " + e.getMessage());
		}
	}

	protected IFolder findTestsFolder() {
		IEclipsePreferences preferences = new ProjectScope(project.getProject()).getNode(GTestConstants.GTEST_NODE);
		String testsDirectoryName = preferences.get(GTestConstants.TESTS_FOLDER_KEY,
				GTestConstants.DEFAULT_TESTS_FOLDER_NAME);
		IFolder testsDirectory = project.getProject().getFolder(testsDirectoryName);
		if (testsDirectory.exists()) {
			return testsDirectory;
		}

		return null;
	}

	private void removeGTestSection(CMakeRoot root) {
		List<CMakeNode> toRemove = new ArrayList<>();
		boolean inGTestBlock = false;

		for (CMakeNode child : root.getChildren()) {
			if (child instanceof CommentNode comment) {
				String commentText = comment.toText().toString().trim();
				if (commentText.equals("#" + GENERATED_COMMENT)) {
					inGTestBlock = true;
				} else if (commentText.equals("#" + END_GENERATED_COMMENT)) {
					inGTestBlock = false;
					toRemove.add(child);
				}
			}

			if (inGTestBlock) {
				toRemove.add(child);
			}
		}

		for (CMakeNode node : toRemove) {
			root.removeChild(node);
		}
	}

	private void addTestsSubdirectory(CMakeRoot root) {
		if (hasTestsSubdirectory(root)) {
			return;
		}

		int insertPosition = findInsertPosition(root);
		CommentNode startComment = new CommentNode(GENERATED_COMMENT);
		root.addChild(insertPosition++, startComment);
		CommandNode addTestsSubdirCommand = new CommandNode("add_subdirectory", root);
		addTestsSubdirCommand.addArg(testsFolder.getName());
		root.addChild(insertPosition++, addTestsSubdirCommand);
		CommentNode endComment = new CommentNode(END_GENERATED_COMMENT);
		root.addChild(insertPosition, endComment);
	}

	private boolean hasTestsSubdirectory(CMakeNode node) {
		for (CMakeNode child : node.getChildren()) {
			if (child instanceof CommandNode command && "add_subdirectory".equals(command.getName())) {
				for (ArgumentNode arg : command.getArgs()) {
					if (testsFolder.getName().equals(arg.getValue())) {
						return true;
					}
				}
			} else if (child instanceof ConditionCommand condition && hasTestsSubdirectory(condition)) {
				return true;
			}
		}

		return false;

	}

	private int findInsertPosition(CMakeRoot root) {
		int lastTargetPosition = -1;
		int projectPosition = -1;

		for (int i = 0; i < root.getChildren().size(); i++) {
			CMakeNode child = root.getChildren().get(i);
			if (child instanceof CommandNode command && "project".equals(command.getName())) {
				projectPosition = i;
			} else if (child instanceof CommandNode command) {
				String name = command.getName();
				if ("add_executable".equals(name) || "add_library".equals(name)) {
					lastTargetPosition = i;
				}
			}
		}

		if (lastTargetPosition != -1) {
			return lastTargetPosition + 1;
		} else if (projectPosition != 1) {
			return projectPosition + 1;
		}

		return root.getChildren().size();
	}

	private String readFromFile(IFile file, String def) throws IOException, CoreException {
		String result = def;
		if (file.exists()) {
			try (InputStream stream = file.getContents()) {
				result = new String(stream.readAllBytes());
			}
		}

		return result;
	}
}