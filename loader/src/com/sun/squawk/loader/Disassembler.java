//if[DEBUG.LOADER]   /* This will selectively exclude the entire file from the build */

package com.sun.squawk.loader;
import java.io.*;
import com.sun.squawk.suite.*;
import com.sun.squawk.vm.OPC;
import com.sun.squawk.vm.Mnemonics;
import com.sun.squawk.vm.SquawkConstants;

class Disassembler {

    DataInputStream in;
    StringBuffer sb;
    int ip;
    int sp = 0;
    int startSp = 0;

    /*
     * DisasmError
     */
    static int DisasmError() {
        throw new RuntimeException("I/O Error reading bytecodes\n");
    }


    /*
     * getMnemonic
     */
    static String getMnemonic(int n) {
//        return mnemonics[n] + " ("+n+")";
        return Mnemonics.OPCODES[n];
    }

    /*
     * read
     */
    int read() {
        try {
            return in.read();
        } catch  (IOException ex) {
            return DisasmError();
        }
    }

    /*
     * readShort
     */
    int readShort() {
        try {
            return in.readShort();
        } catch  (IOException ex) {
            return DisasmError();
        }
    }

    /*
     * readChar
     */
    int readChar() {
        try {
            return in.readChar();
        } catch  (IOException ex) {
            return DisasmError();
        }
    }

    /*
     * readInt
     */
    int readInt() {
        try {
            return in.readInt();
        } catch  (IOException ex) {
            return DisasmError();
        }
    }

/*if[LONGS]*/
    /*
     * readLong
     */
    long readLong() {
        try {
            return in.readLong();
        } catch  (IOException ex) {
            return DisasmError();
        }
    }
/*end[LONGS]*/

/*if[FLOATS]*/
    /*
     * readFloat
     */
    float readFloat() {
        try {
            return in.readFloat();
        } catch  (IOException ex) {
            return DisasmError();
        }
    }

    /*
     * readDouble
     */
    double readDouble() {
        try {
            return in.readDouble();
        } catch  (IOException ex) {
            return DisasmError();
        }
    }
/*end[FLOATS]*/

    /*
     * disassemble
     */
    public String disassemble(byte[] bytecodes) {
        if (bytecodes == null) {
            return "";
        }
        sb = new StringBuffer();
        sb.append("Disassembly of "+bytecodes.length+" bytecodes\n");
        try {
            disassemble2(bytecodes);
        } catch (Throwable ex) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(os);
            ex.printStackTrace(ps);
            try { ps.close(); os.close(); } catch (Throwable e) {}
            sb.append(new String(os.toByteArray()));
        }

        return sb.toString();
    }

    /*
     * disassemble
     */
    private void disassemble2(byte[] bytecodes) {
        in = new DataInputStream(new ByteArrayInputStream(bytecodes));
        sp = startSp = 0;
        int b = 0;

       /*
        * Print header
        *
        * struct {
        *     byte headerSize
        *     byte suiteNumber;
        *     byte classNumber;
        *     byte numberOfParms;
        *     byte numberOfLocals;
        *     int  methodID;
        *     byte oopMap[oopMapSize];  // oopMapSize = (numberOfLocals + 7) / 8;
        *     {
        *         half startIp;
        *         half endIp;
        *         half handlerIp;
        *         half catchType;
        *     } exceptionHandlers[exceptionHandlersLength];
        *     byte bytecode[]
        * }
        */

        int headerSize     = read();
        int suiteNumber    = read();
        int classNumber    = read();
        int numberOfParms  = read();
        int numberOfLocals = read();
        int oopMapSize = (numberOfLocals + 7) / 8;

        sb.append("headerSize     = "+headerSize+"\n");
        sb.append("suiteNumber    = "+suiteNumber+"\n");
        sb.append("classNumber    = "+classNumber+"\n");
        sb.append("numberOfParms  = "+numberOfParms+"\n");
        sb.append("numberOfLocals = "+numberOfLocals+"\n");

        for (int i = 0 ; i < oopMapSize ; i++) {
            sb.append("oopMap["+i+"] = "+read()+"\n");
        }

        int hcount = (headerSize - (5 + oopMapSize)) / 8;
        ExceptionHandler[] handlers = new ExceptionHandler[hcount];
        for (int i = 0 ; i < hcount ; i++) {
            ExceptionHandler handler = handlers[i] = new ExceptionHandler(
                (char)readChar(),
                (char)readChar(),
                (char)readChar(),
                (char)readChar());
            sb.append("handler from:"+(int)handler.from+" to:"+(int)handler.to+" entry:"+(int)handler.entryPoint+" type:"+(int)handler.type+"\n");
        }

//System.out.println(sb.toString());

        ip = headerSize;

        while (true) {

           /*
            * Check to see if there is a handler starting at this address
            */
            if (handlers != null) {
                for (int i = 0 ; i < handlers.length ; i++) {
                    ExceptionHandler handler = handlers[i];
                    if (handler.entryPoint == ip) {
                        sp++;;
                        break;
                    }
                }
            }

            startSp = sp;
            b = read();
            if (b != -1) {
                adjustSp(b);
            }

//System.out.println("b="+b);
            if (b <= OPC.EXTEND_15) {
                switch (b>>4) {
                    case OPC.CONST_0 >>4:   write(1, "const_"+(b&15));                          continue;
                    case OPC.OBJECT_0>>4:   write(1, "object_"+(b&15));                         continue;
                    case OPC.CLASS_0 >>4:   write(1, "class_"+(b&15));                          continue;
                    case OPC.LOAD_0  >>4:   write(1, "load_"+(b&15));                           continue;
                    case OPC.STORE_0 >>4:   write(1, "store_"+(b&15));                          continue;
                    case OPC.WIDE_0  >>4:   parmOp(read(), ((b&15)<<8)|read(), 3);              continue;
                    case OPC.EXTEND_0>>4:   write(1, "extend_"+(b&15));                         continue;
                }
            }

            switch (b) {
                case OPC.WIDE_HALF:         parmOp(read(), readChar(), 4);                      continue;
                case OPC.WIDE_FULL:         parmOp(read(), readInt(), 6);                       continue;
/*if[LONGS]*/
                case OPC.LONGOP:            longOp(read());                                     continue;
/*end[LONGS]*/
/*if[FLOATS]*/
                case OPC.FLOATOP:           floatOp(read());                                    continue;
/*end[FLOATS]*/
                case OPC.CONST_M1:          write(1, "const_m1");                               continue;
                case OPC.CONST_NULL:        write(1, "const_null");                             continue;
                case OPC.CONST_BYTE:        write(2, "const_byte\n\t\t.byte "+read());          continue;
                case OPC.CONST_SHORT:       write(3, "const_short\n\t\t.short "+readShort());   continue;
                case OPC.CONST_CHAR:        write(3, "const_char\n\t\t.char "+readChar());      continue;
                case OPC.CONST_INT:         write(5, "const_int\n\t\t.int "+readInt());         continue;
                case OPC.METHODID:          write(5, "methodid\n\t\t.int "+readInt());          continue;

/*if[LONGS]*/
                case OPC.CONST_LONG:        write(9, "const_long\n\t\t.long "+readLong());      continue;
/*end[LONGS]*/
/*if[FLOATS]*/
                case OPC.CONST_FLOAT:       write(5, "const_float\n\t\t.float "+readFloat());   continue;
                case OPC.CONST_DOUBLE:      write(9, "const_double\n\t\t.double "+readDouble());continue;
/*end[FLOATS]*/

                case OPC.STABLESWITCH_PAD:  read(); stableswitch("stableswitch_pad", true);     continue;
                case OPC.STABLESWITCH:              stableswitch("stableswitch", false);        continue;

                case OPC.IADD:
                case OPC.ISUB:
                case OPC.IMUL:
                case OPC.IDIV:
                case OPC.IREM:
                case OPC.IAND:
                case OPC.IOR:
                case OPC.IXOR:
                case OPC.ISHL:
                case OPC.ISHR:
                case OPC.IUSHR:
                case OPC.RETURN:
                case OPC.RETURN1:
                case OPC.RETURN2:
                case OPC.NEWOBJECT:
                case OPC.CLASS_CLINIT:
                case OPC.THROW:
                case OPC.BPT:
                case OPC.NOP:
                case OPC.POP:
                case OPC.NEG:
                case OPC.I2B:
                case OPC.I2S:
                case OPC.I2C:
                case OPC.LOOKUP:
                case OPC.MONITORENTER:
                case OPC.MONITOREXIT:
                case OPC.CLASS_MONITORENTER:
                case OPC.CLASS_MONITOREXIT:
                case OPC.ARRAYLENGTH:
                case OPC.CLINIT:
                case OPC.NEWARRAY:
                case OPC.NEWDIMENSION:
                case OPC.INSTANCEOF:
                case OPC.CHECKCAST:
                case OPC.EQ:
                case OPC.LT:
                case OPC.LE:
                case OPC.NE:
                case OPC.GT:
                case OPC.GE:
                case OPC.ALOAD:
                case OPC.ALOAD_B:
                case OPC.ALOAD_S:
                case OPC.ALOAD_C:
                case OPC.ALOAD_I2:
                case OPC.ASTORE:
                case OPC.ASTORE_B:
                case OPC.ASTORE_S:
                case OPC.ASTORE_O:
                case OPC.ASTORE_I2:         write(1, getMnemonic(b));                           continue;

                default:                    parmOpX(b, read(), 2);                              continue;
                case -1:                    return;
            }
        }
    }

    /*
     * parmOp
     */
    void parmOp(int b, int val, int size) {
        adjustSp(b);
        parmOpX(b, val, size);
    }

/*if[LONGS]*/
    /*
     * longOp
     */
    void longOp(int b) {
        int blength = (byte)OPC.LONG_LENGTH_TABLE.charAt(b);
        int bstack  = (byte)OPC.LONG_STACK_EFFECT_TABLE.charAt(b);
        sp += bstack;
        write(2, "longOp\n\t\t"+Mnemonics.LONG_OPCODES[b]);
    }
/*end[LONGS]*/

/*if[FLOATS]*/
    /*
     * floatOp
     */
    void floatOp(int b) {
        int blength = (byte)OPC.FLOAT_LENGTH_TABLE.charAt(b);
        int bstack  = (byte)OPC.FLOAT_STACK_EFFECT_TABLE.charAt(b);
        sp += bstack;
        write(2, "floatOp\n\t\t"+Mnemonics.FLOAT_OPCODES[b]);
    }
/*end[FLOATS]*/

    /*
     * parmOp
     */
    void parmOpX(int b, int val, int size) {
        switch (b) {
            case OPC.IFEQ:
            case OPC.IFNE:
            case OPC.IFLT:
            case OPC.IFLE:
            case OPC.IFGT:
            case OPC.IFGE:
            case OPC.IF_ICMPEQ:
            case OPC.IF_ICMPNE:
            case OPC.IF_ICMPLT:
            case OPC.IF_ICMPLE:
            case OPC.IF_ICMPGT:
            case OPC.IF_ICMPGE:
            case OPC.GOTO:                  writeSigned(size, getMnemonic(b), val);             return;
            default:                        write(size, getMnemonic(b), val);                   return;
        }

    }

    /*
     * stableswitch
     */
    void stableswitch(String name, boolean pad) {
        StringBuffer sb = new StringBuffer();
        int size = 1;
        sb.append(name);
        if (pad) {
            sb.append("\n\t\t.pad");
            size++;
        }
        int def  = readShort();
        int low  = readInt();
        int high = readInt();
        size += 10;
        int branchPoint = ip+size;
        sb.append("\n\t\t.short "+def+"\t("+(branchPoint+def)+")\t(def)");
        sb.append("\n\t\t.int   "+low+"\t\t(low)");
        sb.append("\n\t\t.int   "+high+"\t\t(high)");
        Romizer.assume(high >= low);
        for (int i = low ; i <= high ; i++) {
            int addr = readShort();
            sb.append("\n\t\t.short "+addr+"\t("+(branchPoint+addr)+")");
            size += 2;
        }
        write(size, sb.toString());
    }

    /*
     * adjustSp
     */
    void adjustSp(int b) {
        int blength = (byte)OPC.LENGTH_TABLE.charAt(b);
        int bstack  = (byte)OPC.STACK_EFFECT_TABLE.charAt(b);
        if (bstack == -100) {
            sp = 0;
        } else if (bstack == -101) {
            sp = 1;
        } else if (bstack == -102) {
            sp = 2;
        } else {
            sp += bstack;
        }
    }


    /*
     * write
     */
    void write(int size, String s) {
        sb.append(""+ip+":\t"+startSp+"\t"+s+"\n");
        ip += size;
    }

    /*
     * write
     */
    void write(int size, String s, int value) {
        write(size, s, value, ".ubyte", ".char", "");
    }

    /*
     * writeSigned
     */
    void writeSigned(int size, String s, int value) {
        switch (size) {
            case 2: value = value << 24 >> 24; break;
            case 3: value = value << 20 >> 20; break;
            case 4: value = value << 16 >> 16; break;
        }
        int target = ip+size+value;
        write(size, s, value, ".byte", ".short", " ("+target+")");
    }

    /*
     * write
     */
    void write(int size, String s, int value, String byt, String half, String extra) {
        switch (size) {
            case 2: write(2, s+"\n\t\t"+byt+" "+value+extra);
                    break;

            case 3: write(3, "wide_"+((value>>8)&15)+"\n\t\t"+s+"\n\t\t"+byt+" "+(value&0xFF)+extra);
                    break;

            case 4: write(4, "wide_half"+"\n\t\t"+s+"\n\t\t"+half+" "+value+extra);
                    break;

            case 6: write(6, "wide_int"+"\n\t\t"+s+"\n\t\t.int "+value+extra);
                    break;

            default:throw new RuntimeException("Too wide");
        }
    }

}

