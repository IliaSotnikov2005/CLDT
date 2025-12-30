package su.softcom.cldt.testing.gtest.ui.models;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;

import su.softcom.cldt.testing.gtest.core.launch.GTestLaunchUtils;
import su.softcom.cldt.testing.gtest.core.model.GTestExecutable;

/**
 * Элемент, представляющий исполняемый файл gtest в дереве.
 */
public final class GTestExecutableNode extends LaunchableNode {

	private final GTestExecutable gtestExecutable;

	public GTestExecutableNode(IProject project, GTestExecutable gtestExecutable) {
		super(project);
		this.gtestExecutable = gtestExecutable;
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
		Platform.getLog(getClass())
				.info("Creating launch config for: " + gtestExecutable.getExecutable().lastSegment()); //$NON-NLS-1$
		return GTestLaunchUtils.createLaunchConfig(project, gtestExecutable, null);
	}

	@Override
	public String toString() {
		return gtestExecutable.getExecutable().lastSegment();
	}
}