package ppex.proto.msg;

import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.Map;

import io.netty.channel.ChannelHandlerContext;
import ppex.proto.msg.type.TypeMessage;
import ppex.proto.msg.type.TypeMessageHandler;
import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.RudpPack;

public class StandardMessageHandler implements MessageHandler {

    private Map<Integer, TypeMessageHandler> handlers;

    private StandardMessageHandler() {
        init();
    }

    public static StandardMessageHandler New() {
        return new StandardMessageHandler();
    }

    private void init() {
        handlers = new HashMap<>(10, 0.9f);
    }

    public void addTypeMessageHandler(Integer type, TypeMessageHandler handler) {
        if (handlers != null) {
            handlers.put(type, handler);
        }
    }

//    public void handleDatagramPacket(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
//        try {
//            TypeMessage msg = MessageUtil.packet2Typemsg(packet);
//            handlers.get(msg.getType()).handleTypeMessage(ctx, msg, packet.sender());
//        } catch (Exception e) {
//            e.printStackTrace();
//            LOGGER.error("handle datagram packet error" + e.getMessage());
//        }
//    }

    @Override
    public void handleMessage(ChannelHandlerContext ctx, RudpPack rudpPack, IAddrManager addrManager, Message msg) {
        try {
            TypeMessage tmsg = JSON.parseObject(msg.getContent(), TypeMessage.class);
            handlers.get(tmsg.getType()).handleTypeMessage(ctx,rudpPack,addrManager, tmsg);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
