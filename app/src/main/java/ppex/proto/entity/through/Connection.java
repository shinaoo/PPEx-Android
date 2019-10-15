package ppex.proto.entity.through;

import android.util.Log;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import ppex.proto.Message;
import ppex.utils.MessageUtil;

import java.net.InetSocketAddress;

public class Connection {
    private static String TAG = Connection.class.getName();

    public String macAddress;                              //使用mac地址来识别每个Connection
    public String peerName;
    public InetSocketAddress inetSocketAddress;
    public int natType;
    public transient ChannelHandlerContext ctx;

    public Connection(String macAddress,InetSocketAddress inetSocketAddress,String peerName,int natType) {
        this.macAddress = macAddress;
        this.inetSocketAddress = inetSocketAddress;
        this.peerName = peerName;
        this.natType = natType;
    }

    public Connection(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        if (null != ctx.channel() && null != ctx.channel().remoteAddress()) {
            this.inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            Log.d(TAG,"---->new:inetsocketaddress:" + inetSocketAddress.toString());
        }
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public void setPeerName(String peerName) {
        this.peerName = peerName;
    }

    public InetSocketAddress getInetSocketAddress() {
        return this.inetSocketAddress;
    }

    public void sendMsg(Message msg) {
        if (ctx != null) {
            ctx.writeAndFlush(new DatagramPacket(MessageUtil.msg2ByteBuf(msg), inetSocketAddress));
        } else {
        }
    }

    public String getPeerName() {
        return peerName;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Connection that = (Connection) obj;
        return !(macAddress != null ? !macAddress.equals(that.macAddress) : that.macAddress != null);
    }

    @Override
    public String toString() {
        return "Connection{" +
                "inetSocketAddress=" + inetSocketAddress.toString() +
                ", ctx=" + ctx +
                ", peerName='" + peerName + '\'' +
                '}';
    }
}
