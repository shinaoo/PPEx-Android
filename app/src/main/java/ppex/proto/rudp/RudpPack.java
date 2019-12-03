package ppex.proto.rudp;

import android.util.Log;

import org.jctools.queues.MpscArrayQueue;

import java.util.Queue;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import ppex.proto.msg.Message;
import ppex.proto.msg.entity.Connection;
import ppex.utils.tpool.IMessageExecutor;

public class RudpPack {

    private final MpscArrayQueue<Message> queue_snd;
    private final Queue<ByteBuf> queue_rcv;

    private Rudp rudp;
    private Output output;
    private Connection connection;
    private IMessageExecutor iMessageExecutor;
    private ResponseListener listener;
    private ChannelHandlerContext ctx;

    private boolean isActive = true;
    private long lasRcvTime = System.currentTimeMillis(),timeout = 30 * 1000;

    public RudpPack(Output output, Connection connection, IMessageExecutor iMessageExecutor,ResponseListener listener,ChannelHandlerContext ctx) {
        this.output = output;
        this.connection = connection;
        this.iMessageExecutor = iMessageExecutor;
        this.queue_snd = new MpscArrayQueue<>(2 << 11);
        this.queue_rcv = new MpscArrayQueue<>(2 << 11);
        this.rudp = new Rudp(output, connection);
        this.listener = listener;
        this.ctx = ctx;
    }

    public boolean write(Message msg){
        if (!queue_snd.offer(msg)){
            Log.e("MyTag","rudppkg queue snd is full");
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
        this.queue_rcv.add(buf.readSlice(buf.readableBytes()));
        notifyRcvEvent();
    }

    public void sendReset(){
        rudp.sendReset();
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
//        queue_rcv.forEach(buf-> buf.release());
        for (int i = 0;i < queue_rcv.size();i++){
            ByteBuf buf = queue_rcv.poll();
            buf.release();
        }
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public void printRcvShambleAndOrderNum(){
//        LOGGER.info("Rudppack shamble:" + rudp.getQueue_rcv_shambles().size() +  " order:" + rudp.getQueue_rcv_order().size());
//        if (rudp.getQueue_rcv_shambles().size() > 0){
//            rudp.getQueue_rcv_shambles().forEach(frg -> LOGGER.info("frg:" + frg.msgid + " sn:" + frg.sn + " tot:" + frg.tot));
//        }
    }
}
