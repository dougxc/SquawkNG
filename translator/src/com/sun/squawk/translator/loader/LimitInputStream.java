
package com.sun.squawk.translator.loader;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.*;

import java.io.InputStream;
import java.io.IOException;

class LimitInputStream extends InputStream {

   /**
    * Input Stream
    */
    private InputStream is;

   /**
    * Count of remaining bytes
    */
    private int count;

   /**
    * The "current" ip address
    */
    private int ip = 0;

   /**
    * Constructor
    */
    public LimitInputStream(InputStream is, int count) {
        this.is = is;
        this.count = count;
    }

   /**
    * Return EOF state
    */
    boolean atEof() {
        return ip == count;
    }

   /**
    * Return current ip address.
    */
    int getIP() {
        return ip;
    }

   /**
    * Read a byte
    */
    public int read() throws IOException {
        if (ip == count) {
            return -1;
        }
        ip++;
        return is.read();
    }
}