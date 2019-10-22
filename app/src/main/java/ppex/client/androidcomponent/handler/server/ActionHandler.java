package ppex.client.androidcomponent.handler.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ppex.client.androidcomponent.handler.AndroidRequest;
import ppex.client.androidcomponent.utils.RequestUtil;
import ppex.proto.entity.txt.Request;
import ppex.proto.entity.txt.RequestHandle;
import ppex.proto.entity.txt.Response;

public class ActionHandler implements RequestHandle {

    private Map<String, RequestHandle> handles = new HashMap<>();
    private List<String> prefixActions = new ArrayList<>();

    private static ActionHandler actionHandler = null;

    private ActionHandler() {
    }

    public static ActionHandler getInstance() {
        if (actionHandler == null)
            actionHandler = new ActionHandler();
        return actionHandler;
    }

    public void addPrefixActionHandle(String prefix, RequestHandle handle) {
        handles.put(prefix, handle);
        prefixActions.add(prefix);
    }

    @Override
    public Response handleRequest(Request request) {
        AndroidRequest request1 = RequestUtil.request2AndroidRequest(request);
        String prefix = prefixActions.stream().filter(pre -> request1.getAction().startsWith(pre)).findFirst().get();
        if (prefix == null)
            return null;
        return handles.get(prefix).handleRequest(request);
    }
}
