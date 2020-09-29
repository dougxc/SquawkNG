/*
 * @(#)BlockBegin.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import javac1.Assert;
import javac1.backend.code.Label;
import javac1.backend.items.BlockItem;
import javac1.ir.BlockClosure;
import javac1.ir.BlockCollection;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.ValueStack;
import javac1.ir.types.ValueType;

/**
 * The node representing the start of a basic block in the control flow graph.
 * The sequential list of instructions forming a basic block body is always
 * leaded by an instance of this class and terminated by the corresponding end
 * node. This way the control flow graph can be traversed quickly without
 * accessing every single instruction.
 *
 * @see      BlockEnd
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class BlockBegin extends StateSplit implements Comparable {
    /**
     * The constant for all flags cleared.
     */
    public static final int NO_FLAG = 0;

    /**
     * The flag for a standard method entry.
     */
    public static final int STD_ENTRY_FLAG = 1 << 0;
    
    /**
     * The flag for an on-stack-replacement entry.
     */
    public static final int OSR_ENTRY_FLAG = 1 << 1;

    /**
     * The flag for the entry of an exception handler.
     */
    public static final int EXCEPTION_ENTRY_FLAG = 1 << 2;

    /**
     * The flag for the entry of a subroutine.
     */
    public static final int SUBROUTINE_ENTRY_FLAG = 1 << 3;

    /**
     * The flag for the target of a backward branch.
     */
    public static final int BACKW_BRANCH_TARGET_FLAG = 1 << 4;

    /**
     * The flag for already visited nodes.
     */
    public static final int WAS_VISITED_FLAG = 1 << 5;

    /**
     * The flag for blocks that are active during loop detection.
     */
    public static final int LF_ACTIVE_FLAG = 1 << 6;

    /**
     * The flag for blocks that have been visited during loop detection.
     */
    public static final int LF_VISITED_FLAG = 1 << 7;

    /**
     * The unique identification number of this basic block.
     */
    private int blockId;

    /**
     * The scope containing this basic block.
     */
    private IRScope scope;

    /**
     * The flags associated with this block.
     */
    private int flags;

    /**
     * The block weight used for block ordering.
     */
    private int weight;

    /**
     * The corresponding end node terminating the instruction list.
     */
    private BlockEnd end;

    /**
     * The list of subroutines called by this block.
     */
    private List subroutines;

    /**
     * The list of exception handlers potentially invoked by this block.
     */
    private List xhandlers;

    /**
     * The machine-specific information for this basic block.
     */
    private BlockItem blockItem;

    /**
     * The label associated with this block.
     */
    private Label label;

    /**
     * Constructs a new node representing the start of a basic block.
     *
     * @param   scope  scope containing this basic block
     * @param   bci    bytecode index of this node
     */
    public BlockBegin(IRScope scope, int bci) {
        super(scope, ValueType.illegalType);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(scope != null, "scope must exist");
        }
        this.blockId = scope.getParent().getNextBlockId();
        this.scope = scope;
        this.flags = NO_FLAG;
        this.weight = 0;
        this.end = null;
        this.subroutines = new ArrayList();
        this.xhandlers = new ArrayList();
        this.blockItem = null;
        this.label = new Label();
        setBci(bci);
    }

    /**
     * Returns the number of blocks that have been created so far.
     *
     * @return  the number of blocks
     */
    public int countBlocks() {
        return scope.getParent().countBlocks();
    }

    /**
     * Returns the unique identification number of this basic block.
     *
     * @return  the number of this block
     */
    public int getBlockId() {
        return blockId;
    }

    /**
     * Returns the scope containing this basic block.
     *
     * @return  the current scope
     */
    public IRScope getScope() {
        return scope;
    }

    /**
     * Sets the specified flag for this node.
     *
     * @param  flag  the flag to be set
     */
    public void setFlag(int flag) {
        flags |= flag;
    }

    /**
     * Clears the specified flag. Nothing happens if the flag was not set.
     *
     * @param  flag  the flag to be cleared
     */
    public void clearFlag(int flag) {
        flags &= ~flag;
    }

    /**
     * Tests if a certain flag is set or cleared in this node.
     *
     * @param   flag  the flag to test
     * @return  whether or not the flag is set
     */
    public boolean isFlagSet(int flag) {
        return (flags & flag) != 0;
    }

    /**
     * Tests if this is the entry block of a method, exception handler or
     * subroutine.
     *
     * @return  whether or not this is an entry block
     */
    public boolean isEntryBlock() {
        int entryMask = STD_ENTRY_FLAG | EXCEPTION_ENTRY_FLAG | SUBROUTINE_ENTRY_FLAG;
        return (flags & entryMask) != 0;
    }

    /**
     * Sets the node terminating the instruction list of this basic block.
     *
     * @param  end  corresponding end node
     */
    public void setEnd(BlockEnd end) {
        this.end = end;
    }

    /**
     * Returns the node terminating the instruction list of this basic block.
     *
     * @return  corresponding end node
     */
    public BlockEnd getEnd() {
        return end;
    }

    /**
     * Computes the weight of a block by looking at its bytecode index, its
     * distance from the start block and some other attributes.
     *
     * @param  distance  distance from the start block
     */
    private void calcWeight(int distance) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(javac1.Flags.BitsPerWord == 32, "adjust this code");
        }
        int val = ((getBci() & 0x7FFFF) << 10) | (distance & 0x3FF);
        if ((end != null) && ((end instanceof Throw) || (end instanceof Return))
                && !isFlagSet(STD_ENTRY_FLAG)) {
            val |= 1 << ((end instanceof Throw) ? 30 : 29);
        }
        setWeight(val);
    }

    /**
     * Sets the weight of this basic block to the specified value.
     *
     * @param  weight  the weight of this block
     */
    public void setWeight(int weight) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(weight >= 0, "weight should be positive");
        }
        this.weight = weight;
    }

    /**
     * Returns the weight of this basic block used for block ordering.
     *
     * @return  the weight of this block
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Tests if this block is after the specified one. Before code generation
     * the basic blocks are sorted according to their weights in ascending
     * order. Hence a basic block is after another one if and only if its weight
     * is greater or equal the weight of the other block.
     *
     * @return  whether or not this block is after the specified one
     * @see     #calcWeight(int)
     */
    public boolean isAfterBlock(BlockBegin block) {
        return weight >= block.getWeight();
    }

    /**
     * Compares this basic block with the specified object for order. This
     * method is used for sorting blocks according to their weights and hence
     * returns a negative integer, zero, or a positive integer as the weight of
     * this block is less than, equal to, or greater than the weight of the
     * specified one. Since the weight of new blocks is initialized to 0, the
     * method should be invoked only after calculating the weight of each block
     * and may be inconsistent with equals.
     *
     * @param   obj  the object to be compared
     * @return  integer that specifies the order of the blocks
     * @throws  ClassCastException  if the specified object is not a basic block
     * @see     #calcWeight(int)
     * @see     #isAfterBlock(BlockBegin)
     * @see     #iterateAndSetWeight(BlockClosure)
     * @see     BlockCollection#sort()
     */
    public int compareTo(Object obj) {
        return weight - ((BlockBegin) obj).getWeight();
    }

    /**
     * Adds the specified block to the list of subroutines. The block is added
     * only if it is not in the list already.
     *
     * @param  block  the subroutine to be added
     */
    public void addSubroutine(BlockBegin block) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((block != null) && block.isFlagSet(SUBROUTINE_ENTRY_FLAG), "subroutine must exist");
        }
        if (!subroutines.contains(block)) {
            subroutines.add(block);
        }
    }

    /**
     * Returns the subroutine at the specified index.
     *
     * @param   index  index into the list of subroutines
     * @return  subroutine at the specified index
     */
    public BlockBegin subroutineAt(int index) {
        return (BlockBegin) subroutines.get(index);
    }

    /**
     * Returns the number of subroutines called by this basic block.
     *
     * @return  number of subroutines
     */
    public int countSubroutines() {
        return subroutines.size();
    }

    /**
     * Adds the specified block to the list of exception handlers. The block is
     * added only if it is not in the list already.
     *
     * @param  block  the exception handler to be added
     */
    public void addExceptionHandler(BlockBegin block) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((block != null) && block.isFlagSet(EXCEPTION_ENTRY_FLAG), "exception handler must exist");
        }
        if (!xhandlers.contains(block)) {
            xhandlers.add(block);
        }
    }

    /**
     * Returns the exception handler at the specified index.
     *
     * @param   index  index into the list of exception handlers
     * @return  exception handler at the specified index
     */
    public BlockBegin exceptionHandlerAt(int index) {
        return (BlockBegin) xhandlers.get(index);
    }

    /**
     * Returns the number of exception handlers that may be invoked by this
     * basic block.
     *
     * @return  number of exception handlers
     */
    public int countExceptionHandlers() {
        return xhandlers.size();
    }

    /**
     * Links machine-specific information to this basic block.
     *
     * @param  blockItem  the machine-specific information
     */
    public void setBlockItem(BlockItem blockItem) {
        this.blockItem = blockItem;
    }

    /**
     * Returns the machine-specific information that this basic block maintains.
     *
     * @return  the machine-specific information
     */
    public BlockItem getBlockItem() {
        return blockItem;
    }

    /**
     * Returns the label associated with this basic block.
     *
     * @return  the associated label
     */
    public Label getLabel() {
        return label;
    }

    /**
     * Tries to join two basic blocks by comparing their state arrays.
     *
     * @param   state  state array of the other block
     * @return  whether or not the join succeeded
     * @see     #join(ValueStack)
     */
    public boolean tryJoin(ValueStack state) {
        if (getState() == null) {
            setState(new ValueStack(state));
            return true;
        } else {
            return getState().equals(state);
        }
    }

    /**
     * Tries to join two basic blocks and throws an exception if the join fails.
     *
     * @param  state  state array of the other block
     * @see    #tryJoin(ValueStack)
     */
    public void join(ValueStack state) {
        boolean success = tryJoin(state);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(success, "join failed");
        }
    }

    /**
     * Recursively collects all end nodes reachable from this basic block.
     *
     * @param  mark     keeps track of the visited blocks
     * @param  endlist  the list of end nodes
     */
    private void collectEnds(BitSet mark, List endlist) {
        if (!mark.get(blockId)) {
            mark.set(blockId);
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(end != null, "block has not been completed yet");
            }
            if (end.countSux() == 0) {
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(!endlist.contains(end), "element added before");
                }
                endlist.add(end);
            } else {
                for (int i = end.countSux() - 1; i >= 0; i--) {
                    end.suxAt(i).collectEnds(mark, endlist);
                }
            }
        }
    }

    /**
     * Returns the list of end nodes that can be reached from this basic block.
     *
     * @return  the list of end nodes
     */
    public List getEndList() {
        BitSet mark = new BitSet(countBlocks());
        List endlist = new ArrayList(countBlocks());
        collectEnds(mark, endlist);
        return endlist;
    }

    /**
     * Recursively iterates over all blocks reachable from this basic block.
     *
     * @param  mark  keeps track of the visited blocks
     * @param  bc    the block closure
     * @see    #iteratePreorder(BlockClosure)
     */
    private void iteratePreorder(BitSet mark, BlockClosure bc) {
        if (!mark.get(blockId)) {
            mark.set(blockId);
            bc.doBlock(this);
            for (int i = xhandlers.size() - 1; i >= 0; i--) {
                exceptionHandlerAt(i).iteratePreorder(mark, bc);
            }
            for (int i = subroutines.size() - 1; i >= 0; i--) {
                subroutineAt(i).iteratePreorder(mark, bc);
            }
            for (int i = end.countSux() - 1; i >= 0; i--) {
                end.suxAt(i).iteratePreorder(mark, bc);
            }
        }
    }

    /**
     * Iterates over all reachable basic blocks and passes them to the specified
     * block closure. Each block is processed <strong>before</strong> the blocks
     * will be handled that can be reached from it.
     *
     * @param  bc  the block closure
     */
    public void iteratePreorder(BlockClosure bc) {
        BitSet mark = new BitSet(countBlocks());
        iteratePreorder(mark, bc);
    }

    /**
     * Recursively iterates over all blocks reachable from this basic block.
     *
     * @param  mark  keeps track of the visited blocks
     * @param  bc    the block closure
     * @see    #iteratePostorder(BlockClosure)
     */
    private void iteratePostorder(BitSet mark, BlockClosure bc) {
        if (!mark.get(blockId)) {
            mark.set(blockId);
            for (int i = xhandlers.size() - 1; i >= 0; i--) {
                exceptionHandlerAt(i).iteratePostorder(mark, bc);
            }
            for (int i = subroutines.size() - 1; i >= 0; i--) {
                subroutineAt(i).iteratePostorder(mark, bc);
            }
            for (int i = end.countSux() - 1; i >= 0; i--) {
                end.suxAt(i).iteratePostorder(mark, bc);
            }
            bc.doBlock(this);
        }
    }

    /**
     * Iterates over all reachable basic blocks and passes them to the specified
     * block closure. Each block is processed <strong>after</strong> all the
     * blocks have been handled that can be reached from it.
     *
     * @param  bc  the block closure
     */
    public void iteratePostorder(BlockClosure bc) {
        BitSet mark = new BitSet(countBlocks());
        iteratePostorder(mark, bc);
    }

    /**
     * Iterates over all reachable basic blocks in topological order and passes
     * them to the specified block closure.
     *
     * @param  bc  the block closure
     */
    public void iterateTopological(BlockClosure bc) {
        BlockCollection collection = new BlockCollection(countBlocks());
        iteratePostorder(collection);
        collection.iterateBackward(bc);
    }

    /**
     * Recursively iterates over all blocks reachable from this basic block and
     * sets their weights.
     *
     * @param  mark  keeps track of the visited blocks
     * @param  bc    the block closure
     * @param  d     distance from the start block
     * @see    #iterateAndSetWeight(BlockClosure)
     */
    private void iterateAndSetWeight(BitSet mark, BlockClosure bc, int d) {
        if (!mark.get(blockId)) {
            mark.set(blockId);
            calcWeight(d);
            bc.doBlock(this);
            for (int i = xhandlers.size() - 1; i >= 0; i--) {
                exceptionHandlerAt(i).iterateAndSetWeight(mark, bc, d + 1);
            }
            for (int i = subroutines.size() - 1; i >= 0; i--) {
                subroutineAt(i).iterateAndSetWeight(mark, bc, d + 1);
            }
            for (int i = end.countSux() - 1; i >= 0; i--) {
                end.suxAt(i).iterateAndSetWeight(mark, bc, d + 1);
            }
        }
    }

    /**
     * Applies the operation represented by the specified block closure to all
     * reachable blocks while setting their weights in one single pass.
     *
     * @param  bc  the block closure
     */
    public void iterateAndSetWeight(BlockClosure bc) {
        BitSet mark = new BitSet(countBlocks());
        iterateAndSetWeight(mark, bc, 0);
    }

    /**
     * Resolves value references to substituted values for all instructions in
     * this basic block.
     */
    public void resolveSubstitution() {
        doBlockValues(new ValueClosure() {
            public Instruction doValue(Instruction value) {
                return value.getSubst();
            }
        });
    }

    /**
     * Applies the operation represented by the specified value closure object
     * to each input and state value of each instruction in this basic block.
     *
     * @param  vc  the value closure
     * @see    Instruction#doValues(ValueClosure)
     */
    public void doBlockValues(ValueClosure vc) {
        for (Instruction x = this; x != null; x = x.getNext()) {
            x.doValues(vc);
        }
    }

    /**
     * Compares this basic block with the specified object for equality. Two
     * basic blocks are equal if and only if they have the same bytecode index.
     *
     * @param   obj  the reference object with which to compare
     * @return  whether or not the blocks are equal
     */
    public boolean equals(Object obj) {
        return (obj instanceof BlockBegin)
            && (((BlockBegin) obj).getBci() == getBci());
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doBlockBegin(this);
    }
}
