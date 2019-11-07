package ppex.proto.msg;

import io.netty.channel.ChannelHandlerContext;
import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.RudpPack;

public interface MessageHandler {

//    default void handleDatagramPacket(ChannelHandlerContext ctc,DatagramPacket packet) throws Exception{
//        System.out.println("handle pack:" + packet.toString());
//    }
    void handleMessage(ChannelHandlerContext ctx, RudpPack rudpPack, IAddrManager addrManager, Message msg);
}
