package ppex.client;

import android.content.Context;
import android.net.InetAddresses;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import io.netty.handler.timeout.IdleStateHandler;
import ppex.client.rudp.ClientAddrManager;
import ppex.client.rudp.ClientOutput;
import ppex.client.rudp.ClientOutputManager;
import ppex.client.rudp.MsgResponse;
import ppex.proto.entity.Connection;
import ppex.proto.entity.through.Connect;
import ppex.proto.msg.type.TxtTypeMsg;
import ppex.proto.rudp.*;
import ppex.proto.tpool.IThreadExecute;
import ppex.proto.tpool.ThreadExecute;
import ppex.utils.MessageUtil;
import ppex.utils.NatTypeUtil;

import java.net.*;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

public class Client {

    //    private String HOST_SERVER1 = "119.139.198.211";
//    private String HOST_SERVER2 = "61.141.64.171";
    private String HOST_SERVER1 = "192.168.1.100";
    private String HOST_SERVER2 = "192.168.1.102";
    private int PORT_1 = 9123;
    private int PORT_2 = 9124;
    private int PORT_3 = 9125;

    private InetSocketAddress addrLocal;
    private InetSocketAddress addrServer1;
    private InetSocketAddress addrServer2p1;
    private InetSocketAddress addrServer2p2;


    private String addrMac;
    private String name;
    private Connection connServer1;
    private Connection connServer2p1;
    private Connection connServer2p2;
    private Connection connLocal;
    //连接的Connection,另一个Client
    private Connection connTarget;
    private Connect.TYPE connType2Target;

    private IAddrManager addrManager;
    private IOutputManager outputManager;
    private IThreadExecute executor;
    private ResponseListener responseListener;

    private Channel channel;
    private Bootstrap bootstrap;
    private EventLoopGroup eventLoopGroup;
    private ClientHandler clientHandler;


    private static Client instance = null;

    public static Client getInstance() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    private Client() {
    }

    public void start() throws Exception {
        initParam();
        startBootstrap();
        initRudp();
        //android 中这个shutdownhook不执行
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));
    }

    private void initParam() {

        name = "Client1";
//        addrMac = getMacAddress();
//        addrLocal = getLocalIpAddr();
        addrMac = getLocalMacAddressFromIp();
        addrLocal = new InetSocketAddress(getLocalHostIp(), PORT_3);

        addrServer1 = new InetSocketAddress(HOST_SERVER1, PORT_1);
        addrServer2p1 = new InetSocketAddress(HOST_SERVER2, PORT_1);
        addrServer2p2 = new InetSocketAddress(HOST_SERVER2, PORT_2);

        addrManager = new ClientAddrManager();
        executor = new ThreadExecute();
        outputManager = new ClientOutputManager();
        executor.start();
        clientHandler = new ClientHandler(this);
        responseListener = new MsgResponse(addrManager);

        connLocal = new Connection(addrMac, addrLocal, name, NatTypeUtil.NatType.UNKNOWN.getValue());
    }

    private void startBootstrap() {

        int cpunum = Runtime.getRuntime().availableProcessors();
        bootstrap = new Bootstrap();
        boolean epoll = Epoll.isAvailable();
        if (epoll) {
            bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
        }
        eventLoopGroup = epoll ? new EpollEventLoopGroup(cpunum) : new NioEventLoopGroup(cpunum);
        Class<? extends Channel> chnCls = epoll ? EpollDatagramChannel.class : NioDatagramChannel.class;
        bootstrap.channel(chnCls).group(eventLoopGroup);
        bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(Rudp.HEAD_LEN, Rudp.MTU_DEFUALT, Rudp.MTU_DEFUALT));
        bootstrap.handler(new ChannelInitializer<NioDatagramChannel>() {
            @Override
            protected void initChannel(NioDatagramChannel ch) throws Exception {
                //如果不加心跳保持连接,当Server端关闭后,Channel就inactive.后面应该要添加Channel如果inactive后重启的机制.保持Channel在线
                ch.pipeline().addLast(new IdleStateHandler(0,5,0, TimeUnit.SECONDS));
                ch.pipeline().addLast(clientHandler);
            }
        });
        try {
            channel = bootstrap.bind(PORT_3).sync().channel();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void initRudp() {
        //默认的server1,server2p1,server2p2的Connection
        connServer1 = new Connection("unknown", addrServer1, "Server1", NatTypeUtil.NatType.UNKNOWN.getValue());
        //output需要Channel
        IOutput outputServer1 = new ClientOutput(channel, connServer1);
        outputManager.put(addrServer1, outputServer1);
        RudpPack rudpPack = addrManager.get(addrServer1);
        if (rudpPack == null) {
            rudpPack = new RudpPack(outputServer1, executor, responseListener);
            addrManager.New(addrServer1, rudpPack);
//            rudpPack.sendReset();
        }

        RudpScheduleTask task = new RudpScheduleTask(executor, rudpPack, addrManager);
        executor.executeTimerTask(task, rudpPack.getInterval());

        connServer2p1 = new Connection("Server2P1", addrServer2p1, "Server2P1", NatTypeUtil.NatType.UNKNOWN.getValue());
        IOutput outputServer2P1 = new ClientOutput(channel, connServer2p1);
        outputManager.put(addrServer2p1, outputServer2P1);
        RudpPack rudpPack2 = addrManager.get(addrServer2p1);
        if (rudpPack2 == null) {
            rudpPack2 = new RudpPack(outputServer2P1, executor, responseListener);
            addrManager.New(addrServer2p1, rudpPack2);
//            rudpPack2.sendReset();
        }

        RudpScheduleTask task2 = new RudpScheduleTask(executor, rudpPack2, addrManager);
        executor.executeTimerTask(task2, rudpPack2.getInterval());

    }

    public void stop() {
        addrManager.getAll().forEach(rudpPack -> rudpPack.sendFinish());
        if (channel != null){
            channel.close();
        }
        if (executor != null) {
            executor.stop();
        }
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
    }

    private InetSocketAddress getLocalIpAddr() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return new InetSocketAddress(addr, PORT_3);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getMacAddress() {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            byte[] mac = null;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                if (netInterface.isLoopback() || netInterface.isVirtual() || netInterface.isPointToPoint() || !netInterface.isUp()) {
                    continue;
                } else {
                    mac = netInterface.getHardwareAddress();
                    if (mac != null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < mac.length; i++) {
                            sb.append(String.format("%02X", mac[i]));
                        }
                        if (sb.length() > 0) {
                            return sb.toString();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            return "";
        }
        return "";
    }

    //android获取本地IP和mac
    public String getLocalMacAddressFromIp() {
        String mac_s = "";
        try {
            byte[] mac;
            NetworkInterface ne = NetworkInterface.getByInetAddress(InetAddress.getByName(getLocalHostIp()));
            mac = ne.getHardwareAddress();
            mac_s = byte2hex(mac);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mac_s;
    }

    public String byte2hex(byte[] b) {
        StringBuffer hs = new StringBuffer(b.length);
        String stmp = "";
        int len = b.length;
        for (int n = 0; n < len; n++) {
            stmp = Integer.toHexString(b[n] & 0xFF);
            if (stmp.length() == 1)
                hs = hs.append("0").append(stmp);
            else {
                hs = hs.append(stmp);
            }
        }
        return String.valueOf(hs);
    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("WifiPreference IpAddress", ex.toString());
        }

        return null;
    }

    public String getLocalHostIp() {
        String ipaddress = "";
        try {
            Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces();
            // 遍历所用的网络接口
            while (en.hasMoreElements()) {
                NetworkInterface nif = en.nextElement();// 得到每一个网络接口绑定的所有ip
                Enumeration<InetAddress> inet = nif.getInetAddresses();
                // 遍历每一个接口绑定的所有ip
                while (inet.hasMoreElements()) {
                    InetAddress ip = inet.nextElement();
                    // 在这里如果不加isIPv4Address的判断,直接返回,在4.0上获取到的是类似于fe80::1826:66ff:fe23:48e%p2p0的ipv6的地址
                    if (!ip.isLoopbackAddress() && ip instanceof Inet4Address) {
                        return ipaddress = ip.getHostAddress();
                    }
                }

            }
        } catch (SocketException e) {
            Log.e("feige", "获取本地ip地址失败");
            e.printStackTrace();
        }
        return ipaddress;

    }

    public void sendTest() {
        TxtTypeMsg msg = new TxtTypeMsg();
        msg.setContent("this is from client");
        msg.setFrom(new InetSocketAddress("127.0.0.1", PORT_3));
        msg.setTo(new InetSocketAddress("127.0.0.1", PORT_1));
        msg.setReq(true);
        this.getAddrManager().get(addrServer1).write(MessageUtil.txtmsg2Msg(msg));
    }

    public IAddrManager getAddrManager() {
        return addrManager;
    }

    public IOutputManager getOutputManager() {
        return outputManager;
    }

    public IThreadExecute getExecutor() {
        return executor;
    }

    public ResponseListener getResponseListener() {
        return responseListener;
    }

    public InetSocketAddress getAddrServer1() {
        return addrServer1;
    }

    public InetSocketAddress getAddrServer2p1() {
        return addrServer2p1;
    }

    public InetSocketAddress getAddrServer2p2() {
        return addrServer2p2;
    }

    public InetSocketAddress getAddrLocal() {
        return addrLocal;
    }

    public void setAddrLocal(InetSocketAddress addrLocal) {
        this.addrLocal = addrLocal;
    }

    public Connection getConnLocal() {
        return connLocal;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public int getPORT_1() {
        return PORT_1;
    }

    public int getPORT_2() {
        return PORT_2;
    }

    public int getPORT_3() {
        return PORT_3;
    }

    public String getAddrMac() {
        return addrMac;
    }

    public Connection getConnTarget() {
        return connTarget;
    }

    public void setConnTarget(Connection connTarget) {
        this.connTarget = connTarget;
    }

    public Connect.TYPE getConnType2Target() {
        return connType2Target;
    }

    public void setConnType2Target(Connect.TYPE connType2Target) {
        this.connType2Target = connType2Target;
    }
}
