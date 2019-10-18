package ppex.client.entity;

import com.alibaba.fastjson.JSON;

import ppex.proto.entity.through.Connect;
import ppex.proto.entity.through.ConnectMap;
import ppex.proto.entity.through.Connection;
import ppex.utils.Constants;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

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
    public Connection localConnection;
    public Connection targetConnection;

    //保存建立连接的Connections集合
    public List<ConnectMap> connectingMaps = new ArrayList<>();
    public List<ConnectMap> connectedMaps = new ArrayList<>();

    public static Connect.TYPE judgeConnectType(Connection A,Connection B){
        if (B.natType == Constants.NATTYPE.PUBLIC_NETWORK.getValue()){
            return Connect.TYPE.DIRECT;
        }else if (B.natType == Constants.NATTYPE.FULL_CONE_NAT.getValue() || B.natType == Constants.NATTYPE.RESTRICT_CONE_NAT.getValue()){
            return Connect.TYPE.HOLE_PUNCH;
        }else if (B.natType == Constants.NATTYPE.PORT_RESTRICT_CONE_NAT.getValue() && A.natType == Constants.NATTYPE.SYMMETIC_NAT.getValue()){
            return Connect.TYPE.FORWARD;
        }else if (B.natType == Constants.NATTYPE.PORT_RESTRICT_CONE_NAT.getValue() && A.natType != Constants.NATTYPE.SYMMETIC_NAT.getValue()){
            return Connect.TYPE.HOLE_PUNCH;
        }else if (B.natType == Constants.NATTYPE.SYMMETIC_NAT.getValue() && (A.natType == Constants.NATTYPE.PUBLIC_NETWORK.getValue() ||
                A.natType == Constants.NATTYPE.FULL_CONE_NAT.getValue() || A.natType == Constants.NATTYPE.RESTRICT_CONE_NAT.getValue())){
            return Connect.TYPE.REVERSE;
        }else if (B.natType == Constants.NATTYPE.SYMMETIC_NAT.getValue() && (A.natType == Constants.NATTYPE.PORT_RESTRICT_CONE_NAT.getValue() ||
                A.natType == Constants.NATTYPE.SYMMETIC_NAT.getValue())){
            return Connect.TYPE.FORWARD;
        }else{
            return Connect.TYPE.FORWARD;
        }
    }

    public static boolean isConnecting(List<ConnectMap> maps,Connect connect){
        List<Connection> connections = JSON.parseArray(connect.getContent(),Connection.class);
        boolean isConnecting = false;
        return maps.stream().anyMatch(connectMap ->{
            return connectMap.getConnections().get(0).equals(connections.get(0)) && connectMap.getConnections().get(1).equals(connections.get(1));
        });
    }





}
