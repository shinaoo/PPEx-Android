package ppex.proto.msg.type;

import ppex.proto.rudp.RudpPack;

public interface TypeMessageHandler {
    void handleTypeMessage(RudpPack rudpPack, TypeMessage tmsg);
}
