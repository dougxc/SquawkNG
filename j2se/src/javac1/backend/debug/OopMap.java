/*
 * @(#)OopMap.java                      1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import javac1.Assert;
import javac1.backend.code.FrameMap;

/**
 * Represents a map of ordinary object pointers. This map specifies which stack
 * and register locations hold ordinary object pointers for a given code
 * position. During garbage collection these locations need to be visited since
 * they represent root pointers.
 *
 * @see      OopMapValue
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class OopMap {
    /**
     * The list of entires in this map.
     */
    private List entries;

    /**
     * The array that keeps track of already used locations.
     */
    private int[] locsUsed;

    /**
     * Constructs a new map of ordinary object pointers.
     *
     * @param  frameSize  current size of the stack frame
     * @param  argCount   number of method arguments
     */
    public OopMap(int frameSize, int argCount) {
        int len = FrameMap.STACK_0 + frameSize + argCount;
        locsUsed = new int[len];
        entries = new ArrayList();
    }

    /**
     * Converts the number of a register. The compiler uses a different register
     * numbering scheme than the rest of the VM. In both systems the lower
     * numbers refer to machine registers. In the compiler, higher numbers are
     * warped by the framesize because the allocator must use a numbering scheme
     * before it knows how big a frame is. Afterwards the VM just wants stack
     * offsets, so it uses unwarped numbers.
     *
     * @param   reg        register number used by the compiler
     * @param   frameSize  current size of the stack frame
     * @param   argCount   number of method arguments
     * @return  register number to be used in the map
     */
    private int compRegToOopMapReg(int reg, int frameSize, int argCount) {
        if (reg < FrameMap.STACK_0) {
            return reg;
        } else if (reg < FrameMap.stack2reg(argCount)) {
            return reg + frameSize;
        } else {
            return reg - argCount;
        }
    }

    /**
     * Inserts the specified location into this map.
     *
     * @param  reg        number of the register
     * @param  type       the content type
     * @param  frameSize  current size of the stack frame
     * @param  argCount   number of method arguments
     * @param  reg2       optional content register
     */
    private void set(int reg, int type, int frameSize, int argCount, int reg2) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(reg < locsUsed.length, "too big reg value for stack size");
        }
        if (locsUsed[reg] != OopMapValue.UNUSED_VALUE) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(locsUsed[reg] == type, "inserted twice with different type");
            }
            return;
        } else {
            locsUsed[reg] = type;
        }
        int oopMapReg = compRegToOopMapReg(reg, frameSize, argCount);
        OopMapValue entry = new OopMapValue(oopMapReg, type);
        if (type == OopMapValue.CALLEE_SAVED_VALUE) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(reg2 < FrameMap.STACK_0, "trying to callee save stack slot");
            }
            entry.setContentReg(reg2);
        } else if (type == OopMapValue.DERIVED_OOP_VALUE) {
            entry.setContentReg(compRegToOopMapReg(reg2, frameSize, argCount));
        }
        entries.add(entry);
    }

    /**
     * Inserts the specified object pointer into this map.
     *
     * @param  reg        number of the register
     * @param  frameSize  current size of the stack frame
     * @param  argCount   number of method arguments
     */
    public void setOop(int reg, int frameSize, int argCount) {
        set(reg, OopMapValue.OOP_VALUE, frameSize, argCount, -1);
    }

    /**
     * Returns the list of entries in this map.
     *
     * @return  list of entries
     */
    public List getEntries() {
        return entries;
    }
}
