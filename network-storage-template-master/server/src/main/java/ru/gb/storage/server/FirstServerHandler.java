package ru.gb.storage.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ru.gb.storage.commons.message.*;
import ru.gb.storage.commons.Constant;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FirstServerHandler extends SimpleChannelInboundHandler<Message> {
    long counter = 0;
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("New active channel");
        TextMessage answer = new TextMessage();
        answer.setText("Successfully connection");
        ctx.writeAndFlush(answer);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        if (msg instanceof TextMessage) {
            TextMessage message = (TextMessage) msg;
            System.out.println("incoming text message: " + message.getText());
            ctx.writeAndFlush(msg);
        }
        if (msg instanceof DateMessage) {
            DateMessage message = (DateMessage) msg;
            System.out.println("incoming date message: " + message.getDate());
            ctx.writeAndFlush(msg);
        }
        if (msg instanceof AuthMessage) {
            AuthMessage message = (AuthMessage) msg;
            System.out.println(message);
            TextMessage authAnswer = new TextMessage();
            authAnswer.setText("Авторизация пройдена");
            ctx.writeAndFlush(authAnswer);
        }
        if (msg instanceof FileRequestMessage) {
            System.out.println("Запрос на отправку файла");
            FileRequestMessage frm = (FileRequestMessage) msg;
            final File file = new File(frm.getPath());
            try (final RandomAccessFile accessFile = new RandomAccessFile(file, "r")) {
                while (accessFile.getFilePointer() != accessFile.length()) {
                    final byte[] fileContent;
                    final long available = accessFile.length() - accessFile.getFilePointer();
                    if (available > 64 * 1024) {
                        fileContent = new byte[64 * 1024];
                    } else {
                        fileContent = new byte[(int) available];
                    }
                    final FileContentMessage message = new FileContentMessage();
                    message.setStartPosition(accessFile.getFilePointer());
                    accessFile.read(fileContent);
                    message.setContent(fileContent);
                    message.setLast(accessFile.getFilePointer() == accessFile.length());
                    ctx.writeAndFlush(message);
                    counter ++;
                    System.out.println("Пакетов отправлено + " + counter);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("client disconnect");
    }
}