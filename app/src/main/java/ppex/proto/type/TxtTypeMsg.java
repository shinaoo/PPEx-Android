package ppex.proto.type;

import java.net.InetSocketAddress;

public class TxtTypeMsg {
    private boolean isReq;
    private InetSocketAddress from;
    private InetSocketAddress to;
    private String content;

    public TxtTypeMsg() {
    }

    public TxtTypeMsg(boolean isReq, InetSocketAddress from, InetSocketAddress to, String content) {
        this.isReq = isReq;
        this.from = from;
        this.to = to;
        this.content = content;
    }

    public boolean isReq() {
        return isReq;
    }

    public void setReq(boolean req) {
        isReq = req;
    }

    public InetSocketAddress getFrom() {
        return from;
    }

    public void setFrom(InetSocketAddress from) {
        this.from = from;
    }

    public InetSocketAddress getTo() {
        return to;
    }

    public void setTo(InetSocketAddress to) {
        this.to = to;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
