/*
 * @(#)ValueType.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.types;

import javac1.Assert;
import javac1.BasicType;

/**
 * This abstract class is the root of the type hierarchy. For the sake of
 * comfort and performance it also defines constants for the various types, so
 * that one need not create a new object whenever a type is used.
 *
 * @see      javac1.ir.instr.Instruction#getType()
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public abstract class ValueType {
    /**
     * The tag constant for the value type with the related name.
     */
    public static final int
            illegalTag = -1, voidTag    =  0, intTag     =  1, longTag    =  2,
            floatTag   =  3, doubleTag  =  4, objectTag  =  5, addressTag =  6;

    /**
     * The constant for the result type of methods that do not return a value.
     */
    public static final VoidType voidType = new VoidType();

    /**
     * The value type constant for integers.
     */
    public static final IntType intType = new IntType();

    /**
     * The value type constant for long integers.
     */
    public static final LongType longType = new LongType();

    /**
     * The value type constant for single-precision floating-point values.
     */
    public static final FloatType floatType = new FloatType();

    /**
     * The value type constant for double-precision floating-point values.
     */
    public static final DoubleType doubleType = new DoubleType();

    /**
     * The value type constant for objects.
     */
    public static final ObjectType objectType = new ObjectType();

    /**
     * The value type constant for arrays.
     */
    public static final ArrayType arrayType = new ArrayType();

    /**
     * The value type constant for instances.
     */
    public static final InstanceType instanceType = new InstanceType();

    /**
     * The value type constant for classes.
     */
    public static final ClassType classType = new ClassType();

    /**
     * The value type constant for addresses.
     */
    public static final AddressType addressType = new AddressType();

    /**
     * The value type of instructions that do not represent a value.
     */
    public static final IllegalType illegalType = new IllegalType();

    /**
     * Returns the value type for the specified basic type.
     *
     * @param   type  the basic type
     * @return  the corresponding value type object
     */
    public static ValueType valueOf(int type) {
        switch (type) {
        case BasicType.VOID:
            return voidType;
        case BasicType.BYTE:
            /* falls through */
        case BasicType.CHAR:
            /* falls through */
        case BasicType.SHORT:
            /* falls through */
        case BasicType.BOOLEAN:
            /* falls through */
        case BasicType.INT:
            return intType;
        case BasicType.LONG:
            return longType;
        case BasicType.FLOAT:
            return floatType;
        case BasicType.DOUBLE:
            return doubleType;
        case BasicType.ARRAY:
            return arrayType;
        case BasicType.OBJECT:
            return objectType;
        case BasicType.ILLEGAL:
            return illegalType;
        default:
            Assert.shouldNotReachHere();
            return illegalType;
        }
    }

    /**
     * Returns the result type of the operation on two values of this type and
     * the specified one.
     *
     * @param   type  another value type
     * @return  the result type
     */
    public ValueType meet(ValueType type) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(getTag() == type.getTag(), "types must match");
        }
        return getBase();
    }

    /**
     * Returns whether or not this value type is a constant type.
     *
     * @return  whether or not this is a constant type
     */
    public boolean isConstant() {
        return false;
    }

    /**
     * Returns the tag of this value type useful for efficient type comparison.
     *
     * @return  the tag of this value type
     */
    public abstract int getTag();

    /**
     * Returns the size of a value of this type in words.
     *
     * @return  the size of this value type
     */
    public abstract int getSize();

    /**
     * Returns the canonical base type of this value type.
     *
     * @return  the base type of this value type
     */
    public abstract ValueType getBase();

    /**
     * Returns the type character associated with this value type.
     *
     * @return  the type character
     */
    public abstract char getTypeChar();

    /**
     * Returns whether or not this is a value type for integers.
     *
     * @return  whether or not this is an integer type
     */
    public boolean isIntKind() {
        return (getTag() == intTag) || (getTag() == longTag);
    }

    /**
     * Returns whether or not this is a value type for floating-point numbers.
     *
     * @return  whether or not this is a floating-point type
     */
    public boolean isFloatKind() {
        return (getTag() == floatTag) || (getTag() == doubleTag);
    }

    /**
     * Returns whether or not this is a value type for objects.
     *
     * @return  whether or not this is an object type
     */
    public boolean isObjectKind() {
        return getTag() == objectTag;
    }

    /**
     * Tests if the size of a value of this type is one single word.
     *
     * @return  whether or not the size is one word
     */
    public boolean isSingleWord() {
        return getSize() == 1;
    }

    /**
     * Tests if the size of a value of this type is two words.
     *
     * @return  whether or not the size is two words
     */
    public boolean isDoubleWord() {
        return getSize() == 2;
    }

    /**
     * Returns the name of this value type for printing and debugging.
     *
     * @return  the type name
     */
    public String toString() {
        Assert.shouldNotReachHere();
        return null;
    }
}
