package su.softcom.cldt.testing.tests.core;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import su.softcom.cldt.core.cmake.CMakeRoot;
import su.softcom.cldt.testing.gtest.core.GTestConstants;
import su.softcom.cldt.testing.gtest.core.GTestUtils;
import su.softcom.cldt.testing.gtest.internal.core.GTestWizardTreeModifier;

@RunWith(MockitoJUnitRunner.class)
public class GTestUtilsTest {

    @Mock
    private IProject mockProject;
    
    @Mock
    private IFolder mockTestsFolder;
    
    @Mock
    private IFile mockCMakeFile;
    
    @Mock
    private IEclipsePreferences mockPreferences;
    
    @Mock
    private IScopeContext mockProjectScope;
    
    @Test
    public void testCreateTestsFolderIfNotExists_WhenFolderExists() throws CoreException {
        String folderName = "tests";
        
        when(mockProject.getFolder(folderName)).thenReturn(mockTestsFolder);
        when(mockTestsFolder.exists()).thenReturn(true);
        
        IFolder result = GTestUtils.createTestsFolderIfNotExists(mockProject, folderName);
        
        assertEquals(mockTestsFolder, result);
        verify(mockTestsFolder, never()).create(anyBoolean(), anyBoolean(), any());
    }
    
    @Test
    public void testCreateTestsFolderIfNotExists_WhenFolderDoesNotExist() throws CoreException {
        String folderName = "tests";
        
        when(mockProject.getFolder(folderName)).thenReturn(mockTestsFolder);
        when(mockTestsFolder.exists()).thenReturn(false);
        when(mockTestsFolder.getFile("CMakeLists.txt")).thenReturn(mockCMakeFile);
        when(mockCMakeFile.exists()).thenReturn(false);
        
        try (MockedStatic<GTestUtils> gtestUtilsMock = mockStatic(GTestUtils.class, CALLS_REAL_METHODS)) {
            gtestUtilsMock.when(() -> GTestUtils.generateCMakeListsContent())
                         .thenReturn("cmake_minimum_required(VERSION 3.10)\nproject(TestProject)");
            
            IFolder result = GTestUtils.createTestsFolderIfNotExists(mockProject, folderName);
            
            assertEquals(mockTestsFolder, result);
            verify(mockTestsFolder).create(eq(true), eq(true), any(NullProgressMonitor.class));
            verify(mockCMakeFile).create(any(ByteArrayInputStream.class), eq(true), isNull());
        }
    }
    
    @Test
    public void testCreateTestsFolderIfNotExists_WhenProjectIsNull() {
        IFolder result = GTestUtils.createTestsFolderIfNotExists(null, "tests");
        
        assertNull(result);
    }
    
    @Test
    public void testCreateTestsFolderIfNotExists_WhenFolderNameIsEmpty() {
        IFolder result = GTestUtils.createTestsFolderIfNotExists(mockProject, "");
        
        assertNull(result);
    }
    
    @Test
    public void testCreateTestsFolderIfNotExists_WhenFolderNameIsBlank() {
        IFolder result = GTestUtils.createTestsFolderIfNotExists(mockProject, "   ");
        
        assertNull(result);
    }
    
    @Test
    public void testGenerateCMakeListsContent_ReturnsNotNull() {
        String result = GTestUtils.generateCMakeListsContent();
        
        assertNotNull(result);
    }
    
    @Test
    public void testModifyTestsCMakeLists() throws Exception {
        String testFileName = "test_example.cpp";
        String cmakeContent = "cmake_minimum_required(VERSION 3.10)\nproject(MyTests)";
        
        IFile mockCmakeFile = mock(IFile.class);
        when(mockTestsFolder.getFile(GTestConstants.CMAKELISTS)).thenReturn(mockCmakeFile);
        when(mockCmakeFile.exists()).thenReturn(true);
        
        when(mockCmakeFile.getContents()).thenReturn(
            new ByteArrayInputStream(cmakeContent.getBytes(StandardCharsets.UTF_8))
        );
        
        try (MockedStatic<GTestWizardTreeModifier> modifierMock = 
                mockStatic(GTestWizardTreeModifier.class)) {
            
            GTestUtils.modifyTestsCMakeLists(mockTestsFolder, testFileName);
            
            modifierMock.verify(() -> 
                GTestWizardTreeModifier.modifyCMakeTree(any(CMakeRoot.class), eq(testFileName))
            );
        }
    }
}