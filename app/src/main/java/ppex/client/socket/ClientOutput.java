package ppex.client.socket;

import android.util.Log;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.DatagramPacket;
import ppex.proto.msg.entity.Connection;
import ppex.proto.pcp.Pcp;
import ppex.proto.pcp.PcpOutput;
import ppex.proto.rudp.Output;
import ppex.proto.rudp.Rudp;

public class ClientOutput implements PcpOutput, Output {
    @Override
    public void out(ByteBuf data, Pcp pcp) {
//        String result;
//        if (data.hasArray()){
//            result = new String(data.array(),data.arrayOffset()+data.readerIndex(),data.readableBytes());
//        }else{
//            byte[] bytes = new byte[data.readableBytes()];
//            data.getBytes(data.readerIndex(),bytes);
//            result = new String(bytes,0,data.readableBytes());
//        }
//        LOGGER.info("ClientOutput data:" + result + " readable:" + data.readableBytes());
        Connection connection = pcp.getConnection();
        DatagramPacket tmp = new DatagramPacket(data,connection.getAddress());
        connection.getChannel().writeAndFlush(tmp);
    }

    @Override
    public void output(ByteBuf data, Rudp rudp) {
        Connection connection = rudp.getConnection();
        DatagramPacket tmp = new DatagramPacket(data,connection.getAddress());
        ChannelFuture future = connection.getChannel().writeAndFlush(tmp);
        future.addListener(future1 -> {
            if (future1.isSuccess()){
                Log.e("MyTag","ServerOutput success");
            }else{
                Log.e("MyTag","exeception:" + future1.cause().getLocalizedMessage());
                future1.cause().printStackTrace();
            }
        });

    }
}
