package ppex.proto.pcp;

import io.netty.util.Recycler;
import ppex.utils.tpool.ITask;

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

    }
}
