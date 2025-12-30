package su.softcom.cldt.testing.tests.internal.core;

import su.softcom.cldt.core.cmake.ArgumentNode;
import su.softcom.cldt.core.cmake.CMakeNode;
import su.softcom.cldt.core.cmake.CMakeParser;
import su.softcom.cldt.core.cmake.CMakeRoot;
import su.softcom.cldt.core.cmake.CommandNode;
import su.softcom.cldt.testing.gtest.internal.core.GTestWizardTreeModifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GTestWizardTreeModifierTest {
    
    @Test
    public void testModifyCMakeTree_WhenTestTargetExists() throws Exception {
        String cmakeContent = """
            cmake_minimum_required(VERSION 3.10)
            project(MyTests)
            
            add_executable(my_tests test1.cpp test2.cpp)
            """;
        
        CMakeParser parser = new CMakeParser(cmakeContent, false);
        CMakeRoot root = parser.parse();
        
        String testFileName = "test3.cpp";
        
        GTestWizardTreeModifier.modifyCMakeTree(root, testFileName);
        
        boolean foundFileInTarget = false;
        for (CMakeNode child : root.getChildren()) {
            if (child instanceof CommandNode cmd && "add_executable".equals(cmd.getName())) {
                List<ArgumentNode> args = cmd.getArgs();
                if (!args.isEmpty() && "my_tests".equals(args.get(0).getValue())) {
                    foundFileInTarget = args.stream()
                        .anyMatch(arg -> "test3.cpp".equals(arg.getValue()));
                    break;
                }
            }
        }
        
        assertTrue("Should add test file to existing target", foundFileInTarget);
    }
    
    @Test
    public void testModifyCMakeTree_WhenNoTestTarget() throws Exception {
        String cmakeContent = """
            cmake_minimum_required(VERSION 3.10)
            project(MyTests)
            
            add_library(mylib src.cpp)
            """;
        
        CMakeParser parser = new CMakeParser(cmakeContent, false);
        CMakeRoot root = parser.parse();
        
        String testFileName = "test.cpp";
        int initialChildrenCount = root.getChildren().size();
        
        GTestWizardTreeModifier.modifyCMakeTree(root, testFileName);
        
        List<CMakeNode> children = root.getChildren();
        assertEquals("Should add 3 new nodes", initialChildrenCount + 3, children.size());
        
        boolean foundAddExecutable = false;
        boolean foundInclude = false;
        boolean foundGtestDiscover = false;
        
        for (CMakeNode child : children) {
            if (child instanceof CommandNode cmd) {
                if ("add_executable".equals(cmd.getName()) && 
                    "${PROJECT_NAME}.tests".equals(cmd.getArgs().get(0).getValue())) {
                    foundAddExecutable = true;
                } else if ("include".equals(cmd.getName()) && 
                          "GoogleTest".equals(cmd.getArgs().get(0).getValue())) {
                    foundInclude = true;
                } else if ("gtest_discover_tests".equals(cmd.getName()) && 
                          "${PROJECT_NAME}.tests".equals(cmd.getArgs().get(0).getValue())) {
                    foundGtestDiscover = true;
                }
            }
        }
        
        assertTrue("Should create test target", foundAddExecutable);
        assertTrue("Should include GoogleTest", foundInclude);
        assertTrue("Should add gtest_discover_tests", foundGtestDiscover);
    }
    
    @Test
    public void testModifyCMakeTree_FileAlreadyInTarget() throws Exception {
        String cmakeContent = """
            cmake_minimum_required(VERSION 3.10)
            project(MyTests)
            
            add_executable(existing_tests test.cpp other.cpp)
            """;
        
        CMakeParser parser = new CMakeParser(cmakeContent, false);
        CMakeRoot root = parser.parse();
        
        String testFileName = "test.cpp";
        
        int initialArgsCount = -1;
        for (CMakeNode child : root.getChildren()) {
            if (child instanceof CommandNode cmd && "add_executable".equals(cmd.getName())) {
                initialArgsCount = cmd.getArgs().size();
                break;
            }
        }
        
        GTestWizardTreeModifier.modifyCMakeTree(root, testFileName);
        
        for (CMakeNode child : root.getChildren()) {
            if (child instanceof CommandNode cmd && "add_executable".equals(cmd.getName())) {
                List<ArgumentNode> args = cmd.getArgs();
                assertEquals("Should not add duplicate file", 
                            initialArgsCount, args.size());
                
                long testFileCount = args.stream()
                    .filter(arg -> "test.cpp".equals(arg.getValue()))
                    .count();
                assertEquals("File should appear only once", 1, testFileCount);
                break;
            }
        }
    }
}