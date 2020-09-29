/*
 * @(#)BasicType.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1;

/**
 * Defines constants for the basic types. In addition this class allows to
 * map type characters to basic types and to determine the number of bytes
 * that an array element of a certain type uses.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class BasicType {
    /**
     * The basic type constant for truth values.
     */
    public static final int BOOLEAN = 4;

    /**
     * The basic type constant for characters.
     */
    public static final int CHAR = 5;

    /**
     * The basic type constant for single-precision floating-point numbers.
     */
    public static final int FLOAT = 6;

    /**
     * The basic type constant for double-precision floating-point numbers.
     */
    public static final int DOUBLE = 7;

    /**
     * The basic type constant for bytes.
     */
    public static final int BYTE = 8;

    /**
     * The basic type constant for short integers.
     */
    public static final int SHORT = 9;

    /**
     * The basic type constant for integers.
     */
    public static final int INT = 10;

    /**
     * The basic type constant for long integers.
     */
    public static final int LONG = 11;

    /**
     * The basic type constant for objects.
     */
    public static final int OBJECT = 12;

    /**
     * The basic type constant for arrays.
     */
    public static final int ARRAY = 13;

    /**
     * The constant for the result type of methods that do not return a value.
     */
    public static final int VOID = 14;

    /**
     * The basic type constant for addresses.
     */
    public static final int ADDRESS = 15;

    /**
     * The basic type constant for stack values with conflicting contents.
     */
    public static final int CONFLICT = 16;

    /**
     * The constant that represents no type or an illegal type.
     */
    public static final int ILLEGAL = -1;

    /**
     * Maps each basic type to the number of bytes used by its array element.
     */
    public static final int[] ARRAY_ELEM_BYTES =
            new int[] {0, 0, 0, 0, 1, 2, 4, 8, 1, 2, 4, 8, 4, 4, 0, 4, 0};

    /**
     * Maps each basic type to the number of words that it occupies on the
     * expression stack.
     */
    public static final int[] TYPE_TO_SIZE =
            new int[] {-1, 0, 0, 0, 1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 0, 1, -1};

    /**
     * Don't let anyone instantiate this class.
     */
    private BasicType() {}

    /**
     * Tests if a value of the specified type occupies two words on stack.
     *
     * @param   type  basic type to be tested
     * @return  whether or not the size is two words
     */
    public static boolean isDoubleWord(int type) {
        return (type == LONG) || (type == DOUBLE);
    }

    /**
     * Tests if a value of the specified type is an object pointer.
     *
     * @param   type  basic type to be tested
     * @return  whether or not the type denotes a pointer
     */
    public static boolean isOop(int type) {
        return (type == OBJECT) || (type == ARRAY);
    }

    /**
     * Returns the class object for the specified primitive basic type.
     *
     * @param   type  primitive basic type
     * @return  class object for the specified type
     */
    public static Class getTypeClass(int type) {
        switch (type) {
        case BOOLEAN:
            return Boolean.TYPE;
        case CHAR:
            return Character.TYPE;
        case FLOAT:
            return Float.TYPE;
        case DOUBLE:
            return Double.TYPE;
        case BYTE:
            return Byte.TYPE;
        case SHORT:
            return Short.TYPE;
        case INT:
            return Integer.TYPE;
        case LONG:
            return Long.TYPE;
        case VOID:
            return Void.TYPE;
        default:
            Assert.shouldNotReachHere();
            return null;
        }
    }

    /**
     * Returns the basic type for the specified type character.
     *
     * @param   type  the type character
     * @return  the corresponding basic type
     */
    public static int valueOf(char ch) {
        switch (ch) {
        case 'B':
            return BYTE;
        case 'C':
            return CHAR;
        case 'D':
            return DOUBLE;
        case 'F':
            return FLOAT;
        case 'I':
            return INT;
        case 'J':
            return LONG;
        case 'L':
            return OBJECT;
        case 'S':
            return SHORT;
        case 'V':
            return VOID;
        case 'Z':
            return BOOLEAN;
        case '[':
            return ARRAY;
        default:
            Assert.shouldNotReachHere();
            return ILLEGAL;
        }
    }
}
