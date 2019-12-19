package ppex.proto.tpool;

import io.netty.util.TimerTask;

public interface ITask extends Runnable, TimerTask {
    void execute();
}
