/*
 * @(#)CachedLocals.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.reg;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Maps the indices of cached local variables to registers.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class CachedLocals {
    /**
     * The mapping of local variables to registers.
     */
    private List mapping;

    /**
     * Specifies which of the cached variables can contain object pointers.
     */
    private BitSet oops;

    /**
     * Constructs a new mapping of local variables to registers.
     */
    public CachedLocals() {
        mapping = new ArrayList();
        oops = new BitSet();
    }

    /**
     * Returns the register that is mapped to the specified index.
     *
     * @param   index  index of the local variable
     * @return  register that the index is mapped to
     */
    public RInfo getCacheReg(int index) {
        if (index < mapping.size()) {
            return (RInfo) mapping.get(index);
        } else {
            return RInfo.NO_RINFO;
        }
    }

    /**
     * Tests if the local variable with the specified index is cached.
     *
     * @param   index  index of the local variable
     * @return  whether or not the variable is cached
     */
    public boolean isLocalCached(int index) {
        return getCacheReg(index).isValid();
    }

    /**
     * Tests if the cached variable at the specified index can contain an object
     * pointer.
     *
     * @param   index  index of the local variable
     * @return  whether or not the variable can contain object pointer
     */
    public boolean isOop(int index) {
        return oops.get(index);
    }

    /**
     * Maps the local variable at the specified index to a register.
     *
     * @param  index  index of the local variable
     * @param  rinfo  register to map local variable to
     * @param  isOop  whether or not the variable can contain object pointer
     */
    public void cacheLocal(int index, RInfo rinfo, boolean isOop) {
        for (int i = index - mapping.size(); i >= 0; i--) {
            mapping.add(RInfo.NO_RINFO);
        }
        mapping.set(index, rinfo);
        if (isOop) {
            oops.set(index);
        } else {
            oops.clear(index);
        }
    }

    /**
     * Returns the mapping of local variables to registers.
     *
     * @return  mapping of local variables to registers
     */
    public List getMapping() {
        return mapping;
    }

    /**
     * Returns the bit set that specifies which of the variables can contain
     * object pointers.
     *
     * @return  bit set of object pointers
     */
    public BitSet getOops() {
        return oops;
    }
}
