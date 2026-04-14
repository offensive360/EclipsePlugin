package com.o360.sastplugin.scanners;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class FileExclusions {

    public static final long MAX_FILE_SIZE = 50L * 1024L * 1024L; // 50 MB

    private static final Set<String> EXCLUDE_EXTS = new HashSet<>(Arrays.asList(
        ".dll", ".exe", ".pdb", ".obj", ".bin", ".class", ".jar", ".war", ".ear",
        ".zip", ".tar", ".gz", ".rar", ".7z", ".iso", ".img",
        ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
        ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".ico", ".svg",
        ".mp3", ".mp4", ".avi", ".mov", ".wmv", ".flv",
        ".woff", ".woff2", ".ttf", ".eot", ".otf",
        ".map", ".min.js", ".min.css",
        ".sln", ".csproj", ".vbproj", ".fsproj", ".suo", ".user",
        ".vsix", ".nupkg", ".snk", ".pfx", ".cer", ".p12"
    ));

    private static final Set<String> EXCLUDE_FOLDERS = new HashSet<>(Arrays.asList(
        "bin", "obj", "node_modules", ".git", ".svn", ".hg", ".vs", ".idea", ".vscode",
        "packages", "testresults", "debug", "release", "x64", "x86", "arm", "arm64",
        ".nuget", "__pycache__", ".tox", "venv", ".env", "dist", "build", "target",
        "out", ".gradle", ".sasto360", "backup"
    ));

    private static final Pattern BACKUP_PATTERN = Pattern.compile(
        "^(backup|backups|backup\\d+)$", Pattern.CASE_INSENSITIVE
    );

    public static boolean shouldExcludeFile(File file) {
        if (file == null || !file.isFile()) return true;
        if (file.length() > MAX_FILE_SIZE) return true;

        String name = file.getName().toLowerCase();

        // Check compound extensions first (.min.js, .min.css)
        if (name.endsWith(".min.js") || name.endsWith(".min.css")) return true;

        // Check standard extensions
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex >= 0) {
            String ext = name.substring(dotIndex);
            if (EXCLUDE_EXTS.contains(ext)) return true;
        }

        return false;
    }

    public static boolean shouldExcludeFolder(File folder) {
        if (folder == null) return true;
        String name = folder.getName().toLowerCase();
        if (EXCLUDE_FOLDERS.contains(name)) return true;
        if (BACKUP_PATTERN.matcher(name).matches()) return true;
        return false;
    }

    public static boolean shouldExcludeFolder(String folderName) {
        if (folderName == null) return true;
        String name = folderName.toLowerCase();
        if (EXCLUDE_FOLDERS.contains(name)) return true;
        if (BACKUP_PATTERN.matcher(name).matches()) return true;
        return false;
    }
}
