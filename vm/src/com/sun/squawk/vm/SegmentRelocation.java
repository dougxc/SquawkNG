//J2C:segreloc.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/abstract public class SegmentRelocation extends SegmentAccess {


/*-----------------------------------------------------------------------*\
 *                      Memory segment relocation functions              *
\*-----------------------------------------------------------------------*/


//IFC//#ifdef IMAGERELOCATION

    /**
     * Relocate a segment
     */
    private void Segment_reloc(int segment, int delta) {
        setObjectMemoryStart(     segment, getObjectMemoryStart(segment)    + delta);
        setObjectPartitionStart(  segment, getObjectPartitionStart(segment) + delta);
        setObjectPartitionFree(   segment, getObjectPartitionFree(segment)  + delta);
        setObjectPartitionEnd(    segment, getObjectPartitionEnd(segment)   + delta);
    }

    /**
     * Relocate a pointer field of an object. This relocation takes into
     * account pointers that cross segment boundaries.
     * @param object An object in the relocated address space.
     * @param offset
     * @param delta
     */
    private void relocateOop(int object, int offset, int delta) {
        int oop = getWord(object, offset);
        if (oop != 0) {
            int newOop = oop + delta;
            assume(newOop > 0);
            setWord(object, offset, oop + delta);
        }
    }

    /**
     * Relocate a set of pointer fields in an object based on an oop map.
     *
     * @param object
     * @param map
     * @param fieldCount
     * @param delta
     */
    void relocateOopsFromOopMap(int object, int map, int fieldCount, int delta) {
        int i;
        int mapBytes = (fieldCount+7)/8;
        int offset = 0;
        for (i = 0 ; i < mapBytes ; i++) {
            int mapbyte = getUnsignedByte(0, map+i);
            int j;
            for (j = 0; j != 8 && offset != fieldCount; ++j, ++offset) {
                if ((mapbyte & (1<<j)) != 0) {
                    relocateOop(object, offset, delta);
                }
            }
        }
    }

    /**
     * Relocate all the pointers in a segment.
     *
     * @param segment The offset of the segment within the image.
     * @param segmentSize The size (in bytes) of the segment.
     * @param forExecution If true, then relocation is being performed
     * on an image whose pointers are in terms of the image starting at
     * address 0 even though the image may be at some other real location
     * in memory. For example, the image is being relocated after having
     * been loaded from its on-disk form. Otherwise, the image is being
     * reverted to this form for the purpose of writing out a snapshot to
     * disk.
     */
    private void relocateSegment(int segment, int segmentSize, boolean forExecution) {

        int i;
        int start;
        int end;
        int freePtr;
        int delta = (forExecution ? getImageOffset() : -getImageOffset());

        if (forExecution) {
            Segment_reloc(segment, delta);
        }

        start   = getObjectMemoryStart(segment);
        end     = getObjectPartitionEnd(segment);
        freePtr = getObjectPartitionFree(segment);

        /* Relocate the roots. */
        for (i = MSR_roots; i != MSR_SIZE; i++) {
            relocateOop(segment, i, delta);
        }

        /* Iterate over all the objects in the heap. */
        if (freePtr != start) {
            int oop;
            for (oop = Object_blockToOop(start) ; oop != 0 ; oop = Object_nextObject(oop, start, freePtr)) {

                int klass = Object_getClass(oop);
                int cno   = Class_getType(klass);

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
                            int lp, lastLp;

                            /* Relocate the pointers at fixed offsets */
                            relocateOop(oop, STACK_next, delta);
                            relocateOop(oop, STACK_prev, delta);
                            relocateOop(oop, STACK_lastLp, delta);
/*if[CHENEY.COLLECTOR]*/
                            relocateOop(oop, STACK_self, delta);
/*end[CHENEY.COLLECTOR]*/
/*if[LISP2.COLLECTOR]*/
                            relocateOop(oop, STACK_list, delta);
/*end[LISP2.COLLECTOR]*/

                            lp     = StackChunk_getFirstLp(oop);
                            lastLp = StackChunk_getLastLp(oop);

                            if (!forExecution) {
                                lastLp -= delta;
                            }

                            /*
                             * Iterate through each frame
                             */
                            while (lp <= lastLp) {
                                int mp, relativeIp;

                                /* Relocate the pointers at fixed offsets */
                                relocateOop(lp, FRAME_currentCp, delta);
                                relocateOop(lp, FRAME_currentMp, delta);
                                relocateOop(lp, FRAME_currentIp, delta);
                                relocateOop(lp, FRAME_previousLp, delta);

                                mp = Frame_getCurrentMp(lp);
                                relativeIp = Frame_getCurrentIp(lp) - mp;

                                if (!forExecution) {
                                    mp -= delta;
                                }

                                /*
                                 * Update parameters and locals
                                 */
                                if (relativeIp == getUnsignedByte(mp, MTH_headerSize)) {
                                    /*
                                     * Before first instruction
                                     */
                                    int fieldCount = getUnsignedByte(mp, MTH_numberOfParms);
                                    relocateOopsFromOopMap(lp, mp + MTH_oopMap, fieldCount, delta);

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
                                    relocateOopsFromOopMap(lp, mp + MTH_oopMap, fieldCount, delta);

                                    /*
                                     * Advance to next frame in chunk
                                     */
                                    lp = lp + Frame_getStackOffset(lp) + w2b(1);
                                }
                            }
                            continue;
                        }
                        case CNO.GLOBAL_ARRAY: {
                            int count, j, csKlass;
                            csKlass = ClassState_getClass(oop);
                            if (forExecution) {
                                csKlass = csKlass + delta;
                            }
                            assume(csKlass != 0);
                            count = Class_getPointerStaticFieldsLength(csKlass) + CLS_STATE_offsetToFields;
                            for (j = 0 ; j < count ; j++) {
                                relocateOop(oop, j, delta);
                            }
                            continue;
                        }
                        default: {
                            /* An array of references */
                            int arrayCount = Object_getArrayElementCount(oop);
                            for (i = 0 ; i < arrayCount ; i++) {
                                relocateOop(oop, i, delta);
                            }
                            continue;
                        }
                    }
                } else {
                    int map = Class_getOopMap(klass);
                    int fieldCount = Class_getInstanceFieldsLength(klass);
                    if (forExecution) {
                        if (klass >= oop) {
                            /*
                             * The pointer to the map will not have been relocated
                             * yet if the Class object is after the current object
                             * in the heap or this is the Class object for
                             * java.lang.Class.
                             */
                            map = map + delta;
                        }
                    } else {
                        if (klass < oop) {
                            map = map - delta;
                        }
                    }
                    assume(Object_getArrayElementCount(map) == ((fieldCount+7)/8));
                    relocateOopsFromOopMap(oop, map, fieldCount, delta);
                }
            }
        }

        if (!forExecution) {
            Segment_reloc(segment, delta);
        }

    }

    /**
     * Relocate all the pointers in the image.
     *
     * @param forExecution If true, then relocation is being performed
     * on an image whose pointers are in terms of the image starting at
     * address 0 even though the image may be at some other real location
     * in memory. For example, the image is being relocated after having
     * been loaded from its on-disk form. Otherwise, the image is being
     * reverted to this form for the purpose of writing out a snapshot to
     * disk.
     */
    void relocateImage(boolean forExecution) {
        int delta = (forExecution ? getImageOffset() : -getImageOffset());
        if (forExecution) {
            MMR[MMR_romStart]    += delta;
            MMR[MMR_eepromStart] += delta;
            MMR[MMR_ramStart]    += delta;
        }

        relocateSegment(MMR[MMR_romStart],    MMR[MMR_romSize],    forExecution);
        relocateSegment(MMR[MMR_eepromStart], MMR[MMR_eepromSize], forExecution);
        relocateSegment(MMR[MMR_ramStart],    MMR[MMR_ramSize],    forExecution);

        if (!forExecution) {
            MMR[MMR_romStart]    += delta;
            MMR[MMR_eepromStart] += delta;
            MMR[MMR_ramStart]    += delta;
        }
    }

//IFC//#else
//IFC//#define relocateImage(forExecution) /**/
//IFC//#endif /* IMAGERELOCATION */

/*IFJ*/}
