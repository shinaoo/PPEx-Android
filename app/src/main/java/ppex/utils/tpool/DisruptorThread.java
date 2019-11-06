package ppex.utils.tpool;

public class DisruptorThread extends Thread {
    private IMessageExecutor messageExecutor;

    public DisruptorThread(IMessageExecutor messageExecutor) {
        this.messageExecutor = messageExecutor;
    }

    public DisruptorThread(Runnable target, IMessageExecutor messageExecutor) {
        super(target);
        this.messageExecutor = messageExecutor;
    }
}
