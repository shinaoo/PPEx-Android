package ppex.androidcomponent.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import ppex.androidcomponent.handler.AndroidRequest;
import ppex.proto.msg.entity.txt.Request;

public class RequestUtil {
    //将AndroidRequest转成Request
    public static Request androidRequest2Request(AndroidRequest androidRequest) {
        Request request = new Request();
        StringBuffer sbHead = new StringBuffer(), sbBody = new StringBuffer();
        sbHead.append(androidRequest.getAction());
        sbHead.append("\r\n");
        androidRequest.getParams().entrySet().stream().forEach(entry -> {
            sbBody.append(entry.getKey() + "=" + entry.getValue() + "&");
        });
        request.setHead(sbHead.toString());
        request.setBody(sbBody.toString());
        return request;
    }

    //参照上面androidRequest2Request来解
    public static AndroidRequest request2AndroidRequest(Request request) {
        String[] heads = request.getHead().split("\r\n");
        String[] paramStrs = request.getBody().split("&");
        Map<String, String> params = new HashMap<>();
        Arrays.stream(paramStrs).collect(Collectors.toList()).forEach(param -> {
            if (!"".equals(param)) {
                String[] keyvalue = param.split("=");
                if (keyvalue.length == 2) {
                    params.put(keyvalue[0],keyvalue[1]);
                }
            }
        });
        AndroidRequest androidRequest = new AndroidRequest(heads[0], null, params);
        return androidRequest;
    }
}
