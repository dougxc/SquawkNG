package com.sun.squawk.translator.loader;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.Stack;
import java.util.Enumeration;

import com.sun.squawk.util.*;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.ir.*;
import com.sun.squawk.translator.util.JVMConst;
import com.sun.squawk.translator.util.VEConst;
import com.sun.squawk.translator.util.OpConst;
import com.sun.squawk.vm.*;

/**
 * This class implements are parser for the bytecode of a Java bytecode
 * method. The output of the parser is an intermediate representation
 * of the semantics of the parsed bytecode expressed as a linked list of
 * Instruction objects. The parsing also performs a number of transformations
 * for the cases where Squawk semantics differ from standard Java semantics.
 * These differences are listed below:
 *
 *   1, Constructors (i.e. <init> methods) return an object of the constructed
 *      type in Squawk where as they return void in Java. This has a number of
 *      implications:
 *
 *        i)   The 'new' followed by 'dup' idiom must be recognised as the duped
 *             value is now  provided by the value returned from a constructor.
 *        ii)  A 'pop' must be inserted after a call to a constructor where the
 *             returned value is not used (i.e. there was no 'dup' in the original
 *             bytecode.
 */
public final class GraphBuilder implements LinkageException.Context {


    /** Dup optimizeations */
    private final boolean OPTIMIZEDUPS = false;

    /** The VM for this IR.*/
    private final Translator vm;
    /** Stream from where the bytecodes originate. */
    private final BytecodeInputStream in;
    /** The method being processed. */
    private final Method method;
    /** Stackmap for the method. */
    private final StackMap map;
    /** Line number table for method. */
    private final int[] lineNumberTable;
    /** The current line number table index. */
    private int lineNumberTableIndex;
    /** The exception handler table being built while parsing the method. */
    private final Vector irHandlers;
    /** The execution frame. */
    private final ExecutionFrame frame;
    /** The instruction list being built. */
    private final InstructionList ilist;
    /** The instruction factory. */
    private final InstructionFactory ifactory;
    /** Stack of currently active exception handlers. */
    private Stack activeHandlers = new Stack();
    /** Tracks whether or not the local variable holding the receiver of virtual method was overwritten. */
    private boolean receiverWasOverwritten;


    /**
     * Constructor.
     * @param vm The context of the translation.
     * @param in The input stream for the bytecodes.
     * @param holder The staging container for the method's bytecode and its related data.
     */
    static InstructionList read(Translator vm, BytecodeInputStream in, BytecodeHolder holder) throws LinkageException {
        return new GraphBuilder(vm, in, holder).build();
    }

    /**
     * Constructor
     * @param vm The context of the translation.
     * @param in The input stream for the bytecodes.
     * @param holder The staging container for the method's bytecode and its related data.
     */
    private GraphBuilder(Translator vm, BytecodeInputStream in, BytecodeHolder holder) throws LinkageException {
        this.vm              = vm;
        this.in              = in;
        this.method          = holder.method;
        this.map             = holder.stackMap;
        this.lineNumberTable = holder.lineNumberTable;
        this.irHandlers      = new Vector();
        this.ifactory        = new InstructionFactory(vm, method);
        this.frame = new ExecutionFrame(vm,
                method,
                in.trace(),
                this,
                ifactory,
                holder.maxStack,
                new Local[holder.maxLocals],
                ExecutionFrame.FLAG_ALLOCATE_STACK_VARIABLES |
                ExecutionFrame.FLAG_REUSABLE_LOCAL_VARIABLES,
                holder.localVariableTable);
        this.ilist = new InstructionList(frame, false);
        in.setFrame(frame);
        in.setInstructionFactory(ifactory);
    }

    /**
     * Build the graph.
     */
    private InstructionList build() throws LinkageException {
        try {
            readInstructions();
        } catch (AssertionFailed ae) {
            ae.addContext(getContext());
            throw ae;
        } catch (LinkageException le) {
            le.addContext(this);
            throw le;
        }
        doInstructionTrace();
        return ilist;
    }

    private void doInstructionTrace() {
        String name = method.toString();
        if (vm.traceir0(name)) {
            vm.tracer().traceln("\n++IR0 trace for "+name);
            ilist.trace(vm.tracer(), vm.loader(), method.parent().getSourceFilePath(), vm.traceip(), true);
            vm.tracer().traceln("\n--IR0 trace for "+name+"\n");
        }
    }

    /**
     * Append an instruction to the instruction list.
     * @param inst The instruction to append.
     */
    private Instruction append(Instruction inst) throws LinkageException {
        if (inst != null) {
            // Set the original context of the instruction
            int ip = in.getLastIP();
            inst.setContext(ip, getSourceLine(ip)); // line number etc.
            ilist.append(inst);
            Assert.that(inst.type() == null || inst.type().vm() == method.vm());
        }
        return inst;
    }

    /**
     * Append an instruction and then push it.
     */
    private Instruction appendPush(Instruction inst) throws LinkageException {
        append(inst);
        frame.push(inst, true);
        return inst;
    }

    /**
     * Read all the instructions for the method and build the IR graph.
     */
    private void readInstructions() throws LinkageException {
        // Setup initial state
        setControlFlow(true);

        // Check that this method does not try to implement a superclass's final method.
        if (!method.isStatic()) {
            Type superType = method.parent().superType();
            while (superType != vm.UNIVERSE) {
                Method superMethod = superType.lookupMethod(method.name(), method.getParms(), method.type(), method.parent(), false);
                if (superMethod == null) {
                    break;
                }
                frame.verify(!superMethod.isFinal(), VEConst.FINAL_METHOD_OVERRIDE);
                superType = superMethod.parent().superType();
            }
        }

        // If this is a synchronized method then execute a monitorenter and open a try block
        Target finalTarget = null;
        if (method.isSynchronized()) {
            Type[] stack  = new Type[]  { vm.THROWABLE };
            Type[] locals = frame.getParmLocalTypes();
            StackMap.Entry entry = new StackMap.Entry(stack, locals, locals);
            finalTarget = new Target(9999999, entry);
            finalTarget.setExceptionType(vm.THROWABLE);

            // Execute the monitorenter
            methodMonitorEnter();

            // Open the try block
            op_handlerstart(finalTarget, true);
        }

        // Read through all the bytecodes
        while (!in.atEof()) {

            // Print the trace if requested
            frame.printState(false);

            // Get the next bytecode
            int code = in.readBytecode("bcode");

            // If it is not possible to flow into this instruction then there
            // must be a stackmap entry for this ip address.
            frame.verify(flowFromPredecessor || (code >= JVMConst.opc_branchtarget), VEConst.SEQ_BAD_TYPE);

            // Record the current flow state
            boolean currentFlow = flowFromPredecessor;

            // Assume that this instruction will flow into the next one
            setControlFlow(true);

            /*
             * Check that if an exception were to occur on the following bytecode
             * that all the active exception handlers have compatible stackmaps.
             * However don't do this for the pseudo-instructions.
             */
            if (code < JVMConst.opc_branchtarget) {
                verifyActiveHandlerMaps();
            }

            // Dispatch to the specific bytecode
            switch (code) {
                case JVMConst.opc_nop:             op_nop();                                                             break;
                case JVMConst.opc_aconst_null:     op_constant(ifactory.createLoadConstantNull());                       break;
                case JVMConst.opc_iconst_m1:       op_constant(ifactory.createLoadConstantInt(-1));                      break;
                case JVMConst.opc_iconst_0:        op_constant(ifactory.createLoadConstantInt(0));                       break;
                case JVMConst.opc_iconst_1:        op_constant(ifactory.createLoadConstantInt(1));                       break;
                case JVMConst.opc_iconst_2:        op_constant(ifactory.createLoadConstantInt(2));                       break;
                case JVMConst.opc_iconst_3:        op_constant(ifactory.createLoadConstantInt(3));                       break;
                case JVMConst.opc_iconst_4:        op_constant(ifactory.createLoadConstantInt(4));                       break;
                case JVMConst.opc_iconst_5:        op_constant(ifactory.createLoadConstantInt(5));                       break;
                case JVMConst.opc_lconst_0:        op_constant(ifactory.createLoadConstantLong(0L));                     break;
                case JVMConst.opc_lconst_1:        op_constant(ifactory.createLoadConstantLong(1L));                     break;
/*if[FLOATS]*/
                case JVMConst.opc_fconst_0:        op_constant(ifactory.createLoadConstantFloat((float)0));              break;
                case JVMConst.opc_fconst_1:        op_constant(ifactory.createLoadConstantFloat((float)1));              break;
                case JVMConst.opc_fconst_2:        op_constant(ifactory.createLoadConstantFloat((float)2));              break;
                case JVMConst.opc_dconst_0:        op_constant(ifactory.createLoadConstantDouble((double)0));            break;
                case JVMConst.opc_dconst_1:        op_constant(ifactory.createLoadConstantDouble((double)1));            break;
/*end[FLOATS]*/
                case JVMConst.opc_bipush:          op_constant(ifactory.createLoadConstantInt(in.readByte(null)));       break;
                case JVMConst.opc_sipush:          op_constant(ifactory.createLoadConstantInt(in.readShort(null)));      break;
                case JVMConst.opc_ldc:             op_constant(in.readLdc()   );                                         break;
                case JVMConst.opc_ldc_w:           op_constant(in.readLdc_w() );                                         break;
                case JVMConst.opc_ldc2_w:          op_constant(in.readLdc2_w());                                         break;
                case JVMConst.opc_iload:           op_load(vm.INT,    in.readSlot1(1));                                  break;
                case JVMConst.opc_lload:           op_load(vm.LONG,   in.readSlot1(2));                                  break;
/*if[FLOATS]*/
                case JVMConst.opc_fload:           op_load(vm.FLOAT,  in.readSlot1(1));                                  break;
                case JVMConst.opc_dload:           op_load(vm.DOUBLE, in.readSlot1(2));                                  break;
/*end[FLOATS]*/
                case JVMConst.opc_aload:           op_load(vm.NULLOBJECT, in.readSlot1(1));                              break;
                case JVMConst.opc_iload_0:         op_load(vm.INT,    0);                                                break;
                case JVMConst.opc_iload_1:         op_load(vm.INT,    1);                                                break;
                case JVMConst.opc_iload_2:         op_load(vm.INT,    2);                                                break;
                case JVMConst.opc_iload_3:         op_load(vm.INT,    3);                                                break;
                case JVMConst.opc_lload_0:         op_load(vm.LONG,   0);                                                break;
                case JVMConst.opc_lload_1:         op_load(vm.LONG,   1);                                                break;
                case JVMConst.opc_lload_2:         op_load(vm.LONG,   2);                                                break;
                case JVMConst.opc_lload_3:         op_load(vm.LONG,   3);                                                break;
/*if[FLOATS]*/
                case JVMConst.opc_fload_0:         op_load(vm.FLOAT,  0);                                                break;
                case JVMConst.opc_fload_1:         op_load(vm.FLOAT,  1);                                                break;
                case JVMConst.opc_fload_2:         op_load(vm.FLOAT,  2);                                                break;
                case JVMConst.opc_fload_3:         op_load(vm.FLOAT,  3);                                                break;
                case JVMConst.opc_dload_0:         op_load(vm.DOUBLE, 0);                                                break;
                case JVMConst.opc_dload_1:         op_load(vm.DOUBLE, 1);                                                break;
                case JVMConst.opc_dload_2:         op_load(vm.DOUBLE, 2);                                                break;
                case JVMConst.opc_dload_3:         op_load(vm.DOUBLE, 3);                                                break;
/*end[FLOATS]*/
                case JVMConst.opc_aload_0:         op_load(vm.NULLOBJECT, 0);                                            break;
                case JVMConst.opc_aload_1:         op_load(vm.NULLOBJECT, 1);                                            break;
                case JVMConst.opc_aload_2:         op_load(vm.NULLOBJECT, 2);                                            break;
                case JVMConst.opc_aload_3:         op_load(vm.NULLOBJECT, 3);                                            break;
                case JVMConst.opc_iaload:          op_aload(vm.INT_ARRAY   );                                            break;
                case JVMConst.opc_laload:          op_aload(vm.LONG_ARRAY  );                                            break;
/*if[FLOATS]*/
                case JVMConst.opc_faload:          op_aload(vm.FLOAT_ARRAY );                                            break;
                case JVMConst.opc_daload:          op_aload(vm.DOUBLE_ARRAY);                                            break;
/*end[FLOATS]*/
                case JVMConst.opc_aaload:          op_aload(vm.NULLOBJECT_ARRAY);                                        break;
                case JVMConst.opc_baload:          op_aload(vm.BYTE_OR_BOOLEAN_ARRAY);                                   break;
                case JVMConst.opc_caload:          op_aload(vm.CHAR_ARRAY  );                                            break;
                case JVMConst.opc_saload:          op_aload(vm.SHORT_ARRAY );                                            break;
                case JVMConst.opc_istore:          op_store(vm.INT,    in.readSlot1(1));                                 break;
                case JVMConst.opc_lstore:          op_store(vm.LONG,   in.readSlot1(2));                                 break;
/*if[FLOATS]*/
                case JVMConst.opc_fstore:          op_store(vm.FLOAT,  in.readSlot1(1));                                 break;
                case JVMConst.opc_dstore:          op_store(vm.DOUBLE, in.readSlot1(2));                                 break;
/*end[FLOATS]*/
                case JVMConst.opc_astore:          op_store(vm.NULLOBJECT, in.readSlot1(1));                             break;
                case JVMConst.opc_istore_0:        op_store(vm.INT,    0);                                               break;
                case JVMConst.opc_istore_1:        op_store(vm.INT,    1);                                               break;
                case JVMConst.opc_istore_2:        op_store(vm.INT,    2);                                               break;
                case JVMConst.opc_istore_3:        op_store(vm.INT,    3);                                               break;
                case JVMConst.opc_lstore_0:        op_store(vm.LONG,   0);                                               break;
                case JVMConst.opc_lstore_1:        op_store(vm.LONG,   1);                                               break;
                case JVMConst.opc_lstore_2:        op_store(vm.LONG,   2);                                               break;
                case JVMConst.opc_lstore_3:        op_store(vm.LONG,   3);                                               break;
/*if[FLOATS]*/
                case JVMConst.opc_fstore_0:        op_store(vm.FLOAT,  0);                                               break;
                case JVMConst.opc_fstore_1:        op_store(vm.FLOAT,  1);                                               break;
                case JVMConst.opc_fstore_2:        op_store(vm.FLOAT,  2);                                               break;
                case JVMConst.opc_fstore_3:        op_store(vm.FLOAT,  3);                                               break;
                case JVMConst.opc_dstore_0:        op_store(vm.DOUBLE, 0);                                               break;
                case JVMConst.opc_dstore_1:        op_store(vm.DOUBLE, 1);                                               break;
                case JVMConst.opc_dstore_2:        op_store(vm.DOUBLE, 2);                                               break;
                case JVMConst.opc_dstore_3:        op_store(vm.DOUBLE, 3);                                               break;
/*end[FLOATS]*/
                case JVMConst.opc_astore_0:        op_store(vm.NULLOBJECT, 0);                                           break;
                case JVMConst.opc_astore_1:        op_store(vm.NULLOBJECT, 1);                                           break;
                case JVMConst.opc_astore_2:        op_store(vm.NULLOBJECT, 2);                                           break;
                case JVMConst.opc_astore_3:        op_store(vm.NULLOBJECT, 3);                                           break;
                case JVMConst.opc_iastore:         op_astore(vm.INT_ARRAY   );                                           break;
                case JVMConst.opc_lastore:         op_astore(vm.LONG_ARRAY  );                                           break;
/*if[FLOATS]*/
                case JVMConst.opc_fastore:         op_astore(vm.FLOAT_ARRAY );                                           break;
                case JVMConst.opc_dastore:         op_astore(vm.DOUBLE_ARRAY);                                           break;
/*end[FLOATS]*/
                case JVMConst.opc_aastore:         op_astore(vm.NULLOBJECT_ARRAY);                                       break;
                case JVMConst.opc_bastore:         op_astore(vm.BYTE_OR_BOOLEAN_ARRAY);                                  break;
                case JVMConst.opc_castore:         op_astore(vm.CHAR_ARRAY  );                                           break;
                case JVMConst.opc_sastore:         op_astore(vm.SHORT_ARRAY );                                           break;
                case JVMConst.opc_pop:             op_pop();                                                             break;
                case JVMConst.opc_pop2:            op_pop2();                                                            break;
                case JVMConst.opc_dup:
                case JVMConst.opc_dup_x1:
                case JVMConst.opc_dup_x2:
                case JVMConst.opc_dup2:
                case JVMConst.opc_dup2_x1:
                case JVMConst.opc_dup2_x2:
                case JVMConst.opc_swap:            op_dupOrSwap(code);                                                   break;
                case JVMConst.opc_iadd:            op_arithmetic(vm.INT,    OpConst.ADD, OPC.IADD);                           break;
                case JVMConst.opc_ladd:            op_arithmetic(vm.LONG,   OpConst.ADD, OPC.LADD);                           break;
/*if[FLOATS]*/
                case JVMConst.opc_fadd:            op_arithmetic(vm.FLOAT,  OpConst.ADD, OPC.FADD);                           break;
                case JVMConst.opc_dadd:            op_arithmetic(vm.DOUBLE, OpConst.ADD, OPC.DADD);                           break;
/*end[FLOATS]*/
                case JVMConst.opc_isub:            op_arithmetic(vm.INT,    OpConst.SUB, OPC.ISUB);                           break;
                case JVMConst.opc_lsub:            op_arithmetic(vm.LONG,   OpConst.SUB, OPC.LSUB);                           break;
/*if[FLOATS]*/
                case JVMConst.opc_fsub:            op_arithmetic(vm.FLOAT,  OpConst.SUB, OPC.FSUB);                           break;
                case JVMConst.opc_dsub:            op_arithmetic(vm.DOUBLE, OpConst.SUB, OPC.DSUB);                           break;
/*end[FLOATS]*/
                case JVMConst.opc_imul:            op_arithmetic(vm.INT,    OpConst.MUL, OPC.IMUL);                           break;
                case JVMConst.opc_lmul:            op_arithmetic(vm.LONG,   OpConst.MUL, OPC.LMUL);                           break;
/*if[FLOATS]*/
                case JVMConst.opc_fmul:            op_arithmetic(vm.FLOAT,  OpConst.MUL, OPC.FMUL);                           break;
                case JVMConst.opc_dmul:            op_arithmetic(vm.DOUBLE, OpConst.MUL, OPC.DMUL);                           break;
/*end[FLOATS]*/
                case JVMConst.opc_idiv:            op_arithmetic(vm.INT,    OpConst.DIV, OPC.IDIV);                           break;
                case JVMConst.opc_ldiv:            op_arithmetic(vm.LONG,   OpConst.DIV, OPC.LDIV);                           break;
/*if[FLOATS]*/
                case JVMConst.opc_fdiv:            op_arithmetic(vm.FLOAT,  OpConst.DIV, OPC.FDIV);                           break;
                case JVMConst.opc_ddiv:            op_arithmetic(vm.DOUBLE, OpConst.DIV, OPC.DDIV);                           break;
/*end[FLOATS]*/
                case JVMConst.opc_irem:            op_arithmetic(vm.INT,    OpConst.REM, OPC.IREM);                           break;
                case JVMConst.opc_lrem:            op_arithmetic(vm.LONG,   OpConst.REM, OPC.LREM);                           break;
/*if[FLOATS]*/
                case JVMConst.opc_frem:            op_arithmetic(vm.FLOAT,  OpConst.REM, OPC.FREM);                           break;
                case JVMConst.opc_drem:            op_arithmetic(vm.DOUBLE, OpConst.REM, OPC.DREM);                           break;
/*end[FLOATS]*/
                case JVMConst.opc_ineg:            op_negate(vm.INT);                                                    break;
                case JVMConst.opc_lneg:            op_negate(vm.LONG);                                                   break;
/*if[FLOATS]*/
                case JVMConst.opc_fneg:            op_negate(vm.FLOAT);                                                  break;
                case JVMConst.opc_dneg:            op_negate(vm.DOUBLE);                                                 break;
/*end[FLOATS]*/
                case JVMConst.opc_ishl:            op_shift(vm.INT,  OpConst.SHL, OPC.ISHL);                                  break;
                case JVMConst.opc_lshl:            op_shift(vm.LONG, OpConst.SHL, OPC.LSHL);                                  break;
                case JVMConst.opc_ishr:            op_shift(vm.INT,  OpConst.SHR, OPC.ISHR);                                  break;
                case JVMConst.opc_lshr:            op_shift(vm.LONG, OpConst.SHR, OPC.LSHR);                                  break;
                case JVMConst.opc_iushr:           op_shift(vm.INT,  OpConst.USHR, OPC.IUSHR);                                break;
                case JVMConst.opc_lushr:           op_shift(vm.LONG, OpConst.USHR, OPC.LUSHR);                                break;
                case JVMConst.opc_iand:            op_arithmetic(vm.INT,  OpConst.AND, OPC.IAND);                             break;
                case JVMConst.opc_land:            op_arithmetic(vm.LONG, OpConst.AND, OPC.LAND);                             break;
                case JVMConst.opc_ior:             op_arithmetic(vm.INT,  OpConst.OR, OPC.IOR);                               break;
                case JVMConst.opc_lor:             op_arithmetic(vm.LONG, OpConst.OR, OPC.LOR);                               break;
                case JVMConst.opc_ixor:            op_arithmetic(vm.INT,  OpConst.XOR, OPC.IXOR);                             break;
                case JVMConst.opc_lxor:            op_arithmetic(vm.LONG, OpConst.XOR, OPC.LXOR);                             break;
                case JVMConst.opc_iinc:            op_iinc(in.readSlot1(1), in.readByte(null));                          break;
                case JVMConst.opc_i2l:             op_convert(vm.INT   , vm.LONG  , OpConst.I2L);                             break;
/*if[FLOATS]*/
                case JVMConst.opc_i2f:             op_convert(vm.INT   , vm.FLOAT , OpConst.I2F);                             break;
                case JVMConst.opc_i2d:             op_convert(vm.INT   , vm.DOUBLE, OpConst.I2D);                             break;
/*end[FLOATS]*/
                case JVMConst.opc_l2i:             op_convert(vm.LONG  , vm.INT   , OpConst.L2I);                             break;
/*if[FLOATS]*/
                case JVMConst.opc_l2f:             op_convert(vm.LONG  , vm.FLOAT , OpConst.L2F);                             break;
                case JVMConst.opc_l2d:             op_convert(vm.LONG  , vm.DOUBLE, OpConst.L2D);                             break;
                case JVMConst.opc_f2i:             op_convert(vm.FLOAT , vm.INT   , OpConst.F2I);                             break;
                case JVMConst.opc_f2l:             op_convert(vm.FLOAT , vm.LONG  , OpConst.F2L);                             break;
                case JVMConst.opc_f2d:             op_convert(vm.FLOAT , vm.DOUBLE, OpConst.F2D);                             break;
                case JVMConst.opc_d2i:             op_convert(vm.DOUBLE, vm.INT   , OpConst.D2I);                             break;
                case JVMConst.opc_d2l:             op_convert(vm.DOUBLE, vm.LONG  , OpConst.D2L);                             break;
                case JVMConst.opc_d2f:             op_convert(vm.DOUBLE, vm.FLOAT , OpConst.D2F);                             break;
/*end[FLOATS]*/
                case JVMConst.opc_i2b:             op_convert(vm.INT   , vm.BYTE  , OpConst.I2B);                             break;
                case JVMConst.opc_i2c:             op_convert(vm.INT   , vm.CHAR  , OpConst.I2C);                             break;
                case JVMConst.opc_i2s:             op_convert(vm.INT   , vm.SHORT , OpConst.I2S);                             break;
                case JVMConst.opc_lcmp:            op_compare(vm.LONG,   OpConst.LCMP, OPC.LCMP);                             break;
/*if[FLOATS]*/
                case JVMConst.opc_fcmpl:           op_compare(vm.FLOAT,  OpConst.FCMPL, OPC.FCMPL);                           break;
                case JVMConst.opc_fcmpg:           op_compare(vm.FLOAT,  OpConst.FCMPG, OPC.FCMPG);                           break;
                case JVMConst.opc_dcmpl:           op_compare(vm.DOUBLE, OpConst.DCMPL, OPC.DCMPL);                           break;
                case JVMConst.opc_dcmpg:           op_compare(vm.DOUBLE, OpConst.DCMPG, OPC.DCMPG);                           break;
/*end[FLOATS]*/
                case JVMConst.opc_ifeq:            op_if(   vm.INT,    OpConst.EQ, in.readTarget2());                         break;
                case JVMConst.opc_ifne:            op_if(   vm.INT,    OpConst.NE, in.readTarget2());                         break;
                case JVMConst.opc_iflt:            op_if(   vm.INT,    OpConst.LT, in.readTarget2());                         break;
                case JVMConst.opc_ifge:            op_if(   vm.INT,    OpConst.GE, in.readTarget2());                         break;
                case JVMConst.opc_ifgt:            op_if(   vm.INT,    OpConst.GT, in.readTarget2());                         break;
                case JVMConst.opc_ifle:            op_if(   vm.INT,    OpConst.LE, in.readTarget2());                         break;
                case JVMConst.opc_if_icmpeq:       op_ifcmp(vm.INT,    OpConst.EQ, in.readTarget2());                         break;
                case JVMConst.opc_if_icmpne:       op_ifcmp(vm.INT,    OpConst.NE, in.readTarget2());                         break;
                case JVMConst.opc_if_icmplt:       op_ifcmp(vm.INT,    OpConst.LT, in.readTarget2());                         break;
                case JVMConst.opc_if_icmpge:       op_ifcmp(vm.INT,    OpConst.GE, in.readTarget2());                         break;
                case JVMConst.opc_if_icmpgt:       op_ifcmp(vm.INT,    OpConst.GT, in.readTarget2());                         break;
                case JVMConst.opc_if_icmple:       op_ifcmp(vm.INT,    OpConst.LE, in.readTarget2());                         break;
                case JVMConst.opc_if_acmpeq:       op_ifcmp(vm.NULLOBJECT, OpConst.EQ, in.readTarget2());                     break;
                case JVMConst.opc_if_acmpne:       op_ifcmp(vm.NULLOBJECT, OpConst.NE, in.readTarget2());                     break;
                case JVMConst.opc_goto:            op_goto(in.readTarget2());                                            break;
                case JVMConst.opc_jsr:             op_error();                                                           break;
                case JVMConst.opc_ret:             op_error();                                                           break;
                case JVMConst.opc_tableswitch:     op_tableswitch();                                                     break;
                case JVMConst.opc_lookupswitch:    op_lookupswitch();                                                    break;
                case JVMConst.opc_ireturn:         op_return(vm.INT);                                                    break;
                case JVMConst.opc_lreturn:         op_return(vm.LONG);                                                   break;
/*if[FLOATS]*/
                case JVMConst.opc_freturn:         op_return(vm.FLOAT);                                                  break;
                case JVMConst.opc_dreturn:         op_return(vm.DOUBLE);                                                 break;
/*end[FLOATS]*/
                case JVMConst.opc_areturn:         op_return(method.type());                                             break;
                case JVMConst.opc_return:          op_return();                                                          break;
                case JVMConst.opc_getstatic:       op_getstatic(in.readField(true));                                     break;
                case JVMConst.opc_putstatic:       op_putstatic(in.readField(true));                                     break;
                case JVMConst.opc_getfield:        op_getfield(in.readField(false));                                     break;
                case JVMConst.opc_putfield:        op_putfield(in.readField(false));                                     break;
                case JVMConst.opc_invokevirtual:   op_invokevirtual(in.readMethod(false));                               break;
                case JVMConst.opc_invokespecial:   op_invokespecial(in.readMethod(false));                               break;
                case JVMConst.opc_invokestatic:    op_invokestatic(in.readMethod(true));                                 break;
                case JVMConst.opc_invokeinterface: op_invokeinterface(in.readMethod(false),in.readUnsignedShort(null));  break;
                case JVMConst.opc_xxxunusedxxx:    op_error();                                                           break;
                case JVMConst.opc_new:             op_new(in.readNewType());                                             break;
                case JVMConst.opc_newarray:        op_newarray(in.readNewArrayType());                                   break;
                case JVMConst.opc_anewarray:       op_anewarray(in.readType());                                          break;
                case JVMConst.opc_arraylength:     op_arraylength();                                                     break;
                case JVMConst.opc_athrow:          op_athrow();                                                          break;
                case JVMConst.opc_checkcast:       op_checkcast(in.readType());                                          break;
                case JVMConst.opc_instanceof:      op_instanceof(in.readType());                                         break;
                case JVMConst.opc_monitorenter:    op_monitorenter(false);                                               break;
                case JVMConst.opc_monitorexit:     op_monitorexit(false);                                                break;
                case JVMConst.opc_wide:            op_wide(in.readUnsignedByte(null));                                   break;
                case JVMConst.opc_multianewarray:  op_multianewarray(in.readType(), in.readUnsignedByte(null));          break;
                case JVMConst.opc_ifnull:          op_if(vm.NULLOBJECT, OpConst.EQ, in.readTarget2());                        break;
                case JVMConst.opc_ifnonnull:       op_if(vm.NULLOBJECT, OpConst.NE, in.readTarget2());                        break;
                case JVMConst.opc_goto_w:          op_goto(in.readTarget4());                                            break;
                case JVMConst.opc_jsr_w:           op_error();                                                           break;
                case JVMConst.opc_breakpoint:      op_error();                                                           break;
                case JVMConst.opc_branchtarget:    op_branchtarget(in.readTarget(), currentFlow);                        break;
                case JVMConst.opc_exceptiontarget: op_exceptiontarget(in.readTarget());                                  break;
                case JVMConst.opc_handlerstart:    op_handlerstart(in.readTryPoint(true), currentFlow);                  break;
                case JVMConst.opc_handlerend:      op_handlerend(in.readTryPoint(false), currentFlow);                   break;
                default:                  op_error();                                                           break;
            }
        }

        frame.verify(!flowFromPredecessor, VEConst.FALL_THROUGH);

       /*
        * If this is a synchronized method then place the handler
        * here that will release the monitor if an exception is thrown
        */
        if (finalTarget != null) {
            op_handlerend(finalTarget, false);
            frame.adjustMaxStack(1);
            op_exceptiontarget(finalTarget);
            methodMonitorExit();
            op_athrow();
            frame.adjustMaxStack(-1);
        }

        // Complete the emulation of the frame
        frame.finish();

        if (irHandlers.size() > 0) {
            ExceptionHandlerTable.Entry[] entries = new ExceptionHandlerTable.Entry[irHandlers.size()];
            irHandlers.copyInto(entries);
            for (int i = 0; i != entries.length; ++i) {
                ExceptionHandlerTable.Entry handler = entries[i];
                if (handler.handlerEntry() == null) {
                    Target target = handler.handlerEntryTarget();
                    LoadException entry = (LoadException)target.getInstruction();
                    Assert.that(entry != null);
                    handler.setHandlerEntry(entry);
                }
            }
            ilist.setHandlerTable(new ExceptionHandlerTable(entries));
        }
    }

    /**
     * Process a bad bytecode.
     */
    void op_error() throws LinkageException {
        frame.verify(false, VEConst.BAD_INSTR);
    }



   /**
    * methodMonitorEnter
    */
    void methodMonitorEnter() throws LinkageException {
        if (!method.isStatic()) {
            frame.adjustMaxStack(1);
            op_load(vm.NULLOBJECT, 0);
            frame.adjustMaxStack(-1);
        }
        op_monitorenter(method.isStatic());
    }


   /**
    * methodMonitorExit
    */
    void methodMonitorExit() throws LinkageException {
        if (!method.isStatic()) {
            frame.adjustMaxStack(1);
            op_load(vm.NULLOBJECT, 0);
            frame.adjustMaxStack(-1);
        }
        op_monitorexit(method.isStatic());
    }

   /**
    * Read a wide instruction
    * @return an instruction
    */
    void op_wide(int code) throws LinkageException {
        switch (code) {
            case JVMConst.opc_iinc               : op_iinc(in.readSlot2(1), in.readShort(null));             break;
            case JVMConst.opc_iload              : op_load(vm.INT,    in.readSlot2(1));                      break;
            case JVMConst.opc_lload              : op_load(vm.LONG,   in.readSlot2(2));                      break;
/*if[FLOATS]*/
            case JVMConst.opc_fload              : op_load(vm.FLOAT,  in.readSlot2(1));                      break;
            case JVMConst.opc_dload              : op_load(vm.DOUBLE, in.readSlot2(2));                      break;
/*end[FLOATS]*/
            case JVMConst.opc_aload              : op_load(vm.NULLOBJECT, in.readSlot2(1));                  break;
            case JVMConst.opc_istore             : op_store(vm.INT,    in.readSlot2(1));                     break;
            case JVMConst.opc_lstore             : op_store(vm.LONG,   in.readSlot2(2));                     break;
/*if[FLOATS]*/
            case JVMConst.opc_fstore             : op_store(vm.FLOAT,  in.readSlot2(1));                     break;
            case JVMConst.opc_dstore             : op_store(vm.DOUBLE, in.readSlot2(2));                     break;
/*end[FLOATS]*/
            case JVMConst.opc_astore             : op_store(vm.NULLOBJECT, in.readSlot2(1));                 break;
            default                     : op_error();                                               break;
        }
    }


   /**
    * Convert an iinc instruction into a load, add, and store
    * @param javacIndex the local variable slot number
    * @param value the value to be added to the slot
    */
    void op_iinc(int javacIndex, int value) throws LinkageException {
        if (value == 1 || value == -1) {
            Local local = frame.getLocal(vm.INT, javacIndex);
            append(ifactory.createIncDecLocal(local, value == 1));
        }
        else {
            frame.adjustMaxStack(2);
            op_load(vm.INT, javacIndex);
            op_constant(ifactory.createLoadConstantInt(value));
            op_arithmetic(vm.INT, OpConst.ADD, OPC.IADD);
            op_store(vm.INT, javacIndex);
            frame.adjustMaxStack(-2);
        }
    }


   /**
    * Nop - do nothing
    */
    void op_nop() throws LinkageException {
    }

   /**
    * Pop item from stack,
    */
    void op_pop() throws LinkageException {
        append(ifactory.createPop(pop(vm.UNIVERSE)));
    }

   /**
    * Pop two words
    */
    void op_pop2() throws LinkageException {
        Instruction x1 = pop(vm.UNIVERSE);
        if(!x1.isTwoWords()) {
            Instruction x2 = pop(vm.UNIVERSE);
            frame.verify(!x2.isTwoWords(), VEConst.STACK_EXPECT_CAT1);
            append(ifactory.createPop(x1));
            append(ifactory.createPop(x2));
        }
        else {
            append(ifactory.createPop(x1));
        }
    }

    void op_dupOrSwap(int opcode) throws LinkageException {
        Instruction last = ilist.tail();

        // Special code to try and replace simple dup's with a load or constant
        if (OPTIMIZEDUPS && opcode == JVMConst.opc_dup) {
            if (last instanceof LoadConstant) {
                LoadConstant lc = (LoadConstant)last;
                op_constant(ifactory.createLoadConstantInt(lc.getInt()));
                return;
            } else if (last instanceof LoadLocal) {
                LoadLocal ll = (LoadLocal)last;
                op_load(ll.local());
                return;
            }
        }

        // Normal dup case
        frame.dupSwap(opcode);
        while (last instanceof HandlerEnter || last instanceof HandlerExit) {
            last = last.getPrev();
        }
        last.addDupSwapSuccessor(opcode);
    }

   /**
    * Push a constant value
    */
    void op_constant(Instruction inst) throws LinkageException {
        appendPush(inst);
    }

   /**
    * Allocate a load local instruction.
    *
    */
    void op_load(Type type, int javacIndex) throws LinkageException {
        Local local = frame.getLocal(type, javacIndex);
        op_load(local);
    }

   /**
    * Allocate a load local instruction.
    *
    */
    void op_load(Local local) throws LinkageException {
        LoadLocal ld = (LoadLocal)ifactory.createLoadLocal(local, local.type());
        appendPush(ld);
    }


   /**
    * Allocate a store local instruction
    */
    void op_store(Type basicType, int javacIndex) throws LinkageException {
        Assert.that(basicType != vm.OBJECT);
        Instruction parm = pop(basicType);
        Type actualType =  parm.type();
        frame.verifyLocalIndex(actualType, javacIndex);
        Local local = frame.local(javacIndex);
        boolean isInitializer = (local == null);
        local = frame.allocLocal(actualType.localType(), javacIndex, in.getCurrentIP(), null);
        frame.setLocal(local, javacIndex);
        StoreLocal st = (StoreLocal)ifactory.createStoreLocal(local, parm);

        if (isInitializer) {
            st.setIsInitializer();
        }
        if (javacIndex == 0) {
            receiverWasOverwritten = true;
            frame.verifyTranslatable(method.name() != vm.INIT, "Translator cannot handle <init> methods that overwrite the receiver");
        }
        append(st);
    }

   /**
    * Allocate an array load instruction
    */
    void op_aload(Type type) throws LinkageException {
        Instruction index = pop(vm.INT);
        Instruction array = castLoadConstantNullToType(pop(type), type);
        appendPush(ifactory.createLoadIndexed(array, index, type));
    }

   /**
    * Allocate an array store instruction
    */
    void op_astore(Type type) throws LinkageException {
        Instruction value = pop(type.elementType());
        Instruction index = pop(vm.INT);
        Instruction array = castLoadConstantNullToType(pop(type), type);

        // Special handling for AASTORE
        if (type == vm.NULLOBJECT_ARRAY) {
            if (!array.type().elementType().isArray() && !value.type().isArray()) {
                /* As a special case, if both the array element type and
                 * the type are both non-array types (or NULL), then
                 * allow the aastore - it will be checked at runtime. */
            } else if (array.type() == vm.NULLOBJECT || value.type().vIsAssignableTo(array.type().elementType())) {
                /* Success */
            } else {
                frame.verify(false, VEConst.AASTORE_BAD_TYPE);
            }
        }
        append(ifactory.createStoreIndexed(array, index, value, type));
    }


   /**
    * Allocate an arithmetic instruction
    */
    void op_arithmetic(Type type, OpConst op, int opcode) throws LinkageException {
        Instruction p2 = pop(type);
        Instruction p1 = pop(type);
        appendPush(ifactory.createArithmeticOp(op, opcode, p1, p2));
    }

   /**
    * Allocate an compare instruction
    */
    void op_compare(Type type, OpConst op, int opcode) throws LinkageException {
        Instruction p2 = pop(type);
        Instruction p1 = pop(type);
        appendPush(ifactory.createArithmeticOpCmp(op, opcode, p1, p2));
    }

   /**
    * Allocate a shift instruction
    */
    void op_shift(Type type, OpConst op, int opcode) throws LinkageException {
        Instruction p2 = pop(vm.INT);
        Instruction p1 = pop(type);
        appendPush(ifactory.createArithmeticOp(op, opcode, p1, p2));
    }

   /**
    * Allocate a negate instruction
    */
    void op_negate(Type type) throws LinkageException {
        Instruction p1 = pop(type);
        appendPush(ifactory.createNegateOp(p1));
    }

   /**
    * Allocate a type convertion instruction
    */
    void op_convert(Type typeFrom, Type typeTo, OpConst op) throws LinkageException {
        Instruction p1 = pop(typeFrom);
        appendPush(ifactory.createConvertOp(op, typeFrom, typeTo, p1));
    }

   /**
    * Allocate an if-zero instruction
    */
    void op_if(Type type, OpConst op, Target target) throws LinkageException {
        Instruction p2 = pop(type);
        Instruction result = ifactory.createIfOp(op, null, p2, target);
        append(result);
        endBasicBlock((BasicBlockExitDelimiter)result, target, null);
    }

   /**
    * Allocate an if-cmp instruction
    */
    void op_ifcmp(Type type, OpConst op, Target target) throws LinkageException {
        Instruction p2 = pop(type);
        Instruction p1 = pop(type);
        Instruction result = ifactory.createIfOp(op, p1, p2, target);
        append(result);
        endBasicBlock((BasicBlockExitDelimiter)result, target, null);
    }

   /**
    * Allocate a goto instruction
    */
    void op_goto(Target target) throws LinkageException {
        Instruction result = ifactory.createGoto(target);
        append(result);
        endBasicBlock((BasicBlockExitDelimiter)result, target, null);
    }

   /**
    * Allocate a tableswitch instruction
    */
    void op_tableswitch() throws LinkageException {
        Instruction key = pop(vm.INT);
        in.roundToCellBoundry();
        Target defaultTarget =  in.readTarget4();
        int low  = in.readInt(null);
        int high = in.readInt(null);
        TableSwitch tableSwitch = (TableSwitch)ifactory.createTableSwitch(key, low, high, defaultTarget);
        for (int i = low ; i <= high ; i++) {
            Target target = in.readTarget4();
            tableSwitch.addTarget(i, target);
        }
        append(tableSwitch);
        endBasicBlock((BasicBlockExitDelimiter)tableSwitch, defaultTarget, tableSwitch.targets());
    }

   /**
    * Allocate a lookupswitch instruction
    */
    void op_lookupswitch() throws LinkageException {
        int currentIP = in.getLastIP();
        Instruction key = pop(vm.INT);
        in.roundToCellBoundry();
        Target defaultTarget =  in.readTarget4();
        int npairs  = in.readInt(null);
        int lastMatch = -1;
        LookupSwitch lookupSwitch = (LookupSwitch)ifactory.createLookupSwitch(key, npairs, defaultTarget);
        for (int i = 0 ; i < npairs ; i++) {
            int match = in.readInt(null);
            if (i == 0) {
                lastMatch = match;
            }
            Target target = in.readTarget4();
            frame.verify(match >= lastMatch, VEConst.BAD_LOOKUPSWITCH);
            lookupSwitch.addTarget(i, match, target);
            lastMatch = match;
        }
        append(lookupSwitch);
        endBasicBlock((BasicBlockExitDelimiter)lookupSwitch, defaultTarget, lookupSwitch.targets());
    }

   /**
    * Allocate an instanceof instruction
    */
    void op_instanceof(Type type) throws LinkageException {
        Instruction value = pop(vm.NULLOBJECT);
        appendPush(ifactory.createInstanceOf(type, value));
    }

   /**
    * Allocate a checkcast instruction
    */
    void op_checkcast(Type type) throws LinkageException {
        Instruction value = popInitializedOrPrimitive(vm.NULLOBJECT);
        appendPush(ifactory.createCheckCast(type, value));
    }

   /**
    * Allocate a monitorenter instruction
    */
    void op_monitorenter(boolean isForClass) throws LinkageException {
        if (!isForClass) {
            Instruction p1 = castLoadConstantNullToType(popInitializedOrPrimitive(vm.NULLOBJECT), vm.OBJECT);
            append(ifactory.createMonitorEnter(p1));
        }
        else {
            append(ifactory.createMonitorEnter(null));
        }
    }

   /**
    * Allocate a monitorexit instruction
    */
    void op_monitorexit(boolean isForClass) throws LinkageException {
        if (!isForClass) {
            Instruction p1 = castLoadConstantNullToType(popInitializedOrPrimitive(vm.NULLOBJECT), vm.OBJECT);
            append(ifactory.createMonitorExit(p1));
        }
        else {
            append(ifactory.createMonitorExit(null));
        }
    }

    /**
     * Allocate a new instruction. The new instruction in Squawk simply performs the
     * class initialization semantics of 'new'. The memory allocation is actually
     * performed in the call to <init>. Any call to <init> that receives a null
     * receiver does the memory allocation.
     */
    void op_new(Type type) throws LinkageException {
        Instruction newInst = ifactory.createNewObject(type);
        appendPush(newInst);
    }

   /**
    * Allocate a newarray instruction
    */
    void op_newarray(Type type) throws LinkageException {
        Instruction size = pop(vm.INT);
        appendPush(ifactory.createNewArray(type, size));
    }

   /**
    * Allocate an anewarray instruction
    */
    void op_anewarray(Type type) throws LinkageException {
        Instruction size = pop(vm.INT);
        appendPush(ifactory.createNewArray(type.asArray(), size));
    }

   /**
    * Allocate a multianewarray instruction
    */
    void op_multianewarray(Type type, int dims) throws LinkageException {
        frame.verify(dims > 0 && dims <= type.dimensions(), VEConst.MULTIANEWARRAY + ": dims="+dims+" type="+type.dimensions());
        Instruction[] dimList = new Instruction[dims];

        if (dims == 1) {
            dimList[0] = pop(vm.INT);
        }
        else {
            for (int i = dims - 1; i >= 0; i--) {
                dimList[i] = pop(vm.INT);

                // Each dimension must be spillt
                dimList[i].spills();
            }
        }

        // Create a 'NewArray' for the first dimension
        Instruction newArray = ifactory.createNewArray(type, dimList[0]);
        appendPush(newArray);

        // Create 'NewDimension's for the remaining dimensions.
        for (int i = 1; i != dimList.length; i++) {
            Instruction newDimension = ifactory.createNewDimension(pop(newArray.type()), dimList[i]);
            appendPush(newDimension);
            newArray = newDimension;
        }
    }

   /**
    * Allocate an arraylength instruction
    */
    void op_arraylength() throws LinkageException {
        Instruction array = pop(vm.NULLOBJECT);
        frame.verifyTranslatable(array.type() != vm.NULLOBJECT, "Cannot do arraylength on a null array object");
        frame.verify(array.type() == vm.NULLOBJECT || array.type().isArray(), VEConst.EXPECT_ARRAY);
        appendPush(ifactory.createArrayLength(array));
    }

   /**
    * Getstatic
    */
    void op_getstatic(Field field) throws LinkageException {
        appendPush(ifactory.createLoadField(field, null, field.parent() == method.parent()));
    }

   /**
    * Putstatic
    */
    void op_putstatic(Field field) throws LinkageException {
       /*
        * javac 1.4 produces putstatics for constants. These are removed by
        * ifactory.createStoreField() If they are then the clinit is not needed.
        */
        Instruction sf = ifactory.createStoreField(field, null, popInitializedOrPrimitive(field.type()), field.parent() == method.parent());
        if (sf != null) {
            append(sf);
        }
        else {
            Instruction tail = ilist.tail();
            Assert.that(tail instanceof LoadConstant);
            ilist.remove(tail);
        }
    }

   /**
    * Getfield
    */
    void op_getfield(Field field) throws LinkageException {
        Instruction ref = castLoadConstantNullToType(getInstanceFieldInstructionReceiver(field, false), field.parent());
        appendPush(ifactory.createLoadField(field, ref, isLoadReceiver(ref)));
    }

   /**
    * Putfield
    */
    void op_putfield(Field field) throws LinkageException {
        Instruction value = popInitializedOrPrimitive(field.type());
        Instruction ref = castLoadConstantNullToType(getInstanceFieldInstructionReceiver(field, method.name() == vm.INIT), field.parent());
        append(ifactory.createStoreField(field, ref, value, isLoadReceiver(ref)));
    }

    /**
     * Get the 'objectref' for a putfield or getfield instruction.
     * @param field
     * @param isPutfieldInInit Set to true if this is in an <init> method so that
     * proper tests can be made to allow assignment to fields of the uninitialised
     * object if the fields are declared in this class.
     * @return
     * @throws LinkageException
     */
    private Instruction getInstanceFieldInstructionReceiver(Field field, boolean isPutfieldInInit) throws LinkageException {
        // If the field is protected and in a different package than the receiver must
        // be compatible with the type of the method being verified
        Type expectedType;
        if (field.isProtected() && !method.parent().inSamePackageAs(field.parent())) {
            // Receiver must be same kind as method being verified
            expectedType = method.parent();
        } else {
            expectedType = field.parent();
        }

        // Since -INIT- is not assignable to any object type, need to do some
        // special tests when -INIT- is legal as a receiver for a PUTFIELD
        // (i.e. when inside an <init> method and the field being written is
        // declared in the current class).
        if (isPutfieldInInit && field.parent() == method.parent()) {
            Instruction inst = pop(vm.OBJECT);
            if (inst.type() == vm.INITOBJECT) {
                return inst;
            }
            // Just re-push the receiver to now perform the standard test
            frame.push(inst, false);
        }

        return popInitializedOrPrimitive(expectedType);
    }

    private boolean isLoadReceiver(Instruction inst) {
        if (!method.isStatic() &&
            !inst.wasDuped() &&
            !inst.wasMerged() &&
            inst instanceof LoadLocal)
        {
            Local local = ((LoadLocal)inst).local();
            if (local.javacIndex() == 0 && !receiverWasOverwritten) {
                Assert.that(local.type() == method.parent() || local.type() == vm.INITOBJECT);
                return true;
            }
        }
        return false;
    }

    /**
     * Invoke virtual.
     */
    void op_invokevirtual(Method callee) throws LinkageException {
        frame.verify(callee.name().charAt(0) != '<', VEConst.EXPECT_INVOKESPECIAL);
        op_invoke(callee, Invoke.Form.VIRTUAL, false);
    }

   /**
    * Invoke interface
    */
    void op_invokeinterface(Method callee, int garbage) throws LinkageException {
        frame.verify(callee.name().charAt(0) != '<', VEConst.EXPECT_INVOKESPECIAL);
        op_invoke(callee, Invoke.Form.INTERFACE, false);
    }

    /**
     * Invoke special. Invoke special is one of three things:
     *
     *   1, invoke to <init>
     *   2, invoke to a method in the super
     */
    void op_invokespecial(Method callee) throws LinkageException {
        Type parent = method.parent();
        boolean inHierarchy = parent.vIsAssignableTo(callee.parent());
        Invoke.Form form;
        boolean receiverTypeIsINIT = false;
        if (callee.name() == vm.INIT) {
            form = Invoke.Form.INIT;
        }
        else if (callee.isPrivate()) {
            form = Invoke.Form.VIRTUAL;
        }
        else {
            form = Invoke.Form.SUPER;
        }

        if (form == Invoke.Form.INIT) {

            int receiverIndex = frame.stackSize(false) - (callee.getParms().length + 1);
            receiverTypeIsINIT = frame.peekAt(receiverIndex).type() == vm.INITOBJECT;
            if (receiverTypeIsINIT) {

                // Change the receiver type from INITOBJECT
                frame.renameInitTo(method.parent());
            }

        } else {
            // This is not a call to <init> so the callee must be somewhere
            // in superclass hierarchy
            frame.verify(inHierarchy, VEConst.INVOKESPECIAL);
        }
        op_invoke(callee, form, receiverTypeIsINIT);
    }

    /**
     * Invoke static
     */
    void op_invokestatic(Method callee) throws LinkageException {
        frame.verify(callee.name().charAt(0) != '<', VEConst.EXPECT_INVOKESPECIAL);
        op_invoke(callee, Invoke.Form.STATIC, false);
    }

    /**
     * Invoke
     */
    void op_invoke(Method callee, Invoke.Form form, boolean receiverTypeIsINIT) throws LinkageException {

        Type[] parmTypes = callee.getParms();
        boolean hasReceiver = !callee.isStatic();
        int nparms = parmTypes.length + (hasReceiver ? 1 : 0);

        // Pop declared arguments
        Instruction[] parms = new Instruction[nparms];

        int k = nparms;
        for (int i = parmTypes.length - 1 ; i >= 0; --i) {
            Instruction parm = popInitializedOrPrimitive(parmTypes[i]);
            parms[--k] = parm;
        }

        // Records whether or not the result returned by a call to <init>
        // should be popped.
        boolean popInitResult = false;

        // Records the number of dups that need to be applied to the result
        // of a call to <init>.
        int dupInitCount = 0;

        // Records the return type for the method being called.
        Type returnType = callee.type();

        // If this is not an invokestatic then there are a number of things
        // that need to be done to the receiver
        Instruction receiver = null;
        if (form != Invoke.Form.STATIC) {
            // Special processing for calls to <init>
            if (form == Invoke.Form.INIT) {
                // Pop the receiver. Its type is checked later
                receiver = pop(vm.NULLOBJECT);
                returnType = callee.parent();

                /*
                 * If the receiver was the result of a 'new' then the type of
                 * the receiver must be a type proxy (an ITEM_NewObject in the stackmap).
                 * If so the type returned from the 'new' is converted into
                 * the type the proxy represented.
                 */
                boolean receiverIsFromNew = !receiverTypeIsINIT;
                if (receiverIsFromNew) {
                    Type proxy = receiver.type();

                    // If the receiver was the result of 'new' then its type
                    // must be a TypeProxy otherwise this is an attempt to
                    // re-initialise an initialise object.
                    frame.verify(proxy instanceof TypeProxy, VEConst.EXPECT_UNINIT);

                    Type realType = ((TypeProxy)proxy).getProxy();
                    receiver.changeType(realType);
                    receiver.getTemporaryLocal().changeType(realType);
                    if (receiver instanceof LoadLocal) {
                        LoadLocal ld = (LoadLocal)receiver;
                        ld.local().changeType(realType);
                    }
                } else {
                    // Calls to <init> that are not the result of a 'new' must be this()
                    // or super() calls from one constructor method to another.
                    frame.verify(method.name() == vm.INIT  &&                    // current method is a constructor
                                 callee.parent() == method.parent() ||           // a 'this()' call
                                 callee.parent() == method.parent().superClass(),// a 'super()' call
                                 VEConst.BAD_INIT_CALL);
                }

                /*
                 * Check now to see if the uninitialized object was duped. If so,
                 * there should be exactly one more copy of the instance on the
                 * stack. This instance should also be popped as the result of the
                 * 'invokeinit' instruction being created will replace it.
                 */
                if (receiver.wasDuped()) {
                    TemporaryLocal local = receiver.getTemporaryLocal();
                    byte[] dups = receiver.getDupSwapSuccessors();
                    Assert.that(local != null && dups != null);

                    // If the unintialized object is the receiver inside an <init>
                    // method, then we assert here that the receiver must not have
                    // been overwritten otherwise removing the duping may alter
                    // the semantics of the program. This should (hopefully)
                    // never happen...
                    if (!(receiverIsFromNew)) {
                        Assert.that(receiver instanceof LoadLocal && ((LoadLocal)receiver).local().javacIndex() == 0);
                        frame.verifyTranslatable(!receiverWasOverwritten, "Translator cannot handle <init> methods that overwrite the receiver");
                    }

                    // Ensure that the uninitialised object was only duped by the 'dup'
                    // bytecode. While other stack manipulation bytecodes can legally
                    // be applied to an uninitialised object, trying to propogate their
                    // effects to the value returned by the initialiser will require
                    // extensive analysis. Given that this should never occur (even in
                    // the TCK), we just assert this property here.
                    for (int i = 0; i != dups.length; i++) {
                        frame.verifyTranslatable(dups[i] == JVMConst.opc_dup, "Translator cannot handle stack manipulation other than 'dup' of an uninitialised instance");
                        Assert.that(frame.peek() == receiver);
                        pop(receiver.type());
                    }
                    dupInitCount = dups.length - 1;

                    // 'undo' the duping of the uninitialized object
                    receiver.setDuped(false);
                }
                else {
                    // The value returned by the call to <init> must be popped since the 'new' was not duped.
                    popInitResult = true;
                }
            }
            else {
                // Pop the receiver. Its type is checked later
                receiver = popInitializedOrPrimitive(vm.NULLOBJECT);
            }
            parms[--k] = receiver;

            // Having changed the TypeProxys back, now verify the receiver type was okay
            Type expectedReceiverType;

            // If the method is protected and in a different package then the receiver must
            // be compatible with the type of the method being verified.
            if (callee.isProtected() && !method.parent().inSamePackageAs(callee.parent())) {
                expectedReceiverType = method.parent();
            } else {
                expectedReceiverType = callee.parent();
            }

            // Check that the receiver is of the correct type
            frame.verify(receiver.type().vIsAssignableTo(expectedReceiverType), VEConst.STACK_BAD_TYPE + ": " + receiver.type()+" is not kind of "+expectedReceiverType);
            Local.narrowLocalType(receiver, expectedReceiverType, false);

            if (form == Invoke.Form.INTERFACE || form == Invoke.Form.VIRTUAL) {
                castLoadConstantNullToType(receiver, expectedReceiverType);
            }
        }

        /*
         * The default constructor can be invoked as a virtual method invocation via the special
         * method named "_SQUAWK_INTERNAL_init" defined in Object. This must be considered as
         * a constructor invocation for the purpose of IR building but not for verification.
         */
        if (form == Invoke.Form.VIRTUAL && callee.name() == vm.SQUAWK_INIT && callee.parent() == vm.OBJECT) {
            form = Invoke.Form.INIT;
            popInitResult = true;
        }

        /*
         * Link the instruction in
         */
        if (returnType == vm.VOID) {
            append(ifactory.createInvoke(callee, parms, form));
        }
        else {
            Instruction invokeInst = ifactory.createInvoke(callee, parms, form);
            appendPush(invokeInst);

            /*
             * If this is a call to <init>, we need to insert a pop instruction to
             * remove the initialised object that is returned by a constructor.
             * The only exception to this is when the new object was
             * duped (i.e succesive instructions expect the initialized object
             * to be on the stack) or this is a chained constructor call (i.e. a
             * call to this() or super()).
             */
            if ((form == Invoke.Form.INIT)) {
                Assert.that(!popInitResult || (dupInitCount == 0));
                if (popInitResult) {
                    op_pop();
                }
                else if (dupInitCount > 0) {
                    for (int i = 0; i != dupInitCount; i++) {
                        op_dupOrSwap(JVMConst.opc_dup);
                    }
                }
            }
        }
    }

    /**
     * Allocate a return void instruction
     */
    void op_return() throws LinkageException {
        boolean isReturnFromInit = method.name() == vm.INIT;
        /*
         * The return type for <init> according to javac is void but the return type for
         * <init> according to Squawk is the parent type
         */
        frame.verify(method.type() == vm.VOID || isReturnFromInit, VEConst.EXPECT_RETVAL);
        frame.verify(method.name() != vm.INIT || frame.local(0) == null || frame.local(0).type() != vm.INITOBJECT, VEConst.RETURN_UNINIT_THIS);

        if (method.isSynchronized()) {
            methodMonitorExit();
        }

        Instruction result;
        if (isReturnFromInit) {
            /*
             * Insert a load of 'this' (i.e. the object being constructed).
             */
            frame.adjustMaxStack(1);
            op_load(method.parent(), 0);
            frame.adjustMaxStack(-1);
            Instruction loadThis = pop(method.parent());
            result = ifactory.createReturn(loadThis);
        }
        else {
            result = ifactory.createReturnNull();
        }

        // Force all extra (redundant) values on the stack to be spilled.
        forceStackSpilling();

        append(result);
        endBasicBlock((BasicBlockExitDelimiter)result, null, null);
    }

   /**
    * Allocate a return with value instruction
    */
    void op_return(Type type) throws LinkageException {
        frame.verify(method.type() != vm.VOID && method.name() != vm.INIT, VEConst.EXPECT_NO_RETVAL);
        if (method.isSynchronized()) {     // i.e. if this is a synchronized method
            methodMonitorExit();
        }
        Instruction inst = popInitializedOrPrimitive(type);
        frame.verify(inst.type().vIsAssignableTo(method.type().localType()), VEConst.RETVAL_BAD_TYPE);
        Instruction result = ifactory.createReturn(inst);

        // Force all extra (redundant) values on the stack to be spilled.
        forceStackSpilling();

        append(result);
        endBasicBlock((BasicBlockExitDelimiter)result, null, null);
    }

    /**
     * Allocate a throw instruction
     */
    void op_athrow() throws LinkageException {
        Instruction inst = popInitializedOrPrimitive(vm.THROWABLE);
        Instruction result = ifactory.createThrow(inst);
        append(result);

        // Force all extra (redundant) values on the stack to be spilled.
        forceStackSpilling();

        endBasicBlock((BasicBlockExitDelimiter)result, null, null);
    }

    /**
     * Allocate a branch target instruction.
     * @param target The branch target at the current address.
     * @param fellThrough True if this address is also reached by falling
     * through from the lexical predecessor.
     */
    void op_branchtarget(Target target, boolean fellThrough) throws LinkageException {
        if (!fellThrough) {
            // There is no flow from the lexical predecessor so ignore the
            // derived stack and locals and re-initialize them from the target.
            frame.reinitialize(target);
        }
        else {
            // Make sure the stack is empty here
            frame.spillStack(true, null);

            // Verify recorded stack map against currently derived types.
            verifyBranchTarget(target);
        }

        // Merge frame with state saved for target
        frame.mergeWithTarget(target);

        // Allocate the instruction.
        Instruction result = ifactory.createPhi(target);
        result = append(result);
    }

    /**
     * Allocate an exception target instruction.
     */
    void op_exceptiontarget(Target target) throws LinkageException {
        // Occasionally a TCK test or optimized code (e.g. BCO) will contain
        // a sequence of code such that there is something on the stack at the
        // 'goto' that delimits a try-catch block. This value will be correctly
        // spilled and filled as necessary. However, to correctly handle the
        // code in the exception handler, the stack must be cleared on entry
        // to the handler, just as the stack is conceptually cleared during
        // execution.
        while (frame.stackSize(false) != 0) {
            frame.pop(vm.UNIVERSE);
        }

        frame.verify(target.getStackMapEntry().stack.length == 1, "Bad exception target stack, target.getStackMapEntry().stack.length = "+target.getStackMapEntry().stack.length );
        frame.verify(target.isExceptionTarget(),                  "Bad exception target stack, target.isExceptionTarget() = false" );
        frame.reinitialize(target);
        LoadException entry = (LoadException)ifactory.createLoadException(target);

        for (Enumeration e = irHandlers.elements(); e.hasMoreElements(); ) {
            ExceptionHandlerTable.Entry handler = (ExceptionHandlerTable.Entry)e.nextElement();
            Target handlerEntryTarget = handler.handlerEntryTarget();
            if (handlerEntryTarget == target) {
                Assert.that(handler.handlerEntry() == null);
                handler.setHandlerEntry(entry);
            }
        }

        /*
         * This may be one of the code sequences generated by JDK1.4 javac for
         * synchronization where a 'monitorexit' is wrapped in a try/finally
         * block where the block is it's own handler.
         * In this case, the 'JVMConst.opc_exceptiontarget' instruction will actually appear before the
         * 'JVMConst.opc_handlerend' and so the partially built handler will be on top of the
         * activeHandlers stack.
         */
        if (!activeHandlers.empty()) {
            ExceptionHandlerTable.Entry handler = (ExceptionHandlerTable.Entry)activeHandlers.peek();
            if (handler.handlerEntryTarget() == target) {
                Assert.that(handler.handlerEntry() == null);
                handler.setHandlerEntry(entry);
            }
        }
        appendPush(entry);
    }

    /**
     * Allocate an instruction representing the start of a try block.
     * @param handler The entry point of the exception handler corresponding to
     * the try block.
     * @param fellThrough True if this instruction is reached by falling through
     * from its lexical predecessor.
     */
    void op_handlerstart(Target target, boolean fellThrough) throws LinkageException {
        // "Enter" a try block
        HandlerEnter inst = (HandlerEnter)ifactory.createHandlerEnter(target);
        ExceptionHandlerTable.Entry handler = new ExceptionHandlerTable.Entry(inst, target);
        activeHandlers.push(handler);
        append(inst);
        setControlFlow(fellThrough);
    }

    /**
     * Allocate an instruction representing the end of a try block.
     * @param handler The entry point of the exception handler corresponding to
     * the try block.
     * @param fellThrough True if this instruction is reached by falling through
     * from its lexical predecessor.
     */
    void op_handlerend(Target target, boolean fellThrough) throws LinkageException {
        if (!activeHandlers.empty()) {
            ExceptionHandlerTable.Entry handler = (ExceptionHandlerTable.Entry)activeHandlers.pop();
            if (handler.handlerEntryTarget() == target) {
                HandlerExit inst = (HandlerExit)ifactory.createHandlerExit(target);
                handler.setEnd(inst);

                // Add to the exception handler table being built for the IR
                irHandlers.addElement(handler);

                append(inst);
                setControlFlow(fellThrough);
                return;
            }
            frame.verify(false, "Mismatched handler end");
        }
        frame.verify(false, "Missing handler end");
    }

    /**
     * If the current instruction is in the scope of one or more exception handler,
     * verify that the derived types are match the stack map entries corresponding
     * to the starting byte code offset of the exception handler(s).
     */
    private void verifyActiveHandlerMaps() throws LinkageException {

        Enumeration handlers = activeHandlers.elements();
        while (handlers.hasMoreElements()) {
            Target handler = ((ExceptionHandlerTable.Entry)handlers.nextElement()).handlerEntryTarget();
            frame.matchStackMapLocals(handler.getStackMapEntry().locals, false, VEConst.TARGET_BAD_TYPE);
            frame.mergeWithTarget(handler);
        }
    }

    /**
     * Mark all values on the stack as requiring spilling without the possibility
     * of the graph transformer cancelling the spill. This is required for
     * redundant values on the stack at a 'throw' or 'return' instruction where
     * the extra stack values will never be used.
     *
     * @throws LinkageException
     */
    private void forceStackSpilling() throws LinkageException {
        int stackSize = frame.stackSize(false);
        if (stackSize != 0) {
            for (int i = 0; i != stackSize; ++i) {
                Instruction inst = frame.peekAt(i);

                // The result of this instruction must be spilled immediately
                // after it is produced. The instruction's temporary local is
                // also marked as merged (even though it may not be) so that
                // the instruction is not hoisted by the graph transformer.
                inst.spills();
                inst.getTemporaryLocal().setMerged();
            }
        }
    }

/*---------------------------------------------------------------------------*\
 *                     Basic block & control flow helpers                    *
\*---------------------------------------------------------------------------*/

    /**
     * Indicate if there is direct control flow to the current instruction
     * from its lexical predecessor.
     */
    private boolean flowFromPredecessor = true;

   /**
    * Pointer to the "current" stackmap entry
    */
    private int currentStackMapIndex = 0;


    /**
     * Set control flow variable indicating if the next instruction can be lexically
     * flowed into from the current one.
     * @param flow true if the next instructon can be flowed into
     */
    private void setControlFlow(boolean flow) {
        this.flowFromPredecessor = flow;
    }

    /**
     * End a basic block.
     * @param inst The instruction delimiting the BB.
     * @param target defaultTarget The first target (if any) of the instruction delimiting the BB.
     * @param target extraTarget The extra targets (if any) of the instruction delimiting the BB.
     */
    private void endBasicBlock(BasicBlockExitDelimiter delim, Target defaultTarget, Target[] extraTargets) throws LinkageException {

        // Propogate the current stack state to all targets.
        if (defaultTarget != null) {
            verifyTarget(defaultTarget);
            verifyTargetIsNotExceptionHandlerEntry(defaultTarget);

            // Javac should never generate code such that there is something on the operand
            // stack at a backward branch. However, there is at least one TCK test that
            // contains such code (javasoft.sqe.tests.vm.classfmt.vrf.vrfmef202.vrfmef20201m1_1.vrfmef20201m1_1p)
            if (delim instanceof Goto) {
               boolean isBackwardBranch = doBackwardBranch((Goto)delim);
               //Assert.that(!isBackwardBranch || frame.stackSize(false) == 0, "Stack is not empty at backward branch");
            }

            frame.mergeWithTarget(defaultTarget);
        }
        if (extraTargets != null) {
            for (int i = 0; i != extraTargets.length; i++) {
                Target target = extraTargets[i];
                verifyTarget(target);
                verifyTargetIsNotExceptionHandlerEntry(target);
                frame.mergeWithTarget(target);
            }
        }

        setControlFlow(((Instruction)delim).fallsThrough());
        frame.spillStack(true, null);
    }

    /**
     * If a control flow target is also the entry point for an exception
     * handler, then this method is untranslatable. There is no legal way
     * to express such code in Squawk bytecodes: how does the exception
     * object get onto the stack when arriving at the exception handler
     * via this branch instruction? If it is determined that this code *must*
     * be translated (e.g. for TCK completeness), then code will will have
     * to be inserted between the branch instruction and the target that throws
     * a newly created instance of Throwable.
     * @param target
     */
    private void verifyTargetIsNotExceptionHandlerEntry(Target target) throws LinkageException {
        if (target.isExceptionTarget()) {
            frame.verifyTranslatable(false, "Cannot fall through or branch to an exception handler");
        }

    }

/*---------------------------------------------------------------------------*\
 *                     Verification methods                                  *
\*---------------------------------------------------------------------------*/

    /**
     * Check that a branch target is valid.
     */
    private void verifyBranchTarget(Target target) throws LinkageException {
        frame.matchStackMap(target, true, VEConst.TARGET_BAD_TYPE);
    }

    /**
     * Attempt to match the current derived types with a stack map
     * recorded for a non-fall-through successor of the current instruction.
     * @param target the address of the non-fall-through successor of the current instruction.
     */
    private Target verifyTarget(Target target) throws LinkageException {
        int callerIP = in.getLastIP();
        frame.matchStackMap(target, false, VEConst.TARGET_BAD_TYPE);
        //Check that there are no uninitialized objects in the current state.
        frame.verify(callerIP < target.getIP() || !frame.containsUninitializedObject(false), VEConst.BACK_BRANCH_UNINIT);
        return target;
    }

/*---------------------------------------------------------------------------*\
 *                   Exception and error methods                             *
\*---------------------------------------------------------------------------*/

    /**
     * Provide a context string for an error.
     */
    public String getContext() {
        Type clazz = method.parent();
        int ip = in.getLastIP();
        String result = clazz.toSignature(true, true) +
            "." + method.name() + " @ " + ip;
        int srcLine = getSourceLine(ip);
        String sourceFileName = clazz.getSourceFile();
        if (srcLine > 0 && sourceFileName != null) {
            result += " ("+sourceFileName+":"+srcLine+")";
        }
        return result;
    }

/*---------------------------------------------------------------------------*\
 *                   Miscellaneous methods                                   *
\*---------------------------------------------------------------------------*/

    /**
     * Check a branch instruction to see if it is a backward branch. If so, then
     * all instructions between the branch instruction and its destination (i.e.
     * all the instructions in the loop) have their loop nesting count incremented.
     * @return true if this is a backward branch.
     */
    private boolean doBackwardBranch(Goto gotoinst) throws LinkageException {
        Target target = gotoinst.target();
        target.isTargetFor(gotoinst);
       /*
        * If the target already has a target instuction then the target must be a backward branch.
        * In this case there is some kind of loop from the current location to the
        * target. This is the place where the usecount of local variables is multiplied
        * by ten.
        */
        int callerIP = in.getLastIP();
        Instruction inst = target.getInstruction();
        if (inst != null) {
            Assert.that(inst.getOriginalIP() <= callerIP);
            while (inst != null && inst.getOriginalIP() <= callerIP) {
                inst.incrementLoopDepth();
                inst = inst.getNext();
            }
            gotoinst.incrementLoopDepth();
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Get the source line number for a given ip address.
     */
    private int getSourceLine(int ip) {
        if (lineNumberTable == null) {
            // There is no line number table so just return the IP negated
            return -ip;
        }
        int start_pc;
        for (;;) {
            if (lineNumberTableIndex == lineNumberTable.length) {
                return lineNumberTable[lineNumberTableIndex-1];
            }
            start_pc = lineNumberTable[lineNumberTableIndex];
            if (ip <= start_pc) {
                break;
            }
            lineNumberTableIndex += 2;
        }
        if (ip == start_pc) {
            return lineNumberTable[lineNumberTableIndex+1];
        } else {
            return lineNumberTable[lineNumberTableIndex-1];
        }
    }

    /**
     * Pop the top instruction off the stack and verify that it's type is
     * assignable to a specified type and is not the unitialized object type.
     * @param expectedType
     * @return
     * @throws LinkageException
     */
    private Instruction popInitializedOrPrimitive(Type expectedType) throws LinkageException {
        Instruction inst = pop(expectedType);
        frame.verify(!(inst instanceof NewObject), "Illegal use of uninitialized instance");
        return inst;
    }

    /**
     * Pop the top instruction off the stack and verify that it's type is
     * assignable to a specified type.
     * @param expectedType
     * @return
     * @throws LinkageException
     */
    private Instruction pop(Type expectedType) throws LinkageException {
        Instruction inst = frame.pop(expectedType, in.getLastIP());
        Local.narrowLocalType(inst, expectedType, false);
        return inst;
    }

    /**
     * Insert an instruction to cast an 'aconst_null' operand to a specified
     * type. This is required for the operands of certain instructions that
     * must not be the result of 'aconst_null' according to the Suite File
     * Format. This will never occur in javac generated code but exists in
     * a number of TCK tests.
     * @param type
     * @throws LinkageException
     */
    private Instruction castLoadConstantNullToType(Instruction inst, Type type) {
        if (inst instanceof LoadConstant && ((LoadConstant)inst).isConstNull()) {
            LoadConstantNull ldnull = (LoadConstantNull)inst;
            ldnull.setCastType(type);
        }
        return inst;
    }


}