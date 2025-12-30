package su.softcom.cldt.testing.gtest.ui;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

import com.fasterxml.jackson.core.JsonProcessingException;

import su.softcom.cldt.core.ILibrary;
import su.softcom.cldt.testing.gtest.core.GTestConstants;
import su.softcom.cldt.testing.gtest.core.model.GTestInstance;

public final class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "su.softcom.cldt.testing.gtest.ui";

	private ServiceTracker<ILibrary, ILibrary> tracker;
	private static ILibrary lib;

	private static Activator plugin;
	private static BundleContext context;

	@Override
	public void start(BundleContext ctx) throws Exception {
		super.start(ctx);
		plugin = this;
		context = ctx;

		try {
			Filter f = ctx.createFilter("(&(objectClass=" + ILibrary.class.getName() + ")(library.id=gtest))");
			tracker = new ServiceTracker<>(ctx, f, null);
			tracker.open();
			lib = tracker.getService();
			if (lib == null) {
				throw new CoreException(Status.error("GTest library service is not available."));
			}
		} catch (InvalidSyntaxException e) {
			Platform.getLog(getBundle()).error(e.getMessage());
		}

		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(GTestConstants.GTEST_NODE);
		setupGTestInstanses(preferences);
		setupTestsFolder(preferences);
		preferences.flush();

		GTestRefreshListener.init();
	}

	@Override
	public void stop(BundleContext ctx) throws Exception {
		plugin = null;
		context = null;
		if (tracker != null)
			tracker.close();
		lib = null;
		super.stop(ctx);
	}

	private void setupGTestInstanses(IEclipsePreferences preferences) {
		String currentInstances = preferences.get(GTestConstants.GTEST_INSTANCES_KEY, "");
		if (currentInstances.isEmpty() || currentInstances.equals("[]")) {
			List<GTestInstance> instances = GTestInstancesManager.getRefreshedInstanses();
			try {
				preferences.put(GTestConstants.GTEST_INSTANCES_KEY, GTestInstance.listToJson(instances));
			} catch (JsonProcessingException e) {
				Platform.getLog(getClass()).error(e.getMessage());
			}
		}
	}

	private void setupTestsFolder(IEclipsePreferences preferences) {
		String testsFolder = preferences.get(GTestConstants.TESTS_FOLDER_KEY, "");
		if (testsFolder.isEmpty()) {
			preferences.put(GTestConstants.TESTS_FOLDER_KEY, GTestConstants.DEFAULT_TESTS_FOLDER_NAME);
		}
	}

	public static Activator getDefault() {
		return plugin;
	}

	public static BundleContext getContext() {
		return context;
	}

	public static ILibrary getLibrary() {
		return lib;
	}
}
