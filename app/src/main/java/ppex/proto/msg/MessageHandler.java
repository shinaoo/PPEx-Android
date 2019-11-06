package ppex.proto.msg;

import ppex.proto.rudp.RudpPack;

public interface MessageHandler {

//    default void handleDatagramPacket(ChannelHandlerContext ctc,DatagramPacket packet) throws Exception{
//        System.out.println("handle pack:" + packet.toString());
//    }
    void handleMessage(RudpPack rudpPack, Message msg);
}
