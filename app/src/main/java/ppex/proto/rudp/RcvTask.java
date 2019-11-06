package ppex.proto.rudp;

import io.netty.buffer.ByteBuf;
import io.netty.util.Recycler;
import org.apache.log4j.Logger;
import ppex.proto.msg.Message;
import ppex.utils.tpool.ITask;

import java.util.Queue;

public class RcvTask implements ITask {

    private static Logger LOGGER = Logger.getLogger(RcvTask.class);

    private final Recycler.Handle<RcvTask> recyclerHandler;
    private static final Recycler<RcvTask> RECYCLER = new Recycler<RcvTask>() {
        @Override
        protected RcvTask newObject(Handle<RcvTask> handle) {
            return new RcvTask(handle);
        }
    };

    private RcvTask(Recycler.Handle<RcvTask> recyclerHandler) {
        this.recyclerHandler = recyclerHandler;
    }

    private RudpPack rudpkg;

    public static RcvTask New(RudpPack rudpkg) {
        RcvTask rcvTask = RECYCLER.get();
        rcvTask.rudpkg = rudpkg;
        return rcvTask;
    }

    @Override
    public void execute() {
        try {
            long current = System.currentTimeMillis();
            Queue<ByteBuf> queue_rcv = rudpkg.getQueue_rcv();
            boolean hasByteBuf = false;
            for (;;){
                ByteBuf byteBuf = queue_rcv.poll();
                if (byteBuf == null)
                    break;
                rudpkg.input(byteBuf,current);
                byteBuf.release();
                hasByteBuf = true;
            }
            if (!hasByteBuf)
                return;
            while(rudpkg.canRcv()){
                Message msg = rudpkg.mergeRcv();
                if (msg == null)
                    break;
                if (rudpkg.getListener() == null)
                    break;
                rudpkg.getListener().onResponse(rudpkg,msg);
            }
            if (!rudpkg.getQueue_snd().isEmpty() && rudpkg.canSend(false)){
                rudpkg.notifySendEvent();
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            release();
        }
    }

    private void release(){
        rudpkg = null;
        recyclerHandler.recycle(this);
    }
}
