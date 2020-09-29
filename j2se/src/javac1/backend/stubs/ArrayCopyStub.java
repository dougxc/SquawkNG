/*
 * @(#)ArrayCopyStub.java               1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.stubs;

import javac1.Assert;
import javac1.backend.code.CodeEmitter;
import javac1.backend.code.Assembler;
import javac1.backend.code.Label;
import javac1.backend.code.Register;
import javac1.backend.code.RelocInfo;
import javac1.backend.debug.CodeEmitInfo;
import javac1.ci.Runtime1;

/**
 * The code stub for copying array elements.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class ArrayCopyStub extends CodeStub {
    /**
     * The register that specifies the source array.
     */
    private Register srcReg;

    /**
     * The register that contains the start position in the source array.
     */
    private Register srcPosReg;

    /**
     * The register that specifies the destination array.
     */
    private Register dstReg;

    /**
     * The register that contains the start position in the destination array.
     */
    private Register dstPosReg;

    /**
     * The register that contains the number of elements to be copied.
     */
    private Register lengthReg;

    /**
     * The start position in the source array if it is constant.
     */
    private int srcPosConst;

    /**
     * The start position in the destination array if it is constant.
     */
    private int dstPosConst;

    /**
     * The number of elements to be copied if it is constant.
     */
    private int lengthConst;

    /**
     * The associated debug information.
     */
    private CodeEmitInfo info;

    /**
     * The offset of the stack pointer.
     */
    private int espOffset;

    /**
     * The label at the entry of the array copy without checks.
     */
    private Label noCheckEntry;

    /**
     * Constructs a new code stub for copying array elements.
     *
     * @param  srcReg       specifies the source array
     * @param  srcPosReg    contains the start position in the source array
     * @param  dstReg       specifies the destination array
     * @param  dstPosReg    contains the start position in the destination array
     * @param  lengthReg    contains the number of elements to be copied
     * @param  dstPosConst  start position in the destination array if constant
     * @param  srcPosConst  start position in the source array if constant
     * @param  lengthConst  number of elements to be copied if constant
     * @param  info         associated debug information
     * @param  espOffset    offset of the stack pointer
     */
    public ArrayCopyStub(Register srcReg, Register srcPosReg,
            Register dstReg, Register dstPosReg, Register lengthReg,
            int dstPosConst, int srcPosConst, int lengthConst,
            CodeEmitInfo info, int espOffset) {
        this.srcReg = srcReg;
        this.srcPosReg = srcPosReg;
        this.dstReg = dstReg;
        this.dstPosReg = dstPosReg;
        this.lengthReg = lengthReg;
        this.dstPosConst = dstPosConst;
        this.srcPosConst = srcPosConst;
        this.lengthConst = lengthConst;
        this.info = info;
        this.espOffset = espOffset;
        this.noCheckEntry = new Label();
    }

    /**
     * Pushes the parameters onto the stack.
     *
     * @param  ce  reference to the current code emitter
     */
    private void pushParams(CodeEmitter ce) {
        ce.pushReg(srcReg);
        if (srcPosReg.equals(Register.NO_REG)) {
            ce.pushInt(srcPosConst);
        } else {
            ce.pushReg(srcPosReg);
        }
        ce.pushReg(dstReg);
        if (dstPosReg.equals(Register.NO_REG)) {
            ce.pushInt(dstPosConst);
        } else {
            ce.pushReg(dstPosReg);
        }
        if (lengthReg.equals(Register.NO_REG)) {
            ce.pushInt(lengthConst);
        } else {
            ce.pushReg(lengthReg);
        }
    }

    public void emitCode(CodeEmitter ce) {
        ce.setEspOffset(espOffset);
        ce.getMasm().bind(getEntry());
        Label slowCase = new Label();
        pushParams(ce);
        StaticCallStub stub = new StaticCallStub(ce.getMasm().getCodePos(), null);
        ce.getCallStubs().add(stub);
        ce.getMasm().bind(slowCase);
        ce.getMasm().call(Runtime1.getStubEntry(Runtime1.RESOLVE_INVOKESTATIC_ID),
            RelocInfo.STATIC_CALL_TYPE);
        ce.addCallInfoHere(info);
        ce.decStackAfterCall(5);
        ce.getMasm().jmp(getContinuation());
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(ce.getEspOffset() == espOffset, "must be the same when returning");
        }
        ce.setEspOffset(espOffset);
        ce.getMasm().bind(noCheckEntry);
        pushParams(ce);
        pushParams(ce);
        ce.getMasm().call(Runtime1.getRuntimeFnPtr(
            Runtime1.SIMPLE_ARRAY_COPY_NO_CHECKS), RelocInfo.RUNTIME_CALL_TYPE);
        ce.decStack(5);
        ce.getMasm().testl(Register.EAX, Register.EAX);
        ce.getMasm().jcc(Assembler.NEGATIVE, slowCase);
        ce.decStack(5);
        ce.getMasm().jmp(getContinuation());
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(ce.getEspOffset() == espOffset, "must be the same when returning");
        }
        ce.setEspOffset(0);
    }

    /**
     * Returns the label at the entry of the array copy without checks.
     *
     * @return  entry of the array copy without checks
     */
    public Label getNoCheckEntry() {
        return noCheckEntry;
    }
}
