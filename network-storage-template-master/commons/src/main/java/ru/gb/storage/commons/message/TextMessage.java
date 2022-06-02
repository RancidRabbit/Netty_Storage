package ru.gb.storage.commons.message;

import ru.gb.storage.commons.message.Message;

public class TextMessage extends Message {
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "TextMessage{" +
                "text='" + text + '\'' +
                '}';
    }
}
