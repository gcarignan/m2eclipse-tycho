package com.netappsid.m2e.pde.target.internals;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.pde.internal.core.target.DirectoryBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.core.target.provisional.ITargetHandle;
import org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService;
import org.eclipse.pde.internal.core.target.provisional.LoadTargetDefinitionJob;
import org.eclipse.swt.widgets.Shell;

public class MavenPDETarget
{

	private final ITargetPlatformService targetPlatformService;

	public MavenPDETarget(ITargetPlatformService targetPlatformService)
	{
		this.targetPlatformService = targetPlatformService;
	}

	public ITargetDefinition loadMavenTargetDefinition()
	{
		ITargetDefinition newTarget = targetPlatformService.newTarget();
		IMaven maven = MavenPlugin.getMaven();
		IMavenProjectFacade[] projects = MavenPluginActivator.getDefault().getMavenProjectManagerImpl().getProjects();
		newTarget.setBundleContainers(new IBundleContainer[] { new MavenBundleContainer(maven, projects) });
		newTarget.setName("Maven Target");
		LoadTargetDefinitionJob.load(newTarget);

		return newTarget;
	}

	public void saveMavenTargetDefinition(Shell shell, IStructuredSelection selection)
	{
		List<IProject> elements = Collections.checkedList(selection.toList(), IProject.class);

		if (!elements.isEmpty())
		{
			try
			{
				ITargetDefinition newTarget = loadMavenTargetDefinition();

				IFile file = elements.get(0).getFile("PDE_TARGET.target");

				if (file.exists())
				{
					file.delete(true, false, null);
				}

				IFolder folder = elements.get(0).getFolder("PDE_TARGET_Plugins");
				if (folder.exists())
				{
					folder.delete(true, null);
				}

				folder.create(true, true, null);
				File targetFolderPlugin = folder.getLocation().toFile();

				ITargetHandle targetHandle = targetPlatformService.getTarget(file);
				ITargetDefinition directoryTargetDefinition = targetHandle.getTargetDefinition();

				MavenBundleContainer mavenBundleContainer = (MavenBundleContainer) newTarget.getBundleContainers()[0];

				for (Artifact artifact : mavenBundleContainer.getArtifacts(null))
				{
					try
					{
						File bundleFile = artifact.getFile().getAbsoluteFile();
						if (bundleFile.isFile())
						{
							FileUtils.copyFileToDirectory(bundleFile, targetFolderPlugin);
						}
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}

				directoryTargetDefinition.setBundleContainers(new IBundleContainer[] { new DirectoryBundleContainer(targetFolderPlugin.getAbsolutePath()) });

				elements.get(0).refreshLocal(IResource.DEPTH_INFINITE, null);
				targetPlatformService.saveTargetDefinition(directoryTargetDefinition);
			}
			catch (CoreException e)
			{
				e.printStackTrace();
			}

		}
	}
}
