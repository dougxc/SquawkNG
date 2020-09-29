/*
 * @(#)AddressMapGenerator.java         1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend;

import java.util.ArrayList;
import java.util.List;
import javac1.BytecodeStream;
import javac1.backend.oops.*;
import javac1.ci.Method;

/**
 * Records the indices of local variables that are of address type.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class AddressMapGenerator extends GenerateOopMap {
    /**
     * The bytecode index to generate address map for.
     */
    private int forBci;
    
    /**
     * The list of local variables that are of address type.
     */
    private List addressMap;

    /**
     * Constructs a new address map generator for the specified method.
     *
     * @param  method  the method
     * @param  forBci  bytecode index to generate map for
     */
    public AddressMapGenerator(Method method) {
        super(method);
        this.forBci = forBci;
        this.addressMap = new ArrayList();
    }

    /**
     * Tests if garbage collection is possible at the current position of the
     * specified bytecode stream.
     *
     * @param   bcs  the bytecode stream
     * @return  whether garbage collection is possible or not
     */
    public boolean possibleGCPoint(BytecodeStream bcs) {
        return bcs.getBci() == forBci;
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
        if (possibleGCPoint(bcs) && (method().getMaxLocals() > 0)) {
            for (int i = 0; i < vars.size(); i++) {
                if (vars.get(i).isAddress()) {
                    addressMap.add(new Integer(i));
                }
            }
        }
    }
    
    /**
     * Sets the list of local variables that will have to be initialized.
     *
     * @param  initVars  list of variables to be initialized
     */
    public void fillInitVars(List initVars) {
        /* nothing to do */
    }
    
    /**
     * Returns the list of local variables that are of address type.
     *
     * @return  the address map
     */
    public List getAddressMap() {
        return addressMap;
    }
}
