package ppex.client.handlers;

import java.net.InetSocketAddress;

import io.netty.channel.ChannelHandlerContext;
import ppex.proto.msg.type.TypeMessage;
import ppex.proto.msg.type.TypeMessageHandler;
import ppex.proto.rudp.RudpPack;

public class FileTypeMsgHandler implements TypeMessageHandler {

    public void handleTypeMessage(ChannelHandlerContext ctx, TypeMessage typeMessage, InetSocketAddress fromAddress) throws Exception {

    }

    @Override
    public void handleTypeMessage(RudpPack rudpPack, TypeMessage tmsg) {

    }
}