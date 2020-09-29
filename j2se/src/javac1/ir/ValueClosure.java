/*
 * @(#)ValueClosure.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir;

import javac1.ir.instr.Instruction;

/**
 * Represents an operation to be performed on every input or state value of an
 * instruction.
 *
 * @see      Instruction#doInputValues(ValueClosure)
 * @see      Instruction#doStateValues(ValueClosure)
 * @see      Instruction#doValues(ValueClosure)
 * @see      javac1.ir.instr.BlockBegin#doBlockValues(ValueClosure)
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public interface ValueClosure {
    public Instruction doValue(Instruction value);
}
