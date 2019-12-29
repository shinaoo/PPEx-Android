package ppex.proto.rudp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import ppex.proto.msg.Message;
import ppex.proto.rudp2.Chunk;
import ppex.proto.rudp2.Rudp2;
import ppex.proto.rudp2.ScheduleTask;
import ppex.proto.tpool.IThreadExecute;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RudpPack {

    private final ConcurrentLinkedQueue<Message> sndQueue;
    private final ConcurrentLinkedQueue<ByteBuf> rcvQueue;

    private Rudp rudp;
    private IOutput output;
    private IThreadExecute executor;
    private ResponseListener listener;

    private Rudp2 rudp2;

    private ByteBufAllocator bufAllocator = PooledByteBufAllocator.DEFAULT;

    private boolean isActive = true;
    private long lasRcvTime = System.currentTimeMillis(), timeout = 30 * 1000;

    public RudpPack(IOutput output, IThreadExecute executor, ResponseListener listener) {
        this.output = output;
        this.executor = executor;
        this.sndQueue = new ConcurrentLinkedQueue<>();
        this.rcvQueue = new ConcurrentLinkedQueue<>();
        this.listener = listener;
//        this.rudp = new Rudp(output);
        this.rudp2 = new Rudp2(output);
    }

    public boolean write(Message msg) {
        if (!sndQueue.offer(msg)) {
            return false;
        }
        notifySendEvent("Write");
        return true;
    }

    public boolean send(Message msg) {
        return this.rudp.send(msg) == 0;
    }

    public void input(ByteBuf data, long time) {
        this.lasRcvTime = System.currentTimeMillis();
        this.rudp.input(data, time);

    }

    public void read(ByteBuf buf) {
        ByteBuf buf1 = PooledByteBufAllocator.DEFAULT.buffer(buf.readableBytes());
        buf1.writeBytes(buf);
        this.rcvQueue.add(buf1);
        notifyRcvEvent();
    }

    /**
     * 该方法是Client端使用,Server不用
     */
//    public void sendReset() {
//        rudp.sendReset();
//    }

    public void sendFinish(){
        rudp.sendFinish();
    }

    //测试,添加name
    public void notifySendEvent(String name) {
        SndTask task = SndTask.New(this,name);
        this.executor.execute(task);
    }

    public void notifyRcvEvent() {
        RcvTask task = RcvTask.New(this);
        this.executor.execute(task);
    }

    //暂时返回true
    public boolean canSend(boolean current) {
        int max = rudp.getWndSnd() * 2;
        int waitsnd = rudp.waitSnd();
        if (current) {
            return waitsnd < max;
        } else {
            int threshold = Math.max(1, max / 2);
            return waitsnd < threshold;
        }
    }


    public long flush(long current,boolean ackonly) {
        //暂时用ackonly为true来表示是ScheduleTask调用flush.false为SndTask.
        return rudp.flush(ackonly,current);
//        return rudp.flush(false, current);

    }

    public ConcurrentLinkedQueue<ByteBuf> getQueue_rcv() {
        return rcvQueue;
    }

    public int getInterval() {
        return rudp.getInterval();
    }

    public boolean canRcv() {
        return rudp.canRcv();
    }

    public ResponseListener getListener() {
        return listener;
    }

    public Message mergeRcv() {
        return rudp.mergeRcvData();
    }

    public void close() {
        this.isActive = false;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isStop(){
        return rudp.isStop();
    }

    public boolean isStop2(){
        return rudp2.isStop();
    }

    public long getLasRcvTime() {
        return lasRcvTime;
    }

    public long getTimeout() {
        return timeout;
    }

    public void release() {
//        rudp.release();
        rcvQueue.forEach(buf -> buf.release());
        this.rudp2 = null;
    }

    public ConcurrentLinkedQueue<Message> getQueue_snd() {
        return sndQueue;
    }

    public IOutput getOutput() {
        return output;
    }

    public Rudp getRudp() {
        return rudp;
    }

    public static RudpPack newInstance(IOutput output, IThreadExecute executor, ResponseListener responseListener, IAddrManager addrManager) {
        RudpPack rudpPack = new RudpPack(output,executor,responseListener);
//        RudpScheduleTask scheduleTask = new RudpScheduleTask(executor, rudpPack, addrManager);
//        executor.executeTimerTask(scheduleTask, rudpPack.getInterval());
        ScheduleTask scheduleTask = new ScheduleTask(executor,rudpPack,addrManager);
        executor.executeTimerTask(scheduleTask,rudpPack.getInterval2());
        return rudpPack;
    }

    /**
     * ----------------------------------------------------Rudp2
     */
    public boolean send2(Message msg){
        if (!sndQueue.offer(msg))
            return false;
        notifySndTask2();
        return true;
    }

    public void notifySndTask2() {
        ppex.proto.rudp2.SndTask st = ppex.proto.rudp2.SndTask.New(this, "");
        this.executor.execute(st);
    }

    public void send2Rudp2(Message msg){
        this.rudp2.snd(msg);
    }

    public void mvChkFromSnd2SndAck(){
        this.rudp2.mvChkFromSnd2SndAck();
    }
    public void rcv2(ByteBuf buf){
        ByteBuf bufTmp = bufAllocator.buffer(buf.readableBytes());
        bufTmp.writeBytes(buf);
        this.rcvQueue.add(bufTmp);
        notifyRcvTask2();
    }

    public void input2(ByteBuf buf,long time){
        this.lasRcvTime = System.currentTimeMillis();
        this.rudp2.rcv(buf,time);
    }

    public void arrangeRcvData(){
        this.rudp2.arrangeRcvShambles();
    }

    public void notifyRcvTask2() {
        ppex.proto.rudp2.RcvTask rt = ppex.proto.rudp2.RcvTask.New(this, "");
        this.executor.execute(rt);
    }

    public boolean canSnd2(){
        return this.rudp2.canSndMsg();
    }

    public void sndStartConnecting(){
        this.rudp2.sndStartChunk();
    }
    public int getRcvNxt2(){
        return rudp2.getRcvNxt();
    }

    public long flush2(long time){
        return this.rudp2.flush(time);
    }

    public boolean canRcv2(){
        return this.rudp2.canRcv();
    }

    public Message getMsg2(){
        return this.rudp2.mergeMsg();
    }

    public int getInterval2(){
        return this.rudp2.getInterval();
    }

    public LinkedList<Chunk> getRcvOrder(){
        return rudp2.getRcvOrder();
    }

    public LinkedList<Chunk> getRcvShambles(){
        return rudp2.getRcvShambles();
    }

}
