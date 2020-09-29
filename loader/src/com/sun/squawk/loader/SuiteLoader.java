package com.sun.squawk.loader;

import java.io.*;
import com.sun.squawk.suite.*;
import com.sun.squawk.vm.CNO;
import com.sun.squawk.vm.SquawkConstants;
import java.util.Vector;
/*if[DEBUG.SYSTEMCLASSES]*/
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.StringTokenizer;
/*end[DEBUG.SYSTEMCLASSES]*/

/**
 * A SuiteLoader instance is used to load a suite from an input stream.
 * There is at most one instance of SuiteLoader in existence at a time and
 * this is only during the process of loading a suite. This means that
 * all the memory used during the loading process can be reclaimed once
 * loading is complete.
 */
public class SuiteLoader {

/*if[DEBUG.LOADER]*/
    /**
     * Constant specifying that the loader is to trace class and
     * method loading.
     */
    public static final int TRACE_LOADING  = 1 << 0;

    /**
     * Constant specifying that the loader is to trace verification.
     */
    public static final int TRACE_VERIFIER = 1 << 1;

    /**
     * Constant specifying that the loader should display statistics
     * about each suite file as it is parsed.
     */
    public static final int SUITE_STATS    = 1 << 2;

    boolean traceLoading()   { return  (flags & TRACE_LOADING)  != 0; }
    boolean traceVerifier()  { return  (flags & TRACE_VERIFIER) != 0; }
    boolean suiteStats()     { return  (flags & SUITE_STATS)    != 0; }

    private int         flags;

/*if[SYSTEM.STREAMS]*/
    public static PrintStream traceOut = System.err;
/*end[SYSTEM.STREAMS]*/

/*end[DEBUG.LOADER]*/

    /**
     * The singleton suite loader instance.
     */
    public static SuiteLoader theLoader;

    /**
     * The singleton verifier instance.
     */
    final Verifier theVerifier = new Verifier();

    /**
     * The relocators for the classes in the suite currently being loaded.
     */
    private Relocator[] suiteClasses;

    /**
     * The header for the suite currently being loaded.
     */
    private SuiteHeader suite;

/*if[FINALIZATION]*/

    /**
     * The vtable index for the Object.finalize() method.
     */
    private static int slotForFinalize = SquawkConstants.SLOT_UNDEFINED;

    /**
     * Set the vtable index for Object.finalize() or ensure that it matches
     * the current value for this index if it has been set previously.
     *
     * @param slot
     */
    public static void setSlotForFinalize(int slot) {
        if (slotForFinalize != SquawkConstants.SLOT_UNDEFINED &&
            slotForFinalize != slot) {
            throw new LinkageError("The vtable index for Object.finalize() is corrupt");
        }
        slotForFinalize = slot;
    }

    /**
     * Get the vtable index for Object.finalize().
     *
     * @return the vtable index for Object.finalize().
     */
    public static int getSlotForFinalize() {
        return slotForFinalize;
    }

/*end[FINALIZATION]*/


    /**
     * Get the header for the suite currently being loaded.
     *
     * @return the header of the suite currently being loaded.
     */
    SuiteHeader suite() {
        return suite;
    }

    /**
     * Return the size in bytes of a field of a specified type when it is
     * packed into an instance.
     *
     * @param type
     * @return
     */
    public static int getPackedSize(int type) {
        switch (type) {
            case CNO.BOOLEAN: case CNO.BYTE:
                return 1;
            case CNO.CHAR: case CNO.SHORT:
                return 2;
            default:
                return 4;
        }
    }

    /*
     * getSystemTypeFor
     */
    public char getSystemTypeFor(int type) {
        if (type == 0) {
            return 0;
        }
//System.out.println("type="+type);
        return suiteClasses[type].internal_getType();
    }

    /*
     * getSystemTypeFor
     */
    char[] getSystemTypesFor(char[] types) {
        char[] result = new char[types.length];
        for (int i = 0 ; i < types.length ; i++) {
            result[i] = getSystemTypeFor(types[i]);
        }
        return result;
    }

    /*
     * getElementTypeFor
     */
    char getElementTypeFor(StringOfSymbols symbols) {
        for (int i = 1 ; i < suiteClasses.length ; i++) {
            if (isTypeSupported(i)) {
                Relocator relocator = suiteClasses[i];
                Romizer.assume(relocator != null);
                if (symbols.isArrayTypeFor(relocator.getName())) {
                    return (char)i;
                }
            }
        }
        throw new LinkageError("NoClassDefFoundError: "+symbols.getClassName().substring(1));
    }

    /*
     * isAssignable
     */
    boolean isAssignable(int from, int to) {
        if (from == CNO.NULL) {
            return (to != CNO.VOID &&
                    to != CNO.BOOLEAN &&
                    to != CNO.BYTE &&
                    to != CNO.CHAR &&
                    to != CNO.SHORT &&
                    to != CNO.INT &&
                    to != CNO.LONG &&
                    to != CNO.LONG2 &&
/*if[FLOATS]*/
                    to != CNO.FLOAT &&
                    to != CNO.DOUBLE &&
                    to != CNO.DOUBLE2 &&
/*end[FLOATS]*/
                    true);
        }
        return getRelocator(from).isAssignableTo(getRelocator(to));
    }

    /**
     * Get the name of a specified type.
     *
     * @param type The type's identifier.
     * @return
     */
    String getTypeName(int type) {
        if (type == CNO.NULL) {
            return "null";
        } else {
            return getRelocator(type).getName();
        }

    }

    /**
     * Get the symbolic details for a specified member of a specified class.
     *
     * @param type The class in which the member is declared.
     * @param mtype The category of members to search.
     * @param slot The slot identifier for the member.
     * @return the found member
     * @throws LinkageError if the member was not found.
     */
    Member getMember(char type, int mtype, int slot) {
        return getRelocator(type).getMember(mtype, slot);
    }

    /**
     * Given the type identifier for an array class, return the type identifier
     * for the class of its elements.
     * @param arrayType The type identifier for an array class.
     * @return the type identifier for class of <code>arrayType</code>'s elements.
     * @throws LinkageError if <code>arrayType</code> is not an array class.
     */
    char elementType(int arrayType) {
        char res = elementTypeOrNull(arrayType);
        if (res == CNO.NULL) {
            throw new LinkageError("VerifyError: non-array class has no element type: "+arrayType);
        }
        return res;
    }

    /**
     * Given the type identifier for a class, return the type identifier
     * for the class of its elements if it is an array class. Otherwise,
     * return NULL.
     *
     * @param type The type identifier for a class.
     * @return the type identifier for class of <code>type</code>'s elements
     * if <code>type</code> is an array class otherwise NULL.
     */
    char elementTypeOrNull(int type) {
        //return elementTypeOrZero(getRelocator(type).getName());
        return getRelocator(type).suite_getElementType();
    }

    /**
     * Get the relocator for a class within the current suite.
     *
     * @param type the type identifier for the class
     * @return
     */
    Relocator getRelocator(int type) {
        if (type == 0) {
            return null;
        }
        Relocator r = suiteClasses[type];
        if (r == null) {
            throw new LinkageError("Invalid forward reference");
        }
        return r;
    }

    /**
     * Find a relocator for a class with a given name in the current suite.
     *
     * @param type the name of the class.
     * @return the found class or null
     */
    Relocator findRelocatorForName(String name) {
        for (int i = 1 ; i < suiteClasses.length ; i++) {
            Relocator r = suiteClasses[i];
            if (r != null && r.getName().equals(name)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Get the relocator for a suite class that corresponds to an internally
     * linked class.
     *
     * @param systemType the internally linked identifier for the class
     * @return the relocator for the suite class that corresponds to 'systemType'
     * or null if there is no such class.
     */
    Relocator findRelocatorForSystemType(int systemType) {
        char suiteType = findSuiteTypeForSystemType(systemType);
        if (suiteType == 0) {
            return null;
        } else {
            return suiteClasses[suiteType];
        }
    }

    /**
     * Get the type identifier in the suite file namespace for the class that
     * corresponds to an internally linked class.
     *
     * @param systemType the internally linked identifier for the class
     * @return the relocator for the suite class that corresponds to 'systemType'
     * or null if there is no such class.
     */
    public char findSuiteTypeForSystemType(int systemType) {
        for (int i = 1 ; i < suiteClasses.length ; i++) {
            Relocator r = suiteClasses[i];
            // The r != null is only needed for SuiteRelocator.fixupInterfacesSlots()
            // which has to look up its superclass before all the classes are read in.
            // This could be done on a separate pass through the relocator table
            // but it would prevent the SuiteRelocator from being migrated to EEPROM
            // until the fixup was done.
            if (r != null && r.internal_getType() == systemType) {
                 return (char)i;
            }
        }
        return 0;
    }

    /**
     * Load zero or more suites via a specified SuiteParser.
     *
     * @param parser
     */
    private void loadSuites(SuiteParser parser) {
        try {
            parser = parser.nextSuite();
            while (parser != null) {

                suite = parser.readSuiteHeader();
                suite.setNumber(SuiteManager.getFreeSuiteNumber());

                Romizer.logSuite(suite.name);
/*if[DEBUG.LOADER]*/
                if (traceLoading()) {
                    traceOut.println("Loading suite: "+suite.name+" sno="+suite.number());
                }

                if (debugOut != null) {
                    SuiteLoader.debugOut.println();
                    SuiteLoader.debugOut.println();
                    SuiteLoader.debugOut.println("+++++++++++++++ SuiteLoader log for: "+suite.name+ " +++++++++++++++");
                    SuiteLoader.debugOut.println();
                    SuiteLoader.debugOut.println();
                }
/*end[DEBUG.LOADER]*/


               /*
                * Create an array of relocators, load the classes.
                */
                suiteClasses = new Relocator[suite.maxType+1];
                int suiteClassCount = loadClasses(parser);

/*if[DEBUG.LOADER]*/
                if (SuiteLoader.debugOut != null) {
                    printClasses();
                }
/*end[DEBUG.LOADER]*/

                Klass[] klasses = new Klass[suiteClassCount+1];
                loadMethods(parser, klasses);
                suiteClasses = null;
                SuiteManager.addSuite(suite.number(), suite.name, suite.binds, klasses);
/*if[DEBUG.LOADER]*/
                if (traceLoading()) {
                    traceOut.println("Finished suite: "+suite.name+" sno="+suite.number());
                }
                if (suiteStats()) {
                    parser.printStats(traceOut);
                }
/*end[DEBUG.LOADER]*/
                parser = parser.nextSuite();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new LinkageError(ioe.getMessage());
        }

    }

    private SuiteClass readClass(SuiteParser parser) throws IOException {
        SuiteClass sc = parser.readClass();
        if (sc != null) {
            parser.readClassAttributes(sc);
        }
        return sc;
    }

    /**
     * Load the meta-info for all the classes in the current suite.
     *
     * @param parser
     * @return the number of non-proxy classes declared in the suite.
     * @throws IOException
     */
    private int loadClasses(SuiteParser parser) throws IOException {
        int suiteNameSpace  = suiteClasses.length;
        int suiteClassCount = 0;
        SuiteClass nextClass = readClass(parser);

        for (int type = 1; type < suiteNameSpace ; type++) {
            boolean loadingProxies = (suiteClassCount == 0);
            SuiteClass sc;

            // Create stubs for implicit proxy classes or Squawk specific
            // implementation classes.
            if (nextClass.getType() == type) {
                sc = nextClass;
                Romizer.logClass(sc, (sc.getAccess() & SquawkConstants.ACC_PROXY) != 0);
                nextClass = readClass(parser);
            } else {
                boolean floatsSupported = false;
/*if[FLOATS]*/
                floatsSupported = true;
/*end[FLOATS]*/
                if (!floatsSupported &&
                    (type == CNO.FLOAT       ||
                     type == CNO.DOUBLE      ||
                     type == CNO.DOUBLE2     ||
                     type == CNO.FLOAT_ARRAY ||
                     type == CNO.DOUBLE_ARRAY)) {
                    if (!loadingProxies) {
                        suiteClassCount++;
                    }
                    continue;
                }

                sc = getImplicitProxyOrImplementationType(type, loadingProxies);
            }

            verifyClassName(sc, type);

            // Find an internally linked class corresponding to
            // the current suite class
            Klass klass = SuiteManager.lookup(suite.binds, sc.getName());

            if ((sc.getAccess() & SquawkConstants.ACC_PROXY) != 0) {
                if (klass == null && isTypeSupported(type)) {
                    String msg = "NoClassDefFoundError in suite '"+suite.name+"': internal class not found for proxy class: "+sc.getName();
                    if (suite.binds.length > 0) {
                        msg += " (libs:";
                        for (int i = 0; i != suite.binds.length; ++i) {
                            msg += ' '+suite.binds[i];
                        }
                        msg += ')';
                    }
                    throw new LinkageError(msg);
                }
                suiteClasses[type] = ProxyRelocator.create(sc, klass, type);
            } else {
                if (loadingProxies) {
                    fixupProxies();
                }

                // Look for aliased arrays.
                if (klass != null) {
                    if (sc.isArray()) {
/*if[SYSTEM.STREAMS]*/
                        System.out.println("Aliasing non-proxy array class in suite to system array class number: "+klass.getName());
/*end[SYSTEM.STREAMS]*/
                        suiteClasses[type] = ProxyRelocator.create(sc, klass, type);
                        fixupProxy(type);
                        ++suiteClassCount;
                    } else {
                        throw new LinkageError("ClassFormatError: proxy class came after suite class "+klass.getName());
                    }
                } else {
                    suiteClasses[type] = new SuiteRelocator(sc, ++suiteClassCount);
                }
            }

/*if[DEBUG.LOADER]*/
            if (traceLoading()) {
                traceOut.println("Loaded "+(loadingProxies ? "proxy " : "")+"class: "+sc.getName());
            }

            if (debugOut != null) {
                SuiteLoader.debugOut.println("    "+((suiteClasses[type] instanceof ProxyRelocator)? "Proxy":"Suite")+"["+type+"]: "+suiteClasses[type].getName());
            }
/*end[DEBUG.LOADER]*/
        }
        PersistentMemory.allowMigration(suiteClasses);
        return suiteClassCount;
    }

    /**
     * Verify that a class's name does not match the name of one of the
     * implicit/implementation classes or if it does, then verify that
     * its identifier matches that of the implicit/implementation class.
     * @param sc
     * @param type
     */
    private void verifyClassName(SuiteClass sc, int type) {
        for (int i = CNO.FIRST_IMPLICIT_TYPE ; i <= CNO.LAST_IMPL_TYPE ; i++) {
            String expectedName = CNO.IMPLICIT_PROXY_NAMES[i];
            if (sc.equalsClassName(expectedName)) {
                if (type != i) {
                    throw new LinkageError("Class "+sc.getName()+" should be type "+i+" not "+type);
                }
            }
        }
    }

    /*
     * fixupProxies
     */
    private void fixupProxies() throws IOException {
        int totalCount = suiteClasses.length;
        for (int i = 1 ; i < totalCount ; i++) {
            fixupProxy(i);
        }
    }

    /*
     * fixupProxy
     */
    private void fixupProxy(int i) throws IOException {
        ProxyRelocator r = (ProxyRelocator)suiteClasses[i];
        if (r != null) {
            r.fixup();
        }
    }

    /**
     * Load all the method implementations from the suite file.
     * @param parser
     * @return
     * @throws IOException
     */
    private Klass[] loadMethods(SuiteParser parser, Klass[] klasses) throws IOException {
        SuiteMethodImpl methodImpl  = parser.readMethodImpl();
        KlassBuilder builder   = null;
        char currentSuiteType = methodImpl == null ? CNO.NULL : methodImpl.parent;
        boolean suiteIsFinal   = (suite.access & SquawkConstants.ACC_FINAL) != 0;
        Romizer.logMethod();

        // Build Klass objects for classes of all the methods defined in the suite file
        while (currentSuiteType != CNO.NULL) {
            if (builder == null) {
                builder = new KlassBuilder(this, (SuiteRelocator)getRelocator(currentSuiteType));
            }
            builder.addMethodImpl(methodImpl);
            Romizer.logMethod();
            parser.readMethodImplAttributes(methodImpl);

            methodImpl = parser.readMethodImpl();
            if (methodImpl == null) {
                currentSuiteType = CNO.NULL;
                builder.finishKlass(suiteIsFinal, suite.number(), klasses);
            } else if (methodImpl.parent != currentSuiteType) {
                builder.finishKlass(suiteIsFinal, suite.number(), klasses);
                builder = null;
                currentSuiteType = methodImpl.parent;
            }
        }

        // Build Klass objects for the suite classes without methods
        currentSuiteType = (char)((suiteClasses.length - klasses.length) + 1);
        for (int type = 1 ; type < klasses.length ; type++) {
            if (suite.number() != 0 || isTypeSupported(type)) {
                Relocator r = getRelocator(currentSuiteType++);
                if (r instanceof SuiteRelocator) {

                    SuiteRelocator sr = (SuiteRelocator)r;
                    Romizer.assume(sr.finalClassNumber() == type);
                    if (klasses[type] == null) {
                        builder = new KlassBuilder(this, sr);
                        builder.finishKlass(suiteIsFinal, suite.number(), klasses);
                    }
                } else {
                    klasses[type] = ((ProxyRelocator)r).getKlass(); // Alias the array class
                }
            } else {
                currentSuiteType++;
            }
        }
        return klasses;
    }

    /**
     * Get the type corresponding to a predefined class number which will either
     * be an implicit proxy type or Squawk VM implementation specific type.
     * @param index The predefined class number
     * @return
     */
    private SuiteClass getImplicitProxyOrImplementationType(int type, boolean loadingProxies) {
        if (type >= CNO.FIRST_IMPLICIT_TYPE && type <= CNO.LAST_IMPL_TYPE) {
            String name = CNO.IMPLICIT_PROXY_NAMES[type];
            if (type <= CNO.LAST_IMPLICIT_TYPE) {
                if (!loadingProxies && isTypeSupported(type)) {
                    throw new LinkageError("ClassFormatError: missing suite class: "+type);
                }
                return new SuiteClass(
                        new SymbolBuilder(name).toStringOfSymbols(),
                        (char)type,
                        (char)SquawkConstants.ACC_PROXY,
                        (char)0,
                        null,
                        null,
                        null,
                        null,
                        null);
            }
            else {
                int flags = loadingProxies ? SquawkConstants.ACC_PROXY : 0;

                return new SuiteClass(
                        new SymbolBuilder(name).toStringOfSymbols(),
                        (char)type,
                        (char)(flags),
                        (char)CNO.OBJECT,
                        KlassBuilder.NO_TYPES,
                        KlassBuilder.NO_INTERFACE_SLOT_TABLES,
                        KlassBuilder.NO_TYPES,
                        KlassBuilder.NO_OBJECTS,
                        KlassBuilder.NO_INTS
                        );
            }
        }
        else {
            throw new RuntimeException("Invalid implicit proxy or implementation type "+type);
        }
    }

    /**
     * Run the loader to load zero or more suites via a SuiteParser.
     *
     * @param parser The SuiteParser used to load the suite(s).
     * @param out The output stream to be used for debug output or null if no
     * debug output is to be generated.
     * @param closeOut If true, then close 'out' stream before returning.
     * @param flags A mask of flags modifying the execution of the loader. The
     * recognised values are TRACE_LOADING, TRACE_VERIFIER and SUITE_STATS.
     */
    public static void run(SuiteParser parser,
                           OutputStream out,
                           boolean closeOut,
                           int flags)
    {
        try {
            Native.assume(theLoader == null);
            theLoader = new SuiteLoader();
/*if[DEBUG.LOADER]*/
            SuiteLoader.debugOut = (PrintStream)out;
            theLoader.flags = flags;
/*end[DEBUG.LOADER]*/
            theLoader.loadSuites(parser);
        } finally {
/*if[DEBUG.LOADER]*/
            if (closeOut && out != null) {
               try{ out.close(); } catch(Exception e) {}
            }
/*end[DEBUG.LOADER]*/
            StringOfSymbols.flush();
            theLoader = null;
        }
    }

    /**
     * Roundup <code>value</code> to be multiple of <code>size</code>.
     *
     * @param value The value to be rounded up.
     * @param size The size used for rounding. Must be 1, 2 or 4.
     * @return <code>(char)((value + (size-1)) & ~(1 << (size-1)))</code>
     */
    public static char roundup(int value, int size) {
        switch (size) {
            case 1: break;
            case 2: value = (value+1)&0xFFFFFFFE; break;
            case 4: value = (value+3)&0xFFFFFFFC; break;
            default: throw new RuntimeException("bad roundup value="+value+" size="+size);
        }
        if ((value & 0xFFFF) != value) {
            throw new RuntimeException("overflow on roundup value="+value+" size="+size);
        }
        return (char)value;
    }

    /**
     * Verify that a given type is supported by this VM. The appropriate
     * exception is raised if the type is not supported.
     * @param type 8-bit identifier for the type.
     */
    static public boolean isTypeSupported(int type) {
        Romizer.assume((type & 0xff) == type);
/*if[LONGS]*/
        if (type == CNO.LONG  ||
            type == CNO.LONG2 ||
            type == CNO.LONG_ARRAY)
        {
            return true;
        }
/*end[LONGS]*/
        if (type == CNO.LONG  ||
            type == CNO.LONG2 ||
            type == CNO.LONG_ARRAY)
        {
            return false;
        }

/*if[FLOATS]*/
        if (type == CNO.FLOAT       ||
            type == CNO.DOUBLE      ||
            type == CNO.DOUBLE2     ||
            type == CNO.FLOAT_ARRAY ||
            type == CNO.DOUBLE_ARRAY
            )
        {
            return true;
        }
/*end[FLOATS]*/
        if (type == CNO.FLOAT       ||
            type == CNO.DOUBLE      ||
            type == CNO.DOUBLE2     ||
            type == CNO.FLOAT_ARRAY ||
            type == CNO.DOUBLE_ARRAY)
        {
            return false;
        }
        return true;
    }

//=============================================================================================================================
// TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP
//=============================================================================================================================

/*if[DEBUG.LOADER]*/
    public static PrintStream debugOut = System.out;

    int maxSize;
    int maxRam;
    StringBuffer maxTrace;

    /*
     * printClasses
     */
    private void printClasses() {
        for (int i = 1 ; i < suiteClasses.length ; i++) {
            if (isTypeSupported(i)) {
                Relocator r = getRelocator(i);
                r.printClass(i, this);
            }
        }
    }
/*end[DEBUG.LOADER]*/

}
