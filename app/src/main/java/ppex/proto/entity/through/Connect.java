package ppex.proto.entity.through;

/**
 * 因为是客户端和服务端共用.所以比较麻烦
 * 如果是Client端用,直接将CONNECT序列化放入ThroughTypeMsg即可
 * 如果是Server端用,需要将CONNECT序列化放入RECVINFO中,RECVINFO再放入ThroughTypeMsg.
 */
public class Connect {

    public enum TYPE{
        CONNECT_PING,                               //两边之间利用PING,PONG确认打通
        CONNECT_PONG,
        //通信类型
        DIRECT,                                     //根据两边nattype判断类型
        HOLE_PUNCH,
        REVERSE,
        FORWARD,

        CONNECTING,                                //暂时保留，client发给Server，可以检测两边连接状态
        CONNECTED,
        ;
    }

    private int type;
    //之前保留from和to.现在使用content保存connect包下的有关类的JSON字符串
    private String content;


    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Connect{" +
                "type=" + type +
                ", content='" + content + '\'' +
                '}';
    }
}
