package ppex.proto.rudp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import ppex.proto.entity.Connection;

public interface IOutput {
    Connection getConn();

    Channel getChannel();

    void update(Channel channel);

    void output(ByteBuf data, Rudp rudp, long sn);
}
