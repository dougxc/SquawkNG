package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;

/**
 * This is Squawk 'lookup' instruction.
 */
public final class Lookup extends Instruction {

    /** The instruction that pushed the value being searched for. */
    private Instruction key;
    /** The instruction that pushed the array being searched. */
    private Instruction array;

    private int[] matches;

    /**
     * Create a Lookup instruction.
     * @param key The instruction that pushed the value being searched for.
     * @param array The instruction that pushed the array being searched.
     */
    Lookup(Type intType, Instruction key, Instruction array) {
        super(intType);
        this.key   = key;
        this.array = array;
    }

    public Instruction key() { return key; }
    public Instruction array() { return array; }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doLookup(this);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer(30);
        buf.append("lookup [");
        for (int i = 0; i != matches.length; i++) {
            buf.append(" ").append(matches[i]);
        }
        return buf.append(" ]").toString();
    }
}
