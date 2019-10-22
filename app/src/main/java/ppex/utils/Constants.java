package ppex.utils;

import java.util.Arrays;

public class Constants {
    public static byte MSG_VERSION = 1;

    public static int SEND_BUFSIZE=1024;
    public static int RECV_BUFSIZE=1024;

    public static String SERVER_LOCAL_IP = "127.0.0.1";
    public static String SERVER_LOCAL = "localhost";
    public static String SERVER_HOST1 = "119.139.198.123";
    public static String SERVER_HOST2 = "116.24.65.149";
//    public static String SERVER_HOST1 = "10.5.11.162";
//    public static String SERVER_HOST2 = "10.5.11.55";

    public static int PORT1 = 9123;
    public static int PORT2 = 9124;
    public static int PORT3 = 9125;

    public enum NATTYPE{
        UNKNOWN(0),
        SYMMETIC_NAT(1),
        PORT_RESTRICT_CONE_NAT(2),
        RESTRICT_CONE_NAT(3),
        FULL_CONE_NAT(4),
        PUBLIC_NETWORK(5),
        ;
        private int value;
        NATTYPE(int value){
            this.value =value;
        }
        public Integer getValue() {
            return value;
        }
        public static NATTYPE getByValue(int value){
            for (NATTYPE type : values()){
                if (type.getValue() == value)
                    return type;
            }
            return null;
        }
        public static void printValus(){
            Arrays.stream(values()).forEach( type ->{
                System.out.println("value:" + type.getValue() + " ordinal:" + type.ordinal());
            });
        }
    }

    public static String getNatStrByValue(int value){
        switch (NATTYPE.getByValue(value)){
            case UNKNOWN:
                return "UNKNOWN";
            case SYMMETIC_NAT:
                return "SYMMETIC_NAT";
            case PORT_RESTRICT_CONE_NAT:
                return "PORT_RESTRICT_CONE_NAT";
            case RESTRICT_CONE_NAT:
                return "RESTRICT_CONE_NAT";
            case FULL_CONE_NAT:
                return "FULL_CONE_NAT";
            case PUBLIC_NETWORK:
                return "PUBLIC_NETWORK";
            default:
                return "";
        }
    }
}
