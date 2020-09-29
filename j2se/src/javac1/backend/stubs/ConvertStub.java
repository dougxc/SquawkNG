/*
 * @(#)ConvertStub.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.stubs;

import javac1.Assert;
import javac1.backend.code.CodeEmitter;
import javac1.backend.code.Address;
import javac1.backend.code.Register;
import javac1.backend.code.RelocInfo;

/**
 * The code stub for converting the type of a value.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class ConvertStub extends CodeStub {
    /**
     * The conversion function of the runtime to be called.
     */
    private int runtime;

    /**
     * The result register.
     */
    private Register result;

    /**
     * Constructs a new code stub for converting the type of a value.
     *
     * @param  runtime  runtime function to be called
     * @param  result   the result register
     */
    public ConvertStub(int runtime, Register result) {
        this.runtime = runtime;
        this.result = result;
    }

    public void emitCode(CodeEmitter ce) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(result.equals(Register.EAX), "wrong result set");
        }
        ce.getMasm().bind(getEntry());
        ce.bangStack();
        ce.getMasm().pushl(Register.ESI);
        ce.getMasm().pushl(Register.EDI);
        ce.getMasm().pushl(Register.ECX);
        ce.getMasm().pushl(Register.EBX);
        ce.getMasm().pushl(Register.EDX);
        ce.getMasm().pushl(Register.EDX);
        ce.getMasm().fstps(new Address(Register.ESP));
        ce.getMasm().call(runtime, RelocInfo.RUNTIME_CALL_TYPE);
        ce.getMasm().popl(Register.EDX);
        ce.getMasm().popl(Register.EDX);
        ce.getMasm().popl(Register.EBX);
        ce.getMasm().popl(Register.ECX);
        ce.getMasm().popl(Register.EDI);
        ce.getMasm().popl(Register.ESI);
        ce.getMasm().jmp(getContinuation());
    }
}
