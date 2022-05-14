package com.example.bioencryption.models;

import android.net.Uri;

import java.io.File;

public class FileModel {
    private long size;

    public String getBase64id() {
        return base64id;
    }

    public void setBase64id(String base64id) {
        this.base64id = base64id;
    }

    private String base64id;
    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    private Uri uri;
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    private File file;
    public FileModel(long size, String name, String path, String timestamp, String type) {
        this.size = size;
        this.name = name;
        this.path = path;
        this.timestamp = timestamp;
        this.type = type;
    }

    private String name;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    private String path;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    private String timestamp;

    @Override
    public String toString() {
        return "FileModel{" +
                "size=" + size +
                ", name='" + name + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    public FileModel(long size, String name, String type) {
        this.size = size;
        this.name = name;
        this.type = type;
    }

    public FileModel() {
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    private String type;
}
