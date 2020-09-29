//J2C:barrier.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/abstract public class WriteBarrier extends BitVector {


/*if[LISP2.COLLECTOR]*/

/*-----------------------------------------------------------------------*\
 *                             Initialization                            *
\*-----------------------------------------------------------------------*/

        /**
         * Initalize the bitmap
         * @param segment start of the segment
         * @param segmentSize size of the segment
         * @param firstOop the first oop's address in the bitmap
         * @param lastOop the last oop's address in the bitmap + 4
         * @param bitmap word aligned byte address of the bitmap array
         * @param bitmapSize The size (in bytes) of the bitmap array.
         */
/*MAC*/ void WriteBarrier_init(int $segment, int $segmentSize, int $firstOop, int $lastOop, int $bitmap, int $bitmapSize) {
            BitVector_init($segment, $segmentSize, $firstOop, $lastOop, $bitmap, $bitmapSize, true);
        }
/*end[LISP2.COLLECTOR]*/


/*-----------------------------------------------------------------------*\
 *                              Write barrier                            *
\*-----------------------------------------------------------------------*/


        /**
         * Do the write barrier marking
         * @param addr
         * @param offset word offset
         * @param value
         */
/*MAC*/ void WriteBarrier_mark(int $addr, int $offset, int $value) {
            assumeNotInCollector();
/*if[LISP2.COLLECTOR]*/
/*            if ($value != 0) {*/
/*
                if (getTraceGCVerbose()) {
                    trace("WriteBarrier_mark ");
                    traceInt($addr + ($offset * bytesPerWord()));
                    traceln("");
                }
*/
                setOopBit($addr + ($offset * bytesPerWord()));
/*            }*/
/*end[LISP2.COLLECTOR]*/
        }

/*MAC*/ boolean WriteBarrier_isBitSet(int $addr, int $offset) {
            return testOopBit($addr + ($offset * bytesPerWord()));
        }

/*MAC*/ void WriteBarrier_clear(int $addr, int $offset) {
/*if[LISP2.COLLECTOR]*/
            clearOopBit($addr + ($offset * bytesPerWord()));
/*end[LISP2.COLLECTOR]*/
        }

/*IFJ*/}
