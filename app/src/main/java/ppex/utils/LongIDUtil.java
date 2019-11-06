package ppex.utils;

import java.util.concurrent.atomic.LongAdder;

public class LongIDUtil {
    private static final LongAdder ladder = new LongAdder();
    public static long getCurrentId(){
        ladder.increment();
        return ladder.longValue();
    }
}
