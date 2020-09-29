//J2C:object.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/import java.io.*;
/*IFJ*/import java.util.*;

/**
 * This class implements an abstraction layer (on top of the raw memory
 * layer) that deals with a memory of objects.
 *
 *=======================================================================*/

/*IFJ*/abstract public class ObjectMemory extends Lisp2Collector {


/*-----------------------------------------------------------------------*\
 *                            Initialization                             *
\*-----------------------------------------------------------------------*/

        /**
         * Determine whether or not 2 ranges are disjoint.
         * @param r1 The start of the first range.
         * @param r1Size The size of the first range.
         * @param r2 The start of the second range.
         * @param r2Size The size of the second range.
         * @return whether or not r1 and r2 are disjoint.
         */
/*MAC*/ boolean disjoint(int $r1, int $r1Size, int $r2, int $r2Size) {
            if ($r1 < $r2) {
                return $r1 + ($r1Size - 1) < $r2;
            } else if ($r2 < $r1) {
                return $r2 + ($r2Size - 1) < $r1;
            } else {
                return false;
            }
        }

//IFC//#if 0
    /**
     * Initialize the object memory for romizing.
     * @param romSize
     * @param eepromSize
     * @param ramSize
     * @param semiSpaceRom
     */
    protected void ObjectMemory_init(
                                      int     rom,
                                      int     romSize,
                                      int     eeprom,
                                      int     eepromSize,
                                      int     ram,
                                      int     ramSize,
                                      boolean gcRom,
                                      boolean bigEndian
                                    ) {

        assume(disjoint(rom, romSize, eeprom, eepromSize));
        assume(disjoint(rom, romSize, ram,    ramSize));
        assume(disjoint(ram, ramSize, eeprom, eepromSize));

        int memorySize = Math.max(Math.max(rom+romSize, eeprom+eepromSize), ram+ramSize);
        byte[] memory = new byte[memorySize];

        Memory_init(memory, memorySize, bigEndian);

        ROM     = rom;
        EEPROM  = eeprom;
        RAM     = ram;

        Segment_init(
                      ROM,
                      gcRom ? calculateObjectMemoryStart(ROM)         : ROM     + getSegmentMSROverhead(),
                      gcRom ? calculateObjectMemorySize(ROM, romSize) : romSize - getSegmentMSROverhead()
                    );

        Segment_init(
                      EEPROM,
                      EEPROM     + getSegmentMSROverhead(),
                      eepromSize - getSegmentMSROverhead()
                    );

        Segment_init(
                      RAM,
                      calculateObjectMemoryStart(RAM),
                      calculateObjectMemorySize(RAM, ramSize)
                    );

        /* Initialise the Master Memory Record */
        MMR[MMR_magicNumber] = MMR_MAGICNUMBER;
        MMR[MMR_version    ] = MMR_VERSION;
        MMR[MMR_romStart   ] = ROM;
        MMR[MMR_eepromStart] = EEPROM;
        MMR[MMR_ramStart   ] = RAM;
        MMR[MMR_romSize    ] = romSize;
        MMR[MMR_eepromSize ] = eepromSize;
        MMR[MMR_ramSize    ] = ramSize;

        /* Allocation & GC happen in ROM during romzing. */
        setCurrentSegment(ROM, romSize);

        /* Initialise the free list pointer in EEPROM */
        PersistentCollector_init(getObjectPartitionStart(EEPROM), getObjectPartitionEnd(EEPROM));
    }
//IFC//#endif

    /**
     * Initialize the object memory after restart from an image.
     */
    protected void ObjectMemory_reinit(byte image[], boolean isBigEndian) {
        int romSize    = MMR[MMR_romSize];
        int eepromSize = MMR[MMR_eepromSize];
        int ramSize    = MMR[MMR_ramSize];
        int memorySize = romSize + eepromSize + ramSize;

        if (MMR[MMR_magicNumber] !=  MMR_MAGICNUMBER) {
            printMsg("Magic number expected: ");
            printHex(MMR_MAGICNUMBER);
            println();
            printMsg("Magic number received: ");
            printHex(MMR[MMR_magicNumber]);
            println();
            if (MMR[MMR_magicNumber] == MMR_MAGICNUMBER_REVERSED) {
                printMsg("Image appears to have incorrect endianess");
                println();
            }
            fatalVMError("Bad magic number in image");
        }
        if (MMR[MMR_version] !=  MMR_VERSION) {
            fatalVMError("Bad version number in image");
        }

        Memory_init(image, memorySize, isBigEndian);
        relocateImage(true);

        ROM    = MMR[MMR_romStart];
        EEPROM = MMR[MMR_eepromStart];
        RAM    = MMR[MMR_ramStart];

        assume(disjoint(ROM, romSize, EEPROM, eepromSize));
        assume(disjoint(ROM, romSize, RAM,    ramSize));
        assume(disjoint(RAM, ramSize, EEPROM, eepromSize));

        /* Re-hash the association table as objects may have moved */
        processAssociationQueuesAfterRelocation();

        /* Allocation & GC happen in RAM by default. */
        setCurrentSegment(RAM, MMR[MMR_ramSize]);
        initializeCollector(false);
    }

/*---------------------------------------------------------------------------*\
 *                          RAM --> EEPROM migration                         *
\*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*\
 *                         Garbage collection                                *
\*---------------------------------------------------------------------------*/

    boolean gc() {
        boolean gcOK;
        restartTimer();
        gcOK = gcRam();
        stopTimer(TIME_RAM_GC);
        if (!gcOK) {
            int migratedBytes = 0;
            while (migratedBytes < getFailedGCLength()) {
                int assn = lookupNextMigratable();
                if (assn != 0) {
                    int object = ObjectAssociation_getObject(assn);

                    if (getTraceMigrationVerbose() || getTraceGCVerbose()) {
                        trace("auto migrating object for failed GC: ");
                        traceHex(object);
                        trace(" (migratedBytes=");
                        traceInt(migratedBytes);
                        trace(", mem required=");
                        traceInt(getFailedGCLength());
                        traceln(")");
                    }

                    object = makePersistent(object, true);
                    if (object == 0) {
                        return false;
                    } else {
                        migratedBytes += getTotalBytesCopiedToPersistentMemory();
                    }
                } else {
                    return false;
                }
            }
            restartTimer();
            gcOK = gcRam();
            stopTimer(TIME_RAM_GC);
            assume(gcOK);
        }
        setFailedGCLength(0);
        updateHighWaterMark();
        return gcOK;
    }

/*---------------------------------------------------------------------------*\
 *                           Image file saving                               *
\*---------------------------------------------------------------------------*/

    /**
     * Write the memory contents to an image file.
     * @param imageFileName
     */
    public void ObjectMemory_writeImage(String imageFileName) {
        relocateImage(false);
        Memory_writeImage(imageFileName, MMR);
        relocateImage(true);
    }
/*IFJ*/}
