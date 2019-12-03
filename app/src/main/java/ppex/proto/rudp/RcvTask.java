package ppex.proto.rudp;

import java.util.Queue;

import io.netty.buffer.ByteBuf;
import ppex.proto.msg.Message;
import ppex.utils.tpool.ITask;

public class RcvTask implements ITask {


    private RudpPack rudpkg;

    public static RcvTask New(RudpPack rudpkg) {
        RcvTask rcvTask = new RcvTask();
        rcvTask.rudpkg = rudpkg;
        return rcvTask;
    }

    int count = 0;

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
                rudpkg.getListener().onResponse(rudpkg.getCtx(), rudpkg, msg);
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
}
