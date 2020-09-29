/*
 * @(#)TableSwitch.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import java.util.List;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;

/**
 * The instruction node for accessing a jump table by index.
 *
 * @see      LookupSwitch
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class TableSwitch extends Switch {
    /**
     * The lowest key in the jump table.
     */
    private int loKey;

    /**
     * Constructs a new instruction node for accessing a jump table by index.
     *
     * @param  scope  scope containing this instruction
     * @param  tag    index into the jump table
     * @param  sux    the list of successors
     * @param  loKey  the lowest key
     */
    public TableSwitch(IRScope scope, Instruction tag, List sux, int loKey) {
        super(scope, tag, sux);
        this.loKey = loKey;
    }

    /**
     * Returns the lowest key in the jump table.
     *
     * @return  the lowest key
     */
    public int getLoKey() {
        return loKey;
    }

    /**
     * Returns the highest key in the jump table.
     *
     * @return  the highest key
     */
    public int getHiKey() {
        return loKey + getLength() - 1;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doTableSwitch(this);
    }
}
