package su.softcom.cldt.testing.gtest.ui.models;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.actions.ILaunchable;

/**
 * Элемент {@link GTestContentProvider}, поддерживающий запуск тестов.
 */
public abstract class LaunchableNode implements ILaunchable, IGTestContentProviderNode {
	protected final IProject project;

	protected LaunchableNode(IProject project) {
		this.project = project;
	}

	/**
	 * Получает проект, в котором находится элемент.
	 *
	 * @return проект
	 */
	public IProject getProject() {
		return project;
	}

	/**
	 * Получает режим запуска.
	 *
	 * @return режим запуска
	 */
	public String getLaunchMode() {
		return "run"; //$NON-NLS-1$
	}

	/**
	 * Получает конфигурацию запуска.
	 *
	 * @return конфигурация запуска
	 * @throws {@link CoreException} при внутренней ошибке
	 */
	public ILaunchConfiguration getLaunchConfiguration() throws CoreException {
		ILaunchConfiguration config = createLaunchConfig();

		DebugUITools.launch(config, getLaunchMode());
		
		return config;
	}

	/**
	 * Создает конфигурацию запуска для данного элемента.
	 *
	 * @return объект {@link ILaunchConfiguration}
	 * @throws {@link CoreException} при внутренней ошибке
	 */
	protected abstract ILaunchConfiguration createLaunchConfig() throws CoreException;
}