//J2C:platform.c **DO NOT DELETE THIS LINE**

//IFC// //#define CRASHONERROR

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/import  java.util.*;
/*IFJ*/import  java.io.*;
/*IFJ*/import  com.sun.squawk.util.*;
/*IFJ*/import  javax.microedition.io.*;
/*IFJ*/abstract class PlatformAbstraction extends InterpreterOptions {

/*MAC*/ int w2b(int $x) {
            return $x * 4;
        }

/*MAC*/ int bytesPerWord() {
            return w2b(1);
        }

/*MAC*/ int bitsPerByte() {
            return 8;
        }

/*MAC*/ int bitsPerWord() {
            return bytesPerWord() * bitsPerByte();
        }


/*---------------------------------------------------------------------------*\
 *                                  Stats                                    *
\*---------------------------------------------------------------------------*/

//IFC//#ifdef STATS
    long startTime;
    boolean statsFlag;
//IFC//#endif

/*---------------------------------------------------------------------------*\
 *                            Forward References                             *
\*---------------------------------------------------------------------------*/

    abstract Object getSendBuffer(int arr);
    abstract Object getReceiveBuffer(int arr);
    abstract void   setNativeByteArray(int arr, Object buf, int off, int len);

/*---------------------------------------------------------------------------*\
 *                            Printing to console                            *
\*---------------------------------------------------------------------------*/

/*IFJ*/PrintStream SystemErr   = System.err;
/*IFJ*/PrintStream traceStream;
//IFC//FILE*       traceStream;


//IFC//void signalHandler(int signum) {
//IFC//    char* strsignal(int signum);
//IFC//    void fatalVMErrorWithValue(String msg, int value);
//IFC//    if (traceStream != null) {
//IFC//        fflush(traceStream);
//IFC//        fclose(traceStream);
//IFC//        traceStream = null;
//IFC//    }
//IFC//    fatalVMErrorWithValue(strsignal(signum), signum);
//IFC//}

    /**
     * Initialise the underlying platform system.
     * @param url
     * @param stats
     */
    protected void PlatformAbstraction_init(String url, boolean stats) {
//IFC//#if 0
        OutputStream os = null;
        if (url != null && charAt(url, 0) != 0) {
            try {
                os = Connector.openOutputStream(url);
                SystemErr.println("Tracing to " + url);
            } catch (IOException ioe) {
                SystemErr.println("Couldn't open " + url + " for trace output: tracing to stderr");
                traceStream = SystemErr;
            }
        }
        if (os != null) {
            traceStream = new PrintStream(os);
        } else {
            traceStream = SystemErr;
        }

//IFC//#endif
//IFC//#ifdef TRACING
//IFC// if (url != null) {
//IFC//     if (strncmp(url, "file://", 7) == 0) {
//IFC//         url += 7;
//IFC//         if ((traceStream = fopen(url, "w")) == null) {
//IFC//             fprintf(stderr, "Couldn't open %s for trace output: %s\n", url, strerror(errno));
//IFC//         }
//IFC//     } else {
//IFC//         fprintf(stderr, "Native VM can only handle \"file://\" URLs for tracing: tracing disabled (URL given = %s)\n", url);
//IFC//         traceStream = stderr;
//IFC//     }
//IFC// } else {
//IFC//     traceStream = stderr;
//IFC// }
//IFC//#endif

//IFC//#ifdef STATS
//IFC//{long getTime();
        statsFlag  = stats;
        startTime  = getTime();
//IFC//}
//IFC//#endif

//IFC// /* Install the signal handler for the signals we want to catch */
//IFC//#ifndef CRASHONERROR
//IFC//     signal(SIGSEGV, signalHandler);
//IFC//     signal(SIGINT,  signalHandler);
//IFC//#endif
    }

    protected boolean traceIsSystem_err() {
/*IFJ*/ return traceStream == SystemErr;
//IFC//#ifdef TRACING
//IFC// return traceStream == stderr;
//IFC//#else
//IFC// return false;
//IFC//#endif
    }


    protected void PlatformAbstraction_finalize() {
/*IFJ*/if (traceStream != SystemErr && traceStream != null) { traceStream.close(); traceStream = null; }
//IFC//if (traceStream != stderr    && traceStream != null) { fclose(traceStream); traceStream = null; }
    }

    /*
     * println
     */
    void println() {
/*IFJ*/ System.out.println();
//IFC// printf("\n");
    }

    /*
     * printCh
     */
    void printCh(int ch) {
/*IFJ*/ System.out.print((char)ch);
//IFC// printf("%c", ch);
    }

    /*
     * printErrCh
     */
    void printErrCh(int ch) {
/*IFJ*/ System.err.print((char)ch);
//IFC// fprintf(stderr, "%c", ch);
//IFC// fflush(stderr);
    }

    /*
     * printMsg
     */
    void printMsg(String str) {
/*IFJ*/ System.out.print(str);
//IFC// printf("%s", str);
    }

    /*
     * printInt
     */
    void printInt(int i) {
/*IFJ*/ System.out.print(i);
//IFC// printf("%d", i);
    }

    /*
     * printIntPad
     */
    void printIntPad(int i, int cols) {
/*IFJ*/ String s = Integer.toString(i);
/*IFJ*/ cols -= s.length();
/*IFJ*/ System.out.print(i);
/*IFJ*/ while (cols-- > 0) { System.out.print(' '); }
//IFC// printf("%-*d", cols, i);
    }


    /*
     * printLong
     */
    void printLong(long l, boolean signedValue) {
/*IFJ*/ System.out.print(l);
//IFC//#ifdef _MSC_VER
//IFC// printf(signedValue ? "%I64d" : "%I64u", l);
//IFC//#else
//IFC// printf(signedValue ? "%lld" : "%llu", l);
//IFC//#endif /* _MSC_VER */
    }

    /*
     * printHex
     */
    void printHex(int i) {
/*IFJ*/ printMsg(Integer.toHexString(i));
//IFC// printf("%X", i);
    }

    /*
     * printStat
     */
    void printMsgLong(String msg, long l) {
        printMsg(msg);
        printLong(l, false);
        println();
    }

/*---------------------------------------------------------------------------*\
 *                         Tracing                                           *
\*---------------------------------------------------------------------------*/

//IFC//#ifdef TRACING

    /*
     * trace
     */
    void trace(String str) {
        if (traceStream != null) {
/*IFJ*/     traceStream.print(str);
//IFC//     fprintf(traceStream, "%s", str);
        }
    }

    /*
     * traceln
     */
    void traceln(String str) {
        if (traceStream != null) {
/*IFJ*/     traceStream.println(str);
//IFC//     fprintf(traceStream, "%s\n", str);
        }
    }

    /*
     * traceInt
     */
    void traceInt(int i) {
        if (traceStream != null) {
/*IFJ*/     traceStream.print(i);
//IFC//     fprintf(traceStream, "%d", i);
        }
    }

    /*
     * traceIntPad
     */
    void traceIntPad(int i, int cols) {
        if (traceStream != null) {
/*IFJ*/     String s = Integer.toString(i);
/*IFJ*/     cols -= s.length();
/*IFJ*/     traceStream.print(i);
/*IFJ*/     while (cols-- > 0) { traceStream.print(' '); }
//IFC//     fprintf(traceStream, "%-*d", cols, i);
        }
    }

    /*
     * traceHex
     */
    void traceHex(int i) {
        if (traceStream != null) {
            traceInt(i);
///*IFJ*/   trace(Integer.toHexString(i));
////IFC//   fprintf(traceStream, "%x", i);
        }
    }

    /*
     * traceLong
     */
    void traceLong(long l, boolean signedValue) {
        if (traceStream != null) {
/*IFJ*/     traceStream.print(l);
//IFC//#ifdef _MSC_VER
//IFC//     fprintf(traceStream, signedValue ? "%I64d" : "%I64u", l);
//IFC//#else
//IFC//     fprintf(traceStream, signedValue ? "%lld" : "%llu", l);
//IFC//#endif /* _MSC_VER */
        }
    }

    /*
     * traceTab
     */
    void traceTab(int pos) {
        if (traceStream != null) {
/*IFJ*/     traceStream.print("~{");
/*IFJ*/     traceStream.print(pos);
/*IFJ*/     traceStream.print("}");
//IFC//     fprintf(traceStream, "~{%d}", pos);
        }
    }

    /*
     * traceChar
     */
    void traceChar(int c) {
        if (traceStream != null) {
            if ((c >= 0x0020) && (c < 0x007F)) {
/*IFJ*/         traceStream.print((char)c);
//IFC//         fprintf(traceStream, "%c", c);
            } else {
//IFC//         fprintf(traceStream, "\\u%0.4X", c);
/*IFJ*/         traceStream.print("\\u");
/*IFJ*/         String s = Integer.toHexString(c);
/*IFJ*/         for (int i = (4-s.length()); i > 0; --i) {
/*IFJ*/             traceStream.print('0');
/*IFJ*/         }
/*IFJ*/         traceStream.print(s);
            }
        }
    }

//IFC//#ifdef STATS
    void traceInstructionCount(boolean nl) {
//IFC// jlong getInstructionCount();
        trace(" (after ");
        traceLong(getInstructionCount(), false);
        if (nl) {
            traceln(" instructions)");
        } else {
            trace(" instructions)");
        }
    }
//IFC//#else
//IFC//#define traceInstructionCount(nl) /**/
//IFC//#endif /* STATS */

//IFC//#else /* !TRACING */
//IFC//#define trace(str)     /**/
//IFC//#define traceln(str)   /**/
//IFC//#define traceInt(str)  /**/
//IFC//#define traceHex(str)  /**/
//IFC//#define traceLong(str) /**/
//IFC//#define traceTab(str)  /**/
//IFC//#define traceChar(str) /**/
//IFC//#define traceInstructionCount(nl) /**/
//IFC//#endif /* TRACING */

/*---------------------------------------------------------------------------*\
 *                                     Stats                                 *
\*---------------------------------------------------------------------------*/

//IFC//#ifdef STATS

        int initialWaterMark;
        int highWaterMark;

        void initHighWaterMark(int addr) {
            initialWaterMark = addr;
            highWaterMark = 0;
        }
        void updateHighWaterMark() {
//IFC//     int depth;
//IFC//     depth = initialWaterMark - ((int)&depth);
//IFC//     if (depth > highWaterMark) {
//IFC//         highWaterMark = depth;
//IFC//     }
        }



    abstract void printExtensionCalls();
    abstract void printGCStats();
    abstract void printEepromGCStats();

    private long instructionCount       = 0;
    private long branchCount            = 0;
    private long invokeCount            = 0;
    private long invokePrimitiveCount   = 0;
    private long extensionCount         = 0;
    private long yieldCount             = 0;
    private long switchCount            = 0;
    private long allocationCount        = 0;
    private long stackChunkCount        = 0;
    private long smallArrayCount        = 0;
    private long largeArrayCount        = 0;
    private long collectionCount        = 0;

    private long timerStart;
    final static int
        TIME_RAM_GC                     = 0,
        TIME_EEPROM_GC                  = 1,
        TIME_RAM_TO_EEPROM_COPY         = 2,
        TIME_LAST                       = 3;


/*IFJ*/private long actionTimes[] = new long[TIME_LAST];
//IFC//private long actionTimes[TIME_LAST];

/*IFJ*/private long instructionSuiteCount[] = new long[256];
//IFC//private long instructionSuiteCount[256];

    void printVMStats(int exitCode) {
        if (statsFlag) {
//IFC//     long getTime();
            int i;
            println();
            println();
            printMsgLong("Result                  ", exitCode              );
            printMsgLong("Instructions            ", instructionCount      );
            printMsg( "Instructions by suite   ");
            for (i = 0 ; i < 256 ; i++) {
                if (instructionSuiteCount[i] != 0) {
                    printInt(i); printMsg("="); printLong(instructionSuiteCount[i], false); printMsg(" ");
                }
            }
            println();

            printMsgLong("Branches                ", branchCount           );
            printMsgLong("Invokes                 ", invokeCount           );
            printMsgLong("InvokePrimitives        ", invokePrimitiveCount  );
            printMsgLong("Extension calls         ", extensionCount        );
            printMsgLong("Yields                  ", yieldCount            );
            printMsgLong("Switches                ", switchCount           );
            printMsgLong("Allocations             ", allocationCount       );
            printMsgLong("ChunkAllocations        ", stackChunkCount       );
            printMsgLong("SmallArrayCount         ", smallArrayCount       );
            printMsgLong("LargeArrayCount         ", largeArrayCount       );
            printMsgLong("Collections             ", collectionCount       );
//IFC//     printMsgLong("Max C stack depth       ", highWaterMark         );
            printExtensionCalls();
            printMsg("Times: (msecs)");
            println();
            printMsgLong("  RAM GC                ", actionTimes[TIME_RAM_GC]            );
            printMsgLong("  EEPROM GC             ", actionTimes[TIME_EEPROM_GC]         );
            printMsgLong("  RAM -> EEPROM copying ", actionTimes[TIME_RAM_TO_EEPROM_COPY]);
            printMsgLong("  Total                 ", getTime() - startTime );
            println();
            printMsg("RAM collector stats:");
            println();
            printGCStats();
            println();
            printMsg("EEPROM collector stats:");
            println();
            printEepromGCStats();
        }
    }

    void incInstructionCount(int cno) {
        int suite = (cno&0xFF00)>>8;
        int klass =  cno&0xFF;
        instructionCount++;
        instructionSuiteCount[suite]++;
    }

/*MAC*/ void incBranchCount()                   { branchCount++;               }
/*MAC*/ void incInvokeCount()                   { invokeCount++;               }
/*MAC*/ void incInvokePrimitiveCount()          { invokePrimitiveCount++;      }
/*MAC*/ void incExtensionCount()                { extensionCount++;            }
/*MAC*/ void incYieldCount()                    { yieldCount++;                }
/*MAC*/ void incSwitchCount()                   { switchCount++;               }
/*MAC*/ void incAllocationCount()               { allocationCount++;           }
/*MAC*/ void incStackChunkCount()               { stackChunkCount++;           }
/*MAC*/ void incSmallArrayCount()               { smallArrayCount++;           }
/*MAC*/ void incLargeArrayCount()               { largeArrayCount++;           }
/*MAC*/ void incCollectionCount()               { collectionCount++;           }
/*MAC*/ long getInvokeCount()                   { return invokeCount;          }
/*MAC*/ long getInvokePrimitiveCount()          { return invokePrimitiveCount; }
        long getInstructionCount()              { return instructionCount;     }
/*MAC*/ long getTotalInvokeCount()              { return invokeCount + invokePrimitiveCount; }
/*MAC*/ long getCollectionCount()               { return collectionCount; }

/*MAC*/ void restartTimer()                     { timerStart = getTime();                         }
/*MAC*/ void stopTimer(int $action)             { actionTimes[$action] += getTime() - timerStart; }

//IFC//#else

//IFC//#define printVMStats(x)              /**/
//IFC//#define incInstructionCount(x)       /**/
//IFC//#define incBranchCount()             /**/
//IFC//#define incInvokeCount()             /**/
//IFC//#define incInvokePrimitiveCount()    /**/
//IFC//#define incYieldCount()              /**/
//IFC//#define incSwitchCount()             /**/
//IFC//#define incAllocationCount()         /**/
//IFC//#define incStackChunkCount()         /**/
//IFC//#define incSmallArrayCount()         /**/
//IFC//#define incLargeArrayCount()         /**/
//IFC//#define incCollectionCount()         /**/
//IFC//#define getInvokeCount()             0
//IFC//#define getInvokePrimitiveCount()    0
//IFC//#define getInstructionCount()        0
//IFC//#define getTotalInvokeCount()        0
//IFC//#define getCollectionCount()         0
//IFC//#define restartTimer()               /**/
//IFC//#define stopTimer(action)            /**/

//IFC//#define initHighWaterMark(addr)      /**/
//IFC//#define updateHighWaterMark()        /**/

//IFC//#endif /* STATS */

   /*---------------------------------------------------------------------------*\
    *                                Termination                                *
   \*---------------------------------------------------------------------------*/


    /**
     * Stop the VM with a given process exit value.
     * @param The process exit value.
     */
    void exitToOperatingSystem(int exitCode) {
        PlatformAbstraction_finalize();
/*IFJ*/ System.exit(exitCode);
//IFC// exit(exitCode);
    }

    /**
     * Stop the VM with a given process exit value.
     * This function also dumps some VM execution statistics before exiting.
     * @param The process exit value.
     */
    void exitVM(int exitCode) {
//IFC// void chan_finalize();
        chan_finalize();
/*IFJ*/ System.out.flush();
//IFC// fflush(0);
        printVMStats(exitCode);
/*IFJ*/ System.out.flush();
//IFC// fflush(0);
        exitToOperatingSystem(exitCode);
    }


   /*---------------------------------------------------------------------------*\
    *                                Fatal Errors                               *
   \*---------------------------------------------------------------------------*/


    /*
     * dumpStack
     */
    void dumpStack() {
/*IFJ*/ (new Throwable("\n\nNative VM stack trace:")).printStackTrace(traceStream);
    }

    /*
     * dumpHeap
     */
    void dumpHeap() {
    }

    /*
     * Guard to ensure we don't recurse into fatalError which may occur if there
     * is an error while dumping the stack or the heap.
     */
    boolean fatalVMErrorGuard = false;

    /**
     * Report a fatal error indicating that some
     * unexpected situation has been encountered by the VM.
     * This may be due to a bug in the VM.  VM execution will
     * be stopped. This operation should be called only from
     * inside the VM. Stop the VM with a fatal error.
     * @param msg A message describing the error
     * @param value The exit value for the VM.
     */
    void fatalVMErrorWithValue(String msg, int value) {
        if (!fatalVMErrorGuard) {
            fatalVMErrorGuard = true;
//IFC//#ifdef TRACING
            traceln("");
            traceln("");
            dumpStack();
//          dumpHeap();
            traceln("");
//IFC//#endif /* TRACING */
            println();
            printMsg(msg);
            printMsg(" (exit value=");
            printInt(value);
            printMsg(")");
            println();
        }
        else {
            printMsg("fatalVMError called recursively");
            exitToOperatingSystem(value);
        }
//IFC//#ifdef CRASHONERROR
//IFC// {   int *p = (int*)0xFFFFFFFF;
//IFC//     int  q = *p; }
//IFC//#endif
        exitVM(value);
    }

    /*
     * fatalVMError
     */
    void fatalVMError(String msg) {
        fatalVMErrorWithValue(msg, 1);
    }

    /*
     * shouldNotReachHere
     */
    void  shouldNotReachHere() {
        fatalVMError("shouldNotReachHere()");
    }

    /**
     * Assert a given boolean condition to be true, causing a fatal error if
     * it isn't. The C version of this function is only enabled in non-
     * production builds or when the ASSUME macro is defined. The C version
     * behave's much like the standard C <assert.h> functionality.
     * @param b
     */
//IFC//#if 0
    void assume(boolean b) {
         if (!b) {
             fatalVMError("Assume Failure");
         }
    }
//IFC//#else
//IFC//
//IFC//#if defined(ASSUME) || !defined(PRODUCTION)
//IFC//#define assume(x) if (!(x)) { fprintf(stderr, "Assertion failed: \"%s\", at %s:%d\n", #x, __FILE__, __LINE__); fatalVMError(""); }
//IFC//#else
//IFC//#define assume(x) /**/
//IFC//#endif /* defined(ASSUME) || !defined(PRODUCTION) */
//IFC//
//IFC//#endif /* 0 */



//IFC//#if defined(ASSUME) || !defined(PRODUCTION)
/*MAC*/boolean assuming()             { return true;                                  }
       int assumeNonZero(int val, String file, int line) {
/*IFJ*/    assume(val != 0);
//IFC//    if (val == 0) { fprintf(stderr, "Non-zero assertion failed at %s:%d\n", file, line); fatalVMError(""); }
           return val;
       }
//IFC//#else
//IFC//#define assuming()             (false)
//IFC//#define assumeNonZero(x, file, line) (x)
//IFC//#endif /* defined(ASSUME) || !defined(PRODUCTION) */



//IFC//#ifndef PRODUCTION
        private boolean  inCollector = false;
        void        setInCollector(boolean b)   { inCollector = b;                              }
        void        assumeInCollector()         { assume(inCollector);                          }
        void        assumeNotInCollector()      { assume(!inCollector);                         }
//IFC//#else
//IFC//#define      setInCollector(x)           /**/
//IFC//#define      assumeInCollector()         /**/
//IFC//#define      assumeNotInCollector()      /**/
//IFC//#endif


   /*---------------------------------------------------------------------------*\
    *                                Instructions                               *
   \*---------------------------------------------------------------------------*/

/*IFJ*//*MAC*/  int  sll(int $a, int $b)         { return $a<<$b;                               }
//IFC///*MAC*/  int  sll(int $a, int $b)         { return $a<<($b&31);                          }
/*IFJ*//*MAC*/  int  sra(int $a, int $b)         { return $a>>$b;                               }
//IFC///*MAC*/  int  sra(int $a, int $b)         { return $a>>($b&31);                          }
/*IFJ*//*MAC*/  int  srl(int $a, int $b)         { return $a>>>$b;                              }
//IFC///*MAC*/  int  srl(int $a, int $b)         { return ((unsigned)$a)>>($b&31);              }
/*MAC*/         int  i2b(int $i)                 { return (byte)$i;                             }
/*MAC*/         int  i2s(int $i)                 { return (short)$i;                            }
/*MAC*/         int  i2c(int $i)                 { return (char)$i;                             }
/*MAC*/         long i2l(int $i)                 { return (long)$i;                             }
/*IFJ*//*MAC*/  long slll(long $a, int $b)       { return $a<<$b;                               }
//IFC///*MAC*/  long slll(long $a, int $b)       { return $a<<($b&63);                          }
/*IFJ*//*MAC*/  long sral(long $a, int $b)       { return $a>>$b;                               }
//IFC///*MAC*/  long sral(long $a, int $b)       { return $a>>($b&63);                          }
/*IFJ*//*MAC*/  long srll(long $a, int $b)       { return $a>>>$b;                              }
//IFC///*MAC*/  long srll(long $a, int $b)       { return ((ulong)$a)>>($b&63);                 }
/*MAC*/         int  l2i(long $l)                { return (int)$l;                              }
/*MAC*/         int  cmpl(long $l, long $r)      { return ($l < $r) ? -1 : ($l == $r) ? 0 : 1;  }
                void breakpoint()                { fatalVMError("Breakpoint");                  }

/*if[FLOATS]*/
/*IFJ*/         float  ib2f(int i)               { return Float.intBitsToFloat(i);              }
/*IFJ*/         double lb2d(long l)              { return Double.longBitsToDouble(l);           }
/*IFJ*/         int    f2ib(float f)             { return Float.floatToIntBits(f);              }
/*IFJ*/         long   d2lb(double d)            { return Double.doubleToLongBits(d);           }
/*IFJ*/         float  fmodf(float a, float b)   { return a % b;                                }
/*IFJ*/         double fmodd(double a, double b) { return a % b;                                }
//IFC//         union uu { int i; float f; jlong l; double d; };
//IFC//         float  ib2f(int i)               { union uu x; x.i = i; return x.f;             }
//IFC//         double lb2d(long l)              { union uu x; x.l = l; return x.d;             }
//IFC//         int    f2ib(float f)             { union uu x; x.f = f; return x.i;             }
//IFC//         long   d2lb(double d)            { union uu x; x.d = d; return x.l;             }
//IFC//         float  fmodf(float a, float b)   { return (float)fmod(a, b);                    }
//IFC//         double fmodd(double a, double b) { return fmod(a, b);                           }

/*MAC*/         int  i2f(int $i)                 { return f2ib((float)$i);                      }
/*MAC*/         long i2d(int $i)                 { return d2lb((double)$i);                     }
/*MAC*/         int  l2f(long $l)                { return f2ib((float)$l);                      }
/*MAC*/         long l2d(long $l)                { return d2lb((double)$l);                     }
/*MAC*/         int  addf(int $l, int $r)        { return f2ib(ib2f($l) + ib2f($r));            }
/*MAC*/         int  subf(int $l, int $r)        { return f2ib(ib2f($l) - ib2f($r));            }
/*MAC*/         int  mulf(int $l, int $r)        { return f2ib(ib2f($l) * ib2f($r));            }
/*MAC*/         int  divf(int $l, int $r)        { return f2ib(ib2f($l) / ib2f($r));            }
/*MAC*/         int  remf(int $l, int $r)        { return f2ib(fmodf(ib2f($l), ib2f($r)));      }
/*MAC*/         int  negf(int $l)                { return f2ib(((float)0) - ib2f($l));          }
/*MAC*/         int  f2i(int $f)                 { return (int)ib2f($f);                        }
/*MAC*/         long f2l(int $f)                 { return (long)ib2f($f);                       }
/*MAC*/         long f2d(int $f)                 { return d2lb((double)ib2f($f));               }
/*MAC*/         int  cmpf(float $l, float $r)    { return ($l < $r) ? -1 : ($l == $r) ? 0 : 1;  }
/*MAC*/         int  cmpfl(int $l, int $r)       { return cmpf(ib2f($l), ib2f($r));             }
/*MAC*/         int  cmpfg(int $l, int $r)       { return cmpf(ib2f($l), ib2f($r));             }
/*MAC*/         long addd(long $l, long $r)      { return d2lb(lb2d($l) + lb2d($r));            }
/*MAC*/         long subd(long $l, long $r)      { return d2lb(lb2d($l) - lb2d($r));            }
/*MAC*/         long muld(long $l, long $r)      { return d2lb(lb2d($l) * lb2d($r));            }
/*MAC*/         long divd(long $l, long $r)      { return d2lb(lb2d($l) / lb2d($r));            }
/*MAC*/         long remd(long $l, long $r)      { return d2lb(fmodd(lb2d($l), lb2d($r)));      }
/*MAC*/         long negd(long $l)               { return d2lb(((double)0) - lb2d($l));         }
/*MAC*/         int  d2i(long $l)                { return (int)lb2d($l);                        }
/*MAC*/         long d2l(long $l)                { return (long)lb2d($l);                       }
/*MAC*/         int  d2f(long $l)                { return f2ib((float)lb2d($l));                }
/*MAC*/         int  cmpd(double $l, double $r)  { return ($l < $r) ? -1 : ($l == $r) ? 0 : 1;  }
/*MAC*/         int  cmpdl(long $l, long $r)     { return cmpd(lb2d($l), lb2d($r));             }
/*MAC*/         int  cmpdg(long $l, long $r)     { return cmpd(lb2d($l), lb2d($r));             }
/*end[FLOATS]*/


   /*---------------------------------------------------------------------------*\
    *                                Math functions                             *
   \*---------------------------------------------------------------------------*/

/*if[FLOATS]*/
    long math0(int op, long rs1_l, long rs2_l) {
        double rs1 = lb2d(rs1_l);
        double rs2 = lb2d(rs2_l);
        double res = 0.0;
        switch (op) {
/*IFJ*/     case MathOpcodes.SIN:            res =  Math.sin(rs1);                  break;
/*IFJ*/     case MathOpcodes.COS:            res =  Math.cos(rs1);                  break;
/*IFJ*/     case MathOpcodes.TAN:            res =  Math.tan(rs1);                  break;
/*IFJ*/     case MathOpcodes.ASIN:           res =  Math.asin(rs1);                 break;
/*IFJ*/     case MathOpcodes.ACOS:           res =  Math.acos(rs1);                 break;
/*IFJ*/     case MathOpcodes.ATAN:           res =  Math.atan(rs1);                 break;
/*IFJ*/     case MathOpcodes.EXP:            res =  Math.exp(rs1);                  break;
/*IFJ*/     case MathOpcodes.LOG:            res =  Math.log(rs1);                  break;
/*IFJ*/     case MathOpcodes.SQRT:           res =  Math.sqrt(rs1);                 break;
/*IFJ*/     case MathOpcodes.CEIL:           res =  Math.ceil(rs1);                 break;
/*IFJ*/     case MathOpcodes.FLOOR:          res =  Math.floor(rs1);                break;
/*IFJ*/     case MathOpcodes.ATAN2:          res =  Math.atan2(rs1, rs2);           break;
/*IFJ*/     case MathOpcodes.POW:            res =  Math.pow(rs1, rs2);             break;
/*IFJ*/     case MathOpcodes.IEEE_REMAINDER: res =  Math.IEEEremainder(rs1, rs2);   break;
//IFC//     case MathOpcodes.SIN:            res =  sin(rs1);                       break;
//IFC//     case MathOpcodes.COS:            res =  cos(rs1);                       break;
//IFC//     case MathOpcodes.TAN:            res =  tan(rs1);                       break;
//IFC//     case MathOpcodes.ASIN:           res =  asin(rs1);                      break;
//IFC//     case MathOpcodes.ACOS:           res =  acos(rs1);                      break;
//IFC//     case MathOpcodes.ATAN:           res =  atan(rs1);                      break;
//IFC//     case MathOpcodes.EXP:            res =  exp(rs1);                       break;
//IFC//     case MathOpcodes.LOG:            res =  log(rs1);                       break;
//IFC//     case MathOpcodes.SQRT:           res =  sqrt(rs1);                      break;
//IFC//     case MathOpcodes.CEIL:           res =  ceil(rs1);                      break;
//IFC//     case MathOpcodes.FLOOR:          res =  floor(rs1);                     break;
//IFC//     case MathOpcodes.ATAN2:          res =  atan2(rs1, rs2);                break;
//IFC//     case MathOpcodes.POW:            res =  pow(rs1, rs2);                  break;
//IFC//     case MathOpcodes.IEEE_REMAINDER: {
//IFC//         double q = fmod(rs1, rs2);
//IFC//         double d = fabs(rs2);
//IFC//         if (q < 0) {
//IFC//             if (-q > d / 2) {
//IFC//                 q += d;
//IFC//             }
//IFC//         } else {
//IFC//             if (q > d / 2) {
//IFC//                 q -= d;
//IFC//             }
//IFC//         }
//IFC//         res = q;
//IFC//         break;
//IFC//     }
            default:                 shouldNotReachHere();
        }
        return d2lb(res);
    }
/*end[FLOATS]*/


   /*---------------------------------------------------------------------------*\
    *                            Native code interface                          *
   \*---------------------------------------------------------------------------*/

    /*
     * getTime
     */
    long getTime() {
/*IFJ*/ return System.currentTimeMillis();
//IFC// long sysTimeMillis();
//IFC// return sysTimeMillis();
    }


//     /*---------------------------------------------------------------------------*\
//      *                            Map file processing                            *
//     \*---------------------------------------------------------------------------*/
//
//
//  //IFC//#if 0
//
//  Vector mapFileEntries = new Vector();
//
//      void openMapFile(String name) {
//          try {
//              BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(name)));
//              for (;;) {
//                  String s = br.readLine();
//                  if (s == null) {
//                      break;
//                  }
//                  mapFileEntries.addElement(s);
//              }
//              br.close();
//          } catch (IOException ex) {
//          }
//      }
//
//      void traceMapFileEntry(int index) {
//          try {
//              String s = (String)mapFileEntries.elementAt(index);
//              trace(s);
//          } catch (ArrayIndexOutOfBoundsException ex) {
//              trace("**Unknown Method**");
//          }
//      }
//
//  //IFC//#else
//  //IFC//void openMapFile(String name) {
//  //IFC//    notImplementedYet("openMapFile()");
//  //IFC//}
//  //IFC//void traceMapFileEntry(int index) {
//  //IFC//    notImplementedYet("traceMapFileEntry()");
//  //IFC//}
//  //IFC//#endif

   /*---------------------------------------------------------------------------*\
    *                           Image file processing                           *
   \*---------------------------------------------------------------------------*/

   /*
    * The image file has the contents of the heap built by the romizer
    * with the int array representing the MMR prepended.
    */


//IFC//#if 0
    /**
     * Open a file containing the memory image and read the image.
     *
     * @param imageFileName The name of the image file.
     * @param imageOffset The offset at which the image contents should begin
     * in the returned array.
     * @param mmr The array into which the memory management records contents
     * should be read.
     * @param isBigEndian Indicates the endianess of the platform.
     * @return the contents of the image file. Also, the MMR record stored
     * at the start of the image file is written into 'mmr'.
     */
    byte[] readImage(String imageFileName, int imageOffset, int[] mmr, boolean isBigEndian) {
        try {
            assume(mmr.length == MMR_SIZE);
            int fileSize = (int)((new File(imageFileName)).length());
            int imageSize = fileSize - (MMR_SIZE * 4);
            DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(imageFileName), fileSize));

            // Read the MMR
            for (int i = 0; i != MMR_SIZE; i++) {
                if (isBigEndian) {
                    mmr[i] = dis.readInt();
                } else {
                    mmr[i] = dis.readByte()       |
                             dis.readByte() << 8  |
                             dis.readByte() << 16 |
                             dis.readByte() << 24;
                }
            }

            // Read the memory image
            byte[] image = new byte[imageOffset + imageSize];
            int read;
            if ((read = dis.read(image, imageOffset, imageSize)) != imageSize) {
                throw new IOException("Only "+read+" of "+imageSize+" bytes were read from the image file");
            }
            dis.close();
            return image;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            fatalVMError(ioe.getMessage());
            return null;
        }
   }

   /**
    * Write the image to a file.
    *
    * @param imageFileName
    * @param image
    * @param size
    * @param mmr
    * @param isBigEndian
    */
   void writeImage(String imageFileName, byte[] image, int imageSize, int[] mmr, boolean isBigEndian) {
       try {
           int imageOffset = image.length - imageSize;
           int fileSize = imageSize + (MMR_SIZE * 4);
           assume(mmr.length == MMR_SIZE);
           DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(imageFileName), fileSize));

           // Read the MMR
           for (int i = 0; i != MMR_SIZE; i++) {
               if (isBigEndian) {
                   dos.writeInt(mmr[i]);
               } else {
                   int value = mmr[i];
                   dos.writeByte((byte)(value      ));
                   dos.writeByte((byte)(value >> 8 ));
                   dos.writeByte((byte)(value >> 16));
                   dos.writeByte((byte)(value >> 24));
               }
           }

           // Write the image
           dos.write(image, imageOffset, imageSize);
           dos.close();
       } catch (IOException ioe) {
           ioe.printStackTrace();
           fatalVMError(ioe.getMessage());
       }
   }
//IFC//#endif

//IFC//byte* readImage(String imageFileName, int ignored, int mmr[], boolean ignore) {
//IFC//    struct stat buf;
//IFC//    static char errMsg[1024];
//IFC//    if (stat(imageFileName, &buf) == 0) {
//IFC//#ifdef _MSC_VER
//IFC//        int fd = open(imageFileName, O_RDONLY|O_BINARY);
//IFC//#else
//IFC//        int fd = open(imageFileName, O_RDONLY);
//IFC//#endif
//IFC//        if (fd != -1) {
//IFC//            int mmrSize = MMR_SIZE*4;
//IFC//            int   size  = buf.st_size - (mmrSize);
//IFC//            byte* image = (byte*)malloc(size);
//IFC//
//IFC//            /* Read the MMR. */
//IFC//            int count   = 0;
//IFC//            int toRead  = mmrSize;
//IFC//            int offset  = 0;
//IFC//            while (toRead > 0) {
//IFC//                count = read(fd, ((char*)mmr)+offset, toRead);
//IFC//                if (count == -1) {
//IFC//                    sprintf(errMsg, "Call to read() failed: %s (mmrSize=%d, read=%d)\n", strerror(errno), mmrSize, mmrSize-toRead);
//IFC//                    fatalVMError(errMsg);
//IFC//                }
//IFC//                toRead -= count;
//IFC//                offset += count;
//IFC//            }
//IFC//
//IFC//            /* Read the image. */
//IFC//            count   = 0;
//IFC//            toRead  = size;
//IFC//            offset  = 0;
//IFC//            while (toRead > 0) {
//IFC//                count = read(fd, image+offset, toRead);
//IFC//                if (count == -1) {
//IFC//                    sprintf(errMsg, "Call to read() failed: %s (size=%d, read=%d)\n", strerror(errno), size, size-toRead);
//IFC//                    fatalVMError(errMsg);
//IFC//                }
//IFC//                toRead -= count;
//IFC//                offset += count;
//IFC//            }
//IFC//            close(fd);
//IFC//            return image;
//IFC//        } else {
//IFC//            sprintf(errMsg, "Call to open(%s) failed: %s\n", imageFileName, strerror(errno));
//IFC//            fatalVMError(errMsg);
//IFC//        }
//IFC//    } else {
//IFC//        sprintf(errMsg, "Call to stat(%s) failed: %s\n", imageFileName, strerror(errno));
//IFC//        fatalVMError(errMsg);
//IFC//    }
//IFC//    return 0;
//IFC//}

//IFC//   void writeImage(String imageFileName, byte* image, int size, int mmr[], boolean ignore) {
//IFC//#ifdef _MSC_VER
//IFC//        int fd = open(imageFileName, O_RDWR | O_BINARY | O_CREAT, 660);
//IFC//#else
//IFC//        int fd = open(imageFileName, O_RDWR | O_CREAT, 660);
//IFC//#endif
//IFC//        static char errMsg[1024];
//IFC//        if (fd != -1) {
//IFC//            int mmrSize = MMR_SIZE*4;
//IFC//
//IFC//            /* Write the MMR. */
//IFC//            int count   = 0;
//IFC//            int toWrite = mmrSize;
//IFC//            int offset  = 0;
//IFC//            while (toWrite > 0) {
//IFC//                count = write(fd, ((char*)mmr)+offset, toWrite);
//IFC//                if (count == -1) {
//IFC//                    sprintf(errMsg, "Call to write() failed: %s (mmrSize=%d,  written=%d)\n", strerror(errno), mmrSize, mmrSize-toWrite);
//IFC//                    fatalVMError(errMsg);
//IFC//                }
//IFC//                toWrite -= count;
//IFC//                offset  += count;
//IFC//            }
//IFC//
//IFC//            /* Write the image. */
//IFC//            count   = 0;
//IFC//            toWrite = size;
//IFC//            offset  = 0;
//IFC//            while (toWrite > 0) {
//IFC//                count = write(fd, image+offset, size);
//IFC//                if (count == -1) {
//IFC//                    sprintf(errMsg, "Call to write() failed: %s (size=%d,  written=%d)\n", strerror(errno), size, size-toWrite);
//IFC//                    fatalVMError(errMsg);
//IFC//                }
//IFC//                toWrite -= count;
//IFC//                offset  += count;
//IFC//            }
//IFC//            close(fd);
//IFC//        } else {
//IFC//            sprintf(errMsg, "Call to open(%s) failed: %s\n", imageFileName, strerror(errno));
//IFC//            fatalVMError(errMsg);
//IFC//        }
//IFC//   }

   /*---------------------------------------------------------------------------*\
    *                                 Channel I/O                               *
   \*---------------------------------------------------------------------------*/


//IFC//#if 0
    int  cio_execute(int chan, int op, int i1, int i2, int i3, int i4, int i5, int i6, Object send1, Object send2, Object receive) {
        return cio.execute(chan, op, i1, i2, i3, i4, i5, i6, send1, send2, receive);
    }
    long cio_result(int chan)   {   return cio.result(chan);  }
    void cio_waitFor(long time) {   cio.waitFor(time);        }
    int  cio_getEvent()         {   return cio.getEvent();    }
    void freeObject(Object o)   {}

//IFC//#else
//IFC//#define cio_execute(chan, op, i1, i2, i3, i4, i5, i6, o1, o2, o3) (*env)->CallIntMethod(env, cio, MID_execute, chan, op, i1, i2, i3, i4, i5, i6, o1, o2, o3)
//IFC//#define cio_result(chan)  (*env)->CallLongMethod(env, cio, MID_result, chan)
//IFC//#define cio_waitFor(time) (*env)->CallIntMethod(env, cio, MID_waitFor, time)
//IFC//#define cio_getEvent()    (*env)->CallIntMethod(env, cio, MID_getEvent)
//IFC//#define freeObject(o)     (*env)->DeleteGlobalRef(env, o)
//IFC//#endif

/*IFJ*/ChannelIO  cio;
//IFC//JavaVM*    jvm;
//IFC//JNIEnv*    env;
//IFC//jobject    cio;
//IFC//jmethodID  MID_execute;
//IFC//jmethodID  MID_error;
//IFC//jmethodID  MID_result;
//IFC//jmethodID  MID_getEvent;
//IFC//jmethodID  MID_waitFor;

//IFC// void jni_assume(boolean b, String msg) {
//IFC//     if (!b) {
//IFC//         if (msg == null) {
//IFC//             msg = "JNI assume failure";
//IFC//         }
//IFC//         if (env != null && (*env)->ExceptionOccurred(env)) {
//IFC//             (*env)->ExceptionDescribe(env);
//IFC//         }
//IFC//         env = null;
//IFC//         if (jvm != null) {
//IFC//             (*jvm)->DestroyJavaVM(jvm);
//IFC//             jvm = null;
//IFC//         }
//IFC//         fatalVMError(msg);
//IFC//     }
//IFC// }

    /*
     * chan_init
     */
    void chan_init(boolean debug, String vmClassPath) {
/*IFJ*/ cio = new ChannelIO(debug);
//IFC// jint createJVM(JavaVM **jvm, void **env, void *args);
//IFC// JavaVMInitArgs vm_args;
//IFC// JavaVMOption   options[2];
//IFC// jint           res;
//IFC// jclass         clazz;
//IFC// jmethodID      constructorID;
//IFC//
//IFC// char *buf = (char *)malloc(strlen("-Djava.class.path=")+strlen(vmClassPath)+1);
//IFC// strcpy(buf, "-Djava.class.path=");
//IFC// strcat(buf, vmClassPath);
//IFC//
//IFC// options[0].optionString = buf;
//IFC// options[1].optionString = "-verbose";
//IFC// vm_args.version  = JNI_VERSION_1_2;
//IFC// vm_args.options  = options;
//IFC// vm_args.nOptions = 1; /* Set to '2' to make embedded JVM verbose. */
//IFC//
//IFC// //fprintf(stderr, "Starting embedded JVM with options \"%s %s\"\n", options[0].optionString, options[1].optionString);
//IFC//
//IFC// //res = JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args);
//IFC// res = createJVM(&jvm, (void**)&env, &vm_args);
//IFC// jni_assume(res >= 0, "Can't create Java VM\n");
//IFC//
//IFC// clazz = (*env)->FindClass(env, "com/sun/squawk/vm/ChannelIO");
//IFC// jni_assume(clazz != null, "Can't find com.sun.squawk.vm.ChannelIO");
//IFC//
//IFC// MID_execute = (*env)->GetMethodID(env, clazz, "execute", "(IIIIIIIILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)I");
//IFC// jni_assume(MID_execute != null, "Couldn't find method: ChannelIO.execute()");
//IFC//
//IFC// MID_result = (*env)->GetMethodID(env, clazz, "result", "(I)J");
//IFC// jni_assume(MID_result != null, "Couldn't find method: ChannelIO.result()");
//IFC//
//IFC// MID_getEvent = (*env)->GetMethodID(env, clazz, "getEvent", "()I");
//IFC// jni_assume(MID_getEvent != null, "Couldn't find method: ChannelIO.getEvent()");
//IFC//
//IFC// MID_waitFor = (*env)->GetMethodID(env, clazz, "waitFor", "(J)V");
//IFC// jni_assume(MID_waitFor != null, "Couldn't find method: ChannelIO.waitFor()");
//IFC//
//IFC// constructorID = (*env)->GetMethodID(env, clazz, "<init>", "(Z)V");
//IFC// jni_assume(constructorID != null, "Couldn't find method: ChannelIO.<init>(boolean)");
//IFC//
//IFC// cio = (*env)->NewObject(env, clazz, constructorID, debug);
//IFC// jni_assume(cio != null, "Couldn't create ChannelIO object");
    }

    void chan_finalize() {
//IFC// if (jvm != 0) {
//IFC//     (*jvm)->DestroyJavaVM(jvm);
//IFC//     jvm = 0;
//IFC// }
    }

    /*
     * chan_execute
     */
    int chan_execute(int chan, int op, int i1, int i2, int i3, int i4, int i5, int i6, int send1, int send2, int receive) {
        Object s1 = getSendBuffer(send1);
        Object s2 = getSendBuffer(send2);
        Object r1 = getReceiveBuffer(receive);
        int res = cio_execute(chan, op, i1, i2, i3, i4, i5, i6, s1, s2, r1);
        if (s1 != null) freeObject(s1);
        if (s2 != null) freeObject(s2);
        if (r1 != null) {
            setNativeByteArray(receive, r1, i1, i2);
            freeObject(r1);
        }
        updateHighWaterMark();
        return res;
    }

    /*
     * chan_result
     */
    long chan_result(int chan) {
        return cio_result(chan);
    }

    /*
     * waitForEvent
     */
    void waitForEvent(long time) {
        cio_waitFor(time);
    }

    /*
     * getEvent
     */
    int getEvent() {
        return cio_getEvent();
    }

/*IFJ*/}
