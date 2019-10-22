package ppex.client.androidcomponent.handler;

import java.util.Map;

public class AndroidRequest {
    private String action;
    private CallBack callBack;
    private Map<String,String> params;
    public AndroidRequest(String action, CallBack callBack, Map<String, String> params) {
        this.action = action;
        this.callBack = callBack;
        this.params = params;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public CallBack getCallBack() {
        return callBack;
    }

    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }
}
