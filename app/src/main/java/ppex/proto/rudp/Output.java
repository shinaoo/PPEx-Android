package ppex.proto.rudp;

import io.netty.buffer.ByteBuf;

public interface Output {
    void output(ByteBuf data, Rudp rudp);
}
