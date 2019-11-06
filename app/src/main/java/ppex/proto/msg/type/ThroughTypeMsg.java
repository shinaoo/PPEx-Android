package ppex.proto.msg.type;

/**
 * 19-10-8 暂定
 * action 0 为保存connection信息,包括id,peername,nattype,ip address..content内容为id,peername,nattype,ipaddress
 * action 1 为获取所有connection信息,content格式为,from:1,to:2
 * action 2 为连接某个connection.即另一个peer了.content为需要连接的id,根据id找到connection
 * action 3 为返回信息的集合,content根据里面的分类再做分解
 */
public class ThroughTypeMsg {

    public enum ACTION {
        SAVE_CONNINFO,
        GET_CONNINFO,
        CONNECT_CONN,
        RECV_INFO,
    }

    //RECVTYPE为RECVINFO下面的type类型值,目前暂时有Id集合的JSON字符串,为GET_INFO的返回信息.RESPONSE是返回的一般信息反馈
    //返回信息已RECVINFO作为载体.RECVINFO里面type再表示是SAVE_INFO,GET_INFO,CONNECT的返回信息.再进行对应
    public enum RECVTYPE {
        SAVE_CONNINFO,
        GET_CONNINFO,
        CONNECT_CONN,
        RESPONSE,
    }

    private int action;
    private String content;

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "ThroughTypeMsg{" +
                "action=" + action +
                ", content='" + content + '\'' +
                '}';
    }
}
