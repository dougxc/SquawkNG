/*
 * @(#)Base.java                        1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.Assert;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.types.ValueType;

/**
 * The predecessor of the standard method entry.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Base extends BlockEnd {
    /**
     * Constructs a new base node.
     *
     * @param  scope     scope containing this instruction
     * @param  stdEntry  the standard method entry
     * @param  osrEntry  the OSR entry
     */
    public Base(IRScope scope, BlockBegin stdEntry, BlockBegin osrEntry) {
        super(scope, ValueType.illegalType);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(stdEntry.isFlagSet(BlockBegin.STD_ENTRY_FLAG), "standard entry must be flagged");
            Assert.that((osrEntry == null) || osrEntry.isFlagSet(BlockBegin.OSR_ENTRY_FLAG), "osr entry must be flagged");
        }
        if (osrEntry != null) {
            addSux(osrEntry);
        }
        addSux(stdEntry);
    }

    /**
     * Returns the standard entry that this node points to.
     *
     * @return  the standard entry
     */
    public BlockBegin getStdEntry() {
        return defaultSux();
    }
    
    /**
     * Returns the OSR entry that this node points to.
     *
     * @return  the OSR entry
     */
    public BlockBegin getOsrEntry() {
        return (countSux() < 2) ? null : suxAt(0);
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doBase(this);
    }
}
