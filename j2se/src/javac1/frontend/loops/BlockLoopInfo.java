/*
 * @(#)BlockLoopInfo.java               1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend.loops;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import javac1.Assert;
import javac1.ir.instr.BlockBegin;

/**
 * Stores information about a basic block for purposes of loop detection.
 *
 * @see      LoopFinder
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class BlockLoopInfo {
    /**
     * The basic block that is described by this block loop information.
     */
    private BlockBegin block;

    /**
     * Whether or not a backedge goes off from this node.
     */
    private boolean backedgeStart;

    /**
     * The index of the loop that the described basic block belongs to.
     */
    private int loopIndex;

    /**
     * The list of predecessors of the described basic block.
     */
    private List preds;

    /**
     * Constructs a new block loop information for the specified basic block.
     *
     * @param  block      the basic block to be described
     * @param  maxBlocks  maximum number of basic blocks
     */
    public BlockLoopInfo(BlockBegin block, int maxBlocks) {
        this.block = block;
        this.backedgeStart = false;
        this.loopIndex = -1;
        this.preds = new ArrayList();
    }

    /**
     * Returns the basic block that is described by this block loop information.
     *
     * @return  the described basic block
     */
    public BlockBegin getBlock() {
        return block;
    }

    /**
     * Adds the specified basic block to the list of predecessors.
     *
     * @param  pred  predecessor to be added
     */
    public void addPredecessor(BlockBegin pred) {
        preds.add(pred);
    }

    /**
     * Removes the specified basic block from the list of predecessors.
     *
     * @param  pred  predecessor to be removed
     */
    public void removePredecessor(BlockBegin pred) {
        boolean success = preds.remove(pred);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(success, "removal failed");
        }
    }

    /**
     * Returns the number of blocks in the list of predecessors.
     *
     * @return  number of predecessors
     */
    public int countPreds() {
        return preds.size();
    }

    /**
     * Returns the block at the specified index in the list of predecessors.
     *
     * @param   index  index into the list of predecessors
     * @return  predecessor at the specified index
     */
    public BlockBegin predAt(int index) {
        return (BlockBegin) preds.get(index);
    }

    /**
     * Sets whether or not a backedge goes off from this node.
     *
     * @param  backedgeStart  whether or not this is the start of a backedge
     */
    public void setBackedgeStart(boolean backedgeStart) {
        this.backedgeStart = backedgeStart;
    }

    /**
     * Tests if a backedge goes off from this node.
     *
     * @return  whether or not this is the start of a backedge
     */
    public boolean isBackedgeStart() {
        return backedgeStart;
    }

    /**
     * Stores the number of the loop that the described basic block belongs to.
     *
     * @param  loopIndex  index of the loop
     */
    public void setLoopIndex(int loopIndex) {
        this.loopIndex = loopIndex;
    }

    /**
     * Returns the number of the loop that contains the described basic block.
     *
     * @return  index of the loop
     */
    public int getLoopIndex() {
        return loopIndex;
    }
}
