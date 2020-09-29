/*
 * @(#)InstructionPrinter.java          1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend;

import java.util.List;
import javac1.Assert;
import javac1.BasicType;
import javac1.Bytecodes;
import javac1.JavaC1;
import javac1.ci.Klass;
import javac1.ir.InstructionVisitor;
import javac1.ir.ValueStack;
import javac1.ir.instr.*;
import javac1.ir.types.ArrayConstant;
import javac1.ir.types.ClassConstant;
import javac1.ir.types.DoubleConstant;
import javac1.ir.types.FloatConstant;
import javac1.ir.types.InstanceConstant;
import javac1.ir.types.IntConstant;
import javac1.ir.types.LongConstant;
import javac1.ir.types.ObjectConstant;
import javac1.ir.types.ValueType;

/**
 * Prints the instructions of the intermediate representation.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class InstructionPrinter implements InstructionVisitor {
    /**
     * Returns a string representation of the specified basic type.
     *
     * @param   basic type
     * @return  name of the basic type
     */
    private static String basicTypeName(int type) {
        switch (type) {
        case BasicType.BOOLEAN:
            return "boolean";
        case BasicType.BYTE:
            return "byte";
        case BasicType.CHAR:
            return "char";
        case BasicType.SHORT:
            return "short";
        case BasicType.INT:
            return "int";
        case BasicType.LONG:
            return "long";
        case BasicType.FLOAT:
            return "float";
        case BasicType.DOUBLE:
            return "double";
        case BasicType.ARRAY:
            return "array";
        case BasicType.OBJECT:
            return "object";
        default:
            return "???";
        }
    }

    /**
     * Returns a string representation of the specified condition code.
     *
     * @param   cond  condition code
     * @return  name of the condition
     */
    private static String condName(int cond) {
        switch (cond) {
        case If.EQ:
            return "==";
        case If.NE:
            return "!=";
        case If.LT:
            return "<";
        case If.LE:
            return "<=";
        case If.GT:
            return ">";
        case If.GE:
            return ">=";
        default:
            Assert.shouldNotReachHere();
            return null;
        }
    }

    /**
     * Returns the hexadecimal representation of the hash code of the object.
     *
     * @param   obj  object
     * @return  hexadecimal representation
     */
    private static String hexString(Object obj) {
        return "0x" + Integer.toHexString(System.identityHashCode(obj));
    }

    /**
     * Returns a string representation of the specified operation code.
     *
     * @param   op  operation code
     * @return  name of the operation
     */
    private static String opName(int op) {
        switch (op) {
        case Bytecodes._iadd:
            /* falls through */
        case Bytecodes._ladd:
            /* falls through */
        case Bytecodes._fadd:
            /* falls through */
        case Bytecodes._dadd:
            return "+";
        case Bytecodes._isub:
            /* falls through */
        case Bytecodes._lsub:
            /* falls through */
        case Bytecodes._fsub:
            /* falls through */
        case Bytecodes._dsub:
            return "-";
        case Bytecodes._imul:
            /* falls through */
        case Bytecodes._lmul:
            /* falls through */
        case Bytecodes._fmul:
            /* falls through */
        case Bytecodes._dmul:
            return "*";
        case Bytecodes._idiv:
            /* falls through */
        case Bytecodes._ldiv:
            /* falls through */
        case Bytecodes._fdiv:
            /* falls through */
        case Bytecodes._ddiv:
            return "/";
        case Bytecodes._irem:
            /* falls through */
        case Bytecodes._lrem:
            /* falls through */
        case Bytecodes._frem:
            /* falls through */
        case Bytecodes._drem:
            return "%";
        case Bytecodes._ishl:
            /* falls through */
        case Bytecodes._lshl:
            return "<<";
        case Bytecodes._ishr:
            /* falls through */
        case Bytecodes._lshr:
            return ">>";
        case Bytecodes._iushr:
            /* falls through */
        case Bytecodes._lushr:
            return ">>>";
        case Bytecodes._iand:
            /* falls through */
        case Bytecodes._land:
            return "&";
        case Bytecodes._ior:
            /* falls through */
        case Bytecodes._lor:
            return "|";
        case Bytecodes._ixor:
            /* falls through */
        case Bytecodes._lxor:
            return "^";
        default:
            return Bytecodes.getName(op);
        }
    }

    /**
     * Prints the name of the specified class.
     *
     * @param  klass  the class
     */
    private void printClass(Klass klass) {
        JavaC1.out.print("<" + klass + ">");
    }

    /**
     * Prints information about the specified field.
     *
     * @param  field  instruction accessing a field
     */
    private void printField(AccessField field) {
        printValue(field.getObj());
        JavaC1.out.print("._" + field.getOffset());
    }

    /**
     * Prints information about an array's element.
     *
     * @param  indexed  instruction accessing an array
     */
    private void printIndexed(AccessIndexed indexed) {
        printValue(indexed.getArray());
        JavaC1.out.print("[");
        printValue(indexed.getIndex());
        JavaC1.out.print("]");
    }

    /**
     * Prints the index of the specified local variable.
     *
     * @param  local  instruction accessing a local variable
     */
    private void printLocal(AccessLocal local) {
        JavaC1.out.print("L" + local.getIndex());
    }

    /**
     * Prints the identification number of the specified loop.
     *
     * @param  loop  instruction accessing a loop
     */
    private void printLoop(AccessLoop loop) {
        JavaC1.out.print("loop[" + loop.getLoopId() + "]");
    }

    /**
     * Prints the number of a monitor and the object that it is associated with.
     *
     * @param  monitor  instruction accessing a monitor
     */
    private void printMonitor(AccessMonitor monitor) {
        JavaC1.out.print("monitor[" + monitor.getMonitorNo() + "](");
        printValue(monitor.getObj());
        JavaC1.out.print(")");
    }

    /**
     * Prints a string representation of the specified constant object.
     *
     * @param  obj  constant object
     */
    private void printObject(Constant obj) {
        Object value;
        ValueType type = obj.getType();
        if (type instanceof ObjectConstant) {
            value = ((ObjectConstant) type).getValue();
            if (value == null) {
                JavaC1.out.print("null");
            } else {
                JavaC1.out.print("<object " + hexString(value) + ">");
            }
        } else if (type instanceof InstanceConstant) {
            value = ((InstanceConstant) type).getValue();
            JavaC1.out.print("<instance " + hexString(value) + ">");
        } else if (type instanceof ArrayConstant) {
            value = ((ArrayConstant) type).getValue();
            JavaC1.out.print("<array " + hexString(value) + ">");
        } else if (type instanceof ClassConstant) {
            value = ((ClassConstant) type).getValue();
            JavaC1.out.print("<class " + hexString(value) + ">");
        } else {
            JavaC1.out.print("???");
        }
    }

    /**
     * Prints the operands and the operator of a binary operation.
     *
     * @param  instr  binary operation
     */
    private void printOp2(Op2 instr) {
        printValue(instr.getX());
        JavaC1.out.print(" " + opName(instr.getOp()) + " ");
        printValue(instr.getY());
    }

    /**
     * Prints type and identification number of the specified value.
     *
     * @param  value  a value
     */
    private void printTemp(Instruction value) {
        JavaC1.out.print(value.getType().getTypeChar());
        JavaC1.out.print(value.getId());
    }

    /**
     * Prints the specified value.
     *
     * @param  value  value to be printed
     */
    private void printValue(Instruction value) {
        if (value == null) {
            JavaC1.out.print("null");
        } else {
            printTemp(value);
        }
    }

    public void doArithmeticOp(ArithmeticOp x) {
        printOp2(x);
    }

    public void doArrayLength(ArrayLength x) {
        printValue(x.getArray());
        JavaC1.out.print(".length");
    }

    public void doBase(Base x) {
        JavaC1.out.print("std entry B" + x.getStdEntry().getBlockId());
        if (x.countSux() > 1) {
            JavaC1.out.print(" osr entry B" + x.getOsrEntry().getBlockId());
        }
    }

    public void doBlockBegin(BlockBegin x) {
        BlockEnd end = x.getEnd();
        JavaC1.out.print("B" + x.getBlockId());
        JavaC1.out.print(" [" + x.getBci() + ", ");
        JavaC1.out.print((end == null) ? -1 : end.getBci());
        JavaC1.out.print("]");
        if ((end != null) && (end.countSux() > 0)) {
            JavaC1.out.print(" ->");
            for (int i = 0; i < end.countSux(); i++) {
                JavaC1.out.print(" B" + end.suxAt(i).getBlockId());
            }
        }
        if (x.countSubroutines() > 0) {
            JavaC1.out.print(" (");
            for (int i = 0; i < x.countSubroutines(); i++) {
                if (i > 0) {
                    JavaC1.out.print(" ");
                }
                JavaC1.out.print("B" + x.subroutineAt(i).getBlockId());
            }
            JavaC1.out.print(")");
        }
    }

    public void doCheckCast(CheckCast x) {
        JavaC1.out.print("checkcast(");
        printValue(x.getObj());
        JavaC1.out.print(") ");
        printClass(x.getKlass());
    }

    public void doCompareOp(CompareOp x) {
        printOp2(x);
    }

    public void doConstant(Constant x) {
        ValueType type = x.getType();
        switch (type.getTag()) {
        case ValueType.intTag:
            JavaC1.out.print(((IntConstant) type).getValue());
            break;
        case ValueType.longTag:
            JavaC1.out.print(((LongConstant) type).getValue() + "L");
            break;
        case ValueType.floatTag:
            JavaC1.out.print(((FloatConstant) type).getValue());
            break;
        case ValueType.doubleTag:
            JavaC1.out.print(((DoubleConstant) type).getValue() + "D");
            break;
        case ValueType.objectTag:
            printObject(x);
            break;
        default:
            JavaC1.out.print("???");
            break;
        }
    }

    public void doConvert(Convert x) {
        JavaC1.out.print(Bytecodes.getName(x.getOp()) + "(");
        printValue(x.getValue());
        JavaC1.out.print(")");
    }

    public void doGoto(Goto x) {
        JavaC1.out.print("goto B" + x.defaultSux().getBlockId());
    }

    public void doIf(If x) {
        JavaC1.out.print("if ");
        printValue(x.getX());
        JavaC1.out.print(" " + condName(x.getCond()) + " ");
        printValue(x.getY());
        JavaC1.out.print(" then B" + x.suxAt(0).getBlockId());
        JavaC1.out.print(" else B" + x.suxAt(1).getBlockId());
    }

    public void doIfOp(IfOp x) {
        printValue(x.getX());
        JavaC1.out.print(" " + condName(x.getCond()) + " ");
        printValue(x.getY());
        JavaC1.out.print(" ? ");
        printValue(x.getTrueVal());
        JavaC1.out.print(" : ");
        printValue(x.getFalseVal());
    }

    public void doInstanceOf(InstanceOf x) {
        JavaC1.out.print("instanceof(");
        printValue(x.getObj());
        JavaC1.out.print(") ");
        printClass(x.getKlass());
    }

    public void doIntrinsic(Intrinsic x) {
        switch (x.getIntrinsicId()) {
        case Intrinsic.DSIN:
            JavaC1.out.print("dsin(");
            break;
        case Intrinsic.DCOS:
            JavaC1.out.print("dcos(");
            break;
        case Intrinsic.DSQRT:
            JavaC1.out.print("dsqrt(");
            break;
        case Intrinsic.ARRAYCOPY:
            JavaC1.out.print("arraycopy(");
            break;
        default:
            JavaC1.out.print("<unknown intrinsic>(");
            break;
        }
        for (int i = 0; i < x.countArguments(); i++) {
            if (i > 0) {
                JavaC1.out.print(", ");
            }
            printValue(x.argumentAt(i));
        }
        JavaC1.out.print(")");
    }

    public void doInvoke(Invoke x) {
        if (x.hasReceiver()) {
            printValue(x.getReceiver());
            JavaC1.out.print(".");
        }
        JavaC1.out.print(Bytecodes.getName(x.getCode()) + "(");
        for (int i = 0; i < x.countArguments(); i++) {
            if (i > 0) {
                JavaC1.out.print(", ");
            }
            printValue(x.argumentAt(i));
        }
        JavaC1.out.print(")");
    }

    public void doJsr(Jsr x) {
        JavaC1.out.print("jsr B" + x.getSubroutine().getBlockId());
    }

    public void doJsrContinuation(JsrContinuation x) {
        JavaC1.out.print("jsr continuation (bci = ");
        JavaC1.out.print(x.getBci());
        JavaC1.out.print(")");
    }

    public void doLoadField(LoadField x) {
        printField(x);
    }

    public void doLoadIndexed(LoadIndexed x) {
        printIndexed(x);
    }

    public void doLoadLocal(LoadLocal x) {
        printLocal(x);
    }

    public void doLogicOp(LogicOp x) {
        printOp2(x);
    }

    public void doLookupSwitch(LookupSwitch x) {
        JavaC1.out.print("lookupswitch ");
        printValue(x.getTag());
        JavaC1.out.println();
        int len = x.getLength();
        for (int i = 0; i < len; i++) {
            String str = "    " + x.keyAt(i);
            JavaC1.out.print("                   ");
            JavaC1.out.print("case " + str.substring(str.length() - 5));
            JavaC1.out.println(": B" + x.suxAt(i).getBlockId());
        }
        JavaC1.out.print("                   ");
        JavaC1.out.print("default   : B" + x.defaultSux().getBlockId());
    }

    public void doLoopEnter(LoopEnter x) {
        JavaC1.out.print("enter ");
        printLoop(x);
    }

    public void doLoopExit(LoopExit x) {
        JavaC1.out.print("exit ");
        printLoop(x);
    }

    public void doMonitorEnter(MonitorEnter x) {
        JavaC1.out.print("enter ");
        printMonitor(x);
    }

    public void doMonitorExit(MonitorExit x) {
        JavaC1.out.print("exit ");
        printMonitor(x);
    }

    public void doNegateOp(NegateOp x) {
        JavaC1.out.print("-");
        printValue(x.getX());
    }

    public void doNewInstance(NewInstance x) {
        JavaC1.out.print("new instance ");
        printClass(x.getKlass());
    }

    public void doNewMultiArray(NewMultiArray x) {
        JavaC1.out.print("new multi array [");
        List dims = x.getDims();
        for (int i = 0; i < dims.size(); i++) {
            if (i > 0) {
                JavaC1.out.print(", ");
            }
            printValue((Instruction) dims.get(i));
        }
        JavaC1.out.print("] ");
        printClass(x.getKlass());
    }

    public void doNewObjectArray(NewObjectArray x) {
        JavaC1.out.print("new object array [");
        printValue(x.getLength());
        JavaC1.out.print("] ");
        printClass(x.getKlass());
    }

    public void doNewTypeArray(NewTypeArray x) {
        JavaC1.out.print("new " + basicTypeName(x.getElemType()));
        JavaC1.out.print(" array [");
        printValue(x.getLength());
        JavaC1.out.print("]");
    }

    public void doNullCheck(NullCheck x) {
        JavaC1.out.print("null_check(");
        printValue(x.getObj());
        JavaC1.out.print(")");
    }

    public void doPhi(Phi x) {
        JavaC1.out.print("phi[" + x.getIndex());
        JavaC1.out.print(" of " + x.getStackSize() + "]");
    }

    public void doRet(Ret x) {
        JavaC1.out.print("ret L" + x.getIndex());
    }

    public void doReturn(Return x) {
        if (x.getResult() == null) {
            JavaC1.out.print("return");
        } else {
            JavaC1.out.print(x.getType().getTypeChar());
            JavaC1.out.print("return ");
            printValue(x.getResult());
        }
    }

    public void doShiftOp(ShiftOp x) {
        printOp2(x);
    }

    public void doStoreField(StoreField x) {
        printField(x);
        JavaC1.out.print(" := ");
        printValue(x.getValue());
    }

    public void doStoreIndexed(StoreIndexed x) {
        printIndexed(x);
        JavaC1.out.print(" := ");
        printValue(x.getValue());
    }

    public void doStoreLocal(StoreLocal x) {
        printLocal(x);
        JavaC1.out.print(" := ");
        printValue(x.getValue());
    }

    public void doTableSwitch(TableSwitch x) {
        JavaC1.out.print("tableswitch ");
        printValue(x.getTag());
        JavaC1.out.println();
        int len = x.getLength();
        for (int i = 0; i < len; i++) {
            String str = "    " + (x.getLoKey() + i);
            JavaC1.out.print("                   ");
            JavaC1.out.print("case " + str.substring(str.length() - 5));
            JavaC1.out.println(": B" + x.suxAt(i).getBlockId());
        }
        JavaC1.out.print("                   ");
        JavaC1.out.print("default   : B" + x.defaultSux().getBlockId());
    }

    public void doThrow(Throw x) {
        JavaC1.out.print("throw ");
        printValue(x.getException());
    }

    /**
     * Prints the column headings for a basic block.
     */
    public void printHead() {
        JavaC1.out.print("__bci__use__tid____");
        JavaC1.out.println("instr____________________________________");
    }

    /**
     * Prints the values on the specified stack.
     *
     * @param  stack  the value stack
     */
    public void printStack(ValueStack stack) {
        if (stack.isStackEmpty()) {
            JavaC1.out.print("empty stack");
        } else {
            JavaC1.out.print("stack [");
            int i = 0;
            while (i < stack.getStackSize()) {
                if (i > 0) {
                    JavaC1.out.print(", ");
                }
                Instruction value = stack.stackAt(i);
                JavaC1.out.print(i + ":");
                printValue(value);
                i += value.getType().getSize();
            }
            JavaC1.out.print("]");
        }
    }

    /**
     * Prints a line with information about the specified instruction.
     *
     * @param  instr  instruction to be printed
     */
    public void printLine(Instruction instr) {
        StringBuffer buf = new StringBuffer("              ");
        buf.insert(0, instr.isPinned() ? "." : " ");
        buf.insert(2, instr.getBci());
        buf.insert(7, instr.getUseCount());
        buf.insert(12, instr.getType().getTypeChar());
        buf.insert(13, instr.getId());
        JavaC1.out.print(buf.substring(0, 19));
        printInstr(instr);
        JavaC1.out.println();
        if (instr instanceof StateSplit) {
            ValueStack stack = ((StateSplit) instr).getState();
            if ((stack != null) && !stack.isStackEmpty()) {
                JavaC1.out.print("                   ");
                printStack(stack);
                JavaC1.out.println();
            }
        }
    }

    /**
     * Prints the specified instruction.
     *
     * @param  instr  instruction to be printed
     */
    public void printInstr(Instruction instr) {
        instr.visit(this);
    }
}
