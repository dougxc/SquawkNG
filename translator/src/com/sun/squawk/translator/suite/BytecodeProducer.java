package com.sun.squawk.translator.suite;

import java.util.*;
import java.io.PrintStream;
import com.sun.squawk.translator.ir.*;
import java.io.IOException;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.*;
import com.sun.squawk.translator.util.JVMConst;
import com.sun.squawk.translator.util.VEConst;
import com.sun.squawk.vm.*;

/**
 * This class emits the bytecode for a method and does the necessary relocation.
 */
class BytecodeProducer implements InstructionVisitor, LinkageException.Context {

    /**
     * This class represents basic blocks for the purpose of relocation.
     */
    static class BB {
        /** The size of the fixed instructions in the block. */
        final int fixedSize;
        /** The fixed instructions of the basic block. */
        final Instruction[] instructions;
        /** The sizes of the fized instructions of the basic block. */
        final int[] instructionSizes;
        /** The instruction targeted by the branch instruction delimiting this basic block. */
        final Instruction target;
        /** The delimiting instruction. */
        final Instruction delimiter;
        /** Map of offset limits to instruction sizes (in bytes) for the delimiting branch instruction. */
        final SuiteEmitter.BranchEncoding[] branchEncTable;
        /** The branch encoding used for the delimiting branch instruction. */
        SuiteEmitter.BranchEncoding branchEnc;

        /**
         * Get the first instruction of this basic block.
         * @return the first instruction of this basic block.
         */
        Instruction first() {
            if (instructions.length > 0) {
                return instructions[0];
            }
            Assert.that(delimiter != null);
            return delimiter;
        }

        /**
         * Get the last instruction of this basic block.
         * @return the last instruction of this basic block.
         */
        Instruction last() {
            if (delimiter == null) {
                Assert.that(instructions.length > 0);
                return instructions[instructions.length - 1];
            }
            return delimiter;
        }

        /**
         * Get the number of instructions in this basic block.
         * @return the number of instructions in this basic block.
         */
        int getInstructionCount() {
            return instructions.length + (delimiter == null ? 0 : 1);
        }

        /**
         * Get the IP address immediately following the last instruction.
         * @return the IP address immediately following the last instruction.
         */
        private int nextIp() {
            if (delimiter == null) {
                return instructions[0].getRelocIP() + fixedSize;
            }
            else {
                Assert.that(branchEnc != null);
                return delimiter.getRelocIP() + branchEnc.encBytes;
            }
        }

        /**
         * Relocate the instruction at the head of this BB.
         * @param ip
         */
        private void relocateFixed(int startIp) {
            Assert.that(instructions.length > 0);
            int ip = startIp;
            for (int i = 0; i != instructions.length; i++) {
                instructions[i].setRelocIP(ip);
                ip += instructionSizes[i];
                Assert.that(fits(17, ip));
            }
        }

        /**
         * Create a new BB.
         * @param bbInstructions
         * @param bbInstructionSizes
         * @param bbSize
         * @param delimiter
         * @param target
         * @param branchEncTable
         * @param prev
         */
        BB(Vector bbInstructions,
           Vector bbInstructionSizes,
           Instruction delimiter,
           Instruction target,
           SuiteEmitter.BranchEncoding[] branchEncTable,
           SuiteEmitter.BranchEncoding   switchEnc,
           BB prev)
        {
            // Copy the fixed instructions and their sizes into the approriate arrays.
            Assert.that(bbInstructions.size() == bbInstructionSizes.size());
            instructions     = new Instruction[bbInstructions.size()];
            instructionSizes = new int[instructions.length];
            bbInstructions.copyInto(this.instructions);
            int i = 0;
            int total = 0;
            for (Enumeration e = bbInstructionSizes.elements(); e.hasMoreElements(); i++) {
                instructionSizes[i] = ((Integer)e.nextElement()).intValue();
                total += instructionSizes[i];
            }
            fixedSize = total;

            this.delimiter = delimiter;
            Assert.that(target == null || target instanceof Phi);
            this.target = target;
            this.branchEncTable = branchEncTable;

            // Set the initial relocation address for the fixed instructions
            if (instructions.length > 0) {
                Instruction first = instructions[0];
                if (prev != null) {
                    relocateFixed(prev.nextIp());
                }
                else {
                    relocateFixed(0);
                }
            }

            // Set the initial relocation address for the branch instruction
            if (delimiter != null) {
                if (delimiter instanceof SwitchInstruction) {
                    Assert.that(branchEncTable == null);
                    Assert.that(switchEnc != null);
                    this.branchEnc = switchEnc;
                }
                else {
                    Assert.that(branchEncTable != null);
                    Assert.that(switchEnc == null);
                    this.branchEnc = branchEncTable[0];
                }
                this.delimiter.setRelocIP((prev == null ? 0 : prev.nextIp()) + fixedSize);
            }
        }

        /**
         * Relocate the first and last instructions in this basic block.
         * @param growth The accumulative number of bytes by which preceeding basic blocks grew.
         * @return the accumulative number of bytes by which this basic block and all
         * its preceeding basic blocks grew.
         */
        int relocate(int growth) {
            if (instructions.length > 0) {
                relocateFixed(instructions[0].getRelocIP() + growth);
            }
            if (delimiter != null) {
                if (delimiter instanceof SwitchInstruction) {
                    SwitchInstruction inst = (SwitchInstruction)delimiter;
                    Assert.that(inst.getRelocIP() != -1);
                    int switchIp = inst.getRelocIP();

                    // Relocate IP of switch instruction
                    if (growth != 0) {
                        switchIp += growth;
                        inst.setRelocIP(switchIp);
                    }

                    // Adjust padding
                    int padding = inst.padding();
                    int ipAdjust = branchEnc.ipAdjust + padding;
                    if ((ipAdjust + switchIp) % 2 != 0) {
                        if (padding == 1) {
                            padding = 0;
                            growth -= 1;
                        }
                        else {
                            Assert.that(padding == 0);
                            padding = 1;
                            growth += 1;
                        }
                        inst.setPadding(padding);
                    }
                }
                else {
                    int targetIp = target.getRelocIP();
                    Assert.that(targetIp != -1);
                    int delimiterIp = delimiter.getRelocIP();

                    // Account for relocation of targetIp if this is a forward branch
                    if (targetIp > delimiterIp) {
                        targetIp += growth;
                    }

                    // Relocate IP of branch instruction
                    if (growth != 0) {
                        delimiterIp += growth;
                        delimiter.setRelocIP(delimiterIp);
                    }

                    int offset = targetIp - (delimiterIp + branchEnc.ipAdjust);
                    if (!(fits(branchEnc.offsetBits, offset))) {
                        for (int i = 0;; i++) {
                            Assert.that(i != branchEncTable.length, "offset="+offset);
                            SuiteEmitter.BranchEncoding enc = branchEncTable[i];
                            if (fits(enc.offsetBits, offset)) {
                                int delta = enc.encBytes - branchEnc.encBytes;
                                Assert.that(delta > 0);
                                growth += delta;
                                branchEnc = enc;
                                break;
                            }
                        }
                    }
                }
            }
            return growth;
        }
    }

    /** Bytecode emitting pass. */
    private boolean firstPass;
    /** The method whose bytecode is currently being emitted. */
    private final Method method;
    /** The debug attribute for the method being emitted. */
    private final MethodDebugAttribute debugAttribute;
    /** The Translator context. */
    private final Translator vm;
    /** Instruction factory. */
    private final InstructionFactory ifactory;
    /** The local variables. */
    private final Local[] locals;
    /** The SuiteEmitter. */
    private final SuiteEmitter emitter;
    /** The enclosing SuiteProducer. */
    private final SuiteProducer suite;
    /** The list of basic blocks. */
    private final Vector cfg;

    private final ExecutionFrame frame;
    private Type[] stackState;

    /** The instructions of the basic block currently under construction. */
    private final Vector bbInstructions;
    /** The total size of the fixed-size instructions of the basic block currently under construction. */
    private final Vector bbInstructionSizes;
    /** The IP address of the instruction being emitted (only used in second pass). */
    private int ip;

    /** Used during traversal of instructions. */
    private BB currentBB;
    /** Used for context messages. */
    private Instruction currentInst;

    /**
     * Constructor.
     *
     * @param method
     * @param locals
     * @param emitter
     * @param suite
     * @param debugAttribute
     */
    public BytecodeProducer(Method method,
                            Local[] locals,
                            SuiteEmitter emitter,
                            SuiteProducer suite,
                            MethodDebugAttribute debugAttribute) {
        this.method             = method;
        this.debugAttribute     = debugAttribute;
        this.vm                 = method.vm();
        this.locals             = locals;
        this.emitter            = emitter;
        this.suite              = suite;
        this.bbInstructions     = new Vector();
        this.bbInstructionSizes = new Vector();
        this.cfg                = new Vector();
        ifactory                = new InstructionFactory(vm, method);
        stackState              = new Type[10];
        try {
            this.frame = new ExecutionFrame(vm,
                                            method,
                                            false,
                                            this,
                                            null,
                                            -1,
                                            locals,
                                            0,
                                            null);
        } catch (LinkageException le) {
            le.printStackTrace();
            throw new RuntimeException(le.getMessage());
        }
        method.getSquawkMetrics().maxLocals = locals.length;
    }

   /* ------------------------------------------------------------------------ *\
    *                          Helper methods                                  *
   \* ------------------------------------------------------------------------ */


    /**
     * Append an instruction to the current basic block.
     * @param size The encoding size of the instruction.
     * @param inst The logical instruction to which the emitted instruction corresponds.
     * @param isFirst specifies whether or not this is the first instruction emitted for
     * the given logical instruction.
     */
    private void appendBB(int size, Instruction inst, boolean isFirst) {
        Assert.that(size >= 0);
        Assert.that(inst != null);
        if (firstPass) {
            if (isFirst) {
                bbInstructions.addElement(inst);
                bbInstructionSizes.addElement(new Integer(size));
            }
            else {
                Integer current = (Integer)bbInstructionSizes.lastElement();
                bbInstructionSizes.setElementAt(new Integer(size + current.intValue()), bbInstructionSizes.size() - 1);
            }
        }
        else {
            ip += size;
        }
    }

    /**
     *
     * @param delimiter
     * @param target
     */
    private void endBB(Instruction delimiter, Instruction target, SuiteEmitter.BranchEncoding[] branchEncTable, SuiteEmitter.BranchEncoding switchEnc) {
        if (firstPass) {
            BB prevBB;
            if (cfg.isEmpty()) {
                prevBB = null;
            }
            else {
                prevBB = (BB)cfg.lastElement();
            }
            cfg.addElement(new BB(bbInstructions, bbInstructionSizes, delimiter, target, branchEncTable, switchEnc, prevBB));
            bbInstructions.setSize(0);
            bbInstructionSizes.setSize(0);
        }
        else {
            if (delimiter != null) {
                Assert.that(currentBB.branchEnc != null);
                ip += currentBB.branchEnc.encBytes;
                if (delimiter instanceof SwitchInstruction) {
                    SwitchInstruction inst = (SwitchInstruction)delimiter;
                    ip += inst.padding();
                }
            }
        }
    }

    /**
     * Determine if a given number of bits is sufficent to encode a given offset.
     * @param bits
     * @param offset
     * @return
     */
    private static boolean fits(int bits, long offset) {
        long lo = -(1 << (bits-1));
        long hi = (1 << (bits-1)) - 1;
        return (offset >= lo && offset <= hi);
    }

    /**
     * Do the relocation.
     */
    private void relocate() {
        int growth;
        int instructionCount;
//System.err.println("\nBasic blocks for "+method);
//for (Enumeration e = cfg.elements(); e.hasMoreElements();) {
//    BB bb = (BB)e.nextElement();
//    System.err.println("  "+bb.first().getOriginalIP()+" - "+bb.last().getOriginalIP());
//}
        do {
            growth = 0;
            instructionCount = 0;
            for (Enumeration e = cfg.elements(); e.hasMoreElements();) {
                BB bb = (BB)e.nextElement();
                growth = bb.relocate(growth);
                instructionCount += bb.getInstructionCount();
            }
//if (growth != 0) {
//    System.err.println(method+": grew by "+growth+" bytes");
//}
        } while (growth != 0);
        Assert.that(cfg.size() > 0);
        method.getSquawkMetrics().length = ((BB)cfg.lastElement()).nextIp();
        method.getSquawkMetrics().instructionCount = instructionCount;
    }

    /**
     * Helper function to wrap an IR instruction and a relocation IP into a IContext object.
     * @param inst
     * @return
     */
    private SuiteEmitter.IContext context(Instruction inst) {
        if (!firstPass) {
            int stackSize = frame.stackSize(true);
            if (stackState.length < stackSize) {
                stackState = new Type[stackSize];
            }
            frame.copyStackState(stackState, true);
            if (debugAttribute != null) {
                debugAttribute.addLineNumberTableItem(ip, inst.getSourceLineNo());
            }
            return new SuiteEmitter.IContext(inst, ip, stackState, stackSize);
        }
        else {
            return new SuiteEmitter.IContext(inst, ip, null, -1);
        }
    }

    /**
     * Helper function to calculate the relative offset given a branch instruction
     * and the instruction it targets.
     * @param branch
     * @param target
     * @param ipAdjust
     * @return
     */
    private int offset(Instruction branch, Instruction target, int ipAdjust) {
        Assert.that(target != null);
        Assert.that(firstPass || branch.getRelocIP() != -1);
        Assert.that(firstPass || target.getRelocIP() != -1);
        return target.getRelocIP() - (branch.getRelocIP() + ipAdjust);
    }

    /**
     * Helper function to determine which immediate value opcode (if any) to use
     * based on an operand in the range 0-15.
     * @param base The optimal bytecode for a zero-valued operand.
     * @param minusOne The bytecode to use when operand is -1. This should be -1
     * if there is no "negative 1" immediate form opcode.
     * @param operand The operand.
     * @return the immediate operand opcode for operand or -1 if there operand is
     * out of range.
     */
    private int immOpcode(int base, int minusOne, long operand) {
        if (operand == -1) {
            return minusOne;
        }
        else if (operand >= 0 && operand <= 0xF) {
            return base + (int)operand;
        }
        else {
            return -1;
        }
    }

    /**
     * Helper method to emit a load class instruction just prior to an instruction
     * that requires a class reference to be the top thing on the stack.
     * @param type The class to load
     * @param inst The instruction for which the load class is being inserted.
     * @param isFirst specifies whether or not this is the first instruction emitted for
     * the current logical instruction.
     */
    private void emitLoadClass(Type type, Instruction inst, boolean isFirst) throws LinkageException {
        Instruction loadClass = ifactory.createLoadConstantType(type);
        int index = suite.classReferenceToIndex(method.parent(), type);
        int opcode = immOpcode(OPC.CLASS_0, -1, index);
        if (opcode == -1) {
            opcode = OPC.CLASS;
        }
        appendBB(emitter.emitLoadClass(context(inst), opcode, index), inst, isFirst);

        // Emulate effect on frame
        push(loadClass);
    }

    private int longFloatPrefix(Type type) {
        int opcode;
        Assert.that(type.isPrimitive());
        if (type == vm.LONG) {
            return OPC.LONGOP;
        }
/*if[FLOATS]*/
        else if (type == vm.FLOAT || type == vm.DOUBLE) {
            return OPC.FLOATOP;
        }
/*end[FLOATS]*/
        else {
            return -1;
        }
    }

    private int longFloatPrefix2Types(Type type1, Type type2) {
        int opcode = longFloatPrefix(type1);
/*if[FLOATS]*/
        if (opcode != OPC.FLOATOP) {
/*end[FLOATS]*/
            int opcode2 = longFloatPrefix(type2);
/*if[FLOATS]*/
            if (opcode2 == OPC.FLOATOP) {
                return OPC.FLOATOP;
            }
/*end[FLOATS]*/
            if (opcode == OPC.LONGOP || opcode2 == OPC.LONGOP) {
                return OPC.LONGOP;
            }
            return -1;
/*if[FLOATS]*/
        }
        else {
            return OPC.FLOATOP;
        }
/*end[FLOATS]*/
    }

    /**
     * Emit the bytecode.
     * @param firstPass
     * @return the length of the bytecode emitted
     */
    public void emit(boolean firstPass) {
        try {
            try {
                this.firstPass = firstPass;
                emitter.emitBytecodeStart(firstPass, method.getSquawkMetrics().length);
                if (firstPass) {
                    InstructionList ir = method.getIR();
                    ir.logicallyRelocate();
                    for (Instruction inst = ir.head(); inst != null; inst = inst.getNext()) {
                        currentInst = inst;
                        inst.visit(this);
                    }
                    Assert.that(bbInstructionSizes.size() == 0 && bbInstructions.size() == 0);

                    // Update the max stack of the frame so that it will be
                    // verified on the second pass. Currently it should be -1.
                    int maxStack = (DEBUG_EMITTED_BYTECODE_VERIFICATION ? 100 : frame.computedMaxStack(true));
                    method.getSquawkMetrics().maxStack = maxStack;
                    frame.adjustMaxStack(maxStack + 1);
                }
                else {
                    ip = 0;
                    for (Enumeration e = cfg.elements(); e.hasMoreElements();) {
                        currentBB = null;
                        BB bb = (BB)e.nextElement();
                        Instruction[] ilist = bb.instructions;
                        int[] ilistSizes    = bb.instructionSizes;
                        Assert.that(ilist.length == 0 || ilist[0].getRelocIP() == ip);
                        // Emit all the instructions of the current BB apart from the delimiter
                        for (int i = 0; i != ilist.length; i++) {
                            Instruction inst = currentInst = ilist[i];
                            Assert.that(inst.getRelocIP() == ip);
                            inst.visit(this);
                        }
                        // Emit delimiter of the current BB (if it has one)
                        Instruction delimiter = currentInst = bb.delimiter;
                        if (delimiter != null) {
                            Assert.that(ip == delimiter.getRelocIP());
                            currentBB = bb;
                            delimiter.visit(this);
                            currentBB = null;
                        }
                    }
                }
                emitter.emitBytecodeEnd();
                if (firstPass) {
                    relocate();
                }
            } catch (LinkageException le) {
                le.printStackTrace();
                Assert.shouldNotReachHere();
            }
        } catch (AssertionFailed ae) {
            ae.addContext(getContext());
            throw ae;
        }
    }

   /* ------------------------------------------------------------------------ *\
    *                   Frame emulation methods                                *
   \* ------------------------------------------------------------------------ */

    /**
     * Set to true to get a partial trace of emitted bytecode when there
     * is a verification problem with the bytecode being emitted (which
     * should never occur).
     */
    private static final boolean DEBUG_EMITTED_BYTECODE_VERIFICATION = false;

    /**
     * Helper method to ensure that a local variable loaded or stored is
     * indeed in the local variable table for the current method.
     * @param local
     */
    private void assumeLocalAllocated(Local local) {
        if (!firstPass) {
            return;
        }
        for (int i = 0; i != locals.length; i++) {
            if (local == locals[i]) {
                return;
            }
        }
        Assert.shouldNotReachHere();
    }

    /**
     * Verify that the stack is currently empty.
     *
     * @param atBasicBlockBoundary
     * @throws LinkageException
     */
    private void verifyEmptyStack(boolean atBasicBlockBoundary) throws LinkageException {
        if (atBasicBlockBoundary) {
            if (!vm.constrainStack()) {
                return;
            }
        }
        else {
            if (!vm.constrainStackForGC()) {
                return;
            }
        }
        if (DEBUG_EMITTED_BYTECODE_VERIFICATION && firstPass) {
            return;
        }
        if (!vm.constrainStack()) {
            return;
        }
        int stackSize = frame.stackSize(true);
        frame.verify(stackSize == 0, "Stack should be empty but has "+stackSize+" elements");
    }

    /**
     * Pop and return the top of stack value.
     *
     * @param expectedType
     * @throws LinkageException
     */
    private void pop(Type expectedType) throws LinkageException {
        if (DEBUG_EMITTED_BYTECODE_VERIFICATION && firstPass) {
            return;
        }
        if (frame.peek().type().getLinkageError() != null) {
            frame.popLinkageErrorType();
        } else {
            frame.pop(expectedType);
        }
    }

    /**
     * Push a value to the stack
     *
     * @param inst
     * @throws LinkageException
     */
    private void push(Instruction inst) throws LinkageException {
        if (DEBUG_EMITTED_BYTECODE_VERIFICATION && firstPass) {
            return;
        }
        frame.push(inst, false);
    }

    /**
     * Store a value of a given type to a local variable.
     *
     * @param type
     * @param index
     * @throws LinkageException
     */
    private void store(Type type, int index) throws LinkageException {
        if (DEBUG_EMITTED_BYTECODE_VERIFICATION && firstPass) {
            return;
        }
        Type localType = type.localType();
        Type frameType = frame.local(index).type();
        if (localType.getLinkageError() != null || frameType.getLinkageError() != null) {
            frame.verify(localType == frameType, VEConst.LOCALS_BAD_TYPE);
        } else {
            frame.verify(localType.vIsAssignableTo(frameType), VEConst.LOCALS_BAD_TYPE+": "+localType+" is not assignable to "+frameType);
        }
    }

    /**
     * Load a value of a given type from a local variable.
     *
     * @param type
     * @param index
     * @throws LinkageException
     */
    private void load(Type type, int index) throws LinkageException {
        if (DEBUG_EMITTED_BYTECODE_VERIFICATION && firstPass) {
            return;
        }
        if (frame.localType(index).getLinkageError() == null && type.getLinkageError() == null) {
            frame.getLocal(type.localType(), index);
        }
    }

   /* ------------------------------------------------------------------------ *\
    *                   Implementation of InstructionVistor                    *
   \* ------------------------------------------------------------------------ */

    /**
     *
     * @param inst
     * @throws LinkageException
     */
    public void doArithmeticOp(ArithmeticOp inst) throws LinkageException {
        Type leftType  = inst.left().type();
        Type rightType = inst.right().type();
        int opcode = longFloatPrefix2Types(leftType, rightType);
        int longFloatOpcode;
        boolean isFloat = false;
        if (opcode == -1) {
            opcode = inst.opcode();
            longFloatOpcode = -1;
        }
        else {
            longFloatOpcode = inst.opcode();
/*if[FLOATS]*/
            if (opcode == OPC.FLOATOP) {
                isFloat = true;
                method.getSquawkMetrics().usesFloatOrDouble = true;
            } else
/*end[FLOATS]*/
            {
                method.getSquawkMetrics().usesLong = true;
            }
        }
        appendBB(emitter.emitArithmeticOp(context(inst), opcode, longFloatOpcode, isFloat), inst, true);

        // Emulate effect on frame
        pop(rightType);
        pop(leftType);
        push(inst);
    }

    public void doArrayLength(ArrayLength inst) throws LinkageException {
        appendBB(emitter.emitArrayLength(context(inst), OPC.ARRAYLENGTH), inst, true);

        // Emulate effect on frame
        pop(inst.array().type());
        push(inst);
    }

    public void doCheckCast(CheckCast inst) throws LinkageException {
        // Emit an instruction to load the class being cast to
        emitLoadClass(inst.checkType(), inst, true);
        appendBB(emitter.emitCheckcast(context(inst), OPC.CHECKCAST), inst, false);

        // Emulate effect on frame
        pop(vm.CLASS);
        pop(inst.value().type());
        verifyEmptyStack(false);
        push(inst);
    }

    public void doConvertOp(ConvertOp inst) throws LinkageException {
        Type fromType = inst.fromType();
        Type toType   = inst.type();
        int opcode = longFloatPrefix2Types(fromType, toType);
        int longFloatOpcode;
        boolean isFloat = false;
        if (opcode == -1) {
            opcode = inst.op().convBytecode();
            longFloatOpcode = -1;
        }
        else {
            longFloatOpcode = inst.op().convBytecode();
/*if[FLOATS]*/
            if (opcode == OPC.FLOATOP) {
                isFloat = true;
                method.getSquawkMetrics().usesFloatOrDouble = true;
            }
            else
/*end[FLOATS]*/
            {
                method.getSquawkMetrics().usesLong = true;
            }
        }
        appendBB(emitter.emitConversion(context(inst), opcode, longFloatOpcode, isFloat), inst, true);

        // Emulate effect on frame
        pop(fromType);
        push(inst);
    }

    private void emitBranch(int opcode, Instruction branch, Instruction target) {
        Assert.that(target != null);
        Assert.that(firstPass || branch.getRelocIP() != -1);
        Assert.that(firstPass || target.getRelocIP() != -1);
        int offset = offset(branch, target, (firstPass ? 0 : currentBB.branchEnc.ipAdjust));
        SuiteEmitter.BranchEncoding[] branchEncTable = emitter.emitBranch(context(branch), opcode, offset, (firstPass ? null : currentBB.branchEnc));
        endBB(branch, target, branchEncTable, null);
    }

    public void doGoto(Goto inst) throws LinkageException {
        if (inst.target().getInstruction() != inst.getNext()) {
            emitBranch(OPC.GOTO, inst, inst.target().getInstruction());

            // Emulate effect on frame
            verifyEmptyStack(true);
        }
    }

    public void doHandlerEnter(HandlerEnter inst) throws LinkageException {
        appendBB(0, inst, true);
    }

    public void doHandlerExit(HandlerExit inst) throws LinkageException {
        appendBB(0, inst, true);
        // The handler exit may be the last instruction in which case
        // it must be put into its own basic block. Otherwise, the
        // relocIP will not be updated correctly.
        if (inst.getNext() == null) {
            endBB(null, null, null, null);
        }
    }

    public void doIfOp(IfOp inst) throws LinkageException {
        emitBranch(inst.op().ifBytecode(inst.left() == null), inst, inst.target().getInstruction());

        // Emulate effect on frame
        Type cmpType = (inst.right().type().isPrimitive() ? vm.INT : vm.OBJECT);
        pop(cmpType);
        if (inst.left() != null) {
            pop(cmpType);
        }
        verifyEmptyStack(true);
    }

    public void doInstanceOf(InstanceOf inst) throws LinkageException {
        emitLoadClass(inst.checkType(), inst, true);
        appendBB(emitter.emitInstanceOf(context(inst), OPC.INSTANCEOF), inst, false);

        // Emulate effect on frame
        pop(vm.CLASS);
        pop(inst.value().type());
        push(inst);
    }

    public void doIncDecLocal(IncDecLocal inst) throws LinkageException {
        assumeLocalAllocated(inst.local());
        int offset = inst.local().squawkIndex();
        int opcode = inst.isIncrement() ? OPC.INC : OPC.DEC;
        appendBB(emitter.emitIncDecLocal(context(inst), opcode, offset), inst, true);

        // Emulate effect on frame
        load(vm.INT, inst.local().squawkIndex());
        store(vm.INT, inst.local().squawkIndex());
    }

    public void doInvoke(Invoke inst) throws LinkageException {
        // Emit an instruction to load the class of the method being called, This
        // is required for all invocations except to virtual methods.
        Method callee = inst.method();
        Invoke.Form form = inst.form();
        int slot = callee.slot();
        boolean isFirst = true;
        if (inst.form() != Invoke.Form.VIRTUAL) {
            emitLoadClass(callee.parent(), inst, true);
            isFirst = false;
        }
        if (form == Invoke.Form.INIT) {
            appendBB(emitter.emitInvokeInit(context(inst), OPC.INVOKEINIT, slot), inst, isFirst);
        }
        else if (form == Invoke.Form.VIRTUAL) {
            appendBB(emitter.emitInvokeVirtual(context(inst), OPC.INVOKEVIRTUAL, slot), inst, isFirst);
        }
        else if (form == Invoke.Form.SUPER) {
            appendBB(emitter.emitInvokeSuper(context(inst), OPC.INVOKESUPER, slot), inst, isFirst);
        }
        else if (form == Invoke.Form.STATIC) {
            appendBB(emitter.emitInvokeStatic(context(inst), OPC.INVOKESTATIC, slot), inst, isFirst);
        }
        else if (form == Invoke.Form.INTERFACE) {
            appendBB(emitter.emitInvokeInterface(context(inst), OPC.INVOKEINTERFACE, slot), inst, isFirst);
        }
        else {
            Assert.shouldNotReachHere();
        }

        // Emulate effect on frame
        // Pop the class for non-virtual methods
        if (inst.form() != Invoke.Form.VIRTUAL) {
            pop(vm.CLASS);
        }

        // Pop the parameters
        Type[] parmTypes = callee.getParms();
        int j = parmTypes.length;
        for (int i = 0; i != parmTypes.length; i++) {
            pop(parmTypes[--j]);
        }

        // Pop the receiver for non-static methods
        if (form != Invoke.Form.STATIC) {
            pop(callee.parent());
        }
        verifyEmptyStack(false);

        // Push the result (if any)
        if (callee.type() != vm.VOID) {
            push(inst);
        }
    }

    public void doLoadConstant(LoadConstant inst) throws LinkageException {
        long value = 0;
        int opcode = -1;
        int convOpcode = -1;
        int longFloatOpcode = -1;
        if (inst.isInt() || inst.isLong()) {
            value = inst.isInt() ? inst.getInt() : inst.getLong();
            opcode = immOpcode(OPC.CONST_0, OPC.CONST_M1, value);
            if (opcode == -1) {
                if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                    opcode = OPC.CONST_BYTE;
                }
                else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                    opcode = OPC.CONST_SHORT;
                }
                else if (value >= Character.MIN_VALUE && value <= Character.MAX_VALUE) {
                    opcode = OPC.CONST_CHAR;
                }
                else if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                    opcode = OPC.CONST_INT;
                }
                else {
                    opcode = OPC.CONST_LONG;
                }
                method.getSquawkMetrics().usesLong = inst.isLong();
            }
            // A long constant must be converted unless the opcode is OPC.CONST_LONG
            if (inst.isLong()) {
                if (opcode != OPC.CONST_LONG) {
                    convOpcode = OPC.I2L;
                    longFloatOpcode = OPC.LONGOP;
                }
            }
        }
/*if[FLOATS]*/
        else if (inst.isFloat()) {
            method.getSquawkMetrics().usesFloatOrDouble = true;
            float fvalue = inst.getFloat();
            // Use a OPC.CONST_[0,1,2] followed by OPC.I2F for values 0.0, 1.0 and 2.0 respectively
            if (fvalue == 0.0F) {
                opcode = OPC.CONST_0;
                value  = 0;
            }
            else if (fvalue == 1.0F) {
                opcode = OPC.CONST_1;
                value  = 1;
            }
            else if (fvalue == 2.0F) {
                opcode = OPC.CONST_2;
                value  = 2;
            }
            else {
                opcode = OPC.CONST_FLOAT;
                value = Float.floatToIntBits(fvalue);
            }
            // Append the OPC.I2F if necessary
            if (opcode != OPC.CONST_FLOAT) {
                convOpcode = OPC.I2F;
                longFloatOpcode = OPC.FLOATOP;
            }
        } else if (inst.isDouble()) {
            method.getSquawkMetrics().usesFloatOrDouble = true;
            double dvalue = inst.getDouble();
            // Use a OPC.CONST_[0,1,2] followed by OPC.I2D for values 0.0, 1.0 and 2.0 respectively
            if (dvalue == 0.0D) {
                opcode = OPC.CONST_0;
                value  = 0;
            }
            else if (dvalue == 1.0D) {
                opcode = OPC.CONST_1;
                value  = 1;
            }
            else {
                opcode = OPC.CONST_DOUBLE;
                value = Double.doubleToLongBits(dvalue);
            }
            // Append the OPC.I2D if necessary
            if (opcode != OPC.CONST_DOUBLE) {
                convOpcode = OPC.I2D;
                longFloatOpcode = OPC.FLOATOP;
            }
        }
/*end[FLOATS]*/
        else if (!inst.isConstNull()) {
            opcode = -1;
            Assert.shouldNotReachHere();
        }

        // Emit the opcode
        if (inst.isConstNull()) {
            appendBB(emitter.emitLoadConstantNull(context(inst), OPC.CONST_NULL), inst, true);

            // Emit the 'checkcast' required to ensure that a 'null' receiver is not sent
            // the instruction for which this is the receiver
            Type castType = ((LoadConstantNull)inst).castType();
            if (castType != null) {
                emitLoadClass(castType, inst, false);
                pop(vm.CLASS);
                appendBB(emitter.emitCheckcast(context(inst), OPC.CHECKCAST), inst, false);
            }

            // Emulate effect on frame
            push(inst);
        }
        else {
            appendBB(emitter.emitLoadConstant(context(inst), opcode, value), inst, true);
            // Emulate effect on frame
            push(inst);
            if (convOpcode != -1) {
                Assert.that(longFloatOpcode != -1);
                boolean isFloat = false;
/*if[FLOATS]*/
                isFloat = longFloatOpcode == OPC.FLOATOP;
/*end[FLOATS]*/
                appendBB(emitter.emitConversion(context(inst), longFloatOpcode, convOpcode, isFloat), inst, false);
                // Emulate effect on frame
                pop(inst.type());
                push(inst);
            }
        }
    }

    public void doLoadConstantObject(LoadConstantObject inst) throws LinkageException {
        int index = suite.constantToIndex(inst.value(), method.parent());
        int opcode = immOpcode(OPC.OBJECT_0, -1, index);
        if (opcode == -1) {
            opcode = OPC.OBJECT;
        }
        appendBB(emitter.emitLoadObject(context(inst), opcode ,index), inst, true);

        // Emulate effect on frame
        push(inst);
    }

    public void doLoadException(LoadException inst) throws LinkageException {
        appendBB(0, inst, true);

        // Emulate effect on frame
        verifyEmptyStack(true);
        push(inst);
    }

    public void doLoadField (LoadField inst) throws LinkageException {
        Field field = inst.field();
        int opcode;
        if (field.isStatic()) {
            if (!inst.isFieldOfReceiver()) {
                // Load the class of the static method onto the stack
                emitLoadClass(field.parent(), inst, true);
                opcode = OPC.GETSTATIC;
            }
            else {
                // The class containing the static field is the class of the current method
                opcode = OPC.CLASS_GETSTATIC;
            }
            appendBB(emitter.emitGetStatic(context(inst), opcode, inst.field().slot()), inst, inst.isFieldOfReceiver());
        }
        else {
            if (inst.ref() == null) {
                Assert.that(inst.isFieldOfReceiver());
                opcode = OPC.THIS_GETFIELD;
            }
            else {
                opcode = OPC.GETFIELD;
            }
            appendBB(emitter.emitGetField(context(inst), opcode, inst.field().slot()), inst, true);
        }

        // Emulate effect on frame
        if (!field.isStatic()) {
            if (opcode == OPC.GETFIELD) {
                pop(field.parent());
            }
        }
        else if (!inst.isFieldOfReceiver()) {
            pop(vm.CLASS); // from <class>
            verifyEmptyStack(false);
        }
        push(inst);
    }

    public void doLoadIndexed(LoadIndexed inst) throws LinkageException {
        appendBB(emitter.emitArrayLoad(context(inst), OPC.ALOAD), inst, true);

        // Emulate effect on frame
        pop(inst.index().type());
        pop(inst.array().type());
        push(inst);
    }

    public void doLoadLocal(LoadLocal inst) throws LinkageException {
        assumeLocalAllocated(inst.local());
        int offset = inst.local().squawkIndex();
        int opcode = immOpcode(OPC.LOAD_0, -1, offset);
        if (opcode == -1 || inst.local().type().isTwoWords()) {
            opcode = OPC.LOAD;
        }
        appendBB(emitter.emitLoadLocal(context(inst), opcode, offset), inst, true);

        // Emulate effect on frame
        push(inst);
        load(inst.local().type(), inst.local().squawkIndex());
    }

    private void emitSwitch(SwitchInstruction inst, int low, int high) throws LinkageException {
        // Emit the 'stableswitch' instruction
        Target[] targets = inst.targets();
        int[] offsets = new int[targets.length];
        int defaultOffset = 0;
        int padding = inst.padding();

        if (!firstPass) {
            Assert.that(currentBB.branchEnc != null);
            Assert.that(currentBB.delimiter == inst);
            int ipAdjust = currentBB.branchEnc.ipAdjust + padding;
            Assert.that((inst.getRelocIP() + ipAdjust) % 2 == 0);
            for (int i = 0; i != targets.length; i++) {
                Instruction t = targets[i].getInstruction();
                offsets[i] = offset(inst, t, ipAdjust);
                suite.limits.checkTableSwitchOffset(true, offsets[i]);
            }
            defaultOffset = offset(inst, inst.defaultTarget().getInstruction(), ipAdjust);
            // Ensure suite format limits aren't exceeded
            suite.limits.checkTableSwitchOffset(true, defaultOffset);
        }

        // This instruction delimits a basic block
        SuiteEmitter.BranchEncoding switchEnc = emitter.emitTableSwitch(context(inst), OPC.STABLESWITCH, true, defaultOffset, low, high, offsets, padding);
        endBB(inst, null, null, switchEnc);
    }

    public void doLookupSwitch (LookupSwitch inst) throws LinkageException {
        Assert.that(firstPass);

        int[] matches = inst.matches();
        // Emit instruction to load constant array
        Type thisClass = method.parent();
        int index = suite.constantToIndex(matches, method.parent());
        Instruction ldc = ifactory.createLoadConstantObject(vm.INT_ARRAY, matches);
        ldc.setOriginal(inst);
        doLoadConstantObject((LoadConstantObject)ldc);

        // Emit 'lookup' instruction
        Instruction lookup = ifactory.createLookup(ldc, inst.key());
        lookup.setOriginal(inst);
        doLookup((Lookup)lookup);

        // Emit the 'stableswitch' instruction
        TableSwitch tswitch = (TableSwitch)ifactory.createTableSwitch(lookup, 0, matches.length - 1, inst.defaultTarget());
        Target[] targets = inst.targets();
        for (int i = 0; i != targets.length; i++) {
            tswitch.addTarget(i, targets[i]);
        }
        doTableSwitch(tswitch);
    }

    public void doLookup(Lookup inst) throws LinkageException {
        appendBB(emitter.emitLookup(context(inst), OPC.LOOKUP), inst, true);
        pop(vm.INT_ARRAY);
        pop(vm.INT);
        push(inst);
    }

    public void doMonitorEnter(MonitorEnter inst) throws LinkageException {
        boolean isForClass = (inst.value() == null);
        appendBB(emitter.emitMonitorEnter(context(inst), isForClass ? OPC.CLASS_MONITORENTER : OPC.MONITORENTER), inst, true);

        // Emulate effect on frame
        if (!isForClass) {
            pop(inst.value().type());
        }
        verifyEmptyStack(false);
    }

    public void doMonitorExit(MonitorExit inst) throws LinkageException {
        boolean isForClass = (inst.value() == null);
        appendBB(emitter.emitMonitorExit(context(inst), isForClass ? OPC.CLASS_MONITOREXIT : OPC.MONITOREXIT), inst, true);

        // Emulate effect on frame
        if (!isForClass) {
            pop(inst.value().type());
        }
        verifyEmptyStack(false);
    }

    public void doNegateOp (NegateOp inst) throws LinkageException {
        Type type = inst.type();
        int opcode = longFloatPrefix(type);
        int longFloatOpcode = -1;
        boolean isFloat = false;
        if (opcode == -1) {
            opcode = OPC.NEG;
        }
        else {
            if (type == vm.LONG) {
                longFloatOpcode = OPC.LNEG;
            }
/*if[FLOATS]*/
            else if (type == vm.FLOAT) {
                longFloatOpcode = OPC.FNEG;
                isFloat = true;
            }
            else {
                Assert.that(type == vm.DOUBLE);
                longFloatOpcode = OPC.DNEG;
                isFloat = true;
            }
/*end[FLOATS]*/
        }
        appendBB(emitter.emitArithmeticOp(context(inst), opcode, longFloatOpcode, isFloat), inst, true);

        // Emulate effect on frame
        pop(inst.value().type());
        push(inst);
    }

    public void doNewArray(NewArray inst) throws LinkageException {
        // Emit instruction to load array class
        Assert.that(inst.type().isArray());
        emitLoadClass(inst.type(), inst, true);
        appendBB(emitter.emitNewArray(context(inst), OPC.NEWARRAY), inst, false);

        // Emulate effect on frame
        pop(vm.CLASS);
        pop(inst.size().type());
        verifyEmptyStack(false);
        push(inst);
    }

    public void doNewDimension(NewDimension inst) throws LinkageException {
        appendBB(emitter.emitNewArray(context(inst), OPC.NEWDIMENSION), inst, true);

        // Emulate effect on frame
        pop(inst.dimension().type());
        pop(inst.array().type());
        verifyEmptyStack(false);
        push(inst);
    }

    public void doNewMultiArray(NewMultiArray inst) throws LinkageException {
        Assert.shouldNotReachHere();
    }

    public void doNewObject(NewObject inst) throws LinkageException {
        // Load the class being instantiated
        emitLoadClass(inst.type(), inst, true);
        // Emit clinit instruction - allocation is done in <init>
        appendBB(emitter.emitNew(context(inst), OPC.CLINIT), inst, false);
        pop(vm.CLASS);

        // Emit the load 'null' instruction
        appendBB(emitter.emitLoadConstantNull(context(inst), OPC.CONST_NULL), inst, false);

        // Emulate effect on frame
        verifyEmptyStack(false);
        push(inst);
    }

    public void doPop(Pop inst) throws LinkageException {
        appendBB(emitter.emitPop(context(inst), OPC.POP), inst, true);
        if (inst.value().type().isTwoWords()) {
            appendBB(emitter.emitPop(context(inst), OPC.POP), inst, false);
        }

        // Emulate effect on frame
        pop(vm.UNIVERSE);
    }

    public void doPhi(Phi inst) throws LinkageException {
        if (bbInstructions.size() != 0) {
            endBB(null, null, null, null);
        }
        appendBB(0, inst, true);

        // Emulate effect on frame
        verifyEmptyStack(true);
    }

    public void doReturn(Return inst) throws LinkageException {
        appendBB(emitter.emitReturn(context(inst), OPC.RETURN), inst, true);
        // This instruction delimits a basic block where the delimiting instruction is not resizable
        endBB(null, null, null, null);

        // Emulate effect on frame
        if (inst.value() != null) {
            pop(inst.value().type());
        }
        verifyEmptyStack(true);
    }

    public void doStoreField(StoreField inst) throws LinkageException {
        Field field = inst.field();
        int opcode;
        if (field.isStatic()) {
            if (!inst.isFieldOfReceiver()) {
            // Load the class of the static method onto the stack
                emitLoadClass(field.parent(), inst, true);
                opcode = OPC.PUTSTATIC;
            }
            else {
                // The class containing the static field is the class of the current method
                opcode = OPC.CLASS_PUTSTATIC;
            }

            appendBB(emitter.emitPutStatic(context(inst), opcode, inst.field().slot()), inst, inst.isFieldOfReceiver());
        }
        else {
            if (inst.ref() == null) {
                Assert.that(inst.isFieldOfReceiver());
                opcode = OPC.THIS_PUTFIELD;
            }
            else {
                opcode = OPC.PUTFIELD;
            }
            appendBB(emitter.emitPutField(context(inst), opcode, inst.field().slot()), inst, true);
        }

        // Emulate effect on frame
        if (field.isStatic()) {
            if (!inst.isFieldOfReceiver()) {
                pop(vm.CLASS);
            }
            pop(inst.value().type());
            if (!inst.isFieldOfReceiver()) {
                verifyEmptyStack(false);
            }
        }
        else {
            pop(inst.value().type());
            if (opcode == OPC.PUTFIELD) {
                pop(field.parent());
            }
        }
    }

    public void doStoreIndexed(StoreIndexed inst) throws LinkageException {
        appendBB(emitter.emitArrayStore(context(inst), OPC.ASTORE), inst, true);

        // Emulate effect on frame
        pop(inst.value().type());
        pop(inst.index().type());
        pop(inst.array().type());
    }

    public void doStoreLocal(StoreLocal inst) throws LinkageException {
        assumeLocalAllocated(inst.local());
        int offset = inst.local().squawkIndex();
        int opcode = immOpcode(OPC.STORE_0, -1, offset);
        if (opcode == -1 || inst.local().type().isTwoWords()) {
            opcode = OPC.STORE;
        }
        appendBB(emitter.emitStoreLocal(context(inst), opcode, offset), inst, true);

        // Emulate effect on frame
        pop(inst.value().type());
        store(inst.value().type(), inst.local().squawkIndex());
    }

    public void doTableSwitch(TableSwitch inst) throws LinkageException {
        int high = inst.high();
        int low  = inst.low();
        // Emit the 'stableswitch' instruction
        emitSwitch(inst, low, high);

        // Emulate effect on frame
        pop(inst.key().type());
        verifyEmptyStack(true);
    }

    public void doThrow(Throw inst) throws LinkageException {
        appendBB(emitter.emitThrow(context(inst), OPC.THROW), inst, true);
        // This instruction delimits a basic block where the delimiting instruction is not resizable
        endBB(null, null, null, null);

        // Emulate effect on frame
        pop(inst.value().type());
        verifyEmptyStack(true);
    }

/*---------------------------------------------------------------------------*\
 *                   Exception and error methods                             *
\*---------------------------------------------------------------------------*/

    /**
     * Provide a context string for an error.
     */
    public String getContext() {
        String result = method.toString();
        if (currentInst != null) {
            result += " @ " + currentInst.toString(true, true, true);
        }
        return result;
    }
}
