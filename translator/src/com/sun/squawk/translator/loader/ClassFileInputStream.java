
package com.sun.squawk.translator.loader;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.*;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * This class is used to read a stream corresponding to a classfile. While
 * it has an interface similiar to DataInput, it does not actually implement
 * this interface as the exception type throw by the read methods of this
 * class are LinkageException, not IOException.
 */
public class ClassFileInputStream {

    /** The file name. */
    protected final String fileName;
    /** The tracer. */
    protected Tracer tracer = new Tracer(System.err, false);
    /** The Translator context. */
    protected final Translator vm;
    /** The DataInputStream that this class delegates to. */
    protected final DataInputStream in;

    /**
     * Creates a <code>ClassFileInputStream</code>
     * and saves its  argument, the input stream
     * <code>in</code>, for later use.
     *
     * @param in The input stream.
     * @param fileName The file from which the stream was created.
     * @param vm The Translator context of this stream.
     */
    public ClassFileInputStream(InputStream in, String fileName, Translator vm) {
        if (in instanceof DataInputStream) {
            this.in = (DataInputStream)in;
        }
        else {
            this.in = new DataInputStream(in);
        }
        this.fileName = fileName;
        this.vm = vm;
    }

    /**
     * Return the file name this stream was constructed from.
     * @return the file name this stream was constructed from.
     */
    String getFileName() {
        return fileName;
    }

    /**
     * Return the Translator context of this stream.
     * @return the Translator context of this stream.
     */
    Translator getVM() {
        return vm;
    }

    /**
     * Raise a new LinkageException representing a ClassFormatError that was caused by an exception.
     * @param The cause.
     * @return the LinkageException raised. This is never actually returned but given this
     * method a return signature makes the calling code easier to read in that
     * redundant returns do not need to be injected to prevent 'missing return'
     * errors from javac.
     */
    public LinkageException classFormatError(Throwable cause) throws LinkageException {
        throw new LinkageException(vm.CLASSFORMATERROR, "While reading " + fileName, null, cause);
    }

    /**
     * Raise a new LinkageException representing a ClassFormatError.
     * @param msg detailed message.
     * @return the LinkageException raised. This is never actually returned but given this
     * method a return signature makes the calling code easier to read in that
     * redundant returns do not need to be injected to prevent 'missing return'
     * errors from javac.
     */
    public LinkageException classFormatError(String msg) throws LinkageException {
        throw new LinkageException(vm.CLASSFORMATERROR, "While reading " + fileName + ": " + msg);
    }


    /**
     * Return a new LinkageException representing a given LinkageError subclass that includes
     * a contextual string prefix.
     * @param errorClass The LinkageError subclass.
     * @param The detail message for the exception.
     * @return the LinkageException raised. This is never actually returned but given this
     * method a return signature makes the calling code easier to read in that
     * redundant returns do not need to be injected to prevent 'missing return'
     * errors from javac.
     */
    public LinkageException linkageError(Type errorClass, String msg) throws LinkageException {
        throw new LinkageException(errorClass, fileName + ": " + msg);
    }

    /**
     * Set (or unset) the tracing flag.
     * @param value the new tracing value
     * @return the old tracing value.
     */
    public boolean setTrace(boolean value) {
        boolean oldValue = tracer.switchedOn();
        tracer.switchOnOff(value);
        return oldValue;
    }

    /**
     * Return the current tracing state.
     * @return the current tracing state.
     */
    public boolean trace() {
        return tracer.switchedOn();
    }

    public Tracer tracer() {
        return tracer;
    }

    /**
     * Reads some bytes from an input stream and stores them into the buffer
     * array b. The number of bytes read is equal to the length of b.
     * @param b The buffer to fill.
     * @exception LinkageException if the underlying DataInputStream throws an IOException.
     */
    public void readFully(byte[] b) throws LinkageException{
        try {
            in.readFully(b);
        } catch (IOException ioe) {
            throw classFormatError(ioe);
        }
    }

    /**
     * Reads and returns an int from the underlying DataInputStream.
     * @param tracePrefix The prefix used when tracing this read.
     * @return the int value read.
     * @exception LinkageException if the underlying DataInputStream throws an IOException.
     */
    public int readInt(String tracePrefix) throws LinkageException {
        try {
            int value = in.readInt();
            tracer.traceln(tracePrefix != null, tracePrefix+":"+value);
            return value;
        } catch (IOException ioe) {
            throw classFormatError(ioe);
        }
    }

    /**
     * Reads and returns an unsigned short (as an int) from the underlying DataInputStream.
     * @param tracePrefix The prefix used when tracing this read.
     * @return the unsigned short value read.
     * @exception LinkageException if the underlying DataInputStream throws an IOException.
     */
    public int readUnsignedShort(String tracePrefix) throws LinkageException {
        try {
            int value = in.readUnsignedShort();
            tracer.traceln(tracePrefix != null, tracePrefix+":"+value);
            return value;
        } catch (IOException ioe) {
            throw classFormatError(ioe);
        }
    }

    /**
     * Reads and returns an unicode char from the underlying DataInputStream.
     * @param tracePrefix The prefix used when tracing this read.
     * @return the unicode char value read.
     * @exception LinkageException if the underlying DataInputStream throws an IOException.
     */
    public char readChar(String tracePrefix) throws LinkageException {
        return (char)readUnsignedShort(tracePrefix);
    }

    /**
     * Reads and returns an unsigned byte (as an int) from the underlying DataInputStream.
     * @param tracePrefix The prefix used when tracing this read.
     * @return the unsigned byte value read.
     * @exception LinkageException if the underlying DataInputStream throws an IOException.
     */
    public int readUnsignedByte(String tracePrefix) throws LinkageException {
        try {
             int value = in.readUnsignedByte();
             tracer.traceln(tracePrefix != null, tracePrefix+":"+value);
             return value;
        } catch (IOException ioe) {
            throw classFormatError(ioe);
        }

     }


    /**
     * Reads and returns a short (as an int) from the underlying DataInputStream.
     * @param tracePrefix The prefix used when tracing this read.
     * @return the short value read.
     * @exception LinkageException if the underlying DataInputStream throws an IOException.
     */
    public short readShort(String tracePrefix) throws LinkageException {
        try {
            short value = in.readShort();
            tracer.traceln(tracePrefix != null, tracePrefix+":"+value);
            return value;
        } catch (IOException ioe) {
            throw classFormatError(ioe);
        }

    }

    /**
     * Reads and returns a byte(as an int) from the underlying DataInputStream.
     * @param tracePrefix The prefix used when tracing this read.
     * @return the byte value read.
     * @exception LinkageException if the underlying DataInputStream throws an IOException.
     */
    public byte readByte(String tracePrefix) throws LinkageException {
        try {
            byte value = in.readByte();
            tracer.traceln(tracePrefix != null, tracePrefix+":"+value);
            return value;
        } catch (IOException ioe) {
            throw classFormatError(ioe);
        }
    }

    /**
     * Reads and returns a long from the underlying DataInputStream.
     * @param tracePrefix The prefix used when tracing this read.
     * @return the long value read.
     * @exception LinkageException if the underlying DataInputStream throws an IOException.
     */
    public long readLong(String tracePrefix) throws LinkageException {
        try {
            long value = in.readLong();
            tracer.traceln(tracePrefix != null, tracePrefix+":"+value);
            return value;
        } catch (IOException ioe) {
            throw classFormatError(ioe);
        }
    }

/*if[FLOATS]*/
    /**
     * Reads and returns a float from the underlying DataInputStream.
     * @param tracePrefix The prefix used when tracing this read.
     * @return the float value read.
     * @exception LinkageException if the underlying DataInputStream throws an IOException.
     */
    public float readFloat(String tracePrefix) throws LinkageException {
        try {
            float value = in.readFloat();
            tracer.traceln(tracePrefix != null, tracePrefix+":"+value);
            return value;
        } catch (IOException ioe) {
            throw classFormatError(ioe);
        }
    }

    /**
     * Reads and returns a double from the underlying DataInputStream.
     * @param tracePrefix The prefix used when tracing this read.
     * @return the double read.
     * @exception LinkageException if the underlying DataInputStream throws an IOException.
     */
    public double readDouble(String tracePrefix) throws LinkageException {
        try {
            double value = in.readDouble();
            tracer.traceln(tracePrefix != null, tracePrefix+":"+value);
            return value;
        } catch (IOException ioe) {
            throw classFormatError(ioe);
        }
    }
/*end[FLOATS]*/

    /**
     * Reads and returns a string encoded in UTF from the underlying DataInputStream.
     * @param tracePrefix The prefix used when tracing this read.
     * @return the UTF string value read.
     * @exception LinkageException if the underlying DataInputStream throws an IOException.
     */
    public String readUTF(String tracePrefix) throws LinkageException {
        try {
            String value = in.readUTF();
            tracer.traceln(tracePrefix != null, tracePrefix+":"+value);
            return value;
        } catch (IOException ioe) {
            throw classFormatError(ioe);
        }
    }

    /**
     * Closes this input stream and releases any system resources
     * associated with the stream.
     * @exception LinkageException if the underlying DataInputStream throws an IOException.
     */
    public void close() throws LinkageException {
        try {
            in.close();
        } catch (IOException ioe) {
            throw classFormatError(ioe);
        }
    }
}
