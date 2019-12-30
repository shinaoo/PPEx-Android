package ppex.client.handlers;

import android.util.Log;

import com.alibaba.fastjson.JSON;

import org.greenrobot.eventbus.EventBus;

import ppex.androidcomponent.busevent.BusEvent;
import ppex.proto.msg.type.TxtTypeMsg;
import ppex.proto.msg.type.TypeMessage;
import ppex.proto.msg.type.TypeMessageHandler;
import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.RudpPack;

public class TxtTypeMsgHandler implements TypeMessageHandler {

    private static String TAG = TxtTypeMsgHandler.class.getName();

    //目前txt做测试转发


    public TxtTypeMsgHandler() {
    }

    @Override
    public void handleTypeMessage( RudpPack rudpPack, IAddrManager addrManager, TypeMessage tmsg) {
        TxtTypeMsg txtTypeMsg = JSON.parseObject(tmsg.getBody(), TxtTypeMsg.class);
        handlerTxtTypeMsg(txtTypeMsg, rudpPack, addrManager);
    }

    private void handlerTxtTypeMsg(TxtTypeMsg tmsg, RudpPack rudpPack, IAddrManager addrManager) {
        Log.e("MyTag","rcv txt:" + tmsg.getContent());
        EventBus.getDefault().post(new BusEvent(BusEvent.Type.TXT_RESPONSE.getValue(),tmsg.getContent()));
    }

}
