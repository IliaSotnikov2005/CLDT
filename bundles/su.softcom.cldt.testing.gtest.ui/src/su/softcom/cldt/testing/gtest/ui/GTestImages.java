package su.softcom.cldt.testing.gtest.ui;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.osgi.framework.Bundle;

/**
 * Вспомогательный класс, управляющий иконками для GTest.
 */
public class GTestImages {

	private static final IPath ICONS_PATH = new Path("icons"); //$NON-NLS-1$ //$NON-NLS-2$

	public static final ImageDescriptor GOOGLETEST_ICON = createUnManaged("", "google-icon-16.png");
	public static final ImageDescriptor LOADING_ICON = createUnManaged("", "loading.png"); //$NON-NLS-1$
	public static final ImageDescriptor FAILED_ICON = createUnManaged("", "failed.png"); //$NON-NLS-1$
	public static final ImageDescriptor ROOT_ICON = createUnManaged("", "testRoot.png");
	public static final ImageDescriptor EXE_ICON = createUnManaged("", "testExe.png");
	public static final ImageDescriptor SUITE_ICON = createUnManaged("", "testSuite.png");
	public static final ImageDescriptor TEST_ICON = createUnManaged("", "test.png");
	public static final ImageDescriptor PASSED_ICON = createUnManaged("", "passed.png");
	public static final ImageDescriptor FAILED_TEST_ICON = createUnManaged("", "failed2.png");
	public static final ImageDescriptor DISABLED_ICON = createUnManaged("", "disabled.png");
	public static final ImageDescriptor SKIPPED_ICON = createUnManaged("", "skipped.png");
	public static final ImageDescriptor SHOW_FAILED_ICON = createUnManaged("", "failedAct.png");
	public static final ImageDescriptor SHOW_SKIPPED_ICON = createUnManaged("", "skippedAct.png");
	public static final ImageDescriptor SHOW_DISABLED_ICON = createUnManaged("", "disabledAct.png");

	private static ImageDescriptor create(String prefix, String name, boolean useMissingImageDescriptor) {
		IPath path = ICONS_PATH.append(prefix).append(name);
		return createImageDescriptor(Activator.getDefault().getBundle(), path, useMissingImageDescriptor);
	}

	private static ImageDescriptor createUnManaged(String prefix, String name) {
		return create(prefix, name, true);
	}

	/**
	 * Создаёт дескриптор изображения.
	 * @param bundle бандл
	 * @param path путь к изображению относительно бандла
	 * @param useMissingImageDescriptor
	 * @return Дескриптор изображения, null при неудаче
	 */
	public static ImageDescriptor createImageDescriptor(Bundle bundle, IPath path, boolean useMissingImageDescriptor) {
		IPath uriPath = new Path("/plugin").append(bundle.getSymbolicName()).append(path); //$NON-NLS-1$
		URL url = null;
		try {
			URI uri = new URI("platform", null, uriPath.toString(), null); //$NON-NLS-1$
			url = uri.toURL();
		} catch (MalformedURLException | URISyntaxException e) {
			// no image
		}
		URL foundUrl = FileLocator.find(url);
		if (foundUrl != null) {
			return ImageDescriptor.createFromURL(url);
		}
		if (useMissingImageDescriptor) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
		return null;
	}

	private GTestImages() {
		// utility class
	}

}