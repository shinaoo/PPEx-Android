package ppex.proto.msg;

import com.alibaba.fastjson.JSON;
import io.netty.util.CharsetUtil;
import ppex.proto.msg.type.TypeMessage;

/**
 * -----16bits-----+-----16bits-----+-----32bits------+-----content-----+
 * --    0x01    --+--  msg type  --+--contentlength--+--   content   --+
 *-----------------+----------------+-----------------+-----------------+
 *
 * 2019-9-25 修改,将msg type放入content,解析content再获取type类型
 * -----8bits-----+-----32bits------+-----content-----+
 * --    0x01   --+--contentlength--+--   content   --+
 *----------------+-----------------+-----------------+
 *
 * 2019-10-9.加入msg id 64位和 current 64位和total 64位(未实现.todo)
 * +-----64bits---+-----64bits----+----64bits-------+------32bits-------+-----content-----+
 * +    msg id    +     current   +     total       +-- contentlength --+--   content   --+
 * +--------------+---------------+-----------------+-------------------+-----------------+
 *
 * 2019-12-23
 * -----8bits-----+-----64bit------+-----32bit------+---content---+
 * --    0x01   --+--   msg id   --+--   length   --+   content   +
 *----------------+----------------+----------------+-------------+
 *
 */
public class Message {
    public static final int VERSIONLENGTH = 1;
    public static final int ID_LEN = 8;
    public static final int CONTENTLENGTH = 4;

    private byte version;
    private long msgid;
    private int length;
//    private String content;
    private byte[] content;

    public Message(long msgid) {
        this.msgid = msgid;
        this.version = 0x1;
    }

    public byte getVersion() {
        return version;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public int getLength() {
        return length;
    }

    public long getMsgid() {
        return msgid;
    }

    public void setMsgid(long msgid) {
        this.msgid = msgid;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content){
        this.content = content;
        this.length = this.content.length;
    }

    public void setContent(String content) {
        this.content = content.getBytes(CharsetUtil.UTF_8);
        this.length = this.content.length;
    }

    public void setContent(TypeMessage typeMessage){
        this.content = JSON.toJSONString(typeMessage).getBytes(CharsetUtil.UTF_8);
        this.length = this.content.length;
    }

    @Override
    public String toString() {
        return "Message{" +
                "version=" + Byte.toUnsignedInt(version) +
                ", length=" + length +
                ", content='" + content + '\'' +
                '}';
    }


}
