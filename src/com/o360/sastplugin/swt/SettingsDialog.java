package com.o360.sastplugin.swt;

import com.o360.sastplugin.Activator;
import com.o360.sastplugin.util.O360Logger;
import com.o360.sastplugin.util.SslHelper;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.net.HttpURLConnection;
import java.net.URL;

public class SettingsDialog extends Dialog {

    private Text serverUrlText;
    private Text accessTokenText;
    private Button selfSignedCheck;

    public SettingsDialog(Shell parent) {
        super(parent);
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("O360 SAST Settings");
        shell.setSize(550, 350);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        container.setLayout(new GridLayout(2, false));

        IPreferenceStore store = Activator.getDefault().getPreferenceStore();

        // Server URL
        Label urlLabel = new Label(container, SWT.NONE);
        urlLabel.setText("Server URL:");
        serverUrlText = new Text(container, SWT.BORDER);
        serverUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        serverUrlText.setText(store.getString(Activator.PREF_SERVER_URL));
        serverUrlText.setMessage("https://your-o360-server.com");

        // Access Token
        Label tokenLabel = new Label(container, SWT.NONE);
        tokenLabel.setText("Access Token:");
        accessTokenText = new Text(container, SWT.BORDER | SWT.PASSWORD);
        accessTokenText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        accessTokenText.setText(store.getString(Activator.PREF_ACCESS_TOKEN));
        accessTokenText.setMessage("Enter your O360 access token");

        // Show/Hide token
        new Label(container, SWT.NONE); // spacer
        Button showTokenBtn = new Button(container, SWT.CHECK);
        showTokenBtn.setText("Show token");
        showTokenBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String val = accessTokenText.getText();
                Composite p = accessTokenText.getParent();
                accessTokenText.dispose();
                accessTokenText = new Text(p, SWT.BORDER | (showTokenBtn.getSelection() ? SWT.NONE : SWT.PASSWORD));
                accessTokenText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
                accessTokenText.setText(val);
                // Move above the showTokenBtn row
                accessTokenText.moveAbove(showTokenBtn);
                p.layout(true);
            }
        });

        // SSL checkbox
        new Label(container, SWT.NONE); // spacer
        selfSignedCheck = new Button(container, SWT.CHECK);
        selfSignedCheck.setText("Allow self-signed SSL certificates");
        selfSignedCheck.setSelection(store.getBoolean(Activator.PREF_ALLOW_SELF_SIGNED));

        // Test Connection button
        new Label(container, SWT.NONE); // spacer
        Button testBtn = new Button(container, SWT.PUSH);
        testBtn.setText("Test Connection");
        testBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                testConnection();
            }
        });

        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Save", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        String url = serverUrlText.getText().trim();
        // Remove trailing slash
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        store.setValue(Activator.PREF_SERVER_URL, url);
        store.setValue(Activator.PREF_ACCESS_TOKEN, accessTokenText.getText().trim());
        store.setValue(Activator.PREF_ALLOW_SELF_SIGNED, selfSignedCheck.getSelection());
        O360Logger.info("Settings saved. Server: " + url);
        super.okPressed();
    }

    private void testConnection() {
        String url = serverUrlText.getText().trim();
        String token = accessTokenText.getText().trim();
        boolean allowSelfSigned = selfSignedCheck.getSelection();

        if (url.isEmpty() || token.isEmpty()) {
            MessageDialog.openWarning(getShell(), "Test Connection",
                    "Please enter both Server URL and Access Token.");
            return;
        }

        // Remove trailing slash
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        String testUrl = url + "/app/api/ExternalScan";

        try {
            URL u = new URL(testUrl);
            HttpURLConnection conn;

            if (testUrl.startsWith("https") && allowSelfSigned) {
                HttpsURLConnection httpsConn = (HttpsURLConnection) u.openConnection();
                SSLContext sslContext = SslHelper.createTrustAllContext();
                httpsConn.setSSLSocketFactory(sslContext.getSocketFactory());
                httpsConn.setHostnameVerifier(SslHelper.createTrustAllHostnameVerifier());
                conn = httpsConn;
            } else {
                conn = (HttpURLConnection) u.openConnection();
            }

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.setRequestProperty("Authorization", "Bearer " + token);

            int code = conn.getResponseCode();

            if (code == 200 || code == 405 || code == 404 || code == 401) {
                // 405 = Method Not Allowed (expected for GET on POST endpoint)
                // 404 = endpoint exists on the server but different routing
                // 200 = great
                if (code == 401) {
                    MessageDialog.openWarning(getShell(), "Test Connection",
                            "Connection successful but token is invalid (HTTP 401).");
                } else {
                    MessageDialog.openInformation(getShell(), "Test Connection",
                            "Connection successful! (HTTP " + code + ")");
                }
            } else {
                MessageDialog.openWarning(getShell(), "Test Connection",
                        "Server responded with HTTP " + code);
            }
        } catch (Exception ex) {
            MessageDialog.openError(getShell(), "Test Connection",
                    "Connection failed: " + ex.getMessage());
        }
    }
}
