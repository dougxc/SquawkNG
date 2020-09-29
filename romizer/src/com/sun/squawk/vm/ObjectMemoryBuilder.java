package com.sun.squawk.vm;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;
import java.io.PrintStream;

import com.sun.squawk.suite.SuiteClass;
import com.sun.squawk.loader.SuiteLoader;
import com.sun.squawk.util.IntHashtable;
import com.sun.squawk.vm.CNO;
import com.sun.squawk.vm.SquawkConstants;

/**
 * This class is used to generate a Squawk heap image based on a
 * graph of objects in the JVM heap.
 */
public final class ObjectMemoryBuilder extends ObjectMemory {

/* ------------------------------------------------------------------------ *\
 *                Dummy implementations of abstract methods                 *
\* ------------------------------------------------------------------------ */

    void printExtensionCalls() {}


/*---------------------------------------------------------------------------*\
 *                                  Romizing                                 *
\*---------------------------------------------------------------------------*/

    /**
     * Override this method to indicate that the context for
     * the Squawk VM memory system is the romizer.
     *
     * @return
     */
    boolean romizing() {
        return true;
    }

    /**
     * Override this method to ensure that all pointers in the romized image
     * are relative to 0.
     *
     * @return
     */
    int getImageOffset() {
        return 0;
    }

/*-----------------------------------------------------------------------*\
 *                           Options                                     *
\*-----------------------------------------------------------------------*/

    public static final int OPT_GCROM            = 1 << 0;
    public static final int OPT_CLASSSTATS       = 1 << 1;

    public static final int OPT_TRACEGC          = 1 << 2;
    public static final int OPT_TRACEGCVERBOSE   = 1 << 3;
    public static final int OPT_TRACEALLOC       = 1 << 4;
    public static final int OPT_TRACEHEAP        = 1 << 5;

    public static final int OPT_BIGENDIAN        = 1 << 6;

    final int flags;

    boolean gcRom()                         { return canCollectRom() && (flags & OPT_GCROM) != 0;          }
    boolean getTraceGC()                    { return (flags & OPT_TRACEGC) != 0;        }
    boolean getTraceGCVerbose()             { return (flags & OPT_TRACEGCVERBOSE) != 0; }
    boolean getTraceAllocation()            { return (flags & OPT_TRACEALLOC) != 0;     }

    boolean getTraceHeap()                  { return (flags & OPT_TRACEHEAP) != 0;      }
    boolean isBigEndian()                   { return (flags & OPT_BIGENDIAN) != 0;      }


/*-----------------------------------------------------------------------*\
 *                            Initialization                             *
\*-----------------------------------------------------------------------*/

    /**
     * Initialise the image builder.
     *
     * @param rom Start address of ROM.
     * @param romSize Size (in bytes) of ROM memory segment.
     * @param eeprom Start address of EEPROM.
     * @param eepromSize Size (in bytes) of EEPROM memory segment.
     * @param ram Start address of RAM.
     * @param ramSize Size (in bytes) of RAM memory segment.
     * @param flags
     * @param traceURL
     * @param suiteClassesMap
     */
    public ObjectMemoryBuilder(int rom,    int romSize,
                               int eeprom, int eepromSize,
                               int ram,    int ramSize,
                               int flags,
                               String traceURL,
                               Map suiteClassesMap) {
        this.flags = flags;
        this.suiteClassMap = suiteClassesMap;

        PlatformAbstraction_init(traceURL, false);
        ObjectMemory_init(rom, romSize, eeprom, eepromSize, ram, ramSize, gcRom(), isBigEndian());
    }


/*-----------------------------------------------------------------------*\
 *                       Allocation functions                            *
\*-----------------------------------------------------------------------*/

    /**
     * Allocate memory in ROM.
     *
     * @param wordLength The length (in words) of memory segment to allocate.
     * @return the allocated memory segment
     * @throws OutOfMemoryError if the romizer has been configured not to grow
     * the ROM heap as needed and the allocation fails.
     */
    private int allocRom(int wordLength) {
        int byteLength = wordLength*4;
        int csEnd      = getCurrentObjectPartitionEnd();
        int csfreePtr  = getCurrentObjectPartitionFree();
        if (csfreePtr + byteLength > csEnd) {
            throw new OutOfMemoryError("ROM");
        }

        // The allocation is guaranteed to be successful now.
        int ptr = getCurrentObjectPartitionFree();
        setCurrentObjectPartitionFree(ptr + byteLength);

        // Ensure that the object was allocated on a word boundary
        assume((ptr & 0x3) == 0);

        // Zero the bytes of the object
        zeroBytes(ptr, byteLength);

        return ptr;
    }

    /**
     * Allocate an instance of a non-array class.
     *
     * @param instanceClass The class for which an instance is to be allocated.
     * @return the address of the allocated instance.
     */
    private int allocInstance(Klass instanceClass) {
        assume(!instanceClass.isArray());
        int wordLength = instanceClass.instanceFieldsLength;
        int oop = allocRom(wordLength + 1) + 4;
        // Setting the class pointer for "java.lang.Class" object is done specially
        if (instanceClass != klassClass) {
            Object_setClass(oop, getClassOop(instanceClass));
        }

        if (getTraceAllocation()) {
            traceln("allocated a '"+instanceClass.getName()+"' @ "+oop);
        }
        return oop;
    }

    /**
     * Return the memory required (in bytes) of a value of a specified type
     * when it is in an array.
     *
     * @param type
     * @return
     */
    private int getArrayElementSize(int type) {
        switch (type) {
/*if[FLOATS]*/
            case CNO.DOUBLE:
                // fall through ...
/*end[FLOATS]*/
            case CNO.LONG:
                return 8;
            case CNO.CHAR:
                // fall through ...
            case CNO.SHORT:
                return 2;
            case CNO.BOOLEAN:
                // fall through ...
            case CNO.BYTE:
                return 1;
            default:
                return 4;
        }
    }

    /**
     * Allocate an instance of an array class.
     *
     * @param arrayClass The class for which an instance is to be allocated.
     * @param count The length of the array
     * @return the address of the allocated instance.
     */
    private int allocArray(Klass arrayClass, int count) {
        assume(arrayClass.isSquawkArray());
        int elementByteSize = getArrayElementSize(arrayClass.elementType);
        int wordLength = ((count * elementByteSize) + 3) / 4;
        int headerLth  = Object_calculateArrayHeaderLength(count);
        int oop        = allocRom(wordLength + headerLth/4) + headerLth;
        int klass      = getClassOop(arrayClass);
        assume(klass != 0);
        Object_setClassAndArrayCount(oop, klass, count);

        if (getTraceAllocation()) {
            traceln("allocated a '"+arrayClass.getNameInternal()+"' @ "+oop+"  [count="+count+"] klass="+klass);
        }

        return oop;
    }

    /**
     * Allocate an instance of "java.lang.Class".
     *
     * @param aClass The instance of Klass in the JVM heap for which a
     * corresponding instance of "java.lang.Class" is to be allocated
     * in the ROM heap.
     * @return the address of the allocated instance.
     */
    private int allocClass(Klass aClass) {

        int klass = allocInstance(klassClass);

        // The class for "java.lang.Class" has not been allocated yet when
        // allocating the class for "java.lang.Object" and "java.lang.Class"
        if (aClass != klassClass && aClass != klassObject) {
            Object_setClass(klass, getClassOop(klassClass));
        }

        setClassOop(aClass, klass);

        return klass;
    }

    /**
     * Return the amount of memory (in bytes) allocated in the ROM.
     *
     * @return the amount of memory (in bytes) allocated in the ROM.
     */
    public int romUsed() {
        return getCurrentObjectPartitionFree();
    }

/*-----------------------------------------------------------------------*\
 *                    Klass to address mapping functions                 *
\*-----------------------------------------------------------------------*/

    /**
     * Map of Klass instances in the JVM heap to their
     * corresponding instances in the ROM heap.
     */
    private HashMap classAddresses = new HashMap();

    /**
     * Record the ROM heap address for a corresponding JVM Klass instance.
     *
     * @param aClass
     * @param oop
     */
    private void setClassOop(Klass aClass, int oop) {
        Object existing = classAddresses.put(aClass, new Integer(oop));
        assume(existing == null);
    }

    /**
     * Return the ROM heap address for a given KVM Klass instance.
     *
     * @param aClass
     * @return
     */
    private int getClassOop(Klass aClass) {
        assume(aClass != null);
        Integer oop = (Integer)classAddresses.get(aClass);
        assume(oop != null);
        return oop.intValue();
    }

    /**
     * Return whether or not a given JVM Klass instance has a corresponding
     * instance in the ROM heap.
     *
     * @param aClass
     * @return
     */
    private boolean isClassAllocated(Klass aClass) {
        return classAddresses.containsKey(aClass);
    }

/*-----------------------------------------------------------------------*\
 *              Romized instance pool methods                            *
\*-----------------------------------------------------------------------*/

    /**
     * Get a canonical representation of an array of primitive values. This
     * includes any instances of the special classes that are represented
     * internally as an array.
     *
     * @param array
     * @param arrayClass
     * @return
     */
    private String canonicalizePrimitiveArray(Object array, Klass arrayClass) {
        String name = arrayClass.getName() + ':';
        if (array instanceof byte[]) {
            byte[] barray = (byte[])array;
            StringBuffer buf = new StringBuffer(name.length()+barray.length);
            buf.append(name);
            for (int i = 0; i != barray.length; ++i) {
                buf.append((char)(barray[i]&0xFF));
            }
            return buf.toString();
        } else if (array instanceof char[]) {
            return name + new String((char[])array);
        } else if (array instanceof short[]) {
            short[] sarray = (short[])array;
            StringBuffer buf = new StringBuffer(name.length()+sarray.length);
            buf.append(name);
            for (int i = 0; i != sarray.length; ++i) {
                buf.append((char)sarray[i]);
            }
            return buf.toString();
        } else if (array instanceof int[]) {
            int[] iarray = (int[])array;
            StringBuffer buf = new StringBuffer(name.length()+(iarray.length*2));
            buf.append(name);
            for (int i = 0; i != iarray.length; ++i) {
                int val = iarray[i];
                buf.append((char)(val >>> 16)).
                    append((char)(val & 0xFFFF));
            }
            return buf.toString();
        } else if (array instanceof float[]) {
            float[] farray = (float[])array;
            StringBuffer buf = new StringBuffer(name.length()+(farray.length*2));
            buf.append(name);
            for (int i = 0; i != farray.length; ++i) {
                int val = Float.floatToIntBits(farray[i]);
                buf.append((char)(val >>> 16)).
                    append((char)(val & 0xFFFF));
            }
            return buf.toString();
        } else if (array instanceof long[]) {
            long[] larray = (long[])array;
            StringBuffer buf = new StringBuffer(name.length()+(larray.length*4));
            buf.append(name);
            for (int i = 0; i != larray.length; ++i) {
                long val = larray[i];
                buf.append((char)(val >>> 48)).
                    append((char)(val >>> 32)).
                    append((char)(val >>> 16)).
                    append((char)(val & 0xFFFF));
            }
            return buf.toString();
        } else if (array instanceof double[]) {
            double[] darray = (double[])array;
            StringBuffer buf = new StringBuffer(name.length()+(darray.length*4));
            buf.append(name);
            for (int i = 0; i != darray.length; ++i) {
                long val = Double.doubleToLongBits(darray[i]);
                buf.append((char)(val >>> 48)).
                    append((char)(val >>> 32)).
                    append((char)(val >>> 16)).
                    append((char)(val & 0xFFFF));
            }
            return buf.toString();
        } else {
            fatalVMError("Bad array class: "+name+" (getClass: "+array.getClass().getName()+")");
            return null;
        }
    }

    private HashMap romizedPrimitiveArrayPool = new HashMap();
    private HashMap romizedInstancePool = new HashMap();

    /**
     * Get the address of the romized version of a given JVM heap instance.
     *
     * @param object The JVM heap instance for which the address is required.
     * @param klass The class of the object.
     * @return the address of the romized version of 'object' or 0 if it has
     * not yet been romized.
     */
    private int getRomizedInstance(Object object, Klass klass) {
        HashMap pool;
        if (klass.isSquawkArray() && SuiteClass.isPrimitiveType(klass.elementType)) {
            object = canonicalizePrimitiveArray(object, klass);
            pool = romizedPrimitiveArrayPool;
        } else {
            pool = romizedInstancePool;
        }
        Integer address = (Integer)pool.get(object);
        if (address == null) {
            return 0;
        } else {
            return address.intValue();
        }
    }

    /**
     * Set the ROM heap address of a given JVM heap instance.
     *
     * @param object
     * @param klass
     * @param oop
     * @return
     */
    private int setRomizedInstance(Object object, Klass klass, int oop) {
        assume(oop != 0);
        HashMap pool;
        if (klass.isSquawkArray() && SuiteClass.isPrimitiveType(klass.elementType)) {
            object = canonicalizePrimitiveArray(object, klass);
            pool = romizedPrimitiveArrayPool;
        } else {
            pool = romizedInstancePool;
        }
        Object existing = pool.put(object, new Integer(oop));
        assume(existing == null);
        return oop;
    }

/*-----------------------------------------------------------------------*\
 *              Object creation methods                                  *
\*-----------------------------------------------------------------------*/

    /**
     * Return true of the char array only contains 8 bit chars
     */
    private static boolean isEightBit(char[] chars) {
        int lth = chars.length;
        for (int i = 0 ; i < lth ; i++) {
            if (chars[i] > 0xFF) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convert a char[] to a byte[]
     */
    private static byte[] asByteArray(char[] chars) {
        int lth = chars.length;
        byte[] res = new byte[lth];
        for (int i = 0 ; i < lth ; i++) {
            res[i] = (byte)chars[i];
        }
        return res;
    }

    /**
     * Set the value of a field in an object or an element in an array.
     *
     * @param oop
     * @param type
     * @param offset
     * @param value
     */
    private void setValue(int oop, int type, int offset, Object value, boolean oopIsArray) {
        switch (type) {
            case CNO.BYTE:
                setByte(oop, offset, ((Byte)value).byteValue());
                break;
            case CNO.BOOLEAN:
                setByte(oop, offset, value == Boolean.TRUE ? 1 : 0);
                break;
            case CNO.SHORT:
                setHalf(oop, offset, ((Short)value).shortValue());
                break;
            case CNO.CHAR:
                setHalf(oop, offset, ((Character)value).charValue());
                break;
            case CNO.INT:
                setWord(oop, offset, ((Integer)value).intValue());
                break;
            case CNO.FLOAT:
                setWord(oop, offset, Float.floatToIntBits(((Float)value).floatValue()));
                break;
            case CNO.LONG:
                if (!oopIsArray) {
                    setLongAtWord(oop, offset, ((Long)value).longValue());
                } else {
                    setLong(oop, offset, ((Long)value).longValue());
                }
                break;
            case CNO.DOUBLE:
                if (!oopIsArray) {
                    setLongAtWord(oop, offset, Double.doubleToLongBits(((Long)value).longValue()));
                } else {
                    setLong(oop, offset, Double.doubleToLongBits(((Long)value).longValue()));
                }
                break;
            default:
                setWord(oop, offset, createInstance(value));
                break;
        }
    }

    /**
     * Copy an array object from the JVM heap to the ROM heap.
     *
     * @param suite The array object to copy.
     * @return the address of the array object in the ROM heap.
     */
    private int createArray(Object array, Klass arrayClass) {
        assume(arrayClass.isSquawkArray());

        // Convert instances that are represented in the Squawk system
        // as special arrays into the appropriate array instances.
/*if[NEWSTRING]*/
        if (arrayClass == klassString) {
            char[] chars = array.toString().toCharArray();
            if (isEightBit(chars)) {
                arrayClass = klassStringOfBytes;
                array = asByteArray(chars);
            } else {
                array = chars;
            }
        }
/*end[NEWSTRING]*/
/*if[NEWSYMBOLS]*/
        if (arrayClass == klassStringOfSymbols) {
            char[] chars = array.toString().toCharArray();
            if (isEightBit(chars)) {
                array = asByteArray(chars);
            } else {
                throw new Error("Symbols not 8 bit");
            }
        }
/*end[NEWSYMBOLS]*/

        int length = Array.getLength(array);
        char elementType = arrayClass.elementType;
        assume(SuiteManager.lookup(elementType) != null);

/*if[NEWSTRING]*/
        if (arrayClass == klassString)          assume(elementType == CNO.CHAR);
        if (arrayClass == klassStringOfBytes)   assume(elementType == CNO.BYTE);
        if (arrayClass == klassStringOfSymbols) assume(elementType == CNO.BYTE);
/*end[NEWSTRING]*/

        int oop = getRomizedInstance(array, arrayClass);
        if (oop == 0) {
            oop = allocArray(arrayClass, length);
            setRomizedInstance(array, arrayClass, oop);
            for (int i = 0; i != length; ++i) {
                Object value = Array.get(array, i);
                if (value != null) {
                    int offset = i;
                    setValue(oop, elementType, offset, value, true);
                }
            }
        }
        return oop;
    }

    /**
     * Set the value of a instance field within the ROM heap based on the
     * field's value in the JVM heap.
     *
     * @param oop The address of the instance in the ROM heap.
     * @param object The instance in the JVM heap.
     * @param clazz The class of the instance.
     * @param name The name of the instance field.
     * @param type The Squawk type identifier for the class of the field.
     * @param offset The offset of the field in the ROM heap.
     */
    private void setFieldValue(int oop, Object object, Class clazz, String name, int type, int offset) {
        try {
            Field field = clazz.getDeclaredField(name);
            int modifiers = field.getModifiers();
            // We're not interested in static fields
            if (!Modifier.isStatic(modifiers)) {
                field.setAccessible(true);
                Object value = field.get(object);
                if (value != null) {
                    offset = offset / SuiteLoader.getPackedSize(type);
                    setValue(oop, type, offset, value, false);
                }
            }
        } catch (NoSuchFieldException e) {
            shouldNotReachHere();
        } catch (IllegalAccessException e) {
            shouldNotReachHere();
        }

    }

    /**
     * Copy the fields of a JVM heap instance into the ROM heap.
     *
     * @param oop
     * @param object
     * @param clazz
     * @param klass
     * @param sc
     */
    private void setFieldValues(int oop, Object object, Class clazz, Klass klass, SuiteClass sc) {
        while (sc != null) {
            StringOfSymbols symbols = sc.symbols;
            int count = symbols.getMemberCount(SquawkConstants.MTYPE_INSTANCEFIELDS);
            for (int i = 0; i != count; ++i) {
                int id = symbols.lookupMemberID(SquawkConstants.MTYPE_INSTANCEFIELDS, i);
                String name = symbols.getMemberName(id);
                int offset = symbols.getMemberOffset(id);
                int type = symbols.getMemberType(id);

                if (type == CNO.LONG2 || type == CNO.DOUBLE2) {
                    continue;
                }
                setFieldValue(oop, object, clazz, name, type, offset);
            }
            klass = klass.getSuperClass();
            sc = getSuiteClassFromKlass(klass);
        }
    }

    /**
     * Copy an object from the JVM heap to the ROM heap.
     *
     * @param object The object to copy.
     * @return the address of the object in the ROM heap.
     */
    private int createInstance(Object object) {
        assume(object != null);
        Class clazz = object.getClass();
        Klass klass = getKlassFromClass(clazz);
        SuiteClass sc = getSuiteClassFromKlass(klass);

        if (klass.isSquawkArray()) {
            return createArray(object, klass);
        }

        int oop = getRomizedInstance(object, klass);
        if (oop == 0) {
            if (klass == klassClass) {
                Klass klassObj = (Klass)object;
                if (isClassAllocated(klassObj)) {
                    oop = getClassOop(klassObj);
                } else {
                    oop = allocClass(klassObj);
                }
            } else {
                oop = allocInstance(klass);
            }
            setRomizedInstance(object, klass, oop);
            setFieldValues(oop, object, clazz, klass, sc);
        }

        return oop;
    }


/*-----------------------------------------------------------------------*\
 *                    Class to Klass mapping methods                     *
\*-----------------------------------------------------------------------*/

    private Map classKlassMap = new HashMap();

    /**
     * Get the Squawk class name corresponding to a standard Java Class name.
     *
     * @param c
     * @return
     */
    private String getKlassName(Class c) {
        if (c.isArray()) {
            return "[" + getKlassName(c.getComponentType());
        } else if (c.isPrimitive()) {
            String name = c.getName();
            switch (name.charAt(0)) {
                case 'i': return "java.lang._int_";
                case 'l': return "java.lang._long_";
                case 'f': return "java.lang._float_";
                case 'd': return "java.lang._double_";
                case 'b': return name.charAt(1) == 'o' ? "java.lang._boolean_" : "java.lang._byte_";
                case 'c': return "java.lang._char_";
                case 's': return "java.lang._short_";
                case 'v': return "java.lang._void_";
                default: shouldNotReachHere();
                    return null;
            }
        } else {
            if (c == Klass.class) {
                return "java.lang.Class";
            }
            return c.getName();
        }
    }

    /**
     * Get the Klass instance corresponding to a Class instance.
     *
     * @param c
     * @return
     */
    private Klass getKlassFromClass(Class c) {
        Klass klass = (Klass)classKlassMap.get(c);
        if (klass == null) {
            String name = getKlassName(c);
            // Convert to Squawk name
            klass = SuiteManager.forName(name);
            assume(klass != null);
            if (!isClassAllocated(klass)) {
                allocClass(klass);
            }
            classKlassMap.put(c, klass);
        }
        return klass;
    }

/*-----------------------------------------------------------------------*\
 *                    Klass to SuiteClass mapping methods                *
\*-----------------------------------------------------------------------*/

    private final Map suiteClassMap;
    private SuiteClass getSuiteClassFromKlass(Klass klass) {
        return (SuiteClass)suiteClassMap.get(klass);
    }

/*-----------------------------------------------------------------------*\
 *                      'romize' and 'bootstrap' methods                 *
\*-----------------------------------------------------------------------*/

    /** Klass objects that are special to the builder. */
    private Klass klassClass;
    private Klass klassObject;
    private Klass klassString;
    private Klass klassStringOfBytes;
    private Klass klassStringOfSymbols;
    private Klass klassSuite;

    /**
     * Allocate the Class instances in the ROM heap for the
     * Klass instances in the JVM heap representing the bootstrap classes.
     *
     * @param suite0classes
     */
    private void bootstrap(Klass[] suite0classes) {

        // Allocate class "java.lang.Object"
        int classObject = allocClass(klassObject);

        // Allocate class "java.lang.Class"
        int classClass = allocClass(klassClass);

        // Fix up class pointer for "java.lang.Object" and "java.lang.Class"
        Object_setClass(classObject, classClass);
        Object_setClass(classClass,  classClass);

        // Allocate remainder of hard coded class number bootstrap types
        for (char type = CNO.FIRST_IMPLICIT_TYPE; type <= CNO.LAST_IMPLICIT_TYPE; type++) {
            Klass klass = suite0classes[type];
            if (klass != null && !isClassAllocated(klass)) {
                allocClass(klass);
            }
        }
    }

    /**
     * Copy an object from the JVM heap to the ROM heap and record the
     * amount of bytes by which the ROM heap grows.
     *
     * @param object The object to copy.
     * @param sizes The table in which to record the amount of bytes allocated
     * in ROM during the copying of 'object'.
     * @param index The index of the entry in 'table' to update.
     * @return the address of the object in the ROM heap.
     */
    private int createInstanceWithAccounting(Object object, int[] sizes, int index) {
        int before = romUsed();
        int oop = createInstance(object);
        sizes[index] += (romUsed() - before);
        return oop;
    }

    /**
     * Copy a set of objects from the JVM heap into the ROM heap.
     *
     * @param suites
     * @param methodDebugTable
     * @return
     */
    public int[] romize(Suite[] suites, IntHashtable methodDebugTable) {

        Suite   suite0        = suites[0];
        Klass[] suite0classes = suite0.classes;
        int[] sizes           = new int[suites.length+1];

        klassClass           = suite0classes[CNO.CLASS];
        klassObject          = suite0classes[CNO.OBJECT];
        klassString          = suite0classes[CNO.STRING];
        klassStringOfBytes   = suite0classes[CNO.STRING_OF_BYTES];
        klassStringOfSymbols = suite0classes[CNO.STRING_OF_SYMBOLS];
        klassSuite           = suite0.forName("java.lang.Suite");

        bootstrap(suite0classes);
        sizes[0] = romUsed();

        for (int i = 0; i != suites.length; ++i) {
            createInstanceWithAccounting(suites[i], sizes, i);
        }

        // Set suiteTable root
        setSuiteTableRom(createInstanceWithAccounting(suites, sizes, 0));

        // Set the pointer for java.lang.VMExtension
        int javaLangVMExtension = getClassOop(SuiteManager.forName("java.lang.VMExtension"));
        assume(javaLangVMExtension != 0);
        setJavaLangVMExtension(javaLangVMExtension);

/*if[DEBUG.METHODDEBUGTABLE]*/
        if (methodDebugTable != null) {
            setMethodDebugTableRom(createInstanceWithAccounting(methodDebugTable, sizes, suites.length));
        }
/*end[DEBUG.METHODDEBUGTABLE]*/

        // Do a GC of ROM if it was initialized as a semi-space
        // garbage collectable memory segment.
        if (gcRom()) {
            assumeCurrentSegment(ROM);
            initializeCollector(true);
            gc();
            gc();
        }

        if (getTraceHeap()) {
            traceHeap(ROM, true);
        }

        /* Allocation & GC happen in RAM by default. */
        setCurrentSegment(RAM, MMR[MMR_ramSize]);
        initializeCollector(true);

        PlatformAbstraction_finalize();
        return sizes;
    }

}

