package su.softcom.cldt.testing.gtest.internal.core;

import java.util.List;

import su.softcom.cldt.core.cmake.ArgumentNode;
import su.softcom.cldt.core.cmake.CMakeNode;
import su.softcom.cldt.core.cmake.CMakeRoot;
import su.softcom.cldt.core.cmake.CommandNode;

/**
 * Модификатор CMake дерева для визардов GTest.
 * Создаёт цель сборки для тестов или добавляет созданный тест в существующую.
 */
public class GTestWizardTreeModifier {
	private static final String DEFAULT_TEST_TARGET_NAME = "${PROJECT_NAME}.tests";

	private GTestWizardTreeModifier() {
		// empty
	}

	/**
	 * Добавляет тест в цель сборки или создает новую цель сборки для тестов.
	 * @param root корень
	 * @param testFileName имя тестового файла
	 */
	public static void modifyCMakeTree(CMakeRoot root, String testFileName) {
		String testTargetName = findTestTargetName(root);
		if (testTargetName == null) {
			testTargetName = createTestTarget(root, DEFAULT_TEST_TARGET_NAME);
		}

		addFileToTarget(root, testTargetName, testFileName);
	}

	private static String findTestTargetName(CMakeRoot root) {
		for (CMakeNode child : root.getChildren()) {
			if (child instanceof CommandNode cmd && "add_executable".equals(cmd.getName())) {
				List<ArgumentNode> args = cmd.getArgs();
				if (!args.isEmpty()) {
					return args.get(0).getValue();
				}
			}
		}

		return null;
	}

	private static String createTestTarget(CMakeRoot root, String targetName) {
		int insertPos = findInsertPosition(root);

		CommandNode addExeCmd = new CommandNode("add_executable", root);
		addExeCmd.addArg(targetName);

		root.addChild(insertPos++, addExeCmd);

		CommandNode includeCmd = new CommandNode("include", root);
		includeCmd.addArg("GoogleTest");
		root.addChild(insertPos++, includeCmd);

		CommandNode discoverCmd = new CommandNode("gtest_discover_tests", root);
		discoverCmd.addArg(targetName);
		root.addChild(insertPos, discoverCmd);

		return targetName;
	}

	private static void addFileToTarget(CMakeRoot root, String targetName, String fileName) {
		for (CMakeNode child : root.getChildren()) {
			if (child instanceof CommandNode cmd && "add_executable".equals(cmd.getName())) {
				List<ArgumentNode> args = cmd.getArgs();
				if (!args.isEmpty() && targetName.equals(args.get(0).getValue())) {
					boolean fileExists = false;
					for (int i = 1; i < args.size(); ++i) {
						if (fileName.equals(args.get(i).getValue())) {
							fileExists = true;
							break;
						}
					}

					if (!fileExists) {
						cmd.addArg(fileName);
					}

					break;
				}
			}
		}
	}

	private static int findInsertPosition(CMakeRoot root) {
		for (int i = 0; i < root.getChildren().size(); i++) {
			CMakeNode child = root.getChildren().get(i);
			if (child instanceof CommandNode cmd && "project".equals(cmd.getName())) {
				return i + 1;
			}
		}

		return root.getChildren().size();
	}
}