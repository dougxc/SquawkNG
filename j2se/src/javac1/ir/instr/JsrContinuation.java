/*
 * @(#)JsrContinuation.java             1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.types.ValueType;

/**
 * The instruction where execution continues after returning from a subroutine.
 *
 * @see      Jsr
 * @see      Ret
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class JsrContinuation extends Instruction {
    /**
     * Constructs a new instruction for continuation after a subroutine.
     *
     * @param  scope  scope containing this instruction
     */
    public JsrContinuation(IRScope scope) {
        super(scope, ValueType.illegalType);
        setPinned(true);
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doJsrContinuation(this);
    }
}
