/*
 * @(#)ScanBlocks.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javac1.Assert;
import javac1.ir.instr.*;
import javac1.ir.types.ValueType;

/**
 * Analyzes a set of blocks for various properties.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class ScanBlocks implements BlockClosure {
    /**
     * Whether or not single-precision floating-point values are used.
     */
    private boolean hasFloats;

    /**
     * Whether or not double-precision floating-point values are used.
     */
    private boolean hasDoubles;

    /**
     * Whether or not methods or subroutines are called.
     */
    private boolean hasCalls;

    /**
     * Whether or not subroutines are called.
     */
    private boolean hasJsr;

    /**
     * Whether or not slow cases are contained in the list of blocks.
     */
    private boolean hasSlowCases;

    /**
     * Whether or not unloaded or uninitialized fields are accessed.
     */
    private boolean hasClassInit;

    /**
     * Whether or not a value is stored into the local variable at index 0.
     */
    private boolean hasStore0;

    /**
     * The list of access counts for integer variables.
     */
    private List intAccessCount;

    /**
     * The list of access counts for long integer variables.
     */
    private List longAccessCount;

    /**
     * The list of access counts for single-precision floating-point variables.
     */
    private List floatAccessCount;

    /**
     * The list of access counts for double-precision floating-point variables.
     */
    private List doubleAccessCount;

    /**
     * The list of access counts for object variables.
     */
    private List objAccessCount;

    /**
     * The list of access counts for address variables.
     */
    private List addressAccessCount;

    /**
     * Constructs a new block scanner.
     */
    public ScanBlocks() {
        hasFloats = false;
        hasDoubles = false;
        hasCalls = false;
        hasJsr = false;
        hasSlowCases = false;
        hasClassInit = false;
        hasStore0 = false;
        intAccessCount = new ArrayList();
        longAccessCount = new ArrayList();
        floatAccessCount = new ArrayList();
        doubleAccessCount = new ArrayList();
        objAccessCount = new ArrayList();
        addressAccessCount = new ArrayList();
    }

    /**
     * Increments the access count of the specified local variable.
     *
     * @param  list   list of access counts for variables of the same type
     * @param  index  index of the local variable
     * @param  delta  value to increment current count by
     */
    private void incCount(List list, int index, int delta) {
        for (int i = index - list.size(); i >= 0; i--) {
            list.add(new Integer(0));
        }
        int count = ((Integer) list.get(index)).intValue() + delta;
        list.set(index, new Integer(count));
    }

    /**
     * Returns the access count of the specified local variable.
     *
     * @param  list   list of access counts for variables of the same type
     * @param  index  index of the local variable
     */
    private int countAt(List list, int index) {
        if (index < list.size()) {
            return ((Integer) list.get(index)).intValue();
        } else {
            return 0;
        }
    }

    /**
     * Tests if none of the scanned instructions accesses the specified
     * variable.
     *
     * @param  list   list of access counts for variables of the same type
     * @param  index  index of the local variable
     */
    private boolean isZero(List list, int index) {
        return countAt(list, index) == 0;
    }

    /**
     * Increments the access count for the specified variable and type.
     *
     * @param  index  index of the local variable
     * @param  tag    the type tag of the variable
     * @param  delta  value to increment current count by
     */
    private void accumulateAccess(int index, int tag, int delta) {
        switch (tag) {
        case ValueType.intTag:
            incCount(intAccessCount, index, delta);
            break;
        case ValueType.longTag:
            incCount(longAccessCount, index, delta);
            break;
        case ValueType.floatTag:
            incCount(floatAccessCount, index, delta);
            break;
        case ValueType.doubleTag:
            incCount(doubleAccessCount, index, delta);
            break;
        case ValueType.objectTag:
            incCount(objAccessCount, index, delta);
            break;
        case ValueType.addressTag:
            incCount(addressAccessCount, index, delta);
            break;
        default:
            Assert.shouldNotReachHere();
            break;
        }
    }

    /**
     * Scans the instructions of the specified block and updates the internal
     * state.
     *
     * @param  block  basic block to be scanned
     */
    public void doBlock(BlockBegin block) {
        for (Instruction x = block; x != null; x = x.getNext()) {
            int tag = x.getType().getTag();
            if (tag == ValueType.floatTag) {
                hasFloats = true;
            } else if (tag == ValueType.doubleTag) {
                hasDoubles = true;
            }
            if (x instanceof Invoke) {
                hasCalls = true;
            } else if (x instanceof Jsr) {
                hasJsr = true;
                hasCalls = true;
            } else if ((x instanceof AccessMonitor) || (x instanceof NewArray)
                    || (x instanceof NewInstance) || (x instanceof TypeCheck)) {
                hasSlowCases = true;
            } else if ((x instanceof Intrinsic)
                    && (((Intrinsic) x).getIntrinsicId() == Intrinsic.ARRAYCOPY)) {
                hasSlowCases = true;
            } else if ((x instanceof AccessField) && (!((AccessField) x).isLoaded()
                    || !((AccessField) x).isInitialized())) {
                hasClassInit = true;
            } else if (x instanceof AccessLocal) {
                int index = ((AccessLocal) x).getIndex();
                int useCount = (x instanceof StoreLocal) ? 1 : x.getUseCount();
                accumulateAccess(index, tag, useCount);
                if (x.getType().isDoubleWord()) {
                    accumulateAccess(index + 1, tag, useCount);
                }
                if ((x instanceof StoreLocal) && (index == 0)) {
                    hasStore0 = true;
                }
            } else if ((x instanceof StoreIndexed) && (tag == ValueType.objectTag)) {
                hasSlowCases = true;
            }
        }
    }

    /**
     * Tests if single-precision floating-point values are used.
     *
     * @return  whether or not single-precision floating-point values are used
     */
    public boolean hasFloats() {
        return hasFloats;
    }

    /**
     * Tests if double-precision floating-point values are used.
     *
     * @return  whether or not double-precision floating-point values are used
     */
    public boolean hasDoubles() {
        return hasDoubles;
    }

    /**
     * Tests if methods or subroutines are called.
     *
     * @return  whether or not methods are called
     */
    public boolean hasCalls() {
        return hasCalls;
    }

    /**
     * Tests if subroutines are called.
     *
     * @return  whether or not subroutines are called
     */
    public boolean hasJsr() {
        return hasJsr;
    }

    /**
     * Tests if the scanned blocks contain slow cases.
     *
     * @return  whether or not slow cases are contained
     */
    public boolean hasSlowCases() {
        return hasSlowCases;
    }

    /**
     * Tests if unloaded or uninitialized fields are accessed.
     *
     * @return  whether or not unloaded or uninitialized fields are accessed
     */
    public boolean hasClassInit() {
        return hasClassInit;
    }

    /**
     * Tests if a value is stored into the local variable at index 0.
     *
     * @return  whether or not a value is stored into the variable at index 0
     */
    public boolean hasStore0() {
        return hasStore0;
    }

    /**
     * Tests if local variables can be cached in registers.
     *
     * @return  whether or not local variables can be cached
     */
    public boolean canCacheLocals() {
        return !(hasCalls || hasClassInit || hasSlowCases);
    }

    /**
     * Tests if the receiver of the method can be cached in a register.
     *
     * @return  whether or not the receiver can be cached
     */
    public boolean canCacheReceiver() {
        return !(hasCalls || hasClassInit || hasSlowCases || hasStore0);
    }

    /**
     * Returns the access count of the integer variable at the specified index.
     *
     * @param   index  index of the local variable
     * @return  access count of the variable
     */
    public int intCountAt(int index) {
        return countAt(intAccessCount, index);
    }

    /**
     * Returns the access count of the long integer variable at the specified
     * index.
     *
     * @param   index  index of the local variable
     * @return  access count of the variable
     */
    public int longCountAt(int index) {
        return countAt(longAccessCount, index);
    }

    /**
     * Returns the access count of the single-precision floating-point variable
     * at the specified index.
     *
     * @param   index  index of the local variable
     * @return  access count of the variable
     */
    public int floatCountAt(int index) {
        return countAt(floatAccessCount, index);
    }

    /**
     * Returns the access count of the double-precision floating-point variable
     * at the specified index.
     *
     * @param   index  index of the local variable
     * @return  access count of the variable
     */
    public int doubleCountAt(int index) {
        return countAt(doubleAccessCount, index);
    }

    /**
     * Returns the access count of the object variable at the specified index.
     *
     * @param   index  index of the local variable
     * @return  access count of the variable
     */
    public int objCountAt(int index) {
        return countAt(objAccessCount, index);
    }

    /**
     * Returns the access count of the address variable at the specified index.
     *
     * @param   index  index of the local variable
     * @return  access count of the variable
     */
    public int addressCountAt(int index) {
        return countAt(addressAccessCount, index);
    }

    /**
     * Tests if only integer values are stored at the specified index.
     *
     * @param   index  index of the local variable
     * @return  whether or not only integers are stored
     */
    public boolean isIntOnly(int index) {
        return isZero(longAccessCount, index) && isZero(floatAccessCount, index)
            && isZero(doubleAccessCount, index) && isZero(objAccessCount, index)
            && isZero(addressAccessCount, index);
    }

    /**
     * Tests if only long integer values are stored at the specified index.
     *
     * @param   index  index of the local variable
     * @return  whether or not only long integers are stored
     */
    public boolean isLongOnly(int index) {
        return isZero(intAccessCount, index) && isZero(floatAccessCount, index)
            && isZero(doubleAccessCount, index) && isZero(objAccessCount, index)
            && isZero(addressAccessCount, index);
    }

    /**
     * Tests if only single-precision floating-point values are stored at the
     * specified index.
     *
     * @param   index  index of the local variable
     * @return  whether only single-precision floating-point values are stored
     */
    public boolean isFloatOnly(int index) {
        return isZero(intAccessCount, index) && isZero(longAccessCount, index)
            && isZero(doubleAccessCount, index) && isZero(objAccessCount, index)
            && isZero(addressAccessCount, index);
    }

    /**
     * Tests if only double-precision floating-point values are stored at the
     * specified index.
     *
     * @param   index  index of the local variable
     * @return  whether only double-precision floating-point values are stored
     */
    public boolean isDoubleOnly(int index) {
        return isZero(intAccessCount, index) && isZero(longAccessCount, index)
            && isZero(floatAccessCount, index) && isZero(objAccessCount, index)
            && isZero(addressAccessCount, index);
    }

    /**
     * Tests if only object values are stored at the specified index.
     *
     * @param   index  index of the local variable
     * @return  whether or not only objects are stored
     */
    public boolean isObjOnly(int index) {
        return isZero(intAccessCount, index) && isZero(longAccessCount, index)
            && isZero(floatAccessCount, index) && isZero(doubleAccessCount, index)
            && isZero(addressAccessCount, index);
    }

    /**
     * Tests if only address values are stored at the specified index.
     *
     * @param   index  index of the local variable
     * @return  whether or not only addresses are stored
     */
    public boolean isAddressOnly(int index) {
        return isZero(intAccessCount, index) && isZero(longAccessCount, index)
            && isZero(floatAccessCount, index) && isZero(doubleAccessCount, index)
            && isZero(objAccessCount, index);
    }

    /**
     * Sorts all local variables by their usage. Access frequency is stored
     * separately for objects and integers.
     *
     * @return  sorted list of the most used variables
     * @see     Local
     */
    public List mostUsedLocals() {
        int len = Math.max(intAccessCount.size(), objAccessCount.size());
        List locals = new ArrayList();
        for (int i = 0; i < len; i++) {
            int icnt = intCountAt(i);
            int ocnt = objCountAt(i);
            if ((icnt > 0) && isIntOnly(i)) {
                locals.add(new Local(i, false, icnt));
            } else if ((ocnt > 0) && isObjOnly(i)) {
                locals.add(new Local(i, true, ocnt));
            }
        }
        Collections.sort(locals);
        return locals;
    }
}
