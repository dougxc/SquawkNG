//J2C:consts.c **DO NOT DELETE THIS LINE**
/*
 * Copyright 1994-2002 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 */

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/public class SquawkConstants {

    public final static int

    /* Tracing opcodes */

    TRACE_SETTHRESHOLD          =  1,
    TRACE_GETTHRESHOLD          =  2,
    TRACE_METTHRESHOLD          =  3,
    TRACE_GETTRACEINSTRUCTIONS  =  4,
    TRACE_GETTRACEMETHODS       =  5,
    TRACE_GETTRACEALLOCATION    =  6,
    TRACE_GETTRACEGC            =  7,
    TRACE_GETTRACEGCVERBOSE     =  8,
    TRACE_SETTRACEINSTRUCTIONS  =  9,
    TRACE_SETTRACEMETHODS       = 10,
    TRACE_SETTRACEALLOCATION    = 11,
    TRACE_SETTRACEGC            = 12,
    TRACE_SETTRACEGCVERBOSE     = 13,

    /* Section identifiers in the symbols string of a class */
    MTYPE_INSTANCEFIELDS        = 0,
    MTYPE_STATICFIELDS          = 1,
    MTYPE_VIRTUALMETHODS        = 2,
    MTYPE_STATICMETHODS         = 3,

    MTYPE_FIRST                 = MTYPE_INSTANCEFIELDS,
    MTYPE_LAST                  = MTYPE_STATICMETHODS,

    /* Class, field and method access flags */

    ACC_PUBLIC                  = 0x00000001,
    ACC_PRIVATE                 = 0x00000002,
    ACC_PROTECTED               = 0x00000004,
    ACC_STATIC                  = 0x00000008,
    ACC_FINAL                   = 0x00000010,
    ACC_SYNCHRONIZED            = 0x00000020,
    ACC_SUPER                   = 0x00000020,
    ACC_VOLATILE                = 0x00000040,
    ACC_TRANSIENT               = 0x00000080,
    ACC_NATIVE                  = 0x00000100,
    ACC_INTERFACE               = 0x00000200,
    ACC_ABSTRACT                = 0x00000400,
    ACC_PROXY                   = 0x00000800,
    ACC_SYMBOLIC                = 0x00001000,
    ACC_ATTRIBUTES              = 0x00001000,
    ACC_INIT                    = 0x00002000,

    ACC_DEFAULTINIT             = 0x00010000,
    ACC_CLINIT                  = 0x00020000,
    ACC_MAIN                    = 0x00040000,

    /* Non-spec class flags */

    ACC_MUSTCLINIT              = 0x00004000,
    ACC_HASFINALIZER            = 0x00008000,

    RECOGNIZED_SUITE_FILE_SUITE_ACCESS_FLAG_MASK =
        ACC_FINAL,

    RECOGNIZED_SUITE_FILE_CLASS_ACCESS_FLAG_MASK =
        ACC_PUBLIC     |
        ACC_FINAL      |
        ACC_INTERFACE  |
        ACC_ABSTRACT   |
        ACC_PROXY,

    RECOGNIZED_SUITE_FILE_METHOD_ACCESS_FLAG_MASK =
        ACC_PUBLIC     |
        ACC_PRIVATE    |
        ACC_PROTECTED  |
        ACC_FINAL      |
        ACC_ABSTRACT   |
        ACC_NATIVE     |
        ACC_INIT       |
        ACC_SYMBOLIC,

    RECOGNIZED_SUITE_FILE_FIELD_ACCESS_FLAG_MASK =
        ACC_PUBLIC     |
        ACC_PRIVATE    |
        ACC_PROTECTED  |
        ACC_FINAL      |
        ACC_VOLATILE   |
        ACC_TRANSIENT  |
        ACC_SYMBOLIC,

    RECOGNIZED_SUITE_FILE_METHOD_IMPL_ACCESS_FLAG_MASK =
        ACC_STATIC     |
        ACC_NATIVE     |
        ACC_ATTRIBUTES |
        ACC_ABSTRACT,


    /** VMAccessedAttribute flags */
    VMACC_EEPROM    = 0x0001, /* Class: The VM only accesses instances of this class that are in EEPROM. */
    VMACC_UNSIGNED  = 0x0002, /* Field: The field is accessed by the Squawk VM as an unsigned value. */
    VMACC_WBOPAQUE  = 0x0004, /* Field: The write barrier (if any) is not updated when this field is updated. */
    VMACC_READ      = 0x0008, /* Field: The VM requires read access to this field. */
    VMACC_WRITE     = 0x0010, /* Field: The VM requires write access to this field. */
    VMACC_CALL      = 0x0020, /* Method: The VM invokes this static method. */

    /* Tags in constant object pool */

    CONSTANT_String             = 1,
    CONSTANT_Int_array          = 2,
    CONSTANT_Short_array        = 3,
    CONSTANT_Char_array         = 4,
    CONSTANT_Byte_array         = 5,
    CONSTANT_Float_array        = 6,
    CONSTANT_Long_array         = 7,
    CONSTANT_Double_array       = 8,

    /*
     * The layout of a stack chunk is as follows:
     *
     * stack_chunk {
     *     word self     // only in Cheney collector build (required for updating internal oops)
     *     word next     // next chunk
     *     word prev     // previous chunk
     *     word size     // size of chunk (in words)
     *     word lastLp   // last frame in chunk (if 0, chunk can be collected)
     *     frame frames[]
     * }
     *
     * The variables/registers used to model the execution are:
     *
     *     sp: top of stack pointer
     *     lp: frame pointer
     *     ip: instruction pointer
     *     cp: class pointer
     *     mp: method pointer
     *     sl: pointer to last slot of stack chunk
     *     ep: pointer to highest slot required by a method - may be past sl
     *
     * A copy of the first 5 of these variables is maintained for each method
     * currently being executed. The prefix 'p_' is used when the value for
     * such a variable is in the context of the previous (i.e. caller's)
     * frame and 'c_' is used when the context is the current frame.
     *
     * A frame pointer actually points to the word just after this struct so
     * the items are located via negative offsets.
     *
     * frame {
     *     word c_mp        // offset = -5
     *     word c_ip        // offset = -4
     *     word p_lp        // offset = -3
     *     word c_cp        // offset = -2
     *     word stkOffset   // offset = -1
     * }
     *
     *
     */
/*if[CHENEY.COLLECTOR]*/
    STACK_self                  = 0,  /* oop */
/*end[CHENEY.COLLECTOR]*/
/*if[LISP2.COLLECTOR]*/
    STACK_list                  = 0,  /* oop */
/*end[LISP2.COLLECTOR]*/
    STACK_next                  = 1,  /* oop */
    STACK_prev                  = 2,  /* oop */
    STACK_size                  = 3,
    STACK_lastLp                = 4,  /* oop */
    STACK_HEADER_SIZE           = 5,

    FRAME_currentMp             = -5,  /* oop */
    FRAME_currentIp             = -4,  /* real address in method */
    FRAME_previousLp            = -3,  /* real address in stack chunk */
    FRAME_currentCp             = -2,  /* oop */
    FRAME_stackOffset           = -1,  /* number (the offset from lp to the bottom of the operand stack) */
    FRAME_HEADER_SIZE           = 5,   /* Size of fixed portion of a frame */

    /*
     * Method structure
     *
     * struct {
     *     byte headerSize
     *     byte suiteNumber
     *     byte classNumber
     *     byte numberOfParms              // numberOfParms <= numberOfLocals
     *     byte numberOfLocals
     *     byte oopMap[oopMapSize + pad]   // oopMapSize = (numberOfLocals + 7) / 8
     *     {                               // pad = (5 + oopMapSize) & 1
     *         half startIp
     *         half endIp
     *         half handlerIp
     *         half catchType
     *     } exceptionHandlers[exceptionHandlersLength]
     *     byte bytecode[]
     * }
     */
    MTH_headerSize              = 0,
    MTH_suiteNumber             = 1,
    MTH_classNumber             = 2,
    MTH_numberOfParms           = 3,
    MTH_numberOfLocals          = 4,
    MTH_oopMap                  = 5,

    /* Class state */

    CLS_STATE_class             = 0,  /* oop */
    CLS_STATE_next              = 1,  /* oop */
    CLS_STATE_offsetToFields    = 2,
    CLS_STATE_TABLE_SIZE        = /*VAL*/16/*VM.CLASSSTATE_HASHTABLE_SIZE*/, /* Must be a power of 2. */

    /* Internal exception codes */

    EXNO_None                   =  0,
    EXNO_IOException            = -2,
    EXNO_EOFException           = -3,
    EXNO_NoConnection           = -4,

    /* Heap image */

    /**
     * Memory is modeled as 3 segments in the one unified address space. All
     * segments have the same structure from the point of view of the garbage
     * collector. This makes it straight forward to GC any segment of memory
     * with the one collector implementation.
     */

    /* Master memory record (all values are word offsets) */

    MMR_MAGICNUMBER             = 0x03021957,
    MMR_MAGICNUMBER_REVERSED    = 0x57190203,
    MMR_VERSION                 = 100,

    MMR_magicNumber             = 0,
    MMR_version                 = 1,
    MMR_romStart                = 2,
    MMR_romSize                 = 3,
    MMR_eepromStart             = 4,
    MMR_eepromSize              = 5,
    MMR_ramStart                = 6,
    MMR_ramSize                 = 7,
    MMR_SIZE                    = 8,


    /* GC information record (all values are word offsets) */

    GCI_objectMemoryStart             = 0,
    GCI_objectMemorySize              = 1,
    GCI_currentObjectPartitionStart   = 2,
    GCI_currentObjectPartitionFree    = 3,
    GCI_currentObjectPartitionEnd     = 4,
    GCI_failedAllocationSize          = 5,
    GCI_res1                          = 6,
    GCI_res2                          = 7,
    GCI_SIZE                          = 8,

    /* GC Roots (all values are word offsets) */

    ROOT_classStateTable        = 0,    /* Only in RAM */
    ROOT_freeList               = 0,    /* Only in EEPROM */

    ROOT_currentStackChunk      = 1,    /* Only in RAM */
    ROOT_javaLangVMExtension    = 1,    /* Only in ROM */

    ROOT_stackChunkList         = 2,    /* Only in RAM */
    ROOT_suiteTable             = 2,    /* Only in ROM and EEPROM */

    ROOT_persistentMemoryTable  = 3,    /* Only in EEPROM */
/*if[DEBUG.METHODDEBUGTABLE]*/
    ROOT_methodDebugTable       = 3,    /* Only in ROM and RAM */
/*end[DEBUG.METHODDEBUGTABLE]*/

    ROOT_outOfMemoryObject      = 4,    /* Only in RAM */

    ROOT_fastLockStack          = 5,    /* Only in RAM */

    ROOT_associationHashtable   = 6,    /* Only in RAM */

    ROOT_finalizationQueue      = 7,    /* Only in RAM */

    ROOT_SIZE                   = 8,





    /* Memory segment record (all values are word offsets) */

    MSR_gci                     = 0,
    MSR_roots                   = GCI_SIZE,
    MSR_SIZE                    = MSR_roots + ROOT_SIZE,  /* Size of a memory segment record */


    /* ObjectAssociation */

    ASSN_hashtableSize          = /*VAL*/16/*VM.ASSOCIATION_HASHTABLE_SIZE*/,

    /* Vtable indexes */

    SLOT_FIRST                  = 0,
    SLOT_UNDEFINED              = -1,

    DUMMY         = 999;


/*IFJ*/}
