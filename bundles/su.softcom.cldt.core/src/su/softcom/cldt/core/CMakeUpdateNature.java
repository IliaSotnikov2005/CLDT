package su.softcom.cldt.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import su.softcom.cldt.internal.core.builders.CMakeModifier;

public class CMakeUpdateNature implements IProjectNature {

	public static final String ID = "su.softcom.cldt.core.cmakesync"; //$NON-NLS-1$

	IProject project;

	@Override
	public void configure() throws CoreException {
		IProjectDescription description = project.getDescription();
		List<String> natures = Arrays.asList(description.getNatureIds());
		if (!natures.contains(CMakeProjectNature.ID)) {
			List<String> newNatures = new ArrayList<String>();
			newNatures.addAll(natures);
			newNatures.add(CMakeProjectNature.ID);
			description.setNatureIds(newNatures.toArray(new String[newNatures.size()]));

		}
		ICommand buildCommand = description.newCommand();
		buildCommand.setBuilderName(CMakeModifier.ID);
		ICommand[] buildSpec = description.getBuildSpec();
		List<ICommand> list = new ArrayList<>(Arrays.asList(buildSpec));
		list.add(0, buildCommand);
		description.setBuildSpec(list.toArray(new ICommand[list.size()]));
		project.setDescription(description, new NullProgressMonitor());
	}

	@Override
	public void deconfigure() throws CoreException {
		IProjectDescription description = project.getDescription();
		List<ICommand> buildCommands = new ArrayList<>(Arrays.asList(description.getBuildSpec()));
		List<ICommand> toRemove = buildCommands.stream().filter(e -> e.getBuilderName().equals(CMakeModifier.ID))
				.toList();

		if (!toRemove.isEmpty()) {
			buildCommands.removeAll(toRemove);
		}

		description.setBuildSpec(buildCommands.toArray(new ICommand[buildCommands.size()]));
		project.setDescription(description, new NullProgressMonitor());
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}

}
