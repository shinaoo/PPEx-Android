package ppex.proto.rudp;

import io.netty.util.Recycler;
import org.apache.log4j.Logger;
import org.jctools.queues.MpscArrayQueue;
import ppex.proto.msg.Message;
import ppex.utils.tpool.ITask;

public class SndTask implements ITask {
    private static Logger LOGGER = Logger.getLogger(SndTask.class);

    private final Recycler.Handle<SndTask> recyclerHandler;
    private static final Recycler<SndTask> RECYCLER = new Recycler<SndTask>() {
        @Override
        protected SndTask newObject(Handle<SndTask> handle) {
            return new SndTask(handle);
        }
    };

    private SndTask(Recycler.Handle<SndTask> recyclerHandler) {
        this.recyclerHandler = recyclerHandler;
    }

    private RudpPack rudpkg;

    public static SndTask New(RudpPack rudpkg) {
        SndTask sendTask = RECYCLER.get();
        sendTask.rudpkg = rudpkg;
        return sendTask;
    }

    @Override
    public void execute() {
        try {
            MpscArrayQueue<Message> msgs = rudpkg.getQueue_snd();
            while(rudpkg.canSend(false)){
                Message msg = msgs.poll();
                if (msg == null)
                    break;
                this.rudpkg.send(msg);
            }
            long cur = System.currentTimeMillis();
            this.rudpkg.flush(cur);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            release();
        }
    }

    public void release() {
        rudpkg = null;
        recyclerHandler.recycle(this);
    }
}
