package ppex.utils;

import ppex.proto.entity.Connection;
import ppex.proto.entity.through.Connect;

public class NatTypeUtil {
    public enum NatType {
        UNKNOWN(0),
        SYMMETIC_NAT(1),
        PORT_RESTRICT_CONE_NAT(2),
        RESTRICT_CONE_NAT(3),
        FULL_CONE_NAT(4),
        PUBLIC_NETWORK(5),
        ;
        private int value;

        NatType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
        public static NatType getByValue(int value){
            for (NatType type : values()){
                if (type.getValue() == value){
                    return type;
                }
            }
            return null;
        }
    }


    public static String getNatStrByValue(int value){
        switch (NatType.getByValue(value)){
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

    public static Connect.TYPE getConnectTypeByNatType(Connection from, Connection to){
        if (to.getNatType() == NatType.PUBLIC_NETWORK.getValue()){
            return Connect.TYPE.DIRECT;
        }else if (to.getNatType() == NatType.FULL_CONE_NAT.getValue() || to.getNatType() == NatType.RESTRICT_CONE_NAT.getValue()){
            return Connect.TYPE.HOLE_PUNCH;
        }else if (to.getNatType() == NatType.PORT_RESTRICT_CONE_NAT.getValue() && from.getNatType() == NatType.SYMMETIC_NAT.getValue()){
            return Connect.TYPE.FORWARD;
        }else if (to.getNatType() == NatType.PORT_RESTRICT_CONE_NAT.getValue() && from.getNatType() != NatType.SYMMETIC_NAT.getValue()){
            return Connect.TYPE.HOLE_PUNCH;
        }else if (to.getNatType() == NatType.SYMMETIC_NAT.getValue() && (from.getNatType() == NatType.PUBLIC_NETWORK.getValue() || from.getNatType() == NatType.FULL_CONE_NAT.getValue() || from.getNatType() == NatType.RESTRICT_CONE_NAT.getValue())){
            return Connect.TYPE.REVERSE;
        }else if (to.getNatType() == NatType.SYMMETIC_NAT.getValue() && (from.getNatType() == NatType.PORT_RESTRICT_CONE_NAT.getValue() || from.getNatType() == NatType.SYMMETIC_NAT.getValue())){
            return Connect.TYPE.FORWARD;
        }else {
            return Connect.TYPE.FORWARD;
        }
    }

}
