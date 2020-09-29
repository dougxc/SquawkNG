package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

public class MonitorEnter extends Trappable {

    private Instruction value;

    MonitorEnter(Instruction value) {
        super(null);
        this.value = value;
    }

    public Instruction value() { return value; }

    public boolean constrainsStack() {
        return true;
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doMonitorEnter(this);
    }

    public void visit(ParameterVisitor visitor) {
        if (value != null) {
            value = visitor.doParameter(this, value);
        }
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return Mnemonics.OPCODES[value== null ? OPC.CLASS_MONITORENTER : OPC.MONITORENTER];
    }
}
