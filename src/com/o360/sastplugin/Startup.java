package com.o360.sastplugin;

import org.eclipse.ui.IStartup;
import org.eclipse.swt.widgets.Display;

public class Startup implements IStartup {

    @Override
    public void earlyStartup() {
        Display.getDefault().asyncExec(() -> {
            // Ensure activator is loaded
            Activator.getDefault();
        });
    }
}
