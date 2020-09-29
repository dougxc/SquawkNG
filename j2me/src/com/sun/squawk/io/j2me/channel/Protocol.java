/*
 *  Copyright (c) 1999-2001 Sun Microsystems, Inc., 901 San Antonio Road,
 *  Palo Alto, CA 94303, U.S.A.  All Rights Reserved.
 *
 *  Sun Microsystems, Inc. has intellectual property rights relating
 *  to the technology embodied in this software.  In particular, and
 *  without limitation, these intellectual property rights may include
 *  one or more U.S. patents, foreign patents, or pending
 *  applications.  Sun, Sun Microsystems, the Sun logo, Java, KJava,
 *  and all Sun-based and Java-based marks are trademarks or
 *  registered trademarks of Sun Microsystems, Inc.  in the United
 *  States and other countries.
 *
 *  This software is distributed under licenses restricting its use,
 *  copying, distribution, and decompilation.  No part of this
 *  software may be reproduced in any form by any means without prior
 *  written authorization of Sun and its licensors, if any.
 *
 *  FEDERAL ACQUISITIONS:  Commercial Software -- Government Users
 *  Subject to Standard License Terms and Conditions
 */

package com.sun.squawk.io.j2me.channel;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.io.*;
import com.sun.squawk.vm.ChannelOpcodes;

/**
 * Channel Connection
 */

public class Protocol extends ConnectionBase implements StreamConnection, StreamConnectionNotifier {

    /** Channel number */
    int chan = 0;

    /**
     * execChan
     */
    //int execChan() throws IOException {
    //    return Native.execute(chan);
    //}

    /**
     * Public constructor
     */
    public Protocol() {
    }

    /**
     * Private constructor
     */
    private Protocol(int chan) {
        this.chan = chan;
    }

    /**
     * open
     */
    public Connection open(String protocol, String name, int mode, boolean timeouts) throws IOException {
        if (protocol == null || name == null) {
            throw new NullPointerException();
        }
        chan = Native.getChannel();
        Native.execIO(chan, ChannelOpcodes.OPEN, mode, timeouts?1:0, 0, 0, 0, 0, protocol, name, null);
        return this;
    }

    /**
     * openInputStream
     */
    public InputStream openInputStream() throws IOException {
        return new ChannelInputStream(chan);
    }

    /**
     * openOutputStream
     */
    public OutputStream openOutputStream() throws IOException {
        return new ChannelOutputStream(chan);
    }

    /**
     * acceptAndOpen
     */
    public StreamConnection acceptAndOpen() throws IOException {
        int newChan = (int)Native.execIO(chan, ChannelOpcodes.ACCEPT, 0, 0, 0, 0, 0, 0, null, null, null);
        return new Protocol(newChan);
    }

    /**
     * Close the connection.
     */
    synchronized public void close() throws IOException {
        Native.execIO(chan, ChannelOpcodes.CLOSE, 0, 0, 0, 0, 0, 0, null, null, null);
    }

    /**
     * finalize
     */
    protected void finalize() {
        Native.freeChannel(chan);
        chan = -1;
    }
}
