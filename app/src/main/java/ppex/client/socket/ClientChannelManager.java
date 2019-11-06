package ppex.client.socket;

import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import ppex.proto.pcp.IChannelManager;
import ppex.proto.pcp.PcpPack;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientChannelManager implements IChannelManager {

    private Map<SocketAddress,PcpPack> pcpPackMap = new ConcurrentHashMap<>(20,0.9f);

    private ClientChannelManager(){}
    public static ClientChannelManager New(){
        return new ClientChannelManager();
    }

    @Override
    public PcpPack get(Channel channel, DatagramPacket msg) {
        return get(channel,msg.sender());
    }

    public PcpPack get(Channel channel, InetSocketAddress sender) {
        return pcpPackMap.get(sender);
    }

    @Override
    public void New(Channel channel, PcpPack pcpPack) {
        pcpPackMap.put(channel.localAddress(),pcpPack);
    }

    @Override
    public void Del(PcpPack pcpPack) {

    }

    @Override
    public Collection<PcpPack> getAll() {
        return this.pcpPackMap.values();
    }
}
