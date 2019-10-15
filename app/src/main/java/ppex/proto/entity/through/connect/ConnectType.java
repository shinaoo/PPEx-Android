package ppex.proto.entity.through.connect;

import ppex.proto.entity.through.Connection;

public class ConnectType {
    public enum Type{
        DIRECT_SEND,            //直接发送消息过去to
        WAIT_DIRECT_SEND,       //等待直接消息过来
        WAIT_PUNCH,             //等待对方打洞,接收到START_PUNCH,才开始发送数据
        START_PUNCH,            //开始打洞,往to发送消息
        PLATFORM_FORWARD,       //走平台转发
    }
    public int connectType;
    public Connection source;
    public Connection target;
}
