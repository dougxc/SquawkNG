/*
 * @(#)DivByZeroStub.java               1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.stubs;

import javac1.backend.code.CodeEmitter;
import javac1.backend.code.Register;
import javac1.backend.code.RelocInfo;
import javac1.ci.Runtime1;

/**
 * The code stub for a division by zero.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class DivByZeroStub extends CodeStub {
    /**
     * The code offset of the division.
     */
    private int offset;

    /**
     * Constructs a new code stub for a division by zero.
     *
     * @param  offset  code offset of the division
     */
    public DivByZeroStub(int offset) {
        this.offset = offset;
    }

    public void emitCode(CodeEmitter ce) {
        ce.getMasm().bind(getEntry());
        ce.getMasm().movl(Register.EAX, offset);
        ce.getMasm().call(Runtime1.getStubEntry(Runtime1.THROW_DIV0_EXCEPTION_ID),
            RelocInfo.RUNTIME_CALL_TYPE);
    }
}
