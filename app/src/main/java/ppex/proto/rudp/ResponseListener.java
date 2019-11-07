package ppex.proto.rudp;

import io.netty.channel.ChannelHandlerContext;
import ppex.proto.msg.Message;

public interface ResponseListener {
    default void onResponse(ChannelHandlerContext ctx, RudpPack rudpPack, Message message){}
}
