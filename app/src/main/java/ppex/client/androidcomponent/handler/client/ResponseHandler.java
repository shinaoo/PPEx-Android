package ppex.client.androidcomponent.handler.client;

import org.greenrobot.eventbus.EventBus;

import ppex.client.androidcomponent.busevent.BusEvent;

import ppex.proto.entity.txt.Response;
import ppex.proto.entity.txt.ResponseHandle;

public class ResponseHandler implements ResponseHandle {

    private static ResponseHandler responseHandler = null;
    private ResponseHandler(){}

    public static ResponseHandler getInstance(){
        if (responseHandler == null)
            responseHandler = new ResponseHandler();
        return responseHandler;
    }

    @Override
    public void handleResponse(Response response) {
        //这里处理获取到的response.如何跟Activity联系起来返回数据
        if (response.getHead().startsWith("/file/getfiles")){
            //先利用EventBus返回信息
            EventBus.getDefault().post(new BusEvent(BusEvent.Type.FILE_GETFILES.getValue(),response.getBody()));
        }

    }
}
