//J2C:eepromgc.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/import java.io.*;
/*IFJ*/import java.util.*;

/**
 * This class encapsulates the subsystem of the Squawk VM that deals with
 * allocating and garbage collecting a persistent memory such as EEPROM.
 *
 * Persistent memory uses a mark-sweep non-compacting, non-moving garbage
 * collector. This has the advantage of mimimising the writes to
 * persistent memory at the trade off of segmentation.
 *
 * Allocation is done by maintaining a list of free blocks. This list is
 * regenerated after each collection and updated duringe each allocation.
 *
 * The pointer to the head of the free list is nulled during garbage collection
 * so that a system that is subject to abritary power 'tears' (such as a
 * JavaCard) can determine whether or not the tear occurred during a GC of the
 * persistent memory and therefore restart the GC if necessary.
 *
 * The pointer to the head of the free list has the value -1 if there are no
 * free blocks in persistent memory.
 *
 */
/*IFJ*/abstract class PersistentCollector extends MarkStack {

/*---------------------------------------------------------------------------*\
 *                        Initialisation                                     *
\*---------------------------------------------------------------------------*/

    void PersistentCollector_init(int start, int end) {
        int freeList = start;
        int size = end - start;
        FreeBlock_setHeader(freeList, size, 0);
        setEepromFreeList(freeList);
        assume(FreeBlock_getSize(getEepromFreeList()) == size);
    }

/*---------------------------------------------------------------------------*\
 *                         Constants                                         *
\*---------------------------------------------------------------------------*/

    private static final int
        FTN_MARK_OBJECTS                                = 1,
        FTN_UPDATE_POINTERS                             = 2,
        FTN_RESET_FORWARDING                            = 3,
        FTN_UPDATE_ASSOCIATIONS_AND_CLEAR_WRITEBARRIER  = 4,
        FTN_SCAN_RAM_OBJECT                             = 5;

/*---------------------------------------------------------------------------*\
 *                         Stats                                             *
\*---------------------------------------------------------------------------*/

//IFC//#ifdef STATS
        private long persistentGCCount;
        private long persistentAllocCount;
        private long persistentMigrateCount;
        private long persistentCopyCount;

        private void incPersistentGCs()        { persistentGCCount++;        }
        private void incPersistentAllocs()     { persistentAllocCount++;     }
        private void incPersistentMigrations() { persistentMigrateCount++;   }
        private void incPersistentCopies()     { persistentCopyCount++;      }
//IFC//#else
//IFC//#define incPersistentGCs()         /**/
//IFC//#define incPersistentAllocs()      /**/
//IFC//#define incPersistentMigrations()  /**/
//IFC//#define incPersistentCopies()      /**/
//IFC//#endif /* STATS */

    void printEepromGCStats() {
//IFC//#ifdef STATS
        printMsgLong("  Collections              ", persistentGCCount);
        printMsgLong("  Allocations              ", persistentAllocCount);
        printMsgLong("  Migrations to EEPROM     ", persistentMigrateCount);
        printMsgLong("  Copies to EEPROM         ", persistentCopyCount);
//IFC//#endif
    }

/*---------------------------------------------------------------------------*\
 *                         Allocation                                        *
\*---------------------------------------------------------------------------*/

    /**
     * Allocate a raw memory chunk in persistent memory and zero all but the
     * header word(s).
     * @param exactByteSize The exact size (in bytes) of the chunk required.
     * This size will be rounded up to a word size.
     * @param zero If true, then zero the allocated chunk of memory.
     * @return the pointer to the allocated chunk or 0 if the allocation failed.
     */
    int newPersistentChunk(int exactByteSize, boolean zero) {
        int size = roundup4(exactByteSize);   /* Round up to a full word boundry */

        int chunk = getEepromFreeList();
        int prev  = 0;

        if (chunk == -1) {
            return 0;
        }

        while (true) {
            int header    = getWord(chunk, 0);
            int chunkSize = FreeBlock_getSize(chunk);
            int offset    = FreeBlock_getNextOffset(chunk);

            assume(isFreeBlock(chunk));

            if (size <= chunkSize) {
                /*
                 * Update the free list pointers first before zeroing the
                 * new chunk. This ensures the free list will be consistent
                 * in the case where the VM is unexpectedly stopped (e.g.
                 * loses its power) at this point.
                 */
                 int newChunk;
                 if (chunkSize == size) {
                     if (offset == 0) {
                         newChunk = -1;
                     } else {
                         newChunk = chunk + offset;
                     }
                 } else {
                     newChunk = chunk + size;
                     if (offset != 0) {
                         offset = offset - size;
                     }
                     FreeBlock_setHeader(newChunk, chunkSize - size, offset);
                 }

                 if (prev == 0) {
                     setEepromFreeList(newChunk);
                 } else {
                     FreeBlock_setHeader(prev, FreeBlock_getSize(prev), newChunk - prev);
                 }

                 /* Zero the allocated chunk if necessary. */
                 if (zero) {
                     int wordSize = size / bytesPerWord();
                     while (wordSize != 0) {
                         setPersistentWord(chunk, --wordSize, 0);
                     }
                 }

                 if (getTraceEepromAllocation()) {
                     trace("*ALLOC* newPersistentChunk(");
                     traceHex(chunk);
                     trace(", size=");
                     traceInt(size);
                     trace(")");
                     traceInstructionCount(true);
                 }
                 incPersistentAllocs();
                 return chunk;
            }

            /* If this was the last free block in the list, then return null
               indicating that the requested chunk could not be allocated. */
            if (offset == 0) {
                return 0;
            }

            prev = chunk;
            chunk += offset;
        }
    }

/*---------------------------------------------------------------------------*\
 *                         Stats                                             *
\*---------------------------------------------------------------------------*/

    int freePersistentMemory() {
        int chunk = getEepromFreeList();
        int total = 0;
        if (chunk == -1) {
            return 0;
        }
        while (chunk != 0) {
            int offset = FreeBlock_getNextOffset(chunk);
            assume(isFreeBlock(chunk));
            total += FreeBlock_getSize(chunk);
            if (offset == 0) {
                chunk = 0;
            } else {
                chunk += offset;
            }
        }
        return total;
    }

    int totalPersistentMemory() {
        return getObjectPartitionEnd(EEPROM) - getObjectPartitionStart(EEPROM);
    }

/*---------------------------------------------------------------------------*\
 *                   Garbage collection - marking                            *
\*---------------------------------------------------------------------------*/

//IFC//boolean traversePersistentObject(int oop, int klass, int ftn);
//IFC//boolean traversePersistentFunction(int addr, int wordOffset, int ftn);

        /**
         * Mark the objects pointed to by a specified object.
         * @param oop The object whose references are to be followed and
         * have the corresponding objects be marked.
         */
/*MAC*/ void markPersistentObjectReferences(int $oop) {
            assume(testOopBit($oop));
            if (getTraceEepromGCVerbose()) {
                trace("remarkPersistentObject ");
                traceInt($oop);
                trace(" klass ");
                trace_className(Object_getClass($oop));
                trace("   (recurseLevel=");
                traceInt(recurseLevel());
                trace(", markStackIndex=");
                traceInt(markStackIndex());
                traceln(")");
            }
            if (recurseLevel() == 0) {
                pushOnMarkStack($oop);
            } else {
                recurseLevelDec();
                traversePersistentObject($oop, Object_getClass($oop), FTN_MARK_OBJECTS);
                while (($oop = popOffMarkStack()) != 0) {
                    traversePersistentObject($oop, Object_getClass($oop), FTN_MARK_OBJECTS);
                }
                recurseLevelInc();
            }
        }


        /**
         * Mark an object if it is in persistent memory. If the object was not previously
         * marked, then mark all the pointers within the object.
         * @param oop The object to be marked.
         */
/*MAC*/ void markPersistentObject(int $oop, int $ftn) {
            if (inEeprom($oop) && !testAndSetOopBit($oop)) {
                markPersistentObjectReferences($oop);
            }
        }

        /**
         * Keeping trying to mark live persistent objects while the
         * marking overflow stack has elements.
         * @param start
         * @param end
         */
/*MAC*/ void markPersistentOverflowCheck(int $start, int $end) {
            int oop;
            while (markStackOverflow()) {
                if (getTraceEepromGCVerbose()) {
                    trace("Mark persistent overflow scan...");
                }

                resetMarkStackOverflow();

                startIteration($start, $end);
                while ((oop = getNextIteration()) != 0) {
                    markPersistentObjectReferences(oop);
                    iterate();
                }
                endIteration();
            }
        }

    /**
     * Mark the persistent root objects.
     */
    void markPersistentRoots() {
        int i;
        int oop;
        int start;
        int end   = getObjectPartitionFree(RAM);
/*if[LISP2.COLLECTOR]*/
        start = getObjectMemoryStart(RAM);
/*end[LISP2.COLLECTOR]*/
/*if[CHENEY.COLLECTOR]*/
        start = getObjectPartitionStart(RAM);
/*end[CHENEY.COLLECTOR]*/

        /* Sacn persistent roots */
        for (i = 0; i != ROOT_SIZE; i++) {
            if (getTraceEepromGCVerbose()) {
                trace("markPersistentRoots addr=");
                traceInt(EEPROM +(w2b(MSR_roots + i)));
                trace(" value=");
                traceInt(getWord(EEPROM, (MSR_roots + i)));
                traceln("");
            }
            traversePersistentFunction(EEPROM, MSR_roots + i, FTN_MARK_OBJECTS);
        }

        /* Scan all the objects in RAM, looking for pointers to EEPROM */
        for (oop = Object_blockToOop(start) ; oop != 0 ; oop = Object_nextObject(oop, start, end)) {
            /*
             * The oop may be an object that was recently migrated from old
             * space to persistent memory but has not had it's forwarded
             * header fixed up yet (as old space is not always compacted).
             */
            if (Object_isForwarded(oop)) {
                int klass = Object_getClass(Object_getForwardedObject(oop));
                Object_setClass(oop, klass);
            } else {
                traversePersistentObject(oop, Object_getClass(oop), FTN_SCAN_RAM_OBJECT);
            }
        }
    }

    /**
     * Mark all live persistent objects.
     */
    void markPersistent(int markStack) {
        int start = getObjectPartitionStart(EEPROM);
        int end   = getObjectPartitionEnd(EEPROM);

        if (getTraceEepromGCVerbose()) {
            trace("persistent mark: ");
            traceInt(start);
            trace(" - ");
            traceInt(end);
            traceln("");
        }

        /* Clear the bit vector */
        clearOopBitRange(start, end);

        /* Set up the mark stack */
        markStackSetup(markStack, markStack + Object_getObjectLength(markStack));

        /* The mark phase */
        markPersistentRoots();
        markPersistentOverflowCheck(start, end);
        if (getTraceEepromGCVerbose()) {
            traceBitmap();
        }

        /* Finalise and release the mark stack */
        markStackFinish();

    }

/*---------------------------------------------------------------------------*\
 *                   Garbage collection - sweeping                           *
\*---------------------------------------------------------------------------*/


/*MAC*/ private int objectAfterPersistent(int $oop) {
        return Object_blockToOop(Object_nextBlock($oop));
    }

//IFC//#ifndef PRODUCTION
/*MAC*/ private void clearFreeBlock(int $addr, int $size) {
            int i;
            for (i = 0; i != $size; i++) {
                setByte($addr, i, 0);
            }
        }
//IFC//#else
//IFC//#define clearFreeBlock(addr, size) /**/
//IFC//#endif /* PRODUCTION */

/*MAC*/ private void createFreeBlock(int $addr, int $size, int $offset) {
            clearFreeBlock($addr, $size);
            FreeBlock_setHeader($addr, $size, $offset);

            if (getTraceEepromGCVerbose()) {
                trace("sweepPersistent free block ");
                traceHex($addr);
                trace(", size ");
                traceInt(FreeBlock_getSize($addr));
                trace(", nextOffset ");
                traceInt(FreeBlock_getNextOffset($addr));
                traceln("");
            }

        }


    /**
     * Scan the EEPROM heap using the vector of mark bits to rebuild the
     * free block list.
     */
    void sweepPersistent() {
        int start = getObjectPartitionStart(EEPROM);
        int end   = getObjectPartitionEnd(EEPROM);
        int oop;
        int lastOop = 0;
        int firstFreeBlock = 0;

        int freeBlock = 0;
        int freeBlockSize = 0;

        assume(getEepromFreeList() == 0);
        startIteration(start, end);

        if (getTraceEepromGCVerbose()) {
            trace("sweepPersistent start=");
            traceHex(start);
            trace(" end=");
            traceHex(end);
            traceln("");
        }

        /* Handle first object specially */
        if ((oop = getNextIteration()) != 0) {

            if (getTraceEepromGCVerbose()) {
                trace("sweepPersistent marked object ");
                traceHex(oop);
                trace(", size ");
                traceInt(Object_getObjectLength(oop));
                traceln("");
            }

            if (Object_nextBlock(oop) != end) {

                // Handle
                int block = Object_oopToBlock(oop);
                if (block != start) {
                    firstFreeBlock = freeBlock = start;
                    freeBlockSize = block - start;
                }
                lastOop = oop;

                iterate();
                while ((oop = getNextIteration()) != 0) {

                    if (getTraceEepromGCVerbose()) {
                        trace("sweepPersistent marked object ");
                        traceHex(oop);
                        trace(", size ");
                        traceInt(Object_getObjectLength(oop));
                        traceln("");
                    }

                    if (objectAfterPersistent(lastOop) != oop) {
                        /* The space after the last object is free */
                        block = Object_nextBlock(lastOop);

                        if (freeBlock != 0) {
                            /* Finish off the previous free block */
                            createFreeBlock(freeBlock, freeBlockSize,
                                                block - freeBlock);
                        }

                        freeBlock = block;
                        freeBlockSize = Object_oopToBlock(oop) - freeBlock;
                        if (firstFreeBlock == 0) {
                            firstFreeBlock = freeBlock;
                        }
                    } else {
                        /* contiguous objects - just keep going */
                    }

                    lastOop = oop;
                    iterate();
                }

                block = Object_nextBlock(lastOop);

                if (freeBlock == 0) {
                    assume(firstFreeBlock == 0);
                    if (block != end) {
                        firstFreeBlock = block;
                        createFreeBlock(block, end - block, 0);
                    }
                } else {
                    assume(firstFreeBlock != 0);
                    if (block == end) {
                        createFreeBlock(freeBlock, freeBlockSize, 0);
                    } else {
                        createFreeBlock(freeBlock, freeBlockSize, block - freeBlock);
                        createFreeBlock(block, end - block, 0);
                    }
                }
            }
        } else {
            /* There's no first block so all of persistent memory is free */
            PersistentCollector_init(start, end);
            firstFreeBlock = start;
        }

        endIteration();

        /*
         * Set the pointer to the head of the free block list.
         * This puts the system in a state not
         */
        setEepromFreeList(firstFreeBlock == 0 ? -1 : firstFreeBlock);
    }

/*---------------------------------------------------------------------------*\
 *                        Garbage collection                                 *
\*---------------------------------------------------------------------------*/

    /**
     * Perform a garbage collection of the persistent memory segment.
     * @param bitVector The heap allocated int array that will be used as the
     * bit array for the mark phase.
     * @param markStack The heap allocated int array that will be used to
     * minimise recursion in the marking phase. It's size must be greater than
     * 0.
     */
    void gcPersistentMemory(int bitVector, int markStack) {
//IFC// int bvContext[BV_CONTEXT_LENGTH];
/*IFJ*/ int bvContext[] = new int[BV_CONTEXT_LENGTH];
        int before = 0;

        restartTimer();

        assume(Object_getArrayElementCount(markStack) > 0);

        if (getTraceEepromGC()) {
            before = freePersistentMemory();
            traceln("Garbage collecting persistent memory ...");
            traceInstructionCount(true);
            if (getTraceEepromGCVerbose()) {
                trace("start         "); traceHex(EEPROM);                       traceln("");
                trace("end           "); traceHex(EEPROM + MMR[MMR_eepromSize]); traceln("");
                trace("free          "); traceInt(before);                       traceln(" bytes");
                trace("total         "); traceInt(totalPersistentMemory());      traceln(" bytes");
            }
        }

        /* Put the free list into a state that denotes the persistent memory
           collector has started but has not completed. */
        setEepromFreeList(0);

        BitVector_saveContext(bvContext);
        BitVector_init(EEPROM,
                       MMR[MMR_eepromSize],
                       getObjectPartitionStart(EEPROM),
                       getObjectPartitionEnd(EEPROM),
                       bitVector,
                       Object_getObjectLength(bitVector),
                       false);

        /* Mark all live objects in the EEPROM */
        markPersistent(markStack);

        /* Sweep the dead objects, rebuilding the free list */
        sweepPersistent();

        BitVector_restoreContext(bvContext);
        assume(getEepromFreeList() != 0);

        if (getTraceEepromGC()) {
            int now = freePersistentMemory();
            trace("Collected ");
            traceInt(now - before);
            trace(" bytes of persistent garbage (free=");
            traceInt(now);
            trace(" used=");
            traceInt(totalPersistentMemory() - now);
            trace(" total=");
            traceInt(totalPersistentMemory());
            traceln(")");
        }

        if (getTraceEepromGCSummary()) {
            traceHeapSummary(EEPROM);
        }
        stopTimer(TIME_EEPROM_GC);
        incPersistentGCs();
    }

    /*---------------------------------------------------------------------------*\
     *                       Object graph traversal functions                    *
    \*---------------------------------------------------------------------------*/

//IFC//boolean traversePersistentObjectFromOopMap(int object, int map, int fieldCount, int ftn);
//IFC//boolean updatePersistentOop(int object, int offset);

        private boolean traversePersistentFunction(int addr, int wordOffset, int ftn) {
            switch (ftn) {
                case FTN_UPDATE_POINTERS: {
                    if (!updatePersistentOop(addr, wordOffset)) {
                        return false;
                    }
                    break;
                }
                case FTN_UPDATE_ASSOCIATIONS_AND_CLEAR_WRITEBARRIER:
                    WriteBarrier_clear(addr, wordOffset);
                    /* fall through ... */
                case FTN_RESET_FORWARDING: {
                    int oop = getWord(addr, wordOffset);
                    if (oop != 0 && Object_isForwarded(oop)) {
                        int klass = Object_getClass(Object_getForwardedObject(oop));
                        traversePersistentObject(oop, klass, ftn);
                    }
                    break;
                }
                case FTN_SCAN_RAM_OBJECT:
                case FTN_MARK_OBJECTS: {
                    int oop = getWord(addr, wordOffset);
                    if (oop != 0) {
                        markPersistentObject(oop, ftn);
                    }
                    break;
                }
                default:
                    shouldNotReachHere();
            }
            return true;
        }

/*MAC*/ private void traceAssociationUpdate(int $assn, int $oop) {
            int fwdOop = Object_getForwardedObject($oop);
            trace("updating association ");
            traceHex($assn);
            trace(", object: ");
            traceHex($oop);
            trace(" -> ");
            traceHex(fwdOop);
            trace(", class=");
            trace_className(Object_getClass(fwdOop));
            trace(", isMigratable=");
            traceln(ObjectAssociation_isMigratable($assn) ? "true" : "false");
        }

        /**
         * Traverse all the pointers in a stack chunk.
         * @param oop
         * @param ftn
         */
        void traverseStackChunkPersistent(int oop, int ftn) {
            int target = 0;
            int lp     = StackChunk_getFirstLp(oop);
            int lastLp = StackChunk_getLastLp(oop);

            /*
             * Trace
             */
             if (getTraceEepromGCVerbose()) {
                 trace("+++ traverseStackChunkPersistent ");
                 traceInt(oop);
                 traceln("");
             }

             /*
              * Iterate through each frame
              */
             while (lp <= lastLp) {
                 int mp = Frame_getCurrentMp(lp);
                 int relativeIp = Frame_getCurrentIp(lp) - mp;

                 if (getTraceEepromGCVerbose()) {
                     trace("traverseStackChunkPersistent lp=");
                     traceInt(lp);
                     traceln("");
                 }

                 traversePersistentFunction(lp, FRAME_currentCp, ftn);
                 traversePersistentFunction(lp, FRAME_currentMp, ftn);

                 /*
                  * Update parameters and locals
                  */
                 if (relativeIp == getUnsignedByte(mp, MTH_headerSize)) {
                     /*
                      * Before first instruction
                      */
                     int fieldCount = getUnsignedByte(mp, MTH_numberOfParms);
                     if (getTraceEepromGCVerbose()) {
                         trace("traverseStackChunkPersistent about to verify/mark parameters of method ");
                         trace_methodID(mp);
                         trace(" fieldCount ");
                         traceInt(fieldCount);
                         traceln("");
                     }
                     traversePersistentObjectFromOopMap(lp, mp + MTH_oopMap, fieldCount, ftn);

                     /*
                      * Finished with this chunk since no EXTEND was executed.
                      */
                     assume(lp == lastLp);
                     break;
                 } else {
                     /*
                      * After first instruction
                      */
                     int fieldCount = getUnsignedByte(mp, MTH_numberOfLocals);
                     if (getTraceEepromGCVerbose()) {
                         trace("traverseStackChunkPersistent about to verify/mark parameters & locals of method ");
                         trace_methodID(mp);
                         trace(" fieldCount ");
                         traceInt(fieldCount);
                         traceln("");
                     }
                     traversePersistentObjectFromOopMap(lp, mp + MTH_oopMap, fieldCount, ftn);

                     /*
                      * Advance to next frame in chunk
                      */
                     lp = lp + Frame_getStackOffset(lp) + w2b(1);
                 }
             }

             /*
              * Trace
              */
             if (getTraceEepromGCVerbose()) {
                 trace("--- traverseStackChunkPersistent ");
                 traceInt(oop);
                 traceln("");
             }
        }

        /**
         * Visit all the pointers with in a given object.
         * @param oop
         * @param klass
         * @param ftn
         * @return
         */
        private boolean traversePersistentObject(int oop, int klass, int ftn) {
            int cno = Class_getType(klass);

            /* The class pointer of an object never points to RAM */
            assume(!inRam(klass));

            if (Class_isArrayClass(klass)) {
                int arrayCount;
                if (ftn == FTN_RESET_FORWARDING) {
                    arrayCount = Object_getArrayElementCount(Object_getForwardedObject(oop));
                    assume(Object_isForwarded(oop));
                    Object_setClassAndArrayCount(oop, klass, arrayCount);
                } else if (ftn == FTN_UPDATE_ASSOCIATIONS_AND_CLEAR_WRITEBARRIER) {
                    int fwdOop = Object_getForwardedObject(oop);
                    int assn = getAssociation(oop);
                    arrayCount = Object_getArrayElementCount(fwdOop);
                    assume(Object_isForwarded(oop));

                    /*
                     * Update association (if any) - write barrier clearing is
                     * done in traversePersistentFunction().
                     */
                    if (assn != 0) {
                        if (getTraceMigrationVerbose()) {
                            traceAssociationUpdate(assn, oop);
                        }
                        setInCollector(true);
                        ObjectAssociation_setObject_safe(assn, fwdOop);
                        ObjectAssociation_setIsMigratable(assn, false);
                        setInCollector(false);
                    }
                } else {
                    arrayCount = Object_getArrayElementCount(oop);
                }
                switch (cno) {
/*if[NEWSTRING]*/
                    case CNO.STRING:
                    case CNO.STRING_OF_BYTES:
                    case CNO.STRING_OF_SYMBOLS:
/*end[NEWSTRING]*/
                    case CNO.BOOLEAN_ARRAY:
                    case CNO.BYTE_ARRAY:
                    case CNO.CHAR_ARRAY:
                    case CNO.SHORT_ARRAY:
                    case CNO.INT_ARRAY:
                    case CNO.LONG_ARRAY:
/*if[FLOATS]*/
                    case CNO.FLOAT_ARRAY:
                    case CNO.DOUBLE_ARRAY:
/*end[FLOATS]*/
                    {
                        break;
                    }

                    case CNO.LOCAL_ARRAY: {
                        assume(ftn == FTN_SCAN_RAM_OBJECT);
                        traverseStackChunkPersistent(oop, ftn);
                        break;
                    }

                    case CNO.GLOBAL_ARRAY: {
                        int count, i;
                        int gaKlass = ClassState_getClass(oop);
                        assume(ftn == FTN_SCAN_RAM_OBJECT);
                        assume(gaKlass != 0);
                        count = Class_getPointerStaticFieldsLength(gaKlass) + CLS_STATE_offsetToFields;
                        for (i = 0 ; i < count ; i++) {
                            traversePersistentFunction(oop, i, ftn);
                        }
                        break;
                    }

                    default: {
                        /* A normal pointer array */
                        int i;
                        for (i = 0 ; i < arrayCount ; i++) {
                            if (!traversePersistentFunction(oop, i, ftn)) {
                                return false;
                            }
                        }
                    }
                }
                return true;
            } else {
                int map        = Object_getPossiblyForwardedObject(Class_getOopMap(klass));
                int fieldCount = Class_getInstanceFieldsLength(klass);
                int mapBytes = (fieldCount+7)/8;
                int offset = 0;

                if (ftn == FTN_RESET_FORWARDING) {
                    assume(Object_isForwarded(oop));
                    Object_setClass(oop, klass);
                }  else if (ftn == FTN_UPDATE_ASSOCIATIONS_AND_CLEAR_WRITEBARRIER) {
                    int assn = getAssociation(oop);
                    assume(Object_isForwarded(oop));

                    /*
                     * Update association (if any) - write barrier clearing is
                     * done in traversePersistentFunction().
                     */
                    if (assn != 0) {
                        if (getTraceMigrationVerbose()) {
                            traceAssociationUpdate(assn, oop);
                        }

                        setInCollector(true);
                        ObjectAssociation_setObject_safe(assn, Object_getForwardedObject(oop));
                        ObjectAssociation_setIsMigratable(assn, false);
                        setInCollector(false);
                    }
                }

                return traversePersistentObjectFromOopMap(oop, map, fieldCount, ftn);
            }
        }

        /**
         * Update the pointers of an object based on an oop map.
         * @param object
         * @param map
         * @param fieldCount The number of fields in the object to
         * be updated using the map.
         */
        boolean traversePersistentObjectFromOopMap (int object, int map, int fieldCount, int ftn) {
            int i;
            int mapBytes = (fieldCount+7)/8;
            int offset = 0;
            for (i = 0 ; i < mapBytes ; i++) {
                int mapbyte = getUnsignedByte(0, map+i);
                int j;
                for (j = 0; j != 8 && offset != fieldCount; ++j, ++offset) {
                    if ((mapbyte&(1<<j)) != 0) {
                        if (!traversePersistentFunction(object, offset, ftn)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

/*---------------------------------------------------------------------------*\
 *                          RAM --> EEPROM migration                         *
\*---------------------------------------------------------------------------*/

    private int totalBytesCopied;

    /**
     * Determine whether or not a given object needs to be copied into
     * peristent memory. Returns true iff the object is non-null, has not
     * already been copied into persistent memory and is in RAM.
     * @param oop
     * @return
     */
/*MAC*/ private boolean needPersistentCopy(int $oop) {
            return $oop != 0 && !Object_isForwarded($oop) && inRam($oop);
        }

    /**
     * Make a copy of a given object in persistent memory if it is currently
     * in RAM and has not already been copied to persistent memory.
     * @param oop The object to be persisted.
     * @return if a copy was performed, return the addrss of the copied object
     * otherwise return the given object (which must have already been copied
     * or was already in persistent memory/ROM). If a copy was attempted by
     * the allocation in persistent memory failed, then 0 is returned.
     */
    private int copyObjectToPersistentMemory(int oop) {
        assume(oop != 0);
        assume(!Object_isForwarded(oop));
        {
            int klass = Object_getClass(oop);
            int chunk = Object_oopToBlock(oop);
            int headerSize = oop - chunk;
            int chunkSize = headerSize + Object_getObjectLength(oop);
            int pchunk = newPersistentChunk(chunkSize, false);
            if (pchunk != 0) {
//IFC//         boolean traversePersistentObject(int oop, int klass, int ftn);
                int oopCopy = pchunk + headerSize;

                /* Copy the object into the newly allocated chunk */
                int wordSize = chunkSize / bytesPerWord();
                int i;
                for (i = 0; i != wordSize; ++i) {
                    setPersistentWord(pchunk, i, getWord(chunk, i));
                }

                /* Update the total number of bytes copied */
                totalBytesCopied += chunkSize;

                /* Set the forwarding pointer */
                Object_forwardToObject(oop, oopCopy);

                if (getTraceMigrationVerbose()) {
                    trace("copyObjectToPersistentMemory ");
                    traceHex(oop);
                    trace(" -> ");
                    traceHex(oopCopy);
                    trace(", klass=\"");
                    trace_className(klass);
                    trace(", size=");
                    traceInt(Object_getObjectLength(oopCopy));
                    traceln("\"");
                }

                /* Update the pointers in the forwarded object. */
                if (traversePersistentObject(oopCopy, klass, FTN_UPDATE_POINTERS)) {
                    return oopCopy;
                }
            }
            return 0;
        }
    }

    /**
     * Update a pointer field within an object that has just been copied from
     * RAM to persistent memory. This will cause the object pointed to to be
     * copied into persistent memory if necessary.
     * @param object
     * @param offset
     * @return
     */
    private boolean updatePersistentOop(int object, int offset) {
        int oldOop = getWord(object, offset);
        if (needPersistentCopy(oldOop)) {
            int oop = copyObjectToPersistentMemory(oldOop);
            if (oop == 0) {
                return false;
            }
            setPersistentWord(object, offset, oop);
        }
        return true;
    }

    /**
     * Copy a given object graph from RAM to peristent memory.
     * @param root The head of an object graph in RAM that is to be copied into
     * persistent memory.
     * @param copyForMigration If true, then don't reset the forwarding pointers
     * for all the objects that were copied.
     * @return the head of the copied graph.
     */
    int makePersistent(int root, boolean copyForMigration) {
        assume(root != 0);

        /* Reset the copied bytes counter */
        totalBytesCopied = 0;

        if (needPersistentCopy(root)) {
            int pRoot;
            int klass = Object_getClass(root);

            restartTimer();

            if (getTraceMigration()) {
                trace(copyForMigration ? "Migrating" : "Copying");
                trace(" object graph rooted at ");
                traceHex(root);
                trace("(class=");
                trace_className(Object_getClass(root));
                trace(")");
                traceln(" from RAM to EEPROM ...");
                traceInstructionCount(true);
            }

            /* Copy the graph of objects rooted at 'root' */
            pRoot = copyObjectToPersistentMemory(root);

            if (pRoot == 0) {
                if (Object_isForwarded(root)) {
                    /* Undo the forwarding pointers in the original objects that were copied. */
                    boolean ok = traversePersistentObject(root, klass, FTN_RESET_FORWARDING);
                    assume(ok);
                }
            } else if (!copyForMigration) {
                /* Undo the forwarding pointers in the original objects that were copied. */
                boolean ok = traversePersistentObject(root, klass, FTN_RESET_FORWARDING);
                assume(ok);
                incPersistentCopies();
            } else {
                /* Update the associations (if any) for objects that were copied. */
                boolean ok = traversePersistentObject(root, klass, FTN_UPDATE_ASSOCIATIONS_AND_CLEAR_WRITEBARRIER);
                assume(ok);
                incPersistentMigrations();
            }

            root = pRoot;

            if (getTraceMigration()) {
                trace(copyForMigration ? "Migrated " : "Copied ");
                traceInt(totalBytesCopied);
                trace(" bytes ");
                if (root == 0) {
                    trace("before exhausting persistent memory ");
                }
                trace("(");
                traceInt(freePersistentMemory());
                traceln(" bytes free)");
            }

            stopTimer(TIME_RAM_TO_EEPROM_COPY);
        }
        return root;
    }

/*MAC*/ int getTotalBytesCopiedToPersistentMemory() {
            return totalBytesCopied;
        }


    /**
     * Allocate a byte array in eeprom.
     * @param count the number of bytes
     * @return the object or zero if the allocation failed
     */
        int newPersistentByteArray(int count) {
            int klass      = getClassFromCNO(CNO.BYTE_ARRAY);
            int byteLength = count * Object_getArrayElementLength(klass);
            int headerSize = Object_calculateArrayHeaderLength(count);
            int oop        = newPersistentChunk(byteLength+headerSize, false);
            if (oop != 0) {
                oop += headerSize;
                Object_setClassAndArrayCount(oop, klass, count);
            }
            return oop;
        }

/*IFJ*/}