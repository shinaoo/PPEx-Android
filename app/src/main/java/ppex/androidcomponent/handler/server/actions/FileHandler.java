package ppex.androidcomponent.handler.server.actions;

import android.os.Environment;

import com.alibaba.fastjson.JSON;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ppex.androidcomponent.handler.AndroidRequest;
import ppex.androidcomponent.utils.RequestUtil;
import ppex.proto.msg.entity.testpack.Files;
import ppex.proto.msg.entity.txt.Request;
import ppex.proto.msg.entity.txt.RequestHandle;
import ppex.proto.msg.entity.txt.Response;

public class FileHandler implements RequestHandle {

    private static String TAG = FileHandler.class.getName();

    private File sdFile = null;

    public FileHandler() {
        sdFile = Environment.getExternalStorageDirectory();
        sdFile = new File(sdFile,"/test");
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
        if ("root".equals(path)){
            File[] sdfiles = sdFile.listFiles();
            List<Files> responsefile = new ArrayList<>();
//            Arrays.stream(sdfiles).forEach(file -> {
//                Files files = new Files();
//                files.setDirectory(file.isDirectory());
//                files.setName(file.getName());
//                responsefile.add(files);
//            });
            for (int i =0;i < sdfiles.length;i ++){
                Files files = new Files();
                files.setDirectory(sdfiles[i].isDirectory());
                files.setName(sdfiles[i].getName());
                responsefile.add(files);
            }
            response.setBody(JSON.toJSONString(responsefile));
            return response;
        }
        return response;
    }
}
