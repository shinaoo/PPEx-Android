package ppex.client.handlers;


import com.alibaba.fastjson.JSON;
import io.netty.channel.Channel;
import org.apache.log4j.Logger;
import ppex.client.Client;
import ppex.client.process.ThroughProcess;
import ppex.client.rudp.ClientOutput;
import ppex.proto.entity.Connection;
import ppex.proto.entity.through.Connect;
import ppex.proto.entity.through.ConnectMap;
import ppex.proto.entity.through.RecvInfo;
import ppex.proto.msg.type.ThroughTypeMsg;
import ppex.proto.msg.type.TypeMessage;
import ppex.proto.msg.type.TypeMessageHandler;
import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.IOutput;
import ppex.proto.rudp.RudpPack;
import ppex.utils.MessageUtil;
import ppex.utils.NatTypeUtil;

import java.util.List;

public class ThroughTypeMsgHandler implements TypeMessageHandler {

    private static Logger LOGGER = Logger.getLogger(ThroughTypeMsgHandler.class);

    @Override
    public void handleTypeMessage(RudpPack rudpPack, IAddrManager addrManager, TypeMessage tmsg) {
        ThroughTypeMsg ttmsg = JSON.parseObject(tmsg.getBody(), ThroughTypeMsg.class);
        if (ttmsg.getAction() == ThroughTypeMsg.ACTION.RECV_INFO.ordinal()) {
            RecvInfo recvinfo = JSON.parseObject(ttmsg.getContent(), RecvInfo.class);
            if (recvinfo.type == ThroughTypeMsg.RECVTYPE.SAVE_CONNINFO.ordinal()) {
                handleSaveInfoFromServer(rudpPack,recvinfo,addrManager);
            } else if (recvinfo.type == ThroughTypeMsg.RECVTYPE.GET_CONNINFO.ordinal()) {
                handleGetInfoFromServer(rudpPack,recvinfo,addrManager);
            } else if (recvinfo.type == ThroughTypeMsg.RECVTYPE.CONNECT_CONN.ordinal()) {
                handleConnectFromServer(rudpPack,recvinfo,addrManager);
            } else {
//                throw new Exception("Unkown through msg action:" + ttmsg.toString());
            }
        } else if (ttmsg.getAction() == ThroughTypeMsg.ACTION.CONNECT_CONN.ordinal()) {
            try {
                handleConnecCONN(ttmsg,rudpPack,addrManager);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void handleSaveInfoFromServer(RudpPack rudpPack,RecvInfo recvinfo, IAddrManager addrManager) {
        if (recvinfo.type != ThroughTypeMsg.RECVTYPE.SAVE_CONNINFO.ordinal()) {
            return;
        }
        if (!recvinfo.recvinfos.equals("success")) {
            ThroughProcess.getInstance().sendSaveInfo();
        } else {
            //todo 增加心跳连接
//            ThroughProcess.getInstance().getConnectionsFromServer(ctx);
        }
    }

    private void handleGetInfoFromServer(RudpPack rudpPack,RecvInfo recvinfo,IAddrManager addrManager) {
        if (recvinfo.type != ThroughTypeMsg.RECVTYPE.GET_CONNINFO.ordinal())
            return;
        List<Connection> connections = JSON.parseArray(recvinfo.recvinfos, Connection.class);
        //todo 发送要连接的Connection信息
        
    }

    private void handleConnectFromServer(RudpPack rudpPack,RecvInfo recvinfo,IAddrManager addrManager) {
//        RudpPack rudpPack;
        //从服务转发而来的Connect_CONN信息
        if (recvinfo.type == ThroughTypeMsg.RECVTYPE.CONNECT_CONN.ordinal()){
            Connect connect = JSON.parseObject(recvinfo.recvinfos,Connect.class);
            List<Connection> connections = JSON.parseArray(connect.getContent(),Connection.class);
            ThroughTypeMsg ttmsg = new ThroughTypeMsg();
            ttmsg.setAction(ThroughTypeMsg.ACTION.CONNECT_CONN.ordinal());
            if (connect.getType() == Connect.TYPE.HOLE_PUNCH.ordinal()){
                connect.setType(Connect.TYPE.CONNECT_PING.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));

                Channel channel = rudpPack.getOutput().getChannel();
                rudpPack = addrManager.get(connections.get(0).getAddress());
                if (rudpPack == null){
                    Connection connection = new Connection("Unknown",connections.get(0).getAddress(),"Unknown", NatTypeUtil.NatType.UNKNOWN.getValue());
                    IOutput output = new ClientOutput(channel,connection);
                    rudpPack = new RudpPack(output, Client.getInstance().getExecutor(),Client.getInstance().getResponseListener());
                    Client.getInstance().getAddrManager().New(connections.get(0).getAddress(),rudpPack);

                }
                rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));

                connect.setType(Connect.TYPE.RETURN_HOLE_PUNCH.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));

                rudpPack = addrManager.get(Client.getInstance().getAddrServer1());
                rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
            }else if (connect.getType() == Connect.TYPE.RETURN_HOLE_PUNCH.ordinal()){
                //开始给B 发ping消息
                connect.setType(Connect.TYPE.CONNECT_PING.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));
                Channel channel = rudpPack.getOutput().getChannel();
                rudpPack = addrManager.get(connections.get(1).getAddress());
                if (rudpPack == null){
                    Connection connection = new Connection("Unknown",connections.get(1).getAddress(),"Unknown", NatTypeUtil.NatType.UNKNOWN.getValue());
                    IOutput output = new ClientOutput(channel,connection);
                    rudpPack = new RudpPack(output, Client.getInstance().getExecutor(),Client.getInstance().getResponseListener());
                    Client.getInstance().getAddrManager().New(connections.get(0).getAddress(),rudpPack);

                    addrManager.New(connections.get(1).getAddress(), rudpPack);
                }
                rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
            }else if (connect.getType() == Connect.TYPE.REVERSE.ordinal()){
                connect.setType(Connect.TYPE.CONNECT_PING.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));
                Channel channel = rudpPack.getOutput().getChannel();
                rudpPack = addrManager.get(connections.get(0).getAddress());
                if (rudpPack == null){
                    Connection connection = new Connection("Unknown",connections.get(0).getAddress(),"Unknown", NatTypeUtil.NatType.UNKNOWN.getValue());
                    IOutput output = new ClientOutput(channel,connection);
                    rudpPack = new RudpPack(output, Client.getInstance().getExecutor(),Client.getInstance().getResponseListener());
                    Client.getInstance().getAddrManager().New(connections.get(0).getAddress(),rudpPack);
                    addrManager.New(connections.get(0).getAddress(), rudpPack);
                }
                rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
            }else if (connect.getType() == Connect.TYPE.FORWARD.ordinal()){
                connect.setType(Connect.TYPE.RETURN_FORWARD.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));
//                ctx.writeAndFlush(MessageUtil.throughmsg2Packet(ttmsg,Client.getInstance().SERVER1));
                rudpPack = addrManager.get(Client.getInstance().getAddrServer1());
                rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
            }else if (connect.getType() == Connect.TYPE.RETURN_FORWARD.ordinal()){
                connect.setType(Connect.TYPE.CONNECTED.ordinal());
                ttmsg.setContent(JSON.toJSONString(connect));
//                ctx.writeAndFlush(MessageUtil.throughmsg2Packet(ttmsg,Client.getInstance().SERVER1));
                rudpPack = addrManager.get(Client.getInstance().getAddrServer1());
                rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));

                //这个是判断forward类型是否已经连接成功
//                Client.getInstance().connectedMaps.add(new ConnectMap(Connect.TYPE.FORWARD.ordinal(),connections));

            }
        }
    }

    private void handleConnecCONN(ThroughTypeMsg ttmsg,RudpPack rudpPack,IAddrManager addrManager) throws Exception{
        Connect connect = JSON.parseObject(ttmsg.getContent(),Connect.class);
        if (connect.getType() == Connect.TYPE.CONNECT_PING.ordinal()){
            connect.setType(Connect.TYPE.CONNECT_PONG.ordinal());
            ttmsg.setContent(JSON.toJSONString(connect));

            rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
//            if (Client.getInstance().isConnecting(Client.getInstance().connectingMaps,connect)){
//                //还需要判断是不是REVERSE类型
//                ConnectMap connectMap = Client.getInstance().connectingMaps.get(0);
//                if (connectMap.getConnectType() == Connect.TYPE.REVERSE.ordinal()){
//                    Client.getInstance().connectedMaps.add(Client.getInstance().connectingMaps.remove(0));
//                }
//            }
        }else if (connect.getType() == Connect.TYPE.CONNECT_PONG.ordinal()){
            //收到pong,判断Client是否有该连接存在。这个用来判断HOLE_PUNCH和DIRECT类型是否已经连接成功
//            if (Client.getInstance().isConnecting(Client.getInstance().connectingMaps,connect)){
//                //建立连接成功,目前客户端应该只有1个ConnectMap
//                //todo 可以增加心跳
//                Client.getInstance().connectedMaps.add(Client.getInstance().connectingMaps.remove(0));
//            }
            //给服务器发送建立连接成功的消息
            connect.setType(Connect.TYPE.CONNECTED.ordinal());
            ttmsg.setContent(JSON.toJSONString(connect));

            rudpPack = addrManager.get(Client.getInstance().getAddrServer1());
            rudpPack.write(MessageUtil.throughmsg2Msg(ttmsg));
        }else{
            throw new Exception("Client handle unknown connect operate:" + connect.toString());
        }
    }



}
