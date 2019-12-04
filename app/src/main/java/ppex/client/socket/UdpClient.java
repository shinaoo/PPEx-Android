package ppex.client.socket;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.IdleStateHandler;
import ppex.client.entity.Client;
import ppex.proto.msg.entity.Connection;
import ppex.proto.msg.type.TxtTypeMsg;
import ppex.proto.rudp.Output;
import ppex.proto.rudp.RudpPack;
import ppex.proto.rudp.RudpScheduleTask;
import ppex.utils.Constants;
import ppex.utils.Identity;
import ppex.utils.MessageUtil;
import ppex.utils.tpool.DisruptorExectorPool;
import ppex.utils.tpool.IMessageExecutor;

public class UdpClient {


    private DisruptorExectorPool disruptorExectorPool;
    private UdpClientHandler udpClientHandler;
    private List<Channel> channels = new ArrayList<>();

    public void startClient() {
        Identity.INDENTITY = Identity.Type.CLIENT.ordinal();
        getLocalInetSocketAddress();
        try {
            Client.getInstance().bootstrap = new Bootstrap();
            int cpunum = Runtime.getRuntime().availableProcessors();
            disruptorExectorPool = new DisruptorExectorPool();
            IntStream.range(0, 2).forEach(val -> disruptorExectorPool.createDisruptorProcessor("disruptro:" + val));
            Client.getInstance().group =   new NioEventLoopGroup(1);
            Class<? extends Channel> channelCls = NioDatagramChannel.class;

            Client.getInstance().bootstrap.channel(channelCls);
            Client.getInstance().bootstrap.group(Client.getInstance().group);
//            Client.getInstance().bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
            Client.getInstance().bootstrap.option(ChannelOption.SO_BROADCAST, true).option(ChannelOption.SO_REUSEADDR, true);
//                    .option(ChannelOption.RCVBUF_ALLOCATOR,new AdaptiveRecvByteBufAllocator(Rudp.HEAD_LEN,Rudp.MTU_DEFUALT,Rudp.MTU_DEFUALT));

            udpClientHandler = new UdpClientHandler( disruptorExectorPool, Client.getInstance().getAddrManager());
            Client.getInstance().bootstrap.handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) throws Exception {
                    channel.pipeline().addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                    channel.pipeline().addLast(udpClientHandler);
                }
            });
            Channel ch = Client.getInstance().bootstrap.bind(Constants.PORT3).sync().channel();
            channels.add(ch);
            Client.getInstance().setChannels(channels);

            RudpPack rudpPack = Client.getInstance().getAddrManager().get(Client.getInstance().SERVER1);
            IMessageExecutor executor = disruptorExectorPool.getAutoDisruptorProcessor();
            if (rudpPack == null) {
                Connection connection = new Connection("", Client.getInstance().SERVER1, "server1", Constants.NATTYPE.PUBLIC_NETWORK.ordinal(), Client.getInstance().getChannels().get(0));
                Output output = new ClientOutput();
                rudpPack = new RudpPack(output, connection, executor, null, null);
                Client.getInstance().getAddrManager().New(Client.getInstance().SERVER1, rudpPack);
            }

            TxtTypeMsg txtTypeMsg = new TxtTypeMsg();
            txtTypeMsg.setTo(new InetSocketAddress("127.0.0.1",9123));
            txtTypeMsg.setFrom(new InetSocketAddress("127.0.0.1",9124));
            txtTypeMsg.setReq(true);
            txtTypeMsg.setContent("this is from client");
            rudpPack.write(MessageUtil.txtmsg2Msg(txtTypeMsg));

            RudpScheduleTask scheduleTask = new RudpScheduleTask(executor, rudpPack, Client.getInstance().getAddrManager());
            DisruptorExectorPool.scheduleHashedWheel(scheduleTask, rudpPack.getInterval());

            IMessageExecutor executor1 = disruptorExectorPool.getAutoDisruptorProcessor();
            RudpPack rudpPack1 = Client.getInstance().getAddrManager().get(Client.getInstance().SERVER2P1);
            if (rudpPack1 == null){
                Connection connection = new Connection("",Client.getInstance().SERVER2P1,"server2p1",Constants.NATTYPE.PUBLIC_NETWORK.ordinal(),ch);
                Output output = new ClientOutput();
                rudpPack1 = new RudpPack(output,connection,executor1,udpClientHandler,null);
                Client.getInstance().getAddrManager().New(Client.getInstance().SERVER2P1,rudpPack1);
            }
            rudpPack1.sendReset();
            RudpScheduleTask scheduleTasks2 = new RudpScheduleTask(executor1, rudpPack, Client.getInstance().getAddrManager());
            DisruptorExectorPool.scheduleHashedWheel(scheduleTasks2, rudpPack.getInterval());

//            RudpPack finalpack = rudpPack;
//            IntStream.range(0, 100).forEach(val -> {
//                Message msg = MessageUtil.makeTestStr2Msg("this msg from client" + val + " 23123131fh1df1h1f3h13df1g3h1fd31gh32df1gh31df3gh13fd1gh31df31gh31df3g1h3df13gh1d3f1gh3f1gh321df2gh13d2f1gh321dfgh321df3h3df21gh32df1g3h21dfhd3f1gh321df32h13tr5h464gas6d54g65sd4g64sd6fg4s6d4b6ds4b6ds4fb6dsf16sd1fb6sd1fb61sdf6b1dfs6b1d6fs4bd6fb4d6sb4dsf6b4df6b4d6sfb4d6sf4b6dsf4b6ds4b64er64be6erwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwweeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee321321321re31t3we1rt231s3df1g3sd1fg123eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeend");
//                finalpack.write(msg);
//                if (val % 32 == 0) {
//                    try {
//                        TimeUnit.SECONDS.sleep(1);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });


            //1.探测阶段.开始DetectProcess
//            DetectProcess.getInstance().setChannel(ch);
//            DetectProcess.getInstance().startDetect();
//
//            Client.getInstance().NAT_TYPE = DetectProcess.getInstance().getClientNATType().ordinal();
//            System.out.println("Client NAT type is :" + Client.getInstance().NAT_TYPE);
//
//            Connection connection = new Connection(Client.getInstance().MAC_ADDRESS,Client.getInstance().address,
//                    Client.getInstance().peerName,Client.getInstance().NAT_TYPE,ch);
//            Client.getInstance().localConnection = connection;
//
//            //2.穿越阶段
//            ThroughProcess.getInstance().setChannel(ch);
//            ThroughProcess.getInstance().sendSaveInfo();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

    private void getLocalInetSocketAddress() {
        Client.getInstance();
        try {
            InetAddress address = InetAddress.getLocalHost();//获取的是本地的IP地址 //PC-20140317PXKX/192.168.0.121
            String hostAddress = address.getHostAddress();//192.168.0.121
            Client.getInstance().local_address = address.getHostAddress();
            Client.getInstance().SERVER1 = new InetSocketAddress(Constants.SERVER_HOST1, Constants.PORT1);
            Client.getInstance().SERVER2P1 = new InetSocketAddress(Constants.SERVER_HOST2, Constants.PORT1);
            Client.getInstance().SERVER2P2 = new InetSocketAddress(Constants.SERVER_HOST2, Constants.PORT2);
            Client.getInstance().MAC_ADDRESS = getMacAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
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
            return "";
        }
        return "";
    }

    public void stop() {
        Client.getInstance().getChannels().forEach(channel -> {
            if (channel != null && (channel.isOpen() || channel.isActive())){
                channel.close();
            }
        });
        if (disruptorExectorPool != null) {
            disruptorExectorPool.stop();
        }
        if (Client.getInstance().group != null) {
            Client.getInstance().group.shutdownGracefully();
        }
    }

}
