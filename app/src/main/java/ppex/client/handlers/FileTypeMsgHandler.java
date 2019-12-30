package ppex.client.handlers;

import com.alibaba.fastjson.JSON;

import org.greenrobot.eventbus.EventBus;

import ppex.androidcomponent.busevent.BusEvent;
import ppex.client.entity.FileInfo;
import ppex.client.process.FileProcess;
import ppex.proto.msg.type.FileTypeMsg;
import ppex.proto.msg.type.TypeMessage;
import ppex.proto.msg.type.TypeMessageHandler;
import ppex.proto.rudp.IAddrManager;
import ppex.proto.rudp.RudpPack;
import ppex.utils.ByteUtil;
import ppex.utils.MessageUtil;

public class FileTypeMsgHandler implements TypeMessageHandler {

    private FileProcess fileProcess;

    public FileTypeMsgHandler() {
        fileProcess = FileProcess.getInstance();
    }

    @Override
    public void handleTypeMessage(RudpPack rudpPack, IAddrManager addrManager, TypeMessage tmsg) {
        //Client只处理的是接收到的
        FileTypeMsg ftm = JSON.parseObject(tmsg.getBody(), FileTypeMsg.class);
        FileInfo fileInfo = JSON.parseObject(ftm.getData(), FileInfo.class);
        if (ftm.getAction() == FileTypeMsg.ACTION.ADD.ordinal()) {
            boolean ret = fileProcess.acceptADDAction(fileInfo);
            ftm.setAction(ByteUtil.int2byteArr(ret ? FileTypeMsg.ACTION.ADD_ACK_SUCC.ordinal() : FileTypeMsg.ACTION.ADD_ACK_FAIL.ordinal())[0]);
            rudpPack.send2(MessageUtil.filemsg2Msg(ftm));
        } else if (ftm.getAction() == FileTypeMsg.ACTION.ADD_ACK_SUCC.ordinal()) {
//            fileProcess.acceptADDACKSUCCAction(fileInfo);
            EventBus.getDefault().post(new BusEvent(BusEvent.Type.FILE_ADD_SUCC.getValue()));
        } else if (ftm.getAction() == FileTypeMsg.ACTION.ADD_ACK_FAIL.ordinal()) {
            fileProcess.acceptADDACKFAILAction(fileInfo);
        } else if (ftm.getAction() == FileTypeMsg.ACTION.UPD.ordinal()) {
            boolean ret = fileProcess.acceptUPDAction(fileInfo);
            ftm.setAction(ByteUtil.int2byteArr(ret ? FileTypeMsg.ACTION.UPD_ACK_SUCC.ordinal() : FileTypeMsg.ACTION.UPD_ACK_FAIL.ordinal())[0]);
            rudpPack.send2(MessageUtil.filemsg2Msg(ftm));
        } else if (ftm.getAction() == FileTypeMsg.ACTION.UPD_ACK_SUCC.ordinal()) {
            fileProcess.acceptUPDACKSUCCAction(fileInfo);
        } else if (ftm.getAction() == FileTypeMsg.ACTION.UPD_ACK_FAIL.ordinal()) {
            fileProcess.acceptUPDACKFAILAction(fileInfo);
        }


    }
}