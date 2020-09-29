//J2C:nassist.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/abstract public class NativeBuffers extends StringAccess {


/*-----------------------------------------------------------------------*\
 *                     Native function interface                         *
\*-----------------------------------------------------------------------*/

    /**
     * Get the string inside the memory as a String object in the host VM's memory.
     * @param string The address of a String inside the Squawk VM's memory.
     * @return the corresponding String object in the host VM's memory.
     */
    Object getNativeString(int string) {
        Object result;
        int i;
        int count  = String_length(string);
//IFC// jchar *buf = (jchar *)malloc(2*count);
/*IFJ*/ char[] buf = new char[count];;
        for (i = 0; i < count; i++) {
//IFC//     buf[i] = (jchar)String_at(string, i);
/*IFJ*/     buf[i] = (char) String_at(string, i);
        }
/*IFJ*/ result = new String(buf);
//IFC// result = (*env)->NewString(env, (const jchar*)buf, count);
//IFC// jni_assume(result != null, "String allocation failed");
//IFC// free(buf);
        return result;
    }

    Object getNativeByteArray(int arr) {
        int length = Object_getArrayElementCount(arr);
        Object result;
        assume(arr != 0); // This test should be done in Java code so that a NullPointerException can be thrown
/*IFJ*/ byte[] barr = new byte[length];
/*IFJ*/ for (int i = 0; i != length; i++) {
/*IFJ*/     barr[i] = (byte)getUnsignedByte(arr, i);
/*IFJ*/ }
/*IFJ*/ result = barr;
//IFC// result = (*env)->NewByteArray(env, length);
//IFC// jni_assume(result != null, "Byte array allocation failed");
//IFC// (*env)->SetByteArrayRegion(env,  result, 0, length, ptrForJni(arr));
//IFC// jni_assume(!(*env)->ExceptionOccurred(env), "Byte array copy failed");
        return result;
    }

    Object getNativeNewByteArray(int len) {
//IFC// return (*env)->NewByteArray(env, len);
/*IFJ*/ return new byte[len];
    }

    void setNativeByteArray(int arr, Object buf, int off, int len) {
        assume(arr != 0 && buf != null); // This test should be done in Java code so that a NullPointerException can be thrown
/*IFJ*/ byte[] barr = (byte[])buf;
/*IFJ*/ for (int i = 0; i != len; i++) {
/*IFJ*/     setByte(arr, off+i, barr[i]);
/*IFJ*/ }
//IFC// (*env)->GetByteArrayRegion(env, buf, 0, len, ptrForJni(arr+off));
//IFC// jni_assume(!(*env)->ExceptionOccurred(env), "Byte array copy back failed");
    }

    Object getNativeIntArray(int arr) {
        int length = Object_getArrayElementCount(arr);
        Object result;
        assume(arr != 0); // This test should be done in Java code so that a NullPointerException can be thrown
/*IFJ*/ int[] iarr = new int[length];
/*IFJ*/ for (int i = 0; i != length; i++) {
/*IFJ*/     iarr[i] = getWord(arr, i);
/*IFJ*/ }
/*IFJ*/ result = iarr;
//IFC// result = (*env)->NewIntArray(env, length);
//IFC// jni_assume(result != null, "Int array allocation failed");
//IFC// (*env)->SetIntArrayRegion(env,  result, 0, length, (jint*)ptrForJni(arr));
//IFC// jni_assume(!(*env)->ExceptionOccurred(env), "Int array copy failed");
        return result;
    }

    Object getSendBuffer(int obj) {
        if (obj != 0) {
            int cls = Object_getClass(obj);
            int cno = Class_getType(cls);
            if (cno == CNO.STRING || cno == CNO.STRING_OF_BYTES || cno == CNO.STRING_OF_SYMBOLS) {
                return getNativeString(obj);
            } else if (cno == CNO.BYTE_ARRAY) {
                return getNativeByteArray(obj);
            } else if (cno == CNO.INT_ARRAY) {
                return getNativeIntArray(obj);
            } else {
                fatalVMError("Bad reference type");
            }
        }
        return null;
    }

    Object getReceiveBuffer(int obj) {
        if (obj != 0) {
            int cls = Object_getClass(obj);
            int cno = Class_getType(cls);
            if (cno == CNO.BYTE_ARRAY) {
                return getNativeByteArray(obj);
            } else {
                fatalVMError("Bad reference type");
            }
        }
        return null;
    }



/*IFJ*/}
