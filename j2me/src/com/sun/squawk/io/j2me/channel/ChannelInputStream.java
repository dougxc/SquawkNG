/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package com.sun.squawk.io.j2me.channel;
import com.sun.squawk.vm.ChannelOpcodes;

import java.io.*;

/**
 * ChannelInputStream
 */
public class ChannelInputStream extends DataInputStream {

    int chan;
    public ChannelInputStream(int chan) throws IOException {
        super(null);
        this.chan = chan;
        Native.execIO(chan, ChannelOpcodes.OPENINPUT, 0, 0, 0, 0, 0, 0, null, null, null);
    }

    public void close() throws IOException {
        Native.execIO(chan, ChannelOpcodes.CLOSEINPUT, 0, 0, 0, 0, 0, 0, null, null, null);
    }

    public int read() throws IOException {
        return (int)Native.execIO(chan, ChannelOpcodes.READBYTE, 0, 0, 0, 0, 0, 0, null, null, null);
    }

    public int readUnsignedShort() throws IOException {
        return (int)Native.execIO(chan, ChannelOpcodes.READSHORT, 0, 0, 0, 0, 0, 0, null, null, null);
    }

    public int readInt() throws IOException {
        return (int)Native.execIO(chan, ChannelOpcodes.READINT, 0, 0, 0, 0, 0, 0, null, null, null);
    }

    public long readLong() throws IOException {
        return Native.execIO(chan, ChannelOpcodes.READLONG, 0, 0, 0, 0, 0, 0, null, null, null);
    }

    public int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        return (int)Native.execIO(chan, ChannelOpcodes.READBUF, off, len, 0, 0, 0, 0, null, null, b);
    }

    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    public long skip(long n) throws IOException {
        return Native.execIO(chan, ChannelOpcodes.SKIP, (int)(n >>> 32), (int)n, 0, 0, 0, 0, null, null, null);
    }

    public int available() throws IOException {
        return (int)Native.execIO(chan, ChannelOpcodes.AVAILABLE, 0, 0, 0, 0, 0, 0, null, null, null);
    }

    public void mark(int readlimit) {
        try {
            Native.execIO(chan, ChannelOpcodes.MARK, readlimit, 0, 0, 0, 0, 0, null, null, null);
        } catch (IOException ex) {}

    }

    public void reset() throws IOException {
        Native.execIO(chan, ChannelOpcodes.RESET, 0, 0, 0, 0, 0, 0, null, null, null);
    }

    public boolean markSupported() {
        try {
            long res = Native.execIO(chan, ChannelOpcodes.MARK, 0, 0, 0, 0, 0, 0, null, null, null);
            return res != 0;
        } catch (IOException ex) {
            return false;
        }
    }

}


