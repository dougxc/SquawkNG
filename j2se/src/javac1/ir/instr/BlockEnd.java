/*
 * @(#)BlockEnd.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import java.util.ArrayList;
import java.util.List;
import javac1.Assert;
import javac1.ir.IRScope;
import javac1.ir.types.ValueType;

/**
 * The abstract base class for all nodes terminating the instruction list of a
 * basic block. It represents control flow and contains a possibly empty list of
 * successors.
 *
 * @see      BlockBegin
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public abstract class BlockEnd extends StateSplit {
    /**
     * The list of successors of this node.
     */
    private List sux;

    /**
     * Initializes the attributes declared in this class.
     *
     * @param  scope  scope containing this instruction
     * @param  type   the type of the return value if any
     */
    protected BlockEnd(IRScope scope, ValueType type) {
        super(scope, type);
        sux = new ArrayList();
    }

    /**
     * Adds the specified basic block to the list of successors.
     *
     * @param  block  the block to be added
     */
    protected void addSux(BlockBegin block) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(block != null, "successor must exist");
        }
        sux.add(block);
    }

    /**
     * Sets the list of successors to the specified list.
     *
     * @param  sux  the new list of successors
     */
    protected void setSuccessors(List sux) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((sux != null) && !sux.contains(null), "successor must exist");
        }
        this.sux = sux;
    }

    /**
     * Replaces the element at the specified index in the list of successors by
     * the specified block.
     *
     * @param   index  index of the element to replace
     * @param   block  block to be stored at the specified index
     * @return  the block previously at the specified index
     */
    protected BlockBegin setSux(int index, BlockBegin block) {
        return (BlockBegin) sux.set(index, block);
    }

    /**
     * Returns the successor at the specified index.
     *
     * @param   index  index into the list of successors
     * @return  the successor at the specified index
     */
    public BlockBegin suxAt(int index) {
        return (BlockBegin) sux.get(index);
    }

    /**
     * Returns the default successor of this node.
     *
     * @return  the default successor
     */
    public BlockBegin defaultSux() {
        return suxAt(countSux() - 1);
    }

    /**
     * Returns the index of the specified block in the list of successors, or -1
     * if the list does not contain the block.
     *
     * @param   block  successor to search for
     * @return  the index of the successor
     */
    public int suxIndex(BlockBegin block) {
        return sux.indexOf(block);
    }

    /**
     * Returns the number of elements in the list of successors.
     *
     * @return  the number of successors
     */
    public int countSux() {
        return sux.size();
    }

    /**
     * Substitutes the specified successor by the specified basic block.
     *
     * @param  oldSux  successor to substitute
     * @param  newSux  basic block to be substituted by
     */
    public void substituteSux(BlockBegin oldSux, BlockBegin newSux) {
        for (int i = 0; i < sux.size(); i++) {
            BlockBegin block = suxAt(i);
            if (block == oldSux) {
                sux.set(i, newSux);
            }
        }
    }
}
