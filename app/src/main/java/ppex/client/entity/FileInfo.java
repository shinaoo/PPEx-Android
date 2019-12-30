package ppex.client.entity;

public class FileInfo {
    private String name;
    private long length;
    private long seek;
    private String data;

    public FileInfo(String name, long length, long seek, String data) {
        this.name = name;
        this.length = length;
        this.seek = seek;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getSeek() {
        return seek;
    }

    public void setSeek(long seek) {
        this.seek = seek;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
