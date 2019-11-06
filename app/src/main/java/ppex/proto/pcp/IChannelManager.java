package ppex.proto.pcp;

import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;

import java.util.Collection;

public interface IChannelManager {
    PcpPack get(Channel channel, DatagramPacket msg);
    void New(Channel channel, PcpPack pcpPack);
    void Del(PcpPack pcpPack);
    Collection<PcpPack> getAll();
}
