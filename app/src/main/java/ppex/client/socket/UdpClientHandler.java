package ppex.client.socket;

import android.util.Log;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleStateEvent;
import ppex.client.entity.Client;
import ppex.client.handlers.FileTypeMsgHandler;
import ppex.client.handlers.PongTypeMsgHandler;
import ppex.client.handlers.ProbeTypeMsgHandler;
import ppex.client.handlers.ThroughTypeMsgHandler;
import ppex.client.handlers.TxtTypeMsgHandler;
import ppex.proto.MessageHandler;
import ppex.proto.StandardMessageHandler;
import ppex.proto.entity.through.Connect;
import ppex.proto.type.PingTypeMsg;
import ppex.proto.type.TypeMessage;
import ppex.utils.MessageUtil;


public class UdpClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static String TAG = UdpClientHandler.class.getName();

    private MessageHandler msgHandler;

    public UdpClientHandler() {
        msgHandler = new StandardMessageHandler();
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_PROBE.ordinal(), new ProbeTypeMsgHandler());
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_THROUGH.ordinal(), new ThroughTypeMsgHandler());
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_HEART_PONG.ordinal(),new PongTypeMsgHandler());
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_FILE.ordinal(),new FileTypeMsgHandler());
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_TXT.ordinal(),new TxtTypeMsgHandler());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
        try {
            msgHandler.handleDatagramPacket(channelHandlerContext, datagramPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        Message msg = MessageUtil.packet2Msg(datagramPacket);
//        if (msg != null){
//            System.out.println("client recv:" + msg.toString() + " from:" + datagramPacket.sender());
//        }else{
//            System.out.println("client recv error");
//        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            switch (e.state()) {
                case WRITER_IDLE:
                    handleWriteIdle(ctx);
                    break;
                case READER_IDLE:
                    handleReadIdle();
                    break;
                case ALL_IDLE:
                    handleAllIdle();
                    break;
                default:
                    break;
            }
        }
    }

    private void handleWriteIdle(ChannelHandlerContext ctx){
        //心跳包
        PingTypeMsg pingTypeMsg = new PingTypeMsg();
//        pingTypeMsg.setType(PingTypeMsg.Type.HEART.ordinal());
//        pingTypeMsg.setContent(JSON.toJSONString(Client.getInstance().localConnection));
        ctx.writeAndFlush(MessageUtil.pingMsg2Packet(pingTypeMsg, Client.getInstance().SERVER1));
        if (Client.getInstance().connectedMaps.size() != 0){
            if (Client.getInstance().connectedMaps.get(0).getConnectType() != Connect.TYPE.FORWARD.ordinal()){
                ctx.writeAndFlush(MessageUtil.pingMsg2Packet(pingTypeMsg,Client.getInstance().connectedMaps.get(0).getConnections().get(1).inetSocketAddress));
            }
        }
    }
    private void handleReadIdle(){
        Log.d(TAG,"client handle read idle");
    }
    private void handleAllIdle(){
        Log.d(TAG,"client handle all idle");
    }
}
