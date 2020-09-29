//J2C:interp.c **DO NOT DELETE THIS LINE**

/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/abstract class Interpret extends ObjectMemory {

/*---------------------------------------------------------------------------*\
 *                                 Constants                                 *
\*---------------------------------------------------------------------------*/

/*IFJ*/public final static int TIMEQUANTA          = 10000;
//IFC//#define TIMEQUANTA                            100000

/*---------------------------------------------------------------------------*\
 *                             Things for Tracing                            *
\*---------------------------------------------------------------------------*/


//IFC//#ifdef TRACING
    int                 threadID = -1;
    void                setThreadID(int id)         { threadID = id; }
//IFC//#else
//IFC//#define          setThreadID(id)             /**/
//IFC//#endif

/*---------------------------------------------------------------------------*\
 *                                 Variables                                 *
\*---------------------------------------------------------------------------*/

    boolean noYield   = false;
    int fastLockSp    = 0;
    int fastLockLimit = 0;

//IFC//#ifndef MACROIZE
    private int ip, sp, lp, fn, cp, sl, bc;
//IFC//#endif /* MACROIZE */

/*---------------------------------------------------------------------------*\
 *                                   Stats                                   *
\*---------------------------------------------------------------------------*/


//IFC//#ifdef STATS

/*IFJ*/ int histogram[] = new int[256];
//IFC// int histogram[256];
/*IFJ*/ int extension[] = new int[java_lang_VMExtension_MAX_OFFSET+1];
//IFC// int extension[java_lang_VMExtension_MAX_OFFSET+1];


    void printExtensionCalls() {

        int slot;
        for (slot = 0; slot <= java_lang_VMExtension_MAX_OFFSET; ++slot) {
            int count = extension[slot];
            if (count != 0) {
                printMsg("VMExtension[");
                printInt(slot);
                printMsg("] = ");
                printInt(count);
                println();
            }
        }

/*
        for (i = 0 ; i < 256 ; i++) {
            int perc = (int)((histogram[i] * 100) / getInstructionCount());
            printIntPad(perc, 3);
            printMsg("% histogram[");
            printIntPad(i, 3);
            printMsg("] = ");
            printInt(histogram[i]);
            println();
        }
*/
    }

    void incHistogram(int b) {
//        histogram[b]++;
    }



//IFC//#else
//IFC//#define incHistogram(b) /**/
//IFC//#endif


/*---------------------------------------------------------------------------*\
 *                               getChunkFromFrame                           *
\*---------------------------------------------------------------------------*/

    int getChunkFromFrame(int lp) {
        while (Frame_getPreviousLp(lp) != 0) {
            lp = Frame_getPreviousLp(lp);
        }
        return lp - w2b(STACK_HEADER_SIZE + FRAME_HEADER_SIZE);
    }



/*---------------------------------------------------------------------------*\
 *                                   Macros                                  *
\*---------------------------------------------------------------------------*/

/*-----------------------------------------*\
 *                    push                 *
\*-----------------------------------------*/

//IFC//#ifndef PRODUCTION
/*MAC*/void push(int $x) {
           sp += 4;
/*IFJ*/    if (sp >= sl) {System.err.println("sp="+sp+" sl="+sl+ " stack_size="+getMethodStackSize(Frame_getCurrentMp(lp)));}
           assume(sp < sl);
           setWord(sp, 0, $x);
       }
//IFC//#else
//IFC//#define push(x) { pushWord(sp, x); }
//IFC//#endif

/*-----------------------------------------*\
 *                    pop                  *
\*-----------------------------------------*/

/*IFJ*/private int pop() {
/*IFJ*/    int res = getWord(sp, 0);
/*IFJ*/    sp -= 4;
/*IFJ*/    return res;
/*IFJ*/}
//IFC//#ifdef PRODUCTION
//IFC//#define pop()     (poppedWord = (getWord(sp, 0)), sp -=4, poppedWord)
//IFC//#else
//IFC//#define pop()     (sp-=4, getWord((sp+4), 0))
//IFC//#endif

/*-----------------------------------------*\
 *                 pushLong                *
\*-----------------------------------------*/

/*if[LONGS]*/
//IFC//#ifdef PRODUCTION_just_does_not_Work
//IFC//#define pushLong(x) { sp+=4; setDWord(sp, x); sp+=4; }
//IFC//#else
/*MAC*/private void pushLong(long $x) {
           sp += 4;
           setLongAtWord(sp, 0, $x);
           sp += 4;
           assume(sp < sl);
       }
//IFC//#endif
/*end[LONGS]*/

/*-----------------------------------------*\
 *                 popLong                 *
\*-----------------------------------------*/

/*if[LONGS]*/
/*IFJ*/private long popLong() {
/*IFJ*/    sp -= 4;
/*IFJ*/    long res = getLongAtWord(sp, 0);
/*IFJ*/    sp -= 4;
/*IFJ*/    return res;
/*IFJ*/}
//IFC//#ifdef PRODUCTION_just_does_not_Work
//IFC//#define popLong()     (sp-=8, getDWord(sp+4))
//IFC//#else
//IFC//#define popLong()     (sp-=8, getLongAtWord((sp+4), 0))
//IFC//#endif
/*end[LONGS]*/

/*-----------------------------------------*\
 *                clearStack               *
\*-----------------------------------------*/

/*MAC*/private int clearStack() {
        return sp = lp + Frame_getStackOffset(lp);
    }

/*-----------------------------------------*\
 *                nullCheck                *
\*-----------------------------------------*/

/*IFJ*/boolean nullCheck(int r) {
/*IFJ*/    if (r == 0) {
/*IFJ*/        fn = java_lang_VMExtension_nullPointerException;
/*IFJ*/        clearStack();
/*IFJ*/        return true;
/*IFJ*/    } else {
/*IFJ*/        return false;
/*IFJ*/    }
/*IFJ*/}
//IFC//#define nullCheck(r) ((r == 0) ? (fn = java_lang_VMExtension_nullPointerException, clearStack(), true) : false)

/*-----------------------------------------*\
 *                boundsCheck              *
\*-----------------------------------------*/

/*IFJ*/boolean boundsCheck(int p, int i) {
/*IFJ*/    Object_assumeIsArray(p);
/*IFJ*/    if (i < 0 || i >= Object_getArrayElementCount(p)) {
/*IFJ*/        fn = java_lang_VMExtension_arrayBoundsException;
/*IFJ*/        clearStack();
/*IFJ*/        return true;
/*IFJ*/    } else {
/*IFJ*/        return false;
/*IFJ*/    }
/*IFJ*/}
//IFC//#define boundsCheck(p, i) ((i < 0 || i >= Object_getArrayElementCount(p)) ? (fn = java_lang_VMExtension_arrayBoundsException, clearStack(), true) : false)

/*-----------------------------------------*\
 *                pushFrame                *
\*-----------------------------------------*/

//IFC//#ifdef TRACING
    void tracePushFrame(int lp) {
        if (getTraceFrames()) {
            trace("Pushed a stack frame: ");
            trace(" lp=");
            traceHex(lp);
            trace(", prev=");
            traceHex(Frame_getPreviousLp(lp));
            traceln("");
        }
    }
//IFC//#else
//IFC//#define tracePushFrame(lp) /**/
//IFC//#endif /* TRACING */


/*MAC*/void pushFrame() {
           int nextlp = lp + Frame_getStackOffset(lp)+w2b(1); /* Make lp point to the start of the stack area */
           Frame_setCurrentIp(lp, ip);                        /* Save current ip */
           Frame_setPreviousLp(nextlp, lp);                   /* Store the caller's lp into the new frame */
           lp = nextlp;
           assume(lp < sl);
           tracePushFrame(lp);
       }

/*-----------------------------------------*\
 *               setupFrame                *
\*-----------------------------------------*/

/*MAC*/void setupFrame(int $mp) {
           Frame_setCurrentMp(lp, $mp);                      /* Save the current method */
           Frame_setCurrentCp(lp, cp);                       /* Save the current class */
           ip = $mp + getUnsignedByte($mp, 0);               /* Set the ip to start executing the first bytecode */
       }

/*-----------------------------------------*\
 *                popFrame                 *
\*-----------------------------------------*/

//IFC//#ifdef TRACING
    void tracePopStackChunk(int poppedChunk, int currentChunk) {
        if (getTraceFrames()) {
            trace("Popped a stack chunk: ");
            trace(" popped chunk=");
            traceHex(poppedChunk);
            trace(", current chunk=");
            traceHex(currentChunk);
            traceln("");
        }
    }

    void tracePopFrame(int lp, int sp) {
        if (getTraceFrames()) {
            trace("Popped a stack frame: ");
            trace(" lp=");
            traceHex(lp);
            trace(", sp=");
            traceHex(sp);
            traceln("");
        }
    }

//IFC//#else
//IFC//#define tracePopStackChunk(cc, pc) /**/
//IFC//#define tracePopFrame(lp, sp) /**/
//IFC//#endif /* TRACING */

/*MAC*/ void popFrame() {
            int plp = Frame_getPreviousLp(lp);                  /* get previous lp */
            if (plp != 0) {                                     /* zero? */
                sp = lp - w2b(1);                               /* n - then set sp to our lp */
            } else {
                int cc = getChunkFromFrame(lp);                 /* get current chunk */
                int pc = StackChunk_getPrev(cc);                /* get previous chunk */
                assume(pc != 0);
                sl  = pc + w2b(StackChunk_getSize(pc));         /* reset sl */
                plp = StackChunk_getLastLp(pc);                 /* get previous frame */
                StackChunk_setLastLp(pc, 0);                    /* clear last frame pointer */
                sp = plp + Frame_getStackOffset(plp);           /* setup sp */
                tracePopStackChunk(cc, pc);
            }
            lp = plp;                                           /* get the previous lp */
            cp = Frame_getCurrentCp(lp);                        /* reset cp */
            ip = Frame_getCurrentIp(lp);                        /* get next ip */
            assume(sp < sl && lp < sl);
            tracePopFrame(lp, sp);
        }

/*-----------------------------------------*\
 *               setLocal                  *
\*-----------------------------------------*/

//IFC//#ifndef PRODUCTION
/*MAC*/void setLocal(int $frame, int $offset, int $value) {
        assume($frame + w2b($offset) < sl);
        Frame_setLocal($frame, $offset, $value);
    }
//IFC//#else
//IFC//#define setLocal Frame_setLocal
//IFC//#endif /* PRODUCTION */

/*-----------------------------------------*\
 *               storeFailed               *
\*-----------------------------------------*/

/*MAC*/ boolean storeFailed() {
            return fn != java_lang_VMExtension_yield;
        }

/*-----------------------------------------*\
 *               storeByte                 *
\*-----------------------------------------*/

/*MAC*/ void storeByte(int $addr, int $offset, int $value) {
            if (inSegment($addr, RAM, MMR_ramSize)) {
                setByte($addr, $offset, $value);
            } else if (inSegment($addr, EEPROM, MMR_eepromSize)) {
                setPersistentByte($addr, $offset, $value);
            } else {
                /* Trying to write to ROM */
                fn = java_lang_VMExtension_illegalStoreException;
                clearStack();
            }
        }

/*-----------------------------------------*\
 *               storeHalf                 *
\*-----------------------------------------*/

/*MAC*/ void storeHalf(int $addr, int $offset, int $value) {
            if (inSegment($addr, RAM, MMR_ramSize)) {
                setHalf($addr, $offset, $value);
            } else if (inSegment($addr, EEPROM, MMR_eepromSize)) {
                setPersistentHalf($addr, $offset, $value);
            } else {
                /* Trying to write to ROM */
                fn = java_lang_VMExtension_illegalStoreException;
                clearStack();
            }
        }

/*-----------------------------------------*\
 *               storeWord                 *
\*-----------------------------------------*/

/*MAC*/ void storeWord(int $addr, int $offset, int $value) {
            if (inSegment($addr, RAM, MMR_ramSize)) {
             setWord($addr, $offset, $value);
            } else if (inSegment($addr, EEPROM, MMR_eepromSize)) {
             setPersistentWord($addr, $offset, $value);
            } else {
             /* Trying to write to ROM */
             fn = java_lang_VMExtension_illegalStoreException;
             clearStack();
            }
        }


/*-----------------------------------------*\
 *               storeOop                  *
\*-----------------------------------------*/

/*MAC*/ void storeOop(int $addr, int $offset, int $oop) {
             if (setOopStatus($addr, $offset, $oop) == false) {
                 /* Trying to store a pointer from EEPROM -> RAM or trying to write to ROM */
                 fn = java_lang_VMExtension_illegalStoreException;
                 clearStack();
             }
         }

/*-----------------------------------------*\
 *               storeLong                 *
\*-----------------------------------------*/

/*MAC*/ void storeLong(int $addr, int $offset, long $value) {
            if (inSegment($addr, RAM, MMR_ramSize)) {
                setLong($addr, $offset, $value);
            } else if (inSegment($addr, EEPROM, MMR_eepromSize)) {
                setPersistentLong($addr, $offset, $value);
            } else {
                fn = java_lang_VMExtension_illegalStoreException;
                clearStack();
            }
        }

/*-----------------------------------------*\
 *           storeLongAtWord               *
\*-----------------------------------------*/

/*MAC*/ void storeLongAtWord(int $addr, int $offset, long $value) {
            if (inSegment($addr, RAM, MMR_ramSize)) {
                setLongAtWord($addr, $offset, $value);
            } else if (inSegment($addr, EEPROM, MMR_eepromSize)) {
                setPersistentLongAtWord($addr, $offset, $value);
            } else {
                fn = java_lang_VMExtension_illegalStoreException;
                clearStack();
            }
        }



//IFC//#ifdef TRACING

/*---------------------------------------------------------------------------*\
 *                              getStackFrames                               *
\*---------------------------------------------------------------------------*/

    /**
     * Copy the method pointer and current IP (relative to the start of the
     * method array) for a specified frame pointer into two specified arrays
     * to hold these values.
     *
     * @param lp
     * @param index
     * @param stack
     * @param ipStack
     */
    void copyFrameMpAndIp(int lp, int index, int stack, int ipStack) {
        int mp = Frame_getCurrentMp(lp);
        int ip = Frame_getCurrentIp(lp) - mp;
        assume(Object_getArrayElementCount(stack) > index);
        assume(Object_getArrayElementCount(ipStack) > index);
        setOopRam(stack, index, mp);
        setHalf(ipStack, index, ip);
    }

    /**
     * Traverse all the stack frames from a specified frame to
     * the top most frame.
     *
     * @param lp The frame to start at.
     * @param methods If non-zero, then this is a byte[][] into which the
     * methods visited during the traversal should be collected in the order
     * they are visited.
     * @param ipStack the current IP address of each method saved in 'stack'.
     * @return the number of frames on the stack including 'lp'.
     */
    int getStackFrames(int lp, int methods, int ipStack) {
        int depth = 0;
        assume(lp != 1);
        for (;;) {
            int cc, pc;
            assume(lp != 1);
            if (methods != 0) {
                copyFrameMpAndIp(lp, depth, methods, ipStack);
            }
            assume(lp != 1);
            while (Frame_getPreviousLp(lp) != 0) {
                lp = Frame_getPreviousLp(lp);
                depth++;
                if (methods != 0) {
                    copyFrameMpAndIp(lp, depth, methods, ipStack);
                }
            }
            cc = getChunkFromFrame(lp);                     /* get current chunk */
            pc = StackChunk_getPrev(cc);                    /* get previous chunk */
            if (pc == 0) {
                break;
            } else {
                depth++;
            }
            lp = StackChunk_getLastLp(pc);                  /* get previous frame */
            assume(lp != 0);
        }
        return depth+1;
    }

/*---------------------------------------------------------------------------*\
 *                               traceInstruction                            *
\*---------------------------------------------------------------------------*/

        int lastMp;
        int lastMID;
        int lastOpc;
        int lastPreamble;

        void traceOperandStack(int s0, int tos) {
            int sp = s0;
            trace("stk{");
            while (sp <= tos) {
                if (sp != s0) {
                    trace(" ");
                }
                traceInt(getWord(sp, 0));
                sp += 4;
            }
            trace("}");
        }

        void traceLocals(int mp, int lp) {
            int nlocals = getUnsignedByte(mp, MTH_numberOfLocals);
            int i;
            trace("loc{");
            for (i = 0; i != nlocals; i++) {
                if (i != 0) {
                    trace(" ");
                }
                traceInt(getWord(lp, i));
            }
            trace("}");
        }

        int traceOpcodeAndParameters(int opc, int ip) {
            if ((opc >= OPC.WIDE_0 && opc <= OPC.WIDE_15) || opc == OPC.WIDE_HALF) {
                int offset;
                trace("[");
                trace(Mnemonics.OPCODES[opc]);
                trace("] ");
                if (opc == OPC.WIDE_HALF) {
                    opc = fetchUnsignedByte(ip++);
                    offset = fetchShort(ip);
                } else {
                    int wide_15 = (opc&15) << 8;
                    opc = fetchUnsignedByte(ip++);
                    offset = wide_15 | fetchUnsignedByte(ip);
                    offset = offset << 20 >> 20;
                }
                trace(Mnemonics.OPCODES[opc]);
                trace(" ");
                traceInt(offset);
            } else if (opc == OPC.LONGOP) {
                trace(Mnemonics.LONG_OPCODES[fetchUnsignedByte(ip)]);
/*if[FLOATS]*/
            } else if (opc == OPC.FLOATOP) {
                trace(Mnemonics.FLOAT_OPCODES[fetchUnsignedByte(ip)]);
/*end[FLOATS]*/
            } else {
                int blth;
                trace(Mnemonics.OPCODES[opc]);
/*IFJ*/         blth = (byte)OPC.LENGTH_TABLE.charAt(opc);
//IFC//         blth = LENGTH_TABLE[opc];
                if (blth > 1) {
                    if (blth == 2) {
                        switch (opc) {
                            case OPC.IF_ICMPEQ:  case OPC.IFEQ:
                            case OPC.IF_ICMPNE:  case OPC.IFNE:
                            case OPC.IF_ICMPLT:  case OPC.IFLT:
                            case OPC.IF_ICMPLE:  case OPC.IFLE:
                            case OPC.IF_ICMPGT:  case OPC.IFGT:
                            case OPC.IF_ICMPGE:  case OPC.IFGE:
                            case OPC.GOTO: trace(" "); traceInt(fetchByte(ip));         break;
                            default:       trace(" "); traceInt(fetchUnsignedByte(ip)); break;
                        }
                    } else if (blth == 3) {
                        trace(" "); traceInt(fetchShort(ip));
                    } else if (blth == 5) {
                        trace(" "); traceInt(fetchInt(ip));
                    } else if (blth == 9) {
                        trace(" "); traceLong(fetchLong(ip), true);
                    } else {
                        trace(" ???");
                    }
                }
            }
            return ip;
        }

        int traceInstructionPrefix(int ip, int lp) {
            int mp = Frame_getCurrentMp(lp);
            int frameDepth = getStackFrames(lp, 0, 0)-1;
            if (mp != lastMp) {
                int methodIdAddr = getMethodIdAddress(mp);
                if (methodIdAddr != -1) {
                    if (lastMID != -1) {
                        lastMID      = fetchInt(methodIdAddr);
                        lastPreamble = methodIdAddr + 4;
                    } else {
                        lastMID      = -1;
                        lastPreamble = -1;
                    }
                    lastMp = mp;
                }
            }
            trace("*TRACE*");
            traceInt(threadID);        trace(":");
            traceInt(frameDepth);      trace(":");
            traceInt(lastMID);         trace(":");
            traceInt(ip-lastPreamble); trace(":");
            return mp;
        }

        void traceInstruction(int ip, int lp, int sp, int cp) {
            int soff;
            int mp = traceInstructionPrefix(ip, lp);
            int opc  = fetchUnsignedByte(ip);

            if (ip == mp + fetchUnsignedByte(mp)) {
                soff = -w2b(1);                     /* before extend */
            } else {
                soff = Frame_getStackOffset(lp);    /* after extend */
            }

            /* Trace the locals */
            traceLocals(mp, lp);
            trace(" ");

            /* Trace the stack */
            traceOperandStack(lp + soff + w2b(1), sp);

            /* Trace the opcode and its parameters */
            traceTab(50);
            trace(" ");
            ip = traceOpcodeAndParameters(opc, ++ip);

            traceTab(70);
            trace("   sp=");
            traceInt(sp);
            trace(" lp=");
            traceInt(lp);

            traceln("");
        }

/*MAC*/ void startProfiler() {
            if (getProfileMethods()) {
                startProfileTicker();
            }
        }


/*MAC*/ void profileInstruction(int $ip, int $lp) {
            int mp;
            int tks = getProfileTicks();
            while(tks-- > 0) {
                trace("*MPROF* ");
                traceInt(lastMID);
                trace(":");
                traceInt(lastOpc);
                traceln("");
            }
            lastOpc = fetchUnsignedByte($ip);
            mp = Frame_getCurrentMp($lp);
            if (mp != lastMp) {
                int methodIdAddr = getMethodIdAddress(mp);
                if (lastMID != -1) {
                    lastMID      = fetchInt(methodIdAddr);
                    lastPreamble = methodIdAddr + 4;
                } else {
                    lastMID      = -1;
                    lastPreamble = -1;
                }
                lastMp = mp;
            }
            zeroProfileTicks();
        }

/*MAC*/ void debugInstruction(int $ip, int $lp, int $sp, int $cp) {
            if (getProfileMethods()) {
                profileInstruction($ip, $lp);               /* Inlined */
            } else {
                if (getTraceInstructions()) {
                    traceInstruction($ip, $lp, $sp, $cp);   /* Not Inlined */
                }
            }
        }

//IFC//#else
//IFC//#define traceInstruction(ip, lp, sp, cp)         /**/
//IFC//#define debugInstruction(ip, lp, sp, cp)         /**/
//IFC//#define profileInstruction(ip, lp)               /**/
//IFC//#define getStackFrames(lp, methods, ipStack)     0
//IFC//#define zeroProfileTicks()                       /**/
//IFC//#define startProfiler()                          /**/
//IFC//#endif





        void putch(int ch, boolean err) {
            if (ch == '\n') {
               traceln("");
            } else if (ch == '\t') {
               trace("\t");
            } else {
               traceChar(ch);
            }
            if (!traceIsSystem_err()) {
                if (err) {
                    printErrCh(ch);
                } else {
                    printCh(ch);
                }
            }
        }




/*---------------------------------------------------------------------------*\
 *                               addToClassState                             *
\*---------------------------------------------------------------------------*/

    void addToClassState(int cls, int cs) {
        int classHash = Class_getType(cls) & (CLS_STATE_TABLE_SIZE - 1);
        int cstable = getClassStateTable();                         /* Get Hashtable */
        int first = getWord(cstable, classHash);                    /* Lookup first; */
        ClassState_setClass(cs, cls);                               /* Set the class pointer */
        ClassState_setNext(cs, first);                              /* Set the next pointer */
        setOopRam(cstable, classHash, cs);                          /* make cs first in table= */
    }


/*---------------------------------------------------------------------------*\
 *                                findClassState2                            *
\*---------------------------------------------------------------------------*/

    int findClassState2(int cls) {
        int classHash = Class_getType(cls) & (CLS_STATE_TABLE_SIZE - 1); /* Get hash code */
        int cstable = getClassStateTable();                             /* Get hash table */
        int first = getWord(cstable, classHash);                        /* Lookup first (which cannot be cls); */
        int prev = first;

        assume(first != cls);
        if (prev != 0) {
            for (;;) {
                int cs = ClassState_getNext(prev);
                if (cs == 0) {
                    break;
                }
                Object_assumeIsArray(cs);
                if (ClassState_getClass(cs) == cls) {
                    ClassState_setNext(prev, ClassState_getNext(cs));       /* unlink cs */
                    setWord(cstable, classHash, cs);                        /* make cs first */
                    ClassState_setNext(cs, first);                          /* chain old first onto cs */
                    return cs;
                }
                prev = cs;
            }
        }

       /*
        * This is bootstrapping issue. The class state for java.lang.Thread must be
        * allocated here in order to avoid an infinate recursion. Otherwise this is
        * a non critical speed optimization.
        */
        if ((Class_getAccess(cls) & ACC_MUSTCLINIT) == 0) {             /* Do this only if class initialization is not required */
            int cs = newClassState(cls);
            if (cs != 0) {
                addToClassState(cls, cs);
                return cs;
            }
        }
        return 0;
    }


/*---------------------------------------------------------------------------*\
 *                                findClassState                             *
\*---------------------------------------------------------------------------*/

/*MAC*/ int findClassState(int $cls) {
            int classHash = Class_getType($cls) & (CLS_STATE_TABLE_SIZE - 1); /* Get hash code */
            int cstable = getClassStateTable();                              /* Get hash table */
            int cs = getWord(cstable, classHash);                            /* Get first entry */
            if (cs != 0 && ClassState_getClass(cs) == $cls) {                /* Is is the class being searched for? */
                Object_assumeIsArray(cs);
                return cs;                                                   /* Y - return */
            } else {
                return findClassState2($cls);
            }
        }


/*---------------------------------------------------------------------------*\
 *                            createExecutionContext                         *
\*---------------------------------------------------------------------------*/

    int getMethodStackSize(int mp) {
        int p = mp + getUnsignedByte(mp, 0); /* skip header */
        int extendOp = fetchUnsignedByte(p);
        if (extendOp == OPC.EXTEND) {
            return fetchUnsignedByte(p+1);
        } else if (extendOp >= OPC.EXTEND_0 && extendOp <= OPC.EXTEND_15) {
            return extendOp&15;
        } else {
            return 0;
        }
    }

    /**
     * Calculate and return the minimum size (in words) of a new stack chunk
     * based on the size of the first method in the chunk.
     * The size returned takes into account space for 2 frame headers; one
     * for the given method and a pre-allocated one for the next method.
     * @param nstack The max stack size required for the method
     * @param nlocals The number of locals used by the method.
     * @return the minimum size (in words) of a new stack chunk
     * based on the size of the first method in the chunk.
     */
/*MAC*/ int getNewStackChunkMinSize(int $nstack, int $nlocals) {
            return STACK_HEADER_SIZE + (2*FRAME_HEADER_SIZE) + $nlocals + $nstack;
        }

    /**
     * Allocate a new stack chunk with a size at least equal to that required
     * to enter a given method and execute its 'extend' instruction (if any)
     * without having to allocate another stack chunk.
     *
     * @param fn The slot of a static method in java.lang.VMExtension.
     * @param parm
     * @return
     */
    int createExecutionContext(int fn, int parm) {
        int cp      = getJavaLangVMExtension();
        int methods = Class_getStaticMethods(cp);
        int mp      = getWord(methods, fn);
        int minSize = getNewStackChunkMinSize(getMethodStackSize(mp), getUnsignedByte(mp, MTH_numberOfLocals));
        int chunk   = newStackChunk(minSize);
        if (chunk != 0) {
            int lp    = chunk + w2b(STACK_HEADER_SIZE+FRAME_HEADER_SIZE);
            int sp    = lp - w2b(1);
            int ip;
            if (parm != 0) {
                sp += 4;
                setWord(sp, 0, parm);
            }
            Frame_setPreviousLp(lp, 0);
            Frame_setStackOffset(lp, sp - lp);
            Frame_setCurrentMp(lp, mp);                         /* Save the current method */
            Frame_setCurrentCp(lp, cp);                         /* Save the current class */
            ip = mp + getUnsignedByte(mp, 0);                   /* Set the ip to start executing the first bytecode */
            Frame_setCurrentIp(lp, ip);
            StackChunk_setLastLp(chunk, lp);
            if (getTraceFrames()) {
                trace("Created first stack chunk in thread: ");
                traceHex(chunk);
                trace(", size=");
                traceHex(w2b(StackChunk_getSize(chunk)));
                traceln(" bytes");
            }
        }
        return chunk;
    }




/*---------------------------------------------------------------------------*\
 *                                    assumes                                *
\*---------------------------------------------------------------------------*/

//IFC//#ifdef ASSUME

        /**
         * Test to see if an object is an array and the offset is in bounds
         * @param object
         * @param offset
         * @return true if the object if the length is in bounds
         */
        void assumeArrayIndexInBounds(int object, int offset) {
            if (assuming()) {
                int count;
                Object_assumeIsArray(object);
                count = Object_getArrayElementCount(object);
                assume(count >= 0);
                assume(offset >= 0 && offset < count);
            }
        }

        /**
         * Test to see if an object is a object and the offset is in bounds
         * @param object
         * @param offset
         * @param width
         */
        void assumeValidObjectField(int object, int offset, int width) {
            if (assuming()) {
                int cls, lth;
                int byteoffset = (offset+1)*width ;
                Object_assumeNotArray(object);
                cls = Object_getClass(object);
                lth = w2b(Class_getInstanceFieldsLength(cls));
                if (offset < 0) {
                    printMsg("assumeValidObjectField: offset =");
                    printInt(offset);
                    println();
                }
                if (byteoffset > lth) {
                    printMsg("assumeValidObjectField: byteoffset =");
                    printInt(byteoffset);
                    printMsg(" lth=");
                    printInt(lth);
                    printMsg(" object=");
                    printHex(object);
                    printMsg(" cno=");
                    printInt(Class_getType(cls));
                    println();
                }
                assume(offset >= 0 && byteoffset <= lth);
            }
        }

//IFC//#else
//IFC//#define assumeArrayIndexInBounds(object, offset)         /**/
//IFC//#define assumeValidObjectField(object, offset, width)    /**/
//IFC//#endif /* ASSUME */


/*---------------------------------------------------------------------------*\
 *                              isSimplyAssignable                           *
\*---------------------------------------------------------------------------*/


    boolean isSimplyAssignableToCno(int obj, int classNum) {
        int objcls   = Object_getClass(obj);
        for ( ; objcls != 0 ; objcls = Class_getSuperClass(objcls)) {
            if (Class_getType(objcls) == classNum) {
                return true;
            }
        }
        return false;
    }

/*MAC*/ boolean isSimplyAssignable(int $obj, int $cls) {
            return isSimplyAssignableToCno($obj, Class_getType($cls));
        }


/*---------------------------------------------------------------------------*\
 *                                 findMonitorFor                            *
\*---------------------------------------------------------------------------*/

/*
    boolean findMonitorFor(int obj) {
        int mon = getMonitorQueue();
        while (mon != 0) {
            if (Monitor_getObject(mon) == obj) {
                 return true;
            }
            mon = Monitor_getNext(mon);
        }
        return false;
    }
*/

/*---------------------------------------------------------------------------*\
 *                                pushLockedObject                           *
\*---------------------------------------------------------------------------*/

    boolean pushLockedObject(int obj) {
        int stack;
        if (fastLockSp == fastLockLimit) {
            return false;
        }
        stack = getFastLockStack();
        setOopRam(stack, fastLockSp++, obj);
        return true;
    }


/*---------------------------------------------------------------------------*\
 *                                 popLockedObject                           *
\*---------------------------------------------------------------------------*/

    void popLockedObject(int obj) {
        int stack, res;
        assume(fastLockSp > 0);
        stack = getFastLockStack();
        res = getWord(stack, --fastLockSp);
        assume(res == obj);
    }


/*---------------------------------------------------------------------------*\
 *                                  interpret                                *
\*---------------------------------------------------------------------------*/



    /**
     * The main interpreter loop. The variables used in this loop are:
     *
     *   ip: Pointer into the instruction stream
     *   sp: Pointer to the element on top of the stack
     *   lp: Pointer to the current frame
     *   fn: Pointer to the next Java support function to be called
     *   cp: Pointer to the class of the current method
     *   sl: Pointer to the word passed the end of the current stack chunk
     *   bc: Branch count
     *
     * @param startChunk
     * @param gcWorked
     * @return
     */
    int interpret(int startChunk, boolean gcWorked) {

//IFC//#ifdef MACROIZE
//IFC// int ip, sp, lp, fn, cp, sl, bc;
//IFC//#endif /* MACROIZE */

//IFC//#ifdef PRODUCTION
//IFC// int poppedWord;  /* Used in pop() macro. This is required as some
//IFC//                     C compilers (such as 'cc') do not allow the result
//IFC//                     of a cast to be used as an lvalue (which is
//IFC//                     compliant with the C standard. */
//IFC//#endif /* PRODUCTION */

        lp = StackChunk_getLastLp(startChunk);                  /* Setup lp */
        ip = Frame_getCurrentIp(lp);                            /* Setup cp */
        cp = Frame_getCurrentCp(lp);                            /* Setup ip */
        sp = lp + Frame_getStackOffset(lp);                     /* Setup sp */
        sl = startChunk + w2b(StackChunk_getSize(startChunk));  /* Setup sl */
        bc = TIMEQUANTA;                                        /* Setup bc */
        fn = java_lang_VMExtension_yield;                               /* Setup fn */

        StackChunk_setLastLp(startChunk, 0);                    /* Clear last frame pointer */

        if (getTraceFrames()) {
            trace("Restarted interpreter with frame: ");
            traceHex(lp);
            trace(", depth=");
            traceInt(getStackFrames(lp, 0, 0));
            traceln("");
        }

       /*
        * If the last garbage collection failed to reclaim the amount of
        * memory in the last allocation then the interpreter must now
        * throw an OutOfMemoryError. This is done by going directly to the
        * THROW bytecode.
        */
        if (!gcWorked) {
            ip = getAddressOfThrowBytecode();
        }

       /*
        * Don't count time in the collector
        */
        zeroProfileTicks();

        /*
         * Top of the interpreter loop
         */
        loop: for (;;) {
            int b;
            assume(fn == java_lang_VMExtension_yield);
            incInstructionCount(Class_getType(cp));
            debugInstruction(ip, lp, sp, cp);

            b = fetchUnsignedByte(ip++);
            incHistogram(b);
            switch (b) {

                /*-----------------------------------------------------------*\
                 *                          Constants                        *
                \*-----------------------------------------------------------*/

                case OPC.CONST_0:       push(0);                                       continue;
                case OPC.CONST_1:       push(1);                                       continue;
                case OPC.CONST_2:       push(2);                                       continue;
                case OPC.CONST_3:       push(3);                                       continue;
                case OPC.CONST_4:       push(4);                                       continue;
                case OPC.CONST_5:       push(5);                                       continue;
                case OPC.CONST_6:       push(6);                                       continue;
                case OPC.CONST_7:       push(7);                                       continue;
                case OPC.CONST_8:       push(8);                                       continue;
                case OPC.CONST_9:       push(9);                                       continue;
                case OPC.CONST_10:      push(10);                                      continue;
                case OPC.CONST_11:      push(11);                                      continue;
                case OPC.CONST_12:      push(12);                                      continue;
                case OPC.CONST_13:      push(13);                                      continue;
                case OPC.CONST_14:      push(14);                                      continue;
                case OPC.CONST_15:      push(15);                                      continue;
                case OPC.CONST_M1:      push(-1);                                      continue;
                case OPC.CONST_BYTE:    push(fetchByte(ip));                  ip += 1; continue;
                case OPC.CONST_SHORT:   push(fetchShort(ip));                 ip += 2; continue;
                case OPC.CONST_CHAR:    push(fetchUnsignedShort(ip));         ip += 2; continue;
                case OPC.CONST_INT:     push(fetchInt(ip));                   ip += 4; continue;
                case OPC.CONST_LONG:    pushLong(fetchLong(ip));              ip += 8; continue;
/*if[FLOATS]*/
                case OPC.CONST_FLOAT:   push(fetchInt(ip));                   ip += 4; continue;
                case OPC.CONST_DOUBLE:  pushLong(fetchLong(ip));              ip += 8; continue;
/*end[FLOATS]*/


                /*-----------------------------------------------------------*\
                 *                     Object refererences                   *
                \*-----------------------------------------------------------*/

                case OPC.OBJECT_0:  push(getWord(Class_getObjectReferences(cp), 0));   continue;
                case OPC.OBJECT_1:  push(getWord(Class_getObjectReferences(cp), 1));   continue;
                case OPC.OBJECT_2:  push(getWord(Class_getObjectReferences(cp), 2));   continue;
                case OPC.OBJECT_3:  push(getWord(Class_getObjectReferences(cp), 3));   continue;
                case OPC.OBJECT_4:  push(getWord(Class_getObjectReferences(cp), 4));   continue;
                case OPC.OBJECT_5:  push(getWord(Class_getObjectReferences(cp), 5));   continue;
                case OPC.OBJECT_6:  push(getWord(Class_getObjectReferences(cp), 6));   continue;
                case OPC.OBJECT_7:  push(getWord(Class_getObjectReferences(cp), 7));   continue;
                case OPC.OBJECT_8:  push(getWord(Class_getObjectReferences(cp), 8));   continue;
                case OPC.OBJECT_9:  push(getWord(Class_getObjectReferences(cp), 9));   continue;
                case OPC.OBJECT_10: push(getWord(Class_getObjectReferences(cp), 10));  continue;
                case OPC.OBJECT_11: push(getWord(Class_getObjectReferences(cp), 11));  continue;
                case OPC.OBJECT_12: push(getWord(Class_getObjectReferences(cp), 12));  continue;
                case OPC.OBJECT_13: push(getWord(Class_getObjectReferences(cp), 13));  continue;
                case OPC.OBJECT_14: push(getWord(Class_getObjectReferences(cp), 14));  continue;
                case OPC.OBJECT_15: push(getWord(Class_getObjectReferences(cp), 15));  continue;

                case OPC.OBJECT: {
                    int refs = Class_getObjectReferences(cp);
                    push(getWord(refs, fetchUnsignedByte(ip++)));
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                      Class refererences                   *
                \*-----------------------------------------------------------*/

                case OPC.CLASS_0:
                case OPC.CLASS_1:
                case OPC.CLASS_2:
                case OPC.CLASS_3:
                case OPC.CLASS_4:
                case OPC.CLASS_5:
                case OPC.CLASS_6:
                case OPC.CLASS_7:
                case OPC.CLASS_8:
                case OPC.CLASS_9:
                case OPC.CLASS_10:
                case OPC.CLASS_11:
                case OPC.CLASS_12:
                case OPC.CLASS_13:
                case OPC.CLASS_14:
                case OPC.CLASS_15: {
                    int refs = Class_getClassReferences(cp);
                    int type = getUnsignedHalf(refs, b&15);
                    int cls  = getClassFromCNO(type);
                    push(cls);
                    continue;
                }
                case OPC.CLASS: {
                    int refs = Class_getClassReferences(cp);
                    int type = getUnsignedHalf(refs, fetchUnsignedByte(ip++));
                    int cls  = getClassFromCNO(type);
                    push(cls);
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                    Local variable loads                   *
                \*-----------------------------------------------------------*/

                case OPC.LOAD_0:    push(Frame_getLocal(lp, 0));  continue;
                case OPC.LOAD_1:    push(Frame_getLocal(lp, 1));  continue;
                case OPC.LOAD_2:    push(Frame_getLocal(lp, 2));  continue;
                case OPC.LOAD_3:    push(Frame_getLocal(lp, 3));  continue;
                case OPC.LOAD_4:    push(Frame_getLocal(lp, 4));  continue;
                case OPC.LOAD_5:    push(Frame_getLocal(lp, 5));  continue;
                case OPC.LOAD_6:    push(Frame_getLocal(lp, 6));  continue;
                case OPC.LOAD_7:    push(Frame_getLocal(lp, 7));  continue;
                case OPC.LOAD_8:    push(Frame_getLocal(lp, 8));  continue;
                case OPC.LOAD_9:    push(Frame_getLocal(lp, 9));  continue;
                case OPC.LOAD_10:   push(Frame_getLocal(lp, 10)); continue;
                case OPC.LOAD_11:   push(Frame_getLocal(lp, 11)); continue;
                case OPC.LOAD_12:   push(Frame_getLocal(lp, 12)); continue;
                case OPC.LOAD_13:   push(Frame_getLocal(lp, 13)); continue;
                case OPC.LOAD_14:   push(Frame_getLocal(lp, 14)); continue;
                case OPC.LOAD_15:   push(Frame_getLocal(lp, 15)); continue;

                case OPC.LOAD:
                    push(Frame_getLocal(lp, fetchUnsignedByte(ip++)));
                    continue;

                case OPC.LOAD_I2: {
                    int  parm  = fetchUnsignedByte(ip++);
                    long value = Frame_getLocalLong(lp, parm);
                    pushLong(value);
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                    Local variable stores                  *
                \*-----------------------------------------------------------*/

                case OPC.STORE_0:   setLocal(lp, 0,  pop()); continue;
                case OPC.STORE_1:   setLocal(lp, 1,  pop()); continue;
                case OPC.STORE_2:   setLocal(lp, 2,  pop()); continue;
                case OPC.STORE_3:   setLocal(lp, 3,  pop()); continue;
                case OPC.STORE_4:   setLocal(lp, 4,  pop()); continue;
                case OPC.STORE_5:   setLocal(lp, 5,  pop()); continue;
                case OPC.STORE_6:   setLocal(lp, 6,  pop()); continue;
                case OPC.STORE_7:   setLocal(lp, 7,  pop()); continue;
                case OPC.STORE_8:   setLocal(lp, 8,  pop()); continue;
                case OPC.STORE_9:   setLocal(lp, 9,  pop()); continue;
                case OPC.STORE_10:  setLocal(lp, 10, pop()); continue;
                case OPC.STORE_11:  setLocal(lp, 11, pop()); continue;
                case OPC.STORE_12:  setLocal(lp, 12, pop()); continue;
                case OPC.STORE_13:  setLocal(lp, 13, pop()); continue;
                case OPC.STORE_14:  setLocal(lp, 14, pop()); continue;
                case OPC.STORE_15:  setLocal(lp, 15, pop()); continue;

                case OPC.STORE:
                    setLocal(lp, fetchUnsignedByte(ip++), pop());
                    continue;

                case OPC.STORE_I2: {
                    int  parm  = fetchUnsignedByte(ip++);
                    long value = popLong();
                    Frame_setLocalLong(lp, parm, value);
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                   inc/dec local variables                 *
                \*-----------------------------------------------------------*/

                case OPC.INC: {
                    int parm = fetchUnsignedByte(ip++);
                    setLocal(lp, parm, Frame_getLocal(lp, parm) + 1);
                    continue;
                }

                case OPC.DEC:  {
                    int parm = fetchUnsignedByte(ip++);
                    setLocal(lp, parm, Frame_getLocal(lp, parm) - 1);
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *               Wide instructions (only branches)           *
                \*-----------------------------------------------------------*/

                case OPC.WIDE_0:
                case OPC.WIDE_1:
                case OPC.WIDE_2:
                case OPC.WIDE_3:
                case OPC.WIDE_4:
                case OPC.WIDE_5:
                case OPC.WIDE_6:
                case OPC.WIDE_7:
                case OPC.WIDE_8:
                case OPC.WIDE_9:
                case OPC.WIDE_10:
                case OPC.WIDE_11:
                case OPC.WIDE_12:
                case OPC.WIDE_13:
                case OPC.WIDE_14:
                case OPC.WIDE_15:
                case OPC.WIDE_HALF: {
                    int offset;
                    if (b == OPC.WIDE_HALF) {
                        b = fetchUnsignedByte(ip++);
                        offset = fetchShort(ip);  ip += 2;
                    } else {
                        int wide_15 = (b&15) << 8;
                        b = fetchUnsignedByte(ip++);
                        offset = wide_15 | fetchUnsignedByte(ip++);
                        offset = offset << 20 >> 20;
                    }
                    switch (b) {
                        case OPC.IFEQ:          { int l = pop();                if (l == 0) ip += offset;   break; }
                        case OPC.IFNE:          { int l = pop();                if (l != 0) ip += offset;   break; }
                        case OPC.IFLT:          { int l = pop();                if (l <  0) ip += offset;   break; }
                        case OPC.IFLE:          { int l = pop();                if (l <= 0) ip += offset;   break; }
                        case OPC.IFGT:          { int l = pop();                if (l >  0) ip += offset;   break; }
                        case OPC.IFGE:          { int l = pop();                if (l >= 0) ip += offset;   break; }
                        case OPC.IF_ICMPEQ:     { int r = pop(); int l = pop(); if (l == r) ip += offset;   break; }
                        case OPC.IF_ICMPNE:     { int r = pop(); int l = pop(); if (l != r) ip += offset;   break; }
                        case OPC.IF_ICMPLT:     { int r = pop(); int l = pop(); if (l <  r) ip += offset;   break; }
                        case OPC.IF_ICMPLE:     { int r = pop(); int l = pop(); if (l <= r) ip += offset;   break; }
                        case OPC.IF_ICMPGT:     { int r = pop(); int l = pop(); if (l >  r) ip += offset;   break; }
                        case OPC.IF_ICMPGE:     { int r = pop(); int l = pop(); if (l >= r) ip += offset;   break; }
                        case OPC.GOTO:          {                                           ip += offset;   break; }
                        case OPC.OBJECT: {
                            int refs = Class_getObjectReferences(cp);
                            push(getWord(refs, offset));
                            continue;
                        }
                        case OPC.CLASS: {
                            int refs = Class_getClassReferences(cp);
                            int type = getUnsignedHalf(refs, offset);
                            int cls  = getClassFromCNO(type);
                            push(cls);
                            continue;
                        }

                        default:
                            fatalVMErrorWithValue("Bad wide opcode", b);
                    }
                    incBranchCount();
                    if (--bc > 0) {
                        continue;
                    } else {
                        break;
                    }
                }


                /*-----------------------------------------------------------*\
                 *                    Normal branches                        *
                \*-----------------------------------------------------------*/


                case OPC.IFEQ:          { int offset = fetchByte(ip++); int l = pop();                if (l == 0) ip += offset; incBranchCount(); if (--bc > 0) continue; else break; }
                case OPC.IFNE:          { int offset = fetchByte(ip++); int l = pop();                if (l != 0) ip += offset; incBranchCount(); if (--bc > 0) continue; else break; }
                case OPC.IFLT:          { int offset = fetchByte(ip++); int l = pop();                if (l <  0) ip += offset; incBranchCount(); if (--bc > 0) continue; else break; }
                case OPC.IFLE:          { int offset = fetchByte(ip++); int l = pop();                if (l <= 0) ip += offset; incBranchCount(); if (--bc > 0) continue; else break; }
                case OPC.IFGT:          { int offset = fetchByte(ip++); int l = pop();                if (l >  0) ip += offset; incBranchCount(); if (--bc > 0) continue; else break; }
                case OPC.IFGE:          { int offset = fetchByte(ip++); int l = pop();                if (l >= 0) ip += offset; incBranchCount(); if (--bc > 0) continue; else break; }
                case OPC.IF_ICMPEQ:     { int offset = fetchByte(ip++); int r = pop(); int l = pop(); if (l == r) ip += offset; incBranchCount(); if (--bc > 0) continue; else break; }
                case OPC.IF_ICMPNE:     { int offset = fetchByte(ip++); int r = pop(); int l = pop(); if (l != r) ip += offset; incBranchCount(); if (--bc > 0) continue; else break; }
                case OPC.IF_ICMPLT:     { int offset = fetchByte(ip++); int r = pop(); int l = pop(); if (l <  r) ip += offset; incBranchCount(); if (--bc > 0) continue; else break; }
                case OPC.IF_ICMPLE:     { int offset = fetchByte(ip++); int r = pop(); int l = pop(); if (l <= r) ip += offset; incBranchCount(); if (--bc > 0) continue; else break; }
                case OPC.IF_ICMPGT:     { int offset = fetchByte(ip++); int r = pop(); int l = pop(); if (l >  r) ip += offset; incBranchCount(); if (--bc > 0) continue; else break; }
                case OPC.IF_ICMPGE:     { int offset = fetchByte(ip++); int r = pop(); int l = pop(); if (l >= r) ip += offset; incBranchCount(); if (--bc > 0) continue; else break; }
                case OPC.GOTO:          { int offset = fetchByte(ip++);                                           ip += offset; incBranchCount(); if (--bc > 0) continue; else break; }

                case OPC.STABLESWITCH_PAD:
                    ip++;
                case OPC.STABLESWITCH: {
                    int key, off, low, hi;
                    key = pop();
                    off = fetchShort(ip);        ip += 2;
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

                case OPC.LOOKUP: {
                    int key, ref, cnt, i, res = -1;
                    ref = pop();
                    key = pop();
                    cnt = Object_getArrayElementCount(ref);
                    for (i = 0 ; i < cnt ; i++) {
                        int val = getWord(ref, i);
                        if (val == key) {
                            res = i;
                            break;
                        }
                    }
                    push(res);
                    continue;
                }
                case OPC.LOOKUP_B: {
                    int key, ref, cnt, i, res = -1;
                    ref = pop();
                    key = pop();
                    cnt = Object_getArrayElementCount(ref);
                    for (i = 0 ; i < cnt ; i++) {
                        int val = getByte(ref, i);
                        if (val == key) {
                            res = i;
                            break;
                        }
                    }
                    push(res);
                    continue;
                }
                case OPC.LOOKUP_S: {
                    int key, ref, cnt, i, res = -1;
                    ref = pop();
                    key = pop();
                    cnt = Object_getArrayElementCount(ref);
                    for (i = 0 ; i < cnt ; i++) {
                        int val = getHalf(ref, i);
                        if (val == key) {
                            res = i;
                            break;
                        }
                    }
                    push(res);
                    continue;
                }
                case OPC.LOOKUP_C: {
                    int key, ref, cnt, i, res = -1;
                    ref = pop();
                    key = pop();
                    cnt = Object_getArrayElementCount(ref);
                    for (i = 0 ; i < cnt ; i++) {
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

                case OPC.INVOKEVIRTUAL:
                case OPC.INVOKEVIRTUAL1:
                case OPC.INVOKEVIRTUAL2: {
                    int mp;
                    int rcvr;
                    int vtbl;
                    int vstart;
                    int parm = fetchUnsignedByte(ip++);                 /* Get the "number" operand of invoke */
                    incInvokeCount();

                    pushFrame();

                    rcvr = Frame_getLocal(lp, 0);                       /* Get the receiver */
                    if (nullCheck(rcvr)) {
                        lp = Frame_getPreviousLp(lp);
                        sp = lp + Frame_getStackOffset(lp);
                        break;
                    }

                    cp = Object_getClass(rcvr);                         /* Get the callee's class */
                    vstart = Class_getFirstVirtualMethod(cp);           /* Get the number of the first method defined */
                    for (;;) {
                        while (parm < vstart) {                         /* Is the target method there? */
                            cp  = Class_getSuperClass(cp);              /* Go to the super class */
                            assume(cp != 0);
                            vstart = Class_getFirstVirtualMethod(cp);   /* Get the number of the first method defined */
                        }                                               /* Test again... */
                        vtbl = Class_getVirtualMethods(cp);             /* Get the vtable for virtual methods */
                        assumeArrayIndexInBounds(vtbl, parm-vstart);
                        mp = getWord(vtbl, parm-vstart);                /* Get the method */
                        if (mp != 0) {
                            break;
                        } else {                                        /* The vtable slot will be empty if this class
                                                                           did not actually define the method*/
                            cp  = Class_getSuperClass(cp);              /* N - Go to the super class */
                            assume(cp != 0);
                            vstart = Class_getFirstVirtualMethod(cp);   /* Get the number of the first method defined */
                        }
                    }

                    setupFrame(mp);

                    continue;
                }

                case OPC.INVOKESUPER:
                case OPC.INVOKESUPER1:
                case OPC.INVOKESUPER2: {
                    int mp;
                    int vtbl;
                    int vstart;
                    int parm = fetchUnsignedByte(ip++);                 /* Get the "number" operand of invoke */
                    incInvokeCount();

                    pushFrame();

                    cp = pop();                                         /* Get the callee's class */
                    vstart = Class_getFirstVirtualMethod(cp);           /* Get the number of the first method defined */
                    for (;;) {
                        while (parm < vstart) {                         /* Is the target method there? */
                            cp  = Class_getSuperClass(cp);              /* Go to the super class */
                            assume(cp != 0);
                            vstart = Class_getFirstVirtualMethod(cp);   /* Get the number of the first method defined */
                        }                                               /* Test again... */
                        vtbl = Class_getVirtualMethods(cp);             /* Get the vtable for virtual methods */
                        assumeArrayIndexInBounds(vtbl, parm-vstart);
                        mp = getWord(vtbl, parm-vstart);                /* Get the method */
                        if (mp != 0) {
                            break;
                        } else {
                            cp  = Class_getSuperClass(cp);              /* N - Go to the super class */
                            assume(cp != 0);
                            vstart = Class_getFirstVirtualMethod(cp);   /* Get the number of the first method defined */
                        }

                    }

                    setupFrame(mp);

                    continue;
                }

                case OPC.INVOKEINIT:
                case OPC.INVOKEINIT1:
                case OPC.INVOKEINIT2:
                case OPC.INVOKESTATIC:
                case OPC.INVOKESTATIC1:
                case OPC.INVOKESTATIC2: {
                    int mp;
                    int vtbl;
                    int parm = fetchUnsignedByte(ip++);                 /* Get the "number" operand of invoke */
                    incInvokeCount();

                    pushFrame();

                    cp = pop();                                         /* Get the callee's class */
                    vtbl = Class_getStaticMethods(cp);                  /* Get the vtable for virtual methods */
                    assumeArrayIndexInBounds(vtbl, parm);
                    mp = getWord(vtbl, parm);                           /* Get the method */
                    if (mp == 0) {
                        fatalVMError("OPC.INVOKESTATIC mp==0 (Probably a call to an unimplemented native method)");
                    }

                    setupFrame(mp);

                    continue;
                }

                case OPC.INVOKEINTERFACE:
                case OPC.INVOKEINTERFACE1:
                case OPC.INVOKEINTERFACE2: {
                    boolean found = false;
                    int mp;
                    int iklass;
                    int rcvr;
                    int rcvrClass;
                    int vtbl;
                    int vstart;
                    int iklassType;
                    int types;
                    int tcount;
                    int i;
                    int cno;
                    int parm = fetchUnsignedByte(ip++);                         /* Get the "number" operand of invoke */
                    incInvokeCount();

                    pushFrame();

                    iklass     = pop();                                         /* Get the class of the interface */
                    iklassType = Class_getType(iklass);                          /* Get the iklass cno */
                    rcvr       = Frame_getLocal(lp, 0);                         /* Get the receiver */

                    if (nullCheck(rcvr)) {
                        lp = Frame_getPreviousLp(lp);
                        sp = lp + Frame_getStackOffset(lp);
                        break;
                    }

                    rcvrClass  = Object_getClass(rcvr);                         /* Get the receiver class */
                    for (;;) {
                        types   = Class_getInterfaceTypes(rcvrClass);           /* Get the interface types table */
                        tcount  = Object_getArrayElementCount(types);           /* Get the interface types table length */
                        for (i = 0 ; i < tcount ; i++) {                        /* Iterate through the interface types */
                            assumeArrayIndexInBounds(types, i);
                            cno = getUnsignedHalf(types, i);                    /* Get the next type */
                            if (cno == iklassType) {                            /* Match? */
                                int slotTable;
                                int tables = Class_getInterfaceSlotTables(rcvrClass); /* Y - Get the slot tables */
                                assumeArrayIndexInBounds(tables, i);
                                slotTable = getWord(tables, i);                 /* Get specific table */
                                assumeArrayIndexInBounds(slotTable, parm);
                                parm = getUnsignedByte(slotTable, parm);        /* Get virtual method */
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            break;
                        }
                        rcvrClass  = Class_getSuperClass(rcvrClass);            /* Try the next class up */
                        assume(rcvrClass != 0);
                    }

                    cp = Object_getClass(rcvr);                                 /* Get the callee's class */
                    vstart = Class_getFirstVirtualMethod(cp);                   /* Get the number of the first method defined */
                    for (;;) {
                        while (parm < vstart) {                                 /* Is the target method there? */
                            cp  = Class_getSuperClass(cp);                      /* N - Go to the super class */
                            assume(cp != 0);
                            vstart = Class_getFirstVirtualMethod(cp);           /* Get the number of the first method defined */
                        }                                                       /* Test again... */
                        vtbl = Class_getVirtualMethods(cp);                     /* Get the vtable for virtual methods */
                        assumeArrayIndexInBounds(vtbl, parm-vstart);
                        mp = getWord(vtbl, parm-vstart);                        /* Get the method */
                        if (mp != 0) {
                            break;
                        } else {
                            cp  = Class_getSuperClass(cp);                      /* N - Go to the super class */
                            assume(cp != 0);
                            vstart = Class_getFirstVirtualMethod(cp);           /* Get the number of the first method defined */
                        }
                    }

                    setupFrame(mp);

                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                         Extend                            *
                \*-----------------------------------------------------------*/

                case OPC.EXTEND_0:
                case OPC.EXTEND_1:
                case OPC.EXTEND_2:
                case OPC.EXTEND_3:
                case OPC.EXTEND_4:
                case OPC.EXTEND_5:
                case OPC.EXTEND_6:
                case OPC.EXTEND_7:
                case OPC.EXTEND_8:
                case OPC.EXTEND_9:
                case OPC.EXTEND_10:
                case OPC.EXTEND_11:
                case OPC.EXTEND_12:
                case OPC.EXTEND_13:
                case OPC.EXTEND_14:
                case OPC.EXTEND_15:
                case OPC.EXTEND: {
                    int restartIP = ip - 1;
                    int nstack = (b == OPC.EXTEND) ? fetchUnsignedByte(ip++) : b&15;
                    int mp = Frame_getCurrentMp(lp);
                    int nlocals = getUnsignedByte(mp, MTH_numberOfLocals);
                    int nsp = lp + w2b(nlocals + FRAME_HEADER_SIZE - 1);        /* Value for stack pointer after extend */
                    int ep = nsp + w2b(nstack);                                 /* Pointer to highest stack item in new frame */
                    int nparms = getUnsignedByte(mp, MTH_numberOfParms);
                    assume(sp == lp + w2b(nparms - 1));                         /* The stack must only contain the number */
                                                                                /* of parameters declared by this method */

                    if (ep >= sl || getVeryExcessiveGCEnabled()) {              /* Overflow? */
                        int newLp, newSp;
                        int minSize = getNewStackChunkMinSize(nstack, nlocals); /* Work out the minimum needed */
                        int cc = getChunkFromFrame(lp);                         /* Get the current chunk */
                        int nc = StackChunk_getNext(cc);                        /* Get the next chumk if there is one */
                        StackChunk_setNext(cc, 0);                              /* Clear pointer */
                        if (getVeryExcessiveGCEnabled()) {                      /* Only for testing */
                            nc = 0;                                             /* ... */
                        } else {
                            if (nc == 0 || StackChunk_getSize(nc) < minSize) {  /* Large enough? */
                                nc = newStackChunk(minSize);                    /* Allocate chunk */
                            }
                        }
                        if (nc == 0) {                                          /* Failed? */
                            Frame_setCurrentIp(lp, restartIP);                  /* Set to re-execute the extend */
                            Frame_setStackOffset(lp, sp - lp);                  /* Save the stack offset */
                            StackChunk_setLastLp(cc, lp);                       /* Save the lp */
                            return cc;                                          /* Go do gc */
                        }
                        sl = nc + w2b(StackChunk_getSize(nc));                  /* Set up the sl for this chunk */
                        assume(sl == nc+Object_getObjectLength(nc));
                        StackChunk_setPrev(nc, cc);                             /* Save the previous chunk address */
                        StackChunk_setLastLp(nc, 0);                            /* Needed? */
                        StackChunk_setNext(cc, nc);                             /* Save the next chunk */
                        StackChunk_setLastLp(cc, Frame_getPreviousLp(lp));      /* Save the last real frame */
                        newLp = nc + w2b(STACK_HEADER_SIZE+FRAME_HEADER_SIZE);  /* Get the next frame */
                        Frame_setPreviousLp(newLp, 0);                          /* This marks the first frame in the chunk */

                        Frame_setCurrentMp(newLp, Frame_getCurrentMp(lp));      /* Copy from old frame */
                        Frame_setCurrentIp(newLp, Frame_getCurrentIp(lp));      /* ... */
                        Frame_setCurrentCp(newLp, Frame_getCurrentCp(lp));      /* ... */

                        newSp = newLp;                                          /* Get start of params */
                        while (lp <= sp) {                                      /* Copy parms */
                            setWord(newSp, 0, getWord(lp, 0));                  /* ... */
                            newSp += 4;                                         /* ... */
                            lp  += 4;                                           /* ... */
                        }                                                       /* */
                        lp = newLp;                                             /* Get the new lp */
                        sp = newSp - 4;                                         /* Get the new sp */
                        nsp = newLp + w2b(nlocals + FRAME_HEADER_SIZE - 1);     /* Value for stack pointer after extend */
                        if (getTraceFrames()) {
                            trace("Created/reused stack chunk: ");
                            traceHex(nc);
                            trace(", size=");
                            traceHex(w2b(StackChunk_getSize(nc)));
                            trace(" bytes, depth=");
                            traceInt(getStackFrames(lp, 0, 0));
                            trace(", prev=");
                            traceHex(cc);
                            trace(", lp=");
                            traceHex(lp);
                            traceln("");
                        }
                    }
                    while (sp != nsp) {                                         /* Zero locals and next frame */
                        assume(sp+4 >= (lp + getUnsignedByte(mp, MTH_numberOfParms)));
                        push(0);
                    }
                    assume(sp == nsp);
                    assume(lp < sl);
                    assume(sp < sl);

                    Frame_setStackOffset(lp, sp - lp);                          /* Save the stack offset */
                    if (getTraceFrames()) {
                        trace("after extend: sp=");
                        traceHex(sp);
                        traceln("");
                    }
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                         Returns                           *
                \*-----------------------------------------------------------*/

                case OPC.RETURN:
                case OPC.RETURN1:
                case OPC.RETURN2: {

                    int oldsp = sp;

                    popFrame();

                    switch (b) {
                        case OPC.RETURN2: {
                            long temp = getLongAtWord(oldsp, -1);
                            pushLong(temp);
                            break;
                        }

                        case OPC.RETURN1: {
                            int temp1 = getWord(oldsp, 0);
                            push(temp1);
                            break;
                        }
                    }
                    continue;
                }


                /*-----------------------------------------------------------*\
                 *                           Throw                           *
                \*-----------------------------------------------------------*/

                case OPC.THROW: {
                    int cls;
                    int exception;
                    if (gcWorked) {
                        exception = pop();
                    } else {
                       /*
                        * Hack for entry to interpret() with gc failure
                        */
                        exception = getOutOfMemoryError();
                        ip = Frame_getCurrentIp(lp); /* Get the real ip */
                        gcWorked = true;             /* Reset flag */
                        if (exception == 0) {
                            fatalVMError("Not enough memory to start VM - OutOfMemoryError instance was not created");
                        }
                    }

                    if (nullCheck(exception)) {
                        break;
                    }

                    cls = Object_getClass(exception);

                    assume(sp == lp + Frame_getStackOffset(lp)); /* Reset sp */

                    while(true) {
                        int i, klass;
                        int mp = Frame_getCurrentMp(lp);
                        int headerSize = getUnsignedByte(mp, MTH_headerSize);
                        int oopMapSize = (getUnsignedByte(mp, MTH_numberOfLocals) + 7) / 8;
                        int p = 5 + oopMapSize;
                        int hcount = (headerSize - p) / 8;

                        /*
                         * The relative IP address is decremented by one to ensure
                         * it is in the range [X .. Y) where X is the address of
                         * the instruction in the current frame during which the
                         * exception occurred and Y is the address of the next
                         * instruction.
                         */
                        int relip = (ip - mp) - 1;

                        for (i = 0 ; i < hcount ; i++) {
                            int startIP, endIP, handlerIP, classNum;
                            startIP    = fetchUnsignedShort(mp+p); p += 2;
                            endIP      = fetchUnsignedShort(mp+p); p += 2;
                            handlerIP  = fetchUnsignedShort(mp+p); p += 2;
                            classNum   = fetchUnsignedShort(mp+p); p += 2;
                            if (relip >= startIP && relip < endIP) {
                               /*
                                * Iterate up the class hierarchy looking for a match
                                */
                                for (klass = cls ; klass != 0 ; klass = Class_getSuperClass(klass)) {
                                    if (Class_getType(klass) == classNum) {
                                       /*
                                        * Got a match.
                                        */
                                        ip = mp + handlerIP;
                                        push(exception);
/*IFJ*/                                 continue loop;
//IFC//                                 goto loop;
                                    }
                                }
                            }
                        }
                        popFrame();
                    }
                }


                /*-----------------------------------------------------------*\
                 *                       New objects                         *
                \*-----------------------------------------------------------*/

                case OPC.NEWOBJECT:{
                    int restartIP = ip - 1;                                     /* Get restart address */
                    int obj = Frame_getLocal(lp, 0);                            /* Get the receiver */
/*if[NEWSTRING]*/
                    assume(Class_getType(cp) != CNO.STRING);
                    assume(Class_getType(cp) != CNO.STRING_OF_BYTES);
/*end[NEWSTRING]*/
/*if[NEWSYMBOLS]*/
                    assume(Class_getType(cp) != CNO.STRING_OF_SYMBOLS);
/*end[NEWSYMBOLS]*/
                    if (obj == 0) {                                             /* Is is zero? */
                        obj = newInstance(cp);                                  /* Y - Allocate object */
                        if (obj == 0) {                                         /* Was it allocated? */
                            int cc = getChunkFromFrame(lp);                     /* N - Get current chunk */
                            Frame_setCurrentIp(lp, restartIP);                  /* Set to re-execute the extend */
                            assume(Frame_getStackOffset(lp) == sp - lp);
                            StackChunk_setLastLp(cc, lp);                       /* Save the lp */
                            return cc;                                          /* Go do gc */
                        }
                    }
                    setLocal(lp, 0, obj);                                       /* Save the  result */
/*if[FINALIZATION]*/
                    if ((Class_getAccess(cp) & ACC_HASFINALIZER) != 0) {
                        push(obj);
                        fn = java_lang_VMExtension_registerForFinalization;
                        break;
                    }
/*end[FINALIZATION]*/
                    continue;
                }

                case OPC.NEWARRAY: {
                    fn = java_lang_VMExtension_newArray;
                    break;
                }

                case OPC.NEWDIMENSION: {
                    fn = java_lang_VMExtension_newDimension;
                    break;
                }

                /*-----------------------------------------------------------*\
                 *                       Array loads                         *
                \*-----------------------------------------------------------*/

                case OPC.ALOAD: {
                    int ndx = pop();
                    int ref = pop();
                    if (nullCheck(ref)) break;
                    if (boundsCheck(ref, ndx)) break;
                    push(getWord(ref, ndx));
                    continue;
                }

                case OPC.ALOAD_B: {
                    int ndx = pop();
                    int ref = pop();
                    if (nullCheck(ref)) break;
                    if (boundsCheck(ref, ndx)) break;
                    push(getByte(ref, ndx));
                    continue;
                }

                case OPC.ALOAD_S: {
                    int ndx = pop();
                    int ref = pop();
                    if (nullCheck(ref)) break;
                    if (boundsCheck(ref, ndx)) break;
                    push(getHalf(ref, ndx));
                    continue;
                }

                case OPC.ALOAD_C: {
                    int ndx = pop();
                    int ref = pop();
                    if (nullCheck(ref)) break;
                    if (boundsCheck(ref, ndx)) break;
                    push(getUnsignedHalf(ref, ndx));
                    continue;
                }

                case OPC.ALOAD_I2: {
                    int ndx = pop();
                    int ref = pop();
                    if (nullCheck(ref)) break;
                    if (boundsCheck(ref, ndx)) break;
                    pushLong(getLong(ref, ndx));
                    continue;
                }

                /*-----------------------------------------------------------*\
                 *                       Array stores                        *
                \*-----------------------------------------------------------*/

                case OPC.ASTORE: {
                    int val = pop();
                    int ndx = pop();
                    int ref = pop();
                    if (nullCheck(ref)) break;
                    if (boundsCheck(ref, ndx)) break;
                    storeWord(ref, ndx, val);
                    if (storeFailed()) break;
                    continue;
                }

                case OPC.ASTORE_B: {
                    int val = pop();
                    int ndx = pop();
                    int ref = pop();
                    if (nullCheck(ref)) break;
                    if (boundsCheck(ref, ndx)) break;
                    storeByte(ref, ndx, val);
                    if (storeFailed()) break;
                    continue;
                }

                case OPC.ASTORE_S: {
                    int val = pop();
                    int ndx = pop();
                    int ref = pop();
                    if (nullCheck(ref)) break;
                    if (boundsCheck(ref, ndx)) break;
                    storeHalf(ref, ndx, val);
                    if (storeFailed()) break;
                    continue;
                }

                case OPC.ASTORE_O: {
                    int val = pop();
                    int ndx = pop();
                    int ref = pop();
                    if (nullCheck(ref)) break;
                    if (boundsCheck(ref, ndx)) break;
                    if (val != 0) {
                        int refcls = Object_getClass(ref);
                        if (!isSimplyAssignableToCno(val, Class_getElementType(refcls))) {
                           /*
                            * Call Java code to do more comprehensive test
                            */
                            push(ref);
                            push(ndx);
                            push(val);
                            fn = java_lang_VMExtension_astoreOop;
                            break;
                        }
                    }
                    storeOop(ref, ndx, val);
                    if (storeFailed()) break;
                    continue;

                }

                case OPC.ASTORE_I2: {
                    long val = popLong();
                    int  ndx = pop();
                    int  ref = pop();
                    if (nullCheck(ref)) break;
                    if (boundsCheck(ref, ndx)) break;
                    storeLong(ref, ndx, val);
                    if (storeFailed()) break;
                    continue;
                }

                /*-----------------------------------------------------------*\
                 *                      Get/Putfields                        *
                \*-----------------------------------------------------------*/


                case OPC.THIS_GETFIELD:
                    push(Frame_getLocal(lp, 0));

                case OPC.GETFIELD: {
                    int ref = pop();
                    int off = fetchUnsignedByte(ip++);
                    if (nullCheck(ref)) break;
                    assumeValidObjectField(ref, off, 4);
                    push(getWord(ref, off));
                    continue;
                }

                case OPC.THIS_GETFIELD_B:
                    push(Frame_getLocal(lp, 0));
                case OPC.GETFIELD_B: {
                    int ref = pop();
                    int off = fetchUnsignedByte(ip++);
                    if (nullCheck(ref)) break;
                    assumeValidObjectField(ref, off, 1);
                    push(getByte(ref, off));
                    continue;
                }

                case OPC.THIS_GETFIELD_S:
                    push(Frame_getLocal(lp, 0));
                case OPC.GETFIELD_S: {
                    int ref = pop();
                    int off = fetchUnsignedByte(ip++);
                    if (nullCheck(ref)) break;
                    assumeValidObjectField(ref, off, 2);
                    push(getHalf(ref, off));
                    continue;
                }

                case OPC.THIS_GETFIELD_C:
                    push(Frame_getLocal(lp, 0));
                case OPC.GETFIELD_C: {
                    int ref = pop();
                    int off = fetchUnsignedByte(ip++);
                    if (nullCheck(ref)) break;
                    assumeValidObjectField(ref, off, 2);
                    push(getUnsignedHalf(ref, off));
                    continue;
                }

                case OPC.THIS_GETFIELD_I2:
                    push(Frame_getLocal(lp, 0));
                case OPC.GETFIELD_I2: {
                    int ref = pop();
                    int off = fetchUnsignedByte(ip++);
                    if (nullCheck(ref)) break;
                    assumeValidObjectField(ref, off+1, 4);
                    pushLong(getLongAtWord(ref, off));
                    continue;
                }

                case OPC.THIS_PUTFIELD: {
                    int val = pop();
                    push(Frame_getLocal(lp, 0));
                    push(val);
                }
                case OPC.PUTFIELD: {
                    int val = pop();
                    int ref = pop();
                    int off = fetchUnsignedByte(ip++);
                    if (nullCheck(ref)) break;
                    assumeValidObjectField(ref, off, 4);
                    storeWord(ref, off, val);
                    if (storeFailed()) break;
                    continue;
                }

                case OPC.THIS_PUTFIELD_B: {
                    int val = pop();
                    push(Frame_getLocal(lp, 0));
                    push(val);
                }
                case OPC.PUTFIELD_B: {
                    int val = pop();
                    int ref = pop();
                    int off = fetchUnsignedByte(ip++);
                    if (nullCheck(ref)) break;
                    assumeValidObjectField(ref, off, 1);
                    storeByte(ref, off, val);
                    if (storeFailed()) break;
                    continue;
                }

                case OPC.THIS_PUTFIELD_S: {
                    int val = pop();
                    push(Frame_getLocal(lp, 0));
                    push(val);
                }
                case OPC.PUTFIELD_S: {
                    int val = pop();
                    int ref = pop();
                    int off = fetchUnsignedByte(ip++);
                    if (nullCheck(ref)) break;
                    assumeValidObjectField(ref, off, 2);
                    storeHalf(ref, off, val);
                    if (storeFailed()) break;
                    continue;
                }

                case OPC.THIS_PUTFIELD_O: {
                    int val = pop();
                    push(Frame_getLocal(lp, 0));
                    push(val);
                }
                case OPC.PUTFIELD_O: {
                    int val = pop();
                    int ref = pop();
                    int off = fetchUnsignedByte(ip++);
                    checkAddress(val, 0);
                    if (nullCheck(ref)) break;
                    assumeValidObjectField(ref, off, 4);
                    storeOop(ref, off, val);
                    if (storeFailed()) break;
                    continue;
                }

                case OPC.THIS_PUTFIELD_I2: {
                    long val = popLong();
                    push(Frame_getLocal(lp, 0));
                    pushLong(val);
                }
                case OPC.PUTFIELD_I2: {
                    long val = popLong();
                    int  ref = pop();
                    int  off = fetchUnsignedByte(ip++);
                    if (nullCheck(ref)) break;
                    assumeValidObjectField(ref, off+1, 4);
                    storeLongAtWord(ref, off, val);
                    if (storeFailed()) break;
                    continue;
                }

                /*-----------------------------------------------------------*\
                 *                      Get/Putstatics                       *
                \*-----------------------------------------------------------*/


                case OPC.CLASS_GETSTATIC:
                    push(cp);
                case OPC.GETSTATIC: {
                    int offset = fetchUnsignedByte(ip++);
                    int cls = pop();
                    int cs  = findClassState(cls);
                    if (cs != 0) {
                        assumeArrayIndexInBounds(cs, offset);
                        push(getWord(cs, offset));
                        continue;
                    } else {
                        push(offset);
                        push(cls);
                        fn = java_lang_VMExtension_getStatic;
                        break;
                    }
                }

                case OPC.CLASS_GETSTATIC_I2:
                    push(cp);
                case OPC.GETSTATIC_I2: {
                    int offset = fetchUnsignedByte(ip++);
                    int cls = pop();
                    int cs  = findClassState(cls);
                    if (cs != 0) {
                        long val = getLongAtWord(cs, offset);
                        assumeArrayIndexInBounds(cs, offset);
                        pushLong(val);
                        continue;
                    } else {
                        push(offset);
                        push(cls);
                        fn = java_lang_VMExtension_getStaticLong;
                        break;
                    }
                }

                case OPC.CLASS_PUTSTATIC:
                    push(cp);
                case OPC.PUTSTATIC: {
                    int offset = fetchUnsignedByte(ip++);
                    int cls = pop();
                    int val = pop();
                    int cs  = findClassState(cls);
                    if (cs != 0) {
                        assumeArrayIndexInBounds(cs, offset);
                        setWord(cs, offset, val);
                        continue;
                    } else {
                        push(val);
                        push(offset);
                        push(cls);
                        fn = java_lang_VMExtension_putStatic;
                        break;
                    }
                }

                case OPC.CLASS_PUTSTATIC_O:
                    push(cp);
                case OPC.PUTSTATIC_O: {
                    int offset = fetchUnsignedByte(ip++);
                    int cls = pop();
                    int val = pop();
                    int cs  = findClassState(cls);
                    checkAddress(val, 0);
                    if (cs != 0) {
                        assumeArrayIndexInBounds(cs, offset);
                        setOopRam(cs, offset, val);
                        continue;
                    } else {
                        push(val);
                        push(offset);
                        push(cls);
                        fn = java_lang_VMExtension_putStaticObj;
                        break;
                    }
                }

                case OPC.CLASS_PUTSTATIC_I2:
                    push(cp);
                case OPC.PUTSTATIC_I2: {
                    int  offset = fetchUnsignedByte(ip++);
                    int  cls = pop();
                    long val = popLong();
                    int cs  = findClassState(cls);
                    if (cs != 0) {
                        assumeArrayIndexInBounds(cs, offset);
                        setLongAtWord(cs, offset, val);
                        continue;
                    } else {
                        pushLong(val);
                        push(offset);
                        push(cls);
                        fn = java_lang_VMExtension_putStaticLong;
                        break;
                    }
                }

                case OPC.CLASS_CLINIT:
                    push(cp);
                case OPC.CLINIT: {
                    int cls = pop();
                    if ((Class_getAccess(cls) & ACC_MUSTCLINIT) == 0) {
                        continue;  /* Skip if class initialization is not required */
                    } else {
                        int cs  = findClassState(cls);
                        if (cs != 0) {
                            continue;
                        } else {
                            push(cls);
                            fn = java_lang_VMExtension_initializeClass;
                            break;
                        }
                    }
                }


                /*-----------------------------------------------------------*\
                 *                      Integer ops                          *
                \*-----------------------------------------------------------*/

                case OPC.IADD:  { int r = pop() ; int l = pop() ; push(l + r);      continue; }
                case OPC.ISUB:  { int r = pop() ; int l = pop() ; push(l - r);      continue; }
                case OPC.IAND:  { int r = pop() ; int l = pop() ; push(l & r);      continue; }
                case OPC.IOR:   { int r = pop() ; int l = pop() ; push(l | r);      continue; }
                case OPC.IXOR:  { int r = pop() ; int l = pop() ; push(l ^ r);      continue; }
                case OPC.ISHL:  { int r = pop() ; int l = pop() ; push(sll(l, r));  continue; }
                case OPC.ISHR:  { int r = pop() ; int l = pop() ; push(sra(l, r));  continue; }
                case OPC.IUSHR: { int r = pop() ; int l = pop() ; push(srl(l, r));  continue; }
                case OPC.IMUL:  { int r = pop() ; int l = pop() ; push(l * r);      continue; }
                case OPC.NEG:   { int r = pop() ;                 push(0 - r);      continue; }
                case OPC.I2B:   { int r = pop() ;                 push((byte)r);    continue; }
                case OPC.I2S:   { int r = pop() ;                 push((short)r);   continue; }
/*IFJ*/         case OPC.I2C:   { int r = pop() ;                 push((char)r);    continue; }
//IFC//         case OPC.I2C:   { int r = pop() ;       push((unsigned short)r);    continue; }

                case OPC.IDIV: {
                    int r = pop();
                    int l = pop();
                    if (r == 0) {
                        fn = java_lang_VMExtension_arithmeticException;
                        clearStack();
                        break;
                    } else if (l == 0x80000000 && r == -1) {
                        push(l);
                        continue;
                    } else {
                        push(l / r);
                        continue;
                    }
                }

                case OPC.IREM: {
                    int r = pop();
                    int l = pop();
                    if (r == 0) {
                        fn = java_lang_VMExtension_arithmeticException;
                        clearStack();
                        break;
                    } else if (l == 0x80000000 && r == -1) {
                        push(l % 1);
                        continue;
                    } else {
                        push(l % r);
                        continue;
                    }
                }


                /*-----------------------------------------------------------*\
                 *                        Long ops                           *
                \*-----------------------------------------------------------*/
/*if[LONGS]*/
                case OPC.LONGOP: {
                    b = fetchUnsignedByte(ip++);
                    switch (b) {
                        case OPC.LADD: { long r = popLong() ; long l = popLong() ; pushLong(l + r);         continue; }
                        case OPC.LSUB: { long r = popLong() ; long l = popLong() ; pushLong(l - r);         continue; }
                        case OPC.LMUL: { long r = popLong() ; long l = popLong() ; pushLong(l * r);         continue; }
                        case OPC.LAND: { long r = popLong() ; long l = popLong() ; pushLong(l & r);         continue; }
                        case OPC.LOR:  { long r = popLong() ; long l = popLong() ; pushLong(l | r);         continue; }
                        case OPC.LXOR: { long r = popLong() ; long l = popLong() ; pushLong(l ^ r);         continue; }
                        case OPC.LNEG: { long r = popLong() ;                      pushLong(0 - r);         continue; }
                        case OPC.LSHL: { int  r = pop()     ; long l = popLong() ; pushLong(slll(l, r));    continue; }
                        case OPC.LSHR: { int  r = pop()     ; long l = popLong() ; pushLong(sral(l, r));    continue; }
                        case OPC.LUSHR:{ int  r = pop()     ; long l = popLong() ; pushLong(srll(l, r));    continue; }
                        case OPC.LCMP: { long r = popLong() ; long l = popLong() ; push(cmpl(l, r));        continue; }
                        case OPC.I2L:  { int  r = pop()     ;                      pushLong(r);             continue; }
                        case OPC.L2I:  { long r = popLong() ;                      push((int)r);            continue; }

                        case OPC.LDIV: {
                            long r = popLong();
                            long l = popLong();
                            if (r == 0) {
                                break;
                            } else {
                                pushLong(l / r);
                                continue;
                            }
                        }

                        case OPC.LREM: {
                            long r = popLong();
                            long l = popLong();
                            if (r == 0) {
                                break;
                            } else {
                                pushLong(l % r);
                                continue;
                            }
                        }

                        default: fatalVMError("invalid long bytecode");
                    }
                    fn = java_lang_VMExtension_arithmeticException;
                    clearStack();
                    break;
                }
/*end[LONGS]*/

                /*-----------------------------------------------------------*\
                 *                        Float ops                          *
                \*-----------------------------------------------------------*/

/*if[FLOATS]*/
                case OPC.FLOATOP: {
                    b = fetchUnsignedByte(ip++);
                    switch (b) {
                        case OPC.FADD: { int  r = pop()     ; int  l = pop()     ; push(addf(l, r));        continue; }
                        case OPC.FSUB: { int  r = pop()     ; int  l = pop()     ; push(subf(l, r));        continue; }
                        case OPC.FMUL: { int  r = pop()     ; int  l = pop()     ; push(mulf(l, r));        continue; }
                        case OPC.FDIV: { int  r = pop()     ; int  l = pop()     ; push(divf(l, r));        continue; }
                        case OPC.FREM: { int  r = pop()     ; int  l = pop()     ; push(remf(l, r));        continue; }
                        case OPC.FNEG: { int  r = pop()     ;                      push(negf(r));           continue; }
                        case OPC.FCMPG:{ int  r = pop()     ; int  l = pop()     ; push(cmpfg(l, r));       continue; }
                        case OPC.FCMPL:{ int  r = pop()     ; int  l = pop()     ; push(cmpfl(l, r));       continue; }
                        case OPC.DADD: { long r = popLong() ; long l = popLong() ; pushLong(addd(l, r));    continue; }
                        case OPC.DSUB: { long r = popLong() ; long l = popLong() ; pushLong(subd(l, r));    continue; }
                        case OPC.DMUL: { long r = popLong() ; long l = popLong() ; pushLong(muld(l, r));    continue; }
                        case OPC.DDIV: { long r = popLong() ; long l = popLong() ; pushLong(divd(l, r));    continue; }
                        case OPC.DREM: { long r = popLong() ; long l = popLong() ; pushLong(remd(l, r));    continue; }
                        case OPC.DNEG: { long r = popLong() ;                      pushLong(negd(r));       continue; }
                        case OPC.DCMPG:{ long r = popLong() ; long l = popLong() ; push(cmpdg(l, r));       continue; }
                        case OPC.DCMPL:{ long r = popLong() ; long l = popLong() ; push(cmpdl(l, r));       continue; }
                        case OPC.I2F:  { int  r = pop()     ;                      push(i2f(r));            continue; }
                        case OPC.L2F:  { long r = popLong() ;                      push(l2f(r));            continue; }
                        case OPC.F2I:  { int  r = pop()     ;                      push(f2i(r));            continue; }
                        case OPC.F2L:  { int  r = pop()     ;                      pushLong(f2l(r));        continue; }
                        case OPC.I2D:  { int  r = pop()     ;                      pushLong(i2d(r));        continue; }
                        case OPC.L2D:  { long r = popLong() ;                      pushLong(l2d(r));        continue; }
                        case OPC.F2D:  { int  r = pop()     ;                      pushLong(f2d(r));        continue; }
                        case OPC.D2I:  { long r = popLong() ;                      push(d2i(r));            continue; }
                        case OPC.D2L:  { long r = popLong() ;                      pushLong(d2l(r));        continue; }
                        case OPC.D2F:  { long r = popLong() ;                      push(d2f(r));            continue; }
                        default: fatalVMError("invalid float bytecode");
                    }
                }
/*end[FLOATS]*/

                /*-----------------------------------------------------------*\
                 *                            Misc                           *
                \*-----------------------------------------------------------*/

                case OPC.POP:
                    pop();
                case OPC.NOP:
                    continue;

                case OPC.ARRAYLENGTH: {
                    int ref = pop();
                    if (nullCheck(ref)) break;
                    push(Object_getArrayElementCount(ref));
                    continue;
                }

                case OPC.CLASS_MONITORENTER:
                    push(cp);
                case OPC.MONITORENTER: {
                    int obj = pop();
                    if (nullCheck(obj)) break;
                    push(obj);
                    fn = java_lang_VMExtension_monitorEnter;
                    break;
                }

                case OPC.CLASS_MONITOREXIT:
                    push(cp);
                case OPC.MONITOREXIT: {
                    int obj = pop();
                    if (nullCheck(obj)) break;
                    push(obj);
                    fn = java_lang_VMExtension_monitorExit;
                    break;
                }

                case OPC.INSTANCEOF: {
                    int cls = pop();
                    int obj = pop();
                    if (obj != 0 && cls != 0 && isSimplyAssignable(obj, cls)) {
                        push(1);
                        continue;
                    }

                   /*
                    * Call Java code to do more comprehensive test
                    */
                    push(obj);
                    push(cls);
                    fn = java_lang_VMExtension_instanceOf;
                    break;
                }

                case OPC.CHECKCAST: {
                    int cls = pop();
                    int obj = pop();
                    if (obj == 0 || isSimplyAssignable(obj, cls)) {
                        push(obj);
                        continue;
                    }

                   /*
                    * Call Java code to do more comprehensive test
                    */
                    push(obj);
                    push(cls);
                    fn = java_lang_VMExtension_checkcast;
                    break;
                }

                case OPC.BPT:
                    fn = java_lang_VMExtension_breakpoint;
                    break;


                /*-----------------------------------------------------------*\
                 *                       Invoke native                       *
                \*-----------------------------------------------------------*/

                case OPC.INVOKENATIVE:
                case OPC.INVOKENATIVE1:
                case OPC.INVOKENATIVE2: {
                    int tos = pop(); /* pop the unwanted class reference */
                    int parm = fetchUnsignedByte(ip++);
                    incInvokePrimitiveCount();
                    switch (parm) {
                        case java_lang_Native_enableExcessiveGC: {
                            setExcessiveGCEnabled(true);
                            continue;
                        }

                        case java_lang_Native_primAstoreOop: {
                            int val = pop();
                            int ndx = pop();
                            int ref = pop();
                            if (nullCheck(ref)) break;
                            if (boundsCheck(ref, ndx)) break;
                            storeOop(ref, ndx, val);
                            if (storeFailed()) break;
                            continue;
                        }

                        case java_lang_Native_getInstructionCount: {
                            pushLong(getInstructionCount());
                            continue;
                        }

                        case java_lang_Native_getAddress: {
                            continue; /* leave on stack */
                        }

                        case java_lang_Native_traceOop: {
                            int oop = pop();
                            printInt(oop);
                            continue;
                        }

                        case java_lang_Native_getPersistentMemoryTable: {
                            push(getPersistentMemoryTable());
                            continue;
                        }

                        case java_lang_Native_makePersistent: {
                            int oop = pop();                                    /* Get the root of the graph to be migrated */
                            oop = makePersistent(oop, true);                    /* Try to migrate its graph to EEPROM */
                            push(oop);                                          /* return the root of the migrated graph or null if insufficient EEPROM */
                            continue;
                        }

                        case java_lang_Native_makePersistentCopy: {
                            int oop = pop();                                    /* Get the root of the graph to be copied */
                            oop = makePersistent(oop, false);                   /* Try to copy the graph to EEPROM */
                            push(oop);                                          /* return the root of the copied graph or null if insufficient EEPROM */
                            continue;
                        }

                        case java_lang_Native_setPersistentMemoryTable: {
                            int table = pop();
                            setPersistentMemoryTable(table);
                            continue;
                        }

                        case java_lang_Native_setAssociationHashtable: {
                            int table = pop();
                            setAssociationHashtable(table);
                            continue;
                        }

                        case java_lang_Native_exec: {
                            int p3   = pop();
                            int p2   = pop();
                            int p1   = pop();
                            int i6   = pop();
                            int i5   = pop();
                            int i4   = pop();
                            int i3   = pop();
                            int i2   = pop();
                            int i1   = pop();
                            int op   = pop();
                            int chan = pop();
                            push(chan_execute(chan, op, i1, i2, i3, i4, i5, i6, p1, p2, p3));
                            continue;
                        }

                        case java_lang_Native_result: {
                            int chan = pop();
                            pushLong(chan_result(chan));
                            continue;
                        }

                        case java_lang_Native_getNewExecutionContext: {
                            int thread = pop();
                            int ctx = createExecutionContext(java_lang_VMExtension_callrun, thread);
                            push(ctx);
                            continue;
                        }

                        case java_lang_Native_setExecutionContext: {
                            int id        = pop();
                            int newThread = pop();
                            int oldIsDead = pop();
                            int oldThread = pop();
                            int cc = getChunkFromFrame(lp);
                            if (oldThread != 0) {
                                if (oldIsDead != 0) {
                                    StackChunk_setLastLp(cc, 0); /* Mark the chunk as dead */
                                    Thread_setContext(oldThread, 0);
                                } else {
                                    Frame_setCurrentIp(lp, ip);
                                    Frame_setCurrentCp(lp, cp);
                                    assume(Frame_getStackOffset(lp) == sp - lp);
                                    StackChunk_setLastLp(cc, lp);
                                    Thread_setContext(oldThread, cc);
                                }
                            }
                            cc = Thread_getContext(newThread);
                            lp = StackChunk_getLastLp(cc);
                            ip = Frame_getCurrentIp(lp);
                            cp = Frame_getCurrentCp(lp);
                            sp = lp + Frame_getStackOffset(lp);
                            sl = cc + w2b(StackChunk_getSize(cc));
                            bc = TIMEQUANTA;
                            StackChunk_setLastLp(cc, 0);
                            incSwitchCount();
                            setThreadID(id);

                            /*
                             * The 'context' field of the current thread is set
                             * to null as the stack chunk it points to may be
                             * junk (i.e. it was popped) which will confuse the
                             * Cheney collector which expects all live stack chunks
                             * to have a non-null lastLp. This means that this
                             * field of a Thread object should not be relied upon
                             * in Java code (which is currently the case).
                             */
                            Thread_setContext(newThread, 0);
                            continue;
                        }

                        case java_lang_Native_primNewArray: {
                            int siz = pop();
                            int cls = pop();
                            push(newArray(cls, siz));
                            continue;
                        }

                        case java_lang_Native_callStaticNoParm:
                        case java_lang_Native_callStaticOneParm: {
                            int mp;
                            int vtbl;
                            int slot;
                            slot = pop();
                            cp   = pop();
                            pushFrame();
                            vtbl = Class_getStaticMethods(cp);
                            assumeArrayIndexInBounds(vtbl, slot);
                            mp = getWord(vtbl, slot);
                            setupFrame(mp);
                            continue;
                        }

                        case java_lang_Native_getSuiteList: {
                            int suiteTable = getSuiteTableEeprom();
                            if (suiteTable == 0) {
                                suiteTable = getSuiteTableRom();
                            }
                            assume(suiteTable != 0);
                            push(suiteTable);
                            continue;
                        }

                        case java_lang_Native_setSuiteList: {
                            setSuiteTableEeprom(pop());
                            continue;
                        }

                        case java_lang_Native_setOutOfMemoryError: {
                            setOutOfMemoryError(pop());
                            continue;
                        }

                        case java_lang_Native_getargc: {
                            push(getArgumentCount());
                            continue;
                        }

                        case java_lang_Native_getargvchar: {
                            int pos = pop();
                            int arg = pop();
                            push(getArgumentChar(arg, pos));
                            continue;
                        }

                        case java_lang_Native_exit: {
/*if[VM.GCSPY]*/
/*IFJ*/                     gcspyFinalizeLisp2Driver();
/*end[VM.GCSPY]*/
                            exitVM(pop());
                            continue;
                        }

                        case java_lang_Native_gc: {
                            int cc = getChunkFromFrame(lp);                     /* Get current chunk */
                            Frame_setCurrentIp(lp, ip);                         /* Set to execute the next bytecode */
                            assume(Frame_getStackOffset(lp) == sp - lp);        /* Check... */
                            StackChunk_setLastLp(cc, lp);                       /* Save the lp */
                            return cc;                                          /* Go do gc */
                        }

                        case java_lang_Native_freeMemory: {
                            boolean ram = (pop() != 0);
                            if (ram) {
                                pushLong(freeMem());
                            } else {
                                pushLong(freePersistentMemory());
                            }
                            continue;
                        }

                        case java_lang_Native_totalMemory: {
                            boolean ram = (pop() != 0);
                            if (ram) {
                                pushLong(totalMem());
                            } else {
                                pushLong(totalPersistentMemory());
                            }
                            continue;
                        }

                        case java_lang_Native_getTime: {
                            pushLong(getTime());
                            continue;
                        }

                        case java_lang_Native_arraycopy0: {
                            int length = pop();
                            int dstPos = pop();
                            int dst    = pop();
                            int srcPos = pop();
                            int src    = pop();
                            Object_arrayCopy(src, srcPos, dst, dstPos, length);
                            continue;
                        }

                        case java_lang_Native_getEvent: {
                            push(getEvent());
                            continue;
                        }

                        case java_lang_Native_waitForEvent: {
                            long delta = popLong();
                            waitForEvent(delta);
                            continue;
                        }

                        case java_lang_Native_fatalVMError: {
                            fatalVMError("Native.fatalVMError()");
                            continue;
                        }

                        case java_lang_Native_getClass: {
                            int obj = pop();
                            push(Object_getClass(obj));
                            continue;
                        }

                        case java_lang_Native_getArrayLength: {
                            int array = pop();
                            push(Object_getArrayElementCount(array));
                            continue;
                        }

                        case java_lang_Native_putch: {
                            int ch = pop();
                            putch(ch, false);
                            continue;
                        }

                        case java_lang_Native_getAssociationQueue: {
                            int obj = pop();
                            int hash = getAssociationHashEntry(obj);
                            push(getAssociationQueue(hash));
                            continue;
                        }
                        case java_lang_Native_setAssociationQueue: {
                            int asn = pop();
                            int obj = pop();
                            int hash = getAssociationHashEntry(obj);
                            setAssociationQueue(hash, asn);
                            if (assuming()) {
                                int cls = Object_getClass(asn);
                                setAssociationCno(Class_getType(cls));
                            }
                            continue;
                        }

                        case java_lang_Native_getClassState: {
                            int cls = pop();
                            int cs  = findClassState(cls);
                            push(cs);
                            continue;
                        }

                        case java_lang_Native_createClassState: {
                            int cls = pop();
                            int cs  = newClassState(cls);
                            if (cs != 0) {
                                ClassState_setClass(cs, cls);  /* Set the class pointer */
                            }
                            push(cs);
                            continue;
                        }

                        case java_lang_Native_primGetStatic: {
                            int offset = pop();
                            int cs     = pop();
                            assumeArrayIndexInBounds(cs, offset);
                            push(getWord(cs, offset));
                            continue;
                        }

                        case java_lang_Native_primGetStaticLong: {
                            int offset = pop();
                            int cs     = pop();
                            assumeArrayIndexInBounds(cs, offset);
                            pushLong(getLongAtWord(cs, offset));
                            continue;
                        }

                        case java_lang_Native_primPutStatic: {
                            int val    = pop();
                            int offset = pop();
                            int cs     = pop();
                            assumeArrayIndexInBounds(cs, offset);
                            setWord(cs, offset, val);
                            continue;
                        }

                        case java_lang_Native_primPutStaticLong: {
                            long val    = popLong();
                            int  offset = pop();
                            int  cs     = pop();
                            assumeArrayIndexInBounds(cs, offset);
                            setLongAtWord(cs, offset, val);
                            continue;
                        }

                        case java_lang_Native_primPutStaticObj: {
                            int val    = pop();
                            int offset = pop();
                            int cs     = pop();
                            assumeArrayIndexInBounds(cs, offset);
                            setOopRam(cs, offset, val);
                            continue;
                        }

                        case java_lang_Native_setClassState: {
                            int cs  = pop();
                            int cls = pop();
                            addToClassState(cls, cs);
                            continue;
                        }

                        case java_lang_Native_primNewObject: {
                            int slot = pop();
                            int cls  = pop();
                            int res;
                            res  = newInstance(cls);
                            push(res);
                            if (res != 0) {
                                int mp;
                                int vtbl;
                                incInvokeCount();
                                pushFrame();
                                cp = cls;
                                vtbl = Class_getStaticMethods(cls);
                                assumeArrayIndexInBounds(vtbl, slot);
                                mp = getWord(vtbl, slot);
                                setupFrame(mp);
                            }
                            continue;
                        }

                        case java_lang_Native_setFastLockStack: {
                            int stack = pop();
                            setFastLockStack(stack);
                            fastLockLimit = Object_getArrayElementCount(stack);
                            continue;
                        }

                        case java_lang_Native_getStackFrameDepth: {
                            push(getStackFrames(lp, 0, 0));
                            continue;
                        }

                        case java_lang_Native_getActivationStack: {
                            int ipStack = pop();
                            int methods = pop();
                            assume(methods != 0);
                            assume(ipStack != 0);
                            assume(Object_getArrayElementCount(ipStack) == Object_getArrayElementCount(methods));
                            getStackFrames(lp, methods, ipStack);
                            continue;
                        }
/*if[FINALIZATION]*/

                        case java_lang_Native_getFinalizer: {
                            int res = getFinalizerQueue();
                            if (res != 0) {
                                int next = ObjectAssociation_getNext(res);
                                setFinalizerQueue(next);
                                ObjectAssociation_setNext_safe(res, 0);
                            }
                            push(res);
                            continue;
                        }
/*end[FINALIZATION]*/

                        case java_lang_Native_puterrch: {
                            int ch = pop();
                            putch(ch, true);
                            continue;
                        }

                        case java_lang_Native_getPersistentMemorySize: {
                            push(getObjectPartitionEnd(EEPROM) - getObjectPartitionStart(EEPROM));
                            continue;
                        }

                        case java_lang_Native_gcPersistentMemory: {
                            int cc = getChunkFromFrame(lp);                     /* Get current chunk */
                            int markStack = pop();
                            int bitVector = pop();
                            assume(Class_getType(Object_getClass(bitVector)) == CNO.INT_ARRAY);
                            assume(Class_getType(Object_getClass(markStack)) == CNO.INT_ARRAY);
                            StackChunk_setLastLp(cc, lp);                       /* Save the last lp */
                            gcPersistentMemory(bitVector, markStack);
                            StackChunk_setLastLp(cc, 0);                        /* Unset the last lp */
                            continue;
                        }

                        case java_lang_Native_wasPersistentMemoryGCInterrupted: {
                            push(getEepromFreeList() == 0 ? 1 : 0);
                            continue;
                        }

                        case java_lang_Native_setMinimumHeapMode: {
                            int mode = pop();
                            setMinimumHeapMode(mode == 1);
                            continue;
                        }

                        case java_lang_Native_getHeapHighWaterMark: {
                            // TEMP
                            //printMsg("java_lang_Native_getHeapHighWaterMark -- Tracing RAM");
                            //println();
                            //traceln("java_lang_Native_getHeapHighWaterMark -- Tracing RAM");
                            //traceHeapSummary(getCurrentSegment());
                            //traceWholeHeap(getCurrentSegment());
                            //TEMP
                            pushLong(getHeapHighWaterMark());
                            continue;
                        }

/*if[DEBUG.METHODDEBUGTABLE]*/
                        case java_lang_Native_getMethodDebugTable: {
                            int methodDebugTable = getMethodDebugTable(RAM);
                            if (methodDebugTable == 0) {
                                methodDebugTable = getMethodDebugTable(ROM);
                            }
                            push(methodDebugTable);
                            continue;
                        }

                        case java_lang_Native_setMethodDebugTable: {
                            int methodDebugTable = pop();
                            setMethodDebugTable(RAM, methodDebugTable);
                            continue;
                        }
/*end[DEBUG.METHODDEBUGTABLE]*/



/*if[NEWSTRING]*/
                        case java_lang_Native_makeEightBitString: {
                            int oop = pop();
                            int cls = getClassFromCNO(CNO.STRING_OF_BYTES);
                            Object_setClass(oop, cls);
                            push(oop);
                            continue;
                        }

                        case java_lang_Native_makeSixteenBitString: {
                            int oop = pop();
                            int cls = getClassFromCNO(CNO.STRING);
                            Object_setClass(oop, cls);
                            push(oop);
                            continue;
                        }

                        case java_lang_Native_stringcopy0: {
                            int length = pop();
                            int dstPos = pop();
                            int dst    = pop();
                            int srcPos = pop();
                            int src    = pop();
                            int src_itemLength = Object_getArrayElementLength(Object_getClass(src));
                            int dst_itemLength = Object_getArrayElementLength(Object_getClass(dst));
                            int srcEnd = length + srcPos;
                            int dstEnd = length + dstPos;

                            assume(src_itemLength != 0 && dst_itemLength != 0);
                            assume(
                                   (
                                    (length < 0) ||
                                    (srcPos < 0) ||
                                    (dstPos < 0) ||
                                    (length > 0 && (srcEnd < 0 || dstEnd < 0)) ||
                                    (srcEnd > Object_getArrayElementCount(src)) ||
                                    (dstEnd > Object_getArrayElementCount(dst))
                                   ) == false
                                  );

                            if (src_itemLength == dst_itemLength) {
                                Object_arrayCopy(src, srcPos, dst, dstPos, length);
                            } else {
                                int i;
                                if (src_itemLength == 1 && dst_itemLength == 2) {
                                    for (i = 0 ; i < length ; i++) {
                                        int x = getUnsignedByte(src, srcPos++);
                                        storeHalf(dst, dstPos++, x);
                                    }
                                } else if (src_itemLength == 2 && dst_itemLength == 1) {
                                    for (i = 0 ; i < length ; i++) {
                                        int x = getUnsignedHalf(src, srcPos++);
                                        storeByte(dst, dstPos++, x);
                                    }
                                } else {
                                    fatalVMError("Bad java_lang_Native_stringcopy_62");
                                }
                            }
                            continue;
                        }

                        case java_lang_Native_makeStringOfSymbols: {
                            int oop = pop();
                            int cls = getClassFromCNO(CNO.STRING_OF_SYMBOLS);
                            Object_setClass(oop, cls);
                            push(oop);
                            continue;
                        }

                        case java_lang_Native_inRam: {
                            int oop = pop();
                            push(inSegment(oop, RAM, MMR_ramSize) ? 1 : 0);
                            continue;
                        }

                        case java_lang_Native_print: {
                            int oop = pop();
                            if  (oop == 0) {
                                putch('n', false);
                                putch('u', false);
                                putch('l', false);
                                putch('l', false);
                            } else {
                                int cls = Object_getClass(oop);
                                int cno = Class_getType(cls);
                                int lth = String_length(oop);
                                int i;
                                for (i = 0 ; i < lth ; i++) {
                                    int ch = String_at(oop, i);
                                    putch(ch, false);
                                }
                            }
                            continue;
                        }
/*end[NEWSTRING]*/


                        case java_lang_Native_newPersistentByteArray: {
                            int size = pop();
                            push(newPersistentByteArray(size));
                            continue;
                        }


/*if[FLOATS]*/
                        case java_lang_Native_math: {
                            long long2 = popLong();
                            long long1 = popLong();
                            int op = pop();
                            pushLong(math0(op, long1, long2));
                            continue;
                        }

                        case java_lang_Native_floatToIntBits:
                        case java_lang_Native_doubleToLongBits:
                        case java_lang_Native_intBitsToFloat:
                        case java_lang_Native_longBitsToDouble: {
                            continue;
                        }
/*end[FLOATS]*/


                        case java_lang_String_at: {
                            int ndx = tos;
                            int str = pop();
                            if (nullCheck(str)) break;
                            push(String_at(str, ndx));
                            continue;
                        }

                        case java_lang_String_length: {
                            int str = tos;
                            if (nullCheck(str)) break;
                            push(String_length(str));
                            continue;
                        }

                        case java_lang_String_isEightBit: {
                            int str = tos;
                            if (nullCheck(str)) break;
                            push(String_isEightBit(str) ? 1 : 0);
                            continue;
                        }

                        default: {
                            printMsg("Bad native method identifier: ");
                            printInt(parm);
                            println();
                            fatalVMError("Bad native function");
                        }
                    }
                    break;
                }

                case OPC.METHODID: {
                    ip += 4;
                    continue;
                }

                default: {
                    fatalVMError("Bad bytecode");
                }

            } /* end of switch */


           /*-----------------------------------------------------------*\
            *                     Exception handling                    *
           \*-----------------------------------------------------------*/

           /*
            * When some part of the code in the main switch statement needs
            * to invoke some Java code then a 'break' is executed and control
            * is passed to this point. Two entry conditions must be met. The
            * 'fn' variable must be set to a value that corresponds with
            * one of the static slots of the class java.lang.VMExtension and
            * the Java stack must contain the correct parameters for that
            * function.
            */

            {
                int mp, methods;

               /*
                * If this was for thread preemption then test to see if the interpreter
                * is executing system code (where preemption is always disabled).
                * This test is simply to see if the executing method is in suite 0
                * Also if there is data in the native parameter buffer then don't
                * switch now.
                */
                if (fn == java_lang_VMExtension_yield && (((Class_getType(cp) & 0xFF00) == 0) || noYield)) {
                    bc = TIMEQUANTA;
                    continue;
                }

//IFC//#ifdef STATS
                incExtensionCount();
                extension[fn]++;
//IFC//#endif
                pushFrame();
                cp = getJavaLangVMExtension();                                       /* Setup the class */
                methods = Class_getStaticMethods(cp);                           /* Get the table of static methods */
                assumeArrayIndexInBounds(methods, fn);                          /* Check... */
                mp = getWord(methods, fn);                                      /* Get the method */
                setupFrame(mp);

               /*
                * If this was for thread preemption then reset the counter
                */
                if (fn == java_lang_VMExtension_yield) {
                    incYieldCount();
                    bc = TIMEQUANTA;
                }

               /*
                * Set the fn variable back to the default value
                */
                fn = java_lang_VMExtension_yield;
            }

        } /* end of interpreter loop */

    }


    /*---------------------------------------------------------------------------*\
     *                                  Entrypoint                               *
    \*---------------------------------------------------------------------------*/

    public void run() {
        boolean res = true;
        int chunk;
        int counter = 0;

//IFC//#ifdef ASSUME
        arrayCheckingEnabled = true;
//IFC//#endif /* ASSUME */

        /* Create the class state table if necessary */
        if (getClassStateTable() == 0) {
            int cst = newArray(getClassFromCNO(CNO.OBJECT_ARRAY), CLS_STATE_TABLE_SIZE);
            setClassStateTable(cst);
        }

        /* Get the current chunk else create one */
        chunk = getCurrentStackChunk();
        if (chunk == 0) {
            chunk = createExecutionContext(java_lang_VMExtension_vmstart, 0);
            if (chunk == 0) {
                fatalVMError("Not enough memory to start VM");
            }
        }

       /*
        * Start the profiler if requested
        */
        startProfiler();

       /*
        * Run the interpreter until the garbage needs collecting.
        * Then save the current chunk and call the collector and
        * pass the result of the collection back to the interpreter.
        */
        for (;;) {
            chunk = interpret(chunk, res);
            setCurrentStackChunk(chunk);
            res   = gc();

            if (getCheckPointBase() != null) {
//IFC//         char   imageFileName[200];
/*IFJ*/         String imageFileName;
//IFC//         sprintf(imageFileName, "%s%d.image", getCheckPointBase(), counter);
/*IFJ*/         imageFileName = getCheckPointBase() + counter + ".image";
                ObjectMemory_writeImage(imageFileName);
                counter++;
            }

            chunk = getCurrentStackChunk();
        }
    }

/*IFJ*/}
