/*
 * @(#)MonitorEnterStub.java            1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.stubs;

import javac1.backend.code.CodeEmitter;
import javac1.backend.code.Register;
import javac1.backend.code.RelocInfo;
import javac1.backend.debug.CodeEmitInfo;
import javac1.ci.Runtime1;

/**
 * The code stub for entering the monitor of an object.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class MonitorEnterStub extends MonitorAccessStub {
    /**
     * The associated debug information.
     */
    private CodeEmitInfo info;

    /**
     * The offset of the stack pointer.
     */
    private int espOffset;

    /**
     * Constructs a new code stub for entering the monitor of an object.
     *
     * @param  objReg     points to the object to be locked
     * @param  lockReg    stores the address of the basic lock
     * @param  info       associated debug information
     * @param  espOffset  offset of the stack pointer
     */
    public MonitorEnterStub(Register objReg, Register lockReg,
            CodeEmitInfo info, int espOffset) {
        super(objReg, lockReg);
        this.info = info;
        this.espOffset = espOffset;
    }

    public void emitCode(CodeEmitter ce) {
        ce.setEspOffset(espOffset);
        ce.getMasm().bind(getEntry());
        ce.pushReg(getObjReg());
        ce.pushReg(getLockReg());
        ce.getMasm().call(Runtime1.getStubEntry(Runtime1.MONITORENTER_ID),
            RelocInfo.RUNTIME_CALL_TYPE);
        ce.addCallInfoHere(info);
        ce.decStackAfterCall(2);
        if (!ce.getMethod().isStatic()) {
            ce.loadReceiver(CodeEmitter.RECV_REG);
        }
        ce.getMasm().jmp(getContinuation());
    }
}
