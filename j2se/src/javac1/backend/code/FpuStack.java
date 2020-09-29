/*
 * @(#)FpuStack.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.code;

import javac1.Assert;
import javac1.backend.reg.RegAlloc;

/**
 * Simulates the FPU register stack. This class simulates pushing and popping
 * of elements on the FPU stack and maintains the mapping of floating-point
 * registers to stack offsets. This way the offsets between FPU registers are
 * known during code generation.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class FpuStack {
    /**
     * The stack offset for each register or 0 if it is not on the stack.
     */
    private int[] offset;

    /**
     * The number of elements on the stack.
     */
    private int count;

    /**
     * Constructs a new empty FPU stack.
     */
    public FpuStack() {
        offset = new int[RegAlloc.NUM_FPU_REGS];
        clear();
    }

    /**
     * Returns the number of the register at the specified offset.
     *
     * @param   off  offset from top of stack
     * @return  number of the register at the specified offset
     */
    private int findReg(int off) {
        int goal = count - off;
        for (int i = 0; i < offset.length; i++) {
            if (offset[i] == goal) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Swaps the registers with the specified numbers on the stack.
     *
     * @param  a  number of one register
     * @param  b  number of the other register
     */
    private void swap(int a, int b) {
        int temp = offset[a];
        offset[a] = offset[b];
        offset[b] = temp;
    }

    /**
     * Brings the specified register on the top of the stack.
     *
     * @param   rnr  number of the register to be brought on top
     * @return  old offset of the specified register
     */
    public int bringOnTop(int rnr) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(offset[rnr] != 0, "register is not on the stack");
        }
        if (offset[rnr] != count) {
            int tos = findReg(0);
            swap(tos, rnr);
            return count - offset[tos];
        }
        return 0;
    }

    /**
     * Pushes the register with the specified number onto the stack.
     *
     * @param  rnr  number of the register to be pushed
     */
    public void push(int rnr) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(count != offset.length, "stack is full");
        }
        count++;
        offset[rnr] = count;
    }

    /**
     * Pops the register with the specified number off the stack.
     *
     * @param  rnr  number of the register to be popped
     */
    public void pop(int rnr) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(count != 0, "stack is empty");
            Assert.that(offset[rnr] == count, "register is not on top");
        }
        count--;
        offset[rnr] = 0;
    }

    /**
     * Swaps the two topmost registers on the stack.
     */
    public void swapTwoOnTop() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(count >= 2, "not enough space");
        }
        swap(findReg(0), findReg(1));
    }

    /**
     * Returns the offset of the register with the specified number.
     *
     * @param   rnr  number of the register
     * @return  offset of the specified register
     */
    public int getOffset(int rnr) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(offset[rnr] != 0, "register is not on the stack");
        }
        return count - offset[rnr];
    }

    /**
     * Tests if the specified register is at the desired position.
     *
     * @param   rnr  number of the register
     * @param   off  offset from top of stack
     * @return  whether or not the register is at the specified offset
     */
    public boolean isStackPos(int rnr, int off) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(off < count, "wrong offset");
        }
        return offset[rnr] == count - off;
    }

    /**
     * Tests if the FPU stack is empty.
     *
     * @return  whether or not the stack is empty
     */
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * Clears the stack of floating-point registers.
     */
    public void clear() {
        for (int i = 0; i < offset.length; i++) {
            offset[i] = 0;
        }
        count = 0;
    }
}
