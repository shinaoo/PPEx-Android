package ppex.client.androidcomponent.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.Arrays;

import ppex.client.R;
import ppex.client.entity.Client;

public class ConnectedActivity extends Activity {

    private static String TAG = ConnectedActivity.class.getName();

    private ListView lv_showfiles;
    private Button btn_getfiles;
    private TextView tv_back,tv_title;

    private int connectType;

    private File targetFile;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_connected);
        getIntentValue();
        findIds();
        setListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void getIntentValue(){
        connectType = Client.getInstance().connectedMaps.get(0).getConnectType();
        File sdFile = Environment.getExternalStorageDirectory();
        File file = Environment.getExternalStorageDirectory();
        File[] files = file.listFiles();
        targetFile= Arrays.stream(files).filter(f -> f.getName().equals("test.pdf")).findFirst().get();
    }

    private void findIds(){
        lv_showfiles = findViewById(R.id.lv_connected_showfiles);
        btn_getfiles = findViewById(R.id.btn_connected_getfile);
        tv_back = findViewById(R.id.tv_connected_back);
        tv_title = findViewById(R.id.tv_connected_title);

        tv_title.setText(Client.getInstance().targetConnection.getMacAddress());
    }

    private void setListener(){
        tv_back.setOnClickListener(v ->{
            this.finish();
        });
        btn_getfiles.setOnClickListener(v ->{
            //目前暂定获取test.pdf

        });

    }


}
