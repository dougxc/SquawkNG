package com.sun.squawk.translator.util;

import com.sun.squawk.vm.*;

/**
 * Enumerated type for representing various operation semantics that don't
 * take into account operand or return types.
 */
public final class OpConst {
    /** The mnemonic for the operation. */
    public final String mnemonic;
    /** The symbol for the operation. */
    public final String symbol;
    private final int convBytecode;
    private final int cmpBytecodes;

    /**
     * Construct an enum value for an operation that has different mnemonic
     * and symbolic representations.
     * @param mnemonic
     * @param symbol
     */
    private OpConst(String mnemonic,
                 String symbol,
                 int convBytecode,
                 int cmpWithZeroOrNullBytecode,
                 int cmpBytecode) {
        this.mnemonic = mnemonic;
        this.symbol   = symbol;
        this.convBytecode = convBytecode;
        if (cmpWithZeroOrNullBytecode != -1) {
            this.cmpBytecodes = (cmpWithZeroOrNullBytecode << 16) | (cmpBytecode);
        } else {
            this.cmpBytecodes = -1;
        }
    }

    public String toString() {
        return mnemonic;
    }

    /**
     * Return the conversion bytecode corresponding to this operation.
     * @return the conversion bytecode corresponding to this operation.
     * @exception RuntimeException if there is no conversion bytecode corresponding to this operation.
     */
    public int convBytecode() throws RuntimeException {
        if (convBytecode == -1) {
            throw new RuntimeException("'" + this +"' has no conversion bytecode");
        }
        return convBytecode;
    }

    /**
     * Return the conditional branch bytecode corresponding to this operation.
     * @return the conditional branch bytecode corresponding to this operation.
     * @exception RuntimeException if there is no conditional branch bytecode corresponding to this operation.
     */
    public int ifBytecode(boolean cmpWithZeroOrNull) throws RuntimeException {
        if (cmpBytecodes == -1) {
            throw new RuntimeException("'" + this + "' has no conditional branch bytecode");
        }
        if (cmpWithZeroOrNull) {
            return (cmpBytecodes >> 16) & 0xFFFF;
        } else {
            return cmpBytecodes & 0xFFFF;
        }
    }

    /**
     * Constants for the arithmetic and compare operations, independent
     * of operand types.
     */
    public static final OpConst ADD =   new OpConst("add", "+", -1, -1, -1);
    public static final OpConst SUB =   new OpConst("sub", "-", -1, -1, -1);
    public static final OpConst MUL =   new OpConst("mul", "*", -1, -1, -1);
    public static final OpConst DIV =   new OpConst("div", "/", -1, -1, -1);
    public static final OpConst REM =   new OpConst("rem", "%", -1, -1, -1);
    public static final OpConst SHL =   new OpConst("shl", "<<", -1, -1, -1);
    public static final OpConst SHR =   new OpConst("sra", ">>", -1, -1, -1);
    public static final OpConst USHR =  new OpConst("srl", ">>>", -1, -1, -1);
    public static final OpConst AND =   new OpConst("and", "&", -1, -1, -1);
    public static final OpConst OR =    new OpConst("or",  "|", -1, -1, -1);
    public static final OpConst XOR =   new OpConst("xor", "^", -1, -1, -1);
    public static final OpConst NEG =   new OpConst("neg", "!", -1, -1, -1);
    public static final OpConst I2L =   new OpConst("i2l", "i2l", OPC.I2L, -1, -1);
/*if[FLOATS]*/
    public static final OpConst I2F =   new OpConst("i2f", "i2f", OPC.I2F, -1, -1);
    public static final OpConst I2D =   new OpConst("i2d", "i2d", OPC.I2D, -1, -1);
/*end[FLOATS]*/
    public static final OpConst L2I =   new OpConst("l2i", "l2i", OPC.L2I, -1, -1);
/*if[FLOATS]*/
    public static final OpConst L2F =   new OpConst("l2f", "l2f", OPC.L2F, -1, -1);
    public static final OpConst L2D =   new OpConst("l2d", "l2d", OPC.L2D, -1, -1);
    public static final OpConst F2I  =  new OpConst("f2i", "f2i", OPC.F2I, -1, -1);
    public static final OpConst F2L =   new OpConst("f2l", "f2l", OPC.F2L, -1, -1);
    public static final OpConst F2D =   new OpConst("f2d", "f2d", OPC.F2D, -1, -1);
    public static final OpConst D2I =   new OpConst("d2i", "d2i", OPC.D2I, -1, -1);
    public static final OpConst D2L =   new OpConst("d2l", "d2l", OPC.D2L, -1, -1);
    public static final OpConst D2F =   new OpConst("d2f", "d2f", OPC.D2F, -1, -1);
/*end[FLOATS]*/
    public static final OpConst I2B =   new OpConst("i2b", "i2b", OPC.I2B, -1, -1);
    public static final OpConst I2C =   new OpConst("i2c", "i2c", OPC.I2C, -1, -1);
    public static final OpConst I2S =   new OpConst("i2s", "i2s", OPC.I2S, -1, -1);
    public static final OpConst LCMP =  new OpConst("cmpl", "cmpl",  OPC.LCMP, -1, -1);
/*if[FLOATS]*/
    public static final OpConst FCMPL = new OpConst("cmpfl", "cmpfl", OPC.FCMPL, -1, -1);
    public static final OpConst FCMPG = new OpConst("cmpfg", "cmpfg", OPC.FCMPG, -1, -1);
    public static final OpConst DCMPL = new OpConst("cmpdl", "cmpdl", OPC.DCMPL, -1, -1);
    public static final OpConst DCMPG = new OpConst("cmpdg", "cmpdg", OPC.DCMPG, -1, -1);
/*end[FLOATS]*/
    public static final OpConst EQ =    new OpConst("eq", "==", -1, OPC.IFEQ, OPC.IF_ICMPEQ);
    public static final OpConst NE =    new OpConst("ne", "!=", -1, OPC.IFNE, OPC.IF_ICMPNE);
    public static final OpConst LT =    new OpConst("lt", "<", -1,  OPC.IFLT, OPC.IF_ICMPLT);
    public static final OpConst GE =    new OpConst("ge", ">=", -1, OPC.IFGE, OPC.IF_ICMPGE);
    public static final OpConst GT =    new OpConst("gt", ">", -1,  OPC.IFGT, OPC.IF_ICMPGT);
    public static final OpConst LE =    new OpConst("le", "<=", -1, OPC.IFLE, OPC.IF_ICMPLE);
}
