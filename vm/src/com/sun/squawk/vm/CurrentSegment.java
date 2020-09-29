//J2C:curseg.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/import java.io.*;
/*IFJ*/import java.util.*;

/**
 * This class implements an abstraction layer (on top of the raw memory
 * layer) that deals with a memory of objects.
 *
 *=======================================================================*/

/*IFJ*/abstract public class CurrentSegment extends PersistentCollector {

/*-----------------------------------------------------------------------*\
 *                               Variables                               *
\*-----------------------------------------------------------------------*/

        /**
         * The pointer to the base of the current memory segment for allocation
         * and garbage collection.
         */
        private int currentSegment;
        private int currentSegmentEnd;


/*-----------------------------------------------------------------------*\
 *                        Current segment accessors                      *
\*-----------------------------------------------------------------------*/

        /**
         * Setup the current collector's segment
         */
/*MAC*/ void setCurrentSegment(int $segment, int $size) {
            currentSegment    = $segment;
            currentSegmentEnd = $segment + $size;
        }

        /**
         * Assume the current segement
         */
/*MAC*/ void assumeCurrentSegment(int $segment)                      { assume(currentSegment == $segment); }

        /**
         * Get the current segement
         */
/*MAC*/ int  getCurrentSegment()                                     { return currentSegment; }

        /**
         * Get end pointer for the current segement
         */
/*MAC*/ int  getCurrentSegmentEnd()                                  { return currentSegmentEnd; }

        /**
         * Get size of the current segement
         */
/*MAC*/ int  getCurrentSegmentSize()                                 { return currentSegmentEnd - currentSegment; }

        /**
         * Get the start of the heap in the current segment.
         */
/*MAC*/ int getCurrentHeapStart()  { return getHeapStart(currentSegment); }

        /**
         * Get the size of the heap in the current segment.
         */
/*MAC*/ int getCurrentHeapSize()  { return getHeapSize(currentSegment, currentSegmentEnd); }

        /**
         * Get/set the address of the start of the object memory in a given segment.
         */
/*MAC*/ int  getCurrentObjectMemoryStart()                  { return getObjectMemoryStart(currentSegment);         }
/*MAC*/ void setCurrentObjectMemoryStart(int $start)        {        setObjectMemoryStart(currentSegment, $start); }

        /**
         * Get/set the size of the object memory for a given segment.
         */
/*MAC*/ int  getCurrentObjectMemorySize()                   { return getObjectMemorySize(currentSegment);        }
/*MAC*/ void setCurrentObjectMemorySize(int $size)          {        setObjectMemorySize(currentSegment, $size); }

        /**
         * Get/set the address of the end of the object memory in a given segment.
         */
/*MAC*/ int  getCurrentObjectMemoryEnd()                  { return getObjectMemoryEnd(currentSegment); }

        /**
         * Start of the active semi space for a given segment.
         */
/*MAC*/ int  getCurrentObjectPartition()                                  { return getObjectPartitionStart(currentSegment);                            }
/*MAC*/ void setCurrentObjectPartition(int $addr)                         {        setObjectPartitionStart(currentSegment, $addr);                     }

        /**
         * Allocation point in the active semi space for a given segment.
         */
/*MAC*/ int  getCurrentObjectPartitionFree()                           { return getObjectPartitionFree(currentSegment);                             }
/*MAC*/ void setCurrentObjectPartitionFree(int $addr)                  {        setObjectPartitionFree(currentSegment, $addr);                      }

        /**
         * End of the active semi space for a given segment.
         */
/*MAC*/ int  getCurrentObjectPartitionEnd()                               { return getObjectPartitionEnd(currentSegment);                              }
/*MAC*/ void setCurrentObjectPartitionEnd(int $addr)                      {        setObjectPartitionEnd(currentSegment, $addr);                       }

        /**
         * Read/write GC roots
         */
/*MAC*/ int  getRoot(int $offset)                               { return getWord(currentSegment, MSR_roots+$offset);                         }
/*MAC*/ void setRoot(int $offset, int $addr)                    {        setWord(currentSegment, MSR_roots+$offset, $addr);                  }

        /**
         * Get the length of the GC failed size
         */
/*MAC*/ int  getFailedGCLength()                                { return getFailedAllocationSize(currentSegment);                            }
/*MAC*/ void setFailedGCLength(int $lth)                        {        setFailedAllocationSize(currentSegment, $lth);                      }

        /**
         * Test to see of oop is in the current segement
         */
/*MAC*/ boolean inCurrentSegment(int $oop)                      { return ($oop > currentSegment) && ($oop <= currentSegmentEnd);                  }

        /**
         * Test to see of oop is in the current object partition of the current segement
         */
/*MAC*/ boolean inCurrentObjectPartition(int $oop)                        { return $oop > getCurrentObjectPartition() && $oop <= getCurrentObjectPartitionEnd();      }

/*IFJ*/}
