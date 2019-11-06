package ppex.proto.rudp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import org.apache.log4j.Logger;
import ppex.proto.msg.Message;
import ppex.proto.msg.entity.Connection;
import ppex.utils.MessageUtil;
import ppex.utils.set.ReItrLinkedList;
import ppex.utils.set.ReusableListIterator;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

public class Rudp {
    private static Logger LOGGER = Logger.getLogger(Rudp.class);

    public static final int NO_DEFINE_RTO = 30;
    public static final int RTO_MIN = 100;
    public static final int RTO_DEFAULT = 200;
    public static final int RTO_MAX = 60000;

    public static final byte CMD_PUSH = 81;
    public static final byte CMD_ACK = 82;
    public static final byte CMD_ASK_WIN = 83;
    public static final byte CMD_TELL_WIN = 84;

    //need to send cmd_ask_win msg
    public static final int ASK_WIN = 1;
    //need to send cmd_tell_win msg
    public static final int TELL_WIN = 2;

    //超过次数重传就认为连接断开
    public static final int DEAD_LINK = 10;
    //头部数据长度
    public int HEAD_LEN = 45;
    //MTU
    public static final int MTU_DEFUALT = 1471;
    public static final int INTERVAL = 100;
    //接收和发送窗口长度
    public static final int WND_SND = 32;
    public static final int WND_RCV = 32;
    public static final int PROBE_INIT = 7000;
    public static final int PROBE_LIMI = 120000;
    //超过几个就重传
    public static final int RESEND_DEFAULT = 10;

    private int mtu = MTU_DEFUALT;
    private int mss = mtu - HEAD_LEN;
    private long snd_una, snd_nxt, rcv_nxt;

    //rtt和srtt
    private int rx_rttval, rx_srttval;
    private int rx_rto = RTO_DEFAULT, rx_rtomin = RTO_MIN;
    //接收窗口等值
    private int wnd_snd = WND_SND, wnd_rcv = WND_RCV, wnd_rmt = WND_RCV;
    private int wnd_cur = WND_SND;
    //探测值
    private int probe;
    private int interval = INTERVAL;
    //探测时间,探测等待时间
    private long ts_probe, ts_probe_wait;
    //断开链接
    private int deadLink = DEAD_LINK;
    //拥塞控制
    private int congest = 0;
    //表示有几个等待ack的数量
    private int ackcount = 0;
    //等待发送的数据
    private LinkedList<Frg> queue_snd = new LinkedList<>();
    //发送后等待确认数据,与上面queue_snd对应下来
    private ReItrLinkedList<Frg> queue_sndack = new ReItrLinkedList<>();
    //收到有序的消息队列
    private ReItrLinkedList<Frg> queue_rcv_order = new ReItrLinkedList<>();
    //收到无序的消息队列
    private ReItrLinkedList<Frg> queue_rcv_shambles = new ReItrLinkedList<>();

    //发送队列以及接收队列的iterator
    private ReusableListIterator<Frg> itr_queue_rcv_order = queue_rcv_order.listIterator();
    private ReusableListIterator<Frg> itr_queue_rcv_shambles = queue_rcv_shambles.listIterator();
    private ReusableListIterator<Frg> itr_queue_sndack = queue_sndack.listIterator();

    //开始的时间戳
    private long startTicks = System.currentTimeMillis();
    //ByteBuf操作类
    private ByteBufAllocator byteBufAllocator = PooledByteBufAllocator.DEFAULT;
    //超过该数量重传
    private int resend = RESEND_DEFAULT;


    private Output output;
    private Connection connection;
    private long ack;

    public Rudp(Output output, Connection connection) {
        this.output = output;
        this.connection = connection;
    }

    public int send(ByteBuf buf, long msgid) {
        int len = buf.readableBytes();
        if (len == 0){
            buf.release();
            return -1;
        }
        int count = 0;
        if (len <= mss) {
            count = 1;
        } else {
            count = (len + mss - 1) / mss;
        }
        if (count == 0)
            count = 1;
        for (int i = 0; i < count; i++) {
            int size = len > mss ? mss : len;
            Frg frg = Frg.createFrg(buf.readRetainedSlice(size));
            frg.tot = (count - i - 1);
            frg.msgid = msgid;
            queue_snd.add(frg);
            len = buf.readableBytes();
        }
        buf.release();
        return 0;
    }

    public int send(Message msg) {
//        LOGGER.info("Rudp send msg id:" + msg.getMsgid());
        ByteBuf buf = MessageUtil.msg2ByteBuf(msg);
        return send(buf, msg.getMsgid());
    }

    public long flush(boolean ackonly, long current) {
        //发送是先将queue_snd里面的数据发送
        int wnd_count = Math.min(wnd_snd, wnd_rmt);                      //后面加入请求server端窗口数量来控制拥塞
        while (itimediff(snd_nxt, snd_una + wnd_count) < 0) {    //这里控制数量输入，即是窗口的数量控制好了
            Frg frg = queue_snd.poll();
            if (frg == null)
                break;
            frg.cmd = CMD_PUSH;
            frg.sn = snd_nxt;
            queue_sndack.add(frg);
            snd_nxt++;
        }
        for (Iterator<Frg> itr = queue_sndack.listIterator(); itr.hasNext(); ) {
            Frg frg = itr.next();
            boolean send = false;
            if (frg.xmit == 0) {             //第一次发送
                send = true;
                frg.rto = rx_rto;
                frg.ts_resnd = current + frg.rto;
            } else if (frg.fastack >= resend) {           //每次接收ack的时候,看序号,给fastack加1.超过10个就重传
                send = true;
                frg.fastack = 0;
                frg.rto = rx_rto;
                frg.ts_resnd = current + frg.rto;
            } else if (itimediff(current, frg.ts_resnd) >= 0) {    //超过重传时间戳就开始重传
                send = true;
                frg.rto += rx_rto;              //rto翻倍
                frg.fastack = 0;
                frg.ts_resnd = current + frg.rto;
            }
            if (send) {
                frg.xmit++;
                if (frg.xmit >= deadLink) {
                    //todo 连接已断开
                }
                frg.ts = current;
                frg.wnd = wndUnuse();
                frg.una = rcv_nxt;
                ByteBuf flushbuf = createEmptyByteBuf(HEAD_LEN + frg.data.readableBytes());
                encodeFlushBuf(flushbuf, frg);
                if (frg.data.readableBytes() > 0) {
                    flushbuf.writeBytes(frg.data, frg.data.readerIndex(), frg.data.readableBytes());
                }
                output(flushbuf);
            }
        }
        return interval;
    }


    private int itimediff(long later, long earlier) {
        return (int) (later - earlier);
    }

    private int wndUnuse() {
        int tmp = wnd_rcv - queue_rcv_order.size();
        return tmp < 0 ? 0 : tmp;
    }

    private void output(ByteBuf buf) {
        if (buf.readableBytes() > 0) {
            output.output(buf, this);
            return;
        }
        buf.release();
    }

    private ByteBuf createEmptyByteBuf(int len) {
        return byteBufAllocator.ioBuffer(len);
    }

    private int encodeFlushBuf(ByteBuf buf, Frg frg) {
        int offset = buf.writerIndex();
        buf.writeByte(frg.cmd);
        buf.writeLongLE(frg.msgid);
        buf.writeIntLE(frg.tot);
        buf.writeIntLE(frg.wnd);
        buf.writeLongLE(frg.ts);
        buf.writeLongLE(frg.sn);
        buf.writeLongLE(frg.una);
        buf.writeIntLE(frg.data.readableBytes());
        return buf.writerIndex() - offset;
    }

    public int input(ByteBuf data, long time) {
        long old_snd_una = snd_una;
        if (data == null || data.readableBytes() < HEAD_LEN) {
            return -1;
        }
        while (true) {
            byte cmd;
            long msgid, ts, sn, una;
            int tot, wnd, len;
            if (data.readableBytes() < HEAD_LEN) {
                break;
            }
            cmd = data.readByte();
            msgid = data.readLongLE();
            tot = data.readIntLE();
            wnd = data.readIntLE();
            ts = data.readLongLE();
            sn = data.readLongLE();
            una = data.readLongLE();
            len = data.readIntLE();
            if (data.readableBytes() < len) {
                return -2;
            }
            if (cmd != CMD_ACK && cmd != CMD_PUSH && cmd != CMD_ASK_WIN && cmd != CMD_TELL_WIN) {
                return -3;
            }
            this.wnd_rmt = wnd;
            parseUna(una);
            shrinkBuf();
            switch (cmd) {
                case CMD_ACK:
                    LOGGER.info("Rudp ACK msgid:" + msgid + " size of snd:" + queue_snd.size() + " size of ack:" + queue_sndack.size());
                    affirmAck(sn);
                    affirmFastAck(sn, ts);
                    break;
                case CMD_PUSH:
                    //首先判断是否超过窗口
                    if (itimediff(sn, rcv_nxt + wnd_rcv) < 0) {
                        flushAck(sn, ts, msgid);          //返回ack
                        Frg frg;
                        if (len > 0) {
                            frg = Frg.createFrg(data.readRetainedSlice(len));
                        } else {
                            frg = Frg.createFrg(byteBufAllocator, 0);
                        }
                        frg.cmd = cmd;
                        frg.msgid = msgid;
                        frg.tot = tot;
                        frg.wnd = wnd;
                        frg.ts = ts;
                        frg.sn = sn;
                        frg.una = una;
                        parseRcvData(frg);
                        arrangeRcvData();
                    }
                    break;
                case CMD_ASK_WIN:
                    break;
                case CMD_TELL_WIN:
                    break;
            }
        }
        return 0;
    }

    private void parseUna(long una) {
        for (Iterator<Frg> itr = itr_queue_sndack.rewind(); itr.hasNext(); ) {
            Frg frg = itr.next();
            if (itimediff(una, frg.sn) > 0) {
                itr.remove();
            } else {
                break;
            }
        }
    }

    private void shrinkBuf() {
        if (queue_sndack.size() > 0) {
            Frg frg = queue_sndack.peek();
            snd_una = frg.sn;
        } else {
            snd_una = snd_nxt;
        }
    }

    private void affirmAck(long sn) {
        if (itimediff(sn, snd_una) < 0 || itimediff(sn, snd_nxt) >= 0) {
            return;
        }
        for (Iterator<Frg> itr = itr_queue_sndack.rewind(); itr.hasNext(); ) {
            Frg fr = itr.next();
            if (sn == fr.sn) {
                itr.remove();
                break;
            }
            if (itimediff(sn, fr.sn) < 0)
                break;
        }
    }

    private void affirmFastAck(long sn, long ts) {
        if (itimediff(sn, snd_una) < 0 || itimediff(sn, snd_nxt) >= 0) {
            return;
        }
        for (Iterator<Frg> itr = itr_queue_sndack.rewind(); itr.hasNext(); ) {
            Frg frg = itr.next();
            if (itimediff(sn, frg.sn) < 0) {
                break;
            } else if (sn != frg.sn && itimediff(frg.ts, ts) <= 0) {
                frg.fastack++;
            }
        }
    }

    private void flushAck(long sn, long ts, long msgid) {
        Frg frg = Frg.createFrg(byteBufAllocator, 0);
        frg.cmd = CMD_ACK;
        frg.wnd = wndUnuse();
        frg.una = rcv_nxt;
        frg.msgid = msgid;
        frg.sn = sn;
        frg.ts = ts;
        frg.tot = 0;
        ByteBuf flushbuf = createEmptyByteBuf(HEAD_LEN);
        encodeFlushBuf(flushbuf, frg);
        output(flushbuf);
    }

    private void parseRcvData(Frg frg) {
        long sn = frg.sn;
        if (itimediff(sn, rcv_nxt + wnd_rcv) >= 0 || itimediff(sn, rcv_nxt) < 0) {
            return;
        }
        boolean repeat = false, findPos = false;
        ListIterator<Frg> itrList = null;
        if (queue_rcv_shambles.size() > 0) {
            itrList = itr_queue_rcv_shambles.rewind(queue_rcv_shambles.size());
            while (itrList.hasPrevious()) {
                Frg frgTmp = itrList.previous();
                if (frgTmp.sn == sn) {
                    repeat = true;
                    break;
                }
                if (itimediff(sn, frgTmp.sn) > 0) {
                    findPos = true;
                    break;
                }
            }
        }
        if (repeat) {
            //todo 消息重复,目前没有处理
            frg.recycler(true);
        } else if (itrList == null) {
            queue_rcv_shambles.add(frg);
        } else {
            if (findPos)
                itrList.next();
            itrList.add(frg);
        }
    }

    private void arrangeRcvData() {
        for (Iterator<Frg> itr = itr_queue_rcv_shambles.rewind(); itr.hasNext(); ) {
            Frg frg = itr.next();
            if (frg.sn == rcv_nxt && queue_rcv_shambles.size() < wnd_rcv) {
                itr.remove();
                queue_rcv_order.add(frg);
                rcv_nxt++;
            } else {
                break;
            }
        }
    }

    //从queue中找出可以合成message的数据
    public Message mergeRcvData() {
        if (queue_rcv_order.isEmpty())
            return null;
        //获取申请的Bytebuf长度
        int len = lenOfByteBuf();
        if (len < 0)
            return null;
        ByteBuf buf = null;
        for (Iterator<Frg> itr = itr_queue_rcv_order.rewind(); itr.hasNext(); ) {
            Frg frg = itr.next();
            itr.remove();
            if (buf == null) {
                if (frg.tot == 0) {
                    buf = frg.data;
                    break;
                }
                buf = byteBufAllocator.ioBuffer(len);
            }
            buf.writeBytes(frg.data);
            frg.data.release();
            if (frg.tot == 0)
                break;
        }
        arrangeRcvData();
        Message msg = MessageUtil.bytebuf2Msg(buf);
        if (buf != null)
            buf.release();
        return msg;
    }

    public int lenOfByteBuf() {
        if (queue_rcv_order.isEmpty())
            return -1;
        Frg frg = queue_rcv_order.peek();
        if (frg.tot == 0)
            return frg.data.readableBytes();
        if (queue_rcv_order.size() < frg.tot + 1)
            return -1;
        int len = 0;
        for (Iterator<Frg> itr = itr_queue_rcv_order.rewind(); itr.hasNext(); ) {
            Frg f = itr.next();
            len += f.data.readableBytes();
            if (f.tot == 0)
                break;
        }
        return len;
    }


    public Connection getConnection() {
        return connection;
    }

    public int getInterval() {
        return interval;
    }

    public boolean canRcv() {
        if (queue_rcv_order.isEmpty())
            return false;
        Frg frg = queue_rcv_order.peek();
        if (frg.tot == 0)
            return true;
        if (queue_rcv_order.size() < frg.tot + 1) {
            return false;
        }
        return true;
    }

    public void release(){
        queue_rcv_order.forEach(frg -> frg.recycler(true));
        queue_rcv_shambles.forEach( frg -> frg.recycler(true));
        queue_snd.forEach(frg -> frg.recycler(true));
        queue_sndack.forEach(frg -> frg.recycler(true));
    }

    public int getWndSnd(){
        return wnd_snd;
    }
    public int waitSnd(){
        return this.queue_sndack.size() + this.queue_snd.size();
    }

}
