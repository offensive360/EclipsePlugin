package com.o360.sastplugin.views;

import org.eclipse.jface.viewers.ITreeContentProvider;

public class FindingsContentProvider implements ITreeContentProvider {

    @Override
    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof TreeParent parent) {
            return parent.getChildren();
        }
        return new Object[0];
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof TreeParent parent) {
            return parent.getChildren();
        }
        return new Object[0];
    }

    @Override
    public Object getParent(Object element) {
        if (element instanceof TreeObject obj) {
            return obj.getParent();
        }
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof TreeParent parent) {
            return parent.hasChildren();
        }
        return false;
    }
}
