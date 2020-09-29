/*
 * @(#)MonitorExitStub.java             1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.stubs;

import javac1.backend.code.CodeEmitter;
import javac1.backend.code.Address;
import javac1.backend.code.Register;
import javac1.backend.code.RelocInfo;
import javac1.ci.Runtime1;

/**
 * The code stub for exiting the monitor of an object.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class MonitorExitStub extends MonitorAccessStub {
    /**
     * Whether or not the effective address of the basic lock is recomputed.
     */
    private boolean computeLock;

    /**
     * The address of the basic lock.
     */
    private Address lockAddr;

    /**
     * Constructs a new code stub for exiting the monitor of an object.
     *
     * @param  objReg       points to the object to be unlocked
     * @param  lockReg      stores the address of the basic lock
     * @param  computeLock  whether the address of the lock is recomputed
     * @param  lockAddr     the address of the basic lock
     */
    public MonitorExitStub(Register objReg, Register lockReg,
            boolean computeLock, Address lockAddr) {
        super(objReg, lockReg);
        this.computeLock = computeLock;
        this.lockAddr = lockAddr;
    }

    public void emitCode(CodeEmitter ce) {
        ce.getMasm().bind(getEntry());
        if (computeLock) {
            ce.getMasm().leal(getLockReg(), lockAddr);
        }
        ce.pushReg(getObjReg());
        ce.pushReg(getLockReg());
        ce.getMasm().call(Runtime1.getStubEntry(Runtime1.MONITOREXIT_ID),
            RelocInfo.RUNTIME_CALL_TYPE);
        ce.decStackAfterCall(2);
        ce.getMasm().jmp(getContinuation());
    }
}
