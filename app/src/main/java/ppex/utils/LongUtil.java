package ppex.utils;

public class LongUtil {
    private static long long2Uint(long n){
        return n & 0x00000000FFFFFFFFL;
    }
}
