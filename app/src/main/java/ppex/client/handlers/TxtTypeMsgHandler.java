package ppex.client.handlers;

import io.netty.channel.ChannelHandlerContext;
import ppex.proto.type.TypeMessage;
import ppex.proto.type.TypeMessageHandler;

import java.net.InetSocketAddress;

public class TxtTypeMsgHandler implements TypeMessageHandler {

    @Override
    public void handleTypeMessage(ChannelHandlerContext ctx, TypeMessage typeMessage, InetSocketAddress fromAddress) throws Exception {

    }
}
