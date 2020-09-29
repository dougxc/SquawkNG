package java.lang;

import java.io.*;
import java.util.*;
import com.sun.squawk.loader.*;
import com.sun.squawk.suite.*;
import com.sun.squawk.vm.*;
import com.sun.squawk.translator.suite.VMAccessedAttribute;
import com.sun.squawk.util.IntHashtable;


public final class Romizer {

    static final String DEFAULT_TRACE_URL = "file://romize.trace";

/* ------------------------------------------------------------------------ *\
 *                           Instance fields                                *
\* ------------------------------------------------------------------------ */

    /**
     * The start address of the ROM.
     */
    int rom = -1;

    /**
     * The start address of the EEPROM.
     */
    int eeprom = -1;

    /**
     * The start address of the RAM.
     */
    int ram = -1;

    /**
     * The size of the ROM (in bytes).
     */
    int romSize = parseQuantityArg(/*VAL*/"128K"/*ROMIZER.ROM_SIZE*/, "-rom");

    /**
     * The size of the EEPROM (in bytes).
     */
    int eepromSize = parseQuantityArg(/*VAL*/"64K"/*ROMIZER.EEPROM_SIZE*/, "-eeprom");

    /**
     * The size of the RAM (in bytes).
     */
    int ramSize = parseQuantityArg(/*VAL*/"16K"/*ROMIZER.RAM_SIZE*/, "-ram");

    /**
     * The name of the image file to produce.
     */
    String image;

    /**
     * Execution flags.
     */
    int flags;

    /**
     * The URL for VM tracing.
     */
    String traceURL;

    /**
     * Specifies whether or not the ROM should be configured to be growable.
     */
    boolean growRom;

    /**
     * Output format specifier.
     */
    String format = "bin";

    /**
     * Loader execution flags.
     */
    int ldFlags;

    /**
     * Enable logging.
     */
    boolean showLog;

/*if[DEBUG.METHODDEBUGTABLE]*/
    /**
     * Name of file to which method debug info should be written.
     */
    String mapFileName;
/*end[DEBUG.METHODDEBUGTABLE]*/

/*if[DEBUG.LOADER]*/
    String disasmFile;
    boolean disasmAppend;
/*end[DEBUG.LOADER]*/

    /**
     * The suite input files.
     */
    String[] inputFiles;

    /**
     * The ObjectLayoutGenerator instance used to process the
     * classes with a VMAccessed attribute.
     */
    GeneratedObjectLayoutVerifier golv = new GeneratedObjectLayoutVerifier();

/* ------------------------------------------------------------------------ *\
 *                                  main                                    *
\* ------------------------------------------------------------------------ */


    /**
     * Print the usage message.
     *
     * @param errMsg An optional error message.
     */

    static void usage(String errMsg) {
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: romize [-options] suite_files...");
        out.println("where options include:");
        out.println("    -format <fmt>       specifies format of suite input. The legal values are:");
/*if[DEBUG.LOADER]*/
        out.println("                            xml:   XML");
/*end[DEBUG.LOADER]*/
        out.println("                            bin:   Suite File Format binary (default)");
        out.println("    -image <image>      write romized image to <file>");
        out.println("    -rom <addr>         set ROM start address (default=0)");
        out.println("    -rom_sz <size>      set ROM memory segment size (default=128K)");
        out.println("    -eeprom <addr>      set EEPROM start address (default=(rom+rom_sz))");
        out.println("    -eeprom_sz <size>   set EEPROM memory segment size (default=64K)");
        out.println("    -ram <addr>         set RAM start address (default=(eeprom+eeprom_sz))");
        out.println("    -ram_sz <size>      set RAM memory segment size (default=8K)");
        out.println("    -[little|big]endian sets the endianess of the VM to little. The default is");
        out.println("                        determined from the '*.cpu.endian' system property");
        out.println("                        if it exists otherwise it is set to 'big'");
        out.println("    -tck                modify behaviour to process TCK as follows:");
        out.println("                          i. Don't discard package-private symbolic info");
        out.println("    -traceURL <url>     output trace to URL (default=file://romize.trace)");
        out.println("    -tracealloc         trace allocation");
        out.println("    -tracegc            trace test GC of ROM");
        out.println("    -tracegcverbose     verbose trace of test GC of ROM");
        out.println("    -traceheap          do a dump of the image after it is built");
        out.println("    -growrom            allow rom expansion during image building");
        out.println("    -gcrom              configure ROM as a garbage collectable memory.");
        out.println("                        This should not be used for a production image.");
        out.println("    -stats              show class statistics after romizing");
        out.println("    -log                show suite contents log after romizing");
/*if[DEBUG.LOADER]*/
        out.println("    -traceloading       trace suite loading");
        out.println("    -traceverifier      trace verification");
        out.println("    -suitestats         show the suite stats");
        out.println("    -o|a <file>         output/append disassembly to file (default = no trace)");
/*end[DEBUG.LOADER]*/
/*if[DEBUG.METHODDEBUGTABLE]*/
        out.println("    -m   <file>         output a method map file (default = /dev/null)");
/*end[DEBUG.METHODDEBUGTABLE]*/
        out.println("    -help               show this usage message and exit");
        out.println();
    }

    /**
     * Look for any system property that ends with ".cpu.endian" to try and
     * automatically detect the endianess of the underlying platform.
     *
     * @return
     */
    private static boolean isDefaultEndianessBig() {
        Properties properties = System.getProperties();
        Enumeration names = properties.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String)names.nextElement();
            if (name.endsWith(".cpu.endian")) {
                String value = properties.getProperty(name);
                boolean big = (value.toLowerCase().indexOf("big") != -1);
                return big;
            }
        }
        return true;
    }

    /**
     * Parse a option representing a quantity that may include an optional
     * muliplier suffix of 'K' or 'M'.
     *
     * @param arg
     * @param opt
     * @return
     */
    public static int parseQuantityArg(String arg, String opt) {
        int i;
        int val = 0;
        for (i = 0 ; i != arg.length(); i++) {
            char ch = arg.charAt(i);
            if (ch >= '0' && ch <= '9') {
                val = (val * 10) + (ch - '0');
            } else if (ch == 'K' || ch == 'k') {
                val *= 1024;
                break;
            } else if (ch == 'M' || ch == 'm') {
                val *= (1024*1024);
                break;
            } else {
                usage("Bad value for the " + opt + " option.");
                System.exit(1);
            }
        }
        return val;
    }

    /**
     * Get the argument to a command line option. If the argument is not provided,
     * then a usage message is printed and the system exits.
     * @param args The command line arguments.
     * @param index The index at which the option's argument is located
     * @param opt The name of the option.
     * @return the options argument.
     */
    public static String getOptArg(String[] args, int index, String opt) {
        if (index >= args.length) {
            usage("The " + opt + " option requires an argument.");
            System.exit(1);
        }
        return args[index];
    }

    /**
     * Create a new instance of the Romizer based on a set of command line args.
     *
     * @param args
     * @throws IOException
     */
    Romizer(String[] args) {
        boolean sizeGiven = false;

        // Set default endianess
        boolean endianessSpecified = false;
        if (isDefaultEndianessBig()) {
            flags |= ObjectMemoryBuilder.OPT_BIGENDIAN;
        }

        int i;
        for (i = 0; i != args.length; i++) {
            String arg = args[i];
            if (arg.charAt(0) == '-') {
                if (arg.equals("-format")) {
                    format = getOptArg(args, ++i, "-format");
                } else if (arg.equals("-image")) {
                    image = getOptArg(args, ++i, "-image");
                } else if (arg.equals("-rom")) {
                    rom = parseQuantityArg(getOptArg(args, ++i, "-rom"), "-rom");
                } else if (arg.equals("-rom_sz")) {
                    romSize = parseQuantityArg(getOptArg(args, ++i, "-rom_sz"), "-rom_sz");
                } else if (arg.equals("-eeprom")) {
                    eeprom = parseQuantityArg(getOptArg(args, ++i, "-eeprom"), "-eeprom");
                } else if (arg.equals("-eeprom_sz")) {
                    eepromSize = parseQuantityArg(getOptArg(args, ++i, "-eeprom_sz"), "-eeprom_sz");
                } else if (arg.equals("-ram")) {
                    ram = parseQuantityArg(getOptArg(args, ++i, "-ram"), "-ram");
                } else if (arg.equals("-ram_sz")) {
                    ramSize = parseQuantityArg(getOptArg(args, ++i, "-ram_sz"), "-ram_sz");
                } else if (arg.equals("-littleendian")) {
                    flags &= ~ObjectMemoryBuilder.OPT_BIGENDIAN;
                    endianessSpecified = true;
                } else if (arg.equals("-bigendian")) {
                    flags |= ObjectMemoryBuilder.OPT_BIGENDIAN;
                    endianessSpecified = true;
                } else if (arg.equals("-tck")) {
                    isTCK = true;
                } else if (arg.equals("-traceURL")) {
                    traceURL = getOptArg(args, ++i, "-traceURL");
                } else if (arg.equals("-tracealloc")) {
                    flags |= ObjectMemoryBuilder.OPT_TRACEALLOC;
                    if (traceURL == null) {
                        traceURL = DEFAULT_TRACE_URL;
                    }
                } else if (arg.equals("-tracegc")) {
                    flags |= ObjectMemoryBuilder.OPT_TRACEGC;
                    if (traceURL == null) {
                        traceURL = DEFAULT_TRACE_URL;
                    }
                } else if (arg.equals("-tracegcverbose")) {
                    flags |= ObjectMemoryBuilder.OPT_TRACEGC;
                    flags |= ObjectMemoryBuilder.OPT_TRACEGCVERBOSE;
                    if (traceURL == null) {
                        traceURL = DEFAULT_TRACE_URL;
                    }
                } else if (arg.equals("-traceheap")) {
                    flags |= ObjectMemoryBuilder.OPT_TRACEHEAP;
                    if (traceURL == null) {
                        traceURL = DEFAULT_TRACE_URL;
                    }
                } else if (arg.equals("-growrom")) {
                    growRom = true;
                } else if (arg.equals("-gcrom")) {
/*if[CHENEY.COLLECTOR]*/
                    flags |= ObjectMemoryBuilder.OPT_GCROM;
/*end[CHENEY.COLLECTOR]*/
/*if[LISP2.COLLECTOR]*/
                    System.err.println("-gcrom not supported with lisp2 collector");
/*end[LISP2.COLLECTOR]*/
                } else if (arg.equals("-stats")) {
                    flags |= ObjectMemoryBuilder.OPT_CLASSSTATS;
                } else if (arg.equals("-log")) {
                    showLog = true;
/*if[DEBUG.LOADER]*/
                } else if (arg.equals("-traceloading")) {
                    ldFlags |= SuiteLoader.TRACE_LOADING;
                } else if (arg.equals("-traceverifier")) {
                    ldFlags |= SuiteLoader.TRACE_VERIFIER;
                } else if (arg.equals("-suitestats")) {
                    ldFlags |= SuiteLoader.SUITE_STATS;
                } else if (arg.equals("-o")) {
                    disasmFile = getOptArg(args, ++i, "-o");
                    disasmAppend = false;
                } else if (arg.equals("-a")) {
                    disasmFile = getOptArg(args, ++i, "-a");
                    disasmAppend = true;
/*end[DEBUG.LOADER]*/
/*if[DEBUG.METHODDEBUGTABLE]*/
                } else if (arg.equals("-m")) {
                    mapFileName = getOptArg(args, ++i, "-m");
/*end[DEBUG.METHODDEBUGTABLE]*/
                } else if (arg.startsWith("-h")) {
                    usage(null);
                    return;
                } else {
                    // Skip option' parameter if it has one
                    if (!arg.startsWith("-trace")) {
                        arg += " " + getOptArg(args, ++i, arg);
                    }
                    System.err.println("Unknown/unsupported option ignored: "+arg);
                }
            } else {
                break;
            }
        }

        if (!endianessSpecified) {
            boolean big = ((flags & ObjectMemoryBuilder.OPT_BIGENDIAN) != 0);
            if (!big) {
                System.out.println("(building small endian image by default)");
            } else {
                System.out.println("(building big endian image by default)");
            }
        }

        if (i != args.length) {
            int length = args.length - i;
            inputFiles = new String[length];
            System.arraycopy(args, i, inputFiles, 0, length);
        }
        else {
            usage("Requires one or more suite input files");
            System.exit(1);
        }
    }

    /**
     * Entry point for romizer.
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Romizer instance = new Romizer(args);
        instance.run();
    }

    /**
     * Load the suites specified on the command line.
     *
     * @return true if there was not excep
     * @throws IOException
     *
     */
    private boolean loadSuites() throws IOException {

        // Create the disassembly stream
        OutputStream disasmPS = null;
/*if[DEBUG.LOADER]*/
        if (disasmFile != null) {
            String appendParm = (disasmAppend ? ";append=true" : "");
            disasmPS = new PrintStream(javax.microedition.io.Connector.openOutputStream("file://"+disasmFile+appendParm));
        }
/*end[DEBUG.LOADER]*/
        try {
            for (int i = 0; i != inputFiles.length; ++i) {
                String inputFile = inputFiles[i];
                InputStream in = javax.microedition.io.Connector.openInputStream("file://" + inputFile);
                SuiteLoader.run(createSuiteParser(in, format), disasmPS, i == inputFiles.length - 1, ldFlags);
                in.close();
            }
        } finally {
            if (disasmPS != null) {
                disasmPS.close();
            }
        }
        return true;
    }

    /**
     * Create the suite parser that will be used based on a specified format.
     *
     * @param in
     * @param format
     * @return the attribute parser that corresponds to the configured
     * suite parser.
     */
    private SuiteParser createSuiteParser(InputStream in, String format) {
/*if[DEBUG.LOADER]*/
        if (format.equals("xml")) {
            return new RomizingXMLSuiteParser(in, golv);
        }
        else
/*end[DEBUG.LOADER]*/
        if (format.equals("bin")) {
            return new RomizingBinarySuiteParser(in, golv);
        }
        else {
            usage("Bad option for -format: "+format);
            System.exit(1);
            return null;
        }
    }

    /**
     * Write out the contents of the MethodDebugTable to a file. This
     * corresponds to the MethodDebug attributes parsed from the suite(s) and
     * can be used by external tools to map VM trace lines back to source code
     * files and line positions.
     *
     * @throws IOException
     */
    private void writeMapFile() throws IOException {
/*if[DEBUG.METHODDEBUGTABLE]*/
        // Open the map file for writing
        if (mapFileName != null) {
            PrintStream mapPS = new PrintStream(javax.microedition.io.Connector.openOutputStream("file://"+mapFileName));
            MethodDebugTable.dump(mapPS);
            mapPS.close();
        }
/*end[DEBUG.METHODDEBUGTABLE]*/
    }

    /**
     * Write out the romized inmage.
     *
     * @throws IOException
     */
    private void writeImage() throws IOException {

        // Adjust the memory start addresses if none were specified on
        // the command line
        if (rom == -1) {
            rom = 0;
        }
        if (eeprom == -1) {
            eeprom = rom+romSize;
        }
        if (ram == -1) {
            ram = eeprom + eepromSize;
        }

        IntHashtable methodDebugTable = null;
/*if[DEBUG.METHODDEBUGTABLE]*/
        methodDebugTable = MethodDebugTable.table;
/*end[DEBUG.METHODDEBUGTABLE]*/

        Map suiteClassesMap = golv.getSuiteClasses();
        ObjectMemoryBuilder builder = new ObjectMemoryBuilder(rom,
            romSize,
            eeprom,
            eepromSize,
            ram,
            ramSize,
            flags,
            traceURL,
            suiteClassesMap);
        while (true) {
            try {
                builder.romize(SuiteManager.suites, methodDebugTable);
                break;
            } catch (OutOfMemoryError oome) {
                if (!oome.getMessage().equals("ROM")) {
                    throw oome;
                } else if (!growRom) {
                    throw new OutOfMemoryError("ROM exhausted during image building - try higher '-rom' value or '-growrom'");
                } else {
                    System.out.println("Growing ROM: "+romSize+" -> "+romSize*2);
                    eeprom += romSize;
                    ram += romSize;
                    romSize *= 2;
                    builder = new ObjectMemoryBuilder(rom,
                        romSize,
                        eeprom,
                        eepromSize,
                        ram,
                        ramSize,
                        flags,
                        traceURL,
                        suiteClassesMap);
                }
            }
        }

        // Build it once more the correct size

        int romUnused = romSize - builder.romUsed();

        romSize -= romUnused;
        eeprom -= romUnused;
        ram -= romUnused;

        System.out.println("ROM compressed to: "+romSize);

        flags &= ~ObjectMemoryBuilder.OPT_GCROM;
        builder = new ObjectMemoryBuilder(rom, romSize, eeprom, eepromSize, ram, ramSize, flags, traceURL, suiteClassesMap);
        int[] sizes = builder.romize(SuiteManager.suites, methodDebugTable);
        System.out.println("Suite sizes in image:");
        for (int j = 0; j != sizes.length-1; ++j) {
            String suiteName = SuiteManager.suites[j].name;
            System.out.print("  "+suiteName+": ");
            for (int k = suiteName.length(); k < 12; k++) {
                System.out.print(' ');
            }
            System.out.println(sizes[j]+" bytes");
        }
        if (methodDebugTable != null) {
            System.out.println("MethodDebugTable:    "+sizes[sizes.length-1]);
        }

        // Finally write out the image
        builder.ObjectMemory_writeImage(image);
    }

    /**
     * Run the romizer.
     */
    private void run() throws IOException {

        // Load the suites
        loadSuites();

        // Write out the map file
        writeMapFile();

        // Generate/validate the GeneratedObjectLayout.java source file.
        golv.verify();

        if (image != null) {

            // Write the romized image.
            writeImage();

            if (showLog) {
                for (Enumeration e = logs.elements(); e.hasMoreElements();) {
                    SuiteLogger log = (SuiteLogger)e.nextElement();
                    log.printLog(System.out);
                }
            }
        }

/*if[DEBUG.LOADER]*/
         if ((flags & ObjectMemoryBuilder.OPT_CLASSSTATS) != 0) {
             StringOfSymbols.printStats();
         }
/*end[DEBUG.LOADER]*/

    }

/* ------------------------------------------------------------------------ *\
 *                               Logging                                    *
 * ------------------------------------------------------------------------ */

    private static Vector logs = new Vector();
    private static SuiteLogger currentLog;

    static class SuiteLogger {

        final String name;
        SuiteLogger(String name) {
            this.name = name;
        }

        int totalOopMaps;
        int sharedOopMaps;
        void logOopMapCreation(boolean isShared) {
            totalOopMaps++;
            if (isShared) {
                sharedOopMaps++;
            }
        }


        Vector loggedClasses = new Vector();
        Vector loggedProxyClasses = new Vector();

        void logClass(SuiteClass sc, boolean isProxy) {
            String name     = sc.getName();
            if (!isProxy) {
                loggedClasses.addElement(name);
            } else {
                loggedProxyClasses.addElement(name);
            }
        }

        int loggedMethodsCount;
        int[] loggedInstructions = new int[256];
        int[] loggedLongInstructions = new int[256];
        int[] loggedFloatInstructions = new int[256];

        void logMethod() {
            loggedMethodsCount++;
        }
        void logInstruction(int opcode) {
            loggedInstructions[opcode]++;
        }
        void logLongInstruction(int opcode) {
            loggedLongInstructions[opcode]++;
        }
        void logFloatInstruction(int opcode) {
            loggedFloatInstructions[opcode]++;
        }

        static int sumIntArray(int[] array) {
            int sum = 0;
            for (int i = 0; i != array.length; i++) {
                sum += array[i];
            }
            return sum;
        }

        private static SortedMap sort(int[] counts, String[] mnemonics) {
            SortedMap sort = new TreeMap();
            for (int i = 0; i != mnemonics.length; i++) {
                int count = counts[i];
                if (count != 0) {
                    sort.put(new Integer(count), mnemonics[i]);
                }
            }
            return sort;
        }

        private static void printInstructionsHistogram(SortedMap histogram, PrintStream out) {
            for (Iterator entries = histogram.entrySet().iterator(); entries.hasNext();) {
                Map.Entry entry = (Map.Entry)entries.next();
                String opcode = (String)entry.getValue();
                out.print("    "+opcode);
                for (int i = opcode.length(); i != 20; ++i) {
                    out.print(' ');
                }
                out.println(entry.getKey());
            }
        }

        void printLog(PrintStream out) {
            int instTotal  = sumIntArray(loggedInstructions);
            int linstTotal = sumIntArray(loggedLongInstructions);
            int finstTotal = sumIntArray(loggedFloatInstructions);
            out.println("Suite \'"+name+"' log:");
            out.println("  Total oop maps:        "+totalOopMaps);
            out.println("  Shared oop maps:       "+sharedOopMaps);
            out.println("  Total classes:         "+(loggedClasses.size()+loggedProxyClasses.size()));
            out.println("  Total methods:         "+loggedMethodsCount);
            out.println("  Total instructions:    "+(instTotal+linstTotal+finstTotal));
            out.println("    Float instructions:  "+finstTotal);
            out.println("    Long instructions:   "+linstTotal);
            out.println();
            out.println("  Proxy classes:");
            for (Enumeration e = loggedProxyClasses.elements(); e.hasMoreElements();) {
                out.println("    "+e.nextElement());
            }
            out.println("  Classes:");
            for (Enumeration e = loggedClasses.elements(); e.hasMoreElements();) {
                out.println("    "+e.nextElement());
            }

            out.println("  Classes as class files:");
            for (Enumeration e = loggedClasses.elements(); e.hasMoreElements();) {
                String name = (String)e.nextElement();
                if (name.charAt(0) != '[') {
                    out.print(name.replace('.', '/') + ".class ");
                }
            }
            out.println();

            out.println();
            out.println("  Instructions histogram:  ");
            printInstructionsHistogram(sort(loggedInstructions,
                                            com.sun.squawk.vm.Mnemonics.OPCODES), out);
            if (finstTotal != 0) {
                out.println("  Float instructions histogram:  ");
                printInstructionsHistogram(sort(loggedFloatInstructions,
                    com.sun.squawk.vm.Mnemonics.FLOAT_OPCODES), out);
            }
            if (linstTotal != 0) {
                out.println("  Long instructions histogram:  ");
                printInstructionsHistogram(sort(loggedLongInstructions,
                    com.sun.squawk.vm.Mnemonics.LONG_OPCODES), out);
            }
        }
    }

/////////////////////////////////////////////////////////////////////////////////////////////////
////// NOTE: All the following methods must have stubs in j2me/src/java/lang/Romizer.java ///////
/////////////////////////////////////////////////////////////////////////////////////////////////

 /* ------------------------------------------------------------------------ *\
  *                                 Flags                                    *
  * ------------------------------------------------------------------------ */

    private static boolean isTCK;
    public static boolean isTCK() { return isTCK; }

/* ------------------------------------------------------------------------ *\
 *                               Logging                                    *
 * ------------------------------------------------------------------------ */

    public static void logSuite(String name) {
        logs.addElement(currentLog = new SuiteLogger(name));
    }

    public static void logOopMapCreation(boolean isShared)      { currentLog.logOopMapCreation(isShared); }
    public static void logClass(SuiteClass sc, boolean isProxy)  { currentLog.logClass(sc, isProxy); }
    public static void logMethod()                              { currentLog.logMethod(); }
    public static void logInstruction(int opcode)               { currentLog.logInstruction(opcode); }
    public static void logLongInstruction(int opcode)           { currentLog.logLongInstruction(opcode); }
    public static void logFloatInstruction(int opcode)          { currentLog.logFloatInstruction(opcode); }


    /*------------------------------------------------------------------------*
     *                     Native method number allocator                     *
     *------------------------------------------------------------------------*/

    private static int nextNativeMethodNumber;

    /**
     * Allocate a new system wide unique identifier for a native method. The
     * identifier 0 is reserved and is never returned.
     *
     * @return
     */
    public static int getNextNativeMethodNumber() {
        return ++nextNativeMethodNumber;
    }



 /*---------------------------------------------------------------------------*
  *                     Assume methods                                        *
  *---------------------------------------------------------------------------*/

    /**
     *
     * @param b
     */
    public static void assume(boolean b) {
        if (!b) {
            throw new RuntimeException("Assume failure");
        }
    }

    /**
     *
     * @param b
     * @param messge
     */
    public static void assume(boolean b, String messge) {
        if (!b) {
            throw new RuntimeException("Assume failure:" + messge);
        }
    }
}