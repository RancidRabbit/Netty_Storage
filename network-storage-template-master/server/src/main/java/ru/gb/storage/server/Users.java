package ru.gb.storage.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.sctp.SctpChannelOption;
import ru.gb.storage.commons.message.UserMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class Users {


    private Path path = Paths.get("C:\\Server\\USERS.json");



    public void start() throws IOException {
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
    }

    private List<UserMessage> users = new ArrayList<>();


    public List<UserMessage> getUsers() {
        return users;
    }

    public void setUsers(List<UserMessage> users) {
        this.users = users;
    }
}
