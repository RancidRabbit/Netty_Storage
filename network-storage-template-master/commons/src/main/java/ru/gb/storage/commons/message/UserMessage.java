package ru.gb.storage.commons.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class UserMessage extends Message {

    private String login;

    private String password;

    private String rootDir;

    private String auth;

    public UserMessage(String login, String password, String auth) {
        this.login = login;
        this.password = password;
        this.auth = auth;

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

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }
    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }
}
