package su.softcom.cldt.testing.gtest.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import su.softcom.cldt.core.CMakeProjectNature;
import su.softcom.cldt.testing.gtest.core.Activator;
import su.softcom.cldt.testing.gtest.core.ITestIndexService;
import su.softcom.cldt.testing.gtest.core.model.GTestExecutable;
import su.softcom.cldt.testing.gtest.core.model.TestIndexSnapshot;
import su.softcom.cldt.testing.gtest.ui.models.FailedNode;
import su.softcom.cldt.testing.gtest.ui.models.GTestExecutableNode;
import su.softcom.cldt.testing.gtest.ui.models.LoadingNode;
import su.softcom.cldt.testing.gtest.ui.models.TestCaseNode;
import su.softcom.cldt.testing.gtest.ui.models.TestRoot;
import su.softcom.cldt.testing.gtest.ui.models.TestSuiteNode;
import su.softcom.cldt.testing.gtest.ui.models.TestRoot.LoadingState;

/**
 * Представляет content provider для Gtest файлов. Выводит в Project Explorer в
 * раздел "Тесты"
 */
public class GTestContentProvider extends WorkbenchContentProvider implements IPipelinedTreeContentProvider {

	private static GTestContentProvider instance;

	private static final Object[] LOADING_NODE = { new LoadingNode() };

	private static final Object[] FAILED_NODE = { new FailedNode() };

	private static final Object[] NO_CHILDREN = new Object[0];

	private StructuredViewer viewer;

	private ITestIndexService testIndexService;

	private final Map<IProject, TestRoot> projectRoots = new ConcurrentHashMap<>();

	/**
	 * Получает экземпляр провайдера
	 */
	public static GTestContentProvider getInstance() {
		return instance;
	}

	@Override
	public void init(ICommonContentExtensionSite commonContentExtensionSite) {
		instance = this;
		
		BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
		if (context == null) {
			testIndexService = null;
			return;
		}

		ServiceReference<ITestIndexService> ref = context.getServiceReference(ITestIndexService.class);

		if (ref == null) {
			testIndexService = null;
			return;
		}

		try {
			testIndexService = context.getService(ref);
		} finally {
			context.ungetService(ref);
		}
	}

	@Override
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		super.inputChanged(v, oldInput, newInput);
		this.viewer = (v instanceof StructuredViewer sv) ? sv : null;

		GTestRefreshListener.init();
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IWorkspaceRoot root) {
			return root.getProjects();
		}

		return NO_CHILDREN;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IProject project) {
			TestRoot root = projectRoots.computeIfAbsent(project, TestRoot::new);
			if (root.getState() == LoadingState.LOADING) {
				startAsyncDiscovery(project, root);
			}
			if (root.getState() == LoadingState.READY) {
				return new Object[] { root };
			}

			return NO_CHILDREN;
		}

		if (parentElement instanceof TestRoot testRoot) {
			return getTestRootChildren(testRoot);
		}

		if (parentElement instanceof GTestExecutableNode executableNode) {
			return executableNode.getGTestExecutable().getSuites().stream().map(
					suite -> new TestSuiteNode(executableNode.getProject(), executableNode.getGTestExecutable(), suite))
					.toArray();
		}

		if (parentElement instanceof TestSuiteNode suiteNode) {
			return suiteNode.getTestSuite().getTests().stream().map(testCase -> new TestCaseNode(suiteNode.getProject(),
					suiteNode.getGTestExecutable(), suiteNode.getTestSuite(), testCase)).toArray();
		}

		return NO_CHILDREN;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof TestRoot testRoot) {
			return testRoot.getProject();
		}
		if (element instanceof GTestExecutable.TestSuite suite) {
			return suite.parent();
		}
		if (element instanceof GTestExecutable.TestCase testCase) {
			return testCase.parent();
		}

		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof IProject project) {
			TestRoot root = projectRoots.get(project);
			return root != null && root.getState() == LoadingState.READY && isCMakeProject(project);
		}
		if (element instanceof TestRoot testRoot) {
			return testRoot.getState() == LoadingState.READY;
		}
		if (element instanceof GTestExecutableNode executableNode) {
			return !executableNode.getGTestExecutable().getSuites().isEmpty();
		}
		if (element instanceof TestSuiteNode suiteNode) {
			return !suiteNode.getTestSuite().getTests().isEmpty();
		}

		return false;
	}

	private void startAsyncDiscovery(IProject project, TestRoot root) {
		if (testIndexService == null) {
			root.setState(LoadingState.FAILED);
			asyncRefresh(root);
			return;
		}

		root.setState(LoadingState.LOADING);
		asyncRefresh(root);

		Job job = new Job("Async Tests Discovery (" + project.getName() + ")") {
			@Override
			protected IStatus run(IProgressMonitor m) {
				try {
					testIndexService.refreshNow(project, null, m);
					TestIndexSnapshot snapshot = testIndexService.getSnapshot(project, null);

					if (snapshot != null) {
						if (!snapshot.gtestItems().isEmpty()) {
							root.setState(LoadingState.READY);
						} else {
							root.setState(LoadingState.NO_TESTS);
						}
					} else {
						root.setState(LoadingState.FAILED);
					}

					asyncRefresh(project);
					return Status.OK_STATUS;
				} catch (Exception e) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Ошибка поиска тестов: " + e.getMessage(), e);
				}
			}
		};

		job.setRule(project);
		job.setUser(false);
		job.schedule();
	}

	private Object[] getTestRootChildren(TestRoot testRoot) {
		if (testRoot.getState() != LoadingState.READY) {
			return testRoot.getState() == LoadingState.LOADING ? LOADING_NODE : FAILED_NODE;
		}

		try {
			TestIndexSnapshot snapshot = testIndexService.getSnapshot(testRoot.getProject(), null);

			if (snapshot == null) {
				return new Object[] { new FailedNode() };
			}

			List<Object> children = new ArrayList<>();

			for (GTestExecutable gtest : snapshot.gtestItems()) {
				children.add(new GTestExecutableNode(testRoot.getProject(), gtest));
			}

			return children.isEmpty() ? new Object[] { new FailedNode() } : children.toArray();

		} catch (Exception e) {
			testRoot.setState(LoadingState.FAILED);
			return FAILED_NODE;
		}
	}

	private boolean isCMakeProject(IProject project) {
		if (project == null || !project.isAccessible())
			return false;
		try {
			return project.hasNature(CMakeProjectNature.ID);
		} catch (Exception e) {
			return false;
		}
	}

	// ===== IPipelinedTreeContentProvider методы =====
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void getPipelinedChildren(Object parent, Set currentChildren) {
		customize(getChildren(parent), currentChildren);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void getPipelinedElements(Object input, Set currentElements) {
		customize(getElements(input), currentElements);
	}

	@Override
	public Object getPipelinedParent(Object object, Object suggestedParent) {
		return getParent(object);
	}

	@Override
	public PipelinedShapeModification interceptAdd(PipelinedShapeModification addModification) {
		return addModification;
	}

	@Override
	public PipelinedShapeModification interceptRemove(PipelinedShapeModification removeModification) {
		return removeModification;
	}

	@Override
	public boolean interceptRefresh(PipelinedViewerUpdate aRefreshSynchronization) {
		return false;
	}

	@Override
	public boolean interceptUpdate(PipelinedViewerUpdate anUpdateSynchronization) {
		return false;
	}

	@Override
	public void restoreState(IMemento aMemento) {
		// empty
	}

	@Override
	public void saveState(IMemento aMemento) {
		// empty
	}

	// ===== Вспомогательные методы =====

	@Override
	protected void processDelta(IResourceDelta delta) {
		if (delta == null || viewer == null)
			return;

		IResource resource = delta.getResource();
		int kind = delta.getKind();

		if (kind == IResourceDelta.CHANGED && resource instanceof IProject) {
			asyncRefresh(resource);
		}

		for (IResourceDelta childDelta : delta.getAffectedChildren()) {
			processDelta(childDelta);
		}
	}

	private void customize(Object[] ourElements, Set<Object> proposedChildren) {
		for (Object element : ourElements) {
			if (element != null) {
				proposedChildren.add(element);
			}
		}
	}

	private void asyncRefresh(Object element) {
		if (viewer == null)
			return;

		viewer.getControl().getDisplay().asyncExec(() -> {
			if (viewer.getControl().isDisposed())
				return;
			viewer.refresh(element, true);
		});
	}

	/**
	 * Обновляет TestRoot для указанного проекта
	 */
	public void refreshProjectRoot(IProject project) {
		if (project == null || !project.isAccessible()) {
			return;
		}

		TestRoot testRoot = projectRoots.get(project);
		if (testRoot != null) {
			Platform.getLog(getClass()).info("Starting refresh for project: " + project.getName()); //$NON-NLS-1$

			if (testIndexService != null) {
				testIndexService.clear(project, null);
				Platform.getLog(getClass()).info("Cleared test index cache for: " + project.getName()); //$NON-NLS-1$
			}

			startAsyncDiscovery(project, testRoot);
		} else {
			testRoot = new TestRoot(project);
			projectRoots.put(project, testRoot);

			if (testRoot.getState() == LoadingState.LOADING) {
				startAsyncDiscovery(project, testRoot);
			}
		}
	}
}