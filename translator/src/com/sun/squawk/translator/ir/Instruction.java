
package com.sun.squawk.translator.ir;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.*;
import com.sun.squawk.translator.util.*;



import java.io.InputStream;
import java.io.DataInputStream;

/**
 * This is the abstract super class for all IR instruction classes.
 */
public abstract class Instruction {

    /**
     * The enclosing method of the instruction. This is initialized by the
     * constructor for instructions that push a value to the operand stack. Invoking type() on any
     * other instruction object will result in a RuntimeException.
     */
    protected Type type;
    /** Link to lexical successor in a linked list of Instructions. */
    private Instruction next;
    /** Link to lexical predecessor in a linked list of Instructions. */
    private Instruction prev;
    /** The address of this instruction in the original bytecode. */
    private int originalIp = -1;
    /** An address assigned to the instruction for the purpose of relocation. */
    private int relocIp = -1;
    /** The opcode(s) of the dup/swap instruction(s) that followed this one (if any). */
    private byte[] dupSwapSuccessors;
    /** The linked list of other instructions that write to the same stack slot
        as this instruction. */
    private Instruction nextMergee;


    /**
     * The temporary local variable used to represent the value pushed by
     * this instruction onto the operand stack.
     */
    private TemporaryLocal temporaryLocal;

    /** If true, the value pushed onto the operand stack by this instruction was duped. */
    private boolean duped = false;
    /** If true, the value pushed by this instruction is spilled to a local. */
    private boolean spills;

    /**
     * A value used to provide a heuristic value for the dynamic execution frequency
     * of this instruction for the purpose of local variable/register allocation.
     */
    private int loopDepth = 1;

    /** The source line number of the instruction. */
    private int sourceLineNo = -1;

    /** The original instruction for which this one was synthesized. */
    private Instruction original;

/*---------------------------------------------------------------------------*\
 *                        Constructors                                       *
\*---------------------------------------------------------------------------*/

    /**
     * Construct an Instruction.
     * @param type The type of the value pushed onto the operand stack or stored to a
     * local variable by this instruction.
     */
    Instruction(Type type) {
        this.type = type;
    }

/*---------------------------------------------------------------------------*\
 *                        Type accessor methods                              *
\*---------------------------------------------------------------------------*/

    /**
     * Return the type of the value pushed onto the operand stack or stored to a
     * local variable by this instruction.
     */
    public Type type() {
//        Assert.that(type != null, this.getClass().toString() + " does not push to the operand stack");
        return type;
    }

    /**
     * Modify the type of the value pushed onto the operand stack or stored to a
     * local variable by this instruction.
     * @param type The new type.
     */
    public void changeType(Type type) {
        Assert.that(this.type != null, this.getClass().toString() + " does not push to the operand stack");
        this.type = type;
    }

/*---------------------------------------------------------------------------*\
 *                        InstructionList methods                            *
\*---------------------------------------------------------------------------*/

    /**
     * Insert 'after' into a linked list of instructions after this one.
     * @param after
     */
    void insertAfter(Instruction after) {
        Assert.that(after != this);
        if (after == this.next) {
            Assert.that(after.prev == this);
            return;
        }
        after.prev = this;
        after.next = this.next;
        if (this.next != null) {
            this.next.prev = after;
        }
        this.next = after;
    }

    /**
     * Insert 'before' into a linked list of instructions before this one.
     * @param before
     */
    void insertBefore(Instruction before) {
        Assert.that(before != this);
        if (before == this.prev) {
            Assert.that(before.next == this);
            return;
        }
        before.next = this;
        before.prev = this.prev;
        if (this.prev != null) {
            this.prev.next = before;
        }
        this.prev = before;
    }

    /**
     * Remove this instruction from its enclosing linked list of instructions.
     */
    void removeFromList() {
        if (prev == null) {
            if (next != null) {
                next.prev = null;
            }
        }
        else {
            prev.next = next;
            if (next != null) {
                next.prev = prev;
            }
        }
    }

    /**
     * Get the lexical successor of this instruciton.
     */
    public Instruction getNext() {
        return next;
    }

    /**
     * Get the lexical predecessor of this instruciton.
     */
    public Instruction getPrev() {
        return prev;
    }

/*---------------------------------------------------------------------------*\
 *                        Basic block delimiter methods                      *
\*---------------------------------------------------------------------------*/

    /**
     * Does control flow fall through from this instruction to its lexical successor.
     * @return
     */
    public boolean fallsThrough() {
        return true;
    }

/*---------------------------------------------------------------------------*\
 *                        Miscellaneous methods                              *
\*---------------------------------------------------------------------------*/

    /**
     * Return the translator context of this instruction.
     * @return the translator context of this instruction.
     */
    public Translator getVM() {
        return type.vm();
    }

    /**
     * Set the original bytecode address and source line number of this instruction.
     */
    public void setContext(int ip, int sourceLine) {
        this.originalIp = ip;
        this.sourceLineNo = sourceLine;
    }

    /**
     * Return the original IP of this instruction (will be -1 for a generated instruction).
     * @return the original IP of this instruction (will be -1 for a generated instruction).
     */
    public int getOriginalIP() {
        return originalIp;
    }

    /**
     * Return the address assigned to this instruction for the purpose of relocation.
     * This value is only meaningful during relocation as the instruction list
     * may be modified.
     */
    public int getRelocIP() {
        return relocIp;
    }

    /**
     * Set the address of this instruction for relocation.
     */
    public void setRelocIP(int ip) {
        this.relocIp = ip;
    }

    /**
     * Set the source line number of this instruction.
     */
    public void setSourceLineNo(int n) {
        this.sourceLineNo = n;
    }

    /**
     * Get the source line number of this instruction.
     */
    public int getSourceLineNo() {
        if (original != null) {
            return original.sourceLineNo;
        }
        return sourceLineNo;
    }

    /**
     * Return true if the value pushed or stored by this instruction is a
     * double word value.
     */
    public boolean isTwoWords() {
        return type().isTwoWords();
    }

    /**
     * Return a String representation of this instruction.
     */
    public abstract String toString();

    /**
     * Return a String representation of this instruction.
     * @param opcode More specific opcode info. This is optional and
     * will typically be null.
     */
    public String toString(boolean relocIp, boolean originalIp, boolean srcLine) {
        if (relocIp || originalIp || srcLine) {
            StringBuffer buf = new StringBuffer(toString());
            buf.append("\t{");
            if (relocIp) {
                buf.append(" relocIp=").append(this.relocIp);
            }
            if (originalIp) {
                buf.append(" ip=").append(this.originalIp);
            }
            if (srcLine && sourceLineNo > 0) {
                buf.append(" src=").append(this.sourceLineNo);
            }
            return buf.append(" }").toString();
        }
        else {
            return toString();
        }
    }

    protected String annotateType(Type type) {
        return " {"+type.toSignature()+"}";
    }

    /**
     * Set the temporary local variable used to represent the value pushed by
     * this instruction onto the operand stack.
     */
    public void setTemporaryLocal(TemporaryLocal temporaryLocal) {
        Assert.that(temporaryLocal != null);
        Assert.that(this.temporaryLocal == null);
        this.temporaryLocal = temporaryLocal;
    }

    /**
     * Get the temporary local variable used to represent the value pushed by
     * this instruction onto the operand stack.
     */
    public TemporaryLocal getTemporaryLocal() {
        return temporaryLocal;
    }

    public void setOriginal(Instruction original) {
        Assert.that(this.original == null);
        this.original = original;
    }

    public Instruction getOriginal() {
        return original;
    }

    public void spills() {
        spills = true;
    }

    public void cancelSpilling() {
        spills = false;
    }

    public boolean needsSpilling() {
        return spills;
    }

    public void addDupSwapSuccessor(int opcode) {
        if (dupSwapSuccessors == null) {
            dupSwapSuccessors = new byte[] {(byte)opcode};
        }
        else {
            int length = dupSwapSuccessors.length;
            dupSwapSuccessors = Arrays.copy(dupSwapSuccessors, 0, length+1, 0, length);
            dupSwapSuccessors[length] = (byte)opcode;
        }
    }

    public byte[] getDupSwapSuccessors() {
        return dupSwapSuccessors;
    }

    /**
     * @return true if there should be nothing on the stack at this instruction
     * apart from its operands.
     */
    public boolean constrainsStack() {
        return false;
    }

    /**
     * Set/unset the flag indicating if the value pushed onto the operand stack
     * by this instruction is duped.
     */
    public void setDuped(boolean flag) {
        duped = flag;
    }

    /**
     * Return true if the value pushed onto the operand stack by this instruction was duped.
     */
    public boolean wasDuped() {
        return duped;
    }

    /**
     * Increment (by one) the loop nesting level of this instruction.
     */
    public void incrementLoopDepth() {
        loopDepth++;
    }

    /**
     * Get the loop nesting level of this instruction.
     */
    public int getLoopDepth() {
        return loopDepth;
    }

    /**
     * Instructions that derive from a Java instruction that references a type
     * explicitly (i.e. one of its operands is an index into a constant pool
     * entry that references a CONSTANT_Class entry). This default
     * implementation returns null.
     */
    public Type getReferencedType() {
        return null;
    }

    /**
     * Instructions that need to add an entry to the constant object pool in the
     * generated Suite file return that object with this method.
     */
    public Object getConstantObject() {
        return null;
    }

    /**
     * Entry point for a visit from an InstructionVisitor object.
     *
     * @param visitor The InstructionVisitor object.
     */
    public abstract void visit(InstructionVisitor visitor) throws LinkageException;

    /**
     * Entry point for a visit from a ParameterVisitor object.
     *
     * @param visitor The ParameterVisitor object.
     */
    public void visit(ParameterVisitor visitor) {}

    /**
     * Do a linear traverse (i.e. linked list traversal) of an IR starting at a given the head of the IR,
     * visiting each node with a given InstructionVisitor.
     *
     * @param head The head of the IR traversal.
     * @param visitor The InstructionVisitor instance.
     * @return the head of the IR
     */
    public static Instruction visit(Instruction head, InstructionVisitor visitor) throws LinkageException {
        Instruction inst = head;
        while (inst != null) {
            inst.visit(visitor);
            inst = inst.getNext();
        }
        return head;
    }

    /**
     * Do a linear traverse (i.e. linked list traversal) of an IR starting at a given the head of the IR,
     * visiting each node with a given ParameterVisitor.
     * @param head The head of the IR traversal.
     * @param visitor The ParameterVisitor instance.
     * @return the head of the IR
     */
    public static Instruction visit(Instruction head, ParameterVisitor vistor) {
        Instruction inst = head;
        while (inst != null) {
            inst.visit(vistor);
            inst = inst.getNext();
        }
        return head;
    }

/*---------------------------------------------------------------------------*\
 *                  Methods for merging at phi instructions                  *
\*---------------------------------------------------------------------------*/

    /**
     * Return true if this instruction wrote to a stack slot that was written
     * to by another instruction on a different control flow path that merges
     * with the control flow path of this instruction.
     * @return
     */
    public boolean wasMerged() {
        return temporaryLocal != null && temporaryLocal.isMerged();
    }

    /**
     * Merge two Instructions
     * @param i1
     * @param i2
     * @param mergeType
     * @return
     */
    public static Instruction merge(Instruction i1, Instruction i2, Type mergeType) {
        if (i1.findMergee(i2)) {
            return i1;
        } else if (i2.findMergee(i1)) {
            return i2;
        } else {
            i1.mergeWith(i2, mergeType);
            return i1;
        }
    }

    private boolean findMergee(Instruction mergee) {
        Assert.that(this != mergee);
        for (Instruction inst = nextMergee; inst != null; inst = inst.nextMergee) {
            if (inst == mergee) {
                return true;
            }
        }
        return false;
    }

    private void mergeWith(Instruction other, Type mergeType) {
        Assert.that(other.temporaryLocal != null);
        Assert.that(other.temporaryLocal.slot() == temporaryLocal.slot() || duped || other.duped);

        // Update the type of the temporary local as well as the
        // type of this instruction if it does not match
        // the stack map specified type at the merge point.
        if (temporaryLocal.type() != mergeType) {
            temporaryLocal.changeType(mergeType);
            changeType(mergeType);
        }
        temporaryLocal.setMerged();
        for (Instruction inst = other; inst != null; inst = inst.nextMergee) {
            Assert.that(inst != this);
            inst.temporaryLocal = this.temporaryLocal;
            inst.spills = true;
        }

        Instruction tail = this;
        while (tail.nextMergee != null) {
            Assert.that(tail != other);
            tail = tail.nextMergee;
        }

        tail.nextMergee = other;
    }
}
