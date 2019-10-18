package ppex.proto.type;

/**
 *
 * -----32bits-----+-----body------+
 * --    type    --+--   body    --+
 * ---------------+----------------+
 *
 */

public class TypeMessage {

    public enum Type {
        MSG_TYPE_PROBE,
        MSG_TYPE_THROUGH,
        MSG_TYPE_HEART_PING,
        MSG_TYPE_HEART_PONG,
        MSG_TYPE_TXT,
        MSG_TYPE_FILE,
    }

    public TypeMessage() {
    }

    public TypeMessage(int type, String body) {
        this.type = type;
        this.body = body;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    private int type;
    private String body;


    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "TypeMessage{" +
                "type=" + type +
                ", body='" + body + '\'' +
                '}';
    }
}
