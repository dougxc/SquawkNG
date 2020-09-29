
/**
 * This is the class loaded by the JAM for testing purposes.
 */
public class TestHarness {
    public static void main(String[] args) {
        System.out.println("TestHarness calling java.lang.Test.runXTests()");
        java.lang.Test.runXTests();
        System.out.println("TestHarness finished calling java.lang.Test.runXTests()");

        // Run the AAStore TCK derived test
        Isolate i0 = new Isolate("AAStore", new String[0], "", "AAStore");
        i0.start();

        String delay = "20";
        Isolate i1 = new Isolate("StringLocker", new String[] { "lockStringFor"+delay+"Seconds", delay }, "j2me/classes;samples/classes", "StringLocker:"+delay);
        i1.start();

        delay = "1";
        Isolate i2 = new Isolate("StringLocker", new String[] { "lockStringFor"+delay+"Second", delay }, "j2me/classes;samples/classes", "StringLocker:"+delay);
        i2.start();

        try {
            i0.join();
            i1.join();
            i2.join();
        } catch (InterruptedException ie) {
        }
    }
}
