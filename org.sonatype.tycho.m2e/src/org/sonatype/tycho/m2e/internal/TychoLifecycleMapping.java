package org.sonatype.tycho.m2e.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.maven.ide.eclipse.internal.project.CustomizableLifecycleMapping;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectUtils;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.IExtensionLifecycleMapping;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;

@SuppressWarnings( "restriction" )
public class TychoLifecycleMapping
    extends CustomizableLifecycleMapping
    implements IExtensionLifecycleMapping
{

    @Override
    public void configure( ProjectConfigurationRequest request, IProgressMonitor monitor )
        throws CoreException
    {
        super.configure( request, monitor );

        MavenProject mavenProject = request.getMavenProject();
        IProject project = request.getProject();

        String packaging = mavenProject.getPackaging();
        if ( "eclipse-plugin".equals( packaging ) || "eclipse-test-plugin".equals( packaging ) )
        {
            configurePDEBundleProject( project, mavenProject, monitor );
        }
        else if ( "eclipse-feature".equals( packaging ) )
        {
            // see org.eclipse.pde.internal.ui.wizards.feature.AbstractCreateFeatureOperation
            if ( !project.hasNature( PDE.FEATURE_NATURE ) )
            {
                CoreUtility.addNatureToProject( project, PDE.FEATURE_NATURE, monitor );
            }
        }
        else if ( "eclipse-update-site".equals( packaging ) )
        {
            // see org.eclipse.pde.internal.ui.wizards.site.NewSiteProjectCreationOperation
            if ( !project.hasNature( PDE.SITE_NATURE ) )
            {
                CoreUtility.addNatureToProject( project, PDE.SITE_NATURE, monitor );
            }
        }
    }

    private void configurePDEBundleProject( IProject project, MavenProject mavenProject, IProgressMonitor monitor )
        throws CoreException
    {
        // see org.eclipse.pde.internal.ui.wizards.plugin.NewProjectCreationOperation

        if ( !project.hasNature( PDE.PLUGIN_NATURE ) )
        {
            CoreUtility.addNatureToProject( project, PDE.PLUGIN_NATURE, null );
        }

        if ( !project.hasNature( JavaCore.NATURE_ID ) )
        {
            CoreUtility.addNatureToProject( project, JavaCore.NATURE_ID, null );
        }

        // PDE can't handle default JDT classpath
        IJavaProject javaProject = JavaCore.create( project );
        javaProject.setRawClasspath( new IClasspathEntry[0], true, monitor );
        javaProject.setOutputLocation( getOutputLocation( project, mavenProject, monitor ), monitor );

        // see org.eclipse.pde.internal.ui.wizards.tools.UpdateClasspathJob
        IPluginModelBase model = PluginRegistry.findModel( project );
        if ( model != null )
        {
            // PDE populates the model cache lazily from WorkspacePluginModelManager.visit() ResourceChangeListenter
            // Avoid NPE for now, but users may have to invoke PDE->UpdateClasspath manually
            ClasspathComputer.setClasspath( project, model );
        }
    }

    private IPath getOutputLocation( IProject project, MavenProject mavenProject, IProgressMonitor monitor )
        throws CoreException
    {
        File outputDirectory = new File( mavenProject.getBuild().getOutputDirectory() );
        outputDirectory.mkdirs();
        IPath relPath = MavenProjectUtils.getProjectRelativePath( project, mavenProject.getBuild().getOutputDirectory() );
        IFolder folder = project.getFolder( relPath );
        folder.refreshLocal( IResource.DEPTH_INFINITE, monitor );
        return folder.getFullPath();
    }

    @Override
    public List<AbstractProjectConfigurator> getProjectConfigurators( IMavenProjectFacade facade,
                                                                      IProgressMonitor monitor )
        throws CoreException
    {
        MavenProject mavenProject = facade.getMavenProject( monitor );
        Plugin plugin = mavenProject.getPlugin( "org.maven.ide.eclipse:lifecycle-mapping" );

        if ( plugin == null )
        {
            // it is okay to have no mapping
            return new ArrayList<AbstractProjectConfigurator>();
        }

        return super.getProjectConfigurators( facade, monitor );
    }
}
