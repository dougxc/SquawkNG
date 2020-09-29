package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.OpConst;
import com.sun.squawk.translator.Assert;


/**
 * The InstructionFactory.
 */

public class InstructionFactory {

    /** The translator context. */
    private final Translator vm;
    /** The method for which instructions are being created. */
    private final Method method;

    /**
     * Constructor.
     * @param vm
     * @param method
     */
    public InstructionFactory(Translator vm, Method method) {
        this.vm     = vm;
        this.method = method;
    }

    /**
     * Create a new ArithmeticOp representing an arithmetic or logical instruction.
     * @param op The arithmetic or logical operation.
     * @param left Instruction that generates the left operand.
     * @param right Instruction that generates the left operand.
     * @return the new ArithmeticOp instruction.
     */
    public Instruction createArithmeticOp(OpConst op, int opcode, Instruction left, Instruction right) {
        return new ArithmeticOp(left.type(), op, opcode, left, right, false);
    }

    /**
     * Create a new ArithmeticOp representing a comparison instruction.
     * @param op The arithmetic or logical operation.
     * @param left Instruction that generates the left operand.
     * @param right Instruction that generates the left operand.
     * @return the new ArithmeticOp instruction.
     */
    public Instruction createArithmeticOpCmp(OpConst op, int opcode, Instruction left, Instruction right) {
        return new ArithmeticOp(left.getVM().INT, op, opcode, left, right, true);
    }

    /**
     * Create an ArrayLength instruction.
     * @param array The instruction that pushes the array.
     * @return the new ArrayLength instruction.
     */
    public Instruction createArrayLength(Instruction array) {
        return new ArrayLength(array);
    }

    /**
     * Create a CheckCast instruction.
     * @param checkType The type being checked for.
     * @param value The instruction that pushed the value being checked.
     * @return the new CheckCast instruction.
     */
    public Instruction createCheckCast(Type checkType, Instruction value) {
        return new CheckCast(checkType, value);
    }

    /**
     * Create a new ConvertOp representing a conversion instruction.
     * @param op The conversion operation.
     * @param fromType The type being converted from.
     * @param toType The type being converted to.
     * @param value Instruction that pushes the value to be converted.
     * @return the new ConvertOp instruction.
     */
    public Instruction createConvertOp(OpConst op, Type fromType, Type toType, Instruction value) {
        return new ConvertOp(op, fromType, toType, value);
    }

    /**
     * Create a Goto instruction.
     * @param target The target of the branch.
     * @return the new Goto instruction.
     */
    public Instruction createGoto(Target target) {
        return new Goto(target);
    }

    /**
     * Create a HandlerEnter to represent the start of a range of code protected by
     * a single exception handler.
     * @param target The entry address of the exception handler corresponding to this try block start.
     * @return the new HandlerEnter.
     */
    public Instruction createHandlerEnter(Target target) {
        return new HandlerEnter(target);
    }

    /**
     * Create a HandlerExit to represent the end of a range of code protected by
     * a single exception handler.
     * @param target The entry address of the exception handler corresponding to this try block end.
     * @return the new HandlerExit.
     */
    public Instruction createHandlerExit(Target target) {
        return new HandlerExit(target);
    }

    /**
     * Create a new conditional instruction.
     * @param op The conditional operation.
     * @param left The instruction that pushes the left operand or null if this is a compare with zero/null.
     * @param right The instruction that pushes the right operand.
     * @param target The target of the branch.
     * @return the new conditional instruction.
     */
    public Instruction createIfOp(OpConst op, Instruction left, Instruction right, Target target) {
        return new IfOp(op, left, right, target);
    }

    /**
     * Create a new IncDecLocal instruction.
     * @param local The local being incremented or decremented.
     * @param increment True if this is an increment.
     * @return the new IncDecLocal instruction.
     */
    public Instruction createIncDecLocal(Local local, boolean increment) {
        return new IncDecLocal(local, increment);
    }

    /**
     * Create a new InstanceOf instruction.
     * @param checkType The type being checked for.
     * @param value The reference being checked.
     * @return the new InstanceOf instruction.
     */
    public Instruction createInstanceOf(Type checkType, Instruction value) {
        return new InstanceOf(checkType, value);
    }

    public Instruction createInvoke(Method method, Instruction[] parms, Invoke.Form form) {
        return new Invoke(method, parms, form);
    }

    /**
     * Create an IR instruction representing a push of an integer constant to the stack.
     * This is also used to push a byte, char or short to the stack.
     * @param value The constant value.
     * @return the new LoadConstant instruction.
     */
    public Instruction createLoadConstantInt(int value) {
        return new LoadConstantInt(vm.INT, value);
    }

    /**
     * Create an IR instruction representing a push of a long constant to the stack.
     * @param value The constant value.
     * @return the new LoadConstant instruction.
     */
    public Instruction createLoadConstantLong(long value) {
        return new LoadConstantLong(vm.LONG, value);
    }

/*if[FLOATS]*/
    /**
     * Create an IR instruction representing a push of a float constant to the stack.
     * @param value The constant value.
     * @return the new LoadConstant instruction.
     */
    public Instruction createLoadConstantFloat(float value) {
        return new LoadConstantFloat(vm.FLOAT, value);
    }

    /**
     * Create an IR instruction representing a push of a double constant to the stack.
     * @param value The constant value.
     * @return the new LoadConstant instruction.
     */
    public Instruction createLoadConstantDouble(double value) {
        return new LoadConstantDouble(vm.DOUBLE, value);
    }
/*end[FLOATS]*/

    /**
     * Create an IR instruction representing a push of class's ID to the stack.
     * @param value The class whose ID is to be pushed.
     * @return the new LoadConstant instruction.
     */
    public Instruction createLoadConstantType(Type value) {
        return new LoadConstantType(value);
    }

    /**
     * Create an IR instruction representing a push of "null" to the stack.
     * @param vm The translator context of this instruction.
     * @return the new LoadConstant instruction.
     */
    public Instruction createLoadConstantNull() {
        return new LoadConstantNull(vm.NULLOBJECT);
    }

    /**
     *
     * @param type
     * @param value
     * @return
     */
    public Instruction createLoadConstantObject(Type type, Object value) {
        return new LoadConstantObject(type, value);
    }

    /**
     * Create a LoadDummy object to represent a type on the operand stack.
     * @param type The type pushed.
     */
    public Instruction createLoadSynthetic(Type type) {
        return new LoadSynthetic(type);
    }

    /**
     * Create a LoadException instruction.
     * @param target The Target representing the address of this instruction.
     * @return the LoadException instruction.
     */
    public Instruction createLoadException(Target target) {
        return new LoadException(target);
    }

    public Instruction createLoadField(Field field, Instruction ref, boolean isFieldOfReceiver) {
        Translator vm = field.parent().vm();
        // Return a LoadConstant if the field has no slot (it appears that the 1.3 javac resolves constants anyway).
        if (field.isPrimitiveConstant()) {
            Assert.that(field.isFinal() && field.getConstantValue() != null);
            Object o = field.getConstantValue();
            if        (o instanceof Integer) {
                return createLoadConstantInt(((Integer)o).intValue());
            }
            else if (o instanceof Long) {
                return createLoadConstantLong(((Long)o).longValue());
            }
/*if[FLOATS]*/
            else if (o instanceof Float) {
                return createLoadConstantFloat(((Float)o).floatValue());
            } else if (o instanceof Double) {
                return createLoadConstantDouble(((Double)o).doubleValue());
            }
/*end[FLOATS]*/
            Assert.shouldNotReachHere();
            return null;
        }
        return new LoadField(field, ref, isFieldOfReceiver);
    }

    public Instruction createLoadIndexed(Instruction array, Instruction index, Type type) {
        if (array.type() == type.vm().NULLOBJECT) {
           /*
            * If the following occurs:
            *
            *   Object obj = null;
            *   Something x = obj[0];
            *
            * The error should be detected at run time not compile time.
            */
            return new LoadIndexed(type.elementType(), array, index);
        } else {
            return new LoadIndexed(array.type().elementType(), array, index);
        }
    }

    public Instruction createLoadLocal(Local local, Type type) {
        return new LoadLocal(local, type);
    }

    /**
     * Create a LookupSwitch instruction.
     * @param key The instruction that pushed the value being switched on.
     * @param npairs The number of cases in the switch.
     * @param defaultTarget The target for the "default" case.
     * @return the new LookupSwitch instruction.
     */
    public Instruction createLookupSwitch(Instruction key, int npairs, Target defaultTarget) {
        return new LookupSwitch(key, npairs, defaultTarget);
    }

    public Instruction createLookup(Instruction key, Instruction array) {
        return new Lookup(vm.INT, key, array);
    }

    public Instruction createMonitorEnter(Instruction value) {
        return new MonitorEnter(value);
    }

    public Instruction createMonitorExit(Instruction value) {
        return new MonitorExit(value);
    }

    public Instruction createNegateOp(Instruction value) {
        return new NegateOp(value);
    }

    public Instruction createNewArray(Type type, Instruction size) {
        return new NewArray(type, size);
    }

    public Instruction createNewDimension(Instruction array, Instruction dimension) {
        return new NewDimension(array, dimension);
    }

    public Instruction createNewMultiArray(Type type, Instruction[] dimList) {
        Assert.shouldNotReachHere();
        return new NewMultiArray(type, dimList);
    }

    public Instruction createNewObject(Type type) {
        return new NewObject(type);
    }

    public Instruction createPhi(Target target) {
        return new Phi(target);
    }

    /** Create a pop instruction. */
    public Instruction createPop(Instruction value) {
        return new Pop(value);
    }

    public Instruction createReturnNull() {
        return new Return(vm.VOID, null);
    }

    public Instruction createReturn(Instruction value) {
        return new Return(value.type(), value);
    }

    public Instruction createStoreField(Field field, Instruction ref, Instruction value, boolean isFieldOfReceiver) {
        if (field.isPrimitiveConstant()) {
            /*
             * This must be an initialization of a constant static field (only
             * javac 1.4 seems to do this). Given that compilers will have inlined
             * this value, there is no need to do the initialization at runtime.
             */
            Assert.that(field.getConstantValue() != null);
            Assert.that(field.isFinal());
            return null;
        }
        return new StoreField(field, ref, value, isFieldOfReceiver);
    }

    /**
     * Create a StoreIndexed instruction.
     * @param array The instruction that pushed the array object.
     * @param index The instruction that pushed the index at which the value is to be stored.
     * @param value The instruction that pushed the value to be stored into the array.
     * @param basicType The basic type of the array.
     * @return the new StoreIndexed instruction.
     */
    public Instruction createStoreIndexed(Instruction array, Instruction index, Instruction value, Type basicType) {
        return new StoreIndexed(array, index, value, basicType);
    }

    public Instruction createStoreLocal(Local local, Instruction value) {
        return new StoreLocal(local, value);
    }

    public Instruction createTableSwitch(Instruction key, int low, int high, Target defaultTarget) {
        return new TableSwitch(key, low, high, defaultTarget);
    }

    public Instruction createThrow(Instruction value) {
        return new Throw(value);
    }


}
