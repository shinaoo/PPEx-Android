package ppex.proto.rudp2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ppex.proto.Statistic;
import ppex.proto.msg.Message;
import ppex.proto.rudp.IOutput;
import ppex.utils.ByteUtil;
import ppex.utils.MessageUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * 2019-12-23.暂不考虑其他重传算法以及RTT,RTO等时间计算.直接简单粗暴发送与接收
 * 2019-12-24.之前考虑的是有序到达,依靠sndNxt,rcvNxt,sndUna来确定有序到达的顺序.但是出现的问题是当一方断开之后,另一方不知道,还在继续发送.导致出现问题.
 * 没有实现滑动窗口与拥塞控制
 */
public class Rudp2 {
    private static Logger LOGGER = LoggerFactory.getLogger(Rudp2.class);
    //发送数据与接收数据的集合
    private volatile LinkedList<Chunk> sndList = new LinkedList<>();
    private volatile LinkedList<Chunk> sndAckList = new LinkedList<>();
    private volatile LinkedList<Chunk> rcvOrder = new LinkedList<>();
    private volatile LinkedList<Chunk> rcvShambles = new LinkedList<>();

    //多线程操作使用同步
    private Object sndLock = new Object();
    private volatile boolean sndWait = false;
    private Object sndAckLock = new Object();
    private volatile boolean sndAckWait = false;
    private Object rcvOrderLock = new Object();
    private volatile boolean rcvOrderWait = false;
    private Object rcvShamebleLock = new Object();
    private volatile boolean rcvShambleWait = false;

    //2019-12-28。只有状态为old的时候发消息。新增start指令。用来握手
    //现在改成这样达成通信协议。只有通过start指令确认后才可以通信。其它都进行丢弃
    public byte tag = RudpParam.TAG_NEW;
    private volatile boolean isConnecting = false;
    private long startChunkTs = 0;

    //数据长度
    private int mtuBody = RudpParam.MTU_BODY;
    //表示段的发送与接收的数值,sndNxt下一次发送的sn号,rcvNxt下一次应该接受的sn号,sndUna未确认的sn号(就是发送了N过过去之后,依然还有最开始的未确认的sn号)
    private volatile int sndNxt, rcvNxt, sndUna;
    //超过发送次数就认为连接断开的值
    private int deadLink = RudpParam.DEAD_LINK;
    //快速重传
    private int fastAck = RudpParam.FASTACK_DEFAULT;
    //rudp是否已经断开连接标志
    private boolean stop = false;
    private int rto = RudpParam.RTO_DEFAULT;
    private int interval = RudpParam.INTERVAL_DEFAULT;
    private int headLen = RudpParam.HEAD_LEN;

    private ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;

    //先暂定32个窗口
    private volatile int wndSnd = RudpParam.WND_SND;
    private volatile int wndRcv = RudpParam.WND_RCV;

    //发送数据公共接口
    private IOutput output;

    public Rudp2(IOutput output) {
        this.output = output;
        sndNxt = 0;
        rcvNxt = 0;
        sndUna = 0;
        tag = RudpParam.TAG_NEW;
//        LOGGER.info("sndLock:" + Integer.toHexString(System.identityHashCode(sndLock)) + " sndackLock:" + Integer.toHexString(System.identityHashCode(sndAckLock)) +
//                " rcvorderLock:" + Integer.toHexString(System.identityHashCode(rcvOrderLock)) + " rcvShamblesLock:" + Integer.toHexString(System.identityHashCode(rcvShamebleLock)));
    }

    public void sndStartChunk() {
        if (this.tag == RudpParam.TAG_NEW) {
            if (!isConnecting) {
                isConnecting = true;
                try {
                    startChunkTs = System.currentTimeMillis();
                    while (this.tag != RudpParam.TAG_OLD) {
                        Chunk chunk = Chunk.newChunk(new byte[0]);
                        chunk.tag = tag;
                        chunk.cmd = RudpParam.CMD_START;
                        chunk.msgid = -1;
                        chunk.tot = 0;
                        chunk.all = 1;
                        chunk.ots = startChunkTs;
                        chunk.ts = startChunkTs;
                        chunk.sn = 0;
                        chunk.una = sndUna;
                        chunk.length = chunk.data.length;
                        ByteBuf buf1 = createOutputByteBuf(chunk);
                        sndChunk(buf1, 0);
                        TimeUnit.SECONDS.sleep(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    isConnecting = false;
                }
            } else {
                return;
            }
        }
    }

    public boolean snd(Message msg) {
        byte[] msgArr = MessageUtil.msg2Bytes(msg);
        byte[][] msgArrs = ByteUtil.splitArr(msgArr, mtuBody);
        synchronized (sndLock) {
            try {
                while (sndWait) {
                    sndLock.wait();
                }
                sndWait = true;
                long now = System.currentTimeMillis();
                for (int i = 0; i < msgArrs.length; i++) {
                    Chunk chunk = Chunk.newChunk(msgArrs[i]);
                    chunk.tag = tag;
                    chunk.msgid = msg.getMsgid();
                    chunk.tot = i;
                    chunk.all = msgArrs.length;
                    chunk.ots = now;
                    chunk.una = sndUna;
                    chunk.length = chunk.data.length;
                    sndList.add(chunk);
                    Statistic.sndCount.incrementAndGet();
                }
            } catch (Exception e) {
            } finally {
                sndWait = false;
                sndLock.notifyAll();
            }

        }
        return true;
    }

    public void mvChkFromSnd2SndAck() {
        LinkedList<Chunk> tmpList = new LinkedList<>();
        synchronized (sndLock) {
            try {
                while (sndWait) {
                    sndLock.wait();
                }
                sndWait = true;
                while (!sndList.isEmpty()) {
                    Chunk chunk = sndList.removeFirst();
                    chunk.cmd = RudpParam.CMD_SND;
                    chunk.sn = sndNxt;
                    sndNxt++;
                    tmpList.add(chunk);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                sndWait = false;
                sndLock.notifyAll();
            }
        }
        synchronized (sndAckLock) {
            try {
                while (sndAckWait) {
                    sndAckLock.wait();
                }
                sndAckWait = true;
                sndAckList.addAll(tmpList);
                Statistic.sndAckCount.getAndAdd(tmpList.size());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                sndAckWait = false;
                sndAckLock.notifyAll();
            }
        }
    }

    public long flush(long timeCur) {
        synchronized (sndAckLock) {
            try {
                while (sndAckWait) {
                    sndAckLock.wait();
                }
                sndAckWait = true;

                sndAckList.forEach(chunk -> {
                    boolean snd = false;
                    if (chunk.xmit == 0) {
                        snd = true;
                        chunk.rto = rto;
                    } else if (chunk.fastack >= fastAck) {
                        snd = true;
                    } else if (timeDiff(chunk.ts_resnd, timeCur) >= 0) {
                        snd = true;
                        chunk.rto += rto;
                    }
                    if (snd) {
                        chunk.xmit++;
                        chunk.ts_resnd = timeCur + chunk.rto;
                        chunk.ts = timeCur;
                        if (chunk.xmit >= deadLink) {
                            stop = true;
                        }
                        ByteBuf buf = createOutputByteBuf(chunk);
                        sndChunk(buf, chunk.sn);
                    }
                });
                return interval;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                sndAckWait = false;
                sndAckLock.notifyAll();
            }
        }
        return interval;
    }

    public void rcv(ByteBuf buf, long time) {
//        LOGGER.info("rudp2 rcv :" + " order size:" + rcvOrder.size() + " sb size:" + rcvShambles.size());
        if (buf == null || buf.readableBytes() < headLen) {
            return;
        }
        while (true) {
            if (buf.readableBytes() <= 0) {
                break;
            }
            byte tag = buf.readByte();
            byte cmd = buf.readByte();
            long msgid = buf.readLong();
            int tot = buf.readInt();
            int all = buf.readInt();
            long ots = buf.readLong();
            long ts = buf.readLong();
            int sn = buf.readInt();
            int sndMax = buf.readInt();
            int length = buf.readInt();
            if (buf.readableBytes() < length) {
                return;
            }
            if (cmd != RudpParam.CMD_SND && cmd != RudpParam.CMD_ACK && cmd != RudpParam.CMD_START && cmd != RudpParam.CMD_START_ACK) {
                return;
            }
            switch (cmd) {
                case RudpParam.CMD_SND:
                    byte[] data = new byte[length];
                    buf.readBytes(data, 0, length);
                    //todo 应该还要处理一种 this.tag == New && tag== Old的情况
                    affirmSnd(tag, msgid, tot, all, ts, sn, sndMax, length, data);
                    break;
                case RudpParam.CMD_ACK:
                    affirmAck(sn, tag);
                    Statistic.rcvAckCount.getAndIncrement();
                    break;
                case RudpParam.CMD_START:
                    rcvStartChunk(tag, ots);
                    break;
                case RudpParam.CMD_START_ACK:
                    affirmStartChunkAck(tag, ots);
                    break;
            }
        }
    }

    private void rcvStartChunk(byte tag, long ots) {
        //收到startChunk都是New的一方发送过来的。需要清理这边的所有数据
//        LOGGER.info("rcv start chunk:" + tag + " ts:" +ots);
        if (tag == RudpParam.TAG_NEW) {
            if (this.tag == RudpParam.TAG_NEW) {
                this.tag = RudpParam.TAG_OLD;
                this.startChunkTs = ots;
            } else if (this.tag == RudpParam.TAG_OLD && this.startChunkTs != ots) {          //利用这个rcvTagTs判断是否由TAG_NEW转成OLD之后再次接收到的startchunk
                sndNxt = 0;
                sndUna = 0;
                rcvNxt = 0;
                sndList.clear();
                sndWait = false;
                sndAckList.clear();
                sndAckWait = false;
                rcvShambles.removeIf(chunk -> chunk.ts < ots);
                rcvShambleWait = false;
                rcvOrder.removeIf(chunk -> chunk.ts < ots);
                rcvOrderWait = false;
            }
            Chunk chunk = Chunk.newChunk(new byte[0]);
            chunk.tag = this.tag;
            chunk.cmd = RudpParam.CMD_START_ACK;
            chunk.msgid = -1;
            chunk.tot = 0;
            chunk.all = 1;
            chunk.ots = ots;
            chunk.ts = ots;
            chunk.sn = 0;
            chunk.una = sndUna;
            chunk.length = chunk.data.length;
            chunk.xmit = 0;
//            sndAckList.addLast(chunk);
            ByteBuf buf = createOutputByteBuf(chunk);
            sndChunk(buf,chunk.sn);
//            LOGGER.info("snd back chunk");
        }
    }

    private void affirmStartChunkAck(byte tag, long ots) {
        if (this.tag == RudpParam.TAG_NEW && tag == RudpParam.TAG_OLD && this.startChunkTs == ots) {
            this.tag = RudpParam.TAG_OLD;
            isConnecting = false;
        }
    }


    private int timeDiff(long before, long after) {
        return (int) (after - before);
    }

    private ByteBuf createOutputByteBuf(Chunk chunk) {
        int length = headLen + chunk.length;
        ByteBuf buf = byteBufAllocator.ioBuffer(length);
        encodeBytebuf(buf, chunk);
        return buf;
    }

    private void encodeBytebuf(ByteBuf buf, Chunk chunk) {
        buf.writeByte(chunk.tag);
        buf.writeByte(chunk.cmd);
        buf.writeLong(chunk.msgid);
        buf.writeInt(chunk.tot);
        buf.writeInt(chunk.all);
        buf.writeLong(chunk.ots);
        buf.writeLong(chunk.ts);
        buf.writeInt(chunk.sn);
        buf.writeInt(chunk.una);
        buf.writeInt(chunk.length);
        buf.writeBytes(chunk.data);
    }

    private void sndChunk(ByteBuf buf, long sn) {
        if (output != null) {
            output.output(buf, this, sn);
            Statistic.outputCount.getAndIncrement();
        } else {
            System.out.println("output is null");
        }
    }

    private void affirmAck(int sn, byte tag) {
        synchronized (sndAckLock) {
            try {
                while (sndAckWait) {
                    sndAckLock.wait();
                }
                sndAckWait = true;
                sndAckList.removeIf(chunk -> chunk.sn == sn);
                sndAckList.stream().forEach(chunk -> {
                    if (chunk.sn < sn) {
                        chunk.fastack++;
                    }
                });
                if (sndAckList.isEmpty()) {
                    sndUna = sndNxt;
                } else {
                    sndUna = sndAckList.getFirst().sn;
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                sndAckWait = false;
                sndAckLock.notifyAll();
            }
        }
    }

    private void dealTag(int sn, byte tag) {
    }

    private void affirmSnd(byte tag, long msgid, int tot, int all, long ts, int sn, int una, int length, byte[] data) {
        Chunk chunk = Chunk.newChunk(data);
        chunk.msgid = msgid;
        chunk.tot = tot;
        chunk.all = all;
        chunk.ts = ts;
        chunk.sn = sn;
        chunk.una = una;
        chunk.length = length;
//        LOGGER.info("rudp2 affirmSnd :" +  " order size:" + rcvOrder.size() + " sb size:" + rcvShambles.size());
        synchronized (rcvShamebleLock) {
            try {
                while (rcvShambleWait) {
                    rcvShamebleLock.wait();
                }
                rcvShambleWait = true;

                boolean add = false;
                add = sn >= rcvNxt && !rcvShambles.stream().anyMatch(chunk1 -> chunk == null ? false:(chunk1.sn == sn));
                if (add){
                    rcvShambles.addLast(chunk);
                    LOGGER.info("add sn:" + chunk.sn + " " +sn);
                }
//                boolean exist = rcvShambles.stream().anyMatch(chunk1 -> chunk1 == null ? false : (chunk1.sn == sn));
//                exist = sn < rcvNxt;
//                if (!exist) {
//                    rcvShambles.add(chunk);
//                }
                flushAck(sn);
                Statistic.rcvCount.getAndIncrement();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                rcvShambleWait = false;
                rcvShamebleLock.notifyAll();
            }
        }
    }

    private void flushAck(int sn) {
        Chunk chunk = Chunk.newChunk(new byte[0]);
        chunk.cmd = RudpParam.CMD_ACK;
        chunk.sn = sn;
        ByteBuf buf = createOutputByteBuf(chunk);
        sndChunk(buf, sn);
    }

    public void arrangeRcvShambles() {
        LinkedList<Chunk> addList = new LinkedList<>();
        synchronized (rcvShamebleLock) {
            try {
                while (rcvShambleWait) {
                    rcvShamebleLock.wait();
                }
                rcvShambleWait = true;
                while (rcvShambles.stream().anyMatch(chunk -> chunk.sn == rcvNxt)) {
                    for (Iterator<Chunk> itr = rcvShambles.iterator(); itr.hasNext(); ) {
                        Chunk chunk = itr.next();
                        if (chunk.sn == rcvNxt) {
                            addList.add(chunk);
                            itr.remove();
                            rcvNxt++;
                        } else if (chunk.sn < rcvNxt) {
                            itr.remove();
                        }
                    }
                }
                rcvShambles.removeIf(chunk -> chunk.sn < rcvNxt);
//                LOGGER.info("arrangeRcvShambles rcvNxt:" + rcvNxt);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                rcvShambleWait = false;
                rcvShamebleLock.notifyAll();
            }
        }
        synchronized (rcvOrderLock) {
            try {
                while (rcvOrderWait) {
                    rcvOrderLock.wait();
                }
                rcvOrderWait = true;
                rcvOrder.addAll(addList);
                Statistic.rcvOrderCount.getAndAdd(addList.size());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                rcvOrderWait = false;
                rcvOrderLock.notifyAll();
            }
        }
    }

    //返回可以提取的msgid
    public boolean canRcv() {
//        LOGGER.info("rudp2 canRcv:" + " order size:" + rcvOrder.size() + " sb size:" + rcvShambles.size());
        boolean canRcv = true;
        synchronized (rcvOrderLock) {
            try {
                while (rcvOrderWait) {
                    rcvOrderLock.wait();
                }
                rcvOrderWait = true;
                if (rcvOrder.isEmpty()) {
                    return false;
                }
                Chunk chunk = rcvOrder.get(0);
                if (chunk.all == chunk.tot + 1) {
                    canRcv = true;
                    return true;
                }
                canRcv = rcvOrder.stream().anyMatch(chunk1 -> chunk1 == null ? false : (chunk1.tot + 1) == chunk1.all);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                rcvOrderWait = false;
                rcvOrderLock.notifyAll();
            }
        }
        return canRcv;
    }

    public Message mergeMsg() {
//        LOGGER.info("rudp2 mergeMsg :" + " order size:" + rcvOrder.size() + " sb size:" + rcvShambles.size());
        LinkedList<Chunk> chunks = new LinkedList<>();
        synchronized (rcvOrderLock) {
            try {
                while (rcvOrderWait) {
                    rcvOrderLock.wait();
                }
                rcvOrderWait = true;
                Chunk target = rcvOrder.stream().filter(chunk -> chunk == null ? false : (chunk.tot + 1) == chunk.all).findFirst().orElse(null);
                if (target != null) {
                    for (Iterator<Chunk> itr = rcvOrder.iterator(); itr.hasNext(); ) {
                        Chunk chunk = itr.next();
                        if (chunk.msgid == target.msgid) {
                            chunks.addLast(chunk);
                            itr.remove();
                        }
                    }
                    if (!chunks.isEmpty()) {
                        if (chunks.size() != target.all) {
                            chunks.forEach(chunk -> LOGGER.info("get wrong sn:" + chunk.sn + " toto:" + chunk.tot + " all:" + chunk.all + " msgid:" + chunk.msgid));
                            rcvOrder.addAll(0, chunks);
                            chunks.clear();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                rcvOrderWait = false;
                rcvOrderLock.notifyAll();
            }
        }
        Message msg = null;
        if (!chunks.isEmpty()) {
            int length = chunks.stream().mapToInt(chunk -> chunk.length).sum();
            byte[] result = new byte[length];
            Collections.sort(chunks, Comparator.comparingInt(o -> o.tot));
            for (int i = 0; i < chunks.size(); i++) {
                try {
                    System.arraycopy(chunks.get(i).data, 0, result, mtuBody * i, chunks.get(i).data.length);
                } catch (Exception e) {
                    for (int j = 0; j < chunks.size(); j++) {
                        LOGGER.info("j:" + j + " tot:" + chunks.get(j).tot + " msgid:" + chunks.get(j).msgid + " length:" + chunks.get(j).length + " dlen:" + chunks.get(j).data.length);
                    }
                    e.printStackTrace();
                }
            }
            msg = MessageUtil.bytes2Msg(result);
        }
        return msg;
    }


    public boolean isStop() {
        return stop;
    }

    public int getInterval() {
        return interval;
    }

    public LinkedList<Chunk> getRcvOrder() {
        return rcvOrder;
    }

    public LinkedList<Chunk> getRcvShambles() {
        return rcvShambles;
    }

    public int getRcvNxt() {
        return rcvNxt;
    }

    private void sortChunks(LinkedList<Chunk> chunks) {
        Collections.sort(chunks, new Comparator<Chunk>() {
            @Override
            public int compare(Chunk o1, Chunk o2) {
                if (o1.tot > o2.tot)
                    return 0;
                else
                    return 1;
            }
        });
    }

    private String getSnStrs(LinkedList<Chunk> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Chunk chunk : chunks) {
            sb.append(chunk.sn + " ");
        }
        sb.append("]");
        return sb.toString();
    }


    private String getSnStrsByInteger(LinkedList<Integer> sns){
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Integer in : sns){
            sb.append(in + " ");
        }
        sb.append("]");
        return sb.toString();
    }

    private String getIDStrs(LinkedList<Chunk> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Chunk chunk : chunks) {
            sb.append(chunk.msgid + " ");
        }
        sb.append("]");
        return sb.toString();
    }

    //配合一开始是否能够开始发送数据
    public boolean canSndMsg() {
        return this.tag == RudpParam.TAG_OLD;
    }
}
