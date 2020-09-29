/*
 * @(#)CEEliminator.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend.opt;

import javac1.Assert;
import javac1.JavaC1;
import javac1.ir.BlockClosure;
import javac1.ir.IRScope;
import javac1.ir.ValueStack;
import javac1.ir.instr.BlockBegin;
import javac1.ir.instr.Constant;
import javac1.ir.instr.Goto;
import javac1.ir.instr.If;
import javac1.ir.instr.IfOp;
import javac1.ir.instr.Instruction;
import javac1.ir.instr.LoadLocal;
import javac1.ir.types.IntType;
import javac1.ir.types.ObjectType;
import javac1.ir.types.ValueType;

/**
 * This class is used to eliminate conditional expressions. If a branch in the
 * intermediate representation is used only to load one of two alternative
 * values then it can be replaced by an {@link IfOp} instruction.
 *
 * @see      Optimizer
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class CEEliminator implements BlockClosure {
    /**
     * The number of conditional expressions successfully eliminated.
     */
    private int ceeCount;

    /**
     * Constructs a new conditional expression eliminator.
     */
    public CEEliminator() {
        ceeCount = 0;
    }

    /**
     * Tries to construct a copy of the specified value.
     *
     * @param   scope  scope of the value
     * @param   x      the value to be copied
     * @return  copy of the value or <code>null</code> if it cannot be copied
     */
    private static Instruction simpleValueCopy(IRScope scope, Instruction x) {
        ValueType type = x.getType();
        if (type.isFloatKind()) {
            return null;
        } else if (x instanceof Constant) {
            return new Constant(scope, type);
        } else if (x instanceof LoadLocal) {
            return new LoadLocal(scope, type, ((LoadLocal) x).getIndex());
        } else {
            return null;
        }
    }

    /**
     * Looks for a conditional expression at the end of the specified block and
     * tries to eliminate it.
     *
     * @param  block  the basic block to be examined
     */
    public void doBlock(BlockBegin block) {
        if (!(block.getEnd() instanceof If)) {
            return;
        }
        If _if = (If) block.getEnd();
        ValueType type = _if.getX().getType();
        if (!(type instanceof IntType) && !(type instanceof ObjectType)) {
            return;
        }
        Instruction t1 = _if.trueSux().getNext();
        Instruction f1 = _if.falseSux().getNext();
        Instruction tval = simpleValueCopy(block.getScope(), t1);
        Instruction fval = simpleValueCopy(block.getScope(), f1);
        if ((tval == null) || (fval == null)) {
            return;
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((t1.getNext() != null) && (f1.getNext() != null), "successor must exist");
        }
        if (!(t1.getNext() instanceof Goto) || !(f1.getNext() instanceof Goto)) {
            return;
        }
        Goto t2 = (Goto) t1.getNext();
        Goto f2 = (Goto) f1.getNext();
        BlockBegin sux = t2.defaultSux();
        if (sux != f2.defaultSux()) {
            return;
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(tval.getType().getSize() == fval.getType().getSize(), "sizes must match");
        }
        if (_if.getState().getStackSize() + tval.getType().getSize()
                != sux.getState().getStackSize()) {
            return;
        }
        Instruction prev = _if.getPrev(block);
        prev.setNext(null);
        IfOp ifOp = new IfOp(block.getScope(), _if.getX(), _if.getCond(), _if.getY(), tval, fval);
        Goto _goto = new Goto(block.getScope(), sux);
        int bci = _if.getBci();
        prev.append(tval, bci).append(fval, bci).append(ifOp, bci).append(_goto, bci);
        ValueStack state = (ValueStack) _if.getState().clone();
        state.push(ifOp.getType(), ifOp);
        _goto.setState(state);
        sux.join(state);
        block.setEnd(_goto);
        state.pinStackAll();
        ceeCount++;
        if (javac1.Flags.PrintCEE) {
            JavaC1.out.print(ceeCount + ". CEE in B");
            JavaC1.out.println(block.getBlockId());
        }
    }
}
