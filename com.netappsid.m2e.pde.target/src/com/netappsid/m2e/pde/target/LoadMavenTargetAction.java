package com.netappsid.m2e.pde.target;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService;
import org.eclipse.pde.internal.core.target.provisional.LoadTargetDefinitionJob;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.netappsid.m2e.pde.target.internals.MavenBundleContainer;
import com.netappsid.m2e.pde.target.internals.MavenPDETarget;

public class LoadMavenTargetAction implements IObjectActionDelegate
{
	private Shell shell;
	private ISelection selection;

	public LoadMavenTargetAction()
	{
		super();
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart)
	{
		shell = targetPart.getSite().getShell();
	}

	protected Shell getShell()
	{
		return shell;
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	@Override
	public void run(IAction action)
	{
		MavenPDETarget mavenPDETarget = new MavenPDETarget(MavenPDETargetPlugin.getService(ITargetPlatformService.class));

		IMavenProjectFacade[] workspaceMavenProjects = MavenPluginActivator.getDefault().getMavenProjectManagerImpl().getProjects();
		IMavenProjectFacade[] openedProjects = getOpenedProjectsOnly(workspaceMavenProjects);

		MavenBundleContainer mavenBundleContainer = createMavenBundleContainer(openedProjects);

		run(mavenPDETarget, mavenBundleContainer);
	}

	protected void run(MavenPDETarget mavenPDETarget, MavenBundleContainer mavenBundleContainer)
	{
		ITargetDefinition newTarget = mavenPDETarget.loadMavenTargetDefinition(mavenBundleContainer);
		LoadTargetDefinitionJob.load(newTarget);
	}

	protected MavenBundleContainer createMavenBundleContainer(IMavenProjectFacade[] openedProjects)
	{
		IMaven maven = MavenPlugin.getMaven();
		MavenBundleContainer mavenBundleContainer = new MavenBundleContainer(maven, openedProjects);
		return mavenBundleContainer;
	}

	protected IMavenProjectFacade[] getOpenedProjectsOnly(IMavenProjectFacade[] projects)
	{
		List<IMavenProjectFacade> projectsList = new ArrayList<IMavenProjectFacade>(Arrays.asList(projects));

		CollectionUtils.filter(projectsList, new Predicate()
			{
				@Override
				public boolean evaluate(Object item)
				{
					IMavenProjectFacade project = (IMavenProjectFacade) item;
					return project.getProject().isOpen();
				}
			});
		return projectsList.toArray(new IMavenProjectFacade[] {});
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection)
	{
		this.selection = selection;
	}

	protected ISelection getSelection()
	{
		return selection;
	}

}
