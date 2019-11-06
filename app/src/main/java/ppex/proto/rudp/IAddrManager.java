package ppex.proto.rudp;

import java.net.InetSocketAddress;
import java.util.Collection;

public interface IAddrManager {
    RudpPack get(InetSocketAddress sender);
    void New(InetSocketAddress sender, RudpPack rudpPack);
    void Del(RudpPack rudpPack);
    Collection<RudpPack> getAll();
}
