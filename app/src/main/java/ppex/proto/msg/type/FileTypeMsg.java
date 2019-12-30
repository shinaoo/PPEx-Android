package ppex.proto.msg.type;

import java.net.InetSocketAddress;

public class FileTypeMsg {

    public FileTypeMsg() {
    }

    public enum ACTION{
        DEL,
        ADD,
        UPD,
        SEL,
        ADD_ACK_SUCC,
        ADD_ACK_FAIL,
        DEL_ACK_SUCC,
        DEL_ACK_FAIL,
        UPD_ACK_SUCC,
        UPD_ACK_FAIL,
        SEL_ACK_SUCC,
        SEL_ACK_FAIL,
    }
    private InetSocketAddress to;
    private byte action;
    private String data;

    public FileTypeMsg(InetSocketAddress to, byte action, String data) {
        this.to = to;
        this.action = action;
        this.data = data;
    }

    public InetSocketAddress getTo() {
        return to;
    }

    public void setTo(InetSocketAddress to) {
        this.to = to;
    }

    public byte getAction() {
        return action;
    }

    public void setAction(byte action) {
        this.action = action;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
