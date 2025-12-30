package su.softcom.cldt.testing.gtest.ui.models;

import org.eclipse.core.resources.IProject;

/**
 * Корневой элемент тестового дерева.
 */
public final class TestRoot implements IGTestContentProviderNode {
	private final IProject project;
	private LoadingState state = LoadingState.LOADING;
	
	public enum LoadingState {
		LOADING, READY, FAILED, NO_TESTS
	}

	/**
	 * Создаёт новый корень тестов для заданного проекта.
	 *
	 * @param проект
	 */
	public TestRoot(IProject project) {
		this.project = project;
	}

	/**
	 * Получает проект для данного корня.
	 *
	 * @return проект
	 */
	public IProject getProject() {
		return project;
	}

	/**
	 * Получает состояние корня.
	 *
	 * @return состояние корня
	 */
	public LoadingState getState() {
		return state;
	}

	/**
	 * Устанавливает состояние корня.
	 *
	 * @param новое состояние корня
	 */
	public void setState(LoadingState state) {
		this.state = state;
	}

	@Override
	public String toString() {
		return "Тесты";
	}
}