package java.lang;

import java.io.*;

public class ByteBuffer {

    protected int    count;
    protected byte[] data = new byte[32];

    /**
     * Return the count of characters
     */
    int count() {
        return count;
    }

    /**
     * Return byte buffer
     */
    byte[] data() {
        return data;
    }

    /**
     * Clear the buffer
     */
    public void reset() {
        count = 0;
    }

    /**
     * Ensure the buffer is large enough
     */
    private void ensure(int needed) {
        Romizer.assume(needed > 0);
        int remain = data.length - count;
        Romizer.assume(remain >= 0);
        if (remain < needed) {
            int newCapacity = count + needed + 32;
            byte newData[] = new byte[newCapacity];
            System.arraycopy(data, 0, newData, 0, count);
            data = newData;
        }
        Romizer.assume(data.length - count >= needed);
    }

    /**
     * Add a byte to the buffer
     */
    public void addByte(int b) {
        Romizer.assume(b >= 0 && b <= 255);
        ensure(1);
        data[count++] = (byte)b;
    }

    /**
     * Add a string to the buffer
     */
    public void addString(String s) {
        int strlen = s.length();
        int max    = strlen;
        if (max > 0) {
            ensure(max);
            for (int i = 0; i < strlen; i++) {
                int c = s.charAt(i);
                if ((c >= 0x0001) && (c <= 0x007F)) {
                    data[count++] = (byte) c;
                } else if (c > 0x07FF) {
                    ensure(max += 3);
                    data[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                    data[count++] = (byte) (0x80 | ((c >>  6) & 0x3F));
                    data[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
                } else {
                    ensure(max += 2);
                    data[count++] = (byte) (0xC0 | ((c >>  6) & 0x1F));
                    data[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
                }
            }
        }
    }

    /**
     * Add a utf8 string to the buffer
     */
    public int addUTF8(DataInputStream dis) throws IOException {
        final int utflen = dis.readUnsignedShort();
        int rem = utflen;
        ensure(utflen);
        while(rem-- > 0) {
            int ch = dis.read();
            Romizer.assume(ch >= 0);
            data[count++] = (byte)ch;
        }
        return utflen;
    }

    /**
     * Add a '-' to the buffer
     */
    public void addDash() {
        ensure(1);
        data[count++] = '-';
    }

    /**
     * Add an unsigned integer whose value can be encoded in 28 bits or less.
     * The value is encoded in as few bytes as possible with 7 bits of
     * value and one 1 bit of encoding over head per byte. All
     * encoded bytes but the last have a 1 in their most significant bit
     * to signify there are more bytes to follow. The last encoded
     * byte has a 0 in this bit.
     *
     * @param x The value to encode.
     */
    protected void addInt(int x) {
        Romizer.assume(x >= 0 && x < (1 << 27));
        if (x < 128) {
            ensure(1);
            data[count++] = (byte) x;
        } else if (x < 16384) {
            ensure(2);
            data[count++] = (byte) (((x >> 0) & 0x7F) | 0x80);
            data[count++] = (byte)   (x >> 7);
        } else if (x < 2097152) {
            ensure(3);
            data[count++] = (byte) (((x >> 0) & 0x7F) | 0x80);
            data[count++] = (byte) (((x >> 7) & 0x7F) | 0x80);
            data[count++] = (byte)   (x >> 14);
        } else {
            ensure(4);
            data[count++] = (byte) (((x >> 0)  & 0x7F) | 0x80);
            data[count++] = (byte) (((x >> 7)  & 0x7F) | 0x80);
            data[count++] = (byte) (((x >> 14) & 0x7F) | 0x80);
            data[count++] = (byte)   (x >> 21);
        }
    }

    /**
     * Add a ByteBuffer to the buffer
     */
    public void addBuffer(ByteBuffer buf) {
        int    lth  = buf.count();
        byte[] data = buf.data();
        addInt(lth);
        for (int i = 0 ; i < lth ; i++) {
            addByte(data[i] & 0xFF);
        }
    }

    /**
     * Add an access value to the buffer
     */
    public void addAccess(int access) {
        addInt(access);
    }

    /**
     * Add an offset value to the buffer
     */
    public void addOffset(int offset) {
        addInt(offset);
    }

    /**
     * Add a parm value to the buffer
     */
    public void addParm(int type) {
        addInt(type);
    }

    /**
     * Add a type value to the buffer
     */
    public void addType(int type) {
        addParm(type);
    }

    /**
     * Test to see if the buffer equals a string
     */
    public boolean equals(String str) {
        int lth = str.length();
        if (lth != count) {
            return false;
        }
        for (int i = 0 ; i < lth ; i++) {
            if (data[i] != str.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test to see if the buffer starts with a string
     */
    public boolean startsWith(String str) {
        int lth = str.length();
        if (lth > count) {
            return false;
        }
        for (int i = 0 ; i < lth ; i++) {
            if (data[i] != str.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test to see if the buffer ends with a string
     */
    public boolean endsWith(String str) {
        int lth = str.length();
        if (count < lth) {
            return false;
        }
        int pos = count - lth;
        for (int i = 0 ; i < lth ; i++) {
            if (data[pos++] != str.charAt(i)) {
                return false;
            }
        }
        return true;
    }

/*if[DEBUG.LOADER]*/
    public void dump() {
        System.out.println("ByteBuffer");
            for (int i = 0 ; i < count ; i++) {
            int b = data[i] & 0xFF;
            String s = "    "+i;
            while (s.length() < 15) s += " ";
            s += ""+b;
            if (b > ' ' && b < '~') {
                while (s.length() < 25) s += " ";
                s += ""+(char)b;
            }
            System.out.println(s);
        }
    }
/*end[DEBUG.LOADER]*/


}
