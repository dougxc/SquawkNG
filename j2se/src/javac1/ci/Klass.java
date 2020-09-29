/*
 * @(#)Klass.java                       1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ci;

import javac1.Assert;

/**
 * Represents a class in the compiler interface.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Klass extends Obj {
    /**
     * The name of this class.
     */
    private Symbol name;
    
    /**
     * Constructs a new object representing a class.
     *
     * @param  oop  the ordinary object pointer
     */
    protected Klass(Object oop) {
        super(oop);
        checkIsLoaded();
        name = new Symbol(JVM.getKlassName(oop));
    }
    
    /**
     * Constructs a new object representing an unloaded class.
     *
     * @param  name  the name of the class
     */
    protected Klass(Symbol name) {
        super(null);
        this.name = name;
    }
    
    /**
     * Looks up the class with the specified name.
     *
     * @param  name  name of the class
     */
    public static Klass lookup(String name) {
        Object klass = JVM.lookupKlass(name);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(klass != null, "intended for loaded classes only");
        }
        if (JVM.isArrayKlass(klass)) {
            return new ArrayKlass(klass);
        } else if (JVM.isInstanceKlass(klass)) {
            return new InstanceKlass(klass);
        } else {
            Assert.shouldNotReachHere();
            return new Klass(klass);
        }
    }

    /**
     * Returns the name of this class.
     *
     * @return  name of this class
     */
    public Symbol getName() {
        return name;
    }
    
    public String toString() {
        return name.toString();
    }
}
