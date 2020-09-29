package com.sun.squawk.translator.suite;

/**
 * This is the super class for all suite file attributes.
 */
public abstract class SuiteAttribute {

    /**
     * Return the identifier for the sub-type of attribute.
     *
     * @return the identifier for the sub-type of attribute.
     */
    public abstract String getAttributeName();
}