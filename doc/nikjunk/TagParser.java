import java.io.*;

/* ------------------------------------------------------------------------ *\
 *                                TagParser                                 *
\* ------------------------------------------------------------------------ */

public class TagParser {

    public final static int

        ACC_PUBLIC              = 0x0001,
        ACC_PRIVATE             = 0x0002,
        ACC_PROTECTED           = 0x0004,
        ACC_STATIC              = 0x0008,
        ACC_FINAL               = 0x0010,
        ACC_SYNCHRONIZED        = 0x0020,
        ACC_SUPER               = 0x0020,
        ACC_VOLATILE            = 0x0040,
        ACC_TRANSIENT           = 0x0080,
        ACC_NATIVE              = 0x0100,
        ACC_INTERFACE           = 0x0200,
        ACC_ABSTRACT            = 0x0400,
        ACC_LINKAGEERROR        = 0x1000,
        ACC_HASCLINIT           = 0x8000;


    private static class Field {
        int     type;
        int     access;
        String  name;
        int     offset;
    }

    private static class Method {
        int     type;
        int     access;
        String  name;
        int[]   parameterTypes;
        int     offset;
    }

    private static class Klass {
        int      type;
        int      access;
        int      superType;
        int[]    implementedTypes;
        Field[]  staticFields;
        Field[]  virtualFields;
        Method[] staticMethods;
        Method[] virtualMethods;
        int[]    classReferences;
        Object[] objectReferences;
    }

    private static class Handler {
        int     from;
        int     to;
        int     entryPoint;
        int     type;
    }

    private static class Code {
        int       type;
        int       entry;
        int[]     locals;
        Handler[] handlers;
        byte[]    code;
    }

    Tag tag;

    int sequence = 0;

    String   suiteName;
    String[] proxyClassNames;
    String[] suiteClassNames;
    Klass[]  classes;
    Code[]   methods;

    /*
     * assume
     */
    protected static void assume(boolean b) {
       if (!b) {
           throw new RuntimeException("Assume failure");
       }
    }

    /*
     * assume
     */
    protected static void assume(boolean b, String msg) {
       if (!b) {
           throw new RuntimeException("Assume failure: "+msg);
       }
    }


/* ------------------------------------------------------------------------ *\
 *                                   Parsing                                *
\* ------------------------------------------------------------------------ */

    /*
     * countTags
     */
    private int countTags(Tag tag, String name) {
        int count = 0;
        while (tag != null) {
            if (name != null) {
                tag.checkName(name);
            }
            count++;
            tag = tag.getNext();
        }
        return count;
    }

    /*
     * parseName
     */
    private String parseName(Tag nam) {
        nam.checkName("name");
        return nam.getContent().getData();
    }

    /*
     * parseNameList
     */
    private String[] parseNameList(Tag nam) {
        int count = countTags(nam, "name");
        String[] result = new String[count];
        for (int i = 0 ; i < count ; i++) {
            result[i] = parseName(nam);
            nam = nam.getNext();
        }
        return result;
    }

    /*
     * TagParser
     */
    public TagParser(InputStream in) throws IOException {
        tag = Tag.create(in);
        parseSuite();
        parseSuiteName();
        parseProxyNames();
        parseSuiteNames();
        parseClasses();
        parseMethods();
    }

    /*
     * parseSuite
     */
    private void parseSuite() {
        tag.checkName("suite");
        tag = tag.getContent();
    }

    /*
     * parseSuiteName
     */
    private void parseSuiteName() {
        suiteName = parseName(tag);
        tag = tag.getNext();
    }

    /*
     * parseProxyNames
     */
    private void parseProxyNames() {
        tag.checkName("proxy_names");
        proxyClassNames = parseNameList(tag.getContent());
        tag = tag.getNext();
    }

    /*
     * parseSuiteNames
     */
    private void parseSuiteNames() {
        tag.checkName("class_names");
        suiteClassNames = parseNameList(tag.getContent());
        tag = tag.getNext();
    }

    /*
     * parseClasses
     */
    private void parseClasses() {
        tag.checkName("classes");
        Tag content = tag.getContent();
        int count = countTags(content, "class");
        classes = new Klass[count];
        for (int i = 0 ; i < count ; i++) {
            classes[i] = parseClass(content.getContent());
            content = content.getNext();
        }
        tag = tag.getNext();
    }

    /*
     * parseMethods
     */
    private void parseMethods() {
        tag.checkName("methods");
        Tag content = tag.getContent();
        int count = countTags(content, "method_body");
        methods = new Code[count];
        for (int i = 0 ; i < count ; i++) {
            methods[i] = parseMethodBody(content.getContent());
            content = content.getNext();
        }
        tag = tag.getNext();
    }

    /*
     * parseClass
     */
    private Klass parseClass(Tag cls) {
        Klass klass = new Klass();
        klass.type = parseType(cls);
        cls = cls.getNext();
        klass.access = parseAccess(cls);
        cls = cls.getNext();
        cls.checkName("extends");
        klass.superType = parseType(cls.getContent());
        cls = cls.getNext();
        cls.checkName("implements");
        klass.implementedTypes = parseTypeList(cls.getContent());
        cls = cls.getNext();
        cls.checkName("static_fields");
        klass.staticFields = parseFieldList(cls.getContent());
        cls = cls.getNext();
        cls.checkName("virtual_fields");
        klass.virtualFields = parseFieldList(cls.getContent());
        cls = cls.getNext();
        cls.checkName("static_methods");
        klass.staticMethods = parseMethodList(cls.getContent());
        cls = cls.getNext();
        cls.checkName("virtual_methods");
        klass.virtualMethods = parseMethodList(cls.getContent());
        cls = cls.getNext();
        cls.checkName("class_references");
        klass.classReferences = parseTypeList(cls.getContent());
        cls = cls.getNext();
        cls.checkName("object_references");
        klass.objectReferences = parseObjectList(cls.getContent());
        cls = cls.getNext();
        return klass;
    }

    /*
     * parseInt
     */
    private int parseInt(Tag data) {
        try {
            return Integer.parseInt(data.getData());
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Number format error");
        }
    }

    /*
     * parseInt
     */
    private int parseInt(Tag typ, String name) {
        typ.checkName(name);
        return parseInt(typ.getContent());
    }

    /*
     * parseIntArray
     */
    private int[] parseIntArray(Tag lst, String name) {
        int count = countTags(lst, name);
        int[] result = new int[count];
        for (int i = 0 ; i < count ; i++) {
            result[i] = parseInt(lst, name);
            lst = lst.getNext();
        }
        return result;
    }

    /*
     * parseType
     */
    private int parseType(Tag typ) {
        return parseInt(typ, "type");
    }

    /*
     * parseTypeList
     */
    private int[] parseTypeList(Tag typ) {
        return parseIntArray(typ, "type");
    }

    /*
     * parseAccess
     */
    private int parseAccess(Tag acc) {
        acc.checkName("access");
        int result = 0;
        acc = acc.getContent();
        while (acc != null) {
            if (acc.isName("public"))    result |= ACC_PUBLIC;
            else if (acc.isName("private"))   result |= ACC_PRIVATE;
            else if (acc.isName("protected")) result |= ACC_PROTECTED;
            else if (acc.isName("final"))     result |= ACC_FINAL;
            else if (acc.isName("volatile"))  result |= ACC_VOLATILE;
            else if (acc.isName("transient")) result |= ACC_TRANSIENT;
            else if (acc.isName("interface")) result |= ACC_INTERFACE;
            else if (acc.isName("abstract"))  result |= ACC_ABSTRACT;
            else throw new RuntimeException("Bad access flag");
            acc = acc.getNext();
        }
        return result;
    }

    /*
     * parseFieldList
     */
    private Field[] parseFieldList(Tag lst) {
        int count = countTags(lst, "field");
        Field[] result = new Field[count];
        for (int i = 0 ; i < count ; i++) {
            result[i] = parseField(lst.getContent());
            lst = lst.getNext();
        }
        return result;
    }

    /*
     * parseField
     */
    private Field parseField(Tag fld) {
        Field field = new Field();

        field.type = parseType(fld);
        fld = fld.getNext();

        if (fld != null && fld.isName("name")) {
            field.name = parseName(fld);
            fld = fld.getNext();
        }

        if (fld != null && fld.isName("access")) {
            field.access = parseAccess(fld);
            fld = fld.getNext();
        }
        return field;
    }

    /*
     * parseMethodList
     */
    private Method[] parseMethodList(Tag lst) {
        int count = countTags(lst, "method");
        Method[] result = new Method[count];
        for (int i = 0 ; i < count ; i++) {
            result[i] = parseMethod(lst.getContent());
            lst = lst.getNext();
        }
        return result;
    }

    /*
     * parseMethod
     */
    private Method parseMethod(Tag mth) {
        Method method = new Method();

        method.type = parseType(mth);
        mth = mth.getNext();

        method.parameterTypes = parseTypeList(mth.getContent());
        mth = mth.getNext();

        if (mth != null && mth.isName("name")) {
            method.name = parseName(mth);
            mth = mth.getNext();
        }

        if (mth != null && mth.isName("access")) {
            method.access = parseAccess(mth);
            mth = mth.getNext();
        }

        return method;
    }


    /*
     * parseObjectList
     */
    private Object[] parseObjectList(Tag lst) {
        int count = countTags(lst, null);
        Object[] result = new Object[count];
        for (int i = 0 ; i < count ; i++) {
            result[i] = parseObject(lst);
            lst = lst.getNext();
        }
        return result;
    }

    /*
     * parseObject
     */
    private Object parseObject(Tag obj) {
        if (obj.isName("string")) {
            return obj.getContent().getData();
        } else if(obj.isName("int_array")) {
            return parseIntArray(obj.getContent(), "int");
        } else {
            throw new RuntimeException("Unknown object type");
        }
    }

    /*
     * parseMethodBody
     */
    private Code parseMethodBody(Tag cod) {
        Code code = new Code();
        code.type = parseType(cod);
        cod = cod.getNext();
        code.entry = parseInt(cod, "entry");
        cod = cod.getNext();
        cod.checkName("locals");
        code.locals = parseTypeList(cod.getContent());
        cod = cod.getNext();
        if (cod.isName("handlers")) {
            code.handlers = parseHandlerList(cod.getContent());
            cod = cod.getNext();
        }
        cod.checkName("code");
        code.code = parseBytecodes(cod.getContent());
        cod = cod.getNext();
        return code;
    }


    /*
     * parseHandlerList
     */
    private Handler parseHandler(Tag lst) {
        Handler hand    = new Handler();
        hand.from       = parseInt(lst, "from");        lst = lst.getNext();
        hand.to         = parseInt(lst, "to");          lst = lst.getNext();
        hand.entryPoint = parseInt(lst, "entrypoint");  lst = lst.getNext();
        hand.type       = parseInt(lst, "type");        lst = lst.getNext();
        return hand;
    }

    /*
     * parseHandlerList
     */
    private Handler[] parseHandlerList(Tag lst) {
        int count = countTags(lst, "handler");
        Handler[] handlers = new Handler[count];
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
            if (tag.isName("byte")) {
                count++;
            } else if (tag.isName("short") || tag.isName("char")) {
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
            } else if (tag.isName("int")) {
                int value = parseInt(tag.getContent());
                buffer[count++] = (byte)(value >> 24);
                buffer[count++] = (byte)(value >> 16);
                buffer[count++] = (byte)(value >> 8);
                buffer[count++] = (byte)(value);
            } else if (tag.isName("float") || tag.isName("long") || tag.isName("double")) {
                throw new RuntimeException("not implemented");
            } else {
                buffer[count++] = (byte)tag.getBytecode();
            }
            tag = tag.getNext();
        }
    }



/* ------------------------------------------------------------------------ *\
 *                              Public interface                            *
\* ------------------------------------------------------------------------ */

    /*
     * getSuiteName
     */
    String getSuiteName() {
        return suiteName;
    }

    /*
     * getProxyClassCount
     */
    int getProxyClassCount() {
        return proxyClassNames.length;
    }

    /*
     * getSuiteClassCount
     */
    int getSuiteClassCount() {
        return suiteClassNames.length;
    }

    /*
     * getProxyClassNames
     */
    String[] getProxyClassNames() {
        return proxyClassNames;
    }

    /*
     * getSuiteClassNames
     */
    String[] getSuiteClassNames() {
        return suiteClassNames;
    }

    /*
     * getClasses
     */
    Klass[] getClasses() {
        return classes;
    }

    /*
     * getMethods
     */
    Code[] getMethods() {
        return methods;
    }


/* ------------------------------------------------------------------------ *\
 *                               Test program                               *
\* ------------------------------------------------------------------------ */

    /*
     * main
     */
    public static void main(String[] args) throws IOException {
        TagParser parser = new TagParser(System.in);
        System.out.println("getSuiteName()       = " + parser.getSuiteName());
        System.out.println("getProxyClassCount() = " + parser.getProxyClassCount());
        System.out.println("getSuiteClassCount() = " + parser.getSuiteClassCount());
        System.out.println("getProxyClassNames() = " + formatStrings(parser.getProxyClassNames()));
        System.out.println("getSuiteClassNames() = " + formatStrings(parser.getSuiteClassNames()));
        System.out.println("getClasses()         = " + formatClasses(parser.getClasses()));
        System.out.println("getMethods()         = " + formatMethods(parser.getMethods()));

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
    private static String formatClasses(Klass[] args) {
        StringBuffer sb = new StringBuffer();
        sb.append('\n');
        for (int i = 0 ; i < args.length ; i++) {
            Klass klass = args[i];
            sb.append("\n    type             = " + klass.type);
            sb.append("\n    access           = " + klass.access);
            sb.append("\n    superType        = " + klass.superType);
            sb.append("\n    implementedTypes = " + klass.implementedTypes);
            sb.append("\n    staticFields     = " + klass.staticFields);
            sb.append("\n    virtualFields    = " + klass.virtualFields);
            sb.append("\n    staticMethods    = " + klass.staticMethods);
            sb.append("\n    virtualMethods   = " + klass.virtualMethods);
            sb.append("\n    classReferences  = " + klass.classReferences);
            sb.append("\n    objectReferences = " + klass.objectReferences);
            sb.append('\n');
        }
        return sb.toString();
    }

    /*
     * formatMethods
     */
    private static String formatMethods(Code[] args) {
        StringBuffer sb = new StringBuffer();
        sb.append('\n');
        for (int i = 0 ; i < args.length ; i++) {
            Code code = args[i];
            sb.append("\n    type             = " + code.type);
            sb.append("\n    entry            = " + code.entry);
            sb.append("\n    locals           = " + code.locals);
            sb.append("\n    handlers         = " + code.handlers);
            sb.append("\n    code             = " + code.code);
            sb.append('\n');
        }
        return sb.toString();
    }

}

