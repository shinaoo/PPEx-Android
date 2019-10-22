package ppex.client.handlers;

import com.alibaba.fastjson.JSON;

import io.netty.channel.ChannelHandlerContext;
import ppex.client.androidcomponent.handler.client.ResponseHandler;
import ppex.client.androidcomponent.handler.server.ActionHandler;
import ppex.client.androidcomponent.handler.server.actions.FileHandler;
import ppex.proto.entity.txt.Request;
import ppex.proto.entity.txt.Response;
import ppex.proto.type.TxtTypeMsg;
import ppex.proto.type.TypeMessage;
import ppex.proto.type.TypeMessageHandler;
import ppex.utils.MessageUtil;

import java.net.InetSocketAddress;

public class TxtTypeMsgHandler implements TypeMessageHandler {

    public TxtTypeMsgHandler() {
        ActionHandler.getInstance();
        ActionHandler.getInstance().addPrefixActionHandle("/file",new FileHandler());

        ResponseHandler.getInstance();
    }

    @Override
    public void handleTypeMessage(ChannelHandlerContext ctx, TypeMessage typeMessage, InetSocketAddress fromAddress) throws Exception {
        TxtTypeMsg txtTypeMsg = JSON.parseObject(typeMessage.getBody(),TxtTypeMsg.class);
        handlerTxtTypeMsg(ctx,txtTypeMsg,fromAddress);
    }

    private void handlerTxtTypeMsg(ChannelHandlerContext ctx,TxtTypeMsg tmsg,InetSocketAddress fromaddress){
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




}
