package ru.gb.storage.commons.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import ru.gb.storage.commons.Constant;
import ru.gb.storage.commons.message.Message;

import java.util.List;

public class JsonDecoderLog extends MessageToMessageDecoder<String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Override
    protected void decode(ChannelHandlerContext ctx, String str, List<Object> list) throws Exception {
        System.out.println(Constant.ANSI_RED + "Сообщения для лога: " + str + Constant.ANSI_RESET);
        final Message message = OBJECT_MAPPER.readValue(str, Message.class);
        list.add(message);
    }
}
