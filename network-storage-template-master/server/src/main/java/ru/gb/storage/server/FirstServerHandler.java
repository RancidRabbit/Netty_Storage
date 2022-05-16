package ru.gb.storage.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.resolver.dns.MultiDnsServerAddressStreamProvider;
import ru.gb.storage.commons.message.AuthMessage;
import ru.gb.storage.commons.message.CurrentPositionMessage;
import ru.gb.storage.commons.message.FileContentMessage;
import ru.gb.storage.commons.message.FileRequestMessage;
import ru.gb.storage.commons.message.FileUploadMessage;
import ru.gb.storage.commons.message.HelpMessage;
import ru.gb.storage.commons.message.Message;
import ru.gb.storage.commons.message.TextMessage;
import ru.gb.storage.commons.message.UserMessage;
import ru.gb.storage.commons.message.lsMessage;
import ru.gb.storage.commons.message.mkdirMessage;
import ru.gb.storage.commons.message.rmMessage;
import ru.gb.storage.server.Users;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;

public class FirstServerHandler extends SimpleChannelInboundHandler<Message> {
    private RandomAccessFile accessFile;
    private long counter = 0;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private File userFile;
    private Path root;
    private String currentPos;
    private boolean isAuth = false;


    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Новый клиент подключился");
        TextMessage answer = new TextMessage();
        answer.setText("Вызовите [help] для списка доступных команд");
        ctx.writeAndFlush(answer);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws IOException {
        if (msg instanceof UserMessage) {
            UserMessage message = (UserMessage) msg;
            userFile = new File(message.getLogin().concat(".json"));
            OBJECT_MAPPER.writeValue(new FileWriter(userFile), message);
            root = Files.createDirectories(Paths.get("C:\\Server\\Storage").resolve(Paths.get(message.getLogin())));
            message.setRootDir(root.toString());
            OBJECT_MAPPER.writeValue(new FileWriter(userFile), message);
            currentPos = root.toString();
            isAuth = true;
            CurrentPositionMessage position = new CurrentPositionMessage();
            position.setCurrentPos(currentPos);

            ctx.writeAndFlush(position);
        }

        if (msg instanceof HelpMessage) {
            TextMessage message = new TextMessage();
            if (isAuth == true) {
                authMsg(ctx, message);
            } else notAuthMsg(ctx, message);
        }
        if (msg instanceof TextMessage) {
            TextMessage message = (TextMessage) msg;
            System.out.println("incoming text message: " + message.getText());
            ctx.writeAndFlush(msg);
        }

        if (msg instanceof AuthMessage) {
            TextMessage failMessage = new TextMessage();
            if (!Files.exists(Paths.get("USERS.json"))) {
                failMessage.setText("Нет зарегестрированных пользователей");
                ctx.writeAndFlush(failMessage);
            } if (isAuth == true) {
               failMessage.setText("Вы уже авторизированы");
               ctx.writeAndFlush(failMessage);
            }
            else {
                AuthMessage message = (AuthMessage) msg;
                final String incomePassword = message.getPassword();
                final String incomeLogin = message.getLogin();
                final Users users = OBJECT_MAPPER.readValue(new File("USERS.json"), Users.class);
                TextMessage authMessage = new TextMessage();
                for (UserMessage user : users.getUsers()) {
                    if (incomeLogin.matches(user.getLogin()) && incomePassword.matches(user.getPassword())) {
                        authMessage.setText("Добро пожаловать, " + user.getLogin());
                        /* currentPos выдает NullPointerException при авторизации, при авторизации не присваивается root ? */
                        isAuth = true;
                        root = Paths.get(user.getRootDir());
                        currentPos = root.toString();
                        CurrentPositionMessage position = new CurrentPositionMessage();
                        position.setCurrentPos(currentPos);
                        ctx.writeAndFlush(position);
                        break;
                    } else authMessage.setText("Неверный логин или пароль");
                }
                ctx.writeAndFlush(authMessage);
            }
        }

        if (msg instanceof FileRequestMessage) {
            TextMessage message = new TextMessage();
            if (isAuth == false) {
                message.setText("Недостаточно прав для использования команды");
            } else {
                FileRequestMessage frm = (FileRequestMessage) msg;
                final String path = frm.getPath();
                Path file = Paths.get(path);
                final File toFile = root.resolve(file).toFile();
                accessFile = new RandomAccessFile(toFile, "r");
                sendFile(ctx);
            }
        }

        if (msg instanceof FileUploadMessage) {
            TextMessage message = new TextMessage();
            if (isAuth == false) {
                message.setText("Недостаточно прав для использования команды");
            } else {
                FileUploadMessage upload = (FileUploadMessage) msg;
                final String load = upload.getLoadFrom();
                final File loadFrom = Paths.get(load).toFile();
                accessFile = new RandomAccessFile(loadFrom, "r");
                sendFile(ctx);
            }
        }


        if (msg instanceof mkdirMessage) {
            TextMessage message = new TextMessage();
            if (isAuth == false) {
                message.setText("Недостаточно прав для использования команды");
            } else {
                mkdirMessage mkdirMessage = (mkdirMessage) msg;
                Path userDir = Paths.get(mkdirMessage.getPath());
                Path destination = root.resolve(userDir);
                if (!Files.exists(destination)) {
                    Files.createDirectories(destination);
                    message.setText("Создана директория: " + destination);
                } else {
                    message.setText("Директория уже существует!");
                }
            }
            ctx.writeAndFlush(message);
        }

        if (msg instanceof lsMessage) {
            TextMessage message = new TextMessage();
            if (isAuth == false) {
                message.setText("Недостаточно прав для использования команды");
                ctx.writeAndFlush(message);
            } else {
                lsMessage ls = (lsMessage) msg;
                final String dirName = ls.getDirName();
                System.out.println(dirName);
                if (dirName.compareToIgnoreCase("home") == 0) {
                    lsMode(ctx, root, message);
                } else {
                    final Path dest = root.resolve(Paths.get(dirName));
                    lsMode(ctx, dest, message);
                }
            }
        }

        if (msg instanceof rmMessage) {
            TextMessage message = new TextMessage();
            if (isAuth == false) {
                message.setText("Недостаточно прав для использования команды");
            } else {
                rmMessage rm = (rmMessage) msg;
                final String s = rm.getPath();
                final Path path = Paths.get(s);
                final Path rmDir = root.resolve(path);
                if (rmDir.toFile().isFile()) {
                    Files.delete(rmDir);
                    message.setText("Удален файл " + s);
                } else {
                    Files.walkFileTree(rmDir, new myFileDeleter());
                    message.setText("Удалена директория " + s);
                }
            }
            ctx.writeAndFlush(message);
        }
    }


    private void sendFile(ChannelHandlerContext ctx) throws IOException {
        if (accessFile != null) {
            final byte[] fileContent;
            final long available = accessFile.length() - accessFile.getFilePointer();
            if (available > 64 * 1024) {
                fileContent = new byte[64 * 1024];
            } else {
                fileContent = new byte[(int) available];
            }
            final FileContentMessage message = new FileContentMessage();
            message.setStartPosition(accessFile.getFilePointer()); /* Ставим метку для того, что бы клиент смог поставить часть файла в нужное место */
            accessFile.read(fileContent);
            message.setContent(fileContent);
            final boolean last = accessFile.getFilePointer() == accessFile.length();
            message.setLast(last);
            counter++;
            System.out.println("Пакетов отправлено + " + counter);
            ctx.channel().writeAndFlush(message).addListener((ChannelFutureListener) channelFuture -> {
                if (!last) {
                    FirstServerHandler.this.sendFile(ctx);
                }

            });
            if (last) {
                accessFile.close();
                accessFile = null;
            }
        }
    }

    public void lsMode(ChannelHandlerContext ctx, Path path, TextMessage message) throws IOException {
        final myFileIterator visitor = new myFileIterator();
        Files.walkFileTree(path, Collections.singleton(FileVisitOption.FOLLOW_LINKS),1, visitor);
        final List<String> fileName = visitor.getFileName();
        for (String s : fileName) {
            message.setText(s);
            ctx.writeAndFlush(message);
        }
    }

    public void notAuthMsg(ChannelHandlerContext ctx, TextMessage msg) {

        msg.setText("auth - Авторизация " + "\n" +
                "register - Регистрация" + "\n" +
                "exit - Выход из системы");
        ctx.writeAndFlush(msg);
    }


    public void authMsg(ChannelHandlerContext ctx, TextMessage msg) {
        msg.setText("ls [path] - посмотреть список файлов" + "\n" +
                "mkdir [path] - создать новую директорию" + "\n" +
                "rm [path] - удалить файл или директорию" + "\n" +
                "download [source] [target] - скачать файл с сервера" + "\n" +
                "upload [source] [target] - загрузить файл на сервер" + "\n" +
                "exit - выход");
        ctx.writeAndFlush(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws IOException {
        if (userFile != null){
            final UserMessage user = OBJECT_MAPPER.readValue(userFile, UserMessage.class);
            /*OBJECT_MAPPER.writeValue(new FileWriter(userFile), user);*/
            final Users u = OBJECT_MAPPER.readValue(new File("USERS.json"), Users.class);
            u.getUsers().add(user);
            OBJECT_MAPPER.writeValue(new FileWriter("USERS.json"), u);
            Files.delete(userFile.toPath());
        }
        System.out.println("Клиент отключился");
        if (accessFile != null) {
            accessFile.close();
        }
    }
}