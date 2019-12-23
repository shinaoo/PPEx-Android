package ppex.proto.rudp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import ppex.proto.msg.Message;
import ppex.utils.MessageUtil;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Rudp {

    public static final int NO_DEFINE_RTO = 30;
    public static final int RTO_MIN = 100;
    public static final int RTO_DEFAULT = 200;
    public static final int RTO_MAX = 60000;

    public static final byte CMD_PUSH = 81;
    public static final byte CMD_ACK = 82;
    public static final byte CMD_ASK_WIN = 83;
    public static final byte CMD_TELL_WIN = 84;
    public static final byte CMD_RESET = 85;
    public static final byte CMD_FINISH = 86;

    //need to send cmd_ask_win msg
    public static final int ASK_WIN = 1;
    //need to send cmd_tell_win msg
    public static final int TELL_WIN = 2;

    //超过次数重传就认为连接断开
    public static final int DEAD_LINK = 20;
    //头部数据长度
    public static int HEAD_LEN = 45;
    //MTU
    public static final int MTU_DEFUALT = 1389;
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
    private List<Frg> queue_snd = new LinkedList<>();
    //发送后等待确认数据,与上面queue_snd对应下来
    private List<Frg> queue_sndack = new LinkedList<>();
    //收到有序的消息队列
    private List<Frg> queue_rcv_order = new LinkedList<>();
    //收到无序的消息队列
    private List<Frg> queue_rcv_shambles = new LinkedList<>();

    private Object lock_snd = new Object(), lock_sndack = new Object(), lock_rcvorder = new Object(), lock_rcvshambles = new Object();

    //开始的时间戳
    private long startTicks = System.currentTimeMillis();
    //ByteBuf操作类
    private ByteBufAllocator byteBufAllocator = ByteBufAllocator.DEFAULT;
    //超过该数量重传
    private int resend = RESEND_DEFAULT;
    //记录得到sn为0的帧的时间戳.为了截至新的rudp给旧的rudp发送数据造成混乱的修改
    private long zeroSnTimeStamp = System.currentTimeMillis();
    //记录该rudp是否是新建未发送过数据.为了截止旧的rudp给新的rudp发送数据,造成数据不处理
    private boolean isNew = true;

    private IOutput output;
    private long ack;
    private boolean stop = false;

    public Rudp(IOutput output) {
        this.output = output;
        queue_snd = Collections.synchronizedList(queue_snd);
        queue_sndack = Collections.synchronizedList(queue_sndack);
        queue_rcv_order = Collections.synchronizedList(queue_rcv_order);
        queue_rcv_shambles = Collections.synchronizedList(queue_rcv_shambles);
    }

    public int send(ByteBuf buf, long msgid) {
        int len = buf.readableBytes();
        if (len == 0) {
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
        ByteBuf bufduplicate = buf.duplicate();
        for (int i = 0; i < count; i++) {
            int size = len > mss ? mss : len;
            Frg frg = Frg.createFrg(byteBufAllocator, bufduplicate.readSlice(size));
            frg.tot = (count - i - 1);
            frg.msgid = msgid;
            queue_snd.add(frg);
            len = buf.readableBytes();
        }
        buf.release();
        return 0;
    }


//    public void sendReset() {
//        Frg frg = Frg.createFrg(byteBufAllocator, 0);
//        frg.cmd = CMD_RESET;
//        frg.tot = 0;
//        frg.msgid = -1;
//        queue_snd.add(frg);
//        snd_nxt = 0;
//        snd_una = snd_nxt;
//        rcv_nxt = 0;
//    }

    public void sendFinish() {
        Frg frg = Frg.createFrg(byteBufAllocator, 0);
        frg.cmd = CMD_FINISH;
        frg.tot = 0;
        frg.msgid = -1;
        queue_snd.add(frg);
    }

    public int send(Message msg) {
//        LOGGER.info("Rudp send msg id:" + msg.getMsgid());
        ByteBuf buf = MessageUtil.msg2ByteBuf(msg);
        return send(buf, msg.getMsgid());
    }

    public long flush(boolean ackonly, long current) {
        //发送是先将queue_snd里面的数据发送

        int wnd_count = Math.min(wnd_snd, wnd_rmt);                      //后面加入请求server端窗口数量来控制拥塞
        if (!queue_snd.isEmpty()) {
            while (itimediff(snd_nxt, snd_una + wnd_count) < 0 && !queue_snd.isEmpty()) {    //这里控制数量输入，即是窗口的数量控制好了
                Frg frg = null;
                synchronized (lock_snd) {
                    frg = queue_snd.remove(0);
                }
                if (frg == null)
                    break;
                frg.cmd = CMD_PUSH;
                frg.sn = snd_nxt;
                queue_sndack.add(frg);
                snd_nxt++;
            }
        }
        synchronized (queue_sndack) {
            for (Iterator<Frg> itr = queue_sndack.iterator(); itr.hasNext(); ) {
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
                        stop = true;
                    }
                    frg.ts = current;
                    frg.wnd = wndUnuse();
                    frg.una = rcv_nxt;
                    ByteBuf flushbuf = createEmptyByteBuf(HEAD_LEN + frg.data.readableBytes());
                    encodeFlushBuf(flushbuf, frg);
                    if (frg.data.readableBytes() > 0) {
                        flushbuf.writeBytes(frg.data, frg.data.readerIndex(), frg.data.readableBytes());
                    }
                    output(flushbuf, frg.sn);
                }
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

    private void output(ByteBuf buf, long sn) {
        if (buf.readableBytes() > 0) {
            output.output(buf, this, sn);
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
        buf.writeLong(frg.msgid);
        buf.writeInt(frg.tot);
        buf.writeInt(frg.wnd);
        buf.writeLong(frg.ts);
        buf.writeLong(frg.sn);
        buf.writeLong(frg.una);
        buf.writeInt(frg.data.readableBytes());
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
            msgid = data.readLong();
            tot = data.readInt();
            wnd = data.readInt();
            ts = data.readLong();
            sn = data.readLong();
            una = data.readLong();
            len = data.readInt();
            if (data.readableBytes() < len) {
                return -2;
            }
            if (cmd != CMD_ACK && cmd != CMD_PUSH && cmd != CMD_ASK_WIN && cmd != CMD_TELL_WIN && cmd != CMD_RESET) {
                return -3;
            }
//            this.wnd_rmt = wnd;
            parseUna(una);
            shrinkBuf();
            switch (cmd) {
                case CMD_ACK:
                    affirmAck(sn);
                    affirmFastAck(sn, ts);
                    break;
                case CMD_PUSH:
                    //首先判断是否超过窗口
                    //之前增加了cmd_reset之后,逻辑更加混乱,这里设置每当收到sn为0之后,都认为是一个新的开始.设置时间间隔超过1秒才算新的sn0.这个是为了防止新的rudp给旧的rudp发送数据.造成数据混乱
                    //todo 现在还有一个问题是,当一端断开之后,另一端不知道,这样的话,当断开的一端重新连接后,另一端没有断开的sn与rcv等都不对.所以需要一个结束的发送
                    if (sn == 0 && itimediff(ts, zeroSnTimeStamp) > 1000) {
                        reset();
                        zeroSnTimeStamp = ts;
                    }
                    if (sn != 0 && isNew) {
                        snd_nxt = una;
                        rcv_nxt = sn;
                        snd_una = snd_nxt;
                    }
                    if (itimediff(sn, rcv_nxt + wnd_rcv) < 0) {
                        flushAck(sn, ts, msgid);          //返回ack
                        Frg frg;
                        if (len > 0) {
                            frg = Frg.createFrg(byteBufAllocator, data.readBytes(len));
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
                case CMD_RESET:
                    if (itimediff(sn, rcv_nxt + wnd_rcv) < 0) {
                        reset();
                        flushAck(sn, ts, msgid);
                    }
                    break;
                case CMD_FINISH:
                    stop = true;
                    break;
            }
        }
        return 0;
    }

    private void parseUna(long una) {
        synchronized (queue_sndack) {
            for (Iterator<Frg> itr = queue_sndack.iterator(); itr.hasNext(); ) {
                Frg frg = itr.next();
                if (itimediff(una, frg.sn) > 0) {
                    itr.remove();
                } else {
                    break;
                }
            }
        }
    }

    private void shrinkBuf() {
        if (queue_sndack.size() > 0) {
            Frg frg = queue_sndack.get(0);
            snd_una = frg.sn;
        } else {
            snd_una = snd_nxt;
        }
    }

    private void affirmAck(long sn) {
        if (sn == 0) {
            isNew = false;
        }
        if (itimediff(sn, snd_una) < 0 || itimediff(sn, snd_nxt) >= 0) {
            return;
        }
        synchronized (lock_sndack) {
            for (Iterator<Frg> itr = queue_sndack.iterator(); itr.hasNext(); ) {
                Frg frg = itr.next();
                if (sn == frg.sn) {
                    frg.data.release();
                    itr.remove();
                    break;
                }
            }
        }
    }

    private void affirmFastAck(long sn, long ts) {
        if (itimediff(sn, snd_una) < 0 || itimediff(sn, snd_nxt) >= 0) {
            return;
        }
        for (Iterator<Frg> iterator = queue_sndack.iterator(); iterator.hasNext(); ) {
            Frg frg = iterator.next();
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
        output(flushbuf, frg.sn);
    }

    private void parseRcvData(Frg frg) {
        long sn = frg.sn;
        if (itimediff(sn, rcv_nxt + wnd_rcv) >= 0 || itimediff(sn, rcv_nxt) < 0) {
            return;
        }
        boolean repeat = false, findPos = false;
        if (queue_rcv_shambles.size() > 0) {
            for (Iterator<Frg> itr = queue_rcv_shambles.iterator(); itr.hasNext(); ) {
                Frg frgtmp = itr.next();
                if (frg.sn == frgtmp.sn) {
                    repeat = true;
                    break;
                }
            }
        }
        if (repeat) {
            frg.recycler(true);
        } else {
            queue_rcv_shambles.add(frg);
        }
    }

    private void arrangeRcvData() {
        synchronized (lock_rcvshambles) {
            for (Iterator<Frg> itr = queue_rcv_shambles.iterator(); itr.hasNext(); ) {
                Frg frg = itr.next();
                if (frg.sn == rcv_nxt && queue_rcv_shambles.size() < wnd_rcv) {
                    itr.remove();
                    queue_rcv_order.add(frg);
                    rcv_nxt++;
                } else if (frg.sn < rcv_nxt) {
                    itr.remove();
                } else {
                    break;
                }
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
        ByteBuf buf = byteBufAllocator.buffer(len);
        synchronized (lock_rcvorder) {
            for (Iterator<Frg> itr = queue_rcv_order.iterator(); itr.hasNext(); ) {
                Frg frg = itr.next();
                itr.remove();
                if (buf.readableBytes() == len && frg.tot == 0) {
                    break;
                }
                buf.writeBytes(frg.data);
                if (frg.tot == 0) {
                    break;
                }
            }
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
        Frg frg = queue_rcv_order.get(0);
        if (frg.tot == 0) {
            return frg.data.readableBytes();
        }
        if (queue_rcv_order.size() < frg.tot + 1)
            return -1;
        //用msgid保证是同一个msg的长度
        long msgid = queue_rcv_order.get(0).msgid;
        int len = 0;
        synchronized (lock_rcvorder) {
            for (Iterator<Frg> itr = queue_rcv_order.iterator(); itr.hasNext(); ) {
                Frg f = itr.next();
                if (msgid != f.msgid) {
                    continue;
                }
                len += f.data.readableBytes();
                if (f.tot == 0)
                    break;
            }
        }
        return len;
    }

    public int getInterval() {
        return interval;
    }

    public boolean canRcv() {
        if (queue_rcv_order.isEmpty())
            return false;
        Frg frg = queue_rcv_order.get(0);
        if (frg.tot == 0)
            return true;
        if (queue_rcv_order.size() < frg.tot + 1) {
            return false;
        }
        return true;
    }

    public void reset() {
        //解决服务器没断开,而客户端已经重开然后重连的情况.增加CMD_RESET
        snd_nxt = 0;
        snd_una = snd_nxt;
        rcv_nxt = 0;
        release();
        queue_snd.clear();
        queue_sndack.clear();
        queue_rcv_order.clear();
//        queue_rcv_shambles.clear();
    }

    public void release() {
        queue_rcv_order.forEach(frg -> frg.recycler(true));
        queue_rcv_shambles.forEach(frg -> frg.recycler(true));
        queue_snd.forEach(frg -> frg.recycler(true));
        queue_sndack.forEach(frg -> frg.recycler(true));
    }

    public List<Frg> getQueue_snd() {
        return queue_snd;
    }

    public List<Frg> getQueue_rcv_order() {
        return queue_rcv_order;
    }

    public List<Frg> getQueue_rcv_shambles() {
        return queue_rcv_shambles;
    }

    public int getWndSnd() {
        return wnd_snd;
    }

    public int waitSnd() {
        return this.queue_sndack.size() + this.queue_snd.size();
    }

    public boolean isStop() {
        return stop;
    }
}
