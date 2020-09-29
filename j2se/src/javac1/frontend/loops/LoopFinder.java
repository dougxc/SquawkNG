/*
 * @(#)LoopFinder.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend.loops;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import javac1.Assert;
import javac1.JavaC1;
import javac1.ir.BlockPair;
import javac1.ir.IR;
import javac1.ir.IRScope;
import javac1.ir.Loop;
import javac1.ir.ScanBlocks;
import javac1.ir.ValueStack;
import javac1.ir.instr.BlockBegin;
import javac1.ir.instr.BlockEnd;
import javac1.ir.instr.Goto;
import javac1.ir.instr.LoopEnter;
import javac1.ir.instr.LoopExit;

/**
 * Searches the control flow graph for call-free inner loops. The results may
 * help the back end with a better register allocation.<p>
 *
 * At first the control flow graph is traversed in depth-first order while
 * looking for backedges, that is edges from any block to an already visited
 * one. After gathering the blocks that each loop contains, the innermost
 * call-free loops are selected. Finally the entries and exits for these loops
 * are constructed.
 *
 * @see      Loop
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class LoopFinder {
    /**
     * The intermediate representation to be searched for loops.
     */
    private IR ir;

    /**
     * The maximum number of basic blocks.
     */
    private int maxBlocks;

    /**
     * The block loop information.
     */
    private BlockLoopInfo[] info;

    /**
     * Constructs a new loop finder.
     *
     * @param  ir         intermediate representation to be searched for loops
     * @param  maxBlocks  maximum number of basic blocks
     */
    public LoopFinder(IR ir, int maxBlocks) {
        this.ir = ir;
        this.maxBlocks = maxBlocks;
        this.info = new BlockLoopInfo[maxBlocks];
    }

    /**
     * Returns the maximum number of basic blocks.
     *
     * @return  maximum number of basic blocks
     */
    public int getMaxBlocks() {
        return maxBlocks;
    }

    /**
     * Returns the block loop information for the specified block.
     *
     * @param   block  basic block to get information for
     * @return  corresponding block loop information
     */
    public BlockLoopInfo getBlockInfo(BlockBegin block) {
        return info[block.getBlockId()];
    }

    /**
     * Sets the block loop information for the specified block.
     *
     * @param  block  basic block to set information for
     * @param  bli    block loop information
     */
    public void setBlockInfo(BlockBegin block, BlockLoopInfo bli) {
        info[block.getBlockId()] = bli;
    }

    /**
     * Visits the specified block and its successors. While doing so, each
     * detected loop is added to the list of backedges.
     *
     * @param  backedges  the list of backedges
     * @param  block      basic block to start with
     */
    private void walkSux(List backedges, BlockBegin block) {
        block.setFlag(BlockBegin.LF_VISITED_FLAG | BlockBegin.LF_ACTIVE_FLAG);
        BlockLoopInfo bli = getBlockInfo(block);
        BlockEnd end = block.getEnd();
        int countSux = end.countSux();
        for (int i = 0; i < countSux; i++) {
            BlockBegin sux = end.suxAt(i);
            if (sux.isFlagSet(BlockBegin.LF_ACTIVE_FLAG)) {
                bli.setBackedgeStart(true);
                backedges.add(new Loop(sux, block));
            } else if (!sux.isFlagSet(BlockBegin.LF_VISITED_FLAG)) {
                walkSux(backedges, sux);
            }
        }
        block.clearFlag(BlockBegin.LF_ACTIVE_FLAG);
    }

    /**
     * Looks for backedges of natural loops in the control flow graph.
     *
     * @return  list of detected loops
     */
    private List findBackedges() {
        CreateInfoClosure cic = new CreateInfoClosure(this);
        ir.iteratePreorder(cic);
        SetPredsClosure spc = new SetPredsClosure(this);
        ir.iteratePreorder(spc);
        List backedges = new ArrayList();
        walkSux(backedges, ir.getTopScope().getStart());
        return backedges;
    }

    /**
     * Gathers the basic blocks that each loop in the specified list contains.
     *
     * @param  loops  the list of loops
     */
    private void gatherLoopBlocks(List loops) {
        int len = loops.size();
        for (int i = 0; i < len; i++) {
            BitSet blocks = new BitSet();
            Loop loop = (Loop) loops.get(i);
            if (loop.getStart() != loop.getEnd()) {
                blocks.set(loop.getStart().getBlockId());
                blocks.set(loop.getEnd().getBlockId());
                LinkedList stack = new LinkedList();
                stack.addLast(loop.getEnd());
                while (!stack.isEmpty()) {
                    BlockBegin block = (BlockBegin) stack.removeLast();
                    BlockLoopInfo bli = getBlockInfo(block);
                    int preds = bli.countPreds();
                    for (int j = 0; j < preds; j++) {
                        BlockBegin pred = bli.predAt(j);
                        if (!blocks.get(pred.getBlockId())) {
                            blocks.set(pred.getBlockId());
                            loop.addBlock(pred);
                            stack.addLast(pred);
                        }
                    }
                }
                loop.addBlock(loop.getStart());
            }
            loop.addBlock(loop.getEnd());
        }
    }

    /**
     * Returns the list of innermost loops that do not contain any calls. In the
     * first step loops are collected that have no calls and no backedges in the
     * loop except its own. In the second step, among all loops that have the
     * same start or end block respectively, the loop with the fewest blocks is
     * selected in each case.
     *
     * @param   loops  the list of all loops
     * @return  list of call-free inner loops
     */
    private List findInnerLoops(List loops) {
        List innermost = new ArrayList();
        int len = loops.size();
        for (int i = 0; i < len; i++) {
            Loop loop = (Loop) loops.get(i);
            for (int k = loop.countBlocks() - 1; k >= 0; k--) {
                BlockBegin block = loop.blockAt(k);
                if (block != loop.getEnd()) {
                    BlockLoopInfo bli = getBlockInfo(block);
                    if (bli.isBackedgeStart()) {
                        loop = null;
                        break;
                    }
                }
            }
            if (loop != null) {
                ScanBlocks scan = new ScanBlocks();
                loop.iterateBlocks(scan);
                if (!scan.hasCalls() && !scan.hasSlowCases()) {
                    innermost.add(loop);
                }
            }
        }
        for (int i = 0; i < innermost.size(); i++) {
            Loop cur = (Loop) innermost.get(i);
            BlockBegin header = cur.getStart();
            BlockBegin end = cur.getEnd();
            for (int j = i + 1; j < innermost.size(); j++) {
                Loop test = (Loop) innermost.get(j);
                if ((header == test.getStart()) || (end == test.getEnd())) {
                    if (test.countBlocks() > cur.countBlocks()) {
                        innermost.remove(test);
                    } else {
                        innermost.remove(cur);
                    }
                    i = -1;
                    break;
                }
            }
        }
        return innermost;
    }

    /**
     * Looks for entries of the specified loop. Each loop entry is an edge from
     * any block outside the loop to the specified start block of the loop.
     *
     * @param  block  start block of the loop
     * @param  loop   loop to search entries for
     */
    private void findLoopEntries(BlockBegin block, Loop loop) {
        BlockLoopInfo bli = getBlockInfo(block);
        int loopIndex = bli.getLoopIndex();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(bli.countPreds() >= 2, "at least two predecessors required");
        }
        for (int i = bli.countPreds() - 1; i >= 0; i--) {
            BlockBegin pred = bli.predAt(i);
            BlockLoopInfo predBli = getBlockInfo(pred);
            if (predBli.getLoopIndex() != loopIndex) {
                loop.addLoopEntry(pred, block);
            }
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(loop.countLoopEntries() > 0, "loop must be entered from somewhere");
        }
    }

    /**
     * Looks for exits of the specified loop. Each loop exit is an edge from the
     * specified block inside the loop to any block outside the loop.
     *
     * @param  block  block inside the loop
     * @param  loop   loop to search exits for
     */
    private void findLoopExits(BlockBegin block, Loop loop) {
        BlockLoopInfo bli = getBlockInfo(block);
        int loopIndex = bli.getLoopIndex();
        BlockEnd end = block.getEnd();
        for (int i = end.countSux() - 1; i >= 0; i--) {
            BlockBegin sux = end.suxAt(i);
            BlockLoopInfo suxBli = getBlockInfo(sux);
            if (suxBli.getLoopIndex() != loopIndex) {
                loop.addLoopExit(block, sux);
            }
        }
    }

    /**
     * Computes the entries and exits for each loop in the specified list.
     *
     * @param  loops  the list of loops
     */
    private void computeEntriesAndExists(List loops) {
        for (int i = loops.size() - 1; i >= 0; i--) {
            Loop loop = (Loop) loops.get(i);
            for (int j = loop.countBlocks() - 1; j >= 0; j--) {
                BlockBegin block = loop.blockAt(j);
                BlockLoopInfo bli = getBlockInfo(block);
                bli.setLoopIndex(i);
            }
            findLoopEntries(loop.getStart(), loop);
            for (int j = loop.countBlocks() - 1; j >= 0; j--) {
                BlockBegin block = loop.blockAt(j);
                findLoopExits(block, loop);
            }
        }
    }

    /**
     * Constructs a new block and associated block loop information.
     *
     * @param   scope  scope containing the new block
     * @param   bci    bytecode index of the block
     * @return  the created basic block
     */
    private BlockBegin newBlock(IRScope scope, int bci) {
        BlockBegin block = new BlockBegin(scope, bci);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(block.getBlockId() == maxBlocks, "illegal block number");
        }
        maxBlocks++;
        BlockLoopInfo[] old = info;
        info = new BlockLoopInfo[maxBlocks];
        System.arraycopy(old, 0, info, 0, maxBlocks - 1);
        info[maxBlocks - 1] = new BlockLoopInfo(block, maxBlocks);
        return block;
    }

    /**
     * Inserts a new loop entry into the intermediate representation.
     *
     * @param   loopId  identification number of the loop
     * @param   header  header block of the loop
     * @param   bci     bytecode index of the loop entry
     * @return  new block that starts with the entry and jumps to the header
     * @see     LoopEnter
     */
    private BlockBegin createLoopEntry(int loopId, BlockBegin header, int bci) {
        BlockBegin block = newBlock(header.getScope(), bci);
        BlockEnd end = new Goto(header.getScope(), header);
        block.setEnd(end);
        block.append(new LoopEnter(header.getScope(), loopId), bci).append(end, bci);
        if (javac1.Flags.PrintLoops) {
            JavaC1.out.print("added loop entry B" + block.getBlockId());
            JavaC1.out.println(" (header B" + header.getBlockId() + ")");
        }
        return block;
    }

    /**
     * Inserts a loop exit into the intermediate representation.
     *
     * @param   loopId  identification number of the loop
     * @param   dest    target of the loop exit
     * @return  new block that starts with the exit and jumps to the destination
     * @see     LoopExit
     */
    private BlockBegin createLoopExit(int loopId, BlockBegin dest) {
        int bci = dest.getBci();
        BlockBegin block = newBlock(dest.getScope(), bci);
        BlockEnd end = new Goto(dest.getScope(), dest);
        block.setEnd(end);
        block.append(new LoopExit(dest.getScope(), loopId), bci).append(end, bci);
        if (javac1.Flags.PrintLoops) {
            JavaC1.out.print("added loop exit B" + block.getBlockId());
            JavaC1.out.println(" (dest B" + dest.getBlockId() + ")");
        }
        return block;
    }

    /**
     * Inserts a basic block between the two specified blocks.
     *
     * @param   from    the basic block to insert after
     * @param   to      the basic block to insert before
     * @param   insert  the basic block to be inserted
     */
    private void insertBlock(BlockBegin from, BlockBegin to, BlockBegin insert) {
        BlockEnd fromEnd = from.getEnd();
        fromEnd.substituteSux(to, insert);
        insert.join(fromEnd.getState());
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(insert.getState() != null, "illegal operation");
        }
        getBlockInfo(insert).addPredecessor(from);
        getBlockInfo(to).removePredecessor(from);
        getBlockInfo(to).addPredecessor(insert);
    }

    /**
     * Inserts the specified basic block and updates loop entries and exits.
     *
     * @param  loops   the list of loops
     * @param  old     the pair of blocks to insert the new block in between
     * @param  insert  the basic block to be inserted
     * @see    Loop#updateLoopBlocks(BlockPair, BlockBegin)
     */
    private void updateLoops(List loops, BlockPair old, BlockBegin insert) {
        for (int i = loops.size() - 1; i >= 0; i--) {
            ((Loop) loops.get(i)).updateLoopBlocks(old, insert);
        }
    }

    /**
     * Adds one preheader for the specified loop.
     *
     * @param  loops  the list of loops
     * @param  index  index of the loop to add preheader for
     */
    private void addPreheader(List loops, int index) {
        Loop loop = (Loop) loops.get(index);
        BlockBegin header = loop.getStart();
        BlockLoopInfo bli = getBlockInfo(header);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(bli.countPreds() >= 2, "at least two predecessors required");
        }
        int smallestBci = bli.predAt(0).getEnd().getBci();
        for (int i = bli.countPreds() - 1; i > 0; i--) {
            BlockBegin pred = bli.predAt(i);
            smallestBci = Math.min(smallestBci, pred.getEnd().getBci());
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(smallestBci >= 0, "wrong bytecode index");
        }
        BlockBegin loopEntry = createLoopEntry(index, header, smallestBci);
        loop.addBlock(loopEntry);
        for (int i = bli.countPreds() - 1; i >= 0; i--) {
            BlockBegin pred = bli.predAt(i);
            if (pred != loop.getEnd()) {
                BlockPair old = new BlockPair(pred, header);
                insertBlock(pred, header, loopEntry);
                updateLoops(loops, old, loopEntry);
            }
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(loopEntry.getState() != null, "illegal state");
        }
        ValueStack endState = (ValueStack) loopEntry.getState().clone();
        loopEntry.getEnd().setState(endState);
        header.join(endState);
    }

    /**
     * Looks for an edge in the specified list with the desired target.
     *
     * @param   pairs  list of block pairs
     * @param   to     target of the edge to be searched
     * @return  the desired edge or <code>null</code> if none was found
     */
    private BlockPair findBlockPair(List pairs, BlockBegin to) {
        for (int i = pairs.size() - 1; i >= 0; i--) {
            BlockPair pair = (BlockPair) pairs.get(i);
            if (pair.getTo() == to) {
                return pair;
            }
        }
        return null;
    }

    /**
     * Inserts basic blocks and loop exit instructions into the intermediate
     * representation for the exits of the specified loop.
     *
     * @param  loops  the list of loops
     * @param  index  index of the loop to add exits for
     */
    private void addLoopExits(List loops, int index) {
        Loop loop = (Loop) loops.get(index);
        List newBlocks = new ArrayList();
        for (int i = loop.countLoopExits() - 1; i >= 0; i--) {
            BlockPair exit = loop.loopExitAt(i);
            BlockPair newExit = findBlockPair(newBlocks, exit.getTo());
            if (newExit == null) {
                BlockBegin exitBlock = createLoopExit(index, exit.getTo());
                loop.addBlock(exitBlock);
                newExit = new BlockPair(exitBlock, exit.getTo());
                newBlocks.add(newExit);
            }
            insertBlock(exit.getFrom(), exit.getTo(), newExit.getFrom());
            updateLoops(loops, exit, newExit.getFrom());
        }
        for (int i = newBlocks.size() - 1; i >= 0; i--) {
            BlockPair pair = (BlockPair) newBlocks.get(i);
            BlockBegin loopExit = pair.getFrom();
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(loopExit.getState() != null, "illegal state");
            }
            ValueStack endState = (ValueStack) loopExit.getState().clone();
            loopExit.getEnd().setState(endState);
            pair.getTo().join(endState);
        }
    }

    /**
     * Adds preheader and exit blocks to each loop in the specified list. After
     * this pass dominators are not valid anymore.
     *
     * @param  loop  the list of loops
     */
    private void insertBlocks(List loops) {
        for (int i = loops.size() - 1; i >= 0; i--) {
            addPreheader(loops, i);
            addLoopExits(loops, i);
        }
    }

    /**
     * Prints the specified message and each loop of the specified list.
     *
     * @param  msg    introducing message
     * @param  loops  list of loops to be printed
     */
    private void printLoops(String msg, List loops) {
        if ((loops != null) && (loops.size() != 0)) {
            JavaC1.out.println();
            JavaC1.out.println(msg);
            JavaC1.out.println();
            for (int i = 0; i < loops.size(); i++) {
                Loop loop = (Loop) loops.get(i);
                JavaC1.out.print("loop header B");
                JavaC1.out.print(loop.getStart().getBlockId());
                JavaC1.out.print(", backedge B");
                JavaC1.out.println(loop.getEnd().getBlockId());
            }
        }
    }

    /**
     * Computes the list of call-free inner loops in the control flow graph.
     *
     * @return  list of call-free inner loops
     */
    public List computeLoops() {
        List loops = findBackedges();
        gatherLoopBlocks(loops);
        List innermost = findInnerLoops(loops);
        computeEntriesAndExists(innermost);
        insertBlocks(innermost);
        if (javac1.Flags.PrintLoops) {
            printLoops("ALL LOOPS:", loops);
            printLoops("INNER LOOPS:", innermost);
        }
        return innermost;
    }
}
