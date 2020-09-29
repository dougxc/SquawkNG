package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

/**
 * This is the IR instruction representing the Java bytecode
 * TABLESWITCH instructions.
 */
public final class TableSwitch extends SwitchInstruction {

    private final int low;
    private final int high;

    TableSwitch(Instruction key, int low, int high, Target defaultTarget) {
        super(key, high-low+1, defaultTarget);
        this.low = low;
        this.high = high;
    }

    public int low()               { return low;           }
    public int high()              { return high;          }

    public void addTarget(int i, Target target) {
        super.addTarget(i-low, target);
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doTableSwitch(this);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer(30);
        buf.append(Mnemonics.OPCODES[OPC.TABLESWITCH]).
                append("default=").
                append(defaultTarget().getInstruction().getRelocIP()).
                append(" low=").
                append(low).
                append(" high=").
                append(high);
        Target[] targets = targets();
        for (int i = low; i <= high; i++) {
            buf.append(" ").append(i).append("->").append(targets[i-low].getInstruction().getRelocIP());
        }
        return buf.toString();
    }
}
