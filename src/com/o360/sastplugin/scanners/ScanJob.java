package com.o360.sastplugin.scanners;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.o360.sastplugin.Activator;
import com.o360.sastplugin.model.Vulnerability;
import com.o360.sastplugin.update.PluginUpdateChecker;
import com.o360.sastplugin.util.O360Logger;
import com.o360.sastplugin.util.SslHelper;
import com.o360.sastplugin.views.ReportView;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

public class ScanJob extends Job {

    private static final int[] RETRY_DELAYS = {0, 15, 30, 60, 120, 240}; // seconds
    private static final Set<Integer> RETRYABLE_CODES = Set.of(500, 502, 503, 504);
    private static final int CONNECTION_TIMEOUT = 60_000; // 60 seconds
    private static final int READ_TIMEOUT = 14_400_000; // 4 hours

    private final IProject project;
    private final String projectName;
    private final String projectPath;
    private final ObjectMapper mapper;

    public ScanJob(IProject project) {
        super("O360 SAST Scan - " + project.getName());
        this.project = project;
        this.projectName = project.getName();
        this.projectPath = project.getLocation().toOSString();
        this.mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        setUser(true);
    }

    /** Constructor for scanning an external folder (no IProject) */
    public ScanJob(String name, String path) {
        super("O360 SAST Scan - " + name);
        this.project = null;
        this.projectName = name;
        this.projectPath = path;
        this.mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            monitor.beginTask("O360 SAST Scan", 100);

            String serverUrl = Activator.getDefault().getServerUrl();
            String token = Activator.getDefault().getAccessToken();
            boolean allowSelfSigned = Activator.getDefault().isAllowSelfSigned();

            if (serverUrl == null || serverUrl.isEmpty() || token == null || token.isEmpty()) {
                showError("Please configure O360 server URL and access token in Settings (Ctrl+Alt+D).");
                return Status.CANCEL_STATUS;
            }

            // Check for updates (throttled to once per 24h)
            PluginUpdateChecker.checkForUpdatesAsync();

            // Step 1: Compute file hashes
            monitor.subTask("Computing file hashes...");
            Map<String, String> currentHashes = Zipper.computeFileHashes(projectPath);
            monitor.worked(10);

            if (monitor.isCanceled()) return Status.CANCEL_STATUS;

            // Step 2: Check cache
            monitor.subTask("Checking cache...");
            List<Vulnerability> cachedResults = ScanCache.getCachedResults(projectPath, currentHashes);
            if (cachedResults != null) {
                O360Logger.info("Cache hit - using cached results");
                displayResults(cachedResults);
                monitor.done();
                return Status.OK_STATUS;
            }
            monitor.worked(5);

            if (monitor.isCanceled()) return Status.CANCEL_STATUS;

            // Step 3: Zip project
            monitor.subTask("Zipping project files...");
            File zipFile = Zipper.zipProject(projectPath, monitor);
            O360Logger.info("Zip file created: " + zipFile.getAbsolutePath() +
                    " (" + (zipFile.length() / 1024) + " KB)");
            monitor.worked(20);

            if (monitor.isCanceled()) {
                zipFile.delete();
                return Status.CANCEL_STATUS;
            }

            // Step 4: Upload and scan with retry
            monitor.subTask("Uploading to O360 server...");
            String responseBody = null;

            try {
                responseBody = uploadWithRetry(serverUrl, token, zipFile, allowSelfSigned, monitor);
            } finally {
                zipFile.delete();
            }
            monitor.worked(45);

            if (monitor.isCanceled()) return Status.CANCEL_STATUS;

            if (responseBody == null) {
                showError("Scan failed after all retry attempts.");
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Scan failed");
            }

            // Step 5: Parse response
            monitor.subTask("Processing results...");
            List<Vulnerability> vulnerabilities = parseResponse(responseBody);
            O360Logger.info("Scan complete: " + vulnerabilities.size() + " findings");
            monitor.worked(15);

            // Step 6: Cache results
            ScanCache.saveCache(projectPath, currentHashes, vulnerabilities);
            monitor.worked(5);

            // Step 7: Display results
            displayResults(vulnerabilities);
            monitor.done();
            return Status.OK_STATUS;

        } catch (Exception e) {
            O360Logger.error("Scan failed with exception", e);
            showError("Scan failed: " + e.getMessage());
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Scan failed", e);
        }
    }

    private String uploadWithRetry(String serverUrl, String token, File zipFile,
                                    boolean allowSelfSigned, IProgressMonitor monitor) throws IOException {
        String endpoint = serverUrl.endsWith("/")
                ? serverUrl + "app/api/ExternalScan"
                : serverUrl + "/app/api/ExternalScan";

        byte[] zipBytes = Files.readAllBytes(zipFile.toPath());

        for (int attempt = 0; attempt < RETRY_DELAYS.length; attempt++) {
            if (monitor.isCanceled()) return null;

            if (attempt > 0) {
                int delay = RETRY_DELAYS[attempt];
                monitor.subTask("Retrying in " + delay + " seconds (attempt " + (attempt + 1) + "/" + RETRY_DELAYS.length + ")...");
                O360Logger.info("Retry attempt " + (attempt + 1) + " in " + delay + "s");
                try {
                    Thread.sleep(delay * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }

            try {
                monitor.subTask("Uploading... (attempt " + (attempt + 1) + "/" + RETRY_DELAYS.length + ")");
                String boundary = "----O360Boundary" + System.currentTimeMillis();

                HttpURLConnection conn = createConnection(endpoint, allowSelfSigned);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(CONNECTION_TIMEOUT);
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (OutputStream os = conn.getOutputStream()) {
                    writeMultipartBody(os, boundary, zipBytes, projectName);
                }

                int responseCode = conn.getResponseCode();
                O360Logger.info("Server response code: " + responseCode);

                if (responseCode == 200 || responseCode == 201) {
                    try (InputStream is = conn.getInputStream()) {
                        return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    }
                }

                // Non-retryable errors
                if (responseCode == 401 || responseCode == 403) {
                    String errorBody = readErrorStream(conn);
                    showError("Authentication failed (HTTP " + responseCode + "). Please check your access token.\n" + errorBody);
                    return null;
                }

                if (RETRYABLE_CODES.contains(responseCode)) {
                    O360Logger.warn("Retryable error: HTTP " + responseCode);
                    continue;
                }

                // Other non-retryable error
                String errorBody = readErrorStream(conn);
                showError("Server returned HTTP " + responseCode + ": " + errorBody);
                return null;

            } catch (javax.net.ssl.SSLException e) {
                O360Logger.error("SSL error", e);
                showError("SSL connection error. Enable 'Allow self-signed certificates' in Settings.\n" + e.getMessage());
                return null;
            } catch (java.net.ConnectException | java.net.UnknownHostException e) {
                O360Logger.error("Connection error", e);
                showError("Cannot connect to server: " + e.getMessage());
                return null;
            } catch (IOException e) {
                if (attempt < RETRY_DELAYS.length - 1 && !monitor.isCanceled()) {
                    O360Logger.warn("IO error on attempt " + (attempt + 1) + ": " + e.getMessage());
                    continue;
                }
                throw e;
            }
        }
        return null;
    }

    private HttpURLConnection createConnection(String endpoint, boolean allowSelfSigned) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn;

        if (endpoint.startsWith("https") && allowSelfSigned) {
            HttpsURLConnection httpsConn = (HttpsURLConnection) url.openConnection();
            SSLContext sslContext = SslHelper.createTrustAllContext();
            httpsConn.setSSLSocketFactory(sslContext.getSocketFactory());
            httpsConn.setHostnameVerifier(SslHelper.createTrustAllHostnameVerifier());
            conn = httpsConn;
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }
        return conn;
    }

    private void writeMultipartBody(OutputStream os, String boundary, byte[] zipBytes, String projectName) throws IOException {
        String crlf = "\r\n";

        // Project name field
        writeFormField(os, boundary, "name", projectName);

        // KeepInvisibleAndDeletePostScan field
        writeFormField(os, boundary, "keepInvisibleAndDeletePostScan", "True");

        // External scan source type
        writeFormField(os, boundary, "externalScanSourceType", "EclipsePlugin");

        // Optional scan toggles
        writeFormField(os, boundary, "allowDependencyScan", "false");
        writeFormField(os, boundary, "allowLicenseScan", "false");
        writeFormField(os, boundary, "allowMalwareScan", "false");

        // File field (fileSource)
        os.write(("--" + boundary + crlf).getBytes());
        os.write(("Content-Disposition: form-data; name=\"fileSource\"; filename=\"" + projectName + ".zip\"" + crlf).getBytes());
        os.write(("Content-Type: application/zip" + crlf).getBytes());
        os.write(crlf.getBytes());
        os.write(zipBytes);
        os.write(crlf.getBytes());

        // End boundary
        os.write(("--" + boundary + "--" + crlf).getBytes());
        os.flush();
    }

    private void writeFormField(OutputStream os, String boundary, String name, String value) throws IOException {
        String crlf = "\r\n";
        os.write(("--" + boundary + crlf).getBytes());
        os.write(("Content-Disposition: form-data; name=\"" + name + "\"" + crlf).getBytes());
        os.write(crlf.getBytes());
        os.write(value.getBytes());
        os.write(crlf.getBytes());
    }

    private String readErrorStream(HttpURLConnection conn) {
        try {
            InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                return new String(errorStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}
        return "";
    }

    private List<Vulnerability> parseResponse(String responseBody) {
        try {
            // Try parsing as a direct array of vulnerabilities
            try {
                List<Vulnerability> vulns = mapper.readValue(responseBody,
                        new TypeReference<List<Vulnerability>>() {});
                if (vulns != null) return vulns;
            } catch (Exception e) {
                // Not a direct array, try as object
            }

            // Try parsing as object with vulnerabilities field
            JsonNode root = mapper.readTree(responseBody);

            // Check for various possible response structures
            JsonNode vulnsNode = root.get("vulnerabilities");
            if (vulnsNode == null) vulnsNode = root.get("Vulnerabilities");
            if (vulnsNode == null) vulnsNode = root.get("findings");
            if (vulnsNode == null) vulnsNode = root.get("Findings");
            if (vulnsNode == null) vulnsNode = root.get("results");
            if (vulnsNode == null) vulnsNode = root.get("Results");

            if (vulnsNode != null && vulnsNode.isArray()) {
                return mapper.readValue(vulnsNode.toString(),
                        new TypeReference<List<Vulnerability>>() {});
            }

            // If root is an array
            if (root.isArray()) {
                return mapper.readValue(responseBody,
                        new TypeReference<List<Vulnerability>>() {});
            }

            O360Logger.warn("Could not parse vulnerability array from response. Keys: " + root.fieldNames());
            // Return empty list and log the response
            O360Logger.info("Raw response (first 500 chars): " +
                    responseBody.substring(0, Math.min(500, responseBody.length())));
            return new ArrayList<>();
        } catch (Exception e) {
            O360Logger.error("Failed to parse scan response", e);
            return new ArrayList<>();
        }
    }

    private void displayResults(java.util.List<Vulnerability> vulnerabilities) {
        Display.getDefault().asyncExec(() -> {
            try {
                IWorkbenchPage page = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getActivePage();
                ReportView view = (ReportView) page.showView("o360sastplugin.views.ReportView");
                view.setResults(vulnerabilities, project, projectName, projectPath);
            } catch (Exception e) {
                O360Logger.error("Failed to display results", e);
            }
        });
    }

    private void showError(String message) {
        Display.getDefault().asyncExec(() -> {
            org.eclipse.jface.dialogs.MessageDialog.openError(
                Display.getDefault().getActiveShell(),
                "O360 SAST Scan",
                message
            );
        });
    }
}
