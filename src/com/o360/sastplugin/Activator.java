package com.o360.sastplugin;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "com.offensive360.sastplugin";
    public static final String PLUGIN_VERSION = "2.2.0";

    public static final String PREF_SERVER_URL = "o360.serverUrl";
    public static final String PREF_ACCESS_TOKEN = "o360.accessToken";
    public static final String PREF_ALLOW_SELF_SIGNED = "o360.allowSelfSigned";
    public static final String PREF_LAST_UPDATE_CHECK = "o360.lastUpdateCheck";

    private static Activator plugin;

    public Activator() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    public static Activator getDefault() {
        return plugin;
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public String getServerUrl() {
        return getPreferenceStore().getString(PREF_SERVER_URL);
    }

    public String getAccessToken() {
        return getPreferenceStore().getString(PREF_ACCESS_TOKEN);
    }

    public boolean isAllowSelfSigned() {
        return getPreferenceStore().getBoolean(PREF_ALLOW_SELF_SIGNED);
    }
}
