package com.sun.squawk.suite;

import java.io.*;
import com.sun.squawk.loader.Member;
import com.sun.squawk.loader.Verifier;
import com.sun.squawk.loader.SuiteLoader;
import com.sun.squawk.vm.CNO;
import com.sun.squawk.vm.SquawkConstants;

/**
 * This is an implementation of SuiteParser that is used to load suites from
 * a stream whose format complies with the Suite File Format described in the
 * The Squawk System specification.
 */
public class BinarySuiteParser implements SuiteParser {

    /**
     * The InputStream this parser reads from.
     */
    protected final DataInputStream in;

    /**
     * Flag indicating if the parser has read a 'Type_info' structure from the
     * stream for the current suite.
     */
    private boolean startedReadingClasses;

    /**
     * Flag indicating if the parser has read a 'MethodImpl_info' structure
     * from the stream for the current suite.
     */
    private boolean startedReadingMethods;

    /**
     * A counter indicating the number of 'Type_info' structures that remain
     * to be read from the stream for the current suite.
     */
    private int classesToReadCounter;

    /**
     * A counter indicating the number of 'MethodImpl_info' structures that
     * remain to be read from the stream for the current suite.
     */
    private int methodsToReadCounter;

    /**
     * A buffer used while reading the parameters for a method.
     */
    private char[] parmBuf = new char[16];

    /**
     * A general purpose byte buffer used while reading various components
     * from the stream.
     */
    private ByteBuffer byteBuf = new ByteBuffer();

    /**
     * Create a new BinarySuiteParser.
     */
    public BinarySuiteParser(InputStream ins) {
        if (ins instanceof DataInputStream) {
            in = (DataInputStream)ins;
        } else {
            in = new DataInputStream(ins);
        }
    }

    /**
     * Advance the stream read position to the start of the next suite.
     *
     * @return this parser with the read position placed at the start of the
     * next suite in the stream or null if the stream is at EOF.
     * @throws IOException if there is a problem reading from the stream
     * @throws LinkageError if the stream does not have expected file format
     * magic number.
     */
    public SuiteParser nextSuite() throws IOException {
        if (in.available() > 0) {
            startedReadingClasses = false;
            startedReadingMethods = false;
            parseMagicAndVersionNumbers();
            return this;
        } else {
            return null;
        }
    }

    /**
     * Read a suite header from the source.
     *
     * @return the suite header read.
     * @throws IOException
     */
    public SuiteHeader readSuiteHeader() throws IOException {
        int access = in.readUnsignedByte() &
               SquawkConstants.RECOGNIZED_SUITE_FILE_SUITE_ACCESS_FLAG_MASK;
        String name = in.readUTF();
        int maxType = readType();
        String[] binds = new String[in.readUnsignedShort()];
        for (int i = 0; i != binds.length; ++i ) {
            binds[i] = in.readUTF();
        }
        return new SuiteHeader(access, name, maxType, binds);
    }

    /**
     * Read a list of names from the stream that represent
     * a list of suites that the suite currently being read binds to.
     *
     * @return the list of names read.
     * @throws IOException
     */
    public String[] readBindList() throws IOException {
        String[] arr = new String[in.readUnsignedShort()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = in.readUTF();
        }
        return arr;
    }


    /**
     * Read a 'Type_info' structure from the stream.
     *
     * @return a SuiteClass representing the data in the structure read or
     * or null if there is no 'Type_info' structure at the current
     * position in the input stream.
     * @throws IOException
     */
    public SuiteClass readClass() throws IOException {

        if (!startedReadingClasses) {
            classesToReadCounter = in.readUnsignedShort();
            startedReadingClasses = true;
        }

        if (classesToReadCounter-- == 0) {
            return null;
        }

/*if[DEBUG.LOADER]*/
        statsClassesRead++;
/*end[DEBUG.LOADER]*/

        // Read 'name'.
        byteBuf.reset();
        byteBuf.addUTF8(in);

        // Initialize the symbols builder
        SymbolBuilder buf = new SymbolBuilder(byteBuf);

        // Read 'access_flags'.
        char access_flags = (char)(in.readUnsignedShort() &
                                   SquawkConstants.RECOGNIZED_SUITE_FILE_CLASS_ACCESS_FLAG_MASK);

        // Read 'this_type'
        char this_type = readType();

        // Read 'super_class'.
        char super_class = readType();

        // Read 'interfaces'.
        int interfaces_count = in.readUnsignedShort();
        char[] interfaceTypes;
        char[][] interfaceSlotTables;
        if (interfaces_count > 0) {
            interfaceTypes      = new char[interfaces_count];
            interfaceSlotTables = new char[interfaces_count][];
        } else {
            interfaceTypes      = (char[]  )PersistentMemory.get("new char[0]");
            interfaceSlotTables = (char[][])PersistentMemory.get("new char[0][]");
        }
        parseInterfaceList(interfaceTypes, interfaceSlotTables, isInterface(access_flags));

        // Read 'static_fields'
        parseFieldList(buf, SquawkConstants.MTYPE_STATICFIELDS);

        // Read 'instance_fields'
        parseFieldList(buf, SquawkConstants.MTYPE_INSTANCEFIELDS);

        // Read 'static_methods'
        if (parseMethodsList(buf, SquawkConstants.MTYPE_STATICMETHODS)) {
            access_flags |= SquawkConstants.ACC_MUSTCLINIT;
        }

        // Read 'virtual_methods'
        parseMethodsList(buf, SquawkConstants.MTYPE_VIRTUALMETHODS);

        // Read 'overriding'
        int[] overriding;
        int overriding_count = in.readUnsignedShort();
        if (overriding_count > 0) {
            overriding = new int[overriding_count];
        } else {
            overriding = (int[])PersistentMemory.get("new int[0]");
        }
        parseOverriddenList(overriding);

        // Read 'class_refs'.
        int class_refs_count = in.readUnsignedShort();
        char[] class_refs;
        if (class_refs_count > 0) {
            class_refs = new char[class_refs_count];
        } else {
            class_refs = (char[])PersistentMemory.get("new char[0]");
        }
        parseTypeList(class_refs);

        // Read 'objects'.
        int objects_count = in.readUnsignedShort();
        Object[] objects;
        if (objects_count > 0) {
            objects = new Object[objects_count];
        } else {
            objects = (Object[])PersistentMemory.get("new Object[0]");
        }
        parseObjectList(objects);

        StringOfSymbols symbols = buf.toStringOfSymbols();
        return new SuiteClass(symbols,
                              this_type,
                              access_flags,
                              super_class,
                              interfaceTypes,
                              interfaceSlotTables,
                              class_refs,
                              objects,
                              overriding);
    }

    /**
     * Read a sequence of 'Attribute_info' structures from the stream that are
     * associated with a given class. This parser simply
     * skips all class attributes.
     *
     * @param sc The class to which the attributes pertain.
     * @throws IOException
     */
    public void readClassAttributes(SuiteClass sc) throws IOException {
        // Read 'attributes_count'
        int attributes_count = in.readUnsignedShort();
        while (attributes_count-- != 0) {
            String attribute_name = in.readUTF();
            int attribute_length = in.readInt();
            in.skipBytes(attribute_length);
        }
    }

    /**
     * Read a 'MethodImpl_info' structure from the stream.
     *
     * @return a SuiteMethodImpl representing the data in the structure read or
     * or null if there is no 'MethodImpl_info' structure at the current
     * position in the input stream.
     * @throws IOException
     */
    public SuiteMethodImpl readMethodImpl() throws IOException {

        if (!startedReadingMethods) {
            methodsToReadCounter = in.readUnsignedShort();
            startedReadingMethods = true;
        }

        if (methodsToReadCounter-- == 0) {
            return null;
        }

        // Read 'ofClass'.
        char ofClass = readType();

        // Read 'index'
        int index = in.readUnsignedShort();

        // Read 'access_flags'
        int access_flags = in.readUnsignedShort() & SquawkConstants.RECOGNIZED_SUITE_FILE_METHOD_IMPL_ACCESS_FLAG_MASK;

        // Read the 'Code_info' structure if the access flags indicate there is one.
        SuiteMethodImpl methodImpl;
        if ((access_flags & SquawkConstants.ACC_ABSTRACT) == 0 && (access_flags & SquawkConstants.ACC_NATIVE) == 0) {

            // Read 'locals'
            int locals_count = in.readUnsignedShort();
            char[] locals    = new char[locals_count];
            parseTypeList(locals);

            // Read 'stack_size'
            int stack_size = in.readUnsignedShort();

            // Read 'exception_table'
            int exception_table_length = in.readUnsignedByte();
            ExceptionHandler[] exception_table  = new ExceptionHandler[exception_table_length];
            parseExceptionHandlers(exception_table);

            // Read 'code'
            int code_length = in.readInt();
/*if[DEBUG.LOADER]*/
            statsMethodBodiesRead++;
            statsBytecodesRead += code_length;
/*end[DEBUG.LOADER]*/
            return SuiteMethodImpl.create(
                ofClass,
                index,
                access_flags,
                locals,
                stack_size,
                exception_table,
                code_length,
                true,
                in);
        } else {
            return SuiteMethodImpl.create(
                ofClass,
                index,
                access_flags,
                null,
                -1,
                null,
                -1,
                true,
                null);
        }
    }

    /**
     * Read a sequence of 'Attribute_info' structures from the stream that are
     * associated with a given method implementation. This parser simply
     * skips all method implementation attributes.
     *
     * @param methodImpl The method implementation to which the
     * attributes pertain.
     * @throws IOException
     */
    public void readMethodImplAttributes(SuiteMethodImpl methodImpl) throws IOException {
        if ((methodImpl.flags & SquawkConstants.ACC_ATTRIBUTES) != 0) {
            int attributes_count = in.readUnsignedShort();
            while (attributes_count-- != 0) {
                String attribute_name = in.readUTF();
                int attribute_length = in.readInt();
                in.skipBytes(attribute_length);
            }
        }
    }

    /**
     * Parse the 'magic', 'minor_version' and 'major_version' components of a
     * 'SuiteFile' structure.
     *
     * @throws IOException if the magic number is incorrect or there is
     * @throws LinkageError if the stream does not have expected file format
     * magic number.
     */
    private void parseMagicAndVersionNumbers() throws IOException {
        // Read the magic number
        int magic = in.readInt();
        if (magic != 0xCAFEFACE) {
            throw new LinkageError("ClassFormatError: Incorrect magic number: 0x"+Integer.toHexString(magic));
        }

        // Read (and ignore) the version numbers
        in.readUnsignedShort(); // minor
        in.readUnsignedShort(); // major
    }

    /**
     * Parse a sequence of 'Interface_info' structures from the stream.
     *
     * @param interfaceTypes
     * @param interfacesSlotTables
     * @param isInterface
     * @throws IOException
     */
    private void parseInterfaceList(char[] interfaceTypes,
                                    char[][] interfacesSlotTables,
                                    boolean isInterface) throws IOException
    {
        for (int i = 0; i < interfaceTypes.length; i++) {
            interfaceTypes[i] = readType();

            int length = in.readUnsignedShort();
            if (length > 0) {
                char[] slots = interfacesSlotTables[i] = new char[length];
                for (int j = 0; j < length; j++) {
                    slots[j] = (char)in.readUnsignedShort();
                }
            } else {
                interfacesSlotTables[i] = (char[])PersistentMemory.get("new char[0]");
            }
        }
    }

   /**
    * Parse a sequence of 'Field_info' structures from the stream.
    *
    * @param buf The buffer used to store the data from the read structures.
    * @param mtype The tag describing the category of the data added
    * to 'buf' (if any).
    * @throws IOException
    */
    private void parseFieldList(SymbolBuilder buf, int mtype) throws IOException {
        int fields_count = in.readUnsignedShort();
        if (fields_count > 0) {
            buf.addSegmentMarker(mtype);
/*if[DEBUG.LOADER]*/
            statsFieldsRead += fields_count;
/*end[DEBUG.LOADER]*/
            for (int i = 0; i < fields_count; i++) {
                byteBuf.reset();
                int access_flags = in.readUnsignedShort() &
                                   SquawkConstants.RECOGNIZED_SUITE_FILE_FIELD_ACCESS_FLAG_MASK;
                byteBuf.addAccess(access_flags);
                byteBuf.addOffset(i);
                int type = readType();
                if (hasSymbolic(access_flags)) {
                    byteBuf.addUTF8(in); //symbolic info exists
                }
                byteBuf.addDash();
                byteBuf.addType(type);
                buf.addBuffer(byteBuf);
            }
        }
    }

    /**
     * Parse a sequence of 'Method_info' structures from the stream.
     *
     * @param buf The buffer used to store the data from the read structures.
     * @param mtype The tag describing the category of the data added
     * to 'buf' (if any).
     * @return true if the special class initialization method (i.e. <clinit>)
     * was parsed.
     * @throws IOException
     */
    private boolean parseMethodsList(SymbolBuilder buf, int mtype) throws IOException {
        int methods_count = in.readUnsignedShort();
        boolean parsedClinit = false;
        if (methods_count > 0) {
            buf.addSegmentMarker(mtype);
/*if[DEBUG.LOADER]*/
            statsMethodsRead += methods_count;
/*end[DEBUG.LOADER]*/
            for (int i = 0; i < methods_count; i++) {
                byteBuf.reset();
                int access_flags = in.readUnsignedShort() & SquawkConstants.RECOGNIZED_SUITE_FILE_METHOD_ACCESS_FLAG_MASK;
                byteBuf.addAccess(access_flags);
                byteBuf.addOffset(i);
                int returnType = readType();
                int parmsCount = in.readUnsignedShort();
                if (parmBuf.length < parmsCount) {
                    parmBuf = new char[parmsCount];
                }
                for (int j = 0; j < parmsCount; j++) {
                    parmBuf[j] = readType();
                }
                if (hasSymbolic(access_flags)) {
                    int len = byteBuf.addUTF8(in); //symbolic info exists
                    if (mtype == SquawkConstants.MTYPE_STATICMETHODS &&
                        parmsCount == 0 &&
                        returnType == CNO.VOID &&
                        len == 8 &&
                        byteBuf.endsWith("<clinit>"))
                    {
                        parsedClinit = true;
                    }
/*if[FINALIZATION]*/
                    // If this method is the special 'finalize' method then
                    // set the access flag indicating that this class has such
                    // a method
                    else if (parmsCount == 1 &&
                        mtype == SquawkConstants.MTYPE_VIRTUALMETHODS &&
                        returnType == CNO.VOID &&
                        len == 8 &&
                        byteBuf.endsWith("finalize") &&
                        buf.startsWith("java.lang.Object"))
                    {
                        SuiteLoader.setSlotForFinalize(i);
                    }
/*end[FINALIZATION]*/
                }
                byteBuf.addDash();
                byteBuf.addType(returnType);
                for (int j = 0; j < parmsCount; j++) {
                    byteBuf.addParm(parmBuf[j]);
                }
                buf.addBuffer(byteBuf);
            }
        }
        return parsedClinit;
    }

    /**
     * Parse a sequence of 'Overriding_info' structures from the stream. The
     * number of structures to read is equal to the length of 'arr'.
     *
     * @param arr The array to store the read data into. Each 'Overriding_info'
     * structure read stores an entry into this array, where the high 16 bits of
     * the entry correspond to the 'vindex' item and the low 16 bits corresponds
     * to the 'access_flags' item.
     * @throws IOException
     */
    private void parseOverriddenList(int[] arr) throws IOException {
        for (int i = 0; i < arr.length; i++) {
            int vindex = in.readUnsignedShort();
            int access_flags = in.readUnsignedShort();
            arr[i] = (access_flags << 16) | vindex;
        }
    }

    /**
     * Parse a sequence of 'Type' items from the stream. The number of items
     * to read is equal to the length of 'arr'.
     *
     * @param arr The array to store the read data into.
     * @throws IOException
     */
    private void parseTypeList(char[] arr) throws IOException {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = readType();
        }
    }

    /**
     * Parse a sequence of 'Object_info' structure from the stream. The number
     * of structure to read is equal to the length of 'arr'.
     *
     * @param arr The array to store the read data into.
     * @throws IOException
     */
    private void parseObjectList(Object[] arr) throws IOException {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = parseObject();
        }
    }

    /**
     * Parse a single 'Object_info' structure from the stream.
     *
     * @return the Object representing the read structure.
     * @throws IOException
     */
    private Object parseObject() throws IOException {
        int tag = 0;
        tag = in.readUnsignedByte();
        switch (tag) {
            case SquawkConstants.CONSTANT_String:
                //String
                return in.readUTF();
            case SquawkConstants.CONSTANT_Int_array:
                //int[]
                int[] arr = new int[in.readUnsignedShort()];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = in.readInt();
                }
                return arr;
            case SquawkConstants.CONSTANT_Short_array:
                //short[]
                short[] arr2 = new short[in.readUnsignedShort()];
                for (int i = 0; i < arr2.length; i++) {
                    arr2[i] = in.readShort();
                }
                return arr2;
            case SquawkConstants.CONSTANT_Char_array:
                char[] arr3 = new char[in.readUnsignedShort()];
                for (int i = 0; i < arr3.length; i++) {
                    arr3[i] = in.readChar();
                }
                return arr3;
            case SquawkConstants.CONSTANT_Byte_array:
                byte[] arr4 = new byte[in.readUnsignedShort()];
                for (int i = 0; i < arr4.length; i++) {
                    arr4[i] = in.readByte();
                }
                return arr4;
            default:
                throw new LinkageError("ClassFormatError: invalid Object_info tag: " + tag);
        }
    }

    /**
     * Parse a sequence of 'exception_table' entries from the stream. The number
     * of entries to read is equal to the length of 'arr'.
     *
     * @param arr
     * @throws IOException
     */
    private void parseExceptionHandlers(ExceptionHandler[] arr)
                                 throws IOException {

        for (int i = 0; i < arr.length; i++) {
            arr[i] = new ExceptionHandler(
                    (char)in.readInt(),
                    (char)in.readInt(),
                    (char)in.readInt(),
                    readType());
        }
    }

    /**
     * Determine whether or not the bit is set in the specified access flags
     * indicating that a class is an interace.
     * @param access
     * @return
     */
    private static boolean isInterface(int access) {
        return ((access & SquawkConstants.ACC_INTERFACE) != 0);
    }

    /**
     * Determine whether or not the bit is set in the specified access flags
     * indicating that a method or field has symbolic info.
     * @param access
     * @return
     */
    static boolean hasSymbolic(int access) {
        return ((access & SquawkConstants.ACC_SYMBOLIC) != 0);
    }

    /**
     * Read an unsigned short from the stream which represents class number.
     * @return the unsigned short read.
     * @throws IOException
     */
    private char readType() throws IOException {
        return (char)in.readUnsignedShort();
    }

/*if[DEBUG.LOADER]*/

    /** A count of the number of classes read. */
    private static int statsClassesRead;

    /** A count of the number of classes read. */
    private static int statsMethodsRead;

    /** A count of the number of classes read. */
    private static int statsFieldsRead;

    /** A count of the number of classes read. */
    private static int statsMethodBodiesRead;

    /** A count of the number of bytecodes read. */
    private static int statsBytecodesRead;

    /** Timer for statistics. */
    private static long time = System.currentTimeMillis();

    /**
     * Prints
     * @param out
     */
    public void printStats(PrintStream out) {
        out.println("Class    count = "+statsClassesRead);
        out.println("Method   count = "+statsMethodsRead);
        out.println("Field    count = "+statsFieldsRead);
        out.println("Body     count = "+statsMethodBodiesRead);
        out.println("Bytecode count = "+statsBytecodesRead);
        out.println("Time           = "+(System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        statsClassesRead = statsMethodsRead = statsFieldsRead = statsMethodBodiesRead = statsBytecodesRead = 0;
    }
/*end[DEBUG.LOADER]*/
}