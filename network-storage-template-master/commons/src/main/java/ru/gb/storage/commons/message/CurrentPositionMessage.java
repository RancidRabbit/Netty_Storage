package ru.gb.storage.commons.message;

public class CurrentPositionMessage extends Message {

    private String currentPos;

    public String getCurrentPos() {
        return currentPos;
    }

    public void setCurrentPos(String currentPos) {
        this.currentPos = currentPos;
    }
}
