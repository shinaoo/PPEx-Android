package ppex.client.androidcomponent.handler.server.actions;

import android.os.Environment;

import com.alibaba.fastjson.JSON;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ppex.client.androidcomponent.entity.Files;
import ppex.client.androidcomponent.handler.AndroidRequest;
import ppex.client.androidcomponent.utils.RequestUtil;
import ppex.proto.entity.txt.Request;
import ppex.proto.entity.txt.RequestHandle;
import ppex.proto.entity.txt.Response;

public class FileHandler implements RequestHandle {

    private File sdFile = null;

    public FileHandler() {
        sdFile = Environment.getExternalStorageDirectory();
    }

    @Override
    public Response handleRequest(Request request) {
        AndroidRequest request1 = RequestUtil.request2AndroidRequest(request);
        if (!request1.getAction().startsWith("/file"))
            return null;
        if ("/file/getfiles".equals(request1.getAction())){
            return handleGetFiles(request1);
        }
        return null;
    }

    private Response handleGetFiles(AndroidRequest request){
        String path = request.getParams().get("path");
        if (path == null)
            return null;
        Response response = new Response();
        response.setHead(request.getAction()+"\r\n");
        if ("".equals(path)){
            File[] sdfiles = sdFile.listFiles();
            List<Files> responsefile = new ArrayList<>();
            Arrays.stream(sdfiles).forEach(file -> {
                Files files = new Files();
                files.setDirectory(file.isDirectory());
                files.setName(file.getName());
                responsefile.add(files);
            });
            response.setBody(JSON.toJSONString(responsefile));
            return response;
        }
        return response;
    }
}
