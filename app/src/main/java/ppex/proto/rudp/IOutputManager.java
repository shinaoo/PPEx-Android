package ppex.proto.rudp;

import java.net.InetSocketAddress;
import java.util.Collection;

public interface IOutputManager {
    IOutput get(InetSocketAddress sender);
    void put(InetSocketAddress sender,IOutput output);
    void del(InetSocketAddress sender);
    Collection<IOutput> getAll();
}
