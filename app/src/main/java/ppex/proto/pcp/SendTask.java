package ppex.proto.pcp;

import io.netty.buffer.ByteBuf;
import io.netty.util.Recycler;
import org.jctools.queues.MpscArrayQueue;
import ppex.utils.tpool.ITask;

import java.io.IOException;

public class SendTask implements ITask {

    private final Recycler.Handle<SendTask> recyclerHandler;
    private static final Recycler<SendTask> RECYCLER = new Recycler<SendTask>() {
        @Override
        protected SendTask newObject(Handle<SendTask> handle) {
            return new SendTask(handle);
        }
    };

    private SendTask(Recycler.Handle<SendTask> recyclerHandler) {
        this.recyclerHandler = recyclerHandler;
    }

    private PcpPack pcpPack;

    public static SendTask New(PcpPack pcpPack) {
        SendTask sendTask = RECYCLER.get();
        sendTask.pcpPack = pcpPack;
        return sendTask;
    }

    @Override
    public void execute() {
        try {

            MpscArrayQueue<ByteBuf> queue = pcpPack.getSndList();
            while (pcpPack.canSend(false)) {
                ByteBuf byteBuf = queue.poll();
                if (byteBuf == null)
                    break;
                try {
                    this.pcpPack.send(byteBuf);
                    byteBuf.release();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            if (!pcpPack.canSend(false)){   //当发现等待ack的队列数量比窗口的数量还多.执行发送动作
                long now = System.currentTimeMillis();
                long next = pcpPack.flush(now);
                pcpPack.setTsUpdate(now + next);
            }

        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            release();
        }
    }

    public void release() {
        pcpPack = null;
        recyclerHandler.recycle(this);
    }
}
