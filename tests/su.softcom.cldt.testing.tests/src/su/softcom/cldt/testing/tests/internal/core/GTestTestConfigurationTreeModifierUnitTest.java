package su.softcom.cldt.testing.tests.internal.core;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import su.softcom.cldt.core.cmake.ArgumentNode;
import su.softcom.cldt.core.cmake.CMakeNode;
import su.softcom.cldt.core.cmake.CMakeParser;
import su.softcom.cldt.core.cmake.CMakeRoot;
import su.softcom.cldt.core.cmake.CommandNode;
import su.softcom.cldt.core.cmake.CommentNode;
import su.softcom.cldt.core.cmake.ConditionCommand;
import su.softcom.cldt.core.cmake.ICMakeProject;
import su.softcom.cldt.testing.gtest.core.GTestConstants;
import su.softcom.cldt.testing.gtest.core.GTestUtils;
import su.softcom.cldt.testing.gtest.core.model.GTestInstance;
import su.softcom.cldt.testing.gtest.internal.core.GTestTestConfigurationTreeModifier;


@RunWith(MockitoJUnitRunner.class)
public class GTestTestConfigurationTreeModifierUnitTest {

    @Mock
    private ICMakeProject mockCMakeProject;
    
    @Mock
    private IProject mockProject;
    
    private GTestTestConfigurationTreeModifier modifier;
    
    @Before
    public void setUp() {
        modifier = new GTestTestConfigurationTreeModifier();
    }
    
    @Test
    public void testModifyTree_SystemGTest_AddsFindPackageAndLinks() throws Exception {
        String cmakeContent = """
            cmake_minimum_required(VERSION 3.10)
            project(MyTests)
            
            add_executable(unit_test test.cpp)
            add_executable(integration_test integration.cpp)
            
            target_link_libraries(unit_test some_lib)
            target_link_libraries(integration_test another_lib)
            """;
        
        CMakeParser parser = new CMakeParser(cmakeContent, false);
        CMakeRoot tree = parser.parse();
        
        GTestInstance gtest = new GTestInstance("path", "1.12.1", GTestInstance.SYSTEM_TEXT, "Системный");
        
        try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
            gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any()))
                         .thenReturn(gtest);
            
            when(mockCMakeProject.getProject()).thenReturn(mockProject);
            
            modifier.modifyTree(mockCMakeProject, tree);
            
            List<CMakeNode> children = tree.getChildren();
            
            boolean foundStartComment = false;
            boolean foundEndComment = false;
            boolean foundFindPackage = false;
            int linkedTargets = 0;
            boolean unitTestHasSomeLib = false;
            boolean integrationTestHasAnotherLib = false;
         
            for (CMakeNode child : children) {
                if (child instanceof CommentNode comment) {
                    String text = comment.toText().toString().trim();
                    if (text.contains("Сгенерировано GTest-модификатором")) {
                        foundStartComment = true;
                    }
                    if (text.contains("Конец сгенерированного GTest-модификатором")) {
                        foundEndComment = true;
                    }
                }
                if (!foundFindPackage && child instanceof CommandNode cmd && "find_package".equals(cmd.getName())) {
                    List<ArgumentNode> args = cmd.getArgs();
                    if (args.size() >= 2 && 
                        "GTest".equals(args.get(0).getValue()) && 
                        "REQUIRED".equals(args.get(1).getValue())) {
                        foundFindPackage = true;
                    }
                }
                if (child instanceof CommandNode cmd && "target_link_libraries".equals(cmd.getName())) {
                    List<ArgumentNode> args = cmd.getArgs();
                    if (args.size() > 0 && 
                        ("unit_test".equals(args.get(0).getValue()) || 
                         "integration_test".equals(args.get(0).getValue()))) {
                        
                        boolean hasGTest = args.stream()
                            .anyMatch(arg -> GTestConstants.GTEST_LIB.equals(arg.getValue()));
                        boolean hasGTestMain = args.stream()
                            .anyMatch(arg -> GTestConstants.GTEST_MAIN_LIB.equals(arg.getValue()));
                        
                        if (hasGTest && hasGTestMain) {
                            linkedTargets++;
                        }
                    }
                }
                if (child instanceof CommandNode cmd && "target_link_libraries".equals(cmd.getName())) {
                    List<ArgumentNode> args = cmd.getArgs();
                    if (args.size() > 0 && "unit_test".equals(args.get(0).getValue())) {
                        unitTestHasSomeLib = args.stream()
                            .anyMatch(arg -> "some_lib".equals(arg.getValue()));
                    } else if (args.size() > 0 && "integration_test".equals(args.get(0).getValue())) {
                        integrationTestHasAnotherLib = args.stream()
                            .anyMatch(arg -> "another_lib".equals(arg.getValue()));
                    }
                }
            }
            
            assertTrue("Should have start comment", foundStartComment);
            assertTrue("Should have end comment", foundEndComment);
            assertTrue("Should have find_package(GTest REQUIRED) for system GTest", foundFindPackage);
            assertEquals("Both test targets should be linked with GTest", 2, linkedTargets);
            assertTrue("unit_test should preserve some_lib", unitTestHasSomeLib);
            assertTrue("integration_test should preserve another_lib", integrationTestHasAnotherLib);
        }
    }
    
    @Test
    public void testModifyTree_LocalGTest_AddsVariablesAndSubdirectory() throws Exception {
        String cmakeContent = """
            cmake_minimum_required(VERSION 3.10)
            project(MyTests)
            
            add_executable(local_test test.cpp)
            """;
        
        CMakeParser parser = new CMakeParser(cmakeContent, false);
        CMakeRoot tree = parser.parse();
        
        GTestInstance gtest = new GTestInstance("/path/to/gtest", "1.12.1", GTestInstance.USER_TEXT);
        
        try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
            gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any()))
                         .thenReturn(gtest);
            
            when(mockCMakeProject.getProject()).thenReturn(mockProject);
            
            modifier.modifyTree(mockCMakeProject, tree);
            
            List<CMakeNode> children = tree.getChildren();
            
            boolean foundSetCommand = false;
            boolean foundIfCondition = false;
            boolean foundSetVersion = false;
            boolean foundAddSubdirectory = false;
            boolean foundLocalGTestLinking = false;
            
            for (CMakeNode child : children) {
                if (!foundSetCommand && child instanceof CommandNode cmd && "set".equals(cmd.getName())) {
                    List<ArgumentNode> args = cmd.getArgs();
                    if (args.size() >= 2 && 
                        "CLDT_GTEST_ROOT".equals(args.get(0).getValue())) {
                        String pathValue = args.get(1).getValue();
                        if ("\"/path/to/gtest\"".equals(pathValue) || 
                            "/path/to/gtest".equals(pathValue)) {
                            foundSetCommand = true;
                        }
                    }
                }
                if (!foundIfCondition && child instanceof ConditionCommand condition) {
                    CommandNode ifCmd = condition;
                    if ("if".equals(ifCmd.getName())) {
                        List<ArgumentNode> args = ifCmd.getArgs();
                        if (args.size() >= 3 && 
                            "NOT".equals(args.get(0).getValue()) && 
                            "EXISTS".equals(args.get(1).getValue()) &&
                            args.get(2).getValue().contains("CLDT_GTEST_ROOT")) {
                            foundIfCondition = true;
                        }
                    }
                }
                if (!foundSetVersion && child instanceof CommandNode cmd && "set".equals(cmd.getName())) {
                    List<ArgumentNode> args = cmd.getArgs();
                    if (args.size() >= 2 && 
                        "GOOGLETEST_VERSION".equals(args.get(0).getValue()) && 
                        "1.12.1".equals(args.get(1).getValue())) {
                        foundSetVersion = true;
                    }
                }
                if (!foundAddSubdirectory && child instanceof CommandNode cmd && "add_subdirectory".equals(cmd.getName())) {
                    List<ArgumentNode> args = cmd.getArgs();
                    if (args.size() >= 2 && 
                        args.get(0).getValue().contains("CLDT_GTEST_ROOT") &&
                        args.get(1).getValue().contains("CMAKE_BINARY_DIR")) {
                        foundAddSubdirectory = true;
                    }
                }
                if (!foundLocalGTestLinking && child instanceof CommandNode cmd && "target_link_libraries".equals(cmd.getName())) {
                    List<ArgumentNode> args = cmd.getArgs();
                    if (args.size() > 0 && "local_test".equals(args.get(0).getValue())) {
                        boolean hasGTest = args.stream()
                            .anyMatch(arg -> GTestConstants.GTEST.equals(arg.getValue()));
                        boolean hasGTestMain = args.stream()
                            .anyMatch(arg -> GTestConstants.GTEST_MAIN.equals(arg.getValue()));
                        
                        if (hasGTest && hasGTestMain) {
                            foundLocalGTestLinking = true;
                        }
                    }
                }
            }
            
            assertTrue("Should set CLDT_GTEST_ROOT variable for local GTest", foundSetCommand);
            assertTrue("Should have if(NOT EXISTS ...) condition", foundIfCondition);
            assertTrue("Should set GOOGLETEST_VERSION", foundSetVersion);
            assertTrue("Should add GTest subdirectory", foundAddSubdirectory);
            assertTrue("Should link with local GTest libraries", foundLocalGTestLinking);
        }
    }
    
    @Test
    public void testModifyTree_EmbeddedGTest_AddsVariablesAndSubdirectory() throws Exception {
        String cmakeContent = """
            cmake_minimum_required(VERSION 3.10)
            project(MyTests)
            
            add_executable(embedded_test test.cpp)
            """;
        
        CMakeParser parser = new CMakeParser(cmakeContent, false);
        CMakeRoot tree = parser.parse();
        
        GTestInstance gtest = new GTestInstance("/embedded/path", "1.12.1", GTestInstance.EMBEDDED_TEXT);
        
        try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
            gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any()))
                         .thenReturn(gtest);
            
            when(mockCMakeProject.getProject()).thenReturn(mockProject);
            
            modifier.modifyTree(mockCMakeProject, tree);
            
            List<CMakeNode> children = tree.getChildren();
            
            boolean foundSetCommand = false;
            for (CMakeNode child : children) {
                if (child instanceof CommandNode cmd && "set".equals(cmd.getName())) {
                    List<ArgumentNode> args = cmd.getArgs();
                    if (args.size() >= 2 && 
                        "CLDT_GTEST_ROOT".equals(args.get(0).getValue())) {
                        foundSetCommand = true;
                        break;
                    }
                }
            }
            
            assertTrue("Should set CLDT_GTEST_ROOT for embedded GTest", foundSetCommand);
        }
    }
    
    @Test
    public void testModifyTree_WhenNoGTestInstance() throws Exception {
        String cmakeContent = "cmake_minimum_required(VERSION 3.10)\nproject(MyTests)";
        CMakeParser parser = new CMakeParser(cmakeContent, false);
        CMakeRoot tree = parser.parse();
        
        int initialSize = tree.getChildren().size();
        
        try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
            gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any()))
                         .thenReturn(null);
            
            modifier.modifyTree(mockCMakeProject, tree);
            
            assertEquals("Tree should not be modified when no GTest instance", 
                        initialSize, tree.getChildren().size());
        }
    }
    
    @Test
    public void testModifyTree_WhenGTestPathIsEmpty() throws Exception {
        String cmakeContent = "cmake_minimum_required(VERSION 3.10)\nproject(MyTests)";
        CMakeParser parser = new CMakeParser(cmakeContent, false);
        CMakeRoot tree = parser.parse();
        
        int initialSize = tree.getChildren().size();
        
        GTestInstance gtest = new GTestInstance("", "1.12.1", GTestInstance.SYSTEM_TEXT);
        
        try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
            gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any()))
                         .thenReturn(gtest);
            
            when(mockCMakeProject.getProject()).thenReturn(mockProject);
            
            modifier.modifyTree(mockCMakeProject, tree);
            
            assertEquals("Tree should not be modified when GTest path is empty", 
                        initialSize, tree.getChildren().size());
        }
    }
    
    @Test
    public void testModifyTree_RemovesExistingGTestSection() throws Exception {
        String cmakeContent = """
            cmake_minimum_required(VERSION 3.10)
            project(MyTests)
            
            #---------- Сгенерировано GTest-модификатором CMake дерева ------
            find_package(GTest REQUIRED)
            target_link_libraries(old_test gtest gtest_main)
            #---------- Конец сгенерированного GTest-модификатором ----------
            
            add_executable(new_test test.cpp)
            """;
        
        CMakeParser parser = new CMakeParser(cmakeContent, false);
        CMakeRoot tree = parser.parse();
        
        GTestInstance gtest = new GTestInstance("path", "1.12.1", GTestInstance.SYSTEM_TEXT, "Системный");
        
        try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
            gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any()))
                         .thenReturn(gtest);
            
            when(mockCMakeProject.getProject()).thenReturn(mockProject);
            
            modifier.modifyTree(mockCMakeProject, tree);
            
            List<CMakeNode> children = tree.getChildren();
            
            int gtestCommentCount = 0;
            int findPackageCount = 0;
            for (CMakeNode child : children) {
                if (child instanceof CommentNode comment) {
                    String text = comment.toText().toString().trim();
                    if (text.contains("Сгенерировано GTest-модификатором") || 
                        text.contains("Конец сгенерированного GTest-модификатором")) {
                        gtestCommentCount++;
                    }
                }
                if (child instanceof CommandNode cmd && "find_package".equals(cmd.getName())) {
                    List<ArgumentNode> args = cmd.getArgs();
                    if (args.size() >= 2 && 
                        "GTest".equals(args.get(0).getValue()) && 
                        "REQUIRED".equals(args.get(1).getValue())) {
                        findPackageCount++;
                    }
                }
            }
            
            assertEquals("Should have exactly 2 GTest comments after removing old section", 
                        2, gtestCommentCount);
            assertEquals("Should have exactly one find_package(GTest) command", 
                        1, findPackageCount);
        }
    }
    
    @Test
    public void testModifyTree_TestTargetsLinking_NewTarget() throws Exception {
        String cmakeContent = """
            add_executable(new_test_without_link test.cpp)
            """;
        
        CMakeParser parser = new CMakeParser(cmakeContent, false);
        CMakeRoot tree = parser.parse();
        
        GTestInstance gtest = new GTestInstance("path", "1.12.1", GTestInstance.SYSTEM_TEXT, "Системный");
        
        try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
            gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any()))
                         .thenReturn(gtest);
            
            when(mockCMakeProject.getProject()).thenReturn(mockProject);
            
            modifier.modifyTree(mockCMakeProject, tree);
            
            List<CMakeNode> children = tree.getChildren();
            
            boolean foundNewLinkCommand = false;
            for (CMakeNode child : children) {
                if (child instanceof CommandNode cmd && "target_link_libraries".equals(cmd.getName())) {
                    List<ArgumentNode> args = cmd.getArgs();
                    if (args.size() >= 4 && 
                        "new_test_without_link".equals(args.get(0).getValue()) &&
                        "PRIVATE".equals(args.get(1).getValue())) {
                        
                        boolean hasGTest = args.stream()
                            .anyMatch(arg -> GTestConstants.GTEST_LIB.equals(arg.getValue()));
                        boolean hasGTestMain = args.stream()
                            .anyMatch(arg -> GTestConstants.GTEST_MAIN_LIB.equals(arg.getValue()));
                        
                        if (hasGTest && hasGTestMain) {
                            foundNewLinkCommand = true;
                            break;
                        }
                    }
                }
            }
            
            assertTrue("Should create new link command for test without existing link", 
                      foundNewLinkCommand);
        }
    }
    
    @Test
    public void testModifyTree_TestTargetsLinking_ExistingLinkWithoutPrivate() throws Exception {
        String cmakeContent = """
            cmake_minimum_required(VERSION 3.10)
            project(MyTests)
            
            add_executable(test_no_private test.cpp)
            
            target_link_libraries(test_no_private some_lib)
            """;
        
        CMakeParser parser = new CMakeParser(cmakeContent, false);
        CMakeRoot tree = parser.parse();
        
        GTestInstance gtest = new GTestInstance("path", "1.12.1", GTestInstance.SYSTEM_TEXT, "Системный");
        
        try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
            gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any()))
                         .thenReturn(gtest);
            
            when(mockCMakeProject.getProject()).thenReturn(mockProject);
            
            modifier.modifyTree(mockCMakeProject, tree);
            
            List<CMakeNode> children = tree.getChildren();
            
            boolean foundLinkWithPrivate = false;
            for (CMakeNode child : children) {
                if (child instanceof CommandNode cmd && "target_link_libraries".equals(cmd.getName())) {
                    List<ArgumentNode> args = cmd.getArgs();
                    if (args.size() > 0 && "test_no_private".equals(args.get(0).getValue())) {
                        boolean hasPrivate = args.stream()
                            .anyMatch(arg -> "PRIVATE".equals(arg.getValue()));
                        boolean hasSomeLib = args.stream()
                            .anyMatch(arg -> "some_lib".equals(arg.getValue()));
                        
                        if (hasPrivate && hasSomeLib) {
                            foundLinkWithPrivate = true;
                            break;
                        }
                    }
                }
            }
            
            assertTrue("Should add PRIVATE keyword to existing link command", foundLinkWithPrivate);
        }
    }
    
    @Test
    public void testModifyTree_TestTargetsLinking_RemovesDuplicateGTestLibraries() throws Exception {
        String cmakeContent = """
            cmake_minimum_required(VERSION 3.10)
            project(MyTests)
            
            add_executable(test_with_gtest test.cpp)
            
            target_link_libraries(test_with_gtest PRIVATE gtest gtest_main some_lib gtest)
            """;
        
        CMakeParser parser = new CMakeParser(cmakeContent, false);
        CMakeRoot tree = parser.parse();
        
        GTestInstance gtest = new GTestInstance("path", "1.12.1", GTestInstance.USER_TEXT);
        
        try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
            gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any()))
                         .thenReturn(gtest);
            
            when(mockCMakeProject.getProject()).thenReturn(mockProject);
            
            modifier.modifyTree(mockCMakeProject, tree);
            
            List<CMakeNode> children = tree.getChildren();
            
            for (CMakeNode child : children) {
                if (child instanceof CommandNode cmd && "target_link_libraries".equals(cmd.getName())) {
                    List<ArgumentNode> args = cmd.getArgs();
                    if (args.size() > 0 && "test_with_gtest".equals(args.get(0).getValue())) {
                        long gtestCount = args.stream()
                            .filter(arg -> GTestConstants.GTEST.equals(arg.getValue()))
                            .count();
                        long gtestMainCount = args.stream()
                            .filter(arg -> GTestConstants.GTEST_MAIN.equals(arg.getValue()))
                            .count();
                        
                        assertEquals("Should have exactly one gtest library", 1, gtestCount);
                        assertEquals("Should have exactly one gtest_main library", 1, gtestMainCount);
                        
                        boolean hasSomeLib = args.stream()
                            .anyMatch(arg -> "some_lib".equals(arg.getValue()));
                        assertTrue("Should preserve some_lib", hasSomeLib);
                    }
                }
            }
        }
    }
    
    @Test
    public void testModifyTree_InsertionPosition_AfterLastTarget() throws Exception {
        String cmakeContent = """
            cmake_minimum_required(VERSION 3.10)
            project(MyTests)
            
            add_library(mylib src.cpp)
            add_executable(myapp main.cpp)
            """;
        
        CMakeParser parser = new CMakeParser(cmakeContent, false);
        CMakeRoot tree = parser.parse();
        
        GTestInstance gtest = new GTestInstance("path", "1.12.1", GTestInstance.SYSTEM_TEXT, "Системный");
        
        try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class)) {
            gtestUtilsMock.when(() -> GTestUtils.getGTestInstanceForProject(any()))
                         .thenReturn(gtest);
            
            when(mockCMakeProject.getProject()).thenReturn(mockProject);
            
            modifier.modifyTree(mockCMakeProject, tree);
            
            List<CMakeNode> children = tree.getChildren();
            int findPackageIndex = -1;
            int addExecutableIndex = -1;
            
            for (int i = 0; i < children.size(); i++) {
                CMakeNode child = children.get(i);
                if (child instanceof CommandNode cmd && "find_package".equals(cmd.getName())) {
                    List<ArgumentNode> args = cmd.getArgs();
                    if (args.size() >= 2 && 
                        "GTest".equals(args.get(0).getValue()) && 
                        "REQUIRED".equals(args.get(1).getValue())) {
                        findPackageIndex = i;
                    }
                } else if (child instanceof CommandNode cmd && "add_executable".equals(cmd.getName()) &&
                          "myapp".equals(cmd.getArgs().get(0).getValue())) {
                    addExecutableIndex = i;
                }
            }
            
            assertTrue("GTest section should be after the last target", 
                      findPackageIndex > addExecutableIndex);
        }
    }
}