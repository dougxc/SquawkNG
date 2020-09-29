package com.sun.squawk.translator.util;

import java.io.*;
import java.util.Vector;

public class BufferedReader {
    private Reader mReader;
    private char[] mBuffer;

    public BufferedReader(Reader r) {
        mReader = r;
    }

    public String readLine() throws IOException {
        boolean trucking = true;
        boolean eol = true;
        StringBuffer sb = new StringBuffer();
        while (trucking) {
            int c = mReader.read();
            if (c == '\n' || c == -1) {
                trucking = false;
                eol = eol && (c == -1);
            } else {
                eol = false;
                if (c != '\r') {
                    sb.append((char)c);
                }

            }
        }
        if (eol) {
            return null;
        }
        return sb.toString();
    }

    /**
     * Read all the lines from the input stream and add them to a given
     * Vector.
     * @param v The vector to add to or null if it should be created first by
     * this method.
     * @return
     * @throws IOException
     */
    public Vector readLines(Vector v) throws IOException {
        if (v == null) {
            v = new Vector();
        }
        for (String line = readLine(); line != null; line = readLine()) {
            v.addElement(line);
        }
        return v;
    }
}

