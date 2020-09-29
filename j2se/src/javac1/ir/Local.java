/*
 * @(#)Local.java                       1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir;

import javac1.backend.reg.RInfo;

/**
 * Represents a local variable that may be cached in a register. This class
 * stores index and access count of a local variable and is instantiated when
 * selecting the most accessed variables for caching. During register allocation
 * the back end keeps track of all registers used. If some registers remain
 * unused at the end, they are assigned to cache local variables. Note that the
 * class has a natural ordering that is inconsistent with equals.
 *
 * @see      ScanBlocks#mostUsedLocals()
 * @see      Loop#cachedLocals
 * @see      javac1.backend.LocalCaching
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Local implements Comparable {
    /**
     * The index of the local variable.
     */
    private int index;

    /**
     * Whether or not the local variable contains an ordinary object pointer.
     */
    private boolean isOop;

    /**
     * The number of instructions accessing the local variable.
     */
    private int accessCount;

    /**
     * The register that caches the value of the local variable.
     */
    private RInfo rinfo;

    /**
     * Constructs a new local variable selected for caching.
     *
     * @param  index        index of the variable
     * @param  isOop        whether or not variable contains object pointer
     * @param  accessCount  access count of the variable
     */
    public Local(int index, boolean isOop, int accessCount) {
        this.index = index;
        this.isOop = isOop;
        this.accessCount = accessCount;
        this.rinfo = null;
    }

    /**
     * Returns the index of the local variable.
     *
     * @return  index of the variable
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns whether or not the variable contains an ordinary object pointer.
     *
     * @return  whether or not variable contains object pointer
     */
    public boolean isOop() {
        return isOop;
    }

    /**
     * Returns the number of instructions accessing the local variable.
     *
     * @return  access count of the variable
     */
    public int getAccessCount() {
        return accessCount;
    }

    /**
     * Sets the register that stores the value of the local variable.
     *
     * @param  reg  register that caches the local variable
     */
    public void setRInfo(RInfo rinfo) {
        this.rinfo = rinfo;
    }

    /**
     * Returns the register that stores the value of the local variable.
     *
     * @return  register that caches the local variable
     */
    public RInfo getRInfo() {
        return rinfo;
    }

    /**
     * Compares this local variable with the specified object for order. This
     * method returns a negative integer, zero, or a positive integer as this
     * local variable is accessed more often than, as often as, or rarer than
     * the specified one.
     *
     * @param   obj  the object to be compared
     * @return  integer that specifies the order of the variables
     * @throws  ClassCastException  if the specified object is not a variable
     */
    public int compareTo(Object obj) {
        return ((Local) obj).getAccessCount() - accessCount;
    }
}
