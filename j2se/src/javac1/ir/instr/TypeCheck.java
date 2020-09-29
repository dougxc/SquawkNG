/*
 * @(#)TypeCheck.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ci.Klass;
import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.types.ValueType;

/**
 * The abstract base class for instructions checking whether an object is of a
 * given reference type.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public abstract class TypeCheck extends StateSplit {
    /**
     * The reference type to test towards.
     */
    private Klass klass;

    /**
     * The object to check.
     */
    private Instruction obj;

    /**
     * Initializes the attributes declared in this class.
     *
     * @param  scope  scope containing this instruction
     * @param  klass  reference type to test towards
     * @param  obj    object to check
     * @param  type   result type of the type check
     */
    protected TypeCheck(IRScope scope, Klass klass, Instruction obj, ValueType type) {
        super(scope, type);
        this.klass = klass;
        this.obj = obj;
    }

    /**
     * Returns the reference type to test towards.
     *
     * @return  the reference type
     */
    public Klass getKlass() {
        return klass;
    }

    /**
     * Returns the object to check.
     *
     * @return  the object to check
     */
    public Instruction getObj() {
        return obj;
    }

    public boolean canTrap() {
        return true;
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        obj = vc.doValue(obj);
    }
}
