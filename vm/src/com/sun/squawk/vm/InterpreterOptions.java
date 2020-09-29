//J2C:options.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;

/*IFJ*/abstract public class InterpreterOptions extends SquawkConstants {


/*---------------------------------------------------------------------------*\
 *                                  Romizing                                 *
\*---------------------------------------------------------------------------*/

/*MAC*/ boolean romizing() {
            return false;
        }

/*---------------------------------------------------------------------------*\
 *                                   charAt                                  *
\*---------------------------------------------------------------------------*/

        int charAt(String str, int pos) {
/*IFJ*/     if (pos == str.length()) {
/*IFJ*/         return 0;
/*IFJ*/     }
/*IFJ*/     return str.charAt(pos);
//IFC//     return str[pos];
        }

/*---------------------------------------------------------------------------*\
 *                               Program arguments                           *
\*---------------------------------------------------------------------------*/

/*IFJ*/ private String[] programArgv;
//IFC// private char   **programArgv;
        private int      programArgc = 0;


        /*
         * setArgv
         */
/*IFJ*/ void setArgv(String[] argv) {
//IFC// void setArgv(char ** argv) {
            programArgv = argv;
        }

        /*
         * addArgument
         */
/*MAC*/ void addArgument(String $arg) {
            programArgv[programArgc++] = $arg;
        }

        /*
         * getArgumentCount
         */
/*MAC*/ int getArgumentCount() {
            return programArgc;
        }

        /*
         * getArgumentChar
         */
/*MAC*/ int getArgumentChar(int $arg, int $pos) {
            String str = programArgv[$arg];
            return charAt(str, $pos);
        }


/*---------------------------------------------------------------------------*\
 *                       Checkpoint image file name                          *
\*---------------------------------------------------------------------------*/

        private String checkPointBase;

        /*
         * setCheckPointBase
         */
/*MAC*/ void setCheckPointBase(String $base) {
            checkPointBase = $base;
        }

        /*
         * getCheckPointBase
         */
/*MAC*/ String getCheckPointBase() {
            return checkPointBase;
        }

/*---------------------------------------------------------------------------*\
 *                              Tracing threshold                            *
\*---------------------------------------------------------------------------*/

//IFC//#ifdef TRACING

//IFC//#ifdef STATS

        abstract long getInstructionCount();
        private  long traceThreshold = 0;

/*MAC*/ void setThreshold(long $value){
            traceThreshold = $value;
        }

/*MAC*/ long getThreshold() {
            return traceThreshold;
        }

/*MAC*/ boolean metThreshold() {
            return traceThreshold <= 0 || traceThreshold <= getInstructionCount();
        }

//IFC//#else
//IFC//#define  setThreshold(x) /**/
//IFC//#define  getThreshold() 0
//IFC//#define  metThreshold() true
//IFC//#endif /* STATS */
//IFC//#else
//IFC//#define  setThreshold(x) /**/
//IFC//#define  getThreshold() 0
//IFC//#define  metThreshold() true
//IFC//#endif /* TRACING */


/*---------------------------------------------------------------------------*\
 *                              Tracing options                              *
\*---------------------------------------------------------------------------*/

        int ticks;

//IFC//#ifdef TRACING

        abstract long getTime();

        private boolean  traceInstructions     = false;
        private boolean  traceMethods          = false;
        private boolean  traceAllocation       = false;
        private boolean  traceFrames           = false;
        private boolean  traceGC               = false;
        private boolean  traceGCVerbose        = false;
        private boolean  traceGCSummary        = false;
        private boolean  traceRAM              = true;
        private boolean  traceEEPROM           = true;
        private boolean  traceMigration        = false;
        private boolean  traceMigrationVerbose = false;
        private int      profileMethodFreq     = -1;


/*MAC*/ boolean     getTraceInstructions()              { return traceInstructions           && metThreshold(); }
/*MAC*/ boolean     getTraceMethods()                   { return traceMethods                && metThreshold(); }
/*MAC*/ boolean     getTraceFrames()                    { return traceFrames                 && metThreshold(); }
/*MAC*/ boolean     getTraceAllocation()                { return traceAllocation             && metThreshold() && traceRAM; }
/*MAC*/ boolean     getTraceGC()                        { return (traceGC || traceGCVerbose) && metThreshold() && traceRAM; }
/*MAC*/ boolean     getTraceGCVerbose()                 { return traceGCVerbose              && metThreshold() && traceRAM; }
/*MAC*/ boolean     getTraceGCSummary()                 { return traceGCSummary              && metThreshold() && traceRAM; }
/*MAC*/ boolean     getTraceEepromAllocation()          { return traceAllocation             && metThreshold() && traceEEPROM; }
/*MAC*/ boolean     getTraceEepromGC()                  { return (traceGC || traceGCVerbose) && metThreshold() && traceEEPROM; }
/*MAC*/ boolean     getTraceEepromGCVerbose()           { return traceGCVerbose              && metThreshold() && traceEEPROM; }
/*MAC*/ boolean     getTraceEepromGCSummary()           { return traceGCSummary              && metThreshold() && traceEEPROM; }
/*MAC*/ boolean     getTraceMigration()                 { return (traceMigration || traceMigrationVerbose) && metThreshold(); }
/*MAC*/ boolean     getTraceMigrationVerbose()          { return traceMigrationVerbose && metThreshold(); }
/*MAC*/ boolean     getTraceAssociations()              { return getTraceGCVerbose();                     }
/*MAC*/ boolean     getTracingAny()                     { return traceInstructions ||
                                                                 traceMethods      ||
                                                                 traceAllocation   ||
                                                                 traceFrames       ||
                                                                 traceGC           ||
                                                                 traceGCVerbose    ||
                                                                 traceGCSummary    ||
                                                                 traceMigration    ||
                                                                 traceMigrationVerbose;}

/*MAC*/ boolean     getProfileMethods()                 { return profileMethodFreq != -1; }


        void startProfileTicker() {
//IFC//     void startTicker(int);
//IFC//     startTicker(profileMethodFreq);
/*IFJ*/     new Thread() {
/*IFJ*/         public void run() {
/*IFJ*/             for (;;) {
/*IFJ*/                 try { Thread.sleep(profileMethodFreq); } catch (InterruptedException ex) {}
/*IFJ*/                 ticks++;
/*IFJ*/             }
/*IFJ*/         }
/*IFJ*/     }.start();
        }


/*MAC*/ int getProfileTicks() {
            return ticks;
        }

/*MAC*/ void zeroProfileTicks() {
            ticks = 0;
        }


/*MAC*/ void        setTraceInstructions(boolean $b)    { traceInstructions     = $b; }
/*MAC*/ void        setTraceMethods(boolean $b)         { traceMethods          = $b; }
/*MAC*/ void        setTraceAllocation(boolean $b)      { traceAllocation       = $b; }
/*MAC*/ void        setTraceFrames(boolean $b)          { traceFrames           = $b; }
/*MAC*/ void        setTraceGC(boolean $b)              { traceGC               = $b; }
/*MAC*/ void        setTraceGCVerbose(boolean $b)       { traceGCVerbose        = $b; }
/*MAC*/ void        setTraceGCSummary(boolean $b)       { traceGCSummary        = $b; }
/*MAC*/ void        setTraceMigration(boolean $b)       { traceMigration        = $b; }
/*MAC*/ void        setTraceMigrationVerbose(boolean $b){ traceMigrationVerbose = $b; }
/*MAC*/ void        setMethodProfileFreq(int $f)        { profileMethodFreq     = $f; }

/*MAC*/ void        traceSegments(boolean $ram, boolean $eeprom) { traceRAM = $ram; traceEEPROM = $eeprom; }

//IFC//#else
//IFC//#define      traceSegment(ram) false
//IFC//#define      getTraceInstructions() false
//IFC//#define      getTraceMethods() false
//IFC//#define      getTraceFrames() false
//IFC//#define      getTraceAllocation() false
//IFC//#define      getTraceGC() false
//IFC//#define      getTraceGCVerbose() false
//IFC//#define      getTraceGCSummary() false
//IFC//#define      getTraceEepromAllocation() false
//IFC//#define      getTraceEepromGC() false
//IFC//#define      getTraceEepromGCVerbose() false
//IFC//#define      getTraceEepromGCSummary() false
//IFC//#define      getTraceMigration() false
//IFC//#define      getTraceMigrationVerbose() false
//IFC//#define      getTraceAssociations() false

//IFC//#define      setTraceInstructions(b)     /**/
//IFC//#define      setTraceMethods(b)          /**/
//IFC//#define      setTraceAllocation(b)       /**/
//IFC//#define      setTraceFrames(b)           /**/
//IFC//#define      setTraceGC(b)               /**/
//IFC//#define      setTraceGCVerbose(b)        /**/
//IFC//#define      setTraceGCSummary(b)        /**/
//IFC//#define      setTraceMigration(b)        /**/
//IFC//#define      setTraceMigrationVerbose(b) /**/
//IFC//#define      traceSegments(ram, eeprom)  /**/
//IFC//#endif /* TRACING */


/*---------------------------------------------------------------------------*\
 *                                Excessive GC                               *
\*---------------------------------------------------------------------------*/


//IFC//#ifdef EXCESSIVEGC
        private boolean  excessiveGC            = false;
        private boolean  veryExcessiveGC        = false;
        private boolean  excessiveGCEnabled     = false;

/*MAC*/ void        setExcessiveGC(boolean $b)          { excessiveGC = $b;                                                 }
/*MAC*/ void        setVeryExcessiveGC(boolean $b)      { veryExcessiveGC = excessiveGC = $b;                               }

/*MAC*/ void        setExcessiveGCEnabled(boolean $b)   { excessiveGCEnabled = $b;                                          }
/*MAC*/ boolean     getExcessiveGCEnabled()             { return excessiveGCEnabled && excessiveGC     /*&& metThreshold()*/; }
/*MAC*/ boolean     getVeryExcessiveGCEnabled()         { return excessiveGCEnabled && veryExcessiveGC /*&& metThreshold()*/; }
//IFC//#else
//IFC//#define      setExcessiveGC(x)           /**/
//IFC//#define      setExcessiveGCEnabled(x)    /**/
//IFC//#define      getExcessiveGCEnabled()     false
//IFC//#define      getVeryExcessiveGCEnabled() false
//IFC//#endif

/*---------------------------------------------------------------------------*\
 *                                   Full GC                                 *
\*---------------------------------------------------------------------------*/

        private boolean fullGC       = false;
        private int     youngPercent = 10;

/*MAC*/ void        setFullGC(boolean $b)               { fullGC = $b;                                                      }
/*MAC*/ void        setYoungPercent(int $n)             { youngPercent = $n;                                                }

/*MAC*/ boolean     getFullGC()                         { return fullGC;                                                    }
/*MAC*/ int         getYoungPercent()                   { return youngPercent;                                              }

/*IFJ*/ }
