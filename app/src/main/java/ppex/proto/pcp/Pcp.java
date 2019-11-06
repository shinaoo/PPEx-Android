package ppex.proto.pcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import ppex.proto.msg.entity.Connection;
import ppex.utils.set.ReItrLinkedList;
import ppex.utils.set.ReusableListIterator;

import java.util.Iterator;
import java.util.LinkedList;

public class Pcp {
    /**
     * no delay min rto
     */
    public static final int IKCP_RTO_NDL = 30;

    /**
     * normal min rto
     */
    public static final int IKCP_RTO_MIN = 100;

    public static final int IKCP_RTO_DEF = 200;

    public static final int IKCP_RTO_MAX = 60000;

    /**
     * cmd: push data
     */
    public static final byte IKCP_CMD_PUSH = 81;

    /**
     * cmd: ack
     */
    public static final byte IKCP_CMD_ACK = 82;

    /**
     * cmd: window probe (ask)
     * 询问对方当前剩余窗口大小 请求
     */
    public static final byte IKCP_CMD_WASK = 83;

    /**
     * cmd: window size (tell)
     * 返回本地当前剩余窗口大小
     */
    public static final byte IKCP_CMD_WINS = 84;

    /**
     * need to send IKCP_CMD_WASK
     */
    public static final int IKCP_ASK_SEND = 1;

    /**
     * need to send IKCP_CMD_WINS
     */
    public static final int IKCP_ASK_TELL = 2;

    public static final int IKCP_WND_SND = 32;

    public static final int IKCP_WND_RCV = 32;

    public static final int IKCP_MTU_DEF = 1459;

    public static final int IKCP_INTERVAL = 100;

    public int IKCP_OVERHEAD = 24;

    public static final int IKCP_DEADLINK = 20;

    public static final int IKCP_THRESH_INIT = 2;

    public static final int IKCP_THRESH_MIN = 2;

    /**
     * 7 secs to probe window size
     */
    public static final int IKCP_PROBE_INIT = 7000;

    /**
     * up to 120 secs to probe window
     */
    public static final int IKCP_PROBE_LIMIT = 120000;
    public static final int IKCP_HEAD = 174;


    //conv 会话,mtu最大传输单元大小,mss最大分节大小.mtu减去头部分
    private int conv;
    private int mtu = IKCP_MTU_DEF;
    private int mss = this.mtu - IKCP_HEAD;
    //snd_una 已发送但未确认,snd_nxt下次发送下标,rcv_nxt,下次接收下标
    private long snd_una, snd_nxt, rcv_nxt;
    //ts_recent,ts_lastack 上次ack时间,ts_ssthresh 慢启动门限
    private int ts_recent, ts_lastack;
    private int ts_ssthresh = IKCP_THRESH_INIT;
    //rx_rttval RoundTripTime,rx_srtt 平滑rtt,rx_rto 重传超时,rxMinrto 最小重传超时
    private int rx_rttval, rx_srtt;
    private int rx_rto = IKCP_RTO_DEF;
    private int rx_minrto = IKCP_RTO_MIN;
    //snd_wnd 发送窗口,rcv_wnd 接收窗口,rmt_wnd 远端可接受端口,cwnd 拥塞控制窗口,probe 探测标志位
    private int snd_wnd = IKCP_WND_SND;
    private int rcv_wnd = IKCP_WND_RCV;
    private int rmt_wnd = IKCP_WND_RCV;
    private int cwnd, probe;
    //current 当前时间,interval 间隔,ts_flush 发送时间戳,
    private int interval = IKCP_INTERVAL;
    private int ts_flush = IKCP_INTERVAL;
    //nodelay 收到包立即回ack, updated 状态是否更新
    private boolean nodelay, updated;
    //ts_probe 探测时间,probe_探测等待 probe_wait
    private long ts_probe, probe_wait;
    //死链接,重传达到该值时认为连接是断开的
    private int deadLink = IKCP_DEADLINK;
    //拥塞控制量
    private int incr;
    //是否关闭拥塞控制窗口
    private boolean nocwnd;
    //快速重传,超过几个ack就重传.
    private int fastresend;
    //当前开始时间戳
    private long startTicks = System.currentTimeMillis();
    //分配ByteBuf类
    private ByteBufAllocator byteBufAllocator = ByteBufAllocator.DEFAULT;
    //ack二进制标识
    private long ackMask;
    private int ackMaskSize = 0;
    private long lastRcvNxt;
    private long[] acklist = new long[8];

    //未知用途
    private int ackcount;

    //待发送数据,窗口
    private LinkedList<Fragment> sndQueue = new LinkedList<>();
    //发送后待确认队列
    private ReItrLinkedList<Fragment> sndBuf = new ReItrLinkedList<>();
    //收到消息后的有序队列
    private ReItrLinkedList<Fragment> rcvQueue = new ReItrLinkedList<>();
    //收到消息后的无序队列
    private ReItrLinkedList<Fragment> rcvBuf = new ReItrLinkedList<>();

    //发送队列以及接受队列的元素
    private ReusableListIterator<Fragment> rcvQueueItr = rcvQueue.listIterator();
    private ReusableListIterator<Fragment> rcvBufItr = rcvBuf.listIterator();
    public ReusableListIterator<Fragment> sndBufItr = sndBuf.listIterator();

    private PcpOutput pcpOutput;
    private Connection connection;

    public Pcp(int conv, PcpOutput pcpOutput, Connection connection) {
        this.conv = conv;
        this.pcpOutput = pcpOutput;
        this.connection = connection;
    }

    public int send(ByteBuf buf) {
        int len = buf.readableBytes();
        if (len == 0) {
            return -1;
        }
        int count;
        if (len <= mss) {
            count = 1;
        } else {
            count = (len + mss - 1) / mss;
        }
        if (count == 0)
            count = 1;
        for (int i = 0; i < count; i++) {
            int size = len > mss ? mss : len;
            Fragment frg = Fragment.createFragment(buf.readRetainedSlice(size));
            sndQueue.add(frg);
            len = buf.readableBytes();
        }
        return 0;
    }

    public int waitSnd() {
        return this.sndBuf.size() + this.sndQueue.size();//??这里一直不明白为什么相加
    }

    public long flush(boolean ackOnly, long current) {
        current = current - startTicks;
        Fragment fragment = Fragment.createFragment(byteBufAllocator, 0);
        fragment.conv = conv;
        fragment.cmd = IKCP_CMD_ACK;
        fragment.wnd = wndUnused();         //可接收数量
        fragment.una = rcv_nxt;             //已接收数量,下次要接收包的sn.这sn之前的包都接收到了
        ByteBuf byteBuf = createFlushByteBuf();
//        byteBuf.writerIndex()             //没有预留一个长度给fec

        int count = ackcount;
        if (lastRcvNxt != rcv_nxt) {
            ackMask = 0;
            lastRcvNxt = rcv_nxt;
        }
        for (int i = 0; i < count; i++) {
            long sn = acklist[i * 2];
            if (sn < rcv_nxt)
                continue;
            long index = sn - rcv_nxt - 1;
            if (index >= ackMaskSize)
                break;
            if (index >= 0) {
                ackMask |= 1 << index;
            }
        }

        fragment.ackMask = ackMask;
        for (int i = 0; i < count; i++) {
            byteBuf = makeSpace(byteBuf, IKCP_OVERHEAD);
            long sn = acklist[i * 2];
            if (sn >= rcv_nxt || count - 1 == i) {
                fragment.sn = sn;
                fragment.ts = acklist[i * 2 + 1];
                encodeFragment(byteBuf, fragment);
            }
        }

        ackcount = 0;
        if (ackOnly) {
            flushBuffer(byteBuf);
            fragment.recycler(true);
            return interval;
        }

        //拥堵控制,如果对方的接收窗口为0,需要询问对方窗口大小
        if (rmt_wnd == 0) {
            if (probe_wait == 0) {
                probe_wait = IKCP_PROBE_INIT;
                ts_probe = current + probe_wait;
            } else {
                if (itimediff(current, ts_probe) >= 0) {
                    if (probe_wait < IKCP_PROBE_INIT) {
                        probe_wait = IKCP_PROBE_INIT;
                    }
                    probe_wait += probe_wait / 2;
                    if (probe_wait > IKCP_PROBE_LIMIT) {
                        probe_wait = IKCP_PROBE_LIMIT;
                    }
                    ts_probe = current + probe_wait;
                    probe |= IKCP_ASK_SEND;
                }
            }
        } else {
            ts_probe = 0;
            probe_wait = 0;
        }

        if ((probe & IKCP_ASK_SEND) != 0){
            fragment.cmd = IKCP_CMD_WASK;
            byteBuf = makeSpace(byteBuf,IKCP_OVERHEAD);
            encodeFragment(byteBuf,fragment);
        }

        if ((probe & IKCP_ASK_TELL) != 0){
            fragment.cmd = IKCP_CMD_WINS;
            byteBuf = makeSpace(byteBuf,IKCP_OVERHEAD);
            encodeFragment(byteBuf,fragment);
        }

        probe = 0;
        //计算窗口大小
        int cwnd0 = Math.min(snd_wnd,rmt_wnd);
        if (!nocwnd){
            cwnd0 = Math.min(this.cwnd,cwnd0);
        }

        int newFrgsCount = 0;
        while(itimediff(snd_nxt,snd_una + cwnd0) < 0){
            Fragment newFrg = sndQueue.poll();
            if (newFrg == null)
                break;
            newFrg.conv = conv;
            newFrg.cmd = IKCP_CMD_PUSH;
            newFrg.sn = snd_nxt;
            sndBuf.add(newFrg);
            snd_nxt++;
            newFrgsCount++;
        }
        //计算重新发送
        int resent = fastresend > 0 ? fastresend : Integer.MAX_VALUE;
        int change = 0;
        boolean lost = false;
//        int lostFrgs = 0,fastRetranssFrgs = 0,earlyRetransFrgs = 0;
        long minrto = interval;

        for (Iterator<Fragment> itr = sndBufItr.rewind();itr.hasNext();){
            Fragment frg= itr.next();
            boolean needsend =false;
            if (frg.xmit == 0){
                needsend = true;
                frg.rto = rx_rto;
                frg.resendts = current + frg.rto;
            }else if (frg.fastack >= resent){
                needsend = true;
                frg.fastack = 0;
                frg.rto = rx_rto;
                frg.resendts = current + frg.rto;
                change ++;
//                fastRetranssFrgs ++;
            }else if (frg.fastack > 0 && newFrgsCount == 0){
                needsend = true;
                frg.fastack = 0;
                frg.rto = rx_rto;
                frg.resendts = current + frg.rto;
                change++;
//                earlyRetransFrgs ++;
            }else if (itimediff(current,frg.resendts) >= 0){
                needsend = true;
                if (!nodelay){
                    frg.rto += rx_rto;
                }else{
                    frg.rto += rx_rto / 2;
                }
                frg.fastack = 0;
                frg.resendts = current + frg.rto;
                lost = true;
//                lostFrgs ++;
            }

            if (needsend){
                frg.xmit ++;
                frg.ts = long2Uint(current);
                frg.wnd = fragment.wnd;
                frg.una = rcv_nxt;
                frg.setAckMaskSize(this.ackMaskSize);
                frg.ackMask = ackMask;

                ByteBuf frgData = fragment.data;
                int frgLen = frgData.readableBytes();
                int need = IKCP_OVERHEAD + frgLen;
                byteBuf = makeSpace(byteBuf,need);
                encodeFragment(byteBuf,frg);
                if (frgLen > 0){
                    byteBuf.writeBytes(frgData,frgData.readerIndex(),frgLen);
                }

                if (frg.xmit >= deadLink){
                    //连接断开
                }
                long rto = itimediff(frg.resendts,current);
                if (rto > 0 && rto < minrto){
                    minrto = rto;
                }
            }
        }

        flushBuffer(byteBuf);
        fragment.recycler(true);

        if (!nocwnd){
            if (change > 0){
                int inflight = (int) (snd_nxt - snd_una);
                ts_ssthresh  = inflight / 2;
                if (ts_ssthresh < IKCP_THRESH_MIN){
                    ts_ssthresh = IKCP_THRESH_MIN;
                }
                cwnd = ts_ssthresh + resent;
                incr = cwnd * mss;
            }
            if (lost){
                ts_ssthresh = cwnd0 / 2;
                if (ts_ssthresh < IKCP_THRESH_MIN){
                    ts_ssthresh = IKCP_THRESH_MIN;
                }
                cwnd = 1;
                incr = mss;
            }
            if (cwnd < 1){
                cwnd = 1;
                incr = mss;
            }
        }
        return minrto;

    }


    private int wndUnused() {
        int tmp = rcvQueue.size() - rcv_wnd;
        return tmp < 0 ? 0 : tmp;
    }

    private ByteBuf createFlushByteBuf() {
        return byteBufAllocator.ioBuffer(this.mtu);
    }

    private ByteBuf makeSpace(ByteBuf buf, int space) {
        if (buf.readableBytes() + space > mtu) {
            //发送??为什么发送
//            output(buf,this);
            buf = createFlushByteBuf();
//            buf.writerIndex()
        }
        return buf;
    }

    private static int encodeFragment(ByteBuf buf, Fragment fragment) {
        int offset = buf.writerIndex();
        buf.writeIntLE(fragment.conv);
        buf.writeByte(fragment.cmd);
//        buf.writeByte(fragment.frgid);
        buf.writeShortLE(fragment.wnd);
        buf.writeIntLE((int) fragment.ts);
        buf.writeIntLE((int) fragment.sn);
        buf.writeIntLE((int) fragment.una);
        buf.writeIntLE(fragment.data.readableBytes());
        switch (fragment.getAckMaskSize()) {
            case 8:
                buf.writeByte((int) fragment.ackMask);
                break;
            case 16:
                buf.writeShortLE((int) fragment.ackMask);
                break;
            case 32:
                buf.writeIntLE((int) fragment.ackMask);
            case 64:
                buf.writeLongLE(fragment.ackMask);
                break;
        }
        return buf.writerIndex() - offset;
    }

    private void flushBuffer(ByteBuf byteBuf) {
        //发送出去
        if (byteBuf.readableBytes() > 0){
            output(byteBuf,this);
            return;
        }
        byteBuf.release();
    }

    private static void output(ByteBuf data,Pcp pcp){
        if (data.readableBytes() == 0)
            return;
        pcp.pcpOutput.out(data,pcp);
    }

    private static int itimediff(long later, long earlier) {
        return (int) (later - earlier);
    }


    public void setSnd_wnd(int snd_wnd) {
        this.snd_wnd = snd_wnd;
    }

    public int getSnd_wnd() {
        return snd_wnd;
    }

    public void setMtu(int mtu) {
        this.mtu = mtu;
        this.mss = mtu-IKCP_HEAD;
    }

    public int getMss(){
        return this.mss;
    }


    private static long long2Uint(long n){
        return n & 0x00000000FFFFFFFFL;
    }

    public int getInterval() {
        return interval;
    }

    public Connection getConnection() {
        return connection;
    }
}
