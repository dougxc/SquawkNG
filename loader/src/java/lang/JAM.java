package java.lang;

import java.io.*;
import java.util.*;
import com.sun.squawk.loader.SuiteLoader;
import com.sun.squawk.suite.SuiteParser;
import com.sun.squawk.suite.BinarySuiteParser;
/*if[DEBUG.LOADER]*/
import com.sun.squawk.suite.XMLSuiteParser;
/*end[DEBUG.LOADER]*/

public class JAM {

    private static String fixUrl(String url) {
        if (url.indexOf(':') == -1) {
            return "file://"+url;
        } else {
            return url;
        }
    }

    private static boolean loadSuite(String suiteURL, OutputStream disasmPS, int flags, boolean accountMemory) {

        InputStream suiteIn = null;
        try {
            suiteIn = javax.microedition.io.Connector.openDataInputStream(suiteURL);
        } catch (IOException ioe) {
            Native.println("IOException opening suite input stream:");
/*if[SYSTEM.STREAMS]*/
            ioe.printStackTrace();
/*end[SYSTEM.STREAMS]*/
            return false;
        }

        Native.println("Loading suite from "+suiteURL);

        long mem = 0;
        if (accountMemory) {
            Native.gc();
            Native.setMinimumHeapMode(true);
            mem = Native.getHeapHighWaterMark();
        }

        SuiteParser parser;
/*if[DEBUG.LOADER]*/
        if (suiteURL.endsWith(".xml")) {
            parser = new XMLSuiteParser(suiteIn);
        } else
/*end[DEBUG.LOADER]*/
        {
            parser = new BinarySuiteParser(suiteIn);
        }

        SuiteLoader.run(parser, disasmPS, false, flags);

        if (accountMemory) {
            long hwm = Native.getHeapHighWaterMark();
            Native.println("Memory required to load suite from "+suiteURL+": " + (hwm-mem) + " ["+mem+"->"+hwm+"]");
        } else {
            Native.println("Loaded suite from "+suiteURL);
        }
        return true;
    }

    public static void usage(String errMsg) {
/*if[SYSTEM.STREAMS]*/
        PrintStream out = System.err;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: JAM [-options] class [args...]");
        out.println("where options include:");
        out.println("    -suite:<url>      load suite from url");
        out.println("    -accountMemory    measure amount of memory required to load");
        out.println("                      suites (requires -XexcessiveGC VM option)");
/*if[DEBUG.LOADER]*/
        out.println("    -traceloading     trace suite loading");
        out.println("    -traceverifier    trace verification");
        out.println("    -suitestats       show the suite stats");
        out.println("    -o:<url>          output disassembly to url (default = no trace)");
        out.println("    -a:<url>          append disassembly to url (default = no trace)");
/*if[DEBUG.METHODDEBUGTABLE]*/
        out.println("    -m:<url>          output a method map url (default = /dev/null)");
/*end[DEBUG.METHODDEBUGTABLE]*/
/*end[DEBUG.LOADER]*/
        out.println();
        out.println("Note: all <url>'s have 'file://' prepended if they do not contain a ':'");
        if (true) return;
/*end[SYSTEM.STREAMS]*/
        Native.println(errMsg);
    }

    public static void main(String[] args) throws ClassNotFoundException {
        int flags                 = 0;
        Vector suites             = new Vector();
        boolean accountMemory     = false;
/*if[DEBUG.METHODDEBUGTABLE]*/
        String mapURL             = null;
/*end[DEBUG.METHODDEBUGTABLE]*/
/*if[DEBUG.LOADER]*/
        String disasmURL     = null;
        boolean disasmAppend = false;
/*end[DEBUG.LOADER]*/
        int i;
        for (i = 0; i != args.length; i++) {
            String arg = args[i];
            if (arg.charAt(0) != '-') {
                break;
            }
            if (arg.startsWith("-suite:")) {
                suites.addElement(fixUrl(arg.substring("-suite:".length())));
            } else if (arg.equals("-accountMemory")) {
                accountMemory = true;
/*if[DEBUG.LOADER]*/
            } else if (arg.equals("-traceloading")) {
                flags |= SuiteLoader.TRACE_LOADING;
            } else if (arg.equals("-traceverifier")) {
                flags |= SuiteLoader.TRACE_VERIFIER;
            } else if (arg.equals("-suitestats")) {
                flags |= SuiteLoader.SUITE_STATS;
            } else if (arg.startsWith("-o:")) {
                disasmURL = fixUrl(arg.substring("-o:".length()));
                disasmAppend = false;
            } else if (arg.startsWith("-a:")) {
                disasmURL = fixUrl(arg.substring("-a:".length()));
                disasmAppend = true;
/*if[DEBUG.METHODDEBUGTABLE]*/
            } else if (arg.startsWith("-m:")) {
                mapURL = fixUrl(arg.substring("-m:".length()));
/*end[DEBUG.METHODDEBUGTABLE]*/
/*end[DEBUG.LOADER]*/
            } else {
                usage("Unknown option: "+arg);
                return;
            }
        }

        if (i == args.length) {
            usage("Missing class name");
            return;
        }

        OutputStream disasmPS = null;
/*if[DEBUG.LOADER]*/
        // Create the eeprom trace stream
        if (disasmURL != null) {
            try {
                String appendParm = (disasmAppend ? ";append=true" : "");
                disasmPS = new PrintStream(javax.microedition.io.Connector.openOutputStream(disasmURL+appendParm));
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }
        }
/*end[DEBUG.LOADER]*/

        String className = args[i++];
        Object old = args;
        args = new String[args.length - i];
        System.arraycopy(old, i, args, 0, args.length);
        old = null;

        try {
            for (Enumeration e = suites.elements(); e.hasMoreElements(); ) {
                String suiteURL = (String)e.nextElement();
                if (!loadSuite(suiteURL, disasmPS, flags, accountMemory)) {
                    return;
                }
            }
        } finally {
            if (disasmPS != null) {
                try { disasmPS.close(); } catch(Exception e) {}
            }
        }

/*if[DEBUG.METHODDEBUGTABLE]*/
        // Open the map file for writing
        if (mapURL != null) {
            PrintStream mapPS;
            try {
               mapPS = new PrintStream(javax.microedition.io.Connector.openOutputStream(mapURL));
            } catch (IOException ex) {
                System.err.println("Exception opening map stream: "+ex.getMessage());
                ex.printStackTrace();
                return;
            }

            MethodDebugTable.dump(mapPS);
            mapPS.close();
        }
/*end[DEBUG.METHODDEBUGTABLE]*/

        Klass klass = null;
        klass = Klass.forName(className);
        klass.main(args);
    }
}
