package com.o360.sastplugin.views;

import java.util.ArrayList;
import java.util.List;

public class TreeParent extends TreeObject {

    private final List<TreeObject> children = new ArrayList<>();

    public TreeParent(String name) {
        super(name);
    }

    public void addChild(TreeObject child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(TreeObject child) {
        children.remove(child);
        child.setParent(null);
    }

    public TreeObject[] getChildren() {
        return children.toArray(new TreeObject[0]);
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public int getChildCount() {
        return children.size();
    }
}
