package ppex.client.handlers;

import com.alibaba.fastjson.JSON;

import io.netty.channel.ChannelHandlerContext;
import ppex.proto.entity.txt.Request;
import ppex.proto.type.TxtTypeMsg;
import ppex.proto.type.TypeMessage;
import ppex.proto.type.TypeMessageHandler;

import java.net.InetSocketAddress;

public class TxtTypeMsgHandler implements TypeMessageHandler {

    @Override
    public void handleTypeMessage(ChannelHandlerContext ctx, TypeMessage typeMessage, InetSocketAddress fromAddress) throws Exception {
        TxtTypeMsg txtTypeMsg = JSON.parseObject(typeMessage.getBody(),TxtTypeMsg.class);
        handlerTxtTypeMsg(ctx,txtTypeMsg,fromAddress);
    }

    private void handlerTxtTypeMsg(ChannelHandlerContext ctx,TxtTypeMsg tmsg,InetSocketAddress fromaddress){
        if (tmsg.isReq()){
            tmsg.setReq(false);
            Request request = JSON.parseObject(tmsg.getContent(),Request.class);

        }else{

        }
    }



}
