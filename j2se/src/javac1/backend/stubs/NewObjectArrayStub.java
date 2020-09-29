/*
 * @(#)NewObjectArrayStub.java          1.10 02/11/27
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
 * The code stub for creating a new array of object references.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class NewObjectArrayStub extends CodeStub {
    /**
     * The reference type of the array to be created.
     */
    private Register clazz;

    /**
     * The array length in number of elements.
     */
    private Register length;

    /**
     * The result register.
     */
    private Register result;

    /**
     * The associated debug information.
     */
    private CodeEmitInfo info;

    /**
     * The offset of the stack pointer.
     */
    private int espOffset;

    /**
     * Constructs a new code stub for creating an object array.
     *
     * @param  clazz      reference type of the array
     * @param  length     length of the array
     * @param  result     the result register
     * @param  info       associated debug information
     * @param  espOffset  offset of the stack pointer
     */
    public NewObjectArrayStub(Register clazz, Register length, Register result,
            CodeEmitInfo info, int espOffset) {
        this.clazz = clazz;
        this.length = length;
        this.result = result;
        this.info = info;
        this.espOffset = espOffset;
    }

    public void emitCode(CodeEmitter ce) {
        ce.setEspOffset(espOffset);
        ce.getMasm().bind(getEntry());
        ce.pushReg(clazz);
        ce.pushReg(length);
        ce.getMasm().call(Runtime1.getStubEntry(Runtime1.NEW_OBJECT_ARRAY_ID),
            RelocInfo.RUNTIME_CALL_TYPE);
        ce.addCallInfoHere(info);
        ce.decStackAfterCall(2);
        ce.getMasm().jmp(getContinuation());
    }
}
