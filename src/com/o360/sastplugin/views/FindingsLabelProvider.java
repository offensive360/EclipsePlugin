package com.o360.sastplugin.views;

import com.o360.sastplugin.Activator;
import com.o360.sastplugin.model.Vulnerability;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import java.util.HashMap;
import java.util.Map;

public class FindingsLabelProvider extends LabelProvider {

    private final Map<String, Image> imageCache = new HashMap<>();

    @Override
    public String getText(Object element) {
        if (element instanceof TreeObject obj) {
            return obj.getName();
        }
        return element.toString();
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof TreeParent parent) {
            // Severity group node
            String name = parent.getName().toLowerCase();
            if (name.contains("critical")) return getIcon("icons/critical_bug.png");
            if (name.contains("high")) return getIcon("icons/high_bug.png");
            if (name.contains("medium")) return getIcon("icons/medium_bug.png");
            if (name.contains("low")) return getIcon("icons/low_bug.png");
            if (name.contains("info")) return getIcon("icons/info.png");
            return getIcon("icons/Offensive360.png");
        }
        if (element instanceof TreeObject obj) {
            Vulnerability v = obj.getVulnerability();
            if (v != null) {
                String sev = v.getSeverity() != null ? v.getSeverity().toLowerCase() : "";
                if (sev.contains("critical")) return getIcon("icons/critical_bug.png");
                if (sev.contains("high")) return getIcon("icons/high_bug.png");
                if (sev.contains("medium")) return getIcon("icons/medium_bug.png");
                if (sev.contains("low")) return getIcon("icons/low_bug.png");
                return getIcon("icons/info.png");
            }
        }
        return null;
    }

    private Image getIcon(String path) {
        Image img = imageCache.get(path);
        if (img == null || img.isDisposed()) {
            ImageDescriptor desc = Activator.getImageDescriptor(path);
            if (desc != null) {
                img = desc.createImage(false);
                if (img != null) {
                    imageCache.put(path, img);
                }
            }
        }
        return img;
    }

    @Override
    public void dispose() {
        for (Image img : imageCache.values()) {
            if (img != null && !img.isDisposed()) {
                img.dispose();
            }
        }
        imageCache.clear();
        super.dispose();
    }
}
