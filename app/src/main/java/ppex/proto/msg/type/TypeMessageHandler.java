package ppex.proto.msg.type;

import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.RudpPack;

public interface TypeMessageHandler {
    void handleTypeMessage(RudpPack rudpPack, IAddrManager addrManager, TypeMessage tmsg);
}
