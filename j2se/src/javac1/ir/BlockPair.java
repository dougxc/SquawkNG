/*
 * @(#)BlockPair.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir;

import javac1.ir.instr.BlockBegin;

/**
 * Stores a pair of blocks or an edge in the control flow graph respectively.
 * Instances of this class particularly represent loop entries and exits, in
 * other words edges going into or out of a cycle in the control flow graph.
 *
 * @see      Loop
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class BlockPair {
    /**
     * The basic block that the edge starts at.
     */
    private BlockBegin from;

    /**
     * The basic block that the edge ends in.
     */
    private BlockBegin to;

    /**
     * Constructs a new pair of blocks representing an edge in the control flow
     * graph.
     *
     * @param  from  the start of the edge
     * @param  to    the target of the edge
     */
    public BlockPair(BlockBegin from, BlockBegin to) {
        this.from = from;
        this.to = to;
    }

    /**
     * Sets the basic block that the edge starts at.
     *
     * @param  from  the start of the edge
     */
    public void setFrom(BlockBegin from) {
        this.from = from;
    }

    /**
     * Returns the basic block that the edge starts at.
     *
     * @return  the start of the edge
     */
    public BlockBegin getFrom() {
        return from;
    }

    /**
     * Sets the basic block that the edge ends in.
     *
     * @param  to  the target of the edge
     */
    public void setTo(BlockBegin to) {
        this.to = to;
    }

    /**
     * Returns the basic block that the edge ends in.
     *
     * @return  the target of the edge
     */
    public BlockBegin getTo() {
        return to;
    }

    /**
     * Compares this block pair with the specified object for equality.
     *
     * @param   obj  the reference object with which to compare
     * @return  whether or not the pairs are equal
     */
    public boolean equals(Object obj) {
        return (obj instanceof BlockPair)
            && (((BlockPair) obj).getFrom() == from)
            && (((BlockPair) obj).getTo() == to);
    }
}
