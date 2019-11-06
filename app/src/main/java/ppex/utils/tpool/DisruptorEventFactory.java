package ppex.utils.tpool;

import com.lmax.disruptor.EventFactory;

public class DisruptorEventFactory implements EventFactory<DisruptorHandler> {
    @Override
    public DisruptorHandler newInstance() {
        return new DisruptorHandler();
    }
}
