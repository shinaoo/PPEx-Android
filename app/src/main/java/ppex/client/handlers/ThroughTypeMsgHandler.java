package ppex.client.handlers;

import android.util.Log;

import com.alibaba.fastjson.JSON;

import org.greenrobot.eventbus.EventBus;

import java.net.InetSocketAddress;
import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import ppex.client.androidcomponent.busevent.BusEvent;
import ppex.client.entity.Client;
import ppex.client.process.ThroughProcess;
import ppex.proto.Message;
import ppex.proto.entity.through.Connect;
import ppex.proto.entity.through.ConnectMap;
import ppex.proto.entity.through.Connection;
import ppex.proto.entity.through.RecvInfo;
import ppex.proto.type.ThroughTypeMsg;
import ppex.proto.type.TypeMessage;
import ppex.proto.type.TypeMessageHandler;
import ppex.utils.MessageUtil;

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
        //从服务转发而来的Connect_CONN信息
        if (recvinfo.type == ThroughTypeMsg.RECVTYPE.CONNECT_CONN.ordinal()){
            Connect connect = JSON.parseObject(recvinfo.recvinfos,Connect.class);
            List<Connection> connections = JSON.parseArray(connect.getContent(),Connection.class);
            ThroughTypeMsg ttmsg = new ThroughTypeMsg();
            ttmsg.setAction(ThroughTypeMsg.ACTION.CONNECT_CONN.ordinal());
            if (connect.getType() == Connect.TYPE.HOLE_PUNCH.ordinal()){
                Log.e(TAG,"Client handle hole_punch msg:" + connect.toString());
                connect.setType(Connect.TYPE.CONNECT_PING.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));
                //多发几次都可以
                ctx.writeAndFlush(MessageUtil.throughmsg2Packet(ttmsg,connections.get(0).inetSocketAddress));
                ctx.writeAndFlush(MessageUtil.throughmsg2Packet(ttmsg,connections.get(0).inetSocketAddress));

                connect.setType(Connect.TYPE.RETURN_HOLE_PUNCH.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));
                ctx.writeAndFlush(MessageUtil.throughmsg2Packet(ttmsg,Client.getInstance().SERVER1));
            }else if (connect.getType() == Connect.TYPE.RETURN_HOLE_PUNCH.ordinal()){
                Log.e(TAG,"Client handle return_hole_punch msg:" + connect.toString());
                //开始给B 发ping消息
                connect.setType(Connect.TYPE.CONNECT_PING.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));
                ctx.writeAndFlush(MessageUtil.throughmsg2Packet(ttmsg,connections.get(1).inetSocketAddress));
            }else if (connect.getType() == Connect.TYPE.REVERSE.ordinal()){
                Log.e(TAG,"Client handle reverse msg:" + connect.toString());
                connect.setType(Connect.TYPE.CONNECT_PING.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));
                ctx.writeAndFlush(MessageUtil.throughmsg2Packet(ttmsg,connections.get(0).inetSocketAddress));
            }else if (connect.getType() == Connect.TYPE.FORWARD.ordinal()){
                Log.e(TAG,"Client handle forward msg:" + connect.toString());
                connect.setType(Connect.TYPE.RETURN_FORWARD.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));
                ctx.writeAndFlush(MessageUtil.throughmsg2Packet(ttmsg,Client.getInstance().SERVER1));
            }else if (connect.getType() == Connect.TYPE.RETURN_FORWARD.ordinal()){
                Log.e(TAG,"Client handle return_forward msg:" + connect.toString());
                connect.setType(Connect.TYPE.CONNECTED.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));
                ctx.writeAndFlush(MessageUtil.throughmsg2Packet(ttmsg,Client.getInstance().SERVER1));

                //这个是判断forward类型是否已经连接成功
                Client.getInstance().connectedMaps.add(new ConnectMap(Connect.TYPE.FORWARD.ordinal(),connections));
                EventBus.getDefault().post(new BusEvent(BusEvent.Type.THROUGN_CONNECT_END.ordinal(),""));
            }
        }
    }

    private void handleConnecCONN(ChannelHandlerContext ctx,ThroughTypeMsg ttmsg,InetSocketAddress fromaddress) throws Exception{
        Connect connect = JSON.parseObject(ttmsg.getContent(),Connect.class);
        if (connect.getType() == Connect.TYPE.CONNECT_PING.ordinal()){
            Log.e(TAG,"Client handle connect_ping msg:" + connect.toString());
            connect.setType(Connect.TYPE.CONNECT_PONG.ordinal());
            ttmsg.setContent(JSON.toJSONString(connect));
            ctx.writeAndFlush(MessageUtil.throughmsg2Packet(ttmsg,fromaddress));
            if (Client.getInstance().isConnecting(Client.getInstance().connectingMaps,connect)){
                //还需要判断是不是REVERSE类型
                ConnectMap connectMap = Client.getInstance().connectingMaps.get(0);
                if (connectMap.getConnectType() == Connect.TYPE.REVERSE.ordinal()){
                    Client.getInstance().connectedMaps.add(Client.getInstance().connectingMaps.remove(0));
                    EventBus.getDefault().post(new BusEvent(BusEvent.Type.THROUGN_CONNECT_END.ordinal(),""));
                }
            }
        }else if (connect.getType() == Connect.TYPE.CONNECT_PONG.ordinal()){
            Log.e(TAG,"Client handle connect_pong msg:" + connect.toString());
            //收到pong,判断Client是否有该连接存在。这个用来判断HOLE_PUNCH和DIRECT类型是否已经连接成功
            if (Client.getInstance().isConnecting(Client.getInstance().connectingMaps,connect)){
                //建立连接成功,目前客户端应该只有1个ConnectMap
                //todo 可以增加心跳
                Client.getInstance().connectedMaps.add(Client.getInstance().connectingMaps.remove(0));
                EventBus.getDefault().post(new BusEvent(BusEvent.Type.THROUGN_CONNECT_END.ordinal(),""));
            }
            //给服务器发送建立连接成功的消息
            connect.setType(Connect.TYPE.CONNECTED.ordinal());
            ttmsg.setContent(JSON.toJSONString(connect));
            ctx.writeAndFlush(MessageUtil.throughmsg2Packet(ttmsg,Client.getInstance().SERVER1));
        }else{
            throw new Exception("Client handle unknown connect operate:" + connect.toString());
        }
    }



}
