/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package com.sun.squawk.io.j2me.channel;

import java.io.*;
import com.sun.squawk.vm.ChannelOpcodes;

/**
 * ChannelOutputStream
 */
public class ChannelOutputStream extends DataOutputStream {

    int chan;

    public ChannelOutputStream(int chan) throws IOException {
        super(null);
        this.chan = chan;
        Native.execIO(chan, ChannelOpcodes.OPENOUTPUT, 0, 0, 0, 0, 0, 0, null, null, null);
    }

    public void flush() throws IOException {
        Native.execIO(chan, ChannelOpcodes.FLUSH, 0, 0, 0, 0, 0, 0, null, null, null);
    }

    public void close() throws IOException {
        Native.execIO(chan, ChannelOpcodes.CLOSEOUTPUT, 0, 0, 0, 0, 0, 0, null, null, null);
    }

    public void write(int v) throws IOException {
        Native.execIO(chan, ChannelOpcodes.WRITEBYTE, v, 0, 0, 0, 0, 0, null, null, null);
    }

    public void writeShort(int v) throws IOException {
        Native.execIO(chan, ChannelOpcodes.WRITESHORT, v, 0, 0, 0, 0, 0, null, null, null);
    }

    public void writeChar(int v) throws IOException {
        writeShort(v);
    }

    public void writeInt(int v) throws IOException {
        Native.execIO(chan, ChannelOpcodes.WRITEINT, v, 0, 0, 0, 0, 0, null, null, null);
    }

    public void writeLong(long v) throws IOException {
        Native.execIO(chan, ChannelOpcodes.WRITELONG, (int)(v >>> 32), (int)v, 0, 0, 0, 0, null, null, null);
    }

    public void write(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        Native.execIO(chan, ChannelOpcodes.WRITEBUF, off, len, 0, 0, 0, 0, b, null, null);
    }

}


