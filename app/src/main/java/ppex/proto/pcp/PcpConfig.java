package ppex.proto.pcp;

public class PcpConfig {
    //576 (modern) - 20 (ip) - 8 (udp) = 548.后面可以检测mtu实际值来进行设置
    private int mtu = 512;
    private int sndwnd = 512;
    private int rcvwnd = 512;
    private long timeoutMillis;
    private long send_timeout;
    
}
