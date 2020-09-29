/*
 * @(#)InstanceOfStub.java              1.10 02/11/27
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
 * The code stub for testing the type of an object.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class InstanceOfStub extends CodeStub {
    /**
     * The register that specifies the object class.
     */
    private Register objClass;

    /**
     * The result register.
     */
    private Register result;

    /**
     * The register that specifies the class to be tested towards.
     */
    private Register testClass;

    /**
     * The associated debug information.
     */
    private CodeEmitInfo info;

    /**
     * The offset of the stack pointer.
     */
    private int espOffset;

    /**
     * Constructs a new code stub for testing the type of an object.
     *
     * @param  objClass   specifies the object class
     * @param  result     the result register
     * @param  testClass  specifies the class to be tested towards
     * @param  info       associated debug information
     * @param  espOffset  offset of the stack pointer
     */
    public InstanceOfStub(Register objClass, Register result,
            Register testClass, CodeEmitInfo info, int espOffset) {
        this.objClass = objClass;
        this.result = result;
        this.testClass = testClass;
        this.info = info;
        this.espOffset = espOffset;
    }

    public void emitCode(CodeEmitter ce) {
        ce.setEspOffset(espOffset);
        ce.getMasm().bind(getEntry());
        ce.pushReg(objClass);
        ce.pushReg(testClass);
        ce.getMasm().call(Runtime1.getStubEntry(Runtime1.INSTANCEOF_ID),
            RelocInfo.RUNTIME_CALL_TYPE);
        ce.addCallInfoHere(info);
        ce.decStackAfterCall(2);
        ce.moveReg(result, Register.EAX);
        ce.getMasm().jmp(getContinuation());
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(ce.getEspOffset() == espOffset, "must be the same when returning");
        }
        ce.setEspOffset(0);
    }
}
