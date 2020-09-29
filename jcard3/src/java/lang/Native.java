/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

import java.io.*;
import java.util.*;
import javax.microedition.io.Connector;
import com.sun.squawk.util.IntHashtable;
import com.sun.squawk.vm.ChannelOpcodes;
import com.sun.squawk.vm.SquawkConstants;

public class Native {

    /*
     * Must be first
     */
    private Native() {}

    /*
     * NOTE - The "native" keyword does not do anything in the Squawk system.
     * Native methods are defined as methods defined in this class (Native.java)
     * that start with an '_' character. The use of the native keyword is just
     * a convenient way of defining a method without a body. Sometimes native
     * methods are defined with bodies in order to ease romizer integration.
     */
    native   static void                enableExcessiveGC();
    native   static void                primAstoreOop(Object array, int index, Object value);
    native   static long                getInstructionCount();
    native   static int                 getAddress(Object o);
    native   static void                traceOop(Object o);
    native   static Object              getPersistentMemoryTable(); /*@stub: { return null; } */
    native   static Object              makePersistent(Object o);
    native   static Object              makePersistentCopy(Object o); /*@stub: { return o; } */
    native   static void                setPersistentMemoryTable(Object table); /*@stub: {} */
    native   static void                setAssociationHashtable(Object table);
    native   static int                 exec(int chan, int op, int i1, int i2, int i3, int i4, int i5, int i6, Object p1, Object p2, Object p3);
    native   static long                result(int chan);
    native   static Object              getNewExecutionContext(Thread newThread);
    native   static void                setExecutionContext(Thread oldThread, boolean oldIsDead, Thread newThread, int id);
    native   static Object              primNewArray(Klass cls, int size);
    native   static void                callStaticNoParm(Klass k, int slot);
    native   static void                callStaticOneParm(Object parm, Klass k, int slot);
    native   static Suite[]             getSuiteList(); /*@stub: {return new Suite[0];} */
    native   static void                setSuiteList(Suite[] list); /*@stub: {} */
    native   static void                setOutOfMemoryError(Object o);
    native   static int                 getargc();
    native   static int                 getargvchar(int arg, int pos);
    native   static void                exit(int res);
    native   static void                arraycopy0(Object src, int spos, Object dst, int dpos, int lth);
    native   static void                putch(int ch); /*@stub: {} */;
    native   static ObjectAssociation   getAssociationQueue(Object hash); /*@stub: { return null; } */
    native   static void                setAssociationQueue(Object hash, ObjectAssociation assn); /*@stub: {} */
    native   static Object              getClassState(Klass c);
    native   static Object              createClassState(Klass c);
    native   static int                 primGetStatic(Object classState, int offset);
    native   static long                primGetStaticLong(Object classState, int offset);
    native   static void                primPutStatic(Object classState, int offset, int value);
    native   static void                primPutStaticLong(Object classState, int offset, long value);
    native   static void                primPutStaticObj(Object classState, int offset, Object value);
    native   static void                setClassState(Klass c, Object classState);
    native   static Object              primNewObject(Klass c, int slot);
    native   static void                setFastLockStack(Object[] stack);
    native   static int                 getStackFrameDepth();
    native   static void                getActivationStack(byte[][] methods, short[] ipStack);
    native   static ObjectAssociation   getFinalizer();
    native   static void                puterrch(int ch);
    native   static int                 getPersistentMemorySize(); /*@stub: { return 0; } */
    native   static void                gcPersistentMemory(int[] bitVector, int[] markingStack); /*@stub: {} */
    native   static boolean             wasPersistentMemoryGCInterrupted();
    native   static void                setMinimumHeapMode(boolean val);
    native   static long                getHeapHighWaterMark();
    native   static IntHashtable        getMethodDebugTable(); /*@stub: { return new com.sun.squawk.util.IntHashtable();} */
    native   static void                setMethodDebugTable(IntHashtable methodDebugTable); /*@stub: {} */
    native   static String              makeEightBitString(byte[] b);
    native   static String              makeSixteenBitString(char[] c);
    native   static void                stringcopy0(Object src, int spos, Object dst, int dpos, int lth);
    native   static StringOfSymbols     makeStringOfSymbols(String s);
    native   static boolean             inRam(Object ref); /*@stub: { return true; } */
    native   public static byte[]       newPersistentByteArray(int size); /*@stub: { return new byte[size]; } */

/*if[FLOATS]*/
    native   static double              math(int code, double a, double y);
    native   static int                 floatToIntBits(float value);
    native   static long                doubleToLongBits(double value);
    native   static float               intBitsToFloat(int bits);
    native   static double              longBitsToDouble(long bits);
/*end[FLOATS]*/

    /*
     * execIO
     */
    public static long execIO(int chan, int op, int i1, int i2, int i3, int i4, int i5, int i6, Object send1, Object send2, Object receive1) throws IOException {
        for (;;) {
            int result = exec(chan, op, i1, i2, i3, i4, i5, i6, send1, send2, receive1);
            if (result == 0) {
                break;
            }
            if (result < 0) {
                switch (result) {
                    case SquawkConstants.EXNO_NoConnection:  throw new javax.microedition.io.ConnectionNotFoundException();
                    case SquawkConstants.EXNO_IOException:   throw new IOException();
                    case SquawkConstants.EXNO_EOFException:  throw new EOFException();
                    default:                fatalVMError();
                }
            } else {
/*if[NOTROMIZER]*/
                Thread.waitForEvent(result);
/*end[NOTROMIZER]*/
            }
        }
        return result(chan);
    }


    /*
     * execIO2
     */
    public static long execIO2(int op, int i1, int i2, int i3, int i4, int i5, int i6, Object send1, Object send2, Object receive1) {
        try {
            return execIO(2, op, i1, i2, i3, i4, i5, i6, send1, send2, receive1);
        } catch(IOException ex) {
            throw new RuntimeException("Error executing channel 2 "+ex);
        }
    }

    /*
     * execIO3
     */
    public static long execIO3(int op, int i1, int i2, int i3, int i4, int i5, int i6, Object send1, Object send2, Object receive1) {
        try {
            return execIO(3, op, i1, i2, i3, i4, i5, i6, send1, send2, receive1);
        } catch(IOException ex) {
            throw new RuntimeException("Error executing channel 3 "+ex);
        }
    }


    /*
     * getChannel
     */
    public static int getChannel() {
        return exec(0, ChannelOpcodes.GETCHANNEL, 0, 0, 0, 0, 0, 0, null, null, null);
    }

    /*
     * freeChannel
     */
    public static void freeChannel(int chan) {
        exec(0, ChannelOpcodes.FREECHANNEL, chan, 0, 0, 0, 0, 0, null, null, null);
    }


    /*
     *
     */
    static String[] getCommandLineArgs() {
        int argc = getargc();
        String[] args = new String[argc];
        for (int i = 0 ; i < argc ; i++) {
            StringBuffer arg = new StringBuffer();
            for (int j = 0 ;; j++) {
                int ch = getargvchar(i, j);
                if (ch == 0) {
                    break;
                }
                arg.append((char)ch);
            }
            args[i] = arg.toString();
        }
        return args;
    }

    /**
     * Runs the garbage collector.
     */
    public native static void gc(); /*@stub: {} */;

    /**
     * Returns the amount of free memory in the system. Calling the
     * <code>gc</code> method may result in increasing the value returned
     * by <code>freeMemory.</code>
     *
     * @return  an approximation to the total amount of memory currently
     *          available for future allocated objects, measured in bytes.
     */
    public static native long freeMemory(boolean ram);


    /**
     * Returns the total amount of memory in the Squawk Virtual Machine.
     * The value returned by this method may vary over time, depending on
     * the host environment.
     * <p>
     * Note that the amount of memory required to hold an object of any
     * given type may be implementation-dependent.
     *
     * @return  the total amount of memory currently available for current
     *          and future objects, measured in bytes.
     */
    public static native long totalMemory(boolean ram);


    /**
     * Get the current time in milliseconds.
     */
    public static native long getTime();

    /*
     * arraycopy - Must only be called from System.arraycopy(), see the code there.
     */
    static void arraycopy(Object src, int src_position, Object dst, int dst_position, int totalLength) {
        final int MAXMOVE = 4096;
        while (true) {
            int length = Math.min(totalLength, MAXMOVE);
            arraycopy0(src, src_position, dst, dst_position, length);
            totalLength -= length;
            if (totalLength == 0) {
                break;
            }
            src_position += length;
            dst_position += length;
            Thread.yield();
        }
    }


    /*
     * stringcopy - Must only be called from String(), see the code there.
     */
    static void stringcopy(Object src, int src_position, Object dst, int dst_position, int totalLength) {
        final int MAXMOVE = 4096;
        int srcEnd = totalLength + src_position;
        int dstEnd = totalLength + dst_position;
        if (
                (totalLength < 0) ||
                (src_position < 0) ||
                (dst_position < 0) ||
                (totalLength > 0 && (srcEnd < 0 || dstEnd < 0)) ||
                (srcEnd > Native.getArrayLength(src)) ||
                (dstEnd > Native.getArrayLength(dst))
           ) {
            throw new ArrayIndexOutOfBoundsException();
        }
        while (true) {
            int length = Math.min(totalLength, MAXMOVE);
            stringcopy0(src, src_position, dst, dst_position, length);
            totalLength -= length;
            if (totalLength == 0) {
                break;
            }
            src_position += length;
            dst_position += length;
            Thread.yield();
        }
    }


    /**
     * getEvent
     */
    public static native int getEvent();


    /**
     * waitForEvent
     */
    public static native void waitForEvent(long time);


    /**
     * Stop the virtual machine due to a fatal error.
     */
    public static native void fatalVMError();


    /**
     * Test some condition and stop the virtual machine if it is false.
     *
     * @param b the condition to test.
     */
    public static void assume(boolean b) {
        if (!b) {
            fatalVMError();
        }
    }

    /**
     * getClass
     */
    public static native Object getClass(Object obj);


    /**
     * getArrayLength
     */
    public static native int getArrayLength(Object array);


    /*
     * write
     */
    public static void print(char ch) {
        putch(ch);
    }

    public static void printErr(char ch) {
        puterrch(ch);
    }


    /**
     * Print a string to the terminal.
     *
     * @param s The string to print.
     */
    public static native void print(String s); /*@stub: {} */

    public static void print(boolean b)    { print(b ? "true" : "false"); }
    public static void print(int i)        { print(String.valueOf(i)); }
    public static void print(long l)       { print(String.valueOf(l)); }
    public static void print(Object obj)   { print(String.valueOf(obj)); }
    public static void println()           { print("\n"); }
    public static void println(boolean x)  { print(x); println(); }
    public static void println(char x)     { print(x); println(); }
    public static void println(int x)      { print(x); println(); }
    public static void println(long x)     { print(x); println(); }
    public static void println(char x[])   { print(x); println(); }
    public static void println(String x)   { print(x); println(); }
    public static void println(Object x)   { print(x); println(); }


    /**
     * This is the table of system properties underlying the getProperty and
     * setProperty methods in System.
     */

    private static NoSyncHashtable systemProperties;

    /*
     * setProperty
     */
    static String setProperty(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key can't be null");
        }
        if (key.equals("")) {
            throw new IllegalArgumentException("key can't be empty");
        }
        if (systemProperties == null) {
            systemProperties = new NoSyncHashtable();
        }
        return (String)systemProperties.put(key, value);
    }


    /*
     * getProperty
     */
    static String getProperty(String key) {
        if (key == null) {
            throw new NullPointerException("key can't be null");
        }
        if (key.equals("")) {
            throw new IllegalArgumentException("key can't be empty");
        }

        // These are the hard-coded properties that cannot be changed.
        if (key.equals("microedition.configuration"))                   return "CLDC-1.0";
        if (key.equals("microedition.encoding"))                        return "ISO8859_1";
        if (key.equals("microedition.platform"))                        return "j2me";
        if (key.equals("awtcore.classbase"))                            return "awtcore.impl.squawk";
        if (key.equals("javax.microedition.io.Connector.protocolpath")) return "com.sun.squawk.io";

        if (systemProperties != null) {
            return (String)systemProperties.get(key);
        } else {
            return null;
        }
    }

    /**
     * Use the "systemproperties:" protocol if it exists to update the
     * system properties table from the system properties of the host
     * environment.
     *
     * The method enables an application to get access to all the system
     * properties when the VM is running on another JVM or at least
     * using an embedded JVM (via JNI) for it IO and graphics subsystems.
     *
     * Previous set system properties are not overridden.
     */
    public static void updateSystemPropertiesFromHostJVM() {
        NoSyncHashtable properties = systemProperties;
        if (properties == null) {
            properties = systemProperties = new NoSyncHashtable();
        }
        try {
            DataInputStream in = Connector.openDataInputStream("systemproperties:");
            while (in.available() != 0) {
                String key = in.readUTF();
                String value = in.readUTF();
                if (!properties.containsKey(key)) {
                    properties.put(key, value);
                }
            }
        } catch (IOException ioe) {
        }
    }
}

