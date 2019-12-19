package ppex.client.rudp;

import ppex.client.handlers.*;
import ppex.proto.msg.Message;
import ppex.proto.msg.MessageHandler;
import ppex.proto.msg.StandardMessageHandler;
import ppex.proto.msg.type.TypeMessage;
import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.ResponseListener;
import ppex.proto.rudp.RudpPack;

public class MsgResponse implements ResponseListener {

    private MessageHandler msgHandler;
    private IAddrManager addrManager;

    public MsgResponse(IAddrManager addrManager){
        this.addrManager = addrManager;
        this.msgHandler = StandardMessageHandler.New();
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_PROBE.ordinal(), new ProbeTypeMsgHandler());
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_THROUGH.ordinal(), new ThroughTypeMsgHandler());
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_HEART_PONG.ordinal(), new PongTypeMsgHandler());
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_FILE.ordinal(), new FileTypeMsgHandler());
        ((StandardMessageHandler) msgHandler).addTypeMessageHandler(TypeMessage.Type.MSG_TYPE_TXT.ordinal(), new TxtTypeMsgHandler());
    }

    @Override
    public void onResponse(RudpPack rudpPack, Message message) {
        this.msgHandler.handleMessage(rudpPack,addrManager,message);
    }
}
