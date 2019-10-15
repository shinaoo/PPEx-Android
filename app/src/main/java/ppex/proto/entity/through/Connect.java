package ppex.proto.entity.through;

/**
 * 因为是客户端和服务端共用.所以比较麻烦
 * 如果是Client端用,直接将CONNECT序列化放入ThroughTypeMsg即可
 * 如果是Server端用,需要将CONNECT序列化放入RECVINFO中,RECVINFO再放入ThroughTypeMsg.
 */
public class Connect {

    public enum TYPE{
        CONNECT_ERROR,
        REQUEST_CONNECT_SERVER,            //一般是Client发给Server,请求与某个建立连接
        RETURN_REQUEST_CONNECT_SERVER,       //Server通过判断类型后返回Client,让该Client将包发到某个地址
        RETURN_START_PUNCH,                 //Server收到打洞消息返回
        CONNECT_PING,                       //连接消息和响应
        CONNECT_PONG,
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
