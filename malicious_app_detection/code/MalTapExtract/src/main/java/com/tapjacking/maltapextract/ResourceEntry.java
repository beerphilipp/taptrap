package com.tapjacking.maltapextract;

/**
 * Represents an entry for a resource.
 */
public class ResourceEntry {

    public enum ResourceType {
        ANIM, INTERPOLATOR
    }

    private ResourceType type;
    private String file;
    private int version;
    private String hash;
    private String xml;

    public ResourceEntry(ResourceType type, String file, int version, String hash, String xml) {
        this.type = type;
        this.file = file;
        this.version = version;
        this.hash = hash;
        this.xml = xml;
    }

    public ResourceType getType() {
        return type;
    }

    public String getFile() {
        return file;
    }

    public int getVersion() {
        return version;
    }

    public String getHash() {
        return hash;
    }

    public String getXml() {
        return xml;
    }
}
