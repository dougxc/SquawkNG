package com.sun.squawk.suite;

import java.io.*;
import com.sun.squawk.vm.SquawkConstants;

/**
 * A SuiteMethodImpl instance is a container for the information present in a
 * suite file for a method's implementation. It corresponds with the
 * 'MethodImpl_info' structure described in the Suite File format.
 */
public class SuiteMethodImpl {

    public char                 parent;
    public int                  entry;
    public int                  flags;
    public char[]               locals;
    private int                 stackSize;
    public ExceptionHandler[]   handlers;
    public int                  codeSize;
    public byte[]               bytecodes;
    public DataInputStream      inputStream;

    private SuiteMethodImpl() {
    }

    public static SuiteMethodImpl create(
                                      char               parent,
                                      int                entry,
                                      int                flags,
                                      char[]             locals,
                                      int                stackSize,
                                      ExceptionHandler[] handlers,
                                      int                codeSize,
                                      boolean            useShared,
                                      DataInputStream    inputStream
                                   ) {

        SuiteMethodImpl methodImpl;

        //if (!useShared) {
            methodImpl = new SuiteMethodImpl();
        //} else {
        //    methodImpl = sharedInstance;
        //}

        methodImpl.parent      = parent;
        methodImpl.entry       = entry;
        methodImpl.flags       = flags;
        methodImpl.locals      = locals;
        methodImpl.stackSize   = stackSize;
        methodImpl.handlers    = handlers;
        methodImpl.codeSize    = codeSize;
        methodImpl.inputStream = inputStream;
        return methodImpl;
    }


    /**
     * Get the max stack size for this method. The value returned may actually
     * be greater than the size specified in the corresponding suite file. This
     * is to account for the way in which the following instructions are
     * implemented:
     *
     * class_putstatic/class_getstatic: both of these instructions use two more
     *     stack words than specified in the case the current class has not yet
     *     completed initialization
     *
     * this_getfield/this_putfield: both of these instructions use one more
     *     stack word than specified as they push the current receiver onto
     *     the stack.
     *
     * Ideally, a parse through the bytecode could determine if the method
     * actually had any of these instructions and therefore the exact
     * correction (if any) could be made to the max_stack value. However,
     * this would require a buffer for the bytecode which may not be possible
     * in a resource constrained device such as a JavaCard.
     * @return the implementation required max stack size for this method.
     */
    public int stackSize() {
        return stackSize + 2;
    }


    public boolean isAbstract() { return (flags & SquawkConstants.ACC_ABSTRACT) != 0; }
    public boolean isNative()   { return (flags & SquawkConstants.ACC_NATIVE)   != 0; }
    public boolean isStatic()   { return (flags & SquawkConstants.ACC_STATIC)   != 0; }

}


