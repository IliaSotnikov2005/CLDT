package su.softcom.cldt.testing.gtest.ui.actions;

import java.util.List;

import org.eclipse.ui.actions.SelectionListenerAction;

import su.softcom.cldt.testing.gtest.ui.GTestContentProvider;
import su.softcom.cldt.testing.gtest.ui.models.TestRoot;

/**
 * Действие для обновления корня тестового дерева.
 */
public class GTestRefreshRootAction extends SelectionListenerAction {

	private static final String RUN_REFRESH_ACTION_NAME = "Обновить тесты";

	/**
	 * Создаёт новый {@link GTestRefreshRootAction}.
	 */
	public GTestRefreshRootAction() {
		super(RUN_REFRESH_ACTION_NAME);
	}

	@Override
	public void run() {
		List<?> selectedTests = getSelectedNonResources();
		if (selectedTests.isEmpty()) {
			return;
		}

		for (Object element : selectedTests) {
			if (element instanceof TestRoot testRoot) {
				GTestContentProvider.getInstance().refreshProjectRoot(testRoot.getProject());
			}
		}
	}
}