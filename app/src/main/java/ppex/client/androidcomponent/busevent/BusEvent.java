package ppex.client.androidcomponent.busevent;

public class BusEvent {
    private int type;
    private Object data;

    public enum Type{
        DETECT_END_OF(0),
        THROUGH_GET_INFO(1),
        THROUGN_CONNECT_END(2),
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
