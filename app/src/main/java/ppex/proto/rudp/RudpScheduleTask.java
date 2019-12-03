package ppex.proto.rudp;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import ppex.proto.msg.entity.Connection;
import ppex.utils.tpool.DisruptorExectorPool;
import ppex.utils.tpool.IMessageExecutor;
import ppex.utils.tpool.ITask;

public class RudpScheduleTask implements ITask,Runnable, TimerTask {
    private IMessageExecutor executor;
    private RudpPack rudpPack;
    private IAddrManager addrManager;

    public RudpScheduleTask(IMessageExecutor executor, RudpPack rudpPack, IAddrManager addrManager) {
        this.executor = executor;
        this.rudpPack = rudpPack;
        this.addrManager = addrManager;
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
            long now = System.currentTimeMillis();
            //超时,便是关闭连接
            if (now - rudpPack.getTimeout() > rudpPack.getLasRcvTime()){
                rudpPack.close();
            }
            if (!rudpPack.isActive()){
                Connection connection = rudpPack.getConnection();
                connection.getChannel().close();
//                connection.getChannel().eventLoop().execute(()-> addrManager.Del(connection.getAddress()));
                rudpPack.release();
                this.addrManager.Del(rudpPack);
                return;
            }
            //            long next = pcpPack.flush(now);
//            //这个Next时间要看后面得到的时间长短来确定
//            DisruptorExectorPool.scheduleHashedWheel(this, next);
//            if (!pcpPack.getSndList().isEmpty() && pcpPack.canSend(false)) {
//                pcpPack.notifyWriteEvent();
//            }
            long next = rudpPack.flush(now);
            DisruptorExectorPool.scheduleHashedWheel(this,next);
            if (!rudpPack.getQueue_snd().isEmpty() && rudpPack.canSend(false)){
                rudpPack.notifySendEvent();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
