/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

import com.sun.squawk.vm.OPC;
/*if[SYSTEM.STREAMS]*/
import java.io.PrintStream;
/*end[SYSTEM.STREAMS]*/

/**
 * The <code>Throwable</code> class is the superclass of all errors
 * and exceptions in the Java language. Only objects that are
 * instances of this class (or of one of its subclasses) are thrown
 * by the Java Virtual Machine or can be thrown by the Java
 * <code>throw</code> statement. Similarly, only this class or one of
 * its subclasses can be the argument type in a <code>catch</code>
 * clause.
 * <p>
 * Instances of two subclasses, {@link java.lang.Error} and
 * {@link java.lang.Exception}, are conventionally used to indicate
 * that exceptional situations have occurred. Typically, these instances
 * are freshly created in the context of the exceptional situation so
 * as to include relevant information (such as stack trace data).
 * <p>
 * By convention, class <code>Throwable</code> and its subclasses have
 * two constructors, one that takes no arguments and one that takes a
 * <code>String</code> argument that can be used to produce an error
 * message.
 * <p>
 * A <code>Throwable</code> class contains a snapshot of the
 * execution stack of its thread at the time it was created. It can
 * also contain a message string that gives more information about
 * the error.
 * <p>
 * Here is one example of catching an exception:
 * <p><blockquote><pre>
 *     try {
 *         int a[] = new int[2];
 *         a[4];
 *     } catch (ArrayIndexOutOfBoundsException e) {
 *         System.out.println("exception: " + e.getMessage());
 *         e.printStackTrace();
 *     }
 * </pre></blockquote>
 *
 * @author  unascribed
 * @version 1.43, 12/04/99 (CLDC 1.0, Spring 2000)
 * @since   JDK1.0
 */
public class Throwable {

    /** WARNING: this must be the first variable.
     * Specific details about the Throwable.  For example,
     * for FileNotFoundThrowables, this contains the name of
     * the file that could not be found.
     *
     * @serial
     */
    private String detailMessage;

    /**
     * Constructs a new <code>Throwable</code> with <code>null</code> as
     * its error message string.
     */
    public Throwable() {
/*if[DEBUG.METHODDEBUGTABLE]*/
        if (VMExtension.systemUp) {
            fillInStackTrace();
        }
//printStackTrace();
/*end[DEBUG.METHODDEBUGTABLE]*/
    }

    /**
     * Constructs a new <code>Throwable</code> with the specified error
     * message.
     *
     * @param   message   the error message. The error message is saved for
     *          later retrieval by the {@link #getMessage()} method.
     */
    public Throwable(String message) {
        this();
        detailMessage = message;
    }

    /**
     * Returns the error message string of this throwable object.
     *
     * @return  the error message string of this <code>Throwable</code>
     *          object if it was {@link #Throwable(String) created} with an
     *          error message string; or <code>null</code> if it was
     *          {@link #Throwable() created} with no error message.
     *
     */
    public String getMessage() {
        return detailMessage;
    }

    /**
     * Returns a short description of this throwable object.
     * If this <code>Throwable</code> object was
     * {@link #Throwable(String) created} with an error message string,
     * then the result is the concatenation of three strings:
     * <ul>
     * <li>The name of the actual class of this object
     * <li>": " (a colon and a space)
     * <li>The result of the {@link #getMessage} method for this object
     * </ul>
     * If this <code>Throwable</code> object was {@link #Throwable() created}
     * with no error message string, then the name of the actual class of
     * this object is returned.
     *
     * @return  a string representation of this <code>Throwable</code>.
     */
    public String toString() {
        String s = getClass().getName();
        String message = getMessage();
        return (message != null) ? (s + ": " + message) : s;
    }



/*if[SYSTEM.STREAMS]*/
    private PrintStream stream;
/*end[SYSTEM.STREAMS]*/

    private void println() {
/*if[SYSTEM.STREAMS]*/
        if (stream != null) {
            stream.println();
        } else
/*end[SYSTEM.STREAMS]*/
            Native.println();
    }

    private void print(String s) {
/*if[SYSTEM.STREAMS]*/
        if (stream != null) {
            stream.print(s);
        } else
/*end[SYSTEM.STREAMS]*/
            Native.print(s);
    }

    private void print(int i) {
/*if[SYSTEM.STREAMS]*/
        if (stream != null) {
            stream.print(i);
        } else
/*end[SYSTEM.STREAMS]*/
            Native.print(i);
    }

    private void println(String s) {
/*if[SYSTEM.STREAMS]*/
        if (stream != null) {
            stream.println(s);
        } else
/*end[SYSTEM.STREAMS]*/
            Native.println(s);
    }

    /**
     * Prints this <code>Throwable</code> and its backtrace to the
     * standard error stream. This method prints a stack trace for this
     * <code>Throwable</code> object on the error output stream that is
     * the value of the field <code>System.err</code>. The first line of
     * output contains the result of the {@link #toString()} method for
     * this object. <p>
     *
     * The format of the backtrace information depends on the implementation.
     */
    public void printStackTrace() {
        String message = getMessage();
        print(this.getClass().getName());
        if (message != null) {
            print(": ");
            println(message);
        } else {
            println();
        }
/*if[DEBUG.METHODDEBUGTABLE]*/
        printStackTrace0();
/*end[DEBUG.METHODDEBUGTABLE]*/
    }

/*if[SYSTEM.STREAMS]*/
    /**
     * Prints this <code>Throwable</code> and its backtrace to the
     * standard error stream. This method prints a stack trace for this
     * <code>Throwable</code> object on the error output stream that is
     * the value of the field <code>System.err</code>. The first line of
     * output contains the result of the {@link #toString()} method for
     * this object. <p>
     *
     * The format of the backtrace information depends on the implementation.
     */
    public void printStackTrace(PrintStream stream) {
        this.stream = stream;
        printStackTrace();
        this.stream = null;
    }
/*end[SYSTEM.STREAMS]*/



/*if[DEBUG.METHODDEBUGTABLE]*/
    /**
     * Flag to spot recursive errors
     */
    private static boolean inPrintStackTrace0;


    /**
     * getMethodIdOffset
     */
    int getMethodIdOffset(byte[] method) {
        int p = method[0];
        if ((method[p]&0xFF) >= OPC.EXTEND_0 && (method[p]&0xFF) <= OPC.EXTEND_15)  p++;
        if ((method[p]&0xFF) == OPC.EXTEND)                                  p += 2;
        if ((method[p]&0xFF) == OPC.NEWOBJECT)                               p++;
        if ((method[p]&0xFF) == OPC.CLASS_CLINIT)                            p++;
        if ((method[p]&0xFF) == OPC.METHODID) {
            return p+1;
        }
        return -1;
    }

    /**
     * Fills in the execution stack trace. This method records within this Throwable object
     * information about the current state of the stack frames for the current thread.
     */
    protected void fillInStackTrace() {
        int depth        = Native.getStackFrameDepth();
        byte[][] methods = new byte[depth][];
        short[] ips      = new short[depth];
        Native.getActivationStack(methods, ips);
        stackTrace = new StackTrace(methods, ips);
    }

    /**
     * printStackTrace0
     */
    private void printStackTrace0() {
        if (stackTrace == null) {
            return;
        }
        if (inPrintStackTrace0) {
             Native.fatalVMError();
        }
        inPrintStackTrace0 = true;
        try {
            byte[][] methods = stackTrace.methods;
            short[] ips      = stackTrace.ips;
            int depth        = ips.length;
            String guard     = getClass().getName().concat(".<init>");
            for (int i = 0; i != depth; i++) {
                int ip        = ips[i];
                byte[] method = methods[i];

                int offset = getMethodIdOffset(method);
                if (offset != -1) {
                    // Extract the method ID from the bytecode
                    int id = ((method[offset++]&0xFF)<<24) +
                             ((method[offset++]&0xFF)<<16) +
                             ((method[offset++]&0xFF)<<8) +
                              (method[offset++]&0xFF);

                    // Adjust the IP to account for instructions
                    // added by the loader, the last of which is the
                    // OPC_METHODID instruction.
                    ip -= offset;
                    if (ip < 0) {
                        ip = 0;
                    }

                    MethodDebugTable.Method debug = MethodDebugTable.getMethodDebug(id);
                    if (debug != null) {
                        String signature = debug.signature;
                        int start = signature.indexOf(' ')+1;
                        int end   = signature.indexOf('(', start) + 1;
                        if (start != 0 && end != 0) {

                            // Only start printing once we reach the constructor of this Throwable
                            if (guard != null) {
                                if (signature.regionMatches(false, start, guard, 0, guard.length())) {
                                    guard = null;
                                }
                            }

                            if (guard == null) {
                                int index = debug.filePath.lastIndexOf('/') + 1;
                                int sourceLine = debug.getSourceLineFor(ip);
                                print("\t");
                                print(signature.substring(start, end));
                                print(debug.filePath.substring(index));
                                if (sourceLine == -1) {
                                    print(":ip=");
                                    print(ip);
                                } else {
                                    print(":");
                                    print(sourceLine);
                                }
                                println(")");

                            }
                            continue;
                        }
                    }
                }

                // Fall through to here when symbolic info for the current frame
                // is unavailable
                if (guard == null) {
                    println("\t<unknown>");
                }
            }
        } finally {
            inPrintStackTrace0 = false;
        }
    }

    protected StackTrace stackTrace;
    private static class StackTrace {
        final byte[][] methods;
        final short[] ips;
        StackTrace(byte[][] methods, short[] ips) {
            this.methods = methods;
            this.ips     = ips;
        }
    }
/*end[DEBUG.METHODDEBUGTABLE]*/
}