package ppex.proto.msg;

import android.util.Log;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import ppex.proto.msg.type.TypeMessage;
import ppex.proto.msg.type.TypeMessageHandler;
import ppex.utils.MessageUtil;

import java.util.HashMap;
import java.util.Map;

public class StandardMessageHandler implements MessageHandler {
    private static String TAG = StandardMessageHandler.class.getName();
    private Map<Integer, TypeMessageHandler> handlers;

    public StandardMessageHandler() {
        init();
    }

    private void init() {
        handlers = new HashMap<>(10,0.9f);
    }

    public void addTypeMessageHandler(Integer type,TypeMessageHandler handler){
        if (handlers != null){
            handlers.put(type,handler);
        }
    }

    @Override
    public void handleDatagramPacket(ChannelHandlerContext ctx,DatagramPacket packet) throws Exception {
        try {
            Log.d(TAG,"Standard handle datagram packet");
            TypeMessage msg = MessageUtil.packet2Typemsg(packet);
            handlers.get(msg.getType()).handleTypeMessage(ctx,msg,packet.sender());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
