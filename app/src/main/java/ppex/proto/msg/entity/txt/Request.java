package ppex.proto.msg.entity.txt;

public class Request {
    private String head;
    private String body;

    public Request() {
    }

    public Request(String head, String body) {
        this.head = head;
        this.body = body;
    }

    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
