package ppex.proto.rudp2;

import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ppex.proto.Statistic;
import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.ResponseListener;
import ppex.proto.rudp.RudpPack;
import ppex.proto.tpool.ITask;
import ppex.proto.tpool.IThreadExecute;

import java.util.LinkedList;

public class ScheduleTask implements ITask {

    private static Logger LOGGER = LoggerFactory.getLogger(ScheduleTask.class);

    private IThreadExecute executor;
    private RudpPack rudpPack;
    private IAddrManager addrManager;

    public ScheduleTask(IThreadExecute executor, RudpPack rudpPack, IAddrManager addrManager) {
        this.executor = executor;
        this.rudpPack = rudpPack;
        this.addrManager = addrManager;
    }

    @Override
    public void execute() {
        try {
            long now = System.currentTimeMillis();
            if (now - rudpPack.getTimeout() > rudpPack.getLasRcvTime()) {
                rudpPack.close();
            }
            if (!rudpPack.isActive()) {
                createTailTask(rudpPack.getRcvOrder(),rudpPack.getRcvShambles(),rudpPack.getRcvNxt2(),rudpPack.getListener());
                rudpPack.release();
                addrManager.Del(rudpPack);
                rudpPack = null;
                LOGGER.info("rudp is not active");
                return;
            }
            if (rudpPack.isStop2()) {
                createTailTask(rudpPack.getRcvOrder(),rudpPack.getRcvShambles(),rudpPack.getRcvNxt2(),rudpPack.getListener());
                rudpPack.release();
                addrManager.Del(rudpPack);
                rudpPack = null;
                LOGGER.info("rudp is stop");
                return;
            }
            long nxt = rudpPack.flush2(now);
            executor.executeTimerTask(this, nxt);
            if (!rudpPack.getQueue_snd().isEmpty()) {
                rudpPack.notifySndTask2();
            }
            if (rudpPack.getRcvOrder().size() != 0 || rudpPack.getRcvShambles().size() != 0) {
                rudpPack.notifyRcvTask2();
            }
            System.out.printf("snd:%d,sndAck:%d,output:%d,rcv:%d,rcvOrder:%d,rcvAck:%d,response:%d,lostChunk:%d,order:%d,shambles:%d\n",
                    Statistic.sndCount.get(), Statistic.sndAckCount.get(), Statistic.outputCount.get(), Statistic.rcvCount.get(), Statistic.rcvOrderCount.get(), Statistic.rcvAckCount.get(), Statistic.responseCount.get(), Statistic.lostChunkCount.get(),rudpPack.getRcvOrder().size(),rudpPack.getRcvShambles().size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        run();
    }

    @Override
    public void run() {
        execute();
    }

    //create TailTask
    private void createTailTask(LinkedList<Chunk> order, LinkedList<Chunk> shambles, int rcvNxt, ResponseListener listener){
        TailTask tt = TailTask.newTailTask(order,shambles,rcvNxt,listener);
        this.executor.execute(tt);
    }
}
