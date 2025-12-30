package su.softcom.cldt.testing.gtest.ui.models;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;

import su.softcom.cldt.testing.gtest.core.launch.GTestLaunchUtils;
import su.softcom.cldt.testing.gtest.core.model.GTestExecutable;

public final class TestCaseNode extends LaunchableNode {

	private static final String DISABLED_LABEL = "_DISABLED"; //$NON-NLS-1$

	private final GTestExecutable.TestCase testCase;

	private final GTestExecutable.TestSuite testSuite;

	private final GTestExecutable gtestExecutable;

	public TestCaseNode(IProject project, GTestExecutable gtestExecutable, GTestExecutable.TestSuite testSuite,
			GTestExecutable.TestCase testCase) {
		super(project);
		this.gtestExecutable = gtestExecutable;
		this.testSuite = testSuite;
		this.testCase = testCase;
	}

	/**
	 * Получает объект TestCase.
	 *
	 * @return TestCase
	 */
	public GTestExecutable.TestCase getTestCase() {
		return testCase;
	}

	/**
	 * Получает объект TestSuite.
	 *
	 * @return TestSuite
	 */
	public GTestExecutable.TestSuite getTestSuite() {
		return testSuite;
	}

	@Override
	public ILaunchConfiguration createLaunchConfig() throws CoreException {
		String filter = testCase.toGTestFilter();
		Platform.getLog(getClass()).info("Creating launch config for test: " + testCase.getRawName()); //$NON-NLS-1$
		return GTestLaunchUtils.createLaunchConfig(project, gtestExecutable, filter);
	}

	@Override
	public String toString() {
		String name = testCase.getRawName();
		if (testCase.isDisabled()) {
			return name + DISABLED_LABEL;
		}
		return name;
	}
}