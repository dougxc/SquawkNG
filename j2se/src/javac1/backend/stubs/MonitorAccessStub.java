/*
 * @(#)MonitorAccessStub.java           1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.stubs;

import javac1.backend.code.Register;

/**
 * The code stub for accessing the monitor of an object.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public abstract class MonitorAccessStub extends CodeStub {
    /**
     * The register that points to the affected object.
     */
    private Register objReg;

    /**
     * The register that stores the address of the basic lock.
     */
    private Register lockReg;

    /**
     * Initializes the attributes declared in this class.
     *
     * @param  objReg   points to the affected object
     * @param  lockReg  stores the address of the basic lock
     */
    protected MonitorAccessStub(Register objReg, Register lockReg) {
        this.objReg = objReg;
        this.lockReg = lockReg;
    }

    /**
     * Returns the register that points to the affected object.
     *
     * @return  register that points to the affected object
     */
    public Register getObjReg() {
        return objReg;
    }

    /**
     * Returns register that contains the address to the basic lock.
     *
     * @return  register that stores the address of the basic lock
     */
    public Register getLockReg() {
        return lockReg;
    }
}
