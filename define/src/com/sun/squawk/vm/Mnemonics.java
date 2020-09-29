//J2C:mnemonic.c **DO NOT EDIT THIS FILE**
/*
 * Copyright 1994-2002 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/public final class Mnemonics {

//IFC//#ifdef TRACING
    public final static String[] OPCODES = {
        "const_0",
        "const_1",
        "const_2",
        "const_3",
        "const_4",
        "const_5",
        "const_6",
        "const_7",
        "const_8",
        "const_9",
        "const_10",
        "const_11",
        "const_12",
        "const_13",
        "const_14",
        "const_15",
        "object_0",
        "object_1",
        "object_2",
        "object_3",
        "object_4",
        "object_5",
        "object_6",
        "object_7",
        "object_8",
        "object_9",
        "object_10",
        "object_11",
        "object_12",
        "object_13",
        "object_14",
        "object_15",
        "class_0",
        "class_1",
        "class_2",
        "class_3",
        "class_4",
        "class_5",
        "class_6",
        "class_7",
        "class_8",
        "class_9",
        "class_10",
        "class_11",
        "class_12",
        "class_13",
        "class_14",
        "class_15",
        "load_0",
        "load_1",
        "load_2",
        "load_3",
        "load_4",
        "load_5",
        "load_6",
        "load_7",
        "load_8",
        "load_9",
        "load_10",
        "load_11",
        "load_12",
        "load_13",
        "load_14",
        "load_15",
        "store_0",
        "store_1",
        "store_2",
        "store_3",
        "store_4",
        "store_5",
        "store_6",
        "store_7",
        "store_8",
        "store_9",
        "store_10",
        "store_11",
        "store_12",
        "store_13",
        "store_14",
        "store_15",
        "wide_0",
        "wide_1",
        "wide_2",
        "wide_3",
        "wide_4",
        "wide_5",
        "wide_6",
        "wide_7",
        "wide_8",
        "wide_9",
        "wide_10",
        "wide_11",
        "wide_12",
        "wide_13",
        "wide_14",
        "wide_15",
        "extend_0",
        "extend_1",
        "extend_2",
        "extend_3",
        "extend_4",
        "extend_5",
        "extend_6",
        "extend_7",
        "extend_8",
        "extend_9",
        "extend_10",
        "extend_11",
        "extend_12",
        "extend_13",
        "extend_14",
        "extend_15",
        "wide_half",
        "wide_full",
        "longOp",
        "floatOp",
        "const_m1",
        "const_null",
        "iadd",
        "isub",
        "iand",
        "ior",
        "ixor",
        "ishl",
        "ishr",
        "iushr",
        "imul",
        "idiv",
        "irem",
        "return",
        "throw",
        "bpt",
        "nop",
        "pop",
        "neg",
        "i2b",
        "i2s",
        "i2c",
        "lookup",
        "tableswitch",
        "stableswitch",
        "monitorenter",
        "monitorexit",
        "class_monitorenter",
        "class_monitorexit",
        "arraylength",
        "clinit",
        "newarray",
        "newdimension",
        "instanceof",
        "checkcast",
        "eq",
        "lt",
        "le",
        "ne",
        "gt",
        "ge",
        "aload",
        "astore",
        "const_byte",
        "const_short",
        "const_char",
        "const_int",
        "const_long",
        "const_float",
        "const_double",
        "object",
        "class",
        "load",
        "store",
        "extend",
        "inc",
        "dec",
        "invokeinit",
        "invokeinterface",
        "invokestatic",
        "invokesuper",
        "invokevirtual",
        "invokenative",
        "getstatic",
        "putstatic",
        "class_getstatic",
        "class_putstatic",
        "getfield",
        "putfield",
        "this_getfield",
        "this_putfield",
        "ifeq",
        "ifne",
        "iflt",
        "ifle",
        "ifgt",
        "ifge",
        "if_icmpeq",
        "if_icmpne",
        "if_icmplt",
        "if_icmple",
        "if_icmpgt",
        "if_icmpge",
        "goto",
        "methodid",
        "invokeinit1",
        "invokeinterface1",
        "invokestatic1",
        "invokesuper1",
        "invokevirtual1",
        "invokenative1",
        "invokeinit2",
        "invokeinterface2",
        "invokestatic2",
        "invokesuper2",
        "invokevirtual2",
        "invokenative2",
        "lookup_b",
        "lookup_s",
        "lookup_c",
        "newobject",
        "class_clinit",
        "return1",
        "return2",
        "stableswitch_pad",
        "load_i2",
        "store_i2",
        "aload_b",
        "aload_s",
        "aload_c",
        "aload_i2",
        "astore_b",
        "astore_s",
        "astore_o",
        "astore_i2",
        "getstatic_o",
        "getstatic_i2",
        "class_getstatic_o",
        "class_getstatic_i2",
        "putstatic_o",
        "putstatic_i2",
        "class_putstatic_o",
        "class_putstatic_i2",
        "getfield_b",
        "getfield_s",
        "getfield_c",
        "getfield_i2",
        "putfield_b",
        "putfield_s",
        "putfield_o",
        "putfield_i2",
        "this_getfield_b",
        "this_getfield_s",
        "this_getfield_c",
        "this_getfield_i2",
        "this_putfield_b",
        "this_putfield_s",
        "this_putfield_o",
        "this_putfield_i2"
    };
//IFC//#endif
//IFC//#ifdef TRACING
    public final static String[] FLOAT_OPCODES = {
        "fadd",
        "fsub",
        "fmul",
        "fdiv",
        "frem",
        "fneg",
        "fcmpg",
        "fcmpl",
        "dadd",
        "dsub",
        "dmul",
        "ddiv",
        "drem",
        "dneg",
        "dcmpg",
        "dcmpl",
        "i2f",
        "l2f",
        "f2i",
        "f2l",
        "i2d",
        "l2d",
        "f2d",
        "d2i",
        "d2l",
        "d2f"
    };
//IFC//#endif
//IFC//#ifdef TRACING
    public final static String[] LONG_OPCODES = {
        "ladd",
        "lsub",
        "lmul",
        "ldiv",
        "lrem",
        "land",
        "lor",
        "lxor",
        "lneg",
        "lshl",
        "lshr",
        "lushr",
        "lcmp",
        "l2i",
        "i2l"
    };
//IFC//#endif
/*IFJ*/}
