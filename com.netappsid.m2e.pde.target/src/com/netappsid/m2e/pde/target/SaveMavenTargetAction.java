package com.netappsid.m2e.pde.target;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;

import com.netappsid.m2e.pde.target.internals.MavenBundleContainer;
import com.netappsid.m2e.pde.target.internals.MavenPDETarget;

public class SaveMavenTargetAction extends LoadMavenTargetAction
{
	@Override
	protected void run(MavenPDETarget mavenPDETarget, MavenBundleContainer mavenBundleContainer)
	{
		List<IProject> elements = Collections.checkedList(((IStructuredSelection) getSelection()).toList(), IProject.class);
		ITargetDefinition newTarget = mavenPDETarget.saveMavenTargetDefinition(getShell(), elements.get(0), mavenBundleContainer);
	}
}
