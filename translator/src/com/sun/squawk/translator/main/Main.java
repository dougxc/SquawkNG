package com.sun.squawk.translator.main;

import java.io.*;
import java.util.*;
import javax.microedition.io.Connector;
import com.sun.squawk.translator.suite.SuiteEmitter;
import com.sun.squawk.translator.suite.Statistics;
import com.sun.squawk.translator.util.Arrays;
import com.sun.squawk.translator.util.ComputationTimer;
import com.sun.squawk.translator.util.BufferedReader;
import com.sun.squawk.translator.Translator;


/**
 * This is the command line interface to the Translator. By default it creates
 * an XMLSuiteEmitter to output the created suite to System.out.
 */
public final class Main {

    private String libs              = "";
    private String classPath         = "";
    private int flags                = Translator.NAMES_FQN |
                                       Translator.NAMES_PARENT |
                                       Translator.NAMES_SRC |
                                       Translator.PRUNE_SUITE |
                                       Translator.LINKAGEERRORS_EXIT;
    private String match             = null;
    private ComputationTimer timer   = null;
    private String suite             = null;
    private boolean forNameRequest   = true;

    private SuiteEmitter emitter     = null;
    private OutputStream suiteOut    = null;
    private PrintStream statsOut     = null;

    private String[] classesToVerify = null;

    private boolean verifyingOnly() {
        return (flags & Translator.VERIFY_ONLY) != 0;
    }

    /**
     * Print the usage message.
     * @param errMsg An optional error message.
     */
    static void usage(String errMsg) {
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: translate [-options] @driver");
        out.println("          (to translate from a suite driver file)");
        out.println("   or  translate [-options] class");
        out.println("          (to translate a suite for a single class)");
        out.println("   or  translate -v [-options] class... | @classlistfile");
        out.println("          (to verify one or more classes)");
        out.println("where options include:");
        out.println("    -libs <file>[:<file>]* the ordered dependencies of the suite being");
        out.println("                        translated. These are either suite files (with");
        out.println("                        the suffix '.xml' or '.bin') or JAR files (with");
        out.println("                        the suffix '.jar' or '.zip'). If no libs are");
        out.println("                        specified then this is the system suite.");
        out.println("    -cp <path>          the classpath where the classes can be found");
        out.println("                        (as well as their sources for -traceir0 or -traceir1)");
        out.println("    -v                  only verify the suite classes and don't output anything");
        out.println("    -o|a <file>         output/append to file (required if -v not specified)");
        out.println("    -format <fmt>       specifies format of suite output. The legal values are:");
        out.println("                            olg:   Generate GeneratedObjectLayout.java");
        out.println("                            xml:   Output XML (default)");
        out.println("                            bin:   Output Suite File Format binary");
        out.println("                            bin_d: Output textual representation of binary");
        out.println("    -noprune            do not prune the proxy classes in the suite");
        out.println("    -traceloading       trace class loading");
        out.println("    -traceclassinfo     trace loading of class meta-info (i.e. implemented");
        out.println("                        interfaces, field meta-info & method meta-info)");
        out.println("    -traceraw           trace low-level loading");
        out.println("    -tracebytecodes     trace bytecode loading (includes verification trace)");
        out.println("    -tracelocals        extra tracing info for local variable usage");
        out.println("    -traceir0           dump initial IR graph once it is built");
        out.println("    -traceir1           dump transformed IR graph");
        out.println("    -traceip            suffix instruction trace lines with IP and source lines");
        out.println("    -tracelivenessdfa   trace liveness data flow analysis");
        out.println("    -tracepruning       trace mark phase of suite pruning");
        out.println("    -tracedeadcode      trace suite classes and members that are dead");
        out.println("    -linkageerrors:<opt>[,<opt>]* specify how to deal with LinkageErrors caused");
        out.println("                        by class loading or verification:");
        out.println("                             trace: print stack trace for the errors");
        out.println("                             pass:  translate the class such that it will");
        out.println("                                    re-throw the appropriate exception when");
        out.println("                                    first accessed by the Squawk VM. Without");
        out.println("                                    this option, the translator will exit on");
        out.println("                                    the first LinkageError encountered.");
        out.println("    -nominimalvm        do not impose Minimal VM limits (useful for TCK loading)");
        out.println("    -nostackconstraints  stack usage has no constraints");
        out.println("    -nogcstackconstraints only force stack to be empty at basic block boundaries");
        out.println("    -ignorefinal        ignore the suite final flag in the driver file");
        out.println("    -stats[:<opt>[,<opt>]*] show statistics after translation where <opt> is:");
/*if[FLOATS]*/
        out.println("                             fd: include statistics for methods");
        out.println("                                 using floats/doubles");
/*end[FLOATS]*/
        out.println("                             l: include statistics for methods use longs");
        out.println("                             o=<file>: output to <file>");
        out.println("                                (default = stdout)");
        out.println("    -matching <string>  filter trace with simple string filter");
        out.println("    -tracemethod <string> enable all the trace flags from '-tracebytecodes'");
        out.println("                        to '-tracelivenessdfa' for the matching method");
        out.println("    -O[:<opt>[,<opt>]*] perform optimizations. If no extra options are given,");
        out.println("                        then all optimizations are performed otherwise only");
        out.println("                        those specified by the following options are performed:");
        out.println("                             localsinit:  remove initializations of local");
        out.println("                                 variables to their default value");
        out.println("                             localsalloc: optimize local variable re-use");
        out.println("                             thisfieldaccess: use 'this_getfield' and");
        out.println("                                'this_putfield' instructions to optimize");
        out.println("                                 accessing fields of the receiver in virtual");
        out.println("                                 methods");
        out.println("    -names:<opt>[,<opt>]* controls formatting of class and member names in");
        out.println("                        traces and comments where <opt> is:");
        out.println("                             [no]fqn: [don't] use fully qualified class names");
        out.println("                                 i.e. include package names");
        out.println("                             [no]src: [don't] use source-like syntax. e.g.:");
        out.println("                                 \"java.lang.Object\" vs \"Ljava/lang/Object;\"");
        out.println("                             [no]parent: [don't] include the class name for");
        out.println("                                fields and methods");
        out.println("                        (default = \"fqn,src,parent\")");
        out.println("    -comments[:<opt>[,<opt>]*] augment XML with comments where <opt> is:");
        out.println("                             stack: show the stack state before each instruction");
        out.println("    -timer              time the phases of the translator");
        out.println("    -[no]mdbg           [don't] emit the MethodDebug attribute (default is '-mdbg')");
        out.println("    -help               show this help message and exit");
        out.println();
        out.println("Note: 'driver' and 'class' arguments should be specified");
        out.println("      in '.' form (e.g. java.lang.Object)");
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
     * Create an instance of a SuiteEmitter based on the output format requested.
     * @param outURL
     * @param flags
     * @param fmtArg
     * @return
     * @throws IOException
     */
    private SuiteEmitter createSuiteEmitter(String outURL, int flags, String fmtArg) throws IOException {

        OutputStream out = javax.microedition.io.Connector.openOutputStream(outURL);

        // Create the appropriate emitter
        Hashtable properties = null;
        String emitterClassName = "com.sun.squawk.translator.suite.impl.BinarySuiteEmitter";
        if (fmtArg.equals("xml")) {
            emitterClassName = "com.sun.squawk.translator.suite.impl.XMLSuiteEmitter";
        } else if (fmtArg.equals("olg")) {
            emitterClassName = "com.sun.squawk.translator.suite.impl.GeneratedObjectLayoutEmitter";
        } else if (fmtArg.equals("bin_d")) {
            properties = new Hashtable();
            properties.put("debug", "true");
        }

        try {
            Class emitterClass = Class.forName(emitterClassName);
            SuiteEmitter emitter = (SuiteEmitter)emitterClass.newInstance();
            emitter.init(out, flags, properties);
            return emitter;
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException("SuiteEmitter implementation class not found: "+emitterClassName);
        } catch (InstantiationException ie) {
            throw new RuntimeException("Error creating SuiteEmitter: "+ie.getMessage());
        } catch (IllegalAccessException iae) {
            throw new RuntimeException("Error creating SuiteEmitter: "+iae.getMessage());
        }
    }

    private boolean parseArguments(String[] args) {
        boolean showStats     = false;
        String outfile        = null;
        String statsFile      = null;
        String format         = "xml";
        boolean append        = false;
        int emitterFlags      = 0;
/*if[DEBUG.METHODDEBUGTABLE]*/
        emitterFlags          = SuiteEmitter.EMIT_METHOD_DEBUG;
/*end[DEBUG.METHODDEBUGTABLE]*/

        int i = 0;
        for ( ; i < args.length ; i++) {
            String arg = args[i];
            if (arg.charAt(0) != '-') {
                break;
            }
            if (arg.equals("-libs")) {
                libs = getOptArg(args, ++i, "-libs");
            } else if (arg.equals("-classpath") || arg.equals("-cp")) {
                classPath = getOptArg(args, ++i, "-cp");
            } else if (arg.equals("-o")) {
                outfile = getOptArg(args, ++i, "-o");
                append = false;
            } else if (arg.equals("-a")) {
                outfile = getOptArg(args, ++i, "-a");
                append = true;
            } else if (arg.equals("-matching")) {
                match = getOptArg(args, ++i, "-matching");
            } else if (arg.equals("-noprune")) {
                flags &= ~Translator.PRUNE_SUITE;
            } else if (arg.equals("-tracemethod")) {
                match = getOptArg(args, ++i, "-tracemethod");
                flags |= Translator.TRACE_BYTECODES;
                flags |= Translator.TRACE_IR0;
                flags |= Translator.TRACE_IR1;
                flags |= Translator.TRACE_IP;
                flags |= Translator.TRACE_LIVENESSDFA;
                flags |= Translator.TRACE_LOCALS;
            } else if (arg.equals("-traceloading")) {
                flags |= Translator.TRACE_LOADING;
            } else if (arg.equals("-traceclassinfo")) {
                flags |= Translator.TRACE_CLASSINFO;
            } else if (arg.equals("-traceraw")) {
                flags |= Translator.TRACE_RAW;
            } else if (arg.equals("-tracebytecodes")) {
                flags |= Translator.TRACE_BYTECODES;
            } else if (arg.equals("-traceir0")) {
                flags |= Translator.TRACE_IR0;
            } else if (arg.equals("-traceir1")) {
                flags |= Translator.TRACE_IR1;
            } else if (arg.equals("-tracelocals")) {
                flags |= Translator.TRACE_LOCALS;
            } else if (arg.equals("-traceip")) {
                flags |= Translator.TRACE_IP;
            } else if (arg.equals("-tracelivenessdfa")) {
                flags |= Translator.TRACE_LIVENESSDFA;
            } else if (arg.equals("-tracepruning")) {
                flags |= Translator.TRACE_PRUNING;
            } else if (arg.equals("-tracedeadcode")) {
                flags |= Translator.TRACE_DEAD_CODE;
            } else if (arg.equals("-nostackconstraints")) {
                flags |= Translator.NOSTACKCONSTRAINT;
                flags |= Translator.NOGCSTACKCONSTRAINT;
            } else if (arg.equals("-nogcstackconstraints")) {
                flags |= Translator.NOGCSTACKCONSTRAINT;
            } else if (arg.equals("-nominimalvm")) {
                flags |= Translator.NOMINIMALVM;
            } else if (arg.equals("-ignorefinal")) {
                flags |= Translator.IGNOREFINAL;
            } else if (arg.equals("-v")) {
                flags |= Translator.VERIFY_ONLY;
            } else if (arg.startsWith("-stats")) {
                showStats = true;
                flags |= Translator.STATS_ON;
                if (arg.startsWith("-stats:")) {
                    StringTokenizer st = new StringTokenizer(arg.substring("-stats:".length()), ",");
                    while (st.hasMoreTokens()) {
                        String statsArg = st.nextToken();
/*if[FLOATS]*/
                        if (statsArg.equals("fd")) {
                            flags |= Translator.STATS_FLOATDOUBLE;
                        }
                        else
/*end[FLOATS]*/
                        if (statsArg.equals("l")) {
                            flags |= Translator.STATS_LONG;
                        }
                        else if (statsArg.startsWith("o=")) {
                            statsFile = statsArg.substring(2);
                        }
                        else {
                            usage("Bad option for -stats: "+statsArg);
                            return false;
                        }
                    }
                }
            } else if (arg.startsWith("-O")) {
                if (arg.startsWith("-O:")) {
                    StringTokenizer st = new StringTokenizer(arg.substring("-O:".length()), ",");
                    while (st.hasMoreTokens()) {
                        String optArg = st.nextToken();
                        if (optArg.equals("localsinit")) {
                            flags |= Translator.OPT_LOCALSINIT;
                        }
                        else if (optArg.equals("localsalloc")) {
                            flags |= Translator.OPT_LOCALSALLOC;
                        }
                        else if (optArg.equals("thisfieldaccess")) {
                            flags |= Translator.OPT_THISFIELDACCESS;
                        }
                        else {
                            usage("Bad option for -O: "+optArg);
                            return false;
                        }
                    }
                }
                else {
                    flags |= Translator.OPT_ALL;
                }
            } else if (arg.equals("-format")) {
                format = getOptArg(args, ++i, "-format");
            } else if (arg.startsWith("-comments")) {
                emitterFlags |= SuiteEmitter.EMIT_COMMENTS;
                if (arg.startsWith("-comments:")) {
                    String commentsArg = arg.substring("-comments:".length());
                    StringTokenizer st = new StringTokenizer(commentsArg, ",");
                    while (st.hasMoreTokens()) {
                        String commentArg = st.nextToken();
                        if (commentArg.equals("stack")) {
                            emitterFlags |= SuiteEmitter.EMIT_STACK_COMMENTS;
                        }
                        else {
                            usage("Bad option for -comments: "+commentArg);
                            return false;
                        }
                    }
                }
            } else if (arg.startsWith("-names:")) {
                StringTokenizer st = new StringTokenizer(arg.substring("-names:".length()), ",");
                while (st.hasMoreTokens()) {
                    String nameArg = st.nextToken();
                    if (nameArg.equals("fqn")) {
                        flags |= Translator.NAMES_FQN;
                    }
                    else if (nameArg.equals("src")) {
                        flags |= Translator.NAMES_SRC;
                    }
                    else if (nameArg.equals("parent")) {
                        flags |= Translator.NAMES_PARENT;
                    }
                    else if (nameArg.equals("nofqn")) {
                        flags &= ~Translator.NAMES_FQN;
                    }
                    else if (nameArg.equals("nosrc")) {
                        flags &= ~Translator.NAMES_SRC;
                    }
                    else if (nameArg.equals("noparent")) {
                        flags &= ~Translator.NAMES_PARENT;
                    }
                    else {
                        usage("Bad option for -names: "+nameArg);
                        return false;
                    }
                }
            } else if (arg.startsWith("-linkageerrors:")) {
                StringTokenizer st = new StringTokenizer(arg.substring("-linkageerrors:".length()), ",");
                while (st.hasMoreTokens()) {
                    String leArg = st.nextToken();
                    if (leArg.equals("trace")) {
                        flags |= Translator.LINKAGEERRORS_TRACE;
                    }
                    else if (leArg.equals("pass")) {
                        flags &= ~Translator.LINKAGEERRORS_EXIT;
                    }
                }
            } else if (arg.equals("-timer")) {
                timer = new ComputationTimer();
            } else if (arg.equals("-mdbg")) {
                emitterFlags |= SuiteEmitter.EMIT_METHOD_DEBUG;
            } else if (arg.equals("-nomdbg")) {
                emitterFlags &= ~SuiteEmitter.EMIT_METHOD_DEBUG;
            } else if (arg.startsWith("-h")) {
                usage(null);
                System.exit(0);
            } else {
                usage("Bad switch "+arg);
                return false;
            }
        }

        // Ensure that at least one suite is specified
        if (i >= args.length) {
            usage("Missing suite root class name.");
            return false;
        }

        if (verifyingOnly()) {
            Vector classes = new Vector();
            while (i < args.length) {
                String arg = args[i];
                if (arg.charAt(0) == '@') {
                    try {
                        String file = arg.substring(1);
                        InputStream is = Connector.openInputStream("file://"+file);
                        (new BufferedReader(new InputStreamReader(is))).readLines(classes);
                        is.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                        return false;
                    }
                } else {
                    classes.addElement(arg);
                }
                i++;
            }
            classesToVerify = new String[classes.size()];
            classes.copyInto(classesToVerify);
        } else {

            String suiteSpec = args[i++];
            if (suiteSpec.charAt(0) == '@') {
                forNameRequest = false;
                suite = suiteSpec.substring(1);
            }
            else {
                suite = suiteSpec;
            }

            // Create the output stream
            if (outfile == null) {
                usage("The -o switch is required");
                return false;
            }

            try {
                emitter = createSuiteEmitter("file://" + outfile + ";append=" + append, emitterFlags, format);
            }
            catch (IOException ex) {
                ex.printStackTrace();
                return false;
            }
        }

        String osName = System.getProperty("os.name");
        if (osName != null && !osName.startsWith("Windows")) {
            classPath = classPath.replace(';', Translator.pathSeparatorChar);
            classPath = classPath.replace('\\', Translator.separatorChar);
            libs      = libs.replace('\\', Translator.separatorChar);
        }
        else {
            classPath = classPath.replace(':', Translator.pathSeparatorChar);
            classPath = classPath.replace('/', Translator.separatorChar);
            libs      = libs.replace('/', Translator.separatorChar);
        }

        if (showStats) {
            statsOut = System.out;
            if (statsFile != null) {
                try {
                    statsOut = new PrintStream(javax.microedition.io.Connector.openOutputStream("file://"+statsFile));
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
        }

        return true;
    }



    private boolean run() throws Exception {

        if (verifyingOnly()) {
            Translator translator = new Translator(libs, classPath, flags, match, timer);
            for (int i = 0; i != classesToVerify.length; ++i) {
                String klass = classesToVerify[i];
                translator.verify(klass);
            }
            return true;
        }

        try {
            Translator.Result result = Translator.translate(libs, classPath, flags, match, timer, suite, emitter, forNameRequest);
            if (statsOut != null) {
                result.statistics.print(statsOut);
                statsOut.close();
            }
        } finally {
            System.out.flush();
            // Close file
            try {
                emitter.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * Parse command line arguments, create an instance of Translator and
     * run it.
     * @param args command line args
     * @param emitter The SuiteEmitter to be used for serializing the suite.
     */
    public static void main(String[] args) throws Exception {
        final Main instance = new Main();
        if (!instance.parseArguments(args)) {
            System.exit(1);
        }

        if (instance.timer != null) {
            instance.timer.time("other", new ComputationTimer.ComputationException() {
                public Object run() throws Exception {
                    if (!instance.run()) {
                        System.exit(1);
                    }
                    return null;
                }
            });
            System.out.println("column 1: Time spent in computation excluding nested computations");
            System.out.println("column 2: Time spent in computation as % of total time");
            System.out.println("column 3: Time spent in computation including nested computations");
            System.out.println("column 4: Computation identifier");
            Enumeration keys   = instance.timer.getComputations();
            Enumeration ftimes = instance.timer.getFlatTimes();
            Enumeration ttimes = instance.timer.getTotalTimes();
            long total = ((Long)instance.timer.getTotalTime("other")).longValue();
            while (keys.hasMoreElements()) {
                String id = (String)keys.nextElement();
                long ftime = ((Long)ftimes.nextElement()).longValue();
                long ttime = ((Long)ttimes.nextElement()).longValue();
                if (!id.equals("other")) {
                    System.out.println(ftime+"ms\t"+((ftime*100)/(total))+"%\t"+ttime+"ms\t"+id);
                }
            }
            System.out.println("total time: "+total+"ms");
        } else {
            if (!instance.run()) {
                System.exit(1);
            }
        }


//        com.sun.squawk.translator.loader.ExecutionFrame.dumpOopmaps(System.err);
    }
}