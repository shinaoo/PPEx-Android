package ppex.client.process;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import ppex.client.entity.Client;
import ppex.proto.msg.entity.through.ConnectMap;
import ppex.proto.msg.entity.through.Connect;
import ppex.proto.msg.entity.Connection;
import ppex.proto.msg.type.ThroughTypeMsg;
import ppex.utils.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class ThroughProcess {

    private static String TAG = ThroughProcess.class.getName();

    private static ThroughProcess instance = null;

    private ThroughProcess() {
    }

    public static ThroughProcess getInstance() {
        if (instance == null)
            instance = new ThroughProcess();
        return instance;
    }

    private Channel channel;

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void sendSaveInfo() {
        Log.d(TAG,"client send save info");
        try {
            ThroughTypeMsg throughTypeMsg = new ThroughTypeMsg();
            throughTypeMsg.setAction(ThroughTypeMsg.ACTION.SAVE_CONNINFO.ordinal());
            throughTypeMsg.setContent(JSON.toJSONString(Client.getInstance().localConnection));
            this.channel.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg, Client.getInstance().SERVER1));
            if (!channel.closeFuture().await(2000)) {
                System.out.println("查询超时");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getConnectionsFromServer(Channel ch){
        Log.d(TAG,"client get ids from server by channel");
        try {
            ThroughTypeMsg throughTypeMsg = new ThroughTypeMsg();
            throughTypeMsg.setAction(ThroughTypeMsg.ACTION.GET_CONNINFO.ordinal());
            throughTypeMsg.setContent("");
            ch.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg, Client.getInstance().SERVER1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getConnectionsFromServer(ChannelHandlerContext ctx) {
        Log.d(TAG,"client get ids from server");
        try {
            ThroughTypeMsg throughTypeMsg = new ThroughTypeMsg();
            throughTypeMsg.setAction(ThroughTypeMsg.ACTION.GET_CONNINFO.ordinal());
            throughTypeMsg.setContent("");
            ctx.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg, Client.getInstance().SERVER1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connectPeer(Channel channel,Connection connection){
        Log.d(TAG,"client connect peer");
        try {
//            ThroughTypeMsg throughTypeMsg = new ThroughTypeMsg();
//            throughTypeMsg.setAction(ThroughTypeMsg.ACTION.CONNECT_CONN.ordinal());
//            Connect connect = new Connect();
////            connect.setType(Connect.TYPE.REQUEST_CONNECT_SERVER.ordinal());
//            List<Connection> connections = new ArrayList<>();
//            connections.add(Client.getInstance().localConnection);
//            connections.add(connection);
//            connect.setContent(JSON.toJSONString(connections));
//            throughTypeMsg.setContent(JSON.toJSONString(connect));
//            channel.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg, Client.getInstance().SERVER1));
            ThroughTypeMsg throughTypeMsg = new ThroughTypeMsg();
            throughTypeMsg.setAction(ThroughTypeMsg.ACTION.CONNECT_CONN.ordinal());
            Connect.TYPE connectType = Client.judgeConnectType(Client.getInstance().localConnection,connection);
            Connect connect = new Connect();

            List<Connection> connections = new ArrayList<>();
            connections.add(Client.getInstance().localConnection);
            connections.add(connection);
            String connectionsStr = JSON.toJSONString(connections);
            //将建立连接的两边保存,保存在进行中的map中
            ConnectMap connectMap = new ConnectMap(connectType.ordinal(),connections);
            Client.getInstance().connectingMaps.add(connectMap);

            if (connectType == Connect.TYPE.DIRECT){
                connect.setType(Connect.TYPE.CONNECT_PING.ordinal());
                connect.setContent("");
                throughTypeMsg.setContent(JSON.toJSONString(connect));
                //等待返回pong就确认建立连接
                channel.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg,connection.inetSocketAddress));

                //发送给Server端，表明正在建立连接
                connect.setType(Connect.TYPE.CONNECTING.ordinal());
                connect.setContent(connectionsStr);
                throughTypeMsg.setContent(JSON.toJSONString(connect));
                channel.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg,Client.getInstance().SERVER1));

            }else if (connectType == Connect.TYPE.HOLE_PUNCH){
                connect.setType(connectType.ordinal());
                connect.setContent(connectionsStr);
                throughTypeMsg.setContent(JSON.toJSONString(connect));
                //先将消息发给服务，由服务转发给target connection打洞
                channel.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg,Client.getInstance().SERVER1));

                connect.setType(Connect.TYPE.CONNECTING.ordinal());
                throughTypeMsg.setContent(JSON.toJSONString(connect));
                channel.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg,Client.getInstance().SERVER1));
            }else if (connectType == Connect.TYPE.REVERSE){
                //首先向B 打洞
                connect.setType(Connect.TYPE.CONNECT_PING.ordinal());
                connect.setContent(connectionsStr);
                throughTypeMsg.setContent(JSON.toJSONString(connect));
                //率先打洞
                channel.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg,connection.inetSocketAddress));

                //让server给B转发，由B 再通信
                connect.setType(connectType.ordinal());
                connect.setContent(connectionsStr);
                throughTypeMsg.setContent(JSON.toJSONString(connect));
                channel.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg,Client.getInstance().SERVER1));

                connect.setType(Connect.TYPE.CONNECTING.ordinal());
                connect.setContent(connectionsStr);
                throughTypeMsg.setContent(JSON.toJSONString(connect));
                channel.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg,Client.getInstance().SERVER1));
            }else if (connectType == Connect.TYPE.FORWARD){
                connect.setType(Connect.TYPE.FORWARD.ordinal());
                connect.setContent(connectionsStr);
                throughTypeMsg.setContent(JSON.toJSONString(connect));
                channel.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg,Client.getInstance().SERVER1));

                connect.setType(Connect.TYPE.CONNECTING.ordinal());
                throughTypeMsg.setContent(JSON.toJSONString(connect));
                channel.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg,Client.getInstance().SERVER1));
            }else{
                throw new Exception("unknown connect operate:" + connectionsStr);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testConnectPeer(){
        try {

        }catch (Exception e){

        }
    }


}
