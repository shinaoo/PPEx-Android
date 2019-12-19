package ppex.proto.rudp;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface IAddrManager {
    RudpPack get(InetSocketAddress sender);

    void New(InetSocketAddress sender, RudpPack rudpPack);

    void Del(RudpPack rudpPack);

    Collection<RudpPack> getAll();

    Set<Map.Entry<InetSocketAddress, RudpPack>> getAllEntry();
}
