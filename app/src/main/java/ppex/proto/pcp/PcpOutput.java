package ppex.proto.pcp;

import io.netty.buffer.ByteBuf;

public interface PcpOutput {
    void out(ByteBuf data, Pcp pcp);
}
