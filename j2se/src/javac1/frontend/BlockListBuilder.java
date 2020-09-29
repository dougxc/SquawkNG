/*
 * @(#)BlockListBuilder.java            1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javac1.Bytecodes;
import javac1.BytecodeStream;
import javac1.ci.ExceptionHandler;
import javac1.ir.IRScope;
import javac1.ir.instr.BlockBegin;

/**
 * Determines the starts of all basic blocks. This is the first pass of building
 * the intermediate representation. Afterwards the gaps between the basic blocks
 * are filled with instructions.
 *
 * @see      GraphBuilder
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class BlockListBuilder {
    /**
     * The current scope.
     */
    private IRScope scope;

    /**
     * The mapping of bytecode indices to basic blocks.
     */
    private Map bci2block;

    /**
     * The standard entry of the scope.
     */
    private BlockBegin stdEntry;
    
    /**
     * The OSR entry of the scope.
     */
    private BlockBegin osrEntry;

    /**
     * Constructs a new block list builder and determines the starts of basic
     * blocks in the specified scope.
     *
     * @param  scope  the current scope
     */
    public BlockListBuilder(IRScope scope, int osrBci) {
        this.scope = scope;
        bci2block = new HashMap();
        setLeaders();
        setExceptionHandlerEntries();
        stdEntry = newBlockAt(0, BlockBegin.STD_ENTRY_FLAG);
        osrEntry = (osrBci >= 0) ?
            newBlockAt(osrBci, BlockBegin.OSR_ENTRY_FLAG) : null;
    }

    /**
     * Maps the specified bytecode index to a basic block. If the bytecode index
     * has already been mapped to a block previously, then the existing block
     * with accumulated flags will be returned. Otherwise a new basic block will
     * be created and returned.
     *
     * @param   bci   bytecode index that the block starts at
     * @param   flag  flag of the basic block
     * @return  basic block associated with the specified index
     */
    private BlockBegin newBlockAt(int bci, int flag) {
        Integer key = new Integer(bci);
        BlockBegin block = (BlockBegin) bci2block.get(key);
        if (block == null) {
            block = new BlockBegin(scope, bci);
            bci2block.put(key, block);
        }
        block.setFlag(flag);
        return block;
    }

    /**
     * Maps the specified bytecode index to a basic block without setting flags.
     *
     * @param   bci   bytecode index that the block starts at
     * @return  basic block associated with the specified index
     * @see     #newBlockAt(int, int)
     */
    private BlockBegin newBlockAt(int bci) {
        return newBlockAt(bci, BlockBegin.NO_FLAG);
    }

    /**
     * Creates blocks for the jump destinations of a table switch instruction.
     *
     * @param  stream  the bytecode stream
     */
    private void tableSwitch(BytecodeStream stream) {
        int bci = stream.getBci();
        int offset = ((bci + 4) & ~0x03) - bci;
        int lo = stream.getSigned(offset + 4, 4);
        int hi = stream.getSigned(offset + 8, 4);
        newBlockAt(stream.getDestination(offset, true));
        offset += 12;
        for (int i = hi - lo; i >= 0; i--) {
            newBlockAt(stream.getDestination(offset, true));
            offset += 4;
        }
    }

    /**
     * Creates blocks for the jump destinations of a lookup switch instruction.
     *
     * @param  stream  the bytecode stream
     */
    private void lookupSwitch(BytecodeStream stream) {
        int bci = stream.getBci();
        int offset = ((bci + 4) & ~0x03) - bci;
        int npairs = stream.getSigned(offset + 4, 4);
        newBlockAt(stream.getDestination(offset, true));
        offset += 12;
        for (int i = 0; i < npairs; i++) {
            newBlockAt(stream.getDestination(offset, true));
            offset += 8;
        }
    }

    /**
     * Iterates over the bytecodes and determines the starts of basic blocks.
     */
    private void setLeaders() {
        BytecodeStream stream = new BytecodeStream(scope.getMethod().getCode());
        while (stream.next()) {
            switch (stream.getBytecode()) {
            case Bytecodes._ifeq:
                /* falls through */
            case Bytecodes._ifne:
                /* falls through */
            case Bytecodes._iflt:
                /* falls through */
            case Bytecodes._ifge:
                /* falls through */
            case Bytecodes._ifgt:
                /* falls through */
            case Bytecodes._ifle:
                /* falls through */
            case Bytecodes._if_icmpeq:
                /* falls through */
            case Bytecodes._if_icmpne:
                /* falls through */
            case Bytecodes._if_icmplt:
                /* falls through */
            case Bytecodes._if_icmpge:
                /* falls through */
            case Bytecodes._if_icmpgt:
                /* falls through */
            case Bytecodes._if_icmple:
                /* falls through */
            case Bytecodes._if_acmpeq:
                /* falls through */
            case Bytecodes._if_acmpne:
                /* falls through */
            case Bytecodes._ifnull:
                /* falls through */
            case Bytecodes._ifnonnull:
                newBlockAt(stream.getDestination(1, false));
                newBlockAt(stream.getNextBci());
                break;
            case Bytecodes._goto:
                newBlockAt(stream.getDestination(1, false));
                break;
            case Bytecodes._jsr:
                int dest = stream.getDestination(1, false);
                newBlockAt(dest, BlockBegin.SUBROUTINE_ENTRY_FLAG);
                break;
            case Bytecodes._tableswitch:
                tableSwitch(stream);
                break;
            case Bytecodes._lookupswitch:
                lookupSwitch(stream);
                break;
            case Bytecodes._goto_w:
                newBlockAt(stream.getDestination(1, true));
                break;
            case Bytecodes._jsr_w:
                int wide = stream.getDestination(1, true);
                newBlockAt(wide, BlockBegin.SUBROUTINE_ENTRY_FLAG);
                break;
            }
        }
    }

    /**
     * Creates blocks for the exception handlers and sets their entry blocks.
     */
    private void setExceptionHandlerEntries() {
        List list = scope.getExceptionHandlers();
        for (int i = 0; i < list.size(); i++) {
            ExceptionHandler handler = (ExceptionHandler) list.get(i);
            int bci = handler.getTarget();
            BlockBegin block = newBlockAt(bci, BlockBegin.EXCEPTION_ENTRY_FLAG);
            handler.setEntry(block);
        }
    }

    /**
     * Returns the mapping of bytecode indices to basic blocks.
     *
     * @return  mapping of bytecode indices to basic blocks
     */
    public Map getBci2block() {
        return bci2block;
    }

    /**
     * Returns the standard entry of the current scope.
     *
     * @return  the standard entry
     */
    public BlockBegin getStdEntry() {
        return stdEntry;
    }
    
    /**
     * Returns the OSR entry of the current scope.
     *
     * @return  the OSR entry
     */
    public BlockBegin getOsrEntry() {
        return osrEntry;
    }
}
