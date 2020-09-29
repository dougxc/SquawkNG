/*
 * @(#)ValueGenInvariant.java           1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend;

import javac1.backend.code.CodeEmitter;
import javac1.backend.reg.RegAlloc;
import javac1.ci.Method;
import javac1.ir.instr.BlockBegin;

/**
 * Represents invariant data for register allocation and code generation. This
 * class stores references to the register allocator, the code emitter and the
 * current basic block, and provides methods to access them.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class ValueGenInvariant {
    /**
     * The method that is currently being compiled.
     */
    private Method method;

    /**
     * The register allocator.
     */
    private RegAlloc regAlloc;

    /**
     * The code emitter.
     */
    private CodeEmitter codeEmit;

    /**
     * The current basic block.
     */
    private BlockBegin block;

    /**
     * Constructs a new structure that stores invariant data.
     *
     * @param  method    the method being compiled
     * @param  regAlloc  the register allocator
     * @param  codeEmit  the code emitter
     */
    public ValueGenInvariant(Method method, RegAlloc regAlloc,
            CodeEmitter codeEmit) {
        this.method = method;
        this.regAlloc = regAlloc;
        this.codeEmit = codeEmit;
        this.block = null;
    }

    /**
     * Returns the method that is currently being compiled.
     *
     * @return  the method being compiled
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Returns the register allocator.
     *
     * @return  the register allocator
     */
    public RegAlloc getRegAlloc() {
        return regAlloc;
    }

    /**
     * Returns the code emitter.
     *
     * @return  the code emitter
     */
    public CodeEmitter getCodeEmit() {
        return codeEmit;
    }

    /**
     * Sets the current basic block.
     *
     * @param  block  the current basic block
     */
    public void setBlock(BlockBegin block) {
        this.block = block;
    }

    /**
     * Returns the current basic block.
     *
     * @return  the current basic block
     */
    public BlockBegin getBlock() {
        return block;
    }
}
