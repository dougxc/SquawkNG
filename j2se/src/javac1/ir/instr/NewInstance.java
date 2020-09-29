/*
 * @(#)NewInstance.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ci.InstanceKlass;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.types.ValueType;

/**
 * The instruction node for creating a new object. Instance creation must be
 * completed by invoking an instance initialization method on the new object.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class NewInstance extends StateSplit {
    /**
     * The class of the object to create.
     */
    private InstanceKlass klass;

    /**
     * Constructs a new instruction node for creating an object.
     *
     * @param  scope  scope containing this instruction
     * @param  klass  the class to instantiate
     */
    public NewInstance(IRScope scope, InstanceKlass klass) {
        super(scope, ValueType.instanceType);
        this.klass = klass;
    }

    /**
     * Returns the class of the object to create.
     *
     * @return  the class to instantiate
     */
    public InstanceKlass getKlass() {
        return klass;
    }

    public boolean canTrap() {
        return true;
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doNewInstance(this);
    }
}
