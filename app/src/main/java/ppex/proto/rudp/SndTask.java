package ppex.proto.rudp;

import io.netty.util.Timeout;
import ppex.proto.msg.Message;
import ppex.proto.tpool.ITask;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SndTask implements ITask {

    private RudpPack rudpkg;

    public static SndTask New(RudpPack rudpkg) {
        SndTask sendTask = new SndTask();
        sendTask.rudpkg = rudpkg;
        return sendTask;
    }

    @Override
    public void execute() {
        try {
            ConcurrentLinkedQueue<Message> msgs = rudpkg.getQueue_snd();
            while(rudpkg.canSend(false)){
                Message msg = msgs.poll();
                if (msg == null)
                    break;
                this.rudpkg.send(msg);
            }
            long cur = System.currentTimeMillis();
            this.rudpkg.flush(cur,false);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            release();
        }
    }

    public void release() {
        rudpkg = null;
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        run();
    }

    @Override
    public void run() {
        execute();
    }
}
