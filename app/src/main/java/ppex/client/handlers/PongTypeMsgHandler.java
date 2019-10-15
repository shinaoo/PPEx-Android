package ppex.client.handlers;

import android.util.Log;

import io.netty.channel.ChannelHandlerContext;

import ppex.proto.type.TypeMessage;
import ppex.proto.type.TypeMessageHandler;

import java.net.InetSocketAddress;

public class PongTypeMsgHandler implements TypeMessageHandler {
    private static String TAG = PongTypeMsgHandler.class.getName();

    @Override
    public void handleTypeMessage(ChannelHandlerContext ctx, TypeMessage typeMessage, InetSocketAddress fromAddress) {
        Log.d(TAG,"client recv pong msg :" + typeMessage.getBody());
    }
}
