package com.o360.sastplugin.util;

import com.o360.sastplugin.Activator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class O360Logger {

    private static ILog getLog() {
        Activator activator = Activator.getDefault();
        if (activator != null) {
            return activator.getLog();
        }
        return null;
    }

    public static void info(String message) {
        ILog log = getLog();
        if (log != null) {
            log.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, message));
        } else {
            System.out.println("[O360 INFO] " + message);
        }
    }

    public static void warn(String message) {
        ILog log = getLog();
        if (log != null) {
            log.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, message));
        } else {
            System.out.println("[O360 WARN] " + message);
        }
    }

    public static void error(String message, Throwable t) {
        ILog log = getLog();
        if (log != null) {
            log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, t));
        } else {
            System.err.println("[O360 ERROR] " + message);
            if (t != null) t.printStackTrace();
        }
    }

    public static void error(String message) {
        error(message, null);
    }
}
