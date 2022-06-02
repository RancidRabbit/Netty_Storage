package ru.gb.storage.server;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import ru.gb.storage.commons.handler.JsonDecoder;
import ru.gb.storage.commons.handler.JsonEncoder;

import java.io.File;
import java.io.IOException;

public class Server {

    private final int port;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private File usersList = new File("C:\\Server\\USERS.json");

    public static void main(String[] args) throws InterruptedException, IOException {
        new Server(9090).start();
    }

    public Server(int port) throws IOException {
        this.port = port;
    }

    public void start() throws InterruptedException, IOException {
        //ThreadPool отвечающий за инициализацию новых подключений
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        //ThreadPool обслуживающий всех активных клиентов
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap server = new ServerBootstrap();
            server
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) //Используем серверную версию сокета
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) {
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 3, 0, 3),
                                    new LengthFieldPrepender(3),
                                    new JsonDecoder(),
                                    new JsonEncoder(),
                                    new FirstServerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            Channel channel = server.bind(port).sync().channel();

            System.out.println("Сервер запущен");
            Users users = new Users();
            users.start();
           if (!isFileEmpty(usersList)) {
               users = OBJECT_MAPPER.readValue((usersList), Users.class);
           }
           OBJECT_MAPPER.writeValue((usersList), users);
           channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();

        }
    }
    /* Вынести это метод в модуль commons */
    public boolean isFileEmpty(File file) {
        return file.length() == 0;
    }
}