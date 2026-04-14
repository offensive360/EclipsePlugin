package com.o360.sastplugin.scanners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.o360.sastplugin.Activator;
import com.o360.sastplugin.model.Vulnerability;
import com.o360.sastplugin.util.O360Logger;

import java.io.File;
import java.util.*;

public class ScanCache {

    private static final String CACHE_DIR = ".SASTO360";
    private static final String CACHE_FILE = "lastScanResults.json";
    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static class CacheEntry {
        public String pluginVersion;
        public Map<String, String> fileHashes;
        public List<Vulnerability> vulnerabilities;

        public CacheEntry() {}
    }

    /**
     * Checks if cached results are still valid (same file hashes, same plugin version).
     */
    public static List<Vulnerability> getCachedResults(String projectPath, Map<String, String> currentHashes) {
        try {
            File cacheFile = getCacheFile(projectPath);
            if (!cacheFile.exists()) return null;

            CacheEntry entry = mapper.readValue(cacheFile, CacheEntry.class);

            // Invalidate on plugin version change
            if (!Activator.PLUGIN_VERSION.equals(entry.pluginVersion)) {
                O360Logger.info("Cache invalidated: plugin version changed");
                return null;
            }

            // Compare hashes
            if (entry.fileHashes == null || !entry.fileHashes.equals(currentHashes)) {
                O360Logger.info("Cache invalidated: file hashes changed");
                return null;
            }

            O360Logger.info("Using cached scan results (" + entry.vulnerabilities.size() + " findings)");
            return entry.vulnerabilities;
        } catch (Exception e) {
            O360Logger.warn("Failed to read cache: " + e.getMessage());
            return null;
        }
    }

    /**
     * Saves scan results to cache.
     */
    public static void saveCache(String projectPath, Map<String, String> fileHashes, List<Vulnerability> vulnerabilities) {
        try {
            File cacheDir = new File(projectPath, CACHE_DIR);
            if (!cacheDir.exists()) cacheDir.mkdirs();

            CacheEntry entry = new CacheEntry();
            entry.pluginVersion = Activator.PLUGIN_VERSION;
            entry.fileHashes = fileHashes;
            entry.vulnerabilities = vulnerabilities;

            mapper.writeValue(getCacheFile(projectPath), entry);
            O360Logger.info("Scan results cached successfully");
        } catch (Exception e) {
            O360Logger.warn("Failed to save cache: " + e.getMessage());
        }
    }

    private static File getCacheFile(String projectPath) {
        return new File(new File(projectPath, CACHE_DIR), CACHE_FILE);
    }
}
