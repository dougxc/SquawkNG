
package com.sun.squawk.translator.loader;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.JVMConst;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.Vector;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Enumeration;
import javax.microedition.io.*;
import com.sun.squawk.io.connections.*;

/**
 * This is the classfile loader used by the translator.
 */
public class ClassFileLoader {

    /** Static constructor. */
    public static ClassFileLoader create(Translator vm, ClasspathConnection classPath) throws LinkageException {
        return new ClassFileLoader(vm, classPath);
    }

    /** The Translator context for this class loader. */
    private Translator vm;
    /** Classpath connection for classes and sources. */
    private ClasspathConnection classPath;



    /**
     * Private constructor.
     * @param vm
     * @param classpath
     * @throws LinkageException
     */
    private ClassFileLoader(Translator vm, ClasspathConnection classPath) throws LinkageException {
        this.vm = vm;
        this.classPath = classPath;
    }

    public InputStream openSourceFile(String path) {
        try {
            return classPath.openInputStream(path);
        } catch (IOException ioe) {
            return null;
        }
    }

    /**
     * Load a type from its classfile.
     * @param className The name of the class to load.
     */
    public Type load(Type type) throws LinkageException {
        Assert.that(type.getState().compareTo(Type.LOADED) < 0);
        /*
         * Transform class name into a file name:
         *
         *   "Ljava/foo/Bar;" -> "java/foo/Bar"
         *
         *   "java.foo.Bar" -> "java/foo/Bar"
         */
        String className = type.name();
        String fileName;
        if (className.charAt(className.length() - 1) == ';') {
            Assert.that(className.charAt(0) == 'L');
            fileName = className.substring(1, className.length() - 1);
        }
        else {
            fileName = className;
        }

        Assert.that(className.indexOf('.') == -1);

        // Special transformation "java/lang/Class" -> "java/lang/Klass"
        if (fileName.equals("java/lang/Class")) {
            fileName = "java/lang/Klass";
        }

        InputStream is = null;
        try {
            try {
                is = classPath.openInputStream(fileName + ".class");
            } catch (IOException ioe) {
                // Revert back to the standard class file for "java.lang.Class"
                // if the Squawk VM specific one is not found
                if (fileName.equals("java/lang/Klass")) {
                    fileName = "java/lang/Class";
                    try {
                        is = classPath.openInputStream(fileName + ".class");
                    } catch (IOException ioe2) {
                        throw new LinkageException(vm.NOCLASSDEFFOUNDERROR, "NoClassDefFound: " + fileName, null, ioe2);
                    }
                } else {
                    throw new LinkageException(vm.NOCLASSDEFFOUNDERROR, "NoClassDefFound: " + fileName, null, ioe);
                }
            }
            return load(fileName, type, is);
        } catch (AssertionFailed ae) {
            ae.addContext("While loading "+type);
            throw ae;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * The loader's main function.
     * @param fileName
     * @param type
     * @param is
     * @return
     * @throws LinkageException
     */
    private Type load(String fileName, Type type, InputStream is) throws LinkageException {

        // Write trace message
        vm.tracer().traceln(vm.traceloading(fileName), "Loading class " + fileName);

        Assert.that(fileName.indexOf('\\') == -1);

        // Wrap the input stream in a ClassFileInputStream
        ClassFileInputStream in = new ClassFileInputStream(is,  fileName, vm);

        if (type.getState() == Type.LOADING) {
            throw in.linkageError(vm.CLASSCIRCULARITYERROR, type.name());
        }

        // Mark type as being loaded
        Assert.that(type.getState() == Type.DEFINED);
        type.setState(Type.LOADING);

        try {
            try {
                // Set trace if requested
                in.setTrace(vm.traceraw(fileName));

                // Read the magic values
                loadMagicValues(in);

                // Read the constant pool
                ConstantPool pool = loadConstantPool(in, type);

                // Read the class information
                Type t = loadClassInfo(in, type, pool);
                Assert.that(t == type);

                // Read the interface definitions
                loadInterfaces(in, pool, type);

                // Trace
                String classOrInterface = type.isInterface() ? "interface " : "class ";
                vm.tracer().traceln(vm.traceclassinfo(type.name()), "\n"+classOrInterface+type.name()+"        (extends "+type.superType().name()+")");
                traceInterfaces(type);

                // Read the field definitions
                loadFields(in, pool, type);

                // Read the method definitions
                loadMethods(in, pool, type);

                // Read the extra attributes
                loadExtraAttributes(in, pool, type);

                // Close the input stream
                in.close();

                 // Trace
                vm.tracer().traceln(vm.traceloading(fileName), "Finished Loading class " + fileName);

            } catch (IOException ioe) {
                throw in.classFormatError(ioe);
            }
        } catch (LinkageException ex) {
            /*
             * Mark type as raw again as errors occuring during classfile loading are "transient" errors.
             * That is, their cause is temporal in nature and may not occur
             * if a different classfile is submitted at a later point.
             */
            Assert.that(type.getState() == Type.LOADING);
            type.setState(Type.DEFINED);
            throw ex;
        }

        // Mark type as loaded
        Assert.that(type.getState() == Type.LOADING);
        type.setState(Type.LOADED);
        vm.typeLoaded(type);

        // Return the new type
        return type;
    }


    /**
     * Load the magic values.
     * @param in
     * @throws IOException
     * @throws LinkageException
     */
    private void loadMagicValues(ClassFileInputStream in) throws IOException, LinkageException {
        int magic = in.readInt("magic");
        int minor = in.readUnsignedShort("minor");
        int major = in.readUnsignedShort("major");
        if (magic != 0xCAFEBABE) {
            throw in.classFormatError("Bad magic value");
        }
        // Support JDK1.3 and 1.4 classfiles
        if (!((major == 45 /*&& minor == 3*/) ||
              (major == 46 && minor == 0) ||
              (major == 47 && minor == 0) ||
              (major == 48 && minor == 0))) {
            throw in.classFormatError("Bad class file version number: " + major + ":" + minor);
        }
    }

    /**
     * Load the constant pool.
     * @param in
     * @return
     * @throws IOException
     * @throws LinkageException
     */
    private ConstantPool loadConstantPool(ClassFileInputStream in, Type type) throws IOException, LinkageException {
        return ConstantPool.create(vm, in, type);
    }

    /**
     * Load the class information.
     * @param in
     * @param thisType
     * @param pool
     * @return
     * @throws IOException
     * @throws LinkageException
     */
    private Type loadClassInfo(ClassFileInputStream in, Type thisType, ConstantPool pool) throws IOException, LinkageException {
        int accessFlags = in.readUnsignedShort("cls-flags");
        int classIndex  = in.readUnsignedShort("cls-index");
        int superIndex  = in.readUnsignedShort("cls-super index");

        pool.verifyClassFlags(accessFlags);

        // Loading the constant pool will have created the Type object.
        Type type = pool.bootstrapType(classIndex);
        if (type != thisType) {
            /*
             * VMSpec 5.3.5:
             *
             *   Otherwise, if the purported representation does not actually
             *   represent a class named N, loading throws an instance of
             *   NoClassDefFoundError or an instance of one of its
             *   subclasses.
             */
             throw in.linkageError(vm.NOCLASSDEFFOUNDERROR, "ClassFile.this_class indicates wrong type");
        }

        // Set the access flags
        type.setFlags(accessFlags & JVMConst.VALID_CLASS_FLAGS_MASK);

        if (superIndex != 0) {
            Type superType = pool.resolveType(superIndex);
            Assert.that(superType != null);

            // Cannot inherit from an array class.
            if (superType.isArray()) {
                throw in.linkageError(vm.CLASSFORMATERROR, "Cannot inherit from array class");
            }

            /*
             * The superclass cannot be an interface. From the
             * JVM Spec section 5.3.5:
             *
             *   If the class of interface named as the direct
             *   superclass of C is in fact an interface, loading
             *   throws an IncompatibleClassChangeError.
             */
            if (superType.isInterface()) {
                throw in.linkageError(vm.INCOMPATIBLECLASSCHANGEERROR, "Cannot extend an interface class");
            }

            /*
             * The superclass cannot be final.
             * Inheriting from a final class is a VerifyError according
             * to J2SE JVM behaviour. There is no explicit
             * documentation in the JVM Spec.
             */
            if (superType.isFinal()) {
                throw in.linkageError(vm.VERIFYERROR, "Cannot extend a final class");
            }

            // The current class must have access to its super class
//            verifyClassAccess((CLASS)superClass,clazz);

            // If this is an interface class, its superclass must be
            // java.lang.Object.
            if (type.isInterface() && superType != vm.OBJECT) {
                throw in.linkageError(vm.CLASSFORMATERROR, "Interface class must inherit from java.lang.Object");
            }

            superType.validate();
            type.setSuperType(superType);
        }
        else
        if (type != vm.OBJECT) {
            throw in.linkageError(vm.CLASSFORMATERROR, "non Object class must have super-type");
        }

        if (type == null) {
            throw in.classFormatError("invalid this_class entry");
        }

        return type;
    }

    /**
     * Load the class's interfaces.
     * @param in
     * @param pool
     * @param type
     * @throws IOException
     * @throws LinkageException
     */
    private void loadInterfaces(ClassFileInputStream in, ConstantPool pool, Type type) throws IOException, LinkageException {
        int count = in.readUnsignedShort("i/f-count");

        // Read direct superinterfaces
        Vector interfaces = new Vector(count);
        for (int i = 0; i < count; i++) {
            Type iface = pool.resolveType(in.readUnsignedShort("i/f-index"));
            if (!iface.isInterface()) {
                throw new LinkageException(vm.INCOMPATIBLECLASSCHANGEERROR, "Implementing non-interface class: " + iface);
            }
            if (interfaces.indexOf(iface) < 0) {
                interfaces.addElement(iface);
            }
        }
        int directSuperInterfaces = interfaces.size();

        // Get the closure of interfaces implemented by the super class
        // so that we don't add them to the closure of interfaces implemented
        // by this class. If this is a non-abstract class, then the closure
        // is taken from the first non-abstract super class. This is required
        // so that the interface method implementation look up table for
        // this class also includes the methods implemented by abstract
        // superclasses (which have no such table).
        Hashtable superClassInterfaces = new Hashtable();
        if (!type.isInterface() && type != vm.OBJECT) {
            Type superClass = type.superClass();
            if (!type.isAbstract()) {
                while (superClass.isAbstract()) {

                    Type[] sinterfaces = superClass.getInterfaces();
                    for (int i = 0; i != sinterfaces.length; i++) {
                        Type iface = sinterfaces[i];
                        if (interfaces.indexOf(iface) < 0) {
                            interfaces.addElement(iface);
                        }
                    }

                    superClass = superClass.superClass();
                }
            }
            Type[] sinterfaces = superClass.getInterfaces();
            for (int i = 0; i != sinterfaces.length; i++) {
                superClassInterfaces.put(sinterfaces[i], sinterfaces[i]);
            }
        }

        // Get the closure of interfaces implemented this class
        for (int i = 0; i != directSuperInterfaces; i++) {
            Type iface = (Type)interfaces.elementAt(i);
            Type[] superInterfaces = iface.getInterfaces();
            for (int k = 0 ; k < superInterfaces.length ; k++) {
                Type sface = superInterfaces[k];
                if (interfaces.indexOf(sface) < 0 && !superClassInterfaces.containsKey(sface)) {
                    interfaces.addElement(sface);
                }
            }
        }

        if (interfaces.size() > 0) {
            Type[] uniqInterfaces = new Type[interfaces.size()];
            interfaces.copyInto(uniqInterfaces);
            type.setInterfaces(uniqInterfaces, directSuperInterfaces);
        }
    }


    /**
     * Helper method to trace a class's interfaces.
     * @param type
     */
    private void traceInterfaces(Type type) {
        Type[] interfaces = type.getInterfaces();
        for (int i = 0 ; i < interfaces.length ; i++) {
            vm.tracer().traceln(vm.traceclassinfo(type.name()), "    Implements:\t"+interfaces[i].name());
        }
    }

    /**
     * Load the class's fields.
     *
     * @param in
     * @param pool
     * @param type
     * @throws IOException
     * @throws LinkageException
     */
    private void loadFields(ClassFileInputStream in, ConstantPool pool, Type type) throws IOException, LinkageException {

        // Get count of fields
        final int count = in.readUnsignedShort("fld-count");
        if (count == 0) {
            return;
        }

        // Allocate vector to collect the fields
        Vector fields = new Vector(count);

        int staticFieldCount = 0;

        // Read in all the fields
        for (int i = 0; i < count; i++) {
            int accessFlags     = in.readUnsignedShort("fld-flags");
            int nameIndex       = in.readUnsignedShort("fld-nameIndex");
            int descriptorIndex = in.readUnsignedShort("fld-descIndex");
            int attributesCount = in.readUnsignedShort("fld-AttbCount");
            int slot            = -1;
            int constantValueIndex  = 0;

            String fieldName = pool.getUtf8Interning(nameIndex);
            String fieldSig  = pool.getUtf8(descriptorIndex);

            pool.verifyFieldFlags(accessFlags, type.flags());
            pool.verifyName(fieldName, ConstantPool.ValidNameType.FIELD);
            Type fieldType = pool.verifyFieldType(fieldSig);

            accessFlags &= JVMConst.VALID_FIELD_FLAGS_MASK;

            // Process the field's attributes
            for (int j = 0; j < attributesCount; j++) {
                int    attributeNameIndex = in.readUnsignedShort("fld-att-nameIndex");
                int    attributeLength    = in.readInt("fld-att-length");
                String attributeName      = pool.getUtf8(attributeNameIndex);

                if (attributeName.equals("ConstantValue")) {
                    if (attributeLength != 2) {
                        throw in.classFormatError("ConstantValue attribute length is not 2");
                    }
                    if (constantValueIndex != 0) {
                        throw in.classFormatError("Duplicate ConstantValue attribute");
                    }
                    constantValueIndex = in.readUnsignedShort("fld-ConstantValue"); // Get the variable initialzation value
                    if (constantValueIndex == 0) {
                        throw in.classFormatError("Bad ConstantValue index");
                    }
                    // A field_info structure for a non-static field that has a ConstantValue
                    // attribute must be silently ignored.
                    if ((accessFlags & JVMConst.ACC_STATIC) == 0) {
                        //throw in.classFormatError("ConstantValue attribute for non-static field " + fieldName);
                        constantValueIndex = 0;
                    }
                } else {
                    while (attributeLength-- > 0) {
                        in.readByte(null); // Ignore this attribute
                    }
                }
            }

            // Create the field
            Field field = Field.create(vm, type, fieldName, fieldType, accessFlags);

            // Adjust the static field counter if necessary
            if (field.isStatic()) {
                staticFieldCount++;
            }

            // Get the constant value attrbute (if any).
            field.setConstantValue(getFieldConstantValue(in, pool, fieldType, constantValueIndex));

            // Verify that there are no duplicate fields.
            for (Enumeration e = fields.elements(); e.hasMoreElements();) {
                Field f = (Field)e.nextElement();
                if (f.name() == fieldName &&
                    f.type() == fieldType)
                {
                    throw in.classFormatError("Duplicate field found: "+f);
                }
            }

            fields.addElement(field);

            // Tracing
            if (vm.traceclassinfo(field.toString())) {
                String constantStr = field.getConstantValue() == null ? "" : " \t(constantValue="+field.getConstantValue()+")";
                String staticStr = (field.isStatic()) ? "static " : "";
                vm.tracer().traceln(true, "    Field:\t"+staticStr+field+constantStr);
            }
        }

        // Partition the fields into the static and instance field tables.
        int instanceFieldCount = count - staticFieldCount;
        Field[] instanceFields = type.isInterface() ? vm.ZEROFIELDS :  new Field[instanceFieldCount];
        Field[] staticFields = new Field[staticFieldCount];
        int ifield = 0;
        int sfield = 0;
        for (Enumeration e = fields.elements(); e.hasMoreElements();) {
            Field field = (Field)e.nextElement();
            if (field.isStatic()) {
                staticFields[sfield++] = field;
            }
            else {
                instanceFields[ifield++] = field;
            }
        }
        type.setFields(staticFields, true);
        type.setFields(instanceFields, false);
    }

    /**
     * Get the object corresponding to the ConstantValue attribute for a field
     * if it has one.
     *
     * @param in
     * @param pool
     * @param fieldType
     * @param constantValueIndex
     * @return
     * @throws LinkageException
     */
    private Object getFieldConstantValue(ClassFileInputStream in, ConstantPool pool, Type fieldType, int constantValueIndex) throws LinkageException {
        if (constantValueIndex != 0) {
            // Verify that the initial value is of the right type for the field
            if (fieldType == vm.LONG) {
                return pool.getEntry(constantValueIndex, JVMConst.CONSTANT_Long);
            }
/*if[FLOATS]*/
            else if (fieldType == vm.FLOAT) {
                return pool.getEntry(constantValueIndex, JVMConst.CONSTANT_Float);
            }
            else if (fieldType == vm.DOUBLE) {
               return pool.getEntry(constantValueIndex, JVMConst.CONSTANT_Double);
            }
/*end[FLOATS]*/
            else if (fieldType == vm.INT ||
                     fieldType == vm.SHORT ||
                     fieldType == vm.CHAR ||
                     fieldType == vm.BYTE ||
                     fieldType == vm.BOOLEAN) {
                return pool.getEntry(constantValueIndex, JVMConst.CONSTANT_Integer);
            }
            else if (fieldType == vm.STRING) {
                return pool.getEntry(constantValueIndex, JVMConst.CONSTANT_String);
            }
            else {
                throw in.classFormatError("ConstantValue for field of type: " + fieldType.name());
            }
        } else {
            return null;
        }
    }

    /**
     * Load the class's methods.
     *
     * @param in
     * @param pool
     * @param type
     * @throws IOException
     * @throws LinkageException
     */
    private void loadMethods(ClassFileInputStream in, ConstantPool pool, Type type) throws IOException, LinkageException {

        // Get count of methods and return if there are none
        int count = in.readUnsignedShort("mth-count");
        if (count == 0 && (type.isInterface() || type.isAbstract())) {
            return;
        }

        // Allocate the method vector
        Vector methods = new Vector(count);

        int staticMethodCount = 0;

        // Read in all the methods
        for (int i = 0; i < count; i++) {
            int accessFlags     = in.readUnsignedShort("mth-flags");
            int nameIndex       = in.readUnsignedShort("mth-nameIndex");
            int descriptorIndex = in.readUnsignedShort("mth-descIndex");
            int attributesCount = in.readUnsignedShort("mth-AttbCount");
            boolean isDefaultInit = false;

            String methodName = pool.getUtf8Interning(nameIndex);
            String methodSig  = pool.getUtf8(descriptorIndex);

            if (methodName == vm.CLINIT) {
                /*
                 * JVM Spec 4.6:
                 *
                 * Class and interface initialization methods are called
                 * implicitly by the Java virtual machine; the value of their
                 * access_flags item is ignored exception for the settings of the
                 * ACC_STRICT flag.
                 */
                accessFlags = (accessFlags & (JVMConst.ACC_STRICT)) | JVMConst.ACC_STATIC;
            } else {
                pool.verifyMethodFlags(accessFlags, type.flags(), methodName == vm.INIT);
            }
            accessFlags &= JVMConst.VALID_METHOD_FLAGS_MASK;

            pool.verifyName(methodName, ConstantPool.ValidNameType.METHOD);
            Method.SignatureTypes methodSigTypes = pool.verifyMethodType(methodSig, (methodName == vm.CLINIT || methodName == vm.INIT));

            // If this is an <init> method, then change its return type to be the parent.
            if (methodName == vm.INIT) {
                Assert.that((accessFlags & JVMConst.ACC_STATIC) == 0);
                Assert.that(methodSigTypes.returnType == vm.VOID);
                methodSigTypes = methodSigTypes.modifyReturnType(type);
            }

            // Create the method structure
            Method method = Method.create(type, methodName, accessFlags, methodSigTypes.returnType, methodSigTypes.parmTypes);

            // Process the method's attributes
            boolean hasCodeAttribute = false;
            boolean hasExceptionTable = false;
            for (int j = 0; j < attributesCount; j++) {
                int    attributeNameIndex = in.readUnsignedShort("mth-att-nameIndex");
                int    attributeLength    = in.readInt("mth-att-length");
                String attributeName      = pool.getUtf8(attributeNameIndex);

                if (attributeName.equals("Code")) {
                    if (hasCodeAttribute) {
                        throw in.classFormatError("Duplicate Code attribute in method");
                    }
                    hasCodeAttribute = true;
                    // Ignore code attribute of proxy-classes
                    if (type.isProxy()) {
                        while (attributeLength-- > 0) {
                            in.readByte(null); // Ignore this attribute
                        }
                    } else {
                        loadMethodCode(in, pool, method, attributeLength);
                    }
                } else {
                    if (attributeName.equals("Exceptions")) {
                        if (hasExceptionTable) {
                            throw in.classFormatError("Duplicate Exceptions attribute in method");
                        }
                        hasExceptionTable = true;
                    }
                    while (attributeLength-- > 0) {
                        in.readByte(null); // Ignore this attribute
                    }
                }
            }

            // Verify that the methods that should have a Code attribute do and
            // vice-versa.
            if ((method.isAbstract() || method.isNative()) == hasCodeAttribute) {
                if (hasCodeAttribute) {
                    throw in.classFormatError("Code attribute supplied for native or abstract method");
                } else {
                    throw in.classFormatError("Missing Code attribute for method");
                }
            }

            // Look for overridden method in super class(es)
            if (!method.isStatic() && methodName != vm.INIT) {

                // Look for this method in the supertype and set the 'overridden' attribute
                // of the method is a supermethod is found.
                Method superMethod = null;
                if (!type.isInterface() && type.superClass() != null) {
                    superMethod = type.superClass().lookupMethod(methodName, method.getParms(), method.type(), type, false);
                }
                if (superMethod != null) {
                    method.overrides(superMethod);
                }
            }
            else {
                staticMethodCount++;
            }

            // Verify that there are no duplicate methods.
            for (Enumeration e = methods.elements(); e.hasMoreElements();) {
                Method m = (Method)e.nextElement();
                if (m.name() == methodName &&
                    m.getParms() == methodSigTypes.parmTypes &&
                    m.type() == methodSigTypes.returnType) {
                    throw in.classFormatError("Duplicate method found: "+m);
                }
            }

            // Tracing
            if (vm.traceclassinfo(method.toString())) {
                String staticStr = ((method.isStatic()) ? "static " : "");
                vm.tracer().traceln(vm.traceclassinfo(method.toString()), "    Method:"+staticStr+method);
            }

            methods.addElement(method);
        }

        // Return now if this is a primitive class as methods of primitive classes will not
        // verify - they are both Objects and primitives which will confuse the type verifier.
        if (type.isPrimitive()) {
            return;
        }

        // Partition the methods into the static and virtual method tables.
        int virtualMethodCount = (count - staticMethodCount);
        Method[] virtualMethods = new Method[virtualMethodCount];
        Method[] staticMethods  = new Method[staticMethodCount];
        int staticIndex  = 0;
        int virtualIndex = 0;
        for (Enumeration e = methods.elements(); e.hasMoreElements();) {
            Method method = (Method)e.nextElement();
            if (method.isStatic() || method.name() == vm.INIT) {
                staticMethods[staticIndex++] = method;
            }
            else {
                virtualMethods[virtualIndex++] = method;
            }
        }
        type.setMethods(staticMethods, true);
        type.setMethods(virtualMethods, false);
    }

    /**
     * Load the method's code.
     * @param in
     * @param pool
     * @param method
     * @param attributeLengthXX
     * @throws IOException
     * @throws LinkageException
     */
    private void loadMethodCode(ClassFileInputStream in, ConstantPool pool, Method method, int attributeLengthXX) throws IOException, LinkageException {
        int maxStack   = in.readUnsignedShort("cod-maxStack");  // Maximum stack need
        int maxLocals  = in.readUnsignedShort("cod-maxLocals"); // Max locals need
        int codeLength = in.readInt("cod-length");              // Length of the bytecode array
        StackMap   map = null;

        if (codeLength <= 0) {
            throw in.classFormatError("the value of code_length must be greater than 0");
        } else if (codeLength >= 0x7FFF) {
            throw in.classFormatError("Method code longer than 32Kb");
        }

        // Read the bytecodes into a buffer. The GraphBuilder needs to know
        // about the exception handlers and stack maps which come after the
        // bytecodes.
        byte[] bytecodes = new byte[codeLength];
        in.readFully(bytecodes);

        // Read in the exception handlers
        ExceptionHandler[] handlers = new ExceptionHandler[in.readUnsignedShort("hnd-handlers")];
        if (handlers.length > 0) {
            for (int i = 0; i < handlers.length; i++) {
                int startPC    = in.readUnsignedShort("hnd-startPC");               // Code range where handler is valid
                int endPC      = in.readUnsignedShort("hnd-endPC");                 // (as offsets within bytecode)
                int handlerPC  = in.readUnsignedShort("hnd-handlerPC");             // Offset to handler code
                int catchIndex = in.readUnsignedShort("hnd-catchIndex");            // Exception (constant pool index)

                // Check that all the pc addresses look reasionable
                if (startPC >= codeLength ||
                    endPC > codeLength    ||
                    startPC >= endPC      ||
                    handlerPC >= codeLength) {
                    throw in.classFormatError("Bad exception handler found");
                }

                /*
                 * There is a strange case only seen so far in code generated by the javac
                 * in JDK 1.4 for "synchronized" blocks. The following excerpt from the
                 * "Support Readiness Document, Java 2 Standard Edition, Version 1.4, Tools"
                 * explains the change:
                 *
                 * 2.2.2.2 4327029 Erroneous code range in exception table for synchronized statements
                 *   The most notable difference in generated code is in the code generated for the
                 *   synchronized statement. The javac included in J2SE, v. 1.4, includes a new scheme
                 *   for generating exception ranges for try and synchronized statements. The new scheme
                 *   is more correct and prevents multiple executions of the finally clause in the presence
                 *   of asynchronous exceptions and ensures that the monitor is released for a synchronized
                 *   statement.
                 *
                 *   The monitorexit instruction is protected by an exception range whose handler releases
                 *   the monitor with a monitorexit instruction. That handler up to the monitorexit
                 *   instruction is protected by itself. In other words, we now generate an exception range
                 *   whose handler is within that exception range. That is different from the incorrect code
                 *   suggested in the VM specification for compiling the Java synchronized statement, and it
                 *   violates the suggestion in the VM specification, section 4.9.5, that our compiler will
                 *   not generate such code. It is necessary to get the correct semantics for the synchronized
                 *   statement in the presence of asynchronous exceptions.
                 */
                handlers[i] = new ExceptionHandler(startPC, endPC, handlerPC, catchIndex);
            }
        }

        // Read in the code attributes
        int[]  lineNumberTable = null;
        byte[] stackMapData    = null;
        LocalVariableTable localVariableTable = null;
        Liveness liveness = null;
        int attributesCount = in.readUnsignedShort("cod-attributesCount");
        for (int i = 0; i < attributesCount; i++) {
            int attributeNameIndex = in.readUnsignedShort("cod-attributeNameIndex");
            int attributeLength    = in.readInt("cod-attributeLength");
            String attributeName   = pool.getUtf8(attributeNameIndex);
            if (attributeName.equals("StackMap")) {
                stackMapData = new byte[attributeLength];
                in.readFully(stackMapData);
            }
            else if(attributeName.equals("LineNumberTable")) {
                // The LineNumberTable is encoded as a serialized array of pairs: entry N gives
                // the next start_pc value and entry N+1 gives the corresponding line_number value.
                int lineNumberTableLength = in.readUnsignedShort("lin-lineNumberTableLength") * 2;
                lineNumberTable = new int[lineNumberTableLength];
                for (int k = 0 ; k < lineNumberTableLength ; ) {
                    int start_pc = in.readUnsignedShort("lnt-startPC");
                    if (start_pc >= codeLength) {
                        throw in.classFormatError("start_pc of LineNumberTable is out of range");
                    }
                    lineNumberTable[k++] = start_pc;
                    lineNumberTable[k++] = in.readUnsignedShort("lnt-lineNumber");
                }
            }
            else if(attributeName.equals("LocalVariableTable")) {
                localVariableTable = new LocalVariableTable(in, pool, codeLength);
            }
            else if (attributeName.equals("Liveness")) {
                liveness = new Liveness(in, method, maxLocals);
            }
            else {
                in.tracer.traceln("ignored attributeName="+attributeName);
                while (attributeLength-- > 0) {
                    in.readByte(null); // Ignore this attribute
                }
            }

        }

        BytecodeHolder holder = new BytecodeHolder(method,
                pool,
                bytecodes,
                maxStack,
                maxLocals,
                handlers,
                stackMapData,
                lineNumberTable,
                localVariableTable,
                liveness);
        method.setHolder(holder);
    }

    /**
     * Load the class's other attributes.
     * @param in
     * @param pool
     * @param type
     * @throws IOException
     * @throws LinkageException
     */
    private void loadExtraAttributes(ClassFileInputStream in, ConstantPool pool, Type type) throws IOException, LinkageException {
        int attributesCount = in.readUnsignedShort("ex-count");
        for (int i = 0; i < attributesCount; i++) {
            int attributeNameIndex = in.readUnsignedShort("ex-index");
            String attributeName   = pool.getUtf8(attributeNameIndex);
            int attributeLength    = in.readInt("ex-length");
            if(attributeName.equals("SourceFile")) {
                int index = in.readUnsignedShort("sourcefile-index");
                type.setSourceFile(pool.getUtf8(index));
            } else {
                in.tracer.traceln("ignored attributeName="+attributeName);
                while (attributeLength-- > 0) {
                    in.readByte(null); // Ignore this attribute
                }
            }
        }
    }

    /**
     * Return the ClasspathConnection used by this loader.
     * @return the ClasspathConnection used by this loader.
     */
    public ClasspathConnection getClassPath() {
        return classPath;
    }

    /**
     * Set the ClasspathConnection used by this loader.
     * @param classPath
     */
    public void setClassPath(ClasspathConnection classPath) {
        this.classPath = classPath;
    }
}
