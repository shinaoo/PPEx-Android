package ppex.proto.pcp;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import ppex.utils.tpool.DisruptorExectorPool;
import ppex.utils.tpool.IMessageExecutor;
import ppex.utils.tpool.ITask;

public class ScheduleTask implements ITask, Runnable, TimerTask {
    private IMessageExecutor executor;
    private PcpPack pcpPack;
    private IChannelManager channelManager;

    public ScheduleTask(IMessageExecutor executor, PcpPack pcpPack, IChannelManager channelManager) {
        this.executor = executor;
        this.pcpPack = pcpPack;
        this.channelManager = channelManager;
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        run();
    }

    @Override
    public void run() {
        this.executor.execute(this);
    }

    @Override
    public void execute() {
        try {
            //暂时没有判断是否存活.
            long now = System.currentTimeMillis();
            long next = pcpPack.flush(now);
            //这个Next时间要看后面得到的时间长短来确定
            DisruptorExectorPool.scheduleHashedWheel(this, next);
            if (!pcpPack.getSndList().isEmpty() && pcpPack.canSend(false)) {
                pcpPack.notifyWriteEvent();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
