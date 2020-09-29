package com.sun.squawk.suite;

/**
 * A SuiteHeader instance is a container for the information present in a
 * suite file apart from information for all the classes and method
 * implementations in the suite. It corresponds with all but the last 4
 * items in the 'SuiteFile' structure described in the Suite File format.
 */
public class SuiteHeader {

    /**
     * The access flags for the suite.
     */
    public final int access;

    /**
     * The name of the suite.
     */
    public final String name;

    /**
     * The highest class number assigned to a class in the suite.
     */
    public final int maxType;

    /**
     * The list of suites that this suite binds to.
     */
    public final String[] binds;

    /**
     * The system allocated number for the suite.
     */
    private int number = -1;

    /**
     * Create a new SuiteHeader instance.
     *
     * @param access
     * @param name
     * @param maxType
     * @param binds
     */
    SuiteHeader(int access, String name, int maxType, String[] binds) {
        this.access = access;
        this.name = name;
        this.maxType = maxType;
        this.binds = binds;
    }

    /**
     * Set the system allocated unique number for this suite.
     *
     * @int the system allocated unique number for this suite.
     */
    public void setNumber(int number) {
        Romizer.assume(this.number == -1); // The number should only be set once.
        this.number = number;
    }

    /**
     * Get the system allocated unique number for this suite.
     *
     * @return the system allocated unique number for this suite.
     */
    public int number() {
        return number;
    }
}