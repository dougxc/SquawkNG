/*
 * @(#)Obj.java                         1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ci;

import javac1.Assert;

/**
 * The abstract base class for all classes that encapsulate an ordinary object
 * pointer.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public abstract class Obj {
    /**
     * The ordinary object pointer.
     */
    private Object oop;
    
    /**
     * Initializes the attributes declared in this class.
     *
     * @param  oop  the ordinary object pointer
     */
    protected Obj(Object oop) {
        this.oop = oop;
    }
    
    /**
     * Returns the ordinary object pointer.
     *
     * @return  the ordinary object pointer
     */
    public Object getOop() {
        return oop;
    }
    
    /**
     * Tests if the encapsulated item is loaded.
     *
     * @return  whether the item is loaded or not
     */
    public boolean isLoaded() {
        return oop != null;
    }
    
    /**
     * Checks that the encapsulated item is loaded.
     */
    protected void checkIsLoaded() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isLoaded(), "object not loaded");
        }
    }
}
