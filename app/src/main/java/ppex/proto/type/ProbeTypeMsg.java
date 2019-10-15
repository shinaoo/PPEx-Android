package ppex.proto.type;

import java.net.InetSocketAddress;

public class ProbeTypeMsg implements TypeMessageHandler {

    public enum Type {
        FROM_CLIENT,
        FROM_SERVER1,
        FROM_SERVER2_PORT1,
        FROM_SERVER2_PORT2
    }

    public enum Step {
        ONE,
        TWO
    }

    public ProbeTypeMsg() {
    }

    public ProbeTypeMsg(int type, InetSocketAddress inetSocketAddress) {
        this.type = type;
        this.fromInetSocketAddress = inetSocketAddress;
    }

    /**
     * 消息从哪里来,是Type类型的值
     */
    private int type;
    /**
     * 第几阶段,看resources下的工作原理.md
     */
    private byte step;
    private InetSocketAddress fromInetSocketAddress;
    private InetSocketAddress recordInetSocketAddress;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public InetSocketAddress getFromInetSocketAddress() {
        return fromInetSocketAddress;
    }

    public void setFromInetSocketAddress(InetSocketAddress fromInetSocketAddress) {
        this.fromInetSocketAddress = fromInetSocketAddress;
    }

    public InetSocketAddress getRecordInetSocketAddress() {
        return recordInetSocketAddress;
    }

    public void setRecordInetSocketAddress(InetSocketAddress recordInetSocketAddress) {
        this.recordInetSocketAddress = recordInetSocketAddress;
    }

    public byte getStep() {
        return step;
    }

    public void setStep(byte step) {
        this.step = step;
    }

    @Override
    public String toString() {
        return "ProbeTypeMsg{" +
                "type=" + type +
                ", step=" + step +
                ", fromInetSocketAddress=" + fromInetSocketAddress +
                ", recordInetSocketAddress=" + recordInetSocketAddress +
                '}';
    }
}
