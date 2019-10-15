package ppex.client.entity;

import ppex.proto.entity.through.Connection;
import ppex.utils.Constants;

import java.net.InetSocketAddress;

public class Client {

    private static Client instance =null;

    private Client() {
    }

    public static Client getInstance() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    public long id = 1;
    public String peerName = "client1";
    public InetSocketAddress address;
    public int NAT_TYPE = Constants.NATTYPE.UNKNOWN.ordinal();

    public String local_address = null;
    public String MAC_ADDRESS=null;
    public InetSocketAddress SERVER1;
    public InetSocketAddress SERVER2P1;
    public InetSocketAddress SERVER2P2;

    //0是直接发送消息给targetConnection.1是中断,将消息发给Server1
    public int connectType=0;
    public Connection localConnection;
    public Connection targetConnection;

}
