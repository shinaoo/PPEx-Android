package ppex.androidcomponent.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ppex.client.R;
import ppex.androidcomponent.adapter.FilesAdapter;
import ppex.androidcomponent.busevent.BusEvent;
import ppex.androidcomponent.entity.Files;
import ppex.androidcomponent.handler.client.RequestClient;
import ppex.client.entity.Client;
import ppex.proto.msg.entity.through.Connect;
import ppex.proto.msg.type.TxtTypeMsg;
import ppex.proto.rudp.Rudp;
import ppex.proto.rudp.RudpPack;
import ppex.utils.MessageUtil;

public class ConnectedActivity extends Activity {

    private static String TAG = ConnectedActivity.class.getName();

    private ListView lv_showfiles;
    private Button btn_getfiles;
    private TextView tv_back,tv_title;
    private Button btn_send;
    private EditText et_content;

    private int connectType;

    private FilesAdapter filesAdapter;
    private List<Files> files;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_connected);
        EventBus.getDefault().register(this);
        getIntentValue();
        findIds();
        setListener();
        initComponent();
        setRequestHandler();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void getIntentValue(){
        connectType = Client.getInstance().connectedMaps.get(0).getConnectType();
        Log.e(TAG,"connectype is :" + connectType);
        RequestClient.getDefault();
    }

    private void findIds(){
        lv_showfiles = findViewById(R.id.lv_connected_showfiles);
        btn_getfiles = findViewById(R.id.btn_connected_getfiles);
        tv_back = findViewById(R.id.tv_connected_back);
        tv_title = findViewById(R.id.tv_connected_title);

        tv_title.setText(Client.getInstance().targetConnection.getMacAddress());
    }

    private void setListener(){
        tv_back.setOnClickListener(v ->{
            this.finish();
        });
        btn_getfiles.setOnClickListener(v ->{
            //请求获取目录
            Map<String,String> params = new HashMap<>();
            params.put("path","root");
            RequestClient.getDefault().sendRequest("/file/getfiles",null,params);
        });
        btn_send.setOnClickListener(v ->{
            String contetn = et_content.getText().toString();
            TxtTypeMsg txtTypeMsg = new TxtTypeMsg();
            txtTypeMsg.setFrom(Client.getInstance().localConnection.getAddress());
            txtTypeMsg.setTo(Client.getInstance().connectedMaps.get(0).getConnections().get(1).getAddress());
            txtTypeMsg.setReq(true);
            txtTypeMsg.setContent(contetn);
            if(connectType == Connect.TYPE.FORWARD.ordinal()){
                RudpPack rudpPack = Client.getInstance().addrManager.get(Client.getInstance().SERVER1);
                rudpPack.write(MessageUtil.txtmsg2Msg(txtTypeMsg));
            }else{
                RudpPack rudpPack = Client.getInstance().addrManager.get(Client.getInstance().connectedMaps.get(0).getConnections().get(1).getAddress());
                rudpPack.write(MessageUtil.txtmsg2Msg(txtTypeMsg));
            }
        });
    }

    private void initComponent(){
        files = new ArrayList<>();
        filesAdapter = new FilesAdapter(files,this);
        lv_showfiles.setAdapter(filesAdapter);
    }

    private void setRequestHandler(){
        RequestClient.getDefault().setChannel(Client.getInstance().ch);
        RequestClient.getDefault().setTargetConnection(Client.getInstance().connectedMaps.get(0).getConnections().get(1));
        RequestClient.getDefault().setConnectType(connectType);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void handleMainThreadEvent(BusEvent event) {
        switch (BusEvent.Type.getByValue(event.getType())) {
            case FILE_GETFILES:
                Log.e(TAG,TAG+"get files response:" + event.getData());
                String fileStr = (String) event.getData();
                files= JSON.parseArray(fileStr, Files.class);
                filesAdapter.setFiles(files);
                filesAdapter.notifyDataSetChanged();
                break;
        }
    }


}
