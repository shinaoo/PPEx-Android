package ppex.proto;

import java.util.concurrent.atomic.AtomicLong;

public class Statistic {
    public static volatile AtomicLong sndCount = new AtomicLong(0);
    public static volatile AtomicLong sndAckCount = new AtomicLong(0);
    public static volatile AtomicLong outputCount = new AtomicLong(0);
    public static volatile AtomicLong rcvCount = new AtomicLong(0);
    public static volatile AtomicLong rcvOrderCount = new AtomicLong(0);
    public static volatile AtomicLong rcvAckCount = new AtomicLong(0);
    public static volatile AtomicLong responseCount = new AtomicLong(0);
    public static volatile AtomicLong lostMsgCount = new AtomicLong(0);
    public static volatile AtomicLong lostChunkCount = new AtomicLong(0);

}
