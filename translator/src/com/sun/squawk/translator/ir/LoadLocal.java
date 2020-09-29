package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

final public class LoadLocal extends LocalVariableInstruction {

    LoadLocal(Local local, Type type) {
        super(local, type);
    }

    public boolean isDefinition()  { return false;     }
    public boolean isUse()         { return true;      }

    public void updateLocal(Local local, boolean updateUseDef) {
        super.updateLocal(local);
        if (local != null && updateUseDef) {
            local.addUse(this);
        }
    }

    /**
     * Override type() in super class to return the type of the local being
     * loaded if the local is now a local that was allocated by the
     * GraphTransformer.
     * @return
     */
    public Type type() {
        if (local() != null && local().squawkIndex() != -1) {
            return local().type();
        }
        else {
            return super.type();
        }
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doLoadLocal(this);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return super.toString(Mnemonics.OPCODES[OPC.LOAD], type());
    }
}
