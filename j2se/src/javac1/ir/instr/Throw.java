/*
 * @(#)Throw.java                       1.10 02/11/27
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
 * The instruction node for throwing an exception. It leads to the search for
 * the first exception handler that matches the class of the exception.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Throw extends BlockEnd {
    /**
     * The exception to be thrown.
     */
    private Instruction exception;

    /**
     * Constructs a new instruction node for throwing an exception.
     *
     * @param  scope      scope containing this instruction
     * @param  exception  exception to be thrown
     */
    public Throw(IRScope scope, Instruction exception) {
        super(scope, ValueType.illegalType);
        this.exception = exception;
    }

    /**
     * Returns the exception to be thrown.
     *
     * @return  exception to be thrown
     */
    public Instruction getException() {
        return exception;
    }

    public boolean canTrap() {
        return true;
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        exception = vc.doValue(exception);
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doThrow(this);
    }
}
