/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

/**
 * The class <code>Math</code> contains methods for performing basic
 * numeric operations.
 *
 * @author  unascribed
 * @version 1.48, 12/04/99 (CLDC 1.0, Spring 2000)
 * @since   1.3
 */
public final strictfp class Math {

    /**
     * Don't let anyone instantiate this class.
     */
    private Math() {}

    /**
     * Returns the absolute value of an <code>int</code> value.
     * If the argument is not negative, the argument is returned.
     * If the argument is negative, the negation of the argument is returned.
     * <p>
     * Note that if the argument is equal to the value of
     * <code>Integer.MIN_VALUE</code>, the most negative representable
     * <code>int</code> value, the result is that same value, which is
     * negative.
     *
     * @param   a   an <code>int</code> value.
     * @return  the absolute value of the argument.
     * @see     java.lang.Integer#MIN_VALUE
     */
    public static int abs(int a) {
        return (a < 0) ? -a : a;
    }

    /**
     * Returns the absolute value of a <code>long</code> value.
     * If the argument is not negative, the argument is returned.
     * If the argument is negative, the negation of the argument is returned.
     * <p>
     * Note that if the argument is equal to the value of
     * <code>Long.MIN_VALUE</code>, the most negative representable
     * <code>long</code> value, the result is that same value, which is
     * negative.
     *
     * @param   a   a <code>long</code> value.
     * @return  the absolute value of the argument.
     * @see     java.lang.Long#MIN_VALUE
     */
    public static long abs(long a) {
        return (a < 0) ? -a : a;
    }

    /**
     * Returns the greater of two <code>int</code> values. That is, the
     * result is the argument closer to the value of
     * <code>Integer.MAX_VALUE</code>. If the arguments have the same value,
     * the result is that same value.
     *
     * @param   a   an <code>int</code> value.
     * @param   b   an <code>int</code> value.
     * @return  the larger of <code>a</code> and <code>b</code>.
     * @see     java.lang.Long#MAX_VALUE
     */
    public static int max(int a, int b) {
        return (a >= b) ? a : b;
    }

    /**
     * Returns the greater of two <code>long</code> values. That is, the
     * result is the argument closer to the value of
     * <code>Long.MAX_VALUE</code>. If the arguments have the same value,
     * the result is that same value.
     *
     * @param   a   a <code>long</code> value.
     * @param   b   a <code>long</code> value.
     * @return  the larger of <code>a</code> and <code>b</code>.
     * @see     java.lang.Long#MAX_VALUE
     */
    public static long max(long a, long b) {
        return (a >= b) ? a : b;
    }

    /**
     * Returns the smaller of two <code>int</code> values. That is, the
     * result the argument closer to the value of <code>Integer.MIN_VALUE</code>.
     * If the arguments have the same value, the result is that same value.
     *
     * @param   a   an <code>int</code> value.
     * @param   b   an <code>int</code> value.
     * @return  the smaller of <code>a</code> and <code>b</code>.
     * @see     java.lang.Long#MIN_VALUE
     */
    public static int min(int a, int b) {
        return (a <= b) ? a : b;
    }

    /**
     * Returns the smaller of two <code>long</code> values. That is, the
     * result is the argument closer to the value of
     * <code>Long.MIN_VALUE</code>. If the arguments have the same value,
     * the result is that same value.
     *
     * @param   a   a <code>long</code> value.
     * @param   b   a <code>long</code> value.
     * @return  the smaller of <code>a</code> and <code>b</code>.
     * @see     java.lang.Long#MIN_VALUE
     */


    public static long min(long a, long b) {
        return (a <= b) ? a : b;
    }

/*if[FLOATS]*/
    public static double sin(double a)                       { return Native.math(SConst.MATH_sin, a, 0);          }
    public static double cos(double a)                       { return Native.math(SConst.MATH_cos, a, 0);          }
    public static double tan(double a)                       { return Native.math(SConst.MATH_tan, a, 0);          }
    public static double asin(double a)                      { return Native.math(SConst.MATH_asin, a, 0);         }
    public static double acos(double a)                      { return Native.math(SConst.MATH_acos, a, 0);         }
    public static double atan(double a)                      { return Native.math(SConst.MATH_atan, a, 0);         }
    public static double exp(double a)                       { return Native.math(SConst.MATH_exp, a, 0);          }
    public static double log(double a)                       { return Native.math(SConst.MATH_log, a, 0);          }
    public static double sqrt(double a)                      { return Native.math(SConst.MATH_sqrt, a, 0);         }
    public static double ceil(double a)                      { return Native.math(SConst.MATH_ceil, a, 0);         }
    public static double floor(double a)                     { return Native.math(SConst.MATH_floor, a, 0);        }
    public static double atan2(double a, double b)           { return Native.math(SConst.MATH_atan2, a, b);        }
    public static double pow(double a, double b)             { return Native.math(SConst.MATH_pow, a, b);          }
    public static double IEEEremainder(double a, double b)   { return Native.math(SConst.MATH_IEEEremainder, a, b);}
/*end[FLOATS]*/

}
