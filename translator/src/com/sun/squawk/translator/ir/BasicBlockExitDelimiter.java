package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.Type;
import com.sun.squawk.translator.util.BitSet;
import com.sun.squawk.translator.Assert;
import com.sun.squawk.translator.Translator;

/**
 * This is a subclass for IR instructions that delimit a basic block.
 */
public abstract class BasicBlockExitDelimiter extends Instruction {

    /** Cached successors. */
    private Instruction[] successors;
    /** The javac locals live at the exit of the BB whose exit is delimited by this instruction. */
    private BitSet bbLiveOut;
    /** The javac locals live on entry to the BB successor for a fall-through
        terminated basic block. */
    private BitSet fallThroughBBLiveIn;

    protected BasicBlockExitDelimiter(Type type) {
        super(type);
    }

    public void clearDataFlowData() {
        successors          = null;
        bbLiveOut           = null;
        fallThroughBBLiveIn = null;
    }

    /**
     * Get the instructions at the out edges of the basic block delimited by this instruction.
     * @return the instructions at the out edges of the basic block delimited by this instruction.
     */
    public Instruction[] getSuccessors() {
        if (successors == null) {
            successors = createSuccessors();
            if (successors == null) {
                successors = Translator.ZEROINSTRUCTIONS;
            }
        }
        return successors;
    }

    /** Provide this super class with the successors. */
    protected abstract Instruction[] createSuccessors();

    public void setFallThroughBBLiveIn(BitSet fallThroughBBLiveIn) {
        Assert.that(fallsThrough());
        this.fallThroughBBLiveIn = fallThroughBBLiveIn;
    }

    public BitSet getFallThroughBBLiveIn() {
        return fallThroughBBLiveIn;
    }

    /**
     * All basic block delimiting instructions do not fall through except for
     * 'if' instructions.
     * @return true is this is an IfOp object, false otherwise.
     */
    public boolean fallsThrough()  {
        return (this instanceof IfOp);
    }

    /**
     * Get the stack depth constraint upon exit from this instruction.
     * @return 0
     */
    public boolean constrainsStack() {
        return true;
    }


    /** The live in javac locals at the BB started by this instruction. */
    public void setBBLiveOut(BitSet liveOut) {
        bbLiveOut = liveOut;
    }

    public BitSet getBBLiveOut() {
        return bbLiveOut;
    }



}
