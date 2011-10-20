package com.netappsid.m2e.pde.target;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.netappsid.m2e.pde.target.internals.MavenPDETarget;

public class SaveMavenTargetAction implements IObjectActionDelegate {

	private Shell shell;
	private ISelection selection;

	/**
	 * Constructor for Action1.
	 */
	public SaveMavenTargetAction() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	@Override
	public void run(IAction action)
	{
		MavenPDETarget mavenPDETarget = new MavenPDETarget(
				MavenPDETargetPlugin.getService(ITargetPlatformService.class));
		mavenPDETarget.saveMavenTargetDefinition(shell, (IStructuredSelection) selection);
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) 
	{
		this.selection = selection;
	}

}

