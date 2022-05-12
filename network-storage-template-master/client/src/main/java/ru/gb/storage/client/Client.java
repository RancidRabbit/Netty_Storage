package ru.gb.storage.client;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

public class Client {

   private Channel channel;

    public static void main(String[] args) {
        new Client().start();
    }

    public void start() {
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
                                    new SimpleChannelInboundHandler<Message>() {
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
                                                        case "ls" :
                                                            ls(ctx);
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
                                                                final String[] s1 = s.split(" ");
                                                                final String dirPath = s1[1];
                                                                mkdirMessage mkdirMessage = new mkdirMessage();
                                                                mkdirMessage.setPath(dirPath);
                                                                ctx.writeAndFlush(mkdirMessage);
                                                            }
                                                             if (s.startsWith("rm")) {
                                                                 final String[] s2 = s.split(" ");
                                                                 final String rmPath = s2[1];
                                                                rmMessage rm = new rmMessage();
                                                                rm.setPath(rmPath);
                                                                ctx.writeAndFlush(rm);
                                                             }
                                                             if (s.startsWith("download")) {
                                                                 final String[] split = s.split(" ");
                                                                 final String file = split[1];
                                                                 download = split[2];
                                                                 FileRequestMessage frm = new FileRequestMessage();
                                                                 frm.setPath(file);
                                                                 ctx.writeAndFlush(frm);
                                                             }
                                                             if (s.startsWith("upload")) {
                                                                 final String[] strings = s.split(" ");
                                                                 final String from = strings[1];
                                                                 final String fileName = strings[2];
                                                                 upload = userPosition.concat("\\").concat(fileName);
                                                                 FileUploadMessage upload = new FileUploadMessage();
                                                                 upload.setLoadFrom(from);
                                                                 ctx.writeAndFlush(upload);
                                                             }
                                                             else  {
                                                                System.out.println("Not such command");
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
                                                downloadFile(ctx,msg, upload);
                                            }

                                            if (msg instanceof TextMessage) {
                                                readText(ctx, msg);
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
                                                }
                                            } catch (IOException e) {
                                                System.out.println(Constant.ANSI_RED + "Укажите действующую директорию для копирования и название для файла" + "\n" +
                                                                   "C:\\MyFiles\\Read.txt" + Constant.ANSI_RESET);

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
                                            userMessage.setAuth("Y");
                                            Path userFile = Paths.get(userMessage.getLogin().concat(".json"));
                                            if (!Files.exists(userFile)) {
                                                Files.createFile(userFile);
                                            }
                                            ctx.writeAndFlush(userMessage);
                                            System.out.println(login + ", успешно зарегестрирован!");
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

                                        public void readText(ChannelHandlerContext ctx, Message msg) {
                                            TextMessage message = (TextMessage) msg;
                                            System.out.println(message.getText());
                                        }

                                        public void ls(ChannelHandlerContext ctx) {
                                           lsMessage ls = new lsMessage();
                                           ctx.writeAndFlush(ls);
                                        }


                                    }
                            );
                        }
                    });

            System.out.println("Client started");
            channel = bootstrap.connect("localhost", 9090).sync().channel();


            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }
}
