package ppex.utils;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.SocketUtils;
import org.apache.log4j.Logger;
import ppex.proto.Message;
import ppex.proto.type.*;

import java.net.InetSocketAddress;

public class MessageUtil {

    private static Logger LOGGER = Logger.getLogger(MessageUtil.class);

    public static ByteBuf msg2ByteBuf(Message msg) {
        ByteBuf msgBuf = Unpooled.directBuffer(msg.getLength() + Message.VERSIONLENGTH + Message.CONTENTLENGTH + 1);
        msgBuf.writeByte(msg.getVersion());
        msgBuf.writeInt(msg.getLength());
        byte[] bytes = msg.getContent().getBytes(CharsetUtil.UTF_8);
        msgBuf.writeBytes(bytes);
        return msgBuf;
    }

    public static Message bytebuf2Msg(ByteBuf byteBuf) {
        if (byteBuf.readableBytes() < (Message.VERSIONLENGTH + Message.CONTENTLENGTH)) {
            return null;
        }
        byte version = byteBuf.readByte();
        if (version != Constants.MSG_VERSION) {
            return null;
        }
        int length = byteBuf.readInt();
        Message msg = new Message();
        msg.setVersion(version);
        msg.setLength(length);
        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes);
        String content = new String(bytes);
        msg.setContent(content);
        return msg;
    }

    /**
     * -------------------------------------------各类TypeMessage转DatagramPacket部分---------------------------------------------------------
     */
    public static DatagramPacket msg2Packet(Message message, InetSocketAddress inetSocketAddress) {
        return new DatagramPacket(msg2ByteBuf(message), inetSocketAddress);
    }

    public static DatagramPacket msg2Packet(Message message, String host, int port) {
        return new DatagramPacket(msg2ByteBuf(message), SocketUtils.socketAddress(host, port));
    }

    public static DatagramPacket typemsg2Packet(TypeMessage typeMessage, InetSocketAddress inetSocketAddress) {
        Message msg = new Message();
        msg.setContent(typeMessage);
        return msg2Packet(msg, inetSocketAddress);
    }

    public static DatagramPacket typemsg2Packet(TypeMessage typeMessage, String host, int port) {
        return typemsg2Packet(typeMessage, SocketUtils.socketAddress(host, port));
    }

    public static DatagramPacket probemsg2Packet(ProbeTypeMsg msg, InetSocketAddress address) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_PROBE.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Packet(typeMessage, address);
    }

    public static DatagramPacket probemsg2Packet(ProbeTypeMsg probeTypeMsg, String host, int port) {
        return probemsg2Packet(probeTypeMsg, SocketUtils.socketAddress(host, port));
    }


    public static DatagramPacket throughmsg2Packet(ThroughTypeMsg msg, InetSocketAddress address) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_THROUGH.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Packet(typeMessage, address);
    }

    public static DatagramPacket throughmsg2Packet(ThroughTypeMsg msg, String host, int port) {
        return throughmsg2Packet(msg, SocketUtils.socketAddress(host, port));
    }

    public static DatagramPacket pingMsg2Packet(PingTypeMsg msg, InetSocketAddress address) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_HEART_PING.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Packet(typeMessage, address);
    }

    public static DatagramPacket pingMsg2Packet(PingTypeMsg msg, String host, int port) {
        return pingMsg2Packet(msg, SocketUtils.socketAddress(host, port));
    }

    public static DatagramPacket pongMsg2Packet(PongTypeMsg msg,String host,int port){
        return pongMsg2Packet(msg,SocketUtils.socketAddress(host,port));
    }

    public static DatagramPacket pongMsg2Packet(PongTypeMsg msg,InetSocketAddress address){
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_HEART_PONG.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Packet(typeMessage,address);
    }

    public static DatagramPacket fileMsg2Packet(FileTypeMsg msg,InetSocketAddress address){
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_FILE.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Packet(typeMessage,address);
    }

    public static DatagramPacket txtMsg2packet(TxtTypeMsg msg,InetSocketAddress address){
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_TXT.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Packet(typeMessage,address);
    }

    /**
     * ----------------------------------DatagramPacket转各类TypeMessage部分----------------------------------------------------
     **/
    public static Message packet2Msg(DatagramPacket packet) {
        return bytebuf2Msg(packet.content());
    }

    public static TypeMessage packet2Typemsg(DatagramPacket packet) {
        Message msg = packet2Msg(packet);
        TypeMessage tMsg = JSON.parseObject(msg.getContent(), TypeMessage.class);
        return tMsg;
    }

    public static ProbeTypeMsg packet2Probemsg(DatagramPacket packet) {
        TypeMessage tmsg = packet2Typemsg(packet);
        ProbeTypeMsg pmsg = JSON.parseObject(tmsg.getBody(), ProbeTypeMsg.class);
        pmsg.setFromInetSocketAddress(packet.sender());
        return pmsg;
    }

    public static ThroughTypeMsg packet2ThroughMsg(DatagramPacket packet) {
        TypeMessage tmsg = packet2Typemsg(packet);
        ThroughTypeMsg ttmsg = JSON.parseObject(tmsg.getBody(), ThroughTypeMsg.class);
        return ttmsg;
    }

    public static PingTypeMsg packet2PingTypeMsg(DatagramPacket packet) {
        TypeMessage tmsg = packet2Typemsg(packet);
        PingTypeMsg pmsg = JSON.parseObject(tmsg.getBody(), PingTypeMsg.class);
        return pmsg;
    }

    public static PongTypeMsg packet2Pongmsg(DatagramPacket packet){
        TypeMessage typeMessage = packet2Typemsg(packet);
        PongTypeMsg pmsg = JSON.parseObject(typeMessage.getBody(),PongTypeMsg.class);
        return pmsg;
    }

    public static FileTypeMsg packet2FileMsg(DatagramPacket packet){
        TypeMessage typeMessage = packet2Typemsg(packet);
        FileTypeMsg fmsg = JSON.parseObject(typeMessage.getBody(),FileTypeMsg.class);
        return fmsg;
    }

    public static TxtTypeMsg packet2Txtmsg(DatagramPacket packet){
        TypeMessage typeMessage = packet2Typemsg(packet);
        TxtTypeMsg tmsg = JSON.parseObject(typeMessage.getBody(),TxtTypeMsg.class);
        return tmsg;
    }


    /**
     * ----------------------------------------------生成探测ProbeTypeMsg部分----------------------------------------------------
     */
    public static ProbeTypeMsg makeClientStepOneProbeTypeMsg(String host, int port) {
        return makeClientStepOneProbeTypeMsg(SocketUtils.socketAddress(host, port));
    }

    public static ProbeTypeMsg makeClientStepOneProbeTypeMsg(InetSocketAddress inetSocketAddress) {
        ProbeTypeMsg probeTypeMsg = new ProbeTypeMsg(TypeMessage.Type.MSG_TYPE_PROBE.ordinal(), inetSocketAddress);
        probeTypeMsg.setType(ProbeTypeMsg.Type.FROM_CLIENT.ordinal());
        probeTypeMsg.setStep(ByteUtil.int2byteArr(ProbeTypeMsg.Step.ONE.ordinal())[3]);
        return probeTypeMsg;
    }

    public static ProbeTypeMsg makeClientStepTwoProbeTypeMsg(String host, int port) {
        return makeClientStepTwoProbeTypeMsg(SocketUtils.socketAddress(host, port));
    }

    public static ProbeTypeMsg makeClientStepTwoProbeTypeMsg(InetSocketAddress inetSocketAddress) {
        ProbeTypeMsg probeTypeMsg = new ProbeTypeMsg(TypeMessage.Type.MSG_TYPE_PROBE.ordinal(), inetSocketAddress);
        probeTypeMsg.setType(ProbeTypeMsg.Type.FROM_CLIENT.ordinal());
        probeTypeMsg.setStep(ByteUtil.int2byteArr(ProbeTypeMsg.Step.TWO.ordinal())[3]);
        return probeTypeMsg;
    }


}