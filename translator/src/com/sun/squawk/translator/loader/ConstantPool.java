
package com.sun.squawk.translator.loader;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.*;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;

public class ConstantPool {

    /**
     * This is an enumerated type representing the different name types that are
     * found in the constant pool of a Java classfile.
     */
    public static class ValidNameType {
        private static int nextValue = 1;
        private ValidNameType(String asString) {
            this.asString = asString;
            this.value = nextValue++;
        }

        public String toString() {
            return asString;
        }

        public final String asString;
        public final int    value;

        // The enumerated values.
        public static final ValidNameType CLASS  = new ValidNameType("class");
        public static final ValidNameType FIELD  = new ValidNameType("field");
        public static final ValidNameType METHOD = new ValidNameType("method");
    }

    /**
     * The constant pool entries are encoded as regular Java objects. The list
     * of valid objects for each tag type are:
     *
     *   CONSTANT_Utf8               null (Not retained)
     *   CONSTANT_NameAndType        null (Not retained)
     *   CONSTANT_Integer            java.lang.Integer
     *   CONSTANT_Float              java.lang.Float
     *   CONSTANT_Long               java.lang.Long
     *   CONSTANT_Double             java.lang.Double
     *   CONSTANT_String             java.lang.String
     *   CONSTANT_Class              com.sun.squawk.translator.Type
     *   CONSTANT_Field              com.sun.squawk.translator.Field
     *   CONSTANT_Method             com.sun.squawk.translator.Method
     *   CONSTANT_InterfaceMethod    com.sun.squawk.translator.Method
     *
     * Thus only a null, Integer, Long, Float, Double, Type, Field, or Method will
     * be found in this array.
     *
     * CONSTANT_Utf8 entries are converted into Strings
     * CONSTANT_NameAndType are not needed becuse the UTF8 strings they refer
     * to is converted into strings and places in the approperate Field and Method
     * data structures.
     */

    /** The Translator context. */
    private final Translator vm;
    /** The ClassFileInputStream from which the constant pool is read. */
    private final ClassFileInputStream in;
    /** The class enclosing this constant pool. */
    private final Type parent;
    /** Pool entry tags. */
    private final byte[] tags;
    /** Entries that refer to other entries that haven't yet been resolved. */
//    private final int[] unresolvedEntries;
    /** Resolved pool entries for all object types. */
    private final Object[] entries;
    /** Cache of method signature strings to Method.SignatureType objects. */
    private final Hashtable methodSigCache;

    private static String asHex(int i) {
        return "0x" + Integer.toHexString(i).toUpperCase();
    }

    /**
     * Get the class for which this is the constant pool.
     * @return the class for which this is the constant pool.
     */
    public Type parent() {
        return parent;
    }

    /**
     * Verify that an index to an entry is within range and is of an expected type.
     */
    private void verifyEntry(int index, int tag) throws LinkageException {
        if (index < 1 || index >= entries.length) {
            throw new LinkageException(vm.CLASSFORMATERROR, "constant pool index out of range: " + index);
        }
        if (tags[index] != tag) {
            throw new LinkageException(vm.CLASSFORMATERROR, "bad constant pool index: expected " + tag + ", got " + tags[index]);
        }
    }

    /**
     * Verify that legal field name occurs at a given offset of a string.
     * @param s the string
     * @param offset the offset at which a legal field name should occur
     * @param slashOkay
     * @return the first character after the legal field name or -1 if there is no legal field name
     * at the given offset.
     */
    private static int skipOverFieldName(String s, int offset, boolean slashOkay) {
        char lastCh = (char)0;
        char ch;
        int i;
        for (i = offset; i != s.length(); i++, lastCh = ch) {
            ch = s.charAt(i);
            if ((int)ch < 128) {
                /* quick check for ascii */
                if ((ch >= 'a' && ch <= 'z') ||
                    (ch >= 'A' && ch <= 'Z') ||
                    (lastCh != 0 && ch >= '0' && ch <= '9')) {
                    continue;
                }
            } else {
                // This is a unicode character and all unicode characters are valid
                // identifier characters apart from the first character
                if (lastCh == 0) {
                    return -1;
                }
                continue;
            }

            if (slashOkay && (ch == '/') && (lastCh != 0)) {
                if (lastCh == '/') {
                    return -1;    /* Don't permit consecutive slashes */
                }
            } else if (ch == '_' || ch == '$') {
                continue;
            } else {
                return lastCh != 0 ? i : -1;
            }
        }
        return lastCh != 0 ? i : -1;
    }

    /**
     * Verify that a legal type name occurs at a given offset of a string.
     * @param s the string
     * @param offset the offset at which a legal field name should occur
     * @param slashOkay
     * @return the index of the character after the legal field name or -1 if there is no legal field type name
     * at the given offset.
     */
    private int skipOverFieldType(String s, int offset, boolean voidOkay)
    {
        int length = s.length();
        int depth = 0;
        int maxDimensions = vm.minimalVM() ? 31 : 255;
        for (int i = offset; i != length; i++) {
            switch (s.charAt(i)) {
            case 'V':
                if (!voidOkay) return -1;
                /* FALL THROUGH */
            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'I':
            case 'J':
            case 'F':
            case 'D':
                return i + 1;

            case 'L': {
                /* Skip over the class name, if one is there. */
                int end = skipOverFieldName(s, i + 1, true);
                if (end != -1 && end < length && s.charAt(end) == ';') {
                    return end + 1;
                }
                else {
                    return -1;
                }
            }

            case '[':
                /* The rest of what's there better be a legal signature.  */
                if (depth++ == maxDimensions) {
                    return -1;
                }
                voidOkay = false;
                break;

            default:
                return -1;
            }
        }
        return -1;
    }

    /**
     * Test whether a name for a given class component has a valid format for an
     * expected type.
     * @param name The name to test.
     * @param type The expected type.
     * @return true if the given string is valid
     */
    public boolean isLegalName(String name, ValidNameType type) {
        Assert.that(type != null);
        boolean result = false;
        int length = name.length();
        if (length > 0) {
            if (name.charAt(0) == '<') {
                result = (type == ValidNameType.METHOD) &&
                    (name.equals("<init>") ||
                     name.equals("<clinit>"));
            } else {
                int end;
                if (type == ValidNameType.CLASS && name.charAt(0) == '[') {
                    end = skipOverFieldType(name, 0, false);
                } else {
                    end = skipOverFieldName(name, 0, type == ValidNameType.CLASS);
                }
                result = (end != -1) && (end == name.length());
            }
        }
        return result;
    }

    /* ------------------------------------------------------------------------ *\
     *              Well-formedness verification methods                        *
    \* ------------------------------------------------------------------------ */

    /**
     * Verify that a name for a given class component has a valid format for an
     * expected type.
     * @param name The name to test.
     * @param type The expected type.
     * @return the name if it is valid.
     * @exception ClassFormatError if the name is invalid.
     */
    public String verifyName(String name, ValidNameType type) throws LinkageException {
        if (!isLegalName(name, type)) {
            throw new LinkageException(vm.CLASSFORMATERROR, "Bad " + type + " name");
        }
        return name;
    }

    /**
     * Verify that a field signature has a valid format.
     * @param sig The name to test.
     * @return the field's type
     * @exception ClassFormatError if the name is invalid.
     */
    public Type verifyFieldType(String sig) throws LinkageException {
        if (skipOverFieldType(sig, 0, false) != sig.length()) {
            throw new LinkageException(vm.CLASSFORMATERROR, "Bad field signature: " + sig);
        }
        return findOrCreateType(sig);
    }

    /**
     * Verify that a method signature has a valid format.
     * @param sig The signature to test.
     * @param specialMethod True if this method's name startsWith "<".
     * @return the types in the method's signature.
     * @exception ClassFormatError if the name is invalid.
     */
    public Method.SignatureTypes verifyMethodType(String sig, boolean specialMethod) throws LinkageException {
        Method.SignatureTypes sigTypes = (Method.SignatureTypes)methodSigCache.get(sig);
        if (sigTypes == null) {
            Type   returnType  = null;
            Vector parmTypes   = new Vector();
            /* The first character must be a '(' */
            int length = sig.length();
            if (length > 0 && sig.charAt(0) == '(') {
                int offset = 1;
                /* Skip over however many legal field signatures there are */
                while (offset < length) {
                    int nextOffset = skipOverFieldType(sig, offset, false);
                    if (nextOffset == -1) {
                        break;
                    }
                    parmTypes.addElement(findOrCreateType(sig.substring(offset, nextOffset)));
                    offset = nextOffset;
                }

                /* The first non-signature thing better be a ')' */
                if (offset < length && (sig.charAt(offset) == ')')) {
                    offset++;
                    if (specialMethod) {
                    /* All internal methods must return void */
                        if ((offset == length - 1) && (sig.charAt(offset) == 'V')) {
                            returnType = vm.VOID;
                        }
                    } else {
                    /* Now, we better just have a return value. */
                        if (skipOverFieldType(sig, offset, true) == length) {
                            returnType = findOrCreateType(sig.substring(offset, length));
                        }
                    }
                }
            }

            if (returnType == null) {
                sigTypes = Method.SignatureTypes.INVALID;
            }
            else {
                Type[] types = new Type[parmTypes.size()];
                parmTypes.copyInto(types);
                sigTypes = new Method.SignatureTypes(returnType, vm.internTypeList(types));
            }
        }
        if (sigTypes == Method.SignatureTypes.INVALID) {
            throw new LinkageException(vm.CLASSFORMATERROR, "Bad method signature: " + sig);
        }
        return sigTypes;
    }

    /**
     * Verify that the access class flags for a class are valid.
     * @param flags The access flags to test.
     * @exception ClassFormatError if the flags are invalid.
     */
    public void verifyClassFlags(int flags) throws LinkageException {
        boolean valid;
        int finalAndAbstract = (JVMConst.ACC_FINAL | JVMConst.ACC_ABSTRACT);
        if ((flags & JVMConst.ACC_INTERFACE) != 0) {
            valid = (flags & finalAndAbstract) == JVMConst.ACC_ABSTRACT;
        } else {
            valid = (flags & finalAndAbstract) != finalAndAbstract;
        }
        if (!valid) {
            throw new LinkageException(vm.CLASSFORMATERROR, "Invalid class access flags: " + asHex(flags));
        }
    }

    /**
     * Verify that the access class flags for a field are valid.
     * @param flags The access flags to test.
     * @param classFlags The access flags of the enclosing class.
     * @exception ClassFormatError if the flags are invalid.
     */
    public void verifyFieldFlags(int flags, int classFlags) throws LinkageException {
        boolean valid;
        if ((classFlags & JVMConst.ACC_INTERFACE) == 0) {
            // Class or instance fields
            int accessFlags = flags & (JVMConst.ACC_PUBLIC |
                                       JVMConst.ACC_PRIVATE |
                                       JVMConst.ACC_PROTECTED);

            // Make sure that accessFlags has at most one of its ACC_PRIVATE,
            // ACC_PROTECTED bits set. That is, do a population count of these
            // bit positions corresponding to these flags and ensure that it is
            // at most 1.
            valid = (accessFlags == 0 || (accessFlags & ~(accessFlags - 1)) == accessFlags);

            // A field can't be both final and volatile
            int finalAndVolatile = JVMConst.ACC_FINAL | JVMConst.ACC_VOLATILE;
            valid = valid && ((flags & finalAndVolatile) != finalAndVolatile);
        } else {
            // interface fields must be public static final (i.e. constants).
            valid  = (flags == (JVMConst.ACC_STATIC |
                                JVMConst.ACC_FINAL |
                                JVMConst.ACC_PUBLIC));
        }
        if (!valid) {
            throw new LinkageException(vm.CLASSFORMATERROR, "Invalid field access flags: " +
                asHex(flags) + " (class flags = " + asHex(classFlags)+ ")");
        }
    }

    /**
     * Verify that the access class flags for a method are valid.
     * @param flags The access flags to test.
     * @param classFlags The access flags of the enclosing class.
     * @exception ClassFormatError if the flags are invalid.
     */
     public void verifyMethodFlags(int flags, int classFlags, boolean isInit) throws LinkageException {
        // These are all small bits.  The value is between 0 and 7.
        int accessFlags = flags & (JVMConst.ACC_PUBLIC |
                                   JVMConst.ACC_PRIVATE |
                                   JVMConst.ACC_PROTECTED);

        // Make sure that accessFlags has at most one of its ACC_PRIVATE,
        // ACC_PROTECTED bits set. That is, do a population count of these
        // bit positions corresponding to these flags and ensure that it is
        // at most 1.
        boolean valid = (accessFlags == 0 || (accessFlags & ~(accessFlags - 1)) == accessFlags);
        if (valid) {
            if ((classFlags & JVMConst.ACC_INTERFACE) == 0) {
                // class or instance methods
                if ((flags & JVMConst.ACC_ABSTRACT) != 0) {
                    if ((flags & (JVMConst.ACC_FINAL |
                                  JVMConst.ACC_NATIVE |
                                  JVMConst.ACC_SYNCHRONIZED |
                                  JVMConst.ACC_PRIVATE |
                                  JVMConst.ACC_STATIC)) != 0) {
                        valid = false;
                    }
                }
            } else {
                /*
                 * All interface methods must have their ACC_ABSTRACT and ACC_PUBLIC flags
                 * set and may not have any of the other flags in Table 4.5 set (§2.13.3.2).
                 */
                // Note that <clinit> is special, and not handled by this
                // function.  It's not abstract, and static.
                final int abstractAndPublic = JVMConst.ACC_ABSTRACT | JVMConst.ACC_PUBLIC;
                valid = ((flags & (abstractAndPublic | JVMConst.ACC_STATIC)) == (abstractAndPublic));
            }

            if (valid) {
                if (isInit) {
                    /*
                     * A specific instance initialization method (§3.9) may have at most one
                     * of its ACC_PRIVATE, ACC_PROTECTED, and ACC_PUBLIC flags set and may also
                     * have its ACC_STRICT flag set, but may not have any of the other flags in
                     * Table 4.5 set.
                     */
                    valid = ((flags & ~(JVMConst.ACC_PUBLIC |
                                        JVMConst.ACC_PROTECTED |
                                        JVMConst.ACC_PRIVATE |
                                        JVMConst.ACC_STRICT)) == 0);
                }
            }
        }
        if (!valid) {
            throw new LinkageException(vm.CLASSFORMATERROR, "Invalid method access flags: " +
                asHex(flags) + " (class flags = " + asHex(classFlags)+ ")");
        }
    }

    /* ------------------------------------------------------------------------ *\
     *              Access control verification methods                         *
    \* ------------------------------------------------------------------------ */


    /* ------------------------------------------------------------------------ *\
     *                      Accessors to constant entries                       *
    \* ------------------------------------------------------------------------ */

    /**
     * Return the size of the constant pool.
     */
    public int getSize() {
        return entries.length;
    }

    /**
     * Get the tag for the entry at a given index.
     */
    public int getTag(int index) throws LinkageException {
        if (index < 0 || index >= entries.length ) {
            throw in.classFormatError("Bad constant index");
        }
        return tags[index];
    }

    public int getInt(int index) throws LinkageException {
        verifyEntry(index, JVMConst.CONSTANT_Integer);
        return ((Integer)entries[index]).intValue();
    }

    public long getLong(int index) throws LinkageException {
        verifyEntry(index, JVMConst.CONSTANT_Long);
        return ((Long)entries[index]).longValue();
    }
/*if[FLOATS]*/
    public float getFloat(int index) throws LinkageException {
        verifyEntry(index, JVMConst.CONSTANT_Float);
        return ((Float)entries[index]).floatValue();
    }

    public double getDouble(int index) throws LinkageException {
        verifyEntry(index, JVMConst.CONSTANT_Double);
        return ((Double)entries[index]).doubleValue();
    }
/*end[FLOATS]*/

    public String getString(int index) throws LinkageException {
        verifyEntry(index, JVMConst.CONSTANT_String);
        return (String)entries[index];
    }

    public String getUtf8(int index) throws LinkageException {
        verifyEntry(index, JVMConst.CONSTANT_Utf8);
        return (String)entries[index];
    }

    public String getUtf8Interning(int index) throws LinkageException {
        return vm.internString(getUtf8(index));
    }

    public Type bootstrapType(int index) throws LinkageException {
        verifyEntry(index, JVMConst.CONSTANT_Class);
        return (Type)entries[index];
    }

    public Object getEntry(int index, int tag) throws LinkageException {
        verifyEntry(index, tag);
        return entries[index];
    }

    private NameAndType getNameAndType(int index) {
        return (NameAndType)entries[index];
    }

    /* ------------------------------------------------------------------------ *\
     *      Accessors to symbolic reference entries that require resolving      *
    \* ------------------------------------------------------------------------ */

    public Type resolveType(int index) throws LinkageException {
        Type type = bootstrapType(index);
        if (type != null) {
            type.validate();
            if (!type.isLoaded()) {
                type.load();
            }
            if (!type.isAccessibleBy(this.parent)) {
                throw new LinkageException(vm.ILLEGALACCESSERROR, type+" is not accessible by "+this.parent);
            }
        }
        return type;
    }

    public Field resolveField(int index, boolean isStatic) throws LinkageException {
        verifyEntry(index, JVMConst.CONSTANT_Field);

        Field field;
        if (entries[index] instanceof FieldOrMethod) {
            FieldOrMethod entry = (FieldOrMethod)entries[index];
            Type parent    = resolveType(entry.classIndex);
            NameAndType nt = getNameAndType(entry.nameAndTypeIndex);
            Assert.that(parent.superType() != null);

            verifyName(nt.name, ValidNameType.FIELD);
            Type sigType = verifyFieldType(nt.sig);

            field = parent.lookupField(nt.name, sigType, isStatic);
            if (field == null) {
                throw new LinkageException(vm.NOSUCHFIELDERROR, '"' + nt.name + ' ' + nt.sig + "\" in " + parent);
            }
            field.loadSignatureTypes();
            entries[index] = field;

            /*
             * Since access only depends on the class, and not on the
             * specific byte code used to access the field, we don't need
             * to perform this check if the constant pool entry
             * has already been resolved.
             */
            if (!field.isAccessibleBy(this.parent)) {
                throw new LinkageException(vm.ILLEGALACCESSERROR, field + " is not accessible by " + this.parent);
            }
        } else {
            field = (Field)entries[index];
        }
        return field;
    }

    public Method resolveMethod(int index, boolean interfaceMethod, boolean isStatic) throws LinkageException {
        verifyEntry(index, (interfaceMethod ? JVMConst.CONSTANT_InterfaceMethod : JVMConst.CONSTANT_Method));

        // Resolve the method now if necessary
        Method method;
        if (entries[index] instanceof FieldOrMethod) {
            FieldOrMethod entry = (FieldOrMethod)entries[index];
            Type parent    = resolveType(entry.classIndex);
            NameAndType nt = getNameAndType(entry.nameAndTypeIndex);
            String name = nt.name;
            Assert.that(parent.superType() != null);

            verifyName(name, ValidNameType.METHOD);
            boolean isSpecialMethod = !interfaceMethod && (name == vm.CLINIT || name == vm.INIT);
            Method.SignatureTypes sigTypes = verifyMethodType(nt.sig, isSpecialMethod);
            Type returnType = sigTypes.returnType;

            if (!interfaceMethod) {
                // Rename references to Object._SQUAWK_INTERNAL_init to be to Object.<init>
                if (name == vm.SQUAWK_INIT) {
                    throw new AssertionFailed("There should not be any calls to methods named: "+vm.SQUAWK_INIT);
                }
                if (name == vm.INIT) {
                    // Change the signature of a constructor to return an instance of the parent type.
                    sigTypes = sigTypes.modifyReturnType(parent);
                    // <init> methods are stored in the static array
                    isStatic = true;
                    // Since the return type for constructors are unique within each class
                    // under Squawk, they need to be looked up without regard to return
                    // type to match standard Java semantics.
//                    returnType = null;
                }
            }

            method = parent.lookupMethod(name, sigTypes.parmTypes, returnType, parent, isStatic);
            if (method == null) {
                parent.lookupMethod(name, sigTypes.parmTypes, returnType, parent, isStatic);
                throw new LinkageException(vm.NOSUCHMETHODERROR, name+nt.sig+" in "+parent);
            }
            if (!interfaceMethod) {
                if (method.parent().isInterface()) {
                    throw new LinkageException(vm.INCOMPATIBLECLASSCHANGEERROR, "Non-interface method '"+name+"' found in interface "+parent);
                }
            } else {
                if (!method.parent().isInterface()) {
                    throw new LinkageException(vm.INCOMPATIBLECLASSCHANGEERROR, "Interface method '"+name+"' found in class "+parent);
                }
            }
            method.loadSignatureTypes();

            /*
             * Since access only depends on the class, and not on the
             * specific byte code used to access the method, we don't need
             * to perform this check if the constant pool entry
             * has already been resolved.
             */
             if (!method.isAccessibleBy(this.parent)) {
//                 parent.lookupMethod(name, sigTypes.parmTypes, returnType, parent, isStatic);
                 throw new LinkageException(vm.ILLEGALACCESSERROR, method + " is not accessible by " + this.parent);
             }
        } else {
            method = (Method)entries[index];
        }
        return method;
    }

   /* ------------------------------------------------------------------------ *\
    *                             Pool loading code                            *
   \* ------------------------------------------------------------------------ */

    private static class NameAndType {
        final String name;
        final String sig;
        NameAndType(String name, String sig) {
            this.name = name;
            this.sig  = sig;
        }
    }

    /**
     * A instance of this class represents the class pool entry for a field,
     * method or interface method before it is resolved.
     */
    private static class FieldOrMethod {
        final int classIndex;
        final int nameAndTypeIndex;
        FieldOrMethod(int classIndex, int nameAndTypeIndex) {
            this.classIndex       = classIndex;
            this.nameAndTypeIndex = nameAndTypeIndex;
        }
    }

    /**
     * Create a new constant pool from the input stream
     */
    public static ConstantPool create(Translator vm, ClassFileInputStream in, Type parent) throws IOException, LinkageException {
        return new ConstantPool(vm, in, parent);
    }


    private ConstantPool(Translator vm, ClassFileInputStream in, Type parent) throws LinkageException {
        this.methodSigCache = new Hashtable();
        this.vm             = vm;
        this.in             = in;
        this.parent         = parent;

        // Read the constant pool entry count
        int count = in.readUnsignedShort("cp-count");

        // Allocate the required lists
        tags      = new byte[count];
        entries   = new Object[count];
        int[] raw = new int[count];

       /*
        * Read the constant pool entries from the classfile
        * and initialize the constant pool correspondingly.
        * Remember that constant pool indices start from 1
        * rather than 0 and that last index is count-1.
        */

        // Pass 1 read in the primitive values
        for (int i = 1 ; i < count ; i++) {
            int tag = in.readUnsignedByte("cp-tag");
            tags[i] = (byte)tag;
            switch (tag) {
                case JVMConst.CONSTANT_Utf8: {
                    //ntries[i] = vm.internString(in.readUTF("JVMConst.CONSTANT_Utf8"));
                    entries[i] = in.readUTF("JVMConst.CONSTANT_Utf8");
                    break;
                }
                case JVMConst.CONSTANT_Integer: {
                    entries[i] = new Integer(in.readInt("JVMConst.CONSTANT_Integer"));
                    break;
                }
/*if[FLOATS]*/
                case JVMConst.CONSTANT_Float: {
                    entries[i] = new Float(in.readFloat("JVMConst.CONSTANT_Float"));
                    break;
                }
                case JVMConst.CONSTANT_Double: {
                    entries[i] = new Double(in.readDouble("JVMConst.CONSTANT_Double"));
                    i++; // Doubles take two slots
                    if (i == count) {
                        throw in.classFormatError("Bad 64 bit constant in constant pool");
                    }
                    break;
                }
/*end[FLOATS]*/
                case JVMConst.CONSTANT_Long: {
                    entries[i] = new Long(in.readLong("JVMConst.CONSTANT_Long"));
                    i++; // Longs take two slots
                    if (i == count) {
                        throw in.classFormatError("Bad 64 bit constant in constant pool");
                    }
                    break;
                }
                case JVMConst.CONSTANT_String:
                case JVMConst.CONSTANT_Class: {
                    raw[i] = in.readUnsignedShort("JVMConst.CONSTANT_String/Class");
                    break;
                }

                case JVMConst.CONSTANT_Field:
                case JVMConst.CONSTANT_Method:
                case JVMConst.CONSTANT_InterfaceMethod:
                case JVMConst.CONSTANT_NameAndType: {
                    raw[i] = (in.readUnsignedShort("JVMConst.CONSTANT_F/M/I/N-1") << 16) |
                                           (in.readUnsignedShort("JVMConst.CONSTANT_F/M/I/N-2") & 0xFFFF);
                    break;
                }

                default: {
                    throw in.classFormatError("Invalid constant pool entry: tag="+tag);
                }
            }
        }

        // Pass 2 fixup types and strings
        for (int i = 1 ; i < count ; i++) {
            try {
                switch (tags[i]) {
                    case JVMConst.CONSTANT_String: {
                        verifyEntry(raw[i], JVMConst.CONSTANT_Utf8);
                        entries[i] = entries[raw[i]];
                        raw[i] = 0;
                        break;
                    }
                    case JVMConst.CONSTANT_Class: {
                        verifyEntry(raw[i], JVMConst.CONSTANT_Utf8);
                        String name = verifyName((String)entries[raw[i]], ValidNameType.CLASS);
                        raw[i] = 0;
                        if (name.charAt(0) != '[') {
                            name = "L"+name+";";
                        }
                        Type type = findOrCreateType(name);
                        entries[i] = type;
                        break;
                    }
                    case JVMConst.CONSTANT_NameAndType: {
                        int nameAndType = raw[i];
                        int nameIndex        = nameAndType >> 16;
                        int descriptorIndex  = nameAndType & 0xFFFF;
                        verifyEntry(nameIndex, JVMConst.CONSTANT_Utf8);
                        verifyEntry(descriptorIndex, JVMConst.CONSTANT_Utf8);
                        entries[i] = new NameAndType(getUtf8Interning(nameIndex), (String)entries[descriptorIndex]);
                        break;
                    }
                    case JVMConst.CONSTANT_Field:
                    case JVMConst.CONSTANT_Method:
                    case JVMConst.CONSTANT_InterfaceMethod: {
                        int classNameAndType = raw[i];
                        int classIndex = classNameAndType >> 16;
                        int nameAndTypeIndex = classNameAndType & 0xFFFF;
                        verifyEntry(classIndex, JVMConst.CONSTANT_Class);
                        verifyEntry(nameAndTypeIndex, JVMConst.CONSTANT_NameAndType);
                        entries[i] = new FieldOrMethod(classIndex, nameAndTypeIndex);
                        break;
                    }

                }
            } catch (ArrayIndexOutOfBoundsException obe) {
                throw in.classFormatError("bad constant pool index");
            }
        }
    }

    /**
     * A helper method to handle type resolution for a type that may not be
     * supported.
     * @param name
     * @return
     * @throws LinkageException
     */
    public Type findOrCreateType(String name) throws LinkageException {
        Type type = vm.findOrCreateType(name);
        if (type == null) {
            throw in.classFormatError("Unsupported type: "+name);
        }
        return type;
    }
}
