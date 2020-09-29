/*
 * @(#)NewInstanceStub.java             1.10 02/11/27
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
 * The code stub for creating a new instance.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class NewInstanceStub extends CodeStub {
    /**
     * The register that specifies the class to be instantiated.
     */
    private Register clazz;

    /**
     * The associated debug information.
     */
    private CodeEmitInfo info;

    /**
     * The offset of the stack pointer.
     */
    private int espOffset;

    /**
     * Constructs a new code stub for creating a new instance.
     *
     * @param  clazz      specifies the class to be instantiated
     * @param  info       associated debug information
     * @param  espOffset  offset of the stack pointer
     */
    public NewInstanceStub(Register clazz, CodeEmitInfo info, int espOffset) {
        this.clazz = clazz;
        this.info = info;
        this.espOffset = espOffset;
    }

    public void emitCode(CodeEmitter ce) {
        ce.setEspOffset(espOffset);
        ce.getMasm().bind(getEntry());
        ce.getMasm().movl(Register.EAX, clazz);
        ce.getMasm().call(Runtime1.getStubEntry(Runtime1.NEW_INSTANCE_ID),
            RelocInfo.RUNTIME_CALL_TYPE);
        ce.addCallInfoHere(info);
        ce.getMasm().jmp(getContinuation());
    }
}
