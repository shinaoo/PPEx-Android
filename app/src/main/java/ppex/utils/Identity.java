package ppex.utils;

/**
 * 记录身份,目前有Client,Server1,Server2
 */
public class Identity {
    public enum Type{
        CLIENT,
        SERVER1,
        SERVER2_PORT1,
        SERVER2_PORT2,
    }
    public static int INDENTITY = Type.CLIENT.ordinal();
}
