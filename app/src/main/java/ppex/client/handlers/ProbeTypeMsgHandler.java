package ppex.client.handlers;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import org.apache.log4j.Logger;
import org.greenrobot.eventbus.EventBus;

import ppex.androidcomponent.busevent.BusEvent;
import ppex.client.Client;
import ppex.client.process.DetectProcess;
import ppex.proto.msg.type.ProbeTypeMsg;
import ppex.proto.msg.type.TypeMessage;
import ppex.proto.msg.type.TypeMessageHandler;
import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.RudpPack;

/**
 * Client端接收ProbeTypeMsg有三个方向,分两个阶段
 * 都能从Server1,Server2P1,Server2P2接受到消息
 */

public class ProbeTypeMsgHandler implements TypeMessageHandler {

    private static Logger LOGGER = Logger.getLogger(ProbeTypeMsg.class);

    @Override
    public void handleTypeMessage(RudpPack rudpPack, IAddrManager addrManager, TypeMessage tmsg) {
        Log.e("MyTag","ProbeTypeMsg handle:" + tmsg.toString());
        if (tmsg.getType() != TypeMessage.Type.MSG_TYPE_PROBE.ordinal())
            return;
        ProbeTypeMsg pmsg = JSON.parseObject(tmsg.getBody(), ProbeTypeMsg.class);
        pmsg.setFromInetSocketAddress(rudpPack.getOutput().getConn().getAddress());

        if (pmsg.getType() == ProbeTypeMsg.Type.FROM_SERVER1.ordinal()) {
            handleClientFromServer1Msg(pmsg);
        } else if (pmsg.getType() == ProbeTypeMsg.Type.FROM_SERVER2_PORT1.ordinal()) {
            handleClientFromServer2Port1Msg(pmsg);
        } else if (pmsg.getType() == ProbeTypeMsg.Type.FROM_SERVER2_PORT2.ordinal()) {
            handleClientFromServer2Port2Msg(pmsg);
        } else {
        }
    }

    //client端处理消息
    private synchronized void handleClientFromServer1Msg(ProbeTypeMsg msg) {
        Log.e("MyTag","client handle server1 msg:" + msg.toString());
        if (msg.getStep() == ProbeTypeMsg.Step.ONE.ordinal()) {
            if (msg.getFromInetSocketAddress().getHostString().equals(Client.getInstance().getAddrLocal()) && msg.getFromInetSocketAddress().getPort() == Client.getInstance().getPORT_1()) {
//                DetectProcess.getInstance().isPublicNetwork = true;
                DetectProcess.getInstance().setPublicNetwork(true);
            } else {
                DetectProcess.getInstance().setNAT_ADDRESS_FROM_S1(msg.getRecordInetSocketAddress());
                Client.getInstance().setAddrLocal(msg.getRecordInetSocketAddress());
                EventBus.getDefault().post(new BusEvent(BusEvent.Type.DETECT_ONE_FROM_SERVER1.getValue()));
            }
        }
    }

    private synchronized void handleClientFromServer2Port1Msg(ProbeTypeMsg msg) {
        Log.e("MyTag","client handler server2p1 msg:" + msg.toString());
        if (msg.getStep() == ProbeTypeMsg.Step.ONE.ordinal()) {
        } else if (msg.getStep() == ProbeTypeMsg.Step.TWO.ordinal()) {
            DetectProcess.getInstance().setNAT_ADDRESS_FROM_S2P1(msg.getRecordInetSocketAddress());
            EventBus.getDefault().post(new BusEvent(BusEvent.Type.DETECT_TWO_FROM_SERVER2P1.getValue()));
        }
    }

    private synchronized void handleClientFromServer2Port2Msg(ProbeTypeMsg msg) {
        Log.e("MyTag","client handler server2p2 msg:" + msg.toString());
        if (msg.getStep() == ProbeTypeMsg.Step.ONE.ordinal()) {
            DetectProcess.getInstance().setOne_from_server2p2(true);
            EventBus.getDefault().post(new BusEvent(BusEvent.Type.DETECT_ONE_FROM_SERVER2P2.getValue()));
        } else if (msg.getStep() == ProbeTypeMsg.Step.TWO.ordinal()) {
            DetectProcess.getInstance().setTwo_from_server2p2(true);
            EventBus.getDefault().post(new BusEvent(BusEvent.Type.DETECT_TWO_FROM_SERVER2P2.getValue()));
        }
    }


}
