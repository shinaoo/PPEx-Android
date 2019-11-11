package ppex.proto.pcp;

import io.netty.buffer.ByteBuf;
import io.netty.util.Recycler;
import ppex.utils.tpool.ITask;

import java.util.Queue;

public class RecvTask implements ITask {

    private final Recycler.Handle<RecvTask> recyclerHandler;
    private static final Recycler<RecvTask> RECYCLER = new Recycler<RecvTask>() {
        @Override
        protected RecvTask newObject(Handle<RecvTask> handle) {
            return new RecvTask(handle);
        }
    };

    private RecvTask(Recycler.Handle<RecvTask> recyclerHandler) {
        this.recyclerHandler = recyclerHandler;
    }

    private PcpPack pcpPack;

    public static RecvTask New(PcpPack pcpPack) {
        RecvTask recvTask = RECYCLER.get();
        recvTask.pcpPack = pcpPack;
        return recvTask;
    }


    @Override
    public void execute() {
        try {
            boolean hasRcvMessage = false;
            long current = System.currentTimeMillis();
            Queue<ByteBuf> rcvList = pcpPack.getRcvList();
            for(;;){
                ByteBuf byteBuf = rcvList.poll();
                if (byteBuf == null)
                    break;
                hasRcvMessage = true;
                pcpPack.input(byteBuf,current);
                byteBuf.release();
            }
            if (!hasRcvMessage)
                return;
            while(pcpPack.canRecv()){
                ByteBuf rcvBuf = pcpPack.mergeReceive();
//                pcpPack.getPcpListener().onResponse();
            }
            if (!pcpPack.getSndList().isEmpty() && pcpPack.canSend(false)){
                pcpPack.notifyWriteEvent();
            }

        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            release();
        }
    }

    private void release(){
        pcpPack = null;
        recyclerHandler.recycle(this);
    }
}
