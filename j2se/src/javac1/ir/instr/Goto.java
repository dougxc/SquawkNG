/*
 * @(#)Goto.java                        1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.types.ValueType;

/**
 * The instruction node for jumping to the beginning of a basic block
 * unconditionally.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Goto extends BlockEnd {
    /**
     * Constructs a new instruction node for an unconditional jump.
     *
     * @param  scope  scope containing this instruction
     * @param  dest   jump destination block
     */
    public Goto(IRScope scope, BlockBegin dest) {
        super(scope, ValueType.illegalType);
        addSux(dest);
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doGoto(this);
    }
}
