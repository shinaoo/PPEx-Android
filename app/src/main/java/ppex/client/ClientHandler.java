package ppex.client;

import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import ppex.client.rudp.ClientOutput;
import ppex.proto.entity.Connection;
import ppex.proto.rudp.IOutput;
import ppex.proto.rudp.RudpPack;
import ppex.proto.rudp.RudpScheduleTask;

public class ClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);

    private Client client;

    public ClientHandler(Client client) {
        this.client = client;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket packet) throws Exception {
        Log.e("MyTag","read from :" + packet.sender());
        if (packet.sender().equals(Client.getInstance().getAddrLocal())){
            return;
        }
        RudpPack rudpPack = client.getAddrManager().get(packet.sender());
        if (rudpPack != null) {
            rudpPack.getOutput().update(channelHandlerContext.channel());
            rudpPack.rcv2(packet.content());
            return;
        }
        Connection connNew = new Connection("unknown", packet.sender(), "unknown", 0);
        //output需要Channel
        IOutput outputNew = new ClientOutput(channelHandlerContext.channel(), connNew);
//        client.getOutputManager().put(packet.sender(), outputNew);
//        rudpPack = new RudpPack(outputNew, client.getExecutor(), client.getResponseListener());
        rudpPack = RudpPack.newInstance(outputNew,client.getExecutor(),client.getResponseListener(),client.getAddrManager());
        client.getAddrManager().New(packet.sender(), rudpPack);
        rudpPack.rcv2(packet.content());
//        RudpScheduleTask task = new RudpScheduleTask(client.getExecutor(), rudpPack, client.getAddrManager());
//        client.getExecutor().executeTimerTask(task, rudpPack.getInterval());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
