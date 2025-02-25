/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;
import com.sun.cldc.i18n.uclc.*;

/**
 * The Character class wraps a value of the primitive type <code>char</code>
 * in an object. An object of type <code>Character</code> contains a
 * single field whose type is <code>char</code>.
 * <p>
 * In addition, this class provides several methods for determining
 * the type of a character and converting characters from uppercase
 * to lowercase and vice versa.
 *
 * @author  Lee Boynton
 * @author  Guy Steele
 * @author  Akira Tanaka
 * @version 1.60, 12/04/99 (CLDC 1.0, Spring 2000)
 * @since   JDK1.0
 */

public final class Character extends com.sun.cldc.i18n.uclc.DefaultCaseConverter {

    /**
     * The minimum radix available for conversion to and from Strings.
     *
     * @see     java.lang.Integer#toString(int, int)
     * @see     java.lang.Integer#valueOf(java.lang.String)
     */
    public static final int MIN_RADIX = 2;

    /**
     * The maximum radix available for conversion to and from Strings.
     *
     * @see     java.lang.Integer#toString(int, int)
     * @see     java.lang.Integer#valueOf(java.lang.String)
     */
    public static final int MAX_RADIX = 36;

    /**
     * The constant value of this field is the smallest value of type
     * <code>char</code>.
     *
     * @since   JDK1.0.2
     */
    public static final char   MIN_VALUE = '\u0000';

    /**
     * The constant value of this field is the largest value of type
     * <code>char</code>.
     *
     * @since   JDK1.0.2
     */
    public static final char   MAX_VALUE = '\uffff';

    /**
     * The value of the Character.
     *
     * @serial
     */
    private char value;

    /**
     * Constructs a <code>Character</code> object and initializes it so
     * that it represents the primitive <code>value</code> argument.
     *
     * @param  value   value for the new <code>Character</code> object.
     */
    public Character(char value) {
        this.value = value;
    }

    /**
     * Returns the value of this Character object.
     * @return  the primitive <code>char</code> value represented by
     *          this object.
     */
    public char charValue() {
        return value;
    }

    /**
     * Returns a hash code for this Character.
     * @return  a hash code value for this object.
     */
    public int hashCode() {
        return (int)value;
    }

    /**
     * Compares this object against the specified object.
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>Character</code> object that
     * represents the same <code>char</code> value as this object.
     *
     * @param   obj   the object to compare with.
     * @return  <code>true</code> if the objects are the same;
     *          <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (obj instanceof Character) {
            return value == ((Character)obj).charValue();
        }
        return false;
    }

    /**
     * Returns a String object representing this character's value.
     * Converts this <code>Character</code> object to a string. The
     * result is a string whose length is <code>1</code>. The string's
     * sole component is the primitive <code>char</code> value represented
     * by this object.
     *
     * @return  a string representation of this object.
     */
    public String toString() {
        char buf[] = {value};
        return String.valueOf(buf);
    }

/*if[CASE.CONVERSION]*/

   /**
     * Determines if the specified character is a lowercase character.
     *
     * @param   ch   the character to be tested.
     * @return  <code>true</code> if the character is lowercase;
     *          <code>false</code> otherwise.
     * @since   JDK1.0
     */
    public static boolean isLowerCase(char ch) {
        return caseConverter().isLowerCase(ch);
    }

   /**
     * Determines if the specified character is an uppercase character.
     *
     * @param   ch   the character to be tested.
     * @return  <code>true</code> if the character is uppercase;
     *          <code>false</code> otherwise.
     * @see     java.lang.Character#isLowerCase(char)
     * @see     java.lang.Character#toUpperCase(char)
     * @since   1.0
     */
    public static boolean isUpperCase(char ch) {
        return caseConverter().isUpperCase(ch);
    }

    /**
     * Determines if the specified character is a digit.
     *
     * @param   ch   the character to be tested.
     * @return  <code>true</code> if the character is a digit;
     *          <code>false</code> otherwise.
     * @since   JDK1.0
     */
    public static boolean isDigit(char ch) {
        return caseConverter().isDigit(ch);
    }

    /**
     * The given character is mapped to its lowercase equivalent; if the
     * character has no lowercase equivalent, the character itself is
     * returned.
     *
     * @param   ch   the character to be converted.
     * @return  the lowercase equivalent of the character, if any;
     *          otherwise the character itself.
     * @see     java.lang.Character#isLowerCase(char)
     * @see     java.lang.Character#isUpperCase(char)
     * @see     java.lang.Character#toUpperCase(char)
     * @since   JDK1.0
     */
    public static char toLowerCase(char ch) {
        return caseConverter().toLowerCase(ch);
    }

    /**
     * Converts the character argument to uppercase; if the
     * character has no lowercase equivalent, the character itself is
     * returned.
     *
     * @param   ch   the character to be converted.
     * @return  the uppercase equivalent of the character, if any;
     *          otherwise the character itself.
     * @see     java.lang.Character#isLowerCase(char)
     * @see     java.lang.Character#isUpperCase(char)
     * @see     java.lang.Character#toLowerCase(char)
     * @since   JDK1.0
     */
    public static char toUpperCase(char ch) {
        return caseConverter().toUpperCase(ch);
    }

    /**
     * Returns the numeric value of the character <code>ch</code> in the
     * specified radix.
     *
     * @param   ch      the character to be converted.
     * @param   radix   the radix.
     * @return  the numeric value represented by the character in the
     *          specified radix.
     * @see     java.lang.Character#isDigit(char)
     * @since   JDK1.0
     */
    public static int digit(char ch, int radix) {
        return caseConverter().digit(ch, radix);
    }

    static DefaultCaseConverter cc;

    static DefaultCaseConverter caseConverter() {

        if (cc != null) {
           return cc;
        }

        String ccName = null;

        try {
            /* Get the default encoding name */
            ccName = System.getProperty("java.lang.Character.caseConverter");
            if (ccName == null) {
                ccName = "com.sun.cldc.i18n.uclc.DefaultCaseConverter";
            }

            /* Using the decoder names lookup a class to implement the reader */
            Klass clazz = Klass.forName(ccName);

            /* Return a new instance */
            cc = (DefaultCaseConverter)clazz.newInstance();

        } catch(Exception x) {
            throw new RuntimeException("Cannot find case converter class "+ccName+" -> "+x.getMessage());
        }

        return cc;
    }
/*end[CASE.CONVERSION]*/
}


