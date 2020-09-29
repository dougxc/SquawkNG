//J2C:cheney.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/import java.io.*;
/*IFJ*/import java.util.*;
/*if[VM.GCSPY]*/
/*IFJ*/import gcspy.interpreter.server.*;
/*end[VM.GCSPY]*/

/*IFJ*/abstract public class CheneyCollector extends ObjectAssociations {

/*if[CHENEY.COLLECTOR]*/

/*---------------------------------------------------------------------------*\
 *                                 Variables                                 *
\*---------------------------------------------------------------------------*/

        /**
         * These variables are used by the garbage collector and a few other
         * functions that traverse the heap.
         */
        private int targetPartition;         /* Start or target partition */
        private int targetPartitionPtr;      /* Point at which copyNonRoots will start */
        private int targetPartitionFreePtr;  /* Target partition append point */
        private int targetPartitionEnd;      /* End of target partition */

/*MAC*/ private boolean inTargetPartition(int $oop) { return $oop > targetPartition && $oop < targetPartitionEnd; }


/*-----------------------------------------------------------------------*\
 *                          Minimum Heap mode                            *
\*-----------------------------------------------------------------------*/

        /**
         * setMinimumHeapMode
         */
/*MAC*/ void setMinimumHeapMode(boolean $mode) {
            fatalVMError("setMinimumHeapMode not supported in Cheney collector");
        }

        /**
         * getHeapHighWaterMark
         */
/*MAC*/ int getHeapHighWaterMark() {
            return 0;
        }


/*---------------------------------------------------------------------------*\
 *                              Garbage collection                           *
\*---------------------------------------------------------------------------*/

        /**
         * twoSpaceRamCollector
         */
/*MAC*/ boolean twoSpaceRamCollector() {
            return true;
        }

        /**
         * canCollectRom
         */
/*MAC*/ boolean canCollectRom() {
            return true;
        }

        /**
         * Return the address in a given segment at which the object memory starts.
         * For Cheney, this is simply the start of the heap.
         */
/*MAC*/ int calculateObjectMemoryStart(int $segment) {
            return getHeapStart($segment);
        }

        /**
         * Get the amount of memory in a given segment that will be available for objects.
         * For Cheney, this is half the size of a segment once the MSR overhead
         * has been accounted for.
         * @param segment
         * @param segmentSize
         * @return
         */
/*MAC*/ int calculateObjectMemorySize(int $segment, int $segmentSize) {
            return $segmentSize - getSegmentMSROverhead() / 2;
        }

        /**
         * Initialize the collector variables.
         *
         * @param romizing If true, then this is being called by the romizer
         * and therefore collector related variables in the image will be
         * set. Otherwise, only the non-image globals will be set. This enables
         * a VM to restart after its state was checkpointed to an image.
         */
/*MAC*/ void initializeCollector(boolean $romizing) {
            int seg = getCurrentSegment();
            int start = getObjectMemoryStart(seg);
            int expectedObjectMemorySize  = calculateObjectMemorySize(seg, getCurrentSegmentSize()) / 2;

            assume(getObjectPartitionStart(seg) == start);
            assume(getObjectPartitionFree(seg) == start);
            assume(getHeapStart(seg) == calculateObjectMemoryStart(seg));

            /*
             * Handle the case where the segment was configured for a single-space
             * collector such as Lisp2.
             */
            if (getObjectMemorySize(seg) != expectedObjectMemorySize) {
                if (getTraceGC()) {
                    trace("Object memory size changed from ");
                    traceInt(getObjectMemorySize(seg));
                    trace(" to ");
                }

                if ($romizing) {
                    setObjectMemorySize(seg, expectedObjectMemorySize);
                    setObjectPartitionEnd(seg, getCurrentSegment() + expectedObjectMemorySize);
                }

                if (getTraceGC()) {
                    traceInt(expectedObjectMemorySize);
                    trace(" (segment size = ");
                    traceInt(getCurrentSegmentSize());
                    traceln(")");
                }
            }
        }

        /**
         * Update a pointer that points to somewhere inside 'oldObject' to
         * point to the same place in 'newObject'.
         * @param oldObject An object which has an internal pointer.
         * @param newObject The copy of oldObject.
         * @param oop The internal pointer.
         * @return the pointer now pointing into the same place in 'newObject'.
         */
/*MAC*/ private int updateInternalOop(int $oldObject, int $newObject, int $oop) {
            return $newObject + ($oop - $oldObject);
        }

        /**
         * Copy the contents of an object into the target partition. If 'oop' is
         * 0 or points to an object that has already been copied, then no copy
         * is done.
         * @param oop The pointer to the object to be copied.
         * @return the pointer to the (potentially previously) copied object.
         */
        private int copyObject(int oop) {
            if (oop == 0) {
                if (getTraceGCVerbose()) {
                    traceln("copyObject 0");
                }
                return 0;
            }
            assume(inCurrentSegment(oop));
            if (inTargetPartition(oop)) {
                if (getTraceGCVerbose()) {
                    trace("copyObject in target partition " );
                    traceHex(oop);
                    traceln("");
                }
                return oop;
            } else {
                if (Object_isForwarded(oop)) {
                    if (getTraceGCVerbose()) {
                        trace("copyObject already forwarded from " );
                        traceHex(oop);
                        trace(" -> ");
                        traceHex(Object_getForwardedObject(oop));
                        traceln("");
                    }
                    return Object_getForwardedObject(oop);
                } else {
                    int klass      = Object_getClass(oop);
                    int chunk      = Object_oopToBlock(oop);
                    int headerSize = oop - chunk;
                    int chunkSize  = headerSize + Object_getObjectLength(oop);
                    int target     = targetPartitionFreePtr;
                    int targetOop  = target + headerSize;

                    assume(roundup4(chunkSize) == chunkSize);

                    /* Only 'live' stack chunks should be copied. */
                    assume(Class_getType(klass) != CNO.LOCAL_ARRAY || StackChunk_getLastLp(oop) != 0);

                    if (getTraceGCVerbose()) {
                        trace("copyObject about to copy " );
                        traceHex(oop);
                        trace(" klass=\"");
                        trace_className(klass);
                        trace("\"");
                        traceln("");
                    }
                    copyBytes(chunk, target, chunkSize);
                    targetPartitionFreePtr += chunkSize;
                    Object_forwardToObject(oop, targetOop);
                    if (getTraceGCVerbose()) {
                        trace("copyObject ");
                        traceHex(oop);
                        trace(" -> ");
                        traceHex(targetOop);
                        trace(" size ");
                        traceInt(chunkSize);
                        //trace(" klass=\"");      trace_className(Object_getClass(targetOop)); trace("\"");
                        traceln("");
                    }
                    return targetOop;
                }
            }
        }


        /**
         * Update a pointer field of an object.
         * @param object The object containing the pointer field.
         * @param offset The offset (in words) of the pointer field in object.
         */
        private void updateOopPrim(int object, int offset, boolean isForClass) {
            int oldOop;
            if (isForClass) {
                oldOop = Object_getClass(object);
                if (getTraceGCVerbose()) {
                    trace("updateClass for ");
                    traceHex(object);
                    traceln("");
                }
            } else {
                oldOop = getWord(object, offset);
                if (getTraceGCVerbose()) {
                    trace("updateOop ");
                    traceHex(object);
                    trace(" % ");
                    traceHex(offset*4);
                    traceln("");
                }
            }
            if (inCurrentSegment(oldOop)) {       /* Don't cross segment barrier. */
                int newOop = copyObject(oldOop);
                if (isForClass) {
                    Object_setClass(object, newOop);
                    if (getTraceGCVerbose()) {
                        trace("class change for ");
                        traceHex(object);
                        trace(" oldClass=");
                        traceHex(oldOop);
                        trace(" newClass=");
                        traceHex(newOop);
                        traceln("");
                    }
                } else {
                    setWord(object, offset, newOop);
                }
            }
        }


        /**
         * Update a pointer field of an object.
         * @param object The object containing the pointer field.
         * @param offset The offset (in words) of the pointer field in object.
         */
/*MAC*/ void updateOop(int $object, int $offset) {
            updateOopPrim($object, $offset, false);
        }


        /**
         * Update the class pointer of an object.
         * @param object The object containing the pointer field.
         */
/*MAC*/ void updateClassOop(int $object) {
            updateOopPrim($object, Object_wordOffsetToClassPointer(), true);
        }


        /**
         * Update the pointers of an object based on an oop map.
         * @param object
         * @param map
         * @param fieldCount The number of fields in the object to
         * be updated using the map.
         */
        void updateFromOopMap(int object, int map, int fieldCount) {
            int i;
            int mapBytes = (fieldCount+7)/8;
            int offset = 0;
            for (i = 0 ; i < mapBytes ; i++) {
                int mapbyte = getUnsignedByte(0, map+i);
                int j;
                for (j = 0; j != 8 && offset != fieldCount; ++j, ++offset) {
                    if ((mapbyte&(1<<j)) != 0) {
                        updateOop(object, offset);
                    }
                }
            }
        }


        /**
         * Update the pointers in a stack chunk. This includes both external pointers
         * (i.e. to real objects) and internal pointers (i.e. to offsets within the
         * stack chunk).
         * @param oop The copy of the stack chunk in the target partition.
         */
        void updateStackChunkOops(final int oop) {
            int nextChunk;
            int fromOop = StackChunk_getSelf(oop);
            int lastLp  = StackChunk_getLastLp(fromOop);
            int lp      = StackChunk_getFirstLp(oop);
            int level   = 0;

            /*
             * This must only be called on a stack chunk that has not been
             * updated yet.
             */
            assume(fromOop != oop);

            /* Only 'live' stack chunks should have been copied. */
            assume(lastLp != 0);

            lastLp = updateInternalOop(fromOop, oop, lastLp);
            StackChunk_setLastLp(oop, lastLp);

            /* Walk frames in stack */
            while (lp <= lastLp) {
                int relativeIp, mp, prevLp;

                /* Update class pointer */
                updateOop(lp, FRAME_currentCp);

                /* Update method and instruction pointer */
                relativeIp = Frame_getCurrentIp(lp) - Frame_getCurrentMp(lp);
                updateOop(lp, FRAME_currentMp);
                mp = Frame_getCurrentMp(lp);
                Frame_setCurrentIp(lp, relativeIp + mp);

                /* Update previous frame pointer */
                prevLp = Frame_getPreviousLp(lp);
                if (prevLp != 0) {
                    Frame_setPreviousLp(lp, updateInternalOop(fromOop, oop, prevLp));
                }

                /* Update parameters and locals */
                if (relativeIp == getUnsignedByte(mp, MTH_headerSize)) {
                    /* Before first instruction */
                    int fieldCount = getUnsignedByte(mp, MTH_numberOfParms);
                    if (getTraceGCVerbose()) {
                        trace("updateStackChunkOops about to update parameters of method ");
                        trace_methodID(mp);
                        trace(" at depth ");
                        traceInt(level++);
                        trace(" in stack chunk");
                        traceln("");
                    }
                    updateFromOopMap(lp, mp + MTH_oopMap, fieldCount);

                    /* Finished with this chunk since no EXTEND was executed. */
                    assume(lp == lastLp); /* I'm not sure about this assertion... */
                    break;
                } else {
                    int fieldCount = getUnsignedByte(mp, MTH_numberOfLocals);
                    if (getTraceGCVerbose()) {
                        trace("updateStackChunkOops about to update parameters & locals of method ");
                        trace_methodID(mp);
                        trace(" at depth ");
                        traceInt(level++);
                        trace(" in stack chunk");
                        traceln("");
                    }
                    updateFromOopMap(lp, mp + MTH_oopMap, fieldCount);
                    lp = lp + Frame_getStackOffset(lp)+w2b(1);
                }
            }

            /* Update next and prev pointers */
            nextChunk = StackChunk_getNext(oop);          /* Get the next chunk */
            if (nextChunk != 0) {
                if (StackChunk_getLastLp(nextChunk) == 0) {     /* In use? */
                    StackChunk_setNext(oop, 0);           /* N - delete */
                } else {
                    updateOop(oop, STACK_next);           /* Y - Update */
                }
            }
            updateOop(oop, STACK_prev);
            StackChunk_setSelf(oop, oop);
        }



        /**
         * Copy the root objects.
         */
        void copyRoots() {
            int i;
            for (i = 0; i != ROOT_SIZE; i++) {
                if (i != ROOT_associationHashtable) {         /* Do not process the object associations here. */
                    int oop = getRoot(i);
                    if (oop != 0) {
                        if (getTraceGCVerbose()) {
                            int foop  = Object_getPossiblyForwardedObject(oop);
                            int klass = Object_getClass(foop);
                            trace("copyRoots oop="); traceHex(oop);
                            trace(" klass=\"");      trace_className(klass); trace("\"");
                            trace(" cno=");          traceHex(Class_getType(klass));
                            traceln("");
                        }
                        setRoot(i, copyObject(oop));
                    }
                }
            }
        }


        /**
         * copyNonRoots
         */
        void copyNonRoots() {
            int oop;
            if (getCurrentObjectPartitionFree() == getCurrentObjectPartition()) {
                return; /* Empty Heap! */
            }

            if (targetPartitionPtr == targetPartitionFreePtr) {
                return; /* Nothing to do */
            }

            for (oop = Object_blockToOop(targetPartitionPtr) ; oop != 0 ; oop = Object_nextObject(oop, targetPartition, targetPartitionFreePtr)) {
                int klass = Object_getClass(oop);  /* Get the class of the oop*/
                int cno = Class_getType(klass);

                if (getTraceGCVerbose()) {
                    trace("copyNonRoots oop="); traceHex(oop);
                    trace(" klass=\"");         trace_className(klass); trace("\"");
                    trace(" cno=");             traceHex(Class_getType(klass));
                    traceln("");
                }

                /* Mark the object's klass */
                updateClassOop(oop);

                if (Class_isArrayClass(klass)) {
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
                            continue;
                        }

                        case CNO.LOCAL_ARRAY: {
                            updateStackChunkOops(oop);
                            continue;
                        }

                        case CNO.GLOBAL_ARRAY: {
                            int gaklass, count;
                            /* The per-isolate state for a class */
                            gaklass = ClassState_getClass(oop);
                            assume(gaklass != 0);
                            count = Class_getPointerStaticFieldsLength(gaklass) + CLS_STATE_offsetToFields;
                            while (--count >= 0) {
                                updateOop(oop, count);
                            }
                            continue;
                        }

                        default: {
                            /* A normal pointer array */
                            int arrayCount = Object_getArrayElementCount(oop);
                            int i;
                            for (i = 0 ; i < arrayCount ; i++) {
                                updateOop(oop, i);
                            }
                            continue;
                        }
                    }
                } else {
                    int map        = Object_getPossiblyForwardedObject(Class_getOopMap(klass));
                    int fieldCount = Class_getInstanceFieldsLength(klass);
                    assume(Object_getArrayElementCount(map) == ((fieldCount+7)/8));
                    updateFromOopMap(oop, map, fieldCount);
                }
            }
            targetPartitionPtr = targetPartitionFreePtr;
        }

        /**
         * Configure the variables pointing to the 'to' space for the next garbage
         * collection.
         * @param segment
         */
        private void setupTargetPartition() {
            int start = getCurrentHeapStart();
            int size  = getCurrentHeapSize();
            int half  = size / 2;
            int before = 0;

            if (getCurrentObjectPartition() == start) {
                targetPartition         = start + half;
                targetPartitionEnd      = start + size;
            } else {
                assume(getCurrentObjectPartition() == start + half);
                targetPartition         = start;
                targetPartitionEnd      = start + half;
            }

            targetPartitionPtr = targetPartitionFreePtr = targetPartition;
        }

        /**
         * This re-configures the current object partition after a garbage collection based
         * on the current target partition variables.
         * @param segment
         */
        private void finishTargetPartition() {
            setCurrentObjectPartition(targetPartition);
            setCurrentObjectPartitionFree(targetPartitionFreePtr);
            setCurrentObjectPartitionEnd(targetPartitionEnd);

            targetPartition         = 999999999;
            targetPartitionPtr      = 999999999;
            targetPartitionFreePtr  = 999999999;
            targetPartitionEnd      = 999999999;
        }

        /**
         * Perform a garbage collection.
         */
        private void gcPrim() {
            int before = 0;

            setupTargetPartition();

            if (getTraceGC()) {
                traceln("Garbage collecting ...");
                traceInstructionCount(true);
                if (getTraceGCVerbose()) {
                    trace("currentObjectPartition         "); traceHex(getCurrentObjectPartition());          traceln("");
                    trace("currentObjectPartitionFreePtr  "); traceHex(getCurrentObjectPartitionFree());   traceln("");
                    trace("currentObjectPartitionEnd      "); traceHex(getCurrentObjectPartitionEnd());       traceln("");
                    trace("targetPartition          "); traceHex(targetPartition);                traceln("");
                    trace("targetPartitionFreePtr   "); traceHex(targetPartitionFreePtr);         traceln("");
                    trace("targetPartitionEnd       "); traceHex(targetPartitionEnd);             traceln("");
                }
                before = freeMem();
            }

           /*
            * Copy the roots and the things they point to
            */
            copyRoots();
            copyNonRoots();

           /*
            * Proccess the association queue and copy the things they point to
            */
            processAssociationQueues();
            copyNonRoots();
            checkAssociationQueues();

           /*
            * Finish up
            */
            incCollectionCount();
            finishTargetPartition();
            justDoneGC = true;

            if (getTraceGC()) {
                trace("Collected ");
                traceInt(freeMem() - before);
                trace(" bytes of garbage (free=");
                traceInt(freeMem());
                trace(" used=");
                traceInt(totalMem() - freeMem());
                trace(" total=");
                traceInt(totalMem());
                traceln(")");
            }

            if (getTraceGCSummary()) {
                traceHeapSummary(getCurrentSegment());
            }

        }

//IFC//#ifndef PRODUCTION
        /*
         * clearTargetPartition
         */
        void clearTargetPartition() {
            int i;
            int start = getCurrentHeapStart();
            int size  = getCurrentHeapSize();
            int half  = size / 2;

            if (getCurrentObjectPartition() == start) {
                targetPartition         = start + half;
                targetPartitionEnd      = start + size;
            } else {
                targetPartition         = start;
                targetPartitionEnd      = start + half;
            }

            if (getTraceGCVerbose()) {
                trace("cleartargetPartition ");
                traceHex(targetPartition);
                trace(" -> ");
                traceHex(targetPartitionEnd);
                traceln("");
            }

            for (i = targetPartition ; i < targetPartitionEnd ; i+=4) {
                setWord(i, 0, 0xdead1dea);
            }

            targetPartition         = 0;
            targetPartitionFreePtr  = 0;
            targetPartitionEnd      = 0;
        }
//IFC//#else
//IFC//#define clearTargetPartition() /**/
//IFC//#endif /* PRODUCTION */

        /**
         * Perform a garbage collection
         * @return whether or not the amount of free space after collection is
         * greater than the amount that failed to be allocated during the last
         * allocation attempt.
         */
        boolean gcRam() {
            setInCollector(true);
            gcPrim();
            setInCollector(false);
            clearTargetPartition();
            return (freeMem() >= getFailedGCLength());
        }


/*-----------------------------------------------------------------------*\
 *                           Forwarded functions                         *
\*-----------------------------------------------------------------------*/

        boolean IsMarked(int obj) {
            return inTargetPartition(obj);
        }

        boolean IsAlive(int obj) {
            return !inCurrentObjectPartition(obj);
        }

        int KeepObject(int obj) {
            return copyObject(obj);
        }

        void printGCStats() {
        }

        int GetPossiblyForwarded(int obj) {
            return Object_getPossiblyForwardedObject(obj);
        }

/*-----------------------------------------------------------------------*\
 *                            Memory statistics                          *
\*-----------------------------------------------------------------------*/

        /**
         * Get the total amount of memory available (in bytes).
         * @return the total amount of memory available (in bytes).
         */
        int totalMem() {
            return getCurrentObjectMemorySize();
        }

/*if[VM.GCSPY]*/
//IFC//#if 0
/*---------------------------------------------------------------------------*\
 *                               GCSpy interface                             *
\*---------------------------------------------------------------------------*/

        void gcspyInitializeLisp2Driver(ServerInterpreter interpreter, int blockSize) {
            fatalVMError("GCspy driver not implemented for Cheney collector.");
        }
        void gcspyFinalizeLisp2Driver() {}
        public void gcspyEvent(int eventID) {}

//IFC//#endif
/*end[VM.GCSPY]*/


/*end[CHENEY.COLLECTOR]*/

/*IFJ*/}
