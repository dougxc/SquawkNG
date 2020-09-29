package com.sun.squawk.translator;

import com.sun.squawk.util.*;
import com.sun.squawk.translator.util.BufferedReader;
import com.sun.squawk.translator.util.JVMConst;
import com.sun.squawk.translator.util.ComputationTimer;
import com.sun.squawk.translator.util.Tracer;
import com.sun.squawk.translator.util.ArrayHashtable;
import com.sun.squawk.translator.util.SortableVector;
import com.sun.squawk.translator.util.Comparer;
import com.sun.squawk.translator.loader.*;
import com.sun.squawk.translator.ir.*;
import com.sun.squawk.translator.suite.*;
import com.sun.squawk.vm.CNO;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import com.sun.squawk.io.connections.ClasspathConnection;

/**
 * This is the translator which produces Java application suites suitable for
 * execution on the Squawk VM. A suite is a
 * transitive closure of a set of classes. Each class may be represented
 * a proxy class or a complete class, the primary difference being that
 * only the latter includes method bodies.
 *
 * A suite request is made for one of two reasons:
 *
 *   1. To build a suite for installation on a JavaCard.
 *   2. In response to a "Class.forName()" request from an executing process.
 *
 * each of which is detailed below.
 *
 * 1. A request is specified solely by a special class called a 'driver'
 * class. This class must have a field matching the following declaration:
 *
 *     final static String field named "__SUITE_CLASSES__" = ...
 *
 * and and may have optionally have these other fields:
 *
 *     final static int    __SUITE_FLAGS__ = ...
 *
 * Each field must have a ConstantValue attribute in the constant pool of the
 * class. The driver class may have no other methods or fields, must not implement
 * any interfaces and must subclass java.lang.Object.
 *
 * The value of the __SUITE_CLASSES__ field is parsed as a list of space-separated
 * class specifiers. A class specifier is one of the following:
 *
 *   a. A class name (e.g. "java.lang.Thread").
 *   b. A package root (e.g. "java.lang/"). This will include all the classes
 *      in the package "java.lang.*" found on the given classpath.
 *   c. A recursive package root (e.g. "java//"). This will include all the classes
 *      in any package whose first compo+
 * nent is "java.".
 *
 * The suite is then determined to be the transitive closure of the __SUITE_CLASSES__.
 * The classes within a suite are partitioned into proxy classes and non-proxy
 * classes.
 *
 * The value of the __SUITE_FLAGS__ field is parsed as a list of space-separated
 * suite attribute names. The recognized attributes and their semantics are:
 *
 *   Name        | Interpretation
 *   ------------+-------------------------------------------------
 *   final       | The suite is final and cannot be linked against.
 *   reflective  | The members of the classes may be accessed via
 *               | reflection.
 *
 * All array classes are treated as application classes.
 */
public class Translator {

   /* ----------------------------------------------------------------------- *\
    *        CLDC compliant version of some constants in java.io.File          *
   \* ----------------------------------------------------------------------- */

    /**
     * The system-dependent default name-separator character.  This field is
     * initialized to contain the first character of the value of the system
     * property <code>file.separator</code>.  On UNIX systems the value of this
     * field is <code>'/'</code>; on Microsoft Windows systems it is <code>'\'</code>.
     *
     * @see     java.lang.System#getProperty(java.lang.String)
     */
    public static final char separatorChar;

    /**
     * The system-dependent default name-separator character, represented as a
     * string for convenience.  This string contains a single character, namely
     * <code>{@link #separatorChar}</code>.
     */
    public static final String separator;

    /**
     * The system-dependent path-separator character.  This field is
     * initialized to contain the first character of the value of the system
     * property <code>path.separator</code>.  This character is used to
     * separate filenames in a sequence of files given as a <em>path list</em>.
     * On UNIX systems, this character is <code>':'</code>; on Microsoft Windows systems it
     * is <code>';'</code>.
     *
     * @see     java.lang.System#getProperty(java.lang.String)
     */
    public static final char pathSeparatorChar;

    /**
     * The system-dependent path-separator character, represented as a string
     * for convenience.  This string contains a single character, namely
     * <code>{@link #pathSeparatorChar}</code>.
     */
    public static final String pathSeparator;

    public static final String lineSeparator;

    static {
        String property = System.getProperty("file.separator");
        if (property == null) {
            separatorChar = '/';
            separator = "/";
        } else {
            separatorChar = property.charAt(0);
            separator = property;
        }
        property = System.getProperty("path.separator");
        if (property == null) {
            pathSeparatorChar = ':';
            pathSeparator = ":";
        } else {
            pathSeparatorChar = property.charAt(0);
            pathSeparator = property;
        }
        property = System.getProperty("line.separator");
        if (property == null) {
            lineSeparator = "\n";
        } else {
            lineSeparator = property;
        }
    }

    /* ---------------------------------------------------------------------- *\
     *                          Other constants                               *
    \* ---------------------------------------------------------------------- */

    /**
     * Specifies whether or not longs are treated as 2 individual words.
     */
    public static final boolean LONGSARETWOWORDS = true;

    /**
     * The name of the special field in the root class of a translation that
     * lists other classes that are to be included in the translation suite
     * built for the root class.
     */
    public final String SUITE_CLASSES_FIELD_NAME;
    public final String SUITE_FLAGS_FIELD_NAME;


    /** Constants for zero length arrays of common types. */
    public final Type[]   ZEROTYPES;
    public static final Field[]  ZEROFIELDS  = new Field[0];
    public static final Method[] ZEROMETHODS = new Method[0];
    public static final Object[] ZEROOBJECTS = new Object[0];
    public static final Instruction[] ZEROINSTRUCTIONS = new Instruction[0];

    final public Type[] PARAMS_FOR_MAIN;

   /* ------------------------------------------------------------------------ *\
    *                      Per-translator instance constants                   *
   \* ------------------------------------------------------------------------ */

    /**
     * Interned instances of well known and fixed field and method names.
     */
    public final String MAIN, INIT, CLINIT, SQUAWK_INIT, SQUAWK_DUMMY, UNTRANSLATABLECODEERROR;

   /* ------------------------------------------------------------------------ *\
    *                      Types special to the translator                     *
   \* ------------------------------------------------------------------------ */

    /**
     * The route of all data types.
     */
    public final Type UNIVERSE;

    /**
     * Primitive data types.
     */
    public final Type PRIMITIVE, BOOLEAN, BYTE, CHAR, SHORT, INT, LONG, VOID;
/*if[FLOATS]*/
    public final Type FLOAT, DOUBLE;
/*end[FLOATS]*/

    /**
     * Objects.
     */
    public final Type OBJECT, STRING, CLASS;

    /** Special objects used by the verifier. */
    public final Type BOGUS, NULLOBJECT, INITOBJECT, NEWOBJECT, LONG2;
/*if[FLOATS]*/
    public final Type DOUBLE2;
/*end[FLOATS]*/

    /** These are required to model the type ambiguity of 'baload' and 'bastore'. */
    public final Type BYTE_OR_BOOLEAN, BYTE_OR_BOOLEAN_ARRAY;

    /**
     * Throwables.
     */
    public final Type THROWABLE, ERROR, EXCEPTION, ILLEGALACCESSEXCEPTION;

    /**
     * Arrays.
     */
    public final Type BOOLEAN_ARRAY, BYTE_ARRAY, CHAR_ARRAY, SHORT_ARRAY, INT_ARRAY,
                       LONG_ARRAY, OBJECT_ARRAY, STRING_ARRAY, NULLOBJECT_ARRAY;
/*if[FLOATS]*/
    public final Type FLOAT_ARRAY, DOUBLE_ARRAY;
/*end[FLOATS]*/

    /**
     * Late arrivals.
     */
    public final Type STRING_OF_BYTES, STRING_OF_SYMBOLS, RESERVED3, RESERVED4;


    /**
     * Class loading errors.
     */
    public final Type LINKAGEERROR,
                           VERIFYERROR,
                           CLASSFORMATERROR,
                           NOCLASSDEFFOUNDERROR,
                           CLASSCIRCULARITYERROR,
                           UNSATISFIEDLINKERROR,
                           INCOMPATIBLECLASSCHANGEERROR,
                               ABSTRACTMETHODERROR,
                               ILLEGALACCESSERROR,
                               INSTANTIATIONERROR,
                               NOSUCHFIELDERROR,
                               NOSUCHMETHODERROR;

    /** Tracing flags. */
    public final static int TRACE_LOADING       = 1<<0;
    public final static int TRACE_RAW           = 1<<1;
    public final static int TRACE_BYTECODES     = 1<<2;
    public final static int TRACE_IR0           = 1<<3;
    public final static int TRACE_IR1           = 1<<4;
    public final static int TRACE_CLASSINFO     = 1<<5;
    public final static int TRACE_LOCALS        = 1<<7;
    public final static int TRACE_IP            = 1<<8;
    public final static int TRACE_DEAD_CODE     = 1<<9;
    public final static int TRACE_LIVENESSDFA   = 1<<10;
    public final static int TRACE_PRUNING       = 1<<11;

    /** How to deal with LinkageErrors. */
    public final static int LINKAGEERRORS_TRACE = 1<<12;
    public final static int LINKAGEERRORS_EXIT  = 1<<13;

    /** Optimization flags. */
    public final static int OPT_LOCALSINIT      = 1<<14;
    public final static int OPT_LOCALSALLOC     = 1<<15;
    public final static int OPT_THISFIELDACCESS = 1<<16;

    public final static int OPT_ALL             = OPT_LOCALSINIT | OPT_LOCALSALLOC | OPT_THISFIELDACCESS;

    /** Statistics flags. */
    public final static int STATS_ON            = 1<<17;
/*if[FLOATS]*/
    public final static int STATS_FLOATDOUBLE   = 1<<18;
/*end[FLOATS]*/
    public final static int STATS_LONG          = 1<<19;

    /* Name style flags */
    public final static int NAMES_FQN           = 1<<20;
    public final static int NAMES_SRC           = 1<<21;
    public final static int NAMES_PARENT        = 1<<22;

    public final static int NOSTACKCONSTRAINT   = 1<<23;
    public final static int NOGCSTACKCONSTRAINT = 1<<24;
    public final static int NOMINIMALVM         = 1<<25;

    /* Suite stuff */
    public final static int PRUNE_SUITE         = 1<<26;

    /** A flag denoting whether or not the suite under translation is public final or not. Default = false */
    public final static int FINAL_SUITE         = 1<<27;

    /** A flag denoting whether or not the suite under translation can be reflected upon. Default = true */
    public final static int REFLECTIVE_SUITE    = 1<<28;

    /* Suite public finalization */
    public final static int IGNOREFINAL         = 1<<29;

    /** A flag denoting whether the translator is in the load & resolve proxy class phase
        or the load & resolve suite class phase. */
    public final static int RESOLVING_PROXIES   = 1<<30;

    public final static int VERIFY_ONLY         = 1<<31;
    public boolean verifyingOnly() { return (flags & VERIFY_ONLY) != 0; }

   /* ------------------------------------------------------------------------ *\
    *                              I n s t a n c e                             *
   \* ------------------------------------------------------------------------ */


    /** The class loader for this VM. */
    final private ClassFileLoader loader;
    /** The list of suite dependencies/ */
    final Vector suiteDependencyNames = new Vector();
    /** These variables modify the behaviour of translation. */
    private int flags = 0;
    /** Tracing filter. */
    private String match;
    /** Recursion guard for verification. */
    int verifyCount;

    public boolean optimizeLocalInitializers() { return (flags & OPT_LOCALSINIT) != 0;      }
    public boolean optimizeLocalsAllocation()  { return (flags & OPT_LOCALSALLOC) != 0;     }
    public boolean optimizeThisFieldAccess()   { return (flags & OPT_THISFIELDACCESS) != 0; }

    public boolean statsOn()                   { return (flags & STATS_ON) != 0;            }
/*if[FLOATS]*/
    public boolean statsFloatDouble()          { return (flags & STATS_FLOATDOUBLE) != 0;   }
/*end[FLOATS]*/
    public boolean statsLong()                 { return (flags & STATS_LONG) != 0;          }
    public boolean constrainStack()            { return (flags & NOSTACKCONSTRAINT) == 0;   }
    public boolean constrainStackForGC()       { return (flags & NOGCSTACKCONSTRAINT) == 0; }

    public boolean minimalVM()                 { return (flags & NOMINIMALVM) == 0;         }
    public boolean ignorefinal()               { return (flags & IGNOREFINAL) != 0;         }

    private ClasspathConnection openClassPath(String classPath) {
        try {
            return (ClasspathConnection)Connector.open("classpath://"+classPath);
        } catch (IOException ioe) {
            throw new RuntimeException("Could not open class path connection for '"+classPath+"': "+ioe.getMessage());
        }
    }

    /**
     * Constructor.
     */
    public Translator(String deps, String classPath, int traceFlags, String match, ComputationTimer timer) throws LinkageException {
        this.flags = traceFlags;
        this.match = match;
        this.timer = timer;
        Assert.that(classPath != null && classPath.length() > 0);
        loader = ClassFileLoader.create(this, openClassPath(classPath));
        internedStrings = new ArrayHashtable(64);

        boolean floatsSupported = false;
/*if[FLOATS]*/
        floatsSupported = true;
/*end[FLOATS]*/

        flags |= REFLECTIVE_SUITE;

        // The translator resolves proxies first
        flags |= RESOLVING_PROXIES;

        ZEROTYPES     = internTypeList(new Type[0]);
        MAIN          = internString("main");
        INIT          = internString("<init>");
        CLINIT        = internString("<clinit>");
        SQUAWK_INIT   = internString("_SQUAWK_INTERNAL_init");
        SQUAWK_DUMMY  = internString("_SQUAWK_INTERNAL_dummy");

        SUITE_CLASSES_FIELD_NAME        = internString("__SUITE_CLASSES__");
        SUITE_FLAGS_FIELD_NAME          = internString("__SUITE_FLAGS__");

        // The super type of all data types
        UNIVERSE        = createType(null,         "-U-", -2);

        // Unreal data types
        BOGUS           = createType(UNIVERSE,     "-X-", -2);         // An invalid type

        // Objects
        OBJECT          = createType(UNIVERSE,     "Ljava/lang/Object;",         CNO.OBJECT);
        STRING          = createType(OBJECT,       "Ljava/lang/String;",         CNO.STRING);
        THROWABLE       = createType(OBJECT,       "Ljava/lang/Throwable;",      CNO.THROWABLE);
        CLASS           = createType(OBJECT,       "Ljava/lang/Class;",          CNO.CLASS);

        // Primitive data types
        PRIMITIVE       = createType(OBJECT,       "Ljava/lang/_primitive_;",    CNO.PRIMITIVE);
        BYTE_OR_BOOLEAN = createType(PRIMITIVE,    "Ljava/lang/_byte_or_boolean_;", -2);
        VOID            = createType(PRIMITIVE,    "Ljava/lang/_void_;",         CNO.VOID);
        BOOLEAN         = createType(BYTE_OR_BOOLEAN, "Ljava/lang/_boolean_;",   CNO.BOOLEAN);
        BYTE            = createType(BYTE_OR_BOOLEAN, "Ljava/lang/_byte_;",      CNO.BYTE);
        CHAR            = createType(PRIMITIVE,    "Ljava/lang/_char_;",         CNO.CHAR);
        SHORT           = createType(PRIMITIVE,    "Ljava/lang/_short_;",        CNO.SHORT);
        INT             = createType(PRIMITIVE,    "Ljava/lang/_int_;",          CNO.INT);
        LONG            = createType(PRIMITIVE,    "Ljava/lang/_long_;",         CNO.LONG);
        LONG2           = createType(PRIMITIVE,    "Ljava/lang/_long2_;",        CNO.LONG2);
/*if[FLOATS]*/
        FLOAT           = createType(PRIMITIVE,    "Ljava/lang/_float_;",        CNO.FLOAT);
        DOUBLE          = createType(PRIMITIVE,    "Ljava/lang/_double_;",       CNO.DOUBLE);
        DOUBLE2         = createType(PRIMITIVE,    "Ljava/lang/_double2_;",      CNO.DOUBLE2);
/*end[FLOATS]*/
        if (!floatsSupported) {
            loadedTypes.addElement(null);
            loadedTypes.addElement(null);
            loadedTypes.addElement(null);
        }

        // Special objects used by the verifier
        NULLOBJECT      = createType(OBJECT,       "-NULL-",  -2);      // Result of an aconst_null
        INITOBJECT      = createType(OBJECT,       "-INIT-",  -2);      // "this" in <init> before call to super()
        NEWOBJECT       = createType(OBJECT,       "-NEW-",   -2);      // Result of "new" before call to <init>

        // Arrays
        BYTE_OR_BOOLEAN_ARRAY = createArrayType(BYTE_OR_BOOLEAN, -2);
        OBJECT_ARRAY    = createArrayType(OBJECT,      CNO.OBJECT_ARRAY);
        NULLOBJECT_ARRAY= createArrayType(NULLOBJECT,  -2);
        STRING_ARRAY    = createArrayType(STRING,      CNO.STRING_ARRAY);
        BOOLEAN_ARRAY   = createArrayType(BOOLEAN,     CNO.BOOLEAN_ARRAY);
        BYTE_ARRAY      = createArrayType(BYTE,        CNO.BYTE_ARRAY);
        CHAR_ARRAY      = createArrayType(CHAR,        CNO.CHAR_ARRAY);
        SHORT_ARRAY     = createArrayType(SHORT,       CNO.SHORT_ARRAY);
        INT_ARRAY       = createArrayType(INT,         CNO.INT_ARRAY);
        LONG_ARRAY      = createArrayType(LONG,        CNO.LONG_ARRAY);
/*if[FLOATS]*/
        FLOAT_ARRAY     = createArrayType(FLOAT,       CNO.FLOAT_ARRAY);
        DOUBLE_ARRAY    = createArrayType(DOUBLE,      CNO.DOUBLE_ARRAY);
/*end[FLOATS]*/
        if (!floatsSupported) {
            loadedTypes.addElement(null);
            loadedTypes.addElement(null);
        }

        // Reserved section
        STRING_OF_BYTES   = createType(STRING,          "Ljava/lang/StringOfBytes;",      CNO.STRING_OF_BYTES);

        STRING_OF_SYMBOLS = createType(
/*if[OLDSYMBOLS]*/
                OBJECT,
/*end[OLDSYMBOLS]*/
/*if[NEWSYMBOLS]*/
                STRING_OF_BYTES,
/*end[NEWSYMBOLS]*/
                "Ljava/lang/StringOfSymbols;",    CNO.STRING_OF_SYMBOLS);


        RESERVED3         = createType(OBJECT,          "Ljava/lang/Reserved3;",          CNO.RESERVED3);
        RESERVED4         = createType(OBJECT,          "Ljava/lang/Reserved4;",          CNO.RESERVED4);

        // Throwables
        ERROR           = createType(THROWABLE,    "Ljava/lang/Error;",          -1);
        EXCEPTION       = createType(THROWABLE,    "Ljava/lang/Exception;",      -1);

        // Class loading exceptions. All the subclasses of LINKAGEERROR are actually just aliases for LINKAGEERROR
        // until these classes are supported as part of the API.
        LINKAGEERROR                   = createType(ERROR,           "Ljava/lang/LinkageError;",                     -1);
        VERIFYERROR                    = createType(LINKAGEERROR,    "Ljava/lang/VerifyError;",                      -2);
        CLASSFORMATERROR               = createType(LINKAGEERROR,    "Ljava/lang/ClassFormatError;",                 -2);
        NOCLASSDEFFOUNDERROR           = createType(LINKAGEERROR,    "Ljava/lang/NoClassDefFoundError;",             -1);
        CLASSCIRCULARITYERROR          = createType(LINKAGEERROR,    "Ljava/lang/ClassCircularityError;",            -2);
        UNSATISFIEDLINKERROR           = createType(LINKAGEERROR,    "Ljava/lang/UnsatisfiedLinkError;",             -2);
        INCOMPATIBLECLASSCHANGEERROR   = createType(LINKAGEERROR,    "Ljava/lang/IncompatibleClassChangeError;",     -2);
        ABSTRACTMETHODERROR            = createType(INCOMPATIBLECLASSCHANGEERROR, "Ljava/lang/AbstractMethodError;", -2);
        ILLEGALACCESSERROR             = createType(INCOMPATIBLECLASSCHANGEERROR, "Ljava/lang/IllegalAccessError;",  -2);
        INSTANTIATIONERROR             = createType(INCOMPATIBLECLASSCHANGEERROR, "Ljava/lang/InstantiationError;",  -2);
        NOSUCHFIELDERROR               = createType(INCOMPATIBLECLASSCHANGEERROR, "Ljava/lang/NoSuchFieldError;",    -2);
        NOSUCHMETHODERROR              = createType(INCOMPATIBLECLASSCHANGEERROR, "Ljava/lang/NoSuchMethodError;",   -2);


        UNTRANSLATABLECODEERROR        = internString("UntranslatableCodeError");
        ILLEGALACCESSEXCEPTION         = createType(EXCEPTION,    "Ljava/lang/IllegalAccessException;", -1);




        PARAMS_FOR_MAIN = internTypeList(new Type[] { STRING });
    }

   /* ------------------------------------------------------------------------ *\
    *                           Translation modes                              *
   \* ------------------------------------------------------------------------ */

    public boolean resolvingProxies()  { return (flags & RESOLVING_PROXIES) != 0;  }
    public boolean isSuiteFinal()      { return (flags & FINAL_SUITE) != 0;        }
    public boolean isSuiteReflective() { return (flags & REFLECTIVE_SUITE) != 0;   }
    public boolean pruneSuite()        { return (flags & PRUNE_SUITE) != 0;        }

    public boolean isReflectiveMethod(Method m) {
        if (m.name() == MAIN) {
//System.out.println("isReflectiveMethod "+m.name() + " m.getParms()="+m.getParms()+ " PARAMS_FOR_MAIN=" +PARAMS_FOR_MAIN);
//            return m.getParms() == PARAMS_FOR_MAIN;
            return true;
        }
        if (m.name() == CLINIT) {
            return m.getParms() == ZEROTYPES;
        }
        if (m.name() == INIT) {
            return m.getParms() == ZEROTYPES;
        }
        return false;
    }

    public boolean isReflectiveField(Field f) {
        return false;
    }

   /* ------------------------------------------------------------------------ *\
    *                                Tracing                                   *
   \* ------------------------------------------------------------------------ */

    /** A Tracing object that can be used by the components of the Translator. */
    private final Tracer tracer = new Tracer(System.err, true);

    public Tracer tracer() {
        return tracer;
    }

    private boolean matches(String matchData) {
        if (match == null) {
            return true;
        }
        return matchData.indexOf(match) >= 0;
    }

    public boolean traceloading(String matchData)     { return (flags & TRACE_LOADING) != 0 && matches(matchData);     }
    public boolean traceraw(String matchData)         { return (flags & TRACE_RAW) != 0 && matches(matchData);         }
    public boolean tracebytecodes(String matchData)   { return (flags & TRACE_BYTECODES) != 0 && matches(matchData);   }
    public boolean traceir0(String matchData)         { return (flags & TRACE_IR0) != 0 && matches(matchData);         }
    public boolean traceir1(String matchData)         { return (flags & TRACE_IR1) != 0 && matches(matchData);         }
    public boolean traceclassinfo(String matchData)   { return (flags & TRACE_CLASSINFO) != 0 && matches(matchData);   }
    public boolean traceip()                          { return (flags & TRACE_IP) != 0;                                }
    public boolean tracelocals(String matchData)      { return (flags & TRACE_LOCALS) != 0 && matches(matchData);      }
    public boolean tracelivenessDFA(String matchData) { return (flags & TRACE_LIVENESSDFA) != 0 && matches(matchData); }
    public boolean tracepruning()                     { return (flags & TRACE_PRUNING) != 0;                           }
    public boolean tracelinkageerrors()               { return (flags & LINKAGEERRORS_TRACE) != 0;                     }
    public boolean tracedeadcode()                    { return (flags & TRACE_DEAD_CODE) != 0;                         }
    public boolean namesUseFqn()                      { return (flags & NAMES_FQN) != 0;                               }
    public boolean namesUseSrc()                      { return (flags & NAMES_SRC) != 0;                               }
    public boolean namesUseParent()                   { return (flags & NAMES_PARENT) != 0;                            }
    public boolean exitOnLinkageError()               { return (flags & LINKAGEERRORS_EXIT) != 0;                      }

    public ClassFileLoader loader() {
        return loader;
    }

   /* ------------------------------------------------------------------------ *\
    *                         Suite pruning                                    *
   \* ------------------------------------------------------------------------ */

    public int markDepth;
    public void traceMark(Object o) {
        if (tracepruning()) {
            for (int i = 0; i != markDepth; i++) {
                System.err.print("  ");
            }
            System.err.println("mark: "+((o instanceof Type) ? "class " : (o instanceof Method ? "method " : "field "))+o);
        }
    }

   /* ------------------------------------------------------------------------ *\
    *                     Class loading and initialization                     *
   \* ------------------------------------------------------------------------ */

    /** Flag indicating whether or not a conversion is in progress. */
    private Type conversionInProgress = null;
    /** Queue for pending conversions. */
    private final Vector conversionQueue = new Vector();

    /**
     * Execute or queue a type's conversion.
     */
    public void convert(final Type type) throws LinkageException {
        if (conversionInProgress == null) {
            try {
                conversionInProgress = type;
                time("conversion", new ComputationTimer.ComputationException() {
                    public Object run() throws Exception {
                        while(true) {
                            Type nextType = type;
                            nextType.doConversion();
                            int size = conversionQueue.size();
                            if (size == 0) {
                                break;
                            }
                            nextType = (Type)conversionQueue.lastElement();
                            conversionQueue.removeElementAt(size - 1);
                        }
                        return null;
                    }
                });
            } catch (LinkageException le) {
                throw le;
            } catch (Exception e) {
                e.printStackTrace();
                Assert.shouldNotReachHere();
                return;
            } finally {
                conversionInProgress = null;
            }
        } else {
            if (!conversionQueue.contains(type)) {
                conversionQueue.addElement(type);
            }
        }
    }

    /**
     * Loads a type from it classfile into this VM.
     */
    public Type load(final Type type) throws LinkageException {
        Assert.that(!loadedTypes.contains(type) || isInitType(type));
        try {
            return (Type)time("loading", new ComputationTimer.ComputationException() {
                public Object run() throws Exception {
                    return loader.load(type);
                }
            });
        } catch (LinkageException le) {
            throw le;
        } catch (Exception e) {
            e.printStackTrace();
            Assert.shouldNotReachHere();
            return null;
        }
    }

    /**
     * Notify the translator that a class has finished loading. This is used to maintain
     * an ordering of dependencies between classes.
     */
    public void typeLoaded(Type type) {
        Assert.that(!loadedTypes.contains(type) || isInitType(type));
        if (!isInitType(type)) {
            loadedTypes.addElement(type);
        }
    }

    private boolean isInitType(Type type) {
        return type.specID() != 0;
    }

   /* ------------------------------------------------------------------------ *\
    *                     Translation methods                                  *
   \* ------------------------------------------------------------------------ */

    /**
     * This class enscapsulates the state produced during the translation
     * of a single suite.
     */
    public static class Result {

        public final Type[] proxyClasses;
        public final Type[] suiteClasses;
        public final Statistics statistics;

        Result(Type[] proxyClasses, Type[] suiteClasses, Statistics statistics) {
            this.proxyClasses = proxyClasses;
            this.suiteClasses = suiteClasses;
            this.statistics   = statistics;
        }
    }


   /**
    * Translate a request into a suite.
    * @param libs The files containing the suites against which this suite will
    * be linked.
    * @param classPath the classpath used to find the suite classes.
    * @param flags The traceFlags modifying the tracing behaviour of this translation.
    * @param match A simple string filter than can be applied to the trace output.
    * @param suite The suite root class or driver.
    * @param emitter The SuiteEmitter that should be used to emit the suite.
    * @param isForNameRequest
    */
    public static Result translate(String libs,
                                       String classPath,
                                       int flags,
                                       String match,
                                       ComputationTimer timer,
                                       String suite,
                                       SuiteEmitter emitter,
                                       boolean isForNameRequest) throws LinkageException
    {
        Translator translator = new Translator(libs, classPath, flags, match, timer);
        return translator.run(suite, libs, emitter, isForNameRequest);
    }

    /**
     * Convert a class name into its internal form. E.g.
     *
     *     "java.lang.Object"    ->  "Ljava/lang/Object;"
     *     "[[[java.lang.Object" ->  "[[[Ljava/lang/Object;"
     *
     * Note that this does not handle the single character primitive type names.
     */
    public static String toInternalForm(String className) {
        int dims = countDimensions(className);
        String name = className.substring(dims);
        if (name.charAt(name.length() - 1) != ';') {
            name = "L" + name + ';';
        }
        if (dims != 0) {
            name = className.substring(0, dims) + name;
        }
        return name.replace('.', '/');
    }

    /**
     * Find all the '.class' files on a given path, convert them to class
     * names in external form and add them to a given Vector.
     * @param path The path to search which must end with one or two '/'s. If it
     * ends with one, the search is limited to the denoted directory otherwise
     * it also includes (recursively) all the subdirectories of the denoted
     * directory.
     * @param classes The Vector to which the found classes will be added.
     */
    void findClasses(String path, Vector classes, ClasspathConnection cp) {
        try {
            DataInputStream dis = new DataInputStream(cp.openInputStream(path));
            try {
                for (;;) {
                    String name = dis.readUTF();
                    if (name.endsWith(".class")) {
                        String className = name.substring(0, name.length() - 6).replace('/', '.');
                        classes.addElement(className);
                    }
                }
            } catch (EOFException ex) {
            }
            dis.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Process a suite driver class.
     * @param type The suite driver class.
     * @param suiteClasses The list into which the suite classes will be added.
     * @param recursing
     */
    private void processDriverClass(Type type, Vector suiteClasses, boolean recursing) {
        if (type.superClass() != OBJECT) {
            throw new RuntimeException("Suite driver class ("+type+") must subclass java.lang.Object, not " + type.superClass());
        }
        if (type.getInterfaces().length != 0) {
            throw new RuntimeException("Suite driver class ("+type+") must not implement any interfaces");
        }
        if (type.getMethods(false).length != 0) {
            throw new RuntimeException("Suite driver class ("+type+") must not define any virtual methods");
        }
        if (type.getMethods(true).length != 1) {
            throw new RuntimeException("Suite driver class ("+type+") must only define a default constructor");
        }
        Field[] fields = type.getFields(true);
        if (fields.length == 0) {
            throw new RuntimeException("Suite driver class ("+type+") must at least define a field name \""+SUITE_CLASSES_FIELD_NAME+"\"");
        }
        for (int i = 0; i != fields.length; i++) {
            Field f = fields[i];
            if (!(f.isPrivate() && f.isStatic() && f.isFinal())) {
                throw new RuntimeException("Suite driver ("+type+") fields must be private static and final: "+f);
            }
            if (f.name() == SUITE_CLASSES_FIELD_NAME && f.type() == STRING) {
                String value = (String)f.getConstantValue();
                if (value == null) {
                    throw new RuntimeException(f+" must have an initial value");
                }
                StringTokenizer st = new StringTokenizer(value);
                while (st.hasMoreTokens()) {
                    String entry = st.nextToken();
                    // If the entry ends in '/' then it denotes a group of classes in a
                    // similiar manner to import statements that end in '*'. If it ends
                    // with '//' then it is a recursive import.
                    if (entry.charAt(entry.length() - 1) == '/') {
                        findClasses(entry, suiteClasses, loader.getClassPath());
                    }
                    else {
                        suiteClasses.addElement(entry);
                    }
                }
            }
            else if (f.name() == SUITE_FLAGS_FIELD_NAME && f.type() == STRING) {
                String value = (String)f.getConstantValue();
                if (value == null) {
                    throw new RuntimeException(f+" must have an initial value");
                }
                if (!recursing) {
                    if (value.indexOf("final") != -1 && !ignorefinal()) {
                        this.flags |= FINAL_SUITE;
                    } else {
                        this.flags &= ~FINAL_SUITE;
                    }
                    if (value.indexOf("reflective") != -1) {
                        this.flags |= REFLECTIVE_SUITE;
                    } else {
                        this.flags &= ~REFLECTIVE_SUITE;
                    }
                }
            }
            else {
                throw new RuntimeException("Suite driver class has illegal field: "+f);
            }
        }
    }

    /**
     * Load the closure of classes for a given set of root classes.
     * @param roots The root classes.
     * @param isProxy True if loading proxy classes.
     */
    private void loadClosure(Vector roots, boolean isProxy) {
        // Create a Type for all the roots.
        Enumeration e = roots.elements();
        while (e.hasMoreElements()) {
            String name = toInternalForm((String)e.nextElement());
            Type type = findOrCreateType(name);
            if (!isProxy && type.isProxy()) {
                throw new RuntimeException("Class specified as suite class is also a library class: "+type);
            }
        }

        // Compute the closure
        boolean change;
        int converted = 0;
        do {
            change = false;
            e = internedTypes.elements();
            while (e.hasMoreElements()) {
                Object o = e.nextElement();
                if (o instanceof Type) {
                    Type type = (Type)o;
                    if (!type.isUnreal() && type.getState().compareTo(Type.CONVERTING) < 0) {
                        change = true;
                        try {
                            type.load();
                            type.convert();
                            converted++;
                        } catch (LinkageException le) {
                            type.setLinkageError(le);
                        } catch (AssertionFailed ae) {
                            // Give a rough count of how many classes remain to
                            // be converted - useful during TCK testing
                            e = internedTypes.elements();
                            int remaining = 0;
                            while (e.hasMoreElements()) {
                                o = e.nextElement();
                                if (o instanceof Type) {
                                    type = (Type)o;
                                    if (!type.isUnreal()) {
                                        if (type.getState().compareTo(Type.CONVERTED) < 0) {
                                            remaining++;
                                        }
                                    }
                                }
                            }
                            ae.addContext(converted+"/"+(converted+remaining)+" classes converted");
                            throw ae;
                        }
                    }
                }
            }
        } while (change);
    }

    /**
     * Load and resolve the libraries upon which the suite currently being
     * translated depends.
     * @param libs
     * @throws LinkageException
     */
    private void loadAndResolveLibraries(String libs) throws LinkageException {
        // Load the libraries (if any)
        if (libs != null && libs.length() != 0) {
            StringTokenizer st = new StringTokenizer(libs, ":");
            while (st.hasMoreTokens()) {
                String name = st.nextToken();
                if (name.endsWith(".xml") || name.endsWith(".bin")) {
                    loadLibraryFromSuiteFile(name);
                } else if (name.endsWith(".zip") || name.endsWith(".jar")) {
                    loadLibraryFromJarFile(name);
                } else {
                    throw new RuntimeException("Library file name must end with '.xml', '.bin', '.zip' or '.jar': "+name);
                }
            }
        } else {
            // Undo the 'proxiness' of the bootstrap types
            for (Enumeration e = internedTypes.elements(); e.hasMoreElements();) {
                Object o = e.nextElement();
                if (o instanceof Type) {
                    Type type = (Type)o;
                    type.unsetFlag(JVMConst.ACC_PROXY);
                }
            }
        }

        // Any types created from here on are not library classes
        this.flags &= ~RESOLVING_PROXIES;
    }

    /**
     * Load and resolves the classes of the suite currently being translated.
     * @param suite The suite root class or driver name.
     * @return
     */
    private String loadAndResolveSuiteClasses(String suite,
                                              boolean isForNameRequest) throws LinkageException {
        String suiteName;
        Vector suiteClassNames = new Vector();

        if (!isForNameRequest) {
            String driverClassName = suite;
            suiteName = driverClassName;
            Type suiteDriver = findOrCreateType(toInternalForm(driverClassName));
            suiteDriver.load();
            processDriverClass(suiteDriver, suiteClassNames, false);
        }
        else {
            suiteName = suite;
            suiteClassNames.addElement(suite);
        }

        // Compute closure of suite classes
        loadClosure(suiteClassNames, false);

        return suiteName;
    }

    public void verify(String name) throws LinkageException {
        flags &= ~RESOLVING_PROXIES;
        Type klass = findOrCreateType(toInternalForm(name));
        klass.load();
        klass.convert();
    }

    /**
     * Translate a request into a suite.
     * @param suite The suite root class or driver.
     * @param libs The files containing the suites against which this suite will
     * be linked.
     * @param emitter The SuiteEmitter that should be used to emit the suite.
     * @param isForNameRequest
     */
    public Result run(String suite, String libs, final SuiteEmitter emitter, boolean isForNameRequest) throws LinkageException {

        loadAndResolveLibraries(libs);
        final String suiteName = loadAndResolveSuiteClasses(suite, isForNameRequest);

        // Mark all classes and members that should be included in the suite.
        // The roots are the public classes in the suite. A more selective
        // set of roots may be appropriate for a 'final' and/or 'non-reflective'
        // suite but that requires a little more thought yet...
        time("pruning", new ComputationTimer.Computation() {
            public Object run() {
                for (Enumeration e = loadedTypes.elements(); e.hasMoreElements();) {
                    Type type = (Type)e.nextElement();
                    if (type != null) {
                        if (!pruneSuite() ||
                            (!type.isProxy() && (type.isPublic() || isSuiteReflective()))) {
                            type.mark();
                        }
                        else if (tracepruning()) {
                            System.err.println("ignore: " + type);
                        }
                    }
                }
                return null;
            }
        });

        // Assign slot offsets to all marked fields and methods
        for (Enumeration e = loadedTypes.elements(); e.hasMoreElements();) {
            Type type = (Type)e.nextElement();
            if (type != null) {
                if (type.includeInSuite()) {
                    type.substituteHiddenConstructors();
                    type.assignFieldSlotNumbers();
                    type.assignMethodSlotNumbers();
                }
            }
        }

        // Remove driver classes from suite
        if (!isForNameRequest) {
            String driverClassName = suite;
            Type type = findOrCreateType(toInternalForm(driverClassName));
            type.unsetFlag(JVMConst.ACC_INCLUDE);
        }

        // Partition classes into proxy classes and suite classes
        SortableVector proxyVector = new SortableVector(loadedTypes.size());
        SortableVector suiteVector = new SortableVector(loadedTypes.size());
        for (Enumeration e = loadedTypes.elements(); e.hasMoreElements();) {
            Type type = (Type)e.nextElement();
            if (type != null) {
                if (type.includeInSuite()) {
                    if (type.isProxy()) {
                        proxyVector.addElement(type);
                    }
                    else {
                        suiteVector.addElement(type);
                    }
                }
            }
        }

        // Sort the classes by name
        proxyVector.sort(CLASS_NAME_COMPARER);
        suiteVector.sort(CLASS_NAME_COMPARER);

        // Assign suite IDs to the classes
        int nextID = CNO.LAST_IMPL_TYPE + 1;
        nextID = assignSuiteIdentifiers(nextID, proxyVector);
        nextID = assignSuiteIdentifiers(nextID, suiteVector);

        // Sort the classes by the suite IDs
        proxyVector.sort(CLASS_SUITEID_COMPARER);
        suiteVector.sort(CLASS_SUITEID_COMPARER);

        // Copy the classes into arrays
        final Type[] proxyClasses = new Type[proxyVector.size()];
        final Type[] suiteClasses = new Type[suiteVector.size()];
        proxyVector.copyInto(proxyClasses);
        suiteVector.copyInto(suiteClasses);

        return (Result)time("emitting", new ComputationTimer.Computation() {
            public Object run() {
                String[] bindsList = new String[suiteDependencyNames.size()];
                suiteDependencyNames.copyInto(bindsList);
                SuiteProducer producer = new SuiteProducer(Translator.this, suiteName, suiteClasses, proxyClasses, bindsList);
                producer.emitSuite(emitter, minimalVM() ? SuiteProducer.JAVACARD_3 : SuiteProducer.SUITE_FORMAT);
                return new Result(proxyClasses, suiteClasses, producer.getStatistics());
            }
        });
    }

    /**
     * Assign the suite IDs for a collection of classes. The suite IDs are
     * assigned in a manner such that the suite ID for a class is always
     * higher than this suite ID for any of its super classes.
     *
     * @param id the next available suite ID.
     * @param classes
     * @return the next available suite ID after setting the suite IDs for
     * all the classes in 'classes'.
     */
    private int assignSuiteIdentifiers(int id, Vector classes) {
        for (Enumeration e = classes.elements(); e.hasMoreElements();) {
            Type type = (Type)e.nextElement();

            // Undo any linkage error attributed to the class as it has
            // already been moved into the class's <clinit> method
            type.unsetLinkageError();

            id = type.assignSuiteID(id);
        }
        return id;
    }

    /**
     * Load the classes of a library that the suite being translated depends upon.
     * @param suiteFile The file containing the library in Suite File Format.
     * @throws LinkageException
     */
    private void loadLibraryFromSuiteFile(String suiteFile) throws LinkageException {
        String suiteLoaderClassName = (suiteFile.endsWith(".bin")) ?
            "com.sun.squawk.translator.suite.impl.BinarySuiteLoader" :
            "com.sun.squawk.translator.suite.impl.XMLSuiteLoader";

        try {
            Class suiteLoaderClass = Class.forName(suiteLoaderClassName);
            SuiteLoader suiteLoader = (SuiteLoader) suiteLoaderClass.newInstance();
            String suiteName = suiteLoader.loadSuite(this, suiteFile);
            suiteDependencyNames.addElement(suiteName);
        }
        catch (ClassNotFoundException cnfe) {
            throw new RuntimeException("SuiteLoader implementation class not found: " + suiteLoaderClassName);
        } catch (InstantiationException ie) {
            throw new RuntimeException("Error creating SuiteLoader: " + ie.getMessage());
        } catch (IllegalAccessException iae) {
            throw new RuntimeException("Error creating SuiteLoader: " + iae.getMessage());
        }
    }

    /**
     * Load the classes of a library that the suite being translated depends upon.
     * @param jarFile The jar/zip file containing the classes of the library.
     * @throws LinkageException
     */
    private void loadLibraryFromJarFile(String jarFile) throws LinkageException {
        Vector libClassNames = new Vector();
        ClasspathConnection libClassPath = openClassPath(jarFile);
        findClasses("//", libClassNames, libClassPath);

        ClasspathConnection cp = loader.getClassPath();
        loader.setClassPath(libClassPath);
        loadClosure(libClassNames, true);
        loader.setClassPath(cp);

        // Derive the suite name from the jar/zip file
        int sep = jarFile.lastIndexOf(Translator.separatorChar);
        if (sep != -1) {
            jarFile = jarFile.substring(sep+1);
        }
        jarFile = jarFile.substring(0, jarFile.length()-4);
        suiteDependencyNames.addElement(jarFile);
    }

   /* ------------------------------------------------------------------------ *\
    *                      Object type database managemant                     *
   \* ------------------------------------------------------------------------ */

    /** Hashtable to translate class names to types. */
    private final ArrayHashtable internedTypes = new ArrayHashtable(32);
    /** List of types ordered by their load completion time. */
    private final Vector loadedTypes = new Vector();

    /**
     * Count the dimensions of an class name (i.e. how many leading '['s it has).
     */
    public static int countDimensions(String className) {
        int dims = 0;
        while(className.length() > dims && className.charAt(dims) == '[') {
            dims++;
        }
        return dims;
    }

    /**
     * Find an interned type based on a class name in internal class name format (e.g. "Ljava/lang/Object;").
     * @param name The name of a class in internal format.
     * @return the interned type or null.
     */
    public Type findType(String name) {
        Type type = (Type)internedTypes.get(name);
        return type;
    }

    /**
     * Return the interned instance of a given base type creating and interning it first if necessary.
     * Note that the base may itself be an array type.
     * @param name The name of the type to search for in internal format. E.g. "Ljava/lang/Object;".
     */
    public Type findOrCreateBaseType(String name) {
        Type type = findType(name);
        if (type == null) {
            type = Type.create(this,  name);
            internedTypes.put(name, type);
            if (resolvingProxies()) {
                type.setFlag(JVMConst.ACC_PROXY);
            }
        }
        return type;
    }

    /**
     * Return the interned instance of a given type, creating and interning it first if necessary.
     * @param name The name of the type to search for in internal format. E.g. "Ljava/lang/Object;" or
     * "[I".
     */
    public Type findOrCreateType(String name) {
        int dims = countDimensions(name);

        // A minimal VM cannot handle arrays with more than 32 dimensions as they use up too
        // much of the class name space (which is limited to 256 classes) as each
        // dimension is represented by a class.
        Assert.that(!minimalVM() || dims <= 32);

        Type type = null;
        char firstChar = name.charAt(dims);

        // Ensure that a valid name was given for the non-array portion of the name.
        Assert.that(firstChar == 'L' ||     // a non-primitive type name
               firstChar == '-' ||     // an "unreal" type (e.g. "-U-" or "-J2-"
               (name.length() - dims) == 1);   // a primitive type

        boolean isUnrealType = true;
        switch(firstChar) {
            case 'I': type =  INT;     break;
            case 'J': type =  LONG;    break;
            case 'F':
            case 'D': {
/*if[FLOATS]*/
                if (true) {
                    type = (firstChar == 'F' ? FLOAT : DOUBLE);
                    break;
                }
/*end[FLOATS]*/
                return null;
            }
            case 'Z': type =  BOOLEAN; break;
            case 'C': type =  CHAR;    break;
            case 'S': type =  SHORT;   break;
            case 'B': type =  BYTE;    break;
            case 'V': type =  VOID;    break;
            default:
            case 'L':
                    isUnrealType = false;
            case '-':
                    name = name.substring(dims);
                    if (!isUnrealType) {
                        if (name.charAt(name.length()-1) != ';') {
//                            Assert.that(ConstantPool.isLegalName(name, ConstantPool.ValidNameType.CLASS), name + " is an illegal class name");
                            name = "L"+name+';';
                        }
                        else {
                            Assert.that(name.charAt(0) == 'L');
//                            Assert.that(ConstantPool.isLegalName(name.substring(1, name.length() - 1), ConstantPool.ValidNameType.CLASS), name + " is an illegal class name");
                        }
                        if (name.equals("Ljava/lang/Klass;")) {
                            name = "Ljava/lang/Class;";
                        }
                    }
                    type = findOrCreateBaseType(name);
                    break;
        }

        // Create the types for the n - 1 dimensioned array type for an n dimensioned array type
        for (int i = 0 ; i < dims ; i++) {
            name = '[' + type.name();
            type = findOrCreateBaseType(name);
        }

        return type;
    }


    /**
     * Create one of the special (i.e. bootstrap) types that must have well known class number.
     * @param superType
     * @param name
     * @param cno
     * @return
     */
    private Type createType(Type superType, String name, int cno) {
        Assert.that(cno != 0);
        Type type = findOrCreateType(name);
        type.setSuperType(superType);
        type.setSpecID(cno);
        if (cno != -2) {
            loadedTypes.addElement(type);
            Assert.that(cno == -1 || loadedTypes.size() == type.specID());
        } else {
            type.setState(Type.CONVERTED);
        }
        return type;
    }

    /**
     * Create one of the special (i.e. bootstrap) array types that must have a well known class number.
     * @param elementType
     * @param cno
     * @return
     */
    private Type createArrayType(Type elementType, int cno) {
        Assert.that(cno != 0);
        Type type = elementType.asArray();
        type.setSpecID(cno);
        if (cno != -2) {
            loadedTypes.addElement(type);
            Assert.that(cno == -1 || loadedTypes.size() == type.specID());
        }
        return type;
    }

   /* ------------------------------------------------------------------------ *\
    *                              Type[] interning                            *
   \* ------------------------------------------------------------------------ */

    /**
     *
     */
    public Type[] internTypeList(Type[] list) {
        Type[] l = (Type[])internedTypes.get(list);
        if (l == null) {
            internedTypes.put(list, list);
            l = list;
        }
        return l;
    }

   /* ------------------------------------------------------------------------ *\
    *                             Timer                                        *
   \* ------------------------------------------------------------------------ */


    private final ComputationTimer timer;
    public Object time(String id, ComputationTimer.Computation c) {
        if (timer != null) {
            return timer.time(id, c);
        } else {
            return c.run();
        }
    }
    public Object time(String id, ComputationTimer.ComputationException c) throws Exception {
        if (timer != null) {
            return timer.time(id, c);
        } else {
            return c.run();
        }
    }

   /* ------------------------------------------------------------------------ *\
    *                             String interning                             *
   \* ------------------------------------------------------------------------ */

    /** Hashtable of interned Strings. */
    private final ArrayHashtable internedStrings;

    /**
     * Get an interned string.
     * @param string The String object for which an interned instance is requested.
     * @return the interned value of string or null if string itself is null.
     */
    public String internString(String string) {
        if (string != null) {
            String s = (String)internedStrings.get(string);
            if (s == null) {
                internedStrings.put(string, string);
                s = string;
            }
            return s;
        }
        else {
            return null;
        }
    }

/* ------------------------------------------------------------------------ *\
 *                  Local variable unique IDs                               *
\* ------------------------------------------------------------------------ */

    private int nextLocalVariableID;
    public int nextLocalVariableID() {
        return nextLocalVariableID++;
    }

/* ------------------------------------------------------------------------ *\
 *                  Misc utilities                                          *
\* ------------------------------------------------------------------------ */

    /**
     * Return the size of a String in its UTF8 encoded form.
     * @param str
     * @return
     */
    public static int utf8Size(String str) {
        Assert.that(str != null);
        int strlen = str.length();
        int utflen = 0;
        int c, count = 0;

        for (int i = 0; i < strlen; i++) {
            c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }
        return utflen;
    }

    private static Comparer CLASS_NAME_COMPARER = new Comparer() {
        public int compare(Object o1, Object o2) {
            if (o1 == o2) {
                return 0;
            }
            return ((Type)o1).name().compareTo(((Type)o2).name());
        }
    };

    private static Comparer CLASS_SUITEID_COMPARER = new Comparer() {
        public int compare(Object o1, Object o2) {
            if (o1 == o2) {
                return 0;
            }
            return ((Type)o1).suiteID() - ((Type)o2).suiteID();
        }
    };

}