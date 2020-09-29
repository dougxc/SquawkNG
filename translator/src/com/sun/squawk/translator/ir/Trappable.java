package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.Type;
import com.sun.squawk.translator.util.BitSet;
import com.sun.squawk.translator.Assert;

/**
 * This is a subclass for IR instructions that can cause the
 * VM to raise an exception.
 */
public abstract class Trappable extends Instruction {

    protected Trappable(Type type) {
        super(type);
    }

    public boolean canTrap() {
        return true;
    }

    /** The javac locals live at the exit of the BB whose exit is delimited by this instruction. */
    private BitSet bbLiveOut;
    /** The javac locals live on entry to the BB successor. */
    private BitSet successorBBLiveIn;

    public void clearDataFlowData() {
        bbLiveOut         = null;
        successorBBLiveIn = null;
    }

    /**
     * Get the instructions at the out edges of the basic block delimited by this instruction.
     * @return the instructions at the out edges of the basic block delimited by this instruction.
     */
    public Instruction[] getSuccessors() {
        return new Instruction[] { getNext() };
    }

    public void setSuccessorBBLiveIn(BitSet successorBBLiveIn) {
        this.successorBBLiveIn = successorBBLiveIn;
    }

    public BitSet getSuccessorBBLiveIn() {
        Assert.that(successorBBLiveIn != null);
        return successorBBLiveIn;
    }

    /** The live in javac locals at the BB started by this instruction. */
    public void setBBLiveOut(BitSet liveOut) {
        bbLiveOut = liveOut;
    }

    public BitSet getBBLiveOut() {
        return bbLiveOut;
    }
}
