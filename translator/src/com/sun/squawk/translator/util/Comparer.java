
package com.sun.squawk.translator.util;

public interface Comparer {
    /**
     * Compares its two arguments for order.  Returns a negative integer,
     * zero, or a positive integer as the first argument is less than, equal
     * to, or greater than the second.
     */
    public int compare(Object a, Object b);
}