/*
 * @(#)Convert.java                     1.10 02/11/27
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
 * The instruction node for converting the type of a value.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Convert extends Instruction {
    /**
     * The conversion operation code.
     */
    private int op;

    /**
     * The value whose type has to be converted.
     */
    private Instruction value;

    /**
     * Constructs a new instruction node for a type conversion.
     *
     * @param  scope   scope containing this instruction
     * @param  op      conversion operation code
     * @param  value   the value to be cast
     * @param  toType  the target type
     */
    public Convert(IRScope scope, int op, Instruction value, ValueType toType) {
        super(scope, toType);
        this.op = op;
        this.value = value;
    }

    /**
     * Returns the conversion operation code.
     *
     * @return  the operation code
     */
    public int getOp() {
        return op;
    }

    /**
     * Returns the value whose type has to be converted.
     *
     * @return  the value to be cast
     */
    public Instruction getValue() {
        return value;
    }

    public int hashCode() {
        return hash(op, value.getId());
    }

    public boolean equals(Object obj) {
        return (obj instanceof Convert) && (((Convert) obj).getOp() == op)
            && (((Convert) obj).getValue() == value);
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        value = vc.doValue(value);
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doConvert(this);
    }
}
