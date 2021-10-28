package com.hatechnology.apps.core_messaging;

public class SyncFileBase64 {
    private String name;
    private String version;
    private String sourcePath;
    private String destinationPath;
    private String type;
    private String encodedContent;
    private String temporaryPath;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEncodedContent() {
        return encodedContent;
    }

    public void setEncodedContent(String encodedContent) {
        this.encodedContent = encodedContent;
    }

    public String getTemporaryPath() {
        return temporaryPath;
    }

    public void setTemporaryPath(String temporaryPath) {
        this.temporaryPath = temporaryPath;
    }
}
