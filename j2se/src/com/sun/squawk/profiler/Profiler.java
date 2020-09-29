package com.sun.squawk.profiler;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import com.sun.squawk.io.connections.*;
import com.sun.squawk.vm.Mnemonics;

/**
 * Show a tree based, foldable representation of a VM trace. A trace may include
 * trace lines and non-trace lines. Trace lines have the following format:
 *
 *   "*MPROF*" method_id
 */

public class Profiler {

    /**
     * Command line entrance point.
     */
    public static void main(String[] args) throws IOException {
        String traceFile   = null;
        String mapFile     = null;

        int i = 0;
        for (; i < args.length ; i++) {
            if (args[i].charAt(0) != '-') {
                break;
            }
            String arg = args[i];
            if (arg.equals("-map")) {
                mapFile = args[++i];
            } else {
                usage("Bad switch: "+arg);
                return;
            }
        }

        if (i >= args.length) {
            usage("Missing tracefile");
            return;
        } else {
            traceFile = args[i];
        }

        InputStream is = null;
        MethodMap map = null;
        try {
            if (mapFile != null) {
                map = new MethodMap(new FileInputStream(mapFile), mapFile);
            } else {
                map = new MethodMap();
            }

            is = new FileInputStream(traceFile);
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }

        int opcodes[]  = new int[1000];
        int counters[] = new int[10000];
        int samples = 0;

        for (;;) {
            int mid = getMid(is);
            if (mid == -1) {
                break;
            }

            int opc = getOpcode(is);
            opcodes[opc]++;

            if (mid >= counters.length) {
                int[] newCounters = new int[counters.length * 2];
                System.arraycopy(counters, 0, newCounters, 0, counters.length);
                counters = newCounters;
            }
            counters[mid]++;
            samples++;
if (samples % 10000 == 0) {
 System.out.println(""+samples);
}
        }

        System.out.println("Samples = "+samples);

        SortedMap smap = new TreeMap();

        smap.put(new Integer(counters[0]), "*Unknown*");
        for (i = 1 ; i < counters.length ; i++) {
            if (counters[i] != 0) {
                MethodMapEntry entry = map.lookup(i-1);
                String sig = entry.signature;
                int space = sig.indexOf(' ');
                sig = (space == -1) ? sig : sig.substring(space+1);
                if (sig.length() > 60) {
                    sig = sig.substring(0, 60);
                }
                smap.put(new Integer(counters[i]), sig);
            }
        }

        printHistogram(smap, System.out);


        smap = new TreeMap();

        for (i = 0 ; i < opcodes.length ; i++) {
            if (opcodes[i] != 0) {
                if (i == 999) {
                    smap.put(new Integer(opcodes[i]), "*Unknown*");
                } else {
                    smap.put(new Integer(opcodes[i]), Mnemonics.OPCODES[i]);
                }
            }
        }

        printHistogram(smap, System.out);

    }


    private static void printHistogram(SortedMap histogram, PrintStream out) {
        for (Iterator entries = histogram.entrySet().iterator(); entries.hasNext();) {
            Map.Entry entry = (Map.Entry)entries.next();
            String name = (String)entry.getValue();
            out.print(""+name);
            for (int i = name.length(); i < 70; ++i) {
                out.print(' ');
            }
            out.println(entry.getKey());
        }
    }


    private static int getMid(InputStream is) throws IOException {
        for (;;) {
            if (read(is) == '*' &&
                read(is) == 'M' &&
                read(is) == 'P' &&
                read(is) == 'R' &&
                read(is) == 'O' &&
                read(is) == 'F' &&
                read(is) == '*' &&
                read(is) == ' ') {
                    int count = 0;
                    int ch = read(is);
                    if (ch == '?') {
                        return 0;
                    }
                    for (;;) {
                        if (ch == ':') {
//System.out.println("*MPROF* "+count);
                            return count+1;
                        }
                        ch = ch - '0';
                        count = count * 10 + ch;
                        ch = read(is);
                    }
                }

            int ch = read(is);
            if (ch == -1) {
                return -1;
            }
            while (ch != -1 && ch != '\n') {
                ch = read(is);
            }
        }
    }


    private static int getOpcode(InputStream is) throws IOException {
        int count = 0;
        int ch = read(is);
        if (ch < ' ') {
            return 999;
        }
        for (;;) {
            if (ch < ' ') {
//System.out.println("*MPROF* "+count);
                return count;
            }
            ch = ch - '0';
            count = count * 10 + ch;
            ch = read(is);
        }
    }


    private static int read(InputStream is) throws IOException {
        int ch = is.read();
//System.out.println("ch="+(char)ch);
        return ch;
    }





    /**
     * Print usage message.
     * @param errMsg An error message or null.
     */
    static void usage(String errMsg) {
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: Profiler [-options] tracefile ");
        out.println("where options include:");
        out.println("    -map <file>         map file containing method meta info");
        out.println();
    }

}




/*---------------------------------------------------------------------------*\
 *                          Method map file classes                          *
\*---------------------------------------------------------------------------*/

class MethodMap extends BaseFunctions {

    MethodMapEntry[] methods;
    MethodMapEntry nullEntry;

    MethodMap() {
        nullEntry = new MethodMapEntry();
    }

    MethodMap(InputStream in, String mapFileName) {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        nullEntry = new MethodMapEntry();
        try {
            String line = br.readLine();
            Vector entries = new Vector();
            while (line != null) {
                // Read map
                int id = Integer.parseInt(line);
                assume(id == entries.size());
                entries.addElement(new MethodMapEntry(id, br));
                line = br.readLine();
            }
            methods = new MethodMapEntry[entries.size()];
            entries.copyInto(methods);
        } catch (IOException ex) {
            ex.printStackTrace();
            fatalError("Error while parsing map file: "+mapFileName);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            fatalError("Error while parsing map file: "+mapFileName);
        }
    }

    MethodMapEntry lookup(int methodId) {
        if (methods == null || methodId == -1) {
            return nullEntry;
        }
        assume(methods.length > methodId, "methodId="+methodId+" methods.length="+methods.length);
        return methods[methodId];
    }
}

class MethodMapEntry extends BaseFunctions {
    int       id;
    String    file;
    String    signature;
    private int[]     ipTable;
    String    name;
    int       argCount;

    MethodMapEntry() {
    }

    MethodMapEntry(int id, BufferedReader br) throws IOException {
        this.id        = id;
        this.file      = br.readLine();
        this.signature = br.readLine();

        // Parse the signature to extract the (unqualified) name of the
        // method and its parameter count. Method signatures are assumed
        // to be fully qualified.
        int bracket = signature.indexOf('(');
        int start = signature.lastIndexOf('.', bracket);
        name = signature.substring(start+1, bracket);
        while (signature.charAt(++bracket) != ')') {
            if (signature.charAt(bracket) == ',') {
                argCount++;
            } else if (signature.charAt(bracket) != ' ' && argCount == 0) {
                argCount = 1;
            }
        }

        // Build a plausible path out of the signature if file is empty.
        if (file.length() == 0) {
            int space = signature.lastIndexOf(' ', bracket);
            file = signature.substring(space + 1, bracket);

            // Remove method name
            file = file.substring(0, file.lastIndexOf('.'));

            // Convert fully qualified class name to path
            file = file.replace('.', '/') + ".java";
        }

        // Parse the IP table
        String line = br.readLine();
        if (line.length() != 0) {
            StringTokenizer st = new StringTokenizer(line);
            int count = st.countTokens();
            assume((count % 2) == 0);
            int[] tokens = new int[count];
            for (int i = 0; i != count; i++) {
                tokens[i] = Integer.parseInt(st.nextToken());
            }

            int highestIp = tokens[tokens.length - 2];
            ipTable = new int[highestIp + 1];
            for (int i = 0; i != count;) {
                int ip = tokens[i++];
                int src = tokens[i++];
                ipTable[ip] = src;
            }
        }
    }

    int getSourceLine(int ip) {
        if (ipTable != null) {
            assume(ip < ipTable.length, "ip="+ip+" ipTable.length="+ipTable.length);
            return ipTable[ip];
        }
        return 0;
    }
}

abstract class BaseFunctions {
    static void assume(boolean c) {
        if (!c) {
            fatalError("assume failure");
        }
    }
    static void assume(boolean c, String msg) {
        if (!c) {
            fatalError("assume failure: "+msg);
        }
    }

    static void fatalError(String msg) {
        throw new RuntimeException(msg);
    }
}