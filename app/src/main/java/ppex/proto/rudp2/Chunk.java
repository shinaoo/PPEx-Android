package ppex.proto.rudp2;


/**
 * 2019-12-24.之前顺序到达时,依靠sn,una与Rudp的sndNxt,rcvNxt与sndUna配合确定有序到达.
 *           现在实现无序到达的话,sn不变,但是una就变成一个信息的最后编号.例如一个msg分成3个.sn为0开始,那么una就是2
 */
public class Chunk {

    private Chunk() {
        tag = 0;
        xmit = 0;
        cmd = 0;
        tot = 0;
        all = 0;
        ots = 0;
        ts = 0;
        sn = 0;
        una = 0;
        ts_resnd = 0;
        rto = 0;
        fastack = 0;
        length = 0;
    }

    public static Chunk newChunk(byte[] data) {
        Chunk chunk = new Chunk();
        chunk.data = data;
        return chunk;
    }

    public byte[] data;

    public byte tag;
    public byte cmd;
    public long msgid;
    public int tot;
    public int all;
    public long ots;            //origin ts,一开始确定要发送的时间,不会改变
    public long ts;
    public int sn;
    public int una;
    public int length;          //其实是data的长度

    public long ts_resnd;
    public int rto;
    public int fastack;
    public int xmit;

    @Override
    public String toString() {
        return "Chunk{" +
                "cmd=" + cmd +
                ", msgid=" + msgid +
                ", tot=" + tot +
                ", all=" + all +
                ", ts=" + ts +
                ", sn=" + sn +
                ", una=" + una +
                ", length=" + length +
                '}';
    }
}
