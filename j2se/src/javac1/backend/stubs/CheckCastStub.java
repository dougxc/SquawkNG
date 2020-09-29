/*
 * @(#)CheckCastStub.java               1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.stubs;

import javac1.Assert;
import javac1.backend.code.CodeEmitter;
import javac1.backend.code.Register;
import javac1.backend.code.RelocInfo;
import javac1.backend.debug.CodeEmitInfo;
import javac1.ci.Runtime1;

/**
 * The code stub for checking an object against a class.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class CheckCastStub extends CodeStub {
    /**
     * The register that specifies the class to check against.
     */
    private Register testClass;

    /**
     * The associated debug information.
     */
    private CodeEmitInfo info;

    /**
     * The register that points to the object to be checked.
     */
    private Register dst;

    /**
     * The offset of the stack pointer.
     */
    private int espOffset;

    /**
     * Constructs a new code stub for checking an object against a class.
     *
     * @param  testClass  specifies class to check against
     * @param  info       associated debug information
     * @param  dst        points to the object to be checked
     * @param  espOffset  offset of the stack pointer
     */
    public CheckCastStub(Register testClass, CodeEmitInfo info, Register dst,
            int espOffset) {
        this.testClass = testClass;
        this.info = info;
        this.dst = dst;
        this.espOffset = espOffset;
    }

    public void emitCode(CodeEmitter ce) {
        ce.setEspOffset(espOffset);
        ce.getMasm().bind(getEntry());
        ce.pushReg(dst);
        ce.pushReg(testClass);
        ce.getMasm().call(Runtime1.getStubEntry(Runtime1.CHECKCAST_ID),
            RelocInfo.RUNTIME_CALL_TYPE);
        ce.addCallInfoHere(info);
        ce.decStackAfterCall(2);
        ce.moveReg(dst, Register.EAX);
        ce.getMasm().jmp(getContinuation());
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(ce.getEspOffset() == espOffset, "must be the same when returning");
        }
        ce.setEspOffset(0);
    }
}
