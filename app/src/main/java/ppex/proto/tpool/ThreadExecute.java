package ppex.proto.tpool;

import io.netty.util.HashedWheelTimer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadExecute implements IThreadExecute {

    private DefaultThreadFactory defaultThreadFactory;
    private ExecutorService executorService;

    private HashedWheelTimer hashedWheelTimer;

    @Override
    public void start() {
        defaultThreadFactory = new DefaultThreadFactory();
        executorService = Executors.newCachedThreadPool(defaultThreadFactory);
        hashedWheelTimer = new HashedWheelTimer(defaultThreadFactory,1, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        executorService.shutdown();
    }

    @Override
    public void execute(ITask task) {
        executorService.submit(task);
    }

    @Override
    public void executeTimerTask(ITask task,long millseconds) {
        hashedWheelTimer.newTimeout(task,millseconds,TimeUnit.MILLISECONDS);
    }
}
