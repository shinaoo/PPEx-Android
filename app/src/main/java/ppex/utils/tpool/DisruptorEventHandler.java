package ppex.utils.tpool;

import com.lmax.disruptor.EventHandler;

public class DisruptorEventHandler implements EventHandler<DisruptorHandler> {
    @Override
    public void onEvent(DisruptorHandler disruptorHandler, long l, boolean b) throws Exception {
        disruptorHandler.execute();
    }
}
