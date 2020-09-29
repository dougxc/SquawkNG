package com.sun.squawk.translator.util;

import java.util.Enumeration;

/**
 * This class provides a wrapper for an object that augments it with a count.
 * It also implements the Comparer interface and as such enables an array
 * of CountedObjects to be sorted according to their counts.
 */

public class CountedObject {
    /** The count. */
    public int count;
    /** The wrapped object. */
    public Object object;

    /**
     * Constructor.
     * @param object
     */
    public CountedObject(Object object) {
        this.object = object;
        count = 1;
    }

    /**
     * Increment the count.
     */
    public void inc() { count++; }

    /**
     * Set the count for the wrapped object.
     * @param count
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * Copy an enumeration of ContedObjects into an array.
     * @param e The enumeration.
     * @param size The length of the enumeration.
     * @return the array of CountedObjects.
     */
    public static CountedObject[] toArray(Enumeration e, int size) {
        CountedObject[] array = new CountedObject[size];
        int i = 0;
        while (e.hasMoreElements()){
            array[i++] = (CountedObject)e.nextElement();
        }
        return array;
    }

    /**
     * Copy the objects wrapped in an array of CountedObjects into a given array.
     * @param src The array of CountedObjects to copy.
     * @param dst The array to copy into.
     * @boolean reverse If true, copy the array in reverse. This is useful if
     * coArray is a sorted array.
     */
    public static void copyInto(CountedObject[] src, Object[] dst, boolean reverse) {
        int j = (reverse ? src.length - 1 : 0);
        for (int i = 0; i != src.length; i++) {
            dst[j] = src[i].object;
            if (reverse) {
                --j;
            }
            else {
                ++j;
            }
        }
    }

    /** An object that can be passed to Arrays.sort(Object[], Comparer). */
    public static final Comparer COMPARER = new Comparer() {
        /**
         * Imposes an ordering on CountedObjects such that objects with a
         * higher count sort earlier in an array.
         * NOTE: The ordering of objects with equals counts relative to
         * each other is not determined by this method.
         */
        public int compare(Object a, Object b) {
            if (a == b) {
                return 0;
            }
            CountedObject ca = (CountedObject)a;
            CountedObject cb = (CountedObject)b;
            int countA = ca.count;
            int countB = cb.count;
            if (countA < countB) {
                return 1;
            }
            if (countA > countB) {
                return -1;
            }
            return 0;
        }
    };
}
