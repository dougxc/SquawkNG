package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

public class StoreLocal extends LocalVariableInstruction {

    private Instruction value;
    private boolean isInitializer;

    StoreLocal(Local local, Instruction value) {
        super(local, null);
        Assert.that(value != null);
        this.value = value;
    }

    public Instruction value()  { return value; }
    public void setIsInitializer() {
        isInitializer = true;
    }
    public boolean isInitializer() {
        return isInitializer;
    }

    public boolean isDefinition()  { return true;     }
    public boolean isUse()         { return false;    }

    public void updateLocal(Local local, boolean updateUseDef) {
        super.updateLocal(local);
        if (local != null && updateUseDef) {
            local.addDefinition(this);
        }
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doStoreLocal(this);
    }

    public void visit(ParameterVisitor visitor) {
        Assert.that(value != null);
        value = visitor.doParameter(this, value);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return super.toString(Mnemonics.OPCODES[OPC.STORE], value.type());
    }
}
