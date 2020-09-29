package java.lang;

import java.util.*;

public class PersistentMemoryTest {

    static void test(boolean b, String failMsg) {
        if (!b) {
            System.out.println("test failed: "+failMsg);
        }
    }

    public static void main(String[] args) {
        System.out.println("Running: java.lang.PersistentMemoryTest");
        Runtime rt = Runtime.getRuntime();

        // Test the 'migration' of objects
        PersistentMemory.gc();
        java.util.Hashtable ht = new java.util.Hashtable();
        for (int i = 0; i != 1; ++i) {
            String value = Integer.toString(i);
            ht.put(value, "value"+i);
        }
        PersistentMemory.makePersistent(ht);
        try {
            ht.put(new String("one"), new String("one's value"));
            test(false, "IllegalStoreException should have occurred");
        } catch (IllegalStoreException ise) {
        }
        test(ht.get("one") == null, "There should be no entry for 'one': "+ht.get("one"));
        ht.clear();

        // Test the 'auto migration' of objects
        PersistentMemory.gc();
        Vector roots = null;
        int length = (int)rt.freeMemory() - 8;
        test(rt.totalMemory() < PersistentMemory.totalMemory(), "EEPROM must be bigger than RAM for auto migration tests to work");
        try {
            roots = new Vector();
            PersistentMemory.allowMigration(roots);

            // roots should be migrated at some stage to persistent memory so
            // adding a RAM object should result in an IllegalStoreException
            while (true) {
                roots.addElement(new byte[length]);
            }
        } catch (IllegalStoreException ise) {
        } catch (Throwable t) {
            test(false, "Expected IllegalStoreException, not "+t);
        }
        PersistentMemory.gc();
        roots.removeAllElements();

        // Test the persistent roots table
        PersistentMemory.gc();
        String ramName = new String("name");
        PersistentMemory.put(ramName, ramName);
        test(PersistentMemory.get(ramName) != ramName, "'name' should not be in RAM");
        test(PersistentMemory.get(ramName).equals(ramName), "object not copied into EEPROM properly: "+PersistentMemory.get(ramName));
        test(PersistentMemory.get(ramName) == PersistentMemory.get(ramName), "PersistentMemory.get returns 2 diff objects for same name");

        // Test filling up and garbage collection of persistent memory
        PersistentMemory.gc();
        int count = 0;
        String ramString = new String("persistent object");
        try {
            while (true) {
                count++;
                PersistentMemory.makePersistentCopy(ramString);
            }
        } catch (OutOfMemoryError oome) {
            System.out.println("caught OutOfMemoryError after "+count+" objects");
            PersistentMemory.gc();
            if (count > 0) {
                try {
                    PersistentMemory.makePersistentCopy(ramString);
                } catch (OutOfMemoryError e) {
                    System.err.println("PersistentMemory.gc() didn't free up enough space");
                }
            }
        }
    }
}
