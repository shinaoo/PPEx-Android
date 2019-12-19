package ppex.proto.entity.through;

import ppex.proto.entity.Connection;

import java.util.List;

//保存何种方式连接的两个Connection
public class ConnectMap {
    private int connectType;
    private List<Connection> connections;

    public ConnectMap(int connectType, List<Connection> connections) {
        this.connectType = connectType;
        this.connections = connections;
    }

    public int getConnectType() {
        return connectType;
    }

    public void setConnectType(int connectType) {
        this.connectType = connectType;
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public void setConnections(List<Connection> connections) {
        this.connections = connections;
    }
}
