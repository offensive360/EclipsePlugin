package com.o360.sastplugin.handlers;

import com.o360.sastplugin.scanners.ScanJob;
import com.o360.sastplugin.swt.ProjectSelectionDialog;
import com.o360.sastplugin.util.O360Logger;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

public class ScanHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IProject project = getSelectedProject(event);

        if (project == null) {
            IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
            IProject[] openProjects = java.util.Arrays.stream(projects)
                    .filter(IProject::isOpen).toArray(IProject[]::new);

            if (openProjects.length == 0) {
                // No projects in workspace -- offer directory chooser
                DirectoryDialog dirDialog = new DirectoryDialog(
                    Display.getDefault().getActiveShell(), SWT.OPEN);
                dirDialog.setText("O360 SAST - Select Project Folder");
                dirDialog.setMessage("Select a project folder to scan:");
                dirDialog.setFilterPath("c:\\Users\\Administrator\\Desktop");
                String path = dirDialog.open();
                if (path != null) {
                    O360Logger.info("Starting scan for external folder: " + path);
                    java.io.File dir = new java.io.File(path);
                    ScanJob job = new ScanJob(dir.getName(), path);
                    job.schedule();
                }
                return null;
            }

            if (openProjects.length == 1) {
                project = openProjects[0];
            } else {
                ProjectSelectionDialog dialog = new ProjectSelectionDialog(
                    Display.getDefault().getActiveShell(), openProjects);
                if (dialog.open() == org.eclipse.jface.dialogs.Dialog.OK) {
                    project = dialog.getSelectedProject();
                } else {
                    return null;
                }
            }
        }

        if (project != null && project.isOpen()) {
            O360Logger.info("Starting scan for project: " + project.getName());
            ScanJob job = new ScanJob(project);
            job.schedule();
        }
        return null;
    }

    private IProject getSelectedProject(ExecutionEvent event) {
        try {
            ISelection selection = HandlerUtil.getCurrentSelection(event);
            if (selection instanceof IStructuredSelection structured) {
                Object element = structured.getFirstElement();
                if (element instanceof IProject proj) {
                    return proj;
                }
                if (element instanceof IResource resource) {
                    return resource.getProject();
                }
                if (element instanceof org.eclipse.core.runtime.IAdaptable adaptable) {
                    IResource res = adaptable.getAdapter(IResource.class);
                    if (res != null) return res.getProject();
                }
            }
        } catch (Exception e) {
            O360Logger.warn("Could not determine selected project: " + e.getMessage());
        }
        return null;
    }
}
