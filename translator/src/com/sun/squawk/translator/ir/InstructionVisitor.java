package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.loader.LinkageException;
import java.io.PrintStream;

public interface InstructionVisitor {
    public void doArithmeticOp      (ArithmeticOp       inst) throws LinkageException;
    public void doArrayLength       (ArrayLength        inst) throws LinkageException;
    public void doCheckCast         (CheckCast          inst) throws LinkageException;
    public void doConvertOp         (ConvertOp          inst) throws LinkageException;
    public void doGoto              (Goto               inst) throws LinkageException;
    public void doHandlerEnter      (HandlerEnter       inst) throws LinkageException;
    public void doHandlerExit       (HandlerExit        inst) throws LinkageException;
    public void doIfOp              (IfOp               inst) throws LinkageException;
    public void doIncDecLocal       (IncDecLocal        inst) throws LinkageException;
    public void doInstanceOf        (InstanceOf         inst) throws LinkageException;
    public void doInvoke            (Invoke             inst) throws LinkageException;
    public void doLoadConstant      (LoadConstant       inst) throws LinkageException;
    public void doLoadConstantObject(LoadConstantObject inst) throws LinkageException;
    public void doLoadException     (LoadException      inst) throws LinkageException;
    public void doLoadField         (LoadField          inst) throws LinkageException;
    public void doLoadIndexed       (LoadIndexed        inst) throws LinkageException;
    public void doLoadLocal         (LoadLocal          inst) throws LinkageException;
    public void doLookupSwitch      (LookupSwitch       inst) throws LinkageException;
    public void doLookup            (Lookup             inst) throws LinkageException;
    public void doMonitorEnter      (MonitorEnter       inst) throws LinkageException;
    public void doMonitorExit       (MonitorExit        inst) throws LinkageException;
    public void doNegateOp          (NegateOp           inst) throws LinkageException;
    public void doNewArray          (NewArray           inst) throws LinkageException;
    public void doNewDimension      (NewDimension       inst) throws LinkageException;
    public void doNewMultiArray     (NewMultiArray      inst) throws LinkageException;
    public void doNewObject         (NewObject          inst) throws LinkageException;
    public void doPhi               (Phi                inst) throws LinkageException;
    public void doPop               (Pop                inst) throws LinkageException;
    public void doReturn            (Return             inst) throws LinkageException;
    public void doStoreField        (StoreField         inst) throws LinkageException;
    public void doStoreIndexed      (StoreIndexed       inst) throws LinkageException;
    public void doStoreLocal        (StoreLocal         inst) throws LinkageException;
    public void doTableSwitch       (TableSwitch        inst) throws LinkageException;
    public void doThrow             (Throw              inst) throws LinkageException;
}




