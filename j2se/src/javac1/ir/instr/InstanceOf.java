/*
 * @(#)InstanceOf.java                  1.00 02/08/01
 *
 * This file is part of the Just-In-Time Compiler for Java written in the
 * context of a master thesis in the field of computer science at the
 * Institute for Practical Computer Science (System Software Group) of the
 * Johannes Kepler University Linz.
 */

package javac1.ir.instr;

import javac1.ci.Klass;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.types.ValueType;

/**
 * The instruction node for determining if an object is of the given type. The
 * <code>InstanceOf</code> instruction differs from <code>CheckCast</code> only
 * in its treatment of <code>null</code> and its behavior when the test fails.
 *
 * @see      CheckCast
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class InstanceOf extends TypeCheck {
    /**
     * Constructs a new instruction node for a type check.
     *
     * @param  scope  scope containing this instruction
     * @param  clazz  reference type to test towards
     * @param  obj    object to check
     */
    public InstanceOf(IRScope scope, Klass klass, Instruction obj) {
        super(scope, klass, obj, ValueType.intType);
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doInstanceOf(this);
    }
}
