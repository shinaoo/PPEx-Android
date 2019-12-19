package ppex.androidcomponent;

import android.content.Intent;
import android.os.Bundle;
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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    DetectProcess.getInstance().startDetect();
                    try {
                        TimeUnit.SECONDS.sleep(3);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    EventBus.getDefault().post(new BusEvent(BusEvent.Type.DETECT_END_OF.getValue()));
                }
            }).start();
//            Client.getInstance().NAT_TYPE = DetectProcess.getInstance().getClientNATType().ordinal();
//            Log.e(TAG, "Client get nattype is :" + Client.getInstance().NAT_TYPE);
//
//            Connection connection = new Connection(Client.getInstance().MAC_ADDRESS, Client.getInstance().SERVER1, "Server1", Client.getInstance().NAT_TYPE, Client.getInstance().getChannels().get(0));
//            Client.getInstance().localConnection = connection;
//            tv_shownattypeinfo.setText(Constants.getNatStrByValue(Client.getInstance().NAT_TYPE));
        });
        btn_sendinfo.setOnClickListener(v -> {
            ThroughProcess.getInstance().sendSaveInfo();
        });
        btn_getallpeers.setOnClickListener(v -> {
            ThroughProcess.getInstance().getConnectionsFromServer();
        });

        lv_showallpeers.setOnItemClickListener((parent, view, position, id) -> {
            ThroughProcess.getInstance().connectPeer(connections.get(position), Client.getInstance().getAddrManager());
//            Client.getInstance().targetConnection = connections.get(position);
//            startActivity(new Intent(MainActivity.this, ConnectedActivity.class));
        });
        btn_test.setOnClickListener(view -> {
//            TxtTypeMsg txtTypeMsg = new TxtTypeMsg();
//            txtTypeMsg.setReq(true);
//            txtTypeMsg.setFrom(Client.getInstance().localConnection.getAddress());
//            txtTypeMsg.setTo(Client.getInstance().SERVER1);
//            txtTypeMsg.setContent("");
//            Log.e(TAG, "txtTYpemsg:" + txtTypeMsg.getContent());
////            channel.writeAndFlush(MessageUtil.txtMsg2packet(txtTypeMsg, Client.getInstance().SERVER1));
//            RudpPack rudpPack = Client.getInstance().getAddrManager().get(Client.getInstance().SERVER1);
//            rudpPack.write(MessageUtil.txtmsg2Msg(txtTypeMsg));
        });
    }

    private void initComponent() {
        connectionAdapter = new ConnectionAdapter(connections, this);
        lv_showallpeers.setAdapter(connectionAdapter);
    }

    private void initClient() {
        client = Client.getInstance();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    client.start();
                    DetectProcess.getInstance();
                    ThroughProcess.getInstance();
                    EventBus.getDefault().post(new BusEvent(BusEvent.Type.CLIENT_INIT_END.getValue()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void handleMainThreadEvent(BusEvent event) {
        switch (BusEvent.Type.getByValue(event.getType())) {
            case DETECT_END_OF:
                tv_shownattypeinfo.setText(NatTypeUtil.getNatStrByValue(DetectProcess.getInstance().getClientNATType().getValue()));
                break;
            case THROUGH_GET_INFO:
                List<Connection> conns = (List<Connection>) event.getData();
                connections = conns;
                connectionAdapter.setConnectios(connections);
                connectionAdapter.notifyDataSetChanged();
                break;
            case THROUGN_CONNECT_END:
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
        }
    }
}
