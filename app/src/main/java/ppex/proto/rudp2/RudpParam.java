package ppex.proto.rudp2;

/**
 * udp传输设置,看Frg属性
 * +-----8bit----+-----64bit----+----32bit-----+---32bit---+--64bit---+----64bit----+----64bit----+----32bit----+
 * +     cmd     +     msg id   +     tot      +    wnd    +   ts     +     sn      +     una     +    length   +
 * +-------------+--------------+--------------+-----------+----------+-------------+-------------+-------------+
 *  cmd表示是收还是发.看RudpParam中的CMD_SND,CMD_ACK,CMD_START,CMD_FINISH.
 *  msg id表示该分段属于哪一个msg
 *  tot 表示该msg中的第几个段,倒着表示.0表示是该msg的最后一个段
 *  wnd 表示两边的处理窗口长度,就相当于宽带
 *  ts 表示该段的时间戳
 *  sn 表示该段的编号.一个Rudp开始从0开始一直增加.
 *  una 表示未确认的sn编号.例如发出去了sn为0的一个,但是没有收到ack,那么una就为0
 *  length 表示后面的数据长度,可以使用short简化,因为不会超过MTU_DEFAULT
 *
 *  1+8+4+4+8+8+8+4=45个字节,后面可以根据实际优化msg id,tot,wnd,sn,una,length.都可以变成short以及int
 *
 *  2019-12-24,新改动.无序到达
 *+-----8bit----+-----64bit----+----32bit-----+---32bit---+--64bit---+----64bit----+----32bit----+----32bit----+
 *+     cmd     +     msg id   +     tot      +    all    +   ts     +     sn      +     sndmax     +    length   +
 *+-------------+--------------+--------------+-----------+----------+-------------+-------------+-------------+
 *  与上面一版相比,先将wnd替换为all.就一个msg分成了all段.una也变成32bit,una变成sndmax
 *  1+8+4+4+8+8+4+4 = 41
 *
 *   *  2019-12-25,新改动.还是做顺序到达.到时候再更改断开的情况
 *  +-----8bit----+-----64bit----+----32bit-----+---32bit---+--64bit---+----32bit----+----32bit-------+----32bit----+
 *  +     cmd     +     msg id   +     tot      +    all    +   ts     +     sn      +     una        +    length   +
 *  +-------------+--------------+--------------+-----------+----------+-------------+----------------+-------------+
 *
 * 只是将上面的sndMax改成原来的una.sn改为32为
 * 1+8+4+4+8+4+4+4=37
 *
 *  2019-12-25.开头新增一个tag。表示rudp是刚开始新的没有接收过消息的。
 * +--8bit--+-----8bit----+-----64bit----+----32bit-----+---32bit---+--64bit---+----32bit----+----32bit-------+----32bit----+
 * +  tag   +     cmd     +     msg id   +     tot      +    all    +   ts     +     sn      +     una        +    length   +
 * +--------+-------------+--------------+--------------+-----------+----------+-------------+----------------+-------------+
 *
 * 2019-12-27.新增一个ots。用来确认消息是不是同一个sn...跟2019-12-25一样.ots和tag新增是为了解决new和old的问题
 * +--8bit--+-----8bit----+-----64bit----+----32bit-----+---32bit---+----64bit----+--64bit---+----32bit----+----32bit-------+----32bit----+
 * +  tag   +     cmd     +     msg id   +     tot      +    all    +     ots     +   ts     +     sn      +     una        +    length   +
 * +--------+-------------+--------------+--------------+-----------+-------------+----------+-------------+----------------+-------------+
 *
 * 1+1+8+4+4+8+8+4+4+4=46
 *
 * todo 2019-12-28.修改规则。
 * 全是新的。然后需要通过start指令后，改为旧的才可以通信。
 */

public class RudpParam {

    public static final int NO_DEFILE_RTO = 30;
    public static final int RTO_MIN = 100;
    public static final int RTO_DEFAULT = 200;
    public static final int RTO_MAX = 30000;

    public static final byte CMD_SND = 1;
    public static final byte CMD_ACK = 2;
    public static final byte CMD_START = 3;
    public static final byte CMD_START_ACK = 4;
    public static final byte CMD_FINISH = 5;

    //发送超过20次还没收到ack就相当于连接断开
    public static final int DEAD_LINK = 20;

    //头部数据长度
    public static final int HEAD_LEN = 46;
    //MTU默认长度
    public static final int MTU_DEFAULT = 1446;
    //除去头部数据长度之后剩下长度
    public static final int MTU_BODY =  MTU_DEFAULT - HEAD_LEN;

    //默认窗口
    public static final int WND_SND = 32;
    public static final int WND_RCV = 32;

    //默认周期
    public static final int INTERVAL_DEFAULT = 100;

    //快速重传默认值
    public static final int FASTACK_DEFAULT = 5;

    //遗漏默认操作数
    public static final int LOST_DEFAULT = 5000;

    //Tag默认值
    public static final byte TAG_NEW = 1;
    public static final byte TAG_OLD = 2;

}
