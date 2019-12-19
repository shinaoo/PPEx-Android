package ppex.proto.tpool;

public interface IThreadExecute {
    void start();
    void stop();
    void execute(ITask task);
    void executeTimerTask(ITask task,long millseconds);
}
