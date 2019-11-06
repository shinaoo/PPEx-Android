package ppex.client.socket;

import android.util.Log;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.SocketUtils;
import ppex.client.entity.Client;
import ppex.client.process.DetectProcess;
import ppex.client.process.ThroughProcess;
import ppex.proto.msg.entity.Connection;
import ppex.utils.Constants;
import ppex.utils.Identity;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

public class UdpClient {

    private static String TAG = UdpClient.class.getName();

    public void startClient() {

        Identity.INDENTITY = Identity.Type.CLIENT.ordinal();
        getLocalInetSocketAddress();

        EventLoopGroup group = new NioEventLoopGroup(1);
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            channel.pipeline().addLast(new IdleStateHandler(0, 10, 0, TimeUnit.SECONDS));
                            channel.pipeline().addLast(new UdpClientHandler());
                        }
                    });
//                    .handler(new UdpClientHandler());
            Channel ch = bootstrap.bind(Constants.PORT3).sync().channel();

            //1.探测阶段.开始DetectProcess
            DetectProcess.getInstance().setChannel(ch);
            DetectProcess.getInstance().startDetect();

            Client.getInstance().NAT_TYPE = DetectProcess.getInstance().getClientNATType().ordinal();
            System.out.println("Client NAT type is :" + Client.getInstance().NAT_TYPE);

            Connection connection = new Connection(Client.getInstance().MAC_ADDRESS,Client.getInstance().address,
                    Client.getInstance().peerName,Client.getInstance().NAT_TYPE);
            Client.getInstance().localConnection = connection;

            //2.穿越阶段
            ThroughProcess.getInstance().setChannel(ch);
            ThroughProcess.getInstance().sendSaveInfo();

            while (true) {
                TimeUnit.SECONDS.sleep(2);
                Log.d(TAG,"client while...");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    private void getLocalInetSocketAddress() {
        Client.getInstance();
        try {
            InetAddress address = InetAddress.getLocalHost();//获取的是本地的IP地址 //PC-20140317PXKX/192.168.0.121
            String hostAddress = address.getHostAddress();//192.168.0.121
            Client.getInstance().local_address = address.getHostAddress();
            Client.getInstance().SERVER1 = SocketUtils.socketAddress(Constants.SERVER_HOST1, Constants.PORT1);
            Client.getInstance().SERVER2P1 = SocketUtils.socketAddress(Constants.SERVER_HOST2, Constants.PORT1);
            Client.getInstance().SERVER2P2 = SocketUtils.socketAddress(Constants.SERVER_HOST2, Constants.PORT2);
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
}
