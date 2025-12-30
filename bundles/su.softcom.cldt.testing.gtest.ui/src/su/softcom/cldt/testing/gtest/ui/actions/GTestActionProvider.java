package su.softcom.cldt.testing.gtest.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.ui.actions.ILaunchable;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

import su.softcom.cldt.testing.gtest.ui.models.LaunchableNode;
import su.softcom.cldt.testing.gtest.ui.models.TestRoot;

/**
 * ActionProvider для элементов дерева Gtest.
 */
public class GTestActionProvider extends CommonActionProvider {

	private GTestRunAction runTestAction;
	private GTestRefreshRootAction refreshTestRootAction;

	private StructuredViewer viewer;

	@Override
	public void init(ICommonActionExtensionSite commonSite) {
		if (commonSite.getViewSite() instanceof ICommonViewerWorkbenchSite) {
			this.viewer = commonSite.getStructuredViewer();

			initActions();
			// setupDoubleClickSupport();
		}
	}

	private void initActions() {
		runTestAction = new GTestRunAction();
		refreshTestRootAction = new GTestRefreshRootAction();
	}

	private void setupDoubleClickSupport() {
		if (viewer != null) {
			viewer.addDoubleClickListener(event -> {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				Object element = selection.getFirstElement();
				if (element instanceof LaunchableNode testCase) {
					try {
						testCase.launch();
					} catch (CoreException e) {
						Platform.getLog(getClass()).error(e.getMessage());
					}
				}
			});
		}
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();
		Object element = selection.getFirstElement();

		if (element instanceof ILaunchable) {
			runTestAction.selectionChanged(selection);
			menu.add(runTestAction);
		} else if (element instanceof TestRoot) {
			refreshTestRootAction.selectionChanged(selection);
			menu.add(refreshTestRootAction);
		}
	}
}