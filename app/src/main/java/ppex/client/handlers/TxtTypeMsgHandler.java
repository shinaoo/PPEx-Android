package ppex.client.handlers;

import android.util.Log;

import com.alibaba.fastjson.JSON;

import org.greenrobot.eventbus.EventBus;

import io.netty.channel.ChannelHandlerContext;
import ppex.androidcomponent.busevent.BusEvent;
import ppex.androidcomponent.handler.client.ResponseHandler;
import ppex.androidcomponent.handler.server.ActionHandler;
import ppex.androidcomponent.handler.server.actions.FileHandler;
import ppex.proto.msg.type.TxtTypeMsg;
import ppex.proto.msg.type.TypeMessage;
import ppex.proto.msg.type.TypeMessageHandler;
import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.RudpPack;

public class TxtTypeMsgHandler implements TypeMessageHandler {

    private static String TAG = TxtTypeMsgHandler.class.getName();

    public TxtTypeMsgHandler() {
        ActionHandler.getInstance();
        ActionHandler.getInstance().addPrefixActionHandle("/file",new FileHandler());

        ResponseHandler.getInstance();
    }

    private void handlerTxtTypeMsg(ChannelHandlerContext ctx,TxtTypeMsg tmsg,RudpPack rudpPack,IAddrManager addrManager){
        Log.e(TAG,"TxtTypeMsgHandle handle txtmsg:" + tmsg.getContent());
            EventBus.getDefault().post(new BusEvent(BusEvent.Type.TXT_RESPONSE.getValue(),tmsg.getContent()));
//        if (tmsg.isReq()){
//            //需要处理Request变成Response返回去
//            tmsg.setReq(false);
////            Request request = JSON.parseObject(tmsg.getContent(),Request.class);
////            Response response = ActionHandler.getInstance().handleRequest(request);
////            tmsg.setContent(JSON.toJSONString(response));
////            ctx.writeAndFlush(MessageUtil.txtMsg2packet(tmsg,fromaddress));
//            rudpPack = addrManager.get(tmsg.getTo());
//            rudpPack.write(MessageUtil.txtmsg2Msg(tmsg));
//        }else{
//            //得到的Response
////            Response response = JSON.parseObject(tmsg.getContent(),Response.class);
////            ResponseHandler.getInstance().handleResponse(response);
//        }
    }

    @Override
    public void handleTypeMessage(ChannelHandlerContext ctx, RudpPack rudpPack, IAddrManager addrManager, TypeMessage tmsg) {
        Log.e(TAG,"TxtTypeMsgHandle handle typemsg:" + tmsg.getBody());
        TxtTypeMsg txtTypeMsg = JSON.parseObject(tmsg.getBody(),TxtTypeMsg.class);
        handlerTxtTypeMsg(ctx,txtTypeMsg,rudpPack,addrManager);
    }
}
