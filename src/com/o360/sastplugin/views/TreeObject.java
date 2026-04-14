package com.o360.sastplugin.views;

import com.o360.sastplugin.model.Vulnerability;

public class TreeObject {

    private final String name;
    private TreeParent parent;
    private Vulnerability vulnerability;

    public TreeObject(String name) {
        this.name = name;
    }

    public TreeObject(String name, Vulnerability vulnerability) {
        this.name = name;
        this.vulnerability = vulnerability;
    }

    public String getName() { return name; }

    public TreeParent getParent() { return parent; }
    public void setParent(TreeParent parent) { this.parent = parent; }

    public Vulnerability getVulnerability() { return vulnerability; }
    public void setVulnerability(Vulnerability v) { this.vulnerability = v; }

    @Override
    public String toString() { return name; }
}
