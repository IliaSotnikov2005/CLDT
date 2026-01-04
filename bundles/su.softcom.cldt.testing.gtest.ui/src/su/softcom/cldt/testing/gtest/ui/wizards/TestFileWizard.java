package su.softcom.cldt.testing.gtest.ui.wizards;

import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;

import su.softcom.cldt.testing.gtest.core.GTestUtils;
import su.softcom.cldt.testing.gtest.ui.wizards.TestTypeSelectionPage.TestType;

/**
 * Мастер создания тестового файла.
 */
public class TestFileWizard extends AbstractTestWizard {
    
    private ProjectSelectionPage projectSelectionPage;
    private TestTypeSelectionPage testTypeSelectionPage;
    
    @Override
    public void addPages() {
        projectSelectionPage = new ProjectSelectionPage();
        addPage(projectSelectionPage);
    }
    
    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        if (page == projectSelectionPage) {
            IProject selectedProject = projectSelectionPage.getSelectedProject();
            if (selectedProject != null) {
                project = selectedProject;
                
                testTypeSelectionPage = new TestTypeSelectionPage(selectedProject);
                testTypeSelectionPage.setWizard(this);
                return testTypeSelectionPage;
            }
        }
        
        return null;
    }
    
    @Override
    public boolean canFinish() {
        IWizardPage currentPage = getContainer().getCurrentPage();
        return currentPage == testTypeSelectionPage && 
               testTypeSelectionPage.isPageComplete();
    }
    
    @Override
    public boolean performFinish() {
        try {
            String testsFolderName = projectSelectionPage.getTestsFolder();
            
            IFolder testFolder = GTestUtils.createTestsFolderIfNotExists(project, testsFolderName);
            
            String fileName;
            
            if (testTypeSelectionPage.getSelectedTestType() == TestType.EMPTY) {
            	TestConfiguration config = testTypeSelectionPage.getTestConfiguration();
                context.put("testName", config.testName());
                fileName = config.testName() + ".cpp";
                generateTestFile("simple_test.vm", fileName, testFolder);
            } else {
                TestConfiguration config = testTypeSelectionPage.getTestConfiguration();
                
                context.put("newline", "\n");
                context.put("testName", config.testName());
                context.put("className", config.className());
                context.put("methods", config.methods());
                context.put("isFixtureNeeded", config.isFixtureNeeded());
                context.put("headerPath", config.headerPath());
                context.put("namespace", config.namespace());
                
                fileName = config.testName() + ".cpp";
                generateTestFile("test_template.vm", fileName, testFolder);
            }
            
            return super.performFinish(fileName);
            
        } catch (Exception e) {
            Platform.getLog(getClass()).error(e.getMessage());
            MessageDialog.openError(getShell(), "Ошибка", 
                "Ошибка при создании теста: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Конфигурация теста.
     */
    record TestConfiguration(String testName, String className, List<String> methods, 
            boolean isFixtureNeeded, String headerPath, String namespace) {
		@Override
	    public String toString() {
	        return String.format("TestConfiguration{testName='%s', className='%s', methods=%s, isFixtureNeeded=%s, headerPath=%s}",
	            testName, className, methods, isFixtureNeeded, headerPath);
	    }
	}
}