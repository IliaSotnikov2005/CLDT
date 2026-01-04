package su.softcom.cldt.testing.gtest.ui;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

/**
 * Обновляет тесты при обновлении проекта.
 */
public class GTestRefreshListener implements IExecutionListener {
    
    private static GTestRefreshListener instance;
    
    /**
     * Инициализирует слушатель.
     */
    public static void init() {
        if (instance == null) {
            instance = new GTestRefreshListener();
            instance.start();
        }
    }
    
    private void start() {
        ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
        if (commandService != null) {
            commandService.addExecutionListener(this);
        }
    }
    
    @Override
    public void postExecuteSuccess(String commandId, Object returnValue) {
        if ("org.eclipse.ui.file.refresh".equals(commandId)) {
            IProject refreshedProject = getRefreshedProject();
            if (refreshedProject != null) {
                refreshGTestForProject(refreshedProject);
            }
        }
    }
    
    private IProject getRefreshedProject() {
        try {
            ISelection selection = PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow()
                    .getSelectionService()
                    .getSelection();
            
            if (selection instanceof IStructuredSelection structuredSelection) {
                Object firstElement = structuredSelection.getFirstElement();
                if (firstElement instanceof IProject project) {
                    return project;
                }
            }
        } catch (Exception e) {
            Platform.getLog(getClass()).error("Error getting project: " + e.getMessage());
        }
        return null;
    }
    
    private void refreshGTestForProject(IProject project) {
        GTestContentProvider provider = GTestContentProvider.getInstance();
        if (provider != null) {
            provider.refreshProjectRoot(project);
        }
    }
    
    @Override
    public void preExecute(String commandId, ExecutionEvent event) {
    	// empty
    }
    
    @Override
    public void notHandled(String commandId, NotHandledException exception) {
    	// empty
    }
    
    @Override
    public void postExecuteFailure(String commandId, ExecutionException exception) {
    	// empty
    }
}