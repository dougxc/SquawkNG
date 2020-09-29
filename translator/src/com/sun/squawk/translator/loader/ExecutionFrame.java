package com.sun.squawk.translator.loader;

import java.util.Vector;
import java.util.Enumeration;
import com.sun.squawk.translator.*;
import com.sun.squawk.util.IntHashtable;
import com.sun.squawk.translator.ir.*;
import com.sun.squawk.translator.Assert;
import com.sun.squawk.translator.util.JVMConst;
import com.sun.squawk.translator.util.VEConst;
import com.sun.squawk.translator.util.Tracer;
import com.sun.squawk.translator.util.Arrays;
import com.sun.squawk.translator.util.BitSet;

import java.util.Hashtable;
//import java.util.Iterator;
//import java.util.Map;



/**
 * This class emulates the usage of local variables and operand stack by a given method.
 */
public class ExecutionFrame {

    /**
     * Value indicating that it is OK for local variables slots
     * to be reused for different types.
     */
    public static final int FLAG_REUSABLE_LOCAL_VARIABLES  = 1 << 0;

    /**
     * Value indicating that an ExecutionFrame is to allocate
     * instances of TemporaryLocal to model the use of stack
     * slots by the instructions in the method.
     */
    public static final int FLAG_ALLOCATE_STACK_VARIABLES  = 1 << 1;

    /**
     * Value indicating that logically equivalent local variables
     * (i.e. those representing the local variable slot) are to be
     * merged at control flow merge points.
     */
    public static final int FLAG_MERGE_LOCAL_VARIABLES     = 1 << 2;

    /**
     * A mask of all the invalid flag bits.
     */
    private static final int FLAG_INVALID_MASK =
        ~(FLAG_REUSABLE_LOCAL_VARIABLES |
          FLAG_ALLOCATE_STACK_VARIABLES |
          FLAG_MERGE_LOCAL_VARIABLES);

    /** Max stack. */
    private int maxStack;
    /** The context object for verification errors. */
    private final LinkageException.Context errorContext;
    /** The method. */
    /*private*/ final Method method;
    /** The Translator context. */
    /*private*/ final Translator vm;
    /** Operand stack. */
    private Instruction[] stack;
    /** Current logical size of the operand stack. */
    private int jsp = 0;
    /** Current word size of the operand stack. */
    private int jspWords = 0;
    /** The computed max stack (in words). */
    private int computedMaxStackInWords = 0;
    /** Controls tracing. */
    private final Tracer tracer;
    /** The local variables of a method. Long and double variables take two entries. */
    private final Local[] locals;
    /** The Locals allocated for the parameters of the method. */
    private final Local[] parmLocals;
    /** The instruction factory. */
    private final InstructionFactory ifactory;
    /** A mask of the FLAG_* values. */
    private final int flags;

    /** Cache of allocated locals. */
    private final Vector allocatedLocals;
    /** LocalVariableTable. */
    private final LocalVariableTable lvt;

    /**
     * Construct an ExecutionFrame.
     * @param vm
     * @param method
     * @param tracer
     * @param errorContext Used for providing extra context messages when throwing LinkageExceptions.
     * This can be null.
     * @param maxStack The maximum size of the stack. This can be -1 if the caller is using
     * the ExecutionFrame to calculate what this value should be.
     * @param locals The local variables.
     * @param flags
     * @param lvt
     */
    public ExecutionFrame(Translator vm,
                          Method method,
                          boolean trace,
                          LinkageException.Context errorContext,
                          InstructionFactory ifactory,
                          int maxStack,
                          Local[] locals,
                          int flags,
                          LocalVariableTable lvt) throws LinkageException
    {
        Assert.that((flags & FLAG_INVALID_MASK) == 0);
        this.method       = method;
        this.vm           = method.vm();
        this.maxStack     = maxStack;
        this.locals       = locals;
        this.errorContext = errorContext;
        this.ifactory     = ifactory;
        this.allocatedLocals = new Vector();
        this.lvt          = lvt;
        this.tracer       = new Tracer(System.err, trace);
        this.flags        = flags;


        // Initialize the frame based on the method's parameters.

        // Print header
        printMethodInfo1();

        /*
         * It is not known how many logical Java stack entries are used
         * so allocate the number of physical words which is known.
         */
        stack = new Instruction[maxStack != -1 ? maxStack : 0];

        /*
         * Initialize locals from parameters.
         * While iterating through the method parameters, also assign the logical
         * parameter offset to the local variable that is allocated for
         * the physical offset for that parameter type. This is done in
         * order to know which local variables were parameters and what
         * logical offset they are when the CFG is build.
         */
        Type[] parms = method.getParms();
        int javacIndex = 0;
        int parmIndex = 0;
        /*
         * Allocate parm for receiver for non-static methods. The receiver type
         * is INITOBJECT if this is an <init> method (except for Object)
         * otherwise it is the class in which the method was defined (i.e. its parent).
         */
        if (!method.isStatic()) {
            Type parm = method.parent().localType();
            if (method.name() == vm.INIT && method.parent() != vm.OBJECT && isReusingLocalVariables()) {
                parm = vm.INITOBJECT;
            }
            Local local = allocLocal(parm, javacIndex, -1, "this");
            local.setParameterNumber(parmIndex++);
            javacIndex = setLocal(local, javacIndex) + 1;
            Assert.that(javacIndex == 1);
        }

        // Allocate locals for the parameters.
        for (int i = 0; i != parms.length; i++) {
            Type parm = parms[i].localType();
            // Allocate the IR local representing the current parameter
            Local local = allocLocal(parm, javacIndex, 0, null);
            local.setParameterNumber(parmIndex++);
            // Set the local variable slot
            javacIndex = setLocal(local, javacIndex) + 1;
        }

        // Make a copy of the parm locals
        parmLocals = new Local[javacIndex];
        for (int i = 0; i != javacIndex; i++) {
            Local l = this.locals[i];
            parmLocals[i] = l;
            if (l.type().isTwoWords()) {
                parmLocals[++i] = l.secondWordLocal();
            }
        }

        // Print header
        printMethodInfo2();

    }

    /**
     * Re-initialize the operand stack and local variables after an instruction
     * that does not have direct control flow to its lexical successor
     * (i.e.the current instruction).
     * @param target The target containing the stack map and (possibly empty)
     * merged state at this address.
     */
    public void reinitialize(Target target) throws LinkageException {
        reinitializeStack(target);
        reinitializeLocals(target);
    }

    /**
     * Re-initialize the operand stack after an instruction
     * that does not have direct control flow to its lexical successor
     * (i.e.the current instruction).
     * @param target The target containing the stack map and (possibly empty)
     * merged state at this address.
     */
    private void reinitializeStack (Target target) throws LinkageException {
        if (!target.isExceptionTarget()) {
            Instruction[] targetStack = target.getStack();
            if (targetStack == null) {
                // This is the destination of a backwards branch or an unreachable statement.
                Type[] stackMapTypes = target.getStackMapEntry().stack;
                jsp = 0;
                jspWords = 0;
                for (int i = 0; i != stackMapTypes.length; i++) {
                    Type stackMapType = stackMapTypes[i];
                    verify(! (stackMapType instanceof TypeProxy) &&
                           stackMapType != vm.INITOBJECT, VEConst.BACK_BRANCH_UNINIT);
                    Assert.that(ifactory != null);
                    Instruction inst = ifactory.createLoadSynthetic(stackMapType);
                    inst.setContext(target.getIP(), -1);
                    push(inst, true);
                }
            } else {
                jsp = targetStack.length;
                Assert.that(jsp <= stack.length);
                System.arraycopy(targetStack, 0, stack, 0, jsp);
                jspWords = 0;
                for (int i = 0; i != jsp; i++) {
                    jspWords += (stack[i].type().isTwoWords()) ? 2 : 1;
                }
            }
        } else {
            Assert.that(jsp == 0, "Stack should be empty at exception handler entry");
        }

        // Zero the unused stack slots
        for (int i = jsp; i != stack.length; i++) {
            stack[i] = null;
        }
    }

    /**
     * Re-initialize the local variables after an instruction
     * that does not have direct control flow to its lexical successor
     * (i.e.the current instruction).
     * @param target The target containing the stack map and (possibly empty)
     * merged state at this address.
     */
    private void reinitializeLocals(Target target) throws LinkageException {
        Local[] targetLocals = target.getLocals();
        if (targetLocals == null) {
            if (isReusingLocalVariables()) {
                target.initLocalsFromStackMap(this, locals);
            }
        } else {
            for (int i = 0; i != targetLocals.length; i++) {
                if (targetLocals[i] != null && targetLocals[i].mergeParent() != null) {
                    targetLocals[i] = targetLocals[i].mergeRoot();
                }
                locals[i] = targetLocals[i];
            }
        }
    }

    /**
     * This must be called when the method being emulated by this object is completed.
     * That is, the last instruction of the method has been emulated.
     */
    public void finish() {
        // Promote any 'unreal' types to appropriate real types.
        changeUnrealLocalTypes();

        // Update the locals representing parameters in case they were merged
        for (int i = 0; i != parmLocals.length; i++) {
            Local parmLocal = parmLocals[i];
            if (parmLocal.mergeParent() != null) {
                Local l = parmLocal.mergeRoot();
                parmLocals[i] = l;
                if (l.type().isTwoWords()) {
                    parmLocals[++i] = l.secondWordLocal();
                }
            }
        }

        for (int i = 0; i != locals.length; i++) {
            Assert.that(locals[i] == null || locals[i].mergeParent() == null);
        }
    }

    /**
     * Change any instances of uninitialized "this" type to a specified type.
     * @param aType the type to change to.
     */
    public void renameInitTo(Type aType) {
        for (int i = 0 ; i < locals.length ; i++) {
            if (localType(i) == vm.INITOBJECT) {
                locals[i].changeType(aType);
            }
        }
        for (int i = 0 ; i < jsp ; i++) {
            if (stack[i].type() == vm.INITOBJECT) {
                stack[i].changeType(aType);
            }
        }

        for (Enumeration e = allocatedLocals.elements(); e.hasMoreElements(); ) {
            Local local = (Local)e.nextElement();
            if (local.type() == vm.INITOBJECT) {
                local.changeType(aType);
            }
        }
    }

/*---------------------------------------------------------------------------*\
 *                     Configuration                                         *
\*---------------------------------------------------------------------------*/

    /**
     * Return true if the local variable slots are re-usable across
     * different types.
     * @return
     */
    final public boolean isReusingLocalVariables() {
        return (flags & FLAG_REUSABLE_LOCAL_VARIABLES) != 0;
    }

    /**
     * Return true if the TemporaryLocal's are being allocated for
     * stack slots.
     * @return
     */
    final public boolean isAllocatingStackVariables() {
        return (flags & FLAG_ALLOCATE_STACK_VARIABLES) != 0;
    }

    /**
     * Return true if logically equivalent local variables
     * (i.e. those representing the local variable slot) are to be merged at
     * control flow merge points.
     * @return
     */
    final public boolean isMergingLocalVariables() {
        return (flags & FLAG_MERGE_LOCAL_VARIABLES) != 0;
    }

/*---------------------------------------------------------------------------*\
 *                     Operand stack accessor methods                        *
\*---------------------------------------------------------------------------*/

    /**
     * Adjust the max stack limit. This is useful when adding extra instructions
     * that may require a larger stack limit or when calculating the max stack value.
     *
     * @param delta The adjustment amount.
     */
    public void adjustMaxStack(int delta) {
        maxStack += delta;
        if (maxStack > stack.length) {
            growStack(maxStack);
        }
    }

    private void growStack(int newSize) {
        Assert.that(newSize > stack.length);
        if (newSize > stack.length) {
            stack = (Instruction[])Arrays.copy(stack, 0, new Instruction[newSize], 0, stack.length);
        }
    }

    /**
     * Push a type onto the execution stack
     *
     * @param inst the instruction with the type
     * @return the same instruction
     */
    public Instruction push(Instruction inst, boolean alloc) throws LinkageException {
        Assert.that(inst != null);
        int size = (inst.type().isTwoWords() ? 2 : 1);
        if (maxStack == -1) {
            // grow the stack if necessary
            if (jsp >= stack.length) {
                growStack(jsp + 1);
            }
        }
        else {
            verify(jspWords+size <= maxStack, VEConst.STACK_OVERFLOW+" (maxStack = "+maxStack+")");
        }
        stack[jsp++] = inst;
        jspWords += size;
        if (jspWords > computedMaxStackInWords) {
            computedMaxStackInWords = jspWords;
        }

        TemporaryLocal local;
        if (alloc) {
            local = new TemporaryLocal(inst.type().localType(), jsp-1);
            inst.setTemporaryLocal(local);
        } else {
            local = inst.getTemporaryLocal();
        }
        // The local will be null for instructions synthesized by the BytecodeProducer
        if (local != null) {
            local.incrementReferenceCount();
        }
        return inst;
    }


   /**
    * Push a type onto the execution stack
    *
    * @param inst the instruction with the type
    * @return the same instruction
    */
    public void pushForDup(Instruction inst) throws LinkageException {
        push(inst, false);
    }

   /**
    * Pop a type from the execution stack
    *
    * @param type the type that should result
    * @param free TRUE if the instruction's local should be freed
    * @return the instruction popped from the stack
    */
    private Instruction pop(Type type, boolean free, int ip) throws LinkageException {
        Assert.that(type != null);
        verify(jsp > 0, VEConst.STACK_UNDERFLOW);
        int size = (stack[jsp - 1].type().isTwoWords() ? 2 : 1);
        verify(jspWords >= size, VEConst.STACK_UNDERFLOW);
        Assert.that(stack.length != 0 && jsp > 0 && jsp <= stack.length);

        Instruction inst = stack[--jsp];
        jspWords -= size;

        if (type == vm.BOOLEAN         ||
            type == vm.BYTE            ||
            type == vm.BYTE_OR_BOOLEAN ||
            type == vm.SHORT           ||
            type == vm.CHAR)
        {
            type = vm.INT;
        }
        verify(inst.type().localType().vIsAssignableTo(type), VEConst.STACK_BAD_TYPE + ": " + inst.type().localType() + " is not assignable to " + type);

        TemporaryLocal local = inst.getTemporaryLocal();
        // The local will be null for instructions synthesized by the BytecodeProducer
        if (local != null) {
            local.decrementReferenceCount();
        }
        return inst;
    }

    /**
     * Pop a type from the execution stack
     *
     * @param type the type that should result
     * @return the instruction popped from the stack
     */
    public Instruction pop(Type type, int ip) throws LinkageException {
        return pop(type, true, ip);
    }

    /**
     * Pop a type from the execution stack.
     *
     * @param type the type that should result
     * @return the instruction popped from the stack
     */
    public Instruction pop(Type type) throws LinkageException {
        return pop(type, false, -1);
    }

    /**
     * Pop a type from the execution stack prior to a dup
     * This is the same as pop except the local is not freed
     *
     * @param type the type that should result
     * @return the same instruction on popped from the stack
     */
    private Instruction popForDup(Type type) throws LinkageException {
        Instruction value = pop(type, false, -1);
        return value;
    }

    /**
     * Pop a type that has a LinkageError from the execution stack.
     *
     * @param type the type that should result
     * @return the same instruction on popped from the stack
     */
    public void popLinkageErrorType() throws LinkageException {
        verify(jsp > 0, VEConst.STACK_UNDERFLOW);
        int size = (stack[jsp - 1].type().isTwoWords() ? 2 : 1);
        verify(jspWords >= size, VEConst.STACK_UNDERFLOW);
        Assert.that(stack.length != 0 && jsp > 0 && jsp <= stack.length);

        Instruction inst = stack[--jsp];
        jspWords -= size;

        Assert.that(inst.type().getLinkageError() != null);
    }

    /**
     * Return the Instruction at the top of the stack. This assumes that the
     * stack is not empty.
     * @return the Instruction at the top of the stack.
     */
    public Instruction peek() {
        Assert.that(jsp > 0);
        return stack[jsp - 1];
    }

    /**
     * Return the Instruction at a given (valid) stack position.
     * @param pos The stack position to return (0 = bottom of stack).
     * @return
     */
    public Instruction peekAt(int pos) {
        Assert.that(0 <= pos && pos < jsp);
        return stack[pos];
    }

    /**
     * Copy the current types on the stack into a given array.
     * @param stackState The array to copy into.
     * @param asWordSizedTypes
     * @return the number of elements copied into stackState
     */
    public int copyStackState(Type[] stackState, boolean asWordSizedTypes) {
        int length = 0;
        if (stackState != null) {
            for (int i = 0; i != jsp; i++) {
                Type type = stack[i].type();
                Assert.that(stackState.length > length);
                stackState[length++] = type;
                if (asWordSizedTypes && type.isTwoWords()) {
                    Assert.that(stackState.length > length);
                    stackState[length++] = type.secondWordType();
                }
            }
            Assert.that(length == (asWordSizedTypes ? jspWords : jsp));
        }
        return length;
    }

    /**
     * Get the current stack size.
     * @param inWords if true, then return the number of words on the stack otherwise
     * return the number of logical types on the stack.
     * @return the current stack size.
     */
    public int stackSize(boolean inWords) {
        return inWords ? jspWords : jsp;
    }

    /**
     * Get the computed maximum stack size.
     * @param inWords
     * @return
     */
    public int computedMaxStack(boolean inWords) {
        if (inWords) {
            return computedMaxStackInWords;
        }
        else {
            return stack.length;
        }
    }

/*---------------------------------------------------------------------------*\
 *                            Dup/Swap emulation                             *
\*---------------------------------------------------------------------------*/

    /**
     * Emulate the semantics of the dup/swap Java bytecodes.
     * @param opcode
     * @return true if this is 'dup' and it is duping the result of 'new'
     * @throws LinkageException
     */
    public boolean dupSwap(int opcode) throws LinkageException {
        boolean dupedNew = false;
        switch (opcode) {
            case JVMConst.opc_dup: {
                Instruction x1 = popForDup(vm.UNIVERSE);
                pushForDup(x1);
                pushForDup(x1);
                if (x1 instanceof NewObject) {
                    dupedNew = true;
                }
                x1.setDuped(true);
                break;
            }
            case JVMConst.opc_dup2: {
                Instruction x1 = popForDup(vm.UNIVERSE);
                if(x1.isTwoWords()) {
                    pushForDup(x1);
                    pushForDup(x1);
                    x1.setDuped(true);
                } else {
                    Instruction x2 = popForDup(vm.UNIVERSE);
                    pushForDup(x2);
                    pushForDup(x1);
                    pushForDup(x2);
                    pushForDup(x1);
                    x1.setDuped(true);
                    x2.setDuped(true);
                }
                break;
            }
            case JVMConst.opc_dup_x1: {
                Instruction x1 = popForDup(vm.UNIVERSE);
                Instruction x2 = popForDup(vm.UNIVERSE);
                pushForDup(x1);
                pushForDup(x2);
                pushForDup(x1);
                x1.setDuped(true);
                x2.setDuped(true);
                break;
            }
            case JVMConst.opc_dup_x2: {
                Instruction x1 = popForDup(vm.UNIVERSE);
                Instruction x2 = popForDup(vm.UNIVERSE);
                if(x1.isTwoWords()) {
                    pushForDup(x1);
                    pushForDup(x2);
                    pushForDup(x1);
                    x1.setDuped(true);
                    x2.setDuped(true);
                } else {
                    Instruction x3 = popForDup(vm.UNIVERSE);
                    pushForDup(x1);
                    pushForDup(x3);
                    pushForDup(x2);
                    pushForDup(x1);
                    x1.setDuped(true);
                    x2.setDuped(true);
                    x3.setDuped(true);
                }
                break;
            }
            case JVMConst.opc_dup2_x1: {
                Instruction x1 = popForDup(vm.UNIVERSE);
                if(x1.isTwoWords()) {
                    Instruction x2 = popForDup(vm.UNIVERSE);
                    pushForDup(x1);
                    pushForDup(x2);
                    pushForDup(x1);
                    x1.setDuped(true);
                    x2.setDuped(true);
                } else {
                    Instruction x2 = popForDup(vm.UNIVERSE);
                    Instruction x3 = popForDup(vm.UNIVERSE);
                    pushForDup(x2);
                    pushForDup(x1);
                    pushForDup(x3);
                    pushForDup(x2);
                    pushForDup(x1);
                    x1.setDuped(true);
                    x2.setDuped(true);
                    x3.setDuped(true);
                }
                break;
            }
            case JVMConst.opc_dup2_x2: {
                Instruction x1 = popForDup(vm.UNIVERSE);
                if(x1.isTwoWords()) {
                    Instruction x2 = popForDup(vm.UNIVERSE);
                    if(x2.isTwoWords()) {
                        pushForDup(x1);
                        pushForDup(x2);
                        pushForDup(x1);
                        x1.setDuped(true);
                        x2.setDuped(true);
                    } else {
                        Instruction x3 = popForDup(vm.UNIVERSE);
                        pushForDup(x1);
                        pushForDup(x3);
                        pushForDup(x2);
                        pushForDup(x1);
                        x1.setDuped(true);
                        x2.setDuped(true);
                        x3.setDuped(true);
                    }
                } else {
                    Instruction x2 = popForDup(vm.UNIVERSE);
                    Instruction x3 = popForDup(vm.UNIVERSE);
                    if(x3.isTwoWords()) {
                        pushForDup(x2);
                        pushForDup(x1);
                        pushForDup(x3);
                        pushForDup(x2);
                        pushForDup(x1);
                        x1.setDuped(true);
                        x2.setDuped(true);
                        x3.setDuped(true);
                    } else {
                        Instruction x4 = popForDup(vm.UNIVERSE);
                        pushForDup(x2);
                        pushForDup(x1);
                        pushForDup(x4);
                        pushForDup(x3);
                        pushForDup(x2);
                        pushForDup(x1);
                        x1.setDuped(true);
                        x2.setDuped(true);
                        x3.setDuped(true);
                        x4.setDuped(true);
                    }
                }
                break;
            }
            case JVMConst.opc_swap: {
                Instruction x1 = popForDup(vm.UNIVERSE);
                Instruction x2 = popForDup(vm.UNIVERSE);
                pushForDup(x1);
                pushForDup(x2);
                x1.setDuped(true);
                x2.setDuped(true);
                break;
            }
            default:
                Assert.that(false, "Unknown dup/swap opcode: "+opcode);
        }
        return dupedNew;
    }

/*---------------------------------------------------------------------------*\
 *                       Print/debug methods                                 *
\*---------------------------------------------------------------------------*/

    /**
     * Print the method information
     */
    public void printMethodInfo1() {
        tracer.traceln("");
        tracer.traceln("========================================================");
        tracer.traceln(    "               "+method);
    }

    public void printMethodInfo2() {
        tracer.traceln("     s="+maxStack+" l="+locals.length);
        tracer.traceln("   isStatic ="+method.isStatic());
        tracer.traceln("========================================================");
        tracer.traceln("");
    }



    /**
     * Print the state of the stack and local vars.
     */
    public void printState(boolean typesOnly) {
        if (!tracer.switchedOn()) {
            return;
        }
        tracer.traceln("");
        tracer.traceln("--------------------------------------------------------");
        if (locals.length > 0) {
            tracer.traceln("        local("+(locals.length)+") " + localsStateToString(true));
        }
        if (jsp > 0) {
            tracer.traceln("        stack("+jsp+") "+stackStateToString(typesOnly));
        }
        tracer.traceln("--------------------------------------------------------");
        tracer.traceln("");
    }

    public String stackStateToString(boolean typesOnly) {
        if (jsp == 0) {
            return "";
        }
        else {
            StringBuffer buf = new StringBuffer(jsp * 10);
            for(int i = 0 ; i < jsp ; i++) {
                if (typesOnly) {
                    Type type = stack[i].type();
                    buf.append(type.toSignature(false, true));
                }
                else {
                    buf.append(stack[i].getOriginalIP()+":"+stack[i]);
                }
                if (i < (jsp - 1)) {
                    buf.append(", ");
                }
            }
            return buf.toString();
        }
    }

    public String localsStateToString(boolean typesOnly) {
        if (locals.length == 0) {
            return "";
        }
        else {
            StringBuffer buf = new StringBuffer(locals.length * 10);
            for(int i = 0 ; i < locals.length ; i++) {
                Type localType = localType(i);
                if (typesOnly) {
                    buf.append(localType(i).toSignature(false, true));
                }
                else {
                    buf.append(locals[i]);
                }
                if (i < (locals.length - 1)) {
                    buf.append(", ");
                }
            }
            return buf.toString();
        }
    }

    public InstructionFactory getInstructionFactory() {
        return ifactory;
    }

    /**
     * Determine whether the stack or local variables currently contains an
     * uninitialized object.
     * @return the result of the test.
     */
    public boolean containsUninitializedObject(boolean ignoreStack) {
        for (int i = 0; i < locals.length; i++) {
            if (localType(i) == vm.INITOBJECT || localType(i) instanceof TypeProxy) {
                return true;
            }
        }
        if (!ignoreStack) {
            for (int i = 0; i < jsp; i++) {
                if (stack[i].type() == vm.NEWOBJECT || stack[i].type() instanceof TypeProxy) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper method to test a condition and and throw a VerifyError if it fails.
     * @param msg
     * @return
     * @throws LinkageException
     */
    public void verify(boolean condition, Object msg) throws LinkageException {
        if (!condition) {
            if (errorContext == null) {
                throw new LinkageException(vm.VERIFYERROR, msg.toString());
            }
            else {
                throw new LinkageException(vm.VERIFYERROR, msg.toString(), errorContext);
            }
        }
    }

    /**
     * Helper method to test an assertion regarding a constraint on the type of
     * code the translator knows how to translate.
     * @param condition
     * @param msg
     * @throws LinkageException
     */
    public void verifyTranslatable(boolean condition, String msg) throws LinkageException {
        if (!condition) {
            throw new LinkageException(vm.LINKAGEERROR, vm.UNTRANSLATABLECODEERROR+": "+msg);
        }
    }

/*---------------------------------------------------------------------------*\
 *                   Local variable allocation methods                       *
\*---------------------------------------------------------------------------*/

    /**
     * Allocate a new local Squawk variable.
     * @param type the initial inferred type of the variable
     * @param javacIndex the slot Javac assigned
     * @param the address of the definition of the local variable.
     * @return a Local
     */
    public Local allocLocal(Type type, int javacIndex, int javacIp, String name) {
        Assert.that(name == null || !name.equals("this") || allocatedLocals.size() == 0);
        if (name == null && javacIp >= 0) {
            name = LocalVariableTable.getEntry(lvt, javacIp, javacIndex).name;
        }
        Local local = new Local(type, javacIndex, name);
        allocatedLocals.addElement(local);
        return local;
    }

    /**
     * Change the type of all local variables that have type unreal types to an
     * appropriate type. There may be some locals for which a specific type than 'null' could
     * not be inferred. For example 's' in the following method:
     *
     *     void foo() {
     *         String s = null;
     *     }
     *
     * These variables are modified to have the type Object. Also, any remaining
     * instances of BYTE_OR_BOOLEAN_ARRAY are converted to BYTE_ARRAY.
     */
    private void changeUnrealLocalTypes() {
        // Change real locals
        for (Enumeration e = allocatedLocals.elements(); e.hasMoreElements(); ) {
            Local local = (Local)e.nextElement();
            Assert.that(!(local.type() instanceof TypeProxy));
            if (local.mergeParent() == null) {
                Type realType = getRealTypeForUnrealType(local.type(), local);
                if (realType != local.type()) {
                    local.changeType(realType);
                }
            }
        }
    }

    private Type getRealTypeForUnrealType(Type type, Local local) {
        Type base = type;
        int depth = 0;
        while(base.isArray()) {
            depth++;
            base = base.elementType();
        }

        if (base == vm.NULLOBJECT) {

            if (local != null) {
                // Try to infer the type of the local based on the type it is
                // expected to be by another instruction that uses it as a
                // parameter.
                LocalVariableInstruction ld = local.getLastUse();
                if (ld != null && ld instanceof LoadLocal) {
                    Type inferred = null;
                    for (Instruction inst = ld.getNext(); inst != null; inst = inst.getNext()) {
                        if (inst instanceof StoreLocal) {
                            StoreLocal st = (StoreLocal)inst;
                            if (st.value() == ld) {
                                inferred = st.local().type();
                                break;
                            }
                        } else if (inst instanceof LoadField) {
                            LoadField ldfield = (LoadField)inst;
                            if (ldfield.ref() == ld) {
                                inferred = ldfield.field().parent();
                                break;
                            }
                        } else if (inst instanceof StoreField) {
                            StoreField st = (StoreField)inst;
                            if (ld == st.ref()) {
                                inferred = st.ref().type();
                                break;
                            } else if (ld == st.value()) {
                                inferred = st.value().type();
                                break;
                            }
                        }
                    }
                    if (inferred != null) {
                        return getRealTypeForUnrealType(inferred, null);
                    }
                }
            }

            base = vm.OBJECT;
        } else if (base == vm.INITOBJECT) {
            base = method.parent();
        } else if (base == vm.BYTE_OR_BOOLEAN) {
            base = vm.BYTE;
        } else {
            // Ensure that 'base' is now a real type
            Assert.that(base.specID() != -2);
            return type;
        }
        for (int i = 0; i != depth; i++) {
            base = base.asArray();
        }
        return base;
    }
/*---------------------------------------------------------------------------*\
 *                   Local variable access methods                           *
\*---------------------------------------------------------------------------*/

    /**
     * Assign a new local variable to a local variable slot.
     *
     * @param local the local
     * @param javacIndex the slot Javac assigned
     * @return the highest local index written into. This will be javacIndex for
     * for all actualType values but LONG and DOUBLE in which case it will be
     * javacIndex+1.
     */
    public int setLocal(Local local, int javacIndex) throws LinkageException {
        Assert.that(local.mergeParent() == null);
        Type localType = local.type();
        verifyLocalIndex(localType, javacIndex);
        Assert.that(javacIndex >= 0 && javacIndex < locals.length);
        if (isReusingLocalVariables()) {
            locals[javacIndex] = local;
            if (localType.isTwoWords()) {
                locals[javacIndex+1] = local.secondWordLocal();
                return javacIndex+1;
            }
        }
        else {
            verify(localType.vIsAssignableTo(localType(javacIndex)), VEConst.LOCALS_BAD_TYPE + ": " + localType + " is not assignable to " + localType(javacIndex));
            if (localType.isLong()) {
                verify(localType(javacIndex+1) == vm.LONG2, VEConst.LOCALS_BAD_TYPE);
                return javacIndex+1;
/*if[FLOATS]*/
            } else if (localType.isDouble()) {
                verify(localType(javacIndex+1) == vm.DOUBLE2, VEConst.LOCALS_BAD_TYPE);
                return javacIndex+1;
/*end[FLOATS]*/
            }
        }
        return javacIndex;
    }

    /**
     * Get local variable at a given slot.
     *
     * @param basicType the type to which the value of the given local variable
     * is expected to be assignable to.
     * @param javacIndex The javac index of local variable for which the actual
     * type is being requested.
     * @return the local variable
     */
    public Local getLocal(Type basicType, int javacIndex) throws LinkageException {
        verifyLocalIndex(basicType, javacIndex);
        basicType = basicType.localType();
        Type localType = localType(javacIndex);
        verify(localType.vIsAssignableTo(basicType), VEConst.LOCALS_BAD_TYPE + ": " + localType + " is not assignable to " + basicType);
        if (basicType.isLong()) {
            verify(localType(javacIndex+1) == vm.LONG2, VEConst.LOCALS_BAD_TYPE);
        }
/*if[FLOATS]*/
        else if (basicType.isDouble()) {
            verify(localType(javacIndex+1) == vm.DOUBLE2, VEConst.LOCALS_BAD_TYPE);
        }
/*end[FLOATS]*/

        Local local = locals[javacIndex];
        Assert.that(local != null && local.mergeParent() == null);
        return local;
    }

    /**
     * Get the local variable at a given index.
     * @param index the index.
     * @return the type of the local variable at index.
     */
    public Local local(int index) {
        Assert.that(index >= 0 && index < locals.length);
        return locals[index];
    }

    /**
     * Get the type of the local variable at a given index.
     * @param index the index.
     * @return the type of the local variable at index.
     */
    public Type localType(int index) {
        Assert.that(index >= 0 && index < locals.length);
        if (locals[index] == null) {
            return vm.BOGUS;
        }
        return locals[index].type();
    }

    /**
     * Get a copy of the types of the locals that were initialized by the parameters.
     * @return a copy of the types of the locals that were initialized by the parameters.
     */
    public Type[] getParmLocalTypes() {
        int nparms = parmLocals.length;
        if (nparms == 0) {
            return vm.ZEROTYPES;
        }
        else {
            Type[] parmTypes = new Type[nparms];
            for (int i = 0; i != nparms; i++) {
                parmTypes[i] = parmLocals[i].type();
                Assert.that(parmTypes[i] != vm.BOGUS);
            }
            return parmTypes;
        }
    }

    /**
     * Get the locals that correspond to and are initialized by the parameters
     * of this method.
     * @return
     */
    public Local[] getParmLocals() {
        return parmLocals;
    }

/*---------------------------------------------------------------------------*\
 *                     Verification methods                                  *
\*---------------------------------------------------------------------------*/

    /**
     * Validate a local variable index for a given type.
     */
    public void verifyLocalIndex(Type actualType, int javacIndex) throws LinkageException {
        int extra = (actualType.isTwoWords() ? 1 : 0);
        verify(javacIndex >= 0, VEConst.LOCALS_UNDERFLOW);
        verify((javacIndex + extra) < locals.length, VEConst.LOCALS_OVERFLOW);
    }

/*---------------------------------------------------------------------------*\
 *                   Stack map matching & merging methods                    *
\*---------------------------------------------------------------------------*/

    /**
     * Check to see if a recorded stack map matches the current derived stack map.
     * @param target The recorded stack map entry.
     * @param replaceWithTarget If true then derived types are replaced with recorded types.
     * @param verifyErrorCode The verify error code to use if the check fails.
     */
    public void matchStackMap(Target target, boolean replaceWithTarget, VEConst verifyErrorCode) throws LinkageException {
        StackMap.Entry mapEntry = target.getStackMapEntry();
        matchStackMapLocals(mapEntry.locals, replaceWithTarget, verifyErrorCode);
        matchStackMapStack(mapEntry.stack, replaceWithTarget, verifyErrorCode);
    }

    /**
     * Check to see if the locals of a recorded stack map entry matches the current derived locals.
     * @param recordedTypes The local types according to the stack map entry.
     * @param replaceWithTarget If true then derived types are replaced with recorded types.
     * @param verifyErrorCode The verify error code to use if the check fails.
     */
    public void matchStackMapLocals(Type[] recordedTypes, boolean replaceWithTarget, VEConst verifyErrorCode) throws LinkageException {

        int i;

        // Trace the locals
        if (locals.length > 0) {
            tracer.trace("     stackmap locals("+(recordedTypes.length)+") ");
            for(i = 0 ; i < recordedTypes.length ; i++) {
                tracer.trace(" "+recordedTypes[i]);
            }
            tracer.traceln("");
        }

        // Fail if the map has more
        verify(recordedTypes.length <= locals.length, verifyErrorCode);

        // Check the locals
        for (i = 0 ; i < recordedTypes.length ; i++) {
            Type recordedType = recordedTypes[i];
            Type derivedType = localType(i);

            // The code below is probably only required for some border case in the TCK
            // so leave it commented until TCK compatibility is required
//            if (recordedType.isInterface()) {
//                recordedType = vm.OBJECT; // Change interfaces to java.lang.Object
//            }

            verify(derivedType.vIsAssignableTo(recordedType), verifyErrorCode + ": " + derivedType + " not assignable to " + recordedType);
            if (replaceWithTarget && recordedType != vm.BOGUS) {
                locals[i].changeType(recordedType);
            }
        }
    }

    /**
     * Check to see if the operand stack of a recorded stack map entry matches the current derived operand stack.
     * @param recordedTypes The operand stack types according to the stack map entry.
     * @param replaceWithTarget If true then derived types are replaced with recorded types.
     * @param verifyErrorCode The verify error code to use if the check fails.
     */
    public void matchStackMapStack(Type[] recordedTypes, boolean replaceWithTarget, VEConst verifyErrorCode) throws LinkageException {

        int i;

        // Trace the stack items
        if (recordedTypes.length > 0) {
            tracer.trace("     stackmap stack("+recordedTypes.length+") ");
            for(i = 0 ; i < recordedTypes.length ; i++) {
                tracer.trace(" "+recordedTypes[i]);
            }
            tracer.traceln("");
        }

        // Fail if the map sp is different
        verify(recordedTypes.length == jsp, verifyErrorCode);

        // Check the stack items
        for (i = 0 ; i < recordedTypes.length ; i++) {
            Type recordedType = recordedTypes[i];
            Type derivedType = stack[i].type().localType();
            verify(derivedType.vIsAssignableTo(recordedType), verifyErrorCode + ": " + derivedType + " not assignable to " + recordedType);

            if (replaceWithTarget && stack[i].getTemporaryLocal().type() != recordedType) {
                stack[i].getTemporaryLocal().changeType(recordedType);
            }
        }
    }

    /**
     * Merge the local variables and stack types with a given branch/exception target.
     * @param target The branch/exception target.
     */
    public void mergeWithTarget(Target target) {
        if (isAllocatingStackVariables()) {
            target.mergeStackWithFrame(stack, jsp, this);
        } else {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(target.stackMatchesFrame(stack, jsp, this));
            }
        }

        if (isMergingLocalVariables()) {
            target.mergeLocalsWithFrame(locals);
        }
    }

    /**
     * Conceptually trim the stack to be empty. This does not actually
     * modify the value of <code>jsp</code> but marks all the instructions
     * on the stack as requiring spilling apart from the <code>size</code> top entries.
     */
    public void spillStack(boolean atBasicBlockBoundary, Instruction delim) {
        if (atBasicBlockBoundary) {
            if (!vm.constrainStack()) {
                return;
            }
        }
        else {
            if (!vm.constrainStackForGC()) {
                return;
            }
//            accountForGCInstructions(delim);
        }
        for (int i = jsp - 1; i >= 0; i--) {
            Instruction inst = stack[i];
            inst.spills();
        }
    }

/*
    private void accountForGCInstructions(Instruction delim) {
        int pos = 0;
        boolean hasOop = false;
        for (int i = jsp - 1; i >= 0; i--) {
            Instruction inst = stack[i];
            if (!inst.type().isPrimitive()) {
                oopmap[pos++] = '1';
                hasOop = true;
            }
            else {
                oopmap[pos++] = '0';
            }
        }
        if (hasOop) {
            java.util.TreeMap maps = (java.util.TreeMap) oopmaps.get(method);
            if (maps == null) {
                maps = new java.util.TreeMap();
                oopmaps.put(method, maps);
            }
            Assert.that(delim != null);
            maps.put(new Integer(delim.getOriginalIP()), new Object[] {
                delim, new String(oopmap, 0, pos)
            });
        }
        else {
            totalEmptyStackInvokes++;
        }

    }

    private static java.util.Hashtable oopmaps = new java.util.Hashtable();
    private static int totalEmptyStackInvokes;
    private static char[] oopmap = new char[1000];
    public static void dumpOopmaps(java.io.PrintStream out) {
        int total = 0;
        Enumeration methods = oopmaps.keys();
        Enumeration values = oopmaps.elements();
        Hashtable dist = new Hashtable();
        while (methods.hasMoreElements()) {
            Method m = (Method)methods.nextElement();
            java.util.TreeMap table = (java.util.TreeMap)values.nextElement();
            out.println(m+":");
            for (Iterator iter = table.entrySet().iterator(); iter.hasNext(); ) {
                total++;
                Map.Entry entry = (Map.Entry)iter.next();
                Object[] objs = (Object[])entry.getValue();
                String oopmap = (String)objs[1];
                out.println("  "+entry.getKey()+": "+objs[0]+" -> "+oopmap);

                Integer count = (Integer)dist.get(oopmap);
                if (count == null) {
                    count = new Integer(1);
                } else {
                    count = new Integer(count.intValue()+1);
                }
                dist.put(oopmap, count);
            }
        }
        out.println("Total GC constrained instructions with non-empty stack: "+total);
        out.println("Total GC constrained instructions: "+(total+totalEmptyStackInvokes));

        Map.Entry[] sortedDist = new Map.Entry[dist.size()];
        int pos = 0;
        for (Iterator iter = dist.entrySet().iterator(); iter.hasNext();) {
            sortedDist[pos++] = (Map.Entry)iter.next();
        }
        Arrays.sort(sortedDist, new com.sun.squawk.translator.util.Comparer() {
            public int compare(Object o1, Object o2) {
                return ((Integer)(((Map.Entry)o2).getValue())).intValue() -
                       ((Integer)(((Map.Entry)o1).getValue())).intValue();
            }
        });
        out.println();
        out.println("Distribution of oopmaps:");
        for (pos = 0; pos != sortedDist.length; pos++) {
            Map.Entry entry = sortedDist[pos];
            out.println("  "+entry.getKey()+" -> "+entry.getValue());
        }
    }
*/
}


