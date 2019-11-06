package ppex.utils.tpool;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class DisruptorSingleExecutor implements IMessageExecutor {

    int ringBufSize = 2 << 15;
    private WaitStrategy strategy = new BlockingWaitStrategy();
    private Disruptor<DisruptorHandler> disruptor = null;
    private RingBuffer<DisruptorHandler> buffer = null;
    private DisruptorEventFactory eventFactory = new DisruptorEventFactory();
    private static final DisruptorEventHandler handler = new DisruptorEventHandler();
    private DisruptorThread currentThread;

    private AtomicBoolean isStop = new AtomicBoolean();
    private String threadname;

    public DisruptorSingleExecutor(String threadname) {
        this.threadname = threadname;
    }

    @Override
    public void start() {
        LoopThreadfactory loopThreadfactory = new LoopThreadfactory(this);
        disruptor = new Disruptor<>(eventFactory,ringBufSize,loopThreadfactory);
        buffer = disruptor.getRingBuffer();
        disruptor.handleEventsWith(DisruptorSingleExecutor.handler);
        disruptor.start();
    }

    @Override
    public void stop() {
        if (isStop.get())
            return;
        disruptor.shutdown();
        isStop.set(true);
    }

    @Override
    public void execute(ITask task) {
        Thread thread = Thread.currentThread();
        if (thread == this.currentThread){
            task.execute();
            return;
        }
        long next = buffer.next();
        DisruptorHandler event = buffer.get(next);
        event.setTask(task);
        buffer.publish(next);
    }

    //主线程工厂
    private class LoopThreadfactory implements ThreadFactory{
        IMessageExecutor messageExecutor;

        public LoopThreadfactory(IMessageExecutor messageExecutor) {
            this.messageExecutor = messageExecutor;
        }

        @Override
        public Thread newThread(Runnable r) {
            currentThread = new DisruptorThread(r,messageExecutor);
            currentThread.setName(threadname);
            return currentThread;
        }
    }

//    public static void main(String[] args){
//        //test
//        DisruptorSingleExecutor disruptorSingleExecutor = new DisruptorSingleExecutor("aa");
//        disruptorSingleExecutor.start();
////        disruptorSingleExecutor.execute(()-> System.out.println("hahahahahahah"));
//        Stream.of(1,2,3,4,5,6,7).forEach(val->{
//            disruptorSingleExecutor.execute(()-> System.out.println("val:"+val));
//        });
//        try {
//            TimeUnit.SECONDS.sleep(1);
//        }catch (InterruptedException e){
//            e.printStackTrace();
//        }
//    }
}
