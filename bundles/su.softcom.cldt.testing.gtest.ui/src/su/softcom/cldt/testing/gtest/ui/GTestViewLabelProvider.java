package su.softcom.cldt.testing.gtest.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Resource;

import su.softcom.cldt.testing.gtest.ui.models.GTestResult;
import su.softcom.cldt.testing.gtest.ui.models.TestInfo;
import su.softcom.cldt.testing.gtest.ui.models.TestSuite;

/**
 * Поставщик меток для панели результатов тестирования GTest.
 */
public class GTestViewLabelProvider extends LabelProvider {

	private Map<ImageDescriptor, Image> iconImageMap = new HashMap<>();

	@Override
	public String getText(Object element) {
		if (element instanceof GTestResult result) {
			return String.format("%s (%d тестов) - %s", result.name(), result.tests(), formatTime(result.time()));
		} else if (element instanceof TestSuite suite) {
			return String.format("%s (%d тестов) - %s", suite.name(), suite.tests(), formatTime(suite.time()));
		} else if (element instanceof TestInfo testCase) {
			String name = testCase.name();
			if (testCase.isFailed()) {
				name += " [ПРОВАЛЕН]";
			} else if (testCase.isSkipped()) {
				name += " [ПРОПУЩЕН]";
			}
			return String.format("%s - %s", name, formatTime(testCase.time()));
		}
		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof TestSuite) {
			return getIconImage(GTestImages.SUITE_ICON);
		} else if (element instanceof TestInfo testCase) {
			if (testCase.isFailed()) {
				return getIconImage(GTestImages.FAILED_TEST_ICON);
			} else if (testCase.isSkipped()) {
				return getIconImage(GTestImages.SKIPPED_ICON);
			} else if (testCase.isDisabled()) {
				return getIconImage(GTestImages.DISABLED_ICON);
			}

			return getIconImage(GTestImages.PASSED_ICON);
		}

		return null;
	}

	@Override
	public void dispose() {
		iconImageMap.values().stream().forEach(Resource::dispose);
	}

	private String formatTime(String time) {
		if (time == null || time.isEmpty()) {
			return "";
		}

		try {
			String timeStr = time.replace("s", "").trim();
			float seconds = Float.parseFloat(timeStr);

			return String.format("%.3f с", seconds);
		} catch (NumberFormatException e) {
			return time;
		}
	}

	private Image getIconImage(ImageDescriptor desc) {
		return iconImageMap.computeIfAbsent(desc, ImageDescriptor::createImage);
	}
}