package com.sun.squawk.loader;
import java.io.*;
import com.sun.squawk.suite.*;
import com.sun.squawk.vm.OPC;
import com.sun.squawk.vm.CNO;
import com.sun.squawk.vm.Mnemonics;
import com.sun.squawk.vm.SquawkConstants;

/**
 * This is a singleton class which serves the purpose of verifying the
 * bytecode of methods in a suite file and rewriting these bytecodes
 * into their quickened form for efficient execution by the Squawk VM.
 *
 * The format of the byte array containing the verified & quickened
 * bytecode for a method as well as various other details of the
 * method is:
 *
 * Calculate the size of the method header
 *
 * struct {
 *     byte headerSize
 *     byte suiteNumber;
 *     byte classNumber;
 *     byte numberOfParms;
 *     byte numberOfLocals;
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
public class Verifier {

    // Verification error codes.
    private final static int
        VE_BAD_CATCH_VARIABLE               = 1,
        VE_BAD_BYTECODE_BEFORE_HANDLER      = 2,
        VE_UNKNOWN_BYTECODE                 = 3,
        VE_STACK_OVERFLOW                   = 4,
        VE_STACK_UNDERFLOW                  = 5,
        VE_TYPE_MISMATCH                    = 6,
        VE_TOO_WIDE                         = 7,
        VE_BAD_SWITCH                       = 8,
        VE_BAD_ALOAD                        = 9,
        VE_BAD_ASTORE                       = 10,
        VE_BAD_INC                          = 11,
        VE_BAD_DEC                          = 12,
        VE_INVALID_TYPE                     = 13,
        VE_MISSING_INIT                     = 14,
        VE_SP_NOT_ZERO                      = 15,
        VE_TWO_INIT_CALLS                   = 16,
        VE_BAD_WIDE_OP                      = 17,
        VE_BAD_INVOKEINIT                   = 18,
        VE_BAD_JUMPIP                       = 19,
        VE_BAD_JUMPSP                       = 20,
        VE_BAD_LOCAL_TYPE                   = 21,
        VE_BAD_PARM_COUNT                   = 22,
        VE_BRANCH_OVER_INIT                 = 23,
        VE_HEADER_TOO_LARGE                 = 24,
        VE_INTERNAL_ERROR                   = 25,
        VE_NOT_CLASS                        = 26,
        VE_TYPE_NOT_SUPPORTED               = 27,
        VE_BAD_LOOKUP                       = 28,
        VE_TYPE_NATIVE                      = 29,
        VE_NULL                             = 30,
        VE_OFFSETTOOLARGE                   = 31,

        VE_DUMMY=999;

/*if[DEBUG.LOADER]*/
    // Verification error code messages.
    private static String[] VE_ERROR_MESSAGES = {
        "**INVALID**",
        "VE_BAD_CATCH_VARIABLE",
        "VE_BAD_BYTECODE_BEFORE_HANDLER",
        "VE_UNKNOWN_BYTECODE",
        "VE_STACK_OVERFLOW",
        "VE_STACK_UNDERFLOW",
        "VE_TYPE_MISMATCH",
        "VE_TOO_WIDE",
        "VE_BAD_SWITCH",
        "VE_BAD_ALOAD",
        "VE_BAD_ASTORE",
        "VE_BAD_INC",
        "VE_BAD_DEC",
        "VE_INVALID_TYPE",
        "VE_MISSING_INIT",
        "VE_SP_NOT_ZERO",
        "VE_TWO_INIT_CALLS",
        "VE_BAD_WIDE_OP",
        "VE_BAD_INVOKEINIT",
        "VE_BAD_JUMPIP",
        "VE_BAD_JUMPSP",
        "VE_BAD_LOCAL_TYPE",
        "VE_BAD_PARM_COUNT",
        "VE_BRANCH_OVER_INIT",
        "VE_HEADER_TOO_LARGE",
        "VE_INTERNAL_ERROR",
        "VE_NOT_CLASS",
        "VE_TYPE_NOT_SUPPORTED",
        "VE_BAD_LOOKUP",
        "VE_TYPE_NATIVE",
        "VE_NULL",
        "VE_OFFSETTOOLARGE",
    };
/*end[DEBUG.LOADER]*/


    private static ExceptionHandler[] NO_HANDLERS = new ExceptionHandler[0];

    // Instance variables used to track the state of a single verification
    private SuiteLoader loader;
    private SuiteMethodImpl methodImpl;
    private SuiteRelocator relocator;
    private boolean isConstructor;
    private boolean hasReturn;
    private int initCalledAt;
    private char returnType;
    private char[] locals;
    private char[] stack;
    private int sp = 0;
    private ExceptionHandler[] handlers;
    private DataInputStream in;
    private DataOutputStream out;
    private PersistentMemoryOutputStream pmos;
    private int headerSize;
    private int headerAndExtraInstructions;
    private int methodSize;
    private boolean stack0isReceiver;

    Verifier() {}

    /**
     * Re-initalise the state variables of the verifier based on the method
     * about to be verified.
     * @param methodImpl The method about to be verified.
     * @param in The input stream containing the bytecodes of the method.
     * @return the declaration details of the method.
     */
    private Member reinit(SuiteMethodImpl methodImpl) {
        try {
            relocator = (SuiteRelocator)loader.getRelocator(methodImpl.parent);
            if (relocator == null) {
                throw new LinkageError("ClassFormatError: method's 'ofClass' field denotes a non-existent class");
            }
        } catch (ClassCastException cce) {
            throw new LinkageError("ClassFormatError: method's 'ofClass' field denotes a proxy class: "+loader.getRelocator(methodImpl.parent).getName());
        }

        Member method = relocator.getMember(methodImpl.isStatic() ? SquawkConstants.MTYPE_STATICMETHODS : SquawkConstants.MTYPE_VIRTUALMETHODS, methodImpl.entry);
        if (method == null) {
            throw new LinkageError("ClassFormatError: method body does not match a method declaration");
        }

        this.methodImpl       = methodImpl;
        this.isConstructor    = (method.access() & SquawkConstants.ACC_INIT) != 0;

/*if[NEWSTRING]*/
       /*
        * If this is a class in String then force all methods not to be constrictors
        */
        if (loader.suite().number() == 0 && relocator.finalClassNumber() == CNO.STRING) {
            this.isConstructor = false;
        }
/*end[NEWSTRING]*/

        this.hasReturn        = false;
        this.initCalledAt     = -1;
        this.locals           = methodImpl.locals;
        this.returnType       = method.type();
        this.stack            = new char[methodImpl.stackSize()];
        this.handlers         = (methodImpl.handlers == null) ? NO_HANDLERS : methodImpl.handlers;
        this.in               = methodImpl.inputStream;
        this.sp               = 0;
        this.out              = null;
        this.pmos             = null;
        this.headerSize       = 0;
        this.stack0isReceiver = false;

        return method;
    }

    /**
     * Verify a given method and build its in memory representation (as a
     * byte array) and return it. This method also links and quickens the
     * bytecode as it is verified.
     *
     * @param m
     * @return the verified and quickened bytecodes of the method
     * @throws LinkageError if there was a verification error.
     */
    public static void verify(SuiteMethodImpl methodImpl) {
        SuiteLoader l = SuiteLoader.theLoader;
        Verifier v = l.theVerifier;
        v.loader = l;
        SuiteLoader.theLoader.theVerifier.verifyMethod(methodImpl);
        v.loader = null;
    }

    /**
     * This is the private method by which the singleton verifier is invoked.
     */
    private void verifyMethod(SuiteMethodImpl methodImpl) {

        // Re-initialise the member variables of this singleton instance based on
        // the method about to be verified.
        Member method = reinit(methodImpl);

        LinkageError error = null;
        try {
            if (methodImpl.isAbstract()) {
                methodImpl.bytecodes = raiseAbstractMethodError(method);
                return;
            } else if (methodImpl.isNative()) {
                methodImpl.bytecodes = raiseUnsatisifiedLinkError(method);
                return;
            }

            // Widen any boolean, byte, char and short locals to int
            for (int i = 0 ; i < locals.length ; i++) {
                locals[i] = widenToInt(locals[i]);
            }

            // Verify that the parameter types match the local variable types
            verifyParameters(method, methodImpl.isStatic());

            // Set up the output stream and write the header
            preamble(relocator, methodImpl, method);

            // Verify the input bytecodes and write the internal instruction set
            verifyPass1();

            // Close the output stream and get the byte array written
            pmos.close();
            methodImpl.bytecodes = pmos.toByteArray();

            // Verify that a constructor always calls its super constructor unless
            // this is the constructor in Object.
            if (isConstructor && hasReturn) {
                if (initCalledAt == -1) {
                    if (loader.suite().number() != 0 || relocator.finalClassNumber() != CNO.OBJECT) {
                        verifyError(VE_MISSING_INIT, "<init> was not called in constructor");
                    }
                }
            }

            // Verify that all offsets are valid
            verifyPass2(0);

        } catch (LinkageError ex) {
            error = ex;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            error = new LinkageError(ioe.getMessage());
        }

        if (error != null) {
/*if[DEBUG.LOADER]*/
            if (SuiteLoader.debugOut != null) {
                SuiteLoader.debugOut.println("Verification error in suite "+loader.suite().number()+" ("+loader.suite().name+
                                    "), class "+relocator.finalClassNumber()+" ("+relocator.getName()+
                                    "), "+(methodImpl.isStatic() ? "static" : "")+" method "+methodImpl.entry+": "+error);
                SuiteLoader.debugOut.println(new Disassembler().disassemble(methodImpl.bytecodes));
                error.printStackTrace(SuiteLoader.debugOut);
            }
/*end[DEBUG.LOADER]*/
            throw error;
        } else {
            methodImpl.bytecodes = methodImpl.bytecodes;
        }
    }


    /*
     * setBitIfOop
     */
    int setBitIfOop(int bit, char[]local, int position) {
        if (position >= local.length) {
            return 0;
        }
        char t = local[position];
        if (t == CNO.VOID ||
            t == CNO.BOOLEAN ||
            t == CNO.BYTE ||
            t == CNO.CHAR ||
            t == CNO.SHORT) {
            verifyError(VE_BAD_LOCAL_TYPE, loader.getTypeName(t));
        }
        if (SuiteClass.isPrimitiveType(t)) {
            return 0;
        }
        return bit;
    }

/* ------------------------------------------------------------------------ *\
 *                                  Pass 1                                  *
\* ------------------------------------------------------------------------ */

    /*
     * currentIp
     */
    private int currentIp() {
        if (pmos == null) {
            return -1;
        }
        return pmos.size();
    }


    /*
     * verifyPass1
     */
    private void verifyPass1() throws IOException {
        int b = 0;
        while (currentIp() < methodSize) {
           /*
            * Check to see if there is a handler starting at this address
            */
            for (int i = 0 ; i < handlers.length ; i++) {
                ExceptionHandler handler = handlers[i];
                if (handler.entryPoint + headerAndExtraInstructions == currentIp()) {
                    if(!loader.isAssignable(handler.type, CNO.THROWABLE)) {
                        verifyError(VE_BAD_CATCH_VARIABLE, isAssignableErrorMessage(handler.type, CNO.THROWABLE));
                    }
                    if (!(b == OPC.GOTO ||
                          b == OPC.RETURN ||
                          b == OPC.THROW ||
                          b == OPC.STABLESWITCH)) {
                        verifyError(VE_BAD_BYTECODE_BEFORE_HANDLER, Mnemonics.OPCODES[b]);
                    }
                    push(handler.type);
                    break;
                }
            }

           /*
            * Read the next bytecode
            */
            b = in.read();
            Romizer.logInstruction(b);

//if (b != -1)
//System.out.println("************ sp="+sp+" b="+b+" "+mnemonics[b]);

            if (b <= OPC.WIDE_15) {
                int b15 = b & 15;
                switch (b>>4) {
                    case OPC.CONST_0 >>4:   const_n(b15);                                       continue;
                    case OPC.OBJECT_0>>4:   object(b15, 1);                                     continue;
                    case OPC.CLASS_0 >>4:   klass(b15, 1);                                      continue;
                    case OPC.LOAD_0  >>4:   load(b15, 1);                                       continue;
                    case OPC.STORE_0 >>4:   store(b15, 1);                                      continue;
                    case OPC.WIDE_0  >>4:   parmOp(b = in.read(), ((b15)<<8)|in.read(), 3);     continue;
                }
            }

            switch (b) {
                case OPC.WIDE_HALF:         parmOp(b = in.read(), in.readChar(), 4);            continue;
                case OPC.WIDE_FULL:         parmOp(b = in.read(), in.readInt(), 6);             continue;
                case OPC.CONST_M1:          const_m1();                                         continue;
                case OPC.CONST_NULL:        const_null();                                       continue;
                case OPC.CONST_BYTE:        const_byte(in.read());                              continue;
                case OPC.CONST_SHORT:       const_short(in.readShort());                        continue;
                case OPC.CONST_CHAR:        const_char(in.readChar());                          continue;
                case OPC.CONST_INT:         const_int(in.readInt());                            continue;
/*if[LONGS]*/
                case OPC.CONST_LONG:        const_long(in.readLong());                          continue;
                case OPC.LONGOP:            longOp();                                           continue;
/*end[LONGS]*/
/*if[FLOATS]*/
                case OPC.CONST_FLOAT:       const_float(in.readFloat());                        continue;
                case OPC.CONST_DOUBLE:      const_double(in.readDouble());                      continue;
                case OPC.FLOATOP:           floatOp();                                          continue;
/*end[FLOATS]*/
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
                case OPC.IUSHR:             ishift(b);                                          continue;
                case OPC.RETURN:            ret();                                              continue;
                case OPC.THROW:             thro();                                             continue;
                case OPC.BPT:
                case OPC.NOP:               nop(b);                                             continue;
                case OPC.POP:               popp();                                             continue;
                case OPC.NEG:
                case OPC.I2B:
                case OPC.I2S:
                case OPC.I2C:               i2i(b);                                             continue;
                case OPC.LOOKUP:            lookup();                                           continue;
              //case OPC.TABLESWITCH:       tableswitch();                                      continue;
                case OPC.STABLESWITCH:      stableswitch();                                     continue;
                case OPC.MONITORENTER:      monitorenter();                                     continue;
                case OPC.MONITOREXIT:       monitorexit();                                      continue;
                case OPC.CLASS_MONITORENTER:classmonitorenter();                                continue;
                case OPC.CLASS_MONITOREXIT: classmonitorexit();                                 continue;
                case OPC.ARRAYLENGTH:       arraylength();                                      continue;
                case OPC.CLINIT:            clinit();                                           continue;
                case OPC.NEWARRAY:          newarray();                                         continue;
                case OPC.NEWDIMENSION:      newdimension();                                     continue;
                case OPC.INSTANCEOF:        instof();                                           continue;
                case OPC.CHECKCAST:         checkcast();                                        continue;
                case OPC.EQ:
                case OPC.LT:
                case OPC.LE:
                case OPC.NE:
                case OPC.GT:
                case OPC.GE:                relop(b);                                           continue;
                case OPC.ALOAD:             aload();                                            continue;
                case OPC.ASTORE:            astore();                                           continue;
                default:                    parmOp(b, in.read(), 2);                            continue;
            }
        }
    }

    /*
     * parmOp
     */
    private void parmOp(int b, int val, int size) throws IOException {

        // Bytecodes with unsigned parameters

        switch (b) {
            case OPC.OBJECT:                object(val, size);                                  return;
            case OPC.CLASS:                 klass(val, size);                                   return;
            case OPC.LOAD:                  load(val, size);                                    return;
            case OPC.STORE:                 store(val, size);                                   return;
            case OPC.INC:                   inc(val, size);                                     return;
            case OPC.DEC:                   dec(val, size);                                     return;
            case OPC.INVOKEVIRTUAL:         invokevirtual(val, size);                           return;
            case OPC.INVOKESUPER:           invokesuper(val, size);                             return;
            case OPC.INVOKESTATIC:          invokestatic(val, size);                            return;
            case OPC.INVOKEINIT:            invokeinit(val, size);                              return;
            case OPC.INVOKEINTERFACE:       invokeinterface(val, size);                         return;
            case OPC.GETSTATIC:             getstatic(val, size);                               return;
            case OPC.PUTSTATIC:             putstatic(val, size);                               return;
            case OPC.CLASS_GETSTATIC:       classgetstatic(val, size);                          return;
            case OPC.CLASS_PUTSTATIC:       classputstatic(val, size);                          return;
            case OPC.GETFIELD:              getfield(val, size);                                return;
            case OPC.PUTFIELD:              putfield(val, size);                                return;
            case OPC.THIS_GETFIELD:         this_getfield(val, size);                           return;
            case OPC.THIS_PUTFIELD:         this_putfield(val, size);                           return;
        }

        // Bytecodes with signed parameters

        switch (size) {
            case 2: val = val << 24 >> 24; break;
            case 3: val = val << 20 >> 20; break;
            case 4: val = val << 16 >> 16; break;
        }

        switch (b) {
            case OPC.IFEQ:
            case OPC.IFNE:                  if1_any(b, val, size);                              return;
            case OPC.IFLT:
            case OPC.IFLE:
            case OPC.IFGT:
            case OPC.IFGE:                  if1(b, val, size);                                  return;
            case OPC.IF_ICMPEQ:
            case OPC.IF_ICMPNE:             if2_any(b, val, size);                              return;
            case OPC.IF_ICMPLT:
            case OPC.IF_ICMPLE:
            case OPC.IF_ICMPGT:
            case OPC.IF_ICMPGE:             if2(b, val, size);                                  return;
            case OPC.GOTO:                  go2(b, val, size);                                  return;
        }
        verifyError(VE_UNKNOWN_BYTECODE, ""+b);
    }


    /*
     * widenToInt
     */
    private char widenToInt(char type) {
        if (type == CNO.BOOLEAN ||
            type == CNO.BYTE ||
            type == CNO.SHORT ||
            type == CNO.CHAR) {
            return CNO.INT;
        }
        return type;
    }

    /*
     * push
     */
    private void push(char type) {
        if (sp >= stack.length) {
            verifyError(VE_STACK_OVERFLOW, "Stack overflow");
        }
//System.out.println("PUSH "+(int)type);
        stack[sp++] = type;
    }

    /*
     * pop
     */
    private char pop() {
        if (sp <= 0) {
            verifyError(VE_STACK_UNDERFLOW, "Stack underflow");
        }
        if (sp == 1) {
            stack0isReceiver = false;
        }
//System.out.println("POP "+(int)stack[sp-1]);
        return stack[--sp];
    }

    /*
     * pop
     */
    private void pop(char type) {
        type = widenToInt(type);
        char t = pop();
        if (!loader.isAssignable(t, type)) {
            verifyError(VE_TYPE_MISMATCH, isAssignableErrorMessage(t, type));
        }

    }

    /*
     * pushClassInfo
     */
    private void pushClassInfo(char cls) {
        push((char)(cls|0x8000));
    }

    /*
     * popClassInfo
     */
    private char popClassInfo() {
        char t = pop();
        if ((t&0x8000) == 0) {
            verifyError(VE_NOT_CLASS, loader.getTypeName(t&0x7FFF));
        }
        return (char)(t&0x7FFF);
    }


    /*
     * const_n
     */
    private void const_n(int value) throws IOException {
        push(CNO.INT);
        out.write(OPC.CONST_0+value);
    }

    /*
     * const_m1
     */
    private void const_m1() throws IOException {
        push(CNO.INT);
        out.write(OPC.CONST_M1);
    }

    /*
     * const_null
     */
    private void const_null() throws IOException {
        push(CNO.NULL);
        out.write(OPC.CONST_0);
    }

    /*
     * const_byte
     */
    private void const_byte(int value) throws IOException {
        push(CNO.INT);
        out.write(OPC.CONST_BYTE);
        out.write(value);
    }

    /*
     * const_char
     */
    private void const_char(int value) throws IOException {
        push(CNO.INT);
        out.write(OPC.CONST_CHAR);
        out.writeChar(value);
    }

    /*
     * const_short
     */
    private void const_short(int value) throws IOException {
        push(CNO.INT);
        out.write(OPC.CONST_SHORT);
        out.writeShort(value);
    }

    /*
     * const_int
     */
    private void const_int(int value) throws IOException {
        push(CNO.INT);
        out.write(OPC.CONST_INT);
        out.writeInt(value);
    }

/*if[LONGS]*/
    /*
     * const_long
     */
    private void const_long(long value) throws IOException {
        push(CNO.LONG);
        push(CNO.LONG2);
        out.write(OPC.CONST_LONG);
        out.writeLong(value);
    }
/*end[LONGS]*/

/*if[FLOATS]*/
    /*
     * const_float
     */
    private void const_float(float value) throws IOException {
        push(CNO.FLOAT);
        out.write(OPC.CONST_FLOAT);
        out.writeFloat(value);
    }

    /*
     * const_double
     */
    private void const_double(double value) throws IOException {
        push(CNO.DOUBLE);
        push(CNO.DOUBLE2);
        out.write(OPC.CONST_DOUBLE);
        out.writeDouble(value);
    }
/*end[FLOATS]*/

    /*
     * extend
     */
    private void extend(int size) throws IOException {
        if (size <= 15) {
            out.write(OPC.EXTEND_0+size);
        } else {
            if (size > 255) {
                verifyError(VE_TOO_WIDE, "Too wide");
            }
            out.write(OPC.EXTEND);
            out.write(size);
        }
    }

    /*
     * object
     */
    private void object(int index, int size) throws IOException {
        Object obj = relocator.getObjectReference(index);
        if      (obj instanceof String)  push(CNO.STRING);
        else if (obj instanceof int[])   push(CNO.INT_ARRAY);
        else if (obj instanceof short[]) push(CNO.SHORT_ARRAY);
        else if (obj instanceof char[])  push(CNO.CHAR_ARRAY);
        else if (obj instanceof byte[])  push(CNO.BYTE_ARRAY);
        else                             push(CNO.OBJECT);


        switch (size) {
            case 1: out.write(OPC.OBJECT_0+index);
                    break;

            case 2: out.write(OPC.OBJECT);
                    out.write(index);
                    break;

            case 3: out.write(OPC.WIDE_0+((index>>8)&15));
                    out.write(OPC.OBJECT);
                    out.write(index&0xFF);
                    break;

            case 4: out.write(OPC.WIDE_HALF);
                    out.write(OPC.OBJECT);
                    out.writeChar(index);
                    break;

            default: {
                verifyError(VE_TOO_WIDE, "Too wide");
            }
        }
    }

    /*
     * klass
     */
    private void klass(int index, int size) throws IOException {
        pushClassInfo(relocator.getClassReference(index));
        switch (size) {
            case 1: out.write(OPC.CLASS_0+index);
                    break;

            case 2: out.write(OPC.CLASS);
                    out.write(index);
                    break;

            case 3: out.write(OPC.WIDE_0+((index>>8)&15));
                    out.write(OPC.CLASS);
                    out.write(index&0xFF);
                    break;

            case 4: out.write(OPC.WIDE_HALF);
                    out.write(OPC.CLASS);
                    out.writeChar(index);
                    break;

            default: {
                verifyError(VE_TOO_WIDE, "Too wide");
            }
        }
    }

    /*
     * load
     */
    private void load(int index, int size) throws IOException {
        if (sp == 0 && index == 0) {
            stack0isReceiver = true;
        }
        int opc = OPC.LOAD;
        char type = locals[index];
        verifySupportForType(type);
        if (   type == CNO.LONG2
/*if[FLOATS]*/
            || type == CNO.DOUBLE2
/*end[FLOATS]*/
        )
        {
            verifyError(VE_INVALID_TYPE, loader.getTypeName(type));
        }
        push(type);
/*if[LONGS]*/
        if (type == CNO.LONG) {
            if (size == 1) {
                verifyError(VE_BAD_WIDE_OP, "size="+size);
            }
            if (locals[index+1] != CNO.LONG2) {
                verifyError(VE_INVALID_TYPE, loader.getTypeName(locals[index+1]));
            }
            push(CNO.LONG2);
            opc = OPC.LOAD_I2;
        }
/*end[LONGS]*/
/*if[FLOATS]*/
        if (type == CNO.DOUBLE) {
            if (size == 1) {
                verifyError(VE_BAD_WIDE_OP, "size="+size);
            }
            if (locals[index+1] != CNO.DOUBLE2) {
                verifyError(VE_INVALID_TYPE, loader.getTypeName(locals[index+1]));
            }
            push(CNO.DOUBLE2);
            opc = OPC.LOAD_I2;
        }
/*end[FLOATS]*/

        switch (size) {
            case 1: out.write(OPC.LOAD_0+index);
                    break;

            case 2: out.write(opc);
                    out.write(index);
                    break;

            default: {
                verifyError(VE_TOO_WIDE, "Too wide");
            }
        }
    }

    /*
     * store
     */
    private void store(int index, int size) throws IOException {
        int opc = OPC.STORE;
        char type = locals[index];
        verifySupportForType(type);
        if (   type == CNO.LONG2
/*if[FLOATS]*/
            || type == CNO.DOUBLE2
/*end[FLOATS]*/
            )
        {
            verifyError(VE_INVALID_TYPE, loader.getTypeName(type));
        }
/*if[LONGS]*/
        if (type == CNO.LONG) {
            if (size == 1) {
                verifyError(VE_BAD_WIDE_OP, "size="+size);
            }
            if (locals[index+1] != CNO.LONG2) {
                verifyError(VE_INVALID_TYPE, loader.getTypeName(locals[index+1]));
            }
            pop(CNO.LONG2);
            opc = OPC.STORE_I2;
        }
/*end[LONGS]*/
/*if[FLOATS]*/
        if (type == CNO.DOUBLE) {
            if (size == 1) {
                verifyError(VE_BAD_WIDE_OP, "size="+size);
            }
            if (locals[index+1] != CNO.DOUBLE2) {
                verifyError(VE_INVALID_TYPE, loader.getTypeName(locals[index+1]));
            }
            pop(CNO.DOUBLE2);
            opc = OPC.STORE_I2;
        }
/*end[FLOATS]*/
        pop(type);
        switch (size) {
            case 1: out.write(OPC.STORE_0+index);
                    break;

            case 2: out.write(opc);
                    out.write(index);
                    break;

            default: {
                verifyError(VE_TOO_WIDE, "Too wide");
            }
        }
    }

    /*
     * iarith
     */
    private void iarith(int b) throws IOException {
        pop(CNO.INT);
        pop(CNO.INT);
        push(CNO.INT);
        out.write(b);
    }

    /*
     * ishift
     */
    private void ishift(int b) throws IOException {
        pop(CNO.INT);
        pop(CNO.INT);
        push(CNO.INT);
        out.write(b);
    }

    /*
     * ret
     */
    private void ret() throws IOException {
        switch (returnType) {
            case CNO.VOID:
                out.write(OPC.RETURN);
                break;
/*if[LONGS]*/
            case CNO.LONG:
                pop(CNO.LONG2);
                pop(CNO.LONG);
                out.write(OPC.RETURN2);
                break;
/*end[LONGS]*/
/*if[FLOATS]*/
            case CNO.DOUBLE:
                pop(CNO.DOUBLE2);
                pop(CNO.DOUBLE);
                out.write(OPC.RETURN2);
                break;
/*end[FLOATS]*/
            default:
                verifySupportForType(returnType);
                pop(returnType);
                out.write(OPC.RETURN1);
                break;
        }
        verifySpZero();
        hasReturn = true;
    }

    /*
     * thro
     */
    private void thro() throws IOException {
        pop(CNO.THROWABLE);
        verifySpZero();
        out.write(OPC.THROW);
    }

    /*
     * nop
     */
    private void nop(int b) throws IOException {
        out.write(b);
    }

    /*
     * popp
     */
    private void popp() throws IOException {
        pop();
        out.write(OPC.POP);
    }

    /*
     * i2i
     */
    private void i2i(int b) throws IOException {
        pop(CNO.INT);
        push(CNO.INT);
        out.write(b);
    }

    /*
     * lookup
     */
    private void lookup() throws IOException {
        char arrayType = pop();
        pop(CNO.INT);
        push(CNO.INT);
        if (arrayType == CNO.INT_ARRAY) {
            out.write(OPC.LOOKUP);
        } else if(arrayType == CNO.SHORT_ARRAY) {
            out.write(OPC.LOOKUP_S);
        } else if(arrayType == CNO.CHAR_ARRAY) {
            out.write(OPC.LOOKUP_C);
        } else if(arrayType == CNO.BYTE_ARRAY) {
            out.write(OPC.LOOKUP_B);
        } else {
            verifyError(VE_BAD_LOOKUP, "Bad lookup reference type "+loader.getTypeName(arrayType));
        }
    }

    /*
     * stableswitch
     */
    private void stableswitch() throws IOException {
        pop(CNO.INT);
        verifySpZero();
        if (((currentIp() - headerAndExtraInstructions) & 1) == 1) {
            out.write(OPC.STABLESWITCH);
        } else {
            out.write(OPC.STABLESWITCH_PAD);
            out.write(in.read());
        }
        int def  = in.readShort();
        int low  = in.readInt();
        int high = in.readInt();
        if (high < low) {
            verifyError(VE_BAD_SWITCH, "high="+high+" low="+low);
        }
        out.writeShort(def);
        out.writeInt(low);
        out.writeInt(high);

        for (int i = low ; i <= high ; i++) {
            out.writeShort(in.readShort());
        }
        verifySpZero();
    }

    /*
     * monitorenter
     */
    private void monitorenter() throws IOException {
        pop(CNO.OBJECT);
        verifySpZero();
        out.write(OPC.MONITORENTER);
    }

    /*
     * monitorexit
     */
    private void monitorexit() throws IOException {
        pop(CNO.OBJECT);
        verifySpZero();
        out.write(OPC.MONITOREXIT);
    }

    /*
     * classmonitorenter
     */
    private void classmonitorenter() throws IOException {
        out.write(OPC.CLASS_MONITORENTER);
        verifySpZero();
    }

    /*
     * classmonitorexit
     */
    private void classmonitorexit() throws IOException {
        out.write(OPC.CLASS_MONITOREXIT);
        verifySpZero();
    }

    /*
     * arraylength
     */
    private void arraylength() throws IOException {
        pop(CNO.OBJECT);
        push(CNO.INT);
        out.write(OPC.ARRAYLENGTH);
    }

    /*
     * clinit
     */
    private void clinit() throws IOException {
        popClassInfo();
        verifySpZero();
        out.write(OPC.CLINIT);
    }

    /*
     * newarray
     */
    private void newarray() throws IOException {
        char type = popClassInfo();
        pop(CNO.INT);
        verifySpZero();
        push(type);
        out.write(OPC.NEWARRAY);
    }

    /*
     * newdimension
     */
    private void newdimension() throws IOException {
        pop(CNO.INT); // size
        char type = pop();
        if (!loader.isAssignable(type, CNO.OBJECT_ARRAY)) {
            verifyError(VE_TYPE_MISMATCH, isAssignableErrorMessage(type, CNO.OBJECT_ARRAY));
        }
        verifySpZero();
        push(type);
        out.write(OPC.NEWDIMENSION);
    }

    /*
     * instof
     */
    private void instof() throws IOException {
        char type = popClassInfo();
        pop(CNO.OBJECT);
        verifySpZero();
        push(CNO.INT);
        out.write(OPC.INSTANCEOF);
    }

    /*
     * checkcast
     */
    private void checkcast() throws IOException {
        char type = popClassInfo();
        pop(CNO.OBJECT);
        verifySpZero();
        push(type);
        out.write(OPC.CHECKCAST);
    }


    /*
     * relop
     */
    private void relop(int b) throws IOException {
        pop(CNO.INT);
        pop(CNO.INT);
        push(CNO.INT);
        out.write(b);
    }


    /*
     * aload
     */
    private void aload() throws IOException {
        pop(CNO.INT);
        char type = pop();
        if (type == CNO.NULL) {
            verifyError(VE_NULL, "aload to null receiver");
        }
        switch (type) {
            case CNO.BOOLEAN_ARRAY:
            case CNO.BYTE_ARRAY:
                push(CNO.INT);
                out.write(OPC.ALOAD_B);
                break;

            case CNO.CHAR_ARRAY:
                push(CNO.INT);
                out.write(OPC.ALOAD_C);
                break;

            case CNO.SHORT_ARRAY:
                push(CNO.INT);
                out.write(OPC.ALOAD_S);
                break;

            case CNO.INT_ARRAY:
                push(CNO.INT);
                out.write(OPC.ALOAD);
                break;
/*if[LONGS]*/
            case CNO.LONG_ARRAY:
                push(CNO.LONG);
                push(CNO.LONG2);
                out.write(OPC.ALOAD_I2);
                break;
/*end[LONGS]*/
/*if[FLOATS]*/
            case CNO.FLOAT_ARRAY:
                push(CNO.FLOAT);
                out.write(OPC.ALOAD);
                break;

            case CNO.DOUBLE_ARRAY:
                push(CNO.DOUBLE);
                push(CNO.DOUBLE2);
                out.write(OPC.ALOAD_I2);
                break;
/*end[FLOATS]*/
            default:
                verifySupportForType(type);
                if (!loader.isAssignable(type, CNO.OBJECT_ARRAY)) {
                    verifyError(VE_BAD_ALOAD, "Bad aload: "+isAssignableErrorMessage(type, CNO.OBJECT_ARRAY));
                }
                push(loader.elementType(type));
                out.write(OPC.ALOAD);
                break;
        }
    }

    /*
     * astore
     */
    private void astore() throws IOException {
        char value = pop();
/*if[LONGS]*/
        if (value == CNO.LONG2) {
            pop(CNO.LONG);
            value = CNO.LONG;
        }
/*end[LONGS]*/
/*if[FLOATS]*/
        if (value == CNO.DOUBLE2) {
            pop(CNO.DOUBLE);
            value = CNO.DOUBLE;
        }
/*end[FLOATS]*/
        pop(CNO.INT);
        char type  = pop();
        if (type == CNO.NULL) {
            verifyError(VE_NULL, "astore to null receiver");
        }
        char etype = widenToInt(loader.elementType(type));

        switch (type) {
            case CNO.BOOLEAN_ARRAY:
            case CNO.BYTE_ARRAY:
                out.write(OPC.ASTORE_B);
                break;

            case CNO.CHAR_ARRAY:
            case CNO.SHORT_ARRAY:
                out.write(OPC.ASTORE_S);
                break;

            case CNO.INT_ARRAY:
                out.write(OPC.ASTORE);
                break;
/*if[LONGS]*/
            case CNO.LONG_ARRAY:
                out.write(OPC.ASTORE_I2);
                break;
/*end[LONGS]*/
/*if[FLOATS]*/
            case CNO.FLOAT_ARRAY:
                out.write(OPC.ASTORE);
                break;

            case CNO.DOUBLE_ARRAY:
                out.write(OPC.ASTORE_I2);
                break;
/*end[FLOATS]*/
            default:
                verifySpZero();
                verifySupportForType(type);
                if (etype != CNO.NULL && loader.elementTypeOrNull(etype) == CNO.NULL && value != CNO.NULL && loader.elementTypeOrNull(value) == CNO.NULL) {
                    /* As a special case, if both the array element type and
                     * the type are both non-array types (or NULL) then
                     * allow the aastore - it will be checked at runtime. */
                } else {
                    if (type != CNO.NULL && !loader.isAssignable(type, CNO.OBJECT_ARRAY)) {
                        verifyError(VE_BAD_ASTORE, isAssignableErrorMessage(value, CNO.OBJECT_ARRAY));
                    }
                }
                out.write(OPC.ASTORE_O);
                return;
        }
        if (!loader.isAssignable(value, etype)) {
            verifyError(VE_BAD_ASTORE, isAssignableErrorMessage(value, type));
        }
    }

    /*
     * inc
     */
    private void inc(int index, int size) throws IOException {
        char type = locals[index];
        if (type != CNO.INT) {
            verifyError(VE_BAD_INC, "Bad type: "+loader.getTypeName(type));
        }
        if (size != 2) {
            verifyError(VE_BAD_INC, "Bad size: "+size);
        }
        out.write(OPC.INC);
        out.write(index);
    }

    /*
     * dec
     */
    private void dec(int index, int size) throws IOException {
        char type = locals[index];
        if (type != CNO.INT) {
            verifyError(VE_BAD_DEC, "Bad type: "+loader.getTypeName(type));
        }
        if (size != 2) {
            verifyError(VE_BAD_DEC, "Bad size: "+size);
        }
        out.write(OPC.DEC);
        out.write(index);
    }

    /*
     * go2
     */
    private void go2(int b, int value, int size) throws IOException {
        switch (size) {
            case 2: out.write(b);
                    out.write(value);
                    break;

            case 3: out.write(OPC.WIDE_0+((value>>8)&15));
                    out.write(b);
                    out.write(value&0xFF);
                    break;

            case 4: out.write(OPC.WIDE_HALF);
                    out.write(b);
                    out.writeChar(value);
                    break;

            default: {
                verifyError(VE_TOO_WIDE, "Too wide");
            }
        }
        verifySpZero();
    }

    /*
     * if1
     */
    private void if1(int b, int value, int size) throws IOException {
        pop(CNO.INT);
        go2(b, value, size);
    }

    /*
     * if1_any
     */
    private void if1_any(int b, int value, int size) throws IOException {
        pop();
        go2(b, value, size);
    }

    /*
     * if2
     */
    private void if2(int b, int value, int size) throws IOException {
        pop(CNO.INT);
        if1(b, value, size);
    }

    /*
     * if2_any
     */
    private void if2_any(int b, int value, int size) throws IOException {
        int type1 = pop();
        int type2 = pop();
        if (type1 == CNO.INT) {
            if (type2 != CNO.INT) {
                verifyError(VE_TYPE_MISMATCH, isAssignableErrorMessage(type1, CNO.INT));
            }
        } else {
            if (type2 == CNO.INT) {
                verifyError(VE_TYPE_MISMATCH, isAssignableErrorMessage(type2, CNO.INT));
            }
        }
        go2(b, value, size);
    }

    /*
     * getfield
     */
    private void getfield(int field, int size) throws IOException {
        getfieldX(0, field, size);
    }

    /*
     * putfield
     */
    private void putfield(int field, int size) throws IOException {
        putfieldX(0, field, size);
    }

    /*
     * this_getfield
     */
    private void this_getfield(int field, int size) throws IOException {
        push(locals[0]);
        getfieldX(1, field, size);
    }

    /*
     * this_putfield
     */
    private void this_putfield(int field, int size) throws IOException {
        char val = pop();
        if (val == CNO.LONG2
/*if[FLOATS]*/
            || val == CNO.DOUBLE2
/*end[FLOATS]*/
            ) {
            char val2 = pop();
            push(locals[0]);
            push(val2);
            push(val);
            putfieldX(1, field, size);
        } else {
            push(locals[0]);
            push(val);
            putfieldX(1, field, size);
        }
    }


    /*
     * getfieldX
     */

    private static int[][] getTable  = new int[][] {
        new int[] {      OPC.GETFIELD,      OPC.GETFIELD_B,      OPC.GETFIELD_S,      OPC.GETFIELD_C,      OPC.GETFIELD_I2 },
        new int[] { OPC.THIS_GETFIELD, OPC.THIS_GETFIELD_B, OPC.THIS_GETFIELD_S, OPC.THIS_GETFIELD_C, OPC.THIS_GETFIELD_I2 }
    };


    private void getfieldX(int row, int fieldSlot, int size) throws IOException {
        if (size != 2) {
            verifyError(VE_TOO_WIDE, "size: "+size);
        }

        int col = 0; // Default is int/reference type
        char type = pop();
        if (type == CNO.NULL) {
            verifyError(VE_NULL, "getfield to null receiver");
        }
        Member field   = loader.getMember(type, SquawkConstants.MTYPE_INSTANCEFIELDS, fieldSlot);
        int offset     = field.offset();
        char fieldType = field.type();

        push(widenToInt(fieldType));

        switch (fieldType) {
            case CNO.BOOLEAN:
            case CNO.BYTE:
                col = 1;
                break;

            case CNO.SHORT:
                col = 2; offset /= 2;
                break;

            case CNO.CHAR:
                col = 3; offset /= 2;
                break;

            case CNO.INT:
                offset /= 4;
                break;
/*if[LONGS]*/
            case CNO.LONG:
                push(CNO.LONG2);
                col = 4; offset /= 4;
                break;
/*end[LONGS]*/
/*if[FLOATS]*/
            case CNO.FLOAT:
                offset /= 4;
                break;

            case CNO.DOUBLE:
                push(CNO.DOUBLE2);
                col = 4; offset /= 4;
                break;
/*end[FLOATS]*/
            default:
                verifySupportForType(fieldType);
                offset /= 4;
                break;
        }

        if (   fieldType == CNO.VOID
            || fieldType == CNO.LONG2
/*if[FLOATS]*/
            || fieldType == CNO.DOUBLE2
/*end[FLOATS]*/
            )
        {
            verifyError(VE_INVALID_TYPE, loader.getTypeName(fieldType));
        }
        if (offset > 255) {
            verifyError(VE_OFFSETTOOLARGE, "Offset: "+offset);
        }

        out.write(getTable[row][col]);
        out.write(offset);
    }

    /*
     * putfieldX
     */

    private static int[][] putTable  = new int[][] {
        new int[] {      OPC.PUTFIELD,      OPC.PUTFIELD_B,      OPC.PUTFIELD_S,      OPC.PUTFIELD_O,      OPC.PUTFIELD_I2 },
        new int[] { OPC.THIS_PUTFIELD, OPC.THIS_PUTFIELD_B, OPC.THIS_PUTFIELD_S, OPC.THIS_PUTFIELD_O, OPC.THIS_PUTFIELD_I2 }
    };

    private void putfieldX(int row, int fieldSlot, int size) throws IOException {
        if (size != 2) {
            verifyError(VE_TOO_WIDE, "Size: "+size);
        }
        int col = 3; // Default is reference type
        char value = pop();
        if (value == CNO.LONG2) {
            pop(CNO.LONG);
            value = CNO.LONG;
        }
/*if[FLOATS]*/
        if (value == CNO.DOUBLE2) {
            pop(CNO.DOUBLE);
            value = CNO.DOUBLE;
        }
/*end[FLOATS]*/
        char type = pop();
        if (type == CNO.NULL) {
            verifyError(VE_NULL, "putfield to null receiver");
        }
        Member field   = loader.getMember(type, SquawkConstants.MTYPE_INSTANCEFIELDS, fieldSlot);
        int offset     = field.offset();
        char fieldType = field.type();

        if (!loader.isAssignable(value, widenToInt(fieldType))) {
            verifyError(VE_TYPE_MISMATCH, isAssignableErrorMessage(value, fieldType));
        }

        switch (fieldType) {
            case CNO.BOOLEAN:
            case CNO.BYTE:
                col = 1;
                break;

            case CNO.SHORT:
            case CNO.CHAR:
                col = 2; offset /= 2;
                break;

            case CNO.INT:
                col = 0; offset /= 4;
                break;
/*if[LONGS]*/
            case CNO.LONG:
                col = 4; offset /= 4;
                break;
/*end[LONGS]*/
/*if[FLOATS]*/
            case CNO.FLOAT:
                col = 0; offset /= 4;
                break;

            case CNO.DOUBLE:
                col = 4; offset /= 4;
                break;
/*end[FLOATS]*/
            default:
                verifySupportForType(fieldType);
                offset /= 4;
                break;
        }

        if (   fieldType == CNO.VOID
            || fieldType == CNO.LONG2
/*if[FLOATS]*/
            || fieldType == CNO.DOUBLE2
/*end[FLOATS]*/
            )
        {
            verifyError(VE_INVALID_TYPE, loader.getTypeName(fieldType));
        }
        if (offset > 255) {
            verifyError(VE_OFFSETTOOLARGE, "Offset: "+offset);
        }

        out.write(putTable[row][col]);
        out.write(offset);
    }


    /**
     * Verify and re-write a GETSTATIC instruction.
     * @param field The logical offset of the static field.
     * @param size The size of the encoded instruction (in bytes).
     */
    private void getstatic(int field, int size) throws IOException {
        char ctype = popClassInfo();
        verifySpZero();
        getstaticX(field, size, ctype, OPC.GETSTATIC, OPC.GETSTATIC_O, OPC.GETSTATIC_I2);
    }

    /**
     * Verify and re-write a CLASS_GETSTATIC instruction.
     * @param field The logical offset of the static field.
     * @param size The size of the encoded instruction (in bytes).
     */
    private void classgetstatic(int field, int size) throws IOException {
        verifySpZero();
        getstaticX(field, size, methodImpl.parent, OPC.CLASS_GETSTATIC, OPC.CLASS_GETSTATIC_O, OPC.CLASS_GETSTATIC_I2);
    }

    /**
     * Verify and re-write a GETSTATIC instruction.
     * @param field The logical offset of the static field.
     * @param size The size of the instruction (in bytes).
     * @param type The type of the field.
     * @param OPC The opcode to use for accessing a non-reference field.
     * @param OPC_O The opcode to use for accessinging a reference field.
     * @param OPC_I2 The opcode to use for accessing a double-word field.
     */
    private void getstaticX(int fieldSlot, int size, char type, int OPC, int OPC_O, int OPC_I2) throws IOException {
        int opc = OPC_O;
        if (size != 2) {
            verifyError(VE_TOO_WIDE, "Size: "+size);
        }
        Member field   = loader.getMember(type, SquawkConstants.MTYPE_STATICFIELDS, fieldSlot);
        char fieldType = field.type();

        push(widenToInt(fieldType));
        verifySupportForType(fieldType);

        if (   fieldType == CNO.BOOLEAN
            || fieldType == CNO.BYTE
            || fieldType == CNO.SHORT
            || fieldType == CNO.CHAR
            || fieldType == CNO.INT
/*if[FLOATS]*/
            || fieldType == CNO.FLOAT
/*end[FLOATS]*/
        ) {
            opc = OPC;
        }
/*if[LONGS]*/
        else
        if (fieldType == CNO.LONG) {
            push(CNO.LONG2);
            opc = OPC_I2;
        }
/*end[LONGS]*/
/*if[FLOATS]*/
        else
        if (fieldType == CNO.DOUBLE) {
            push(CNO.DOUBLE2);
            opc = OPC_I2;
        }
/*end[FLOATS]*/

        if (   fieldType == CNO.VOID
            || fieldType == CNO.LONG2
/*if[FLOATS]*/
            || fieldType == CNO.DOUBLE2
/*end[FLOATS]*/
            )
     {
            verifyError(VE_INVALID_TYPE, loader.getTypeName(fieldType));
        }

       /*
        * Add the offset to the start of the variables in the ClassState and if
        * the field type is not a pointer add the number of pointer fields
        * to skip over these.
        */
        int offset = field.offset() + SquawkConstants.CLS_STATE_offsetToFields;
        if (opc != OPC_O) {
            Relocator r = loader.getRelocator(type);
            offset += r.internal_getStaticFieldsSize(true);
        } else {
            opc = OPC; // No need for OPC.GETSTATIC_O it is the same as OPC.GETSTATIC
        }

        if (offset > 255) {
            verifyError(VE_OFFSETTOOLARGE, "Offset: "+offset);
        }

        out.write(opc);
        out.write(offset);
    }

    /**
     * Verify and re-write a PUTSTATIC instruction.
     * @param field The logical offset of the static field.
     * @param size The size of the instruction (in bytes).
     */
    private void putstatic(int field, int size) throws IOException {
        putstaticX(field, size, popClassInfo(), OPC.PUTSTATIC, OPC.PUTSTATIC_O, OPC.PUTSTATIC_I2);
        verifySpZero();
    }

    /**
     * Verify and re-write a CLASS_PUTSTATIC instruction.
     * @param field The logical offset of the static field.
     * @param size The size of the instruction (in bytes).
     */
    private void classputstatic(int field, int size) throws IOException {
        putstaticX(field, size, methodImpl.parent, OPC.CLASS_PUTSTATIC, OPC.CLASS_PUTSTATIC_O, OPC.CLASS_PUTSTATIC_I2);
        verifySpZero();
    }

    /**
     * Verify and re-write a PUTSTATIC instruction.
     * @param field The logical offset of the static field.
     * @param size The size of the instruction (in bytes).
     * @param type The type of the field.
     * @param OPC The opcode to use for accessing a non-reference field.
     * @param OPC_O The opcode to use for accessinging a reference field.
     * @param OPC_I2 The opcode to use for accessing a double-word field.
     */
    private void putstaticX(int fieldSlot, int size, char type, int OPC, int OPC_O, int OPC_I2) throws IOException {
        int opc = OPC_O;
        if (size != 2) {
            verifyError(VE_TOO_WIDE, "Size: "+size);
        }
        char value = pop();
/*if[LONGS]*/
       if (value == CNO.LONG2) {
            pop(CNO.LONG);
            value = CNO.LONG;
        }
/*end[LONGS]*/
/*if[FLOATS]*/
        if (value == CNO.DOUBLE2) {
            pop(CNO.DOUBLE);
            value = CNO.DOUBLE;
        }
/*end[FLOATS]*/
        Member field   = loader.getMember(type, SquawkConstants.MTYPE_STATICFIELDS, fieldSlot);
        char fieldType = field.type();

        verifySupportForType(fieldType);

        if (   fieldType == CNO.BOOLEAN
            || fieldType == CNO.BYTE
            || fieldType == CNO.SHORT
            || fieldType == CNO.CHAR
            || fieldType == CNO.INT
/*if[FLOATS]*/
            || fieldType == CNO.FLOAT
/*end[FLOATS]*/
            )
        {
            opc = OPC;
        }
/*if[LONGS]*/
        if (fieldType == CNO.LONG) {
            opc = OPC_I2;
        }
/*end[LONGS]*/
/*if[FLOATS]*/
        if (fieldType == CNO.DOUBLE) {
            opc = OPC_I2;
        }
/*end[FLOATS]*/

        if (   fieldType == CNO.VOID
            || fieldType == CNO.LONG2
/*if[FLOATS]*/
            || fieldType == CNO.DOUBLE2
/*end[FLOATS]*/
            )
        {
            verifyError(VE_INVALID_TYPE, loader.getTypeName(fieldType));
        }

       /*
        * Add the offset to the start of the variables in the ClassState and if
        * the field type is not a pointer add the number of pointer fields
        * to skip over these.
        */
        int offset = field.offset() + SquawkConstants.CLS_STATE_offsetToFields;
        if (opc != OPC_O) {
            Relocator r = loader.getRelocator(type);
            offset += r.internal_getStaticFieldsSize(true);
        }

        if (offset > 255) {
            verifyError(VE_OFFSETTOOLARGE, "Offset: "+offset);
        }

        out.write(opc);
        out.write(offset);
    }

    /**
     * Verify and generate code for an 'invokevirtual' instruction.
     *
     * @param method
     * @param size
     * @throws IOException
     */
    private void invokevirtual(int method, int size) throws IOException {
        char type = stack[0];
        if (type == CNO.NULL) {
            verifyError(VE_NULL, "invokevirtual to null receiver");
        }
        invoke(OPC.INVOKEVIRTUAL, type, method, SquawkConstants.MTYPE_VIRTUALMETHODS, false);
    }

    /**
     * Verify and generate code for an 'invokesuper' instruction.
     *
     * @param method
     * @param size
     * @throws IOException
     */
    private void invokesuper(int method, int size) throws IOException {
        char type = popClassInfo();
        invoke(OPC.INVOKESUPER, type, method, SquawkConstants.MTYPE_VIRTUALMETHODS, false);
    }

    /**
     * Verify and generate code for an 'invokestatic' instruction.
     *
     * @param method
     * @param size
     * @throws IOException
     */
    private void invokestatic(int method, int size) throws IOException {
        char type = popClassInfo();
//System.out.println("invokestatic class="+(int)type+" "+loader.getRelocator(type).getName());
        invoke(OPC.INVOKESTATIC, type, method, SquawkConstants.MTYPE_STATICMETHODS, false);
    }

    /**
     * Verify and generate code for an 'invokeinit' instruction.
     *
     * @param method
     * @param size
     * @throws IOException
     */
    private void invokeinit(int method, int size) throws IOException {
        boolean s0rcvr = stack0isReceiver;
//System.out.println("invokeinit sp="+sp);
        char type = popClassInfo();
        Member m = invoke(OPC.INVOKEINIT, type, method, SquawkConstants.MTYPE_STATICMETHODS, false);
        if ((m.access() & SquawkConstants.ACC_INIT) == 0) {
            verifyError(VE_BAD_INVOKEINIT, "m.access="+m.access()+" n.name="+m.name());
        }
        if (isConstructor && s0rcvr) {
            // Calling super constructor
            if (initCalledAt != -1) {
                verifyError(VE_TWO_INIT_CALLS, "");
            }
            initCalledAt = currentIp();
            // Change the type on the top of the stack (i.e. the type returned
            // the call to the super constructor) to be the current class
            stack[sp-1] = methodImpl.parent;
        }
    }

    /**
     * Verify and generate code for an 'invokeinterface' instruction.
     *
     * @param method
     * @param size
     * @throws IOException
     */
    private void invokeinterface(int method, int size) throws IOException {
        char type = popClassInfo();
        invoke(OPC.INVOKEINTERFACE, type, method, SquawkConstants.MTYPE_VIRTUALMETHODS, true);
    }


    /**
     * Verify and generate code for one of the 'invoke*' instructions.
     *
     * @param opc The opcode of the instruction.
     * @param type The derived class of the method to be invoked. For a virtual
     * call, this will be the derived class of the receiver. For all other
     * calls, it will be the class in which the method was defined (i.e.
     * it will be the class pushed to the stack by the preceeding 'class'
     * instruction).
     * @param invokeSlot The index of the method to invoke in the
     * relevant table of methods.
     * @param mtype SquawkConstants.MTYPE_STATICMETHODS or SquawkConstants.MTYPE_VIRTUALMETHODS.
     * @param invokeinterface True if this is for the 'invokeinterface'
     * instruction.
     * @return the Member instance representing the method invoked.
     * @throws IOException
     */
    private Member invoke(int opc, char type, int invokeSlot, int mtype, boolean invokeinterface) throws IOException {
        // Must adjust the method slot for an interface method by the number of
        // virtual methods in Object
        int memberSlot = invokeSlot;
        if (invokeinterface) {
            memberSlot += loader.getRelocator(CNO.OBJECT).suite_getSlotsCount(SquawkConstants.MTYPE_VIRTUALMETHODS);
        }

        Member method   = loader.getRelocator(type).getMember(mtype, memberSlot);
        int  parmCount  = method.parmCount();
        char methodType = method.type();
        int  vtableSlot = invokeinterface ? invokeSlot : method.offset();

        // Hack to change invokestatic to invokenative and to change the slot
        // number to the globally allocated identifier for the native method.
        if (method.isNative()) {

            int nmid = method.nativeMethodIdentifier();

            // If the loader has given this native method a non-zero
            // globally unique identifier, then it is in suite 0 and
            // is implemented internally by the VM.
            if (nmid != 0) {
                opc = OPC.INVOKENATIVE;
                vtableSlot = nmid;
            }
        }

        verifySupportForType(method.type());

        if (sp != parmCount) {
            verifyError(VE_BAD_PARM_COUNT, "sp="+sp+" parmCount="+parmCount);
        }
        for (int i = 0 ; i < sp ; i++) {
            char stck = stack[i];
            char parm = method.parmAt(i);
            if (!loader.isAssignable(stck, widenToInt(parm))) {
                verifyError(VE_TYPE_MISMATCH, "Parameter "+i+": "+isAssignableErrorMessage(stck, parm));
            }
        }
        sp = 0;
        stack0isReceiver = false;
        if (methodType != CNO.VOID) {
            push(widenToInt(methodType));
/*if[LONGS]*/
            if (methodType == CNO.LONG) {
                push(CNO.LONG2);
            }
/*end[LONGS]*/
/*if[FLOATS]*/
            if (methodType == CNO.DOUBLE) {
                push(CNO.DOUBLE2);
            }
/*end[FLOATS]*/
        }

        opc -= OPC.INVOKEINIT;
        if (sp == 0) {
            opc += OPC.INVOKEINIT;
        } else if (sp == 1) {
            opc += OPC.INVOKEINIT1;
        } else {
            opc += OPC.INVOKEINIT2;
        }
        out.write(opc);

        if (vtableSlot > 255) {
            verifyError(VE_OFFSETTOOLARGE, "offset="+vtableSlot);
        }
        out.write(vtableSlot);

        return method;
    }


/*if[LONGS]*/
    /*
     * longOp
     */
    private void longOp() throws IOException {
        out.write(OPC.LONGOP);
        int b = in.read();
        out.write(b);
        Romizer.logLongInstruction(b);
        switch (b) {
            case OPC.LADD:
            case OPC.LSUB:
            case OPC.LMUL:
            case OPC.LDIV:
            case OPC.LREM:
            case OPC.LAND:
            case OPC.LOR:
            case OPC.LXOR:
                pop(CNO.LONG2);
                pop(CNO.LONG);
                pop(CNO.LONG2);
                pop(CNO.LONG);
                push(CNO.LONG);
                push(CNO.LONG2);
                return;

            case OPC.LNEG:
                pop(CNO.LONG2);
                pop(CNO.LONG);
                push(CNO.LONG);
                push(CNO.LONG2);
                return;

            case OPC.LSHL:
            case OPC.LSHR:
            case OPC.LUSHR:
                pop(CNO.INT);
                pop(CNO.LONG2);
                pop(CNO.LONG);
                push(CNO.LONG);
                push(CNO.LONG2);
                return;

            case OPC.LCMP:
                pop(CNO.LONG2);
                pop(CNO.LONG);
                pop(CNO.LONG2);
                pop(CNO.LONG);
                push(CNO.INT);
                return;

            case OPC.L2I:
                pop(CNO.LONG2);
                pop(CNO.LONG);
                push(CNO.INT);
                return;

            case OPC.I2L:
                pop(CNO.INT);
                push(CNO.LONG);
                push(CNO.LONG2);
                return;
        }
        verifyError(VE_UNKNOWN_BYTECODE, "Unknown bytecode "+b);
    }
/*end[LONGS]*/

/*if[FLOATS]*/
    /*
     * floatOp
     */
    private void floatOp() throws IOException {
        out.write(OPC.FLOATOP);
        int b = in.read();
        out.write(b);
        Romizer.logFloatInstruction(b);
        switch (b) {
            case OPC.FADD:
            case OPC.FSUB:
            case OPC.FMUL:
            case OPC.FDIV:
            case OPC.FREM:
                pop(CNO.FLOAT);
                pop(CNO.FLOAT);
                push(CNO.FLOAT);
                return;

            case OPC.FNEG:
                pop(CNO.FLOAT);
                push(CNO.FLOAT);
                return;

            case OPC.FCMPG:
            case OPC.FCMPL:
                pop(CNO.FLOAT);
                pop(CNO.FLOAT);
                push(CNO.INT);
                return;

            case OPC.DADD:
            case OPC.DSUB:
            case OPC.DMUL:
            case OPC.DDIV:
            case OPC.DREM:
                pop(CNO.DOUBLE2);
                pop(CNO.DOUBLE);
                pop(CNO.DOUBLE2);
                pop(CNO.DOUBLE);
                push(CNO.DOUBLE);
                push(CNO.DOUBLE2);
                return;

            case OPC.DNEG:
                pop(CNO.DOUBLE2);
                pop(CNO.DOUBLE);
                push(CNO.DOUBLE);
                push(CNO.DOUBLE2);
                return;

            case OPC.DCMPG:
            case OPC.DCMPL:
                pop(CNO.DOUBLE2);
                pop(CNO.DOUBLE);
                pop(CNO.DOUBLE2);
                pop(CNO.DOUBLE);
                push(CNO.INT);
                return;

            case OPC.I2F:
                pop(CNO.INT);
                push(CNO.FLOAT);
                return;

            case OPC.L2F:
                pop(CNO.LONG2);
                pop(CNO.LONG);
                push(CNO.FLOAT);
                return;

            case OPC.F2I:
                pop(CNO.FLOAT);
                push(CNO.INT);
                return;

            case OPC.F2L:
                pop(CNO.FLOAT);
                push(CNO.LONG);
                push(CNO.LONG2);
                return;

            case OPC.I2D:
                pop(CNO.INT);
                push(CNO.DOUBLE);
                push(CNO.DOUBLE2);
                return;

            case OPC.L2D:
                pop(CNO.LONG2);
                pop(CNO.LONG);
                push(CNO.DOUBLE);
                push(CNO.DOUBLE2);
                return;

            case OPC.F2D:
                pop(CNO.FLOAT);
                push(CNO.DOUBLE);
                push(CNO.DOUBLE2);
                return;

            case OPC.D2I:
                pop(CNO.DOUBLE2);
                pop(CNO.DOUBLE);
                push(CNO.INT);
                return;

            case OPC.D2L:
                pop(CNO.DOUBLE2);
                pop(CNO.DOUBLE);
                push(CNO.LONG);
                push(CNO.LONG2);
                return;

            case OPC.D2F:
                pop(CNO.DOUBLE2);
                pop(CNO.DOUBLE);
                push(CNO.FLOAT);
                return;
        }
        verifyError(VE_UNKNOWN_BYTECODE, "Unknown bytecode "+b);
    }
/*end[FLOATS]*/


/* ------------------------------------------------------------------------ *\
 *                                  Pass 2                                  *
\* ------------------------------------------------------------------------ */

    /*
     * byteAt
     */
    private int byteAt(int ip) {
        return methodImpl.bytecodes[ip] &0xFF;
    }

    /*
     * signedByteAt
     */
    private int signedByteAt(int ip) {
        return methodImpl.bytecodes[ip];
    }

    /*
     * shortAt
     */
    private int shortAt(int ip) {
        return byteAt(ip++) << 8 | byteAt(ip++);
    }

    /*
     * signedShortAt
     */
    private int signedShortAt(int ip) {
        int res = byteAt(ip++) << 8 | byteAt(ip++);
        return res << 16 >> 16;
    }

    /*
     * intAt
     */
    private int intAt(int ip) {
        return byteAt(ip++) << 24 | byteAt(ip++) << 16 | byteAt(ip++) << 8 | byteAt(ip++);
    }

    /*
     * verifyPass2
     */
    private void verifyPass2(int endPoint) {
        verifyPass2(endPoint, 0);
    }

    /*
     * verifyPass2
     */
    private void verifyPass2(int endPoint, int expectedSp) {

//System.out.println("verifyPass2 "+endPoint);
        int sp = 0;
        int ip = headerSize;
        int istart = ip;
        int wide = 0;
        int wide_n = 0;
        boolean iteration2;
        if (endPoint == 0) {
            endPoint = methodImpl.bytecodes.length;
            iteration2 = true;
            for (int i = 0 ; i < handlers.length ; i++) {
                ExceptionHandler handler = handlers[i];
                verifyPass2(handler.to + headerAndExtraInstructions, -1);
                verifyPass2(handler.from + headerAndExtraInstructions, -1);
                verifyPass2(handler.entryPoint + headerAndExtraInstructions, 0);
            }
        } else {
            iteration2 = false;
        }



        while (ip < endPoint) {
           /*
            * Check to see if there is a handler starting at this address
            */
            if (handlers != null) {
                for (int i = 0 ; i < handlers.length ; i++) {
                    ExceptionHandler handler = handlers[i];
                    if (handler.entryPoint + headerAndExtraInstructions == ip) {
                        sp = 1;
                        break;
                    }
                }
            }


            int b = byteAt(ip);
            int blength = (byte)OPC.LENGTH_TABLE.charAt(b);
            int bstack  = (byte)OPC.STACK_EFFECT_TABLE.charAt(b);
//System.out.println("ip="+ip+" sp="+sp+" b="+b+ " bstack="+bstack+"     "+mnemonics[b]);

            switch (bstack) {
                case -100: sp = 0;       break;
                case -101: sp = 1;       break;
                case -102: sp = 2;       break;
                default:   sp += bstack; break;
            }

            if (sp < 0) {
                verifyError(VE_STACK_UNDERFLOW, ip, "Stack underflow in pass2 sp="+sp);
            }

            if (blength == 0) {
                switch (b) {
                    case OPC.WIDE_0:  case OPC.WIDE_1:  case OPC.WIDE_2:  case OPC.WIDE_3:
                    case OPC.WIDE_4:  case OPC.WIDE_5:  case OPC.WIDE_6:  case OPC.WIDE_7: {
                        wide = 1;
                        wide_n = (b & 15) << 8;
                        ip++;
                        continue;
                    }

                    case OPC.WIDE_8:  case OPC.WIDE_9:  case OPC.WIDE_10: case OPC.WIDE_11:
                    case OPC.WIDE_12: case OPC.WIDE_13: case OPC.WIDE_14: case OPC.WIDE_15: {
                        wide = 1;
                        wide_n = (b & 15) << 8 | 0xFFFFF000;
                        ip++;
                        continue;
                    }

                    case OPC.WIDE_HALF: {
                        wide = 2;
                        ip++;
                        continue;
                    }

                    case OPC.WIDE_FULL: {
                        wide = 4;
                        ip++;
                        continue;
                    }
/*if[FLOATS]*/
                    case OPC.FLOATOP: {
                        b = byteAt(++ip);
                        blength = (byte)OPC.FLOAT_LENGTH_TABLE.charAt(b);
                        bstack  = (byte)OPC.FLOAT_STACK_EFFECT_TABLE.charAt(b);
                        sp += bstack;
                        ip += blength;
                        istart = ip;
                        continue;
                    }
/*end[FLOATS]*/
/*if[LONGS]*/
                    case OPC.LONGOP: {
                        b = byteAt(++ip);
                        blength = (byte)OPC.LONG_LENGTH_TABLE.charAt(b);
                        bstack  = (byte)OPC.LONG_STACK_EFFECT_TABLE.charAt(b);
                        sp += bstack;
                        ip += blength;
                        istart = ip;
                        continue;
                    }
/*end[LONGS]*/
                    case OPC.STABLESWITCH: {
                        ip = stablelength(ip+1, iteration2);
                        istart = ip;
                        continue;
                    }

                    case OPC.STABLESWITCH_PAD: {
                        ip = stablelength(ip+2, iteration2);
                        istart = ip;
                        continue;
                    }
                }
            }

            if (iteration2) {
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
                    case OPC.GOTO: {
                        int addr;
                        switch (wide) {
                            case 0:     addr = signedByteAt(ip+1);     break;
                            case 1:     addr = wide_n | byteAt(ip+1);  break;
                            case 2:     addr = signedShortAt(ip+1);    break;
                            default:    addr = intAt(ip+1);            break;
                        }
                        int target = istart + blength + wide + addr;
                        if (initCalledAt != -1) {
                            if (istart < initCalledAt) {
                                if (target >= initCalledAt) {
                                    verifyError(VE_BRANCH_OVER_INIT, "istart="+istart+" target="+target+" initCalledAt="+initCalledAt);
                                }
                            } else {
                                if (target <= initCalledAt) {
                                    verifyError(VE_BRANCH_OVER_INIT, "istart="+istart+" target="+target+" initCalledAt="+initCalledAt);
                                }
                            }
                        }
                        verifyPass2(target);
                        break;
                    }
                }
            }
            ip += blength;
            if (wide > 0) {
               ip += wide - 1;
            }
            wide = 0;
            istart = ip;
        }
        if (ip != endPoint) {
            verifyError(VE_BAD_JUMPIP, ip, "Did not end on an instruction boundary: target ip="+endPoint);
        }
        if (expectedSp != -1) {
            if (sp != expectedSp) {
                verifyError(VE_BAD_JUMPSP, ip, "Stack is not expected value "+expectedSp+" sp="+sp);
            }
        }
    }


    /*
     * stablelength
     */
    private int stablelength(int ip, boolean iteration2) {
        int startIP = ip;
        int addr = signedShortAt(ip); ip += 2;
        int low  = intAt(ip);   ip += 4;
        int high = intAt(ip);   ip += 4;
        int branchPoint = ip;
        if (iteration2) {
            verifyPass2(branchPoint + addr);
        }
        for (int i = low ; i <= high ; i++) {
            addr = signedShortAt(ip); ip += 2;
            if (iteration2) {
                int target = branchPoint + addr;
                if (initCalledAt != -1) {
                    if (branchPoint < initCalledAt) {
                        if (target >= initCalledAt) {
                            verifyError(VE_BRANCH_OVER_INIT, startIP, "target="+target+" initCalledAt="+initCalledAt+" branchPoint="+branchPoint);
                        }
                    } else {
                        if (target <= initCalledAt) {
                            verifyError(VE_BRANCH_OVER_INIT, startIP, "target="+target+" initCalledAt="+initCalledAt+" branchPoint="+branchPoint);
                        }
                    }
                }
                verifyPass2(target);
            }
        }
        return ip;
    }

    private void verifyParameters(Member m, boolean isStaticMethod) {

        // Check parms match
        int lth = m.parmCount();
        for (int i = 0 ; i < lth ; i++) {
            int l, p;
            /*
             * The normal check is that the parameters is assignable to a locals. So, for instance
             * "void foo(Object x);" might be passed a parameter of type String.
             *
             * However in the case of parameters which are the receivers the check must be the other
             * way around. Thus "int hashcode()" might be defined in class Object but when it is implemented
             * by, say, class String then the parameter is of type Object but the local is of type String.
             *
             */
            if (i == 0 && !isStaticMethod) {
                p = locals[i];
                l = widenToInt(m.parmAt(i));
            } else {
                p = widenToInt(m.parmAt(i));
                l = locals[i];
            }
            if(!loader.isAssignable(p, l)) {
                verifyError(VE_TYPE_MISMATCH, "Parameter "+i+": "+isAssignableErrorMessage(p, l));
            }
        }
    }


   /* ------------------------------------------------------------------------ *\
    *                            Preamble                                      *
   \* ------------------------------------------------------------------------ */


    private void preamble(SuiteRelocator relocator, SuiteMethodImpl methodImpl, Member method) throws IOException {

       /*
        * Calculate the size of the method header
        *
        * struct {
        *     byte headerSize
        *     byte suiteNumber;
        *     byte classNumber;
        *     byte numberOfParms;
        *     byte numberOfLocals;
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

        int basicSize  = 5;
        int oopMapSize = (locals.length + 7) / 8;
        int extraInstructions = 1; // for the extend_n instruction

        // Account for the OPC.EXTEND instructions
        if (methodImpl.stackSize() > 15) {
            extraInstructions++;
        }

        // Account for the OPC.NEWOBJECT / OPC.CLINIT instructions
        boolean mustCallClinit;
        if (isConstructor) {
            mustCallClinit = false;
            extraInstructions++; // for the newobject bytecode
        } else {
            mustCallClinit = methodImpl.isStatic() &&
                             relocator.internal_mustClinit() &&
                             ((method.access() & SquawkConstants.ACC_CLINIT) == 0);
            if (mustCallClinit) {
                extraInstructions++; // for the class_clinit bytecode
            }
        }

/*if[DEBUG.METHODDEBUGTABLE]*/
        // Account for the OPC.METHODID instruction
        extraInstructions += 5;
/*end[DEBUG.METHODDEBUGTABLE]*/

        headerSize = basicSize +
                     oopMapSize +
                     (handlers.length * 8);
        if (headerSize > 255) {
            verifyError(VE_HEADER_TOO_LARGE, ""+headerSize);
        }

        headerAndExtraInstructions = headerSize + extraInstructions;
        methodSize = headerAndExtraInstructions + methodImpl.codeSize;

        // Allocate the output streams
        pmos = new PersistentMemoryOutputStream(methodSize);
        out  = new DataOutputStream(pmos);

        // Write the header
        out.write(headerSize);
        out.write(loader.suite().number());
        out.write(relocator.finalClassNumber());
        out.write(method.parmCount());
        out.write(locals.length);
        for (int i = 0, j = 0 ; i < oopMapSize ; i++) {
            int b = 0;
            b |= setBitIfOop(0x1,  locals, j++);
            b |= setBitIfOop(0x2,  locals, j++);
            b |= setBitIfOop(0x4,  locals, j++);
            b |= setBitIfOop(0x8,  locals, j++);
            b |= setBitIfOop(0x10, locals, j++);
            b |= setBitIfOop(0x20, locals, j++);
            b |= setBitIfOop(0x40, locals, j++);
            b |= setBitIfOop(0x80, locals, j++);
            out.write(b&0xFF);
        }

        // Write the exception handlers
        if (methodImpl.handlers != null) {
            for (int i = 0 ; i < methodImpl.handlers.length ; i++) {
                ExceptionHandler handler = methodImpl.handlers[i];
                out.writeChar(handler.from + headerAndExtraInstructions);
                out.writeChar(handler.to + headerAndExtraInstructions);
                out.writeChar(handler.entryPoint + headerAndExtraInstructions);
                out.writeChar(loader.getSystemTypeFor(handler.type));
            }
        }

        if (pmos.size() != headerSize) {
            verifyError(VE_INTERNAL_ERROR, "headerSize="+headerSize+"pmos.size()="+pmos.size());
        }

        // Output the extend instrution
        extend(methodImpl.stackSize());

        // If this is a constructor output the newobject instruction
        if (isConstructor) {
            out.write(OPC.NEWOBJECT);
        }

        // If this is a static method of a class that requires initialization
        // then add the class_clinit instuction
        if (mustCallClinit) {
            out.write(OPC.CLASS_CLINIT);
        }

/*if[DEBUG.METHODDEBUGTABLE]*/
        out.write(OPC.METHODID);
        out.writeInt(MethodDebugTable.getNextAvailableMethodID());
/*end[DEBUG.METHODDEBUGTABLE]*/
    }

   /* ------------------------------------------------------------------------ *\
    *               Verification error messages and methods                    *
   \* ------------------------------------------------------------------------ */


   /**
    * Throw a VerifyError.
    * @param code
    * @param ip
    * @param str
    */
    private void verifyError(int code, int ip, String str) {
        String prefix = "VerifyError "+code+" @ "+ip+": ";
/*if[DEBUG.LOADER]*/
        prefix = "VerifyError in "+getMethodSig()+": "+VE_ERROR_MESSAGES[code]+" @ "+ip+": ";
/*end[DEBUG.LOADER]*/
        throw new LinkageError(prefix + str);
    }

/*if[DEBUG.LOADER]*/
    private String getMethodSig() {
        Member method = relocator.getMember(methodImpl.isStatic() ? SquawkConstants.MTYPE_STATICMETHODS : SquawkConstants.MTYPE_VIRTUALMETHODS, methodImpl.entry);
        if (method == null) {
            return "<not found>";
        }
        return method.toString(relocator.getName(), methodImpl.isStatic(), true, loader);
    }
/*end[DEBUG.LOADER]*/

    /**
     * Throw a VerifyError.
     * @param code
     * @param ip
     * @param str
     */
    private void verifyError(int code, String str) {
        int ip = currentIp();
        if (ip != -1) {
            ip = ip - headerAndExtraInstructions;
        }
        verifyError(code, ip, str);
    }

    /**
     * Create the error message string when one type is not assignable to
     * another using the full names of the types.
     * @param from
     * @param to
     * @return
     */
    private String isAssignableErrorMessage(int from, int to) {
        return loader.getTypeName(from) + " is not assignable to " + loader.getTypeName(to);
    }

   /* ------------------------------------------------------------------------ *\
    *               Method stub generators for abstract and native methods     *
   \* ------------------------------------------------------------------------ */

    /**
     * Lookup the symbols for "java.lang.VMExtension".
     *
     * @return
     */
    private StringOfSymbols lookupJavaLangVMExtensionSymbols() {
        Klass klass = SuiteManager.forName("java.lang.VMExtension");
        if (klass == null) {

            // We must be loading suite 0 - look up the relocators
            Relocator relocator = loader.findRelocatorForName("java.lang.VMExtension");
            if (relocator != null) {
                return relocator.getSymbols();
            }
            throw new InternalError("Cannot find symbols for java.lang.VMExtension");
        } else {
            return klass.symbols;
        }

    }

    /**
     * Lookup the offset for a static method in java.lang.VMExtension.
     * This lookup process only takes into account
     * the name of the method and will not differentiate between methods
     * with the same name but different signatures. It simply returns the
     * first match.
     *
     * @param name The name of the method to lookup.
     * @return the offset of the method found or -1 if it is not found.
     */
    private int lookupVMExtensionStaticMethod(String name) {
        // Find the symbols for "java.lang.VMExtension".
        StringOfSymbols symbols = lookupJavaLangVMExtensionSymbols();

        // Lookup the method and get its offset.
        int count = symbols.getMemberCount(SquawkConstants.MTYPE_STATICMETHODS);
        nextMethod:
        for (int i = 0; i != count; ++i) {
            int id = symbols.lookupMemberID(SquawkConstants.MTYPE_STATICMETHODS, i);
            int length = symbols.getMemberNameLength(id);
            if (length == name.length()) {
                for (int c = 0; c != length; ++c) {
                    if (name.charAt(c) != symbols.getMemberNameChar(id, c)) {
                        continue nextMethod;
                    }
                }
                return symbols.getMemberOffset(id);
            }
        }
        throw new InternalError("Cannot find 'static java.lang.VMExtension."+name+"'");
    }

    /**
     * Generate a method body that invokes one of the static methods in
     * java.lang.VMExtension. The native function must not return (i.e. it
     * either throws an exception or stop the VM).
     *
     * @param name
     * @param parmCount
     * @return
     * @throws IOException
     */
    private byte[] invokeVMExtensionStaticMethodStub(String name, int parmCount) throws IOException {
        int offset = lookupVMExtensionStaticMethod(name);
        int jlvmeRefIndex = relocator.getSuiteClass().classReferences.length;

        int size = 11;
/*if[DEBUG.METHODDEBUGTABLE]*/
        size += 5; // OPC.METHODID + value
/*end[DEBUG.METHODDEBUGTABLE]*/
        pmos = new PersistentMemoryOutputStream(size);
        out  = new DataOutputStream(pmos);


        out.write(5);                             // headerSize
        out.write(loader.suite().number());       // suiteNumber
        out.write(relocator.finalClassNumber());  // classNumber
        out.write(parmCount);                     // numberOfParms (ignored)
        out.write(0);                             // numberOfLocals (ignored)
        out.write(OPC.EXTEND_1);
/*if[DEBUG.METHODDEBUGTABLE]*/
        out.write(OPC.METHODID);
        out.writeInt(MethodDebugTable.getNextAvailableMethodID());
/*end[DEBUG.METHODDEBUGTABLE]*/
        out.write(OPC.CLASS);         // class reference
        out.write(jlvmeRefIndex);     // ...
        out.write(OPC.INVOKESTATIC);  // invoke a method in java.lang.VMExtension
        out.write(offset);
        out.close();
        return pmos.toByteArray();
    }

    /**
     * Generate a method body that throws an AbstractMethodError.
     *
     * @param method
     * @return
     * @throws IOException
     */
    private byte[] raiseAbstractMethodError(Member method) throws IOException {
        return invokeVMExtensionStaticMethodStub("abstractMethodError", method.parmCount());
    }

    /**
     * Generate a method body that throws an UnsatisfiedLinkError.
     *
     * @param method
     * @return
     * @throws IOException
     */
    private byte[] raiseUnsatisifiedLinkError(Member method) throws IOException {
        return invokeVMExtensionStaticMethodStub("unsatisfiedLinkError", method.parmCount());
    }

   /* ------------------------------------------------------------------------ *\
    *               Verification helper methods                                *
   \* ------------------------------------------------------------------------ */

   /**
    * Verify that a given type is supported by this VM. The appropriate
    * exception is raised if the type is not supported.
    * @param type The type to test.
    */
    private void verifySupportForType(char type) {
        if (!loader.isTypeSupported(type)) {
            boolean isLong = (type == CNO.LONG ||
                              type == CNO.LONG2 ||
                              type == CNO.LONG_ARRAY);
            verifyError(VE_TYPE_NOT_SUPPORTED, (isLong ? "Longs" : "Floats") + " not supported");
        }
    }

    /**
     * Verify that the stack currently has no elements, raising the appropriate
     * exception if it does.
     */
    private void verifySpZero() {
        if (sp != 0) {
            verifyError(VE_SP_NOT_ZERO, "sp="+sp);
        }
    }


}