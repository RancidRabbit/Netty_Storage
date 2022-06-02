package ru.gb.storage.commons.message;

public class AuthMessage extends Message {
    private String login;
    private String password;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }



    @Override
    public String toString() {
        return "AuthMessage{" +
                "login='" + login + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
