package ppex.client.handlers;


import android.util.Log;

import com.alibaba.fastjson.JSON;

import org.apache.log4j.Logger;
import org.greenrobot.eventbus.EventBus;

import java.util.List;

import io.netty.channel.Channel;
import ppex.androidcomponent.busevent.BusEvent;
import ppex.client.Client;
import ppex.client.process.ThroughProcess;
import ppex.client.rudp.ClientOutput;
import ppex.proto.entity.Connection;
import ppex.proto.entity.through.Connect;
import ppex.proto.entity.through.RecvInfo;
import ppex.proto.msg.type.ThroughTypeMsg;
import ppex.proto.msg.type.TypeMessage;
import ppex.proto.msg.type.TypeMessageHandler;
import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.IOutput;
import ppex.proto.rudp.RudpPack;
import ppex.utils.MessageUtil;

public class ThroughTypeMsgHandler implements TypeMessageHandler {

    private static Logger LOGGER = Logger.getLogger(ThroughTypeMsgHandler.class);

    @Override
    public void handleTypeMessage(RudpPack rudpPack, IAddrManager addrManager, TypeMessage tmsg) {
        ThroughTypeMsg ttmsg = JSON.parseObject(tmsg.getBody(), ThroughTypeMsg.class);
        Log.e("MyTag", "client handle through msg:" + ttmsg.getContent());
        if (ttmsg.getAction() == ThroughTypeMsg.ACTION.RECV_INFO.ordinal()) {
            RecvInfo recvinfo = JSON.parseObject(ttmsg.getContent(), RecvInfo.class);
            if (recvinfo.type == ThroughTypeMsg.RECVTYPE.SAVE_CONNINFO.ordinal()) {
                handleSaveInfoFromServer(rudpPack, recvinfo, addrManager);
            } else if (recvinfo.type == ThroughTypeMsg.RECVTYPE.GET_CONNINFO.ordinal()) {
                handleGetInfoFromServer(rudpPack, recvinfo, addrManager);
            } else if (recvinfo.type == ThroughTypeMsg.RECVTYPE.CONNECT_CONN.ordinal()) {
                handleConnectFromServer(rudpPack, recvinfo, addrManager);
            } else {
//                throw new Exception("Unkown through msg action:" + ttmsg.toString());
            }
        } else if (ttmsg.getAction() == ThroughTypeMsg.ACTION.CONNECT_CONN.ordinal()) {
            try {
                handleConnecCONN(ttmsg, rudpPack, addrManager);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleSaveInfoFromServer(RudpPack rudpPack, RecvInfo recvinfo, IAddrManager addrManager) {
        if (recvinfo.type != ThroughTypeMsg.RECVTYPE.SAVE_CONNINFO.ordinal()) {
            return;
        }
        Log.e("MyTag", "client handle server through msg save info:" + recvinfo.recvinfos);
        if (!recvinfo.recvinfos.equals("success")) {
            ThroughProcess.getInstance().sendSaveInfo();
        } else {
            Log.e("MyTag", "client handle server through msg save info succ");
        }
    }

    private void handleGetInfoFromServer(RudpPack rudpPack, RecvInfo recvinfo, IAddrManager addrManager) {
        if (recvinfo.type != ThroughTypeMsg.RECVTYPE.GET_CONNINFO.ordinal())
            return;
        List<Connection> connections = JSON.parseArray(recvinfo.recvinfos, Connection.class);
        EventBus.getDefault().post(new BusEvent(BusEvent.Type.THROUGN_GET_CONN_INFO.getValue(), connections));
    }

    private void handleConnectFromServer(RudpPack rudpPack, RecvInfo recvinfo, IAddrManager addrManager) {
        //从服务转发而来的Connect_CONN信息
        if (recvinfo.type == ThroughTypeMsg.RECVTYPE.CONNECT_CONN.ordinal()) {
            Connect connect = JSON.parseObject(recvinfo.recvinfos, Connect.class);
            List<Connection> connections = JSON.parseArray(connect.getContent(), Connection.class);
            Channel channel = rudpPack.getOutput().getChannel();

            ThroughTypeMsg ttmsg = new ThroughTypeMsg();
            ttmsg.setAction(ThroughTypeMsg.ACTION.CONNECT_CONN.ordinal());
            if (connect.getType() == Connect.TYPE.HOLE_PUNCH.ordinal()) {
                connect.setType(Connect.TYPE.CONNECT_PING_NORMAL.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));

                rudpPack = addrManager.get(connections.get(0).getAddress());
                if (rudpPack == null) {
                    IOutput output = new ClientOutput(channel, connections.get(0));
//                    rudpPack = new RudpPack(output, Client.getInstance().getExecutor(), Client.getInstance().getResponseListener());
                    rudpPack = RudpPack.newInstance(output,Client.getInstance().getExecutor(),Client.getInstance().getResponseListener(),addrManager);
                    Client.getInstance().getAddrManager().New(connections.get(0).getAddress(), rudpPack);
                    rudpPack.send2(MessageUtil.throughmsg2Msg(ttmsg));
//                    rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
//                    RudpScheduleTask rudpScheduleTask = new RudpScheduleTask(Client.getInstance().getExecutor(), rudpPack, addrManager);
//                    Client.getInstance().getExecutor().executeTimerTask(rudpScheduleTask, rudpPack.getInterval());
                } else {
//                    rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
                    rudpPack.send2(MessageUtil.throughmsg2Msg(ttmsg));
                }

                Log.e("MyTag", "------------->hole_punch SND connect ping normal");
                connect.setType(Connect.TYPE.RETURN_HOLE_PUNCH.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));

                rudpPack = addrManager.get(Client.getInstance().getAddrServer1());
                rudpPack.send2(MessageUtil.throughmsg2Msg(ttmsg));
            } else if (connect.getType() == Connect.TYPE.RETURN_HOLE_PUNCH.ordinal()) {
                //开始给B 发ping消息
                connect.setType(Connect.TYPE.CONNECT_PING_NORMAL.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));
                rudpPack = addrManager.get(connections.get(1).getAddress());
                if (rudpPack == null) {
                    IOutput output = new ClientOutput(channel, connections.get(1));
//                    rudpPack = new RudpPack(output, Client.getInstance().getExecutor(), Client.getInstance().getResponseListener());
                    rudpPack = RudpPack.newInstance(output,Client.getInstance().getExecutor(),Client.getInstance().getResponseListener(),addrManager);
                    addrManager.New(connections.get(1).getAddress(), rudpPack);
//                    rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
                    rudpPack.send2(MessageUtil.throughmsg2Msg(ttmsg));
//                    RudpScheduleTask rudpScheduleTask = new RudpScheduleTask(Client.getInstance().getExecutor(), rudpPack, addrManager);
//                    Client.getInstance().getExecutor().executeTimerTask(rudpScheduleTask, rudpPack.getInterval());
                } else {
//                    rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
                    rudpPack.send2(MessageUtil.throughmsg2Msg(ttmsg));
                }
                Log.e("MyTag", "------------->return_hole_punch SND connect ping normal");
            } else if (connect.getType() == Connect.TYPE.REVERSE.ordinal()) {
                connect.setType(Connect.TYPE.CONNECT_PING_REVERSE.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));
                rudpPack = addrManager.get(connections.get(0).getAddress());
                if (rudpPack == null) {
                    IOutput output = new ClientOutput(channel, connections.get(0));
//                    rudpPack = new RudpPack(output, Client.getInstance().getExecutor(), Client.getInstance().getResponseListener());
                    rudpPack = RudpPack.newInstance(output,Client.getInstance().getExecutor(),Client.getInstance().getResponseListener(),addrManager);
                    addrManager.New(connections.get(0).getAddress(), rudpPack);
//                    rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
                    rudpPack.send2(MessageUtil.throughmsg2Msg(ttmsg));
//                    RudpScheduleTask rudpScheduleTask = new RudpScheduleTask(Client.getInstance().getExecutor(), rudpPack, addrManager);
//                    Client.getInstance().getExecutor().executeTimerTask(rudpScheduleTask, rudpPack.getInterval());
                } else {
//                    rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
                    rudpPack.send2(MessageUtil.throughmsg2Msg(ttmsg));
                }
                Log.e("MyTag", "------------->reverse SND connect ping reverse");
            } else if (connect.getType() == Connect.TYPE.FORWARD.ordinal()) {
                connect.setType(Connect.TYPE.RETURN_FORWARD.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));
                rudpPack = addrManager.get(Client.getInstance().getAddrServer1());
//                rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
                rudpPack.send2(MessageUtil.throughmsg2Msg(ttmsg));
                //forward的接收方已经表示连接了
//                EventBus.getDefault().post(new BusEvent(BusEvent.Type.THROUGN_RCV_CONNECT_PONG.getValue()));
                Log.e("MyTag", "------------->forward");
            } else if (connect.getType() == Connect.TYPE.RETURN_FORWARD.ordinal()) {
                connect.setType(Connect.TYPE.CONNECTED.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));
                rudpPack = addrManager.get(Client.getInstance().getAddrServer1());
//                rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
                rudpPack.send2(MessageUtil.throughmsg2Msg(ttmsg));

                //这个是判断forward类型是否已经连接成功
                Client.getInstance().setConnTarget(connections.get(1));
                Client.getInstance().setConnType2Target(Connect.TYPE.FORWARD);
                //表示已经连接上了forward
                EventBus.getDefault().post(new BusEvent(BusEvent.Type.THROUGN_RCV_CONNECT_PONG.getValue()));
                Log.e("MyTag", "------------->return forward");
            }
        }
    }

    private void handleConnecCONN(ThroughTypeMsg ttmsg, RudpPack rudpPack, IAddrManager addrManager) throws Exception {
        Connect connect = JSON.parseObject(ttmsg.getContent(), Connect.class);
        List<Connection> connections = JSON.parseArray(connect.getContent(), Connection.class);
        Channel channel = rudpPack.getOutput().getChannel();
        //直接连接接收到connect_ping消息或者也能从Hole_punch收到connect_ping,能从Reverse那里收到connect_ping
        if (connect.getType() == Connect.TYPE.CONNECT_PING_NORMAL.ordinal()) {
            connect.setType(Connect.TYPE.CONNECT_PONG.ordinal());
            ttmsg.setContent(JSON.toJSONString(connect));

            if (!connections.get(0).getAddress().equals(Client.getInstance().getAddrLocal())){
                rudpPack = addrManager.get(connections.get(0).getAddress());
                if (rudpPack == null) {
                    IOutput output = new ClientOutput(channel, connections.get(0));
//                rudpPack = new RudpPack(output, Client.getInstance().getExecutor(), Client.getInstance().getResponseListener());
                    rudpPack = RudpPack.newInstance(output,Client.getInstance().getExecutor(),Client.getInstance().getResponseListener(),addrManager);
                    addrManager.New(connections.get(0).getAddress(), rudpPack);
//                rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
                    rudpPack.send2(MessageUtil.throughmsg2Msg(ttmsg));
//                RudpScheduleTask rudpScheduleTask = new RudpScheduleTask(Client.getInstance().getExecutor(), rudpPack, addrManager);
//                Client.getInstance().getExecutor().executeTimerTask(rudpScheduleTask, rudpPack.getInterval());
                } else {
//                rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
                    rudpPack.send2(MessageUtil.throughmsg2Msg(ttmsg));
                }
            }
            Log.e("MyTag", "----------->rcv connectping normal return connect pong");
            //todo 之前有一个ConnectedMap保存已经连接上的连接.现在去掉了,之后再考虑如何
//            if (Client.getInstance().isConnecting(Client.getInstance().connectingMaps,connect)){
//                //还需要判断是不是REVERSE类型
//                ConnectMap connectMap = Client.getInstance().connectingMaps.get(0);
//                if (connectMap.getConnectType() == Connect.TYPE.REVERSE.ordinal()){
//                    Client.getInstance().connectedMaps.add(Client.getInstance().connectingMaps.remove(0));
//                }
//            }
        } else if (connect.getType() == Connect.TYPE.CONNECT_PING_REVERSE.ordinal()) {
            connect.setType(Connect.TYPE.CONNECT_PONG.ordinal());
            ttmsg.setContent(JSON.toJSONString(connect));

            rudpPack = addrManager.get(connections.get(1).getAddress());
            if (rudpPack == null) {
                IOutput output = new ClientOutput(channel, connections.get(1));
//                rudpPack = new RudpPack(output, Client.getInstance().getExecutor(), Client.getInstance().getResponseListener());
                rudpPack = RudpPack.newInstance(output,Client.getInstance().getExecutor(),Client.getInstance().getResponseListener(),addrManager);
                addrManager.New(connections.get(1).getAddress(), rudpPack);
//                rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
                rudpPack.send2(MessageUtil.throughmsg2Msg(ttmsg));
//                RudpScheduleTask rudpScheduleTask = new RudpScheduleTask(Client.getInstance().getExecutor(), rudpPack, addrManager);
//                Client.getInstance().getExecutor().executeTimerTask(rudpScheduleTask, rudpPack.getInterval());
            } else {
//                rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
                rudpPack.send2(MessageUtil.throughmsg2Msg(ttmsg));
            }
            Log.e("MyTag", "----------->rcv connectping reverse return connect pong");
        } else if (connect.getType() == Connect.TYPE.CONNECT_PONG.ordinal()) {
            //todo 收到pong,表示已经建立连接.后面考虑如何保存这个连接
            //收到pong,判断Client是否有该连接存在。这个用来判断HOLE_PUNCH和DIRECT类型是否已经连接成功
//            if (Client.getInstance().isConnecting(Client.getInstance().connectingMaps,connect)){
//                //建立连接成功,目前客户端应该只有1个ConnectMap
//                //todo 可以增加心跳
//                Client.getInstance().connectedMaps.add(Client.getInstance().connectingMaps.remove(0));
//            }
            Log.e("MyTag", "----------->rcv connect pong");
            EventBus.getDefault().post(new BusEvent(BusEvent.Type.THROUGN_RCV_CONNECT_PONG.getValue()));

            //给服务器发送建立连接成功的消息
            connect.setType(Connect.TYPE.CONNECTED.ordinal());
            ttmsg.setContent(JSON.toJSONString(connect));

            rudpPack = addrManager.get(Client.getInstance().getAddrServer1());
//            rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
            rudpPack.send2(MessageUtil.throughmsg2Msg(ttmsg));
        } else {
            throw new Exception("Client handle unknown connect operate:" + connect.toString());
        }
    }


}
