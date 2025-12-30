package su.softcom.cldt.testing.gtest.ui.actions;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.actions.SelectionListenerAction;

import su.softcom.cldt.testing.gtest.ui.models.LaunchableNode;

/**
 * Представляет действие для запуска тестов.
 */
public class GTestRunAction extends SelectionListenerAction {
	private static final String RUN_TEST_ACTION_NAME = "Запустить тест";

	/**
	 * Создает новый GTestRunAction.
	 */
	public GTestRunAction() {
		super(RUN_TEST_ACTION_NAME);
	}

	@Override
	public void run() {
		List<?> selectedTests = getSelectedNonResources();
		if (selectedTests.isEmpty()) {
			return;
		}

		for (Object element : selectedTests) {
			if (element instanceof LaunchableNode launchable) {
				try {
					launchable.launch();
				} catch (CoreException e) {
					Platform.getLog(getClass()).error(e.getMessage());
				}
			}
		}
	}
}