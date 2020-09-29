// NMI's Java Code Viewer 5.1 © 1997-2001 B. Lemaire
// http://njcv.htmlplanet.com - info@njcv.htmlplanet.com

// Copy registered to Evaluation Copy

// Source File Name:   /usr/re/cldc-tck/ws-build/CLDC-TCK_10a/tests/vm/instr/aastore/aastore001/aastore00101m1/aastore00101m1.jasm

import java.io.PrintStream;
import java.util.Random;

public class AAStore {

    static final int mask = 8191;
    static final int delta = 123;
    static final int Len = 1000;
    static Random rand;

    static int nextInt() {
        int i = (rand.nextInt() & 0x1fff) - 123;
        return i;
    }

    static int randomWrite(Object aobj[]) {
        int i = aobj.length;
        char c = '\u3039';
        for(int k = 0; k < i; k++) {
            int j = nextInt();
            try {
                aobj[j] = new Integer(j);
                if(j < 0 || j >= i)
                    return 1;
            }
            catch(ArrayIndexOutOfBoundsException _ex) {
                if(j >= 0 && j < i)
                    return 2;
            }
        }

        return 0;
    }

    static int randomRead(Object aobj[]) {
        int i = aobj.length;
        int j = 54321;
        for(int l = 0; l < i; l++) {
            int k = nextInt();
            try {
                Object obj = aobj[k];
                if(k < 0 || k >= i)
                    return 1;
                if(obj != null && ((Integer)obj).intValue() != k)
                    return 3;
            }
            catch(ArrayIndexOutOfBoundsException _ex) {
                if(k >= 0 && k < i)
                    return 2;
            }
        }

        return 0;
    }

    public static int run(String as[], PrintStream printstream) {
        rand = new Random();
        rand.setSeed(0x75bcd15L);
        Object aobj[] = new Object[1000];
        return randomWrite(aobj) == 0 && randomRead(aobj) == 0 ? 0 : 1;
    }

    public static void main(String args[]) {
        System.exit(run(args, System.out) + 95);
    }

}
