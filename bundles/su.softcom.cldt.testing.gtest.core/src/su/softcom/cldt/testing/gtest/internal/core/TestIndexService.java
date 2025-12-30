package su.softcom.cldt.testing.gtest.internal.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.component.annotations.Component;

import su.softcom.cldt.core.CMakeCorePlugin;
import su.softcom.cldt.core.cmake.ICMakeProject;
import su.softcom.cldt.testing.ctest.CTestJob;
import su.softcom.cldt.testing.ctest.CTestRunner.CTestResult;
import su.softcom.cldt.testing.gtest.core.Activator;
import su.softcom.cldt.testing.gtest.core.GTestConstants;
import su.softcom.cldt.testing.gtest.core.ITestIndexService;
import su.softcom.cldt.testing.gtest.core.model.CTestItem;
import su.softcom.cldt.testing.gtest.core.model.GTestExecutable;
import su.softcom.cldt.testing.gtest.core.model.TestExecutable;
import su.softcom.cldt.testing.gtest.core.model.TestIndexSnapshot;

@Component(service = ITestIndexService.class)
public class TestIndexService implements ITestIndexService {

	private static final class ExecResult {
		final int exitCode;
		final String stdout;

		ExecResult(int code, String out) {
			this.exitCode = code;
			this.stdout = out;
		}
	}

	private final Map<Key, TestIndexSnapshot> cache = new ConcurrentHashMap<>();

	@Override
	public TestIndexSnapshot getSnapshot(IProject project, String config) {
		return cache.getOrDefault(Key.of(project, config),
				new TestIndexSnapshot(project, nvl(config), List.of(), List.of()));
	}

	@Override
	public void refreshAsync(IProject project, String config) {
		Job job = new Job("CTest (" + project.getName() + "/" + nvl(config) + ")") {
			@Override
			protected IStatus run(IProgressMonitor m) {
				try {
					refreshNow(project, config, m);
					return Status.OK_STATUS;
				} catch (CoreException e) {
					return e.getStatus();
				} catch (Exception e) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "CTest ошибка: " + e.getMessage(), e);
				}
			}
		};

		job.setRule(project);
		job.setUser(false);
		job.schedule();
	}

	@Override
	public TestIndexSnapshot refreshNow(IProject project, String config, IProgressMonitor monitor)
			throws CoreException {

		monitor = monitor != null ? monitor : new NullProgressMonitor();

		ICMakeProject cmakePrj = CMakeCorePlugin.getDefault().getProject(project);
		IPath buildDir = cmakePrj.getBuildFolder().getLocation();

		if (!buildDir.toFile().exists()) {
	        return new TestIndexSnapshot(project, config, new ArrayList<>(), new ArrayList<>());
	    }
		
		IEclipsePreferences preferences = new ProjectScope(project).getNode("su.softcom.cldt.testing.gtest");
	    String testsFolderName = preferences.get(GTestConstants.TESTS_FOLDER_KEY, GTestConstants.DEFAULT_TESTS_FOLDER_NAME);
	    IPath testsBuildDir = buildDir.append(testsFolderName);
		
	    if (!testsBuildDir.toFile().exists()) {
	        return new TestIndexSnapshot(project, config, new ArrayList<>(), new ArrayList<>());
	    }
	    
	    Platform.getLog(getClass()).info("Поиск тестов в папке: " + testsBuildDir);
	    
		CTestResult execResult = CTestJob.runNow(testsBuildDir, createCTestArgs(config), monitor);

		TestIndexSnapshot snap = processCTestResult(project, config, execResult, monitor);

		cache.put(Key.of(project, config), snap);
		return snap;
	}

	private ExecResult execTest(List<String> cmd, IPath workDir, IProgressMonitor m) throws CoreException {
		Process p = null;
		try {
			p = new ProcessBuilder(cmd).directory(workDir.toFile()).redirectErrorStream(true).start();
			StringBuilder out = new StringBuilder();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line;
				while ((line = br.readLine()) != null) {
					if (m.isCanceled())
						break;
					out.append(line).append('\n');
				}
			}

			p.waitFor();
			if (p.exitValue() != 0) {
				throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
						cmd.get(0) + " exited with code " + p.exitValue()));
			}

			return new ExecResult(p.exitValue(), out.toString());
		} catch (CoreException e) {
			throw e;
		} catch (InterruptedException | IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Failed to run " + cmd.get(0) + ": " + e.getMessage(), e));
		} finally {
			if (p != null && p.isAlive())
				p.destroyForcibly();
		}
	}

	@Override
	public void clear(IProject project, String config) {
		cache.remove(Key.of(project, config));
	}

	private record Key(String project, String config) {
		static Key of(IProject p, String cfg) {
			return new Key(p.getName(), cfg == null ? "" : cfg);
		}
	}

	private static String nvl(String s) {
		return s == null ? "" : s;
	}

	private GTestExecutable listGTestItems(TestExecutable exe, IProgressMonitor m) throws CoreException {
		List<String> cmd = List.of(exe.exePath().toOSString(), "--gtest_list_tests", "--gtest_color=no");
		ExecResult result = execTest(cmd, exe.workDir(), m);
		if (result.exitCode != 0) {
			return null;
		}

		return GTestParserUtils.parseGTestList(exe.exePath(), result.stdout);
	}

	private boolean isTestActive(TestExecutable test, IProject project) {
		File exeFile = test.exePath().toFile();

		if (exeFile.exists()) {
			long exeLastModified = exeFile.lastModified();
			long cmakeCacheModified = getCMakeCacheLastModified(project);

			if (cmakeCacheModified > exeLastModified) {
				Platform.getLog(getClass())
						.info("Test executable is outdated (CMake reconfigured)" + test.exePath().toString());
				Platform.getLog(getClass()).info("Test executable deleted: " + test.exePath().toString());
				exeFile.delete();
				return false;
			}

			return true;
		}

		return false;
	}

	private long getCMakeCacheLastModified(IProject project) {
		ICMakeProject cmakeProject = CMakeCorePlugin.getDefault().getProject(project);
		IPath buildDir = cmakeProject.getBuildFolder().getLocation();
		File cmakeCache = buildDir.append("CMakeCache.txt").toFile();

		if (cmakeCache.exists()) {
			return cmakeCache.lastModified();
		}

		return 0;
	}

	private List<String> createCTestArgs(String config) {
		List<String> args = new ArrayList<>(List.of("--show-only=json-v1"));
		if (config != null && !config.isBlank()) {
			args.add("-C");
			args.add(config);
		}
		
		return args;
	}

	private TestIndexSnapshot processCTestResult(IProject project, String config, CTestResult result,
			IProgressMonitor monitor) throws CoreException {
		List<CTestItem> items = GTestParserUtils.parseCTestJson(result.stdout());
		var execs = items.stream().map(CTestItem::executable)
				.collect(
						Collectors.toMap(TestExecutable::exePath, Function.identity(), (a, b) -> a, LinkedHashMap::new))
				.values();
		List<GTestExecutable> gtestListsExe = new ArrayList<>();
		for (TestExecutable exe : execs) {
			if (monitor.isCanceled()) {
				break;
			}
			
			if (isTestActive(exe, project)) {
				gtestListsExe.add(listGTestItems(exe, monitor));
			}
		}

		return new TestIndexSnapshot(project, config, items, gtestListsExe);
	}
}
