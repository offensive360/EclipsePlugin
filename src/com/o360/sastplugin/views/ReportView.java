package com.o360.sastplugin.views;

import com.o360.sastplugin.Activator;
import com.o360.sastplugin.model.Vulnerability;
import com.o360.sastplugin.util.O360Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.IEditorPart;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ReportView extends ViewPart {

    public static final String ID = "o360sastplugin.views.ReportView";

    private TreeViewer treeViewer;
    private CTabFolder detailTabs;
    private Browser detailsBrowser;
    private Browser fixBrowser;
    private Browser referencesBrowser;
    private Label statusLabel;
    private IProject currentProject;
    private String currentProjectName;
    private String currentProjectPath;
    private java.util.List<Vulnerability> currentVulnerabilities;

    @Override
    public void createPartControl(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        // Status bar
        statusLabel = new Label(parent, SWT.NONE);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        statusLabel.setText("O360 SAST - Ready");

        // Split: tree on left, details on right
        SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
        sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Tree viewer
        treeViewer = new TreeViewer(sash, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        treeViewer.setContentProvider(new FindingsContentProvider());
        treeViewer.setLabelProvider(new FindingsLabelProvider());

        // Details panel with tabs
        detailTabs = new CTabFolder(sash, SWT.BORDER | SWT.BOTTOM);

        // Details tab
        CTabItem detailsItem = new CTabItem(detailTabs, SWT.NONE);
        detailsItem.setText("Details");
        detailsBrowser = new Browser(detailTabs, SWT.NONE);
        detailsItem.setControl(detailsBrowser);

        // How to Fix tab
        CTabItem fixItem = new CTabItem(detailTabs, SWT.NONE);
        fixItem.setText("How to Fix");
        fixBrowser = new Browser(detailTabs, SWT.NONE);
        fixItem.setControl(fixBrowser);

        // References tab
        CTabItem refsItem = new CTabItem(detailTabs, SWT.NONE);
        refsItem.setText("References");
        referencesBrowser = new Browser(detailTabs, SWT.NONE);
        refsItem.setControl(referencesBrowser);

        detailTabs.setSelection(0);

        sash.setWeights(new int[]{40, 60});

        // Selection listener for detail display
        treeViewer.addSelectionChangedListener(event -> {
            IStructuredSelection sel = event.getStructuredSelection();
            if (!sel.isEmpty() && sel.getFirstElement() instanceof TreeObject obj) {
                Vulnerability v = obj.getVulnerability();
                if (v != null) {
                    showVulnerabilityDetails(v);
                }
            }
        });

        // Double-click to navigate
        treeViewer.addDoubleClickListener(event -> {
            IStructuredSelection sel = (IStructuredSelection) event.getSelection();
            if (!sel.isEmpty()) {
                Object elem = sel.getFirstElement();
                if (elem instanceof TreeParent treeParent) {
                    // Expand/collapse group
                    treeViewer.setExpandedState(treeParent, !treeViewer.getExpandedState(treeParent));
                } else if (elem instanceof TreeObject obj && obj.getVulnerability() != null) {
                    navigateToCode(obj.getVulnerability());
                }
            }
        });

        // Context menu
        createContextMenu();

        // Initial empty state
        setEmptyMessage();
    }

    private void createContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> {
            IStructuredSelection sel = treeViewer.getStructuredSelection();
            if (!sel.isEmpty() && sel.getFirstElement() instanceof TreeObject obj) {
                Vulnerability v = obj.getVulnerability();
                if (v != null) {
                    manager.add(new Action("Go to Code") {
                        @Override
                        public void run() {
                            navigateToCode(v);
                        }
                    });
                    manager.add(new Action("Suppress Finding") {
                        @Override
                        public void run() {
                            v.setSuppressed(true);
                            refreshTree();
                        }
                    });
                    manager.add(new Separator());
                    manager.add(new Action("Get Help") {
                        @Override
                        public void run() {
                            try {
                                String url = v.getReferences();
                                if (url == null || url.isEmpty() || !url.startsWith("http")) {
                                    url = "https://owasp.org/www-community/attacks/";
                                }
                                org.eclipse.swt.program.Program.launch(url);
                            } catch (Exception e) {
                                O360Logger.warn("Failed to open help URL: " + e.getMessage());
                            }
                        }
                    });
                }
            }
            manager.add(new Separator());
            manager.add(new Action("Clear All Results") {
                @Override
                public void run() {
                    currentVulnerabilities = null;
                    setEmptyMessage();
                    clearDetails();
                    statusLabel.setText("O360 SAST - Ready");
                }
            });
        });

        Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
        treeViewer.getControl().setMenu(menu);
    }

    public void setResults(java.util.List<Vulnerability> vulnerabilities, IProject project) {
        this.currentProject = project;
        this.currentProjectName = project != null ? project.getName() : "project";
        this.currentProjectPath = project != null ? project.getLocation().toOSString() : null;
        this.currentVulnerabilities = vulnerabilities;
        refreshTree();
    }

    public void setResults(java.util.List<Vulnerability> vulnerabilities, IProject project,
                           String projectName, String projectPath) {
        this.currentProject = project;
        this.currentProjectName = projectName;
        this.currentProjectPath = projectPath;
        this.currentVulnerabilities = vulnerabilities;
        refreshTree();
    }

    private void refreshTree() {
        if (currentVulnerabilities == null) {
            setEmptyMessage();
            return;
        }

        TreeParent root = new TreeParent("Scan Results");
        Map<String, TreeParent> severityGroups = new LinkedHashMap<>();

        // Create severity groups in order
        String[] severities = {"Critical", "High", "Medium", "Low", "Info"};
        for (String sev : severities) {
            severityGroups.put(sev.toLowerCase(), new TreeParent(sev));
        }

        int visibleCount = 0;
        for (Vulnerability v : currentVulnerabilities) {
            if (v.isSuppressed()) continue;
            visibleCount++;

            String sev = v.getSeverity() != null ? v.getSeverity().toLowerCase() : "info";
            // Normalize severity
            if (sev.contains("critical")) sev = "critical";
            else if (sev.contains("high")) sev = "high";
            else if (sev.contains("medium")) sev = "medium";
            else if (sev.contains("low")) sev = "low";
            else sev = "info";

            TreeParent group = severityGroups.get(sev);
            if (group == null) {
                group = severityGroups.get("info");
            }

            String label = v.getTitle();
            if (v.getFileName() != null) {
                String shortFile = v.getFileName();
                int lastSlash = Math.max(shortFile.lastIndexOf('/'), shortFile.lastIndexOf('\\'));
                if (lastSlash >= 0) shortFile = shortFile.substring(lastSlash + 1);
                label += " (" + shortFile;
                if (v.getLineNum() > 0) label += ":" + v.getLineNum();
                label += ")";
            }

            TreeObject node = new TreeObject(label, v);
            group.addChild(node);
        }

        // Add non-empty groups to root
        for (Map.Entry<String, TreeParent> entry : severityGroups.entrySet()) {
            TreeParent group = entry.getValue();
            if (group.hasChildren()) {
                // Rename to include count
                TreeParent namedGroup = new TreeParent(
                    group.getName() + " (" + group.getChildCount() + ")");
                for (TreeObject child : group.getChildren()) {
                    namedGroup.addChild(child);
                }
                root.addChild(namedGroup);
            }
        }

        treeViewer.setInput(root);
        treeViewer.expandAll();

        statusLabel.setText("O360 SAST - " + visibleCount + " finding(s) in " +
                (currentProjectName != null ? currentProjectName : "project"));
    }

    private void setEmptyMessage() {
        TreeParent root = new TreeParent("No scan results");
        root.addChild(new TreeObject("Use Ctrl+Alt+S or right-click a project to scan"));
        treeViewer.setInput(root);
        treeViewer.expandAll();
    }

    private void clearDetails() {
        if (detailsBrowser != null && !detailsBrowser.isDisposed()) detailsBrowser.setText("");
        if (fixBrowser != null && !fixBrowser.isDisposed()) fixBrowser.setText("");
        if (referencesBrowser != null && !referencesBrowser.isDisposed()) referencesBrowser.setText("");
    }

    private void showVulnerabilityDetails(Vulnerability v) {
        String css = "<style>"
            + "body { font-family: Segoe UI, Arial, sans-serif; font-size: 12px; padding: 10px; "
            + "background: #1e1e1e; color: #d4d4d4; }"
            + "h2 { color: #569cd6; margin-top: 0; }"
            + "h3 { color: #4ec9b0; }"
            + ".severity { padding: 3px 8px; border-radius: 3px; font-weight: bold; color: white; }"
            + ".critical { background: #d32f2f; }"
            + ".high { background: #f57c00; }"
            + ".medium { background: #fbc02d; color: #333; }"
            + ".low { background: #388e3c; }"
            + ".info { background: #1976d2; }"
            + "pre { background: #2d2d2d; padding: 10px; border-radius: 4px; overflow-x: auto; "
            + "border: 1px solid #444; white-space: pre-wrap; word-wrap: break-word; }"
            + "code { color: #ce9178; }"
            + ".field { margin-bottom: 8px; }"
            + ".label { font-weight: bold; color: #9cdcfe; }"
            + "</style>";

        String sevClass = v.getSeverity() != null ? v.getSeverity().toLowerCase() : "info";

        // Details tab
        StringBuilder details = new StringBuilder();
        details.append("<html><head>").append(css).append("</head><body>");
        details.append("<h2>").append(esc(v.getTitle())).append("</h2>");
        details.append("<span class='severity ").append(sevClass).append("'>")
               .append(esc(v.getSeverity())).append("</span>");

        if (v.getFileName() != null) {
            details.append("<div class='field'><span class='label'>File: </span>")
                   .append(esc(v.getFileName()));
            if (v.getLineNum() > 0) details.append(" (line ").append(v.getLineNum()).append(")");
            details.append("</div>");
        }
        if (v.getType() != null) {
            details.append("<div class='field'><span class='label'>Type: </span>")
                   .append(esc(v.getType())).append("</div>");
        }
        if (v.getVulnerability() != null && !v.getVulnerability().equals(v.getTitle())) {
            details.append("<div class='field'><span class='label'>Vulnerability: </span>")
                   .append(esc(v.getVulnerability())).append("</div>");
        }
        if (v.getEffect() != null) {
            details.append("<h3>Impact</h3><p>").append(esc(v.getEffect())).append("</p>");
        }

        String snippet = v.getDecodedCodeSnippet();
        if (snippet != null && !snippet.isEmpty()) {
            details.append("<h3>Code Snippet</h3><pre><code>").append(esc(snippet)).append("</code></pre>");
        }

        details.append("</body></html>");
        detailsBrowser.setText(details.toString());

        // Fix tab
        StringBuilder fix = new StringBuilder();
        fix.append("<html><head>").append(css).append("</head><body>");
        fix.append("<h2>How to Fix</h2>");
        if (v.getRecommendation() != null) {
            fix.append("<p>").append(esc(v.getRecommendation())).append("</p>");
        } else {
            fix.append("<p>No recommendation available.</p>");
        }
        fix.append("</body></html>");
        fixBrowser.setText(fix.toString());

        // References tab
        StringBuilder refs = new StringBuilder();
        refs.append("<html><head>").append(css).append("</head><body>");
        refs.append("<h2>References</h2>");
        if (v.getReferences() != null && !v.getReferences().isEmpty()) {
            // Try to make URLs clickable
            String refText = v.getReferences();
            if (refText.startsWith("http")) {
                refs.append("<p><a href='").append(esc(refText)).append("'>")
                    .append(esc(refText)).append("</a></p>");
            } else {
                refs.append("<p>").append(refText.replace("\n", "<br/>")).append("</p>");
            }
        } else {
            refs.append("<p>No references available.</p>");
        }
        refs.append("</body></html>");
        referencesBrowser.setText(refs.toString());
    }

    private void navigateToCode(Vulnerability v) {
        if (v.getFileName() == null) return;

        try {
            String fileName = v.getFileName().replace('\\', '/');

            // Try IProject-based lookup first
            if (currentProject != null) {
                IFile file = null;
                file = currentProject.getFile(new Path(fileName));
                if (!file.exists() && fileName.startsWith("/")) {
                    file = currentProject.getFile(new Path(fileName.substring(1)));
                }
                if (!file.exists()) {
                    String baseName = fileName;
                    int lastSlash = baseName.lastIndexOf('/');
                    if (lastSlash >= 0) baseName = baseName.substring(lastSlash + 1);
                    file = findFileRecursive(currentProject, baseName);
                }

                if (file != null && file.exists()) {
                    openFileInEditor(file, v.getLineNum());
                    return;
                }
            }

            // Fallback: try external file path
            if (currentProjectPath != null) {
                java.io.File externalFile = new java.io.File(currentProjectPath, fileName);
                if (!externalFile.exists()) {
                    // Try with filePath
                    String fp = v.getFilePath();
                    if (fp != null) {
                        externalFile = new java.io.File(currentProjectPath, fp.replace('\\', '/'));
                    }
                }
                if (externalFile.exists()) {
                    IWorkbenchPage page = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage();
                    java.net.URI uri = externalFile.toURI();
                    IEditorPart editor = IDE.openEditorOnFileStore(page,
                        org.eclipse.core.filesystem.EFS.getStore(uri));
                    if (v.getLineNum() > 0 && editor instanceof ITextEditor textEditor) {
                        IDocument doc = textEditor.getDocumentProvider()
                                .getDocument(textEditor.getEditorInput());
                        if (doc != null) {
                            int lineNum = Math.max(0, v.getLineNum() - 1);
                            if (lineNum < doc.getNumberOfLines()) {
                                int offset = doc.getLineOffset(lineNum);
                                int length = doc.getLineLength(lineNum);
                                textEditor.selectAndReveal(offset, length);
                            }
                        }
                    }
                    return;
                }
            }

            O360Logger.warn("Could not find file: " + fileName);
        } catch (Exception e) {
            O360Logger.error("Failed to navigate to code", e);
        }
    }

    private void openFileInEditor(IFile file, int lineNum) throws Exception {
        IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();
        IEditorPart editor = IDE.openEditor(page, file);
        if (lineNum > 0 && editor instanceof ITextEditor textEditor) {
            IDocument doc = textEditor.getDocumentProvider()
                    .getDocument(textEditor.getEditorInput());
            if (doc != null) {
                int line = Math.max(0, lineNum - 1);
                if (line < doc.getNumberOfLines()) {
                    int offset = doc.getLineOffset(line);
                    int length = doc.getLineLength(line);
                    textEditor.selectAndReveal(offset, length);
                }
            }
        }
    }

    private IFile findFileRecursive(IProject project, String fileName) {
        try {
            final IFile[] found = {null};
            project.accept(resource -> {
                if (found[0] != null) return false;
                if (resource.getType() == IResource.FILE && resource.getName().equals(fileName)) {
                    found[0] = (IFile) resource;
                    return false;
                }
                return true;
            });
            return found[0];
        } catch (Exception e) {
            return null;
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("\n", "<br/>");
    }

    @Override
    public void setFocus() {
        if (treeViewer != null) {
            treeViewer.getControl().setFocus();
        }
    }
}
