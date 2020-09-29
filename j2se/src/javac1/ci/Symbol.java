/*
 * @(#)Symbol.java                      1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ci;

/**
 * Represents a symbol in the compiler interface.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Symbol extends Obj {
    /**
     * The string representation of this symbol.
     */
    private String str;
    
    /**
     * Constructs a new symbol from the specified object pointer.
     *
     * @param  oop  the ordinary object pointer
     */
    protected Symbol(Object oop) {
        super(oop);
        str = JVM.getSymbolString(oop);
    }
    
    /**
     * Constructs a new symbol from the specified string.
     *
     * @param  str  the string
     */
    protected Symbol(String str) {
        super(null);
        this.str = str;
    }
    
    /**
     * Returns the string representation of this symbol.
     *
     * @return  the string representation
     */
    public String toString() {
        return str;
    }
}
