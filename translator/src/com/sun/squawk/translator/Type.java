package com.sun.squawk.translator;

import java.io.*;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import com.sun.squawk.translator.util.JVMConst;
import com.sun.squawk.translator.util.Comparer;
import com.sun.squawk.translator.util.Arrays;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.translator.suite.VMAccessedAttribute;

import com.sun.squawk.vm.SquawkConstants;
import com.sun.squawk.vm.CNO;

/**
 * This is the equivalent to java.lang.Class.
 *
 * A proxy class is included in a suite iff:
 *
 *    1. It is public or protected AND
 *    2. At least one of its methods is accessed and /or overriden by a suite class OR
 *
 *
 */
public class Type extends AccessFlags {

   /* ------------------------------------------------------------------------ *\
    *                              Static functions                            *
   \* ------------------------------------------------------------------------ */

   /**
    * Get an object array type for named type
    */
    private Type getArraySuperTypeFor(String name) throws LinkageException {
        int dims = 0;
        while (name.charAt(dims) == '[') {
             dims++;
        }

        String baseTypeName = name.substring(dims);
        Type superType;

        if (baseTypeName.startsWith("Ljava/lang/_") || baseTypeName.equals("Ljava/lang/Object;")) {
            dims--;
            superType = vm.OBJECT;
        } else {
            Type baseType = vm.findOrCreateType(baseTypeName);
            baseType.load();
            superType = baseType.superType();
        }

        while (dims-- > 0) {
            superType = superType.asArray();
        }
        return superType;
    }


   /* ------------------------------------------------------------------------ *\
    *                               Type definiion                             *
   \* ------------------------------------------------------------------------ */

    /** Enumerated type for class state. */
    public static final class State {
        /** Ordering of states. */
        static int nextValue;
        /** Name of the state. */
        private final String name;
        /** Ordinal position of state. */
        private final int value;
        /** Constructor for a constant. */
        private State(String name) {
            this.name  = name;
            this.value = nextValue++;
        }
        /** Return the name of the constant. */
        public String toString() { return name; }
        /**
         * Compare this state with another.
         * @param other Another state.
         * @return a negative integer, zero, or a positive integer as this state is earlier than,
         * equal to, or later than the specified state.
         */
        public int compareTo(State other) {
            return this.value - other.value;
        }
    }

    /** Constant for class is defined. */
    public final static State DEFINED         = new State("DEFINED");
    public final static State LOADING         = new State("LOADING");
    public final static State LOADED          = new State("LOADED");
    public final static State CONVERTING      = new State("CONVERTING");
    public final static State CONVERTED       = new State("CONVERTED");
    public final static State LINKAGEERROR    = new State("LINKAGEERROR");

    /** The loaded-resolved state of the class. */
    private State state = DEFINED;
    /** The internal name of the type (e.g "Ljava/lang/Object;"). */
    final private String name;
    /** The value returned from toString(). */
    private String asString;
    /** The cached return value of sourceFilePath(). */
    private String sourceFilePath;
    /** The Translator context of this type. */
    final private Translator vm;
    /** The superType. */
    private Type superType;
    /** An array's element type (null for non-array types). */
    private Type elementType;
    /** Interface types implemented by this type. */
    private Type[] interfaces;
    /** Number of direct superinterfaces implemented by this type. These will occupy the front of interfaces. */
    private int directSuperInterfaces;
    /** Static methods defined by this type. */
    private Method[] staticMethods;
    /** Virtual methods defined by this type (overriding methods first). */
    private Method[] virtualMethods;
    /** The number of superclass methods overridden by this class's methods. */
    private int overrisdden = 0;
    /** Static fields defined by this type. */
    private Field[] staticFields;
    /** Virtual fields defined by this type. */
    private Field[] instanceFields;
    /** The unique ID for a class within its suite. */
    private int suiteId = -1;
    /** The value of the SourceFile attribute or null if there wasn't one. */
    private String sourceFile;
    /** The LinkageError raised when the class was loaded/converted. */
    private LinkageException linkageError;

    /**
     *  The spec defined class number for this class. This values of field can be:
     *
     *  -2  : one of the special types that should not be emitted (e.g. BOGUS, BOOLEAN_OR_BYTE)
     *  -1  : a type special to the translator that should be emitted but is not in the spec (e.g. LINKAGEERROR)
     *  0   : a type that should be emitted but is not in the spec
     *  > 0 : a type that should be emitted and is in the spec
     */
    private int specId = 0;

   /* ------------------------------------------------------------------------ *\
    *                               Class creation                             *
   \* ------------------------------------------------------------------------ */

   /**
    * Static constructor only called from the Translator.java
    * @param vm
    * @param name
    * @return
    */
    static Type create(Translator vm, String name) {
        return new Type(vm, name);
    }

    /**
     * Protected constructor. Note that this disables the default constructor.
     * @param vm
     * @param name
     */
    protected Type(Translator vm,  String name) {
        Assert.that(vm != null);
        this.vm   = vm;
        this.name = name;
        this.interfaces     = vm.ZEROTYPES;
        this.staticFields   = vm.ZEROFIELDS;
        this.instanceFields = vm.ZEROFIELDS;
        this.staticMethods  = vm.ZEROMETHODS;
        this.virtualMethods = vm.ZEROMETHODS;

//Assert.that(!name.equals("B"), name);
//Assert.that(!name.equals("[B"), name);
//Assert.that(!name.equals("[[B"), name);

        if (isArray() || isUnreal()) {
            setFlag(JVMConst.ACC_PUBLIC);
        }
    }

   /* ------------------------------------------------------------------------ *\
    *                     Class loading and initialization                     *
   \* ------------------------------------------------------------------------ */

   /**
    * Helper method to convert all the methods in a table from their clasfile form
    * to their Squawk IR form.
    * @param isStatic
    */
    private void convertMethods(boolean isStatic) throws LinkageException {
        Method[] methods = getMethods(isStatic);
        for (int i = 0 ; i < methods.length ; i++) {
            Method m = methods[i];
            if (!m.isAbstract() && !m.isNative()) {
                methods[i].convert();
            }
        }

    }

    /**
     * Perform the linking that would normally be triggered during class initialization
     * of this type. This method should only be called from Translator.convert.
     */
    void doConversion() throws LinkageException {
        validate();
        if (state.compareTo(CONVERTING) < 0) {
            Assert.that(state == LOADED);
            // Write trace message and set state
            Assert.that(vm != null);
            vm.tracer().traceln(vm.traceloading(name), "Converting class " + name);
            state = CONVERTING;

            try {
                // Convert this type's supertype first
                if (this != vm.OBJECT) {
                    superType().load();
                    superType().doConversion();
                }

                // Ensure that this class implements all interface methods if
                // it is a non-abstract, non-interface class
                if (!(isAbstract() || isInterface())) {
                    for (int i = 0; i != interfaces.length; i++) {
                        Type iface = interfaces[i];
                        Method[] ifaceMethods = iface.getMethods(false);
                        for (int j = 0; j != ifaceMethods.length; j++) {
                            Method ifaceMethod = ifaceMethods[j];
                            Method implMethod = lookupMethod(ifaceMethod.name(), ifaceMethod.getParms(), ifaceMethod.type(), null, false);
                            if (implMethod == null || !implMethod.isPublic()) {
                                throw new LinkageException(vm.ABSTRACTMETHODERROR, this+" does not implement "+ifaceMethod);
                            }
                        }
                    }
                }

                // Convert static methods
                convertMethods(true);

                // Convert virtual methods
                convertMethods(false);

            } catch (LinkageException le) {
                setLinkageError(le);
                throw le;
            }
            state = CONVERTED;
            vm.tracer().traceln(vm.traceloading(name), "Converted class " + name);
        }
    }

    /**
     * Load this class.
     */
    public void load() throws LinkageException {
        validate();
        // ClassCircularityError errors will be detected by the loader.
        if (state.compareTo(LOADED) < 0) {
            int initial = name.charAt(0);
            if (initial == 'L') {
                Assert.that(state == DEFINED || state == LOADING);
                vm.load(this);
                Assert.that(state == LOADED, "name = " + name + ", state = " + state);
            } else {
                if (initial == '[') {
                    elementType().load();
                }
                vm.typeLoaded(this);
                state = LOADED;
            }

            // Proxy classes do not have their method code loaded and so are
            // considered to be converted once they are loaded.
            if (isProxy()) {
                state = CONVERTED;
            }
        }
    }

    /**
     * Return the state of this class.
     */
    public State getState() {
        return state;
    }

    /**
     * Set the state of this class.
     * @param state
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * Convert the methods in a class.
     */
    public void convert() throws LinkageException {
        validate();
        if (state.compareTo(CONVERTING) < 0) {
            load();
            vm.convert(this);
        }
    }




   /* ------------------------------------------------------------------------ *\
    *                             Instance functions                           *
   \* ------------------------------------------------------------------------ */

    /**
     * Return the type's VM.
     */
    public Translator vm() {
        return vm;
    }

    /**
     * Return the type's superType.
     */
    public Type superType() throws LinkageException {
        validate();
        if (superType == null && name.charAt(0) == '[') {
//            superType = getArraySuperTypeFor(name);
            superType = vm.OBJECT;
        }
        Assert.that(superType != null, "superType null for class " + name);
        return superType;
    }

    /**
     * Set the type's superType
     */
    public void setSuperType(Type superType) {
        if (superType != null) {
            if (this == vm.BYTE || this == vm.BOOLEAN) {
                Assert.that(this.superType == vm.BYTE_OR_BOOLEAN);
            } else {
                Assert.that(this.superType == superType || this.superType == null, "this="+this+" superType="+superType+" this.superType="+this.superType);
                this.superType = superType;
            }
        }
    }

    /**
     * Set the type's interface table.
     * @param interfaces The total closure of interfaces implemented by this
     * class. This does not include any interfaces in the super class's total
     * interface closure apart from except for shared direct superinterfaces.
     * @param directSuperInterfaces The number of direct superinterfaces of
     * this class. These interfaces will occupy the first directSuperInterfaces
     * slots in interfaces.
     */
    public void setInterfaces(Type[] interfaces, int directSuperInterfaces) {
        this.interfaces = interfaces;
    }

    /**
     * Get the type's interface table.
     */
    public Type[] getInterfaces() {
        return interfaces;
    }

    /**
     * Get the number of direct superinterfaces of this class.
     * @return the number of direct superinterfaces of this class.
     */
    public int getDirectSuperInterfaces() {
        return directSuperInterfaces;
    }

    /**
     * Set the type's static or instance field table.
     *
     * @param isStatic If true, set the static field table otherwise set
     * the instance field table.
     * @param fields
     */
    public void setFields(Field[] fields, boolean isStatic) {
        if (isStatic) {
            this.staticFields = fields;
        }
        else {
            this.instanceFields = fields;
        }
    }



    /**
     * Get the type's static or instance field table. The static fields of a class
     * representing a non-String constant are all at the end of the returned array
     * and do not have a valid slot number.
     * @param isStatic If true, return the static field table otherwise return
     * the instance field table.
     * @return the static or instance field table depending on isStatic.
     */
    public final Field[] getFields(boolean isStatic) {
        return (isStatic ? staticFields : instanceFields);
    }

    /**
     * Set the type's static or virtual method table.
     *
     * @param methods
     * @param isStatic If true, set the static method table otherwise set
     * the virtual method table.
     */
    public void setMethods(Method[] methods, boolean isStatic) {
        if (isStatic) {
            if (!(isInterface() || isAbstract())) {
                methods = addDefaultConstructorIfNecessary(methods);
            }
            this.staticMethods = methods;
        }
        else {
            this.virtualMethods = methods;
        }
    }

    /**
     * Add a default constructor to an array of methods if the array
     * does not contain any constructors.
     *
     * @param methods
     * @return
     */
    private Method[] addDefaultConstructorIfNecessary(Method[] methods) {
        // Add a default constructor if the class has no constructors.
        for (int i = 0; i != methods.length; i++) {
            Method m = methods[i];
            if (m.name() == vm.INIT) {
                return methods;
            }
        }

        int length = methods.length;
        methods    = (Method[])Arrays.copy(methods, 0, new Method[length+1], 1, length);
        methods[0] = Method.create(this, vm.INIT, JVMConst.ACC_PUBLIC, this, vm.ZEROTYPES);
        try {
            methods[0].convertToDefaultConstructorMethod(vm);
            methods[0].setSynthetic();
        } catch (LinkageException le) {
            le.printStackTrace();
            Assert.shouldNotReachHere();
        }
        return methods;
    }

    /**
     * Get the type's static or virtual method table.
     *
     * @param isStatic If true, return the static method table otherwise return
     * the virtual method table.
     * @return the static or virtual method table depending on isStatic.
     */
    public final Method[] getMethods(boolean isStatic) {
        return (isStatic ? staticMethods : virtualMethods);
    }

    /**
     * Get the combined size of the virtual and static methods table.
     *
     * @return the combined size of the virtual and static methods table.
     */
    public int getTotalMethodsSize() {
        int count = 0;
        for (int i = 0; i != virtualMethods.length; i++) {
            if (!vm.pruneSuite() || virtualMethods[i].includeInSuite()) {
                count++;
            }
        }
        for (int i = 0; i != staticMethods.length; i++) {
            if (!vm.pruneSuite() || staticMethods[i].includeInSuite()) {
                count++;
            }
        }
        return count;
    }


    /**
     * An enumerator to traverse all the methods of a class.
     */
    final class MethodsEnumerator implements Enumeration {
        private Method[] methods;
        private int count = -1;

        MethodsEnumerator() {
            this.methods = (staticMethods.length > 0) ? staticMethods : virtualMethods;
            advance();
        }

        private void advance() {
            while (true) {
                count++;
                if (count == methods.length) {
                    if (methods == virtualMethods) {
                        count = -1;
                        return;
                    }
                    else {
                        count = -1;
                        methods = virtualMethods;
                        continue;
                    }
                }

                Method m = methods[count];
                if (!vm.pruneSuite() || m.includeInSuite()) {
                    return;
                }
                Assert.that(count < methods.length);
            }
        }

        public Object nextElement() {
            if (count != -1) {
                Assert.that(count < methods.length);
                Object o = methods[count];
                advance();
                return o;
            }
            throw new NoSuchElementException(Type.this.toString());
        }

        public boolean hasMoreElements() {
            return count != -1;
        }
    }

    /**
     * Get an enumeration over all the static and virtual methods.
     * @return an enumeration over all the static and virtual methods.
     */
    public Enumeration methods() {
        return new MethodsEnumerator();
    }

    /**
     * Set the type's source file.
     */
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

   /**
    * Get the type's source file (may be null).
    */
    public String getSourceFile() {
        return sourceFile;
    }

    public String getSourceFilePath() {
        if (sourceFile != null) {
            if (sourceFilePath == null) {
                String pkg = toSignature(true, true);
                int index = pkg.lastIndexOf('.');
                if (index != -1) {
                    pkg = pkg.substring(0, index+1).replace('.', '/');
                } else {
                    pkg = "";
                }

                // Discard any directory component in the SourceFile attribute
                // (this only occurs in TCK classes).
                if ((index = sourceFile.lastIndexOf('/')) != -1) {
                    sourceFilePath = pkg + sourceFile.substring(index+1);
                } else if ((index = sourceFile.lastIndexOf('\\')) != -1) {
                    sourceFilePath = pkg + sourceFile.substring(index+1);
                } else {
                    sourceFilePath = pkg + sourceFile;
                }
            }
            return sourceFilePath;
        }
        return null;
    }

    /**
     * Return the fully qualified name of the class file that this class would
     * canonically be loaded from.
     * @return the fully qualified name of the class file that this class would
     * canonically be loaded from or null if this is an array class.
     */
    public String getClassFilePath() {
        if (isArray()) {
            return null;
        }
        return toSignature(true, true).replace('.', '/') + ".class";
    }

    /**
     * Return the internal name of the type.
     */
    public String name() {
        return name;
    }

    /**
     * Return the name of the type in the format used by Class.forName and Class.getName.
     * @return the name of the type in the format used by Class.forName and Class.getName.
     */
    public String suiteName() {
        String suiteName;
        if (isArray()) {
            int dims = Translator.countDimensions(name);
            Assert.that(dims > 0);
            suiteName = name.substring(dims);
            suiteName = suiteName.substring(1, suiteName.length() - 1);
            suiteName = name.substring(0, dims) + suiteName;
        }
        else {
            // Don't allow the "unreal" types here.
            Assert.that(name.charAt(name.length() - 1) == ';', "no external name for: "+name());
            suiteName = name.substring(1, name.length() - 1);
        }
        suiteName = suiteName.replace('/', '.');
        return suiteName;
    }



    /**
     * Get the spec defined class number for this class if this is a spec defined
     * class.
     * @return -2  : one of the special types that should not be emitted (e.g. BOGUS, BOOLEAN_OR_BYTE)
     *         -1  : a type special to the translator that should be emitted but is not in the spec (e.g. LINKAGEERROR)
     *          0  : a type that should be emitted but is not in the spec
     *        > 0  : a type that should be emitted and is in the spec
     */
    public int specID() {
        return specId;
    }

    public boolean isImplicitProxy() {
        return isProxy() && specId >= CNO.FIRST_IMPLICIT_TYPE;
    }

    /**
     * This is only to be used by Translator.
     */
    void setSpecID(int id) {
        Assert.that(id < 0 || (id >= CNO.FIRST_IMPLICIT_TYPE && id <= CNO.LAST_IMPLICIT_TYPE));
        specId = suiteId = id;
    }

    /**
     * Set the LinkageException instance that was raised when this Type was loaded/converted.
     * Any access to this type after this method will result in that exception being thrown.
     */
    public void setLinkageError(LinkageException le) {
        if (vm.exitOnLinkageError()) {
            le.printStackTrace();
            System.exit(1);
        }
        Assert.that(linkageError == null || linkageError == le);
        if (linkageError == null) {
//            le.printStackTrace();
            linkageError = le;

            // Clear element type
            elementType = null;
            // Clear static fields
//            setFields(vm.ZEROFIELDS, true);
            // Clear instance fields
//            setFields(vm.ZEROFIELDS, false);
            // Clear interfaces
//            setInterfaces(vm.ZEROTYPES, 0);
            // Clear virtual methods
//            setMethods(vm.ZEROMETHODS, false, 0);
//            overridden = 0;

            // Add a <clinit> method to staticMethods if there isn't one there already
            if (lookupMethod(vm.CLINIT, vm.ZEROTYPES, vm.VOID, null, true) == null) {
                int length       = staticMethods.length;
                staticMethods    = (Method[])Arrays.copy(staticMethods, 0, new Method[length+1], 1, length);
                staticMethods[0] = Method.create(this, vm.CLINIT, JVMConst.ACC_STATIC | JVMConst.ACC_PUBLIC, vm.VOID, vm.ZEROTYPES);
            }
            convertToErrorMethods(staticMethods,  le);
            convertToErrorMethods(virtualMethods, le);

            setState(LINKAGEERROR);
            if (vm.tracelinkageerrors()) {
                System.err.println(le.errorClass()+": " + this + ": "+ le.getMessage());
            }
        }
    }

    /**
     * Convert the bodies of a set of methods to throw a LinkageError.
     * @param methods
     * @param le
     */
    private void convertToErrorMethods(Method[] methods, LinkageException le) {
        for (int i = 0; i != methods.length; i++) {
            Method method = methods[i];
            // Create new method that re-throws the error
            try {
                Type errorClass = le.errorClass();
                // Find the most specific error class for which there
                // is actually a class file.
                while (errorClass.suiteId == -2) {
                    errorClass = errorClass.superClass();
                }
                method.convertToThrowsExceptionMethod(vm, errorClass, le.getMessage());
            } catch (LinkageException ex) {
                ex.printStackTrace();
                Assert.shouldNotReachHere();
            }
            method.setSynthetic();
        }
    }


    final public void unsetLinkageError() {
        linkageError = null;
    }

    final public LinkageException getLinkageError() {
        return linkageError;
    }

    /**
     * Check that this class is valid before any access.
     * @throws LinkageException if the class is invalid.
     */
    final public void validate() throws LinkageException {
        if (linkageError != null) {
            throw linkageError;
        }
    }

    /**
     * For a two-word type (i.e. 'long' or 'double') this returns the type
     * representing the first word of a value of this type. For all other types,
     * calling this method is an error.
     * @return the type of the first word of a value of this two-word type
     */
    public Type firstWordType() {
/*if[FLOATS]*/
        if (this == vm.DOUBLE) {
            return vm.DOUBLE;
        }
/*end[FLOATS]*/
        Assert.that(this == vm.LONG);
        return vm.LONG;
    }

    /**
     * For a two-word type (i.e. 'long' or 'double') this returns the type
     * representing the second word of a value of this type. For all other types,
     * calling this method is an error.
     * @return the type of the second word of a value of this type
     */
    public Type secondWordType() {
/*if[FLOATS]*/
        if (this == vm.DOUBLE) {
            return vm.DOUBLE2;
        }
/*end[FLOATS]*/
        Assert.that(this == vm.LONG);
        return vm.LONG2;
    }

    public boolean isSecondWordType() {
/*if[FLOATS]*/
        if (this == vm.DOUBLE2) {
            return true;
        }
/*end[FLOATS]*/
        return (this == vm.LONG2);
    }

    /**
     * Return a string representation of the type.
     */
    public String toString() {
        if (asString == null) {
            asString = toSignature();
        }
        return asString;
    }

    /**
     * Return the signature of this class.
     */
    public String toSignature() {
        return toSignature(vm.namesUseFqn(), vm.namesUseSrc());
    }

    /**
     * Get the type of this type when it is a local variable or stack value.
     * This essentially promotes boolean, byte, char and short to be int.
     * @return the type of this type when it is a local variable or stack value.
     */
    public Type localType() {
        if (this == vm.BOOLEAN || this == vm.BYTE || this == vm.BYTE_OR_BOOLEAN || this == vm.SHORT || this == vm.CHAR) {
            return vm.INT;
        }
        return this;
    }

    /**
     * Return the signature of this class.
     * @param includePackage
     * @param asSourceDecl
     * @return
     */
    public String toSignature(boolean includePackage, boolean asSourceDecl) {
        Type t = this;
        int depth = 0;
        while(t.isArray()) {
            depth++;
            t = t.elementType();
        }
        String res = t.toSignaturePrim(includePackage, asSourceDecl);
        while (depth-- > 0) {
            if (asSourceDecl) {
               res += "[]";
            }
            else {
               res = '[' + res;
            }
        }
        return res;
    }

    /**
     * Get a String representation for a list of Types.
     *
     * @param types
     * @param includePackage
     * @param asSourceDecl
     * @param sep
     * @return
     */
    public static String toSignature(Type[] types, String sep) {
        return toSignature(types, 0, types.length, sep);
    }

    /**
     * Get a String representation for a list of Types.
     * @param types
     * @param offset
     * @param length
     * @param includePackage
     * @param asSourceDecl
     * @param sep
     * @return
     */
    public static String toSignature(Type[] types, int offset, int length, String sep) {
        StringBuffer buf = new StringBuffer(types.length * 10);
        length = length+offset;
        for (int i = offset; i != length; i++) {
            Assert.that(types[i] != null);
            buf.append(types[i].toSignature());
            if (i != length - 1) {
                buf.append(sep);
            }
        }
        return buf.toString();
    }

    /**
     * Return the signature of this class.
     * @param includePackage
     * @param asSourceDecl
     * @return
     */
    private String toSignaturePrim(boolean includePackage, boolean asSourceDecl) {
        if (asSourceDecl) {
            if (this == vm.VOID)    return "void";
            if (this == vm.INT)     return "int";
            if (this == vm.LONG)    return "long";
/*if[FLOATS]*/
            if (this == vm.FLOAT)   return "float";
            if (this == vm.DOUBLE)  return "double";
/*end[FLOATS]*/
            if (this == vm.BOOLEAN) return "boolean";
            if (this == vm.CHAR)    return "char";
            if (this == vm.SHORT)   return "short";
            if (this == vm.BYTE)    return "byte";
            if (this == vm.BYTE_OR_BOOLEAN) return "byte_or_boolean";
        }
        else {
            if (this == vm.VOID)    return "V";
            if (this == vm.INT)     return "I";
            if (this == vm.LONG)    return "J";
/*if[FLOATS]*/
            if (this == vm.FLOAT)   return "F";
            if (this == vm.DOUBLE)  return "D";
/*end[FLOATS]*/
            if (this == vm.BOOLEAN) return "Z";
            if (this == vm.CHAR)    return "C";
            if (this == vm.SHORT)   return "S";
            if (this == vm.BYTE)    return "B";
            if (this == vm.BYTE_OR_BOOLEAN) return "B_or_Z";
        }
        if (name.charAt(0) == '-') {
            return name;
        }
        Assert.that(name.charAt(0) == 'L' && name.charAt(name.length() - 1) == ';', name);
        String result = name;
        if (!includePackage) {
            int base = result.lastIndexOf('/') + 1;
            if (base != -1) {
                result = "L" + result.substring(base);
            }
        }
        if (asSourceDecl) {
            result = result.substring(1,result.length() - 1).replace('/', '.');//.replace('$', '.');
        }
        return result;
    }


    /**
     * Get the Java super class of this type.
     */
    public Type superClass() {
        if (this == vm.OBJECT) {
            return null;
        }
        if (linkageError != null) {

        }
        if (isArray()) {
            return vm.OBJECT;
        } else if (linkageError != null) {
            return superType == null ? vm.OBJECT : superType;
        } else {
            try {
                return superType();
            } catch (LinkageException le) {
                // This should never happen for a non array type!
                le.printStackTrace();
                throw new RuntimeException(le.getMessage());
            }
        }
    }

    /**
     * Work out if this is a hierarchical subtype of another type.
     * @param aType The type to test for being a supertype of this type.
     * @param includeInterfaceHierarchy Search through the interface
     * hierarchy of this class if true.
     * @return true if this type is a subtype of 'aType'.
     * @throws LinkageException
     */
    public boolean isKindOf(Type aType, boolean includeInterfaceHierarchy) throws LinkageException {
        validate();
        // Primitives never match non-primitives
        if (this.isPrimitive() != aType.isPrimitive()) {
            return false;
        }

        // Check to see if this is a subclass of aType
        Type thiz = this;
        while (thiz != vm.UNIVERSE) {
            if (thiz == aType) {
                return true;
            }
            else {
                // Check implemented interfaces if required
                if (includeInterfaceHierarchy) {
                    for (int i = 0; i != thiz.interfaces.length; i++) {
                        if (aType == thiz.interfaces[i]) {
                            return true;
                        }
                    }
                }
            }
            thiz = thiz.superType();
        }


        return false;
    }

    /**
     * Work out if this type is 'narrower' than a given type. This is almost identical
     * to the vIsAssignableTo test except for how it handle interface types.
     * @param aType
     * @param interfaceTypeNarrowsConcreteType If true, then allow an interface type to
     * narrow a concrete type. This variable behaviour is required to handle the
     * case where narrowing is being performed in the context of the original
     * stack maps as opposed to the stack maps that have been fixed up after
     * liveness analysis.
     * @return true if this type can be narrowed to 'aType'.
     * @throws LinkageException
     */
    public boolean isNarrowerThan(Type aType, boolean interfaceTypeNarrowsConcreteType) throws LinkageException {
        if (interfaceTypeNarrowsConcreteType && this.isInterface() && !aType.isInterface()) {
            Assert.that(!aType.isPrimitive());
            return !aType.isKindOf(this, true);
        }
        return (this != aType && vIsAssignableTo(aType) && (aType == vm.NULLOBJECT || isKindOf(aType, true)));
    }

    /**
     * Work out this type can be assigned to another type for the purpose
     * of verification.
     * @param aType
     * @return
     */
    public boolean vIsAssignableTo(Type aType) throws LinkageException {
        validate();
        // Quickly check for common values
        if (this == vm.BOGUS && aType != vm.BOGUS) {
            return false;
        }
        if (this == aType || aType == vm.UNIVERSE || aType == vm.BOGUS) {
           return true;
        }

        // Special subtyping rules for some primitives
        if ((this == vm.BOOLEAN || this == vm.BYTE || this == vm.BYTE_OR_BOOLEAN || this == vm.SHORT || this == vm.CHAR) && aType == vm.INT) {
            return true;
        }

        // NEWOBJECT never matches
        if (aType == vm.NEWOBJECT || this == vm.NEWOBJECT) {
            return false;
        }

        // NULLOBJECT matches all non-primitives
        if (aType == vm.NULLOBJECT && !this.isPrimitive() && this != vm.UNIVERSE) {
            return true;
        }

        // For verification all interfaces are treated as java.lang.Object
        if (aType.isInterface()) {
            aType = vm.OBJECT;
        }

        // Check to see if this class is somewhere in aType's hierarchy
        if (this.isKindOf(aType, false)) {
            return true;
        }

        // If aType is some like of object and this is the null object
        // then assignment is allowed
        if (this == vm.NULLOBJECT && aType.isKindOf(vm.OBJECT, false)) {
            return true;
        }

        // This is needed to cast arrays of classes into arrays of interfaces
        if (this.isArray() && aType.isArray()) {
            return this.elementType().vIsAssignableTo(aType.elementType());
        }

        // Otherwise there is no match
        return false;
    }

    /**
     * Determine whether or not this class is accessible by a specified class.
     * @param other A class that refers to this class.
     * @return true if 'other' is null or has access to this class.
     */
    public boolean isAccessibleBy(Type other) {
        return (other == null ||
                other == this ||
                this.isPublic() ||
                inSamePackageAs(other));
    }

    /**
     * Return true if this class has a state greater than or equals to Type.LOADED.
     * @return true if this class has a state greater than or equals to Type.LOADED.
     */
    public boolean isLoaded() {
        return state.compareTo(LOADED) >= 0;
    }

    /**
     * isPrimitive
     */
    public boolean isPrimitive() {
        return this == vm.PRIMITIVE || superType == vm.PRIMITIVE || superType == vm.INT || superType == vm.BYTE_OR_BOOLEAN;
    }

    /**
     * Get the size of a field of this type in bytes. Java 2 word types are only
     * regarded as 4 bytes in size as they are expressed in the Suite format as
     * 2 consecutive word sized variables.
     * @return the size of an instance of this type in bytes.
     */
    public int sizeOfField() {
        if (isPrimitive()) {
            if (this == vm.VOID    || this == vm.INT  || this == vm.LONG)            return 4;
/*if[FLOATS]*/
            if (this == vm.FLOAT   || this == vm.DOUBLE)                             return 4;
/*end[FLOATS]*/
            if (this == vm.BOOLEAN || this == vm.BYTE || this == vm.BYTE_OR_BOOLEAN) return 1;
            if (this == vm.CHAR    || this == vm.SHORT)                              return 2;
            Assert.shouldNotReachHere();
            return -1;
        }
        else {
            return 4;
        }
    }

    /**
     * isTwoWords
     */
    public boolean isTwoWords() {
        Assert.that(this != vm.VOID);
        if (this == vm.LONG) {
            return true;
        }
/*if[FLOATS]*/
        if (this == vm.DOUBLE) {
            return true;
        }
/*end[FLOATS]*/
        return false;
    }

    /**
     * isLong
     */
    public boolean isLong() {
        return this == vm.LONG;
    }

/*if[FLOATS]*/
    /**
     * isDouble
     */
    public boolean isDouble() {
        return this == vm.DOUBLE;
    }

     /**
      * isFloatOrDouble
      */
     public boolean isFloatOrDouble() {
         return this == vm.DOUBLE || this == vm.FLOAT;
     }
/*end[FLOATS]*/

     /**
      * isArray
      */
     public boolean isArray() {
         return name.charAt(0) == '[';
     }

     /**
      * isUnreal
      */
     public boolean isUnreal() {
         return name.charAt(0) == '-';
     }

    /**
     * dimensions
     */
    public int dimensions() {
        Assert.that(this != vm.VOID);
        int i;
        for (i = 0 ; i < name.length() ; i++) {
            if (name.charAt(i) != '[') {
                return i;
            }
        }
        Assert.shouldNotReachHere();
        return 0;
    }

    /**
     * elementType
     */
    public Type elementType() {
        Assert.that(name.charAt(0) == '[',"name="+name);
        if (elementType == null) {
            elementType = vm.findOrCreateType(name.substring(1));
        }
        Assert.that(elementType != null && elementType != this);
        return elementType;
    }


    /**
     * Get the array type of this type
     */
    public Type asArray() {
        return vm.findOrCreateType("["+name);
    }

    /**
     * Return true if a given type is in the same package as this type.
     * @param aType the type to compare against.
     * @return true if aType is in the same package as this type.
     */
    public boolean inSamePackageAs(Type aType) {
        String name1 = this.name();
        String name2 = aType.name();
        int last1 = name1.lastIndexOf('/');
        int last2 = name2.lastIndexOf('/');
        if (last1 != last2) {
            return false;
        }
        if (last1 == -1) {
            return true;
        }
        for (int i = 0 ; i < last1 ; i++) {
            if (name1.charAt(i) != name2.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Assign the ID of this class within a suite if it has not already been
     * assigned. The suite ID for this class's super class(es) are set before
     * the ID of this class. This ensures that a class is preceeded by all its
     * super classes in a suite.
     *
     * @param id the next available suite ID
     * @return the next avaiable suite ID after setting this class's suite
     * ID and the ID of all it super classes.
     */
    public int assignSuiteID(int id) {
        if (includeInSuite() && suiteId == -1) {
            Type superClass = superClass();
            if (superClass != null) {
                id = superClass.assignSuiteID(id);
            }

            suiteId = id;
            return id + 1;
        } else {
            return id;
        }
    }

    /** Get the suite ID. */
    public int suiteID() {
        Assert.that(suiteId != -1, "suiteId not set for "+this);
        return suiteId;
    }

   /* ------------------------------------------------------------------------ *\
    *                               Member lookup                              *
   \* ------------------------------------------------------------------------ */

    /**
     * Lookup a field based on a given name and type.
     * @param name The field's name.
     * @param isStatic if true, then only look in the static fields for this class.
     * Otherwise, look in the instance fields of this class and recurse on the
     * superclass if it doesn't exist in this class.
     * @return the matching field or null if there isn't one.
     */
    public Field lookupField(String name, Type type, boolean isStatic) {
        Assert.that(name == vm.internString(name));
        Field[] fields = getFields(isStatic);
        for (int i = 0 ; i < fields.length ; i++) {
            Field f = fields[i];
            if (name == f.name() && f.type() == type) {
                return f;
            }
        }
        // Recurse to superclass. This is done even for static method lookup.
        if (/*!isStatic && */superClass() != null) {
            return superClass().lookupField(name, type, isStatic);
        }
        else {
            return null;
        }
    }

    /**
     * Lookup up a method based on name, parameter types and return type.
     * @param name The method name.
     * @param parmTypes The parameter types.
     * @param returnType The return type that is to also be matched.
     * @param currentClass The class context in which the lookup is performed or
     * null if there is no current class context.
     * @param isStatic if true, then only look in the static methods for this class.
     * Otherwise, look in the virtual methods of this class and recurse on the
     * superclass if it doesn't exist in this class.
     * @return the matching method or null if there isn't one.
     */
    public Method lookupMethod(String name, Type[] parmTypes, Type returnType, Type currentClass, boolean isStatic) {
        Method[] methods = getMethods(isStatic || name == vm.INIT);
        for (int i = 0 ; i < methods.length ; i++) {
            if (methods[i] != null) {
                Method m = methods[i];
                if (name == m.name() && parmTypes == m.getParms()) {
                    if (currentClass == null ||
                        currentClass == this ||
                        m.isPublic() ||
                        m.isProtected() ||
                        (!m.isPrivate() && inSamePackageAs(currentClass))) {
                        if ((m.type() == returnType) ||
                            (name == vm.INIT && returnType == vm.VOID)) {
                            return m;
                        }
                    }
                }
            }
        }

        // Recurse to superclass. This is done even for static method lookup
        // except when looking for <clinit>
        if (/*!isStatic && */superClass() != null && name != vm.INIT && name != vm.CLINIT) {
            Method method = superClass().lookupMethod(name, parmTypes, returnType, currentClass, isStatic);
            if (method != null) {
                return method;
            }
        }

        // Check implemented interfaces if this is an interface class
        if (!isStatic && isInterface() && interfaces != null) {
            for (int i = 0; i != interfaces.length; i++) {
                Method method = interfaces[i].lookupMethod(name, parmTypes, returnType, currentClass, false);
                if (method != null) {
                    return method;
                }
            }
        }

        return null;
    }

   /* ------------------------------------------------------------------------ *\
    *                         Suite pruning                                    *
   \* ------------------------------------------------------------------------ */

    public void mark() {
        if (!includeInSuite()) {
            vm.traceMark(this);
            vm.markDepth++;
            setFlag(JVMConst.ACC_INCLUDE);

            // Mark the element type of an array class
            if (isArray()) {
                elementType().mark();
            }

            if (!vm.pruneSuite() || !isProxy()) {
                // Mark superclass
                if (superClass() != null) {
                    superClass().mark();
                }

                // Mark implemented interfaces
                for (int i = 0; i != interfaces.length; i++) {
                    Type iface = interfaces[i];
                    iface.mark();

                    // Mark all implementations of the interface methods
                    Method[] ifaceMethods = iface.getMethods(false);
                    for (int j = 0; j != ifaceMethods.length; j++) {
                        Method ifaceMethod = ifaceMethods[j];
                        Method implMethod = lookupMethod(ifaceMethod.name(),
                                                         ifaceMethod.getParms(),
                                                         ifaceMethod.type(),
                                                         null,
                                                         false);
                        if (implMethod != null && implMethod.isPublic()) {
                            implMethod.mark();
                        }
                    }

                }
            }
            vm.markDepth--;
        }
        markMembers();
    }

    private boolean membersMarked;

    public void markMembers() {
        if (!vm.pruneSuite() || !isProxy() || isInterface()) {
            if (!membersMarked) {
                vm.markDepth++;
                membersMarked = true;

                // Only mark virtual methods for a proxy interface - all
                // other members of these classes will be marked if and
                // only if they are referenced by suite classes.
                if (vm.pruneSuite() && isProxy() && isInterface()) {
                    markMembers(getMethods(false));
                } else {
                    // Mark methods and fields
                    markMembers(getMethods(true));
                    markMembers(getMethods(false));
                    markMembers(getFields(true));
                    markMembers(getFields(false));

                    // Mark methods called internally by the VM
                    Method m = lookupMethod(vm.CLINIT, vm.ZEROTYPES, vm.VOID, null, true);
                    if (m != null && !m.includeInSuite() && m.parent() == this) {
                        m.mark();
                    }
                    m = lookupMethod(vm.INIT, vm.ZEROTYPES, this, null, true);
                    if (m != null && !m.includeInSuite() && m.parent() == this) {
                        m.mark();
                    }
                    m = lookupMethod(vm.MAIN, vm.PARAMS_FOR_MAIN, vm.VOID, null, true);
                    if (m != null && !m.includeInSuite() && m.parent() == this) {
                        m.mark();
                    }
                }
                vm.markDepth--;
            }
        }
    }

    private void markMembers(Member[] members) {
        Assert.that(!vm.pruneSuite() || !isProxy() || isInterface());
        for (int i = 0; i != members.length; i++) {
            Member member = members[i];
            member.mark();
        }
    }

   /* ------------------------------------------------------------------------ *\
    *                           Slot number assignment                         *
   \* ------------------------------------------------------------------------ */

    private boolean fieldSlotsAssigned;
    private boolean methodSlotsAssigned;
    private boolean hasAtLeastOneIncludedMember;
    /** Cached value. */
    private int virtualMethodSlotCount = -1;
    /** Cached value. */
    private int instanceFieldSlotCount = -1;

    /**
     * A Comparer implementation for sorting members according to their slot numbers.
     * This also puts all non-suite-included members at the end of a sorted array.
     */
    public static Comparer MEMBER_SLOT_COMPARER = new Comparer() {
        public int compare(Object o1, Object o2) {
            Member m1 = (Member)o1;
            Member m2 = (Member)o2;
            int m1Slot = m1.includeInSuite() ? m1.slot() : Integer.MAX_VALUE ;
            int m2Slot = m2.includeInSuite() ? m2.slot() : Integer.MAX_VALUE ;
            if (m1Slot < m2Slot) {
                return -1;
            }
            else if (m1Slot > m2Slot) {
                return 1;
            }
            else {
                return 0;
            }
        }
    };

    /**
     * Get the number of slots occupied by the instance fields of this class.
     * @return the number of slots occupied by the instance fields of this class.
     */
    private int instanceFieldSlotCount() {
        if (!includeInSuite()) {
            return 0;
        }
        Assert.that(fieldSlotsAssigned);
        if (instanceFieldSlotCount == -1) {
            instanceFieldSlotCount = 0;
            for (int i = 0; i != instanceFields.length; i++) {
                Field field = instanceFields[i];
                if (!field.isPrimitiveConstant() && (!vm.pruneSuite() || field.includeInSuite())) {
                    instanceFieldSlotCount += (field.type().isTwoWords() ? 2 : 1);
                }
            }
        }
        return instanceFieldSlotCount;
    }

    /**
     * Get the number of slots occupied by the virtual methods of this class.
     * @param includeOverriders If true, don't count methods that override a super class method.
     * @return
     */
    private int virtualMethodSlotCount(boolean includeOverriders) {
        if (!includeInSuite()) {
            return 0;
        }
        Assert.that(methodSlotsAssigned);
        if (virtualMethodSlotCount == -1) {
            virtualMethodSlotCount = 0;
            for (int i = 0; i != virtualMethods.length; i++) {
                Method method = virtualMethods[i];
                if (!vm.pruneSuite() || method.includeInSuite()) {
                    if (includeOverriders || method.overridden() == null) {
                        virtualMethodSlotCount++;
                    }
                }
            }
        }
        return virtualMethodSlotCount;
    }

    private void handleDeadMember(Member m, String type) {
        vm.tracer().traceln(vm.tracedeadcode(), type+" is dead: "+m);
//        m.mark();
    }

    private int assignFieldSlot(Field field, int slot) {
        if (!field.isPrimitiveConstant()) {
            if (!isProxy() && !field.includeInSuite()) {
                handleDeadMember(field, "Field");
            }
            if (!vm.pruneSuite() || field.includeInSuite()) {
                field.setSlot(slot++);
                if (field.type().isTwoWords()) {
                    slot++;
                }
            }
        }
        return slot;
    }

    private int assignMethodSlot(Method method, int slot) {
        if (!isProxy() && !method.includeInSuite()) {
            handleDeadMember(method, "Method");
        }
        if (!vm.pruneSuite() || method.includeInSuite()) {
            Method smethod = method.isStatic() ? null : method.overridden();
            if (smethod != null && (!vm.pruneSuite() || smethod.includeInSuite())) {
                method.setSlot(smethod.slot());
            }
            else {
                method.setSlot(slot++);
            }
        }
        return slot;
    }

    /**
     * Assign slot numbers to the methods of this class. Upon completion, the
     * methods array (as returned by getMethods()) is sorted according to
     * slot number. If vm.pruneSuite() returns true, then all non included
     * methods are at the end of the array.
     */
    public void assignMethodSlotNumbers() {
        if (methodSlotsAssigned) {
            return;
        }
        methodSlotsAssigned = true;

        // Static methods
        if (staticMethods.length > 0) {

            // Sort the methods into a deterministic ordering.
            sortMethodsBySignature(staticMethods);

            int slot = SquawkConstants.SLOT_FIRST;
            for (int i = 0; i != staticMethods.length; i++) {
                Method method = staticMethods[i];
                slot = assignMethodSlot(method, slot);
            }
            Arrays.sort(staticMethods, MEMBER_SLOT_COMPARER);
            hasAtLeastOneIncludedMember |= (slot > SquawkConstants.SLOT_FIRST);
        }

        // Virtual methods
        if (virtualMethods.length > 0) {
            int slot = SquawkConstants.SLOT_FIRST;

            // Adjust slot to account for super class methods
            if (!isInterface()) {
                Type sclass = superClass();
                while (sclass != null) {
                    slot += (sclass.virtualMethodSlotCount(false));
                    sclass = sclass.superClass();
                }
            }

            // Sort the methods into a deterministic ordering.
            sortMethodsBySignature(virtualMethods);

            for (int i = 0; i != virtualMethods.length; i++) {
                Method method  = virtualMethods[i];
                slot = assignMethodSlot(method, slot);
            }
            Arrays.sort(virtualMethods, MEMBER_SLOT_COMPARER);
            hasAtLeastOneIncludedMember |= (slot > SquawkConstants.SLOT_FIRST);
        }
    }

    /**
     * Sort an array of methods with the primary sort key being a method's
     * signature.
     *
     * @param fields
     */
    private static void sortMethodsBySignature(Method[] methods) {
        Arrays.sort(methods, new Comparer() {
            public int compare(Object o1, Object o2) {
                if (o1 == o2) {
                    return 0;
                }
                Method m1 = (Method)o1;
                Method m2 = (Method)o2;

                // Sort by name
                int result = m1.name().compareTo(m2.name());
                if (result == 0) {
                    // Sort by return type
                    result = m1.type().name().compareTo(m2.type().name());
                    if (result == 0) {
                        // Sort by parameter count
                        Type[] m1Parms = m1.getParms();
                        Type[] m2Parms = m2.getParms();

                        result = m1Parms.length - m2Parms.length;
                        if (result == 0) {
                            // Sort by parameter types
                            for (int i = 0; i != m1Parms.length; ++i) {
                                if ((result = m1Parms[i].name().compareTo(m2Parms[i].name())) != 0) {
                                    break;
                                }
                            }

                            // There should never be identical methods by this point!
                            Assert.that(result != 0);
                        }
                    }
                }
                return result;
            }
        });
    }

    /**
     * Assign slot numbers to the fields of this class. Upon completion, the
     * fields array (as returned by getFields()) is sorted according to
     * slot number. If vm.pruneSuite() returns true, then all non-included
     * fields are at the end of the array.
     */
    public void assignFieldSlotNumbers() {
        if (fieldSlotsAssigned) {
            return;
        }
        fieldSlotsAssigned = true;

        // Static fields
        if (staticFields.length > 0) {

            // Sort the fields into a deterministic ordering.
            sortFieldsBySizePointernessName(staticFields);

            int slot = SquawkConstants.SLOT_FIRST;
            for (int i = 0; i != staticFields.length; i++) {
                Field field = staticFields[i];
                slot = assignFieldSlot(field, slot);
            }
            Arrays.sort(staticFields, MEMBER_SLOT_COMPARER);
            hasAtLeastOneIncludedMember |= (slot > SquawkConstants.SLOT_FIRST);
        }

        // Instance fields
        if (instanceFields.length > 0) {
            int slot = SquawkConstants.SLOT_FIRST;
            if (!isInterface()) {
                Type sclass = superClass();
                while (sclass != null) {
                    slot += (sclass.instanceFieldSlotCount());
                    sclass = sclass.superClass();
                }
            }

            // Sort the fields into a deterministic ordering.
            sortFieldsBySizePointernessName(instanceFields);

            for (int i = 0; i != instanceFields.length; i++) {
                Field field = instanceFields[i];
                slot = assignFieldSlot(field, slot);
            }
            Arrays.sort(instanceFields, MEMBER_SLOT_COMPARER);
            hasAtLeastOneIncludedMember |= (slot > SquawkConstants.SLOT_FIRST);
        }
    }

    /**
     * Sort an array of fields with the primary sort key being a field's
     * size (decreasing order), the secondary key being a field's
     * pointerness (pointer-typed fields sort earlier) and the ternary
     * key being the field's name.
     *
     * @param fields
     */
    private static void sortFieldsBySizePointernessName(Field[] fields) {
        // Sort instance fields so that larger sized fields will be
        // given a lower slot number which in turn enables the loader
        // to do a simple form of object packing. Also, pointer fields
        // come before non-pointer fields.
        Arrays.sort(fields, new Comparer() {
            public int compare(Object o1, Object o2) {
                if (o1 == o2) {
                    return 0;
                }
                Field f1 = (Field)o1;
                Field f2 = (Field)o2;

                Type ftype1 = f1.type();
                Type ftype2 = f2.type();

                // Sort by size of field's type
                if (ftype1.sizeOfField() < ftype2.sizeOfField()) {
                    return 1;
                } else if (ftype1.sizeOfField() > ftype2.sizeOfField()) {
                    return -1;
                } else {

                    // Sort by pointerness of field's type
                    int ptr1 = ftype1.isPrimitive() ? 1 : 0;
                    int ptr2 = ftype2.isPrimitive() ? 1 : 0;
                    if (ptr1 != ptr2) {
                        return ptr1 - ptr2;
                    } else {

                        // Sort by name
                        return f1.name().compareTo(f2.name());
                    }
                }
            }
        });
    }


    /**
     * Determine whether or not this class has any members that were assigned
     * slot numbers (i.e. whether or not this class has any members that should
     * be included in the suite).
     *
     * @return
     */
    public boolean hasAtLeastOneIncludedMember() {
        Assert.that(fieldSlotsAssigned && methodSlotsAssigned);
        return hasAtLeastOneIncludedMember;
    }

    /* ------------------------------------------------------------------------ *\
     *                       Substitute hidden constructors                     *
    \* ------------------------------------------------------------------------ */

    private Method findMatchingConstructor(Method[] methods, Type[] parms) {
        Assert.that(parms[0] == this);
nextCandidate:
        for (int i = 0; i != methods.length; ++i ) {
            Method candidate = methods[i];
            if (candidate.name() == vm.INIT) {
                Type[] cParms = candidate.getParms();
                if (cParms.length == parms.length - 1) {
                    for (int j = 0; j != cParms.length; ++j) {
                        if (cParms[j] != parms[j+1]) {
                            continue nextCandidate;
                        }
                    }
                    return candidate;
                }
            }
        }
        return null;
    }


    /**
     * For all static methods in this class whose name equals the
     * value of vm.SQUAWK_INIT, try and find a constructor with
     * matching parameter types and if found, replace the body of
     * the constructor with the body of the corresponding SQUAWK_INIT
     * method.
     */
    public void substituteHiddenConstructors() {
        Method[] methods = getMethods(true);
        for (int i = 0; i != methods.length; ++i ) {
            Method method = methods[i];
            if (method.name() == vm.SQUAWK_INIT) {
                Assert.that(method.isPrivate());
                Method candidate = findMatchingConstructor(methods, method.getParms());
                if (candidate != null) {
                    candidate.replaceIR(method);

                    // Remove the hidden constructor from the suite
                    method.unsetFlag(JVMConst.ACC_INCLUDE);

                }
            }
        }
    }

    /* ---------------------------------------------------------------------- *\
     *                  VMAccessedAttribute methods                           *
    \* ---------------------------------------------------------------------- */


    /**
     * Lookup a special field in this class that specifies Squawk implementation
     * specific attributes for this class or a field/method of this class.
     * These fields are all static final fields of type String.
     *
     * @param name The name of the field to search for.
     * @return the found field or null if there was no match.
     */
    private Field lookupSquawkAttributeField(String name) {
        Field field = lookupField(vm.internString(name), vm.INT, true);
        if (field != null && field.parent() != this) {
            field = null;
        }
        Assert.that(field == null ||
                    (field.isFinal() &&
                     field.getConstantValue() != null),
                     field+ " must be static, final and have a constant value");
         return field;
    }

    /**
     * Get the class attribute describing the Squawk-specific implementation
     * details (if any) for this class.
     *
     * @return
     */
    public VMAccessedAttribute getVMAccessedAttribute() {
        VMAccessedAttribute attribute = null;
        if (!isProxy()) {
            Field classAttributes = lookupSquawkAttributeField("class$vmaccessed");
            if (classAttributes != null) {
                int classFlags = ((Integer)classAttributes.getConstantValue()).intValue();
                attribute = new VMAccessedAttribute(classFlags);

                // Get the attributes for the instance fields accessed by the VM
                Field[] fields = instanceFields;
                for (int i = 0; i != fields.length; ++i) {
                    Field field = fields[i];
                    if (field.includeInSuite()) {
                        Field fieldAttributes = lookupSquawkAttributeField(field.name()+"$vmaccessed_field");
                        if (fieldAttributes != null) {
                            int fieldFlags = ((Integer)fieldAttributes.getConstantValue()).intValue();
                            attribute.addInstanceFieldsTableItem(field.slot(), fieldFlags);
                        }
                    }
                }

                // Get the attributes for the static methods accessed by the VM
                Method[] methods = staticMethods;
                for (int i = 0; i != methods.length; ++i) {
                    Method method = methods[i];
                    if (method.includeInSuite()) {
                        Field methodAttributes = lookupSquawkAttributeField(method.name()+"$vmaccessed_method");
                        if (methodAttributes != null) {
                            int methodFlags = ((Integer)methodAttributes.getConstantValue()).intValue();
                            attribute.addStaticMethodsTableItem(method.slot(), methodFlags);
                        }
                    }
                }
            }
        }
        return attribute;
    }
}


