package ppex.client.rudp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.DatagramPacket;
import ppex.proto.entity.Connection;
import ppex.proto.rudp.IOutput;
import ppex.proto.rudp.Rudp;

public class ClientOutput implements IOutput {

    private Channel channel;
    private Connection connection;

    public ClientOutput(Channel channel, Connection connection) {
        this.channel = channel;
        this.connection = connection;
    }

    @Override
    public Connection getConn() {
        return this.connection;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public void update(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void output(ByteBuf data, Rudp rudp, long sn) {
        DatagramPacket packet = new DatagramPacket(data, connection.getAddress());
        if (channel.isActive() && channel.isOpen()) {
            ChannelFuture fu = channel.writeAndFlush(packet);
            fu.addListener(future -> {
                if (future.isSuccess()) {
                } else {
                    System.out.println("channel writeandflush failed");
                    future.cause().printStackTrace();
                }
            });
        } else {
            System.out.println("channel is close");
        }
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }
}
