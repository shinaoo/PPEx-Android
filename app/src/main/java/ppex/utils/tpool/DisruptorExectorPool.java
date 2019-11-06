package ppex.utils.tpool;

import io.netty.channel.DefaultEventLoop;
import io.netty.util.HashedWheelTimer;
import io.netty.util.TimerTask;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DisruptorExectorPool {
    private List<IMessageExecutor> executors = new Vector<>();
    protected AtomicInteger index = new AtomicInteger();
    //定时线程池
    private static final DefaultEventLoop EVENT_EXECUTORS = new DefaultEventLoop();

    private static final HashedWheelTimer hashedWheelTimer = new HashedWheelTimer(new TimerThreadFactory(),1, TimeUnit.MILLISECONDS);


    public IMessageExecutor createDisruptorProcessor(String threadName){
        IMessageExecutor singprocess = new DisruptorSingleExecutor(threadName);
        executors.add(singprocess);
        singprocess.start();
        return singprocess;
    }

    public void stop(){
        executors.forEach(msgexecutor -> msgexecutor.stop());
        if (!EVENT_EXECUTORS.isShuttingDown()){
            EVENT_EXECUTORS.shutdownGracefully();
        }
    }

    public IMessageExecutor getAutoDisruptorProcessor(){
        int index = this.index.incrementAndGet();
        return executors.get(index % executors.size());
    }


    //定时器线程工厂
    private static class TimerThreadFactory implements ThreadFactory{
        private AtomicInteger timeThreadName = new AtomicInteger(0);
        public Thread newThread(Runnable r){
            Thread thread = new Thread(r,"timer thread:" + timeThreadName.addAndGet(1));
            return thread;
        }
    }
    public static ScheduledFuture<?> scheduledWithFixedDelay(Runnable runnable,long millSeconds){
        return EVENT_EXECUTORS.scheduleWithFixedDelay(runnable,millSeconds,millSeconds,TimeUnit.MILLISECONDS);
    }
    public static void scheduleHashedWheel(TimerTask timerTask, long millseconds){
        hashedWheelTimer.newTimeout(timerTask,millseconds,TimeUnit.MILLISECONDS);
    }
}
