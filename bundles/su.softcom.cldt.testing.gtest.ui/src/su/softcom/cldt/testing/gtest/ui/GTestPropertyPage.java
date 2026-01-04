package su.softcom.cldt.testing.gtest.ui;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.fasterxml.jackson.core.JsonProcessingException;

import su.softcom.cldt.core.CMakeUpdateNature;
import su.softcom.cldt.internal.core.builders.CMakeModifier;
import su.softcom.cldt.testing.gtest.core.GTestConstants;
import su.softcom.cldt.testing.gtest.core.GTestUtils;
import su.softcom.cldt.testing.gtest.core.model.GTestInstance;

/**
 * Страница настроек тестирования для проекте.
 */
public final class GTestPropertyPage extends FieldEditorPreferencePage implements IWorkbenchPropertyPage {

	private IProject project;
	private boolean needsBuildUpdate = false;
	
	ComboFieldEditor gtestInstanceCombo;

	/**
	 * Создаёт страницу настроек тестирования.
	 */
	public GTestPropertyPage() {
		super(GRID);
		setDescription("Настройки Google Test для проекта");
		noDefaultAndApplyButton();
	}

	@Override
	public void setElement(IAdaptable element) {
		this.project = element.getAdapter(IProject.class);
	}

	@Override
	public IAdaptable getElement() {
		return project;
	}

	@Override
	protected void createFieldEditors() {
		if (project == null) {
			setErrorMessage("Не удалось определить проект");
			return;
		}

		setPreferenceStore(new ScopedPreferenceStore(new ProjectScope(project), GTestConstants.GTEST_NODE));
		getPreferenceStore().setDefault(GTestConstants.TESTS_FOLDER_KEY, GTestConstants.DEFAULT_TESTS_FOLDER_NAME);

		List<GTestInstance> availableInstances = GTestInstancesManager.getRefreshedInstanses();

		String[][] gtestInstances = createComboItems(availableInstances);

		gtestInstanceCombo = new ComboFieldEditor(GTestConstants.GTEST_INSTANCE_KEY,
				"Экземпляр GTest:", gtestInstances, getFieldEditorParent()) {
			@Override
			protected void fireValueChanged(String property, Object oldValue, Object newValue) {
				super.fireValueChanged(property, oldValue, newValue);
				if (!String.valueOf(oldValue).equals(String.valueOf(newValue))) {
					needsBuildUpdate = true;
				}
			}
		};

		addField(gtestInstanceCombo);

		StringFieldEditor testsFolderEditor = new StringFieldEditor(GTestConstants.TESTS_FOLDER_KEY,
				"Название папки для тестов:", getFieldEditorParent()) {
			private Text textField;

			@Override
			public Text getTextControl(Composite parent) {
				if (textField == null) {
					textField = super.getTextControl(parent);
					setupTextField();
				}

				return textField;
			}

			@Override
			protected void doFillIntoGrid(Composite parent, int numColumns) {
				super.doFillIntoGrid(parent, numColumns);
				setupTextField();
			}

			private void setupTextField() {
				if (textField != null && !textField.isDisposed()) {
					textField.setText(GTestConstants.DEFAULT_TESTS_FOLDER_NAME);

					textField.addFocusListener(new FocusAdapter() {
						@Override
						public void focusLost(FocusEvent e) {
							if (textField.getText().trim().isEmpty()) {
								textField.setText(GTestConstants.DEFAULT_TESTS_FOLDER_NAME);
							}
						}
					});

					GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
					gd.widthHint = 300;
					textField.setLayoutData(gd);
				}
			}

			@Override
			protected void fireValueChanged(String property, Object oldValue, Object newValue) {
				super.fireValueChanged(property, oldValue, newValue);
				if (!String.valueOf(oldValue).equals(String.valueOf(newValue))) {
					needsBuildUpdate = true;
				}
			}

			@Override
			protected boolean checkState() {
				String error = validateFolderName(getStringValue());
				if (error != null) {
					showErrorMessage(error);
					return false;
				}

				clearErrorMessage();
				return true;
			}
		};

		addField(testsFolderEditor);
		
		Composite linkComposite = new Composite(getFieldEditorParent(), SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 10;
		layout.marginHeight = 40;
		linkComposite.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		linkComposite.setLayoutData(gd);
		

		Link prefsLink = new Link(linkComposite, SWT.NONE);
		prefsLink.setText("<a>Настроить глобальные параметры GTest...</a>");
		prefsLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
		prefsLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openGTestPreferences();
			}
		});

		try {
			if (project != null && !project.hasNature(CMakeUpdateNature.ID)) {
				setMessage("Для полной функциональности требуется синхронизация CMake ", IMessageProvider.WARNING);
			}
		} catch (CoreException e) {
			log(e);
		}
	}

	@Override
	public boolean performOk() {
		boolean result = super.performOk();

		if (result && project != null) {
			String testsFolder = getPreferenceStore().getString(GTestConstants.TESTS_FOLDER_KEY);

			testsFolder = !testsFolder.equals("") ? testsFolder : GTestConstants.DEFAULT_TESTS_FOLDER_NAME;
			if (GTestUtils.createTestsFolderIfNotExists(project, testsFolder) != null) {
				needsBuildUpdate = true;
			}

			if (needsBuildUpdate) {
				updateProjectCMake();
				needsBuildUpdate = false;
			}
		}

		return result;
	}
	
	private String[][] createComboItems(List<GTestInstance> instances) {
		String[][] comboItems = new String[instances.size()][2];
		
		for (int i = 0; i < instances.size(); i++) {
			GTestInstance instance = instances.get(i);
			String displayName = formatDisplayName(instance);
			
			try {
				String instanceJson = instance.toJson();
				comboItems[i][0] = displayName;
				comboItems[i][1] = instanceJson;
			} catch (JsonProcessingException e) {
				log(e);
			}
		}
		
		return comboItems;
	}

	private String formatDisplayName(GTestInstance instance) {
		String path = instance.path();
		String version = instance.version();
		String tag = instance.tag();
		String type = instance.type();
		
		String baseName = String.format("%s (%s) - %s", !tag.isEmpty() ? tag : path, version, type);
		
		if (baseName.length() > 80) {
			int maxPathLength = 60 - instance.version().length() - instance.type().length();
			if (path.length() > maxPathLength) {
				int partLength = maxPathLength / 2 - 1;
				String shortenedPath = path.substring(0, partLength) + "..."
						+ path.substring(path.length() - partLength);
				baseName = String.format("%s (%s) - %s", shortenedPath, version, type);
			}
		}

		return baseName;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();

		getPreferenceStore().setValue(GTestConstants.TESTS_FOLDER_KEY, GTestConstants.DEFAULT_TESTS_FOLDER_NAME);
		needsBuildUpdate = true;
	}

	@SuppressWarnings("restriction")
	private void updateProjectCMake() {
		try {
			if (!project.isOpen() || !project.hasNature(CMakeUpdateNature.ID)) {
				return;
			}

			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, CMakeModifier.ID, null,
					new NullProgressMonitor());
			Platform.getLog(getClass()).info("Обновлён CMakeLists.txt для проекта " + project.getName());
		} catch (CoreException e) {
			log(e);
		}
	}

	private String validateFolderName(String name) {
		if (name == null || name.isEmpty()) {
			return "Имя папки не может быть пустым";
		}

		if (name.endsWith(".") || name.endsWith(" ")) {
			return "Имя не может заканчиваться точкой или пробелом";
		}

		if (!name.matches("\\w([a-zA-Z0-9_\\-\\.]*\\w)?")) {
			return "Имя папки содержит недопустимые символы";
		}

		if (name.length() > 255) {
			return "Имя слишком длинное (максимум 255 символов)";
		}

		return null;
	}
	
	private void openGTestPreferences() {	    
	    Shell shell = getControl().getShell();
	    if (shell != null && !shell.isDisposed()) {
	        shell.dispose();
	    }
	    
	    Display.getDefault().asyncExec(() -> {
	        try {
	            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	            if (window != null) {
	                String prefPageId = GTestPreferencePage.ID;
	                
	                PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
	                    window.getShell(),
	                    prefPageId,
	                    null,
	                    null
	                );
	                
	                if (dialog != null) {
	                    dialog.open();
	                }
	            }
	        } catch (Exception e) {
	            log(e);
	        }
	    });
	}

	private static void log(Throwable t) {
		Bundle bundle = FrameworkUtil.getBundle(GTestPropertyPage.class);
		IStatus status = new Status(IStatus.ERROR, bundle.getSymbolicName(),
				t.getMessage() == null ? t.toString() : t.getMessage(), t);
		Platform.getLog(bundle).log(status);
	}
}
