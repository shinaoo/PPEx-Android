package ppex.proto.msg.type;

import io.netty.channel.ChannelHandlerContext;
import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.RudpPack;

public interface TypeMessageHandler {
    void handleTypeMessage(ChannelHandlerContext ctx, RudpPack rudpPack, IAddrManager addrManager, TypeMessage tmsg);
}
