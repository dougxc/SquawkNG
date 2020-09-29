/*
 * @(#)Ret.java                         1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.types.ValueType;

/**
 * The instruction node for returning from a subroutine. This instruction is
 * used together with the {@link Jsr} instruction in the implementation of
 * <code>finally</code> clauses.
 *
 * @see      JsrContinuation
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Ret extends BlockEnd {
    /**
     * The index of the local variable that contains the return address.
     */
    private int index;

    /**
     * Constructs a new instruction node for returning from a subroutine.
     *
     * @param  scope  scope containing this instruction
     * @param  index  index of the variable that contains the return address
     */
    public Ret(IRScope scope, int index) {
        super(scope, ValueType.illegalType);
        this.index = index;
    }

    /**
     * Returns the index of the local variable that contains the return address.
     *
     * @return  index of the local variable
     */
    public int getIndex() {
        return index;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doRet(this);
    }
}
