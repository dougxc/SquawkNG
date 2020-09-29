/*
 * @(#)IR.java                          1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir;

import java.util.List;
import javac1.Assert;
import javac1.ci.Method;
import javac1.ir.instr.Base;
import javac1.ir.instr.BlockBegin;

/**
 * This class encapsulates the intermediate representation of a method. It
 * contains a hierarchy of scopes for the method to be compiled and each inlined
 * method. The control flow graph is made up of the basic blocks of the
 * corresponding method. Within a basic block individual bytecodes are
 * represented via a sequential list of instruction nodes.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class IR {
    /**
     * The next number used for autonumbering basic blocks.
     */
    private int nextBlockId;
    
    /**
     * The next number used for autonumbering instructions.
     */
    private static int nextInstrId = 0;

    /**
     * The root of the scope hierarchy.
     */
    private IRScope topScope;

    /**
     * The relevant loops of the intermediate representation.
     */
    private List loops;

    /**
     * The blocks in code generation order with use counts.
     */
    private BlockCollection code;

    /**
     * Information about the basic blocks in the control flow graph.
     */
    private ScanBlocks scanResult;

    /**
     * Constructs a new intermediate representation.
     *
     * @param  method  the method to be compiled
     */
    public IR(Method method) {
        nextBlockId = 0;
        topScope = new IRScope(this, null, method);
        loops = null;
        code = null;
        scanResult = null;
    }

    /**
     * Returns an unique identification number for a new basic block.
     *
     * @return  next block identification number
     */
    public int getNextBlockId() {
        return nextBlockId++;
    }
    
    /**
     * Returns the number of blocks that have been created so far.
     *
     * @return  the number of blocks
     */
    public int countBlocks() {
        return nextBlockId;
    }
    
    /**
     * Returns an unique identification number for a new instruction.
     *
     * @return  next instruction identification number
     */
    public int getNextInstrId() {
        return nextInstrId++;
    }

    /**
     * Returns the root of the scope hierarchy.
     *
     * @return  the top scope
     */
    public IRScope getTopScope() {
        return topScope;
    }

    /**
     * Returns the method to be compiled.
     *
     * @return  the method to be compiled
     */
    public Method getMethod() {
        return topScope.getMethod();
    }

    /**
     * Returns the number of monitor lock slots needed.
     *
     * @return  the number of lock slots needed
     */
    public int getNumberOfLocks() {
        return topScope.getNumberOfLocks();
    }

    /**
     * Sets the list of relevant loops of the intermediate representation.
     *
     * @param  loops  the list of loops
     */
    public void setLoops(List loops) {
        this.loops = loops;
    }

    /**
     * Returns the list of relevant loops of the intermediate representation.
     *
     * @return  the list of loops
     */
    public List getLoops() {
        return loops;
    }

    /**
     * Stores a reference to the collection that contains the basic blocks in
     * code generation order with use counts.
     *
     * @param  code  sorted collection of basic blocks
     */
    public void setCode(BlockCollection code) {
        this.code = code;
        if (code != null) {
            scanResult = new ScanBlocks();
            code.iterateForward(scanResult);
        }
    }

    /**
     * Returns the collection of basic blocks in code generation order with use
     * counts.
     *
     * @return  the collection of basic blocks
     */
    public BlockCollection getCode() {
        return code;
    }

    /**
     * Returns information about the basic blocks in the control flow graph.
     *
     * @return  information about the basic blocks
     * @see     #setCode(BlockCollection)
     */
    public ScanBlocks getScanResult() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(code != null, "code has not been set yet");
        }
        return scanResult;
    }

    /**
     * Tests if this intermediate representation is valid.
     *
     * @return  whether or not the intermediate representation is valid
     */
    public boolean isValid() {
        return topScope.isValid();
    }

    /**
     * Asserts that this intermediate representation is valid.
     */
    private void assertValid() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isValid(), "ir must be valid");
        }
    }

    /**
     * Iterates over all basic blocks of the intermediate representation and
     * passes them to the specified block closure. Each block is processed
     * <strong>before</strong> the blocks will be handled that can be reached
     * from it.
     *
     * @param  bc  the block closure
     */
    public void iteratePreorder(BlockClosure bc) {
        assertValid();
        topScope.getStart().iteratePreorder(bc);
    }

    /**
     * Iterates over all basic blocks of the intermediate representation and
     * passes them to the specified block closure. Each block is processed
     * <strong>after</strong> all the blocks have been handled that can be
     * reached from it.
     *
     * @param  bc  the block closure
     */
    public void iteratePostorder(BlockClosure bc) {
        assertValid();
        topScope.getStart().iteratePostorder(bc);
    }

    /**
     * Iterates over all basic blocks of the intermediate representation in
     * topological order and passes them to the specified block closure.
     *
     * @param  bc  the block closure
     */
    public void iterateTopological(BlockClosure bc) {
        assertValid();
        topScope.getStart().iterateTopological(bc);
    }

    /**
     * Applies the operation represented by the specified block closure to all
     * basic blocks of the intermediate representation while setting their
     * weights in one single pass.
     *
     * @param  bc  the block closure
     */
    public void iterateAndSetWeight(BlockClosure bc) {
        assertValid();
        topScope.getStart().iterateAndSetWeight(bc);
    }
    
    /**
     * Returns the entry block for on-stack-replacement.
     *
     * @param  the OSR entry block
     */
    public BlockBegin getOsrEntry() {
        return ((Base) topScope.getStart().getEnd()).getOsrEntry();
    }
}
