/*
 * @(#)ArrayStoreCheckStub.java         1.10 02/11/27
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
 * The code stub for checks that a value can be stored into the specified array.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class ArrayStoreCheckStub extends CodeStub {
    /**
     * The register that specifies the array to store value into.
     */
    private Register array;

    /**
     * The register that specifies the value to be stored.
     */
    private Register value;

    /**
     * The register that specifies the index to store value at.
     */
    private Register index;

    /**
     * The index if it is constant.
     */
    private int indexConst;

    /**
     * The associated debug information.
     */
    private CodeEmitInfo info;

    /**
     * The offset of the stack pointer.
     */
    private int espOffset;

    /**
     * Constructs a new code stub for checks that a value can be stored into the
     * specified array.
     *
     * @param  array       specifies the array to store value into
     * @param  value       specifies the value to be stored
     * @param  index       specifies the index to store value at
     * @param  indexConst  the index if it is constant
     * @param  info        associated debug information
     * @param  espOffset   offset of the stack pointer
     */
    public ArrayStoreCheckStub(Register array, Register value, Register index,
            int indexConst, CodeEmitInfo info, int espOffset) {
        this.array = array;
        this.value = value;
        this.index = index;
        this.indexConst = indexConst;
        this.info = info;
        this.espOffset = espOffset;
    }

    public void emitCode(CodeEmitter ce) {
        ce.setEspOffset(espOffset);
        ce.getMasm().bind(getEntry());
        ce.pushReg(value);
        ce.pushReg(array);
        if (index.equals(Register.NO_REG)) {
            ce.pushInt(indexConst);
        } else {
            ce.pushReg(index);
        }
        ce.getMasm().call(Runtime1.getStubEntry(Runtime1.STORE_CHECK_ID),
            RelocInfo.RUNTIME_CALL_TYPE);
        ce.addCallInfoHere(info);
        ce.decStack(3);
        ce.getMasm().jmp(getContinuation());
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(ce.getEspOffset() == espOffset, "must be the same when returning");
        }
        ce.setEspOffset(0);
    }
}
