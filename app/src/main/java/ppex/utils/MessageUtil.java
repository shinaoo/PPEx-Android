package ppex.utils;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ppex.proto.msg.Message;
import ppex.proto.msg.type.*;

import java.net.InetSocketAddress;

public class MessageUtil {

    private static Logger LOGGER = LoggerFactory.getLogger(MessageUtil.class);

    //分配ByteBuf类
    private static ByteBufAllocator byteBufAllocator = ByteBufAllocator.DEFAULT;

    public static ByteBuf msg2ByteBuf(Message msg) {
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        ByteBuf msgBuf = allocator.ioBuffer(msg.getLength() + Message.VERSIONLENGTH + Message.CONTENTLENGTH + 1);
//        msgBuf.writeLongLE(msg.getMsgid());
        msgBuf.writeLong(msg.getMsgid());
        msgBuf.writeByte(msg.getVersion());
        msgBuf.writeInt(msg.getLength());
        byte[] bytes = msg.getContent();
        msgBuf.writeBytes(bytes);
        return msgBuf;
    }

    public static Message bytebuf2Msg(ByteBuf byteBuf) {
        if (byteBuf.readableBytes() < (Message.VERSIONLENGTH + Message.CONTENTLENGTH + Message.ID_LEN)) {
            return null;
        }
        long msgid = byteBuf.readLong();
        byte version = byteBuf.readByte();
        if (version != Constants.MSG_VERSION) {
            return null;
        }
        int length = byteBuf.readInt();
        Message msg = new Message(LongIDUtil.getCurrentId());
        msg.setMsgid(msgid);
        msg.setVersion(version);
        msg.setLength(length);
        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes);
        String content = new String(bytes);
        msg.setContent(content);
        return msg;
    }

//    public static List<ByteBuf> msg2ByteBuf(Message msg, InetSocketAddress inetSocketAddress, int mss){
//        ByteBuf total = msg2ByteBuf(msg);
//        long now = System.currentTimeMillis();
//        if (total.readableBytes() <= mss){
//
//        }
//
//        total.readableBytes();
//    }

    /**
     * -------------------------------------------各类TypeMessage转DatagramPacket部分---------------------------------------------------------
     */
    public static DatagramPacket msg2Packet(Message message, InetSocketAddress inetSocketAddress) {
        return new DatagramPacket(msg2ByteBuf(message), inetSocketAddress);
    }

    public static DatagramPacket msg2Packet(Message message, String host, int port) {
        return new DatagramPacket(msg2ByteBuf(message), new InetSocketAddress(host, port));
    }

    public static DatagramPacket typemsg2Packet(TypeMessage typeMessage, InetSocketAddress inetSocketAddress) {
        Message msg = new Message(LongIDUtil.getCurrentId());
        msg.setContent(typeMessage);
        return msg2Packet(msg, inetSocketAddress);
    }

    public static DatagramPacket typemsg2Packet(TypeMessage typeMessage, String host, int port) {
        return typemsg2Packet(typeMessage, new InetSocketAddress(host, port));
    }

    public static DatagramPacket probemsg2Packet(ProbeTypeMsg msg, InetSocketAddress address) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_PROBE.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Packet(typeMessage, address);
    }

    public static DatagramPacket probemsg2Packet(ProbeTypeMsg probeTypeMsg, String host, int port) {
        return probemsg2Packet(probeTypeMsg, new InetSocketAddress(host, port));
    }


    public static DatagramPacket throughmsg2Packet(ThroughTypeMsg msg, InetSocketAddress address) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_THROUGH.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Packet(typeMessage, address);
    }

    public static DatagramPacket throughmsg2Packet(ThroughTypeMsg msg, String host, int port) {
        return throughmsg2Packet(msg, new InetSocketAddress(host, port));
    }

    public static DatagramPacket pingMsg2Packet(PingTypeMsg msg, InetSocketAddress address) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_HEART_PING.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Packet(typeMessage, address);
    }

    public static DatagramPacket pingMsg2Packet(PingTypeMsg msg, String host, int port) {
        return pingMsg2Packet(msg, new InetSocketAddress(host, port));
    }

    public static DatagramPacket pongMsg2Packet(PongTypeMsg msg, String host, int port) {
        return pongMsg2Packet(msg, new InetSocketAddress(host, port));
    }

    public static DatagramPacket pongMsg2Packet(PongTypeMsg msg, InetSocketAddress address) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_HEART_PONG.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Packet(typeMessage, address);
    }

    public static DatagramPacket fileMsg2Packet(FileTypeMsg msg, InetSocketAddress address) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_FILE.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Packet(typeMessage, address);
    }

    public static DatagramPacket txtMsg2packet(TxtTypeMsg msg, InetSocketAddress address) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_TXT.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Packet(typeMessage, address);
    }

    /**
     * ----------------------------------各类TypeMessage转ByteBuf部分----------------------------------------------------
     **/

    public static ByteBuf typemsg2Bytebuf(TypeMessage typeMessage) {
        Message msg = new Message(LongIDUtil.getCurrentId());
        msg.setContent(typeMessage);
        return msg2ByteBuf(msg);
    }

    public static ByteBuf probemsg2Bytebuf(ProbeTypeMsg msg) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_PROBE.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Bytebuf(typeMessage);
    }

    public static ByteBuf throughmsg2Bytebuf(ThroughTypeMsg msg) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_THROUGH.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Bytebuf(typeMessage);
    }

    public static ByteBuf pingMsg2Bytebuf(PingTypeMsg msg) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_HEART_PING.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Bytebuf(typeMessage);
    }

    public static ByteBuf pongMsg2Bytebuf(PongTypeMsg msg) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_HEART_PONG.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Bytebuf(typeMessage);
    }

    public static ByteBuf fileMsg2Packet(FileTypeMsg msg) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_FILE.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Bytebuf(typeMessage);
    }

    public static ByteBuf txtMsg2packet(TxtTypeMsg msg) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_TXT.ordinal());
        typeMessage.setBody(JSON.toJSONString(msg));
        return typemsg2Bytebuf(typeMessage);
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

    public static PongTypeMsg packet2Pongmsg(DatagramPacket packet) {
        TypeMessage typeMessage = packet2Typemsg(packet);
        PongTypeMsg pmsg = JSON.parseObject(typeMessage.getBody(), PongTypeMsg.class);
        return pmsg;
    }

    public static FileTypeMsg packet2FileMsg(DatagramPacket packet) {
        TypeMessage typeMessage = packet2Typemsg(packet);
        FileTypeMsg fmsg = JSON.parseObject(typeMessage.getBody(), FileTypeMsg.class);
        return fmsg;
    }

    public static TxtTypeMsg packet2Txtmsg(DatagramPacket packet) {
        TypeMessage typeMessage = packet2Typemsg(packet);
        TxtTypeMsg tmsg = JSON.parseObject(typeMessage.getBody(), TxtTypeMsg.class);
        return tmsg;
    }

    /**
     * ------------------------------------Message转各类TypeMessage部分------------------------------------------
     */
    public static TxtTypeMsg msg2TxtMsg(Message msg) {
        TypeMessage typeMessage = JSON.parseObject(msg.getContent(), TypeMessage.class);
        TxtTypeMsg tmsg = JSON.parseObject(typeMessage.getBody(), TxtTypeMsg.class);
        return tmsg;
    }


    /**
     * --------------------------------各类TypeMessage转Message部分-----------------------------------
     */
    public static Message txtmsg2Msg(TxtTypeMsg ttmsg) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setBody(JSON.toJSONString(ttmsg));
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_TXT.ordinal());
        Message msg = new Message(LongIDUtil.getCurrentId());
        msg.setContent(typeMessage);
        return msg;
    }

    public static Message pongmsg2Msg(PongTypeMsg pongTypeMsg) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_HEART_PONG.ordinal());
        typeMessage.setBody(JSON.toJSONString(pongTypeMsg));
        Message msg = new Message(LongIDUtil.getCurrentId());
        msg.setContent(typeMessage);
        return msg;
    }

    public static Message probemsg2Msg(ProbeTypeMsg probeTypeMsg) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_PROBE.ordinal());
        typeMessage.setBody(JSON.toJSONString(probeTypeMsg));
        Message msg = new Message(LongIDUtil.getCurrentId());
        msg.setContent(typeMessage);
        return msg;
    }

    public static Message throughmsg2Msg(ThroughTypeMsg tmsg) {
        TypeMessage typeMessage = new TypeMessage();
        typeMessage.setType(TypeMessage.Type.MSG_TYPE_THROUGH.ordinal());
        typeMessage.setBody(JSON.toJSONString(tmsg));
        Message msg = new Message(LongIDUtil.getCurrentId());
        msg.setContent(typeMessage);
        return msg;
    }


    /**
     * ----------------------------------------------生成探测ProbeTypeMsg部分----------------------------------------------------
     */
    public static ProbeTypeMsg makeClientStepOneProbeTypeMsg(String host, int port) {
        return makeClientStepOneProbeTypeMsg(new InetSocketAddress(host, port));
    }

    public static ProbeTypeMsg makeClientStepOneProbeTypeMsg(InetSocketAddress inetSocketAddress) {
        ProbeTypeMsg probeTypeMsg = new ProbeTypeMsg(TypeMessage.Type.MSG_TYPE_PROBE.ordinal(), inetSocketAddress);
        probeTypeMsg.setType(ProbeTypeMsg.Type.FROM_CLIENT.ordinal());
        probeTypeMsg.setStep(ByteUtil.int2byteArr(ProbeTypeMsg.Step.ONE.ordinal())[3]);
        return probeTypeMsg;
    }

    public static ProbeTypeMsg makeClientStepTwoProbeTypeMsg(String host, int port) {
        return makeClientStepTwoProbeTypeMsg(new InetSocketAddress(host, port));
    }

    public static ProbeTypeMsg makeClientStepTwoProbeTypeMsg(InetSocketAddress inetSocketAddress) {
        ProbeTypeMsg probeTypeMsg = new ProbeTypeMsg(TypeMessage.Type.MSG_TYPE_PROBE.ordinal(), inetSocketAddress);
        probeTypeMsg.setType(ProbeTypeMsg.Type.FROM_CLIENT.ordinal());
        probeTypeMsg.setStep(ByteUtil.int2byteArr(ProbeTypeMsg.Step.TWO.ordinal())[3]);
        return probeTypeMsg;
    }

    /**
     * ---------------------------测试
     */
    public static ByteBuf makeTestBytebuf(String content) {
        byte[] bytes = content.getBytes(CharsetUtil.UTF_8);
        ByteBuf msgBuf = Unpooled.directBuffer(bytes.length);
        msgBuf.writeBytes(bytes);
        return msgBuf;
    }

    public static Message makeTestStr2Msg(String content) {
        TxtTypeMsg ttmsg = new TxtTypeMsg();
        ttmsg.setContent(content);
        return txtmsg2Msg(ttmsg);
    }

    public static String bytebuf2Str(ByteBuf buf) {
        return ByteBufUtil.hexDump(buf);
    }

    /***
     * ----->使用新的Chunk之后,不保存ByteBuf类当数据,使用byte[]当数据
     */
    public static byte[] msg2Bytes(Message msg) {
        int length = msg.getLength() + Message.VERSIONLENGTH + Message.CONTENTLENGTH + Message.ID_LEN;
        byte[] result = new byte[length];

        byte[] version = new byte[Message.VERSIONLENGTH];
        version[0] = msg.getVersion();
        System.arraycopy(version, 0, result, 0, version.length);

        byte[] msgid = ByteUtil.long2ByteArr(msg.getMsgid());
        System.arraycopy(msgid, 0, result, version.length, msgid.length);

        byte[] len = ByteUtil.int2byteArr(msg.getLength());
        System.arraycopy(len, 0, result, version.length + msgid.length, len.length);

        byte[] bytes = msg.getContent();
        System.arraycopy(bytes, 0, result, version.length + msgid.length + len.length, bytes.length);
        return result;
    }

    public static Message bytes2Msg(byte[] result) {
        byte[] versionArr = new byte[Message.VERSIONLENGTH];
        System.arraycopy(result, 0, versionArr, 0, versionArr.length);

        byte[] msgidArr = new byte[Message.ID_LEN];
        System.arraycopy(result, Message.VERSIONLENGTH, msgidArr, 0, msgidArr.length);
        long msgid = ByteUtil.bytearr2Long(msgidArr);

        byte[] lengthArr = new byte[Message.CONTENTLENGTH];
        System.arraycopy(result, Message.VERSIONLENGTH + Message.ID_LEN, lengthArr, 0, lengthArr.length);
        int length = ByteUtil.bytearr2Int(lengthArr);

        byte[] bytesArr = new byte[length];
        try {
            System.arraycopy(result, Message.VERSIONLENGTH + Message.ID_LEN + Message.CONTENTLENGTH, bytesArr, 0, bytesArr.length);
        }catch (Exception e){
            LOGGER.info("version:" + versionArr[0] + " msgid:" + msgid + " length:" + length + " result length:" + result.length);
            e.printStackTrace();
        }

        Message msg = new Message(msgid);
        msg.setLength(length);
        msg.setVersion(versionArr[0]);
        msg.setContent(bytesArr);
        return msg;
    }


}