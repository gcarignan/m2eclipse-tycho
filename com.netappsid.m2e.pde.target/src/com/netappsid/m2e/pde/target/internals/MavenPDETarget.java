package com.netappsid.m2e.pde.target.internals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.pde.internal.core.target.DirectoryBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.core.target.provisional.ITargetHandle;
import org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService;
import org.eclipse.pde.internal.core.target.provisional.LoadTargetDefinitionJob;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public class MavenPDETarget
{

	private static final String MAVEN_TARGET = "Maven Target";
	public static final String PDE_TARGET_TARGET = "PDE_TARGET.target";
	public static final String PDE_TARGET_OTHER_PLUGINS = "PDE_TARGET_OtherPlugins";
	public static final String PDE_TARGET_PLUGINS = "PDE_TARGET_Plugins";
	private final ITargetPlatformService targetPlatformService;

	public MavenPDETarget(ITargetPlatformService targetPlatformService)
	{
		this.targetPlatformService = targetPlatformService;
	}

	public ITargetDefinition loadMavenTargetDefinition(MavenBundleContainer mavenBundleContainer)
	{
		ITargetDefinition newTarget = targetPlatformService.newTarget();
		newTarget.setBundleContainers(new IBundleContainer[] { mavenBundleContainer });
		newTarget.setName(MAVEN_TARGET);
		return newTarget;
	}

	public ITargetDefinition saveMavenTargetDefinition(Shell shell, IProject targetProject, MavenBundleContainer mavenBundleContainer)
	{
		try
		{
			ITargetDefinition newTarget = loadMavenTargetDefinition(mavenBundleContainer);

			IFile targetFile = targetProject.getFile(PDE_TARGET_TARGET);

			// Ensure plugins folder is reset
			IFolder pluginsFolder = targetProject.getFolder(PDE_TARGET_PLUGINS);
			if (pluginsFolder.exists())
			{
				pluginsFolder.delete(true, null);
			}
			pluginsFolder.create(true, true, null);

			// Ensure otherPlugins folder exists
			IFolder otherPluginsFolder = targetProject.getFolder(PDE_TARGET_OTHER_PLUGINS);
			if (!otherPluginsFolder.exists())
			{
				otherPluginsFolder.create(true, true, null);
			}

			File pluginsFolderFile = pluginsFolder.getLocation().toFile();

			// Copy all maven dependencies in pluginsFolder

			// Keep all project artifactIds to ensure they are used instead of any other dependencies
			final HashSet<String> projectArtifactId = new HashSet<String>();
			for (IMavenProjectFacade mavenProject : mavenBundleContainer.getMavenProjects())
			{
				projectArtifactId.add(mavenProject.getArtifactKey().getArtifactId());
			}

			Collection<Artifact> mostRecentArtifacts = getMostRecentArtifacts(mavenBundleContainer.getArtifacts(null));

			CollectionUtils.filter(mostRecentArtifacts, new Predicate()
				{
					@Override
					public boolean evaluate(Object param)
					{
						Artifact artifact = (Artifact) param;

						return !projectArtifactId.contains(artifact.getArtifactId());
					}
				});

			for (Artifact artifact : mostRecentArtifacts)
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
			NullProgressMonitor monitor = new NullProgressMonitor();
			targetFile.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			pluginsFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			otherPluginsFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			targetProject.refreshLocal(IResource.DEPTH_INFINITE, monitor);

			// Save target definition
			targetPlatformService.saveTargetDefinition(directoryTargetDefinition);

			LoadTargetDefinitionJob.load(directoryTargetDefinition);
			return newTarget;
		}
		catch (Exception e)
		{
			MessageBox msgbox = new MessageBox(shell, SWT.ALPHA);
			
			msgbox.setMessage("Unable to create PDE Target, delelete file " + PDE_TARGET_TARGET + " in your project");
			msgbox.setText("Error while creating PDE Target");
			msgbox.open();

			e.printStackTrace();

			throw new RuntimeException("Unable to create target");
		}
	}

	private Collection<Artifact> getMostRecentArtifacts(Set<Artifact> artifacts)
	{
		Map<String, Artifact> artifactIdToArtifact = new HashMap<String, Artifact>();

		for (Artifact artifact : artifacts)
		{
			if (artifact.getScope() != null && artifact.getScope().contains("provide"))
			{
				continue;
			}

			String artifactId = artifact.getArtifactId();

			ArtifactVersion artifactVersion;

			try
			{
				artifactVersion = new DefaultArtifactVersion(artifact.getVersion());
			}
			catch (Exception e)
			{
				artifactVersion = null;
			}

			Artifact actualArtifactForId = artifactIdToArtifact.get(artifactId);
			ArtifactVersion actualArtifactVersion = (actualArtifactForId == null) ? null : new DefaultArtifactVersion(actualArtifactForId.getVersion());

			if (actualArtifactVersion == null)
			{
				artifactIdToArtifact.put(artifactId, artifact);
			}
			else
			{
				if (actualArtifactVersion.compareTo(artifactVersion) < 0)
				{
					artifactIdToArtifact.put(artifactId, artifact);
				}
			}
		}

		return artifactIdToArtifact.values();
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
