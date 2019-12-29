package ppex.proto.rudp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import ppex.proto.entity.Connection;
import ppex.proto.rudp2.Rudp2;

public interface IOutput {
    Connection getConn();

    Channel getChannel();

    void update(Channel channel);

    void output(ByteBuf data, Rudp rudp, long sn);

    void output(ByteBuf data, Rudp2 rudp2,long sn);
}
