package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

public class StoreField extends FieldInstruction {

    private Instruction value;
    StoreField(Field field, Instruction ref, Instruction value, boolean isFieldOfReceiver) {
        super(null, field, ref, isFieldOfReceiver);
        this.value = value;
        Assert.that(!field.isPrimitiveConstant());
    }

    public Instruction value()  { return value; }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doStoreField(this);
    }

    public void visit(ParameterVisitor visitor) {
        super.visit(visitor);
        value = visitor.doParameter(this, value);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        if (field().isStatic()) {
            return super.toString(Mnemonics.OPCODES[isFieldOfReceiver() ? OPC.CLASS_PUTSTATIC : OPC.PUTSTATIC]);
        }
        else {
            return super.toString(Mnemonics.OPCODES[isFieldOfReceiver() ? OPC.THIS_PUTFIELD : OPC.PUTFIELD]);
        }
    }
}
