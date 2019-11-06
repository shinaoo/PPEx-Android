package ppex.client.socket;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.RudpPack;

public class ClientAddrManager implements IAddrManager {
    private Map<InetSocketAddress, RudpPack> rudpPacks = new ConcurrentHashMap<>(30,0.9f);

    private static ClientAddrManager clientAddrManager = null;
    private ClientAddrManager(){}
    public static ClientAddrManager getInstance(){
        if (clientAddrManager == null)
            clientAddrManager = new ClientAddrManager();
        return clientAddrManager;
    }

    @Override
    public RudpPack get(InetSocketAddress sender) {
        return rudpPacks.get(sender);
    }

    @Override
    public void New(InetSocketAddress sender, RudpPack rudpPack) {
        rudpPacks.put(sender,rudpPack);
    }

    @Override
    public void Del(RudpPack rudpPack) {
    }

    @Override
    public Collection<RudpPack> getAll() {
        return rudpPacks.values();
    }
}
