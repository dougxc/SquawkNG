/*
 * @(#)InstructionVisitor.java          1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir;

import javac1.ir.instr.*;

/**
 * Provides type based dispatch for instruction nodes. Functionality that needs
 * to be implemented for all classes is factored out into a specialised visitor
 * instead of being added to the instruction classes itself. There is one
 * function for each concrete class.
 *
 * @see      Instruction#visit(InstructionVisitor)
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public interface InstructionVisitor {
    public void doArithmeticOp(ArithmeticOp x);
    public void doArrayLength(ArrayLength x);
    public void doBase(Base x);
    public void doBlockBegin(BlockBegin x);
    public void doCheckCast(CheckCast x);
    public void doCompareOp(CompareOp x);
    public void doConstant(Constant x);
    public void doConvert(Convert x);
    public void doGoto(Goto x);
    public void doIf(If x);
    public void doIfOp(IfOp x);
    public void doInstanceOf(InstanceOf x);
    public void doIntrinsic(Intrinsic x);
    public void doInvoke(Invoke x);
    public void doJsr(Jsr x);
    public void doJsrContinuation(JsrContinuation x);
    public void doLoadField(LoadField x);
    public void doLoadIndexed(LoadIndexed x);
    public void doLoadLocal(LoadLocal x);
    public void doLogicOp(LogicOp x);
    public void doLookupSwitch(LookupSwitch x);
    public void doLoopEnter(LoopEnter x);
    public void doLoopExit(LoopExit x);
    public void doMonitorEnter(MonitorEnter x);
    public void doMonitorExit(MonitorExit x);
    public void doNegateOp(NegateOp x);
    public void doNewInstance(NewInstance x);
    public void doNewMultiArray(NewMultiArray x);
    public void doNewObjectArray(NewObjectArray x);
    public void doNewTypeArray(NewTypeArray x);
    public void doNullCheck(NullCheck x);
    public void doPhi(Phi x);
    public void doRet(Ret x);
    public void doReturn(Return x);
    public void doShiftOp(ShiftOp x);
    public void doStoreField(StoreField x);
    public void doStoreIndexed(StoreIndexed x);
    public void doStoreLocal(StoreLocal x);
    public void doTableSwitch(TableSwitch x);
    public void doThrow(Throw x);
}
