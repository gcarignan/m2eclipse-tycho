package com.netappsid.m2e.pde.target.internals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.target.AbstractBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.IResolvedBundle;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;

public class MavenBundleContainer extends AbstractBundleContainer {

	/**
	 * Constant describing the type of bundle container
	 */
	public static final String TYPE = "Maven"; //$NON-NLS-1$

	private final IMaven maven;
	private final IMavenProjectFacade[] mavenProjects;

	public MavenBundleContainer(IMaven maven, IMavenProjectFacade[] projects) {
		this.maven = maven;
		this.mavenProjects = projects;
	}

	@Override
	protected IResolvedBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {

		Set<Artifact> artifacts = new HashSet<Artifact>();

		for (int i = 0; i < mavenProjects.length; i++) {
			IMavenProjectFacade mavenProjectFacade = mavenProjects[i];
			artifacts.addAll(mavenProjectFacade.getMavenProject().getArtifacts());
		}

		List<IResolvedBundle> resolvedBundles = new ArrayList<IResolvedBundle>();

		for (Artifact artifact : artifacts) {
			IResolvedBundle generateBundle = generateBundle(artifact.getFile());

			if (generateBundle != null) {
				resolvedBundles.add(generateBundle);
			}
		}

		return resolvedBundles.toArray(new IResolvedBundle[0]);
	}

	@Override
	protected IFeatureModel[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		return new IFeatureModel[0];
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public String getLocation(boolean resolve) throws CoreException {
		return maven.getLocalRepositoryPath();
	}

	@Override
	public boolean isContentEqual(AbstractBundleContainer container) {
		return false;
	}

}
