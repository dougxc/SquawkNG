/*
 * @(#)GraphBuilder.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javac1.Assert;
import javac1.BailOut;
import javac1.BasicType;
import javac1.Bytecodes;
import javac1.BytecodeStream;
import javac1.JavaC1;
import javac1.ci.ConstantPool;
import javac1.ci.ExceptionHandler;
import javac1.ci.Field;
import javac1.ci.InstanceKlass;
import javac1.ci.Klass;
import javac1.ci.Method;
import javac1.ci.Runtime1;
import javac1.ir.*;
import javac1.ir.instr.*;
import javac1.ir.types.*;

/**
 * Fills the gaps between the basic blocks. This is the second pass of building
 * the intermediate representation and is done immediately after determining the
 * starts of all basic blocks.
 *
 * @see      BlockListBuilder
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class GraphBuilder implements ValueClosure {
    /**
     * The canonicalizer used for simplifying values.
     */
    private Canonicalizer canonicalizer;

    /**
     * The stream to read bytecodes from.
     */
    private BytecodeStream stream;

    /**
     * The mapping of bytecode indices to basic blocks.
     */
    private Map bci2block;

    /**
     * The value map for common subexpression elimination.
     */
    private Map vmap;

    /**
     * The current basic block.
     */
    private BlockBegin block;

    /**
     * The current execution state for the scope.
     */
    private ValueStack state;

    /**
     * The current scope.
     */
    private IRScope scope;

    /**
     * The last instruction added.
     */
    private Instruction last;

    /**
     * Whether or not the method has at least one exception handler.
     */
    private boolean hasHandler;

    /**
     * Whether or not a jump to a subroutine has been found.
     */
    private boolean foundJsr;

    /**
     * Constructs a new graph builder and builds the control flow graph for the
     * bytecodes of the specified scope.
     *
     * @param  scope      the current scope
     * @param  bci2block  mapping of bytecode indices to basic blocks
     * @param  start      the basic block where execution starts
     */
    public GraphBuilder(IRScope scope, Map bci2block, BlockBegin start) {
        this.canonicalizer = new Canonicalizer(scope);
        this.scope = scope;
        this.stream = new BytecodeStream(getMethod().getCode());
        this.bci2block = bci2block;
        this.vmap = new HashMap(29);
        this.hasHandler = !getExceptionHandlers().isEmpty();
        this.foundJsr = false;
        buildTransitiveClosure(start);
    }
    
    /**
     * Returns whether or not a jump to a subroutine has been found.
     *
     * @return  whether or not jsr has been found
     */
    public boolean foundJsr() {
        return foundJsr;
    }

    /**
     * Returns the method that is currently being compiled.
     *
     * @return  the method being compiled
     */
    private Method getMethod() {
        return scope.getMethod();
    }

    /**
     * Returns the list of exception handlers in the current scope.
     *
     * @return  the exception handlers
     */
    private List getExceptionHandlers() {
        return scope.getExceptionHandlers();
    }

    /**
     * Returns the basic block that starts at the specified bytecode index.
     *
     * @param   bci  bytecode index
     * @return  the block at the specified index
     */
    private BlockBegin blockAt(int bci) {
        Integer key = new Integer(bci);
        return (BlockBegin) bci2block.get(key);
    }

    /**
     * Looks for an existing value that is equal to the specified one and can
     * be reused instead. This method is used to perform a simple common
     * subexpression elimination.
     *
     * @param   instr  value to look for
     * @return  reusable value or the specified one if no equal value exists
     * @see     Instruction#hashCode()
     */
    private Instruction find(Instruction instr) {
        if (!javac1.Flags.UseValueNumbering || (instr.hashCode() == 0)) {
            return instr;
        } else if (vmap.containsKey(instr)) {
            return (Instruction) vmap.get(instr);
        } else {
            vmap.put(instr, instr);
            return instr;
        }
    }

    /**
     * Recursively appends the specified instruction and all its operands that
     * have been generated in the context of canonicalization.
     *
     * @param   value  instruction to be appended
     * @return  the appended instruction or a value that can be reused instead
     * @see     Instruction#doInputValues(ValueClosure)
     */
    public Instruction doValue(Instruction value) {
        if (value.getBci() != -1) {
            return value;
        } else {
            if ((value.hashCode() == 0) || !vmap.containsKey(value)) {
                value.doInputValues(this);
            }
            Instruction i1 = find(value);
            if (i1 == value) {
                last = last.append(i1, stream.getBci());
            }
            return i1;
        }
    }

    /**
     * Appends the specified instruction to the end of the instruction list of
     * the current basic block.
     *
     * @param   instr  instruction to be appended
     * @return  the appended instruction or a value that can be reused instead
     */
    private Instruction append(Instruction instr) {
        Instruction i1 = canonicalizer.simplify(instr);
        if (i1.getBci() != -1) {
            return i1;
        } else if (i1 != instr) {
            i1.doInputValues(this);
        }
        Instruction i2 = find(i1);
        if (i2 == i1) {
            last = last.append(i2, stream.getBci());
            if ((i2 instanceof StateSplit) && !(i2 instanceof BlockEnd)) {
                vmap.clear();
                state.clearLocals();
                state.pinStackForStateSplit();
                ((StateSplit) i2).setState((ValueStack) state.clone());
            }
        }
        return i2;
    }

    /**
     * Invalidates all common subexpressions that are memory accesses. All
     * instructions loading a field or an array element with the specified type
     * are removed from the set of reusable values.
     *
     * @param  type  type of instructions to be removed
     */
    private void killMemory(ValueType type) {
        Iterator iterator = vmap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry mapping = (Map.Entry) iterator.next();
            Instruction x = (Instruction) mapping.getValue();
            if (((x instanceof LoadField) || (x instanceof LoadIndexed))
                    && (x.getType().getTag() == type.getTag())) {
                iterator.remove();
            }
        }
    }

    /**
     * Generates an instruction for loading a value from the constant pool.
     *
     * @param  index  index into the constant pool
     * @see    Constant
     */
    private void loadConstant(int index) {
        ValueType type = ValueType.illegalType;
        ConstantPool cpool = getMethod().getConstants();
        switch (getMethod().getConstants().getTagAt(index)) {
        case ConstantPool.JVM_CONSTANT_INTEGER:
            type = new IntConstant(cpool.getIntAt(index));
            break;
        case ConstantPool.JVM_CONSTANT_LONG:
            type = new LongConstant(cpool.getLongAt(index));
            break;
        case ConstantPool.JVM_CONSTANT_FLOAT:
            type = new FloatConstant(cpool.getFloatAt(index));
            break;
        case ConstantPool.JVM_CONSTANT_DOUBLE:
            type = new DoubleConstant(cpool.getDoubleAt(index));
            break;
        case ConstantPool.JVM_CONSTANT_STRING:
            /* falls through */
        case ConstantPool.JVM_CONSTANT_UNRESOLVED_STRING:
            type = new InstanceConstant(cpool.getStringAt(index));
            break;
        default:
            throw new BailOut("could not resolve a constant");
        }
        state.push(type, append(new Constant(scope, type)));
    }

    /**
     * Generates an instruction for loading a value from a local variable.
     *
     * @param  type   type of the local variable
     * @param  index  index into the local variable array of the current frame
     * @see    LoadLocal
     */
    private void loadLocal(ValueType type, int index) {
        Instruction x = null;
        if (javac1.Flags.EliminateLoads) {
            x = state.loadLocal(index);
        }
        if (x == null) {
            x = append(new LoadLocal(scope, type, index));
            state.storeLocal(index, x);
        } else if (javac1.Flags.PrintLoadElimination) {
            JavaC1.out.print("load local " + index);
            JavaC1.out.println(" eliminated at " + stream.getBci());
        }
        state.push(type, x);
    }

    /**
     * Generates an instruction for loading a value from an array.
     *
     * @param  type  the component's <strong>basic</strong> type
     * @see    LoadIndexed
     */
    private void loadIndexed(int type) {
        Instruction index = state.ipop();
        Instruction array = state.apop();
        LoadIndexed result = new LoadIndexed(scope, array, index, type);
        state.push(ValueType.valueOf(type), append(result));
    }

    /**
     * Generates an instruction for storing a value into a local variable.
     *
     * @param  type   type of the value
     * @param  index  index into the local variable array of the current frame
     * @see    StoreLocal
     */
    private void storeLocal(ValueType type, int index) {
        Instruction x = state.pop(type);
        if (javac1.Flags.RoundFloatsWithStore && type.isFloatKind()) {
            state.killLocal(index);
        } else {
            state.storeLocal(index, x);
        }
        append(new StoreLocal(scope, type, index, x));
        state.pinStackLocals(index);
        if (type.isDoubleWord()) {
            state.pinStackLocals(index + 1);
        }
    }

    /**
     * Generates an instruction for storing a value into an array.
     *
     * @param  type  the component's <strong>basic</strong> type
     * @see    StoreIndexed
     */
    private void storeIndexed(int type) {
        Instruction value = state.pop(ValueType.valueOf(type));
        Instruction index = state.ipop();
        Instruction array = state.apop();
        StoreIndexed result = new StoreIndexed(scope, array, index, type, value);
        if (value.getType() instanceof ObjectType) {
            vmap.clear();
            state.clearLocals();
            state.pinStackAll();
            result.setState((ValueStack) state.clone());
        } else {
            killMemory(value.getType());
            state.pinStackIndexed();
        }
        append(result);
    }

    /**
     * Modifies the operand stack.
     *
     * @param   code  stack operation code
     */
    private void stackOp(int code) {
        Instruction w1, w2, w3, w4;

        switch (code) {
        case Bytecodes._pop:
            state.rawPop();
            break;
        case Bytecodes._pop2:
            state.rawPop();
            state.rawPop();
            break;
        case Bytecodes._dup:
            w1 = state.rawPop();
            state.rawPush(w1);
            state.rawPush(w1);
            break;
        case Bytecodes._dup_x1:
            w1 = state.rawPop();
            w2 = state.rawPop();
            state.rawPush(w1);
            state.rawPush(w2);
            state.rawPush(w1);
            break;
        case Bytecodes._dup_x2:
            w1 = state.rawPop();
            w2 = state.rawPop();
            w3 = state.rawPop();
            state.rawPush(w1);
            state.rawPush(w3);
            state.rawPush(w2);
            state.rawPush(w1);
            break;
        case Bytecodes._dup2:
            w1 = state.rawPop();
            w2 = state.rawPop();
            state.rawPush(w2);
            state.rawPush(w1);
            state.rawPush(w2);
            state.rawPush(w1);
            break;
        case Bytecodes._dup2_x1:
            w1 = state.rawPop();
            w2 = state.rawPop();
            w3 = state.rawPop();
            state.rawPush(w2);
            state.rawPush(w1);
            state.rawPush(w3);
            state.rawPush(w2);
            state.rawPush(w1);
            break;
        case Bytecodes._dup2_x2:
            w1 = state.rawPop();
            w2 = state.rawPop();
            w3 = state.rawPop();
            w4 = state.rawPop();
            state.rawPush(w2);
            state.rawPush(w1);
            state.rawPush(w4);
            state.rawPush(w3);
            state.rawPush(w2);
            state.rawPush(w1);
            break;
        case Bytecodes._swap:
            w1 = state.rawPop();
            w2 = state.rawPop();
            state.rawPush(w1);
            state.rawPush(w2);
            break;
        default:
            Assert.shouldNotReachHere();
            break;
        }
    }

    /**
     * Generates an instruction for executing an arithmetic operation.
     *
     * @param  type  type of the operands
     * @param  code  arithmetic operation code
     * @see    ArithmeticOp
     */
    private void arithmeticOp(ValueType type, int code) {
        Instruction y = state.pop(type);
        Instruction x = state.pop(type);
        state.push(type, append(new ArithmeticOp(scope, code, x, y)));
    }

    /**
     * Generates an instruction for negating a value.
     *
     * @param  type  type of the operand
     * @see    NegateOp
     */
    private void negateOp(ValueType type) {
        state.push(type, append(new NegateOp(scope, state.pop(type))));
    }

    /**
     * Generates an instruction for shifting a value.
     *
     * @param  type  type of the operand
     * @param  code  shifting operation code
     * @see    ShiftOp
     */
    private void shiftOp(ValueType type, int code) {
        Instruction y = state.ipop();
        Instruction x = state.pop(type);
        state.push(type, append(new ShiftOp(scope, code, x, y)));
    }

    /**
     * Generates an instruction for executing a logical operation.
     *
     * @param  type  type of the operands
     * @param  code  logical operation code
     * @see    LogicOp
     */
    private void logicOp(ValueType type, int code) {
        Instruction y = state.pop(type);
        Instruction x = state.pop(type);
        state.push(type, append(new LogicOp(scope, code, x, y)));
    }

    /**
     * Generates an instruction for incrementing a local variable.
     *
     * @param  index  index into the local variable array of the current frame
     * @param  delta  signed constant to increment by
     */
    private void increment(int index, int delta) {
        loadLocal(ValueType.intType, index);
        state.ipush(append(new Constant(scope, new IntConstant(delta))));
        arithmeticOp(ValueType.intType, Bytecodes._iadd);
        storeLocal(ValueType.intType, index);
    }

    /**
     * Generates an instruction for converting the type of a value.
     *
     * @param  code  type conversion code
     * @param  from  basic source type
     * @param  to    basic target type
     * @see    Convert
     */
    private void convert(int code, int from, int to) {
        ValueType src = ValueType.valueOf(from);
        ValueType target = ValueType.valueOf(to);
        state.push(target, append(new Convert(scope, code, state.pop(src), target)));
    }

    /**
     * Generates an instruction for comparing two values.
     *
     * @param  type  type of the operands
     * @param  code  comparing operation code
     * @see    CompareOp
     */
    private void compareOp(ValueType type, int code) {
        Instruction y = state.pop(type);
        Instruction x = state.pop(type);
        state.ipush(append(new CompareOp(scope, code, x, y)));
    }

    /**
     * Generates an instruction for branching under a certain condition.
     *
     * @param  x     operand to compare
     * @param  cond  condition code
     * @param  y     operand to be compared with
     * @see    If
     */
    private void ifNode(Instruction x, int cond, Instruction y) {
        BlockBegin tsux = blockAt(stream.getDestination(1, false));
        BlockBegin fsux = blockAt(stream.getNextBci());
        append(new If(scope, x, cond, false, y, tsux, fsux));
    }

    /**
     * Generates an instruction for comparing a value with zero.
     *
     * @param  type  type of the operand
     * @param  cond  condition code
     */
    private void ifZero(ValueType type, int cond) {
        Instruction y = append(new Constant(scope, IntConstant.ZERO));
        Instruction x = state.ipop();
        ifNode(x, cond, y);
    }

    /**
     * Generates an instruction for comparing two values.
     *
     * @param  type  type of the operands
     * @param  cond  condition code
     */
    private void ifCompare(ValueType type, int cond) {
        Instruction y = state.pop(type);
        Instruction x = state.pop(type);
        ifNode(x, cond, y);
    }

    /**
     * Generates an instruction for branching depending on whether a reference
     * is <code>null</code> or not.
     *
     * @param  cond  condition code
     */
    private void ifNull(int cond) {
        Instruction y = append(new Constant(scope, ObjectConstant.NULL));
        Instruction x = state.apop();
        ifNode(x, cond, y);
    }

    /**
     * Generates an instruction for jumping to a subroutine within the same
     * method.
     *
     * @param   dest  target address
     * @see     Jsr
     */
    private void jsr(int dest) {
        foundJsr = true;
        BlockBegin subroutine = blockAt(dest);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(subroutine != null, "entry must be control flow begin");
            Assert.that(subroutine.isFlagSet(BlockBegin.SUBROUTINE_ENTRY_FLAG), "must be marked as subroutine entry");
        }
        if (!subroutine.isFlagSet(BlockBegin.WAS_VISITED_FLAG)) {
            ValueType address = new AddressConstant(stream.getNextBci());
            state.push(ValueType.addressType, new Constant(scope, address));
            if (!subroutine.tryJoin(state)) {
                throw new BailOut("jsr join failed");
            }
            state.rawPop();
            new GraphBuilder(scope, bci2block, subroutine);
        }
        List endlist = subroutine.getEndList();
        Ret ret = null;
        for (int i = endlist.size() - 1; i >= 0; i--) {
            BlockEnd end = (BlockEnd) endlist.get(i);
            if (end instanceof Ret) {
                if (ret == null) {
                    ret = (Ret) end;
                } else if (!ret.getState().equals(end.getState())) {
                    throw new BailOut("jsr ret states incompatible");
                }
            }
        }
        if (ret != null) {
            block.addSubroutine(subroutine);
            append(new Jsr(scope, subroutine));
            last = last.append(new JsrContinuation(scope), stream.getNextBci());
            state = (ValueStack) ret.getState().clone();
            if (!state.isStackEmpty()) {
                throw new BailOut("jsr returns with non-empty stack");
            }
        } else {
            ValueType address = new AddressConstant(stream.getNextBci());
            state.push(ValueType.addressType, append(new Constant(scope, address)));
            append(new Goto(scope, blockAt(dest)));
        }
    }

    /**
     * Generates an instruction for accessing a jump table by index.
     *
     * @see  TableSwitch
     */
    private void tableSwitch() {
        int bci = stream.getBci();
        int offset = ((bci + 4) & ~0x03) - bci;
        BlockBegin defaultSux = blockAt(stream.getDestination(offset, true));
        int lo = stream.getSigned(offset + 4, 4);
        int hi = stream.getSigned(offset + 8, 4);
        List sux = new ArrayList(hi - lo + 2);
        offset += 12;
        for (int i = hi - lo; i >= 0; i--) {
            sux.add(blockAt(stream.getDestination(offset, true)));
            offset += 4;
        }
        sux.add(defaultSux);
        append(new TableSwitch(scope, state.ipop(), sux, lo));
    }

    /**
     * Generates an instruction for accessing a jump table by key match.
     *
     * @see  LookupSwitch
     */
    private void lookupSwitch() {
        int bci = stream.getBci();
        int offset = ((bci + 4) & ~0x03) - bci;
        BlockBegin defaultSux = blockAt(stream.getDestination(offset, true));
        int npairs = stream.getSigned(offset + 4, 4);
        List keys = new ArrayList(npairs);
        List sux = new ArrayList(npairs + 1);
        offset += 8;
        for (int i = 0; i < npairs; i++) {
            keys.add(new Integer(stream.getSigned(offset, 4)));
            sux.add(blockAt(stream.getDestination(offset + 4, true)));
            offset += 8;
        }
        sux.add(defaultSux);
        append(new LookupSwitch(scope, state.ipop(), sux, keys));
    }

    /**
     * Generates an instruction for returning control from a method to its
     * invoker.
     *
     * @param  x  return value or <code>null</return> to pass no value back
     * @see    Return
     */
    private void methodReturn(Instruction x) {
        int index = getMethod().isSynchronized() ? state.unlock() : -1;
        append(new Return(scope, x, index));
    }

    /**
     * Generates an instruction for loading a value from a field.
     *
     * @param  index  index of the field in the constant pool
     * @param  code   bytecode used for accessing the field
     * @see    LoadField
     */
    private void loadField(int index, int code) {
        Field field = getMethod().getConstants().getFieldAt(index, code);
        InstanceKlass holder = field.getHolder();
        int fieldType = field.getFieldType();
        ValueType type = ValueType.valueOf(fieldType);
        boolean isStatic = (code == Bytecodes._getstatic);
        Instruction obj = isStatic ?
            append(new Constant(scope, new ClassConstant(holder))) : state.apop();
        state.push(type, append(new LoadField(scope, obj, field.getOffset(),
            fieldType, isStatic, field.isLoaded(), field.isInitialized())));
    }

    /**
     * Generates an instruction for storing a value into a field.
     *
     * @param  index  index of the field in the constant pool
     * @param  code   bytecode used for accessing the field
     * @see    StoreField
     */
    private void storeField(int index, int code) {
        Field field = getMethod().getConstants().getFieldAt(index, code);
        InstanceKlass holder = field.getHolder();
        int fieldType = field.getFieldType();
        ValueType type = ValueType.valueOf(fieldType);
        Instruction value = state.pop(type);
        boolean isStatic = (code == Bytecodes._putstatic);
        Instruction obj = isStatic ?
            append(new Constant(scope, new ClassConstant(holder))) : state.apop();
        append(new StoreField(scope, obj, field.getOffset(), fieldType, value,
            isStatic, field.isLoaded(), field.isInitialized()));
        killMemory(type);
        state.pinStackFields();
    }

    /**
     * Tries to inline the specified callee if it is an intrinsic mathematical
     * method. The specified method can be native or not.
     *
     * @param   callee  method to be inlined
     * @return  whether or not the method has been inlined successfully
     */
    private boolean tryInlineIntrinsics(Method callee) {
        if (!javac1.Flags.InlineIntrinsics || callee.isSynchronized()) {
            return false;
        }
        int id = callee.getIntrinsicId();
        switch (id) {
        case Intrinsic.DSIN:
            /* falls through */
        case Intrinsic.DCOS:
            /* falls through */
        case Intrinsic.DSQRT:
            /* falls through */
        case Intrinsic.ARRAYCOPY:
            break;
        default:
            return false;
        }
        ValueType resultType = ValueType.valueOf(callee.getReturnType());
        List args = state.popArguments(callee.getArgSize());
        Intrinsic result = new Intrinsic(scope, resultType, id, args);
        if (!callee.isStatic()) {
            append(new NullCheck(scope, (Instruction) args.get(0)));
        }
        append(result);
        if (resultType != ValueType.voidType) {
            state.push(resultType, result);
        }
        return true;
    }

    /**
     * Tries to inline the specified method. The method can only be inlined if
     * it has neither exception handlers nor local variables, if it is not
     * synchronized and if its code does not exceed a certain size. If the
     * method is not empty then it must not consist of more than one block and
     * must not be called inside an already inlined method.
     *
     * @param   callee  method to be inlined
     * @return  whether or not the method has been inlined successfully
     */
    private boolean tryInlineSimple(Method callee) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!callee.isNative(), "callee must not be native");
        }
        byte[] code = callee.getCode();
        int resType = callee.getReturnType();
        int argSize = callee.getArgSize();
        int argBase = state.getStackSize() - argSize;
        Instruction result = null;
        if (!javac1.Flags.InlineSimpleMethods || callee.hasExceptionHandlers()
                || (callee.getMaxLocals() != argSize) || callee.isSynchronized()
                || (code.length > javac1.Flags.MaxInlineSize)
                || (getMethod().isStrict() != callee.isStrict())) {
            return false;
        } else if ((code.length == 1) && (code[0] == Bytecodes._return)) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(resType == BasicType.VOID, "inconsistent result type");
            }
            if (stream.getBytecode() != Bytecodes._invokestatic) {
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(!callee.isStatic(), "callee must not be static");
                    Assert.that(argSize > 0, "must have at least receiver");
                }
                NullCheck nullCheck = new NullCheck(scope, state.stackAt(argBase));
                last = last.append(nullCheck, stream.getBci());
            }
        } else if (scope.getLevel() > 0) {
            return false;
        } else {
            IRScope calleeScope = new IRScope(scope.getParent(), scope, callee);
            FrontEnd.buildGraph(calleeScope, -1);
            Base base = (Base) calleeScope.getStart().getEnd();
            BlockBegin entry = base.getStdEntry();
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(entry.getNext() == entry.getEnd(), "should be an empty block");
                Assert.that(entry.getEnd() instanceof Goto, "should have a goto at the end");
            }
            BlockBegin start = entry.getEnd().defaultSux();
            if (!(start.getEnd() instanceof Return)) {
                return false;
            }
            Return stop = (Return) start.getEnd();
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(stop.getType().getTag() == ValueType.valueOf(resType).getTag(), "types must match");
                Assert.that(start.getState().getStackSize() == 0, "stack must be empty");
            }
            InstructionFilter ifilter = new InstructionFilter(state, argSize);
            ifilter.apply(start.getNext());
            if (!ifilter.isOkay()) {
                return false;
            }
            start.resolveSubstitution();
            if (stream.getBytecode() != Bytecodes._invokestatic) {
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(!callee.isStatic(), "callee must not be static");
                }
                LoadField getfield = null;
                Instruction receiver = state.stackAt(argBase);
                if ((start.getNext() instanceof LoadLocal)
                        && (start.getNext().getNext() instanceof LoadField)) {
                    getfield = (LoadField) start.getNext().getNext();
                }
                if ((getfield == null) || (getfield.getObj() != receiver)) {
                    NullCheck nullCheck = new NullCheck(scope, receiver);
                    last = last.append(nullCheck, stream.getBci());
                } else if (Assert.ASSERTS_ENABLED) {
                    Assert.that(!getfield.isStatic(), "must not be static");
                }
            }
            last.append(start.getNext(), stream.getBci());
            while (last.getNext() != stop) {
                last = last.getNext();
                last.setBci(stream.getBci());
            }
            last.setNext(null);
            if (callee.getReturnType() != BasicType.VOID) {
                result = stop.getResult();
            }
        }
        int i = argBase;
        while (i < state.getStackSize()) {
            Instruction instr = state.stackAt(i);
            instr.setPinned(true);
            i += instr.getType().getSize();
        }
        state.truncateStack(argBase);
        if (result != null) {
            result.setPinned(true);
            state.push(result.getType(), result);
        }
        if (javac1.Flags.PrintInlining) {
            JavaC1.out.print("inlined method at " + stream.getBci());
            JavaC1.out.println(" (" + code.length + " bytes)");
        }
        return true;
    }

    /**
     * Tries to inline the specified intrinsic or simple method.
     *
     * @param   callee  method to be inlined
     * @return  whether or not the method has been inlined successfully
     */
    private boolean tryInline(Method callee) {
        if (tryInlineIntrinsics(callee)) {
            return true;
        } else if (callee.isNative()) {
            return false;
        } else {
            return tryInlineSimple(callee);
        }
    }

    /**
     * Generates an instruction for invoking an interface method, class method
     * or instance method. If possible the method is being inlined.
     *
     * @param  code   instruction code
     * @param  index  index of the method
     * @see    Invoke
     */
    private void invoke(int code, int index) {
        InstanceKlass accessor = getMethod().getHolder();
        Method target = getMethod().getConstants().getMethodAt(accessor, index, code);
        if (target.isLoaded() && (target.isStatic() != (code == Bytecodes._invokestatic))) {
            throw new BailOut("invalid code for calling this method");
        }
        InstanceKlass holder = target.getHolder();
        boolean inlining = javac1.Flags.Inline
            && holder.isLoaded() && holder.isInitialized() && target.isLoaded()
            && ((code == Bytecodes._invokestatic) || (code == Bytecodes._invokespecial)
            || ((code == Bytecodes._invokevirtual) && target.isFinalMethod()))
            && tryInline(target);
        if (!inlining) {
            boolean isStatic = (code == Bytecodes._invokestatic);
            ValueType resultType = ValueType.valueOf(target.getReturnType());
            List args = state.popArguments(target.getArgSizeNoReceiver());
            Instruction recv = isStatic ? null : state.apop();
            Invoke result = new Invoke(scope, code, resultType, recv, args,
                target.isLoaded() && target.isFinalMethod(), true);
            append(result);
            if (resultType != ValueType.voidType) {
                state.push(resultType, result);
            }
        }
    }

    /**
     * Generates an instruction for creating a new object.
     *
     * @param  index  index of the class to instantiate
     * @see    NewInstance
     */
    private void newInstance(int index) {
        Klass klass = getMethod().getConstants().getKlassAt(index);
        state.apush(append(new NewInstance(scope, (InstanceKlass) klass)));
    }

    /**
     * Generates an instruction for creating a new array. The number of elements
     * in the array is popped off the operand stack.
     *
     * @param  type  basic type of the array to be created
     * @see    NewTypeArray
     */
    private void newTypeArray(int type) {
        state.apush(append(new NewTypeArray(scope, state.ipop(), type)));
    }

    /**
     * Generates an instruction for creating a new array of object references.
     * The length of the array is popped of the operand stack.
     *
     * @param  index  index of the component type
     * @see    NewObjectArray
     */
    private void newObjectArray(int index) {
        Klass klass = getMethod().getConstants().getKlassAt(index);
        state.apush(append(new NewObjectArray(scope, klass, state.ipop())));
    }

    /**
     * Generates an instruction for checking whether an object is of the given
     * type. The runtime constant pool item at the specified index must be a
     * symbolic reference to a class, array, or interface type.
     *
     * @param  index  index of the type to test towards
     * @see    CheckCast
     */
    private void checkCast(int index) {
        Klass klass = getMethod().getConstants().getKlassAt(index);
        state.apush(append(new CheckCast(scope, klass, state.apop())));
    }

    /**
     * Generates an instruction for determining if an object is of the given
     * type.
     *
     * @param  index  index of the type to test towards
     * @see    InstanceOf
     */
    private void instanceOf(int index) {
        Klass klass = getMethod().getConstants().getKlassAt(index);
        state.ipush(append(new InstanceOf(scope, klass, state.apop())));
    }

    /**
     * Generates an instruction for entering the monitor for an object.
     *
     * @param  x  the object that the monitor is associated with
     * @see    MonitorEnter
     */
    private void monitorEnter(Instruction x) {
        append(new MonitorEnter(scope, x, state.lock(scope, stream.getBci())));
    }

    /**
     * Generates an instruction for exiting the monitor for an object.
     *
     * @param  x  the object that the monitor is associated with
     * @see    MonitorExit
     */
    private void monitorExit(Instruction x) {
        if (state.getLocksSize() < 1) {
            throw new BailOut("monitor stack underflow");
        }
        append(new MonitorExit(scope, x, state.unlock()));
    }

    /**
     * Generates an instruction for creating a new multidimensional array.
     *
     * @param  index       index of the component type
     * @param  dimensions  number of dimensions
     * @see    NewMultiArray
     */
    private void newMultiArray(int index, int dimensions) {
        Klass klass = getMethod().getConstants().getKlassAt(index);
        List dims = new ArrayList(dimensions);
        for (int i = 0; i < dimensions; i++) {
            dims.add(0, state.ipop());
        }
        state.apush(append(new NewMultiArray(scope, klass, dims)));
    }

    /**
     * Joins the current basic block with all exception handlers that are
     * potentially invoked by the current bytecode.
     *
     * @param  isAsync  whether or not an asynchronous exception may be thrown
      */
    private void handleException(boolean isAsync) {
        List handlers = getExceptionHandlers();
        for (int i = 0; i < handlers.size(); i++) {
            ExceptionHandler h = (ExceptionHandler) handlers.get(i);
            if (h.covers(stream.getBci())) {
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(h.getEntry() == blockAt(h.getTarget()), "blocks must correspond");
                }
                if (h.getEntry().isFlagSet(BlockBegin.WAS_VISITED_FLAG)) {
                    ValueStack stack = h.getEntry().getState();
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(stack != null, "state must be set");
                    }
                    if (state.getLocksSize() != stack.getLocksSize()) {
                        if (isAsync) {
                            return;
                        } else {
                            throw new BailOut("illegal monitor state");
                        }
                    }
                } else {
                    ValueStack stack = (ValueStack) state.clone();
                    stack.clearStack();
                    stack.apush(new Constant(scope, ObjectConstant.NULL));
                    h.getEntry().join(stack);
                    new GraphBuilder(scope, bci2block, h.getEntry());
                }
                block.addExceptionHandler(h.getEntry());
                if (h.getTypeIndex() == 0) {
                    return;
                }
            }
        }
    }

    /**
     * Generates instructions of the intermediate representation and updates the
     * state according to the specified bytecode.
     *
     * @param  code  the bytecode to handle
     */
    private void handleBytecode(int code) {
        switch (code) {
        case Bytecodes._nop:
            /* nothing to do */
            break;
        case Bytecodes._aconst_null:
            state.apush(append(new Constant(scope, ObjectConstant.NULL)));
            break;
        case Bytecodes._iconst_m1:
            state.ipush(append(new Constant(scope, new IntConstant(-1))));
            break;
        case Bytecodes._iconst_0:
            state.ipush(append(new Constant(scope, IntConstant.ZERO)));
            break;
        case Bytecodes._iconst_1:
            state.ipush(append(new Constant(scope, IntConstant.ONE)));
            break;
        case Bytecodes._iconst_2:
            state.ipush(append(new Constant(scope, new IntConstant(2))));
            break;
        case Bytecodes._iconst_3:
            state.ipush(append(new Constant(scope, new IntConstant(3))));
            break;
        case Bytecodes._iconst_4:
            state.ipush(append(new Constant(scope, new IntConstant(4))));
            break;
        case Bytecodes._iconst_5:
            state.ipush(append(new Constant(scope, new IntConstant(5))));
            break;
        case Bytecodes._lconst_0:
            state.lpush(append(new Constant(scope, new LongConstant(0))));
            break;
        case Bytecodes._lconst_1:
            state.lpush(append(new Constant(scope, new LongConstant(1))));
            break;
        case Bytecodes._fconst_0:
            state.fpush(append(new Constant(scope, new FloatConstant(0))));
            break;
        case Bytecodes._fconst_1:
            state.fpush(append(new Constant(scope, new FloatConstant(1))));
            break;
        case Bytecodes._fconst_2:
            state.fpush(append(new Constant(scope, new FloatConstant(2))));
            break;
        case Bytecodes._dconst_0:
            state.dpush(append(new Constant(scope, new DoubleConstant(0))));
            break;
        case Bytecodes._dconst_1:
            state.dpush(append(new Constant(scope, new DoubleConstant(1))));
            break;
        case Bytecodes._bipush:
            state.ipush(append(new Constant(scope,
                new IntConstant(stream.getSigned(1, 1)))));
            break;
        case Bytecodes._sipush:
            state.ipush(append(new Constant(scope,
                new IntConstant(stream.getSigned(1, 2)))));
            break;
        case Bytecodes._ldc:
            loadConstant(stream.getUnsigned(1, 1));
            break;
        case Bytecodes._ldc_w:
            /* falls through */
        case Bytecodes._ldc2_w:
            loadConstant(stream.getUnsigned(1, 2));
            break;
        case Bytecodes._iload:
            loadLocal(ValueType.intType, stream.getUnsigned(1, 1, 2, 2));
            break;
        case Bytecodes._lload:
            loadLocal(ValueType.longType, stream.getUnsigned(1, 1, 2, 2));
            break;
        case Bytecodes._fload:
            loadLocal(ValueType.floatType, stream.getUnsigned(1, 1, 2, 2));
            break;
        case Bytecodes._dload:
            loadLocal(ValueType.doubleType, stream.getUnsigned(1, 1, 2, 2));
            break;
        case Bytecodes._aload:
            loadLocal(ValueType.instanceType, stream.getUnsigned(1, 1, 2, 2));
            break;
        case Bytecodes._iload_0:
            loadLocal(ValueType.intType, 0);
            break;
        case Bytecodes._iload_1:
            loadLocal(ValueType.intType, 1);
            break;
        case Bytecodes._iload_2:
            loadLocal(ValueType.intType, 2);
            break;
        case Bytecodes._iload_3:
            loadLocal(ValueType.intType, 3);
            break;
        case Bytecodes._lload_0:
            loadLocal(ValueType.longType, 0);
            break;
        case Bytecodes._lload_1:
            loadLocal(ValueType.longType, 1);
            break;
        case Bytecodes._lload_2:
            loadLocal(ValueType.longType, 2);
            break;
        case Bytecodes._lload_3:
            loadLocal(ValueType.longType, 3);
            break;
        case Bytecodes._fload_0:
            loadLocal(ValueType.floatType, 0);
            break;
        case Bytecodes._fload_1:
            loadLocal(ValueType.floatType, 1);
            break;
        case Bytecodes._fload_2:
            loadLocal(ValueType.floatType, 2);
            break;
        case Bytecodes._fload_3:
            loadLocal(ValueType.floatType, 3);
            break;
        case Bytecodes._dload_0:
            loadLocal(ValueType.doubleType, 0);
            break;
        case Bytecodes._dload_1:
            loadLocal(ValueType.doubleType, 1);
            break;
        case Bytecodes._dload_2:
            loadLocal(ValueType.doubleType, 2);
            break;
        case Bytecodes._dload_3:
            loadLocal(ValueType.doubleType, 3);
            break;
        case Bytecodes._aload_0:
            loadLocal(ValueType.objectType, 0);
            break;
        case Bytecodes._aload_1:
            loadLocal(ValueType.objectType, 1);
            break;
        case Bytecodes._aload_2:
            loadLocal(ValueType.objectType, 2);
            break;
        case Bytecodes._aload_3:
            loadLocal(ValueType.objectType, 3);
            break;
        case Bytecodes._iaload:
            loadIndexed(BasicType.INT);
            break;
        case Bytecodes._laload:
            loadIndexed(BasicType.LONG);
            break;
        case Bytecodes._faload:
            loadIndexed(BasicType.FLOAT);
            break;
        case Bytecodes._daload:
            loadIndexed(BasicType.DOUBLE);
            break;
        case Bytecodes._aaload:
            loadIndexed(BasicType.OBJECT);
            break;
        case Bytecodes._baload:
            loadIndexed(BasicType.BYTE);
            break;
        case Bytecodes._caload:
            loadIndexed(BasicType.CHAR);
            break;
        case Bytecodes._saload:
            loadIndexed(BasicType.SHORT);
            break;
        case Bytecodes._istore:
            storeLocal(ValueType.intType, stream.getUnsigned(1, 1, 2, 2));
            break;
        case Bytecodes._lstore:
            storeLocal(ValueType.longType, stream.getUnsigned(1, 1, 2, 2));
            break;
        case Bytecodes._fstore:
            storeLocal(ValueType.floatType, stream.getUnsigned(1, 1, 2, 2));
            break;
        case Bytecodes._dstore:
            storeLocal(ValueType.doubleType, stream.getUnsigned(1, 1, 2, 2));
            break;
        case Bytecodes._astore:
            storeLocal(ValueType.objectType, stream.getUnsigned(1, 1, 2, 2));
            break;
        case Bytecodes._istore_0:
            storeLocal(ValueType.intType, 0);
            break;
        case Bytecodes._istore_1:
            storeLocal(ValueType.intType, 1);
            break;
        case Bytecodes._istore_2:
            storeLocal(ValueType.intType, 2);
            break;
        case Bytecodes._istore_3:
            storeLocal(ValueType.intType, 3);
            break;
        case Bytecodes._lstore_0:
            storeLocal(ValueType.longType, 0);
            break;
        case Bytecodes._lstore_1:
            storeLocal(ValueType.longType, 1);
            break;
        case Bytecodes._lstore_2:
            storeLocal(ValueType.longType, 2);
            break;
        case Bytecodes._lstore_3:
            storeLocal(ValueType.longType, 3);
            break;
        case Bytecodes._fstore_0:
            storeLocal(ValueType.floatType, 0);
            break;
        case Bytecodes._fstore_1:
            storeLocal(ValueType.floatType, 1);
            break;
        case Bytecodes._fstore_2:
            storeLocal(ValueType.floatType, 2);
            break;
        case Bytecodes._fstore_3:
            storeLocal(ValueType.floatType, 3);
            break;
        case Bytecodes._dstore_0:
            storeLocal(ValueType.doubleType, 0);
            break;
        case Bytecodes._dstore_1:
            storeLocal(ValueType.doubleType, 1);
            break;
        case Bytecodes._dstore_2:
            storeLocal(ValueType.doubleType, 2);
            break;
        case Bytecodes._dstore_3:
            storeLocal(ValueType.doubleType, 3);
            break;
        case Bytecodes._astore_0:
            storeLocal(ValueType.objectType, 0);
            break;
        case Bytecodes._astore_1:
            storeLocal(ValueType.objectType, 1);
            break;
        case Bytecodes._astore_2:
            storeLocal(ValueType.objectType, 2);
            break;
        case Bytecodes._astore_3:
            storeLocal(ValueType.objectType, 3);
            break;
        case Bytecodes._iastore:
            storeIndexed(BasicType.INT);
            break;
        case Bytecodes._lastore:
            storeIndexed(BasicType.LONG);
            break;
        case Bytecodes._fastore:
            storeIndexed(BasicType.FLOAT);
            break;
        case Bytecodes._dastore:
            storeIndexed(BasicType.DOUBLE);
            break;
        case Bytecodes._aastore:
            storeIndexed(BasicType.OBJECT);
            break;
        case Bytecodes._bastore:
            storeIndexed(BasicType.BYTE);
            break;
        case Bytecodes._castore:
            storeIndexed(BasicType.CHAR);
            break;
        case Bytecodes._sastore:
            storeIndexed(BasicType.SHORT);
            break;
        case Bytecodes._pop:
            /* falls through */
        case Bytecodes._pop2:
            /* falls through */
        case Bytecodes._dup:
            /* falls through */
        case Bytecodes._dup_x1:
            /* falls through */
        case Bytecodes._dup_x2:
            /* falls through */
        case Bytecodes._dup2:
            /* falls through */
        case Bytecodes._dup2_x1:
            /* falls through */
        case Bytecodes._dup2_x2:
            /* falls through */
        case Bytecodes._swap:
            stackOp(code);
            break;
        case Bytecodes._iadd:
            arithmeticOp(ValueType.intType, code);
            break;
        case Bytecodes._ladd:
            arithmeticOp(ValueType.longType, code);
            break;
        case Bytecodes._fadd:
            arithmeticOp(ValueType.floatType, code);
            break;
        case Bytecodes._dadd:
            arithmeticOp(ValueType.doubleType, code);
            break;
        case Bytecodes._isub:
            arithmeticOp(ValueType.intType, code);
            break;
        case Bytecodes._lsub:
            arithmeticOp(ValueType.longType, code);
            break;
        case Bytecodes._fsub:
            arithmeticOp(ValueType.floatType, code);
            break;
        case Bytecodes._dsub:
            arithmeticOp(ValueType.doubleType, code);
            break;
        case Bytecodes._imul:
            arithmeticOp(ValueType.intType, code);
            break;
        case Bytecodes._lmul:
            arithmeticOp(ValueType.longType, code);
            break;
        case Bytecodes._fmul:
            arithmeticOp(ValueType.floatType, code);
            break;
        case Bytecodes._dmul:
            arithmeticOp(ValueType.doubleType, code);
            break;
        case Bytecodes._idiv:
            arithmeticOp(ValueType.intType, code);
            break;
        case Bytecodes._ldiv:
            arithmeticOp(ValueType.longType, code);
            break;
        case Bytecodes._fdiv:
            arithmeticOp(ValueType.floatType, code);
            break;
        case Bytecodes._ddiv:
            arithmeticOp(ValueType.doubleType, code);
            break;
        case Bytecodes._irem:
            arithmeticOp(ValueType.intType, code);
            break;
        case Bytecodes._lrem:
            arithmeticOp(ValueType.longType, code);
            break;
        case Bytecodes._frem:
            arithmeticOp(ValueType.floatType, code);
            break;
        case Bytecodes._drem:
            arithmeticOp(ValueType.doubleType, code);
            break;
        case Bytecodes._ineg:
            negateOp(ValueType.intType);
            break;
        case Bytecodes._lneg:
            negateOp(ValueType.longType);
            break;
        case Bytecodes._fneg:
            negateOp(ValueType.floatType);
            break;
        case Bytecodes._dneg:
            negateOp(ValueType.doubleType);
            break;
        case Bytecodes._ishl:
            shiftOp(ValueType.intType, code);
            break;
        case Bytecodes._lshl:
            shiftOp(ValueType.longType, code);
            break;
        case Bytecodes._ishr:
            shiftOp(ValueType.intType, code);
            break;
        case Bytecodes._lshr:
            shiftOp(ValueType.longType, code);
            break;
        case Bytecodes._iushr:
            shiftOp(ValueType.intType, code);
            break;
        case Bytecodes._lushr:
            shiftOp(ValueType.longType, code);
            break;
        case Bytecodes._iand:
            logicOp(ValueType.intType, code);
            break;
        case Bytecodes._land:
            logicOp(ValueType.longType, code);
            break;
        case Bytecodes._ior:
            logicOp(ValueType.intType, code);
            break;
        case Bytecodes._lor:
            logicOp(ValueType.longType, code);
            break;
        case Bytecodes._ixor:
            logicOp(ValueType.intType, code);
            break;
        case Bytecodes._lxor:
            logicOp(ValueType.longType, code);
            break;
        case Bytecodes._iinc:
            increment(stream.getUnsigned(1, 1, 2, 2), stream.getSigned(2, 1, 4, 2));
            break;
        case Bytecodes._i2l:
            convert(code, BasicType.INT, BasicType.LONG);
            break;
        case Bytecodes._i2f:
            convert(code, BasicType.INT, BasicType.FLOAT);
            break;
        case Bytecodes._i2d:
            convert(code, BasicType.INT, BasicType.DOUBLE);
            break;
        case Bytecodes._l2i:
            convert(code, BasicType.LONG, BasicType.INT);
            break;
        case Bytecodes._l2f:
            convert(code, BasicType.LONG, BasicType.FLOAT);
            break;
        case Bytecodes._l2d:
            convert(code, BasicType.LONG, BasicType.DOUBLE);
            break;
        case Bytecodes._f2i:
            convert(code, BasicType.FLOAT, BasicType.INT);
            break;
        case Bytecodes._f2l:
            convert(code, BasicType.FLOAT, BasicType.LONG);
            break;
        case Bytecodes._f2d:
            convert(code, BasicType.FLOAT, BasicType.DOUBLE);
            break;
        case Bytecodes._d2i:
            convert(code, BasicType.DOUBLE, BasicType.INT);
            break;
        case Bytecodes._d2l:
            convert(code, BasicType.DOUBLE, BasicType.LONG);
            break;
        case Bytecodes._d2f:
            convert(code, BasicType.DOUBLE, BasicType.FLOAT);
            break;
        case Bytecodes._i2b:
            convert(code, BasicType.INT, BasicType.BYTE);
            break;
        case Bytecodes._i2c:
            convert(code, BasicType.INT, BasicType.CHAR);
            break;
        case Bytecodes._i2s:
            convert(code, BasicType.INT, BasicType.SHORT);
            break;
        case Bytecodes._lcmp:
            compareOp(ValueType.longType, code);
            break;
        case Bytecodes._fcmpl:
            compareOp(ValueType.floatType, code);
            break;
        case Bytecodes._fcmpg:
            compareOp(ValueType.floatType, code);
            break;
        case Bytecodes._dcmpl:
            compareOp(ValueType.doubleType, code);
            break;
        case Bytecodes._dcmpg:
            compareOp(ValueType.doubleType, code);
            break;
        case Bytecodes._ifeq:
            ifZero(ValueType.intType, If.EQ);
            break;
        case Bytecodes._ifne:
            ifZero(ValueType.intType, If.NE);
            break;
        case Bytecodes._iflt:
            ifZero(ValueType.intType, If.LT);
            break;
        case Bytecodes._ifge:
            ifZero(ValueType.intType, If.GE);
            break;
        case Bytecodes._ifgt:
            ifZero(ValueType.intType, If.GT);
            break;
        case Bytecodes._ifle:
            ifZero(ValueType.intType, If.LE);
            break;
        case Bytecodes._if_icmpeq:
            ifCompare(ValueType.intType, If.EQ);
            break;
        case Bytecodes._if_icmpne:
            ifCompare(ValueType.intType, If.NE);
            break;
        case Bytecodes._if_icmplt:
            ifCompare(ValueType.intType, If.LT);
            break;
        case Bytecodes._if_icmpge:
            ifCompare(ValueType.intType, If.GE);
            break;
        case Bytecodes._if_icmpgt:
            ifCompare(ValueType.intType, If.GT);
            break;
        case Bytecodes._if_icmple:
            ifCompare(ValueType.intType, If.LE);
            break;
        case Bytecodes._if_acmpeq:
            ifCompare(ValueType.objectType, If.EQ);
            break;
        case Bytecodes._if_acmpne:
            ifCompare(ValueType.objectType, If.NE);
            break;
        case Bytecodes._goto:
            append(new Goto(scope, blockAt(stream.getDestination(1, false))));
            break;
        case Bytecodes._jsr:
            jsr(stream.getDestination(1, false));
            break;
        case Bytecodes._ret:
            append(new Ret(scope, stream.getUnsigned(1, 1, 2, 2)));
            break;
        case Bytecodes._tableswitch:
            tableSwitch();
            break;
        case Bytecodes._lookupswitch:
            lookupSwitch();
            break;
        case Bytecodes._ireturn:
            methodReturn(state.ipop());
            break;
        case Bytecodes._lreturn:
            methodReturn(state.lpop());
            break;
        case Bytecodes._freturn:
            methodReturn(state.fpop());
            break;
        case Bytecodes._dreturn:
            methodReturn(state.dpop());
            break;
        case Bytecodes._areturn:
            methodReturn(state.apop());
            break;
        case Bytecodes._return:
            methodReturn(null);
            break;
        case Bytecodes._getstatic:
            loadField(stream.getUnsigned(1, 2), code);
            break;
        case Bytecodes._putstatic:
            storeField(stream.getUnsigned(1, 2), code);
            break;
        case Bytecodes._getfield:
            loadField(stream.getUnsigned(1, 2), code);
            break;
        case Bytecodes._putfield:
            storeField(stream.getUnsigned(1, 2), code);
            break;
        case Bytecodes._invokevirtual:
            /* falls through */
        case Bytecodes._invokespecial:
            /* falls through */
        case Bytecodes._invokestatic:
            /* falls through */
        case Bytecodes._invokeinterface:
            invoke(code, stream.getUnsigned(1, 2));
            break;
        case Bytecodes._new:
            newInstance(stream.getUnsigned(1, 2));
            break;
        case Bytecodes._newarray:
            newTypeArray(stream.getSigned(1, 1));
            break;
        case Bytecodes._anewarray:
            newObjectArray(stream.getUnsigned(1, 2));
            break;
        case Bytecodes._arraylength:
            state.ipush(append(new ArrayLength(scope, state.apop())));
            break;
        case Bytecodes._athrow:
            append(new Throw(scope, state.apop()));
            break;
        case Bytecodes._checkcast:
            checkCast(stream.getUnsigned(1, 2));
            break;
        case Bytecodes._instanceof:
            instanceOf(stream.getUnsigned(1, 2));
            break;
        case Bytecodes._monitorenter:
            monitorEnter(state.apop());
            break;
        case Bytecodes._monitorexit:
            monitorExit(state.apop());
            break;
        case Bytecodes._multianewarray:
            newMultiArray(stream.getUnsigned(1, 2), stream.getSigned(3, 1));
            break;
        case Bytecodes._ifnull:
            ifNull(If.EQ);
            break;
        case Bytecodes._ifnonnull:
            ifNull(If.NE);
            break;
        case Bytecodes._goto_w:
            append(new Goto(scope, blockAt(stream.getDestination(1, true))));
            break;
        case Bytecodes._jsr_w:
            jsr(stream.getDestination(1, true));
            break;
        case Bytecodes._breakpoint:
            throw new BailOut("concurrent setting of breakpoint");
        default:
            Assert.shouldNotReachHere();
            break;
        }
    }

    /**
     * Fills the specified basic block with instructions. The method translates
     * the bytecodes of the basic block into instructions of the intermediate
     * representation and appends them to the end of the corresponding
     * instruction list.
     *
     * @param  begin  the basic block
     */
    private BlockEnd connectToEnd(BlockBegin begin) {
        vmap.clear();
        block = begin;
        state = (ValueStack) begin.getState().clone();
        last = begin;
        int pos = begin.getBci();
        stream.reset(pos);
        int prev = pos;
        boolean prevIsMonitorenter = false;
        while (!(last instanceof BlockEnd) && stream.next()
                && ((blockAt(pos) == null) || (blockAt(pos) == begin))) {
            int code = stream.getBytecode();
            if (hasHandler && (prevIsMonitorenter || Bytecodes.canTrap(code))) {
                handleException(prevIsMonitorenter || Bytecodes.isAsync(code));
            }
            handleBytecode(code);
            prevIsMonitorenter = (code == Bytecodes._monitorenter);
            prev = pos;
            pos = stream.getNextBci();
        }
        if (!(last instanceof BlockEnd)) {
            last = last.append(new Goto(scope, blockAt(pos)), prev);
        }
        BlockEnd end = (BlockEnd) last;
        if ((end instanceof Return) || (end instanceof Throw)) {
            state.clearStack();
        }
        state.pinStackAll();
        begin.setEnd(end);
        end.setState(state);
        for (int i = 0; i < end.countSux(); i++) {
            if (!end.suxAt(i).tryJoin(state)) {
                throw new BailOut("block join failed");
            }
        }
        return end;
    }

    /**
     * Generates instructions of the specified basic block and its successors
     * recursively.
     *
     * @param  begin  the basic block
     */
    private void buildTransitiveClosure(BlockBegin begin) {
        if (!begin.isFlagSet(BlockBegin.WAS_VISITED_FLAG)) {
            begin.setFlag(BlockBegin.WAS_VISITED_FLAG);
            BlockEnd end = connectToEnd(begin);
            for (int i = end.countSux() - 1; i >= 0; i--) {
                buildTransitiveClosure(end.suxAt(i));
            }
        }
    }
}
