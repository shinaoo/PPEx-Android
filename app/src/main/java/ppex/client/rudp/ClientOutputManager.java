package ppex.client.rudp;

import ppex.proto.rudp.IOutput;
import ppex.proto.rudp.IOutputManager;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientOutputManager implements IOutputManager {

    private Map<InetSocketAddress,IOutput> outputs = new ConcurrentHashMap<>(5,0.9f);

    @Override
    public IOutput get(InetSocketAddress sender) {
        return outputs.get(sender);
    }

    @Override
    public void put(InetSocketAddress sender,IOutput output) {
        outputs.put(sender,output);
    }

    @Override
    public void del(InetSocketAddress sender) {
        outputs.entrySet().removeIf(entry -> entry.getKey().equals(sender));
    }

    @Override
    public Collection<IOutput> getAll() {
        return outputs.values();
    }
}
