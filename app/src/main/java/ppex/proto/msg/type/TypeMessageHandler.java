package ppex.proto.msg.type;

import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;

public interface TypeMessageHandler {
    default void handleTypeMessage(ChannelHandlerContext ctx, TypeMessage typeMessage, InetSocketAddress fromAddress) throws Exception{
//        System.out.println("handleTypemsg:" + msg.toString());
    }
}
