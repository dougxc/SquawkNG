//if[DEBUG.LOADER]   /* This will selectively exclude the entire file from the build */
package com.sun.squawk.suite;

import java.io.*;
import com.sun.squawk.loader.Member;
import com.sun.squawk.loader.Verifier;
import com.sun.squawk.loader.SuiteLoader;
import com.sun.squawk.xml.Tag;
import com.sun.squawk.vm.CNO;
import java.util.StringTokenizer;
import com.sun.squawk.vm.SquawkConstants;

/**
 * This is an implementation of SuiteParser that is used to load suites from
 * a stream whose format is XML.
 */
public class XMLSuiteParser implements SuiteParser {

    /**
     * The InputStream this parser reads from.
     */
    private final InputStream in;

    /**
     * The entry point to the next '<suite>' element.
     */
    private Tag suiteTag;

    /**
     * The last parsed suite header.
     */
    private SuiteHeader suiteHeader;

    /**
     * The last parsed array of classes.
     */
    private SuiteClass[] classes;

    /**
     * The attributes of the last parsed array of classes.
     */
    private Tag[][] classesAttributes;

    /**
     * The last parsed array of method implementations.
     */
    private SuiteMethodImpl[] methods;

    /**
     * The attributes of last parsed array of method implementations.
     */
    private Tag[][] methodsAttributes;

    /**
     * A general purpose byte buffer used while reading various components
     * from the stream.
     */
    private ByteBuffer byteBuf = new ByteBuffer();

    int nextClass;
    int nextMethod;

/*if[DEBUG.LOADER]*/
    public void printStats(PrintStream out) {
        out.println("No Stats in XMLSuiteParser");
    }
/*end[DEBUG.LOADER]*/

/* ------------------------------------------------------------------------ *\
 *                              Public interface                            *
\* ------------------------------------------------------------------------ */

    /**
     * Create a new XMLSuiteParser.
     */
    public XMLSuiteParser(InputStream in) {
        this.in = in;
        int ch;
        try {
            // skip leading whitespace
            while ((ch = in.read()) != '<' && ch != -1) {
                if (ch != ' ' && ch != '\t') {
                    throw new RuntimeException(
                        "Found bad character while looking for '<' in XML file: " +
                        ch);
                }
            }

            suiteTag = Tag.create(in, '<');
        } catch (IOException ioe) {
            throw new RuntimeException("Error at head of XML input stream: "+ioe);
        }
    }

    /**
     * Advance the stream read position to the start of the next suite.
     *
     * @return this parser with the read position placed at the start of the
     * next suite in the stream or null if the stream is at EOF.
     * @throws IOException if there is a problem reading from the stream
     * @throws LinkageError if the data in the stream does not appear to be
     * in the format expected by the parser.
     */
    public SuiteParser nextSuite() throws IOException {
        Tag tag  = suiteTag;
        if (tag == null) {
            return null;
        }
        suiteTag = suiteTag.getNext();
        tag.checkName("suite");
        tag = tag.getContent();
        tag = parseSuiteHeader(tag);
        tag = parseClasses(tag);
        tag = parseMethods(tag);
        return this;
    }

    /**
     * Read a suite header from the source.
     *
     * @return the suite header read.
     * @throws IOException
     */
    public SuiteHeader readSuiteHeader() throws IOException {
        return suiteHeader;
    }

    /**
     * Read a class from the stream.
     *
     * @return the class read or null if there is no class at the current
     * position in the input stream.
     * @throws IOException
     */
     public SuiteClass readClass() throws IOException {
         if (nextClass >= classes.length) {
             return null;
         }
         return classes[nextClass++];
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
     }

    /**
     * Read a method implementation from the stream.
     *
     * @return the method implementation read or null if there is no method
     * implementation at the current position in the input stream.
     * @throws IOException
     */
    public SuiteMethodImpl readMethodImpl() throws IOException {
        if (nextMethod >= methods.length) {
            return null;
        }
        return methods[nextMethod++];
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
    }

    /**
     * Return the list of class attributes for the last class returned by
     * 'readClass'. This allows a subclass of this parser to handle attributes.
     *
     * @return the list of class attributes for the last class returned by
     * 'readClass' or null if there are none.
     */
    protected Tag[] getClassAttributes() {
        return classesAttributes[nextClass - 1];
    }

    /**
     * Return the list of method implementation attributes for the last
     * method implementation returned by 'readMethodImpl'. This allows a
     * subclass of this parser to handle attributes.
     *
     * @return the list of method implementation attributes for the last
     * method implementation returned by 'readMethodImpl' or null if there
     * are none.
     */
    protected Tag[] getMethodImplAttributes() {
        return methodsAttributes[nextMethod - 1];
    }


    /* ------------------------------------------------------------------------ *\
     *                                   Parsing                                *
    \* ------------------------------------------------------------------------ */

    /**
     * Parse a <name> element, returning its data.
     */
    protected String parseName(Tag nam) {
        nam.checkName("name");
        Tag cont = nam.getContent();
        if (cont == null) {
            return "";
        }
        return Tag.unescape(cont.getData());
    }

    /*
     * parseNameList
     */
    protected String[] parseNameList(Tag nam) {
        int count = Tag.countTags(nam, "name");
        String[] result = new String[count];
        for (int i = 0 ; i < count ; i++) {
            result[i] = parseName(nam);
            nam = nam.getNext();
        }
        return result;
    }

    /*
     * parseInt
     */
    protected int parseInt(Tag data) {
        try {
            return Integer.parseInt(data.getData());
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Number format error");
        }
    }

    /*
     * parseLong
     */
    protected long parseLong(Tag data) {
        try {
            return Long.parseLong(data.getData());
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Number format error");
        }
    }

    /**
     * Parse the data of a given content tag as an integer and return the
     * parsed integer value.
     *
     * @param content The tag containg the integer.
     * @param name The expected name of the tag.
     * @return the parsed integer value.
     */
    protected int parseInt(Tag content, String name) {
        content.checkName(name);
        return parseInt(content.getContent());
    }

    /*
     * parseChar
     */
    protected char parseChar(Tag data, String name) {
        return (char)parseInt(data, name);
    }

    /**
     * Parse the suite header.
     *
     * @param tag The contents of the <suite> element.
     * @return the remainder of the contents of the <suite> element once the
     * suite header has been parsed.
     */
    private Tag parseSuiteHeader(Tag tag) {

        int access = 0;
        if (tag.isName("access")) {
            access = parseAccess(tag);
            tag = tag.getNext();
        }

        String name = parseName(tag);
        tag = tag.getNext();

        int maxType = 0;
        if (tag.isName("highest")) {
            maxType = parseInt(tag, "highest");
            tag = tag.getNext();
        }

        String[] binds = null;
        if (tag.isName("bind")) {
            binds = parseNameList(tag.getContent());
            tag = tag.getNext();
        }
        suiteHeader = new SuiteHeader(access, name, maxType, binds);
        return tag;
    }

    /*
     * parseClasses
     */
    private Tag parseClasses(Tag tag) {
        tag.checkName("classes");
        Tag content = tag.getContent();
        int count = Tag.countTags(content, "class");
        classes = new SuiteClass[count];
        classesAttributes = new Tag[count][];
        int lastClassNumber = 0;
        for (int i = 0 ; i < count ; i++) {
            parseClass(content.getContent(), i);
            if (classes[i] == null || classes[i].getType() == 0) {
                throw new Error();
                //classes[i].type = (char)(lastClassNumber + 1);
            }
            lastClassNumber = classes[i].getType();
            content = content.getNext();
        }
        return tag.getNext();
    }

    /*
     * parseMethods
     */
    private Tag parseMethods(Tag tag) {
        tag.checkName("methods");
        Tag content = tag.getContent();
        int count = Tag.countTags(content, "method_body");
        methods = new SuiteMethodImpl[count];
        methodsAttributes = new Tag[count][];
        for (int i = 0 ; i < count ; i++) {
            parseMethodImpl(content.getContent(), i);
            content = content.getNext();
        }
        return tag.getNext();
    }

    /*
     * parseClass
     */
    private void parseClass(Tag cls, int index) {
        Member[] staticFields   = null;
        Member[] instanceFields = null;
        Member[] staticMethods  = null;
        Member[] virtualMethods = null;

        char         type = 0;
        char         access = 0;
        char         superType = 0;
        char[]       interfaceTypes;
        char[][]     interfaceSlotTables;
        char[]       classReferences;
        Object[]     objectReferences;
        int[]        overriddenAccess;

        String name = parseName(cls);
        boolean isJavaLangObject = name.equals("java.lang.Object");
        if (name.endsWith(";")) {
            int L = name.indexOf('L');
            Romizer.assume(L != -1);
            name = name.substring(0, L) + name.substring(L+1, name.length() - 1);
        }
        cls = cls.getNext();

        if (cls != null && cls.isName("access")) {
            access = (char)(parseAccess(cls) & SquawkConstants.RECOGNIZED_SUITE_FILE_CLASS_ACCESS_FLAG_MASK);
            cls = cls.getNext();
        }

        if (cls != null && cls.isName("type")) {
            type = parseType(cls);
            cls = cls.getNext();
        }

        if (cls != null && cls.isName("extends")) {
            superType = parseType(cls.getContent());
            cls = cls.getNext();
        }

        if (cls != null && cls.isName("implements")) {
            int count = parseInterfaceCount(cls.getContent());
            interfaceTypes      = new char[count];
            interfaceSlotTables = new char[count][];
            parseInterfaceList(interfaceTypes, interfaceSlotTables, cls.getContent());
            cls = cls.getNext();
        } else {
            interfaceTypes      = new char[0];
            interfaceSlotTables = new char[0][];
        }

        SymbolBuilder buf = new SymbolBuilder(name);

        if (cls != null && cls.isName("static_fields")) {
            parseFieldList(cls.getContent(), buf, SquawkConstants.MTYPE_STATICFIELDS);
            cls = cls.getNext();
        }

        if (cls != null && cls.isName("instance_fields")) {
            parseFieldList(cls.getContent(), buf, SquawkConstants.MTYPE_INSTANCEFIELDS);
            cls = cls.getNext();
        }

        if (cls != null && cls.isName("static_methods")) {
            if (parseMethodList(cls.getContent(), buf, SquawkConstants.MTYPE_STATICMETHODS, isJavaLangObject)) {
                access |= SquawkConstants.ACC_MUSTCLINIT;
            }
            cls = cls.getNext();
        }

        if (cls != null && cls.isName("virtual_methods")) {
            parseMethodList(cls.getContent(), buf, SquawkConstants.MTYPE_VIRTUALMETHODS, isJavaLangObject);
            cls = cls.getNext();
        }

        if (cls != null && cls.isName("overridden_access")) {
            overriddenAccess = parseOverriddenList(cls.getContent());
            cls = cls.getNext();
        } else {
            overriddenAccess = new int[0];
        }

        if (cls != null && cls.isName("class_references")) {
            classReferences = parseTypeList(cls.getContent());
            cls = cls.getNext();
        } else {
            classReferences = new char[0];
        }

        if (cls != null && cls.isName("object_references")) {
            objectReferences = parseObjectList(cls.getContent());
            cls = cls.getNext();
        } else {
            objectReferences = new Object[0];
        }

        classes[index] = new SuiteClass(
                buf.toStringOfSymbols(),
                type,
                (char)access,
                superType,
                interfaceTypes,
                interfaceSlotTables,
                classReferences,
                objectReferences,
                overriddenAccess
        );

        if (cls != null && cls.isName("attributes")) {
            Tag[] attributes = parseAttributeList(cls.getContent());
            classesAttributes[index] = attributes;
            cls = cls.getNext();
        }
    }

    private Tag[] parseAttributeList(Tag lst) {
        int count = Tag.countTags(lst, "attribute");
        Tag[] attributes = new Tag[count];
        for (int i = 0 ; i < count ; i++) {
            attributes[i] = lst.getContent();
            lst = lst.getNext();
        }
        return attributes;
    }

    /*
     * parseIntArray
     */
    private int[] parseIntArray(Tag lst, String name) {
        int count = Tag.countTags(lst, name);
        int[] result = new int[count];
        for (int i = 0 ; i < count ; i++) {
            result[i] = parseInt(lst, name);
            lst = lst.getNext();
        }
        return result;
    }

    /*
     * parseCharArray
     */
    private char[] parseCharArray(Tag lst, String name) {
        int count = Tag.countTags(lst, name);
        char[] result = new char[count];
        for (int i = 0 ; i < count ; i++) {
            result[i] = parseChar(lst, name);
            lst = lst.getNext();
        }
        return result;
    }

    /*
     * parseByteArray
     */
    private byte[] parseByteArray(Tag lst, String name) {
        int count = Tag.countTags(lst, name);
        byte[] result = new byte[count];
        for (int i = 0 ; i < count ; i++) {
            result[i] = (byte)parseInt(lst, name);
            lst = lst.getNext();
        }
        return result;
    }

    /*
     * parseType
     */
    private char parseType(Tag typ) {
        return parseChar(typ, "type");
    }

    /*
     * parseTypeList
     */
    private char[] parseTypeList(Tag typ) {
        return parseCharArray(typ, "type");
    }

    /*
     * parseAccess
     */
    private char parseAccess(Tag acc) {
        acc.checkName("access");
        char result = 0;
        acc = acc.getContent();
        while (acc != null) {
            result |= (char)TagLookup.getAccessCode(acc.getName());
            acc = acc.getNext();
        }
        return result;
    }


    /*
     * parseInterfaceCount
     */
    private int parseInterfaceCount(Tag lst) {
        return Tag.countTags(lst, "interface");
    }

    /*
     * parseInterfaceList
     */
    private void parseInterfaceList(char[] interfaceTypes, char[][] interfaceSlotTables, Tag lst) {
        int count = interfaceTypes.length;
        for (int i = 0 ; i < count ; i++) {
            parseInterface(interfaceTypes, interfaceSlotTables, i, lst.getContent());
            lst = lst.getNext();
        }
    }

    /*
     * parseInterface
     */
    private void parseInterface(char[] interfaceTypes, char[][] interfaceSlotTables, int index, Tag lst) {
        interfaceTypes[index] = parseType(lst);
        lst = lst.getNext();
        if (lst == null) {
            interfaceSlotTables[index] = new char[0];
        } else {
            lst.checkName("slots");
            interfaceSlotTables[index] = parseCharArray(lst.getContent(), "slot");
        }
    }

    /*
     * parseFieldList
     */
    private void parseFieldList(Tag lst, SymbolBuilder buf, int mtype) {
        int count = Tag.countTags(lst, "field");
        if (count != 0) {
            buf.addSegmentMarker(mtype);
            for (int i = 0 ; i < count ; i++) {
                parseField(lst.getContent(), buf, i);
                lst = lst.getNext();
            }
        }
    }

    /*
     * parseField
     */
    private void parseField(Tag fld, SymbolBuilder buf, int slot) {
        int access = parseAccess(fld) & SquawkConstants.RECOGNIZED_SUITE_FILE_FIELD_ACCESS_FLAG_MASK;
        fld = fld.getNext();

        char type = parseType(fld);
        fld = fld.getNext();

        String name;
        if (fld != null && fld.isName("name")) {
            name = parseName(fld);
            fld = fld.getNext();
        } else {
            name = "";
        }
        byteBuf.reset();
        byteBuf.addAccess(access);
        byteBuf.addOffset(slot);
        byteBuf.addString(name);
        byteBuf.addDash();
        byteBuf.addType(type);
        buf.addBuffer(byteBuf);
    }

    /*
     * parseMethodList
     */
    private boolean parseMethodList(Tag lst, SymbolBuilder buf, int mtype, boolean isJavaLangObject) {
        int count = Tag.countTags(lst, "method");
        boolean parsedClinit = false;
        if (mtype != SquawkConstants.MTYPE_VIRTUALMETHODS) {
            isJavaLangObject = false;
        }
        if (count != 0) {
            buf.addSegmentMarker(mtype);
            for (int i = 0 ; i < count ; i++) {
                if (parseMethod(lst.getContent(), buf, i, isJavaLangObject) && mtype == SquawkConstants.MTYPE_STATICMETHODS) {
                    parsedClinit = true;
                }
                lst = lst.getNext();
            }
        }
        return parsedClinit;
    }

    /**
     * parseMethod
     *
     * @return true if the special class initialization method (i.e. <clinit>)
     * was parsed.
     */
    private boolean parseMethod(Tag mth, SymbolBuilder buf, int slot, boolean isJavaLangObject) {
        int access = parseAccess(mth) & SquawkConstants.RECOGNIZED_SUITE_FILE_METHOD_ACCESS_FLAG_MASK;
        mth = mth.getNext();

        char type = parseType(mth);
        mth = mth.getNext();

        char[] parms = parseTypeList(mth.getContent());
        mth = mth.getNext();

        String name;
        boolean isClinit = false;
        if (mth != null && mth.isName("name")) {
            name = parseName(mth);
            if (name.equals("<clinit>") &&
                parms.length == 0 &&
                type == CNO.VOID) {
                isClinit = true;
            }
/*if[FINALIZATION]*/
            if (isJavaLangObject &&
                name.equals("finalize") &&
                parms.length == 1 &&
                type == CNO.VOID) {
                SuiteLoader.setSlotForFinalize(slot);
            }
/*end[FINALIZATION]*/
            mth = mth.getNext();
        } else {
            name = "";
        }
        byteBuf.reset();
        byteBuf.addAccess(access);
        byteBuf.addOffset(slot);
        byteBuf.addString(name);
        byteBuf.addDash();
        byteBuf.addType(type);
        for (int j = 0; j < parms.length; j++) {
            byteBuf.addParm(parms[j]);
        }
        buf.addBuffer(byteBuf);
        return isClinit;
    }

    /*
     * parseOverriddenList
     */
    private int[] parseOverriddenList(Tag lst) {
        int count = Tag.countTags(lst, "method");
        int[] list = new int[count];
        for (int i = 0 ; i < count ; i++) {
            list[i] = parseOverridden(lst.getContent());
        }
        return list;
    }

    /*
     * parseOverridden
     */
    private int parseOverridden(Tag mth) {
        int slot = parseInt(mth, "slot");
        mth = mth.getNext();
        slot += parseAccess(mth) << 16;
        mth = mth.getNext();
        return slot;
    }


    /*
     * parseObjectList
     */
    private Object[] parseObjectList(Tag lst) {
        int count = Tag.countTags(lst, null);
        Object[] result = new Object[count];
        for (int i = 0 ; i < count ; i++) {
            result[i] = parseObject(lst);
//System.out.println("object="+result[i]);
            lst = lst.getNext();
        }
        return result;
    }

    /*
     * parseObject
     */
    private Object parseObject(Tag obj) {
        Tag con = obj.getContent();
        if (obj.isName("string")) {
            if (con == null) {
                return "";
            }
            return Tag.unescape(con.getData());
        } else if(obj.isName("int_array")) {
            return parseIntArray(con, "int");
        } else {
            throw new RuntimeException("Unknown object type: "+obj.getName());
        }
    }

    /*
     * parseMethodImpl
     */
    private SuiteMethodImpl parseMethodImpl(Tag cod, int index) {
        char parent = parseType(cod);
        cod = cod.getNext();

        int entry = parseInt(cod, "entry");
        cod = cod.getNext();

        int flags = parseAccess(cod) & SquawkConstants.RECOGNIZED_SUITE_FILE_METHOD_IMPL_ACCESS_FLAG_MASK;
        cod = cod.getNext();

        SuiteMethodImpl methodImpl;

        if ((flags & SquawkConstants.ACC_ABSTRACT) == 0 && (flags & SquawkConstants.ACC_NATIVE) == 0) {
            ExceptionHandler[] handlers;
            cod.checkName("locals");
            char[] locals = parseTypeList(cod.getContent());     cod = cod.getNext();
            int stackSize = parseInt(cod, "stack");              cod = cod.getNext();
            handlers      = parseHandlerList(cod.getContent());  cod = cod.getNext();
            byte[] bytes  = parseBytecodes(cod.getContent());    cod = cod.getNext();

            methodImpl = SuiteMethodImpl.create(
                                            parent,
                                            entry,
                                            flags,
                                            locals,
                                            stackSize,
                                            handlers,
                                            bytes.length,
                                            false,
                                            new DataInputStream(new ByteArrayInputStream(bytes))
                                          );
        } else {
            methodImpl = SuiteMethodImpl.create(
                                            parent,
                                            entry,
                                            flags,
                                            null,
                                            -1,
                                            null,
                                            -1,
                                            false,
                                            null
                                          );
        }
        methods[index] = methodImpl;

        if (cod != null && cod.isName("attributes")) {
            Tag[] attributes = parseAttributeList(cod.getContent());
            methodsAttributes[index] = attributes;
            cod = cod.getNext();
        }

        return methodImpl;
    }

    /*
     * parseHandlerList
     */
    private ExceptionHandler parseHandler(Tag lst) {
        char from       = parseChar(lst, "from");        lst = lst.getNext();
        char to         = parseChar(lst, "to");          lst = lst.getNext();
        char entryPoint = parseChar(lst, "entrypoint");  lst = lst.getNext();
        char type       = parseChar(lst, "type");        lst = lst.getNext();
        return new ExceptionHandler(from, to, entryPoint, type);
    }

    /*
     * parseHandlerList
     */
    private ExceptionHandler[] parseHandlerList(Tag lst) {
        int count = Tag.countTags(lst, "handler");
        ExceptionHandler[] handlers = new ExceptionHandler[count];
        for (int i = 0 ; i < count ; i++) {
            handlers[i] = parseHandler(lst.getContent());
            lst = lst.getNext();
        }
        return handlers;
    }

    /*
     * parseBytecodes
     */
    private byte[] parseBytecodes(Tag lst) {
        byte[] buffer = new byte[countBytecodes(lst)];
        assembleBytecodes(lst, buffer);
        return buffer;
    }

    /*
     * countBytecodes
     */
    private int countBytecodes(Tag tag) {
        int count = 0;
        while (tag != null) {
            if (tag.isName("short") || tag.isName("char")) {
                count += 2;
            } else if (tag.isName("int")   || tag.isName("float")) {
                count += 4;
            } else if (tag.isName("long")  || tag.isName("double")) {
                count += 8;
            } else {
                count++;
            }
            tag = tag.getNext();
        }
        return count;
    }

    /*
     * assembleBytecodes
     */
    private void assembleBytecodes(Tag tag, byte[] buffer) {
        int count = 0;
        while (tag != null) {
            if (tag.isName("byte")) {
                buffer[count++] = (byte)parseInt(tag.getContent());
            } else if (tag.isName("short") || tag.isName("char")) {
                int value = parseInt(tag.getContent());
                buffer[count++] = (byte)(value >> 8);
                buffer[count++] = (byte)(value);
            } else if (tag.isName("int") || tag.isName("float")) {
                int value = parseInt(tag.getContent());
                buffer[count++] = (byte)(value >> 24);
                buffer[count++] = (byte)(value >> 16);
                buffer[count++] = (byte)(value >> 8);
                buffer[count++] = (byte)(value);
            } else if (tag.isName("long") || tag.isName("double")) {
                long value = parseLong(tag.getContent());
                buffer[count++] = (byte)(value >> 56);
                buffer[count++] = (byte)(value >> 48);
                buffer[count++] = (byte)(value >> 40);
                buffer[count++] = (byte)(value >> 32);
                buffer[count++] = (byte)(value >> 24);
                buffer[count++] = (byte)(value >> 16);
                buffer[count++] = (byte)(value >> 8);
                buffer[count++] = (byte)(value);
            } else {
                buffer[count++] = (byte)TagLookup.getBytecode(tag.getName());
            }
            tag = tag.getNext();
        }
    }

/* ------------------------------------------------------------------------ *\
 *                               Test program                               *
\* ------------------------------------------------------------------------ */

/*if[DEBUG.LOADER]*/

    /*
     * main
     */
    public static void main(String[] args) throws IOException {
        XMLSuiteParser parser = new XMLSuiteParser(System.in);
        SuiteHeader suiteHeader = parser.readSuiteHeader();
        System.out.println("readSuiteName()      = " + suiteHeader.name);
        System.out.println("readSuiteAccess()    = " + suiteHeader.access);
        System.out.println("readBindList()       = " + suiteHeader.binds);
        System.out.println("readHighestClass()   = " + suiteHeader.maxType);
        System.out.println("getClasses()         = " + formatClasses(parser.classes));
        System.out.println("getMethods()         = " + formatMethods(parser.methods));
    }

    /*
     * formatStrings
     */
    private static String formatStrings(String[] args) {
        String result = "";
        for (int i = 0 ; i < args.length ; i++) {
            if (i != 0) {
                result += " ";
            }
            result += args[i];
        }
        return result;
    }

    /*
     * formatClasses
     */
    private static String formatClasses(SuiteClass[] args) {
        StringBuffer sb = new StringBuffer();
        sb.append('\n');
        for (int i = 0 ; i < args.length ; i++) {
            SuiteClass sc = args[i];
            StringOfSymbols symbols = sc.symbols;
            sb.append("\n    name                = " + sc.getName());
            sb.append("\n    type                = " + (int)sc.getType());
            sb.append("\n    access              = " + (int)sc.getAccess());
            sb.append("\n    superType           = " + (int)sc.getSuperType());
            sb.append("\n    interfaceTypes      = " + formatTypes(sc.interfaceTypes));
            sb.append("\n    interfaceSlotTables = " + sc.interfaceSlotTables);
            sb.append("\n    instanceFields      = " + formatMembers(symbols, SquawkConstants.MTYPE_INSTANCEFIELDS));
            sb.append("\n    staticFields        = " + formatMembers(symbols, SquawkConstants.MTYPE_STATICFIELDS));
            sb.append("\n    virtualMethods      = " + formatMembers(symbols, SquawkConstants.MTYPE_VIRTUALMETHODS));
            sb.append("\n    staticMethods       = " + formatMembers(symbols, SquawkConstants.MTYPE_STATICMETHODS));
/*if[OVERRIDDENACCESS]*/
            sb.append("\n    overriddenAccess    = " + sc.getOverriddenAccess());
/*end[OVERRIDDENACCESS]*/
            sb.append("\n    classReferences     = " + formatTypes(sc.classReferences));
            sb.append("\n    objectReferences    = " + sc.objectReferences);
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String formatOverriddenAccess(int[] overriddenAccess) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i != overriddenAccess.length; ++i) {
            int overriding = overriddenAccess[i];
            int vindex = (overriding >> 16) & 0xFF;
            int access = overriding & 0xFF;
            buf.append("{ vindex=").append(vindex).
                append(", access=0X").append(Integer.toHexString(access)).
                append("} ");
        }
        return buf.toString();
    }

    /*
     * formatMembers
     */
    private static String formatMembers(StringOfSymbols symbols, int mtype) {
        StringBuffer sb = new StringBuffer();
        int memberID = symbols.lookupMemberID(mtype, 0);
        if (memberID == -1) {
            return "";
        }
        symbols.formatMember(memberID, sb);
        return sb.toString();
    }

    /*
     * formatMethods
     */
    private static String formatMethods(SuiteMethodImpl[] args) {
        StringBuffer sb = new StringBuffer();
        sb.append('\n');
        for (int i = 0 ; i < args.length ; i++) {
            SuiteMethodImpl code = args[i];
            sb.append("\n    parent           = " + (int)code.parent);
            sb.append("\n    entry            = " + (int)code.entry);
            sb.append("\n    locals           = " + formatTypes(code.locals));
            sb.append("\n    stackSize        = " + code.stackSize());
            sb.append("\n    handlers         = " + code.handlers);
            sb.append("\n    codeSize         = " + code.codeSize);
            sb.append('\n');
        }
        return sb.toString();
    }

    /*
     * formatTypes
     */
    private static String formatTypes(char[] types) {
        StringBuffer sb = new StringBuffer();
        if (types != null) {
            for (int i = 0 ; i < types.length ; i++) {
                int type = types[i];
                sb.append(""+type+" ");
            }
        }
        return sb.toString();
    }
/*end[DEBUG.LOADER]*/
}