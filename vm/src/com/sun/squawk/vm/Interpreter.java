//J2C:squawk.c **DO NOT DELETE THIS LINE**

//IFC//#include <stdio.h>
//IFC//#include <stdlib.h>
//IFC//#ifndef MACOSX
//IFC//#include <malloc.h>
//IFC//#endif /* MACOSX */
//IFC//#include <string.h>
//IFC//#include <sys/types.h>
//IFC//#include <sys/stat.h>
//IFC//#include <fcntl.h>
//IFC//#include <errno.h>
//IFC//#include <string.h>
//IFC//#include <signal.h>
//IFC//#include <jni.h>
/*if[FLOATS]*/
//IFC//#include "math.h"
/*end[FLOATS]*/

//IFC//#define public /**/
//IFC//#define private /**/
//IFC//#define protected /**/
//IFC//#define abstract /**/
//IFC//#define true 1
//IFC//#define false 0
//IFC//#define boolean int
//IFC//#define null 0
//IFC//#define final /**/
//IFC//#define byte signed char
//IFC//#define jlong  long long
//IFC//#define ujlong unsigned long long
//IFC//#define INLINE /**/

//IFC//#ifdef PRODUCTION
//IFC//#define REALMEMORY
//IFC//#define IMAGERELOCATION
//IFC//#endif

//IFC//#ifdef _MSC_VER
//IFC//#include <io.h>
//IFC//#undef  jlong
//IFC//#undef  ujlong
//IFC//#undef  INLINE
//IFC//#define INLINE __inline
//IFC//#define jlong  __int64
//IFC//#define ujlong unsigned __int64
//IFC//#endif /* _MSC_VER */

//IFC//#ifdef __GNUC__
//IFC//#include <unistd.h>
//IFC//#undef  jlong
//IFC//#undef  ujlong
//IFC//#undef  INLINE
//IFC//#define INLINE __inline__
//IFC//#define jlong  int64_t
//IFC//#ifdef sun
//IFC//#define ujlong uint64_t
//IFC//#else
//IFC//#define ujlong u_int64_t
//IFC//#endif
//IFC//#endif /* __GNUC__ */

//IFC//#define String char*
//IFC//#define Object void*

//IFC//#include "opc.c"        /* OPC:                  the opcode constant definitions */
//IFC//#include "mnemonic.c"   /* Mnemonics:            symbolic representations of the opcodes */
//IFC//#include "consts.c"     /* SquawkConstants:      */
//IFC//#include "cno.c"        /* CNO:                  */
//IFC//#include "options.c"    /* InterpreterOptions:   */
//IFC//#include "platform.c"   /* PlatformAbstraction:  */
//IFC//#include "memory.c"     /* Memory:               */
//IFC//#include "pmemory.c"    /* PersistentMemory:     */
//IFC//#include "bitvec.c"     /* BitVector:            */
//IFC//#include "barrier.c"    /* WriteBarrier:         */
//IFC//#include "segments.c"   /* Segments:             */
//IFC//#include "gol.c"        /* GeneratedObjectLayout:*/
//IFC//#include "layout.c"     /* ObjectLayout:         */
//IFC//#include "header.c"     /* HeaderLayout:         */
//IFC//#include "string.c"     /* StringAccess:         */
//IFC//#include "nassist.c"    /* NativeBuffers:        */
//IFC//#include "segacc.c"     /* SegmentAccess:        */
//IFC//#include "segreloc.c"   /* SegmentRelocation:    */
//IFC//#include "htrace.c"     /* HeapTracing:          */
//IFC//#include "markstk.c"    /* MarkStack:            */
//IFC//#include "eepromgc.c"   /* PersistentCollector:  */
//IFC//#include "curseg.c"     /* CurrentSegment:       */
//IFC//#include "malloc.c"     /* MemoryAllocator:      */
//IFC//#include "objassoc.c"   /* ObjectAssociation:    */
//IFC//#include "cheney.c"     /* CheneyCollector:      */
//IFC//#include "lisp2.c"      /* Lisp2Collector:       */
//IFC//#include "object.c"     /* ObjectMemory:         */
//IFC//#include "interp.c"     /* Interpret:            the interpreter loop */

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/import  java.io.*;
/*if[VM.GCSPY]*/
/*IFJ*/import  gcspy.interpreter.*;
/*IFJ*/import  gcspy.interpreter.server.*;
/*IFJ*/import  java.util.*;
/*end[VM.GCSPY]*/

/**
 * This is the Squawk VM. The various subsystems of the VM are
 * encapsulated in a linear object hierarchy with each class in the hierarchy
 * only depending on the subsystems represented by classes higher up in the
 * hierarchy. The hierarchy and a brief description of each susbsystem is
 * given in the sequence of C preprocessor include directives above.
 */
/*IFJ*/public final class Interpreter extends Interpret {


/*---------------------------------------------------------------------------*\
 *                                  startsWith                               *
\*---------------------------------------------------------------------------*/

    private boolean startsWith(String line, String match) {
        int i;
        for (i = 0 ;; i++) {
            int ch      = charAt(line,  i);
            int matchCh = charAt(match, i);
            if (matchCh == 0) {
                return true;
            }
            if (ch == 0) {
                return false;
            }
            if (matchCh != ch) {
                return false;
            }
        }
    }

/*---------------------------------------------------------------------------*\
 *                                    equals                                 *
\*---------------------------------------------------------------------------*/

    private boolean equals(String line, String match) {
        int i;
        for (i = 0 ;; i++) {
            int ch = charAt(line, i);
            if (ch == 0) {
                return charAt(match, i) == 0;
            }
            if (charAt(match, i) != ch) {
                return false;
            }
        }
    }

/*---------------------------------------------------------------------------*\
 *                           parseLong                                       *
\*---------------------------------------------------------------------------*/

    private long parseUnsignedLong(String line, int offset) {
        long value;
//IFC//#ifdef _MSC_VER
//IFC// sscanf(line + offset, "%I64u", &value);
//IFC//#else
//IFC// sscanf(line + offset, "%llu", &value);
//IFC//#endif
/*IFJ*/ value = Long.parseLong(line.substring(offset));
        return value;
    }

/*---------------------------------------------------------------------------*\
 *                               parseQuantity                               *
\*---------------------------------------------------------------------------*/

    private int parseQuantity(String line, int offset, String errMsg) {
        int i;
        int val = 0;
        for (i = offset ;; i++) {
            int ch = charAt(line, i);
            if (ch == 0) {
                break;
            } else if (ch >= '0' && ch <= '9') {
                val = (val * 10) + (ch - '0');
            } else if (ch == 'K') {
                val *= 1024;
                break;
            } else if (ch == 'M') {
                val *= (1024*1024);
                break;
            } else {
                fatalVMError(errMsg);
            }
        }
        return val;
    }

    private void setSegmentsTraceFilter(String line, int offset, String errMsg) {
        int ch = charAt(line, offset);
        if (ch == 'a') {
            traceSegments(true, false);
        } else if (ch == 'e') {
            traceSegments(false, true);
        } else if  (ch == 'b') {
            traceSegments(true, true);
        } else {
            fatalVMError(errMsg);
        }

    }

/*---------------------------------------------------------------------------*\
 *                               parsePercent                                *
\*---------------------------------------------------------------------------*/

    private int parsePercent(String line, int offset, String errMsg) {
        int n = parseQuantity(line, offset, errMsg);
        if (n <= 0 || n > 100) {
            fatalVMError(errMsg);
        }
        return n;
    }

/*---------------------------------------------------------------------------*\
 *                            usage                                          *
\*---------------------------------------------------------------------------*/

    void usage() {
/*               "<---------------- width of 80 character console window stops here ------------>" */
        printMsg("Usage: squawk [-options] class [args...]                                       "); println();
        printMsg("where options include:                                                         "); println();
        printMsg("  -Ximage:<image>           Start VM with image in file <image> (required)     "); println();
/*IFJ*/ printMsg("  -Xresourcepath:<path>     Path for resources (default: java.class.path       "); println();
/*IFJ*/ printMsg("                            system property)                                   "); println();
//IFC// printMsg("  -Xresourcepath:<path>     Path for resources (default: -cp value)            "); println();
//IFC// printMsg("  -Xcp:<path>               Path for embedded JVM to use (default: CLASSPATH   "); println();
//IFC// printMsg("                            environment variable)                              "); println();
        printMsg("  -Xnoyield                 Disable thread premption                           "); println();
        printMsg("  -XfullGC                  Always do full GCs                                 "); println();
        printMsg("  -Xyoung:n                 %heap for the young generation (default:10)        "); println();
//IFC//#ifdef TRACING
        printMsg("  -XdebugIO                 Log IO channel input/output to files named         "); println();
        printMsg("                            'channel<id>.input' and 'channel<id>.output'       "); println();
//IFC//#ifndef PRODUCTION
        printMsg("  -XtraceThreshold:<num> -Xtt:<num> Delay any tracing until after <num>        "); println();
        printMsg("                            instructions have executed                         "); println();
//IFC//#endif
        printMsg("  -XtraceInstructions -Xti  Enable instruction tracing                         "); println();
        printMsg("  -XtraceMethods      -Xtm  Enable method tracing                              "); println();
        printMsg("  -XtraceAllocation   -Xta  Enable allocation tracing                          "); println();
        printMsg("  -XtraceFrames       -Xtf  Enable activation frame tracing                    "); println();
        printMsg("  -XtraceGC           -Xtg  Enable garbage collection tracing                  "); println();
        printMsg("  -XtraceGCVerbose    -Xtgv Enable verbose garbage collection tracing          "); println();
        printMsg("  -XtraceGCSummary    -Xtgs Write a heap summary after each GC                 "); println();
        printMsg("  -XtraceURL:<url>    -Xtu:<url> URL for tracing output (default: file://trace)"); println();
        printMsg("  -XtraceImage        -Xts  Trace the startup image                            "); println();
        printMsg("  -XtraceSegments:<flags> -Xtss:<flags> Trace allocation and collection        "); println();
        printMsg("                            in only RAM ('a'), in only EEPROM ('e') or         "); println();
        printMsg("                            both ('b'). (default: 'b')                         "); println();
        printMsg("  -XtraceMigration    -Xto  Enable RAM -> EEPROM migration tracing             "); println();
        printMsg("  -XtraceMigrationVerbose -Xtov Enable verbose RAM -> EEPROM migration tracing "); println();
        printMsg("  -XmethodProf:<freq>       Set method profiling frequency                     "); println();
        printMsg("  -Xcheckpoint[:<base>]     Write a checkpoint of the memory after each GC to  "); println();
        printMsg("                            the file '<base><i>.image'. The default for <base> "); println();
        printMsg("                            is 'checkpoint' and <i> is the GC count.           "); println();
//IFC//#endif /* TRACING */
//IFC//#ifdef EXCESSIVEGC
        printMsg("  -XexcessiveGC             Run the VM with excessive garbage collection       "); println();
        printMsg("  -XveryexcessiveGC         Run the VM with very excessive garbage collection  "); println();
//IFC//#endif /* EXCESSIVEGC */
//IFC//#ifdef STATS
        printMsg("  -Xstats                   Display interpreter stats at VM exit               "); println();
//IFC//#endif /* STATS */
/*if[VM.GCSPY]*/
/*IFJ*/ printMsg("  -Xgcspy[:(<opt>=<val>)*]  Start GCspy server with args:                      "); println();
/*IFJ*/ printMsg("                               port: server port (def = 3000)                  "); println();
/*IFJ*/ printMsg("                               bs:   block size                                "); println();
/*end[VM.GCSPY]*/
        printMsg("  -Xhelp                    Display this message and exit                      "); println();
    }


/*---------------------------------------------------------------------------*\
 *                               processArguments                            *
\*---------------------------------------------------------------------------*/

//IFC// union { int i; char c[4]; } endianTest;

    private void processArguments(int argc, String argv[]) {
//IFC// int i = 1;
/*IFJ*/ int i = 0;
        boolean debugIO      = false;
        String  imageFile    = null;
        String  traceURL     = null;
        boolean traceRAM     = true;
        boolean traceEEPROM  = true;
        boolean traceImage   = false;
        boolean stats        = false;
/*IFJ*/ String  resourcepath = System.getProperty("java.class.path");
//IFC// String  resourcepath = null;
//IFC// String  cp           = getenv("CLASSPATH") == null ? "." : getenv("CLASSPATH");

/*IFJ*/ boolean isBigEndian  = true;
//IFC// boolean isBigEndian  = (endianTest.i = 1, endianTest.c[3] == 1);

/*if[VM.GCSPY]*/
/*IFJ*/ String gcspyOpts     = null;
/*end[VM.GCSPY]*/

        for (; i < argc ; i++) {
            String arg = argv[i];
            if (startsWith(arg, "-X")) {
                if (equals(arg, "-Xhelp")) {
                    usage();
                    exitToOperatingSystem(0);
                } else if (startsWith(arg, "-Ximage:")) {
                    imageFile = arg;
                } else if (startsWith(arg, "-Xcp:")) {
//IFC//             cp = arg+strlen("-Xcp:");
                } else if (startsWith(arg, "-Xresourcepath:")) {
                    resourcepath = arg;
//IFC//             resourcepath += 15;
/*IFJ*/             resourcepath = resourcepath.substring("-Xresourcepath:".length());
                } else if (startsWith(arg, "-Xcheckpoint")) {
//IFC//             String base = arg + 12;
/*IFJ*/             String base = arg.substring("-Xcheckpoint".length());
                    if (startsWith(base, ":")) {
//IFC//                 base += 1;
/*IFJ*/                 base = base.substring(1);
                    } else {
                        base = "checkpoint";
                    }
                    setCheckPointBase(base);
                } else if (equals(arg, "-Xnoyield")) {
                    noYield = true;
                } else if (equals(arg, "-XfullGC")) {
                    setFullGC(true);
                } else if (startsWith(arg, "-Xyoung:")) {
                    setYoungPercent(parsePercent(arg, 8, "bad -Xyoung"));
//IFC//#ifdef TRACING
                } else if (equals(arg, "-XdebugIO")) {
                    debugIO = true;
                } else if (startsWith(arg, "-XtraceURL:")) {
//IFC//             traceURL = arg + 11;
/*IFJ*/             traceURL = arg.substring("-XtraceURL:".length());
                } else if (startsWith(arg, "-Xtu:")) {
//IFC//             traceURL = arg + 5;
/*IFJ*/             traceURL = arg.substring("-Xtu:".length());
//IFC//#ifdef STATS
                } else if (startsWith(arg, "-XtraceThreshold:")) {
                    setThreshold(parseUnsignedLong(arg, 17));
                } else if (startsWith(arg, "-Xtt:")) {
                    setThreshold(parseUnsignedLong(arg, 5));
//IFC//#endif
                } else if (equals(arg, "-XtraceInstructions") || equals(arg, "-Xti")) {
                    setTraceInstructions(true);
                } else if (equals(arg, "-XtraceMethods") || equals(arg, "-Xtm")) {
                    setTraceMethods(true);
                } else if (equals(arg, "-XtraceAllocation") || equals(arg, "-Xta")) {
                    setTraceAllocation(true);
                } else if (equals(arg, "-XtraceFrames") || equals(arg, "-Xtf")) {
                    setTraceFrames(true);
                } else if (equals(arg, "-XtraceGC") || equals(arg, "-Xtg")) {
                    setTraceGC(true);
                } else if (equals(arg, "-XtraceGCVerbose") || equals(arg, "-Xtgv")) {
                    setTraceGCVerbose(true);
                } else if (equals(arg, "-XtraceGCSummary") || equals(arg, "-Xtgs")) {
                    setTraceGCSummary(true);
                } else if (equals(arg, "-XtraceMigration") || equals(arg, "-Xto")) {
                    setTraceMigration(true);
                } else if (equals(arg, "-XtraceMigrationVerbose") || equals(arg, "-Xtov")) {
                    setTraceMigrationVerbose(true);
                } else if (equals(arg, "-XtraceImage") || equals(arg, "-Xts")) {
                    traceImage = true;
                } else if (startsWith(arg, "-XtraceSegments:")) {
                    setSegmentsTraceFilter(arg, 16, "bad -XtraceSegments");
                } else if (startsWith(arg, "-Xtss:")) {
                    setSegmentsTraceFilter(arg, 6, "bad -Xtss");
                } else if (startsWith(arg, "-XmethodProf:")) {
                    setMethodProfileFreq(parseQuantity(arg, 13, "bad -XmethodProf"));
//IFC//#endif
//IFC//#ifdef EXCESSIVEGC
                } else if (equals(arg, "-XexcessiveGC")) {
                    setExcessiveGC(true);
                } else if (equals(arg, "-XveryexcessiveGC")) {
                    setVeryExcessiveGC(true);
//IFC//#endif /* EXCESSIVEGC */
//IFC//#ifdef STATS
                } else if (equals(arg, "-Xstats")) {
                    stats = true;
//IFC//#endif /* STATS */
/*if[VM.GCSPY]*/
                } else if (startsWith(arg, "-Xgcspy")) {
/*IFJ*/             gcspyOpts = arg.substring("-Xgcspy".length());
/*IFJ*/             if (gcspyOpts.startsWith(":")) { gcspyOpts = gcspyOpts.substring(1); }
/*end[VM.GCSPY]*/
                } else if (startsWith(arg, "-XX")) {
                    addArgument(arg);
                } else {
                    printMsg("Unknown/unsupported option ignored: ");
                    printMsg(arg);
                    println();
                }
            } else {
                break;
            }
        }

//IFC// if (resourcepath == null) {
//IFC//     resourcepath = cp;
//IFC// }

/*if[RESOURCE.CONNECTION]*/
        if (resourcepath != null) {
//IFC//     char *buf = (char *)malloc(strlen(resourcepath)+15+1);
//IFC//     strcpy(buf, "-XXresourcepath:");
//IFC//     strcat(buf, resourcepath);
//IFC//     addArgument(buf);
/*IFJ*/     addArgument("-XXresourcepath:"+resourcepath);
        }
/*end[RESOURCE.CONNECTION]*/

        while (i < argc) {
            addArgument(argv[i++]);
        }

//IFC//#ifdef TRACING
        if (traceURL == null && (getTracingAny() || traceImage)) {
            traceURL = "file://trace";
        }
//IFC//#endif /* TRACING */

        /* Initialize the underlying platform support. */
        PlatformAbstraction_init(traceURL, stats);

        if (imageFile != null) {
/*IFJ*/     byte[] image;
//IFC//     byte* image;
//IFC//     imageFile += 8;
/*IFJ*/     imageFile =  imageFile.substring("-Ximage:".length());
            image = readImage(imageFile, getImageOffset(), MMR, isBigEndian);

/*IFJ*/     if (MMR[MMR_magicNumber] == MMR_MAGICNUMBER_REVERSED) {
/*IFJ*/         for (int j = 0; j != MMR.length; j++) {
/*IFJ*/             int  word = MMR[j];
/*IFJ*/             MMR[j] = (word >> 24) & 0x000000FF |
/*IFJ*/                      (word >> 8)  & 0x0000FF00 |
/*IFJ*/                      (word << 8)  & 0x00FF0000 |
/*IFJ*/                      (word << 24) & 0xFF000000;
/*IFJ*/         }
/*IFJ*/         isBigEndian = false;
/*IFJ*/     }

            ObjectMemory_reinit(image, isBigEndian);
        } else {
            fatalVMError("-Ximage option is missing");
        }
/*IFJ*/ chan_init(debugIO, null); /* must come after Memory_init */
//IFC// chan_init(debugIO, cp); /* must come after Memory_init */

//IFC//#ifdef TRACING
        if (traceImage) {
            traceMMR();
            traceHeap(ROM, true);
            traceHeap(EEPROM, true);
            traceHeap(RAM, true);
        }
//IFC//#endif

/*if[VM.GCSPY]*/
//IFC//#if 0
        if (gcspyOpts != null) {

            int port      = 3000;
            int blockSize = -1;
            StringTokenizer st = new StringTokenizer(gcspyOpts, ",=");
            while (st.hasMoreTokens()) {
                String opt = st.nextToken();
                try {
                    if (opt.equals("port")) {
                        port = Integer.parseInt(st.nextToken());
                    } else if (opt.equals("bs")) {
                        blockSize = Integer.parseInt(st.nextToken());
                    } else {
                        printMsg("Unknown/unsupported GCspy option ignored: ");
                        printMsg(opt);
                        println();
                    }
                } catch (Exception ex) {
                    printMsg("Error while parsing GCspy options: "+ex);
                }
            }

            /* initialise the server */
            Events events = new Events(GCSPY_EVENT_NAMES);
            String generalInfo = "Squawk VM\n\nGeneral Info";
            ServerInterpreter interpreter = new ServerInterpreter("SquawkVM",
                true,
                events,
                2 /* max space number */);
            interpreter.setVerbose(true);
            interpreter.setGeneralInfo(generalInfo);

            gcspyInitializeLisp2Driver(interpreter, blockSize);

            try {
                interpreter.startServer(port, true);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
//IFC//#endif
/*end[VM.GCSPY]*/

    }


/*---------------------------------------------------------------------------*\
 *                                 Entrypoint                                *
\*---------------------------------------------------------------------------*/

/*IFJ*/     public Interpreter(String[] args) {
/*IFJ*/         setArgv(new String[args.length]);
/*IFJ*/         processArguments(args.length, args);
/*IFJ*/     }

/*IFJ*/     public static void main(String[] args) {
/*IFJ*/         Interpreter vm = new Interpreter(args);
/*IFJ*/         try {
/*IFJ*/             vm.run();


/*IFJ*/         } catch (Throwable ex) {
/*IFJ*/             vm.traceln(""+ex);
/*IFJ*/             ex.printStackTrace(vm.traceStream);
/*IFJ*/         }
/*IFJ*/         vm.exitVM(0);
/*IFJ*/     }


//IFC//     int main(int argc, char *argv[]) {
//IFC//         initHighWaterMark((int)&argc);
//IFC//         setArgv((char **)malloc(argc * sizeof(char *)));
//IFC//         processArguments(argc, argv);
//IFC//         run();
//IFC//         exitVM(0);
//IFC//     }

/*IFJ*/ }
