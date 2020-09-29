/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.io;

/**
 * This class implements an output stream into persistent memory.
 * The buffer has a fixed size determined at instance initialization
 * time.
 * The data can be retrieved using <code>toByteArray()</code> and
 * <code>toString()</code>.
 * @author  Nik Shaylor
 * @version 1.42, 12/04/99 (CLDC 1.0, Spring 2000)
 * @since   JDK1.0
 */

public class PersistentMemoryOutputStream extends ByteArrayOutputStream {

    /**
     * Creates a new byte array output stream, with a buffer capacity of
     * the specified size, in bytes.
     *
     * @param   size   the initial size.
     * @exception  IllegalArgumentException if size is negative.
     */
    public PersistentMemoryOutputStream(int size) {
/*if[NOTROMIZER]*/
        super(Native.newPersistentByteArray(size));
/*end[NOTROMIZER]*/
/*if[ROMIZER]*/
        super(size);
/*end[ROMIZER]*/
    }

    /**
     * Creates a new byte array
     */
    protected byte[] newBuf(int size) {
        throw new RuntimeException("PersistentMemoryOutputStream cannot be extended");
    }
}


