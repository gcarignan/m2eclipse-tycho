package com.netappsid.m2e.pde.target.internals;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService;
import org.eclipse.pde.internal.core.target.provisional.LoadTargetDefinitionJob;

public class MavenPDETarget {
	
	private final ITargetPlatformService targetPlatformService;

	public MavenPDETarget(ITargetPlatformService targetPlatformService) {
		this.targetPlatformService = targetPlatformService;
	}
	
	public void loadMavenTargetDefinition()
	{
		ITargetDefinition newTarget = targetPlatformService.newTarget();
		IMaven maven = MavenPlugin.getMaven();
		IMavenProjectFacade[] projects = MavenPluginActivator.getDefault().getMavenProjectManagerImpl().getProjects();
		newTarget.setBundleContainers(new IBundleContainer[]{new MavenBundleContainer(maven,projects)});
		newTarget.setName("Maven Target");
		LoadTargetDefinitionJob.load(newTarget);
	}
	
}
