package ppex.proto.rudp;

import io.netty.buffer.ByteBuf;
import io.netty.util.Timeout;
import ppex.proto.msg.Message;
import ppex.proto.tpool.ITask;

import java.util.Queue;

public class RcvTask implements ITask {

    private RudpPack rudpkg;

    public static RcvTask New(RudpPack rudpkg) {
        RcvTask rcvTask = new RcvTask();
        rcvTask.rudpkg = rudpkg;
        return rcvTask;
    }

    @Override
    public void execute() {
        try {
            long current = System.currentTimeMillis();
            Queue<ByteBuf> queue_rcv = rudpkg.getQueue_rcv();
            boolean hasByteBuf = false;
            for (; ; ) {
                ByteBuf byteBuf = queue_rcv.poll();
                if (byteBuf == null)
                    break;
                rudpkg.input(byteBuf, current);
                byteBuf.release();
                hasByteBuf = true;
            }
            if (!hasByteBuf)
                return;
//            rudpkg.printRcvShambleAndOrderNum();
            while (rudpkg.canRcv()) {
                Message msg = rudpkg.mergeRcv();
                if (msg == null)
                    break;
                if (rudpkg.getListener() == null)
                    break;
                rudpkg.getListener().onResponse(rudpkg, msg);
            }
            if (!rudpkg.getQueue_snd().isEmpty() && rudpkg.canSend(false)) {
                rudpkg.notifySendEvent();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            release();
        }
    }

    private void release() {
        rudpkg = null;
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        execute();
    }

    @Override
    public void run() {
        execute();
    }
}
