package java.lang;

public class Suite {

    /*@vmaccessed: eeprom */

    /**
     * The classes in the suite.
     */
    public final Klass[] classes; /*@vmaccessed: read */

    /**
     * The name of the suite.
     */
    public final String name; /*@vmaccessed: read */

    /**
     * The other suites to which this one is bound or null if this is the
     * primordial suite.
     */
    public final String[] dependentSuiteNames; /*@vmaccessed: read */

    /**
     * Count of references to this suite from other suites that are bound
     * to it.
     */
    int references;

    /*
     * Constructor
     */
    Suite(String name, String[] dependentSuiteNames, Klass[] classes) {
        this.name = name;
        this.dependentSuiteNames = dependentSuiteNames;
        this.classes = classes;
        SuiteManager.adjustReferences(dependentSuiteNames, 1);
    }


    /**
     * Get the Class instance from this suite corresponding to a specified
     * class number.
     * @param cno The 8-bit class number.
     * @return the Class instance corresponding to 'cno' or null if there isn't one.
     */
    Klass lookup(int cno) {
        if (cno < 0 || cno >= classes.length) {
            return null;
        }
        return classes[cno];
    }

    /**
     * Get the Class instance from this suite corresponding to a specified
     * class name in internal form.
     * @param internalName The name (int internal form) of the class to find.
     * @return the Class instance corresponding to 'internalName' or null
     * if there isn't one.
     */
    Klass lookup(String internalName) {
        for (int i = 1 ; i < classes.length ; i++) {
            Klass klass = classes[i];
            if (klass != null && klass.equalsInternalName(internalName)) {
                return klass;
            }
        }
        return null;
    }

    /**
     * Find a class in this suite by name.
     * @param name
     * @return
     */
    public Klass forName(String name) {
        for (int i = 0 ; i < classes.length ; i++) {
            Klass klass = classes[i];
            if (klass != null && klass.equalsInternalName(name)) {
                return klass;
            }
        }
        return null;
    }

    /**
     * Determine if this suite's name is present in a specified list of
     * suite names.
     * @param suiteNames The list to test.
     * @return true if this suite's name is present in 'suiteNames'.
     */
    boolean nameMatches(String[] suiteNames) {
        for (int i = 0 ; i < suiteNames.length ; i++) {
            if (suiteNames[i].equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adjust the reference count of this suite by a specified value.
     * @param value the adjustment value.
     */
    void adjustReferences(int value) {
        try {
            // Potentially
            references += value;
        } catch (IllegalStoreException ise) {
            // this must be a suite in ROM
        }
    }

    /**
     * Attempt to remove this suite for the system. This will only be successful
     * if there are no references to this suite from another suite. If the suite
     * is actually removed, then the references counts of all other suites
     * referenced by this one are decremented by one.
     * @return whether or not the suite was removed.
     */
    boolean remove() {
        if (references == 0) {
            SuiteManager.adjustReferences(dependentSuiteNames, -1);
            return true;
        } else {
            return false;
        }
    }
}


