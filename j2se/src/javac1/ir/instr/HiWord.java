/*
 * @(#)HiWord.java                      1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.Assert;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.ValueClosure;

/**
 * Occupies the high word of a two-word expression stack entry. High and low
 * words must appear paired on the expression stack, otherwise the bytecode
 * sequence is illegal.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class HiWord extends Instruction {
    /**
     * The low word of the two-word expression.
     */
    private Instruction loWord;

    /**
     * Constructs a new high word for the specified low word.
     *
     * @param  scope   scope containing this instruction
     * @param  loWord  the corresponding low word
     */
    public HiWord(IRScope scope, Instruction loWord) {
        super(scope, loWord.getType());
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(loWord.getType().isDoubleWord(), "must be used for 2-word values only");
        }
        this.loWord = loWord;
    }

    /**
     * Returns the corresponding low word.
     *
     * @return  the low word
     */
    public Instruction getLoWord() {
        return loWord;
    }

    public void doInputValues(ValueClosure vc) {
        Assert.shouldNotReachHere();
    }

    public void visit(InstructionVisitor visitor) {
        Assert.shouldNotReachHere();
    }
}
