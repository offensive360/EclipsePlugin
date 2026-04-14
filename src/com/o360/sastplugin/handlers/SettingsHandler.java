package com.o360.sastplugin.handlers;

import com.o360.sastplugin.swt.SettingsDialog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;

public class SettingsHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        SettingsDialog dialog = new SettingsDialog(Display.getDefault().getActiveShell());
        dialog.open();
        return null;
    }
}
