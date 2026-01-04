package su.softcom.cldt.testing.gtest.internal.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import su.softcom.cldt.core.cmake.ArgumentNode;
import su.softcom.cldt.core.cmake.CMakeNode;
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
 * Модификатор CMake дерева. В конфигурацию CMake добавляет экземпляр GTest, выбранный
 * в настройках. Сгенерированный код помечается комментариями.
 */
public class GTestTestConfigurationTreeModifier implements ICMakeTreeModifier{
	private static final String GENERATED_COMMENT =     "---------- Сгенерировано GTest-модификатором CMake дерева ------";
	private static final String END_GENERATED_COMMENT = "---------- Конец сгенерированного GTest-модификатором ----------";
	private static final String PRIVATE = "PRIVATE";
	private static final String CLDT_GTEST_ROOT_VARIABLE = "CLDT_GTEST_ROOT";

	private GTestInstance instance;

	private List<CommandNode> savedLinkCommands = new ArrayList<>();
	private Set<String> testTargets = new HashSet<>();

	public GTestTestConfigurationTreeModifier() {
		// empty
	}

	@Override
	public void modifyTree(ICMakeProject project, CMakeRoot root) {
		instance = GTestUtils.getGTestInstanceForProject(project.getProject());

		if (instance == null) {
			return;
		}

		
		if (!instance.path().isEmpty()) {
			removeGTestConfiguration(root);
			addGTestConfiguration(root);
		}
	}

	private void removeGTestConfiguration(CMakeRoot root) {
		collectTestTargets(root);
		savedLinkCommands.clear();
		saveTestTargetsLinkCommands(root);

		removeGTestSection(root);

		savedLinkCommands.forEach(root::removeChild);

		removeFindPackageGTest(root);
	}

	private void collectTestTargets(CMakeRoot root) {
		testTargets.clear();

		for (CMakeNode child : root.getChildren()) {
			if (child instanceof CommandNode command && "add_executable".equals(command.getName())
					&& !command.getArgs().isEmpty()) {
				String targetName = command.getArgs().get(0).getValue();
				if (targetName != null && !targetName.isEmpty()) {
					testTargets.add(targetName);
				}
			}
		}
	}

	private void saveTestTargetsLinkCommands(CMakeNode root) {
		for (CMakeNode child : root.getChildren()) {
			if (child instanceof CommandNode command && "target_link_libraries".equals(command.getName())
					&& !command.getArgs().isEmpty() && testTargets.contains(command.getArgs().get(0).getValue())) {
				savedLinkCommands.add(command);
			}
		}
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
			} else if (child instanceof ConditionCommand condition) {
				saveTestTargetsLinkCommands(condition);
			}

			if (inGTestBlock) {
				toRemove.add(child);
			}
		}

		for (CMakeNode node : toRemove) {
			root.removeChild(node);
		}
	}

	private void removeFindPackageGTest(CMakeRoot root) {
		List<CMakeNode> toRemove = new ArrayList<>();

		for (CMakeNode child : root.getChildren()) {
			if (child instanceof CommandNode command && "find_package".equals(command.getName())
					&& command.getArgs().stream().anyMatch(arg -> "GTest".equals(arg.getValue()))) {
				toRemove.add(child);
			}
		}

		for (CMakeNode node : toRemove) {
			root.removeChild(node);
		}
	}

	private void addGTestConfiguration(CMakeRoot root) {
		int insertPosition = findInsertPosition(root);

		CommentNode startComment = new CommentNode(GENERATED_COMMENT);
		root.addChild(insertPosition++, startComment);

		CommentNode cautionComment = new CommentNode(" Не удаляйте комментарии");
		root.addChild(insertPosition++, cautionComment);
		CommentNode caution2Comment = new CommentNode(" Не изменяйте без необходимости");
		root.addChild(insertPosition++, caution2Comment);

		if (GTestInstance.SYSTEM_TEXT.equals(instance.type())) {
			CommandNode findPackage = createFindPackageGTestCommand(root);
			root.addChild(insertPosition++, findPackage);
			addTestTargetsLinking(insertPosition, root, true);
			insertPosition += testTargets.size();
		} else {
			CommandNode setGTestRoot = createGTestRootVariable(root);
			root.addChild(insertPosition++, setGTestRoot);

			ConditionCommand checkGTestRoot = createGTestRootCheckCondition(root);
			root.addChild(insertPosition++, checkGTestRoot);

			CommandNode endifCommand = new CommandNode("endif", root);
			root.addChild(insertPosition++, endifCommand);

			CommandNode gtestVersionBlock = createGTestVersionCommand(root);
			root.addChild(insertPosition++, gtestVersionBlock);

			CommandNode addSubdirCommand = new CommandNode("add_subdirectory", root);
			addSubdirCommand.addArg("${" + CLDT_GTEST_ROOT_VARIABLE + "}");
			addSubdirCommand.addArg("${CMAKE_BINARY_DIR}/googletest");
			root.addChild(insertPosition++, addSubdirCommand);

			addTestTargetsLinking(insertPosition, root, false);
			insertPosition += testTargets.size();
		}

		CommentNode endComment = new CommentNode(END_GENERATED_COMMENT);
		root.addChild(insertPosition, endComment);
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

	private CommandNode createFindPackageGTestCommand(CMakeRoot root) {
		CommandNode findPackageCmd = new CommandNode("find_package", root);
		findPackageCmd.addArg("GTest");
		findPackageCmd.addArg("REQUIRED");

		return findPackageCmd;
	}

	private CommandNode createGTestRootVariable(CMakeNode root) {
		CommandNode setCommand = new CommandNode("set", root);
		setCommand.addArg(CLDT_GTEST_ROOT_VARIABLE);

		ArgumentNode pathArg = new ArgumentNode(setCommand, instance.path());
		pathArg.makeQuoted();
		setCommand.addArg(pathArg);

		return setCommand;
	}

	private ConditionCommand createGTestRootCheckCondition(CMakeRoot root) {
		CommandNode ifNotCommand = new CommandNode("if", root);
		ifNotCommand.addArg("NOT");
		ifNotCommand.addArg("EXISTS");

		ArgumentNode checkArg = new ArgumentNode(ifNotCommand, "${" + CLDT_GTEST_ROOT_VARIABLE + "}/CMakeLists.txt");
		ifNotCommand.addArg(checkArg);

		ConditionCommand condition = new ConditionCommand(ifNotCommand, root);

		CommandNode messageCommand = new CommandNode("message", condition);
		messageCommand.addArg("FATAL_ERROR");

		ArgumentNode errorArg = new ArgumentNode(messageCommand,
				"Не найден CMakeLists.txt в GTest: ${" + CLDT_GTEST_ROOT_VARIABLE + "}");
		messageCommand.addArg(errorArg);

		condition.addChild(messageCommand);

		return condition;
	}

	private CommandNode createGTestVersionCommand(CMakeNode root) {
		CommandNode setCommand = new CommandNode("set", root);
		setCommand.addArg("GOOGLETEST_VERSION");
		setCommand.addArg(instance.version());

		return setCommand;
	}

	private void addTestTargetsLinking(int insertPosition, CMakeNode root, boolean isSystemGTest) {
		for (String targetName : testTargets) {
			addTargetGTestLink(insertPosition++, targetName, root, isSystemGTest);
		}
	}

	private void addTargetGTestLink(int insertPosition, String targetName, CMakeNode root, boolean isSystemGTest) {
		CommandNode existingLinkCmd = null;
		for (CommandNode cmd : savedLinkCommands) {
			if (targetName.equals(cmd.getArgs().get(0).getValue())) {
				existingLinkCmd = cmd;
				break;
			}
		}

		if (existingLinkCmd != null) {
			List<ArgumentNode> newArgs = new ArrayList<>();

			boolean hasPrivate = false;
			for (ArgumentNode arg : existingLinkCmd.getArgs()) {
				if (!isGTestLibrary(arg.getValue())) {
					newArgs.add(arg);
				}
				if (arg.getValue().equals(PRIVATE)) {
					hasPrivate = true;
				}
			}

			if (!hasPrivate) {
				newArgs.add(1, new ArgumentNode(existingLinkCmd, PRIVATE));
			}

			existingLinkCmd.setArgs(newArgs);
			if (isSystemGTest) {
				existingLinkCmd.addArg(GTestConstants.GTEST_LIB);
				existingLinkCmd.addArg(GTestConstants.GTEST_MAIN_LIB);
			} else {
				existingLinkCmd.addArg(GTestConstants.GTEST);
				existingLinkCmd.addArg(GTestConstants.GTEST_MAIN);
			}

			root.addChild(insertPosition, existingLinkCmd);
		} else {
			CommandNode linkCmd = new CommandNode("target_link_libraries", root);
			linkCmd.addArg(targetName);
			linkCmd.addArg("PRIVATE");

			if (isSystemGTest) {
				linkCmd.addArg(GTestConstants.GTEST_LIB);
				linkCmd.addArg(GTestConstants.GTEST_MAIN_LIB);
			} else {
				linkCmd.addArg(GTestConstants.GTEST);
				linkCmd.addArg(GTestConstants.GTEST_MAIN);
			}

			root.addChild(insertPosition, linkCmd);
		}
	}

	private boolean isGTestLibrary(String libName) {
		return GTestConstants.GTEST_LIB.equals(libName) || GTestConstants.GTEST_MAIN_LIB.equals(libName)
				|| GTestConstants.GTEST.equals(libName) || GTestConstants.GTEST_MAIN.equals(libName);
	}
}