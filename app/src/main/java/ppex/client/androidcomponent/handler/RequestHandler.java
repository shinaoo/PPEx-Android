package ppex.client.androidcomponent.handler;

import com.alibaba.fastjson.JSON;

import io.netty.channel.Channel;
import ppex.client.entity.Client;
import ppex.proto.entity.through.Connect;
import ppex.proto.entity.through.Connection;
import ppex.proto.entity.txt.Request;
import ppex.proto.type.TxtTypeMsg;
import ppex.utils.MessageUtil;

public class RequestHandler {
    private static RequestHandler requestHandler = null;

    private Channel channel;
    private Connection targetConnection;
    private int connectType;

    private RequestHandler() {}

    public static RequestHandler getDefault(){
        if (requestHandler == null)
            requestHandler = new RequestHandler();
        return requestHandler;
    }

    //要设置Channel以及目的地
    public void setChannel(Channel channel){
        this.channel = channel;
    }
    public void setTargetConnection(Connection connection){
        this.targetConnection = connection;
    }
    private void setConnectType(int connectType){
        this.connectType = connectType;
    }

    public void sendRequest(String request){

    }
    //同步。目前没有异步
    public void sendRequest(String request,CallBack callBack){
        Request request1 = new Request();
        request1.setHead(request);
        request1.setBody("");
        TxtTypeMsg txtTypeMsg = new TxtTypeMsg();
        txtTypeMsg.setTo(targetConnection.inetSocketAddress);
        txtTypeMsg.setContent(JSON.toJSONString(request1));
        if (connectType == Connect.TYPE.FORWARD.ordinal()){
            channel.writeAndFlush(MessageUtil.txtMsg2packet(txtTypeMsg, Client.getInstance().SERVER1));
        }else{
            channel.writeAndFlush(MessageUtil.txtMsg2packet(txtTypeMsg,targetConnection.inetSocketAddress));
        }
    }
}
