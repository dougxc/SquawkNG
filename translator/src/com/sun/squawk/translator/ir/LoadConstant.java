package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

/**
 * This class is the base class for the IR instructions that push a constant
 * value onto the stack.
 */
public abstract class LoadConstant extends Instruction {

    LoadConstant(Type type) {
        super(type);
    }

    public int getInt() {
        Assert.shouldNotReachHere();
        return 0;
    }

    public long getLong() {
        Assert.shouldNotReachHere();
        return 0L;
    }

/*if[FLOATS]*/
    public float getFloat() {
        Assert.shouldNotReachHere();
        return 0f;
    }

    public double getDouble() {
        Assert.shouldNotReachHere();
        return 0d;
    }
/*end[FLOATS]*/

    public String getString() {
        Assert.shouldNotReachHere();
        return null;
    }

    public Type getType() {
        Assert.shouldNotReachHere();
        return null;
    }

    public boolean isConstNull() {
        return false;
    }

    public boolean isInt() {
        return false;
    }

    public boolean isLong() {
        return false;
    }

/*if[FLOATS]*/
    public boolean isFloat() {
        return false;
    }

    public boolean isDouble() {
        return false;
    }
/*end[FLOATS]*/

    public boolean isType() {
        return false;
    }

    /**
     * Return whether or not this is a load of the default value for the type.
     * @return
     */
    public abstract boolean isDefaultValue();

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doLoadConstant(this);
    }
}

class LoadConstantInt extends LoadConstant {
    int value;
    LoadConstantInt(Type type, int value) {
        super(type);
        this.value = value;
    }
    public boolean isInt() {
        return true;
    }
    public int getInt() {
        return value;
    }
    public String toString() {
        return Mnemonics.OPCODES[OPC.CONST_INT] + " " +value;
    }
    public boolean isDefaultValue() {
        return value == 0;
    }
}

class LoadConstantLong extends LoadConstant {
    long value;
    LoadConstantLong(Type type, long value) {
        super(type);
        this.value = value;
    }
    public boolean isLong() {
        return true;
    }
    public long getLong() {
        return value;
    }
    public String toString() {
        return Mnemonics.OPCODES[OPC.CONST_LONG] + " " +value;
    }
    public boolean isDefaultValue() {
        return value == 0L;
    }
}

/*if[FLOATS]*/
class LoadConstantFloat extends LoadConstant {
    float value;
    LoadConstantFloat(Type type, float value) {
        super(type);
        this.value = value;
    }
    public boolean isFloat() {
        return true;
    }
    public float getFloat() {
        return value;
    }
    public int getInt() {
        return Float.floatToIntBits(value);
    }
    public String toString() {
        return Mnemonics.OPCODES[OPC.CONST_FLOAT] + " " +value+"F";
    }
    public boolean isDefaultValue() {
        return value == 0F;
    }
}

class LoadConstantDouble extends LoadConstant {
    double value;
    LoadConstantDouble(Type type, double value) {
        super(type);
        this.value = value;
    }
    public boolean isDouble() {
        return true;
    }
    public double getDouble() {
        return value;
    }
    public long getLong() {
        return Double.doubleToLongBits(value);
    }
    public String toString() {
        return Mnemonics.OPCODES[OPC.CONST_DOUBLE] + " " +value+"D";
    }
    public boolean isDefaultValue() {
        return value == 0D;
    }
}
/*end[FLOATS]*/

class LoadConstantType extends LoadConstant {
    Type realType;
    LoadConstantType(Type realType) {
        super(realType.vm().CLASS);
        this.realType = realType;
    }
    public boolean isType() {
        return true;
    }
    public Type getType() {
        return realType;
    }
    public String toString() {
        return Mnemonics.OPCODES[OPC.CLASS] + realType.toSignature(true, true);
    }
    public boolean isDefaultValue() {
        return false;
    }
}