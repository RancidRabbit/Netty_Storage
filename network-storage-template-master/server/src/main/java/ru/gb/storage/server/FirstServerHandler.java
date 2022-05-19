package ru.gb.storage.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ru.gb.storage.commons.Constant;
import ru.gb.storage.commons.message.*;

import java.io.*;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;

public class FirstServerHandler extends SimpleChannelInboundHandler<Message> {
    private RandomAccessFile accessFile;
    private long counter = 0;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private Path root;
    private String currentPos;
    private boolean isAuth = false;
    private File users = new File("USERS.json");


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
            UserMessage userMessage = (UserMessage) msg;
            final String incomeLogin = userMessage.getLogin();
            TextMessage regMsg = new TextMessage();
            if (isFileEmpty(users)) {
                regUser(ctx, userMessage);
                regMsg.setText("Пользователь " + incomeLogin + " успешно зарегестрирован");
            } else {
                final Users usersList = OBJECT_MAPPER.readValue(users, Users.class);
                final long count = usersList.getUsers().
                        stream().
                        map(UserMessage::getLogin).
                        filter(incomeLogin::equals).
                        count();

                if (count == 0) {
                    regUser(ctx, userMessage);
                    regMsg.setText("Пользователь " + incomeLogin + " успешно зарегестрирован");
                } else regMsg.setText("Пользователь c ником " + incomeLogin + " уже существует!");
            }
            ctx.writeAndFlush(regMsg);
        }


        if (msg instanceof HelpMessage) {
            TextMessage message = new TextMessage();
            if (isAuth == true) {
                authMsg(ctx, message);
            } else notAuthMsg(ctx, message);
        }
        if (msg instanceof TextMessage) {
            TextMessage message = (TextMessage) msg;
            ctx.writeAndFlush(msg);
        }

        if (msg instanceof AuthMessage) {
            TextMessage failMessage = new TextMessage();
            if (isFileEmpty(users)) {
                failMessage.setText("Нет зарегестрированных пользователей");
                ctx.writeAndFlush(failMessage);
            }
            if (isAuth == true) {
                failMessage.setText("Вы уже авторизированы");
                ctx.writeAndFlush(failMessage);
            } else {
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
                try {
                    FileRequestMessage frm = (FileRequestMessage) msg;
                    final String path = frm.getPath();
                    final myFileUploader visitor = new myFileUploader(path);
                    Files.walkFileTree(root, visitor);
                    accessFile = new RandomAccessFile(String.valueOf(visitor.getDownloadedFilePath()), "r");
                    sendFile(ctx);
                } catch (FileNotFoundException e) {
                    message.setText(Constant.ANSI_RED + "Данный файл не обнаружен в вашей директории, проверьте правильность введенного значения" + Constant.ANSI_RESET);
                    ctx.writeAndFlush(message);
                }
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
                deleteCMD(rmDir, s, message);
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
        try {
            final myFileIterator visitor = new myFileIterator();
            Files.walkFileTree(path, Collections.singleton(FileVisitOption.FOLLOW_LINKS), 1, visitor);
            final List<String> fileName = visitor.getFileName();
            for (String s : fileName) {
                message.setText(s);
                ctx.writeAndFlush(message);
            }
        } catch (NoSuchFileException e) {
            message.setText(Constant.ANSI_RED + "Такой папки не обнаружено, проверьте правильность введенного значения" + Constant.ANSI_RESET);
            ctx.writeAndFlush(message);
        }
    }


    public void regUser(ChannelHandlerContext ctx, UserMessage userMessage) throws IOException {
        root = Files.createDirectories(Paths.get("C:\\Server\\Storage").resolve(Paths.get(userMessage.getLogin())));
        currentPos = root.toString();
        isAuth = true;
        userMessage.setRootDir(root.toString());
        CurrentPositionMessage position = new CurrentPositionMessage();
        position.setCurrentPos(currentPos);
        final Users u = OBJECT_MAPPER.readValue(new File("USERS.json"), Users.class);
        u.getUsers().add(userMessage);
        OBJECT_MAPPER.writeValue(new FileWriter("USERS.json"), u);
        ctx.writeAndFlush(position);
    }

    public void deleteCMD(Path rmDir, String s, TextMessage message) throws IOException {
        try {
            if (rmDir.toFile().isDirectory()) {
                Files.walkFileTree(rmDir, new myDirDeleter());
                message.setText("Удалена директория " + s);
            } else {
                Files.walkFileTree(root, new myFileDeleter(s));
                message.setText("Удален файл " + s);
            }
        } catch (NoSuchFileException e) {
            message.setText(Constant.ANSI_RED + "Такой папки или файла не обнаружено, проверьте правильность введенного значения" + Constant.ANSI_RESET);
        }

    }

    public void notAuthMsg(ChannelHandlerContext ctx, TextMessage msg) {

        msg.setText("auth - Авторизация " + "\n" +
                "register - Регистрация" + "\n" +
                "exit - Выход из системы");
        ctx.writeAndFlush(msg);
    }


    public void authMsg(ChannelHandlerContext ctx, TextMessage msg) {
        msg.setText("ls [home] для просмотра корневой папки или ls [название папки в вашей директории] - посмотреть список файлов" + "\n" +
                "mkdir [название папки] - создать новую директорию" + "\n" +
                "rm [название файла/директории] - удалить файл или директорию" + "\n" +
                "download [название файла с расширением] [полный путь для скачивания файла с расширением] - скачать файл с сервера" + "\n" +
                "upload [полный путь загружаемого файла с расширением] [название файла с расширением] - загрузить файл на сервер" + "\n" +
                "exit - выход" + "\n" +
                "Обратите внимание, что наличие пробелов не допускается в названиях файлов и директорий!");
        ctx.writeAndFlush(msg);
    }

    public boolean isFileEmpty(File file) {
        return file.length() == 0;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws IOException {
        System.out.println("Клиент отключился");
        if (accessFile != null) {
            accessFile.close();
        }
    }
}