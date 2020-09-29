//if[JCARD3.OLDTEST]
package java.lang;


public class Test {

/*---------------------------------------------------------------------------*\
 *                                   "X" Tests                               *
\*---------------------------------------------------------------------------*/

    private static int two;
    private String sfield;

    public static void main(String[] args) {
        System.out.print("Running: java.lang.Test");
        for (int i = 0; i < args.length; i++) {
            System.out.print(" " + args[i]);
        }
        System.out.println();
        runXTests();
    }

    public static void runXTests() {
        two = 2;

        x2_1();
        x2_2();
        x2_3();
        x2_4();
/*if[FLOATS]*/
        x2_5();
        x2_6();
/*end[FLOATS]*/
        x3();
        x4();
        x5(2);
        x6(2);
        x7(2);
        x8(2);
        x9(2);
        x10();
        x11();
        x12();
        x13();
        x14();
        x15();
        x16();
        x20();
        x30();
        x31();
        x32();
        x33();
        x34();
        x35();
        x36();
        x37();
        x38();
        Native.print("Xpassed\n");
        System.exit(12345);
    }

    static void passed(String name) {
        //Native.print("Test ");
        //Native.print(name);
        //Native.print(" passed\n");
    }

    static void failed(String name) {
        Native.print("Test ");
        Native.print(name);
        Native.print(" failed\n");
    }

    static void result(String name, boolean b) {
        if (b) {
            passed(name);
        } else {
            failed(name);
        }
    }

    static void x2_1() { result("x2_1",Integer.toString(2).equals("2"));     }
    static void x2_2() { result("x2_2",Long.toString(2L).equals("2"));       }
    static void x2_3() { result("x2_3",String.valueOf(true).equals("true")); }
    static void x2_4() { result("x2_4",String.valueOf('2').equals("2"));     }

/*if[FLOATS]*/
    static void x2_5() { result("x2_5",Double.toString(2.0d).equals("2.0")); }
    static void x2_6() { result("x2_6",Float.toString(2.0f).equals("2.0"));  }
/*end[FLOATS]*/

    static void x3() {
       // result("x3", ClassBase.classTable.getClass() == ClassBase.classTable.getClass());
        passed("x3");
    }

    static void x4() {
        passed("x4");
    }

    static void x5(int n) {
        boolean res = false;
        if (n == 2) {
            res = true;
        }
        result("x5", res);
    }

    static void x6(int n) {
        result("x5", n == 2);
    }

    static void x7(int n) {
        result("x7", 5+n == 7);
    }

    static void x8(int n) {
        result("x8", 5*n == 10);
    }

    static void x9(int n) {
        result("x9", -5*n == -10);
    }

    static void x10() {
        result("x10", -5*two == -10);
    }

    static void x11() {
        for (int i = 0 ; i < 10 ; i++) {
            Native.gc();
        }
        passed("x11");
    }

    static void x12() {
        result("x12", fib(20) == 10946);
    }

    public static int fib (int n) {
        if (n == 0) {
            Native.gc();
        }
        if (n<2) {
            return 1;
        }
        int x = fib(n/2-1);
        int y = fib(n/2);
        if (n%2==0) {
            return x*x+y*y;
        } else {
            return (x+x+y)*y;
        }
    }

    static void x13() {
        result("x13",(!(null instanceof Test)));
    }

    static void x14() {
        result("x14",("a string" instanceof String));
    }

    static void x15() {
        boolean res = true;
        try {
            Klass c = (Klass)null;
        } catch (Throwable t) {
            res = false;
        }
        result("x15",res);
    }

    static void x16() {
        boolean res = true;
        try {
            (new String[3])[1] = null;
        } catch (Throwable t) {
            res = false;
        }
        result("x16",res);
    }

    static void x20() {
        Test t = new Test();
        result("x20", t != null);
    }


    static void x30() {
        Object[] o = new Object[1];
        result("x30", o != null);
    }


    static void x31() {
        Object[] o = new Object[1];
        o[0] = o;
        result("x31", o[0] == o);
    }

    static void x32() {
        Object[] o1 = new Object[1];
        Object[] o2 = new Object[1];
        o1[0] = o1;
        System.arraycopy(o1, 0, o2, 0, 1);
        result("x32", o2[0] == o1);
    }

    static void x33() {
        Object[] o1 = new Object[2];
        Object[] o2 = new Object[2];
        o1[0] = o1;
        o1[1] = o2;
        System.arraycopy(o1, 0, o2, 0, 2);
        result("x33", o2[0] == o1 && o2[1] == o2);
    }

    static void x34() {
        Object[] o1 = new Object[2];
        String[] o2 = new String[2];
        o1[0] = "Hello";
        o1[1] = "World";
        System.arraycopy(o1, 0, o2, 0, 2);
        result("x34", o2[0].equals("Hello") && o2[1].equals("World"));
    }

    static void x35() {
        Object o = new Throwable();
        result("x35", o != null);
    }

    static void x36() {
        long l = 0xFF;
        int  i = 0xFF;
        result("x36",(l << 32) == 0xFF00000000L && ((long)i << 32) == 0xFF00000000L);
    }

    static void x37() {
        byte[] o1 = new byte[2];
        o1[0] = (byte)-3;
        result("x37", o1[0] == -3 && o1[1] == 0);
    }

    static Object x38() {
        Object x = null;
        Object o = new Object();
        java.util.Vector v1 = new java.util.Vector();
        v1.addElement(v1);
        java.util.Vector v2 = new java.util.Vector();
        v2.addElement(v2);
        for (int i = 0 ; i < 10000 ; i++) {
            synchronized(o) {
                synchronized(v2) {
                    x = v1.elementAt(0);
                }
                synchronized(v1) {
                    x = v2.elementAt(0);
                }
            }
        }
        return x;
    }

}
