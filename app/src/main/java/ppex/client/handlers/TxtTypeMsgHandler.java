package ppex.client.handlers;

import com.alibaba.fastjson.JSON;
import ppex.proto.msg.type.TxtTypeMsg;
import ppex.proto.msg.type.TypeMessage;
import ppex.proto.msg.type.TypeMessageHandler;
import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.RudpPack;

public class TxtTypeMsgHandler implements TypeMessageHandler {

    private static String TAG = TxtTypeMsgHandler.class.getName();

    public TxtTypeMsgHandler() {
    }

    @Override
    public void handleTypeMessage( RudpPack rudpPack, IAddrManager addrManager, TypeMessage tmsg) {
        TxtTypeMsg txtTypeMsg = JSON.parseObject(tmsg.getBody(), TxtTypeMsg.class);
        handlerTxtTypeMsg(txtTypeMsg, rudpPack, addrManager);
    }

    private void handlerTxtTypeMsg(TxtTypeMsg tmsg, RudpPack rudpPack, IAddrManager addrManager) {
    }

}
