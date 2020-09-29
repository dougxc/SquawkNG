/*
 * @(#)StaticCallStub.java              1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.stubs;

import java.util.List;
import javac1.Assert;
import javac1.backend.code.CodeEmitter;
import javac1.backend.code.Register;
import javac1.backend.code.RelocInfo;
import javac1.ci.Obj;

/**
 * The code stub for a static procedure call.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class StaticCallStub extends CodeStub {
    /**
     * The program counter at the moment of the call.
     */
    private int callPc;

    /**
     * The type signature of the procedure to be called.
     */
    private List sigTypes;

    /**
     * Constructs a new code stub for a static procedure call.
     *
     * @param  callPc    code position of the call
     * @param  sigTypes  type signature of the procedure
     */
    public StaticCallStub(int callPc, List sigTypes) {
        this.callPc = callPc;
        this.sigTypes = sigTypes;
    }

    /**
     * Returns the type signature of the procedure to be called. The signature
     * of a method is a list of the arguments' basic types.
     *
     * @return  type signature of the procedure
     */
    public List getSigTypes() {
        return sigTypes;
    }

    public void emitCode(CodeEmitter ce) {
        int addr = ce.getMasm().getCodePos();
        int offset = (addr - callPc) / RelocInfo.OFFSET_UNIT;
        RelocInfo reloc = new RelocInfo(RelocInfo.STATIC_STUB_TYPE, offset);
        ce.getMasm().relocate(reloc);
        ce.getMasm().movl(Register.EBX, (Obj) null);
        ce.getMasm().jmp(-1, RelocInfo.RUNTIME_CALL_TYPE);
    }
}
