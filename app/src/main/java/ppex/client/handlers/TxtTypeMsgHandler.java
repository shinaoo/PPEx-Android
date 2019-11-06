package ppex.client.handlers;

import android.util.Log;

import com.alibaba.fastjson.JSON;

import io.netty.channel.ChannelHandlerContext;
import ppex.androidcomponent.handler.client.ResponseHandler;
import ppex.androidcomponent.handler.server.ActionHandler;
import ppex.androidcomponent.handler.server.actions.FileHandler;
import ppex.proto.msg.entity.txt.Request;
import ppex.proto.msg.entity.txt.Response;
import ppex.proto.msg.type.TxtTypeMsg;
import ppex.proto.msg.type.TypeMessage;
import ppex.proto.msg.type.TypeMessageHandler;
import ppex.proto.rudp.RudpPack;
import ppex.utils.MessageUtil;

import java.net.InetSocketAddress;

public class TxtTypeMsgHandler implements TypeMessageHandler {

    private static String TAG = TxtTypeMsgHandler.class.getName();

    public TxtTypeMsgHandler() {
        ActionHandler.getInstance();
        ActionHandler.getInstance().addPrefixActionHandle("/file",new FileHandler());

        ResponseHandler.getInstance();
    }

    public void handleTypeMessage(ChannelHandlerContext ctx, TypeMessage typeMessage, InetSocketAddress fromAddress) throws Exception {
        Log.e(TAG,"TxtTypeMsgHandle handle typemsg:" + typeMessage.getBody());
        TxtTypeMsg txtTypeMsg = JSON.parseObject(typeMessage.getBody(),TxtTypeMsg.class);
        handlerTxtTypeMsg(ctx,txtTypeMsg,fromAddress);
    }

    private void handlerTxtTypeMsg(ChannelHandlerContext ctx,TxtTypeMsg tmsg,InetSocketAddress fromaddress){
        Log.e(TAG,"TxtTypeMsgHandle handle txtmsg:" + tmsg.getContent());
        if (tmsg.isReq()){
            //需要处理Request变成Response返回去
            tmsg.setReq(false);
            Request request = JSON.parseObject(tmsg.getContent(),Request.class);
            Response response = ActionHandler.getInstance().handleRequest(request);
            tmsg.setContent(JSON.toJSONString(response));
            ctx.writeAndFlush(MessageUtil.txtMsg2packet(tmsg,fromaddress));
        }else{
            //得到的Response
            Response response = JSON.parseObject(tmsg.getContent(),Response.class);
            ResponseHandler.getInstance().handleResponse(response);
        }
    }


    @Override
    public void handleTypeMessage(RudpPack rudpPack, TypeMessage tmsg) {

    }
}
