package com.netappsid.m2e.pde.target.internals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.project.MavenProjectHelper;
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

	/**
	 * @component
	 */
	public MavenProjectHelper mavenProjectHelper;

	private static final String MAVEN_TARGET = "Maven Target";
	public static final String PDE_TARGET_TARGET = "PDE_TARGET.target";
	public static final String PDE_TARGET_OTHER_PLUGINS = "PDE_TARGET_OtherPlugins";
	public static final String PDE_TARGET_PLUGINS = "PDE_TARGET_Plugins";
	public static final String PDE_TARGET_PLUGINS_TEST = "PDE_TARGET_Test_Plugins";
	public static final String PDE_TARGET_PLUGINS_SOURCES = "PDE_TARGET_Plugins_Sources";
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
		return saveMavenTargetDefinition(shell, targetProject, mavenBundleContainer, false);
	}

	public ITargetDefinition saveMavenTargetDefinition(Shell shell, IProject targetProject, MavenBundleContainer mavenBundleContainer, boolean withSources)
	{
		try
		{
			ITargetDefinition newTarget = loadMavenTargetDefinition(mavenBundleContainer);

			IFile targetFile = targetProject.getFile(PDE_TARGET_TARGET);

			// Ensure plugins folder is reset
			IFolder pluginsFolder = ensureProjectFolder(targetProject, PDE_TARGET_PLUGINS);
			IFolder testPluginsFolder = ensureProjectFolder(targetProject, PDE_TARGET_PLUGINS_TEST);

			// Ensure otherPlugins folder exists
			IFolder otherPluginsFolder = ensureProjectFolder(targetProject, PDE_TARGET_OTHER_PLUGINS);
			if (!otherPluginsFolder.exists())
			{
				otherPluginsFolder.create(true, true, null);
			}

			File pluginsFolderFile = pluginsFolder.getLocation().toFile();
			File testPluginsFolderFile = testPluginsFolder.getLocation().toFile();

			// Copy all maven dependencies in pluginsFolder

			// Keep all project artifactIds to ensure they are used instead of any other dependencies
			final HashSet<String> projectArtifactId = new HashSet<String>();
			for (IMavenProjectFacade mavenProject : mavenBundleContainer.getMavenProjects())
			{
				projectArtifactId.add(mavenProject.getArtifactKey().getArtifactId());
			}

			Set<Artifact> artifacts = mavenBundleContainer.getArtifacts(null);

			ArtifactsData mostRecentArtifactsData = getMostRecentArtifacts(artifacts);

			CollectionUtils.filter(mostRecentArtifactsData.getArtifacts(), new Predicate()
				{
					@Override
					public boolean evaluate(Object param)
					{
						Artifact artifact = (Artifact) param;

						return !projectArtifactId.contains(artifact.getArtifactId());
					}
				});

			File sourcesPluginsFolderFile = (withSources) ? pluginsFolderFile : null;
			copyArtifactsToFolder(mostRecentArtifactsData.getArtifacts(), pluginsFolderFile, sourcesPluginsFolderFile);

			copyArtifactsToFolder(mostRecentArtifactsData.getTestArtifacts(), testPluginsFolderFile, null);

			// Update targetFile with the DirectoryBundelContainer if not already present
			ITargetHandle targetHandle = targetPlatformService.getTarget(targetFile);
			ITargetDefinition directoryTargetDefinition = targetHandle.getTargetDefinition();

			String pluginsFolderPath = pluginsFolderFile.getAbsolutePath();
			String otherPluginsPath = otherPluginsFolder.getLocation().toFile().getAbsolutePath();
			String testPluginsFolderPath = testPluginsFolderFile.getAbsolutePath();

			if (targetFile.exists())
			{
				// Find already existing DirectoryBundleContainer for maven repository path
				addBundleContainerToTargetDefinitionIfNotPresent(directoryTargetDefinition, pluginsFolderPath);
				addBundleContainerToTargetDefinitionIfNotPresent(directoryTargetDefinition, otherPluginsPath);
				addBundleContainerToTargetDefinitionIfNotPresent(directoryTargetDefinition, testPluginsFolderPath);
			}
			else
			{
				directoryTargetDefinition.setBundleContainers(new IBundleContainer[] { new DirectoryBundleContainer(pluginsFolderPath),
						new DirectoryBundleContainer(otherPluginsPath), new DirectoryBundleContainer(testPluginsFolderPath) });
			}

			// Refresh project folder tree
			NullProgressMonitor monitor = new NullProgressMonitor();
			targetFile.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			pluginsFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			otherPluginsFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			testPluginsFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
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

			throw new RuntimeException("Unable to create target", e);
		}
	}

	private IFolder ensureProjectFolder(IProject targetProject, String folderName) throws CoreException
	{
		IFolder folder = targetProject.getFolder(folderName);
		if (folder.exists())
		{
			folder.delete(true, null);
		}
		folder.create(true, true, null);

		return folder;
	}

	private void copyArtifactsToFolder(Collection<Artifact> artifacts, File pluginsFolder, File sourceFolder) throws IOException
	{
		for (Artifact artifact : artifacts)
		{
			try
			{
				File bundleFile = artifact.getFile().getAbsoluteFile();
				if (bundleFile.isFile())
				{
					FileUtils.copyFileToDirectory(bundleFile, pluginsFolder);

					if (sourceFolder != null)
					{
						String absolutePath = bundleFile.getAbsolutePath();
						File sourcesJarFile = new File(absolutePath.replace(".jar", "-sources.jar"));

						if (sourcesJarFile.exists())
						{
							FileUtils.copyFileToDirectory(sourcesJarFile, sourceFolder);
						}
					}
				}

			}
			catch (IOException e)
			{
				throw e;
			}
		}
	}

	private ArtifactsData getMostRecentArtifacts(Set<Artifact> artifacts)
	{
		ArtifactsData artifactsData = new ArtifactsData();

		for (Artifact artifact : artifacts)
		{
			String scope = artifact.getScope();

			if (scope != null && scope.contains("provide"))
			{
				continue;
			}
			Map<String, Artifact> artifactIdToArtifact;

			if (scope.contains("test"))
			{
				artifactIdToArtifact = artifactsData.getMostRecentArtifactIdToTestArtifacts();
			}
			else
			{
				artifactIdToArtifact = artifactsData.getMostRecentArtifactIdToArtifacts();
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

		return artifactsData;
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
