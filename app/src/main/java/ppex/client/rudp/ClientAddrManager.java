package ppex.client.rudp;

import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.RudpPack;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientAddrManager implements IAddrManager {

    private Map<InetSocketAddress, RudpPack> rudppacks = new ConcurrentHashMap<>(5, 0.9f);


    @Override
    public RudpPack get(InetSocketAddress sender) {
        return rudppacks.get(sender);
    }

    @Override
    public void New(InetSocketAddress sender, RudpPack rudpPack) {
        rudppacks.put(sender, rudpPack);
    }

    @Override
    public void Del(RudpPack rudpPack) {
        rudppacks.entrySet().removeIf(entry -> entry.getValue().equals(rudpPack));
    }

    @Override
    public Collection<RudpPack> getAll() {
        return rudppacks.values();
    }

    @Override
    public Set<Map.Entry<InetSocketAddress, RudpPack>> getAllEntry() {
        return rudppacks.entrySet();
    }
}
