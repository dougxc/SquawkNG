package com.sun.squawk.translator.ir;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.StackMap;
import com.sun.squawk.translator.util.BitSet;

/**
 * This represents a control flow merge point. It is inserted between the
 * instruction at this address and its lexical predecessor.
 */
public abstract class BasicBlockEntryDelimiter extends Instruction implements StackMapEntryInstruction {

    /** The target the merge point represents. */
    private Target target;

    /** The javac locals live upon entry the basic block started by this instruction. */
    private BitSet bbLiveIn;

    BasicBlockEntryDelimiter(Type type, Target target) {
        super(type);
        this.target = target;
        target.setInstruction(this);
    }

    public void clearDataFlowData() {
        bbLiveIn = null;
    }

    /**
     * Return the Target representing the address of this instruction.
     * @return the Target representing the address of this instruction.
     */
    public final Target target() { return target; }

    /**
     * Get the stack map entry associated with this instruction.
     * @return the stack map entry associated with this instruction.
     */
    public final StackMap.Entry stackMapEntry() {
        return target.getStackMapEntry();
    }

    public final void setBBLiveIn(BitSet liveIn) {
        bbLiveIn = liveIn;
    }
    public final BitSet getBBLiveIn() {
        return bbLiveIn;
    }
}
