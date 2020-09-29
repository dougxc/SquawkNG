package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.vm.*;

public class LoadConstantNull extends LoadConstant {
    private Type castType;
    LoadConstantNull(Type type) {
        super(type);
    }
    public boolean isConstNull() {
        return true;
    }
    public boolean isDefaultValue() {
        return true;
    }
    public String toString() {
        return Mnemonics.OPCODES[OPC.CONST_NULL];
    }

    public void setCastType(Type type) {
        Assert.that(type != null && type != type.vm().NULLOBJECT);
        castType = type;
    }
    public Type castType() {
        return castType;
    }
    public Type getReferencedType() {
        return castType();
    }
}
