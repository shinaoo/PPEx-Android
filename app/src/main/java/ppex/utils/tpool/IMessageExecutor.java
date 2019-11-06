package ppex.utils.tpool;

public interface IMessageExecutor {
    void start();
    void stop();
    void execute(ITask task);
}
