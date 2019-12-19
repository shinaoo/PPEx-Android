package ppex.androidcomponent.handler.client;

import android.os.Environment;

import java.io.File;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import ppex.androidcomponent.handler.AndroidRequest;
import ppex.androidcomponent.handler.CallBack;
import ppex.client.Client;

public class RequestClient {
    private static String TAG = RequestClient.class.getName();

    private static RequestClient requestClient = null;

    private Client client;

    File sdFile = null;

    private RequestClient() {
        sdFile = Environment.getExternalStorageDirectory();
    }

    public static RequestClient getDefault(){
        if (requestClient == null)
            requestClient = new RequestClient();
        return requestClient;
    }

    public void setClient(Client client){
        this.client = client;
    }

    private Queue<AndroidRequest> requests = new LinkedBlockingQueue<>(10);

    public void sendRequest(String request){

    }
    //同步。目前没有异步
    public void sendRequest(String request, CallBack callBack, Map<String,String> params){
//        AndroidRequest androidRequest = new AndroidRequest(request,callBack,params);
//        requests.add(androidRequest);
//        Request request1 = RequestUtil.androidRequest2Request(androidRequest);
//        TxtTypeMsg txtTypeMsg = new TxtTypeMsg();
//        txtTypeMsg.setReq(true);
//        txtTypeMsg.setFrom(Client.getInstance().localConnection.getAddress());
//        txtTypeMsg.setTo(targetConnection.getAddress());
//        txtTypeMsg.setContent(JSON.toJSONString(request1));
//        Log.e(TAG,"txtTYpemsg:" + txtTypeMsg.getContent());
//        if (connectType == Connect.TYPE.FORWARD.ordinal()){
////            channel.writeAndFlush(MessageUtil.txtMsg2packet(txtTypeMsg, Client.getInstance().SERVER1));
//            RudpPack rudpPack = addrManager.get(Client.getInstance().SERVER1);
//            rudpPack.write(MessageUtil.txtmsg2Msg(txtTypeMsg));
//        }else{
////            channel.writeAndFlush(MessageUtil.txtMsg2packet(txtTypeMsg,targetConnection.getAddress()));
//            RudpPack rudpPack = addrManager.get(targetConnection.getAddress());
//            rudpPack.write(MessageUtil.txtmsg2Msg(txtTypeMsg));
//        }
    }

}
