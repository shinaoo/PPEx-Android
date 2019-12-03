package ppex.client.socket;

import android.util.Log;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.DatagramPacket;
import ppex.proto.msg.entity.Connection;
import ppex.proto.rudp.Output;
import ppex.proto.rudp.Rudp;

public class ClientOutput implements Output {

    @Override
    public void output(ByteBuf data, Rudp rudp,long sn) {
        Connection connection = rudp.getConnection();
        DatagramPacket tmp = new DatagramPacket(data,connection.getAddress());
        Channel channel = connection.getChannel();
        if (!channel.isActive() || !channel.isOpen()){
            Log.e("MyTag","channel is close:" );
        }
        ChannelFuture future = channel.writeAndFlush(tmp);
        future.addListener(future1 -> {
            if (future1.isSuccess()){
                Log.e("MyTag","writeAndFlush success:" + sn);
            }else{
                Log.e("MyTag","exeception:" + future1.cause().getLocalizedMessage());
                future1.cause().printStackTrace();
            }
        });

    }
}
