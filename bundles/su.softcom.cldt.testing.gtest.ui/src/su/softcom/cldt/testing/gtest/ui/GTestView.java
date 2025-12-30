package su.softcom.cldt.testing.gtest.ui;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import su.softcom.cldt.testing.gtest.core.GTestConstants;
import su.softcom.cldt.testing.gtest.ui.models.Failure;
import su.softcom.cldt.testing.gtest.ui.models.GTestResult;
import su.softcom.cldt.testing.gtest.ui.models.Skipped;
import su.softcom.cldt.testing.gtest.ui.models.TestInfo;
import su.softcom.cldt.testing.gtest.ui.models.TestSuite;

/**
 * Окно результатов тестирования GTest.
 */
public class GTestView extends ViewPart {
	public static final String ID = "com.example.gtest.views.GTestView";

	private TreeViewer treeViewer;
	private Text detailsText;
	private Label failedLabel;
    private Label erroredLabel;
    private Label executedLabel;
	
    private Action showFailedOnlyAction;
    private Action showSkippedOnlyAction;
    private Action showDisabledOnlyAction;
    
    private boolean showFailedOnly = false;
    private boolean showSkippedOnly = false;
    private boolean showDisabledOnly = false;

	private GTestJsonParser parser;
	private IProject activeProject;
	
	private GTestViewContentProvider contentProvider;

	@Override
	public void createPartControl(Composite parent) {
		parser = new GTestJsonParser();
		contentProvider = new GTestViewContentProvider();

		parent.setLayout(new GridLayout(1, false));

		createViewToolbar();
		createStatsPanel(parent);

		createMainArea(parent);
		loadTestsForActiveProject();
	}
	
	private void createViewToolbar() {
        IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
        
        showFailedOnlyAction = new Action("Показать только проваленные", IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
                if (isChecked()) {
                    showFailedOnly = true;
                    if (showSkippedOnlyAction != null) {
                        showSkippedOnlyAction.setChecked(false);
                        showSkippedOnly = false;
                    }
                    
                    if (showDisabledOnlyAction != null) {
                        showDisabledOnlyAction.setChecked(false);
                        showDisabledOnly = false;
                    }
                } else {
                    showFailedOnly = false;
                    
                }
                
                applyFilters();
            }
        };
        showFailedOnlyAction.setToolTipText("Показать только проваленные тесты");
        showFailedOnlyAction.setImageDescriptor(GTestImages.SHOW_FAILED_ICON);
        
        showSkippedOnlyAction = new Action("Показать пропущенные", IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
            	if (isChecked()) {
                    showSkippedOnly = true;
                    if (showFailedOnlyAction != null) {
                        showFailedOnlyAction.setChecked(false);
                        showFailedOnly = false;
                    }
                    
                    if (showDisabledOnlyAction != null) {
                        showDisabledOnlyAction.setChecked(false);
                        showDisabledOnly = false;
                    }
                } else {
                    showSkippedOnly = false;
                }
            	
                applyFilters();
            }
        };
        showSkippedOnlyAction.setToolTipText("Показать пропущенные тесты");
        showSkippedOnlyAction.setImageDescriptor(GTestImages.SHOW_SKIPPED_ICON);
        
        showDisabledOnlyAction = new Action("Показать отключенные", IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
                if (isChecked()) {
                    showDisabledOnly = true;
                    if (showFailedOnlyAction != null) {
                        showFailedOnlyAction.setChecked(false);
                        showFailedOnly = false;
                    }
                    
                    if (showSkippedOnlyAction != null) {
                        showSkippedOnlyAction.setChecked(false);
                        showSkippedOnly = false;
                    }
                } else {
                    showDisabledOnly = false;
                }
                
                applyFilters();
            }
        };
        showDisabledOnlyAction.setToolTipText("Показать отключенные тесты");
        showDisabledOnlyAction.setImageDescriptor(GTestImages.SHOW_DISABLED_ICON);
        
        toolBarManager.add(showFailedOnlyAction);
        toolBarManager.add(showSkippedOnlyAction);
        toolBarManager.add(showDisabledOnlyAction);
        
        toolBarManager.update(true);
    }
	
	private void applyFilters() {
        if (contentProvider != null) {
            contentProvider.setFilters(showFailedOnly, showSkippedOnly, showDisabledOnly);
            treeViewer.refresh();
            treeViewer.expandAll();
        }
    }

	private void createStatsPanel(Composite parent) {
	    Composite statsPanel = new Composite(parent, SWT.NONE);
	    statsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    
	    GridLayout layout = new GridLayout(3, true);
	    statsPanel.setLayout(layout);
	    
	    executedLabel = new Label(statsPanel, SWT.CENTER);
	    executedLabel.setText("Запущено: 0/0");
	    GridData gd1 = new GridData(GridData.FILL_HORIZONTAL);
	    gd1.horizontalAlignment = SWT.CENTER;
	    executedLabel.setLayoutData(gd1);
	    
	    failedLabel = new Label(statsPanel, SWT.CENTER);
	    failedLabel.setText("Провалено: 0");
	    GridData gd2 = new GridData(GridData.FILL_HORIZONTAL);
	    gd2.horizontalAlignment = SWT.CENTER;
	    failedLabel.setLayoutData(gd2);
	    
	    erroredLabel = new Label(statsPanel, SWT.CENTER);
	    erroredLabel.setText("Ошибок: 0");
	    GridData gd3 = new GridData(GridData.FILL_HORIZONTAL);
	    gd3.horizontalAlignment = SWT.CENTER;
	    erroredLabel.setLayoutData(gd3);
	}

	private void createMainArea(Composite parent) {
		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

		createTestTree(sashForm);
		createDetailsPanel(sashForm);

		sashForm.setWeights(70, 30);
	}

	private void createTestTree(Composite parent) {
		Group treeGroup = new Group(parent, SWT.NONE);
		treeGroup.setText("Тесты");
		treeGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		treeGroup.setLayout(new GridLayout(1, false));

		treeViewer = new TreeViewer(treeGroup, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));

		treeViewer.setContentProvider(contentProvider);
		treeViewer.setLabelProvider(new GTestViewLabelProvider());

		treeViewer.addSelectionChangedListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			Object selectedElement = selection.getFirstElement();
			showDetails(selectedElement);
		});

		treeViewer.addDoubleClickListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			Object selectedElement = selection.getFirstElement();
			if (selectedElement instanceof TestInfo testCase) {
				openTestInEditor(testCase);
			}
		});
	}

	private void createDetailsPanel(Composite parent) {
		Group detailsGroup = new Group(parent, SWT.NONE);
		detailsGroup.setText("Детали теста");
		detailsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		detailsGroup.setLayout(new GridLayout(1, false));

		detailsText = new Text(detailsGroup,
				SWT.BORDER | SWT.MULTI | SWT.VERTICAL | SWT.READ_ONLY | SWT.WRAP | SWT.H_SCROLL);
		detailsText.setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	private void updateStatsPanel(GTestResult result) {
	    if (result == null) {
	        if (executedLabel != null && !executedLabel.isDisposed()) {
	            executedLabel.setText("Запущено: 0/0");
	        }
	        if (failedLabel != null && !failedLabel.isDisposed()) {
	            failedLabel.setText("Провалено: 0");
	        }
	        if (erroredLabel != null && !erroredLabel.isDisposed()) {
	            erroredLabel.setText("Ошибок: 0");
	        }
	        
	        return;
	    }

	    int executed = result.tests() - result.disabled();
	    int failed = result.failures();
	    int errors = result.errors();

	    if (executedLabel != null && !executedLabel.isDisposed()) {
	        executedLabel.setText("Запущено: " + executed + "/" + result.tests());
	    }
	    
	    if (failedLabel != null && !failedLabel.isDisposed()) {
	        failedLabel.setText("Провалено: " + failed);
	    }
	    
	    if (erroredLabel != null && !erroredLabel.isDisposed()) {
	        erroredLabel.setText("Ошибок: " + errors);
	    }
	}

	private IProject getActiveProject() {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		IViewPart projectExplorer = page.findView("org.eclipse.ui.navigator.ProjectExplorer");

		if (projectExplorer != null) {
			ISelection selection = page.getSelection(projectExplorer.getSite().getId());

			if (selection != null && !selection.isEmpty()
					&& selection instanceof IStructuredSelection structuredSelection) {
				IProject project = getProjectFromSelection(structuredSelection);
				if (project != null && project.isOpen()) {
					return project;
				}
			}
		}

		IEditorPart activeEditor = page.getActiveEditor();
		if (activeEditor != null) {
			IProject project = getProjectFromEditor(activeEditor);
			if (project != null && project.isOpen()) {
				return project;
			}
		}

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();
		for (IProject project : projects) {
			if (project.isOpen()) {
				return project;
			}
		}

		return null;
	}

	private IProject getProjectFromEditor(IEditorPart editor) {
		if (editor == null)
			return null;

		IEditorInput input = editor.getEditorInput();
		if (input instanceof IFileEditorInput fileInput) {
			return fileInput.getFile().getProject();
		}

		IResource resource = input.getAdapter(IResource.class);
		if (resource != null) {
			return resource.getProject();
		}

		return null;
	}

	private IProject getProjectFromSelection(IStructuredSelection selection) {
		if (selection == null || selection.isEmpty())
			return null;

		Object firstElement = selection.getFirstElement();

		if (firstElement instanceof IResource resource) {
			return resource.getProject();
		}

		if (firstElement instanceof IAdaptable adaptable) {
			IResource resource = adaptable.getAdapter(IResource.class);
			if (resource != null) {
				return resource.getProject();
			}
		}

		return null;
	}

	private void loadTestsForActiveProject() {
		activeProject = getActiveProject();
		if (activeProject == null) {
			showErrorMessage("Не удалось определить активный проект");
			return;
		}

		Display.getDefault().asyncExec(() -> {
			try {
				setPartName("GTest - " + activeProject.getName());
				
				IFile testFile = findTestResultsFile(activeProject);

				if (testFile != null) {
					GTestResult result = parser.parseJsonFile(testFile);
					displayTestResults(result);
					showDetails(null);
				} else {
					displayTestResults(null);
					showErrorMessage("Не найдены результаты тестирования для " + activeProject.getName());
				}
			} catch (IOException e) {
				showErrorMessage("Ошибка при загрузке тестов: " + e.getMessage());
			}
		});
	}

	private IFile findTestResultsFile(IProject project) {
		if (project == null || !project.exists() || !project.isOpen()) {
			return null;
		}

		IEclipsePreferences preferences = new ProjectScope(project).getNode(GTestConstants.GTEST_NODE);
		String testsFolderName = preferences.get(GTestConstants.TESTS_FOLDER_KEY,
				GTestConstants.DEFAULT_TESTS_FOLDER_NAME);

		IFile testDetailsFile = project.getFolder("build").getFolder("Release").getFolder(testsFolderName)
				.getFile("test_detail.json");
		try {
			testDetailsFile.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			Platform.getLog(getClass()).error(e.getMessage());
		}

		if (testDetailsFile.exists()) {
			return testDetailsFile;
		}

		return null;
	}

	private void displayTestResults(GTestResult result) {
		if (treeViewer != null && !treeViewer.getControl().isDisposed()) {
			Display.getDefault().asyncExec(() -> {
				treeViewer.setInput(result);
				treeViewer.expandAll();
				updateStatsPanel(result);
				applyFilters();
			});
		}
	}

	private void showDetails(Object element) {
		if (detailsText == null || detailsText.isDisposed())
			return;

		StringBuilder details = new StringBuilder();

		if (element instanceof GTestResult result) {
			details.append("=== ОБЩИЕ РЕЗУЛЬТАТЫ ===\n\n");
			details.append(
					String.format("Проект: %s%n", activeProject != null ? activeProject.getName() : "неизвестно"));
			details.append(String.format("Всего тестов: %d%n", result.tests()));
			details.append(String.format("Запущено: %d%n", result.getExecutedCount()));
			details.append(String.format("Пройдено: %d%n", result.getPassedCount()));
			details.append(String.format("Провалено: %d%n", result.failures()));
			details.append(String.format("Ошибок: %d%n", result.errors()));
			details.append(String.format("Отключено: %d%n", result.disabled()));
			details.append(String.format("Время выполнения: %s%n", result.time()));

			if (result.testsuites() != null) {
				details.append(String.format("%nТестовых наборов: %d%n", result.testsuites().size()));
			}

		} else if (element instanceof TestSuite suite) {
			details.append("=== ТЕСТОВЫЙ НАБОР ===\n\n");
			details.append(String.format("Название: %s%n", suite.name()));
			details.append(String.format("Тестов: %d%n", suite.tests()));
			details.append(String.format("Запущено: %d%n", suite.tests() - suite.disabled()));
			details.append(String.format("Пройдено: %d%n", suite.tests() - suite.failures()));
			details.append(String.format("Провалено: %d%n", suite.failures()));
			details.append(String.format("Отключено: %d%n", suite.disabled()));
			details.append(String.format("Время выполнения: %s%n", suite.time()));

			if (suite.testsuite() != null) {
				details.append(String.format("%nТестовые случаи: %d%n", suite.testsuite().size()));
			}

		} else if (element instanceof TestInfo testInfo) {
			details.append("=== ТЕСТОВЫЙ СЛУЧАЙ ===\n\n");
			details.append(String.format("Название: %s%n", testInfo.name()));
			details.append(String.format("Время: %s%n", testInfo.time()));

			if (testInfo.failures() != null && !testInfo.failures().isEmpty()) {
				details.append("\n=== ОШИБКИ (").append(testInfo.failures().size()).append(") ===\n");
				for (int i = 0; i < testInfo.failures().size(); i++) {
					Failure failure = testInfo.failures().get(i);
					details.append(String.format("%nОшибка #%d:%n", i + 1));
					if (!failure.type().isEmpty()) {
						details.append(String.format("Тип: %s%n", failure.type()));
					}

					details.append(String.format("Сообщение:%n%s%n", failure.failure()));
				}
			}

			if (testInfo.skipped() != null && !testInfo.skipped().isEmpty()) {
				details.append("\n=== ПРОПУЩЕН ===\n");
				for (Skipped skipped : testInfo.skipped()) {
					details.append(skipped.message()).append("\n");
				}
			}

			if (testInfo.isFailed()) {
				details.append("ТЕСТ НЕ ПРОЙДЕН\n");
			} else if (testInfo.isPassed()) {
				details.append("ТЕСТ ПРОЙДЕН\n");
			} else if (testInfo.isSkipped()) {
				details.append("ТЕСТ ПРОПУЩЕН\n");
			} else if (testInfo.isDisabled()) {
				details.append("ТЕСТ ОТКЛЮЧЕН\n");
			}

		} else {
			details.append("Выберите тест для просмотра деталей");
		}

		detailsText.setText(details.toString());
	}

	private void openTestInEditor(TestInfo testCase) {
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			String filePath = testCase.file();
			IFile file = findFileInWorkspace(filePath);

			if (file != null && file.exists()) {
				IDE.openEditor(page, file, true);

				IEditorPart editor = page.getActiveEditor();
				if (editor instanceof ITextEditor textEditor) {
					IDocumentProvider provider = textEditor.getDocumentProvider();
					if (provider != null) {
						IDocument document = provider.getDocument(editor.getEditorInput());
						if (document != null) {
							try {
								int offset = document.getLineOffset(testCase.line() - 1);
								textEditor.selectAndReveal(offset, 4);
							} catch (BadLocationException e) {
								//empty
							}
						}
					}
				}
			}
		} catch (CoreException e) {
			Platform.getLog(getClass()).error(e.getMessage(), e);
			showErrorMessage("Не удалось открыть файл: " + e.getMessage());
		}
	}

	private IFile findFileInWorkspace(String absolutePath) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IFile[] files = root.findFilesForLocationURI(new File(absolutePath).toURI());
		if (files.length > 0) {
			return files[0];
		}

		return null;
	}

	private void showErrorMessage(String message) {
		if (detailsText != null && !detailsText.isDisposed()) {
			Display.getDefault().asyncExec(() -> detailsText.setText("====ОШИБКА====\n\n" + message));
		}
	}

	@Override
	public void setFocus() {
		if (treeViewer != null && !treeViewer.getControl().isDisposed()) {
			treeViewer.getControl().setFocus();

			loadTestsForActiveProject();
		}
	}
}