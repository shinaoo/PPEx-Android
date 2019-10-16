package ppex.client.handlers;

import android.util.Log;

import com.alibaba.fastjson.JSON;

import org.greenrobot.eventbus.EventBus;

import java.net.InetSocketAddress;
import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import ppex.client.androidcomponent.busevent.BusEvent;
import ppex.client.process.ThroughProcess;
import ppex.proto.entity.through.Connect;
import ppex.proto.entity.through.Connection;
import ppex.proto.entity.through.RecvInfo;
import ppex.proto.type.ThroughTypeMsg;
import ppex.proto.type.TypeMessage;
import ppex.proto.type.TypeMessageHandler;

public class ThroughTypeMsgHandler implements TypeMessageHandler {
    private static String TAG = ThroughTypeMsgHandler.class.getName();

    @Override
    public void handleTypeMessage(ChannelHandlerContext ctx, TypeMessage msg, InetSocketAddress address) throws Exception {
//        ThroughTypeMsg ttmsg = MessageUtil.packet2ThroughMsg(packet);
        Log.d(TAG,"client handle ThrougnTypemsg:" + msg.getBody());
        ThroughTypeMsg ttmsg = JSON.parseObject(msg.getBody(), ThroughTypeMsg.class);
        if (ttmsg.getAction() == ThroughTypeMsg.ACTION.RECV_INFO.ordinal()) {
            RecvInfo recvinfo = JSON.parseObject(ttmsg.getContent(), RecvInfo.class);
            if (recvinfo.type == ThroughTypeMsg.RECVTYPE.SAVE_CONNINFO.ordinal()) {
                handleSaveInfoFromServer(ctx, recvinfo);
            } else if (recvinfo.type == ThroughTypeMsg.RECVTYPE.GET_CONNINFO.ordinal()) {
                handleGetInfoFromServer(ctx, recvinfo);
            } else if (recvinfo.type == ThroughTypeMsg.RECVTYPE.CONNECT_CONN.ordinal()) {
                handleConnectFromServer(ctx, recvinfo);
            } else {
                throw new Exception("Unkown through msg action:" + ttmsg.toString());
            }
        } else if (ttmsg.getAction() == ThroughTypeMsg.ACTION.CONNECT_CONN.ordinal()) {
            handleConnecCONN(ctx, ttmsg, address);
        }
    }

    private void handleSaveInfoFromServer(ChannelHandlerContext ctx, RecvInfo recvinfo) {
        Log.d(TAG,"client handle ThroughTypemsg saveinfo from server:" + recvinfo.toString());
        if (recvinfo.type != ThroughTypeMsg.RECVTYPE.SAVE_CONNINFO.ordinal()) {
            return;
        }
        if (!recvinfo.recvinfos.equals("success")) {
            ThroughProcess.getInstance().sendSaveInfo();
        } else {
            //todo 增加心跳连接
            Log.d(TAG,"client get ids from server");
//            ThroughProcess.getInstance().getConnectionsFromServer(ctx);
        }
    }

    private void handleGetInfoFromServer(ChannelHandlerContext ctx, RecvInfo recvinfo) {
        Log.d(TAG,"client handle ThroughTypeMsg getInfo from server:" + recvinfo.toString());
        if (recvinfo.type != ThroughTypeMsg.RECVTYPE.GET_CONNINFO.ordinal())
            return;
        Log.d(TAG,"All connection:" + recvinfo.recvinfos);
        List<Connection> connections = JSON.parseArray(recvinfo.recvinfos, Connection.class);
        //todo 发送要连接的Connection信息
//        ThroughProcess.getInstance().connectOtherPeer(ctx, connections.get(0));
        //todo 将Connections消息返回到MainActivity
        EventBus.getDefault().post(new BusEvent(BusEvent.Type.THROUGH_GET_INFO.getValue(),connections));
    }

    private void handleConnectFromServer(ChannelHandlerContext ctx, RecvInfo recvinfo) {

    }

    private void handleConnectType(ChannelHandlerContext ctx, Connect connect) {

    }

    private void handleReturnStartPunch(ChannelHandlerContext ctx, Connect connect) {
    }

    private void handleConnecCONN(ChannelHandlerContext ctx, ThroughTypeMsg ttmsg, InetSocketAddress fromaddress) {
    }

}
