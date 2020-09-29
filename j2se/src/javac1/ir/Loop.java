/*
 * @(#)Loop.java                        1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javac1.backend.reg.RegMask;
import javac1.ir.instr.BlockBegin;

/**
 * Represents a loop in the control flow graph. The front end traverses the
 * basic blocks to determine innermost loops. This information may be used by
 * the back end for better register allocation.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Loop {
    /**
     * The first basic block of this loop.
     */
    private BlockBegin loopStart;

    /**
     * The last basic block of this loop containing the backedge.
     */
    private BlockBegin loopEnd;

    /**
     * All blocks contained inside the loop including start and end node.
     */
    private List blocks;

    /**
     * The list of loop entries.
     */
    private List loopEntries;

    /**
     * The list of loop exits.
     */
    private List loopExits;

    /**
     * The list of local variables that are cached in registers.
     */
    private List cachedLocals;

    /**
     * The registers locked for caching of local variables in the loop.
     */
    private RegMask lockout;

    /**
     * Constructs a new loop with the specified start and end block.
     *
     * @param  loopStart  start block of the loop
     * @param  loopEnd    end block of the loop
     */
    public Loop(BlockBegin loopStart, BlockBegin loopEnd) {
        this.loopStart = loopStart;
        this.loopEnd = loopEnd;
        this.blocks = new ArrayList();
        this.loopEntries = new ArrayList();
        this.loopExits = new ArrayList();
        this.cachedLocals = new ArrayList();
        this.lockout = RegMask.EMPTY_SET;
    }

    /**
     * Sets the registers locked for caching of local variables. This method
     * guarantees that the specified registers are never used inside the blocks
     * of the loop.
     *
     * @param  lockout  registers locked for caching
     */
    public void setLockout(RegMask lockout) {
        this.lockout = lockout;
    }

    /**
     * Returns the registers locked for caching of local variables.
     *
     * @return  the registers locked for caching
     */
    public RegMask getLockout() {
        return lockout;
    }

    /**
     * Returns the basic block that this loop starts at.
     *
     * @return  the start block of the loop
     */
    public BlockBegin getStart() {
        return loopStart;
    }

    /**
     * Returns the end block of this loop containing the backedge.
     *
     * @return  the end block of the loop
     */
    public BlockBegin getEnd() {
        return loopEnd;
    }

    /**
     * Adds the specified basic block to the list of blocks inside the loop.
     *
     * @param  block  basic block to be added
     */
    public void addBlock(BlockBegin block) {
        blocks.add(block);
    }

    /**
     * Returns the number of basic blocks contained inside the loop.
     *
     * @return  number of blocks inside the loop
     */
    public int countBlocks() {
        return blocks.size();
    }

    /**
     * Returns the basic block contained inside the loop at the specified index.
     *
     * @param   index  index into the list of blocks inside the loop
     * @return  the basic block at the specified index
     */
    public BlockBegin blockAt(int index) {
        return (BlockBegin) blocks.get(index);
    }

    /**
     * Returns the list of basic blocks contained inside the loop.
     *
     * @return  list of basic blocks inside the loop
     */
    public List getBlocks() {
        return blocks;
    }

    /**
     * Returns the list of local variables that are cached in registers.
     *
     * @return  list of cached local variables
     */
    public List getCachedLocals() {
        return cachedLocals;
    }

    /**
     * Adds the specified variable to the list of cached local variables.
     *
     * @param  cl  the cached local variable
     */
    public void addCachedLocal(Local cl) {
        cachedLocals.add(cl);
    }

    /**
     * Adds the specified pair of blocks to the list of loop entries. A loop
     * entry is an edge that connects any block outside the loop with the start
     * block of the loop.
     *
     * @param  from  the start of the edge
     * @param  to    the target of the edge
     */
    public void addLoopEntry(BlockBegin from, BlockBegin to) {
        loopEntries.add(new BlockPair(from, to));
    }

    /**
     * Adds the specified pair of blocks to the list of loop exits. A loop exit
     * is an edge that connects any block inside the loop with any block outside
     * the loop.
     *
     * @param  from  the start of the edge
     * @param  to    the target of the edge
     */
    public void addLoopExit(BlockBegin from, BlockBegin to) {
        loopExits.add(new BlockPair(from, to));
    }

    /**
     * Returns the number of loop exits.
     *
     * @return  number of loop exits
     */
    public int countLoopExits() {
        return loopExits.size();
    }

    /**
     * Returns the loop exit at the specified index.
     *
     * @param   index  index into the list of loop exits
     * @return  loop exit at the specified index
     */
    public BlockPair loopExitAt(int index) {
        return (BlockPair) loopExits.get(index);
    }

    /**
     * Returns the number of loop entries.
     *
     * @return  number of loop entries
     */
    public int countLoopEntries() {
        return loopEntries.size();
    }

    /**
     * Returns the loop entry at the specified index.
     *
     * @param   index  index into the list of loop entries
     * @return  loop entry at the specified index
     */
    public BlockPair loopEntryAt(int index) {
        return (BlockPair) loopEntries.get(index);
    }

    /**
     * Inserts the specified basic block and updates loop entries and exits. If
     * the specified edge has previously represented a loop entry then the entry
     * will afterwards start at the block to be inserted. On the other hand if
     * the edge has represented a loop exit then the block to be inserted will
     * become the new target of the loop exit.
     *
     * @param  old     the pair of blocks to insert the new block in between
     * @param  insert  the basic block to be inserted
     */
    public void updateLoopBlocks(BlockPair old, BlockBegin insert) {
        for (int i = loopEntries.size() - 1; i >= 0; i--) {
            BlockPair bp = (BlockPair) loopEntries.get(i);
            if (bp.equals(old)) {
                bp.setFrom(insert);
            }
        }
        for (int i = loopExits.size() - 1; i >= 0; i--) {
            BlockPair bp = (BlockPair) loopExits.get(i);
            if (bp.equals(old)) {
                bp.setTo(insert);
            }
        }
    }

    /**
     * Sorts the basic blocks contained inside the loop according to their
     * weights.
     *
     * @see  BlockBegin#compareTo(Object)
     */
    public void sortBlocks() {
        Collections.sort(blocks);
    }

    /**
     * Iterates over the blocks inside the loop and passes them to the specified
     * block closure.
     *
     * @param  bc  the block closure
     */
    public void iterateBlocks(BlockClosure bc) {
        int size = blocks.size();
        for (int i = 0; i < size; i++) {
            bc.doBlock((BlockBegin) blocks.get(i));
        }
    }

    /**
     * Compares this loop with the specified object for equality.
     *
     * @param   obj  the reference object with which to compare
     * @return  whether or not the loops are equal
     */
    public boolean equals(Object obj) {
        return (obj instanceof Loop)
            && (((Loop) obj).getStart() == loopStart)
            && (((Loop) obj).getEnd() == loopEnd);
    }
}
