package ppex.client.process;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import ppex.client.entity.Client;
import ppex.proto.entity.through.Connect;
import ppex.proto.entity.through.Connection;
import ppex.proto.type.ThroughTypeMsg;
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

    public void connectOtherPeer(ChannelHandlerContext ctx, Connection connection) {
        Log.d(TAG,"client connect other peer");
        try {
            ThroughTypeMsg throughTypeMsg = new ThroughTypeMsg();
            throughTypeMsg.setAction(ThroughTypeMsg.ACTION.CONNECT_CONN.ordinal());
            Connect connect = new Connect();
            connect.setType(Connect.TYPE.REQUEST_CONNECT_SERVER.ordinal());
            List<Connection> connections = new ArrayList<>();
            connections.add(Client.getInstance().localConnection);
            connections.add(connection);
            connect.setContent(JSON.toJSONString(connections));
            throughTypeMsg.setContent(JSON.toJSONString(connect));
            ctx.writeAndFlush(MessageUtil.throughmsg2Packet(throughTypeMsg, Client.getInstance().SERVER1));
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
