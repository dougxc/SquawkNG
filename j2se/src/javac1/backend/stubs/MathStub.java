/*
 * @(#)MathStub.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.stubs;

import javac1.backend.code.CodeEmitter;
import javac1.backend.code.Address;
import javac1.backend.code.Register;
import javac1.backend.code.RelocInfo;

/**
 * The code stub for a trigonometric function.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class MathStub extends CodeStub {
    /**
     * The address of the runtime routine to be called.
     */
    private int runtimeAddr;

    /**
     * Constructs a new code stub for a trigonometric function.
     *
     * @param  runtimeAddr  address of the routine to be called
     */
    public MathStub(int runtimeAddr) {
        this.runtimeAddr = runtimeAddr;
    }

    public void emitCode(CodeEmitter ce) {
        ce.getMasm().bind(getEntry());
        ce.bangStack();
        ce.getMasm().pushad();
        ce.getMasm().addl(Register.ESP, -8);
        ce.getMasm().fstpd(new Address(Register.ESP));
        ce.getMasm().call(runtimeAddr, RelocInfo.RUNTIME_CALL_TYPE);
        ce.getMasm().addl(Register.ESP, 8);
        ce.getMasm().popad();
        ce.getMasm().jmp(getContinuation());
    }
}
