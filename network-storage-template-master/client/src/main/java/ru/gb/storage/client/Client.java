package ru.gb.storage.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import ru.gb.storage.commons.Constant;
import ru.gb.storage.commons.handler.JsonDecoder;
import ru.gb.storage.commons.handler.JsonEncoder;
import ru.gb.storage.commons.message.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Scanner;

public class Client {

    private Channel channel;

    public static void main(String[] args) {
        new Client().start();
    }

    public void start() throws NullPointerException {
        final NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {

                            ch.pipeline().addLast(
                                    //максимальный размер сообщения равен 1024*1024 байт, в начале сообщения пдля хранения длины зарезервировано 3 байта,
                                    //которые отбросятся после получения всего сообщения и передачи его дальше по цепочке
                                    new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 3, 0, 3),
                                    //Перед отправкой добавляет в начало сообщение 3 байта с длиной сообщения
                                    new LengthFieldPrepender(3),
                                    new JsonDecoder(),
                                    new JsonEncoder(),
                                    new SimpleChannelInboundHandler<Message>()  {
                                        Scanner sc = new Scanner(System.in);
                                        private String download;
                                        private String userPosition;
                                        private String upload;

                                        @Override
                                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                            new Thread(() -> {
                                                while (true) {
                                                    final String s = sc.nextLine();
                                                    switch (s) {
                                                        case "help":
                                                            HelpMessage helpMessage = new HelpMessage();
                                                            ctx.writeAndFlush(helpMessage);
                                                            break;
                                                        case "auth":
                                                            authUser(ctx);
                                                            break;
                                                        case "register":
                                                            try {
                                                                regUser(ctx);
                                                            } catch (IOException e) {
                                                                e.printStackTrace();
                                                            }
                                                            break;
                                                        case "exit":
                                                            try {
                                                                group.shutdownGracefully();
                                                                channel.closeFuture().sync();
                                                                System.exit(0);
                                                            } catch (InterruptedException e) {
                                                                e.printStackTrace();
                                                            }
                                                            break;
                                                        default:
                                                            if (s.startsWith("mkdir")) {
                                                                try {
                                                                    mkdirCMD(ctx, s);
                                                                    break;
                                                                } catch (ArrayIndexOutOfBoundsException e) {
                                                                    TextMessage text = new TextMessage();
                                                                    text.setText(Constant.ANSI_RED + "Команда создания директории содержит следующий вид: " + "\n" +
                                                                            "mkdir [название папки]" + Constant.ANSI_RESET);
                                                                    ctx.writeAndFlush(text);
                                                                    break;
                                                                }

                                                            }
                                                            if (s.startsWith("ls")) {
                                                                TextMessage text = new TextMessage();
                                                                try {
                                                                    ls(ctx, s);
                                                                    break;
                                                                } catch (ArrayIndexOutOfBoundsException e) {

                                                                    text.setText(Constant.ANSI_RED + "Команда просмотра файлов в директории содержит следующий вид: " + "\n" +
                                                                            "ls [Home] для просмотра корневой папки или ls [название папки в вашей директории]" + Constant.ANSI_RESET);
                                                                    ctx.writeAndFlush(text);
                                                                    break;
                                                                }
                                                            }
                                                            if (s.startsWith("rm")) {
                                                                try {
                                                                    deleteCMD(ctx, s);
                                                                    break;
                                                                } catch (ArrayIndexOutOfBoundsException e) {
                                                                    TextMessage text = new TextMessage();
                                                                    text.setText(Constant.ANSI_RED + "Команда удаления содержит следующий вид: " + "\n" +
                                                                            "rm [название файла/директории]" + Constant.ANSI_RESET);
                                                                    ctx.writeAndFlush(text);
                                                                    break;
                                                                }
                                                            }
                                                            if (s.startsWith("download")) {
                                                                try {
                                                                    downloadCMD(ctx, s);
                                                                    break;
                                                                } catch (ArrayIndexOutOfBoundsException e) {
                                                                    TextMessage text = new TextMessage();
                                                                    text.setText(Constant.ANSI_RED + "Команда загрузки файла c сервера содержит следующий вид: " + "\n" +
                                                                            "download [book.txt] [C:\\Users\\newBook.txt]" + Constant.ANSI_RESET);
                                                                    ctx.writeAndFlush(text);
                                                                    break;
                                                                } catch (NullPointerException e) {
                                                                    TextMessage text = new TextMessage();
                                                                    text.setText(Constant.ANSI_RED + "Директория, которую вы ввели не существует! " + Constant.ANSI_RESET);
                                                                    ctx.writeAndFlush(text);
                                                                    break;
                                                                }
                                                            }
                                                            if (s.startsWith("upload")) {
                                                                try {
                                                                    uploadCMD(ctx, s);
                                                                    break;
                                                                } catch (ArrayIndexOutOfBoundsException e) {
                                                                    TextMessage text = new TextMessage();
                                                                    text.setText(Constant.ANSI_RED + "Команда загрузки файла на сервер содержит следующий вид: " + "\n" +
                                                                            "upload [C:\\Users\\book.txt] [newBook.txt]" + Constant.ANSI_RESET);
                                                                    ctx.writeAndFlush(text);
                                                                    break;
                                                                } catch (FileNotFoundException e) {
                                                                    TextMessage text = new TextMessage();
                                                                    text.setText(Constant.ANSI_RED + "Указанный файл для загрузки не найден или указан неверный формат директории" + Constant.ANSI_RESET);
                                                                    ctx.writeAndFlush(text);
                                                                    break;
                                                                }
                                                            } else {
                                                                System.out.println("Такой команды не существует. Вызовите [help] для списка доступных команд");
                                                            }

                                                    }
                                                }
                                            }).start();


                                        }

                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
                                            /* Передавать File через FileContentMessage ? */
                                            if (msg instanceof FileContentMessage && download != null) {
                                                downloadFile(ctx, msg, download);
                                            }
                                            if (msg instanceof FileContentMessage) {
                                                downloadFile(ctx, msg, upload);
                                            }

                                            if (msg instanceof TextMessage) {
                                                readText(msg);
                                            }
                                            if (msg instanceof CurrentPositionMessage) {
                                                CurrentPositionMessage position = (CurrentPositionMessage) msg;
                                                userPosition = position.getCurrentPos();
                                            }

                                        }

                                        public void downloadFile(ChannelHandlerContext ctx, Message msg, String to) {

                                            FileContentMessage fcm = (FileContentMessage) msg;
                                            try (final RandomAccessFile accessFile = new RandomAccessFile(to, "rw")) {
                                                accessFile.seek(fcm.getStartPosition());
                                                accessFile.write(fcm.getContent());
                                                if (fcm.isLast()) {
                                                    download = null;
                                                    System.out.println("Загрузка успешно завершена");
                                                }
                                            } catch (IOException e ) {
                                                System.out.println(Constant.ANSI_RED + "Директория для скачивания не найдена. Проверьте правильность введенных данных"
                                                        + Constant.ANSI_RESET);

                                            } catch (NullPointerException e) {

                                            }
                                        }

                                        public void regUser(ChannelHandlerContext ctx) throws IOException {
                                            System.out.println("Login");
                                            final String login = sc.nextLine();
                                            System.out.println("Password");
                                            final String password = sc.nextLine();
                                            UserMessage userMessage = new UserMessage();
                                            userMessage.setLogin(login);
                                            userMessage.setPassword(password);
                                            ctx.writeAndFlush(userMessage);
                                        }

                                        public void authUser(ChannelHandlerContext ctx) {
                                            System.out.println("Login");
                                            final String login = sc.nextLine();
                                            System.out.println("Password");
                                            final String password = sc.nextLine();
                                            AuthMessage authMessage = new AuthMessage();
                                            authMessage.setLogin(login);
                                            authMessage.setPassword(password);
                                            ctx.writeAndFlush(authMessage);
                                        }

                                        public void readText(Message msg) {
                                            TextMessage message = (TextMessage) msg;
                                            System.out.println(message.getText());
                                        }

                                        public void ls(ChannelHandlerContext ctx, String s) throws ArrayIndexOutOfBoundsException {
                                            final String[] s1 = s.split(" ");
                                            final String dirName = s1[1];
                                            lsMessage ls = new lsMessage();
                                            ls.setDirName(dirName);
                                            ctx.writeAndFlush(ls);
                                        }

                                        public void deleteCMD(ChannelHandlerContext ctx, String s) throws ArrayIndexOutOfBoundsException {
                                            final String[] s2 = s.split(" ");
                                            final String rmPath = s2[1];
                                            rmMessage rm = new rmMessage();
                                            rm.setPath(rmPath);
                                            ctx.writeAndFlush(rm);
                                        }

                                        public void mkdirCMD(ChannelHandlerContext ctx, String s) throws ArrayIndexOutOfBoundsException {
                                            final String[] s1 = s.split(" ");
                                            final String dirPath = s1[1];
                                            mkdirMessage mkdirMessage = new mkdirMessage();
                                            mkdirMessage.setPath(dirPath);
                                            ctx.writeAndFlush(mkdirMessage);
                                        }

                                        public void downloadCMD(ChannelHandlerContext ctx, String s) throws ArrayIndexOutOfBoundsException, NullPointerException {
                                            final String[] split = s.split(" ");
                                            final String file = split[1];
                                            if (Paths.get(split[2]).getParent().toFile().isDirectory()) {
                                                download = split[2];
                                                FileRequestMessage frm = new FileRequestMessage();
                                                frm.setPath(file);
                                                ctx.writeAndFlush(frm);
                                            } else throw new NullPointerException();

                                        }


                                        public void uploadCMD(ChannelHandlerContext ctx, String s) throws ArrayIndexOutOfBoundsException, FileNotFoundException {
                                            final String[] strings = s.split(" ");
                                            final String from = strings[1];
                                            if (Paths.get(from).toFile().isFile()) {
                                                final String fileName = strings[2];
                                                upload = userPosition.concat("\\").concat(fileName);
                                                FileUploadMessage upload = new FileUploadMessage();
                                                upload.setLoadFrom(from);
                                                ctx.writeAndFlush(upload);
                                            } else throw new FileNotFoundException();
                                        }
                                    }
                            );
                        }
                    });

            System.out.println("Вы успешно подключились на сервер");
            channel = bootstrap.connect("localhost", 9090).sync().channel();


            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }
}
