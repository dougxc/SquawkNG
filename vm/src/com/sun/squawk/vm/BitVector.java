//J2C:bitvec.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;

/**
 * This class encapsulates the subsystem of the Squawk VM that provides
 * a fixed-size bit vector data structure. More than one such data
 * structure can exist in the ssystem at any given time. However,
 * all bit vector operations only pertain to the 'current' bit vector.
 * Setting the 'current' bit vector is done via the BitVector_init,
 * BitVector_saveContext and BitVector_restoreContext functions.
 */
/*IFJ*/abstract public class BitVector extends PersistentMemory {


/*-----------------------------------------------------------------------*\
 *                                Variables                              *
\*-----------------------------------------------------------------------*/

        static final int
            BV_base           = 0,
            BV_firstOop       = 1,
            BV_lastOop        = 2,
            BV_CONTEXT_LENGTH = 3;

        /**
         * These variables are the context of the 'current' bit vector.
         */
        private int base;
        private int firstOop;
        private int lastOop;

/*-----------------------------------------------------------------------*\
 *                                TEMP CODE                              *
\*-----------------------------------------------------------------------*/


        /** temp code
         * setNthBitInWord
         * @param addr
         * @param word
         * @param bit
         */
/*MAC*/ void setNthBitInWord(int $base, int $word, int $bit) {
            int word = getWord($base, $word);
            word |= (1 << $bit);
            setWord($base, $word, word);
        }


        /** temp code
         * clearNthBitInWord
         * @param addr
         * @param word
         * @param bit
         */
/*MAC*/ void clearNthBitInWord(int $base, int $word, int $bit) {
            int word = getWord($base, $word);
            word &= ~(1 << $bit);
            setWord($base, $word, word);
        }


        /** temp code
         * testNthBitInWord
         * @param addr
         * @param word
         * @param bit
         */
/*MAC*/ boolean testNthBitInWord(int $base, int $word, int $bit) {
            int word = getWord($base, $word);
            word &= (1 << $bit);
            return word != 0;
        }


        /** temp code
         * testAndSetNthBitInWord
         * @param addr
         * @param word
         * @param bit
         */
/*MAC*/ boolean testAndSetNthBitInWord(int $base, int $word, int $bit) {
            boolean result = testNthBitInWord($base, $word, $bit);
            setNthBitInWord($base, $word, $bit);
            return result;
        }


/*-----------------------------------------------------------------------*\
 *                             Bit manipulation                          *
\*-----------------------------------------------------------------------*/



        /**
         * assumeValidBit
         * @param addr
         */
/*MAC*/ void assumeValidBit(int $addr) {
            assume(firstOop != 0);
            assume($addr % 4 == 0);
            assume($addr >= firstOop);
            assume($addr <= lastOop);
        }

        /**
         * Calculate the offset in words from the start of memory for a
         * specified address.
         * @param addr
         * @return the offset in words from the start of memory
         */
/*MAC*/ int wordOffset(int $addr) {
            return $addr / bytesPerWord();
        }

        /**
         * setOopBit
         * @param addr
         */
/*MAC*/ void setOopBit(int $addr) {
            assumeValidBit($addr);
            setNthBitInWord(
                             base,
                             wordOffset($addr) / bitsPerWord(),
                             wordOffset($addr) % bitsPerWord()
                           );
        }

        /**
         * clearOopBit
         * @param addr
         */
/*MAC*/ void clearOopBit(int $addr) {
            assumeValidBit($addr);
            clearNthBitInWord(
                               base,
                               wordOffset($addr) / bitsPerWord(),
                               wordOffset($addr) % bitsPerWord()
                             );
        }

        /**
         * testOopBit
         * @param addr
         */
/*MAC*/ boolean testOopBit(int $addr) {
            assumeValidBit($addr);
            return testNthBitInWord(
                                     base,
                                     wordOffset($addr) / bitsPerWord(),
                                     wordOffset($addr) % bitsPerWord()
                                   );
        }

        /**
         * testAndSetOopBit
         * @param addr
         */
/*MAC*/ boolean testAndSetOopBit(int $addr) {
            assumeValidBit($addr);
            return testAndSetNthBitInWord(
                                           base,
                                           wordOffset($addr) / bitsPerWord(),
                                           wordOffset($addr) % bitsPerWord()
                                         );
        }


        /**
         * findNextMarkedOop
         * @param addr
         */
/*MAC*/ int findNextMarkedOop(int $from, int $to) {
            int i;
            assume($from <= $to);
            assume($to <= lastOop);
            assumeValidBit($from);
            assumeValidBit($to);
             /* TEMP CODE */
            for (i = $from + bytesPerWord() ; i <= $to ; i += bytesPerWord()) {
                if (testOopBit(i)) {
                    return i;
                }
            }
            return 0;
        }


        /**
         * clearOopBitRange
         * @param from
         * @param to
         */
/*MAC*/ void clearOopBitRange(int $from, int $to) {
            int i;
            assume($from <= $to);
            assume($to <= lastOop);
            assumeValidBit($from);
            assumeValidBit($to);
            /* TEMP CODE */
            for (i = $from + bytesPerWord() ; i <= $to ; i += bytesPerWord()) {
                clearOopBit(i);
            }
        }


        /**
         * clearAllOopBits
         */
/*MAC*/ void clearAllOopBitsOLDOLDOLD() {
            int i;
            /* TEMP CODE */
            for (i = firstOop ; i <= lastOop ; i += bytesPerWord()) {
                clearOopBit(i);
            }
        }


        /**
         * clearAllOopBits
         */
/*MAC*/ void clearAllOopBits() {
            int firstWordOffset = wordOffset(firstOop) / bitsPerWord();
            int lastWordOffset  = wordOffset(lastOop)  / bitsPerWord();
            int firstAddress    = base + (firstWordOffset * bytesPerWord());
            int lastAddress     = base + (lastWordOffset  * bytesPerWord());

            zeroBytes(firstAddress, lastAddress - firstAddress + bytesPerWord());
        }


        /**
         *
         */
        void traceBitmap() {
            int i;
            traceln("traceBitmap:");
            for (i = firstOop; i < lastOop; i += bytesPerWord()) {
                if (testOopBit(i)) {
                    trace("    ");
                    traceInt(i);
                    traceln("");
                }
            }
        }


/*-----------------------------------------------------------------------*\
 *                             Initialization                            *
\*-----------------------------------------------------------------------*/

        /**
         * Save the state (or context) of the 'current' bit vector so that a new
         * bit vector can be created or a previously saved bit vector
         * can be restored.
         * @param context The array in which the state will be saved. Its length
         * must be exactly <code>BV_CONTEXT_LENGTH</code>.
         */
/*MAC*/ void BitVector_saveContext(int $context[]) {
            assume($context != null);
/*IFJ*/     assume($context.length == BV_CONTEXT_LENGTH);
            $context[BV_base]     = base;
            $context[BV_firstOop] = firstOop;
            $context[BV_lastOop]  = lastOop;
        }

        /**
         * Restore a saved bit vector state (or context) so that it becomes
         * the 'current' beit vector.
         * @param context The array from which the state will be restored.
         * Its length must be exactly <code>BV_CONTEXT_LENGTH</code>.
         */
/*MAC*/ void BitVector_restoreContext(int $context[]) {
            assume($context != null);
/*IFJ*/     assume($context.length == BV_CONTEXT_LENGTH);
            base     = $context[BV_base];
            firstOop = $context[BV_firstOop];
            lastOop  = $context[BV_lastOop];
        }

/*MAC*/ private boolean inSegmentTest(int $segment, int $segmentSize, int $oop) {
            return ($oop >= $segment && $oop < ($segment+$segmentSize));
        }

        /**
         * Initalize the 'current' bit vector.
         * @param segment start of the segment
         * @param segmentSize size of the segment
         * @param firstOop the first oop's address in the bitmap
         * @param lastOop the last oop's address in the bitmap + 4
         * @param bitmap word aligned byte address of the bitmap array
         * @param bitmapSize The size (in bytes) of the bitmap array.
         * @param inSegment a flag stating whether or not the bit vector is within the
         * specified segment.
         */
/*MAC*/ void BitVector_init(int $segment, int $segmentSize, int $firstOop, int $lastOop, int $bitmap, int $bitmapSize, boolean $inSegment) {
            boolean bitmapIsForEeprom = $inSegment;
            /* A 'chunk' is 32 words of memory than can be represented by a
               signle word in the bitmap. */
            int chunkSize = bitsPerWord() * bytesPerWord();

            int firstBitMapOop, wordsFromZero, negativeOffsetInBytes;
            int addressOfBitmapWordForLastOop, addressOfLastBitmapWord;

            assume($bitmap % 4 == 0);
            assume($bitmap != 0);
            assume($segment != 0);
            assume($firstOop > $segment);
            assume($lastOop > $firstOop);
            assume($bitmap > $lastOop);

            /* Ensure that the first word of the bitmap is within the current segment
               only if it is meant to be. */
            assume($inSegment == inSegmentTest($segment, $segmentSize, $bitmap));

            /* Round down firstOop to be the */
            $firstOop &= ~(chunkSize-1); /* round down to start of first chunk */
            firstBitMapOop = $firstOop;

            wordsFromZero         = firstBitMapOop / bytesPerWord();
            negativeOffsetInBytes = wordsFromZero / bitsPerByte();

            firstOop = $firstOop;
            lastOop  = $lastOop;
            base     = $bitmap - negativeOffsetInBytes;

            assumeValidBit(firstOop);
            assumeValidBit(lastOop);

            /*
             * Ensure that the bit corresponding to 'lastOop' is within the bitmap array
             * If this assertion fails, then that typically means that there is an error
             * in calculating the required array length.
             */
            addressOfBitmapWordForLastOop = base + w2b(wordOffset($lastOop) / bitsPerWord());
            addressOfLastBitmapWord = ($bitmap + $bitmapSize) - bytesPerWord();
            assume(addressOfBitmapWordForLastOop <= addressOfLastBitmapWord);

            /* Ensure that the last word of the bitmap is within the current segment
               only if it is meant to be. */
            assume($inSegment == inSegmentTest($segment, $segmentSize, addressOfBitmapWordForLastOop));

            if ((getTraceGCVerbose() && !bitmapIsForEeprom) || (getTraceEepromGCVerbose() && bitmapIsForEeprom)) {
                trace("BitVector_init initialised ");
                traceln("");
                trace("    $segment       = "); traceInt($segment);                 traceln("");
                trace("    $segmentSize   = "); traceInt($segmentSize);             traceln("");
                trace("    segmentEnd     = "); traceInt($segment+$segmentSize);    traceln("");
                trace("    $bitmap        = "); traceInt($bitmap);                  traceln("");
                trace("    $bitmapSize    = "); traceInt($bitmapSize);              traceln("");
                trace("    $firstOop      = "); traceInt($firstOop);                traceln("");
                trace("    firstBitMapOop = "); traceInt(firstBitMapOop);           traceln("");
                trace("    $lastOop       = "); traceInt($lastOop);                 traceln("");
                trace("    base           = "); traceInt(base);                     traceln("");
                traceln("");
                traceBitmap();
            }
        }

/*-----------------------------------------------------------------------*\
 *                            Bitvector iteration                        *
\*-----------------------------------------------------------------------*/

        /* TEMP CODE */

        boolean iteratorInUse = false;
        int     iteration;
        int     iterationEnd;
        int     nextIterationOop;

/*MAC*/ void iterate() {
            while (true) {
                nextIterationOop = iteration;
                if (iteration == iterationEnd) {
                    nextIterationOop = 0;
                    break;
                } else {
                    boolean res = testOopBit(iteration);
                    iteration += bytesPerWord();
                    if (res) {
                        break;
                    }
                }
            }
        }

        /**
         * Create an iterator over all the objects in the address range
         * 'from' .. 'to'. There cannot be an object exactly at 'from' as
         * it's header would not be in the range. However, there can be an object
         * exactly at 'to' if its size is 0.
         * There must not be two succesive calls to 'startIteration' without an
         * intervening call to 'endIteration'. That is, there can only ever be one
         * iteration at a time.
         * @param from
         * @param to
         */
/*MAC*/ void startIteration(int $from, int $to) {
            assume($from <= $to);
            assume(!iteratorInUse);
            iteratorInUse = true;
            iteration     = $from + bytesPerWord();
            iterationEnd  = $to   + bytesPerWord();
            iterate();
        }

/*MAC*/ int getNextIteration() {
            return nextIterationOop;
        }

/*MAC*/ void endIteration() {
            iteratorInUse = false;
        }

/*IFJ*/}
