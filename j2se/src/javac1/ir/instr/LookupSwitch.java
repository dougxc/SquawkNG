/*
 * @(#)LookupSwitch.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import java.util.List;
import javac1.Assert;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;

/**
 * The instruction node for accessing a jump table by key match.
 *
 * @see      TableSwitch
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class LookupSwitch extends Switch {
    /**
     * The list of keys in the jump table.
     */
    private List keys;

    /**
     * Constructs a new instruction for accessing a jump table by key match.
     *
     * @param  scope  scope containing this instruction
     * @param  tag    the key to look up
     * @param  sux    the list of successors
     * @param  keys   the list of keys
     */
    public LookupSwitch(IRScope scope, Instruction tag, List sux, List keys) {
        super(scope, tag, sux);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((keys != null) && (keys.size() == getLength()), "incompatible lengths");
        }
        this.keys = keys;
    }

    /**
     * Returns the key of the entry at the specified index.
     *
     * @param   index  index into the jump table
     * @return  the key at the specified index
     */
    public int keyAt(int index) {
        return ((Integer) keys.get(index)).intValue();
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doLookupSwitch(this);
    }
}
