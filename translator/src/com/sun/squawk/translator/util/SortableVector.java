package com.sun.squawk.translator.util;

import java.util.*;

/**
 * This is a subclass of Vector that can have its elements sorted by a
 * Comparer object.
 */
public class SortableVector extends Vector {

    /**
     * Constructs an empty vector with the specified initial capacity and
     * capacity increment.
     *
     * @param   initialCapacity     the initial capacity of the vector.
     * @param   capacityIncrement   the amount by which the capacity is
     *                              increased when the vector overflows.
     * @exception IllegalArgumentException if the specified initial capacity
     *            is negative
     */
    public SortableVector(int initialCapacity, int capacityIncrement) {
        super(initialCapacity, capacityIncrement);
    }

    /**
     * Constructs an empty vector with the specified initial capacity.
     *
     * @param   initialCapacity   the initial capacity of the vector.
     * @since   JDK1.0
     */
    public SortableVector(int initialCapacity) {
        super(initialCapacity, 0);
    }

    /**
     * Constructs an empty vector.
     *
     * @since   JDK1.0
     */
    public SortableVector() {
        super(10);
    }

    /**
     * Sort the elements in the vector using a given Comparer.
     * @param comparer
     */
    public void sort(Comparer comparer) {
        Arrays.sort(elementData, 0, elementCount, comparer);
    }
}