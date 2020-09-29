/*
 * @(#)CollectConstants.java            1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend;

import javac1.backend.code.ConstantTable;
import javac1.ir.BlockClosure;
import javac1.ir.instr.BlockBegin;
import javac1.ir.instr.Instruction;
import javac1.ir.types.DoubleConstant;
import javac1.ir.types.FloatConstant;
import javac1.ir.types.ValueType;

/**
 * Collects floating-point constants from the intermediate representation.
 *
 * @see      ConstantTable
 * @see      FloatConstant
 * @see      DoubleConstant
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class CollectConstants implements BlockClosure {
    /**
     * The table of floating-point constants.
     */
    private ConstantTable table;

    /**
     * Constructs a new block closure for collecting constants.
     *
     * @param  table  constant table that stores the values
     */
    public CollectConstants(ConstantTable table) {
        this.table = table;
    }

    /**
     * Searches the specified basic block for floating-point constants.
     *
     * @param  block  basic block to be searched for constants
     */
    public void doBlock(BlockBegin block) {
        for (Instruction x = block; x != null; x = x.getNext()) {
            ValueType type = x.getType();
            if (type instanceof FloatConstant) {
                table.appendFloat(((FloatConstant) type).getValue());
            } else if (type instanceof DoubleConstant) {
                table.appendDouble(((DoubleConstant) type).getValue());
            }
        }
    }
}
