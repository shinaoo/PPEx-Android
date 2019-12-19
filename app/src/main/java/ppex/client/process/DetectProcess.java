package ppex.client.process;

import ppex.client.Client;
import ppex.proto.rudp.RudpPack;
import ppex.utils.MessageUtil;
import ppex.utils.NatTypeUtil;

import java.net.InetSocketAddress;


/**
 * Device探测自己的NAT类型
 * 根据工作原理.md里面的记录.19-10-8对第一阶段和第二阶段的优化记录.
 * 第一阶段和第二阶段的探测就不用隔开一段时间,需要Server1:Port1向Server2:Port2发送包
 */
public class DetectProcess {

    private static String TAG = DetectProcess.class.getName();

    public boolean stop = false;

    private Client client;

    private static DetectProcess instance = null;
    public static DetectProcess getInstance(){
        if (instance == null){
            instance = new DetectProcess();
        }
        return instance;
    }
    private DetectProcess(){}

    public void setClient(Client client) {
        this.client = client;
    }

    //首先client给s1发送消息,根据s1返回的消息判断是否处于公网,如果不是公网,保存返回的 NAT地址
    private boolean isPublicNetwork = false;
    private InetSocketAddress NAT_ADDRESS_FROM_S1 = null;

    //然后s1给s2p2发送消息,如果能收到消息,而且是第一阶段的,是Full Cone NAT
    private boolean one_from_server2p2 = false;         //第一阶段的是否已经从server2p2返回信息

    //然后client给S2P1发送的消息,根据S2P1返回的消息,比较S1 返回的NAT地址,看端口是否一样,如果不一样,则是SymmetricNAT.
    private InetSocketAddress NAT_ADDRESS_FROM_S2P1 = null;
    private boolean NAT_ADDRESS_SAME = false;        //比较结果

    //如果一样,判断S2P2是否返回第二阶段消息,如果没返回,是PORT RESTRICT CONE NAT,返回了则是RESTRICT CONE NAT
    private boolean two_from_server2p2 = false;         //是否已经从Server2p2返回信息

    public void startDetect() {
        try {
            one_send2s1();
            two_send2s2p1();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send2S1() throws Exception {
        one_send2s1();
    }

    public void send2S2P1()throws Exception {
        two_send2s2p1();
    }
    public NatTypeUtil.NatType getClientNATType(){
        //开始判断NAT类型
        if(isPublicNetwork){
            return NatTypeUtil.NatType.PUBLIC_NETWORK;
        }
        if (isOne_from_server2p2()){
            return NatTypeUtil.NatType.FULL_CONE_NAT;
        }
        if (!NAT_ADDRESS_SAME){
            return NatTypeUtil.NatType.SYMMETIC_NAT;
        }else{
            if (isTwo_from_server2p2()){
                return NatTypeUtil.NatType.RESTRICT_CONE_NAT;
            }else{
                return NatTypeUtil.NatType.PORT_RESTRICT_CONE_NAT;
            }
        }
    }

    public void one_send2s1() throws Exception {
        RudpPack rudpPack = client.getAddrManager().get(client.getAddrServer1());
        rudpPack.write(MessageUtil.probemsg2Msg(MessageUtil.makeClientStepOneProbeTypeMsg(client.getAddrLocal())));
    }

    public void two_send2s2p1() throws Exception {
        RudpPack rudpPack = client.getAddrManager().get(client.getAddrServer2p1());
        rudpPack.write(MessageUtil.probemsg2Msg(MessageUtil.makeClientStepTwoProbeTypeMsg(client.getAddrLocal())));
    }

    public boolean isPublicNetwork() {
        return isPublicNetwork;
    }

    public void setPublicNetwork(boolean publicNetwork) {
        isPublicNetwork = publicNetwork;
    }

    public InetSocketAddress getNAT_ADDRESS_FROM_S1() {
        return NAT_ADDRESS_FROM_S1;
    }

    public void setNAT_ADDRESS_FROM_S1(InetSocketAddress NAT_ADDRESS_FROM_S1) {
        this.NAT_ADDRESS_FROM_S1 = NAT_ADDRESS_FROM_S1;
    }

    public boolean isOne_from_server2p2() {
        return one_from_server2p2;
    }

    public void setOne_from_server2p2(boolean one_from_server2p2) {
        this.one_from_server2p2 = one_from_server2p2;
    }

    public InetSocketAddress getNAT_ADDRESS_FROM_S2P1() {
        return NAT_ADDRESS_FROM_S2P1;
    }

    public void setNAT_ADDRESS_FROM_S2P1(InetSocketAddress NAT_ADDRESS_FROM_S2P1) {
        this.NAT_ADDRESS_FROM_S2P1 = NAT_ADDRESS_FROM_S2P1;
    }

    public boolean isNAT_ADDRESS_SAME() {
        return NAT_ADDRESS_SAME;
    }

    public void setNAT_ADDRESS_SAME(boolean NAT_ADDRESS_SAME) {
        this.NAT_ADDRESS_SAME = NAT_ADDRESS_SAME;
    }

    public boolean isTwo_from_server2p2() {
        return two_from_server2p2;
    }

    public void setTwo_from_server2p2(boolean two_from_server2p2) {
        this.two_from_server2p2 = two_from_server2p2;
    }
}
