
/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/public interface Bytecodes {


/*IFJ*/public final static int
//IFC//enum {
    Native_asInt_1                      = 1,
    Native_asClass_2                    = 2,
    Native_asOop_3                      = 3,
    Native_asIntArray_4                 = 4,
    Native_asByteArray_5                = 5,
    Native_floatToIntBits_6             = 6,
    Native_doubleToLongBits_7           = 7,
    Native_intBitsToFloat_8             = 8,
    Native_longBitsToDouble_9           = 9,
    Native_parm_10                      = 10,
    Native_parm_11                      = 11,
    Native_exec_12                      = 12,
    Native_error_13                     = 13,
    Native_result_14                    = 14,
    Native_getAR_15                     = 15,
    Native_setAR_16                     = 16,
    Native_primNewArray_17              = 17,
    Native_callClinitForClass_18        = 18,
    Native_callMainForClass_19          = 19,

    VMExtension_doClinit_1              = 1,
    VMExtension_arithmeticException_2   = 2,
    VMExtension_arrayBoundsException_3  = 3,
    VMExtension_nullPointerException_4  = 4,
    VMExtension_breakpoint_5            = 5,
    VMExtension_checkcast_6             = 6,
    VMExtension_instanceof_7            = 7,
    VMExtension_throwException_8        = 8,
    VMExtension_yeild_9                 = 9,
    VMExtension_getStatic_10            = 10,
    VMExtension_getStaticLong_11        = 11,
    VMExtension_putStatic_12            = 12,
    VMExtension_putStaticLong_13        = 13,
    VMExtension_putStaticObj_14         = 14,
    VMExtension_monitorEnter_15         = 15,
    VMExtension_monitorExit_16          = 16,
    VMExtension_newArray_17             = 17,
    VMExtension_newDimension_18         = 18,
    VMExtension_DUMMY=0
//IFC//}
;



/*IFJ*/private int sp;


/*IFJ*/private void push(int x) {
/*IFJ*/    setWord(sp, 0, x);
/*IFJ*/    sp += ADDRESS_UNITS;
/*IFJ*/}
/*IFJ*/private int pop() {
/*IFJ*/    sp -= ADDRESS_UNITS;
/*IFJ*/    return getWord(sp, 0);
/*IFJ*/}

//IFC//#define push(x) { setWord(sp, 0, x); sp += ADDRESS_UNITS; }
//IFC//#define pop()     getWord((sp -= ADDRESS_UNITS, sp), 0)


/*if[LONGS]*/

/*IFJ*/private void pushLong(long x) {
/*IFJ*/    setLong(sp, 0, x);
/*IFJ*/    sp += (ADDRESS_UNITS*2);
/*IFJ*/}
/*IFJ*/private long popLong() {
/*IFJ*/    sp -= (ADDRESS_UNITS*2);
/*IFJ*/    return getLong(sp, 0);
/*IFJ*/}

//IFC//#define pushLong(x) { setLong(sp, 0, x); sp += (ADDRESS_UNITS*2); }
//IFC//#define popLong()     getLong((sp -= (ADDRESS_UNITS*2), sp), 0)

/*end[LONGS]*/



/*---------------------------------------------------------------------------*\
 *                                findClassState                             *
\*---------------------------------------------------------------------------*/

    int findClassState(int cls) {
        int cstable = getIsolateStateTable();                       // Get Hashtable
        int first = getWord(cstable, klassType & 15);               // Lookup first (which cannot be cls);
        int prev = first;
        assume(first != cls);
        do {
            int cs = ClassState_getNext(prev);
            if (cs == 0) {
                return 0;
            }
            if (ClassState_getKlass(cs) == cls) {
                ClassState_setNext(prev, ClassState_getNext(cs));   // unlink cs
                setWord(cstable, klassType & 15, cs);               // make cs first
                ClassState_setNext(cs, first);                      // chain old first onto cs
                return cs
            }
            prev = cs;
        }
    }



/*---------------------------------------------------------------------------*\
 *                                  interpret                                *
\*---------------------------------------------------------------------------*/

    void interpret(int cc) {

/*IFJ*/ int bc, nf, lp, ip, cp, sl;
//IFC// int bc, nf, lp, ip, cp, sl, sp;

        lp = StackChunk_getLastLp(cc);                          // Setup lp
        ip = Frame_getCurrentIp(lp);                            // Setup cp
        cp = Frame_getCurrentCp(lp);                            // Setup ip
        sp = lp + Frame_getStackOffset(lp);                     // Setup sp
        sl = cc + StackChunk_getSize(cc);                       // Setup sl
        bc = TIMEQUANTA;                                        // Setup bc
        fn = VMExtension_yeild_9;                               // Setup nf

        cc->lastlp = 0;                                         // Clear last frame pointer

        for (;;) {
            int b = fetchUnsignedByte(ip++);
            assume(fn == VMExtension_yeild_9);
            switch (b) {

                /*-----------------------------------------------------------*\
                 *                          Constants                        *
                \*-----------------------------------------------------------*/

                case OPC_CONST_0:
                case OPC_CONST_1:
                case OPC_CONST_2:
                case OPC_CONST_3:
                case OPC_CONST_4:
                case OPC_CONST_5:
                case OPC_CONST_6:
                case OPC_CONST_7:
                case OPC_CONST_8:
                case OPC_CONST_9:
                case OPC_CONST_10:
                case OPC_CONST_11:
                case OPC_CONST_12:
                case OPC_CONST_13:
                case OPC_CONST_14:
                case OPC_CONST_15:
                    push(b&15);
                    continue;

                case OPC_CONST_BYTE:
                    push(fetchByte(ip++));
                    continue;

                case OPC_CONST_SHORT:
                    push(fetchShort(ip)); ip += 2;
                    continue;

                case OPC_CONST_CHAR:
                    push(fetchUnsignedShort(ip)); ip += 2;
                    continue;

                case OPC_CONST_INT:
                case OPC_CONST_FLOAT:
                    push(fetchInt(ip));   ip += 4;
                    continue;

                case OPC_CONST_LONG:
                case OPC_CONST_DOUBLE:
                    push(fetchInt(ip));   ip += 4;
                    push(fetchInt(ip));   ip += 4;
                    continue;

                case OPC_CONST_M1:
                    push(-1);
                    continue;


                /*-----------------------------------------------------------*\
                 *                     Object refererences                   *
                \*-----------------------------------------------------------*/

                case OPC_OBJECT_0:
                case OPC_OBJECT_1
                case OPC_OBJECT_2:
                case OPC_OBJECT_3:
                case OPC_OBJECT_4:
                case OPC_OBJECT_5:
                case OPC_OBJECT_6:
                case OPC_OBJECT_7:
                case OPC_OBJECT_8:
                case OPC_OBJECT_9:
                case OPC_OBJECT_10:
                case OPC_OBJECT_11:
                case OPC_OBJECT_12:
                case OPC_OBJECT_13:
                case OPC_OBJECT_14:
                case OPC_OBJECT_15:
                    int refs = Class_getObjectReferences(cp);
                    push(getWord(refs, b&15));
                    continue;

                case OPC_OBJECT:
                    int refs = Class_getObjectReferences(cp);
                    push(getWord(refs, fetchUnsignedByte(ip++)));
                    continue;


                /*-----------------------------------------------------------*\
                 *                      Class refererences                   *
                \*-----------------------------------------------------------*/

                case OPC_CLASS_0:
                case OPC_CLASS_1:
                case OPC_CLASS_2:
                case OPC_CLASS_3:
                case OPC_CLASS_4:
                case OPC_CLASS_5:
                case OPC_CLASS_6:
                case OPC_CLASS_7:
                case OPC_CLASS_8:
                case OPC_CLASS_9:
                case OPC_CLASS_10:
                case OPC_CLASS_11:
                case OPC_CLASS_12:
                case OPC_CLASS_13:
                case OPC_CLASS_14:
                case OPC_CLASS_15:
                    int refs = Class_getClassReferences(cp);
                    int type = getUnsignedHalf(refs, b&15);
                    push(getClassFromCNO(type));
                    continue;

                case OPC_CLASS:
                    int refs = Class_getClassReferences(cp);
                    int type = getUnsignedHalf(refs, fetchUnsignedByte(ip++));
                    push(getClassFromCNO(type));
                    continue;


                /*-----------------------------------------------------------*\
                 *                    Local variable loads                   *
                \*-----------------------------------------------------------*/

                case OPC_LOAD_0:
                case OPC_LOAD_1:
                case OPC_LOAD_2:
                case OPC_LOAD_3:
                case OPC_LOAD_4:
                case OPC_LOAD_5:
                case OPC_LOAD_6:
                case OPC_LOAD_7:
                case OPC_LOAD_8:
                case OPC_LOAD_9:
                case OPC_LOAD_10:
                case OPC_LOAD_11:
                case OPC_LOAD_12:
                case OPC_LOAD_13:
                case OPC_LOAD_14:
                case OPC_LOAD_15:
                    push(Frame_getLocal(lp, b&15));
                    continue;

                case OPC_LOAD:
                    push(Frame_getLocal(lp, fetchUnsignedByte(ip++)));
                    continue;

                case OPC_LOAD_I2: {
                    int parm = fetchUnsignedByte(ip++);
                    push(Frame_getLocal(lp, parm));
                    push(Frame_getLocal(lp, parm+1))
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                    Local variable stores                  *
                \*-----------------------------------------------------------*/

                case OPC_STORE_0:
                case OPC_STORE_1:
                case OPC_STORE_2:
                case OPC_STORE_3:
                case OPC_STORE_4:
                case OPC_STORE_5:
                case OPC_STORE_6:
                case OPC_STORE_7:
                case OPC_STORE_8:
                case OPC_STORE_9:
                case OPC_STORE_10:
                case OPC_STORE_11:
                case OPC_STORE_12:
                case OPC_STORE_13:
                case OPC_STORE_14:
                case OPC_STORE_15:
                    Frame_setLocal(lp, b&15, pop());
                    continue;

                case OPC_STORE:
                    Frame_setLocal(lp, fetchUnsignedByte(ip++), pop());
                    continue;

                case OPC_STORE_I2: {
                    int parm = fetchUnsignedByte(ip++);
                    Frame_setLocal(lp, parm+1, pop());
                    Frame_setLocal(lp, parm,   pop());
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                   Inc/dec local variables                 *
                \*-----------------------------------------------------------*/

                case OPC_INC: {
                    int parm = fetchUnsignedByte(ip++);
                    Frame_setLocal(lp, parm, Frame_getLocal(lp, parm) + 1);
                    continue;
                }

                case OPC_DEC:  {
                    int parm = fetchUnsignedByte(ip++);
                    Frame_setLocal(lp, parm, Frame_getLocal(lp, parm) - 1);
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *               Wide instructions (only branches)           *
                \*-----------------------------------------------------------*/

                case OPC_WIDE_0:
                case OPC_WIDE_1:
                case OPC_WIDE_2:
                case OPC_WIDE_3:
                case OPC_WIDE_4:
                case OPC_WIDE_5:
                case OPC_WIDE_6:
                case OPC_WIDE_7:
                case OPC_WIDE_8:
                case OPC_WIDE_9:
                case OPC_WIDE_10:
                case OPC_WIDE_11:
                case OPC_WIDE_12:
                case OPC_WIDE_13:
                case OPC_WIDE_14:
                case OPC_WIDE_15:
                case OPC_WIDE_HALF: {
                    int offset;
                    if (b == OPC_WIDE_HALF) {
                        b = fetchUnsignedByte(ip++);
                        offset = getSignedHalf(ip);
                        ip += 2;
                    } else {
                        int wide_15 = (b&15) << 8;
                        b = fetchUnsignedByte(ip++);
                        offset = wide_15 | fetchUnsignedByte(ip++);
                        offset = offset << 24 >> 24;
                    }
                    switch (b) {
                        case OPC_IFEQ:          if (pop() == 0)     ip += offset;   break;
                        case OPC_IFNE:          if (pop() != 0)     ip += offset;   break;
                        case OPC_IFLT:          if (pop() <  0)     ip += offset;   break;
                        case OPC_IFLE:          if (pop() <= 0)     ip += offset;   break;
                        case OPC_IFGT:          if (pop() >  0)     ip += offset;   break;
                        case OPC_IFGE:          if (pop() >= 0)     ip += offset;   break;
                        case OPC_IF_ICMPEQ:     if (pop() == pop()) ip += offset;   break;
                        case OPC_IF_ICMPNE:     if (pop() != pop()) ip += offset;   break;
                        case OPC_IF_ICMPLT:     if (pop() >= pop()) ip += offset;   break;
                        case OPC_IF_ICMPLE:     if (pop() >  pop()) ip += offset;   break;
                        case OPC_IF_ICMPGT:     if (pop() <= pop()) ip += offset;   break;
                        case OPC_IF_ICMPGE:     if (pop() <  pop()) ip += offset;   break;
                        case OPC_GOTO:                              ip += offset;   break;

                        default:                fatal("Bad wide bytecode");
                    }
                    if (--bc > 0) {
                        continue
                    } else {
                        break;
                    }


                /*-----------------------------------------------------------*\
                 *                    Normal branches                        *
                \*-----------------------------------------------------------*/

                case OPC_IFEQ:          if (pop() == 0)     ip += fetchByte(ip++); if (--bc > 0) continue; else break;
                case OPC_IFNE:          if (pop() != 0)     ip += fetchByte(ip++); if (--bc > 0) continue; else break;
                case OPC_IFLT:          if (pop() <  0)     ip += fetchByte(ip++); if (--bc > 0) continue; else break;
                case OPC_IFLE:          if (pop() <= 0)     ip += fetchByte(ip++); if (--bc > 0) continue; else break;
                case OPC_IFGT:          if (pop() >  0)     ip += fetchByte(ip++); if (--bc > 0) continue; else break;
                case OPC_IFGE:          if (pop() >= 0)     ip += fetchByte(ip++); if (--bc > 0) continue; else break;
                case OPC_IF_ICMPEQ:     if (pop() == pop()) ip += fetchByte(ip++); if (--bc > 0) continue; else break;
                case OPC_IF_ICMPNE:     if (pop() != pop()) ip += fetchByte(ip++); if (--bc > 0) continue; else break;
                case OPC_IF_ICMPLT:     if (pop() >= pop()) ip += fetchByte(ip++); if (--bc > 0) continue; else break;
                case OPC_IF_ICMPLE:     if (pop() >  pop()) ip += fetchByte(ip++); if (--bc > 0) continue; else break;
                case OPC_IF_ICMPGT:     if (pop() <= pop()) ip += fetchByte(ip++); if (--bc > 0) continue; else break;
                case OPC_IF_ICMPGE:     if (pop() <  pop()) ip += fetchByte(ip++); if (--bc > 0) continue; else break;
                case OPC_GOTO:                              ip += fetchByte(ip++); if (--bc > 0) continue; else break;

                case OPC_STABLESWITCH_PAD:
                    ip++;
                case OPC_STABLESWITCH: {
                    int key, off, low, hi;
                    key = pop();
                    off = fetchShort(ip);   ip += 2;
                    low = fetchInt(ip);          ip += 4;
                    hi  = fetchInt(ip);          ip += 4;
                    if (key >= low && key <= hi) {
                        off = fetchShort(ip+((key-low)*2));
                    }
                    ip += off;
                    continue;
                }

                /*-----------------------------------------------------------*\
                 *                          Lookups                          *
                \*-----------------------------------------------------------*/

                case OPC_LOOKUP: {
                    int key, ref, lth, i, res = -1;
                    ref = pop();
                    key = pop();
                    lth = Object_getLength(ref);
                    for (i = 0 ; i < lth ; i++) {
                        int val = getWord(ref, i);
                        if (val == key) {
                            res = i;
                            break;
                        }
                    }
                    push(res);
                    continue;
                }

                case OPC_LOOKUP_B: {
                    int key, ref, lth, i, res = -1;
                    ref = pop();
                    key = pop();
                    lth = Object_getLength(ref);
                    for (i = 0 ; i < lth ; i++) {
                        int val = getByte(ref, i);
                        if (val == key) {
                            res = i;
                            break;
                        }
                    }
                    push(res);
                    continue;
                }

                case OPC_LOOKUP_S: {
                    int key, ref, lth, i, res = -1;
                    ref = pop();
                    key = pop();
                    lth = Object_getLength(ref);
                    for (i = 0 ; i < lth ; i++) {
                        int val = getHalf(ref, i);
                        if (val == key) {
                            res = i;
                            break;
                        }
                    }
                    push(res);
                    continue;
                }

                case OPC_LOOKUP_C: {
                    int key, ref, lth, i, res = -1;
                    ref = pop();
                    key = pop();
                    lth = Object_getLength(ref);
                    for (i = 0 ; i < lth ; i++) {
                        int val = getUnsignedHalf(ref, i);
                        if (val == key) {
                            res = i;
                            break;
                        }
                    }
                    push(res);
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                         Invokes                           *
                \*-----------------------------------------------------------*/

                case OPC_INVOKEVIRTUAL:
                case OPC_INVOKEVIRTUAL1:
                case OPC_INVOKEVIRTUAL2: {
                    int mp;
                    int rcvr;
                    int vtbl;
                    int vstart;
                    int parm = fetchUnsignedByte(ip++);                 // Get the "number" operand of invoke
                    int oldlp = lp;                                     // Copy caller's lp
                    Frame_setCurrentIp(lp, ip);                         // Save current ip
                    lp = lp + Frame_getStackOffset(lp);                 // Make lp point to the start of the stack area
                    Frame_setCurrentLp(lp, oldlp);                      // Store the caller's lp into the new frame

                    rcvr = Frame_getLocal(lp, 0);                       // Get the receiver
                    if (rcvr == 0) {                                    // Test for zero
                        fn = VMExtension_nullPointerException_4;        // Y - set error code
                        break;                                          // Jump
                    }                                                   // ...

                    klass = cp = Object_getClass(rcvr);                 // Get the callee's class
                    vstart = Class_getFirstVirtualMethod(klass);        // Get the number of the first method defined
                    while (parm < vstart) {                             // Is the target method there?
                        klass  = Class_getSuperKlass(klass);            // Go to the super class
                        vstart = Class_getFirstVirtualMethod(klass);    // Get the number of the first method defined
                    }                                                   // Test again...
                    vtbl = Class_getVirtualMethods(klass);              // Get the vtable for virtual methods
                    mp = getWord(vtbl, parm-vstart);                    // Get the method

                    Frame_setCurrentMp(lp, mp);                         // Save the current method
                    Frame_setCurrentCp(lp, cp);                         // Save the current class
                    ip = mp + getUnsignedByte(mp, 0);                   // Set the ip to start executing the first bytecode
                    continue;
                }

                case OPC_INVOKESUPER:
                case OPC_INVOKESUPER1:
                case OPC_INVOKESUPER2: {
                    int mp;
                    int vtbl;
                    int vstart;
                    int parm = fetchUnsignedByte(ip++);                 // Get the "number" operand of invoke
                    int oldlp = lp;                                     // Copy caller's lp
                    Frame_setCurrentIp(lp, ip);                         // Save current ip
                    lp = lp + Frame_getStackOffset(lp);                 // Make lp point to the start of the stack area
                    Frame_setCurrentLp(lp, oldlp);                      // Store the caller's lp into the new frame

                    cp = pop();                                         // Get the callee's class
                    vstart = Class_getFirstVirtualMethod(cp);           // Get the number of the first method defined
                    while (parm < vstart) {                             // Is the target method there?
                        klass  = Class_getSuperKlass(klass);            // Go to the super class
                        vstart = Class_getFirstVirtualMethod(klass);    // Get the number of the first method defined
                    }                                                   // Test again...
                    vtbl = Class_getVirtualMethods(klass);              // Get the vtable for virtual methods
                    mp = getWord(vtbl, parm-vstart);                    // Get the method

                    Frame_setCurrentMp(lp, mp);                         // Save the current method
                    Frame_setCurrentCp(lp, cp);                         // Save the current class
                    ip = mp + getUnsignedByte(mp, 0);                   // Set the ip to start executing the first bytecode
                    continue;
                }

                case OPC_INVOKEINIT:
                case OPC_INVOKEINIT1:
                case OPC_INVOKEINIT2:
                case OPC_INVOKESTATIC:
                case OPC_INVOKESTATIC1:
                case OPC_INVOKESTATIC2: {
                    int mp;
                    int vtbl;
                    int parm = fetchUnsignedByte(ip++);                 // Get the "number" operand of invoke
                    int oldlp = lp;                                     // Copy caller's lp
                    Frame_setCurrentIp(lp, ip);                         // Save current ip
                    lp = lp + Frame_getStackOffset(lp);                 // Make lp point to the start of the stack area
                    Frame_setCurrentLp(lp, oldlp);                      // Store the caller's lp into the new frame

                    cp = pop();                                         // Get the callee's class
                    vtbl = Class_getStaticMethods(cp);                  // Get the vtable for virtual methods
                    mp = getWord(vtbl, parm);                           // Get the method

                    Frame_setCurrentMp(lp, mp);                         // Save the current method
                    Frame_setCurrentCp(lp, cp);                         // Save the current class
                    ip = mp + getUnsignedByte(mp, 0);                   // Set the ip to start executing the first bytecode
                    continue;
                }

                case OPC_INVOKEINTERFACE:
                case OPC_INVOKEINTERFACE1:
                case OPC_INVOKEINTERFACE2: {
                    int mp;
                    int iklass;
                    int rcvr;
                    int vtbl;
                    int vstart;
                    int parm = fetchUnsignedByte(ip++);                 // Get the "number" operand of invoke
                    int oldlp = lp;                                     // Copy caller's lp
                    Frame_setCurrentIp(lp, ip);                         // Save current ip
                    lp = lp + Frame_getStackOffset(lp);                 // Make lp point to the start of the stack area
                    Frame_setCurrentLp(lp, oldlp);                      // Store the caller's lp into the new frame

                    iklass = pop();                                     // Get the class of the interface
                    rcvr = Frame_getLocal(lp, 0);                       // Get the receiver
                    if (rcvr == 0) {                                    // Test for zero
                        fn = VMExtension_nullPointerException_4;        // Y - set error code
                        break;                                          // Jump
                    }

                    klass = cp = Object_getClass(rcvr);                 // Get the callee's class
                    klassType = Class_getType(klass);                   // Get the klass cno

                    types   = Class_getInterfaceTypes(cp);              // Get the interface types table
                    tlength = Object_getLength(types);                  // Get the interface types table length
                    for (i = 0 ; assume(i < tlength) ; i++) {           // Iterate through the interface types
                        int cno = getUnsignedShort(types, i);           // Get the next type
                        if (cno == klassType) {                         // Match?
                            int tables = Class_getInterfaceSlotTables(cp); // Y - Get the slot tabes
                            int slotTable = getWord(tables, i);         // Get specific table
                            parm = getUnsignedByte(slotTable, parm);    // Get virtual method
                        }                                               //
                    }                                                   //

                    klass = cp = Object_getClass(rcvr);                 // Get the callee's class
                    vstart = Class_getFirstVirtualMethod(klass);        // Get the number of the first method defined
                    while (parm < vstart) {                             // Is the target method there
                        klass  = Class_getSuperKlass(klass);            // N - Go to the super class
                        vstart = Class_getFirstVirtualMethod(klass);    // Get the number of the first method defined
                    }                                                   // Test again...
                    vtbl = Class_getVirtualMethods(klass);              // Get the vtable for virtual methods
                    mp = getWord(vtbl, parm-vstart);                    // Get the method

                    Frame_setCurrentMp(lp, mp);                         // Save the current method
                    Frame_setCurrentCp(lp, cp);                         // Save the current class
                    ip = mp + getUnsignedByte(mp, 0);                   // Set the ip to start executing the first bytecode
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                         Extend                            *
                \*-----------------------------------------------------------*/

                case OPC_EXTEND_0:
                case OPC_EXTEND_1:
                case OPC_EXTEND_2:
                case OPC_EXTEND_3:
                case OPC_EXTEND_4:
                case OPC_EXTEND_5:
                case OPC_EXTEND_6:
                case OPC_EXTEND_7:
                case OPC_EXTEND_8:
                case OPC_EXTEND_9:
                case OPC_EXTEND_10:
                case OPC_EXTEND_11:
                case OPC_EXTEND_12:
                case OPC_EXTEND_13:
                case OPC_EXTEND_14:
                case OPC_EXTEND_15:
                case OPC_EXTEND: {
                    int cs;
                    int restartIP = ip - 1;
                    int nstack = (b == OPC_EXTEND) ? fetchUnsignedByte(ip++) : b&15;
                    int mp = Frame_getCurrentMp(lp);
                    int nlocals = getUnsignedByte(mp, MTH_locals);

                    int zp = lp + nlocals;                              // Point past last local
                    int ep = zp + FSIZE + nstack;                       // Point past the last word used
                    if (ep > sl) {                                      // Overflow?
                        int minsize = ep-lp+FSIZE+CHUNKHEADERSIZE;      // Work out the minimum needed
                        int cc = getCurrentChunk(lp);                   // Get the current chunk
                        int nc = StackChunk_getNext(cc);                // Get the next chumk if there is one
                        StackChunk_setNext(cc, 0)                       // Clear pointer
                        if (nc == null || StackChunk_getSize(nc) < minsize) { // Large enough?
                            nc = allocChuck(minsize);                   // Allocate chunk
                        }
                        if (nc == null) {                               // Failed?
                            Frame_setCurrentIp(lp, restartIP);          // Set to re-execute the extend
                            Frame_setStackOffset(lp, sp - lp);          // Save the stack offset
                            StackChunk_setLastLp(cc, lp);               // Save the lp
                            return cc;                                  // Go do gc
                        }
                        sl = nc + StackChunk_getSize(nc);               // Set up the sl for this chunk
                        StackChunk_setPrev(nc, cc);                     // Save the previous chunk address
                        StackChunk_setLastLp(nc, 0);                    // Needed?
                        StackChunk_setNext(cc, nc);                     // Save the next chunk
                        StackChunk_setLastLp(cc, Frame_getPreviousLp(lp)); // Save the last real frame
                        nlp = nc + CHUNKHEADERSIZE+FSIZE;               // Get the next frame
                        nlp->p_lp = 0;                                  // This marks the first frame in the chunk

                        Frame_setCurrentMp(nlp, Frame_getCurrentMp(lp));// Copy from old frame
                        Frame_setCurrentIp(nlp, Frame_getCurrentIp(lp));// ...
                        Frame_setCurrentCp(nlp, Frame_getCurrentCp(lp));// ...

                        int nsp = nlp;                                  // Get start of param
                        while (lp != sp) {                              // Copy parms
                            setWord(nsp, 0, getWord(lp, 0));            // ...
                            nsp += ADDRESS_UNITS;                       // ...
                            lp  += ADDRESS_UNITS;                       // ...
                        }                                               //
                        lp = nlp;                                       // Get the new lp
                        sp = nsp;                                       // Get the new sp
                        zp = lp + nlocals;                              // Point past last local
                    }
                    while (sp != zp) {                                  // Zero locals
                        setWord(sp, 0, 0);                              // ...
                        sp += ADDRESS_UNITS;                            // ...
                    }                                                   //
                    sp += FSIZE;                                        // Skip frame
                    Frame_setStackOffset(lp, sp - lp);                  // Save the stack offset


                   /*
                    * Finally cause <clinit> to be called if the class has not yet been initialized
                    */
                    cs = getWord(getIsolateStateTable(), Class_getCno(cp) & 15);
                    if (cs != 0 && ClassState_getKlass(cs) != cls) {
                        cs = findClassState(cls);
                    }
                    if (cs == 0 || ClassState_getInitializingThread(cs) != 0) {
                        push(cp);
                        fn = VMExtension_doClinit_1;
                        break;
                    }
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                         Returns                           *
                \*-----------------------------------------------------------*/

                case OPC_RETURN:
                case OPC_RETURN1:
                case OPC_RETURN2: {

                    int oldsp = sp;                                     // Save
                    int plp = Frame_getPreviousLp(lp);                  // get previous lp
                    if (plp != 0) {                                     // zero?
                        sp = lp;                                        // n - then set sp to our lp
                    } else {
                        int cc = getCurrentChunk(lp);                   // get current chunk
                        int pc = StackChunk_getPrev(cc);                // get previous chunk
                        assume(pc != null);
                        sl  = pc + StackChunk_getSize(pc);              // reset sl
                        plp = StackChunk_getLastLp(pc);                 // get previous frame
                        pc->lastlp = 0;                                 // clear last frame pointer
                        sp = plp + Frame_getStackOffset(plp);           // setup sp
                    }
                    lp = plp;                                           // get the previous lp
                    cp = lp->c_cp;                                      // reset cp
                    ip = lp->c_ip;                                      // get next ip

                    switch (b) {
                        case OPC_RETURN2: {
                            int temp1 = getWord(oldsp, -2);
                            int temp2 = getWord(oldsp, -1);
                            push(temp1);
                            push(temp2);
                            continue;
                        }

                        case OPC_RETURN1: {
                            int temp1 = getWord(oldsp, -1);
                            push(temp1);
                            continue;
                        }
                    }
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                       New objects                         *
                \*-----------------------------------------------------------*/

                case OPC_NEWOBJECT: {
                    int restartIP = ip - 1;                             // Get restart address
                    int obj = allocObjectForClass(cp);                  // Allocate object
                    if (obj == 0) {                                     // OK?
                        Frame_setCurrentIp(lp, restartIP);              // Set to re-execute the extend
                        Frame_setStackOffset(lp, sp - lp);              // Save the stack offset
                        StackChunk_setLastLp(cc, lp);                   // Save the lp
                        return cc;                                      // Go do gc
                    }
                    push(obj);                                          // Push result
                    continue;
                }

                case OPC_NEWARRAY:
                    fn = VMExtension_newArray_17;
                    break;

                case OPC_NEWDIMENSION:
                    fn = VMExtension_newDimension_18;
                    break;


                /*-----------------------------------------------------------*\
                 *                       Array loads                         *
                \*-----------------------------------------------------------*/



                case OPC_ALOAD: {
                    aload(getWord)
                    continue;





                case OPC_ALOAD: {
                    int ndx = pop();
                    int ref = pop();
                    int lth;
                    if (ref == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    if (ndx < 0 || ndx >= Object_getLength(ref)) {
                        fn = VMExtension_arrayBoundsException_3;
                        break;
                    }
                    push(getWord(ref, ndx));
                    continue;
                }

                case OPC_ALOAD_B: {
                    int ndx = pop();
                    int ref = pop();
                    int lth;
                    if (ref == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    if (ndx < 0 || ndx >= Object_getLength(ref)) {
                        fn = VMExtension_arrayBoundsException_3;
                        break;
                    }
                    push(getByte(ref, ndx));
                    continue;
                }

                case OPC_ALOAD_S: {
                    int ndx = pop();
                    int ref = pop();
                    int lth;
                    if (ref == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    if (ndx < 0 || ndx >= Object_getLength(ref)) {
                        fn = VMExtension_arrayBoundsException_3;
                        break;
                    }
                    push(getHalf(ref, ndx));
                    continue;
                }

                case OPC_ALOAD_C: {
                    int ndx = pop();
                    int ref = pop();
                    int lth;
                    if (ref == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    if (ndx < 0 || ndx >= Object_getLength(ref)) {
                        fn = VMExtension_arrayBoundsException_3;
                        break;
                    }
                    push(getUnsignedHalf(ref, ndx));
                    continue;
                }

                case OPC_ALOAD_I2: {
                    int ndx = pop();
                    int ref = pop();
                    int lth;
                    if (ref == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    if (ndx < 0 || ndx >= Object_getLength(ref)) {
                        fn = VMExtension_arrayBoundsException_3;
                        break;
                    }
                    push(getWord(ref, ndx*2));
                    push(getWord(ref, ndx*2+1));
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                       Array stores                        *
                \*-----------------------------------------------------------*/

                case OPC_ASTORE: {
                    int val = pop();
                    int ndx = pop();
                    int ref = pop();
                    int lth;
                    if (ref == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    if (ndx < 0 || ndx >= Object_getLength(ref)) {
                        fn = VMExtension_arrayBoundsException_3;
                        break;
                    }
                    setWord(ref, ndx, val);
                    continue;
                }

                case OPC_ASTORE_B: {
                    int val = pop();
                    int ndx = pop();
                    int ref = pop();
                    int lth;
                    if (ref == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    if (ndx < 0 || ndx >= Object_getLength(ref)) {
                        fn = VMExtension_arrayBoundsException_3;
                        break;
                    }
                    setByte(ref, ndx, val);
                    continue;
                }

                case OPC_ASTORE_S: {
                    int val = pop();
                    int ndx = pop();
                    int ref = pop();
                    int lth;
                    if (ref == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    if (ndx < 0 || ndx >= Object_getLength(ref)) {
                        fn = VMExtension_arrayBoundsException_3;
                        break;
                    }
                    setHalf(ref, ndx, val);
                    continue;
                }

                case OPC_ASTORE_O: {
                    int val = pop();
                    int ndx = pop();
                    int ref = pop();
                    int lth;
                    if (ref == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    if (ndx < 0 || ndx >= Object_getLength(ref)) {
                        fn = VMExtension_arrayBoundsException_3;
                        break;
                    }
                    setOop(ref, ndx, val);
                    continue;
                }

                case OPC_ASTORE_I2: {
                    int val2 = pop();
                    int val1 = pop();
                    int ndx  = pop();
                    int ref  = pop();
                    int lth;
                    if (ref == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    if (ndx < 0 || ndx >= Object_getLength(ref)) {
                        fn = VMExtension_arrayBoundsException_3;
                        break;
                    }
                    setWord(ref, ndx*2,   val1);
                    setWord(ref, ndx*2+1, val2);
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                        Getfields                          *
                \*-----------------------------------------------------------*/

                case OPC_THIS_GETFIELD:
                    push(Frame_getLocal(lp, 0));
                case OPC_GETFIELD: {
                    int obj = pop();
                    if (obj == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    push(getWord(obj, fetchUnsignedByte(ip++));
                    continue;
                }

                case OPC_THIS_GETFIELD_B:
                    push(Frame_getLocal(lp, 0));
                case OPC_GETFIELD_B: {
                    int obj = pop();
                    if (obj == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    push(getByte(obj, fetchUnsignedByte(ip++));
                    continue;
                }

                case OPC_THIS_GETFIELD_S:
                    push(Frame_getLocal(lp, 0));
                case OPC_GETFIELD_S: {
                    int obj = pop();
                    if (obj == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    push(getHalf(obj, fetchUnsignedByte(ip++));
                    continue;
                }

                case OPC_THIS_GETFIELD_C:
                    push(Frame_getLocal(lp, 0));
                case OPC_GETFIELD_C: {
                    int obj = pop();
                    if (obj == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    push(getUnsignedHalf(obj, fetchUnsignedByte(ip++));
                    continue;
                }

                case OPC_THIS_GETFIELD_I2:
                    push(Frame_getLocal(lp, 0));
                case OPC_GETFIELD_I2: {
                    int obj = pop();
                    int offset;
                    if (obj == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    offset = fetchUnsignedByte(ip++);
                    push(getWord(obj, offset);
                    push(getWord(obj, offset+1);
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                        Putfields                          *
                \*-----------------------------------------------------------*/

                case OPC_THIS_PUTFIELD: {
                    int val = pop();
                    push(Frame_getLocal(lp, 0));
                    push(val);
                }
                case OPC_PUTFIELD: {
                    int val = pop();
                    int obj = pop();
                    if (obj == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    setWord(obj, fetchUnsignedByte(ip++), val);
                    continue;
                }

                case OPC_THIS_PUTFIELD_B: {
                    int val = pop();
                    push(Frame_getLocal(lp, 0));
                    push(val);
                }
                case OPC_PUTFIELD_B: {
                    int val = pop();
                    int obj = pop();
                    if (obj == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    setByte(obj, fetchUnsignedByte(ip++), val);
                    continue;
                }

                case OPC_THIS_PUTFIELD_S: {
                    int val = pop();
                    push(Frame_getLocal(lp, 0));
                    push(val);
                }
                case OPC_PUTFIELD_S: {
                    int val = pop();
                    int obj = pop();
                    if (obj == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    setHalf(obj, fetchUnsignedByte(ip++), val);
                    continue;
                }

                case OPC_THIS_PUTFIELD_O: {
                    int val = pop();
                    push(Frame_getLocal(lp, 0));
                    push(val);
                }
                case OPC_PUTFIELD_O: {
                    int val = pop();
                    int obj = pop();
                    if (obj == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    setOop(obj, fetchUnsignedByte(ip++), val);
                    continue;
                }

                case OPC_THIS_PUTFIELD_I2: {
                    int val1 = pop();
                    int val2 = pop();
                    push(Frame_getLocal(lp, 0));
                    push(val2);
                    push(val1);
                }
                case OPC_PUTFIELD_I2: {
                    int val2 = pop();
                    int val1 = pop();
                    int obj = pop();
                    int offset;
                    if (obj == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    offset = fetchUnsignedByte(ip++);
                    setWord(obj, offset,   val1);
                    setWord(obj, offset+1, val2);
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                        Getstatics                         *
                \*-----------------------------------------------------------*/

                case OPC_CLASS_GETSTATIC:
                case OPC_CLASS_GETSTATIC_O:
                    push(cp);
                case OPC_GETSTATIC: {
                case OPC_GETSTATIC_O:
                    int offset = fetchUnsignedByte(ip++);
                    int cls = pop();
                    int cs = getWord(getIsolateStateTable(), Class_getCno(cls) & 15);
                    if (cs != 0 && ClassState_getKlass(cs) != cls) {
                        cs = findClassState(cls);
                    }
                    if (cs != 0 && ClassState_getInitializingThread(cs) == 0) {
                        push(getWord(cs, offset));
                        continue;
                    } else {
                        push(offset);
                        push(cls);
                        fn = VMExtension_getStatic_10;
                        break;
                    }
                }


                case OPC_CLASS_GETSTATIC_I2:
                    push(cp);
                case OPC_GETSTATIC_I2:
                    int offset = fetchUnsignedByte(ip++);
                    int cls = pop();
                    int cs = getWord(getIsolateStateTable(), klassType & 15);
                    if (cs != 0 && ClassState_getKlass(cs) != cls) {
                        cs = findClassState(cls);
                    }
                    if (cs != 0 && ClassState_getInitializingThread(cs) == 0) {
                        push(getWord(cs, offset));
                        push(getWord(cs, offset+1));
                        continue;
                    } else {
                        push(offset);
                        push(cls);
                        fn = VMExtension_getStaticLong_11;
                        break;
                    }
                }


                /*-----------------------------------------------------------*\
                 *                        Putstatics                         *
                \*-----------------------------------------------------------*/

                case OPC_CLASS_PUTSTATIC:
                    push(cp);
                case OPC_PUTSTATIC:
                    int offset = fetchUnsignedByte(ip++);
                    int cls = pop();
                    int val = pop();
                    int cs = getWord(getIsolateStateTable(), klassType & 15);
                    if (cs != 0 && ClassState_getKlass(cs) != cls) {
                        cs = findClassState(cls);
                    }
                    if (cs != 0 && ClassState_getInitializingThread(cs) == 0) {
                        setWord(cs, offset, val);
                        continue;
                    } else {
                        push(offset);
                        push(val);
                        push(cls);
                        fn = VMExtension_putStatic_12;
                        break;
                    }
                }


                case OPC_CLASS_PUTSTATIC_O:
                    push(cp);
                case OPC_PUTSTATIC_O:
                    int offset = fetchUnsignedByte(ip++);
                    int cls = pop();
                    int val = pop();
                    int cs = getWord(getIsolateStateTable(), klassType & 15);
                    if (cs != 0 && ClassState_getKlass(cs) != cls) {
                        cs = findClassState(cls);
                    }
                    if (cs != 0 && ClassState_getInitializingThread(cs) == 0) {
                        setOop(cs, offset, val);
                        continue;
                    } else {
                        push(offset);
                        push(val);
                        push(cls);
                        fn = VMExtension_putStaticObj_14;
                        break;
                    }
                }

                case OPC_CLASS_PUTSTATIC_I2:
                    push(cp);
                case OPC_PUTSTATIC_I2:
                    int offset = fetchUnsignedByte(ip++);
                    int cls = pop();
                    int val2 = pop();
                    int val1 = pop();
                    int cs = getWord(getIsolateStateTable(), klassType & 15);
                    if (cs != 0 && ClassState_getKlass(cs) != cls) {
                        cs = findClassState(cls);
                    }
                    if (cs != 0 && ClassState_getInitializingThread(cs) == 0) {
                        setWord(cs, offset,   val1);
                        setWord(cs, offset+1, val2);
                        continue;
                    } else {
                        push(offset);
                        push(val1);
                        push(val2);
                        push(cls);
                        fn = VMExtension_putStaticLong_13;
                        break;
                    }
                }



                /*-----------------------------------------------------------*\
                 *                      Integer ops                          *
                \*-----------------------------------------------------------*/

                case OPC_IADD:  { int r = pop() ; int l = pop() ; push(l + r);      continue; }
                case OPC_ISUB:  { int r = pop() ; int l = pop() ; push(l - r);      continue; }
                case OPC_IAND:  { int r = pop() ; int l = pop() ; push(l & r);      continue; }
                case OPC_IOR:   { int r = pop() ; int l = pop() ; push(l | r);      continue; }
                case OPC_IXOR:  { int r = pop() ; int l = pop() ; push(l ^ r);      continue; }
                case OPC_ISHL:  { int r = pop() ; int l = pop() ; push(l << r);     continue; }
                case OPC_ISHR:  { int r = pop() ; int l = pop() ; push(l >> r);     continue; }
                case OPC_IUSHR: { int r = pop() ; int l = pop() ; push(srl(l, r));  continue; }
                case OPC_IMUL:  { int r = pop() ; int l = pop() ; push(l * r);      continue; }
                case OPC_NEG:   { int r = pop() ;                 push(0 - r);      continue; }
                case OPC_I2B:   { int r = pop() ;                 push((byte)r);    continue; }
                case OPC_I2S:   { int r = pop() ;                 push((short)r);   continue; }
                case OPC_I2C:   { int r = pop() ;                 push((char)r);    continue; }

                case OPC_IDIV: {
                    int r = pop();
                    int l = pop();
                    if (r == 0) {
                        fn = VMExtension_arithmeticException_2;
                        break;
                    }
                    push(r / l);
                    continue;
                }

                case OPC_IREM: {
                    int r = pop();
                    int l = pop();
                    if (r == 0) {
                        fn = VMExtension_arithmeticException_2;
                        break;
                    }
                    push(r % l);
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                        Long ops                           *
                \*-----------------------------------------------------------*/
/*if[LONGS]*/
                case OPC_LONGOP: {
                    b = fetchUnsignedByte(ip++);
                    switch (b) {
                        case OPC_LADD: { long r = popLong() ; long l = popLong() ; pushLong(l + r);     continue; }
                        case OPC_LSUB: { long r = popLong() ; long l = popLong() ; pushLong(l - r);     continue; }
                        case OPC_LMUL: { long r = popLong() ; long l = popLong() ; pushLong(l * r);     continue; }
                        case OPC_LAND: { long r = popLong() ; long l = popLong() ; pushLong(l & r);     continue; }
                        case OPC_LOR:  { long r = popLong() ; long l = popLong() ; pushLong(l | r);     continue; }
                        case OPC_LXOR: { long r = popLong() ; long l = popLong() ; pushLong(l ^ r);     continue; }
                        case OPC_LNEG: { long r = popLong() ;                      pushLong(0 - r);     continue; }
                        case OPC_LSHL: { int  r = pop()     ; long l = popLong() ; pushLong(l << r);    continue; }
                        case OPC_LSHR: { int  r = pop()     ; long l = popLong() ; pushLong(l >> r);    continue; }
                        case OPC_LUSHR:{ int  r = pop()     ; long l = popLong() ; pushLong(srll(l, r));continue; }
                        case OPC_LCMP: { long r = popLong() ; long l = popLong() ; pushLong(cmpl(l, r));continue; }
                        case OPC_I2L:  { int  r = pop()     ;                      pushLong(r);         continue; }
                        case OPC_L2I:  { long r = popLong() ;                      push((int)r);        continue; }

                        case OPC_LDIV: {
                             long r = popLong();
                             long l = popLong();
                             if (l == 0) {
                                 fn = VMExtension_arithmeticException_2;
                                 break;
                             }
                             pushLong(l / r);
                             continue
                        }

                        case OPC_LREM: {
                             long r = popLong();
                             long l = popLong();
                             if (l == 0) {
                                 fn = VMExtension_arithmeticException_2;
                                 break;
                             }
                             pushLong(l % r);
                             continue
                        }
                    }
                    fatal("invalid long bytecode");
                }
/*end[LONGS]*/

                /*-----------------------------------------------------------*\
                 *                        Float ops                          *
                \*-----------------------------------------------------------*/

/*if[FLOATS]*/
                case OPC_FLOATOP: {
                    b = fetchUnsignedByte(ip++);
                    switch (b) {
                        case OPC_FADD: { int  r = pop()     ; int  l = pop()     ; push(addf(l, r));        continue; }
                        case OPC_FSUB: { int  r = pop()     ; int  l = pop()     ; push(subf(l, r));        continue; }
                        case OPC_FMUL: { int  r = pop()     ; int  l = pop()     ; push(mulf(l, r));        continue; }
                        case OPC_FDIV: { int  r = pop()     ; int  l = pop()     ; push(divf(l, r));        continue; }
                        case OPC_FREM: { int  r = pop()     ; int  l = pop()     ; push(remf(l, r));        continue; }
                        case OPC_FNEG: { int  r = pop()     ;                      push(negf(r));           continue; }
                        case OPC_FCMPG:{ int  r = pop()     ; int  l = pop()     ; push(cmpfg(l, r));       continue; }
                        case OPC_FCMPL:{ int  r = pop()     ; int  l = pop()     ; push(cmpfl(l, r));       continue; }
                        case OPC_DADD: { long r = popLong() ; long l = popLong() ; pushLong(addd(l, r));    continue; }
                        case OPC_DSUB: { long r = popLong() ; long l = popLong() ; pushLong(subd(l, r));    continue; }
                        case OPC_DMUL: { long r = popLong() ; long l = popLong() ; pushLong(muld(l, r));    continue; }
                        case OPC_DDIV: { long r = popLong() ; long l = popLong() ; pushLong(divd(l, r));    continue; }
                        case OPC_DREM: { long r = popLong() ; long l = popLong() ; pushLong(remd(l, r));    continue; }
                        case OPC_DNEG: { long r = popLong() ;                      pushLong(negd(l, r));    continue; }
                        case OPC_DCMPG:{ long r = popLong() ; long l = popLong() ; pushLong(cmpdg(l, r));   continue; }
                        case OPC_DCMPL:{ long r = popLong() ; long l = popLong() ; pushLong(cmpdl(l, r));   continue; }
                        case OPC_I2F:  { int  r = pop()     ;                      push(i2f(r));            continue; }
                        case OPC_L2F:  { long r = popLong() ;                      push(l2f(r));            continue; }
                        case OPC_F2I:  { int  r = pop()     ;                      push(f2i(r));            continue; }
                        case OPC_F2L:  { int  r = pop()     ;                      pushLong(f2l(r));        continue; }
                        case OPC_I2D:  { int  r = pop()     ;                      pushLong(i2d(r));        continue; }
                        case OPC_L2D:  { long r = popLong() ;                      pushLong(l2d(r));        continue; }
                        case OPC_F2D:  { int  r = pop()     ;                      pushLong(f2d(r));        continue; }
                        case OPC_D2I:  { long r = popLong() ;                      push(d2i(r));            continue; }
                        case OPC_D2L:  { long r = popLong() ;                      pushLong(d2l(r));        continue; }
                        case OPC_D2F:  { long r = popLong() ;                      push(d2f(r));            continue; }
                        default:
                    }
                    fatal("invalid float bytecode");
                }
/*end[FLOATS]*/

                /*-----------------------------------------------------------*\
                 *                            Misc                           *
                \*-----------------------------------------------------------*/

                case OPC_NOP:
                    continue;

                case OPC_POP:
                    pop();
                    continue;

                case OPC_ARRAYLENGTH: {
                    int ref = pop();
                    if (ref == 0) {
                        fn = VMExtension_nullPointerException_4;
                        break;
                    }
                    push(Object_getLength(ref));
                    continue;
                }

                case OPC_CLASS_MONITORENTER:
                    push(cp);
                    fn = VMExtension_monitorEnter_15;
                    break;

                case OPC_CLASS_MONITOREXIT:
                    push(cp);
                    fn = VMExtension_monitorExit_16;
                    break;

                case OPC_CLINIT:
                    fn = VMExtension_doClinit_1;
                    break;

                case OPC_INSTANCEOF:
                    fn = VMExtension_instanceof_7;
                    break;

                case OPC_CHECKCAST:
                    fn = VMExtension_checkcast_6;
                    break;

                case OPC_BPT:
                    fn = VMExtension_breakpoint_5;
                    break;

                case OPC_THROW:
                    fn = VMExtension_throwException_8;
                    break;


                /*-----------------------------------------------------------*\
                 *                       Invoke native                       *
                \*-----------------------------------------------------------*/

                case OPC_INVOKENATIVE:
                case OPC_INVOKENATIVE1:
                case OPC_INVOKENATIVE2: {
                    int parm = fetchUnsignedByte(ip++);
                    switch (parm) {
                        case Native_asInt_1:
                        case Native_asClass_2:
                        case Native_asOop_3:
                        case Native_asIntArray_4:
                        case Native_asByteArray_5:
                        case Native_floatToIntBits_6:
                        case Native_doubleToLongBits_7:
                        case Native_intBitsToFloat_8:
                        case Native_longBitsToDouble_9:
                            break;

                        case Native_parm_10:
                        case Native_parm_11:
                            break;

                        case Native_exec_12:
                            break;

                        case Native_error_13:
                            break;

                        case Native_result_14:
                            break;

                        case Native_getAR_15:
                            break;

                        case Native_setAR_16:
                            break;

                        case Native_primNewArray_17:
                            int siz = pop();
                            int cls = pop();
                            push(allocArrayObject(cls, siz);
                            break;

                        case Native_callClinitForClass_18:
                            break;

                        case Native_callMainForClass_19:
                            break;
                    }
                    continue;
                }


            } // end of switch


                /*-----------------------------------------------------------*\
                 *                     Exception handling                    *
                \*-----------------------------------------------------------*/


            {
                int mp;
                int vtbl;
                int oldlp = lp;

                Frame_setCurrentIp(lp, ip);                         // Save current ip
                lp = lp + Frame_getStackOffset(lp);                 // Make lp point to the start of the stack area
                Frame_setCurrentLp(lp, oldlp);                      // Store the caller's lp into the new frame

                cp = getClassFromCNO(VMEXTENSION);                  // Setup the class
                vtbl = Class_getStaticMethods(cp);                  // Get the vtable for virtual methods
                mp = getWord(vtbl, fn);                             // Get the method

                Frame_setCurrentMp(lp, mp);                         // Save the current method
                Frame_setCurrentCp(lp, cp);                         // Save the current class
                ip = mp + getUnsignedByte(mp, 0);                   // Set the ip to start executing the first bytecode
            }


            if (fn == VMExtension_yeild_9) {
                bc = TIMEQUANTA;
            }
            fn = VMExtension_yeild_9;
        }
    }


