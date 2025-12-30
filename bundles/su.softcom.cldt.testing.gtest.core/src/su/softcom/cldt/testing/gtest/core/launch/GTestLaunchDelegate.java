package su.softcom.cldt.testing.gtest.core.launch;

import static su.softcom.cldt.testing.gtest.core.launch.GTestLaunchAttr.ATTR_SELECTED_TESTS;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import su.softcom.cldt.debug.core.launch.AbstractLaunchDelegate;

public class GTestLaunchDelegate extends AbstractLaunchDelegate {

	public static final String ID = "su.softcom.cldt.testing.gtest.core.launchConfigurationType";

	@Override
	protected List<String> getArgs(ILaunchConfiguration cfg) throws CoreException {
		List<String> cmd = new ArrayList<>();
		List<String> filter = cfg.getAttribute(ATTR_SELECTED_TESTS, Collections.emptyList());
		if (!filter.isEmpty()) {
			cmd.add("--gtest_filter=" + filter.stream().collect(Collectors.joining(":")));
		}
		
		cmd.add("--gtest_color=no");
		
		String additionalArgs = cfg.getAttribute(GTestLaunchAttr.ATTR_ARGS, "");
	    if (!additionalArgs.isEmpty()) {
	        String[] args = additionalArgs.split("\\s+");
	        for (String arg : args) {
	            if (!arg.trim().isEmpty()) {
	                cmd.add(arg.trim());
	            }
	        }
	    }
	    
		return cmd;
	}

	@Override
	public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) {
		return false;
	}

	@Override
	public boolean finalLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor)
			throws CoreException {
		return true;
	}

	@Override
	public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor)
			throws CoreException {
		return true;
	}

	@Override
	protected void attachToDebugTarget(ILaunch launch, ILaunchConfiguration cfg, String dbgId, InetSocketAddress addr,
			long pid, File exe, List<String> args) throws CoreException {
		// no DEBUG
	}

	@Override
	protected String debuggerId(ILaunchConfiguration cfg) {
		// no DEBUG
		return null;
	}

	@Override
	protected File getExecutable(ILaunchConfiguration cfg) {
		try {
			String exeStr = cfg.getAttribute(GTestLaunchAttr.ATTR_EXECUTABLE, "").trim();
			if (!exeStr.isBlank()) {
				return new File(exeStr);
			}
		} catch (CoreException e) {
			error("Не удалось получить исполняемый файл", e);
		}
		return null;
	}

}
