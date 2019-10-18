package ppex.proto.type;

import java.net.InetSocketAddress;

public class FileTypeMsg {
    private String filename;
    private InetSocketAddress to;
    private long uniqueid;
    private long total;
    private long start;         //start=-1时结束.
    private long end;
    private String data;

    public FileTypeMsg() {
    }

    public FileTypeMsg(String filename, InetSocketAddress to, long uniqueid, long total, long start, long end, String data) {
        this.filename = filename;
        this.to = to;
        this.uniqueid = uniqueid;
        this.total = total;
        this.start = start;
        this.end = end;
        this.data = data;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getUniqueid() {
        return uniqueid;
    }

    public void setUniqueid(long uniqueid) {
        this.uniqueid = uniqueid;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getStart() {
        return start;
    }

    public InetSocketAddress getTo() {
        return to;
    }

    public void setTo(InetSocketAddress to) {
        this.to = to;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
