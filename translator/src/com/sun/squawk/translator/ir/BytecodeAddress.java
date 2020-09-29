package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.*;
import java.util.Hashtable;

public abstract class BytecodeAddress {

    /**
     * Objects of this class can be sorted. This table imposes
     * an ordering between objects that have the same address based on the exact
     * type of the object. The end points of try blocks are sorted earlier than the
     * start points of try blocks *at the same address* so that there is no overlap
     * of try blocks (which would be contrary to the correct semantics of exception
     * handlers).
     */
//    static private final Hashtable subclassSortOrder = new Hashtable();
//    static {
//        subclassSortOrder.put(TryEndPoint.class,   new Integer(0));
//        subclassSortOrder.put(TryStartPoint.class, new Integer(1));
//        subclassSortOrder.put(Target.class,        new Integer(2));
//    }

    /** IP address in the java bytecode. */
    private final int ip;


    /**
     * Constructor.
     */
    public BytecodeAddress(int ip) {
        Assert.that(ip >= 0);
        this.ip = ip;
    }

    /**
     * Get the ip.
     */
    public int getIP() {
        return ip;
    }

    /**
     * Compare two BytecodeAddress objects based on their primary and secondary keys.
     * @param a The first BytecodeAddress object to be compared.
     * @param b The second BytecodeAddress object to be compared.
     * @return a negative integer, zero, or a positive integer if a is less
     * than, equal to, or greater than b.
     */
    public int compare(Object a, Object b) {
        BytecodeAddress o1 = (BytecodeAddress)a;
        BytecodeAddress o2 = (BytecodeAddress)b;
        int res = 0;
        if (o1 != o2) {
            res = o1.primaryKey() - o2.primaryKey();
            if (res == 0) {
                res = o1.secondaryKey() - o2.secondaryKey();
            }
        }
        return res;
    }

    /**
     * Return the primary key used for sorting.
     * This imposes an ordering such that all objects
     * representing the same address preceed (i.e. return a lower primary key value) all
     * objects representing a higher address. Within the set of objects representing the
     * same address, the primary key orders the objects according to type (as determined
     * by the subclassSortOrder table).
     */
    public final int primaryKey() {
        int typeSortOrder = 0;
        if (this instanceof TryStartPoint) {
            typeSortOrder = 1;
        }
        else if (this instanceof Target) {
            typeSortOrder = 2;
        } else {
            Assert.that(this instanceof TryEndPoint, this.getClass().getName());
        }
        return (ip << 16) + typeSortOrder;
    }

    /**
     * Return the secondary key used for sorting when the primary key for two elements matches.
     */
    public abstract int secondaryKey();

    /**
     * Return the psuedo-opcode representing this address.
     */
    public abstract int opcode();

    /**
     * An object that can be passed to Arrays.sort(Object[], Comparer) to sort an
     * array of BytecodeAddresses.
     */
    public static final Comparer COMPARER = new Comparer() {
        /**
         * Compare two BytecodeAddress objects based on their primary and secondary keys.
         * @param a The first BytecodeAddress object to be compared.
         * @param b The second BytecodeAddress object to be compared.
         * @return a negative integer, zero, or a positive integer if a is less
         * than, equal to, or greater than b.
         */
        public int compare(Object a, Object b) {
            BytecodeAddress o1 = (BytecodeAddress)a;
            BytecodeAddress o2 = (BytecodeAddress)b;
            int res = 0;
            if (o1 != o2) {
                res = o1.primaryKey() - o2.primaryKey();
                if (res == 0) {
                    res = o1.secondaryKey() - o2.secondaryKey();
                }
            }
            return res;
    }};
}
