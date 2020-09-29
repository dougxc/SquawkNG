/*
 * @(#)InstructionFilter.java           1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend;

import javac1.Assert;
import javac1.ir.InstructionVisitor;
import javac1.ir.ValueStack;
import javac1.ir.instr.*;

/**
 * Tests if a method contains instructions that prevent it from being inlined.
 * At the same time each instruction that loads an argument is substituted by
 * the instruction of the outer method that produces the actual value of the
 * argument.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class InstructionFilter implements InstructionVisitor {
    /**
     * The expression stack of the outer method holding the arguments.
     */
    private ValueStack state;

    /**
     * The size of the arguments of the method to be inlined.
     */
    private int argSize;

    /**
     * The start of the actual parameters on the expression stack.
     */
    private int argBase;

    /**
     * Whether or not the method can be inlined.
     */
    private boolean okay;

    /**
     * Constructs a new instruction filter.
     *
     * @param   state    expression stack of the outer method
     * @param   argSize  size of the arguments
     */
    public InstructionFilter(ValueStack state, int argSize) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(state != null, "state must exist");
            Assert.that(state.getStackSize() >= argSize, "stack too small");
        }
        this.state = state;
        this.argSize = argSize;
        argBase = state.getStackSize() - argSize;
        okay = true;
    }

    /**
     * Substitutes the specified loading instruction by the actual value of the
     * argument.
     *
     * @param  x  instruction that loads an argument
     */
    private void substituteLoad(LoadLocal x) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((x.getIndex() >= 0) || (x.getIndex() < argSize), "index out of bounds");
        }
        x.setSubst(state.stackAt(argBase + x.getIndex()));
    }

    public void doArithmeticOp(ArithmeticOp x) {
        /* nothing to do */
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
        okay = false;
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
        Assert.shouldNotReachHere();
    }

    public void doIf(If x) {
        Assert.shouldNotReachHere();
    }

    public void doIfOp(IfOp x) {
        /* nothing to do */
    }

    public void doInstanceOf(InstanceOf x) {
        okay = false;
    }

    public void doIntrinsic(Intrinsic x) {
        okay = false;
    }

    public void doInvoke(Invoke x) {
        okay = false;
    }

    public void doJsr(Jsr x) {
        okay = false;
    }

    public void doJsrContinuation(JsrContinuation x) {
        okay = false;
    }

    public void doLoadField(LoadField x) {
        /* nothing to do */
    }

    public void doLoadIndexed(LoadIndexed x) {
        /* nothing to do */
    }

    public void doLoadLocal(LoadLocal x) {
        substituteLoad(x);
    }

    public void doLogicOp(LogicOp x) {
        /* nothing to do */
    }

    public void doLookupSwitch(LookupSwitch x) {
        Assert.shouldNotReachHere();
    }

    public void doLoopEnter(LoopEnter x) {
        Assert.shouldNotReachHere();
    }

    public void doLoopExit(LoopExit x) {
        Assert.shouldNotReachHere();
    }

    public void doMonitorEnter(MonitorEnter x) {
        okay = false;
    }

    public void doMonitorExit(MonitorExit x) {
        okay = false;
    }

    public void doNegateOp(NegateOp x) {
        /* nothing to do */
    }

    public void doNewInstance(NewInstance x) {
        okay = false;
    }

    public void doNewMultiArray(NewMultiArray x) {
        okay = false;
    }

    public void doNewObjectArray(NewObjectArray x) {
        okay = false;
    }

    public void doNewTypeArray(NewTypeArray x) {
        okay = false;
    }

    public void doNullCheck(NullCheck x) {
        okay = false;
    }

    public void doPhi(Phi x) {
        /* nothing to do */
    }

    public void doRet(Ret x) {
        okay = false;
    }

    public void doReturn(Return x) {
        /* nothing to do */
    }

    public void doShiftOp(ShiftOp x) {
        /* nothing to do */
    }

    public void doStoreField(StoreField x) {
        /* nothing to do */
    }

    public void doStoreIndexed(StoreIndexed x) {
        /* nothing to do */
    }

    public void doStoreLocal(StoreLocal x) {
        okay = false;
    }

    public void doTableSwitch(TableSwitch x) {
        Assert.shouldNotReachHere();
    }

    public void doThrow(Throw x) {
        okay = false;
    }

    /**
     * Returns whether or not the method can be inlined.
     *
     * @return  whether or not the method can be inlined
     */
    public boolean isOkay() {
        return okay;
    }

    /**
     * Examines the specified instruction and its successors.
     *
     * @param  x  the instruction to start with
     */
    public void apply(Instruction x) {
        for (Instruction instr = x; instr != null; instr = instr.getNext()) {
            instr.visit(this);
        }
    }
}
