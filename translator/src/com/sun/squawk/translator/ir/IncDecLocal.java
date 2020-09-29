package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

/**
 * Instruction that increments or decrements a local variable by 1.
 */
public class IncDecLocal extends LocalVariableInstruction {

    /** Flags whether or not this is an increment. */
    private final boolean increment;

    public IncDecLocal(Local local, boolean increment) {
        super(local, null);
        this.increment = increment;
    }

    public boolean isIncrement()   { return increment; }
    public boolean isDefinition()  { return true;      }
    public boolean isUse()         { return true;      }

    public void updateLocal(Local local, boolean updateUseDef) {
        super.updateLocal(local);
        if (local != null && updateUseDef) {
            local.addUse(this);
        }
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doIncDecLocal(this);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return super.toString(Mnemonics.OPCODES[increment ? OPC.INC : OPC.DEC], local().type());
    }
}