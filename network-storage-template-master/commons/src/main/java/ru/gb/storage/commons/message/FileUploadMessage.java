package ru.gb.storage.commons.message;

public class FileUploadMessage extends Message {

    private String loadFrom;

    public String getLoadFrom() {
        return loadFrom;
    }

    public void setLoadFrom(String loadFrom) {
        this.loadFrom = loadFrom;
    }
}
