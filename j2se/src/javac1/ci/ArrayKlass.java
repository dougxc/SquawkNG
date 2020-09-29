/*
 * @(#)ArrayKlass.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ci;

import javac1.Assert;

/**
 * Represents an array class in the compiler interface.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class ArrayKlass extends Klass {
    /**
     * Constructs a new object representing an array class.
     *
     * @param  oop  the ordinary object pointer
     */
    protected ArrayKlass(Object oop) {
        super(oop);
    }
    
    /**
     * Constructs a new object representing an unloaded array class.
     *
     * @param  name  the name of the class
     */
    protected ArrayKlass(Symbol name) {
        super(name);
    }
    
    /**
     * Constructs a new type array class from the specified element type.
     *
     * @param   elemType  the element type
     * @return  the type array class
     */
    public static ArrayKlass make(int elemType) {
        return new ArrayKlass(JVM.makeTypeArrayKlass(elemType));
    }
    
    /**
     * Constructs a new object array class from the specified element class.
     *
     * @param   elemKlass  the element class
     * @return  the object array class
     */
    public static ArrayKlass make(Klass elemKlass) {
        if (elemKlass.isLoaded()) {
            return new ArrayKlass(JVM.makeObjectArrayKlass(elemKlass.getOop()));
        } else {
            String elemKlassName = elemKlass.toString();
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(elemKlassName.startsWith("[") || elemKlassName.startsWith("L"), "should be reference type");
            }
            return new ArrayKlass(new Symbol("[" + elemKlassName));
        }       
    }
}
