//if[FINALIZATION]
//if[JCARD3.OLDTEST]
package java.lang;

public class FinalizerTest {

    static int count = 5;

    public static void main(String[] args) {
/*if[SYSTEM.STREAMS]*/
        System.out.println("Running: java.lang.FinalizerTest");
/*end[SYSTEM.STREAMS]*/
        System.gc();
        createObjects();
        while (count > 0) {
/*if[SYSTEM.STREAMS]*/
            System.out.println("Calling gc()");
/*end[SYSTEM.STREAMS]*/
            System.gc();
            Thread.yield();
        }
/*if[SYSTEM.STREAMS]*/
        System.out.println("FinalizerTest done");
/*end[SYSTEM.STREAMS]*/
    }

    static void createObjects() {
/*if[SYSTEM.STREAMS]*/
        System.out.println("Createing "+new FinalizerTest());
        System.out.println("Createing "+new FinalizerTest());
        System.out.println("Createing "+new FinalizerTest());
        System.out.println("Createing "+new FinalizerTest());
        System.out.println("Createing "+new FinalizerTest());
/*end[SYSTEM.STREAMS]*/
    }

    protected void finalize() {
/*if[SYSTEM.STREAMS]*/
        System.out.println("finalize() called for "+this);
/*end[SYSTEM.STREAMS]*/
        --count;
    }
}
