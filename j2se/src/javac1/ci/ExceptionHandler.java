/*
 * @(#)ConstantPool.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ci;

import javac1.Assert;
import javac1.ir.instr.BlockBegin;

/**
 * This class represents an exception handler for a method.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class ExceptionHandler {
    /**
     * The loader to be used for resolving the exception class.
     */
    private InstanceKlass loader;
    
    /**
     * The start of the range in the code array at which the handler is active.
     */
    private int start;

    /**
     * The end of the range in the code array at which the handler is active.
     */
    private int limit;

    /**
     * The starting bytecode index of the exception handler.
     */
    private int target;

    /**
     * The index of the exception class that this handler catches.
     */
    private int typeIndex;
    
    /**
     * The exception class that this handler catches.
     */
    private InstanceKlass type;

    /**
     * The exception handler entry block.
     */
    private BlockBegin entry;

    /**
     * Constructs a new exception handler.
     *
     * @param  loader     the loader class  
     * @param  start      start of the corresponding try block
     * @param  limit      end of the try block
     * @param  target     start of the exception handler
     * @param  typeIndex  index of the class that this handler catches
     */
    protected ExceptionHandler(InstanceKlass loader, int start, int limit,
            int target, int typeIndex) {
        this.loader = loader;
        this.start = start;
        this.limit = limit;
        this.target = target;
        this.typeIndex = typeIndex;
        this.type = null;
        this.entry = null;
    }

    /**
     * Sets the exception handler entry block.
     *
     * @param  entry  the entry block
     */
    public void setEntry(BlockBegin entry) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(entry.isFlagSet(BlockBegin.EXCEPTION_ENTRY_FLAG), "must be an exception handler entry");
            Assert.that(entry.getBci() == target, "bytecode indices must correspond");
        }
        this.entry = entry;
    }

    /**
     * Returns the exception handler entry block.
     *
     * @return  the entry block
     */
    public BlockBegin getEntry() {
        return entry;
    }

    /**
     * Returns the start of the range in the code array at which the handler is
     * active.
     *
     * @return  start of the try block
     */
    public int getStart() {
        return start;
    }

    /**
     * Returns the end of the range in the code array at which the handler is
     * active.
     *
     * @return  end of the try block
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Returns the starting bytecode index of this exception handler.
     *
     * @return  the start of this handler
     */
    public int getTarget() {
        return target;
    }

    /**
     * Returns the index of the exception class that this handler catches.
     *
     * @return  index of the class that this handler catches
     */
    public int getTypeIndex() {
        return typeIndex;
    }
    
    /**
     * Returns the exception class that this handler catches.
     *
     * @return  class that this handler catches
     */
    public InstanceKlass getType() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(typeIndex != 0, "should not reach here");
        }
        if (type == null) {
            type = (InstanceKlass) loader.getConstants().getKlassAt(typeIndex);
        }
        return type;
    }

    /**
     * Returns whether this handler is active at the specified bytecode index.
     *
     * @param   bci  the bytecode index
     * @return  whether the range covers the specified index
     */
    public boolean covers(int bci) {
        return (start <= bci) && (bci < limit);
    }
}
