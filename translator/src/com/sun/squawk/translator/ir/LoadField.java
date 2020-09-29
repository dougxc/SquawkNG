package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

public final class LoadField extends FieldInstruction {

    LoadField(Field field, Instruction ref, boolean isFieldOfReceiver) {
        super(field.type(), field, ref, isFieldOfReceiver);
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doLoadField(this);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        if (field().isStatic()) {
            return super.toString(Mnemonics.OPCODES[isFieldOfReceiver() ? OPC.CLASS_GETSTATIC :
                                                                  OPC.GETSTATIC]);
        }
        else {
            return super.toString(Mnemonics.OPCODES[isFieldOfReceiver() ? OPC.THIS_GETFIELD :
                                                                  OPC.GETFIELD]);
        }
    }
}
