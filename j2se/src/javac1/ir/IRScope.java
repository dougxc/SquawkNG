/*
 * @(#)IRScope.java                     1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir;

import java.util.ArrayList;
import java.util.List;
import javac1.Assert;
import javac1.ci.Method;
import javac1.ir.instr.Base;
import javac1.ir.instr.BlockBegin;
import javac1.ir.instr.BlockEnd;
import javac1.ir.instr.Goto;

/**
 * This class represents a scope of the intermediate representation. Whenever a
 * method can be inlined a new scope will be created for it.
 *
 * @see      IR
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class IRScope {
    /**
     * The intermediate representation that this scope belongs to.
     */
    private IR parent;
    
    /**
     * The inlining level.
     */
    private int level;

    /**
     * The caller scope.
     */
    private IRScope caller;

    /**
     * The corresponding method.
     */
    private Method method;

    /**
     * The inlined method scopes.
     */
    private List callees;

    /**
     * The list of exception handlers in this scope.
     */
    private List xhandlers;

    /**
     * The start block whose successors are method entries.
     */
    private BlockBegin start;

    /**
     * The number of monitor lock slots needed in this scope.
     */
    private int numberOfLocks;

    /**
     * Whether or not the bytecodes of this scope contain loops.
     */
    private boolean hasLoops;

    /**
     * Constructs a new scope of the intermediate representation.
     *
     * @param  parent  the intermediate representation
     * @param  caller  the caller scope
     * @param  method  the corresponding method
     */
    public IRScope(IR parent, IRScope caller, Method method) {
        this.parent = parent;
        this.level = (caller == null) ? 0 : caller.getLevel() + 1;
        this.caller = caller;
        this.method = method;
        this.callees = new ArrayList();
        this.xhandlers = method.getExceptionHandlers();
        this.start = null;
        this.numberOfLocks = 0;
        this.hasLoops = false;
    }
    
    /**
     * Returns the intermediate representation that this scope belongs to.
     *
     * @return  parent intermediate representation
     */
    public final IR getParent() {
        return parent;
    }

    /**
     * Constructs a new header block for the specified entry block.
     *
     * @param   entry  the entry block
     * @param   flag   flag of the header block
     * @return  the constructed header block
     */
    private static BlockBegin headerBlock(BlockBegin entry, int flag) {
        if (entry == null) {
            return null;
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(entry.isFlagSet(flag), "entry/flag mismatch");
        }
        BlockBegin header = new BlockBegin(entry.getScope(), entry.getBci());
        BlockEnd end = new Goto(entry.getScope(), entry);
        header.append(end, entry.getBci());
        header.setEnd(end);
        header.setFlag(flag);
        ValueStack state = (ValueStack) entry.getState().clone();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(state.isStackEmpty(), "must have empty stack at entry point");
        }
        end.setState(state);
        return header;
    }

    /**
     * Sets the start block of this scope. The successor of the new start block
     * is a base block that points to the header block built from the data of
     * the specified entry block.
     *
     * @param  stdEntry  the standard entry block
     * @param  osrEntry  the OSR entry block
     * @see    Base
     */
    public void setStart(BlockBegin stdEntry, BlockBegin osrEntry) {
        start = new BlockBegin(this, 0);
        Base base = new Base(this, headerBlock(stdEntry, BlockBegin.STD_ENTRY_FLAG),
            headerBlock(osrEntry, BlockBegin.OSR_ENTRY_FLAG));
        start.append(base, 0);
        start.setEnd(base);
        ValueStack state = new ValueStack(this, method.getMaxLocals());
        start.setState(state);
        base.setState((ValueStack) state.clone());
        base.getStdEntry().join(state);
        if (osrEntry != null) {
            base.getOsrEntry().join(state);
        }
    }

    /**
     * Returns the start block of this scope.
     *
     * @return  the start block
     */
    public BlockBegin getStart() {
        return start;
    }

    /**
     * Returns the current inlining level.
     *
     * @return  the inlining level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Returns the caller scope.
     *
     * @return  the caller scope
     */
    public IRScope getCaller() {
        return caller;
    }

    /**
     * Returns the corresponding method.
     *
     * @return  the corresponding method
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Appends the specified scope to the list of callees.
     *
     * @param  callee  the called scope
     */
    public void addCallee(IRScope callee) {
        callees.add(callee);
    }

    /**
     * Returns the number of scopes that are called from within this scope.
     *
     * @return  the number of callees
     */
    public int countCallees() {
        return callees.size();
    }

    /**
     * Returns the callee at the specified index.
     *
     * @param   index  index into the list of callees
     * @return  the callee at the specified index
     */
    public IRScope calleeAt(int index) {
        return (IRScope) callees.get(index);
    }

    /**
     * Returns the list of exception handlers in this scope.
     *
     * @return  the exception handlers
     */
    public List getExceptionHandlers() {
        return xhandlers;
    }

    /**
     * Ensures that the stored number of locks is at least equal to the
     * specified minimum.
     *
     * @param  count  number of locks
     */
    public void setMinNumberOfLocks(int count) {
        if (count > numberOfLocks) {
            numberOfLocks = count;
        }
    }

    /**
     * Returns the number of monitor lock slots needed in this scope.
     *
     * @return  the number of lock slots needed
     */
    public int getNumberOfLocks() {
        return numberOfLocks;
    }

    /**
     * Sets whether or not loops have been detected in the bytecodes.
     *
     * @param  hasLoops  whether or not the method contains loops
     */
    public void setHasLoops(boolean hasLoops) {
        this.hasLoops = hasLoops;
    }

    /**
     * Returns whether or not loops have been detected in the bytecodes.
     *
     * @return  whether or not the method contains loops
     */
    public boolean hasLoops() {
        return hasLoops;
    }

    /**
     * Tests if this scope is valid, that is has a start block.
     *
     * @return  whether or not this scope is valid
     */
    public boolean isValid() {
        return start != null;
    }
}
