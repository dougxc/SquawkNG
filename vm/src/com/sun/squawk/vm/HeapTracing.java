//J2C:htrace.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/abstract public class HeapTracing extends SegmentRelocation {


//IFC//#ifdef TRACING
    /**
     * Trace (part of) a string.
     *
     * @param str
     * @param offset
     * @param length
     */
    void trace_string(int str, int offset, int length) {
        int i;
        for (i = 0; i < length; i++) {
            traceChar(String_at(str, i+offset));
        }
    }

    /*
     * Trace the name of a class.
     */
    void trace_className(int klass) {
        int symbols = Class_getSymbols(klass);
        int classNameLength;
/*if[OLDSYMBOLS]*/
        assume(symbols != 0);
        symbols = getWord(symbols, 0);
/*end[OLDSYMBOLS]*/
        classNameLength = String_at(symbols, 0);
        trace_string(symbols, 1, classNameLength);
    }


    private int symbols;
    private int pos;
    private int count;

    /**
     * Read an encoded integer from the current position
     */
    private int getInt() {
        int lo, mid, hi, last;
        lo = String_at(symbols, pos++);
        if (lo < 128) {
            return lo;
        }
        lo &= 0x7f;
        mid = String_at(symbols, pos++);
        if (mid < 128) {
            return ((mid << 7) + lo);
        }
        mid &= 0x7f;
        hi = String_at(symbols, pos++);
        if (hi < 128) {
            return ((hi << 14) + (mid << 7) + lo);
        }
        hi &= 0x7f;
        last = String_at(symbols, pos++);
        if (last < 128) {
            return ((last << 21) + (hi << 14) + (mid << 7) + lo);
        }
        fatalVMError("getInt() ");
        return 0;
    }

    /**
     * Read an encoded type
     */
    int getType() {
        return getInt();
    }

    /*
     * trace_accessFlags
     */
    void trace_accessFlags(int flag, boolean c) {
        if ((flag & ACC_PUBLIC) != 0)             {  trace("|ACC_PUBLIC");           }
        if ((flag & ACC_PRIVATE) != 0)            {  trace("|ACC_PRIVATE");          }
        if ((flag & ACC_PROTECTED) != 0)          {  trace("|ACC_PROTECTED");        }
        if ((flag & ACC_STATIC) != 0)             {  trace("|ACC_STATIC");           }
        if ((flag & ACC_FINAL) != 0)              {  trace("|ACC_FINAL");            }
        if ((flag & ACC_SYNCHRONIZED) != 0 && !c) {  trace("|ACC_SYNCHRONIZED");     }
        if ((flag & ACC_SUPER) != 0 && c)         {  trace("|ACC_SUPER");            }
        if ((flag & ACC_VOLATILE) != 0)           {  trace("|ACC_VOLATILE");         }
        if ((flag & ACC_TRANSIENT) != 0)          {  trace("|ACC_TRANSIENT");        }
        if ((flag & ACC_NATIVE) != 0)             {  trace("|ACC_NATIVE");           }
        if ((flag & ACC_INTERFACE) != 0)          {  trace("|ACC_INTERFACE");        }
        if ((flag & ACC_ABSTRACT) != 0)           {  trace("|ACC_ABSTRACT");         }
        if ((flag & ACC_PROXY) != 0)              {  trace("|ACC_PROXY");            }
        if ((flag & ACC_SYMBOLIC) != 0)           {  trace("|ACC_SYMBOLIC");         }
        if ((flag & ACC_INIT) != 0)               {  trace("|ACC_INIT");             }
        if ((flag & ACC_MUSTCLINIT) != 0)         {  trace("|ACC_MUSTCLINIT");       }
        if ((flag & ACC_DEFAULTINIT) != 0)        {  trace("|ACC_DEFAULTINIT");      }
        if ((flag & ACC_CLINIT) != 0)             {  trace("|ACC_CLINIT");           }
        if ((flag & ACC_MAIN) != 0)               {  trace("|ACC_MAIN");             }
    }

    /*
     * trace_members
     */
    void trace_members(int symbols, boolean isMethod) {
        while (pos < count) {
            int memberLength = getInt();


            if (memberLength == MTYPE_STATICFIELDS   ||
                memberLength == MTYPE_INSTANCEFIELDS ||
                memberLength == MTYPE_STATICMETHODS  ||
                memberLength == MTYPE_VIRTUALMETHODS) {
            } else {
                int type   = 0;
                int end    = pos + memberLength;
                int access = getInt();
                int offset = getInt();
                int nmid   = srl(offset, 16);

                trace("    access=");
                trace_accessFlags(access, false);

                trace(" offset=");
                traceInt(offset&0xFFFF);

                if (nmid != 0) {
                    trace(" nmid=");
                    traceInt(nmid);
                }

                trace(" name=\"");
                while (true) {
                    int ch = String_at(symbols, pos++);
                    if (ch == '-') {
                        break;
                    }
                    traceChar(ch);
                }
                trace("\"");

                type = getType();
                trace(" type=");
                trace_className(getClassFromCNO(type));

                if (isMethod) {
                    trace(" parms={");
                    while (pos < end) {
                        type = getType();
                        trace_className(getClassFromCNO(type));
                        if (pos < end) {
                            trace(", ");
                        }
                    }
                    trace("}");

                }
                traceln("");
            }
        }
    }




    /*
     * trace_classSymbols
     */
    void trace_classSymbols(int klass) {
        int classNameLength;
        boolean isMethod = false;

        symbols         = Class_getSymbols(klass);
/*if[OLDSYMBOLS]*/
        assume(symbols != 0);
        symbols = getWord(symbols, 0);
/*end[OLDSYMBOLS]*/
        count           = String_length(symbols);
        pos             = 0;
        classNameLength = getInt();
        trace_string(symbols, pos, classNameLength);
        traceln(":");
        pos += classNameLength;

        trace("  access="); trace_accessFlags(Class_getAccess(klass), true); traceln("");

        while (pos < count) {
            int mtype = String_at(symbols, pos);
            switch (mtype) {
                case MTYPE_INSTANCEFIELDS: traceln("  instance fields:"); isMethod = false; break;
                case MTYPE_STATICFIELDS:   traceln("  static fields:");   isMethod = false; break;
                case MTYPE_VIRTUALMETHODS: traceln("  virtual fields:");  isMethod = true;  break;
                case MTYPE_STATICMETHODS:  traceln("  static methods:");  isMethod = true;  break;
            }
            trace_members(symbols, isMethod);
        }
    }

    /*
     * getMethodIdAddress
     */
    int getMethodIdAddress(int mp) {
        int b0 = getUnsignedByte(mp, 0);
        int p  = mp + b0; // skip header
        if (fetchUnsignedByte(p) >= OPC.EXTEND_0 && fetchUnsignedByte(p) <= OPC.EXTEND_15)  p++;
        if (fetchUnsignedByte(p) == OPC.EXTEND)                                             p += 2;
        if (fetchUnsignedByte(p) == OPC.NEWOBJECT)                                          p++;
        if (fetchUnsignedByte(p) == OPC.CLASS_CLINIT)                                       p++;
        if (fetchUnsignedByte(p) == OPC.METHODID) {
            return p+1;
        } else {
/*if[DEBUG.METHODDEBUGTABLE]*/
            fatalVMError("No debug info for method: OPC.METHODID bytecode not found");
/*end[DEBUG.METHODDEBUGTABLE]*/
            return -1;
        }
    }

    /*
     * trace_methodID
     */
    void trace_methodID(int mp) {
        int methodIdAddr = getMethodIdAddress(mp);
        if (methodIdAddr != -1) {
            traceInt(fetchInt(methodIdAddr));
        }
    }


    /*
     * traceInstance
     */
    void traceInstance(int oop) {
        int klass;

///*IFJ*/System.err.println(" traceInstance oop="+oop);

        Object_getClass(oop);
        oop = Object_getPossiblyForwardedObject(oop);
        klass = Object_getClass(oop);

        traceHex(oop);
        trace(": ");
        trace_className(klass);
        trace("  [ class=");
        traceHex(klass);
        if (Class_isArrayClass(klass)) {
            int count = Object_getArrayElementCount(oop);
            trace(" count=");
            traceInt(count);
            trace(" { ");
            switch (Class_getType(klass)) {
/*if[NEWSTRING]*/
                case CNO.STRING_OF_BYTES:
                case CNO.STRING_OF_SYMBOLS:{
                    int i;
                    trace("\"");
                    for (i = 0; i != count; i++) {
                        traceChar(getUnsignedByte(oop, i));
                    }
                    trace("\" ");
                    break;
                }
/*end[NEWSTRING]*/
                case CNO.BYTE_ARRAY:
                case CNO.BOOLEAN_ARRAY: {
                    int i;
                    for (i = 0; i != count; i++) {
                        traceHex(getUnsignedByte(oop, i));
                        trace(" ");
                    }
                    break;
                }
/*if[NEWSTRING]*/
                case CNO.STRING:
/*end[NEWSTRING]*/
                case CNO.CHAR_ARRAY: {
                    int i;
                    trace("\"");
                    for (i = 0; i != count; i++) {
                        traceChar(getUnsignedHalf(oop, i));
                    }
                    trace("\" ");
                    break;
                }
                case CNO.SHORT_ARRAY: {
                    int i;
                    for (i = 0; i != count; i++) {
                        traceHex(getUnsignedHalf(oop, i));
                        trace(" ");
                    }
                    break;
                }
                case CNO.INT_ARRAY:
/*if[FLOATS]*/
                case CNO.FLOAT_ARRAY:
/*end[FLOATS]*/
                {
                    int i;
                    for (i = 0; i != count; i++) {
                        traceHex(getWord(oop, i));
                        trace(" ");
                    }
                    break;
                }
                case CNO.LONG_ARRAY:
/*if[FLOATS]*/
                case CNO.DOUBLE_ARRAY:
/*end[FLOATS]*/
                {
                    int i;
                    for (i = 0; i != count; i++) {
                        long l = getLong(oop, i);
                        traceHex((int)(l >> 32));
                        traceHex((int)l);
                        trace(" ");
                    }
                    break;
                }

                case CNO.GLOBAL_ARRAY: {
                    int kls = ClassState_getClass(oop);
                    trace(" klass=");
                    trace_className(kls);
                    trace(" ");
                    /* Drop thru */
                }

                default: {
                    /* oop array */
                    int i;
                    for (i = 0; i != count; i++) {
                        traceHex(getWord(oop, i));
                        trace(" ");
                    }
                    break;
                }
            }
            traceln("} ]");
        } else {
            int map    = Class_getOopMap(klass);
            int length = Class_getInstanceFieldsLength(klass);
            int offset = 0;
            int i;
            trace(" ]");
            for (i = 0 ; i < length ; i++) {
                int mapbyte = getUnsignedByte(map, (i/8));
                int mask = 1 << (i % 8);
                traceln("");
                trace("    ");
                traceInt(i);
                trace(": ");
                traceHex(getWord(oop, i));
                if ((mapbyte & mask) != 0) {
                    trace(" (oop)");
                } else {

                }
            }
            traceln("");

            if (Class_getType(klass) == CNO.CLASS) {
                traceln("<symbolic>");
                trace_classSymbols(oop);
                traceln("</symbolic>");
            }
        }
    }

    private void traceQuantity(int size) {
        traceInt(size);
        trace("  (");
        traceInt(size/1024);
        trace("K)");
    }

    /**
     * Trace the contents of the heap for a given memory segment.
     * @param segment
     * @param verbose
     */
    public void traceHeap(int segment, boolean verbose) {
        int oop;
        String name = null;
        int start, freePtr, end;

             if (segment == ROM)     { name = "ROM";      }
        else if (segment == EEPROM)  { name = "EEPROM";   }
        else if (segment == RAM)     { name = "RAM";      }
        else shouldNotReachHere();

        start   = getObjectMemoryStart(segment);
        freePtr = getObjectPartitionFree(segment);
        end     = getObjectPartitionEnd(segment);


        trace("---- "); trace(name); trace(" start -----");                        traceln("");
        trace("segment object space allocated  "); traceQuantity(freePtr - start); traceln("");
        trace("segment object space free       "); traceQuantity(end - freePtr);   traceln("");
        trace("segment object space start      "); traceHex(start);                traceln("");
        trace("segment object space free ptr   "); traceHex(freePtr);              traceln("");
        trace("segment object space end        "); traceHex(end);                  traceln("");
        traceln("");

        if (verbose) {
            /* Iterate over all the objects in the heap. */
            if (start != freePtr) {
                for (oop = Object_blockToOop(start) ; oop != 0 ; oop = Object_nextObject(oop, start, freePtr)) {
                    traceInstance(oop);
                }
            }
        }
        trace("---- "); trace(name); trace(" end -----"); traceln("");
    }


/*if[LISP2.COLLECTOR]*/

    /**
     * traceWholeHeap
     */
    public void traceWholeHeap(int segment) {
        int save = getObjectPartitionStart(segment);
        setObjectPartitionStart(segment, getObjectMemoryStart(segment));
        trace("getClassStateTable()       "); traceHex(getClassStateTable());   traceln("");
        traceHeap(segment, true);
        setObjectPartitionStart(segment, save);
    }

/*end[LISP2.COLLECTOR]*/



/*IFJ*/ int[] summary = new int[65536];
//IFC// int   summary[65536];
/*IFJ*/ int[] sizeSummary = new int[65536];
//IFC// int   sizeSummary[65536];

    /**
     * Trace the contents of the heap for a given memory segment.
     * @param segment
     */
    public void traceHeapSummary(int segment) {
        int oop, i;
        int targetSpace        = getObjectPartitionStart(segment);
        int targetSpaceFreePtr = getObjectPartitionFree(segment);
        int targetSpaceEnd     = getObjectPartitionEnd(segment);

/*if[LISP2.COLLECTOR]*/
        if (segment == RAM) {
            targetSpace        = getObjectMemoryStart(segment);
        }
/*end[LISP2.COLLECTOR]*/

        if (targetSpace != targetSpaceFreePtr) {
            int countTotal = 0;
            int sizeTotal = 0;

            trace("---- ");
            if (segment == ROM)    trace("ROM");
            if (segment == EEPROM) trace("EEPROM");
            if (segment == RAM)    trace("RAM");
            trace(" Summary after ");
            traceLong(getCollectionCount(), false);
            traceln(" collections -----");

            for (oop = Object_blockToOop(targetSpace) ; oop != 0 ; oop = Object_nextObject(oop, targetSpace, targetSpaceFreePtr)) {
                int klass = Object_getClass(oop);
                int cno = Class_getType(klass);
                summary[cno]++;
                sizeSummary[cno] += Object_getObjectLength(oop) + Object_getHeaderSize(oop);
            }

            for (i = 0 ; i < 65536 ; i++) {
                if (summary[i] != 0) {
                    int sum = summary[i];
                    countTotal += sum;
                    traceIntPad(sum, 10);
                    sum = sizeSummary[i];
                    sizeTotal += sum;
                    traceIntPad(sum, 15);
                    trace_className(getClassFromCNO(i));
                    traceln("");
                    summary[i] = 0;
                    sizeSummary[i] = 0;
                }
            }

            traceIntPad(countTotal, 10);
            traceIntPad(sizeTotal, 15);
            traceln("<totals>");
        }
    }



    public void traceMMR() {
        traceln("---- Master Memory Record start -----");
        traceln("");
        trace("magic number:     "); traceHex(MMR[MMR_magicNumber]);      traceln("");
        trace("version:          "); traceHex(MMR[MMR_version]);          traceln("");
        trace("ROM:              "); traceHex(MMR[MMR_romStart]);         traceln("");
        trace("ROM size:         "); traceQuantity(MMR[MMR_romSize]);     traceln("");
        trace("EEPROM:           "); traceHex(MMR[MMR_eepromStart]);      traceln("");
        trace("EEPROM size:      "); traceQuantity(MMR[MMR_eepromSize]);  traceln("");
        trace("RAM:              "); traceHex(MMR[MMR_ramStart]);         traceln("");
        trace("RAM size:         "); traceQuantity(MMR[MMR_ramSize]);     traceln("");
        traceln("");
        traceln("---- Master Memory Record end -----");
    }

//IFC//#else /* TRACING */
//IFC//#define trace_chars(chars, offset, length)               /**/
//IFC//#define trace_className(x)                               /**/
//IFC//#define trace_accessFlags(x, y)                          /**/
//IFC//#define trace_members(symbols, pos, length, isMethod)    0
//IFC//#define trace_classSymbols(x)                            /**/
//IFC//#define trace_methodID(x)                                /**/
//IFC//#define traceInstance(oop)                               /**/
//IFC//#define traceHeapSummary(seg)                            /**/
//IFC//#endif /* TRACING */




/*IFJ*/}
