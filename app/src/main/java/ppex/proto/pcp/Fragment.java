package ppex.proto.pcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.Recycler;

public class Fragment {
    private final Recycler.Handle<Fragment> recyclerHandler;
    private static final Recycler<Fragment> RECYCLER = new Recycler<Fragment>() {
        @Override
        protected Fragment newObject(Handle<Fragment> handle) {
            return new Fragment(handle);
        }
    };

    private Fragment(Recycler.Handle<Fragment> recyclerHandler) {
        this.recyclerHandler = recyclerHandler;
    }

    public static Fragment createFragment(ByteBuf data) {
        Fragment fragment = RECYCLER.get();
        fragment.data = data;
        return fragment;
    }

    public static Fragment createFragment(ByteBufAllocator allocator, int size) {
        Fragment fragment = RECYCLER.get();
        if (size == 0) {
            fragment.data = allocator.ioBuffer(0, 0);
        } else {
            fragment.data = allocator.ioBuffer(size);
        }
        return fragment;
    }

    public void recycler(boolean releaseBuffer) {
        conv = 0;
        cmd = 0;
//        frgid = 0;
        wnd = 0;
        ts = 0;
        sn = 0;
        una = 0;
        resendts = 0;
        rto = 0;
        fastack = 0;
        xmit = 0;
        ackMask = 0;
        if (releaseBuffer)
            data.release();
        data = null;
        recyclerHandler.recycle(this);
    }

    public int conv;
    public byte cmd;                       //命令
//    public short frgid;                    //message分片后的fragmentid
    public int wnd;                        //剩余接收窗口大小
    public long ts;                        //message发送的时间戳
    public long sn;                        //message分片fragment的序号
    public long una;                       //待接收消息序号,接收滑动窗口左端
    public long resendts;                  //下次超时重传时间戳
    public int rto;                        //该分片的超时重传等待时间
    public int fastack;                    //收到ack时计算该分片被跳过的累计次数,即该分片后面都接收到了,达到一定次数,重传它
    public int xmit;                       //发送分片的次数,每发送一次加1
    public long ackMask;
    public ByteBuf data;
    private int ackMaskSize;

    public int getAckMaskSize() {
        return ackMaskSize;
    }

    public void setAckMaskSize(int ackMaskSize) {
        this.ackMaskSize = ackMaskSize;
    }
}