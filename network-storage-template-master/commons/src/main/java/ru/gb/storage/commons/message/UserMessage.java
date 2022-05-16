package ru.gb.storage.commons.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class UserMessage extends Message {

    private String login;

    private String password;

    private String rootDir;


    public UserMessage(String login, String password) {
        this.login = login;
        this.password = password;

    }

    public UserMessage() {
    }



    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }
}
