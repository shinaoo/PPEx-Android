package ppex.client.handlers;

import android.util.Log;

import com.alibaba.fastjson.JSON;

import io.netty.channel.ChannelHandlerContext;

import ppex.client.entity.Client;
import ppex.client.process.ThroughProcess;
import ppex.proto.entity.through.Connect;
import ppex.proto.entity.through.Connection;
import ppex.proto.entity.through.RecvInfo;
import ppex.proto.entity.through.connect.ConnectType;
import ppex.proto.type.ThroughTypeMsg;
import ppex.proto.type.TypeMessage;
import ppex.proto.type.TypeMessageHandler;
import ppex.utils.MessageUtil;

import java.net.InetSocketAddress;
import java.util.List;

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
            ThroughProcess.getInstance().getConnectionsFromServer(ctx);
        }
    }

    private void handleGetInfoFromServer(ChannelHandlerContext ctx, RecvInfo recvinfo) {
        Log.d(TAG,"client handle ThroughTypeMsg getInfo from server:" + recvinfo.toString());
        if (recvinfo.type != ThroughTypeMsg.RECVTYPE.GET_CONNINFO.ordinal())
            return;
        Log.d(TAG,"All connection:" + recvinfo.recvinfos);
        List<Connection> connections = JSON.parseArray(recvinfo.recvinfos, Connection.class);
        //todo 发送要连接的Connection信息
        ThroughProcess.getInstance().connectOtherPeer(ctx, connections.get(0));
    }

    private void handleConnectFromServer(ChannelHandlerContext ctx, RecvInfo recvinfo) {
        Log.d(TAG,"client handle ThroughTypeMsg connect from server:" + recvinfo.toString());
        if (recvinfo.type != ThroughTypeMsg.RECVTYPE.CONNECT_CONN.ordinal())
            return;
        Connect connect = JSON.parseObject(recvinfo.recvinfos, Connect.class);
        //todo 发送包给目标地址.首先看对面类型.然后选择穿越方法
        if (connect.getType() == Connect.TYPE.CONNECT_ERROR.ordinal()) {

        } else if (connect.getType() == Connect.TYPE.RETURN_REQUEST_CONNECT_SERVER.ordinal()) {
            //todo 开始和目标建立连接.建立连接失败就走转发
            handleConnectType(ctx, connect);
        } else if (connect.getType() == Connect.TYPE.RETURN_START_PUNCH.ordinal()) {
            handleReturnStartPunch(ctx, connect);
        }

    }

    private void handleConnectType(ChannelHandlerContext ctx, Connect connect) {
        Log.d(TAG,"Client handle ConnectType:" + connect.toString());
        ConnectType type = JSON.parseObject(connect.getContent(), ConnectType.class);
        Client.getInstance().targetConnection = type.target;
        ThroughTypeMsg throughTypeMsg = new ThroughTypeMsg();

//        PingTypeMsg pingTypeMsg = new PingTypeMsg();
//        pingTypeMsg.setContent(JSON.toJSONString(Client.getInstance().localConnection));
//        pingTypeMsg.setType(PingTypeMsg.Type.CONNECT.ordinal());
        if (type.connectType == ConnectType.Type.DIRECT_SEND.ordinal()) {
            connect.setType(Connect.TYPE.CONNECT_PING.ordinal());
            connect.setContent(JSON.toJSONString(type));
            throughTypeMsg.setContent(JSON.toJSONString(connect));
            ctx.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg, type.target.inetSocketAddress));
//            ctx.writeAndFlush(MessageUtil.pingMsg2Packet(pingTypeMsg, type.target.inetSocketAddress));
        } else if (type.connectType == ConnectType.Type.WAIT_DIRECT_SEND.ordinal()) {
            //todo 暂时没有反应.就是还是等待就可以了

        } else if (type.connectType == ConnectType.Type.START_PUNCH.ordinal()) {
            connect.setType(Connect.TYPE.RETURN_START_PUNCH.ordinal());
            connect.setContent(JSON.toJSONString(type));
            throughTypeMsg.setAction(ThroughTypeMsg.ACTION.CONNECT_CONN.ordinal());
            throughTypeMsg.setContent(JSON.toJSONString(connect));
            //todo 这个可以多发几次
            ctx.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg, type.target.inetSocketAddress));
            ctx.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg, Client.getInstance().SERVER1));
        } else if (type.connectType == ConnectType.Type.WAIT_PUNCH.ordinal()) {
            //todo 暂时没有反应,等待就可以
        } else if (type.connectType == ConnectType.Type.PLATFORM_FORWARD.ordinal()) {
            connect.setType(Connect.TYPE.CONNECT_PING.ordinal());
            connect.setContent(JSON.toJSONString(type));
            throughTypeMsg.setAction(ThroughTypeMsg.ACTION.CONNECT_CONN.ordinal());
            throughTypeMsg.setContent(JSON.toJSONString(connect));
            ctx.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg, Client.getInstance().SERVER1));
        } else {

        }
    }

    private void handleReturnStartPunch(ChannelHandlerContext ctx, Connect connect) {
        Log.d(TAG,"Client handle Return start punch:" + connect.toString());
        ConnectType type = JSON.parseObject(connect.getContent(), ConnectType.class);
        if (type.connectType != ConnectType.Type.START_PUNCH.ordinal())
            return;
        //往type的source发
        connect.setType(Connect.TYPE.CONNECT_PING.ordinal());
        connect.setContent(JSON.toJSONString(type));
        ThroughTypeMsg throughTypeMsg = new ThroughTypeMsg();
        throughTypeMsg.setAction(ThroughTypeMsg.ACTION.CONNECT_CONN.ordinal());
        throughTypeMsg.setContent(JSON.toJSONString(connect));
        ctx.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg, type.source.inetSocketAddress));
    }

    private void handleConnecCONN(ChannelHandlerContext ctx, ThroughTypeMsg ttmsg, InetSocketAddress fromaddress) {
        Log.d(TAG,"Client handle ConnectConn:" + ttmsg.getContent());
        Connect connect = JSON.parseObject(ttmsg.getContent(), Connect.class);
        if (connect.getType() == Connect.TYPE.CONNECT_PING.ordinal()) {
            connect.setType(Connect.TYPE.CONNECT_PONG.ordinal());
            ttmsg.setContent(JSON.toJSONString(connect));
            ctx.writeAndFlush(MessageUtil.throughmsg2Packet(ttmsg, fromaddress));
        } else if (connect.getType() == Connect.TYPE.CONNECT_PONG.ordinal()) {
            //todo 发送消息成功
            Log.d(TAG,"client 接受Connect pong");
            Client.getInstance().connectType = 0;
        }
    }

}
