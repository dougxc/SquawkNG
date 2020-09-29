package java.lang;

/**
 * This class provides a number of static methods that comprise the interface
 * to the suites of classes loaded in the system.
 */
public class SuiteManager {

    /** This field needs to be accessible by the romizer. */
    /*private*/ static Suite[] suites = (Suite[])PersistentMemory.getSuites();

    /**
     * Get the Class instance corresponding to a specified suite and class
     * identifier.
     * @param id The suite and class identifier. The suite number is in the high
     * 8 bits of the 'id' and the class number is in the low 8 bits of 'id'.
     * @return the Class instance corresponding to 'id' or null if there isn't one.
     */
    public static Klass lookup(char id) {
        return suites[id>>8].lookup(id&0xFF);
    }

    /**
     * Look up a Class instance based on a specified list of suites and a
     * specified class name in internal form (see Klass.getNameInternal()).
     * @param suiteNames The names of the suites to search.
     * @param internalName The name of the class to find.
     * @return the found Class instance or null if there isn't one.
     */
    public static Klass lookup(String[] suiteNames, String internalName) {
        Klass klass = (suites.length == 0 || suites[0] == null) ? null : suites[0].lookup(internalName);
        if (klass == null) {
            search:
            for (int i = 1; i < suites.length; i++) {
                Suite suite = suites[i];
                if (suite != null) {
                    if (suite.nameMatches(suiteNames)) {
                        klass = suite.lookup(internalName);
                        if (klass != null) {
                            break search;
                        }
                    }
                }
            }
        }
        return klass;
    }

    /**
     * Find an installed class by name.
     * @param name
     * @return
     */
    public static Klass forName(String name) {
//Native.print("SuiteManager.forName ");
//Native.print(name);
        for (int i = 0 ; i < suites.length ; i++) {
            Klass klass = suites[i].forName(name);
            if (klass != null) {
//Native.println(" -- Okay");
                return klass;
            }
        }
//Native.println(" -- Fail");
        return null;
    }

    /**
     * Adjust the reference count of a specified list of suites by a specified
     * value.
     *
     * @param suiteNames The names of the suites whose reference count is to
     * be adjusted.
     * @param value The adjustment value.
     */
    public static void adjustReferences(String[] suiteNames, int value) {
        for (int i = 1 ; i < suites.length ; i++) {
            Suite suite = suites[i];
            if (suite != null && suite.nameMatches(suiteNames)) {
                suite.adjustReferences(value);
            }
        }
    }

    /**
     * Return the next available suite number.
     *
     * @return the next available suite number.
     */
    public static int getFreeSuiteNumber() {
        for (int i = 0 ; i < suites.length ; i++) {
            if (suites[i] == null) {
                return i;
            }
        }
        return suites.length;
    }

    /**
     * Create a new suite and add it to the list of suites in the system.
     *
     * @param id The 8-bit idenitifier of the suite.
     * @param name The name of the suite.
     * @param dependentSuiteNames The names of the other existing suites the
     * new suite binds to.
     * @param classes the classes in the suite.
     */
    public static void addSuite(int number, String name, String[] dependentSuiteNames, Klass[] classes) {

        if (number >= suites.length) {
            Suite[] array = new Suite[suites.length + 1];
            System.arraycopy(suites, 0, array, 0, suites.length);
            suites = (Suite[])PersistentMemory.makePersistentCopy(array);
            // Inform the VM of the newly expanded suite list
            PersistentMemory.setSuites(suites);
        } else {
            if (suites[number] != null) {
                throw new InternalError("cannot overwrite existing suite");
            }
        }

        // Create the new suite
        Suite suite = (Suite)PersistentMemory.makePersistentCopy(new Suite(name, dependentSuiteNames, classes));

        // Atomic update of word in persistent memory
        suites[number] = suite;

/*if[DEBUG.LOADER]*/
        PersistentMemory.gc();
        Suite garbage = new Suite(name, dependentSuiteNames, classes);
        PersistentMemory.makePersistentCopy(garbage);
        PersistentMemory.gc();
/*end[DEBUG.LOADER]*/
    }

    /**
     * Attempt to remove an existing suite from the system. The suite is only
     * removed if its reference count is 0 (i.e. no other suite depends on the
     * suite).
     * @param name The name of the suite to remove.
     * @return whether or not the suite was actually removed.
     */
    public static boolean removeSuite(String name) {
        for (int i = 1 ; i < suites.length ; i++) {
            Suite suite = suites[i];
            if (suite.name.equals(name)) {
                boolean removed = suite.remove();
                if (removed) {
                    suites[i] = null;
                }
                return removed;
            }
        }
        return false;
    }
}
