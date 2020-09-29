//J2C:lisp2.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/import java.io.*;
/*IFJ*/import java.util.*;
/*if[VM.GCSPY]*/
/*IFJ*/import gcspy.interpreter.*;
/*IFJ*/import gcspy.interpreter.server.*;
/*IFJ*/import gcspy.utils.ColorDB;
/*end[VM.GCSPY]*/

/*IFJ*/abstract public class Lisp2Collector extends CheneyCollector {

/*if[LISP2.COLLECTOR]*/

/*-----------------------------------------------------------------------*\
 *                               Descripton                              *
\*-----------------------------------------------------------------------*/

/*

             segmentEnd ->

                                Bit vector


                                Mark stack

                heapEnd ->

                                Unused (except as extra marking stack)

          collectionEnd ->

                                Young Generation

        collectionStart ->

                                Old Generation

              heapStart ->

                                MSR

           segmentStart ->


*/

/*-----------------------------------------------------------------------*\
 *                           Traverse functions                          *
\*-----------------------------------------------------------------------*/

    final static int
        UPDATEPOINTERS      = 1,
        MARKOBJECTS         = 2,
        VERIFYOBJECTS       = 3;


/*-----------------------------------------------------------------------*\
 *                            Utility functions                          *
\*-----------------------------------------------------------------------*/

/*MAC*/ int     getCollectionStart()        { return getCurrentObjectPartition();                       }
/*MAC*/ int     getCollectionEnd()          { return getCurrentObjectPartitionFree();                   }
/*MAC*/ boolean inCollectionSpace(int $oop) { return inCurrentObjectPartition($oop);                    }

/*MAC*/ boolean isMarked(int $oop)          { assume(inCollectionSpace($oop)); return testOopBit($oop); }
/*MAC*/ boolean isAlive(int $oop)           { return !inCollectionSpace($oop) || isMarked($oop);        }

//IFC// void traverseRoots(int ftn);
//IFC// void traverseWriteBarrierOops(int ftn);
//IFC// void traverseObject(int oop, int klass, int ftn);
//IFC// void markStackChunks();
//IFC// void updateStackChunks();


/*-----------------------------------------------------------------------*\
 *                          Minimum Heap mode                            *
\*-----------------------------------------------------------------------*/

        boolean minHeapMode = false;
        int     heapHWM;
        int     idealYoungGenerationSize;

        /**
         * setIdealYoungGenerationSize
         */
/*MAC*/ void setIdealYoungGenerationSize() {
            idealYoungGenerationSize = minHeapMode ? 16 : (getCurrentObjectMemorySize() * getYoungPercent()) / 100;
        }

        /**
         * setMinimumHeapMode
         */
/*MAC*/ void setMinimumHeapMode(boolean $mode) {
            setFullGC($mode);
            minHeapMode = $mode;
            setCurrentObjectPartitionEnd(getCurrentObjectPartitionFree());
            heapHWM = getCollectionEnd();
            setIdealYoungGenerationSize();
        }

        /**
         * getHeapHighWaterMark
         */
/*MAC*/ int getHeapHighWaterMark() {
            return heapHWM - getCurrentObjectMemoryStart();
        }

/*-----------------------------------------------------------------------*\
 *                                  Stats                                *
\*-----------------------------------------------------------------------*/


//IFC//#ifdef STATS
        long fullGCs;
        long partialGCs;
        long markStackOverFlows;
        long incompleteRelocations;

        long totalTime;
        long testTime;
        long markTime;
        long computeAddressTime;
        long updatePointerTime;
        long relocateTime;

        void incFullGCs()                       { fullGCs++;                                }
        void incPartialGCs()                    { partialGCs++;                             }
        void incMarkStackOverFlows()            { markStackOverFlows++;                     }
        void incIncompleteRelocations()         { incompleteRelocations++;                  }

        long getStartTime()                     { return getTime();                         }
        void incTotalTimer(long time)           { totalTime += getTime() - time;            }
        void incTestTimer(long time)            { testTime += getTime() - time;             }
        void incMarkTimer(long time)            { markTime += getTime() - time;             }
        void incComputeAddressTimer(long time)  { computeAddressTime += getTime() - time;   }
        void incUpdatePointerTimer(long time)   { updatePointerTime += getTime() - time;    }
        void incRelocateTimer(long time)        { relocateTime += getTime() - time;         }

//IFC//#else
//IFC//#define incFullGCs()                     /**/
//IFC//#define incPartialGCs()                  /**/
//IFC//#define incMarkStackOverFlows()          /**/
//IFC//#define incIncompleteRelocations()       /**/

//IFC//#define getStartTime()                    0
//IFC//#define incTotalTimer(time)              /**/
//IFC//#define incTestTimer(time)               /**/
//IFC//#define incMarkTimer(time)               /**/
//IFC//#define incComputeAddressTimer(time)     /**/
//IFC//#define incUpdatePointerTimer(time)      /**/
//IFC//#define incRelocateTimer(time)           /**/


//IFC//#endif

        void printGCStats() {
//IFC//#ifdef STATS
            printMsgLong("  Total GCs                ", fullGCs+partialGCs        );
            printMsgLong("  Full GCs                 ", fullGCs                   );
            printMsgLong("  Partial GCs              ", partialGCs                );
            printMsgLong("  Mark stack overflows     ", markStackOverFlows        );
            printMsgLong("  Incomplete Relocations   ", incompleteRelocations     );
            printMsgLong("  High water mark          ", getHeapHighWaterMark()    );
            printMsgLong("  Lisp2 GC time (ms)       ", totalTime                 );
//            printMsgLong("  Test Time                ", testTime                  );
            printMsgLong("  Mark time (ms)           ", markTime                  );
            printMsgLong("  ComputeAddress time (ms) ", computeAddressTime        );
            printMsgLong("  UpdatePointer time (ms)  ", updatePointerTime         );
            printMsgLong("  Relocate time (ms)       ", relocateTime              );

//IFC//#endif
        }


/*---------------------------------------------------------------------------*\
 *                          Garbage collection setup                         *
\*---------------------------------------------------------------------------*/

        /**
         * twoSpaceRamCollector
         */
/*MAC*/ boolean twoSpaceRamCollector() {
            return false;
        }

        /**
         * canCollectRom
         */
/*MAC*/ boolean canCollectRom() {
            return false;
        }

        /**
         * Return the address in a given segment at which the object memory starts.
         * For Lisp2, this is simply the start of the heap.
         */
/*MAC*/ int calculateObjectMemoryStart(int $segment) {
            return getHeapStart($segment);
        }

        /**
         * Get the amount of memory in a given segment that will be available for objects.
         * @param segment
         * @param segmentSize
         * @return
         */
/*MAC*/ int calculateObjectMemorySize(int $segment, int $segmentSize) {
            return calculateObjectMemoryAndBitmapSize($segment, $segmentSize, null);
        }

        /**
         * Calculate the amount of a specified segment that can be used as the
         * object memory as well as the size of the bitmap vector that will be required
         * for the write barrier of the object memory.
         * @param segment
         * @param segmentSize
         * @param bitmapSizeRef
         * @return
         */
/*MAC*/ private int calculateObjectMemoryAndBitmapSize(int $segment, int $segmentSize, int $bitmapSizeRef[]) {
           /*
            * Note -- Always have an extra word for the bitmap because it needs to cover
            * a range from firstOop to lastOop inclusively.
            */
            int bitmapSize    = bytesPerWord();
            int chunkSize     = bitsPerWord() * bytesPerWord();
            int firstOop      = calculateObjectMemoryStart($segment);
            int lastOop       = firstOop;
            int segRemain     = $segmentSize - getSegmentMSROverhead() - MINMARKSTACKSIZE;

            if ((lastOop & (chunkSize-1)) != 0) {
                lastOop  &= ~(chunkSize-1);             // Round up to first chunk boundery
                lastOop  += chunkSize;                  // ...
                bitmapSize += bytesPerWord();
            }

            while (segRemain - bitmapSize - (lastOop - firstOop) > chunkSize) {
                bitmapSize += bytesPerWord();
                lastOop    += chunkSize;
            }

            if (segRemain - bitmapSize - (lastOop - firstOop) > bytesPerWord()) {
                bitmapSize += bytesPerWord();
                lastOop = firstOop + (segRemain - bitmapSize - MINMARKSTACKSIZE);
            }

            if ($bitmapSizeRef != null) {
                $bitmapSizeRef[0] = bitmapSize;
            }
            return lastOop - firstOop;
        }





/*-----------------------------------------------------------------------*\
 *                             Initialization                            *
\*-----------------------------------------------------------------------*/


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
/*IFJ*/     int bitmapSizeRef[] = new int[1];
//IFC//     int bitmapSizeRef[1];
            int expectedObjectMemorySize = calculateObjectMemoryAndBitmapSize(seg, getCurrentSegmentSize(), bitmapSizeRef);

            assume(getObjectMemoryStart(seg) == calculateObjectMemoryStart(seg));

            /*
             * Handle the case where the segment was configured for a two-space
             * collector such as Cheney.
             */
            if (getObjectMemorySize(seg) != expectedObjectMemorySize) {
                if (getTraceGC()) {
                    trace("Object memory size changed from ");
                    traceInt(getObjectMemorySize(seg));
                    trace(" to ");
                }

                setObjectMemorySize(seg, expectedObjectMemorySize);
                setObjectPartitionEnd(seg, seg + expectedObjectMemorySize);

                if (getTraceGC()) {
                    traceInt(getObjectMemorySize(seg));
                    trace(" (segment size = ");
                    traceInt(getCurrentSegmentSize());
                    traceln(")");
                }
            }

            assume(getObjectPartitionFree(seg) < getObjectPartitionEnd(seg));

            setIdealYoungGenerationSize();
            if ($romizing) {
                setObjectPartitionEnd(seg,  getObjectPartitionStart(seg) + roundup4(idealYoungGenerationSize));
                setObjectPartitionFree(seg, getObjectPartitionStart(seg));
            }

            if (getTraceGC()) {
                trace("Lisp2Collector initialised ");
                traceln("");
                trace("    Object memory start = "); traceInt(getObjectMemoryStart(seg));   traceln("");
                trace("    Object memory end   = "); traceInt(getObjectMemoryEnd(seg));     traceln("");
                trace("    Marking stack       = "); traceInt(getObjectMemoryEnd(seg));     traceln("");
                trace("    Bitmap              = "); traceInt(getObjectMemoryEnd(seg) + MINMARKSTACKSIZE);  traceln("");
                traceln("");
            }

            WriteBarrier_init(
                               getCurrentSegment(),
                               getCurrentSegmentSize(),
                               getObjectMemoryStart(seg),
                               getObjectMemoryEnd(seg),
                               getObjectMemoryEnd(seg) + MINMARKSTACKSIZE,
                               bitmapSizeRef[0]
                             );
        }


/*-----------------------------------------------------------------------*\
 *                               Mark Phase                              *
\*-----------------------------------------------------------------------*/


        /**
         * remarkObject
         */
/*MAC*/ void remarkObject(int $oop) {
            assume(testOopBit($oop));
            if (getTraceGCVerbose()) {
                trace("markObject ");
                traceInt($oop);
                trace(" klass ");
                trace_className(Object_getClass($oop));
                traceln("");
            }
            if (recurseLevel() == 0) {
                pushOnMarkStack($oop);
                updateHighWaterMark();
            } else {
                recurseLevelDec();
                traverseObject($oop, Object_getClass($oop), MARKOBJECTS);
                while (($oop = popOffMarkStack()) != 0) {
                    traverseObject($oop, Object_getClass($oop), MARKOBJECTS);
                }
                recurseLevelInc();
            }
        }


        /**
         * markObject
         */
/*MAC*/ void markObject(int $oop) {
            if (getTraceGCVerbose() && $oop > getCollectionEnd()) {
                trace("markObject ");
                traceInt($oop);
                trace(" is above end of collection space ");
                traceInt(getCollectionEnd());
                traceln("");
            }

           /*
            * Ensure that the object is in range and not forwarded
            */
            assume($oop <= getCollectionEnd());
            assume(!Object_isForwarded($oop));

           /*
            * If the object is in the collection space and the corresponding bit
            * is not set then set it and traverse the objects pointers
            */
            if (inCollectionSpace($oop) && !testAndSetOopBit($oop)) {
                remarkObject($oop);
            }
        }


        /**
         * Set the mark bit for a specified object, 'oop'. If 'addr' is non-zero
         * then 'oop' is an object that has potentially just been migrated to
         * persistent memory and 'addr' and 'wordOffset' denote a field
         * (in another object) that refers to 'oop'. If 'oop' was migrated, then
         * the field is simply updated to reflect the destination of the
         * migration and no marking is done.
         *
         * If the mark bit is set and this is the first time it was set for
         * 'oop' then the marking recurses on the pointer fields of 'oop'.
         *
         * @param addr
         * @param wordOffset
         * @param oop
         */
/*MAC*/ void markOrForwardObject(int $addr, int $wordOffset, int $oop) {

           /*
            * Ensure the address is non zero and that object is the value in that field.
            */
            assume($addr != 0);
            assume(getWord($addr, $wordOffset) == $oop);

           /*
            * If the object is forwarded to an eeprom address then rewrite it.
            * Otherwise mark the object
            */
            if (Object_isForwarded($oop)) {
                if (getTraceGCVerbose()) {
                    trace("markOrForwardObject ");
                    traceInt($addr);
                    trace(" % ");
                    traceInt($wordOffset);
                    trace(" rewritten from ");
                    traceInt($oop);
                    trace(" -> ");
                    traceInt(Object_getForwardedObject($oop));
                    traceln("");
                }
                assume(inRam($oop));
                assume(inEeprom(Object_getForwardedObject($oop)));
                setWord($addr, $wordOffset, Object_getForwardedObject($oop));
            } else {
                markObject($oop);
            }
        }


        /**
         * markOverflowCheck
         */
/*MAC*/ void markOverflowCheck() {
            int oop;
            while (markStackOverflow()) {
                if (getTraceGCVerbose()) {
                    trace("Mark overflow scan...");
                }
                incMarkStackOverFlows();
                resetMarkStackOverflow();

                startIteration(getCollectionStart(), getCollectionEnd());
                while ((oop = getNextIteration()) != 0) {
                    remarkObject(oop);
                    iterate();
                }
                endIteration();
            }
        }

        /**
         * mark
         */
        void mark() {
            long time = getStartTime();
            if (getTraceGCVerbose()) {
                trace("mark getCollectionStart() = ");
                traceInt(getCollectionStart());
                trace(" getCollectionEnd() ");
                traceInt(getCollectionEnd());
                traceln("");
            }

            markStackSetup(getCollectionEnd(), getCurrentObjectMemoryEnd() + MINMARKSTACKSIZE);

            clearOopBitRange(getCollectionStart(), getCollectionEnd());
            if (getTraceGCVerbose()) {
                traceBitmap();
            }

            traverseRoots(MARKOBJECTS);
            traverseWriteBarrierOops(MARKOBJECTS);
            markStackChunks();
            markOverflowCheck();

            processAssociationQueues();
            checkAssociationQueues();
            markOverflowCheck();

            if (getTraceGCVerbose()) {
                traceBitmap();
            }

            markStackFinish();

            incMarkTimer(time);
        }

/*-----------------------------------------------------------------------*\
 *                           Heap Verification                           *
\*-----------------------------------------------------------------------*/

//IFC//#ifndef PRODUCTION

        /**
         * reverifyObject
         */
/*MAC*/ void reverifyObject(int $oop) {
            int klass = Object_getClass($oop);
            if (getTraceGCVerbose()) {
                trace("verifyObject ");
                traceInt($oop);
                trace(" klass ");
                trace_className(klass);
                traceln("");
            }

            assume(testOopBit($oop));

            if (recurseLevel() == 0) {
                pushOnMarkStack($oop);
            } else {
                recurseLevelDec();
                traverseObject($oop, klass, VERIFYOBJECTS);
                while (($oop = popOffMarkStack()) != 0) {
                    traverseObject($oop, Object_getClass($oop), VERIFYOBJECTS);
                }
                recurseLevelInc();
            }
        }

        /**
         * verifyObject
         */
/*MAC*/ void verifyObject(int $addr, int $wordOffset, int $oop) {
            if (getTraceGCVerbose()) {
                trace("verifing ");
                traceInt($addr);
                trace(" % ");
                traceInt($wordOffset);
                trace(" = ");
                traceInt($oop);
                traceln("");
            }
            assume(inRom($oop) || inEeprom($oop) || inRam($oop));
            assume($oop <= getCollectionEnd());
            if ($oop > getCurrentObjectMemoryStart() && !testAndSetOopBit($oop)) {
                reverifyObject($oop);
            }
        }

        /**
         * verifyOverflowCheck
         */
/*MAC*/ void verifyOverflowCheck() {
            int oop;
            while (markStackOverflow()) {
                resetMarkStackOverflow();
                if (getTraceGCVerbose()) {
                    trace("Verify overflow scan...");
                }
                startIteration(getCollectionStart(), getCollectionEnd());
                while ((oop = getNextIteration()) != 0) {
                    reverifyObject(oop);
                    iterate();
                }
                endIteration();
            }
        }

        /**
         * verifyHeap
         */
        void verifyHeap() {
            clearAllOopBits();
            markStackSetup(getCollectionEnd(), getCurrentObjectMemoryEnd() + MINMARKSTACKSIZE);
            traverseRoots(VERIFYOBJECTS);
            verifyOverflowCheck();
            markStackFinish();
            clearAllOopBits();
        }


//IFC//#else
//IFC//#define verifyObject(addr, wordOffset, oop) /**/
//IFC//#define verifyHeap() /**/
//IFC//#endif


/*-----------------------------------------------------------------------*\
 *                           Pointer forwarding                          *
\*-----------------------------------------------------------------------*/

        private int rcnMask;
        private int fwdShift;
        private int fwdDelta;

        /**
         * setupForwarding
         */
/*MAC*/ void setupForwardingParameters() {
            int bits = 32;
            int test = 0x80000000;
            int mask = 0xFFFFFFFF;

            int max = getMaximumRcn();
            assume(max != 0);

            while (true) {
                if ((max & test) != 0) {
                    break;
                }
                test = srl(test, 1);
                mask = srl(mask, 1);
                bits--;
            }

            rcnMask  = mask;
            fwdShift = bits;
            fwdDelta = ((1 << (32 - bits - compactArrayBitCount())) - 1) * bytesPerWord() ;

            if (getTraceGC()) {
                traceln("setupForwardingParameters()");
                trace("    Max RCN  = "); traceInt(max);      traceln("");
                trace("    rcnMask  = "); traceInt(rcnMask);  traceln("");
                trace("    fwdShift = "); traceInt(fwdShift); traceln("");
                trace("    fwdDelta = "); traceInt(fwdDelta); traceln("");
            }
        }

        /**
         * forwardObject
         */
/*MAC*/ void forwardObject(int $oop, int $delta) {
            int rcn = getRcnFromKlass(Object_getClass($oop));
            assume((rcn & ~rcnMask) == 0);
            assume($delta <= fwdDelta);
            assume(($delta & 3) == 0);
            $delta /= bytesPerWord();
            Object_setEncodedLowBits($oop, ($delta << fwdShift) | rcn);
        }

        /**
         * getForwardOffset
         */
/*MAC*/ int getForwardOffset(int $oop) {
            return (Object_getEncodedLowBits($oop) >> fwdShift) * bytesPerWord();
        }

        /**
         * getForwardedAddress
         */
/*MAC*/ int getForwardedAddress(int $oop) {
            if (getTraceGC() && inCollectionSpace($oop) && !isMarked($oop)) {
                trace("Unmarked object inCollectionSpace needed for forwarding ");
                traceInt($oop);
                traceln("");
            }
            assume(!inCollectionSpace($oop) || isMarked($oop));
            return inCollectionSpace($oop) ? $oop - getForwardOffset($oop) : $oop;
        }

        /**
         * getForwardedKlass
         */
/*MAC*/ int getForwardedKlass(int $oop) {
            return getKlassFromRcn(Object_getEncodedLowBits($oop) & rcnMask);
        }

        /**
         * unforwardObject
         */
/*MAC*/ void unforwardObject(int $oop) {
            int klass = getForwardedKlass($oop);
            Object_setClass($oop, klass);
        }

        /**
         * updatePointer
         */
/*MAC*/ void updatePointer(int $addr, int $wordOffset, int $oop) {
            if (getTraceGCVerbose()) {
                trace("updating ");
                traceInt($addr);
                trace(" % ");
                traceInt($wordOffset);
                trace(" from ");
                traceInt(getWord($addr, $wordOffset));
                trace(" -> ");
                traceInt($oop);
                traceln("");
            }
            assumeIsValidOop($oop);
            setWord($addr, $wordOffset, $oop);
        }


/*-----------------------------------------------------------------------*\
 *                         Compute Address Phase                         *
\*-----------------------------------------------------------------------*/


        /**
         * computeAddresses
         */
        boolean computeAddresses() {
            long time = getStartTime();
            int free = getCollectionStart();
            int oop;
            boolean holeInHeap = false;

            startIteration(free, getCollectionEnd());

            while ((oop = getNextIteration()) != 0) {
                int headerSize, delta, length;

///*IFJ*/traceln(" oop="+oop+" free="+free);

                headerSize = oop - Object_oopToBlock(oop);

///*IFJ*/traceln(" oop="+oop+" free="+free+" headerSize="+headerSize);

                delta      = oop - (free + headerSize);
                length     = Object_getObjectLength(oop);

                if (delta > fwdDelta) {
                     delta = fwdDelta;
                     holeInHeap = true;
                }

                if (getTraceGCVerbose()) {
                    trace("computeAddress ");
                    traceInt(oop);
                    trace(" klass ");
                    trace_className(Object_getClass(oop));
                    trace(" length ");
                    traceInt(length);
                    trace(" delta ");
                    traceInt(delta);
                    traceln("");
                }

                forwardObject(oop, delta);
                free = oop - delta + length;

                iterate();
            }

            endIteration();

            incComputeAddressTimer(time);

            return holeInHeap;
        }


/*-----------------------------------------------------------------------*\
 *                          Update Pointer Phase                         *
\*-----------------------------------------------------------------------*/

        void updatePointers() {
            int oop;

            long time = getStartTime();

            updateStackChunks();

            /* Ignore the associations hash table if it hasn't been allocated yet*/
            if (getAssociationHashtable() != 0) {
                updateAssociationPointers();
            }
            traverseWriteBarrierOops(UPDATEPOINTERS);
            traverseRoots(UPDATEPOINTERS);

            startIteration(getCollectionStart(), getCollectionEnd());
            while ((oop = getNextIteration()) != 0) {
                traverseObject(oop, getForwardedKlass(oop), UPDATEPOINTERS);
                iterate();
            }
            endIteration();

            incUpdatePointerTimer(time);
        }


/*-----------------------------------------------------------------------*\
 *                            Relocate Phase                             *
\*-----------------------------------------------------------------------*/

        void relocate() {

            long time = getStartTime();

            int oop;
            int target = 0;
            startIteration(getCollectionStart(), getCollectionEnd());

            while ((oop = getNextIteration()) != 0) {
                int oopLength, headerSize;

                target = getForwardedAddress(oop);
                unforwardObject(oop);

                oopLength  = Object_getObjectLength(oop);
                headerSize = oop - Object_oopToBlock(oop);

                if (oop != target) {
                    if (getTraceGCVerbose()) {
                        trace("relocating  ");
                        traceInt(oop);
                        trace(" (klass ");
                        trace_className(Object_getClass(oop));
                        trace(") to ");
                        traceInt(target);
                        traceln("");
                    }
                    copyBytes(oop - headerSize, target - headerSize, oopLength + headerSize);
                }
                iterate();
            }

            endIteration();

           /*
            * Work out where the next free point is and set it
            */
            if (target == 0) {
                target = getCollectionStart();
            } else {
                target += Object_getObjectLength(target);
            }
            setCurrentObjectPartitionFree(target);
            if (getTraceGC()) {
                trace("setCurrentObjectPartitionFreePtr = ");
                traceInt(target);
                traceln("");
            }

           /*
            * Work out how much memory to be used before next gc.
            */
            target += getFailedGCLength() + roundup4(idealYoungGenerationSize);
            if (target >= getCurrentObjectMemoryEnd() || target < 0) {
                target = getCurrentObjectMemoryEnd();
            }

           /*
            * Bump the high water mark if necessary
            */
            if (target > heapHWM) {
                heapHWM = target;
                if (getTraceGC()) {
                    trace("heapHWM = ");
                    traceInt(heapHWM);
                    traceln("");
                }
            }

           /*
            * If in minimim heap mode then let the target grow to the high water mark
            */
            if (minHeapMode && (heapHWM > target)) {
                target = heapHWM;
            }

           /*
            * Set the end of the partition
            */
            setCurrentObjectPartitionEnd(target);
            if (getTraceGC()) {
                trace("setCurrentObjectPartitionEnd = ");
                traceInt(target);
                traceln("");
                trace("getFailedGCLength = ");
                traceInt(getFailedGCLength());
                traceln("");
            }


           /*
            * Finally rebuild the associations so they hash to the correct
            * addresses. This step is skipped if the associations hash table
            * hasn't been allocated yet
            */
           if (getAssociationHashtable() != 0) {
               rebuildAssociationQueues();
           }

            incRelocateTimer(time);
        }


/*-----------------------------------------------------------------------*\
 *                              Traversals                               *
\*-----------------------------------------------------------------------*/


        /**
         * traverseFunction
         */
        void traverseFunction(int addr, int wordOffset, int ftn) {
            int oop = getWord(addr, wordOffset);
///*IFJ*/ traceln("traverseFunction addr="+addr+" wordOffset="+wordOffset+" oop="+oop);
            if (oop != 0) {
                switch (ftn) {
                    case MARKOBJECTS: {
                        markOrForwardObject(addr, wordOffset, oop);
                        break;
                    }
                    case UPDATEPOINTERS: {
                        updatePointer(addr, wordOffset, getForwardedAddress(oop));
                        break;
                    }
//IFC//#ifndef PRODUCTION
                    case VERIFYOBJECTS: {
                        verifyObject(addr, wordOffset, oop);
                        break;
                    }
//IFC//#endif
                    default: shouldNotReachHere();
                }
            }
        }


        /**
         * traverseRoots
         */
        void traverseRoots(int ftn) {
            int i;
            for (i = 0; i != ROOT_SIZE; i++) {
                if (ftn == MARKOBJECTS && i == ROOT_associationHashtable) {
                    continue;  /* Do not mark object associations here. */
                }
                if (getTraceGCVerbose()) {
                    trace("*** traverseRoots addr=");
                    traceInt((getCurrentSegment()+((MSR_roots + i)*bytesPerWord())));
                    trace(" value=");
                    traceInt(getWord(getCurrentSegment(), MSR_roots + i));
                    traceln("");
                }
                traverseFunction(getCurrentSegment(), MSR_roots + i, ftn);
            }
        }


        /**
         * traverseWriteBarrierOops
         */
        void traverseWriteBarrierOops(int ftn) {
            int addr;
            startIteration(getCurrentObjectMemoryStart(), getCollectionStart());
            while ((addr = getNextIteration()) != 0) {
                boolean skip = false;
                if (getTraceGCVerbose()) {
                    trace("*** traverseWriteBarrierOops addr=");
                    traceInt(addr);
                    trace(" value=");
                    traceInt(getWord(addr, 0));
                    traceln("");
                }

                if (ftn == MARKOBJECTS) {
                    int oop = getWord(addr, 0);
                    if (oop != 0) {
                        if (Object_isForwarded(oop)) {
                            /*
                             * The write barrier bit must be unset for this pointer
                             * that points to a migrated object so that it is
                             * not re-updated in the update pointer phase.
                             */
                            WriteBarrier_clear(addr, 0);
                        } else if (assuming()) {
                            int cls = Object_getClass(oop);
                            if (Class_getType(cls) == getAssociationCno()) {
                                skip = true;
                                if (getTraceGCVerbose()) {
                                    trace(
                                        "traverseWriteBarrierOops -- skipped association at ");
                                    traceInt(addr);
                                    traceln("");
                                }
                            }
                        }
                    }
                }

                if (!skip) {
                    traverseFunction(addr, 0, ftn);
                }
                iterate();
            }

            endIteration();
        }


        /**
         * Update the pointers of an object based on an oop map.
         *
         * @param object
         * @param map
         * @param fieldCount The number of fields in the object to
         * be updated using the map.
         */
        void traverseFunctionFromOopMap(int object, int map, int fieldCount, int ftn) {
            int i;
            int mapBytes = (fieldCount+7)/8;
            int offset = 0;
            for (i = 0 ; i < mapBytes ; i++) {
                int mapbyte = getUnsignedByte(0, map+i);
                int j;
                for (j = 0; j != 8 && offset != fieldCount; ++j, ++offset) {
                    if ((mapbyte & (1<<j)) != 0) {
                        traverseFunction(object, offset, ftn);
                    }
                }
            }
        }

        /**
         * Update a pointer that points to somewhere inside 'oldObject' to
         * point to the same place in 'newObject'.
         * @param oldObject An object which has an internal pointer.
         * @param newObject The copy of oldObject.
         * @param addr The internal pointer in oldObject.
         * @return the pointer now pointing into the same place in 'newObject'.
         */
/*MAC*/ private int updateInternalOop(int $oldObject, int $newObject, int $addr) {
            return $newObject + ($addr - $oldObject);
        }


        /**
         * traverseStackChunk
         */
        void traverseStackChunk(int oop, int ftn) {
            int target = 0;
            int lp     = StackChunk_getFirstLp(oop);
            int lastLp = StackChunk_getLastLp(oop);

           /*
            * Don't update stack chunks in this function
            */
            if (ftn == UPDATEPOINTERS) {
                return; /* Done separately */
            }

           /*
            * Trace
            */
            if (getTraceGCVerbose()) {
                trace("+++ traverseStackChunk ");
                traceInt(oop);
                traceln("");
            }

           /*
            * Check that if the next chunk exists it is being used
            */
            if (assuming()) {
                int nextChunk = StackChunk_getNext(oop);
                if (nextChunk != 0) {
                    assume(StackChunk_getLastLp(nextChunk) != 0);
                }
            }

           /*
            * Iterate through each frame
            */
            while (lp <= lastLp) {
                int mp = Frame_getCurrentMp(lp);
                int relativeIp = Frame_getCurrentIp(lp) - mp;

                if (getTraceGCVerbose()) {
                    trace("traverseStackChunk lp=");
                    traceInt(lp);
                    traceln("");
                }

                traverseFunction(lp, FRAME_currentCp, ftn);
                traverseFunction(lp, FRAME_currentMp, ftn);

               /*
                * Update parameters and locals
                */
                if (relativeIp == getUnsignedByte(mp, MTH_headerSize)) {
                   /*
                    * Before first instruction
                    */
                    int fieldCount = getUnsignedByte(mp, MTH_numberOfParms);
                    if (getTraceGCVerbose()) {
                        trace("traverseStackChunk about to verify/mark parameters of method ");
                        trace_methodID(mp);
                        trace(" fieldCount ");
                        traceInt(fieldCount);
                        traceln("");
                    }
                    traverseFunctionFromOopMap(lp, mp + MTH_oopMap, fieldCount, ftn);

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
                    if (getTraceGCVerbose()) {
                        trace("traverseStackChunk about to verify/mark parameters & locals of method ");
                        trace_methodID(mp);
                        trace(" fieldCount ");
                        traceInt(fieldCount);
                        traceln("");
                    }
                    traverseFunctionFromOopMap(lp, mp + MTH_oopMap, fieldCount, ftn);

                   /*
                    * Advance to next frame in chunk
                    */
                    lp = lp + Frame_getStackOffset(lp)+w2b(1);
                }
            }

            traverseFunction(oop, STACK_list, ftn);
            traverseFunction(oop, STACK_next, ftn);
            traverseFunction(oop, STACK_prev, ftn);

           /*
            * Trace
            */
            if (getTraceGCVerbose()) {
                trace("--- traverseStackChunk ");
                traceInt(oop);
                traceln("");
            }
        }


        /**
         * markStackChunks
         */
        void markStackChunks() {
            int chunk = getStackChunkList();
            if (getTraceGCVerbose()) {
                traceln("markStackChunks()");
            }
            assume(chunk != 0);
            while (chunk != 0) {
                int next = StackChunk_getList(chunk);
                if (!inCollectionSpace(chunk)) {
                    traverseStackChunk(chunk, MARKOBJECTS);
                } else {
                    markObject(chunk);
                }
                chunk = next;
            }
        }


        /**
         * updateStackChunk
         */
        void updateStackChunk(int oop) {
            int target = 0;
            int lp     = StackChunk_getFirstLp(oop);
            int lastLp = StackChunk_getLastLp(oop);

            boolean marked = inCollectionSpace(oop) && isMarked(oop);

            if (getTraceGCVerbose()) {
                trace("+++ updateStackChunk ");
                traceInt(oop);
                traceln("");
            }

           /*
            * If marked then make lastLp absolute
            */
            if (marked) {
                target = getForwardedAddress(oop);
                StackChunk_setLastLp(oop, updateInternalOop(oop, target, lastLp));
            }

           /*
            * Iterate through each frame
            */
            while (lp <= lastLp) {
                int mp = Frame_getCurrentMp(lp);
                int relativeIp = Frame_getCurrentIp(lp) - mp;

                if (getTraceGCVerbose()) {
                    trace("updateStackChunk lp=");
                    traceInt(lp);
                    traceln("");
                }

                traverseFunction(lp, FRAME_currentCp, UPDATEPOINTERS);
                traverseFunction(lp, FRAME_currentMp, UPDATEPOINTERS);

               /*
                * Update previous frame pointer and ip address
                */
                if (marked) {
                    int plp = Frame_getPreviousLp(lp);
                    if (plp != 0) {
                        Frame_setPreviousLp(lp, updateInternalOop(oop, target, plp));
                    }
                }
                Frame_setCurrentIp(lp, relativeIp + mp);

               /*
                * Update parameters and locals
                */
                if (relativeIp == getUnsignedByte(mp, MTH_headerSize)) {
                   /*
                    * Before first instruction
                    */
                    int fieldCount = getUnsignedByte(mp, MTH_numberOfParms);
                    if (getTraceGCVerbose()) {
                        trace("updateStackChunk about to update parameters of method ");
                        trace_methodID(mp);
                        trace(" fieldCount ");
                        traceInt(fieldCount);
                        traceln("");
                    }
                    traverseFunctionFromOopMap(lp, mp + MTH_oopMap, fieldCount, UPDATEPOINTERS);

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
                    if (getTraceGCVerbose()) {
                        trace("updateStackChunk about to update parameters & locals of method ");
                        trace_methodID(mp);
                        trace(" fieldCount ");
                        traceInt(fieldCount);
                        traceln("");
                    }
                    traverseFunctionFromOopMap(lp, mp + MTH_oopMap, fieldCount, UPDATEPOINTERS);

                   /*
                    * Advance to next frame in chunk
                    */
                    lp = lp + Frame_getStackOffset(lp)+w2b(1);
                }
            }

            traverseFunction(oop, STACK_list, UPDATEPOINTERS);
            traverseFunction(oop, STACK_next, UPDATEPOINTERS);
            traverseFunction(oop, STACK_prev, UPDATEPOINTERS);

            if (getTraceGCVerbose()) {
                trace("--- updateStackChunk ");
                traceInt(oop);
                traceln("");
            }
        }


        /**
         * removeDeadStackChunks
         */
        void removeDeadStackChunks() {
            int live = 0;
            int chunk = getStackChunkList();
            setStackChunkList(0);

            while (chunk != 0) {
                int next = StackChunk_getList(chunk);
                StackChunk_setList(chunk, 0);

                if (StackChunk_getLastLp(chunk) == 0) {
                    if (getTraceGCVerbose()) {
                        trace("removeDeadStackChunks found dead chunk ");
                        traceInt(chunk);
                        traceln("");
                    }
                } else {
                    int nextChunk;
                    if (getTraceGCVerbose()) {
                        trace("removeDeadStackChunks found live chunk ");
                        traceInt(chunk);
                        traceln("");
                    }
                    nextChunk = StackChunk_getNext(chunk);
                    if (nextChunk != 0 && StackChunk_getLastLp(nextChunk) == 0) {
                        if (getTraceGCVerbose()) {
                            trace("removeDeadStackChunks found dead next chunk ");
                            traceInt(nextChunk);
                            traceln("");
                        }
                        StackChunk_setNext(chunk, 0);
                    }
                    if (assuming()) {
                        int prevChunk = StackChunk_getPrev(chunk);
                        if (prevChunk != 0) {
                            assume(StackChunk_getLastLp(prevChunk) != 0);
                        }
                    }
                    StackChunk_setList(chunk, getStackChunkList());
                    setStackChunkList(chunk);
                    live++;
                }
                chunk = next;
            }
            if (getTraceGC()) {
                trace("Total live stack chunks = ");
                traceInt(live);
                traceln("");
            }
        }


        /**
         * updateStackChunks
         */
        void updateStackChunks() {
            int chunk = getStackChunkList();
            if (getTraceGCVerbose()) {
                traceln("updateStackChunks()");
            }
            assume(chunk != 0);
            while (chunk != 0) {
                int next = StackChunk_getList(chunk);
                updateStackChunk(chunk);
                chunk = next;
            }
        }


        /**
         * traverseObject
         */
        void traverseObject(int oop, int klass, int ftn) {

            if (getTraceGCVerbose()) {
                trace("traverseObject for oop ");
                traceInt(oop);
                trace(" klass ");
                trace_className(klass);
                traceln("");
            }

           /*
            * Note -- No need to mark/update the class pointer because
            *         this is always in ROM/EEPROM and does not move.
            */

           /*
            * Deal with arrays
            */
            if (Class_isArrayClass(klass)) {
                int cno = Class_getType(klass);
                switch (cno) {

                   /*
                    * Primitive arrays
                    */
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

                   /*
                    * Stack chunks
                    */
                    case CNO.LOCAL_ARRAY: {
                        traverseStackChunk(oop, ftn);
                        break;
                    }

                   /*
                    * Class state objects
                    */
                    case CNO.GLOBAL_ARRAY: {
                        int count, i;
                        int csKlass = ClassState_getClass(oop);
                        assume(csKlass != 0);
                        count = Class_getPointerStaticFieldsLength(csKlass) + CLS_STATE_offsetToFields;
                        for (i = 0 ; i < count ; i++) {
                            traverseFunction(oop, i, ftn);
                        }
                        break;
                    }

                   /*
                    * Arrays of references
                    */
                    default: {
                        int i;
                        int arrayCount = Object_getArrayElementCountNotAssuming(oop);
                        for (i = 0 ; i < arrayCount ; i++) {
                            traverseFunction(oop, i, ftn);
                        }
                        break;
                    }
                }

            } else {

               /*
                * Non arrays
                */
                int map        = Class_getOopMap(klass);
                int fieldCount = Class_getInstanceFieldsLength(klass);
                assume(Object_getArrayElementCount(map) == ((fieldCount+7)/8));
                traverseFunctionFromOopMap(oop, map, fieldCount, ftn);
            }
        }



        /**
         * updateObjectIfNotMarked
         */
        void updateObjectIfNotMarked(int oop) {
            assume(oop != 0);
            if (inCollectionSpace(oop)) {
                assume(isMarked(oop));
            } else {
                int i;
                int wordSize = Object_getObjectLength(oop) / bytesPerWord();
                traverseObject(oop, Object_getClass(oop), UPDATEPOINTERS);
                /*
                 * Clear any write barrier bits set for pointers in this object
                 * so they are not updated twice.
                 */
                for (i = 0; i != wordSize; ++i) {
                    WriteBarrier_clear(oop, i);
                }
            }
        }



/*-----------------------------------------------------------------------*\
 *                              Main Functions                           *
\*-----------------------------------------------------------------------*/

        boolean lastCollectionWasFull;

        /**
         * Perform a collection of a current object partition of the heap. The
         * boundaries of this partition are set by the caller and will determine
         * whether or not this is a collection of the whole heap or only a
         * partial collection. More specifially, if the start of the current
         * object partition equals the start of the heap, then this is a full
         * GC.
         *
         * @return true if there is sufficient memory free memory after the
         * collection such that the last failed allocation attempt will now
         * succeed.
         */
        boolean gcCurrentObjectPartition() {
            boolean lastCollectionWasFull = getCurrentObjectPartition() == getCurrentObjectMemoryStart();
            boolean holeInHeap = true;

            if (lastCollectionWasFull) {
                incFullGCs();
                if (getTraceGC()) {
                    traceln("Full GC");
                }
            } else {
                incPartialGCs();
                if (getTraceGC()) {
                    traceln("Partial GC");
                }
            }

//if (getTraceGC()) {
//    traceln("Heap trace before GC");
//    traceHeap(RAM, true);
//}
            setInCollector(true);
            while (holeInHeap) {
                mark();
                holeInHeap = computeAddresses();
                updatePointers();
                relocate();
                if (holeInHeap) {
                    incIncompleteRelocations();
                    if (getTraceGC()) {
                        traceln("Extra GC needed because of incomplete compaction");
                    }
                }
            }
            setCurrentObjectPartition(getCurrentObjectPartitionFree());
            clearOopBitRange(getCurrentObjectMemoryStart(), getCollectionStart());
//if (getTraceGC()) {
//    traceln("Heap trace after GC");
//    traceHeap(RAM, true);
//}
            verifyHeap();

            setInCollector(false);

            if (freeMem() >= getFailedGCLength()) {
                if (getTraceGC()) {
                    trace("Sucessfully recovered ");
                    traceInt(getFailedGCLength());
                    traceln(" bytes");
                }
                return true;
            } else {
                if (getTraceGC()) {
                    trace("Failed to recover ");
                    traceInt(getFailedGCLength());
                    traceln(" bytes");
                }
                return false;
            }
        }


        /**
         * gcPrim1
         */
/*MAC*/ boolean gcPrim1() {
            boolean ok;
            if (getFullGC() == false) {
/*if[VM.GCSPY]*/
/*IFJ*/         gcspyEvent(GCSPY_EVENT_START_PARTIAL_GC);
/*end[VM.GCSPY]*/
                ok = gcCurrentObjectPartition();
/*if[VM.GCSPY]*/
/*IFJ*/         gcspyEvent(GCSPY_EVENT_END_PARTIAL_GC);
/*end[VM.GCSPY]*/
                if (lastCollectionWasFull) {
                    return ok;
                }
                if (ok) {
                    /*
                     * Do a full GC if there is less than the memory required
                     * for the ideal sized young generation. This prevents
                     * a large increase in the frequency of collections as the
                     * young generation becomes constrained by the top of the
                     * heap. Doing a full collection sooner in this case will
                     * result in fewer collections.
                     */
                    int freeMemory = freeMem();
                    int minimum = getFailedGCLength() + (roundup4(idealYoungGenerationSize));
                    if (freeMemory >= minimum) {
                        return ok;
                    }
                }
            }
            setCurrentObjectPartition(getCurrentObjectMemoryStart());
/*if[VM.GCSPY]*/
/*IFJ*/         gcspyEvent(GCSPY_EVENT_START_FULL_GC);
/*end[VM.GCSPY]*/
            ok = gcCurrentObjectPartition();
/*if[VM.GCSPY]*/
/*IFJ*/     gcspyEvent(GCSPY_EVENT_END_FULL_GC);
/*end[VM.GCSPY]*/
            return ok;
        }


        /**
         * gcPrim
         */
        boolean gcPrim() {
            boolean needFullCollection = getTraceGCSummary() || getExcessiveGCEnabled();
            boolean res = gcPrim1();
            if (!lastCollectionWasFull && needFullCollection) {
                setCurrentObjectPartition(getCurrentObjectMemoryStart());
                res = gcPrim1();
            }
            return res;
        }


        /**
         * gc
         */
        boolean gcRam() {

            boolean res;
            int before = 0;

            long time = getStartTime();

            assume(!romizing());

            if (getTraceGC()) {
                traceln("Garbage collecting ...");
                traceInstructionCount(true);
                if (getTraceGCVerbose()) {
                    trace("currentObjectMemoryStart       "); traceHex(getCurrentObjectMemoryStart());     traceln("");
                    trace("currentObjectMemoryEnd         "); traceHex(getCurrentObjectMemoryEnd());       traceln("");
                    trace("currentObjectPartition         "); traceHex(getCurrentObjectPartition());       traceln("");
                    trace("currentObjectPartitionFreePtr  "); traceHex(getCurrentObjectPartitionFree());   traceln("");
                    trace("currentObjectPartitionEnd      "); traceHex(getCurrentObjectPartitionEnd());    traceln("");
                }
                before = freeMem();
            }

            setupForwardingParameters();
            removeDeadStackChunks();

            res = gcPrim();

            if (getTraceGC()) {
                int freeMemory = freeMem();
                int totalMemory = totalMem();
                trace("Collected ");
                traceInt(freeMemory - before);
                trace(" bytes of garbage (");
                traceInt(freeMemory);
                trace("/");
                traceInt(totalMemory);
                trace(" [");
                traceInt((freeMemory*100)/totalMemory);
                traceln("%] bytes free)");
            }

            justDoneGC = true;
            incCollectionCount();

            if (getTraceGCSummary()) {
                traceHeapSummary(getCurrentSegment());
            }

            incTotalTimer(time);

            return res;
        }


/*-----------------------------------------------------------------------*\
 *                           Forwarded functions                         *
\*-----------------------------------------------------------------------*/

        int KeepObject(int oop) {
            markObject(oop);
            return oop;
        }

        boolean IsMarked(int oop) {
            if (inCollectionSpace(oop)) {
                return isMarked(oop);
            } else {
                return false;
            }
        }

        boolean IsAlive(int oop) {
            return isAlive(oop);
        }

        int GetPossiblyForwarded(int obj) {
            return obj;
        }

/*-----------------------------------------------------------------------*\
 *                           Memory statistics                           *
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

    final class GCspyHeapDriver {

        private final ServerSpace heapSpace;
        private final int heapSpaceID;

        private final int tileCount;
        private final int startAddr;
        private final int endAddr;
        private final int tileSize;

        private int objects[];
        private int usedSpace[];
        private int writeBarrierTable[];

        private int objectsSummary[];
        private int usedSpaceSummary[];
        private int writeBarrierTableSummary[];

        private Stream objectsStream;
        private Stream usedSpaceStream;
        private Stream writeBarrierTableStream;

        /**
         * Get the index of the tile containing a given address.
         * @param addr
         * @return the index of the tile containing 'addr'.
         */
        private int getTile(int addr) {
            int diff = addr - startAddr;
            return (diff / tileSize);
        }

        /**
         * Get the first address which lies within a given tile.
         * @param tile The tile to inspect.
         * @return the first address which lies within 'tile'.
         */
        private int getStartAddrOfTile(int tile) {
            return startAddr + (tile * tileSize);
        }

        /**
         * Get the address one beyond the last address within a given tile.
         * @param tile The tile to inspect.
         * @return the address one beyond the last address within 'tile'.
         */
        private int getEndAddrOfTile(int tile) {
            return startAddr + ((tile+1) * tileSize);
        }

        /**
         * Update the tiles in the 'objects' and 'used space' streams.
         * @param spaceStart The start of the memory region for which the tiles
         * are to be updated.
         * @param spaceEnd The end of the memory region for which the tiles
         * are to be updated.
         */
        private void updateObjectAndUsedSpaceTiles(int spaceStart, int spaceEnd) {
            /* Iterate over all the objects in the heap. */
            int oop;
            if (spaceStart != spaceEnd) {
                for (oop = Object_blockToOop(spaceStart) ; oop != 0 ; ) {
                    /*
                     * The oop may be an object that was recently migrated from old
                     * space to persistent memory.
                     */
                    boolean forwarded = Object_isForwarded(oop);
                    int forwardedOop = 0;
                    if (forwarded) {
                        forwardedOop = Object_getForwardedObject(oop);
                        int klass = Object_getClass(forwardedOop);
                        Object_setClass(oop, klass);
                    }

                    int block = Object_oopToBlock(oop);
                    object(block, Object_getObjectLength(oop) + (oop - block));

                    if (forwarded) {
                        int oldOop = oop;
                        oop = Object_nextObject(oop, spaceStart, spaceEnd);
                        /* reset the forwarding pointer */
                        Object_forwardToObject(oldOop, forwardedOop);
                    } else {
                        oop = Object_nextObject(oop, spaceStart, spaceEnd);
                    }
                }
            }
        }

        /**
         * Update the tiles in the 'write barrier table' streams.
         * @param oldGenStart The start of the old generation for which the
         * write barrier is maintained.
         * @param oldGenEnd The end of the old generation for which the
         * write barrier is maintained.
         */
        private void updateWriteBarrierTableTiles(int oldGenStart, int oldGenEnd) {
            int addr;
            startIteration(oldGenStart, oldGenEnd);
            while ((addr = getNextIteration()) != 0) {
                int tile = getTile(addr);
                writeBarrierTable[tile] += 1;
                writeBarrierTableSummary[0] += 1;
                iterate();
            }
            endIteration();
        }

        /**
         * Reset all the tiles up to a given address to their default state.
         * @param address The address up to which all corresponding tiles are to
         * be reset
         * @return the number of tiles reset.
         */
        private int zero(int address) {
            /* maxAddr is the current max address used in the heap */
            int usedTileCount = getTile(address) + 1;

            /* this ensures that all data arrays in the streams are of the
               correct size - if they are, it leaves them alone - if they are not,
               it re-allocates them */
            heapSpace.setData(usedTileCount);

            /* get the data from the corresponding streams, just in case it
               got re-allocated during the setData call above */
            objects = objectsStream.getIntData();
            usedSpace = usedSpaceStream.getIntData();
            writeBarrierTable = writeBarrierTableStream.getIntData();

            /* this initialises the data of the streams to their default values,
               which is passed to the stream during initialisation - it also
               zeros the summary array.
               if necessary, the driver can always initialise these itself */
            heapSpace.resetData();

            /* this is the total space */
            usedSpaceSummary[1] = (endAddr - startAddr);

            return usedTileCount;
        }

        /**
         * Modify the 'objects' and 'usedSpace' streams to reflect the fact that
         * there is an object at a given address that has a given size.
         * @param addr The address at which the header of an object begins.
         * @param size The size of the object (in bytes) including the header.
         */
        private void object(int addr, int size) {
            int tile = getTile(addr);

            objects[tile] += 1;
            objectsSummary[0] += 1;

            /* Calculate used space */
            usedSpaceSummary[0] += size;
            int lastTile = getTile(addr + (size-1));
            while (tile <= lastTile) {
                int endAddrOfTile = getEndAddrOfTile(tile);
                int usedInTile = endAddrOfTile - addr;
                if (usedInTile < size) {
                    size = size - usedInTile;
                } else {
                    usedInTile = size;
                }

                assume(usedSpace[tile] + usedInTile <= tileSize);
                usedSpace[tile] += usedInTile;

                tile++;
                addr = getStartAddrOfTile(tile);
            }
        }

        /**
         * Initialise the names of the tiles based on the address ranges they
         * represent.
         * @param from
         * @param to
         */
        private void setupNames(int from, int to) {
            for (int i = from; i < to; ++i) {
                int start = startAddr + (i * tileSize);
                int end = start + tileSize;
                if (end > endAddr) {
                    end = endAddr;
                }
                heapSpace.setTileName(i, " [" + start + "-" + end + ")");
            }
        }

        /**
         * Set up the control stream that informs the client which tiles are
         * unused as well as where the boundaries of the young generation are.
         * @param event
         * @param usedTileCount
         */
        private void setControlInfo(int usedTileCount) {
            /* reset the control stream */
            heapSpace.startControl();
            if (usedTileCount < tileCount) {
                /* setup which tiles at the end are not used */
                heapSpace.setControlRange(Space.CONTROL_UNUSED,
                                      usedTileCount, tileCount - usedTileCount);
            }

            int firstTileInYoungGeneration = getTile(getCurrentObjectPartition());
            int lastTileInYoungGeneration = getTile(getCurrentObjectPartitionEnd());
            heapSpace.setControl(heapSpace.CONTROL_SEPARATOR, firstTileInYoungGeneration);
            heapSpace.setControl(heapSpace.CONTROL_SEPARATOR, lastTileInYoungGeneration);

            heapSpace.finishControl();
        }

        /**
         * The number of tiles that can be represented in the visual
         * GCspy client on a 1280x1024 screen without overflowing the
         * display space.
         */
        private static final int MAX_TILE_COUNT = 4500;

        /**
         * A rough guess as to the average object size for the purpose of
         * giving tiles in the 'objects' stream a reasonable upper limit
         * for number of objects in a tile.
         */
        private static final int AVG_OBJECT_SIZE = 20;

        /**
         * Calculate a tile size such that the total number of
         * tiles required to represent a contiguous memory of a
         * given size does not exceed a MAX_TILE_COUNT.
         * @param memorySize
         * @return
         */
        private int calculateTileSize(int memorySize) {
            int tileSize = 4;
            while ((memorySize / tileSize) > MAX_TILE_COUNT) {
                tileSize *= 2;
            }
            return tileSize;
        }

        /**
         * Communicate a GC event to a listening GCspy client.
         * @param event
         */
        void event(int eventID) {
                int spaceStart    = getCurrentObjectMemoryStart();
                int oldGenStart   = spaceStart;
                int oldGenEnd     = getCurrentObjectPartition();
                int youngGenStart = oldGenEnd;
                int youngGenEnd   = getCurrentObjectPartitionFree();

                /* this will appear in the Summary string */
                int youngGenSize = youngGenEnd - youngGenStart;
                int oldGenSize   = oldGenEnd   - oldGenStart;
                String spaceInfo = "Heap:       " + (endAddr - startAddr) + " bytes\n";
                spaceInfo +=       "Young gen:  " + youngGenSize + " bytes\n";
                spaceInfo +=       "Old gen:    " + oldGenSize + " bytes\n";
                spaceInfo += "\n";
                heapSpace.setSpaceInfo(spaceInfo);

                /* reset the tiles for the used part of memory */
                int usedTileCount = zero(youngGenEnd);

                /* update the 'objects' and 'used space' streams */
                updateObjectAndUsedSpaceTiles(spaceStart, youngGenEnd);

                /* update the 'write barrier table' stream */
                updateWriteBarrierTableTiles(oldGenStart, oldGenEnd);

                /* set up the control stream */
                setControlInfo(usedTileCount);
        }

        /**
         * Initialise a GCspy driver for the lisp2 collector.
         * @param interpreter
         * @param tileSize
         */
        GCspyHeapDriver(ServerInterpreter interpreter, int tileSize) {

            ColorDB colorDB = ColorDB.getColorDB();

            /* initialise memory boundaries represented by the heap space */
            this.startAddr = getCurrentObjectMemoryStart();
            this.endAddr   = getCurrentObjectMemoryEnd();
            int memorySize = getCurrentObjectMemorySize();

            /* initialise the tile size */
            if (tileSize < 0) {
                this.tileSize = tileSize = calculateTileSize(memorySize);
            } else {
                if (tileSize < 4) {
                    tileSize = 4;
                }
                this.tileSize = tileSize;
            }

            /* initialise the tile count, adding a sentinel tile so
               that a separator can be placed at the end of the heap */
            this.tileCount = ((memorySize + (tileSize-1)) / tileSize) + 1;

            /* a driver may have more than one spaces
               e.g. M&S driver might have heap and free list spaces */
            heapSpace = new ServerSpace("Heap", /* space name */
                                    "Lisp2 Mark & Compact", /* driver name */
                                    tileCount, /* tile num */
                                    "Block ", /* title */
                                    "Size: " + tileSize + " bytes\n", /* block info */
                                    3, /* stream count */
                                    "UNUSED", /* unused string */
                                    true /* is this the main space? */);
            setupNames(0, tileCount);

            heapSpaceID = interpreter.addServerSpace(heapSpace);

            int maxObjectsPerTile = tileSize / AVG_OBJECT_SIZE;
            if (maxObjectsPerTile < 4) {
                maxObjectsPerTile = 4;
            }
            objectsStream = new Stream("Objects",
                                       Stream.INT_TYPE, /* data type */
                                       0, /* min value */
                                       maxObjectsPerTile, /* max value */
                                       0, /* zero value */
                                       0, /* default value */
                                       "Objects: ", /* string pre */
                                       "", /* string post */
                                       Stream.PRESENTATION_PLUS,
                                       Stream.PAINT_STYLE_ZERO,
                                       0,
                                       colorDB.getColor("Yellow"),
                                       null
                                       /* enum names */);
            heapSpace.addStream(objectsStream);
            objectsSummary = objectsStream.getSummary();

            int maxObjectRefsPerTile = maxObjectsPerTile * 2;
            writeBarrierTableStream = new Stream("Write Barrier Table",
                                     Stream.INT_TYPE, /* data type */
                                     0, /* min value */
                                     maxObjectRefsPerTile, /* max value */
                                     0, /* zero value */
                                     0, /* default value */
                                     "Young gen refs: ", /* string pre */
                                     "", /* string post */
                                     Stream.PRESENTATION_PLUS,
                                     Stream.PAINT_STYLE_ZERO,
                                     0,
                                     colorDB.getColor("Yellow"),
                                     null /* enum names */);
            heapSpace.addStream(writeBarrierTableStream);
            writeBarrierTableSummary = writeBarrierTableStream.getSummary();

            usedSpaceStream = new Stream("Used Space",
                                         Stream.INT_TYPE, /* data type */
                                         0, /* min value */
                                         tileSize, /* max value */
                                         0, /* zero value */
                                         0, /* default value */
                                         "Used Space: ", /* string pre */
                                         " bytes", /* string post */
                                         Stream.PRESENTATION_PERCENT,
                                         Stream.PAINT_STYLE_ZERO,
                                         0,
                                         colorDB.getColor("Red"),
                                         null);
            heapSpace.addStream(usedSpaceStream);
            usedSpaceSummary = usedSpaceStream.getSummary();
        }
    }


    private GCspyHeapDriver gcspyHeapDriver;

    /** The server that communicates with a GCspy client. */
    private ServerInterpreter gcspyInterpreter;


    void gcspyInitializeLisp2Driver(ServerInterpreter interpreter, int blockSize) {
        gcspyHeapDriver = new GCspyHeapDriver(interpreter, blockSize);
        gcspyInterpreter = interpreter;
    }

    void gcspyFinalizeLisp2Driver() {
        if (gcspyInterpreter != null) {
            try {
                gcspyInterpreter.sendShutdown();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void gcspyEvent(int eventID) {
        if (gcspyInterpreter != null && gcspyInterpreter.shouldTransmit(eventID)) {
            gcspyHeapDriver.event(eventID);

            /* transmit the event to the client */
            gcspyInterpreter.countingEventBoundary(eventID);
        }
    }

//IFC//#endif
/*end[VM.GCSPY]*/


/*end[LISP2.COLLECTOR]*/


/*IFJ*/}
