package ppex.androidcomponent;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import ppex.androidcomponent.activity.ConnectedActivity;
import ppex.androidcomponent.adapter.ConnectionAdapter;
import ppex.androidcomponent.busevent.BusEvent;
import ppex.client.Client;
import ppex.client.R;
import ppex.client.process.DetectProcess;
import ppex.client.process.FileProcess;
import ppex.client.process.ThroughProcess;
import ppex.proto.entity.Connection;
import ppex.utils.NatTypeUtil;

public class MainActivity extends AppCompatActivity {

    private static String TAG = MainActivity.class.getName();

    private Button btn_getnattype, btn_getallpeers, btn_test, btn_sendinfo;
    private TextView tv_shownattypeinfo, tv_showconectinfo, tv_showlocalpeername, tv_showlocalip, tv_showlocalmac, tv_showrcvcontent;
    private ListView lv_showallpeers;

    private ConnectionAdapter connectionAdapter;
    private List<Connection> connections = new ArrayList<>();

    private Client client;
    private Object detectResult = new Object();

    private FileProcess fileProcess;
    private volatile boolean isEnter = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        findIds();
        setEventListener();
        initComponent();
        initClient();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        client.stop();
        EventBus.getDefault().unregister(this);
    }

    private void findIds() {
        btn_getnattype = findViewById(R.id.btn_main_getnattype);
        btn_getallpeers = findViewById(R.id.btn_main_getallpeers);
        tv_shownattypeinfo = findViewById(R.id.tv_main_shownattypeinfo);
        tv_showconectinfo = findViewById(R.id.tv_main_showconnectinfo);
        tv_showlocalpeername = findViewById(R.id.tv_main_showlocalpeername);
        tv_showlocalip = findViewById(R.id.tv_main_showlocalip);
        tv_showlocalmac = findViewById(R.id.tv_main_showlocalmac);
        lv_showallpeers = findViewById(R.id.lv_main_showallpeers);
        tv_showrcvcontent = findViewById(R.id.tv_main_showrcvcontent);
        btn_test = findViewById(R.id.btn_main_test);
        btn_sendinfo = findViewById(R.id.btn_main_sendinfo);

    }

    private void setEventListener() {
        btn_getnattype.setOnClickListener(v -> {
            new Thread(() -> {
                DetectProcess.getInstance().startDetect();
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        });
        btn_sendinfo.setOnClickListener(v -> ThroughProcess.getInstance().sendSaveInfo());
        btn_getallpeers.setOnClickListener(v -> ThroughProcess.getInstance().getConnectionsFromServer());
        lv_showallpeers.setOnItemClickListener((parent, view, position, id) -> ThroughProcess.getInstance().connectPeer(connections.get(position), client.getAddrManager()));
        btn_test.setOnClickListener(view -> {
        });
    }

    private void initComponent() {
        connectionAdapter = new ConnectionAdapter(connections, this);
        lv_showallpeers.setAdapter(connectionAdapter);
    }

    private void initClient() {
        client = Client.getInstance();
        try {
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            DetectProcess.getInstance().setClient(client);
            ThroughProcess.getInstance().setClient(client);
            EventBus.getDefault().post(new BusEvent(BusEvent.Type.CLIENT_INIT_END.getValue()));
        }).start();

        fileProcess = FileProcess.getInstance();
        fileProcess.setRoot(Environment.getExternalStorageDirectory());

    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void handleMainThreadEvent(BusEvent event) {
        switch (BusEvent.Type.getByValue(event.getType())) {
            case THROUGN_GET_CONN_INFO:
                List<Connection> conns = (List<Connection>) event.getData();
                connections = conns;
                connectionAdapter.setConnectios(connections);
                connectionAdapter.notifyDataSetChanged();
                break;
            case THROUGH_END:
//                Log.e(TAG, "穿越结束,已经连接数：" + Client.getInstance().connectedMaps.size());
                startActivity(new Intent(MainActivity.this, ConnectedActivity.class));
                break;
            case TXT_RESPONSE:
                tv_showrcvcontent.setText((String) event.getData());
                break;
            case CLIENT_INIT_END:
                tv_showlocalmac.setText(Client.getInstance().getConnLocal().getMacAddress());
                tv_showlocalip.setText(Client.getInstance().getAddrLocal().toString());
                tv_showlocalpeername.setText(Client.getInstance().getConnLocal().getPeerName());
                break;
            case DETECT_ONE_FROM_SERVER1:
                if (DetectProcess.getInstance().getNAT_ADDRESS_FROM_S2P1() != null) {
                    client.getConnLocal().setAddress(DetectProcess.getInstance().getNAT_ADDRESS_FROM_S1());
                    if (DetectProcess.getInstance().getNAT_ADDRESS_FROM_S1().equals(DetectProcess.getInstance().getNAT_ADDRESS_FROM_S2P1())) {
                        DetectProcess.getInstance().setNAT_ADDRESS_SAME(true);
                    } else {
                        DetectProcess.getInstance().setNAT_ADDRESS_SAME(false);
                    }
                }
                break;
            case DETECT_TWO_FROM_SERVER2P1:
                if (DetectProcess.getInstance().getNAT_ADDRESS_FROM_S1() != null) {
                    if (DetectProcess.getInstance().getNAT_ADDRESS_FROM_S1().equals(DetectProcess.getInstance().getNAT_ADDRESS_FROM_S2P1())) {
                        DetectProcess.getInstance().setNAT_ADDRESS_SAME(true);
                    } else {
                        DetectProcess.getInstance().setNAT_ADDRESS_SAME(false);
                    }
                }
                break;
            case DETECT_ONE_FROM_SERVER2P2:
            case DETECT_TWO_FROM_SERVER2P2:
                synchronized (detectResult) {
                    int nattype = DetectProcess.getInstance().getClientNATType().getValue();
                    client.getConnLocal().setNatType(nattype);
                    tv_shownattypeinfo.setText(NatTypeUtil.getNatStrByValue(DetectProcess.getInstance().getClientNATType().getValue()));
                }
                break;
            case THROUGN_RCV_CONNECT_PONG:
                if (isEnter) {
                    isEnter = false;
                } else {
                    isEnter = true;
                    startActivity(new Intent(MainActivity.this, ConnectedActivity.class));
                }
                break;
        }
    }
}
