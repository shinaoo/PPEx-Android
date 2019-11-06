package ppex.proto.rudp;

import io.netty.buffer.ByteBuf;
import org.apache.log4j.Logger;
import org.jctools.queues.MpscArrayQueue;
import org.jctools.queues.SpscArrayQueue;
import ppex.proto.msg.Message;
import ppex.proto.msg.entity.Connection;
import ppex.utils.tpool.IMessageExecutor;

import java.util.Queue;

public class RudpPack {

    private static Logger LOGGER = Logger.getLogger(RudpPack.class);

    private final MpscArrayQueue<Message> queue_snd;
    private final Queue<ByteBuf> queue_rcv;

    private Rudp rudp;
    private Output output;
    private Connection connection;
    private IMessageExecutor iMessageExecutor;
    private ResponseListener listener;

    private boolean isActive = true;
    private long lasRcvTime = System.currentTimeMillis(),timeout = 30 * 1000;

    public RudpPack(Output output, Connection connection, IMessageExecutor iMessageExecutor,ResponseListener listener) {
        this.output = output;
        this.connection = connection;
        this.iMessageExecutor = iMessageExecutor;
        this.queue_snd = new MpscArrayQueue<>(2 << 11);
        this.queue_rcv = new SpscArrayQueue<>(2 << 11);
        this.rudp = new Rudp(output, connection);
        this.listener = listener;
    }

    public boolean write(Message msg){
        if (!queue_snd.offer(msg)){
            LOGGER.info("rudppkg queue snd is full");
            return false;
        }
        notifySendEvent();
        return true;
    }

    public boolean send(Message msg) {
        return this.rudp.send(msg) == 0;
    }

    public void input(ByteBuf data, long time) {
        this.lasRcvTime = System.currentTimeMillis();
        this.rudp.input(data, time);

    }

    public void read(ByteBuf buf){
        this.queue_rcv.add(buf.readRetainedSlice(buf.readableBytes()));
        notifyRcvEvent();
    }

    public void notifySendEvent() {
        SndTask task = SndTask.New(this);
        this.iMessageExecutor.execute(task);
    }

    public void notifyRcvEvent(){
        RcvTask task = RcvTask.New(this);
        this.iMessageExecutor.execute(task);
    }

    //暂时返回true
    public boolean canSend(boolean current) {
        int max = rudp.getWndSnd() * 2;
        int waitsnd = rudp.waitSnd();
        if (current){
            return waitsnd < max;
        }else{
            int threshold = Math.max(1,max/2);
            return waitsnd < threshold;
        }
    }

    public MpscArrayQueue<Message> getQueue_snd() {
        return queue_snd;
    }

    public long flush(long current) {
        return rudp.flush(false, current);
    }

    public Queue<ByteBuf> getQueue_rcv() {
        return queue_rcv;
    }

    public int getInterval() {
        return rudp.getInterval();
    }

    public boolean canRcv(){
        return rudp.canRcv();
    }

    public ResponseListener getListener() {
        return listener;
    }

    public Message mergeRcv(){
        return rudp.mergeRcvData();
    }

    public void close(){
        this.isActive = false;
    }

    public boolean isActive() {
        return isActive;
    }

    public long getLasRcvTime() {
        return lasRcvTime;
    }

    public long getTimeout() {
        return timeout;
    }

    public Connection getConnection(){
        return rudp.getConnection();
    }

    public void release(){
        rudp.release();
        queue_rcv.forEach(buf-> buf.release());
    }

}
