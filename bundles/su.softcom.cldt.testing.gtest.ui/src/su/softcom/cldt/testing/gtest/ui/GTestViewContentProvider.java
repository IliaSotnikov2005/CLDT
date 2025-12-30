package su.softcom.cldt.testing.gtest.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;

import su.softcom.cldt.testing.gtest.ui.models.GTestResult;
import su.softcom.cldt.testing.gtest.ui.models.TestInfo;
import su.softcom.cldt.testing.gtest.ui.models.TestSuite;

/**
 * Content provider для окна результатов тестирования GTest.
 */
public class GTestViewContentProvider implements ITreeContentProvider {
    
    private boolean showFailedOnly = false;
    private boolean showSkippedOnly = false;
    private boolean showDisabledOnly = false;
    
    public void setFilters(boolean showFailedOnly, boolean showSkipped, boolean showDisabled) {
        this.showFailedOnly = showFailedOnly;
        this.showSkippedOnly = showSkipped;
        this.showDisabledOnly = showDisabled;
    }
    
    @Override
    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof GTestResult result) {
            if (showFailedOnly || showSkippedOnly || showDisabledOnly) {
                return result.testsuites().stream()
                    .filter(this::hasVisibleTests)
                    .toArray();
            }
            
            return result.testsuites().toArray();
        }
        return new Object[0];
    }
    
    private boolean hasVisibleTests(TestSuite suite) {
        if (suite.testsuite() == null) return false;
        for (TestInfo test : suite.testsuite()) {
            if (shouldShowTest(test)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof TestSuite suite) {
            if (suite.testsuite() == null) {
                return new Object[0];
            }
            
            List<TestInfo> filteredTests = new ArrayList<>();
            for (TestInfo test : suite.testsuite()) {
                if (shouldShowTest(test)) {
                    filteredTests.add(test);
                }
            }
            
            return filteredTests.toArray();
        }
        
        return new Object[0];
    }
    
    private boolean shouldShowTest(TestInfo test) {
        if (showFailedOnly) {
            return test.isFailed();
        }
        
        if (showSkippedOnly) {
            return test.isSkipped();
        }
        
        if (showDisabledOnly) {
            return test.isDisabled();
        }
        
        return true;
    }
    
    @Override
    public Object getParent(Object element) {
        return null;
    }
    
    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof TestSuite suite) {
            return hasVisibleTests(suite);
        }
        
        return false;
    }
}