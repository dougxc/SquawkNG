package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;

/**
 * This is the IR instruction representing the Java bytecode
 * LOOKUPSWITCH instructions.
 */
public final class LookupSwitch extends SwitchInstruction {

    /** The case match constant values. */
    private int[] matches;

    /**
     * Create a LookupSwitch instruction.
     * @param key The instruction that pushed the value being switched on.
     * @param npairs The number of cases in the switch.
     * @param defaultTarget The target for the "default" case.
     */
    LookupSwitch(Instruction key, int npairs, Target defaultTarget) {
        super(key, npairs, defaultTarget);
        this.matches = new int[npairs];
    }

    /**
     * Add a case to the switch.
     * @param index The index in the targets table.
     * @param match The match value for this case.
     * @param target The address of the target.
     */
    public void addTarget(int index, int match, Target target) {
        super.addTarget(index, target);
        matches[index] = match;
    }

    /**
     * Return the case match constant values.
     * @return the case match constant values.
     */
    public int[] matches() {
        return matches;
    }

    public Object getConstantObject() {
        return matches;
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doLookupSwitch(this);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer(30);
        buf.append("lookupswitch default=").append(defaultTarget().getInstruction().getRelocIP());
        Target[] targets = targets();
        for (int i = 0; i != matches.length; i++) {
            buf.append(" ").append(matches[i]).append("->").append(targets[i].getInstruction().getRelocIP());
        }
        return buf.toString();
    }
}
