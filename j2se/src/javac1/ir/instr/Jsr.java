/*
 * @(#)Jsr.java                         1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.Assert;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.types.ValueType;

/**
 * The instruction node for jumping into a subroutine. This instruction is used
 * together with the {@link Ret} instruction in the implementation of
 * <code>finally</code> clauses.
 *
 * @see      JsrContinuation
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Jsr extends StateSplit {
    /**
     * The first basic block of the subroutine.
     */
    private BlockBegin subroutine;

    /**
     * Constructs a new instruction for jumping into a subroutine.
     *
     * @param  scope        scope containing this instruction
     * @param  subroutine  the subroutine
     */
    public Jsr(IRScope scope, BlockBegin subroutine) {
        super(scope, ValueType.illegalType);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(subroutine != null, "subroutine must exist");
        }
        this.subroutine = subroutine;
    }

    /**
     * Returns the first basic block of the subroutine.
     *
     * @return  the subroutine
     */
    public BlockBegin getSubroutine() {
        return subroutine;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doJsr(this);
    }
}
