/*
 * @(#)CodeEmitInfo.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.debug;

import java.util.List;
import javac1.ir.ValueStack;

/**
 * Debug information that is generated during code emission.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class CodeEmitInfo implements Cloneable {
    /**
     * The list of registers that contain ordinary object pointers.
     */
    private List registerOops;

    /**
     * The bytecode index of the associated instruction.
     */
    private int bci;

    /**
     * The list of spill indices that are ordinary object pointers.
     */
    private List spilledOops;

    /**
     * The value stack of the associated instruction.
     */
    private ValueStack stack;

    /**
     * Constructs a new information object about code emission.
     *
     * @param  bci           bytecode index of the instruction
     * @param  spilledOops   list of spill indices that are object pointers
     * @param  stack         value stack of the instruction
     * @param  registerOops  list of registers that contain object pointers
     */
    public CodeEmitInfo(int bci, List spilledOops, ValueStack stack,
            List registerOops) {
        this.bci = bci;
        this.spilledOops = spilledOops;
        this.stack = stack;
        this.registerOops = registerOops;
    }

    /**
     * Constructs a new information object about code emission.
     *
     * @see  #CodeEmitInfo(int, List, ValueStack, List)
     */
    public CodeEmitInfo(int bci, List spilledOops, ValueStack stack) {
        this(bci, spilledOops, stack, null);
    }

    /**
     * Returns the bytecode index of the associated instruction.
     *
     * @return  bytecode index of the instruction
     */
    public int getBci() {
        return bci;
    }

    /**
     * Returns the list of spill indices that are ordinary object pointers.
     *
     * @return  list of spill indices that are object pointers
     */
    public List getSpilledOops() {
        return spilledOops;
    }

    /**
     * Returns the value stack of the associated instruction.
     *
     * @return  value stack of the instruction
     */
    public ValueStack getStack() {
        return stack;
    }

    /**
     * Returns the list of registers that contain ordinary object pointers.
     *
     * @return  list of registers that contain object pointers
     */
    public List getRegisterOops() {
        return registerOops;
    }

    /**
     * Tests if at least one of the registers can contain an object pointer.
     *
     * @return  whether or not any register contains object pointer
     */
    public boolean hasRegisterOops() {
        return (registerOops != null) && (registerOops.size() > 0);
    }
}
