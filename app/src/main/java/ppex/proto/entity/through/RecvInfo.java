package ppex.proto.entity.through;

public class RecvInfo {
    public RecvInfo(int type, String recvinfos){
        this.type = type;
        this.recvinfos = recvinfos;
    }

    public RecvInfo(int type) {
        this.type = type;
    }

    public int type;
    public String recvinfos;

    @Override
    public String toString() {
        return "RecvInfo{" +
                "type=" + type +
                ", recvinfos='" + recvinfos + '\'' +
                '}';
    }
}
