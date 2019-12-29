package ppex.proto.rudp2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import ppex.proto.msg.Message;
import ppex.proto.rudp.IOutput;
import ppex.proto.rudp.ResponseListener;
import ppex.proto.tpool.IThreadExecute;

import java.util.concurrent.ConcurrentLinkedQueue;

public class RudpPkg {
    private Rudp2 rudp2;
    private IOutput output;
    private IThreadExecute executor;
    private ResponseListener listener;

    private ConcurrentLinkedQueue<Message> sndList = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<ByteBuf> rcvList = new ConcurrentLinkedQueue<>();

    private ByteBufAllocator bufAllocator = PooledByteBufAllocator.DEFAULT;

    public RudpPkg(IOutput output, IThreadExecute executor, ResponseListener listener) {
        this.output = output;
        this.executor = executor;
        this.listener = listener;
        rudp2 = new Rudp2(this.output);
    }

    public boolean send(Message msg) {
        if (!sndList.offer(msg))
            return false;
        notifySndTask();
        return true;
    }

    public void notifySndTask() {
//        SndTask st = SndTask.New(this, "");
//        this.executor.execute(st);
    }

    public void rcv(ByteBuf buf) {
        ByteBuf bufTmp = bufAllocator.buffer(buf.readableBytes());
        bufTmp.writeBytes(buf);
        this.rcvList.add(bufTmp);
        notifyRcvTask();
    }

    public void input2Rpkg(ByteBuf buf,long time){
        this.rudp2.rcv(buf,time);
    }

    public void notifyRcvTask() {
//        RcvTask rt = RcvTask.New(this, "");
//        this.executor.execute(rt);
    }

    public void flush(long time){
        this.rudp2.flush(time);
    }

    public boolean canRcv(){
        return this.rudp2.canRcv();
    }

    public Message getMsg(){
        return rudp2.mergeMsg();
    }

    public ConcurrentLinkedQueue<Message> getSndList() {
        return sndList;
    }

    public ConcurrentLinkedQueue<ByteBuf> getRcvList() {
        return rcvList;
    }

    public ResponseListener getListener() {
        return listener;
    }
}
