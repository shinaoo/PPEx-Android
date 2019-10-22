package ppex.androidcomponent.handler.client;

import android.os.Environment;

import com.alibaba.fastjson.JSON;

import java.io.File;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import io.netty.channel.Channel;
import ppex.androidcomponent.handler.AndroidRequest;
import ppex.androidcomponent.handler.CallBack;
import ppex.androidcomponent.utils.RequestUtil;
import ppex.client.entity.Client;
import ppex.proto.entity.through.Connect;
import ppex.proto.entity.through.Connection;
import ppex.proto.entity.txt.Request;
import ppex.proto.type.TxtTypeMsg;
import ppex.utils.MessageUtil;

public class RequestClient {
    private static RequestClient requestClient = null;

    private Channel channel;
    private Connection targetConnection;
    private int connectType;

    File sdFile = null;

    private RequestClient() {
        sdFile = Environment.getExternalStorageDirectory();
    }

    public static RequestClient getDefault(){
        if (requestClient == null)
            requestClient = new RequestClient();
        return requestClient;
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

    private Queue<AndroidRequest> requests = new LinkedBlockingQueue<>(10);

    public void sendRequest(String request){

    }
    //同步。目前没有异步
    public void sendRequest(String request, CallBack callBack, Map<String,String> params){
        AndroidRequest androidRequest = new AndroidRequest(request,callBack,params);
        requests.add(androidRequest);
        Request request1 = RequestUtil.androidRequest2Request(androidRequest);
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
