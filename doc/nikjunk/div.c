#include "stdio.h"


/* Macros for NaN (Not-A-Number) and Infinity for floats and doubles */
#define F_POS_INFINITY    0x7F800000
#define F_NEG_INFINITY    0xFF800000
#define F_L_POS_NAN       0x7F800001
#define F_H_POS_NAN       0x7FFFFFFF
#define F_L_NEG_NAN       0xFF800001
#define F_H_NEG_NAN       0xFFFFFFFF

#define D_POS_INFINITY    0x7FF0000000000000
#define D_NEG_INFINITY    0xFFF0000000000000
#define D_L_POS_NAN       0x7FF0000000000001
#define D_H_POS_NAN       0x7FFFFFFFFFFFFFFF
#define D_L_NEG_NAN       0xFFF0000000000001
#define D_H_NEG_NAN       0xFFFFFFFFFFFFFFFF

/* MAX and MIN values for INT and LONG */
#define MAX_INT           0x7FFFFFFF
#define MIN_INT           0x80000000
#define MAX_LONG          0x7FFFFFFFFFFFFFFF
#define MIN_LONG          0x8000000000000000


#ifndef DOUBLE_REMAINDER
#define DOUBLE_REMAINDER(a,b) fmod(a,b)
#endif

#define long64 __int64

double JFP_lib_dmul_x86(double lvalue, double rvalue) {
    double pos_d = 9.745314011399999E288;
    double neg_d = 1/pos_d;

    long double value;
    value = (((neg_d * neg_d * neg_d * neg_d * neg_d * neg_d * neg_d * neg_d *
               neg_d * neg_d * neg_d * neg_d * neg_d * neg_d * neg_d * neg_d) * lvalue) * rvalue) *
              (pos_d * pos_d * pos_d * pos_d * pos_d * pos_d * pos_d * pos_d *
               pos_d * pos_d * pos_d * pos_d * pos_d * pos_d * pos_d * pos_d);

    return value;
}

double JFP_lib_ddiv_x86(double lvalue, double rvalue) {
    double pos_d = 9.745314011399999E288;
    double neg_d = 1/pos_d;

    long double value;
    value = (((neg_d * neg_d * neg_d * neg_d * neg_d * neg_d * neg_d * neg_d *
               neg_d * neg_d * neg_d * neg_d * neg_d * neg_d * neg_d * neg_d) * lvalue) / rvalue) *
              (pos_d * pos_d * pos_d * pos_d * pos_d * pos_d * pos_d * pos_d *
               pos_d * pos_d * pos_d * pos_d * pos_d * pos_d * pos_d * pos_d);

    return value;
}

int JFP_lib_fcmpl_x86(float lvalue, float rvalue) {
    long  rrawbits = *(long *)&rvalue;
    long  lrawbits = *(long *)&lvalue;

    if ((lrawbits >= F_L_POS_NAN) && (lrawbits <= F_H_POS_NAN) ||
        (lrawbits >= F_L_NEG_NAN) && (lrawbits <= F_H_NEG_NAN) ||
        (rrawbits >= F_L_POS_NAN) && (rrawbits <= F_H_POS_NAN) ||
        (rrawbits >= F_L_NEG_NAN) && (rrawbits <= F_H_NEG_NAN)) {
        return -1;
    } else {
        return (lvalue >  rvalue) ?  1 : (lvalue == rvalue) ?  0 : -1;
    }
}

int JFP_lib_fcmpg_x86(float lvalue, float rvalue) {
    long  rrawbits = *(long *)&rvalue;
    long  lrawbits = *(long *)&lvalue;

    if ((lrawbits >= F_L_POS_NAN) && (lrawbits <= F_H_POS_NAN) ||
        (lrawbits >= F_L_NEG_NAN) && (lrawbits <= F_H_NEG_NAN) ||
        (rrawbits >= F_L_POS_NAN) && (rrawbits <= F_H_POS_NAN) ||
        (rrawbits >= F_L_NEG_NAN) && (rrawbits <= F_H_NEG_NAN)) {
        return 1;
    } else {
        return (lvalue >  rvalue) ?  1 : (lvalue == rvalue) ?  0 : (lvalue <  rvalue) ? -1 : 1;
    }
}

int JFP_lib_dcmpl_x86(double lvalue, double rvalue) {
    long64 rrawbits = *(long64 *)&rvalue;
    long64 lrawbits = *(long64 *)&lvalue;

    if ((lrawbits >= D_L_POS_NAN) && (lrawbits <= D_H_POS_NAN) ||
        (lrawbits >= D_L_NEG_NAN) && (lrawbits <= D_H_NEG_NAN) ||
        (rrawbits >= D_L_POS_NAN) && (rrawbits <= D_H_POS_NAN) ||
        (rrawbits >= D_L_NEG_NAN) && (rrawbits <= D_H_NEG_NAN)) {
        return -1;
    } else {
        return (lvalue >  rvalue) ?  1 : (lvalue == rvalue) ?  0 : -1;
    }
}

int JFP_lib_dcmpg_x86(double lvalue, double rvalue) {
    long64 rrawbits = *(long64 *)&rvalue;
    long64 lrawbits = *(long64 *)&lvalue;

    if ((lrawbits >= D_L_POS_NAN) && (lrawbits <= D_H_POS_NAN) ||
        (lrawbits >= D_L_NEG_NAN) && (lrawbits <= D_H_NEG_NAN) ||
        (rrawbits >= D_L_POS_NAN) && (rrawbits <= D_H_POS_NAN) ||
        (rrawbits >= D_L_NEG_NAN) && (rrawbits <= D_H_NEG_NAN)) {
        return 1;
    } else {
        return (lvalue >  rvalue) ?  1 : (lvalue == rvalue) ?  0 : (lvalue <  rvalue) ? -1 : 1;
    }
}

double JFP_lib_frem_x86(float lvalue, float rvalue) {
    float result;
    long  rrawbits = *(long *)&rvalue;
    long  lrawbits = *(long *)&lvalue;

    if (((rrawbits == F_POS_INFINITY) || (rrawbits == F_NEG_INFINITY)) &&
        ((lrawbits & 0x7FFFFFFF) < F_POS_INFINITY)) {
        return lvalue;
    } else {
        result = (float)DOUBLE_REMAINDER(lvalue, rvalue);
        /* Retrieve the sign bit to find +/- 0.0 */
        if ((lrawbits & 0x80000000) == 0x80000000) {
            if ((*(long *)&result & 0x80000000) != 0x80000000) {
                result *= -1;
            }
        }
        return result;
    }
}

double JFP_lib_drem_x86(double lvalue, double rvalue) {
    double result;
    long64 rrawbits = *(long64 *)&rvalue;
    long64 lrawbits = *(long64 *)&lvalue;

    if (((rrawbits == D_POS_INFINITY) || (rrawbits == D_NEG_INFINITY)) &&
        ((lrawbits & 0x7FFFFFFFFFFFFFFFL) < D_POS_INFINITY)) {
        return lvalue;
    } else {
        result = DOUBLE_REMAINDER(lvalue, rvalue);
        /* Retrieve the sign bit to find +/- 0.0 */
        if ((lrawbits & 0x8000000000000000L) == 0x8000000000000000L) {
            if ((*(long64 *)&result & 0x8000000000000000L) != 0x8000000000000000L) {
                result *= -1;
            }
        }
        return result;
    }
}

void main() {

    printf("mul = %f\n", JFP_lib_dmul_x86(3.14159265, 2.0));
    printf("div = %f\n", JFP_lib_ddiv_x86(3.14159265, 2.0));
    printf("rem = %f\n", JFP_lib_drem_x86(3.14159265, 2.0));

}