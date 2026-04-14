package com.o360.sastplugin.scanners;

import com.o360.sastplugin.util.O360Logger;
import org.eclipse.core.runtime.IProgressMonitor;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zipper {

    /**
     * Zips the project directory while respecting exclusion rules.
     * Returns the path to the created zip file.
     */
    public static File zipProject(String projectPath, IProgressMonitor monitor) throws IOException {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            throw new IOException("Project directory does not exist: " + projectPath);
        }

        File tempZip = File.createTempFile("o360scan_", ".zip");
        tempZip.deleteOnExit();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZip))) {
            zos.setLevel(6);
            Path basePath = projectDir.toPath();

            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (monitor != null && monitor.isCanceled()) return FileVisitResult.TERMINATE;

                    if (!dir.equals(basePath)) {
                        String folderName = dir.getFileName().toString();
                        if (FileExclusions.shouldExcludeFolder(folderName)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (monitor != null && monitor.isCanceled()) return FileVisitResult.TERMINATE;

                    File f = file.toFile();
                    if (FileExclusions.shouldExcludeFile(f)) return FileVisitResult.CONTINUE;

                    String entryName = basePath.relativize(file).toString().replace('\\', '/');
                    ZipEntry entry = new ZipEntry(entryName);
                    zos.putNextEntry(entry);

                    try (FileInputStream fis = new FileInputStream(f)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                    }
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    O360Logger.warn("Failed to visit file: " + file + " - " + exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return tempZip;
    }

    /**
     * Computes MD5 hashes of all source files in the project (respecting exclusion rules).
     * Returns a map of relative path to MD5 hash.
     */
    public static Map<String, String> computeFileHashes(String projectPath) {
        Map<String, String> hashes = new HashMap<>();
        File projectDir = new File(projectPath);
        if (!projectDir.exists()) return hashes;

        Path basePath = projectDir.toPath();
        try {
            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!dir.equals(basePath)) {
                        if (FileExclusions.shouldExcludeFolder(dir.getFileName().toString())) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    File f = file.toFile();
                    if (FileExclusions.shouldExcludeFile(f)) return FileVisitResult.CONTINUE;

                    try {
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        try (InputStream is = Files.newInputStream(file)) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = is.read(buffer)) > 0) {
                                md.update(buffer, 0, len);
                            }
                        }
                        byte[] digest = md.digest();
                        StringBuilder sb = new StringBuilder();
                        for (byte b : digest) {
                            sb.append(String.format("%02x", b));
                        }
                        String relPath = basePath.relativize(file).toString().replace('\\', '/');
                        hashes.put(relPath, sb.toString());
                    } catch (Exception e) {
                        O360Logger.warn("Failed to hash file: " + file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            O360Logger.error("Error walking project for hashes", e);
        }
        return hashes;
    }
}
