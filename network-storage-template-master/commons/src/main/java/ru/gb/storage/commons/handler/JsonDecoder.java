package ru.gb.storage.commons.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import ru.gb.storage.commons.message.Message;

import java.util.Arrays;
import java.util.List;


public class JsonDecoder extends MessageToMessageDecoder<ByteBuf> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) throws Exception {
        final byte[] bytes = ByteBufUtil.getBytes(buf);
        final Message message = OBJECT_MAPPER.readValue(bytes, Message.class);
        list.add(message);
    }
}
