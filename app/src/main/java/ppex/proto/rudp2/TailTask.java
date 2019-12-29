package ppex.proto.rudp2;

import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ppex.proto.Statistic;
import ppex.proto.msg.Message;
import ppex.proto.rudp.ResponseListener;
import ppex.proto.tpool.ITask;
import ppex.utils.MessageUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * 当服务端认为断开连接了.但是实际上还有消息未处理
 */
public class TailTask implements ITask {

    private static Logger LOGGER = LoggerFactory.getLogger(TailTask.class);
    private LinkedList<Chunk> order = new LinkedList<>();
    private LinkedList<Chunk> shambles = new LinkedList<>();
    private int rcvNxt = 0;
    private ResponseListener listener = null;

    private TailTask() {
    }

    public static TailTask newTailTask(LinkedList<Chunk> order, LinkedList<Chunk> shambles, int rcvNxt, ResponseListener listener) {
        TailTask tt = new TailTask();
        tt.order.addAll(order);
        tt.shambles.addAll(shambles);
        tt.rcvNxt = rcvNxt;
        tt.listener = listener;
        return tt;
    }

    @Override
    public void execute() {
        LOGGER.info("TailTask execute. order size:" + order.size() + " shamble:" + shambles.size());
        while (!shambles.isEmpty()) {
            for (Iterator<Chunk> itr = shambles.iterator(); itr.hasNext(); ) {
                Chunk chunk = itr.next();
                if (chunk.sn == rcvNxt) {
                    order.add(chunk);
                    itr.remove();
                    rcvNxt++;
                } else if (chunk.sn < rcvNxt) {
                    itr.remove();
                }
            }
        }
        LOGGER.info("TailTask execute order size:" + order.size() + " shambles:" + shambles.size());
        while (!order.isEmpty()) {
            LinkedList<Chunk> chunks = new LinkedList<>();
            Chunk target = order.getFirst();
            for (Iterator<Chunk> itr = order.iterator(); itr.hasNext(); ) {
                Chunk chunk = itr.next();
                if (chunk.msgid == target.msgid) {
                    chunks.addLast(chunk);
                    itr.remove();
                }
            }
            if (!chunks.isEmpty()) {
                if (target.all != chunks.size()) {
                    order.addAll(0, chunks);
                    chunks.clear();
                }
            }
            Message msg = null;
            if (!chunks.isEmpty()) {
                int length = chunks.stream().mapToInt(chunk -> chunk.length).sum();
                byte[] result = new byte[length];
                Collections.sort(chunks, Comparator.comparingInt(o -> o.tot));
                for (int i = 0; i < chunks.size(); i++) {
                    System.arraycopy(chunks.get(i).data, 0, result, RudpParam.MTU_BODY * i, chunks.get(i).data.length);
                }
                msg = MessageUtil.bytes2Msg(result);
            }
            //todo 处理null的rudppack需要判断
            listener.onResponse(null, msg);
        }
        System.out.printf("TailTask snd:%d,sndAck:%d,output:%d,rcv:%d,rcvOrder:%d,rcvAck:%d,response:%d,order:%d,shambles:%d\n",
                Statistic.sndCount.get(), Statistic.sndAckCount.get(), Statistic.outputCount.get(), Statistic.rcvCount.get(), Statistic.rcvOrderCount.get(), Statistic.rcvAckCount.get(), Statistic.responseCount.get(), order.size(), shambles.size());

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
