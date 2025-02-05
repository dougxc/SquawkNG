//if[DEBUG.LOADER]   /* This will selectively exclude the entire file from the build */
//**DO NOT EDIT THIS FILE**
/*
 * Copyright 1994-2002 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

package com.sun.squawk.suite;
import com.sun.squawk.vm.OPC;
import com.sun.squawk.vm.SquawkConstants;
public class TagLookup extends TagLookupBase {

static {
    put("public",            -SquawkConstants.ACC_PUBLIC);
    put("private",           -SquawkConstants.ACC_PRIVATE);
    put("protected",         -SquawkConstants.ACC_PROTECTED);
    put("final",             -SquawkConstants.ACC_FINAL);
    put("volatile",          -SquawkConstants.ACC_VOLATILE);
    put("transient",         -SquawkConstants.ACC_TRANSIENT);
    put("interface",         -SquawkConstants.ACC_INTERFACE);
    put("abstract",          -SquawkConstants.ACC_ABSTRACT);
    put("native",            -SquawkConstants.ACC_NATIVE);
    put("init",              -SquawkConstants.ACC_INIT);
    put("static",            -SquawkConstants.ACC_STATIC);
    put("proxy",             -SquawkConstants.ACC_PROXY);
    put("symbolic",          -SquawkConstants.ACC_SYMBOLIC);
    put("const_0",            OPC.CONST_0);
    put("const_1",            OPC.CONST_1);
    put("const_2",            OPC.CONST_2);
    put("const_3",            OPC.CONST_3);
    put("const_4",            OPC.CONST_4);
    put("const_5",            OPC.CONST_5);
    put("const_6",            OPC.CONST_6);
    put("const_7",            OPC.CONST_7);
    put("const_8",            OPC.CONST_8);
    put("const_9",            OPC.CONST_9);
    put("const_10",           OPC.CONST_10);
    put("const_11",           OPC.CONST_11);
    put("const_12",           OPC.CONST_12);
    put("const_13",           OPC.CONST_13);
    put("const_14",           OPC.CONST_14);
    put("const_15",           OPC.CONST_15);
    put("object_0",           OPC.OBJECT_0);
    put("object_1",           OPC.OBJECT_1);
    put("object_2",           OPC.OBJECT_2);
    put("object_3",           OPC.OBJECT_3);
    put("object_4",           OPC.OBJECT_4);
    put("object_5",           OPC.OBJECT_5);
    put("object_6",           OPC.OBJECT_6);
    put("object_7",           OPC.OBJECT_7);
    put("object_8",           OPC.OBJECT_8);
    put("object_9",           OPC.OBJECT_9);
    put("object_10",          OPC.OBJECT_10);
    put("object_11",          OPC.OBJECT_11);
    put("object_12",          OPC.OBJECT_12);
    put("object_13",          OPC.OBJECT_13);
    put("object_14",          OPC.OBJECT_14);
    put("object_15",          OPC.OBJECT_15);
    put("class_0",            OPC.CLASS_0);
    put("class_1",            OPC.CLASS_1);
    put("class_2",            OPC.CLASS_2);
    put("class_3",            OPC.CLASS_3);
    put("class_4",            OPC.CLASS_4);
    put("class_5",            OPC.CLASS_5);
    put("class_6",            OPC.CLASS_6);
    put("class_7",            OPC.CLASS_7);
    put("class_8",            OPC.CLASS_8);
    put("class_9",            OPC.CLASS_9);
    put("class_10",           OPC.CLASS_10);
    put("class_11",           OPC.CLASS_11);
    put("class_12",           OPC.CLASS_12);
    put("class_13",           OPC.CLASS_13);
    put("class_14",           OPC.CLASS_14);
    put("class_15",           OPC.CLASS_15);
    put("load_0",             OPC.LOAD_0);
    put("load_1",             OPC.LOAD_1);
    put("load_2",             OPC.LOAD_2);
    put("load_3",             OPC.LOAD_3);
    put("load_4",             OPC.LOAD_4);
    put("load_5",             OPC.LOAD_5);
    put("load_6",             OPC.LOAD_6);
    put("load_7",             OPC.LOAD_7);
    put("load_8",             OPC.LOAD_8);
    put("load_9",             OPC.LOAD_9);
    put("load_10",            OPC.LOAD_10);
    put("load_11",            OPC.LOAD_11);
    put("load_12",            OPC.LOAD_12);
    put("load_13",            OPC.LOAD_13);
    put("load_14",            OPC.LOAD_14);
    put("load_15",            OPC.LOAD_15);
    put("store_0",            OPC.STORE_0);
    put("store_1",            OPC.STORE_1);
    put("store_2",            OPC.STORE_2);
    put("store_3",            OPC.STORE_3);
    put("store_4",            OPC.STORE_4);
    put("store_5",            OPC.STORE_5);
    put("store_6",            OPC.STORE_6);
    put("store_7",            OPC.STORE_7);
    put("store_8",            OPC.STORE_8);
    put("store_9",            OPC.STORE_9);
    put("store_10",           OPC.STORE_10);
    put("store_11",           OPC.STORE_11);
    put("store_12",           OPC.STORE_12);
    put("store_13",           OPC.STORE_13);
    put("store_14",           OPC.STORE_14);
    put("store_15",           OPC.STORE_15);
    put("wide_0",             OPC.WIDE_0);
    put("wide_1",             OPC.WIDE_1);
    put("wide_2",             OPC.WIDE_2);
    put("wide_3",             OPC.WIDE_3);
    put("wide_4",             OPC.WIDE_4);
    put("wide_5",             OPC.WIDE_5);
    put("wide_6",             OPC.WIDE_6);
    put("wide_7",             OPC.WIDE_7);
    put("wide_8",             OPC.WIDE_8);
    put("wide_9",             OPC.WIDE_9);
    put("wide_10",            OPC.WIDE_10);
    put("wide_11",            OPC.WIDE_11);
    put("wide_12",            OPC.WIDE_12);
    put("wide_13",            OPC.WIDE_13);
    put("wide_14",            OPC.WIDE_14);
    put("wide_15",            OPC.WIDE_15);
    put("extend_0",           OPC.EXTEND_0);
    put("extend_1",           OPC.EXTEND_1);
    put("extend_2",           OPC.EXTEND_2);
    put("extend_3",           OPC.EXTEND_3);
    put("extend_4",           OPC.EXTEND_4);
    put("extend_5",           OPC.EXTEND_5);
    put("extend_6",           OPC.EXTEND_6);
    put("extend_7",           OPC.EXTEND_7);
    put("extend_8",           OPC.EXTEND_8);
    put("extend_9",           OPC.EXTEND_9);
    put("extend_10",          OPC.EXTEND_10);
    put("extend_11",          OPC.EXTEND_11);
    put("extend_12",          OPC.EXTEND_12);
    put("extend_13",          OPC.EXTEND_13);
    put("extend_14",          OPC.EXTEND_14);
    put("extend_15",          OPC.EXTEND_15);
    put("wide_half",          OPC.WIDE_HALF);
    put("wide_full",          OPC.WIDE_FULL);
    put("longOp",             OPC.LONGOP);
    put("floatOp",            OPC.FLOATOP);
    put("const_m1",           OPC.CONST_M1);
    put("const_null",         OPC.CONST_NULL);
    put("iadd",               OPC.IADD);
    put("isub",               OPC.ISUB);
    put("iand",               OPC.IAND);
    put("ior",                OPC.IOR);
    put("ixor",               OPC.IXOR);
    put("ishl",               OPC.ISHL);
    put("ishr",               OPC.ISHR);
    put("iushr",              OPC.IUSHR);
    put("imul",               OPC.IMUL);
    put("idiv",               OPC.IDIV);
    put("irem",               OPC.IREM);
    put("return",             OPC.RETURN);
    put("throw",              OPC.THROW);
    put("bpt",                OPC.BPT);
    put("nop",                OPC.NOP);
    put("pop",                OPC.POP);
    put("neg",                OPC.NEG);
    put("i2b",                OPC.I2B);
    put("i2s",                OPC.I2S);
    put("i2c",                OPC.I2C);
    put("lookup",             OPC.LOOKUP);
    put("tableswitch",        OPC.TABLESWITCH);
    put("stableswitch",       OPC.STABLESWITCH);
    put("monitorenter",       OPC.MONITORENTER);
    put("monitorexit",        OPC.MONITOREXIT);
    put("class_monitorenter", OPC.CLASS_MONITORENTER);
    put("class_monitorexit",  OPC.CLASS_MONITOREXIT);
    put("arraylength",        OPC.ARRAYLENGTH);
    put("clinit",             OPC.CLINIT);
    put("newarray",           OPC.NEWARRAY);
    put("newdimension",       OPC.NEWDIMENSION);
    put("instanceof",         OPC.INSTANCEOF);
    put("checkcast",          OPC.CHECKCAST);
    put("eq",                 OPC.EQ);
    put("lt",                 OPC.LT);
    put("le",                 OPC.LE);
    put("ne",                 OPC.NE);
    put("gt",                 OPC.GT);
    put("ge",                 OPC.GE);
    put("aload",              OPC.ALOAD);
    put("astore",             OPC.ASTORE);
    put("const_byte",         OPC.CONST_BYTE);
    put("const_short",        OPC.CONST_SHORT);
    put("const_char",         OPC.CONST_CHAR);
    put("const_int",          OPC.CONST_INT);
    put("const_long",         OPC.CONST_LONG);
    put("const_float",        OPC.CONST_FLOAT);
    put("const_double",       OPC.CONST_DOUBLE);
    put("object",             OPC.OBJECT);
    put("class",              OPC.CLASS);
    put("load",               OPC.LOAD);
    put("store",              OPC.STORE);
    put("extend",             OPC.EXTEND);
    put("inc",                OPC.INC);
    put("dec",                OPC.DEC);
    put("invokeinit",         OPC.INVOKEINIT);
    put("invokeinterface",    OPC.INVOKEINTERFACE);
    put("invokestatic",       OPC.INVOKESTATIC);
    put("invokesuper",        OPC.INVOKESUPER);
    put("invokevirtual",      OPC.INVOKEVIRTUAL);
    put("invokenative",       OPC.INVOKENATIVE);
    put("getstatic",          OPC.GETSTATIC);
    put("putstatic",          OPC.PUTSTATIC);
    put("class_getstatic",    OPC.CLASS_GETSTATIC);
    put("class_putstatic",    OPC.CLASS_PUTSTATIC);
    put("getfield",           OPC.GETFIELD);
    put("putfield",           OPC.PUTFIELD);
    put("this_getfield",      OPC.THIS_GETFIELD);
    put("this_putfield",      OPC.THIS_PUTFIELD);
    put("ifeq",               OPC.IFEQ);
    put("ifne",               OPC.IFNE);
    put("iflt",               OPC.IFLT);
    put("ifle",               OPC.IFLE);
    put("ifgt",               OPC.IFGT);
    put("ifge",               OPC.IFGE);
    put("if_icmpeq",          OPC.IF_ICMPEQ);
    put("if_icmpne",          OPC.IF_ICMPNE);
    put("if_icmplt",          OPC.IF_ICMPLT);
    put("if_icmple",          OPC.IF_ICMPLE);
    put("if_icmpgt",          OPC.IF_ICMPGT);
    put("if_icmpge",          OPC.IF_ICMPGE);
    put("goto",               OPC.GOTO);
    put("methodid",           OPC.METHODID);
    put("invokeinit1",        OPC.INVOKEINIT1);
    put("invokeinterface1",   OPC.INVOKEINTERFACE1);
    put("invokestatic1",      OPC.INVOKESTATIC1);
    put("invokesuper1",       OPC.INVOKESUPER1);
    put("invokevirtual1",     OPC.INVOKEVIRTUAL1);
    put("invokenative1",      OPC.INVOKENATIVE1);
    put("invokeinit2",        OPC.INVOKEINIT2);
    put("invokeinterface2",   OPC.INVOKEINTERFACE2);
    put("invokestatic2",      OPC.INVOKESTATIC2);
    put("invokesuper2",       OPC.INVOKESUPER2);
    put("invokevirtual2",     OPC.INVOKEVIRTUAL2);
    put("invokenative2",      OPC.INVOKENATIVE2);
    put("lookup_b",           OPC.LOOKUP_B);
    put("lookup_s",           OPC.LOOKUP_S);
    put("lookup_c",           OPC.LOOKUP_C);
    put("newobject",          OPC.NEWOBJECT);
    put("class_clinit",       OPC.CLASS_CLINIT);
    put("return1",            OPC.RETURN1);
    put("return2",            OPC.RETURN2);
    put("stableswitch_pad",   OPC.STABLESWITCH_PAD);
    put("load_i2",            OPC.LOAD_I2);
    put("store_i2",           OPC.STORE_I2);
    put("aload_b",            OPC.ALOAD_B);
    put("aload_s",            OPC.ALOAD_S);
    put("aload_c",            OPC.ALOAD_C);
    put("aload_i2",           OPC.ALOAD_I2);
    put("astore_b",           OPC.ASTORE_B);
    put("astore_s",           OPC.ASTORE_S);
    put("astore_o",           OPC.ASTORE_O);
    put("astore_i2",          OPC.ASTORE_I2);
    put("getstatic_o",        OPC.GETSTATIC_O);
    put("getstatic_i2",       OPC.GETSTATIC_I2);
    put("class_getstatic_o",  OPC.CLASS_GETSTATIC_O);
    put("class_getstatic_i2", OPC.CLASS_GETSTATIC_I2);
    put("putstatic_o",        OPC.PUTSTATIC_O);
    put("putstatic_i2",       OPC.PUTSTATIC_I2);
    put("class_putstatic_o",  OPC.CLASS_PUTSTATIC_O);
    put("class_putstatic_i2", OPC.CLASS_PUTSTATIC_I2);
    put("getfield_b",         OPC.GETFIELD_B);
    put("getfield_s",         OPC.GETFIELD_S);
    put("getfield_c",         OPC.GETFIELD_C);
    put("getfield_i2",        OPC.GETFIELD_I2);
    put("putfield_b",         OPC.PUTFIELD_B);
    put("putfield_s",         OPC.PUTFIELD_S);
    put("putfield_o",         OPC.PUTFIELD_O);
    put("putfield_i2",        OPC.PUTFIELD_I2);
    put("this_getfield_b",    OPC.THIS_GETFIELD_B);
    put("this_getfield_s",    OPC.THIS_GETFIELD_S);
    put("this_getfield_c",    OPC.THIS_GETFIELD_C);
    put("this_getfield_i2",   OPC.THIS_GETFIELD_I2);
    put("this_putfield_b",    OPC.THIS_PUTFIELD_B);
    put("this_putfield_s",    OPC.THIS_PUTFIELD_S);
    put("this_putfield_o",    OPC.THIS_PUTFIELD_O);
    put("this_putfield_i2",   OPC.THIS_PUTFIELD_I2);
    put("ladd",               OPC.LADD);
    put("lsub",               OPC.LSUB);
    put("lmul",               OPC.LMUL);
    put("ldiv",               OPC.LDIV);
    put("lrem",               OPC.LREM);
    put("land",               OPC.LAND);
    put("lor",                OPC.LOR);
    put("lxor",               OPC.LXOR);
    put("lneg",               OPC.LNEG);
    put("lshl",               OPC.LSHL);
    put("lshr",               OPC.LSHR);
    put("lushr",              OPC.LUSHR);
    put("lcmp",               OPC.LCMP);
    put("l2i",                OPC.L2I);
    put("i2l",                OPC.I2L);
    put("fadd",               OPC.FADD);
    put("fsub",               OPC.FSUB);
    put("fmul",               OPC.FMUL);
    put("fdiv",               OPC.FDIV);
    put("frem",               OPC.FREM);
    put("fneg",               OPC.FNEG);
    put("fcmpg",              OPC.FCMPG);
    put("fcmpl",              OPC.FCMPL);
    put("dadd",               OPC.DADD);
    put("dsub",               OPC.DSUB);
    put("dmul",               OPC.DMUL);
    put("ddiv",               OPC.DDIV);
    put("drem",               OPC.DREM);
    put("dneg",               OPC.DNEG);
    put("dcmpg",              OPC.DCMPG);
    put("dcmpl",              OPC.DCMPL);
    put("i2f",                OPC.I2F);
    put("l2f",                OPC.L2F);
    put("f2i",                OPC.F2I);
    put("f2l",                OPC.F2L);
    put("i2d",                OPC.I2D);
    put("l2d",                OPC.L2D);
    put("f2d",                OPC.F2D);
    put("d2i",                OPC.D2I);
    put("d2l",                OPC.D2L);
    put("d2f",                OPC.D2F);
    }
}
