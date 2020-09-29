/*
 * @(#)Instruction.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.Assert;
import javac1.backend.items.Item;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.types.ValueType;

/**
 * The root of the instruction node hierarchy. Instructions that must be
 * executed in the original order are marked as pinned, the execution order of
 * all other instructions is only constrained by their data dependencies.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public abstract class Instruction {
    /**
     * The unique identification number of this instruction.
     */
    private int instrId;

    /**
     * The value type of this instruction.
     */
    private ValueType type;

    /**
     * Whether or not this instruction is pinned.
     */
    private boolean pinned;

    /**
     * The next instruction if any or <code>null</code> for end nodes.
     */
    private Instruction next;

    /**
     * The bytecode index of this instruction.
     */
    private int bci;

    /**
     * The count of instructions using this value.
     */
    private int useCount;

    /**
     * The substitution instruction if any.
     */
    private Instruction subst;

    /**
     * The machine-specific information for this instruction.
     */
    private Item item;

    /**
     * Initializes the attributes declared in this class.
     *
     * @param  scope  scope containing this instruction
     * @param  type   type of the instruction
     */
    protected Instruction(IRScope scope, ValueType type) {
        this.instrId = scope.getParent().getNextInstrId();
        this.type = type;
        this.pinned = false;
        this.next = null;
        this.bci = -1;
        this.useCount = 0;
        this.subst = null;
        this.item = null;
    }

    /**
     * A short cut for setting the bytecode index of an instruction and
     * installing it as this instruction's successor. For the reason of comfort
     * the specified successor is also returned again.
     *
     * @param   next  the successor
     * @param   bci   bytecode index of the successor
     * @return  the successor
     */
    public Instruction append(Instruction next, int bci) {
        if (next != null) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(!(this instanceof BlockEnd), "end nodes must not have next");
            }
            next.setBci(bci);
        }
        this.next = next;
        return next;
    }

    /**
     * Sets the instruction that follows this one.
     *
     * @param  next  the next instruction
     */
    public void setNext(Instruction next) {
        this.next = next;
    }

    /**
     * Returns the successor of this instruction.
     *
     * @return  the next instruction
     */
    public Instruction getNext() {
        return next;
    }

    /**
     * Returns the predecessor of this instruction. This is a very expensive
     * operation and should be used carefully.
     *
     * @param   block  basic block that contains this instruction
     * @return  the previous instruction
     */
    public Instruction getPrev(BlockBegin block) {
        Instruction prev = null;
        Instruction cur = block;
        while (cur != this) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(cur != null, "instruction not in basic block");
            }
            prev = cur;
            cur = cur.getNext();
        }
        return prev;
    }

    /**
     * Sets the bytecode index of this instruction.
     *
     * @param  bci  the bytecode index
     */
    public void setBci(int bci) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(bci >= 0, "illegal bytecode index");
        }
        this.bci = bci;
    }

    /**
     * Returns the bytecode index of this instruction.
     *
     * @return  the bytecode index
     */
    public int getBci() {
        return bci;
    }

    /**
     * Marks this instruction as pinned or not pinned.
     *
     * @param  pinned  whether to pin or unpin the instruction
     */
    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    /**
     * Returns whether or not this instruction is pinned.
     *
     * @return  whether or not this instruction is pinned
     */
    public boolean isPinned() {
        return pinned || javac1.Flags.PinAllInstructions;
    }

    /**
     * Returns whether or not this instruction is the root of an instruction
     * tree. By marking pinned instructions and instructions that are referred
     * to more than once as roots, the directed acyclic control flow graph is
     * broken into trees.
     *
     * @return  whether or not this instruction is a root
     */
    public boolean isRoot() {
        return isPinned() || (useCount > 1);
    }

    /**
     * Returns the unique identification number of this instruction.
     *
     * @return  the number of this instruction
     */
    public int getId() {
        return instrId;
    }

    /**
     * Returns the value type of this instruction.
     *
     * @return  the type of this instruction
     */
    public ValueType getType() {
        return type;
    }

    /**
     * Sets the use count of this instruction to the specified value.
     *
     * @param  useCount  the new use count value
     */
    public void setUseCount(int useCount) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(useCount >= 0, "illegal use count");
        }
        this.useCount = useCount;
    }

    /**
     * Returns the use count of this instruction. It specifies the number of
     * instructions that use this value.
     *
     * @return  the use count
     */
    public int getUseCount() {
        return useCount;
    }

    /**
     * Increments the use count of this instruction.
     */
    public void use() {
        useCount++;
    }

    /**
     * Sets the instruction that this instruction has to be substituted by.
     *
     * @param  subst  the substitution instruction
     * @see    BlockBegin#resolveSubstitution()
     */
    public void setSubst(Instruction subst) {
        this.subst = subst;
    }

    /**
     * Returns the instruction that this instruction has to be substituted by.
     * The method recursively determines the last instruction in the chain of
     * substitutions.
     *
     * @return  the substitution instruction
     * @see     BlockBegin#resolveSubstitution()
     */
    public Instruction getSubst() {
        return (subst == null) ? this : subst.getSubst();
    }

    /**
     * Links machine-specific information to this instruction.
     *
     * @param  item  the machine-specific information
     */
    public void setItem(Item item) {
    	if (Assert.ASSERTS_ENABLED) {
            Assert.that(item != null, "item must exist");
        }
        this.item = item;
    }

    /**
     * Removes the machine-specific information from this instruction.
     */
    public void clearItem() {
        item = null;
    }

    /**
     * Returns the machine-specific information that this instruction maintains.
     *
     * @return  the machine-specific information
     */
    public Item getItem() {
        return item;
    }

    /**
     * Returns whether or not this instruction can trap.
     *
     * @return  whether or not this instruction can trap
     */
    public boolean canTrap() {
        return false;
    }

    /**
     * Helper function for calculating hash codes.
     *
     * @see  #hashCode()
     */
    protected final int hash(int x) {
        return (getClass().hashCode() << 7) ^ x;
    }

    /**
     * Helper function for calculating hash codes.
     *
     * @see  #hashCode()
     */
    protected final int hash(int x, int y) {
        return (hash(x) << 7) ^ y;
    }

    /**
     * Helper function for calculating hash codes.
     *
     * @see  #hashCode()
     */
    protected final int hash(int x, int y, int z) {
        return (hash(x, y) << 7) ^ z;
    }

    /**
     * Returns a hash code value for the instruction. This value is used for
     * common subexpression elimination. The method can return 0 to indicate
     * that the instruction should not be considered for value numbering.
     *
     * @return  a hash code value for the instruction
     */
    public int hashCode() {
        return 0;
    }

    /**
     * Compares two instructions for equality. The result of this comparison is
     * used for common subexpression elimination.
     *
     * @param   obj  the reference object with which to compare
     * @return  whether or not the instructions are equal
     * @see     #hashCode
     */
    public boolean equals(Object obj) {
        return false;
    }

    /**
     * Applies the operation represented by the specified value closure object
     * to each input value that this instruction operates on.
     *
     * @param  vc  the value closure
     */
    public void doInputValues(ValueClosure vc) {
        /* nothing to do */
    }

    /**
     * Applies the operation represented by the specified value closure object
     * to each value in the state array of this instruction.
     *
     * @param  vc  the value closure
     */
    public void doStateValues(ValueClosure vc) {
        /* nothing to do */
    }

    /**
     * Applies the operation represented by the specified value closure object
     * to each input and state value of this instruction.
     *
     * @param  vc  the value closure
     */
    public void doValues(ValueClosure vc) {
        doInputValues(vc);
        doStateValues(vc);
    }

    /**
     * Calls the visitor's method that corresponds to this kind of instruction.
     *
     * @param  visitor  the instruction visitor
     */
    public abstract void visit(InstructionVisitor visitor);
}
