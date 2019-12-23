package ppex.client;

import android.util.Log;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import ppex.client.rudp.ClientOutput;
import ppex.proto.entity.Connection;
import ppex.proto.msg.type.PingTypeMsg;
import ppex.proto.rudp.IOutput;
import ppex.proto.rudp.RudpPack;
import ppex.proto.rudp.RudpScheduleTask;
import ppex.utils.MessageUtil;

public class ClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private Client client;

    public ClientHandler(Client client) {
        this.client = client;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        Log.e("MyTag","channel active");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        client.getAddrManager().getAll().forEach(rudppack -> rudppack.sendFinish());
        super.channelInactive(ctx);
        Log.e("MyTag","Channel inactive");
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        Log.e("MyTag","Channel registered");
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        Log.e("MyTag","CHannel unregistered");
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
        Log.e("MyTag","Channel writabilityChanged");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket packet) throws Exception {
//        Log.e("MyTag","rcv from :" + packet.sender());
        RudpPack rudpPack = client.getAddrManager().get(packet.sender());
        if (rudpPack != null) {
            rudpPack.getOutput().update(channelHandlerContext.channel());
//            client.getOutputManager().get(packet.sender()).update(channelHandlerContext.channel());
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

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        IdleState state = ((IdleStateEvent)evt).state();
        if (state == IdleState.WRITER_IDLE){
            handleWriteIdle();
        }
    }

    private void handleWriteIdle(){
        PingTypeMsg pingTypeMsg = new PingTypeMsg();
        this.client.getAddrManager().getAll().forEach(rudpPack -> rudpPack.write(MessageUtil.pingMsg2Msg(pingTypeMsg)));
    }
}
