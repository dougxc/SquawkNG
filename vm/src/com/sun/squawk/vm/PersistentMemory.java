//J2C:pmemory.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/abstract public class PersistentMemory extends Memory {


/*-----------------------------------------------------------------------*\
 *                     Persistent memory write access                    *
\*-----------------------------------------------------------------------*/

/*MAC*/ void setPersistentByte(int $oop, int $offset, int $value) {
            setByte($oop, $offset, $value);
        }

/*MAC*/ void setPersistentHalf(int $oop, int $offset, int $value) {
            setHalf($oop, $offset, $value);
        }

/*MAC*/ void setPersistentWord(int $oop, int $offset, int $value) {
            setWord($oop, $offset, $value);
        }

/*MAC*/ void setPersistentLong(int $oop, int $offset, long $value) {
            setLong($oop, $offset, $value);
        }

/*MAC*/ void setPersistentLongAtWord(int $oop, int $offset, long $value) {
            setLongAtWord($oop, $offset, $value);
        }


/*IFJ*/}
