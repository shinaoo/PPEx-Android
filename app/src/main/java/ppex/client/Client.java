package ppex.client;

import android.util.Log;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

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

public class Client {

    private String HOST_SERVER1 = "119.139.198.211";
    private String HOST_SERVER2 = "61.141.64.171";
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
    public static Client getInstance(){
        if (instance == null){
            instance = new Client();
        }
        return instance;
    }
    private Client(){}

    public void start() throws Exception {
        initParam();
        startBootstrap();
        initRudp();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));
        System.out.println("addrLocal:" + addrLocal.toString());
    }

    private void initParam() {

        name = "Client1";
        addrMac = getMacAddress();
        addrLocal = getLocalIpAddr();

        addrServer1 = new InetSocketAddress(HOST_SERVER1, PORT_1);
        addrServer2p1 = new InetSocketAddress(HOST_SERVER2, PORT_1);
        addrServer2p2 = new InetSocketAddress(HOST_SERVER2, PORT_2);

        addrManager = new ClientAddrManager();
        executor = new ThreadExecute();
        outputManager = new ClientOutputManager();
        executor.start();
        clientHandler = new ClientHandler(this);
        responseListener = new MsgResponse(addrManager);

        connLocal = new Connection(name, addrLocal, name, NatTypeUtil.NatType.UNKNOWN.getValue());
    }

    private void startBootstrap() throws Exception {

        int cpunum = Runtime.getRuntime().availableProcessors();
        bootstrap = new Bootstrap();
        boolean epoll = Epoll.isAvailable();
        if (epoll) {
            bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
        }
        eventLoopGroup = epoll ? new EpollEventLoopGroup(cpunum) : new NioEventLoopGroup(cpunum);
        Class<? extends Channel> chnCls = epoll ? EpollDatagramChannel.class : NioDatagramChannel.class;
        bootstrap.channel(chnCls).group(eventLoopGroup);
        bootstrap.option(ChannelOption.SO_BROADCAST,true).option(ChannelOption.SO_REUSEADDR,true)
                .option(ChannelOption.RCVBUF_ALLOCATOR,new AdaptiveRecvByteBufAllocator(Rudp.HEAD_LEN,Rudp.MTU_DEFUALT,Rudp.MTU_DEFUALT));

        bootstrap.handler(clientHandler);
        channel = bootstrap.bind(PORT_3).sync().channel();
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

        connServer2p1 = new Connection("Server2P1",addrServer2p1,"Server2P1", NatTypeUtil.NatType.UNKNOWN.getValue());
        IOutput outputServer2P1 = new ClientOutput(channel,connServer2p1);
        outputManager.put(addrServer2p1,outputServer2P1);
        RudpPack rudpPack2 = addrManager.get(addrServer2p1);
        if (rudpPack2 == null){
            rudpPack2 = new RudpPack(outputServer2P1,executor,responseListener);
            addrManager.New(addrServer2p1,rudpPack2);
//            rudpPack2.sendReset();
        }

        RudpScheduleTask task2 = new RudpScheduleTask(executor,rudpPack2,addrManager);
        executor.executeTimerTask(task2,rudpPack2.getInterval());

    }

    private void stop() {
        Log.e("MyTag","hook thread run stop");
        addrManager.getAll().forEach(rudpPack -> rudpPack.sendFinish());
        if (executor != null) {
            executor.stop();
        }
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
    }

    private InetSocketAddress getLocalIpAddr(){
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return new InetSocketAddress(addr,PORT_3);
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
