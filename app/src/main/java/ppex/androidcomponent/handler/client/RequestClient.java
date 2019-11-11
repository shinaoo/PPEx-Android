package ppex.androidcomponent.handler.client;

import android.os.Environment;
import android.util.Log;

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
import ppex.proto.msg.entity.Connection;
import ppex.proto.msg.entity.through.Connect;
import ppex.proto.msg.entity.txt.Request;
import ppex.proto.msg.type.TxtTypeMsg;
import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.RudpPack;
import ppex.utils.MessageUtil;

public class RequestClient {
    private static String TAG = RequestClient.class.getName();

    private static RequestClient requestClient = null;

    private Channel channel;
    private Connection targetConnection;
    private int connectType;
    private RudpPack rudpPack;
    private IAddrManager addrManager;

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
    public void setConnectType(int connectType){
        this.connectType = connectType;
    }

    public void setRudpPack(RudpPack rudpPack) {
        this.rudpPack = rudpPack;
    }

    public void setAddrManager(IAddrManager addrManager) {
        this.addrManager = addrManager;
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
        txtTypeMsg.setReq(true);
        txtTypeMsg.setFrom(Client.getInstance().localConnection.getAddress());
        txtTypeMsg.setTo(targetConnection.getAddress());
        txtTypeMsg.setContent(JSON.toJSONString(request1));
        Log.e(TAG,"txtTYpemsg:" + txtTypeMsg.getContent());
        if (connectType == Connect.TYPE.FORWARD.ordinal()){
//            channel.writeAndFlush(MessageUtil.txtMsg2packet(txtTypeMsg, Client.getInstance().SERVER1));
            RudpPack rudpPack = addrManager.get(Client.getInstance().SERVER1);
            rudpPack.write(MessageUtil.txtmsg2Msg(txtTypeMsg));
        }else{
//            channel.writeAndFlush(MessageUtil.txtMsg2packet(txtTypeMsg,targetConnection.getAddress()));
            RudpPack rudpPack = addrManager.get(targetConnection.getAddress());
            rudpPack.write(MessageUtil.txtmsg2Msg(txtTypeMsg));
        }
    }

}
