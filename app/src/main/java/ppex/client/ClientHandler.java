package ppex.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import ppex.client.rudp.ClientOutput;
import ppex.proto.entity.Connection;
import ppex.proto.rudp.IOutput;
import ppex.proto.rudp.RudpPack;
import ppex.proto.rudp.RudpScheduleTask;

public class ClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private Client client;

    public ClientHandler(Client client) {
        this.client = client;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket packet) throws Exception {
        System.out.println("read from :" + packet.sender());
        RudpPack rudpPack = client.getAddrManager().get(packet.sender());
        if (rudpPack != null) {
            client.getOutputManager().get(packet.sender()).update(channelHandlerContext.channel());
            rudpPack.read(packet.content());
            return;
        }
        Connection connNew = new Connection("unknown", packet.sender(), "unknown", 0);
        //output需要Channel
        IOutput outputNew = new ClientOutput(channelHandlerContext.channel(), connNew);
        client.getOutputManager().put(packet.sender(), outputNew);
        rudpPack = new RudpPack(outputNew, client.getExecutor(), client.getResponseListener());
        client.getAddrManager().New(packet.sender(), rudpPack);
        rudpPack.read(packet.content());
        RudpScheduleTask task = new RudpScheduleTask(client.getExecutor(), rudpPack, client.getAddrManager());
        client.getExecutor().executeTimerTask(task, rudpPack.getInterval());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
