package ppex.client.process;

import io.netty.channel.Channel;
import ppex.client.entity.Client;
import ppex.utils.Constants;
import ppex.utils.MessageUtil;

import java.net.InetSocketAddress;


/**
 * Device探测自己的NAT类型
 * 根据工作原理.md里面的记录.19-10-8对第一阶段和第二阶段的优化记录.
 * 第一阶段和第二阶段的探测就不用隔开一段时间,需要Server1:Port1向Server2:Port2发送包
 */
public class DetectProcess {

    public boolean stop = false;

    private Channel channel;

    public static DetectProcess instance = null;


    private DetectProcess() {
    }

    public static DetectProcess getInstance() {
        if (instance == null)
            instance = new DetectProcess();
        return instance;
    }

    //首先client给s1发送消息,根据s1返回的消息判断是否处于公网,如果不是公网,保存返回的 NAT地址
    public boolean isPublicNetwork = false;
    public InetSocketAddress NAT_ADDRESS_FROM_S1;

    //然后s1给s2p2发送消息,如果能收到消息,而且是第一阶段的,是Full Cone NAT
    private boolean one_from_server2p2 = false;         //第一阶段的是否已经从server2p2返回信息

    //然后client给S2P1发送的消息,根据S2P1返回的消息,比较S1 返回的NAT地址,看端口是否一样,如果不一样,则是SymmetricNAT.
    public InetSocketAddress NAT_ADDRESS_FROM_S2P1;
    public boolean NAT_ADDRESS_SAME = false;        //比较结果

    //如果一样,判断S2P2是否返回第二阶段消息,如果没返回,是PORT RESTRICT CONE NAT,返回了则是RESTRICT CONE NAT
    private boolean two_from_server2p2 = false;         //是否已经从Server2p2返回信息


    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void startDetect() {
        try {
            one_send2s1();
            two_send2s2p1();
//            while (!stop) {
//                System.out.println("Client sleep 2000");
//                Thread.sleep(2000);
//            }
            if (!channel.closeFuture().await(2000)){
                System.out.println("查询超时");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Constants.NATTYPE getClientNATType(){
        //开始判断NAT类型
        if(isPublicNetwork){
            return Constants.NATTYPE.PUBLIC_NETWORK;
        }
        if (isOne_from_server2p2()){
            return Constants.NATTYPE.FULL_CONE_NAT;
        }
        if (!DetectProcess.getInstance().NAT_ADDRESS_SAME){
            return Constants.NATTYPE.SYMMETIC_NAT;
        }else{
            if (DetectProcess.getInstance().isTwo_from_server2p2()){
                return Constants.NATTYPE.RESTRICT_CONE_NAT;
            }else{
                return Constants.NATTYPE.PORT_RESTRICT_CONE_NAT;
            }
        }
    }

    public void one_send2s1() throws Exception {
        this.channel.writeAndFlush(MessageUtil.probemsg2Packet(MessageUtil.makeClientStepOneProbeTypeMsg(Client.getInstance().local_address, Constants.PORT1), Client.getInstance().SERVER1));
    }

    public void two_send2s2p1() throws Exception {
        this.channel.writeAndFlush(MessageUtil.probemsg2Packet(MessageUtil.makeClientStepTwoProbeTypeMsg(Client.getInstance().local_address, Constants.PORT1), Client.getInstance().SERVER2P1));
        System.out.println("send2s2p1...");
    }

    public void sendS2P2() throws Exception {
        this.channel.writeAndFlush(MessageUtil.probemsg2Packet(MessageUtil.makeClientStepTwoProbeTypeMsg(Client.getInstance().local_address, Constants.PORT1), Client.getInstance().SERVER2P2));
    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }


    public boolean isOne_from_server2p2() {
        return one_from_server2p2;
    }

    public void setOne_from_server2p2(boolean one_from_server2p2) {
        this.one_from_server2p2 = one_from_server2p2;
    }

    public boolean isTwo_from_server2p2() {
        return two_from_server2p2;
    }

    public void setTwo_from_server2p2(boolean two_from_server2p2) {
        this.two_from_server2p2 = two_from_server2p2;
    }


}
