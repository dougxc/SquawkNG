
package com.sun.squawk.translator.ir;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.*;
import com.sun.squawk.translator.util.*;

import java.io.InputStream;
import java.io.DataInputStream;
import java.util.Vector;

/**
 * This class represents a position in the bytecode that is the destination of
 * control flow instruction and/or an exception entry point container. These
 * points will always have a recorded stack map (otherwise it is a
 * verification error). At the point of contruction, the instruction may not
 * yet be known and so can be set later. However, it can only be set once.
 */
public final class Target extends BytecodeAddress {

    /** The instruction at the address represented by this object. */
    private Instruction instruction;
    /** Flag to indicate that this is the target of a backward branch. */
    boolean isBackwardTarget;
    /**
     * Exception class type. This is only given a non-null value if the instruction
     * at this address is the entry point to an exception handler.
     */
    private Type exceptionType;
    /** The stack map entry recorded for this address. */
    private final StackMap.Entry stackMapEntry;

    /**
     * Constructor.
     * @param ip The address of this target.
     * @param
     */
    public Target(int ip, StackMap.Entry stackMapEntry) {
        super(ip);
        Assert.that(stackMapEntry != null);
        this.stackMapEntry = stackMapEntry;
    }

/*---------------------------------------------------------------------------*\
 *              Exception handler entry point methods                        *
\*---------------------------------------------------------------------------*/

    /**
     * Set the exception type for this target. This implies that this target is
     * an exception handler entry point.
     */
    public void setExceptionType(Type type) {
        // The inlining done for try-finally blocks by the CLDC preverifier
        // may result in the exception type for a handler being set more than once
        Assert.that(exceptionType == null || exceptionType == type);
        exceptionType = type;
    }

    /**
     * Get the exception type for the exception handler whose entry point is
     * represented by this object.
     */
    public Type getExceptionType() {
        return exceptionType;
    }

    /**
     * Return true if this object represents the entry point for an exception handler.
     */
    public boolean isExceptionTarget() {
        return exceptionType != null;
    }

/*---------------------------------------------------------------------------*\
 *                        Target instruction methods                         *
\*---------------------------------------------------------------------------*/

    /**
     * Set the instruction at the address represented by this object.
     * @param inst The instruction at the address represented by this object.
     */
    public void setInstruction(Instruction inst) {
        Assert.that(instruction == null);
        Assert.that(inst instanceof Phi || inst instanceof LoadException);
        instruction = inst;
    }

    /**
     * Return the instruction at the address represented by this object.
     *
     * @return the instruction at the address represented by this object.
     */
    public Instruction getInstruction() {
        return instruction;
    }

    /**
     * Get the relocation address of this target. This can only be called once a
     * target has had its target instruction assigned as it get the relocation
     * address from that instruction.
     */
    public int getRelocIP() {
        Assert.that(instruction != null);
        return instruction.getRelocIP();
    }

/*---------------------------------------------------------------------------*\
 *                      Stack map methods                                    *
\*---------------------------------------------------------------------------*/

    /**
     * Return the stack map entry recorded for this target.
     */
    public StackMap.Entry getStackMapEntry() {
        return stackMapEntry;
    }

    /**
     * Initialize an array of Locals according to the stack map at this target.
     * @param locals
     * @return the next available local index.
     */
    public void initLocalsFromStackMap(ExecutionFrame frame, Local[] locals) {
        Type[] localTypes = stackMapEntry.locals;
        Assert.that(locals.length >= localTypes.length);
        int javacIndex = 0;
        while (javacIndex < localTypes.length) {
            Type localType = localTypes[javacIndex];
            if (localType != localType.vm().BOGUS) {
                Local local = frame.allocLocal(localType, javacIndex, getIP(), null);
                locals[javacIndex++] = local;
                if (localType.isTwoWords()) {
                    locals[javacIndex++] = local.secondWordLocal();
                }
            }
            else {
                locals[javacIndex++] = null;
            }
        }
        while (javacIndex != locals.length) {
            locals[javacIndex++] = null;
        }
    }

/*---------------------------------------------------------------------------*\
 *                           Merging methods                                 *
\*---------------------------------------------------------------------------*/

    /** The merged stack state from each in edge to this target. */
    private Instruction[] stack;
    /** The merged live locals from each in edge to this target. */
    private Local[] locals;

    /**
     * Merge the stack state at this merge point with the stack state derived
     * on another branch to this merge point.
     * @param frameStack The stack state on the incoming branch.
     * @param jsp The height of the stack.
     * @param frame The frame state on the incoming branch.
     */
    public void mergeStackWithFrame(Instruction[] frameStack, int jsp, ExecutionFrame frame) {
        if (!isExceptionTarget()) {
            Assert.that(jsp <= frameStack.length);
            if (stack == null) {
                stack = (Instruction[]) Arrays.copy(frameStack, 0, new Instruction[jsp], 0, jsp);
            }
            else {
                Assert.that(jsp == stack.length);
                for (int i = 0; i != jsp; i++) {
                    if (stack[i] != frameStack[i]) {
                        stack[i] = frameStack[i] = Instruction.merge(stack[i], frameStack[i], stackMapEntry.stack[i]);
                    }
                }
            }
        }
    }

    /**
     * Determine whether or not the current stack state of a frame matches
     * the stack state saved at this target.
     * @param frameStack
     * @param jsp
     * @param frame
     * @return
     */
    public boolean stackMatchesFrame(Instruction[] frameStack, int jsp, ExecutionFrame frame) {
        if (!isExceptionTarget()) {
            Assert.that(jsp == stack.length);
            for (int i = 0; i != jsp; i++) {
                if (stack[i].getTemporaryLocal() != frameStack[i].getTemporaryLocal()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Merge the locals state at this merge point with the locals state derived
     * on another branch to this merge point.
     * @param frameLocals The locals state on the incoming branch.
     */
    public void mergeLocalsWithFrame(Local[] frameLocals) {
        /*
         * Merging of locals relies upon stack maps to specify liveness. Only the
         * locals that have a non-BOGUS entry in the stack map are live and it is
         * only these variables that need to be merged - they are exactly the same
         * variable.
         */
        if (locals == null) {
            locals = new Local[frameLocals.length];
            for (int javacIndex = 0; javacIndex != locals.length; javacIndex++) {
                Local stackMapLocal = mergeFrameLocalAndStackMapType(frameLocals[javacIndex], javacIndex);
                locals[javacIndex] = stackMapLocal;
            }
        }
        else {
            Assert.that(locals.length == frameLocals.length);
            for (int javacIndex = 0; javacIndex != locals.length; javacIndex++) {
                Local frameLocal    = frameLocals[javacIndex];
                Local stackMapLocal = mergeFrameLocalAndStackMapType(frameLocal, javacIndex);
                Local local         = locals[javacIndex];

                // The saved merged state must correspond with the stack map state
                Assert.that((local == null) == (stackMapLocal == null));

                if (stackMapLocal != null) {
                    // The local variable is live - needs merging
                    Assert.that(frameLocal != null);
                    if (frameLocal != local) {
                        if (local.type() != stackMapLocal.type()) {
                            if (local.mergeParent() != null) {
                                local = local.mergeRoot();
                            }
                            local.changeType(stackMapLocal.type());
                        }
                        Local merged = Local.merge(frameLocal, local);
                        frameLocals[javacIndex] = locals[javacIndex] = merged;
                        Assert.that(merged.mergeParent() == null);
                    }
                }
            }
        }
    }

    /**
     * Merge the current local with the type defined by the stack map.
     * @param local
     * @param javacIndex
     * @return
     */
    private Local mergeFrameLocalAndStackMapType(Local frameLocal, int javacIndex) {
        if (javacIndex >= stackMapEntry.locals.length) {
            // The local is undefined at this target
            return null;
        }

        Type stackMapType = stackMapEntry.locals[javacIndex];
        Translator vm = stackMapType.vm();
        if (stackMapType == vm.BOGUS) {
            return null;
        }
        Assert.that(frameLocal != null);
        if (frameLocal.type() != stackMapType) {
            if (stackMapType != vm.NULLOBJECT) {
                frameLocal.changeType(stackMapType);
            }
        }
        return frameLocal;
    }
    /**
     * Return the stack state for this merge point.
     * @return the stack state for this merge point.
     */
    public Instruction[] getStack() {
        return stack;
    }

    /**
     * Return the local variables for this merge point.
     * @return the local variables for this merge point.
     */
    public Local[] getLocals() {
        return locals;
    }

/*---------------------------------------------------------------------------*\
 *                      Misc                                                 *
\*---------------------------------------------------------------------------*/

    /**
     * There should never be a clash between two Target objects being at the
     * same address.
     */
    public int secondaryKey() {
        Assert.shouldNotReachHere();
        return 0;
    }

    /**
     * Return the pseudo-opcode representing this target in an instruction stream.
     */
    public int opcode() {
        return isExceptionTarget() ? JVMConst.opc_exceptiontarget :
                                     JVMConst.opc_branchtarget;
    }

    /**
     * Return true if this target has a source at a higher address in the bytecode array.
     * @return true if this target has a source at a higher address in the bytecode array.
     */
    public boolean isBackwardTarget() {
        return isBackwardTarget;
    };

    /**
     * Given a source instruction for this target, mark this target as a backward target
     * if the source instruction is at a higher address than this target.
     */
    public void isTargetFor(Instruction inst) {
        if (inst.getOriginalIP() > getIP()) {
            isBackwardTarget = true;
        }
    }

    /**
     * Return a String representation of this target.
     */
    public String toString() {
        return (exceptionType == null ? "Target(": "ExceptionHandler(type: " + exceptionType + " ") +
            "ip: " + getIP() + ")";
    }
}
