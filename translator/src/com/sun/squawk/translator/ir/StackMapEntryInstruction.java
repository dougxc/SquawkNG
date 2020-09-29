package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.loader.StackMap;

/**
 * This interface extends PseudoInstruction to represent points in a method
 * for which there is a StackMap.Entry.
 */

public interface StackMapEntryInstruction extends PseudoInstruction {
    /**
     * Get the stack map entry associated with this instruction.
     * @return the stack map entry associated with this instruction.
     */
    public StackMap.Entry stackMapEntry();

    /**
     * Get the Target encapsulating the stack map entry.
     * @return the Target encapsulating the stack map entry.
     */
    public Target target();
}