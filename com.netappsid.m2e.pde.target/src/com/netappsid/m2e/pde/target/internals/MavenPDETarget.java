package com.netappsid.m2e.pde.target.internals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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
				IProject project = elements.get(0);

				ITargetDefinition newTarget = loadMavenTargetDefinition();

				IFile targetFile = project.getFile("PDE_TARGET.target");

				// Ensure plugins folder is reset
				IFolder pluginsFolder = project.getFolder("PDE_TARGET_Plugins");
				if (pluginsFolder.exists())
				{
					pluginsFolder.delete(true, null);
				}
				pluginsFolder.create(true, true, null);

				// Ensure otherPlugins folder exists
				IFolder otherPluginsFolder = project.getFolder("PDE_TARGET_OtherPlugins");
				if (!otherPluginsFolder.exists())
				{
					otherPluginsFolder.create(true, true, null);
				}

				File pluginsFolderFile = pluginsFolder.getLocation().toFile();

				// Copy all maven dependencies in pluginsFolder
				MavenBundleContainer mavenBundleContainer = (MavenBundleContainer) newTarget.getBundleContainers()[0];

				for (Artifact artifact : mavenBundleContainer.getArtifacts(null))
				{
					try
					{
						File bundleFile = artifact.getFile().getAbsoluteFile();
						if (bundleFile.isFile())
						{
							FileUtils.copyFileToDirectory(bundleFile, pluginsFolderFile);
						}
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}

				// Update targetFile with the DirectoryBundelContainer if not already present
				ITargetHandle targetHandle = targetPlatformService.getTarget(targetFile);
				ITargetDefinition directoryTargetDefinition = targetHandle.getTargetDefinition();

				IBundleContainer mavenDirectoryBundleContainer = null;

				String pluginsFolderPath = pluginsFolderFile.getAbsolutePath();
				String otherPluginsPath = otherPluginsFolder.getLocation().toFile().getAbsolutePath();
				
				if (targetFile.exists())
				{
					// Find already existing DirectoryBundleContainer for maven repository path
					addBundleContainerToTargetDefinitionIfNotPresent(directoryTargetDefinition, pluginsFolderPath);
					addBundleContainerToTargetDefinitionIfNotPresent(directoryTargetDefinition, otherPluginsPath);
				}
				else
				{
					directoryTargetDefinition.setBundleContainers(new IBundleContainer[] { new DirectoryBundleContainer(pluginsFolderPath),
							new DirectoryBundleContainer(otherPluginsPath) });
				}
				

				// Refresh project folder tree
				project.refreshLocal(IResource.DEPTH_INFINITE, null);

				// Save target definition
				targetPlatformService.saveTargetDefinition(directoryTargetDefinition);
			}
			catch (CoreException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void addBundleContainerToTargetDefinitionIfNotPresent(ITargetDefinition directoryTargetDefinition, String folderPath) throws CoreException
	{
		IBundleContainer bundleContainer = findBundleContainerForPath(directoryTargetDefinition, folderPath);

		// Add BundleContainer to target definition if not already present
		if (bundleContainer == null)
		{
			IBundleContainer[] bundleContainers = directoryTargetDefinition.getBundleContainers();

			IBundleContainer[] newBundleContainers = Arrays.copyOf(bundleContainers, bundleContainers.length + 1);

			newBundleContainers[newBundleContainers.length - 1] = new DirectoryBundleContainer(folderPath);
			directoryTargetDefinition.setBundleContainers(newBundleContainers);
		}
	}

	private IBundleContainer findBundleContainerForPath(ITargetDefinition directoryTargetDefinition, String pluginsFolderPath) throws CoreException
	{
		IBundleContainer returnedBundleContainer = null;

		for (IBundleContainer bundleContainer : directoryTargetDefinition.getBundleContainers())
		{
			if (bundleContainer instanceof DirectoryBundleContainer)
			{
				DirectoryBundleContainer directoryBundleContainer = (DirectoryBundleContainer) bundleContainer;

				if (directoryBundleContainer.getLocation(false).equals(pluginsFolderPath))
				{
					returnedBundleContainer = bundleContainer;
					break;
				}
			}
		}

		return returnedBundleContainer;
	}
}
