import java.util.Vector;
import java.util.Enumeration;

public class CodeGen {

    public static Option option;

    public static int next        = 0;
    public static boolean isLong  = false;
    public static boolean isFloat = false;

    public static StringBuffer instructionLengths = new StringBuffer();
    public static StringBuffer instructionStackEffects = new StringBuffer();
    public static StringBuffer floatInstructionLengths = new StringBuffer();
    public static StringBuffer floatInstructionStackEffects = new StringBuffer();
    public static StringBuffer longInstructionLengths = new StringBuffer();
    public static StringBuffer longInstructionStackEffects = new StringBuffer();

    public static void main(String[] args) throws Exception {

        int optionNo = 0;
        if (args.length > 0) {
            optionNo = Integer.parseInt(args[0]);
        }

       /*
        * Options:
        *
        * 0 - Make for spec
        * 1 - Make OPC.java
        * 2 - Make TagLookup.java
        * 3 - Make MNEMONICS.java
        * 4 - Make ClassNumbers.java
        */

        String extend = "extend";

        switch(optionNo) {
            case 0: option = OPTION_0; extend = "reserved"; break;
            case 1: option = OPTION_1; break;
            case 2: option = OPTION_2; break;
            case 3: option = OPTION_3; break;
            default: throw new RuntimeException("unknown option: " + optionNo);
        }
        option.preamble();

        // Bytecodes containing operand


        //  name,    length in bytes,   stack change in words

        make16("const",             1,  1);
        make16("object",            1,  1);
        make16("class",             1,  1);
        make16("load",              1,  1);
        make16("store",             1, -1);
        make16("wide",              0,  0);
        //make16("getfield_i");
        //make16("this_getfield_i");
        make16(extend,              1,  0);

        // Shift bytecodes

        make("wide_half",           0,  0);
        make("wide_full",           0,  0);
        make("longOp",              0,  0);
        make("floatOp",             0,  0);

        // Simple bytecodes

        make("const_m1",            1,  1);
        make("const_null",          1,  1);
        make("iadd",                1, -1);
        make("isub",                1, -1);
        make("iand",                1, -1);
        make("ior",                 1, -1);
        make("ixor",                1, -1);
        make("ishl",                1, -1);
        make("ishr",                1, -1);
        make("iushr",               1, -1);
        make("imul",                1, -1);
        make("idiv",                1, -1);
        make("irem",                1, -1);
        make("return",              1,  0);
        make("throw",               1, -1);
        make("bpt",                 1,  0);
        make("nop",                 1,  0);
        make("pop",                 1, -1);
        make("neg",                 1,  0);
        make("i2b",                 1,  0);
        make("i2s",                 1,  0);
        make("i2c",                 1,  0);
        make("lookup",              1, -1);
        make("tableswitch",         0, -1);
        make("stableswitch",        0, -1);
        make("monitorenter",        1, -1);
        make("monitorexit",         1, -1);
        make("class_monitorenter",  1,  0);
        make("class_monitorexit",   1,  0);
        make("arraylength",         1,  0);
        make("clinit",              1, -1);
        make("newarray",            1, -1);
        make("newdimension",        1, -1);
        make("instanceof",          1, -1);
        make("checkcast",           1, -1);
        make("eq",                  1, -1);
        make("lt",                  1, -1);
        make("le",                  1, -1);
        make("ne",                  1, -1);
        make("gt",                  1, -1);
        make("ge",                  1, -1);
        make("aload",               1, -1);
        make("astore",              1, -3);

        // Bytecodes with operand in succeeding value

        make("const_byte",          2,  1);
        make("const_short",         3,  1);
        make("const_char",          3,  1);
        make("const_int",           5,  1);
        make("const_long",          9,  2);
        make("const_float",         5,  1);
        make("const_double",        9,  2);


        // Bytecodes with operand in succeeding unsigned byte that can be shifted with wide_n, wide_half or wide_full

        make("object",              2,  1);
        make("class",               2,  1);
        make("load",                2,  1);
        make("store",               2, -1);
        make(extend,                2,  0);

        make("inc",                 2,  0);
        make("dec",                 2,  0);

        make("invokeinit",          2, -100);
        make("invokeinterface",     2, -100);
        make("invokestatic",        2, -100);
        make("invokesuper",         2, -100);
        make("invokevirtual",       2, -100);
        make("invokenative",        2, -100);


        make("getstatic",           2,  0);
        make("putstatic",           2, -2);
        make("class_getstatic",     2,  1);
        make("class_putstatic",     2, -1);

        make("getfield",            2,  0);
        make("putfield",            2, -2);
        make("this_getfield",       2,  1);
        make("this_putfield",       2, -1);


        // Bytecodes with operand in succeeding signed byte that can be shifted with wide_n, wide_half or wide_full

        make("ifeq",                2, -1);
        make("ifne",                2, -1);
        make("iflt",                2, -1);
        make("ifle",                2, -1);
        make("ifgt",                2, -1);
        make("ifge",                2, -1);
        make("if_icmpeq",           2, -2);
        make("if_icmpne",           2, -2);
        make("if_icmplt",           2, -2);
        make("if_icmple",           2, -2);
        make("if_icmpgt",           2, -2);
        make("if_icmpge",           2, -2);
        make("goto",                2,  0);


        // Extensions

        makex("methodid",           5,  0);

        makex("invokeinit1",        2,  -101);
        makex("invokeinterface1",   2,  -101);
        makex("invokestatic1",      2,  -101);
        makex("invokesuper1",       2,  -101);
        makex("invokevirtual1",     2,  -101);
        makex("invokenative1",      2,  -101);

        makex("invokeinit2",        2,  -102);
        makex("invokeinterface2",   2,  -102);
        makex("invokestatic2",      2,  -102);
        makex("invokesuper2",       2,  -102);
        makex("invokevirtual2",     2,  -102);
        makex("invokenative2",      2,  -102);

        make("lookup_b",            1, -1);
        make("lookup_s",            1, -1);
        make("lookup_c",            1, -1);

        makex("newobject",          1,  0);
        makex("class_clinit",       1,  0);

        makex("return1",            1, -1);
        makex("return2",            1, -2);
        makex("stableswitch_pad",   0, -1);

        makex("load_i2",            2,  2);
        makex("store_i2",           2, -2);

        makex("aload_b",            1, -1);
        makex("aload_s",            1, -1);
        makex("aload_c",            1, -1);
        makex("aload_i2",           1,  0);

        makex("astore_b",           1, -3);
        makex("astore_s",           1, -3);
        makex("astore_o",           1, -3);
        makex("astore_i2",          1, -4);

        makex("getstatic_o",        2,  0);
        makex("getstatic_i2",       2,  1);

        makex("class_getstatic_o",  2,  1);
        makex("class_getstatic_i2", 2,  2);

        makex("putstatic_o",        2, -2);
        makex("putstatic_i2",       2, -3);

        makex("class_putstatic_o",  2, -1);
        makex("class_putstatic_i2", 2, -2);

        makex("getfield_b",         2,  0);
        makex("getfield_s",         2,  0);
        makex("getfield_c",         2,  0);
        makex("getfield_i2",        2,  1);

        makex("putfield_b",         2, -2);
        makex("putfield_s",         2, -2);
        makex("putfield_o",         2, -2);
        makex("putfield_i2",        2, -3);

        makex("this_getfield_b",    2,  1);
        makex("this_getfield_s",    2,  1);
        makex("this_getfield_c",    2,  1);
        makex("this_getfield_i2",   2,  2);

        makex("this_putfield_b",    2, -1);
        makex("this_putfield_s",    2, -1);
        makex("this_putfield_o",    2, -1);
        makex("this_putfield_i2",   2, -2);

        next   = 0;

        isLong = true;
        makelong("ladd",            1, -2);
        makelong("lsub",            1, -2);
        makelong("lmul",            1, -2);
        makelong("ldiv",            1, -2);
        makelong("lrem",            1, -2);
        makelong("land",            1, -2);
        makelong("lor",             1, -2);
        makelong("lxor",            1, -2);
        makelong("lneg",            1,  0);
        makelong("lshl",            1, -1);
        makelong("lshr",            1, -1);
        makelong("lushr",           1, -1);
        makelong("lcmp",            1, -3);
        makelong("l2i",             1, -1);
        makelong("i2l",             1,  1);
        isLong = false;

        next = 0;

        isFloat = true;
        makefloat("fadd",           1, -1);
        makefloat("fsub",           1, -1);
        makefloat("fmul",           1, -1);
        makefloat("fdiv",           1, -1);
        makefloat("frem",           1, -1);
        makefloat("fneg",           1,  0);
        makefloat("fcmpg",          1, -1);
        makefloat("fcmpl",          1, -1);
        makefloat("dadd",           1, -2);
        makefloat("dsub",           1, -2);
        makefloat("dmul",           1, -2);
        makefloat("ddiv",           1, -2);
        makefloat("drem",           1, -2);
        makefloat("dneg",           1,  0);
        makefloat("dcmpg",          1, -3);
        makefloat("dcmpl",          1, -3);
        makefloat("i2f",            1,  0);
        makefloat("l2f",            1, -1);
        makefloat("f2i",            1,  0);
        makefloat("f2l",            1,  1);
        makefloat("i2d",            1,  1);
        makefloat("l2d",            1,  0);
        makefloat("f2d",            1,  1);
        makefloat("d2i",            1, -1);
        makefloat("d2l",            1,  0);
        makefloat("d2f",            1, -1);
        isFloat = false;

        option.postamble();
    }


    static void make16(String name, int x, int y) {
        for (int i = 0 ; i < 16 ; i++) {
            make(name+"_"+i, x, y);
        }
    }

    static void makex(String name, int x, int y) {
        append(name, x, y, instructionLengths, instructionStackEffects);
        makex(name);
    }

    static void make(String name, int x, int y) {
        append(name, x, y, instructionLengths, instructionStackEffects);
        make(name);
    }

    static void makefloat(String name, int x, int y) {
        append(name, x, y, floatInstructionLengths, floatInstructionStackEffects);
        makefloat(name);
    }

    static void makelong(String name, int x, int y) {
        append(name, x, y, longInstructionLengths, longInstructionStackEffects);
        makelong(name);
    }

    static void append(String name, int length, int stackEffect, StringBuffer lengths, StringBuffer stackEffects) {
        lengths.append((char)length);
        stackEffects.append((char)stackEffect);
    }


    static void makex(String name) {
        if (option != OPTION_0) {
            make(name);
        }
    }

    static void make(String name) {
        String uname = name.toUpperCase();
        option.make(name);
    }

    static void makelong(String name) {
        make(name);
    }

    static void makefloat(String name) {
        make(name);
    }

    static String space30(String name) {
        while(name.length() < 30) {
            name += " ";
        }
        return name;
    }

    static void makeSlot(String name) {
        option.makeSlot(name);
    }

    static void makeSlotAlias(String alias, String name) {
        option.makeSlotAlias(alias, name);
    }

/* ------------------------------------------------------------------------ *\
 *                         Template Option                                  *
\* ------------------------------------------------------------------------ */

static abstract class Option {
    void preamble()  {}
    void postamble() {}
    void make(String name)    {}
    void makeSlot(String name) {}
    void makeSlotAlias(String alias, String name) {}
}



/* ------------------------------------------------------------------------ *\
 *                                  Option 0                                *
\* ------------------------------------------------------------------------ */

    static final Option OPTION_0 = new Option() {
        void make0(String name) {
            System.out.println(space30("    "+name.toUpperCase())+" = "+(next++));
        }
    };

/* ------------------------------------------------------------------------ *\
 *                      Option 1 - "OPC.java"                               *
\* ------------------------------------------------------------------------ */

    static final Option OPTION_1 = new Option() {

        void preamble() {
            System.out.println("//J2C:opc.c **DO NOT EDIT THIS FILE**");
            System.out.println("/*");
            System.out.println(" * Copyright 1994-2002 Sun Microsystems, Inc. All Rights Reserved.");
            System.out.println(" *");
            System.out.println(" * This software is the proprietary information of Sun Microsystems, Inc.");
            System.out.println(" * Use is subject to license terms.");
            System.out.println(" */");
            System.out.println("");
            System.out.println("/*IFJ*/package com.sun.squawk.vm;");
            System.out.println("/*IFJ*/public final class OPC {");
            System.out.println("//IFC//#if 0");
            System.out.println();
            System.out.println("    public final static int");
        }

        void make(String name) {
            System.out.println(space30("        "+name.toUpperCase())+" = "+(next++)+",");
        }

        void postamble() {
            System.out.println("    DUMMY=0");
            System.out.println(";");
            System.out.println("");
            System.out.println("//IFC//#endif");

            System.out.println("//IFC//#ifdef TRACING");
            System.out.println("//IFC//public static final String LENGTH_TABLE = " + makeString(instructionLengths, true));
            System.out.println("//IFC//#endif /* TRACING */");
            System.out.println("/*IFJ*/public static final String LENGTH_TABLE = " + makeString(instructionLengths, false));
            System.out.println("/*IFJ*/public static final String STACK_EFFECT_TABLE = " + makeString(instructionStackEffects, false));
            System.out.println("/*IFJ*/public static final String LONG_LENGTH_TABLE = " + makeString(longInstructionLengths, false));
            System.out.println("/*IFJ*/public static final String LONG_STACK_EFFECT_TABLE = " + makeString(longInstructionStackEffects, false));
            System.out.println("/*IFJ*/public static final String FLOAT_LENGTH_TABLE = " + makeString(floatInstructionLengths, false));
            System.out.println("/*IFJ*/public static final String FLOAT_STACK_EFFECT_TABLE = " + makeString(floatInstructionStackEffects, false));

            System.out.println("");
            System.out.println("/*IFJ*/}");
        }

        String makeString(StringBuffer b, boolean C) {
            String s = b.toString();
 //System.err.println("s.length()="+s.length());
            b = new StringBuffer();
            b.append('"');
            for (int i = 0 ; i < s.length() ; i++) {
                char ch = s.charAt(i);
                char converted = (char)((byte)ch);
                if (converted != ch) {
                    throw new RuntimeException("Adding a non-byte sized char to a StringOfBytes table");
                }
                b.append(C ? "\\x" : "\\u00");
                b.append(hex(ch>>4));
                b.append(hex(ch>>0));
            }
            b.append('"');
            b.append(';');
            return b.toString();
        }

        char hex(int i) {
            String hextable = "0123456789abcdef";
            return (char)hextable.charAt(i&0xF);
        }

    };


/* ------------------------------------------------------------------------ *\
 *                         Option 2 - "TagLookup.java"                      *
\* ------------------------------------------------------------------------ */

    static final Option OPTION_2 = new Option() {
        void preamble() {
            System.out.println("//if[DEBUG.LOADER]   /* This will selectively exclude the entire file from the build */");
            System.out.println("//**DO NOT EDIT THIS FILE**");
            System.out.println("/*");
            System.out.println(" * Copyright 1994-2002 Sun Microsystems, Inc. All Rights Reserved.");
            System.out.println(" *");
            System.out.println(" * This software is the proprietary information of Sun Microsystems, Inc.");
            System.out.println(" * Use is subject to license terms.");
            System.out.println(" */");
            System.out.println("");
            System.out.println("package com.sun.squawk.suite;");
            System.out.println("import com.sun.squawk.vm.OPC;");
            System.out.println("import com.sun.squawk.vm.SquawkConstants;");
            System.out.println("public class TagLookup extends TagLookupBase {");
            System.out.println("");
            System.out.println("static {");
            System.out.println("    put(\"public\",            -SquawkConstants.ACC_PUBLIC);");
            System.out.println("    put(\"private\",           -SquawkConstants.ACC_PRIVATE);");
            System.out.println("    put(\"protected\",         -SquawkConstants.ACC_PROTECTED);");
            System.out.println("    put(\"final\",             -SquawkConstants.ACC_FINAL);");
            System.out.println("    put(\"volatile\",          -SquawkConstants.ACC_VOLATILE);");
            System.out.println("    put(\"transient\",         -SquawkConstants.ACC_TRANSIENT);");
            System.out.println("    put(\"interface\",         -SquawkConstants.ACC_INTERFACE);");
            System.out.println("    put(\"abstract\",          -SquawkConstants.ACC_ABSTRACT);");
            System.out.println("    put(\"native\",            -SquawkConstants.ACC_NATIVE);");
            System.out.println("    put(\"init\",              -SquawkConstants.ACC_INIT);");
            System.out.println("    put(\"static\",            -SquawkConstants.ACC_STATIC);");
            System.out.println("    put(\"proxy\",             -SquawkConstants.ACC_PROXY);");
            System.out.println("    put(\"symbolic\",          -SquawkConstants.ACC_SYMBOLIC);");
        }

        void make(String name) {
            System.out.println(space30("    put(\""+name+"\",")+"OPC."+name.toUpperCase()+");");
        }

        void postamble() {
            System.out.println("    }");
            System.out.println("}");
        }
    };

/* ------------------------------------------------------------------------ *\
 *                       Option 3 - "MNEMONICS.java"                        *
\* ------------------------------------------------------------------------ */

    static final Option OPTION_3 = new Option() {

        Vector mnemonics = new Vector();
        Vector floatMnemonics = new Vector();
        Vector longMnemonics = new Vector();
        int n = 0;

        void preamble() {
            System.out.println("//J2C:mnemonic.c **DO NOT EDIT THIS FILE**");
            System.out.println("/*");
            System.out.println(" * Copyright 1994-2002 Sun Microsystems, Inc. All Rights Reserved.");
            System.out.println(" *");
            System.out.println(" * This software is the proprietary information of Sun Microsystems, Inc.");
            System.out.println(" * Use is subject to license terms.");
            System.out.println(" */");
            System.out.println("");
            System.out.println("/*IFJ*/package com.sun.squawk.vm;");
            System.out.println("/*IFJ*/public final class Mnemonics {");
            System.out.println("");
        }

        void make(String name) {
//            String line = ("        \""+name+" "+(n++)+"\"");
            String line = ("        \""+name+"\"");

            if (isLong) {
                longMnemonics.addElement(line);
            }
            else if (isFloat) {
                floatMnemonics.addElement(line);
            }
            else {
                mnemonics.addElement(line);
            }
        }

        void printMnemonics(Vector mnemonics, String name) {
            if (mnemonics.size() != 0) {
                System.out.println("//IFC//#ifdef TRACING");
                System.out.println("    public final static String[] " + name + " = {");
                for (Enumeration e = mnemonics.elements(); e.hasMoreElements(); ) {
                    System.out.print(e.nextElement());
                    if (e.hasMoreElements()) {
                        System.out.println(",");
                    }
                    else {
                        System.out.println();
                    }
                }
                System.out.println("    };");
                System.out.println("//IFC//#endif");
            }

        }

        void postamble() {
            printMnemonics(mnemonics,      "OPCODES");
            printMnemonics(floatMnemonics, "FLOAT_OPCODES");
            printMnemonics(longMnemonics,  "LONG_OPCODES");
            System.out.println("/*IFJ*/}");
        }
    };
}
