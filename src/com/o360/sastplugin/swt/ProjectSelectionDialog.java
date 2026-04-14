package com.o360.sastplugin.swt;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class ProjectSelectionDialog extends Dialog {

    private final IProject[] projects;
    private IProject selectedProject;
    private org.eclipse.swt.widgets.List projectList;

    public ProjectSelectionDialog(Shell parent, IProject[] projects) {
        super(parent);
        this.projects = projects;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Select Project to Scan");
        shell.setSize(400, 300);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        container.setLayout(new GridLayout(1, false));

        Label label = new Label(container, SWT.NONE);
        label.setText("Select a project to scan:");

        projectList = new org.eclipse.swt.widgets.List(container, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        projectList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        for (IProject p : projects) {
            if (p.isOpen()) {
                projectList.add(p.getName());
            }
        }

        if (projectList.getItemCount() > 0) {
            projectList.select(0);
        }

        projectList.addListener(SWT.MouseDoubleClick, e -> okPressed());

        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Scan", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed() {
        int index = projectList.getSelectionIndex();
        if (index >= 0) {
            String name = projectList.getItem(index);
            for (IProject p : projects) {
                if (p.getName().equals(name)) {
                    selectedProject = p;
                    break;
                }
            }
        }
        super.okPressed();
    }

    public IProject getSelectedProject() {
        return selectedProject;
    }
}
