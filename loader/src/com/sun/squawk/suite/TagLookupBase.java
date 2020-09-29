////if[DEBUG.LOADER]   /* This will selectively exclude the entire file from the build */
package com.sun.squawk.suite;

import java.io.*;
import java.util.*;

/* ------------------------------------------------------------------------ *\
 *                              TagLookupBase                               *
\* ------------------------------------------------------------------------ */

public class TagLookupBase {

    private static Hashtable bytecodes;

    static {
        bytecodes = new Hashtable();
        new TagLookup(); // just to get the clinit called
    }

    /*
     * put
     */
    protected static void put(String name, int value) {
        bytecodes.put(name, new Integer(value));
//System.out.println("put "+name+" = "+value);
    }


    /*
     * getBytecode
     */
    public static int getBytecode(String name) {
        Integer i = (Integer)bytecodes.get(name);
        if (i == null) {
            throw new RuntimeException("Unknown bytecode: " + name);
        }
        int value = i.intValue();
        if (value < 0) {
            throw new RuntimeException("Negative bytecode: " + name + '(' + value + ')');
        }

//System.out.println(name+" = "+value);

        return value;
    }

    /*
     * getAccessCode
     */
    public static int getAccessCode(String name) {
        Integer i = (Integer)bytecodes.get(name);
        if (i == null) {
            throw new RuntimeException("Unknown access code: " + name);
        }
        int value = 0 - i.intValue();
        if (value <= 0) {
            throw new RuntimeException("Negative access flag: " + name + '(' + value + ')');
        }
        return value;
    }

}

