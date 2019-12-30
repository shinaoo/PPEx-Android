package ppex.androidcomponent.busevent;

public class BusEvent {
    private int type;
    private Object data;

    public enum Type{
        CLIENT_INIT_END(0),

        //暂时设置一些测试
        FILE_GETFILES(4),

        //文字
        TXT_RESPONSE(5),

        //探测阶段
        DETECT_ONE_FROM_SERVER1(6),
        DETECT_ONE_FROM_SERVER2P2(7),
        DETECT_TWO_FROM_SERVER2P1(8),
        DETECT_TWO_FROM_SERVER2P2(9),

        //穿越阶段
        THROUGH_SAVEN_INFO(10),
        THROUGN_GET_CONN_INFO(11),
        THROUGH_SND_CONNECT_PING(12),
        THROUGN_RCV_CONNECT_PONG(13),
        THROUGH_END(14),

        //文件
        FILE_ADD_SUCC(15),
        FILE_ADD_FAIL(16),

        ;
        private int value;
        Type(int value){
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Type getByValue(int value){
            for (Type type:values()){
                if (type.getValue() == value)
                    return type;
            }
            return null;
        }
    }

    public BusEvent(int type){
        this.type = type;
    }

    public BusEvent(int type, Object data) {
        this.type = type;
        this.data = data;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
