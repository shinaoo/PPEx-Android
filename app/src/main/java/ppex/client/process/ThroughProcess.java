package ppex.client.process;

import com.alibaba.fastjson.JSON;

import io.netty.channel.Channel;
import ppex.client.Client;
import ppex.client.R;
import ppex.client.rudp.ClientOutput;
import ppex.proto.entity.Connection;
import ppex.proto.entity.through.Connect;
import ppex.proto.msg.type.ThroughTypeMsg;
import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.IOutput;
import ppex.proto.rudp.RudpPack;
import ppex.proto.rudp.RudpScheduleTask;
import ppex.utils.MessageUtil;
import ppex.utils.NatTypeUtil;

import java.util.ArrayList;
import java.util.List;

public class ThroughProcess {

    private static String TAG = ThroughProcess.class.getName();
    private Client client;

    private static ThroughProcess instance = null;

    public static ThroughProcess getInstance() {
        if (instance == null) {
            instance = new ThroughProcess();
        }
        return instance;
    }

    private ThroughProcess() {
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void sendSaveInfo() {
        try {
            ThroughTypeMsg throughTypeMsg = new ThroughTypeMsg();
            throughTypeMsg.setAction(ThroughTypeMsg.ACTION.SAVE_CONNINFO.ordinal());
            throughTypeMsg.setContent(JSON.toJSONString(client.getConnLocal()));
            RudpPack rudpPack = client.getAddrManager().get(client.getAddrServer1());

            if (rudpPack == null) {
                Connection connection = new Connection("", client.getAddrServer1(), "Server1", NatTypeUtil.NatType.UNKNOWN.getValue());
                IOutput outputServer1 = new ClientOutput(Client.getInstance().getChannel(), connection);
                rudpPack = new RudpPack(outputServer1, client.getExecutor(), client.getResponseListener());
                client.getAddrManager().New(client.getAddrServer1(), rudpPack);
                rudpPack.write(MessageUtil.throughmsg2Msg(throughTypeMsg));
                RudpScheduleTask task = new RudpScheduleTask(client.getExecutor(), rudpPack, client.getAddrManager());
                client.getExecutor().executeTimerTask(task, rudpPack.getInterval());
            } else {
                rudpPack.write(MessageUtil.throughmsg2Msg(throughTypeMsg));
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getConnectionsFromServer() {
        try {
            ThroughTypeMsg throughTypeMsg = new ThroughTypeMsg();
            throughTypeMsg.setAction(ThroughTypeMsg.ACTION.GET_CONNINFO.ordinal());
            throughTypeMsg.setContent("");
            RudpPack rudpPack = client.getAddrManager().get(client.getAddrServer1());
            if (rudpPack == null) {
                Connection connection = new Connection("", client.getAddrServer1(), "Server1", NatTypeUtil.NatType.UNKNOWN.getValue());
                IOutput outputServer1 = new ClientOutput(Client.getInstance().getChannel(), connection);
                rudpPack = new RudpPack(outputServer1, client.getExecutor(), client.getResponseListener());
                client.getAddrManager().New(client.getAddrServer1(), rudpPack);
                rudpPack.write(MessageUtil.throughmsg2Msg(throughTypeMsg));
                RudpScheduleTask task = new RudpScheduleTask(client.getExecutor(), rudpPack, client.getAddrManager());
                client.getExecutor().executeTimerTask(task, rudpPack.getInterval());
            } else {
                rudpPack.write(MessageUtil.throughmsg2Msg(throughTypeMsg));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connectPeer(Connection to, IAddrManager addrManager) {
        try {
            ThroughTypeMsg throughTypeMsg = new ThroughTypeMsg();
            throughTypeMsg.setAction(ThroughTypeMsg.ACTION.CONNECT_CONN.ordinal());
            //根据两者Connection类型,判断连接类型
            Connect.TYPE connType = NatTypeUtil.getConnectTypeByNatType(client.getConnLocal(), to);

            client.setConnTarget(to);
            client.setConnType2Target(connType);

            Connect connect = new Connect();

            List<Connection> connections = new ArrayList<>();
            connections.add(client.getConnLocal());
            connections.add(to);
            String connectionsStr = JSON.toJSONString(connections);

            //Rudppack
            RudpPack rudpPack;

            if (connType == Connect.TYPE.DIRECT) {
                connect.setType(Connect.TYPE.CONNECT_PING_NORMAL.ordinal());
                connect.setContent("");
                throughTypeMsg.setContent(JSON.toJSONString(connect));
                //等待返回pong就确认建立连接
                rudpPack = addrManager.get(to.getAddress());
                if (rudpPack == null) {
                    IOutput output = new ClientOutput(client.getChannel(), to);
                    client.getOutputManager().put(to.getAddress(), output);
                    rudpPack = RudpPack.newInstance(output, client.getExecutor(), client.getResponseListener());
                    client.getAddrManager().New(to.getAddress(), rudpPack);
                    rudpPack.write(MessageUtil.throughmsg2Msg(throughTypeMsg));
                    RudpScheduleTask rudpScheduleTask = new RudpScheduleTask(client.getExecutor(), rudpPack, addrManager);
                    client.getExecutor().executeTimerTask(rudpScheduleTask, rudpPack.getInterval());
                } else {
                    rudpPack.write(MessageUtil.throughmsg2Msg(throughTypeMsg));
                }

                //发送给Server端，表明正在建立连接
                connect.setType(Connect.TYPE.CONNECTING.ordinal());
                connect.setContent(connectionsStr);
                throughTypeMsg.setContent(JSON.toJSONString(connect));

                rudpPack = addrManager.get(client.getAddrServer1());
                rudpPack.write(MessageUtil.throughmsg2Msg(throughTypeMsg));
            } else if (connType == Connect.TYPE.HOLE_PUNCH) {
                connect.setType(connType.ordinal());
                connect.setContent(connectionsStr);
                throughTypeMsg.setContent(JSON.toJSONString(connect));
                //先将消息发给服务，由服务转发给target connection打洞
                rudpPack = addrManager.get(client.getAddrServer1());
                rudpPack.write(MessageUtil.throughmsg2Msg(throughTypeMsg));


                connect.setType(Connect.TYPE.CONNECTING.ordinal());
                throughTypeMsg.setContent(JSON.toJSONString(connect));
                rudpPack = addrManager.get(client.getAddrServer1());
                rudpPack.write(MessageUtil.throughmsg2Msg(throughTypeMsg));
            } else if (connType == Connect.TYPE.REVERSE) {
                //首先向B 打洞
                connect.setType(Connect.TYPE.CONNECT_PING_REVERSE.ordinal());
                connect.setContent(connectionsStr);
                throughTypeMsg.setContent(JSON.toJSONString(connect));
                //率先打洞
                rudpPack = addrManager.get(to.getAddress());
                if (rudpPack == null) {
                    IOutput output = new ClientOutput(client.getChannel(), to);
                    rudpPack = RudpPack.newInstance(output, client.getExecutor(), client.getResponseListener());
                    addrManager.New(to.getAddress(), rudpPack);
                    for (int i = 0; i < 5; i++) {
                        rudpPack.write(MessageUtil.throughmsg2Msg(throughTypeMsg));
                    }
                    RudpScheduleTask rudpScheduleTask = new RudpScheduleTask(client.getExecutor(), rudpPack, addrManager);
                    client.getExecutor().executeTimerTask(rudpScheduleTask, rudpPack.getInterval());
                } else {
                    for (int i = 0; i < 5; i++) {
                        rudpPack.write(MessageUtil.throughmsg2Msg(throughTypeMsg));
                    }
                }


                //让server给B转发，由B 再通信
                connect.setType(connType.ordinal());
                connect.setContent(connectionsStr);
                throughTypeMsg.setContent(JSON.toJSONString(connect));
                rudpPack = addrManager.get(client.getAddrServer1());
                rudpPack.write(MessageUtil.throughmsg2Msg(throughTypeMsg));

                connect.setType(Connect.TYPE.CONNECTING.ordinal());
                connect.setContent(connectionsStr);
                throughTypeMsg.setContent(JSON.toJSONString(connect));

                rudpPack = addrManager.get(client.getAddrServer1());
                rudpPack.write(MessageUtil.throughmsg2Msg(throughTypeMsg));
            } else if (connType == Connect.TYPE.FORWARD) {
                connect.setType(Connect.TYPE.FORWARD.ordinal());
                connect.setContent(connectionsStr);
                throughTypeMsg.setContent(JSON.toJSONString(connect));

                rudpPack = addrManager.get(client.getAddrServer1());
                rudpPack.write(MessageUtil.throughmsg2Msg(throughTypeMsg));

                connect.setType(Connect.TYPE.CONNECTING.ordinal());
                throughTypeMsg.setContent(JSON.toJSONString(connect));

                rudpPack = addrManager.get(client.getAddrServer1());
                rudpPack.write(MessageUtil.throughmsg2Msg(throughTypeMsg));
            } else {
                throw new Exception("unknown connect operate:" + connectionsStr);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testConnectPeer() {
        try {

        } catch (Exception e) {

        }
    }


}
