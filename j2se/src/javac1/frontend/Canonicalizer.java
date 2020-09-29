/*
 * @(#)Canonicalizer.java               1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend;

import javac1.Assert;
import javac1.Bytecodes;
import javac1.JavaC1;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.instr.*;
import javac1.ir.types.DoubleConstant;
import javac1.ir.types.FloatConstant;
import javac1.ir.types.IntConstant;
import javac1.ir.types.LongConstant;
import javac1.ir.types.ValueType;

/**
 * Transforms a generated instruction into its canonical form. This
 * transformation covers constant folding, as well as simplifying shift
 * operations, conditional branches and switch instructions.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class Canonicalizer implements InstructionVisitor {
    /**
     * The instruction printer used for printing graph node canonicalization.
     */
    private static final InstructionPrinter ip = new InstructionPrinter();
    
    /**
     * The current scope of the intermediate representation.
     */
    private IRScope scope;

    /**
     * The canonical form of the current instruction.
     */
    private Instruction canonical;
    
    /**
     * Constructs a new canonicalizer used for simplifying values.
     *
     * @param  scope  the current scope
     */
    public Canonicalizer(IRScope scope) {
        this.scope = scope;
    }

    /**
     * Returns whether or not the specified value is a power of two.
     *
     * @param   x  integer value
     * @return  whether or not the value is a power of two
     */
    private static boolean isPowerOf2(int x) {
        return (x > 0) && ((x & (x - 1)) == 0);
    }

    /**
     * Returns the logarithm of the specified value to the base 2.
     *
     * @param   x  integer value
     * @return  the logarithm
     */
    private static int log2(int x) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(x > 0, "value must be greater than zero");
        }
        int i = -1;
        for (int p = 1; p <= x; p *= 2) {
            i++;
        }
        return i;
    }

    /**
     * Moves the constant operand to the right. If the specified operation is
     * commutative and the left operand is a constant but the right one is not,
     * then the operands will be swapped.
     *
     * @param  x  the binary operation
     */
    private void moveConstToRight(Op2 x) {
        if (x.getX().getType().isConstant() && !x.getY().getType().isConstant()
                && x.isCommutative()) {
            x.swapOperands();
        }
    }

    /**
     * Simplifies the specified arithmetic operation and performs constant
     * folding.
     *
     * @param   x  the arithmetic operation
     * @return  the possibly simplified instruction
     */
    private Instruction simplifyArithmeticOp(ArithmeticOp x) {
        moveConstToRight(x);
        ValueType lt = x.getX().getType();
        ValueType rt = x.getY().getType();
        boolean ltconst = lt.isConstant();
        boolean rtconst = rt.isConstant();
        if (lt.getTag() == ValueType.intTag) {
            int a = ltconst ? ((IntConstant) lt).getValue() : 0;
            int b = rtconst ? ((IntConstant) rt).getValue() : 0;
            switch (x.getOp()) {
            case Bytecodes._iadd:
                if (ltconst && rtconst) {
                    return new Constant(scope, new IntConstant(a + b));
                } else if (rtconst && (b == 0)) {
                    return x.getX();
                }
                break;
            case Bytecodes._isub:
                if (ltconst && rtconst) {
                    return new Constant(scope, new IntConstant(a - b));
                } else if (ltconst && (a == 0)) {
                    return new NegateOp(scope, x.getY());
                } else if (rtconst && (b == 0)) {
                    return x.getX();
                }
                break;
            case Bytecodes._imul:
                if (ltconst && rtconst) {
                    return new Constant(scope, new IntConstant(a * b));
                } else if (rtconst && (b == 0)) {
                    return new Constant(scope, IntConstant.ZERO);
                } else if (rtconst && (b == 1)) {
                    return x.getX();
                } else if (rtconst && isPowerOf2(b)) {
                    Constant y = new Constant(scope, new IntConstant(log2(b)));
                    return new ShiftOp(scope, Bytecodes._ishl, x.getX(), y);
                } else if (rtconst && isPowerOf2(b - 1)) {
                    Constant y = new Constant(scope, new IntConstant(log2(b - 1)));
                    ShiftOp z = new ShiftOp(scope, Bytecodes._ishl, x.getX(), y);
                    return new ArithmeticOp(scope, Bytecodes._iadd, z, x.getX());
                } else if (rtconst && isPowerOf2(b + 1)) {
                    Constant y = new Constant(scope, new IntConstant(log2(b + 1)));
                    ShiftOp z = new ShiftOp(scope, Bytecodes._ishl, x.getX(), y);
                    return new ArithmeticOp(scope, Bytecodes._isub, z, x.getX());
                }
                break;
            case Bytecodes._idiv:
                if (ltconst && rtconst && (b != 0)) {
                    return new Constant(scope, new IntConstant(a / b));
                } else if (rtconst && (b == 1)) {
                    return x.getX();
                }
                break;
            case Bytecodes._irem:
                if (ltconst && rtconst && (b != 0)) {
                    return new Constant(scope, new IntConstant(a % b));
                } else if (rtconst && (b == 1)) {
                    return new Constant(scope, IntConstant.ZERO);
                } else if (rtconst && isPowerOf2(b)) {
                    Constant y = new Constant(scope, new IntConstant(b - 1));
                    return new LogicOp(scope, Bytecodes._iand, x.getX(), y);
                }
                break;
            }
        } else if (lt.getTag() == ValueType.longTag) {
            long a = ltconst ? ((LongConstant) lt).getValue() : 0;
            long b = rtconst ? ((LongConstant) rt).getValue() : 0;
            switch (x.getOp()) {
            case Bytecodes._ladd:
                if (ltconst && rtconst) {
                    return new Constant(scope, new LongConstant(a + b));
                } else if (rtconst && (b == 0)) {
                    return x.getX();
                }
                break;
            case Bytecodes._lsub:
                if (ltconst && rtconst) {
                    return new Constant(scope, new LongConstant(a - b));
                } else if (ltconst && (a == 0)) {
                    return new NegateOp(scope, x.getY());
                } else if (rtconst && (b == 0)) {
                    return x.getX();
                }
                break;
            case Bytecodes._lmul:
                if (ltconst && rtconst) {
                    return new Constant(scope, new LongConstant(a * b));
                } else if (rtconst && (b == 0)) {
                    return new Constant(scope, new LongConstant(0));
                } else if (rtconst && (b == 1)) {
                    return x.getX();
                }
                break;
            case Bytecodes._ldiv:
                if (ltconst && rtconst && (b != 0)) {
                    return new Constant(scope, new LongConstant(a / b));
                } else if (rtconst && (b == 1)) {
                    return x.getX();
                }
                break;
            case Bytecodes._lrem:
                if (ltconst && rtconst && (b != 0)) {
                    return new Constant(scope, new LongConstant(a % b));
                } else if (rtconst && (b == 1)) {
                    return new Constant(scope, new LongConstant(0));
                }
                break;
            }
        }
        return x;
    }

    /**
     * Simplifies the specified logical operation by constant folding.
     *
     * @param   x  the logical operation
     * @return  the possibly simplified instruction
     */
    private Instruction simplifyLogicOp(LogicOp x) {
        ValueType lt = x.getX().getType();
        ValueType rt = x.getY().getType();
        if (lt.isConstant() && rt.isConstant()) {
            if (x.getType().getTag() == ValueType.intTag) {
                int a = ((IntConstant) lt).getValue();
                int b = ((IntConstant) rt).getValue();
                switch (x.getOp()) {
                case Bytecodes._iand:
                    return new Constant(scope, new IntConstant(a & b));
                case Bytecodes._ior:
                    return new Constant(scope, new IntConstant(a | b));
                case Bytecodes._ixor:
                    return new Constant(scope, new IntConstant(a ^ b));
                }
            } else if (x.getType().getTag() == ValueType.longTag) {
                long a = ((LongConstant) lt).getValue();
                long b = ((LongConstant) rt).getValue();
                switch (x.getOp()) {
                case Bytecodes._land:
                    return new Constant(scope, new LongConstant(a & b));
                case Bytecodes._lor:
                    return new Constant(scope, new LongConstant(a | b));
                case Bytecodes._lxor:
                    return new Constant(scope, new LongConstant(a ^ b));
                }
            }
        }
        moveConstToRight(x);
        return x;
    }

    /**
     * Returns the truth value produced by the comparison of two constant
     * values.
     *
     * @param   x     first operand
     * @param   cond  condition code
     * @param   y     second operand
     * @return  whether or not the condition is true
     */
    private boolean isTrue(long x, int cond, long y) {
        switch (cond) {
        case If.EQ:
            return x == y;
        case If.NE:
            return x != y;
        case If.LT:
            return x < y;
        case If.LE:
            return x <= y;
        case If.GT:
            return x > y;
        case If.GE:
            return x >= y;
        default:
            Assert.shouldNotReachHere();
            return false;
        }
    }

    public void doArithmeticOp(ArithmeticOp x) {
        canonical = simplifyArithmeticOp(x);
    }

    public void doArrayLength(ArrayLength x) {
        /* nothing to do */
    }

    public void doBase(Base x) {
        /* nothing to do */
    }

    public void doBlockBegin(BlockBegin x) {
        /* nothing to do */
    }

    public void doCheckCast(CheckCast x) {
        /* nothing to do */
    }

    public void doCompareOp(CompareOp x) {
        /* nothing to do */
    }

    public void doConstant(Constant x) {
        /* nothing to do */
    }

    public void doConvert(Convert x) {
        /* nothing to do */
    }

    public void doGoto(Goto x) {
        /* nothing to do */
    }

    public void doIf(If x) {
        if (x.getX().getType().isConstant()) {
            x.swapOperands();
        }
        Instruction l = x.getX();
        ValueType lt = l.getType();
        Instruction r = x.getY();
        ValueType rt = r.getType();
        if (lt.isConstant() && rt.isConstant()) {
            if (lt.getTag() == ValueType.intTag) {
                int a = ((IntConstant) lt).getValue();
                int b = ((IntConstant) rt).getValue();
                canonical = new Goto(scope, x.suxFor(isTrue(a, x.getCond(), b)));
            } else if (lt.getTag() == ValueType.longTag) {
                long a = ((LongConstant) lt).getValue();
                long b = ((LongConstant) rt).getValue();
                canonical = new Goto(scope, x.suxFor(isTrue(a, x.getCond(), b)));
            }
        } else if (rt instanceof IntConstant) {
            int rc = ((IntConstant) rt).getValue();
            if (l instanceof CompareOp) {
                CompareOp cmp = (CompareOp) l;
                Instruction a = cmp.getX();
                Instruction b = cmp.getY();
                BlockBegin ltSux = x.suxFor(isTrue(-1, x.getCond(), rc));
                BlockBegin eqSux = x.suxFor(isTrue(0, x.getCond(), rc));
                BlockBegin gtSux = x.suxFor(isTrue(1, x.getCond(), rc));
                BlockBegin nanSux = cmp.isUnorderedLess() ? ltSux : gtSux;
                if ((ltSux == eqSux) && (eqSux == gtSux)) {
                    canonical = new Goto(scope, ltSux);
                } else {
                    int cond;
                    BlockBegin tsux;
                    BlockBegin fsux;
                    if (ltSux == eqSux) {
                        cond = If.LE; tsux = ltSux; fsux = gtSux;
                    } else if (ltSux == gtSux) {
                        cond = If.NE; tsux = ltSux; fsux = eqSux;
                    } else if (eqSux == gtSux) {
                        cond = If.GE; tsux = eqSux; fsux = ltSux;
                    } else {
                        Assert.shouldNotReachHere();
                        cond = If.EQ; tsux = null; fsux = null;
                    }
                    canonical = new If(scope, a, cond, nanSux == tsux, b, tsux, fsux);
                }
            }
        }
    }

    public void doIfOp(IfOp x) {
        moveConstToRight(x);
    }

    public void doInstanceOf(InstanceOf x) {
        /* nothing to do */
    }

    public void doIntrinsic(Intrinsic x) {
        /* nothing to do */
    }

    public void doInvoke(Invoke x) {
        /* nothing to do */
    }

    public void doJsr(Jsr x) {
        /* nothing to do */
    }

    public void doJsrContinuation(JsrContinuation x) {
        /* nothing to do */
    }

    public void doLoadField(LoadField x) {
        /* nothing to do */
    }

    public void doLoadIndexed(LoadIndexed x) {
        /* nothing to do */
    }

    public void doLoadLocal(LoadLocal x) {
        /* nothing to do */
    }

    public void doLogicOp(LogicOp x) {
        canonical = simplifyLogicOp(x);
    }

    public void doLookupSwitch(LookupSwitch x) {
        if (x.countSux() == 2) {
            Constant key = new Constant(scope, new IntConstant(x.keyAt(0)));
            BlockBegin tsux = x.suxAt(0);
            BlockBegin fsux = x.defaultSux();
            canonical = new If(scope, x.getTag(), If.EQ, true, key, tsux, fsux);
        }
    }

    public void doLoopEnter(LoopEnter x) {
        /* nothing to do */
    }

    public void doLoopExit(LoopExit x) {
        /* nothing to do */
    }

    public void doMonitorEnter(MonitorEnter x) {
        /* nothing to do */
    }

    public void doMonitorExit(MonitorExit x) {
        /* nothing to do */
    }

    public void doNegateOp(NegateOp x) {
        ValueType type = x.getX().getType();
        if (type.isConstant()) {
            switch (type.getTag()) {
            case ValueType.intTag:
                int i = ((IntConstant) type).getValue();
                canonical = new Constant(scope, new IntConstant(-i));
                break;
            case ValueType.longTag:
                long l = ((LongConstant) type).getValue();
                canonical = new Constant(scope, new LongConstant(-l));
                break;
            case ValueType.floatTag:
                float f = ((FloatConstant) type).getValue();
                canonical = new Constant(scope, new FloatConstant(-f));
                break;
            case ValueType.doubleTag:
                double d = ((DoubleConstant) type).getValue();
                canonical = new Constant(scope, new DoubleConstant(-d));
                break;
            default:
                Assert.shouldNotReachHere();
                break;
            }
        }
    }

    public void doNewInstance(NewInstance x) {
        /* nothing to do */
    }

    public void doNewMultiArray(NewMultiArray x) {
        /* nothing to do */
    }

    public void doNewObjectArray(NewObjectArray x) {
        /* nothing to do */
    }

    public void doNewTypeArray(NewTypeArray x) {
        /* nothing to do */
    }

    public void doNullCheck(NullCheck x) {
        /* nothing to do */
    }

    public void doPhi(Phi x) {
        /* nothing to do */
    }

    public void doRet(Ret x) {
        /* nothing to do */
    }

    public void doReturn(Return x) {
        /* nothing to do */
    }

    public void doShiftOp(ShiftOp x) {
        ValueType rt = x.getY().getType();
        if (rt instanceof IntConstant) {
            int s1 = ((IntConstant) rt).getValue() & (x.getType().isSingleWord() ? 0x1f : 0x3f);
            if (s1 == 0) {
                canonical = x.getX();
            } else if (x.getX() instanceof ShiftOp) {
                ShiftOp l = (ShiftOp) x.getX();
                ValueType lrt = l.getY().getType();
                if ((lrt instanceof IntConstant)
                        && (x.getOp() == Bytecodes._iushr)
                        && (l.getOp() == Bytecodes._ishl)) {
                    int s0 = ((IntConstant) lrt).getValue() & 0x1f;
                    if (s0 == s1) {
                        if (Assert.ASSERTS_ENABLED) {
                            Assert.that((s0 > 0) && (s0 < javac1.Flags.BitsPerWord), "adjust this code");
                        }
                        int m = (1 << (javac1.Flags.BitsPerWord - s0)) - 1;
                        Instruction s = new Constant(scope, new IntConstant(m));
                        canonical = new LogicOp(scope, Bytecodes._iand, l.getX(), s);
                    }
                }
            }
        }
    }

    public void doStoreField(StoreField x) {
        /* nothing to do */
    }

    public void doStoreIndexed(StoreIndexed x) {
        /* nothing to do */
    }

    public void doStoreLocal(StoreLocal x) {
        /* nothing to do */
    }

    public void doTableSwitch(TableSwitch x) {
        if (x.countSux() == 2) {
            Constant key = new Constant(scope, new IntConstant(x.getLoKey()));
            BlockBegin tsux = x.suxAt(0);
            BlockBegin fsux = x.defaultSux();
            canonical = new If(scope, x.getTag(), If.EQ, true, key, tsux, fsux);
        }
    }

    public void doThrow(Throw x) {
        /* nothing to do */
    }

    /**
     * Transforms the specified instruction into its canonical form.
     *
     * @param   x  instruction to be simplified
     * @return  the canonical form of the instruction
     */
    public Instruction simplify(Instruction x) {
        canonical = x;
        if (javac1.Flags.CanonicalizeNodes) {
            x.visit(this);
            if (javac1.Flags.PrintCanonicalization && (canonical != x)) {
                ip.printInstr(x);
                JavaC1.out.print(" canonicalized to ");
                ip.printInstr(canonical);
                JavaC1.out.println();
            }
        }
        return canonical;
    }
}
