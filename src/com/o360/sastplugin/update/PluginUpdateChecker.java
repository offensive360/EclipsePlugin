package com.o360.sastplugin.update;

import com.o360.sastplugin.Activator;
import com.o360.sastplugin.util.O360Logger;

import org.eclipse.swt.widgets.Display;
import org.eclipse.jface.dialogs.MessageDialog;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PluginUpdateChecker {

    private static final String GITHUB_RELEASES_URL =
            "https://api.github.com/repos/offensive360/EclipsePlugin/releases/latest";
    private static final long CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L; // 24 hours

    /**
     * Checks for plugin updates asynchronously. Throttled to once per 24 hours.
     */
    public static void checkForUpdatesAsync() {
        new Thread(() -> {
            try {
                long lastCheck = Activator.getDefault().getPreferenceStore()
                        .getLong(Activator.PREF_LAST_UPDATE_CHECK);
                long now = System.currentTimeMillis();

                if (now - lastCheck < CHECK_INTERVAL_MS) {
                    return; // Throttled
                }

                Activator.getDefault().getPreferenceStore()
                        .setValue(Activator.PREF_LAST_UPDATE_CHECK, now);

                HttpURLConnection conn = (HttpURLConnection) new URL(GITHUB_RELEASES_URL).openConnection();
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("User-Agent", "O360-Eclipse-Plugin/" + Activator.PLUGIN_VERSION);

                int code = conn.getResponseCode();
                if (code != 200) return;

                String body;
                try (InputStream is = conn.getInputStream()) {
                    body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }

                // Simple parse for "tag_name" field
                String latestVersion = extractJsonField(body, "tag_name");
                if (latestVersion == null) return;

                // Remove 'v' prefix if present
                latestVersion = latestVersion.replace("v", "").replace("V", "");
                String currentVersion = Activator.PLUGIN_VERSION;

                if (isNewerVersion(latestVersion, currentVersion)) {
                    String htmlUrl = extractJsonField(body, "html_url");
                    String downloadUrl = htmlUrl != null ? htmlUrl : "https://github.com/offensive360/EclipsePlugin/releases";
                    String finalVersion = latestVersion;
                    String finalUrl = downloadUrl;

                    Display.getDefault().asyncExec(() -> {
                        boolean update = MessageDialog.openQuestion(
                                Display.getDefault().getActiveShell(),
                                "O360 Plugin Update Available",
                                "A new version of the O360 SAST plugin is available.\n\n" +
                                "Current version: " + currentVersion + "\n" +
                                "Latest version: " + finalVersion + "\n\n" +
                                "Would you like to open the download page?");
                        if (update) {
                            try {
                                org.eclipse.swt.program.Program.launch(finalUrl);
                            } catch (Exception e) {
                                O360Logger.warn("Failed to open update URL");
                            }
                        }
                    });
                }
            } catch (Exception e) {
                // Silent failure for update check
                O360Logger.info("Update check skipped: " + e.getMessage());
            }
        }, "O360-UpdateChecker").start();
    }

    private static String extractJsonField(String json, String field) {
        String search = "\"" + field + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        idx = json.indexOf(":", idx);
        if (idx < 0) return null;
        idx = json.indexOf("\"", idx + 1);
        if (idx < 0) return null;
        int end = json.indexOf("\"", idx + 1);
        if (end < 0) return null;
        return json.substring(idx + 1, end);
    }

    private static boolean isNewerVersion(String latest, String current) {
        try {
            String[] latestParts = latest.split("\\.");
            String[] currentParts = current.split("\\.");
            int maxLen = Math.max(latestParts.length, currentParts.length);

            for (int i = 0; i < maxLen; i++) {
                int l = i < latestParts.length ? Integer.parseInt(latestParts[i].replaceAll("[^0-9]", "")) : 0;
                int c = i < currentParts.length ? Integer.parseInt(currentParts[i].replaceAll("[^0-9]", "")) : 0;
                if (l > c) return true;
                if (l < c) return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
