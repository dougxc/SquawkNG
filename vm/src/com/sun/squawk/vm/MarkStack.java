//J2C:markstk.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/import java.io.*;
/*IFJ*/import java.util.*;

/**
 * This class encapsulates the subsystem of the Squawk VM that provides
 * a data structure that can be used to limit the recursiveness of any
 * object graph marking operation.
 */
/*IFJ*/abstract class MarkStack extends HeapTracing {

/*---------------------------------------------------------------------------*\
 *                         Constants                                         *
\*---------------------------------------------------------------------------*/

    final static int
        RECURSELEVEL        = 2,
        MINMARKSTACKSIZE    = 20; // bytes


    final static boolean
        TESTMARKSTACKOVERFLOW = false; /* Set true to test */

/*---------------------------------------------------------------------------*\
 *                         Variables                                         *
\*---------------------------------------------------------------------------*/

        private boolean stackInUse = false;
        private int stack;
        private int stackIndex;
        private int stackSize;

        private boolean overflow;
        private int level;

/*-----------------------------------------------------------------------*\
 *                       Marking stack                                   *
\*-----------------------------------------------------------------------*/

        /**
         * markStackSetup
         */
/*MAC*/ void markStackSetup(int $start, int $end) {
            assume(!stackInUse);
            stackInUse    = true;
            stack         = $start;
            stackIndex    = 0;
            stackSize     = ($end - $start) / bytesPerWord();
            overflow = false;
            level      = TESTMARKSTACKOVERFLOW ? 1 : RECURSELEVEL;
        }

        /**
         * pushOnMarkStack
         */
/*MAC*/ void pushOnMarkStack(int $oop) {
            assume(stackInUse);
            if (stackIndex == stackSize || TESTMARKSTACKOVERFLOW) {
                overflow = true;
            } else {
                setWord(stack, stackIndex++, $oop);
            }
        }

        /**
         * popOffMarkStack
         */
/*MAC*/ int popOffMarkStack() {
            assume(stackInUse);
            if (stackIndex == 0) {
                return 0;
            } else {
                return getWord(stack, --stackIndex);
            }
        }

/*MAC*/ boolean markStackOverflow() {
            assume(stackInUse);
            return overflow;
        }

/*MAC*/ int markStackSize() {
            assume(stackInUse);
            return (stackSize);
        }

/*MAC*/ int markStackIndex() {
            assume(stackInUse);
            return (stackIndex);
        }

/*MAC*/ void resetMarkStackOverflow() {
            assume(stackInUse);
            overflow = false;
        }

/*MAC*/ int recurseLevel() {
            assume(stackInUse);
            return level;
        }

/*MAC*/ void recurseLevelInc() {
            assume(stackInUse);
            level++;
        }

/*MAC*/ void recurseLevelDec() {
            assume(stackInUse);
            level--;
        }

/*MAC*/ void markStackFinish() {
            assume(stackInUse);
            stackInUse = false;
        }
/*IFJ*/}