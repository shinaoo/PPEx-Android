package ppex.client.androidcomponent;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.SocketUtils;
import ppex.client.R;
import ppex.client.androidcomponent.activity.ConnectedActivity;
import ppex.client.androidcomponent.adapter.ConnectionAdapter;
import ppex.client.androidcomponent.busevent.BusEvent;
import ppex.client.entity.Client;
import ppex.client.process.DetectProcess;
import ppex.client.process.ThroughProcess;
import ppex.client.socket.UdpClientHandler;
import ppex.proto.entity.through.Connection;
import ppex.utils.Constants;
import ppex.utils.Identity;

public class MainActivity extends AppCompatActivity {

    private static String TAG = MainActivity.class.getName();

    private Button btn_getnattype, btn_getallpeers;
    private TextView tv_shownattypeinfo, tv_showconectinfo, tv_showlocalpeername, tv_showlocalip, tv_showlocalmac;
    private ListView lv_showallpeers;

    private ConnectionAdapter connectionAdapter;
    private List<Connection> connections = new ArrayList<>();

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
        Client.getInstance().group.shutdownGracefully();
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
    }

    private void setEventListener() {
        btn_getallpeers.setOnClickListener(v -> {
            ThroughProcess.getInstance().setChannel(Client.getInstance().ch);
            ThroughProcess.getInstance().sendSaveInfo();
            ThroughProcess.getInstance().getConnectionsFromServer(Client.getInstance().ch);
        });
        btn_getnattype.setOnClickListener(v -> {
            DetectProcess.getInstance().setChannel(Client.getInstance().ch);
            DetectProcess.getInstance().startDetect();
            Client.getInstance().NAT_TYPE = DetectProcess.getInstance().getClientNATType().ordinal();
            Log.e(TAG, "Client get nattype is :" + Client.getInstance().NAT_TYPE);

            Connection connection = new Connection(Client.getInstance().MAC_ADDRESS, Client.getInstance().address,
                    Client.getInstance().peerName, Client.getInstance().NAT_TYPE);
            Client.getInstance().localConnection = connection;
//            EventBus.getDefault().post(new BusEvent(BusEvent.Type.DETECT_END_OF.getValue(),""));
            tv_shownattypeinfo.setText(Constants.getNatStrByValue(Client.getInstance().NAT_TYPE));
        });
        lv_showallpeers.setOnItemClickListener((parent, view, position, id) -> {
            ThroughProcess.getInstance().connectPeer(Client.getInstance().ch,connections.get(position));
            Client.getInstance().targetConnection =connections.get(position);
//            startActivity(new Intent(MainActivity.this, ConnectedActivity.class));
        });
    }

    private void initComponent() {
        connectionAdapter = new ConnectionAdapter(connections, this);
        lv_showallpeers.setAdapter(connectionAdapter);
    }

    private void initClient() {
        Identity.INDENTITY = Identity.Type.CLIENT.ordinal();
        getLocalInetSocketAddress();

        Client.getInstance().group = new NioEventLoopGroup(1);
        try {
            Client.getInstance().bootstrap = new Bootstrap();
            Client.getInstance().bootstrap.group(Client.getInstance().group).channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            channel.pipeline().addLast(new IdleStateHandler(0, 10, 0, TimeUnit.SECONDS));
                            channel.pipeline().addLast(new UdpClientHandler());
                        }
                    });
//                    .handler(new UdpClientHandler());
            Client.getInstance().ch = Client.getInstance().bootstrap.bind(Constants.PORT3).sync().channel();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

    private void getLocalInetSocketAddress() {
        Client.getInstance();
//            InetAddress address = InetAddress.getLocalHost();//获取的是本地的IP地址 //PC-20140317PXKX/192.168.0.121
//            String hostAddress = address.getHostAddress();//192.168.0.121
        Client.getInstance().local_address = getLocalIPAddress();
        Client.getInstance().SERVER1 = SocketUtils.socketAddress(Constants.SERVER_HOST1, Constants.PORT1);
        Client.getInstance().SERVER2P1 = SocketUtils.socketAddress(Constants.SERVER_HOST2, Constants.PORT1);
        Client.getInstance().SERVER2P2 = SocketUtils.socketAddress(Constants.SERVER_HOST2, Constants.PORT2);
        Client.getInstance().MAC_ADDRESS = getMacAddress();

        //
        tv_showlocalmac.setText(Client.getInstance().MAC_ADDRESS);
        tv_showlocalip.setText(Client.getInstance().local_address);
        tv_showlocalpeername.setText(Client.getInstance().peerName);
    }

    private String getLocalIPAddress() {
        try {
            String ip;
            ConnectivityManager connMan = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMan.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo wifinetworkinfo = connMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo.isConnected()) {
                ip = getLocalIPv4Address();
                return ip;
            } else if (wifinetworkinfo.isConnected()) {
                WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                int ipaddress = wifiInfo.getIpAddress();
                ip = int2Ip(ipaddress);
                return ip;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String int2Ip(int ipaddress) {
        return (ipaddress & 0xFF) + "." + ((ipaddress >> 8) & 0xFF) + "." + ((ipaddress >> 16) & 0xFF) + "." + ((ipaddress >> 24) & 0xFF);
    }

    private String getLocalIPv4Address() {
        try {
            String ip;
            ArrayList<NetworkInterface> nilist = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni : nilist) {
                ArrayList<InetAddress> iaList = Collections.list(ni.getInetAddresses());
                for (InetAddress addr : iaList) {
                    if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        ip = addr.getHostAddress();
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getMacAddress() {
        try {
            String macAddress = null;
            StringBuffer sb = new StringBuffer();
            NetworkInterface networkInterface = null;
            networkInterface = NetworkInterface.getByName("eth1");
            if (networkInterface == null)
                networkInterface = NetworkInterface.getByName("wlan0");
            if (networkInterface == null)
                return "02:00:00:00:00:02";
            byte[] addr = networkInterface.getHardwareAddress();
            for (byte b : addr) {
                sb.append(String.format("%02X:", b));
            }
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }
            macAddress = sb.toString();
            return macAddress;
        } catch (SocketException e) {
            return "";
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void handleMainThreadEvent(BusEvent event) {
        switch (BusEvent.Type.getByValue(event.getType())) {
            case DETECT_END_OF:
                break;
            case THROUGH_GET_INFO:
                List<Connection> conns = (List<Connection>) event.getData();
                connections = conns;
                connectionAdapter.setConnectios(connections);
                connectionAdapter.notifyDataSetChanged();
                break;
            case THROUGN_CONNECT_END:
                Log.e(TAG,"穿越结束,已经连接数：" + Client.getInstance().connectedMaps.size());
                startActivity(new Intent(MainActivity.this, ConnectedActivity.class));
                break;
        }
    }
}
