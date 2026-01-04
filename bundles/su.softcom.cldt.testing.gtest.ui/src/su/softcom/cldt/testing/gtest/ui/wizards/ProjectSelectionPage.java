package su.softcom.cldt.testing.gtest.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import su.softcom.cldt.core.CMakeProjectNature;
import su.softcom.cldt.testing.gtest.core.GTestConstants;

/**
 * Страница выбора проекта для мастера создания теста.
 */
public class ProjectSelectionPage extends WizardPage {
    
    private Combo projectCombo;
    private Text testsFolderText;
    private IProject selectedProject;
    private List<IProject> cmakeProjects = new ArrayList<>();
    
    /**
     * Создаёт новую страницу.
     */
    public ProjectSelectionPage() {
        super("projectSelectionPage");
        setTitle("Настройки теста");
        setDescription("Выберите проект и директорию для тестов");
    }
    
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new GridLayout(1, false));
        
        Group projectSettingsGroup = new Group(container, SWT.NONE);
        projectSettingsGroup.setText("Настройки проекта");
        projectSettingsGroup.setLayout(new GridLayout(2, false));
        projectSettingsGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        
        Label projectLabel = new Label(projectSettingsGroup, SWT.NONE);
        projectLabel.setText("Проект:");
        projectLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        
        projectCombo = new Combo(projectSettingsGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        projectCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        projectCombo.addModifyListener(e -> onProjectSelected());
        
        Label folderLabel = new Label(projectSettingsGroup, SWT.NONE);
        folderLabel.setText("Директория тестов:");
        folderLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        
        testsFolderText = new Text(projectSettingsGroup, SWT.BORDER);
        testsFolderText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        testsFolderText.setEditable(false);
        
        loadAvailableProjects();
        
        validatePage();
    }
    
    /**
     * Получает выбранный проект.
     * @return выбранный проект
     */
    public IProject getSelectedProject() {
        return selectedProject;
    }
    
    /**
     * Получает папку тестов проекта.
     * @return папка тестов проекта
     */
    public String getTestsFolder() {
        return testsFolderText.getText();
    }
    
    private void loadAvailableProjects() {
        cmakeProjects.clear();
        projectCombo.removeAll();
        
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject[] projects = workspace.getRoot().getProjects();
        
        for (IProject project : projects) {
            try {
                if (project.isOpen() && project.hasNature(CMakeProjectNature.ID)) {
                    cmakeProjects.add(project);
                    projectCombo.add(project.getName());
                }
            } catch (CoreException e) {
                Platform.getLog(getClass()).error(e.getMessage());
            }
        }
        
        if (!cmakeProjects.isEmpty()) {
            IProject activeProject = findActiveProject();
            if (activeProject != null && cmakeProjects.contains(activeProject)) {
                int index = cmakeProjects.indexOf(activeProject);
                projectCombo.select(index);
            } else {
                projectCombo.select(0);
            }
            
            onProjectSelected();
        } else {
            projectCombo.add("Нет доступных CMake проектов");
            projectCombo.select(0);
            projectCombo.setEnabled(false);
        }
    }
    
    private IProject findActiveProject() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
        	return null;
        }
        
        IWorkbenchPage page = window.getActivePage();
        if (page == null) {
        	return null;
        }
        
        ISelection selection = page.getSelection();
        if (selection instanceof IStructuredSelection structSelection) {
            Object firstElement = structSelection.getFirstElement();
            if (firstElement instanceof IResource resource) {
                return resource.getProject();
            } else if (firstElement instanceof IAdaptable adaptable) {
                IResource resource = adaptable.getAdapter(IResource.class);
                if (resource != null) {
                    return resource.getProject();
                }
            }
        }
        
        return null;
    }
    
    private void onProjectSelected() {
        int index = projectCombo.getSelectionIndex();
        if (index >= 0 && index < cmakeProjects.size()) {
            selectedProject = cmakeProjects.get(index);
            
            IEclipsePreferences preferences = new ProjectScope(selectedProject).getNode(GTestConstants.GTEST_NODE);
            String testsFolder = preferences.get(GTestConstants.TESTS_FOLDER_KEY, GTestConstants.DEFAULT_TESTS_FOLDER_NAME);
            testsFolderText.setText(testsFolder);
        } else {
            selectedProject = null;
            testsFolderText.setText("не найдена");
        }
        
        validatePage();
    }
    
    private void validatePage() {
        String errorMessage = null;
        
        if (selectedProject == null) {
            errorMessage = "Выберите проект CMake";
        } else if (testsFolderText.getText().trim().isEmpty()) {
            errorMessage = "Директория тестов не может быть пустой";
        }
        
        setPageComplete(errorMessage == null);
        setErrorMessage(errorMessage);
    }
}