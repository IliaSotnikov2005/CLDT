package su.softcom.cldt.testing.gtest.ui.models;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;

import su.softcom.cldt.testing.gtest.core.launch.GTestLaunchUtils;
import su.softcom.cldt.testing.gtest.core.model.GTestExecutable;

/**
 * Элемент {@link GTestContentProvider}, представляющий группу тестов в тестовом дереве.
 */
public final class TestSuiteNode extends LaunchableNode {

	private static final String DISABLED_LABEL = "_DISABLED"; //$NON-NLS-1$

	private final GTestExecutable.TestSuite testSuite;

	private final GTestExecutable gtestExecutable;

	/**
	 * Создаёт новый {@link TestSuiteNode}
	 * @param project проект
	 * @param gtestExecutable объект исполняемого файла GTest
	 * @param testSuite объект тестового набора
	 */
	public TestSuiteNode(IProject project, GTestExecutable gtestExecutable, GTestExecutable.TestSuite testSuite) {
		super(project);
		this.gtestExecutable = gtestExecutable;
		this.testSuite = testSuite;
	}

	/**
	 * Получает объект группы тестов.
	 *
	 * @return объект группы тестов
	 */
	public GTestExecutable.TestSuite getTestSuite() {
		return testSuite;
	}

	/**
	 * Получает объект исполняемого файла gtest.
	 *
	 * @return объект исполняемого файла gtest
	 */
	public GTestExecutable getGTestExecutable() {
		return gtestExecutable;
	}

	@Override
	public ILaunchConfiguration createLaunchConfig() throws CoreException {
		String filter = testSuite.getRawName() + ".*"; //$NON-NLS-1$
		Platform.getLog(getClass()).info("Creating launch config for suite: " + testSuite.getRawName()); //$NON-NLS-1$
		return GTestLaunchUtils.createLaunchConfig(project, gtestExecutable, filter);
	}

	@Override
	public String toString() {
		String name = testSuite.getRawName();
		if (testSuite.isDisabled()) {
			return name + DISABLED_LABEL;
		}

		return name;
	}
}