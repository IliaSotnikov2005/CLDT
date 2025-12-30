package su.softcom.cldt.testing.gtest.ui.wizards;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import su.softcom.cldt.internal.core.builders.CMakeModifier;
import su.softcom.cldt.testing.gtest.core.GTestConstants;
import su.softcom.cldt.testing.gtest.core.GTestUtils;

/**
 * Абстрактный класс для тестового визарда.
 */
public abstract class AbstractTestWizard extends Wizard implements INewWizard {
	protected IProject project;
	protected VelocityEngine velocityEngine;
	protected VelocityContext context;
	protected File templatesFolder;
	protected IFolder testsFolder;
	protected String testFileName = null;

	protected AbstractTestWizard() {
		initVelocity();
	}

	private void initVelocity() {
		try {
			velocityEngine = new VelocityEngine();
			context = new VelocityContext();

			Bundle bundle = FrameworkUtil.getBundle(getClass());
			if (bundle != null) {
				URL templatesUrl = FileLocator.toFileURL(bundle.getEntry("resources/templates/"));
				templatesFolder = new File(templatesUrl.toURI());

				velocityEngine.setProperty("file.resource.loader.path", templatesFolder.getAbsolutePath());
				velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
				velocityEngine.setProperty(RuntimeConstants.INPUT_ENCODING, StandardCharsets.UTF_8.name());

				velocityEngine.init();
			}
		} catch (IOException | URISyntaxException e) {
			Platform.getLog(getClass()).error(e.getMessage());
		}

	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		Object firstElement = selection.getFirstElement();

		if (firstElement instanceof IAdaptable adaptable) {
			IResource selectedResource = adaptable.getAdapter(IResource.class);
			if (selectedResource != null) {
				project = selectedResource.getProject();

				if (project != null) {
					IEclipsePreferences preferences = new ProjectScope(project).getNode(GTestConstants.GTEST_NODE);
					String testsFolderName = preferences.get(GTestConstants.TESTS_FOLDER_KEY,
							GTestConstants.DEFAULT_TESTS_FOLDER_NAME);
					testsFolder = project.getFolder(testsFolderName);
				}
			}
		}
	}

	@SuppressWarnings("restriction")
	public boolean performFinish(String testFileName) {
		try {
			if (testFileName != null && !testFileName.isEmpty()) {
				GTestUtils.modifyTestsCMakeLists(testsFolder, testFileName);
			}

			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, CMakeModifier.ID, null,
					new NullProgressMonitor());
			Platform.getLog(getClass()).info("Обновили CMakeLists.txt прокта после создания теста.");
			return true;
		} catch (CoreException e) {
			Platform.getLog(getClass()).error(e.getMessage());
			return false;
		}
	}

	protected void generateTestFile(String templateName, String fileName, IFolder folder) {
		testFileName = fileName;
		Template template = velocityEngine.getTemplate(templateName);

		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		String fileContent = writer.toString();

		try {
			IFile file = folder.getFile(fileName);

			if (!file.exists()) {
				ByteArrayInputStream inputStream = new ByteArrayInputStream(
						fileContent.getBytes(StandardCharsets.UTF_8));

				file.create(inputStream, IResource.FORCE, new NullProgressMonitor());
			}

			folder.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			Platform.getLog(getClass()).error(e.getMessage());
		}
	}
}
