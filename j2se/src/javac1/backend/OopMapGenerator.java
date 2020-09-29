/*
 * @(#)OopMapGenerator.java             1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import javac1.Bytecodes;
import javac1.BytecodeStream;
import javac1.backend.oops.*;
import javac1.ci.ExceptionHandler;
import javac1.ci.Method;

/**
 * Records the indices of local variables that can contain pointers.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class OopMapGenerator extends GenerateOopMap {
    /**
     * The maximum number of local variables in the method.
     */
    private int maxLocals;
    
    /**
     * Whether the method has exception handlers or not.
     */
    private boolean hasExceptionHandlers;

    /**
     * Whether the method is declared as synchronized or not.
     */
    private boolean isSynchronized;

    /**
     * The bit set that specifies where garbage collection is possible.
     */
    private BitSet isGCPoint;
    
    /**
     * The list of local variables that will have to be initialized.
     */
    private List initVars;
    
    /**
     * The mapping of bytecode indices to a set of indices of local variables
     * that can store references.
     */
    private Map cachedOops;

    /**
     * Constructs a new pointer map generator for the specified method.
     *
     * @param  method  the method
     */
    public OopMapGenerator(Method method) {
        super(method);
        maxLocals = method.getMaxLocals();
        hasExceptionHandlers = method.hasExceptionHandlers();
        isSynchronized = method.isSynchronized();
        isGCPoint = new BitSet();
        ListIterator itr = method.getExceptionHandlers().listIterator();
        while (itr.hasNext()) {
            ExceptionHandler handler = (ExceptionHandler) itr.next();
            isGCPoint.set(handler.getTarget());
        }
        BytecodeStream bcs = new BytecodeStream(method.getCode());
        while (bcs.next()) {
            if (bcs.getBytecode() == Bytecodes._jsr) {
                isGCPoint.set(bcs.getNextBci());
            }
        }
        cachedOops = new HashMap();
    }

    /**
     * Tests if garbage collection is possible at the current position of the
     * specified bytecode stream.
     *
     * @param   bcs  the bytecode stream
     * @return  whether garbage collection is possible or not
     */
    public boolean possibleGCPoint(BytecodeStream bcs) {
        int bci = bcs.getBci();
        int code = bcs.getBytecode();
        if (((isGCPoint != null) && isGCPoint.get(bci))
                || (isSynchronized && (bci == 0))) {
            return true;
        }
        switch (code) {
        case Bytecodes._iaload:
            /* falls through */
        case Bytecodes._laload:
            /* falls through */
        case Bytecodes._faload:
            /* falls through */
        case Bytecodes._daload:
            /* falls through */
        case Bytecodes._aaload:
            /* falls through */
        case Bytecodes._baload:
            /* falls through */
        case Bytecodes._caload:
            /* falls through */
        case Bytecodes._saload:
            /* falls through */
        case Bytecodes._iastore:
            /* falls through */
        case Bytecodes._lastore:
            /* falls through */
        case Bytecodes._fastore:
            /* falls through */
        case Bytecodes._dastore:
            /* falls through */
        case Bytecodes._bastore:
            /* falls through */
        case Bytecodes._castore:
            /* falls through */
        case Bytecodes._sastore:
            /* falls through */
        case Bytecodes._idiv:
            /* falls through */
        case Bytecodes._ldiv:
            /* falls through */
        case Bytecodes._irem:
            /* falls through */
        case Bytecodes._lrem:
            /* falls through */
        case Bytecodes._arraylength:
            return hasExceptionHandlers || isSynchronized;
        case Bytecodes._athrow:
            return true;
        case Bytecodes._getstatic:
            /* falls through */
        case Bytecodes._putstatic:
            /* falls through */
        case Bytecodes._getfield:
            /* falls through */
        case Bytecodes._putfield:
            /* falls through */
        case Bytecodes._invokevirtual:
            /* falls through */
        case Bytecodes._invokespecial:
            /* falls through */
        case Bytecodes._invokestatic:
            /* falls through */
        case Bytecodes._invokeinterface:
            return true;
        case Bytecodes._new:
            /* falls through */
        case Bytecodes._newarray:
            /* falls through */
        case Bytecodes._anewarray:
            /* falls through */
        case Bytecodes._multianewarray:
            return true;
        case Bytecodes._aastore:
            /* falls through */
        case Bytecodes._checkcast:
            /* falls through */
        case Bytecodes._instanceof:
            /* falls through */
        case Bytecodes._monitorenter:
            return true;
        case Bytecodes._jsr_w:
            /* falls through */
        case Bytecodes._jsr:
            /* falls through */
        case Bytecodes._ret:
            return javac1.Flags.UseCompilerSafepoints;
        case Bytecodes._tableswitch:
            /* falls through */
        case Bytecodes._lookupswitch:
            /* falls through */
        case Bytecodes._goto_w:
            /* falls through */
        case Bytecodes._goto:
            /* falls through */
        case Bytecodes._ifeq:
            /* falls through */
        case Bytecodes._ifne:
            /* falls through */
        case Bytecodes._iflt:
            /* falls through */
        case Bytecodes._ifge:
            /* falls through */
        case Bytecodes._ifgt:
            /* falls through */
        case Bytecodes._ifle:
            /* falls through */
        case Bytecodes._ifnull:
            /* falls through */
        case Bytecodes._ifnonnull:
            /* falls through */
        case Bytecodes._if_icmpeq:
            /* falls through */
        case Bytecodes._if_icmpne:
            /* falls through */
        case Bytecodes._if_icmplt:
            /* falls through */
        case Bytecodes._if_icmpge:
            /* falls through */
        case Bytecodes._if_icmpgt:
            /* falls through */
        case Bytecodes._if_icmple:
            /* falls through */
        case Bytecodes._if_acmpeq:
            /* falls through */
        case Bytecodes._if_acmpne:
            return javac1.Flags.UseCompilerSafepoints;
        default:
            return false;
        }
    }
    
    /**
     * This method is called just before the pointer maps are filled.
     *
     * @param  nofGCPoints  number of garbage collection points
     */
    public void fillStackmapProlog(int nofGCPoints) {
        /* nothing to do */
    }
    
    /**
     * This method is called after the pointer maps have been filled.
     */
    public void fillStackmapEpilog() {
        /* nothing to do */
    }
    
    /**
     * Fills the pointer map for the current bytecode index with the specified
     * information.
     *
     * @param  bcs       the bytecode stream
     * @param  vars      cell type states of the local variables
     * @param  stack     cell type states of stack locations
     * @param  stackTop  top of stack
     */
    public void fillStackmapForOpcodes(BytecodeStream bcs, CellTypeStateList vars,
            CellTypeStateList stack, int stackTop) {
        if (possibleGCPoint(bcs) && (maxLocals > 0)) {
            BitSet oops = new BitSet(maxLocals);
            for (int i = 0; i < vars.size(); i++) {
                if (vars.get(i).isReference()) {
                    oops.set(i);
                }
            }
            cachedOops.put(new Integer(bcs.getBci()), oops);
        }
    }
    
    /**
     * Sets the list of local variables that will have to be initialized.
     *
     * @param  initVars  list of variables to be initialized
     */
    public void fillInitVars(List initVars) {
        int size = initVars.size();
        this.initVars = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            this.initVars.add(initVars.get(i));
        }
    }
    
    /**
     * Returns the list of local variables that will have to be initialized.
     *
     * @return  list of variables to be initialized
     */
    public List getInitVars() {
        return initVars;
    }
    
    /**
     * Returns the mapping of bytecode indices to pointer maps.
     *
     * @return  set of cached pointer maps
     */
    public Map getCachedOops() {
        return cachedOops;
    }
}
