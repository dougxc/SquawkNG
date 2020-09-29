package com.sun.squawk.translator.suite.impl;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.suite.SuiteLoader;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.translator.loader.ClassFileInputStream;
import com.sun.squawk.translator.util.JVMConst;
import com.sun.squawk.vm.CNO;
import com.sun.squawk.vm.SquawkConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import javax.microedition.io.*;

import com.sun.squawk.suite.*;

/**
 * This is the suite loader used by the translator. This loader only handles
 * loading of suites for the purpose of binding against them. That is, it only
 * loads a single suite from a given file and it does not load the
 * MethodImplementations_component of a suite.
 */
public class BinarySuiteLoader implements SuiteLoader {

    /** The Translator context during a load. */
    Translator vm;

/*-----------------------------------------------------------------------*\
 *               Containers for suite file components                    *
\*-----------------------------------------------------------------------*/

    /**
     * This class encapsulates a suite as expressed in a suite file.
     */
    public static class Suite {

        /**
         * Construct a new container for a suite.
         * @param vm
         * @param name
         * @param accessFlags
         * @param bindsList
         * @param maxType
         */
        Suite(Translator vm,
              String name,
              int accessFlags,
              String[] bindsList,
              int maxType) {
            this.vm = vm;
            this.name = name;
            this.accessFlags = accessFlags;
            this.bindsList = bindsList;
            this.types = new SuiteType[maxType + 1];
        }

        /** The Translator context. */
        private final Translator vm;
        /** The name of the suite. */
        private final String name;
        /** The access flags of the suite. */
        private final int accessFlags;
        /** The list of suites this suite binds to. */
        private final String[] bindsList;
        /** The classes within the suite. */
        private final SuiteType[] types;

        /**
         * Return the name of the suite.
         * @return the name of the suite.
         */
        public String getName() {
            return name;
        }

        /**
         * Get the suite representation of a class corresponding to a
         * specified suite-relative class index.
         * @param index the index of the requested class within the suite.
         * @return the suite representation of the requested class
         */
        SuiteType getSuiteType(int index) {
            Assert.that(index < types.length && index > 0);
            return types[index];
        }

        SuiteType lookupSuiteType(String name) {
            for (int i = 1; i != types.length; ++i) {
                SuiteType type = types[i];
                if (type != null && type.name.equals(name)) {
                    return type;
                }
            }
            return null;
        }

        /**
         * Insert a suite representation of a class into the class
         * table for this suite.
         * @param index The suite-relative index of the class.
         * @param suiteType The class to be inserted.
         */
        void addSuiteType(int index, SuiteType suiteType) {
            Assert.that(index < types.length && index > 0);
            Assert.that(index <= CNO.LAST_IMPLICIT_TYPE || types[index] == null);
            types[index] = suiteType;
        }

        /**
         * Return the internal class structure corresponding to a suite-relative
         * class index.
         * @param index The suite-relative class index.
         * @return The internal class structure corresponding to 'index'.
         */
        Type getType(int index) {
            SuiteType info = getSuiteType(index);
            if (info == null) {
                String[] implicitNames = CNO.IMPLICIT_PROXY_NAMES;
                if (index < implicitNames.length) {
                    String name = convertSuiteNameToInternalName(implicitNames[
                        index]);
                    int accessFlags = SquawkConstants.ACC_PROXY |
                        SquawkConstants.ACC_PUBLIC;
                    info = new SuiteType(index, 1, name, accessFlags);
                    types[index] = info;
                }
                else {
                    Assert.shouldNotReachHere();
                }
            }
            return vm.findOrCreateBaseType(info.name);
        }

        /**
         * Resolve all the suite classes into corresponding internal classes.
         */
        void resolve() {
            for (int i = 1; i != types.length; ++i) {
                SuiteType type = types[i];
                if (type != null) {
                    type.resolve(vm, this);
                }
            }
        }

        /**
         * Dump the suite to a PrintStream.
         * @param excludeProxies
         * @param asClassFiles
         * @param out
         */
        void dumpClasses(boolean excludeProxies, boolean asClassFiles,
                         PrintStream out) {
            for (int i = 0; i != types.length; ++i) {
                SuiteType info = types[i];
                if (info != null) {
                    if (!info.isProxy() || !excludeProxies) {
                        String name = info.name;
                        if (asClassFiles) {
                            if (name.charAt(0) != '[') {
                                name = name.substring(1, name.length() - 1);
                                out.println(name.replace('.', '/') + ".class");
                            }
                        }
                        else {
                            out.println(name);
                        }
                    }
                }

            }
        }
    }

    /**
     * This class encapsulates a class as expressed in a suite file.
     */
    public static class SuiteType {
        SuiteType(int index, int superIndex, String name, int accessFlags) {
            this.index = index;
            this.superIndex = superIndex;
            this.accessFlags = accessFlags;
            this.name = name;
        }

        final int index;
        final int superIndex;
        final int accessFlags;
        final String name;

        private int maxVtableIndex = -1;

        private SuiteMethod[] staticMethods;
        private SuiteMethod[] virtualMethods;
        private SuiteMethod[] overridingVirtualMethods;
        private SuiteField[] staticFields;
        private SuiteField[] instanceFields;
        private int[] interfaces;

        private Type resolved;

        boolean isProxy() {
            return (accessFlags & SquawkConstants.ACC_PROXY) != 0;
        }

        /**
         * Get the super type (in terms of the suite) for this suite type.
         * @param suite
         * @return
         */
        SuiteType getSuperClass(Suite suite) {
            if (superIndex != 0) {
                return suite.getSuiteType(superIndex);
            } else {
                if (isProxy()) {
                    Type type = suite.vm.findOrCreateBaseType(name);
                    Assert.that(type.getState() == Type.CONVERTED);
                    Type superClass = type.superClass();
                    while (superClass != null) {
                        SuiteType suiteSuperClass = suite.lookupSuiteType(superClass.name());
                        if (suiteSuperClass != null) {
                            return suiteSuperClass;
                        }
                        superClass = superClass.superClass();
                    }
                }
                return null;
            }
        }

        int getMaxVtableIndex() {
            return maxVtableIndex;
        }

        /**
         * Resolve this suite class to an internal class.
         * @param vm
         * @param suite
         * @return
         */
        public Type resolve(Translator vm, Suite suite) {
            if (resolved == null) {
                Type type = vm.findOrCreateBaseType(name);
//                Assert.that(type.isProxy() == isProxy());

                if (type.getState() == Type.DEFINED) {

                    // Set the access flags
                    type.setFlags((accessFlags & JVMConst.VALID_CLASS_FLAGS_MASK) |
                                  JVMConst.ACC_PROXY);

                    // resolve super class
                    if (superIndex != 0) {
                        Type superClass = suite.getType(superIndex);
                        if (type != vm.BYTE &&
                            type != vm.BOOLEAN &&
                            type != vm.BYTE_ARRAY &&
                            type != vm.BOOLEAN_ARRAY) {
                            type.setSuperType(superClass);
                        }
                    }

                    // resolve interfaces
                    if (interfaces != null) {
                        Type[] ifaces = new Type[interfaces.length];
                        for (int i = 0; i != ifaces.length; ++i) {
                            ifaces[i] = suite.getType(interfaces[i]);
                        }
                        type.setInterfaces(ifaces, 0);
                    }

                    resolveFields(suite, type, true, staticFields);
                    resolveFields(suite, type, false, instanceFields);

                    resolveStaticMethods(suite, type);
                    resolveVirtualMethods(suite, type);

                    type.setState(Type.CONVERTED);
                    type.vm().typeLoaded(type);
                }
                else {
                    // Could do some verification here to ensure that proxy
                    // types match those already in the system
                    Assert.that(type.getState() == Type.CONVERTED);
                }
                resolved = type;
            }
            return resolved;
        }

        /**
         * Set the type's static or virtual method table.
         * @param methods The linkable methods of this type.
         * @param count The number of methods. The length of the 'methods' array
         * may be longer than this number.
         * @param isStatic True if setting the static methods.
         */
        void setMethods(SuiteMethod[] methods, int count, boolean isStatic) {
            if (count > 0) {
                if (count < methods.length) {
                    Object old = methods;
                    methods = new SuiteMethod[count];
                    System.arraycopy(old, 0, methods, 0, count);
                }
                if (isStatic) {
                    staticMethods = methods;
                } else {
                    virtualMethods = methods;
                    maxVtableIndex = methods[methods.length - 1].getVtableIndex();
                }
            }
        }

        /**
         * Set the type's virtual methods that override a method in a superclass
         * with different access flags.
         * @param methods The linkable overriding methods of this type.
         */
        void setOverridingMethods(SuiteMethod[] methods) {
            overridingVirtualMethods = methods;
        }

        /**
         * Set the type's static or instance field table.
         * @param fields The linkable fields of this type.
         * @param count The number of fields. The length of the 'fields' array
         * may be longer than this number.
         * @param isStatic True if setting the static fields.
         */
        void setFields(SuiteField[] fields, int count, boolean isStatic) {
            if (count > 0) {
                if (count < fields.length) {
                    Object old = fields;
                    fields = new SuiteField[count];
                    System.arraycopy(old, 0, fields, 0, count);
                }
                if (isStatic) {
                    staticFields = fields;
                } else {
                    instanceFields = fields;
                }
            }
        }

        /**
         * Set the interfaces implemented by this suite class.
         * @param interfaces
         */
        void setInterfaces(int[] interfaces) {
            this.interfaces = interfaces;
        }

        /**
         * Get the suite method in the class or its superclasses corresponding
         * to a given vtable index.
         * @param vtableIndex
         * @return
         */
        SuiteMethod lookupMethod(Suite suite, int vtableIndex) {
            for (int i = 0; i != virtualMethods.length; ++i) {
                SuiteMethod method = virtualMethods[i];
                if (method.getVtableIndex() == vtableIndex) {
                    return method;
                }
            }

            if (overridingVirtualMethods != null) {
                for (int i = 0; i != overridingVirtualMethods.length; ++i) {
                    SuiteMethod method = overridingVirtualMethods[i];
                    if (method.getVtableIndex() == vtableIndex) {
                        return method;
                    }
                }
            }

            SuiteType superClass = getSuperClass(suite);
            if (superClass != null) {
                return superClass.lookupMethod(suite, vtableIndex);
            }

            return null;
        }

        /**
         * Resolve the static methods for a class.
         * @param suite
         * @param type
         * @param isStatic
         * @param suiteMethods
         */
        void resolveStaticMethods(Suite suite, Type type) {
            if (staticMethods != null) {
                Method[] methods = new Method[staticMethods.length];
                for (int i = 0; i != staticMethods.length; ++i) {
                    methods[i] = staticMethods[i].resolve(suite, type);
                }
                type.setMethods(methods, true);
            }
        }

        /**
         * Resolve the virtual methods for a class.
         *
         * @param suite
         * @param type
         * @param isStatic
         * @param suiteMethods
         */
        void resolveVirtualMethods(Suite suite, Type type) {
            int overridden = (overridingVirtualMethods == null ? 0 : overridingVirtualMethods.length);
            int total =  overridden + (virtualMethods == null ? 0 : virtualMethods.length);
            if (total != 0) {
                Method[] methods = new Method[total];

                if (overridden != 0) {
                    for (int i = 0; i != overridden; ++i) {
                        Method method = overridingVirtualMethods[i].resolve(suite, type);
                        method.overrides(type.superClass().lookupMethod(method.name(), method.getParms(), method.type(), type, false));
                        methods[i] = method;
                    }
                }

                if (virtualMethods != null) {
                    for (int i = 0; i != virtualMethods.length; ++i) {
                        methods[i + overridden] = virtualMethods[i].resolve(suite, type);
                    }
                }

                type.setMethods(methods, false);
            }
        }

        /**
         * Resolve the static or instance fields for a class.
         * @param suite
         * @param type
         * @param isStatic
         * @param SuiteFields
         */
        void resolveFields(Suite suite,
                           Type type,
                           boolean isStatic,
                           SuiteField[] SuiteFields) {
            if (SuiteFields != null) {
                Field[] fields = new Field[SuiteFields.length];
                for (int i = 0; i != SuiteFields.length; ++i) {
                    fields[i] = SuiteFields[i].resolve(type.vm(), suite, type);
                }
                type.setFields(fields, isStatic);
            }
        }

    }

    /**
     * This class encapsulates a method as expressed in a suite file.
     */
    public static class SuiteMethod
        {
        SuiteMethod(int accessFlags, String name, int type, int[] parms, int vtableIndex) {
            this.accessFlags = accessFlags;
            this.name = name;
            this.type = type;
            this.parms = parms;
            this.vtableIndex = vtableIndex;
        }

        Method resolve(Suite suite, Type parent) {
            Translator vm = parent.vm();
            Type[] parmTypes = new Type[parms.length];
            int actualCount = 0;
            for (int i = 0; i != parmTypes.length; ++i) {
                Type parmType = suite.getType(parms[i]);
                parmTypes[i] = suite.getType(parms[i]);
                if (parmType == vm.LONG) {
                    ++i;
                    Assert.that(suite.getType(parms[i]) == vm.LONG2);
                }
/*if[FLOATS]*/
                else if (parmType == vm.DOUBLE) {
                    ++i;
                    Assert.that(suite.getType(parms[i]) == vm.DOUBLE2);
                }
/*end[FLOATS]*/
                parmTypes[actualCount++] = parmType;
            }
            if (actualCount != parmTypes.length) {
                Object old = parmTypes;
                parmTypes = new Type[actualCount];
                System.arraycopy(old, 0, parmTypes, 0, actualCount);
            }
            return Method.create(parent, vm.internString(name), accessFlags,
                                 suite.getType(type),
                                 vm.internTypeList(parmTypes));
        }

        int getVtableIndex() {
            return vtableIndex;
        }

        private final int accessFlags;
        final String name;
        final int type;
        final int[] parms;
        private final int vtableIndex;
    }

    /**
     * This class encapsulates a field as expressed in a suite file.
     */
    public static class SuiteField {
        SuiteField(int accessFlags, String name, int type) {
            this.accessFlags = accessFlags;
            this.name = name;
            this.type = type;
        }

        Field resolve(Translator vm, Suite suite, Type parent) {
            return Field.create(vm, parent, vm.internString(name),
                                suite.getType(type), accessFlags);
        }

        private final int accessFlags;
        private final String name;
        private final int type;
    }

/*-----------------------------------------------------------------------*\
 *                        Binary suite parsing                              *
\*-----------------------------------------------------------------------*/

    /**
     * Load a suite from a specified file.
     * @param fileName
     * @return
     * @throws LinkageException
     */
    public String loadSuite(Translator vm, String fileName) throws LinkageException {
        InputStream in = null;
        this.vm = vm;
        try {
            in = Connector.openInputStream("file://"+fileName);

            // Wrap the input stream in a ClassFileInputStream
            ClassFileInputStream cfin = new ClassFileInputStream(in, fileName, vm);

            // Set trace if requested
            cfin.setTrace(vm.traceraw(fileName));

            Suite suite = loadSuiteInfo(cfin);
            loadTypesComponent(cfin, suite);
            suite.resolve();
            return suite.getName();
        } catch (IOException ioe) {
            throw new LinkageException(vm.LINKAGEERROR, "Error opening/reading suite from '"+fileName+"': "+ioe.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    /**
     * Load the suite access, name and binds list.
     * @param in
     * @throws IOException
     * @throws LinkageException
     */
    private Suite loadSuiteInfo(ClassFileInputStream in) throws IOException, LinkageException {
        int magic   = in.readInt("ste-magic");
        if (magic != 0xCAFEFACE) {
            throw new LinkageException(vm.CLASSFORMATERROR, "Invalid magic number: 0x"+Integer.toHexString(magic));
        }

        in.readUnsignedShort("ste-minor_version");
        in.readUnsignedShort("ste-major_version");

        int access  = in.readByte("ste-acc");
        String name = in.readUTF("ste-name");
        int maxType = in.readUnsignedShort("ste-maxType");
        String[] bindsList = new String[in.readUnsignedShort("ste-bndLength")];
        for (int i = 0; i != bindsList.length; ++i) {
            bindsList[i] = in.readUTF("ste-bndElement");
        }
        return new Suite(vm, name, access, bindsList, maxType);
    }

    /**
     * Load the Types component.
     * @param in
     * @return
     * @throws IOException
     * @throws LinkageException
     */
    private void loadTypesComponent(ClassFileInputStream in, Suite suite) throws LinkageException {
        int typesCount = in.readUnsignedShort("tyc-count");
        for (int i = 0; i != typesCount; ++i) {
            loadType(in, suite);
        }
    }

    /**
     * convertSuiteNameToInternalName
     * @param name
     * @return
     */
    static String convertSuiteNameToInternalName(String name) {
        int dims = 0;
        while (name.charAt(dims) == '[') {
            dims++;
        }
        Assert.that(name.charAt(name.length()-1) != ';');
        name = "L"+name.substring(dims).replace('.', '/')+";";
        while (dims-- != 0) {
            name = '['+name;
        }
        return name;
    }

    /**
     * Load a single Type element.
     * @param in
     * @param suite
     * @throws IOException
     * @throws LinkageException
     */
    private void loadType(ClassFileInputStream in, Suite suite) throws LinkageException {
        String name     = in.readUTF("typ-name");
        int accessFlags = in.readUnsignedShort("typ-flags") &
                         (JVMConst.VALID_CLASS_FLAGS_MASK | JVMConst.ACC_PROXY);
        int thisIndex   = in.readUnsignedShort("typ-index");
        int superIndex  = in.readUnsignedShort("typ-super index");

        name = convertSuiteNameToInternalName(name);

        SuiteType suiteType = new SuiteType(thisIndex, superIndex, name, accessFlags);
        suite.addSuiteType(thisIndex, suiteType);

        loadInterfaces(in, suiteType);
        loadFields(in, suiteType, true);
        loadFields(in, suiteType, false);

        loadMethods(in, suiteType, null, true);
        loadMethods(in, suiteType, suiteType.getSuperClass(suite), false);

        // load the Overriding info
        loadOverriding(in, suite, suiteType);

        // load and ignore the class refs
        int classRefsCount = in.readUnsignedShort("clr-count");
        for (int i = 0; i != classRefsCount; ++i) {
            in.readUnsignedShort("clr-class");
        }

        // load and ignore the objects table
        int objectsCount = in.readUnsignedShort("obj-count");
        for (int i = 0; i != objectsCount; ++i) {
            int tag = in.readUnsignedByte("obj-tag");
            switch (tag) {
                case SquawkConstants.CONSTANT_String: {
                    in.readUTF("obj-string");
                    break;
                }
                case SquawkConstants.CONSTANT_Byte_array: {
                    int length = in.readUnsignedShort("obj-byteArrayLength");
                    for (int j = 0; j != length; ++j) {
                        in.readUnsignedByte("obj-byteArrayElement");
                    }
                    break;
                }
                case SquawkConstants.CONSTANT_Short_array:
                case SquawkConstants.CONSTANT_Char_array: {
                    int length = in.readUnsignedShort("obj-charOrShortArrayLength");
                    for (int j = 0; j != length; ++j) {
                        in.readUnsignedShort("obj-charOrShortArrayElement");
                    }
                    break;
                }
                case SquawkConstants.CONSTANT_Int_array:
                case SquawkConstants.CONSTANT_Float_array: {
                    int length = in.readUnsignedShort("obj-intOrFloatArrayLength");
                    for (int j = 0; j != length; ++j) {
                        in.readInt("obj-intOrFloatArrayElement");
                    }
                    break;
                }
                case SquawkConstants.CONSTANT_Double_array:
                case SquawkConstants.CONSTANT_Long_array: {
                    int length = in.readUnsignedShort("obj-doubleOrLongArrayLength");
                    for (int j = 0; j != length; ++j) {
                        in.readInt("obj-doubleOrLongArrayElement");
                    }
                    break;
                }
                default:
                    Assert.shouldNotReachHere();
            }
        }

        // load and ignore attributes
        loadExtraAttributes(in);
    }

    /**
     * Load the class's interfaces.
     * @param in
     * @param suite
     * @param type
     * @throws IOException
     * @throws LinkageException
     */
    private void loadInterfaces(ClassFileInputStream in, SuiteType suiteType) throws LinkageException {
        int count = in.readUnsignedShort("i/f-count");
        int[] interfaces = new int[count];
        for (int i = 0; i != count; ++i) {
            int ifaceType = in.readUnsignedShort("i/f-type");
            int implMethodsCount = in.readUnsignedShort("i/f-implMethodsCount");

            // ignore impl methods
            for (int j = 0; j != implMethodsCount; ++j) {
                in.readUnsignedShort("i/f-implMethod");
            }

            interfaces[i] = ifaceType;
        }
        suiteType.setInterfaces(interfaces);
    }


    /**
     * loadFields
     * @param in
     * @param suite
     * @param type
     * @param isStatic
     * @throws LinkageException
     */
    private void loadFields(ClassFileInputStream in, SuiteType suiteType, boolean isStatic) throws LinkageException {
        int count = in.readUnsignedShort("fld-count");
        SuiteField[] fields = new SuiteField[count];
        int actualCount = 0;
        for (int i = 0; i != count; ++i) {
            int accessFlags = in.readUnsignedShort("fld-acc");
            int fieldType = in.readUnsignedShort("fld-type");
            if ((accessFlags & SquawkConstants.ACC_SYMBOLIC) != 0) {
                String name = in.readUTF("fld-name");
                if (isStatic) {
                    accessFlags |= JVMConst.ACC_STATIC;
                }
                fields[actualCount++] = new SuiteField(accessFlags & JVMConst.VALID_FIELD_FLAGS_MASK, name, fieldType);
            }
        }
        suiteType.setFields(fields, actualCount, isStatic);
    }

    /**
     * loadMethods
     * @param in
     * @param suite
     * @param type
     * @param isStatic
     * @throws LinkageException
     */
    private void loadMethods(ClassFileInputStream in, SuiteType suiteType, SuiteType superSuiteType, boolean isStatic) throws LinkageException {
        int count = in.readUnsignedShort("mth-count");
        SuiteMethod[] methods = new SuiteMethod[count];
        int actualCount = 0;
        int vtableIndex = superSuiteType == null ? 0 : superSuiteType.getMaxVtableIndex() + 1;
        for (int i = 0; i != count; ++i) {
            int accessFlags = in.readUnsignedShort("mth-acc");
            int methodType  = in.readUnsignedShort("mth-type");
            int parmCount   = in.readUnsignedShort("mth-parmCount");
            boolean isInit  = (accessFlags & SquawkConstants.ACC_INIT) != 0;

            // Make receiver of virtual methods and <init> implicit
            if (!isStatic || isInit) {
                Assert.that(parmCount > 0);
                parmCount--;
                in.readUnsignedShort("mth-rcvrParm");
            }

            int[] parms = new int[parmCount];
            for (int j = 0; j != parmCount; ++j) {
                parms[j] = in.readUnsignedShort("mth-parm");
            }
            if ((accessFlags & SquawkConstants.ACC_SYMBOLIC) != 0) {
                String name = in.readUTF("mth-name");
                if (isStatic && !isInit) {
                    accessFlags |= JVMConst.ACC_STATIC;
                }
                methods[actualCount++] = new SuiteMethod(accessFlags & JVMConst.VALID_METHOD_FLAGS_MASK, name, methodType, parms, vtableIndex);
            }

            vtableIndex++;

        }
        suiteType.setMethods(methods, actualCount, isStatic);
    }

    /**
     * Load the methods in the 'overriding' section of the suite.
     * @param in
     * @param suite
     * @param suiteType
     * @throws LinkageException
     */
    private void loadOverriding(ClassFileInputStream in, Suite suite, SuiteType suiteType) throws LinkageException {
        int overridingCount = in.readUnsignedShort("ovr-count");
        if (overridingCount != 0) {
            SuiteMethod[] methods = new SuiteMethod[overridingCount];
            for (int i = 0; i != overridingCount; ++i) {
                int vindex = in.readUnsignedShort("ovr-vindex");
                int accessFlags = in.readUnsignedShort("ovr-acc");

                SuiteMethod overridden = suiteType.getSuperClass(suite).lookupMethod(suite, vindex);
                Assert.that(overridden != null);
                SuiteMethod method = new SuiteMethod(accessFlags, overridden.name, overridden.type, overridden.parms, vindex);
                methods[i] = method;
            }
            suiteType.setOverridingMethods(methods);
        }


    }

    /**
     * Load the class's other attributes.
     * @param in
     * @param pool
     * @param type
     * @throws IOException
     * @throws LinkageException
     */
    private void loadExtraAttributes(ClassFileInputStream in) throws LinkageException {
        int attributesCount = in.readUnsignedShort("att-count");
        for (int i = 0; i < attributesCount; i++) {
            String attributeName   = in.readUTF("att-name");
            int attributeLength    = in.readInt("att-length");
            while (attributeLength-- > 0) {
                in.readByte(null); // Ignore this attribute
            }
        }
    }
}