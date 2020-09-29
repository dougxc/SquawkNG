//if[SYSTEM.STREAMS]
/*
 * Copyright 1996-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.io;

public class NativePrintStream extends PrintStream {


    public NativePrintStream() {
    }

    public void flush() {
    }

    public void close() {
    }

    public boolean checkError() {
        return false;
    }

    protected void setError() {
        Native.fatalVMError();
    }

    public void write(int b) {
        Native.fatalVMError();
    }

    public void write(byte buf[], int off, int len) {
        Native.fatalVMError();
    }

    protected void write(char buf[]) {
        Native.fatalVMError();
    }

    protected void write(String s) {
        Native.print(s);
    }

    protected void newLine() {
        Native.println();
    }



}


