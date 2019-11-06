package ppex.proto.msg;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

public interface MessageHandler {

    default void handleDatagramPacket(ChannelHandlerContext ctc, DatagramPacket packet) throws Exception{
        System.out.println("handle pack:" + packet.toString());
    }
}
