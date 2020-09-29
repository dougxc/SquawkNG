//J2C:string.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/abstract public class StringAccess extends HeaderLayout {

/*-----------------------------------------------------------------------*\
 *               Class "java.lang.String" accessor methods               *
\*-----------------------------------------------------------------------*/

/*if[OLDSTRING]*/

/*MAC*/ void String_assume(int $str) {
            if (assuming()) {
                int cls = Object_getClass($str);
                int cno = Class_getType(cls);
                assume(cno == STRING);
            }
        }

/*MAC*/ int String_length(int $str) {
            int value;
            String_assume($str);
            value = String_getValue($str);
            return Object_getArrayElementCount(value);
        }

/*MAC*/ boolean String_isEightBit(int $str) {
            int value;
            String_assume($str);
            value  = String_getValue($str);
            if (Class_getType(Object_getClass(value)) == CHAR_ARRAY) {
                return false;
            } else {
                assume(Class_getType(Object_getClass(value)) == BYTE_ARRAY);
                return true;
            }
        }

/*MAC*/ int String_at(int $str, int $pos) {
            int value, count;
            String_assume($str);
            value  = String_getValue($str);
            count  = String_length($str);
            assume($pos < count);
            if (Class_getType(Object_getClass(value)) == CHAR_ARRAY) {
                return getUnsignedHalf(value, $pos);
            } else {
                assume(Class_getType(Object_getClass(value)) == BYTE_ARRAY);
                return getUnsignedByte(value, $pos);
            }
        }

/*end[OLDSTRING]*/



/*if[NEWSTRING]*/

/*MAC*/ void String_assume(int $str) {
            if (assuming()) {
                int cno = Class_getType(Object_getClass($str));
                assume(cno == CNO.STRING || cno == CNO.STRING_OF_BYTES || cno == CNO.STRING_OF_SYMBOLS);
            }
        }

/*MAC*/ int String_length(int $str) {
            String_assume($str);
            return Object_getArrayElementCount($str);
        }

/*MAC*/ boolean String_isEightBit(int $str) {
            String_assume($str);
            return Class_getType(Object_getClass($str)) != CNO.STRING;
        }

/*MAC*/ int String_at(int $str, int $pos) {
            int count;
            assume($str != 0);
            count = String_length($str);
            assume($pos < count);
            if (String_isEightBit($str)) {
                return getUnsignedByte($str, $pos);
            } else {
                return getUnsignedHalf($str, $pos);
            }
        }

/*end[NEWSTRING]*/



/*IFJ*/}












///*IFJ*/System.out.println("String_at half value["+$pos+"]="+(int)getUnsignedHalf(value, $pos));
