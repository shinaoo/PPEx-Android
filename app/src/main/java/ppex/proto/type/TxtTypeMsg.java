package ppex.proto.type;

import java.net.InetSocketAddress;

public class TxtTypeMsg {

    private InetSocketAddress to;
    private String content;

    public TxtTypeMsg() {
    }

    public TxtTypeMsg(InetSocketAddress to, String content) {
        this.to = to;
        this.content = content;
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
