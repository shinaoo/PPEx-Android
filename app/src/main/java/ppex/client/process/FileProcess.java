package ppex.client.process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import ppex.client.entity.FileInfo;

/**
 * 目前FileProcess的流程是给接收者准备的.
 * 当一个发出ADD Action之后,收到该Action之后,就是创建文件,大小,往回发ADDACK Action.然后等待UPD信息
 * 当收到UPD信息后,根据文件名查找文件,根据里面的File信息,seek和data大小写入.每次都是写入list,等待所有的.
 */
public class FileProcess {
    private FileProcess() {
    }

    private static FileProcess instance = null;

    public static FileProcess getInstance() {
        if (instance == null)
            instance = new FileProcess();
        return instance;
    }

    //都要设置root.
    private File root;

    public void setRoot(File root) {
        this.root = root;
    }

    //接收时
    private String addName;
    private long length;
    private LinkedList<FileInfo> fileInfos;

    private Object fileLock = new Object();
    private volatile boolean fileWait = false;

    public boolean acceptADDAction(FileInfo fileInfo) {
        addName = fileInfo.getName();
        length = fileInfo.getLength();
        File file = new File(root, addName);
        boolean addResult = false;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "rw");
            raf.setLength(length);
            addResult = true;
        } catch (Exception e) {
            addResult = false;
            e.printStackTrace();
        } finally {
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return addResult;
    }

    public void acceptADDACKSUCCAction(FileInfo fileInfo) {

    }

    public void acceptADDACKFAILAction(FileInfo fileInfo) {

    }

    public boolean acceptUPDAction(FileInfo fileInfo) {
        synchronized (fileLock) {
            RandomAccessFile raf = null;
            try {
                while (fileWait) {
                    fileLock.wait();
                }
                fileWait = true;
                File file = new File(root,fileInfo.getName());
                raf = new RandomAccessFile(file,"w");
                raf.seek(fileInfo.getSeek());
                raf.write(fileInfo.getData().getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                if (raf != null){
                    try {
                        raf.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                fileWait = false;
                fileLock.notifyAll();
            }
        }
        return true;
    }

    public void acceptUPDACKSUCCAction(FileInfo fileInfo) {

    }

    public void acceptUPDACKFAILAction(FileInfo fileInfo) {

    }


}
