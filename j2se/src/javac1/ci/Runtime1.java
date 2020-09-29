/*
 * @(#)Runtime1.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ci;

/**
 * This class provides the code emitter with information about the interface
 * to the runtime system.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Runtime1 {
    /**
     * The identification number for a stub routine.
     */
    public static final int
            NEW_INSTANCE_ID                =  0, NEW_TYPE_ARRAY_ID              =  1,
            NEW_OBJECT_ARRAY_ID            =  2, NEW_MULTI_ARRAY_ID             =  3,
            RESOLVE_INVOKESTATIC_ID        =  4, RESOLVE_INVOKE_ID              =  5,
            HANDLE_IC_MISS_ID              =  6, HANDLE_WRONG_STATIC_METHOD_ID  =  7,
            HANDLE_WRONG_METHOD_ID         =  8, THROW_ABSTRACT_METHOD_ERROR_ID =  9,
            MUST_COMPILE_METHOD_ID         = 10, ENTERING_NON_ENTRANT_ID        = 11,
            ARITHMETIC_CALL_ID             = 12, RANGE_CHECK_FAILED_ID          = 13,
            THROW_DIV0_EXCEPTION_ID        = 14, THROW_NULL_EXCEPTION_ID        = 15,
            THROW_EXCEPTION_ID             = 16, HANDLE_EXCEPTION_ID            = 17,
            UNWIND_EXCEPTION_ID            = 18, STORE_CHECK_ID                 = 19,
            CHECKCAST_ID                   = 20, INSTANCEOF_ID                  = 21,
            MONITORENTER_ID                = 22, MONITOREXIT_ID                 = 23,
            IMPLICIT_NULL_EXCEPTION_ID     = 24, IMPLICIT_DIV0_EXCEPTION_ID     = 25,
            STACK_OVERFLOW_EXCEPTION_ID    = 26, INTERPRETER_ENTRIES_ID         = 27,
            INIT_CHECK_PATCHING_ID         = 28, LOAD_KLASS_PATCHING_ID         = 29,
            OSR_FRAME_RETURN_ID            = 30, OSR_UNWIND_EXCEPTION_ID        = 31,
            ILLEGAL_INSTRUCTION_HANDLER_ID = 32, ADDRESS_BCI_2_PC_ID            = 33,
            SAFEPOINT_BLOCK_ID             = 34, JVMPI_METHOD_ENTRY             = 35,
            JVMPI_METHOD_EXIT              = 36;

    /**
     * The identification number for a runtime function.
     */
    public static final int
            LMUL                        =  0, LDIV                        =  1,
            LREM                        =  2, FREM                        =  3,
            DREM                        =  4, F2I                         =  5,
            F2L                         =  6, D2I                         =  7,
            D2L                         =  8, D2F                         =  9,
            L2F                         = 10, L2D                         = 11,
            DSIN                        = 12, DCOS                        = 13,
            SIMPLE_ARRAY_COPY_NO_CHECKS = 14, DEBUG                       = 15;
    
    /**
     * Don't let anyone instantiate this class.
     */
    private Runtime1() {}
    
    /**
     * Returns the size of an object after alignment.
     *
     * @param   size  original object size
     * @return  object size after alignment
     */
    public static native int alignObjectSize(int size);
    
    /**
     * Stops the execution of the JVM for debugging.
     */
    public static native void debugHalt();
    
    /**
     * Returns the offset of the pointer to the block of handles.
     *
     * @return  offset of the pointer to active handles
     */
    public static native int getActiveHandlesOffset();
    
    /**
     * Returns the address of the FPU control word that switches to 24-bit mode
     * and rounding to nearest.
     *
     * @return  address of the control word
     */
    public static native int getAddrFpuCntrlWrd24();
    
    /**
     * Returns the address of the FPU control word that switches to 53-bit mode
     * and rounding to nearest.
     *
     * @return  address of the control word
     */
    public static native int getAddrFpuCntrlWrdStd();

    /**
     * Returns the address of the FPU control word that switches to 53-bit mode
     * and rounding to zero.
     *
     * @return  address of the control word
     */
    public static native int getAddrFpuCntrlWrdTrunc();
    
    /**
     * Returns the byte offset of the first element in array objects.
     *
     * @param   type  component type of the array
     * @return  array base offset in bytes
     */
    public static native int getArrayBaseOffset(int type);
    
    /**
     * Returns the offset of the pointer to the element class of an object
     * array.
     *
     * @return  element class offset in bytes
     */
    public static native int getArrayElementKlassOffset();
    
    /**
     * Returns the header size of an array with the specified element type.
     *
     * @param   type  component type of the array
     * @return  array header size in words
     */
    public static native int getArrayHeaderSize(int type);

    /**
     * Returns the byte offset of the implicit length field in array objects.
     *
     * @return  array length offset in bytes
     */
    public static native int getArrayLengthOffset();

    /**
     * Returns the size of stack banging used to predetect a possible stack
     * overflow for exception processing.
     *
     * @return  offset to bang the stack with
     */
    public static native int getBangStackSizeForExceptions();
    
    /**
     * Returns the offset of the lock within a basic object lock.
     *
     * @return  basic object lock offset
     */
    public static native int getBasicObjectLockOffset();
    
    /**
     * Returns the size of the basic object lock.
     *
     * @return  basic object lock size
     */
    public static native int getBasicObjectLockSize();

    /**
     * Returns the address of the breakpoint function.
     *
     * @return  address of the breakpoint function.
     */
    public static native int getBreakpointFnPtr();
    
    /**
     * Returns the shift distance used to access the card marking array.
     *
     * @return  the card shift distance
     */
    public static native int getCardShift();
    
    /**
     * Returns the offset of the displaced header within a basic lock.
     *
     * @return  displaced header offset in bytes
     */
    public static native int getDisplacedHeaderOffset();
    
    /**
     * Retrieves the current value of the high-resolution performance counter.
     *
     * @return  current value of counter
     */
    public static native long getElapsedCounter();
    
    /**
     * Retrieves the frequency of the high-resolution performance counter.
     *
     * @return  frequency of counter
     */
    public static native long getElapsedFrequency();
    
    /**
     * Returns the entry of the stub that forwards an exception. This address
     * is the continuation point for runtime calls returning with a pending
     * exception.
     *
     * @return  forward exception stub entry
     */
    public static native int getForwardExceptionEntry();
    
    /**
     * Returns the state of a fully initialized class.
     *
     * @return  the fully initialized state
     */
    public static native int getFullyInitializedState();
    
    /**
     * Returns the offset of the index of the next unused handle.
     *
     * @return  handle block top offset
     */
    public static native int getHandleBlockTopOffset();
    
    /**
     * Returns the bit mask that selects the hash code of an object.
     *
     * @return  the hash mask in place
     */
    public static native int getHashMaskInPlace();
    
    /**
     * Returns the number of bits that the hash code is shifted to the left.
     *
     * @return  the hash code shift distance
     */
    public static native int getHashShift();

    /**
     * Returns the address of the field that points to the end of the space
     * used for inlined allocation.
     *
     * @return  address of the end field
     */
    public static native int getHeapEndAddr();
    
    /**
     * Returns the address of the field that points to the top of the space
     * used for inlined allocation.
     *
     * @return  address of the top field
     */
    public static native int getHeapTopAddr();
    
    /**
     * Returns the offset of the initialization state field.
     *
     * @return  initialization state offset in bytes
     */
    public static native int getInitStateOffset();
    
    /**
     * Returns the offset of the first element in the subtype cache.
     *
     * @return  offset of the first cache element
     */
    public static native int getIsACache1Offset();
    
    /**
     * Returns the offset of the second element in the subtype cache.
     *
     * @return  offset of the second cache element
     */
    public static native int getIsACache2Offset();
    
    /**
     * Returns the offset of the field that specifies whether or not an implicit
     * exception is handled.
     *
     * @return  field offset in bytes
     */
    public static native int getIsHandlingImplicitExceptionOffset();
    
    /**
     * Returns the offset of the JNI environment of a thread.
     *
     * @return  JNI environment offset
     */
    public static native int getJniEnvOffset();
    
    /**
     * Returns the byte offset of the class pointer in object headers.
     *
     * @return  class pointer offset in bytes
     */
    public static native int getKlassOffset();
    
    /**
     * Returns the offset of the field that stores the last frame pointer.
     *
     * @return  last frame pointer offset
     */
    public static native int getLastJavaFpOffset();
    
    /**
     * Returns the offset of the field that stores the last stack pointer.
     *
     * @return  last stack pointer offset
     */
    public static native int getLastJavaSpOffset();
    
    /**
     * Returns the offset of the field that stores the last native position.
     *
     * @return  last native position offset
     */
    public static native int getLastNativePosOffset();

    /**
     * Returns the byte offset of the mark word in object headers.
     *
     * @return  mark offset in bytes
     */
    public static native int getMarkOffset();

    /**
     * Returns a prototype for the mark word in object headers.
     *
     * @return  mark word prototype
     */
    public static native int getMarkPrototype();

    /**
     * Returns the value that the mark word equals if the object is unlocked.
     *
     * @return  mark value of unlocked object
     */
    public static native int getMarkUnlockedValue();
    
    /**
     * Returns the offset of the bottom of the monitor block in an interpreter
     * frame.
     *
     * @return  monitor block bottom offset
     */
    public static native int getMonitorBlockBottomOffset();
    
    /**
     * Returns the state used when threads are not synchronized at a safepoint.
     *
     * @return  not safepoint synchronized state
     */
    public static native int getNotSafepointSyncState();
    
    /**
     * Returns the header size of an object pointer descriptor.
     *
     * @return  descriptor header size
     */
    public static native int getOopDescHeaderSize();
    
    /**
     * Returns the offset of the field for a pending exception.
     *
     * @return  offset of field for pending exception
     */
    public static native int getPendingExceptionOffset();
    
    /**
     * Returns the maximum number of recursive compilations allowed per thread.
     *
     * @return  recursive compilation limit
     */
    public static native int getRecursiveCompileLimit();
    
    /**
     * Returns the card marking array base of the remembered set.
     *
     * @return  card marking array base
     */
    public static native int getRSByteMapBase();
    
    /**
     * Returns the address of the runtime function with the specified number.
     *
     * @param   fnId  number of the runtime function
     * @return  address of the runtime function
     */
    public static native int getRuntimeFnPtr(int fnId);
    
    /**
     * Returns the address of the safepoint synchronization state.
     *
     * @return  safepoint synchronization state address
     */
    public static native int getSafepointSyncStateAddr();
    
    /**
     * Returns the entry for the stub routine with the specified number.
     *
     * @param   stubId  number of the stub routine
     * @return  entry address of the stub routine
     */
    public static native int getStubEntry(int stubId);
    
    /**
     * Returns the identification number of the current thread.
     *
     * @return  thread identification number
     */
    public static native int getThreadId();
    
    /**
     * Returns the state of threads running in Java or in stub code.
     *
     * @return  state for Java threads
     */
    public static native int getThreadInJavaState();
    
    /**
     * Returns the state of threads running in native code.
     *
     * @return  state for native threads
     */
    public static native int getThreadInNativeState();
        
    /**
     * Returns the offset of the field that specifies the thread state.
     *
     * @return  thread state field offset
     */
    public static native int getThreadStateOffset();
    
    /**
     * Returns the array offset of the thread local storage.
     *
     * @return  the array offset
     */
    public static native int getTLSArrayOffset();

    /**
     * Returns the base offset of the thread local storage.
     *
     * @return  the base offset
     */
    public static native int getTLSBaseOffset();
    
    /**
     * Returns the thread offset of the thread local storage.
     *
     * @return  the thread offset
     */
    public static native int getTLSThreadOffset();
    
    /**
     * Returns the size in bytes of one page in virtual memory.
     *
     * @return  virtual memory page size
     */
    public static native int getVMPageSize();
    
    /**
     * Returns the offset of the oop result that is passed back into Java code.
     *
     * @return  virtual machine result offset
     */
    public static native int getVMResultOffset();

    /**
     * Tests if this program runs on a multi-processor machine.
     *
     * @return  whether this is a multi-processor machine or not
     */
    public static native boolean isMultiProcessor();
    
    /**
     * Tests if this program runs on a P6 family processor.
     *
     * @return  whether this is a P6 processor or not
     */
    public static native boolean isP6();
    
    /**
     * Stores an object pointer in the specified array.
     *
     * @param  array  array of object pointers
     * @param  index  index to store pointer at
     * @param  obj    object pointer to be stored
     */
    public static native void setOopAt(Object[] array, int index, Object obj);
}
