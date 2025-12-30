package su.softcom.cldt.testing.gtest.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Resource;

import su.softcom.cldt.testing.gtest.ui.models.FailedNode;
import su.softcom.cldt.testing.gtest.ui.models.GTestExecutableNode;
import su.softcom.cldt.testing.gtest.ui.models.LoadingNode;
import su.softcom.cldt.testing.gtest.ui.models.TestCaseNode;
import su.softcom.cldt.testing.gtest.ui.models.TestRoot;
import su.softcom.cldt.testing.gtest.ui.models.TestSuiteNode;

/**
 * Класс GTestLabelProvider.
 */
public class GTestLabelProvider extends LabelProvider {
	
	// <a href="https://www.flaticon.com/ru/free-icons/" title="фляга иконки">Фляга иконки от Freepik - Flaticon</a>
	
	private Map<ImageDescriptor, Image> iconImageMap = new HashMap<>();
	
	@Override
	public void dispose() {
		iconImageMap.values().stream().forEach(Resource::dispose);
	}
	
	@Override
	public Image getImage(Object element) {

		if (element instanceof TestRoot) {
			return getIconImage(GTestImages.ROOT_ICON);
		}
		if (element instanceof LoadingNode) {
			return getIconImage(GTestImages.LOADING_ICON);
		}
		if (element instanceof FailedNode) {
			return getIconImage(GTestImages.FAILED_ICON);
		}
		if (element instanceof GTestExecutableNode) {
			return getIconImage(GTestImages.EXE_ICON);
		}
		if (element instanceof TestSuiteNode) {
			return getIconImage(GTestImages.SUITE_ICON);
		}
		if (element instanceof TestCaseNode) {
			return getIconImage(GTestImages.TEST_ICON);
		}

		return null;
	}

	private Image getIconImage(ImageDescriptor desc) {
		return iconImageMap.computeIfAbsent(desc, ImageDescriptor::createImage);
	}
}