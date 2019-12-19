package ppex.client.handlers;

import com.alibaba.fastjson.JSON;
import org.apache.log4j.Logger;
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
        System.out.println("ProbeTypeMsg handle:" + tmsg.toString());
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
    private void handleClientFromServer1Msg(ProbeTypeMsg msg) {
        System.out.println("client handle server1 msg:" + msg.toString());
        if (msg.getStep() == ProbeTypeMsg.Step.ONE.ordinal()) {
            if (msg.getFromInetSocketAddress().getHostString().equals(Client.getInstance().getAddrLocal()) && msg.getFromInetSocketAddress().getPort() == Client.getInstance().getPORT_1()) {
//                DetectProcess.getInstance().isPublicNetwork = true;
                DetectProcess.getInstance().setPublicNetwork(true);
            } else {
                DetectProcess.getInstance().setNAT_ADDRESS_FROM_S1(msg.getRecordInetSocketAddress());
                Client.getInstance().setAddrLocal(msg.getRecordInetSocketAddress());
            }
            //todo 2019.11.08与第二阶段的返回的信息相比较。这里需要做一个顺序先后。与handleClientFromS2P1Msg做比较
            //todo 2019.12.16上面的这个信息比较做的不对.后面等待时间后再统一做比较
//            if (DetectProcess.getInstance().getNAT_ADDRESS_FROM_S2P1() != null) {
//                if (DetectProcess.getInstance().getNAT_ADDRESS_FROM_S1().equals(DetectProcess.getInstance().getNAT_ADDRESS_FROM_S2P1())) {
//                    DetectProcess.getInstance().setNAT_ADDRESS_SAME(true);
//                } else {
//                    DetectProcess.getInstance().setNAT_ADDRESS_SAME(false);
//                }
//            }
        }
    }

    private synchronized void handleClientFromServer2Port1Msg(ProbeTypeMsg msg) {
        System.out.println("client handler server2p1 msg:" + msg.toString());
        if (msg.getStep() == ProbeTypeMsg.Step.ONE.ordinal()) {
        } else if (msg.getStep() == ProbeTypeMsg.Step.TWO.ordinal()) {
            DetectProcess.getInstance().setNAT_ADDRESS_FROM_S2P1(msg.getRecordInetSocketAddress());
        }
    }

    private synchronized void handleClientFromServer2Port2Msg(ProbeTypeMsg msg) {
        System.out.println("client handler server2p2 msg:" + msg.toString());
        if (msg.getStep() == ProbeTypeMsg.Step.ONE.ordinal()) {
            DetectProcess.getInstance().setOne_from_server2p2(true);
        } else if (msg.getStep() == ProbeTypeMsg.Step.TWO.ordinal()) {
            DetectProcess.getInstance().setTwo_from_server2p2(true);
        }
    }


}
