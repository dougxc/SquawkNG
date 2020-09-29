/*
 * @(#)AddressType.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.types;

/**
 * The value type of instructions that produce an address.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class AddressType extends ValueType {
    public int getTag() {
        return ValueType.addressTag;
    }

    public int getSize() {
        return 1;
    }

    public ValueType getBase() {
        return ValueType.addressType;
    }

    public char getTypeChar() {
        return 'r';
    }

    public String toString() {
        return "address";
    }
}
