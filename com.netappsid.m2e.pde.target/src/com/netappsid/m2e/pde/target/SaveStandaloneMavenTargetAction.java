package com.netappsid.m2e.pde.target;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2e.core.project.IMavenProjectFacade;

public class SaveStandaloneMavenTargetAction extends SaveMavenTargetAction
{
	@Override
	protected IMavenProjectFacade[] getOpenedProjectsOnly(IMavenProjectFacade[] projects)
	{
		List<IProject> elements = Collections.checkedList(((IStructuredSelection) getSelection()).toList(), IProject.class);

		IMavenProjectFacade selectedMavenProject = null;
		IProject selectedProject = elements.get(0);

		for (IMavenProjectFacade facade : projects)
		{
			if (facade.getProject() == selectedProject)
			{
				selectedMavenProject = facade;
				break;
			}
		}

		IMavenProjectFacade[] singleSelection;

		if (selectedMavenProject == null)
		{
			singleSelection = new IMavenProjectFacade[] {};
		}
		else
		{
			singleSelection = new IMavenProjectFacade[] { selectedMavenProject };
		}

		return singleSelection;
	}
}