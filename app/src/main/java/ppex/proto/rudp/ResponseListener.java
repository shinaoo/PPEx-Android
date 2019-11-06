package ppex.proto.rudp;

import ppex.proto.msg.Message;

public interface ResponseListener {
    void onResponse(RudpPack rudpPack, Message message);
}
