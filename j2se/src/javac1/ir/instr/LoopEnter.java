/*
 * @(#)LoopEnter.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.types.ValueType;

/**
 * The instruction node for the start of a loop.
 *
 * @see      LoopExit
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class LoopEnter extends AccessLoop {
    /**
     * Constructs a new instruction node for the start of a loop.
     *
     * @param  scope   scope containing this instruction
     * @param  loopId  the identification number of the loop
     */
    public LoopEnter(IRScope scope, int loopId) {
        super(scope, loopId);
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doLoopEnter(this);
    }
}
