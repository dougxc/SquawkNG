//J2C:malloc.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/import java.io.*;
/*IFJ*/import java.util.*;

/**
 * This class implements an abstraction layer (on top of the raw memory
 * layer) that deals with a memory of objects.
 *
 *=======================================================================*/

/*IFJ*/abstract public class MemoryAllocator extends CurrentSegment
/*if[VM.GCSPY]*/
/*IFJ*/         implements GCspy
/*end[VM.GCSPY]*/
/*IFJ*/ {

/*---------------------------------------------------------------------------*\
 *                                 Variables                                 *
\*---------------------------------------------------------------------------*/

    boolean justDoneGC      = false;    /* Flag to show the last memory operation was a GC */

//IFC//#ifdef TRACING
    abstract int totalMem();
//IFC//#endif


/*-----------------------------------------------------------------------*\
 *                              Constants                                *
\*-----------------------------------------------------------------------*/

    /**
     * The size of a stack chunk allocated in newStackChunk() will be the
     * maximum of this constant and the value passed to newStackChunk().
     */
    public final static int
        REGULAR_CHUNK_SIZE = /*VAL*/32/*VM.REGULAR_CHUNK_SIZE*/,
        MAX_INT_VALUE      = 0x7fffffff;

/*-----------------------------------------------------------------------*\
 *                           Memory allocation                           *
\*-----------------------------------------------------------------------*/

    /**
     * Allocate a raw memory chunk in persistent memory and zero all but the
     * header word(s).
     * @param exactByteSize The exact size (in bytes) of the chunk required.
     * This size will be rounded up to a word size.
     * @return the pointer to the allocated chunk or 0 if the allocation failed.
     */
    int newChunk(int exactByteSize) {
        int byteSize = roundup4(exactByteSize);                 /* Round up to a full word boundry */
        int current  = getCurrentObjectPartitionFree();
        int newEnd   = current + byteSize;
        assume(exactByteSize > 0);

        if ((getCurrentObjectPartitionEnd() - current) < byteSize) {
            setFailedGCLength(byteSize);
            if (getTraceAllocation()) {
/*IFJ*/         traceln("********** newChunk failure ********** gcSegment="+getCurrentSegment()+" size (in bytes)="+byteSize);
//IFC//         traceln("********** newChunk failure **********");
            }
            return 0;
        }

        if (getExcessiveGCEnabled()) {
//IFC//     int freeMem();
//IFC//     int totalMem();
            if (justDoneGC == false) {
                if (getTraceGC()) {
                    trace("********** EXCESSIVEGC ********** size=");
                    traceInt(byteSize);
                    traceln("");
                }
                return 0;
            } else {
                if (getTraceGC()) {
                    trace("********** ALLOCATING ********** size=");
                    traceInt(byteSize);
                    trace(" current=");
                    traceInt(current);
                    trace(" newend=");
                    traceInt(newEnd);
                    traceln("");
                }
                justDoneGC = false;
            }
        }

        zeroBytes(current, byteSize);

        // Ensure that memory is allocated on a word boundary
        assume((current & 0x3) == 0);

        setCurrentObjectPartitionFree(newEnd);
        incAllocationCount();

        assume(getCurrentObjectPartitionFree() <= getCurrentObjectPartitionEnd());

        return current;
    }

    /**
     * Allocate a new non-array class instance.
     * @param klass The pointer to the class.
     * @return the newly allocated instance or 0 if the allocation failed.
     */
    int newInstance(int klass) {
        int byteLength, headerSize, object;

        assume(!Class_isArrayClass(klass));

        byteLength = w2b(Class_getInstanceFieldsLength(klass)); // in bytes
        headerSize = w2b(1);                                    // in bytes
        object = newChunk(byteLength+headerSize);

        if (getTraceAllocation()) {
            trace("*ALLOC* newObject(");
            trace_className(klass);
            trace(") size ");
            traceInt(byteLength+headerSize);
        }

        if (object != 0) {
            object += headerSize;
            Object_setClass(object, klass);
            if (getTraceAllocation()) {
                trace(" -> ");
                traceHex(object);
                traceInstructionCount(true);
            }
/*if[VM.GCSPY]*/
/*IFJ*/     gcspyEvent(GCSPY_EVENT_ALLOC_OBJECT);
/*end[VM.GCSPY]*/
        } else {
            if (getTraceAllocation()) {
                traceln(" Failed");
                traceInstructionCount(true);
            }
        }
        return object;
    }

    /**
     * Allocate a new array instance.
     * @param klass The pointer to the array class.
     * @param count The number of elements in the array.
     * @return the newly allocated array instance or 0 if the allocation failed.
     */
    private int newArray2(int klass, int count) {
        int byteLength = count * Object_getArrayElementLength(klass);   // in bytes
        int headerSize = Object_calculateArrayHeaderLength(count);      // in bytes

       /*
        * Need to handle integer arithmetic wrapping. If byteLength is very large
        * then "length * Object_getArrayElementLength(klass)" can go negative.
        */
        if (byteLength < 0) {
            setFailedGCLength(MAX_INT_VALUE);
            return 0;
        } else {
            int object = newChunk(byteLength+headerSize);

            if (getTraceAllocation()) {
                trace("*ALLOC* newArray(");
                trace_className(klass);
                trace(", count=");
                traceInt(count);
                trace(", size=");
                traceInt(byteLength+headerSize);
                trace(")");
            }

            if (object != 0) {
                object += headerSize;
                Object_setClassAndArrayCount(object, klass, count);
                if (getTraceAllocation()) {
                    trace(" -> ");
                    traceHex(object);
                    traceInstructionCount(false);
                }
                if (Object_isLargeArrayHeaderLength(count)) {
                    incLargeArrayCount();
                } else {
                    incSmallArrayCount();
                }
/*if[VM.GCSPY]*/
/*IFJ*/         gcspyEvent(GCSPY_EVENT_ALLOC_ARRAY);
/*end[VM.GCSPY]*/
            } else {
                if (getTraceAllocation()) {
                    traceln(" Failed");
                    traceInstructionCount(false);
                }
            }
            return object;
        }
    }

    /**
     * Allocate a new array instance.
     * @param klass The pointer to the array class.
     * @param count The number of elements in the array.
     * @return the newly allocated array instance or 0 if the allocation failed.
     */
    int newArray(int klass, int count) {
        int res = newArray2(klass, count);
        if (getTraceAllocation()) {
            traceln("");
        }
        return res;
    }


    /**
     * Allocate a new class state instance.
     * @param klass The pointer to the array class.
     * @return the newly allocated instance or 0 if the allocation failed.
     */
    int newClassState(int klass) {
        int siz = Class_getStaticFieldsLength(klass);
        int res = newArray2(getClassFromCNO(CNO.GLOBAL_ARRAY), CLS_STATE_offsetToFields + siz);
        if (getTraceAllocation()) {
            trace(" *CLASSSTATE* for ");
            trace_className(klass);
            traceln("");
        }
        return res;
    }

    /**
     * Allocate a new stack chunk.
     * @param size The size of the stack chunk (in words).
     * @return the newly allocated stack chunk or 0 if the allocation failed.
     */
    int newStackChunk(int minsize) {
        int size = minsize;
        int chunk;
        if (getVeryExcessiveGCEnabled() == false && size < REGULAR_CHUNK_SIZE) {
            size = REGULAR_CHUNK_SIZE;
        }
        chunk = newArray(getClassFromCNO(CNO.LOCAL_ARRAY), size);
        if (chunk != 0) {
            incStackChunkCount();
/*if[CHENEY.COLLECTOR]*/
            StackChunk_setSelf(chunk,   chunk);
/*end[CHENEY.COLLECTOR]*/
/*if[LISP2.COLLECTOR]*/
            StackChunk_setList(chunk,   getStackChunkList());
            setStackChunkList(chunk);
/*end[LISP2.COLLECTOR]*/
            StackChunk_setSize(chunk,   size);
            StackChunk_setNext(chunk,   0);
            StackChunk_setPrev(chunk,   0);
            StackChunk_setLastLp(chunk, 0);
        }
        return chunk;
    }

/*-----------------------------------------------------------------------*\
 *                           Memory statistics                           *
\*-----------------------------------------------------------------------*/

    /**
     * Get the amount of free memory (in words).
     * @return the amount of free memory (in words).
     */
    private int freeMemInWords() {
        return ((getCurrentObjectMemoryEnd() - getCurrentObjectPartitionFree()) / 4) - 1;
    }

    /**
     * Get the amount of free memory (in bytes).
     * @return the amount of free memory (in bytes).
     */
    int freeMem() {
        return freeMemInWords() * 4;
    }

/*IFJ*/}
