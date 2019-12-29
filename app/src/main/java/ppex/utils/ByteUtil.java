package ppex.utils;

public class ByteUtil {

    public static byte[] int2byteArr(int num) {
        int temp = num;
        byte[] b = new byte[4];
        for (int i = 0; i < b.length; i++) {
            b[i] = new Integer(temp & 0xff).byteValue();//
            temp = temp >> 8; // 向右移8位
        }
        return b;
    }

    public static byte[] long2ByteArr(long num) {
        long temp = num;
        byte[] b = new byte[8];
        for (int i = 0; i < b.length; i++) {
            b[i] = new Long(temp & 0xff).byteValue();//
            temp = temp >> 8; // 向右移8位
        }
        return b;
    }

    public static int bytearr2Int(byte[] b) {
        int s = 0;
        int s0 = b[0] & 0xff;// 最低位
        int s1 = b[1] & 0xff;
        int s2 = b[2] & 0xff;
        int s3 = b[3] & 0xff;
        s3 <<= 24;
        s2 <<= 16;
        s1 <<= 8;
        s = s0 | s1 | s2 | s3;
        return s;
    }

    public static long bytearr2Long(byte[] b) {
        long s = 0;
        long s0 = b[0] & 0xff;// 最低位
        long s1 = b[1] & 0xff;
        long s2 = b[2] & 0xff;
        long s3 = b[3] & 0xff;
        long s4 = b[4] & 0xff;// 最低位
        long s5 = b[5] & 0xff;
        long s6 = b[6] & 0xff;
        long s7 = b[7] & 0xff;

        s1 <<= 8;
        s2 <<= 16;
        s3 <<= 24;
        s4 <<= 8 * 4;
        s5 <<= 8 * 5;
        s6 <<= 8 * 6;
        s7 <<= 8 * 7;
        s = s0 | s1 | s2 | s3 | s4 | s5 | s6 | s7;
        return s;
    }

    //以一个mtu的值将src分割成几份byte[]
    public static byte[][] splitArr(byte[] src, int mtu) {
        int count = 0;
        if (src.length < mtu) {
            count = 1;
        } else {
            count = (src.length + mtu - 1) / mtu;
        }
        if (count == 0)
            count = 1;
        int position = 0;
        byte[][] result = new byte[count][];
        for (int i = 0; i < count; i++) {
            if (i == count - 1) {
                result[i] = new byte[src.length - mtu * i];
            } else {
                result[i] = new byte[mtu];
            }
            System.arraycopy(src, mtu * i, result[i], 0, result[i].length);
        }
        return result;
    }

    public static byte[] mergeArr(byte[][] src, int mtu) {
        int length = 0;
        byte[] result;
        for (int i = 0; i < src.length; i++) {
            length += src[i].length;
        }
        result = new byte[length];
        for (int i = 0;i < src.length;i++){
            System.arraycopy(src[i],0,result,mtu*i,src[i].length);
        }
        return result;
    }

    

}


