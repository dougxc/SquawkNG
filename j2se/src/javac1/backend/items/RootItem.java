/*
 * @(#)RootItem.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.items;

import javac1.ir.instr.Instruction;

/**
 * Represents an item that is assigned to a root instruction.
 *
 * @see      Instruction#isRoot()
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class RootItem extends Item {
    /**
     * Constructs a new item and links it up with the specified instruction.
     *
     * @param  instr  the instruction to construct item for
     */
    public RootItem(Instruction instr) {
        super(instr);
    }
}
