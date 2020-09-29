//J2C:segments.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/abstract public class Segments extends WriteBarrier {


/*-----------------------------------------------------------------------*\
 *                               Variables                               *
\*-----------------------------------------------------------------------*/

//IFC//      int   MMR[MMR_SIZE];
/*IFJ*/final int[] MMR = new int[MMR_SIZE];

    int ROM;
    int EEPROM;
    int RAM;

    /**
     * Determine whether or not an address is in a specified segment.
     * @param addr The address to test.
     * @param segment The start address of the segment being tested.
     * @param segmentSizeIndex The constant index into MMR at which the size
     * of the segment can be found.
     * @return whether or not 'addr' is in the segment starting at 'segment'.
     */
/*MAC*/ boolean inSegment(int $addr, int $segment, int $segmentSizeIndex) {
            return ($segment <= $addr) && ($addr < $segment + MMR[$segmentSizeIndex]);
        }

/*MAC*/ boolean inRom(int $oop)          { return inSegment($oop, ROM,    MMR_romSize);    }
/*MAC*/ boolean inRam(int $oop)          { return inSegment($oop, RAM,    MMR_ramSize);    }
/*MAC*/ boolean inEeprom(int $oop)       { return inSegment($oop, EEPROM, MMR_eepromSize); }

/*MAC*/ void assumeIsValidOop(int $oop) {
            assume(($oop % 4) == 0);
            assume(inRom($oop) || inRam($oop) || inEeprom($oop));
        }


/*-----------------------------------------------------------------------*\
 *                               SetOop                                  *
\*-----------------------------------------------------------------------*/

        /**
         * Set the value of a pointer where the object containing the pointer is
         * guaranteed to be in RAM.
         * @param addr
         * @param offset
         * @param value
         */
/*MAC*/ void setOopRam(int $addr, int $offset, int $value) {
            assume(inRam($addr));
            WriteBarrier_mark($addr, $offset, $value);
            setWord($addr, $offset, $value);
        }

        /**
         * Set the value of a pointer where the object containing the pointer may
         * be in EEPROM.
         * @param addr The address of the object containing the pointer.
         * @param offset The offset of the pointer field or element in the containing object.
         * @param oop The pointer value to be set.
         * @return whether or not the update succeeded.
         */
        boolean setOopStatus(int addr, int offset, int oop) {
            if (inRam(addr)) {
                setOopRam(addr, offset, oop);
                return true;
            }
            if (inEeprom(addr)) {
                if (inRam(oop)) {
                    return false;
                } else {
                    setPersistentWord(addr, offset, oop);
                    return true;
                }
            }
            return false;
        }

        /**
         * Set the value of a pointer where the object containing the pointer may
         * be in EEPROM.
         * @param addr The address of the object containing the pointer.
         * @param offset The offset of the pointer field or element in the containing object.
         * @param oop The pointer value to be set.
         */
        void setOop(int addr, int offset, int oop) {
            if (romizing()) {
                setWord(addr, offset, oop);
            } else {
                if (!setOopStatus(addr, offset, oop)) {
                    if (inEeprom(addr)) {
                        fatalVMError("Cannot store a RAM address in EEPROM");
                    } else {
                        fatalVMError("Cannot write to ROM");
                    }
                }
            }
        }

        /**
         * Set the value of a pointer slot in the MSR (i.e. a root) of a segment.
         * @param segment This will be ROM, EEPROM or RAM.
         * @param offset The offset of the root pointer.
         * @param oop The pointer value to be set.
         */
        void setOopRoot(int segment, int offset, int oop) {
            if (romizing()) {
                assume(segment == ROM || segment == EEPROM);
                setWord(segment, offset, oop);
            } else {
                assume(segment == RAM || segment == EEPROM);
                if (segment == RAM) {
                    setWord(segment, offset, oop);
                } else {
                    if (inRam(oop)) {
                        fatalVMError("Cannot store a RAM address in EEPROM");
                    } else {
                        setPersistentWord(segment, offset, oop);
                    }
                }
            }
        }

/*IFJ*/}
