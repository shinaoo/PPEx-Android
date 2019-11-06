package ppex.proto.pcp;

import ppex.proto.msg.Message;

public interface PcpListener {
    default void onResponse(Message message){}
}
