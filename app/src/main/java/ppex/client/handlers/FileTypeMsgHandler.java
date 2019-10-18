package ppex.client.handlers;

import java.net.InetSocketAddress;

import io.netty.channel.ChannelHandlerContext;
import ppex.proto.type.TypeMessage;
import ppex.proto.type.TypeMessageHandler;

public class FileTypeMsgHandler implements TypeMessageHandler {

    @Override
    public void handleTypeMessage(ChannelHandlerContext ctx, TypeMessage typeMessage, InetSocketAddress fromAddress) throws Exception {

    }
}