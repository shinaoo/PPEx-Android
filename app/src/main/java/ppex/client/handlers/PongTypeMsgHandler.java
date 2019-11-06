package ppex.client.handlers;

import ppex.proto.msg.type.TypeMessage;
import ppex.proto.msg.type.TypeMessageHandler;
import ppex.proto.rudp.RudpPack;

public class PongTypeMsgHandler implements TypeMessageHandler {
    private static String TAG = PongTypeMsgHandler.class.getName();

    @Override
    public void handleTypeMessage(RudpPack rudpPack, TypeMessage tmsg) {

    }
}
