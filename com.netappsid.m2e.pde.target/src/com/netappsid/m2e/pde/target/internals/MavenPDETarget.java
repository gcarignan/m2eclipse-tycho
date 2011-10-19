package com.netappsid.m2e.pde.target.internals;

import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.internal.resources.File;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.core.target.provisional.ITargetHandle;
import org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService;
import org.eclipse.pde.internal.core.target.provisional.LoadTargetDefinitionJob;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class MavenPDETarget{
	
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
	
	public void saveMavenTargetDefinition(Shell shell)
	{
		org.eclipse.swt.widgets.FileDialog fileDialog =	new	org.eclipse.swt.widgets.FileDialog(shell, SWT.SAVE);
		fileDialog.setFilterExtensions(new String[]{".target"});
		
		String dialogResult = fileDialog.open();
		if(dialogResult != null)
		{
			try 
			{
				targetPlatformService.getTarget(new URI(dialogResult));
			} 
			catch (URISyntaxException e) 
			{
				e.printStackTrace();
			}			
		}
	}
}
