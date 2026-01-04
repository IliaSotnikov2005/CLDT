package su.softcom.cldt.testing.gtest.core.launch;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;

import su.softcom.cldt.testing.gtest.core.GTestConstants;
import su.softcom.cldt.testing.gtest.core.model.GTestExecutable;

/**
 * Вспомогательные методы для конфигурации запуска тестов GTest.
 */
public final class GTestLaunchUtils {
	private static final String GTEST_OUTPUT_JSON = "test_detail.json";
	
	private GTestLaunchUtils() {
		// empty
	}
    
    /**
     * Создаёт конфигурацию запуска для {@link GTestExecutable}.
     *
     * @param project проект
     * @param gtest объект теста
     * @param gtestFilter фильтр для запуска теста
     * @return объект {@link ILaunchConfiguration}
     * @throws {@link CoreException} при внутренней ошибке
     */
    public static ILaunchConfiguration createLaunchConfig(IProject project, 
                                                         GTestExecutable gtest,
                                                         String gtestFilter) throws CoreException {
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
       
        ILaunchConfigurationType configType = launchManager
            .getLaunchConfigurationType(GTestLaunchDelegate.ID);
        
        String configName = createConfigName(project, gtest, gtestFilter);
        ILaunchConfigurationWorkingCopy workingCopy = configType.newInstance(null, configName);
        
        setupGTestLaunchAttributes(workingCopy, project, gtest, gtestFilter);
        
        return workingCopy.doSave();
    }
    
    private static String createConfigName(IProject project, GTestExecutable gtest, String gtestFilter) {
        StringBuilder name = new StringBuilder("GTest-");
        name.append(project.getName());
        name.append("-").append(gtest.getExecutable().lastSegment());
        
        if (gtestFilter != null && !gtestFilter.isEmpty()) {
        	gtestFilter = replaceForbiddenSymbols(gtestFilter);
            if (gtestFilter.length() > 20) {
                name.append("-").append(gtestFilter.substring(0, 17)).append("...");
            } else {
                name.append("-").append(gtestFilter.replace(".*", ""));
            }
        }

        return name.toString();
    }
    
    private static void setupGTestLaunchAttributes(ILaunchConfigurationWorkingCopy config,
                                                  IProject project,
                                                  GTestExecutable gtest,
                                                  String gtestFilter) {
        config.setAttribute(GTestLaunchAttr.ATTR_PROJECT, project.getName());
        config.setAttribute(GTestLaunchAttr.ATTR_EXECUTABLE, gtest.getExecutable().toOSString());
        
        IPath workDir = gtest.getExecutable().removeLastSegments(1);
        config.setAttribute(GTestLaunchAttr.ATTR_WORKDIR, workDir.toOSString());
        
        if (gtestFilter != null && !gtestFilter.isEmpty()) {
            config.setAttribute(GTestLaunchAttr.ATTR_FILTER, gtestFilter);
            
            List<String> selectedTests = parseFilterToTestList(gtestFilter);
            config.setAttribute(GTestLaunchAttr.ATTR_SELECTED_TESTS, selectedTests);
        }
        
        String testsFolderName = getTestsFolderName(project);
        IPath projectLocation = project.getLocation();
        IPath jsonOutputPath = projectLocation
            .append("build")
            .append("Release")
            .append(testsFolderName)
            .append(GTEST_OUTPUT_JSON);
        
        List<String> additionalArgs = List.of(
            "--gtest_output=json:" + jsonOutputPath.toOSString()
        );
        
        config.setAttribute(GTestLaunchAttr.ATTR_ARGS, String.join(" ", additionalArgs));
        config.setAttribute(GTestLaunchAttr.ATTR_JSON_OUTPUT_PATH, jsonOutputPath.toOSString());
    }
    
    private static List<String> parseFilterToTestList(String gtestFilter) {
        return Arrays.stream(gtestFilter.split(":"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
    
    private static String getTestsFolderName(IProject project) {
        IEclipsePreferences preferences = new ProjectScope(project).getNode(GTestConstants.GTEST_NODE);
        
        String folderName = preferences.get(GTestConstants.TESTS_FOLDER_KEY, 
                                            GTestConstants.DEFAULT_TESTS_FOLDER_NAME);
        
        if (folderName == null || folderName.trim().isEmpty()) {
            folderName = GTestConstants.DEFAULT_TESTS_FOLDER_NAME;
        }
        
        return folderName;
    }

    private static String replaceForbiddenSymbols(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        String safe = input
            .replace("/", "-")
            .replace("\\", "-")
            .replace(":", "-")
            .replace("?", "-")
            .replace("\"", "-")
            .replace("<", "-")
            .replace(">", "-")
            .replace("|", "-");
        
        safe = safe.trim();
        
        return safe;
    }
}