package ppex.client.handlers;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import io.netty.channel.ChannelHandlerContext;
import ppex.client.entity.Client;
import ppex.client.process.DetectProcess;
import ppex.proto.msg.type.ProbeTypeMsg;
import ppex.proto.msg.type.TypeMessage;
import ppex.proto.msg.type.TypeMessageHandler;
import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.RudpPack;
import ppex.utils.Constants;

import java.net.InetSocketAddress;


public class ProbeTypeMsgHandler implements TypeMessageHandler {

    private static String TAG = ProbeTypeMsgHandler.class.getName();

    public void handleTypeMessage(ChannelHandlerContext ctx, TypeMessage typeMessage, InetSocketAddress adress) throws Exception{
        if (typeMessage.getType() != TypeMessage.Type.MSG_TYPE_PROBE.ordinal())
            return;
        ProbeTypeMsg pmsg = JSON.parseObject(typeMessage.getBody(),ProbeTypeMsg.class);
        pmsg.setFromInetSocketAddress(adress);
//        ProbeTypeMsg pmsg = MessageUtil.packet2Probemsg(packet);
        if (pmsg.getType() == ProbeTypeMsg.Type.FROM_CLIENT.ordinal()){
            throw new Exception("Wrong ProbeTypeMsg:" + pmsg.toString());
        }else if (pmsg.getType() == ProbeTypeMsg.Type.FROM_SERVER1.ordinal()){
            handleClientFromServer1Msg(ctx,pmsg);
        } else if (pmsg.getType() == ProbeTypeMsg.Type.FROM_SERVER2_PORT1.ordinal()){
            handleClientFromServer2Port1Msg(ctx,pmsg);
        }else if (pmsg.getType() == ProbeTypeMsg.Type.FROM_SERVER2_PORT2.ordinal()){
            handleClientFromServer2Port2Msg(ctx,pmsg);
        }else{
            throw new Exception("Unknown ProbeTypeMsg:" + pmsg.toString());
        }
    }

    //client端处理消息
    private void handleClientFromServer1Msg(ChannelHandlerContext ctx, ProbeTypeMsg msg) {
        Log.d(TAG,"client handle msg recv from s1:" + msg.toString());
        if (msg.getStep() == ProbeTypeMsg.Step.ONE.ordinal()) {
            if (msg.getFromInetSocketAddress().getHostString().equals(Client.getInstance().local_address) && msg.getFromInetSocketAddress().getPort() == Constants.PORT1) {
                DetectProcess.getInstance().isPublicNetwork = true;
            }else{
                DetectProcess.getInstance().NAT_ADDRESS_FROM_S1 = msg.getRecordInetSocketAddress();
                Client.getInstance().address = msg.getRecordInetSocketAddress();
            }
            //与第二阶段的返回的信息相比较。这里需要做一个顺序先后。与handleClientFromS2P1Msg做比较
            Log.d(TAG,"s1 addr:" + DetectProcess.getInstance().NAT_ADDRESS_FROM_S1 + " s2p1:" + DetectProcess.getInstance().NAT_ADDRESS_FROM_S2P1);
            if (DetectProcess.getInstance().NAT_ADDRESS_FROM_S2P1 != null){
                if (DetectProcess.getInstance().NAT_ADDRESS_FROM_S1.equals(DetectProcess.getInstance().NAT_ADDRESS_FROM_S2P1)){
                    DetectProcess.getInstance().NAT_ADDRESS_SAME = true;
                }else{
                    DetectProcess.getInstance().NAT_ADDRESS_SAME = false;
                }
            }
        }
    }

    private void handleClientFromServer2Port1Msg(ChannelHandlerContext ctx, ProbeTypeMsg msg) {
        Log.d(TAG,"client handle msg recv from s2p1:" + msg.toString());
        if (msg.getStep() == ProbeTypeMsg.Step.ONE.ordinal()){
        }else if (msg.getStep() == ProbeTypeMsg.Step.TWO.ordinal()){
            DetectProcess.getInstance().NAT_ADDRESS_FROM_S2P1 = msg.getRecordInetSocketAddress();
            Log.d(TAG,"s1 addr:" + DetectProcess.getInstance().NAT_ADDRESS_FROM_S1 + " s2p1:" + DetectProcess.getInstance().NAT_ADDRESS_FROM_S2P1);
            if (DetectProcess.getInstance().NAT_ADDRESS_FROM_S1 != null){
                if (DetectProcess.getInstance().NAT_ADDRESS_FROM_S1.toString().equals(DetectProcess.getInstance().NAT_ADDRESS_FROM_S2P1.toString())){
                    DetectProcess.getInstance().NAT_ADDRESS_SAME = true;
                }else{
                    DetectProcess.getInstance().NAT_ADDRESS_SAME = false;
                }
            }
        }
    }

    private void handleClientFromServer2Port2Msg(ChannelHandlerContext ctx, ProbeTypeMsg msg) {
        Log.d(TAG,"client handle msg recv from s2p2:" + msg.toString());
        if (msg.getStep() == ProbeTypeMsg.Step.ONE.ordinal()){
            DetectProcess.getInstance().setOne_from_server2p2(true);
        }else if (msg.getStep() == ProbeTypeMsg.Step.TWO.ordinal()){
            DetectProcess.getInstance().setTwo_from_server2p2(true);
        }
    }

    @Override
    public void handleTypeMessage(ChannelHandlerContext ctx, RudpPack rudpPack, IAddrManager addrManager, TypeMessage tmsg) {
        if (tmsg.getType() != TypeMessage.Type.MSG_TYPE_PROBE.ordinal())
            return;
        ProbeTypeMsg pmsg = JSON.parseObject(tmsg.getBody(),ProbeTypeMsg.class);
        pmsg.setFromInetSocketAddress(rudpPack.getConnection().getAddress());
//        ProbeTypeMsg pmsg = MessageUtil.packet2Probemsg(packet);
        if (pmsg.getType() == ProbeTypeMsg.Type.FROM_CLIENT.ordinal()){
//            throw new Exception("Wrong ProbeTypeMsg:" + pmsg.toString());
        }else if (pmsg.getType() == ProbeTypeMsg.Type.FROM_SERVER1.ordinal()){
            handleClientFromServer1Msg(ctx,pmsg);
        } else if (pmsg.getType() == ProbeTypeMsg.Type.FROM_SERVER2_PORT1.ordinal()){
            handleClientFromServer2Port1Msg(ctx,pmsg);
        }else if (pmsg.getType() == ProbeTypeMsg.Type.FROM_SERVER2_PORT2.ordinal()){
            handleClientFromServer2Port2Msg(ctx,pmsg);
        }else{
//            throw new Exception("Unknown ProbeTypeMsg:" + pmsg.toString());
        }
    }
}
