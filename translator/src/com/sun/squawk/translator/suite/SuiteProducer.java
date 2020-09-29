package com.sun.squawk.translator.suite;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import com.sun.squawk.translator.Assert;
import com.sun.squawk.translator.util.Arrays;
import com.sun.squawk.translator.util.ArrayHashtable;
import com.sun.squawk.translator.util.Comparer;
import com.sun.squawk.translator.util.CountedObject;
import com.sun.squawk.translator.loader.TypeProxy;
import com.sun.squawk.translator.ir.*;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.JVMConst;
import com.sun.squawk.vm.SquawkConstants;

/**
 * This class is used to organize a group of classes into a suite.
 * It can interact with a SuiteEmitter to produce a physical
 * representation of a suite.
 */
public class SuiteProducer {

    /**
     * This class can be used to enforce limits on the suite files
     * emmitted by the SuiteProducer. The constraints imposed by this
     * class represent those imposed by the Suite File Format.
     * Subclasses are at liberty to modify these constraints.
     */
    public static abstract class SuiteLimits {
        /** String representation. */
        private final String name;
        public SuiteLimits(String name) {
            this.name = name;
        }
        /** Return string representation. */
        public String toString() {
            return name;
        }

        protected void check(boolean test, String msg) {
            if (!test) {
                throw new SuiteFormatError(this + ": " + msg);
            }
        }

        /** Enforce the limit on the number of classes in a suite. */
        public abstract void checkNumSuiteClasses(int num);
        /** Enforce the limit on the number of interfaces implemented by a class. */
        public abstract void checkNumImplementedInterfaces(int num, Type type);
        /** Enforce the limit on the number of static fields in a class. */
        public abstract void checkNumStaticFields(int num, Type type);
        /** Enforce the limit on the number of instance fields in a class's hierarchy. */
        public abstract void checkNumInstanceFields(int num, Type type);
        /** Enforce the limit on the number of static methods in a class. */
        public abstract void checkNumStaticMethods(int num, Type type);
        /** Enforce the limit on the number of virtual methods in a class's hierachy. */
        public abstract void checkNumVirtualMethods(int num, Type type);
        /** Enforce the limit on the number of classes referenced by a class. */
        public abstract void checkNumReferencedClasses(int num, Type type);
        /** Enforce the limit on the number of constant objects in a class. */
        public abstract void checkNumConstantObjects(int num, Type type);
        /** Enforce the limit on the number of local variables in a method. */
        public abstract void checkNumLocalVariables(int num, Method method);
        /** Enforce the limit on the size of a tableswitch instruction. */
        public abstract void checkTableSwitchOffset(boolean is16Bit, int offset);
    }

    /** Constant for JavaCard 3 classfile compatibile suite file format. */
    public static final SuiteLimits JAVACARD_3 = new SuiteLimits("JavaCard 3") {
        public void checkNumSuiteClasses(int num)          { check(num <= 0xFF, "too many classes in a suite: " + num); }
        public void checkNumImplementedInterfaces(int num, Type type) { check(num <= 0xFF, type+": too many implemented interfaces: " + num); }
        public void checkNumStaticFields(int num, Type type)          { check(num <= 0xFF, type+": too many static fields: " + num); }
        public void checkNumInstanceFields(int num, Type type)        { check(num <= 0xFF, type+": too many instance fields: " + num); }
        public void checkNumStaticMethods(int num, Type type)         { check(num <= 0xFF, type+": too many static methods: " + num); }
        public void checkNumVirtualMethods(int num, Type type)        { check(num <= 0xFF, type+": too many virtual methods: " + num); }
        public void checkNumReferencedClasses(int num, Type type)     { check(num <= 0xFFFF, type+": too many referenced classes: " + num); }
        public void checkNumConstantObjects(int num, Type type)       { check(num <= 0xFFFF, type+": too many constant objects: " + num); }
        public void checkNumLocalVariables(int num, Method method)    { check(num <= 0xFF, method+": too many local variables: " + num); }
        public void checkTableSwitchOffset(boolean is16Bit, int offset) { check(offset >= Short.MIN_VALUE && offset <= Short.MAX_VALUE, "tableswitch offset too big: " + offset); }
    };

    /** Constant for complete J2SE Suite File Format classfile compatibile suite file format. */
    public static final SuiteLimits SUITE_FORMAT = new SuiteLimits("J2SE") {
        public void checkNumSuiteClasses(int num)                     { check(num <= 0xFFFF, "too many classes in a suite: " + num); }
        public void checkNumImplementedInterfaces(int num, Type type) { check(num <= 0xFFFF, type+": too many implemented interfaces: " + num); }
        public void checkNumStaticFields(int num, Type type)          { check(num <= 0xFFFF, type+": too many static fields: " + num); }
        public void checkNumInstanceFields(int num, Type type)        { check(num <= 0xFFFF, type+": too many instance fields: " + num); }
        public void checkNumStaticMethods(int num, Type type)         { check(num <= 0xFFFF, type+": too many static methods: " + num); }
        public void checkNumVirtualMethods(int num, Type type)        { check(num <= 0xFFFF, type+": too many virtual methods: " + num); }
        public void checkNumReferencedClasses(int num, Type type)     { check(num <= 0xFFFF, type+": too many referenced classes: " + num); }
        public void checkNumConstantObjects(int num, Type type)       { check(num <= 0xFFFF, type+": too many constant objects: " + num); }
        public void checkNumLocalVariables(int num, Method method)    { check(num <= 0xFFFF, method+": too many local variables: " + num); }
        public void checkTableSwitchOffset(boolean is16Bit, int offset) { check(offset >= Short.MIN_VALUE && offset <= Short.MAX_VALUE, "tableswitch offset too big: " + offset); }
    };


    /** The Translation context. */
    private final Translator vm;
    /** The name of the suite being produced. */
    private final String suiteName;
    /** The non-proxy classes of the suite being produced. */
    private final Type[] suiteClasses;
    /** The proxy classes of the suite being produced. */
    private final Type[] proxyClasses;
    /** The binds to list. */
    private final String[] bindsList;
    /** The SuiteEmitter being used to emit the suite. */
    private SuiteEmitter emitter;
    /** The suite file format limits. */
    SuiteLimits limits;

    /** The table of class names to the class reference -> index table for each class. */
    private Hashtable classReferences_reverseMapping;
    /** The table of class names to the constant pool table for each class. */
    private Hashtable constantPools_reverseMapping;
    /** Misc suite stats. */
    private Statistics stats;

    /**
     * Add a reverse mapping for the class references table of a given class.
     * @param type
     * @param reverseMapping
     */
    private void addClassReferencesReverseMapping(Type type, Hashtable reverseMapping) {
        String key = type.name();
        Assert.that(classReferences_reverseMapping.get(key) == null);
        classReferences_reverseMapping.put(key, reverseMapping);
    }

    /**
     * Add a reverse mapping for the constants table of a given class.
     *
     * @param type
     * @param reverseMapping
     */
    private void addConstantsReverseMapping(Type type, ArrayHashtable reverseMapping) {
        String key = type.name();
        Assert.that(constantPools_reverseMapping.get(key) == null);
        constantPools_reverseMapping.put(key, reverseMapping);
    }

    /**
     * Get the index of a class being referred to in the class constant pool of
     * the referrer.
     *
     * @param referrer
     * @param referee
     * @return the index of referee
     */
    int classReferenceToIndex(Type referrer, Type referee) {
        Hashtable reverseMapping = (Hashtable)classReferences_reverseMapping.get(referrer.name());
        Assert.that(reverseMapping != null);
        Integer index = (Integer)reverseMapping.get(referee.name());
        Assert.that(index != null, "referrer="+referrer+" referee="+referee);
        return index.intValue();
    }

    /**
     * Get the index of a given constant within the constant pool of a given class.
     * @param value
     * @param type
     * @return
     */
    int constantToIndex(Object value, Type type) {
        ArrayHashtable reverseMapping = (ArrayHashtable)constantPools_reverseMapping.get(type.name());
        Assert.that(reverseMapping != null);
        Integer index = (Integer)reverseMapping.get(value);
        Assert.that(index != null);
        return index.intValue();
    }

    /**
     * Get the statistics collected during suite production.
     */
    public Statistics getStatistics() {
        return stats;
    }

    /**
     * Create a new SuiteProducer.
     * @param vm The translator context.
     * @param suiteName The name of the suite.
     * @param suiteClasses The non-proxy classes of the suite.
     * @param proxyClasses The proxy classes of the suite.
     */
    public SuiteProducer(Translator vm,
                         String suiteName,
                         Type[] suiteClasses,
                         Type[] proxyClasses,
                         String[] bindsList) {
        this.vm               = vm;
        this.suiteName        = suiteName;
        this.suiteClasses     = suiteClasses;
        this.proxyClasses     = proxyClasses;
        this.bindsList        = bindsList;
        this.stats            = new Statistics();
        classReferences_reverseMapping = new Hashtable();
        constantPools_reverseMapping   = new Hashtable();
    }

    /**
     * Emit a suite via a given SuiteEmitter.
     *
     * @param emitter The SuiteEmitter to use.
     * @param limits The suite file format limits.
     */
    public void emitSuite(SuiteEmitter emitter, SuiteLimits limits) {
        // First see if whole suite emitting is completely handled by the emitter
        boolean isFinal = vm.isSuiteFinal();
        if (!emitter.emitSuite(suiteName, isFinal, proxyClasses, suiteClasses)) {
            return;
        }

        try {
            this.emitter = emitter;
            this.limits  = limits;

            // Emit the suite header
            emitter.emitSuiteStart(suiteName, isFinal, bindsList);

            // Start emitting the meta info for all classes.
            limits.checkNumSuiteClasses(suiteClasses[suiteClasses.length-1].suiteID());
            int emitableProxyClasses=getEmittableProxyClassesNum(proxyClasses);
            emitter.emitAllClassesMetaInfoStart(emitableProxyClasses + suiteClasses.length);

            // Emit the meta-info for the proxy classes
            emitClassesMetaInfo(proxyClasses, true);

            // Emit the meta-info for the suite classes
            emitClassesMetaInfo(suiteClasses, false);

            // Finish emitting the meta info for all classes.
            emitter.emitAllClassesMetaInfoEnd();

            // Emit the method bodies for the suite classes
            emitMethodBodies(suiteClasses);

            // Emit the suite tail
            emitter.emitSuiteEnd();
        }
        finally {
            this.emitter = null;
        }

    }

    /**
     * @return the number of proxy classes that would actually be emitted
     */
    private int getEmittableProxyClassesNum(Type[] classes){
        int counter=0;
        for (int i = 0; i != classes.length; i++) {
            Type type = classes[i];

            // Omit implicit proxies that have no members (or no referenced members)
            if (!type.isImplicitProxy() || type.hasAtLeastOneIncludedMember()) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * Emit the meta info for a list of classes.
     *
     * @param classes The list of classes.
     * @param isProxy true if this is a list of proxy classes, false otherwise.
     */
    private void emitClassesMetaInfo(Type[] classes, boolean isProxy) {
        if (emitter.emitClassesMetaInfoStart(classes, isProxy)) {
            for (int i = 0; i != classes.length; i++) {
                Type type = classes[i];
                try {

                    // Omit implicit proxies that have no members (or no referenced members)
                    if (type.isImplicitProxy() && !type.hasAtLeastOneIncludedMember()) {
                        emitter.emitComment(" ", "omitted implicity proxy with no (referenced) members: "+type, true);
                        continue;
                    }

                    if (emitter.emitClassMetaInfoStart(type, isProxy, i == classes.length -1)) {

                        SuiteAttribute[] attributes = null;
                        VMAccessedAttribute attribute = type.getVMAccessedAttribute();
                        if (attribute != null) {
                            attributes = new SuiteAttribute[] { attribute };
                        }

                        // Emit class name
                        emitter.emitClassName(type.suiteName());

                        // Emit access flags (not for proxy classes)
                        if (!isProxy) {
                            emitter.emitClassAccessFlags(type.suiteFlags(false, false));
                        }
                        else {
                            emitter.emitClassAccessFlags(SquawkConstants.ACC_PROXY);
                        }

                        // Emit class number
                        Assert.that(type.specID() != -2);
                        emitter.emitClassType(type);


                        // Emit super class (not for proxy classes)
                        if (!isProxy) {
                            Type superClass = type.superClass();
                            if (superClass == vm.BYTE_OR_BOOLEAN) {
                                superClass = vm.PRIMITIVE;
                            }
                            Assert.that(superClass == null || superClass.specID() != -2);
                            emitter.emitClassExtends(superClass);
                        }
                        else {
                            emitter.emitClassExtends(null);
                        }

                        // Emit interfaces implemented (not for proxy classes)
                        if (!isProxy) {
                            emitClassImplements(type);
                        }
                        else {
                            emitter.emitClassImplementsStart(0);
                            emitter.emitClassImplementsEnd();
                        }

                        // Emit static fields
                        emitFields(type, true, isProxy);

                        // Emit instance fields
                        emitFields(type, false, isProxy);

                        // Emit static methods
                        emitMethods(type, true, isProxy);

                        // Emit virtual methods
                        Vector accessOverriders = emitMethods(type, false, isProxy);

                        if (!type.isProxy()) {
                            if (accessOverriders != null) {
                                // Emit <overrides> section
                                int mask = SquawkConstants.ACC_PRIVATE | SquawkConstants.ACC_PUBLIC | SquawkConstants.ACC_PROTECTED;
                                emitter.emitOverriddenAccessStart(accessOverriders.size());
                                for (Enumeration e = accessOverriders.elements(); e.hasMoreElements();) {
                                    Method m = (Method)e.nextElement();
                                    emitter.emitOverriddenAccessMethodStart(m);
                                    emitter.emitOverriddenAccessMethodSlot(m.slot());
                                    emitter.emitOverriddenAccessMethodAccess(m.suiteFlags(false, false) & mask);
                                    emitter.emitOverriddenAccessMethodEnd();
                                }
                                emitter.emitOverriddenAccessEnd();
                            }
                            else {
                                emitter.emitOverriddenAccessStart(0);
                                emitter.emitOverriddenAccessEnd();
                            }
                        }
                        else {
                            emitter.emitOverriddenAccessStart(0);
                            emitter.emitOverriddenAccessEnd();
                        }

                        // Emit class references
                        if (!type.isProxy()) {
                            Hashtable reverseMapping = new Hashtable();
                            emitter.emitClassReferences(buildClassReferences(type, reverseMapping));
                            addClassReferencesReverseMapping(type, reverseMapping);
                        }
                        else {
                            emitter.emitClassReferences(new Type[0]);
                        }

                        // Emit constants
                        if (!type.isProxy()) {
                            ArrayHashtable revMapping = new ArrayHashtable();
                            Object[] constants = buildConstants(type, revMapping);
                            addConstantsReverseMapping(type, revMapping);
                            limits.checkNumConstantObjects(constants.length, type);
                            if (emitter.emitConstantsStart(constants)) {
                                for (int j = 0; j < constants.length; j++) {
                                    if (constants[j] instanceof String) {
                                        emitter.emitStringConstant(j, (String)constants[j]);
                                    }
                                    else if (constants[j] instanceof short[]){
                                        emitter.emitShortArrayConstant(j, (short[])constants[j]);
                                    }
                                    else if (constants[j] instanceof int[]){
                                        emitter.emitIntArrayConstant(j, (int[])constants[j]);
                                    }
                                    else if (constants[j] instanceof byte[]){
                                        emitter.emitByteArrayConstant(j, (byte[])constants[j]);
                                    }
                                    else if (constants[j] instanceof char[]){
                                        emitter.emitCharArrayConstant(j, (char[])constants[j]);
                                    }
                                    else {
                                        Assert.that(false, "Cannot handle constant objects of type: "+constants[j].getClass().getName());
                                    }
                                }
                                emitter.emitConstantsEnd();
                            }
                        }
                        else {
                            if(emitter.emitConstantsStart(new Object[0]))
                                emitter.emitConstantsEnd();
                        }

                        // Emit the class attributes (if any).
                        emitter.emitClassAttributes(type, attributes);

                        // Notify the emitter that the class meta-info has been emitted
                        emitter.emitClassMetaInfoEnd();
                    }
                } catch (AssertionFailed ae) {
                    ae.addContext(type+": ");
                    throw ae;
                }
            }
            emitter.emitClassesMetaInfoEnd();
        }

    }

    /**
     * Emit the interfaces implemented by a given type as well as the mapping
     * from interface method slots to implementation methods slots for non-interface
     * classes.
     * @param type
     */
    private void emitClassImplements(Type type) {
        Type[] interfaces = type.getInterfaces();
        limits.checkNumImplementedInterfaces(interfaces.length, type);
        emitter.emitClassImplementsStart(interfaces.length);
        for (int i = 0; i != interfaces.length; i++) {
            Type iface = interfaces[i];
            emitter.emitInterfaceStart();
            emitter.emitInterfaceType(iface);

            if (!(type.isInterface() || type.isAbstract())) {
                Method[] ifaceMethods = iface.getMethods(false);
                int ifaceMethodSlotCount = ifaceMethods.length;
                Assert.that(ifaceMethodSlotCount == countIncludedMemberSlots(ifaceMethods));
                emitter.emitInterfaceMethodImplementationSlotsStart(ifaceMethodSlotCount);
                for (int j = 0; j != ifaceMethodSlotCount; j++) {
                    Method ifaceMethod = ifaceMethods[j];
                    Assert.that(ifaceMethod.includeInSuite());
                    Method implMethod = type.lookupMethod(ifaceMethod.name(), ifaceMethod.getParms(), ifaceMethod.type(), null, false);
                    if (implMethod == null || !implMethod.isPublic()) {
                        // This is only required to pass-through TCK classes that test
                        // the rules for implementing interfaces. The method in the
                        // 0'th vtable slot will have been converted to simply throw the LinkageError.
                        emitter.emitInterfaceMethodImplementationSlot(0);
                    }
                    else {
                        Assert.that(implMethod.includeInSuite());
                        emitter.emitInterfaceMethodImplementationSlot(implMethod.slot());
                    }
                }
                emitter.emitInterfaceMethodImplementationSlotsEnd();
            }
            else {
                emitter.emitInterfaceMethodImplementationSlotsStart(0);
                emitter.emitInterfaceMethodImplementationSlotsEnd();
            }
            emitter.emitInterfaceEnd();
        }
        emitter.emitClassImplementsEnd();
    }

    /**
     * Count the number of slots occupied by a given set of members.
     * @param members
     * @return
     */
    private int countIncludedMemberSlots(Member[] members) {
        int count = 0;
        for (int i = 0; i != members.length; i++) {
            Member m = members[i];
            if (m.includeInSuite()) {
                count++;
                if (m instanceof Field && m.type().isTwoWords()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Emit the meta info for a table of static or instance fields.
     *
     * @param type
     * @param isStatic Emitting static fields.
     * @param isProxy true if this is a list of fields for a proxy class, false otherwise.
     */
    private void emitFields(Type type,
                            boolean isStatic,
                            boolean isProxy) {

        Field[] fields = type.getFields(isStatic);
        int fieldSlotCount = countIncludedMemberSlots(fields);
        if (emitter.emitFieldsStart(fields, fieldSlotCount, isStatic, isProxy)) {
            int emitted = 0;
            int i = 0;
            int slot = 0;
            while (emitted != fieldSlotCount) {
                Field f = fields[i++];
                Assert.that(f.includeInSuite());
                Assert.that(!f.isPrimitiveConstant());
                slot = f.slot();
                Assert.that(slot != -1);
                if (emitter.emitFieldStart(f, isProxy)) {

                    // The name of a field is included only for proxy classes
                    // (required for linking) or for non-final suites or if
                    // it is reflected upon.
                    boolean hasSymbolicInfo = isProxy || !vm.isSuiteFinal() || vm.isReflectiveField(f);

                    // Emit field access flags
                    emitter.emitFieldAccessFlags(f.suiteFlags(false, hasSymbolicInfo));

                    // Emit field type
                    Type fieldType = f.type();
                    if (fieldType.isTwoWords()) {
                        fieldType = fieldType.firstWordType();
                    }
                    emitter.emitFieldType(fieldType);

                    // Emit field name
                    if (hasSymbolicInfo) {
                        emitter.emitMemberName(f.name());
                    }

                    // Notify the emitter that the method meta-info has been emitted
                    emitter.emitFieldEnd();
                }
                emitted++;
                // Emit the field holding the second word of a double word value
                if (f.type().isTwoWords()) {
                    if (emitter.emitFieldStart(f, isProxy)) {

                        // Emit field access flags
                        emitter.emitFieldAccessFlags(f.suiteFlags(false, false));

                        // Emit field type
                        emitter.emitFieldType(f.type().secondWordType());

                        // Notify the emitter that the method meta-info has been emitted
                        emitter.emitFieldEnd();
                    }
                    emitted++;
                    slot++;
                }

            }
            if (isStatic) {
                limits.checkNumStaticFields(slot, type);
            }
            else {
                limits.checkNumInstanceFields(slot, type);
            }

            emitter.emitFieldsEnd(isStatic);
        }
    }

    /**
     * Emit the meta info for a list of methods.
     *
     * @param type
     * @param isStatic Emitting static methods.
     * @param isProxy true if this is a list of methods for a proxy class, false otherwise.
     * @return a Vector of methods that override a superclass methods and have a different
     * access modifiers than the overridden methods. This will be null is no such methods
     * exist.
     */
    private Vector emitMethods(Type type, boolean isStatic, boolean isProxy) {

        Method[] methods = type.getMethods(isStatic);
        int methodSlotCount = countIncludedMemberSlots(methods);
        Vector accessOverriders = null;
        if (isStatic) {
            limits.checkNumStaticMethods(methodSlotCount, type);
        } else {
            if (methodSlotCount > 0) {
                limits.checkNumVirtualMethods(methods[methodSlotCount - 1].slot(), type);
            }
        }

        // Calculate number of methods that override a superclass's method
        int numOverridden = 0;
        if (!isStatic) {
            for (int i = 0; i != methodSlotCount; i++) {
                Method m = methods[i];
                Assert.that(m.includeInSuite());
                if (m.overridden() != null) {
                    numOverridden++;
                }
            }
        }
        if (emitter.emitMethodsStart(methods, methodSlotCount-numOverridden, isStatic, isProxy)) {
            for (int i = 0; i != methodSlotCount; i++) {
                Method m = methods[i];
                Method overridden = isStatic ? null : m.overridden();
                Assert.that(m.includeInSuite());
                if (overridden == null) {
                    if (emitter.emitMethodStart(m, isProxy)) {

                        // Emit method access flags
                        boolean hasSymbolicInfo = isProxy || !vm.isSuiteFinal() || vm.isReflectiveMethod(m);
                        emitter.emitMethodAccessFlags(m.suiteFlags(m.name() == vm.INIT, hasSymbolicInfo));

                        // Emit method type
                        Type receiver;
                        if (!m.isStatic()) {
                            receiver = m.parent();
                        } else {
                            receiver = null;
                        }
                        Type[] parms = m.getParms();
                        int realParmsSize = 0;
                        for (int j = 0; j != parms.length; j++) {
                            realParmsSize += (parms[j].isTwoWords() ? 2 : 1);
                        }
                        Type[] realParms = new Type[realParmsSize];
                        realParmsSize = 0;
                        for (int j = 0; j != parms.length; j++) {
                            Type parm = parms[j];
                            if (parm.isTwoWords()) {
                                realParms[realParmsSize++] = parm.firstWordType();
                                realParms[realParmsSize++] = parm.secondWordType();
                            }
                            else {
                                realParms[realParmsSize++] = parm;
                            }
                        }
                        emitter.emitMethodType(receiver, realParms, m.type());

                        // Emit method name
                        if (hasSymbolicInfo) {
                            emitter.emitMemberName(m.name());
                        }

                        // Notify the emitter that the method meta-info has been emitted
                        emitter.emitMethodEnd();
                    }
                }
                else {
                    // An overridden method must be included in the suite if the
                    // overriding method is.
                    Assert.that(overridden.includeInSuite());

                    int mask = JVMConst.ACC_PRIVATE | JVMConst.ACC_PUBLIC | JVMConst.ACC_PROTECTED;
                    if ((overridden.flags() & mask) != (m.flags() & mask)) {
                        if (accessOverriders == null) {
                            accessOverriders = new Vector(10);
                        }
                        accessOverriders.addElement(m);
                    }
                }
            }
            emitter.emitMethodsEnd(isStatic);
        }
        return accessOverriders;
    }

    /**
     * Build the class reference table.
     *
     * @param type The class.
     * @param reverseMapping the table into which the reverse mapping (i.e. from
     * each class to its index in the table) is placed.
     */
    private Type[] buildClassReferences(Type type, Hashtable reverseMapping) {
        // Compute a ref count for each class referred to from within this class's bytecode
        Hashtable refCount = new Hashtable();
        Enumeration e = type.methods();
        while (e.hasMoreElements()) {
            Method m = (Method)e.nextElement();
            Assert.that(m != null);
            if (!m.hasIR()) {
                continue;
            }
            InstructionList ir = m.getIR();
            Assert.that(ir != null);
            for (Instruction inst = ir.head(); inst != null; inst = inst.getNext()) {
                Type referencedType = inst.getReferencedType();
                Assert.that(!(referencedType instanceof TypeProxy));
                if (referencedType != null) {
                    CountedObject co = (CountedObject)refCount.get(referencedType);
                    if (co == null) {
                        co = new CountedObject(referencedType);
                    }
                    else {
                        co.inc();
                    }
                    refCount.put(referencedType, co);
                }
            }
        }

        // Sort the referenced types by ref count and then by name of their class
        Type[] referencedTypes;
        boolean empty = refCount.isEmpty();
        Assert.that(empty == (refCount.size() == 0));
        if (!empty) {
            CountedObject[] entries = CountedObject.toArray(refCount.elements(), refCount.size());
            Arrays.sort(entries, new Comparer() {
                public int compare(Object a, Object b) {
                    int result = CountedObject.COMPARER.compare(a, b);
                    if (result == 0) {
                        return ((Type)((CountedObject)a).object).name().compareTo(((Type)((CountedObject)b).object).name());
                    } else {
                        return result;
                    }
                }
            });
            referencedTypes = new Type[entries.length];
            CountedObject.copyInto(entries, referencedTypes, false);
        }
        else {
            referencedTypes = new Type[0];
        }
        // Emit the sorted references
        limits.checkNumReferencedClasses(referencedTypes.length, type);

        // Build the reverse mapping of references to their indexes for this type
        for (int i = 0; i != referencedTypes.length; i++) {
            reverseMapping.put(referencedTypes[i].name(), new Integer(i));
        }
        return referencedTypes;
    }

    /**
     * Build the constants table for a given class.
     *
     * @param type The class.
     * @param reverseMapping the table into which the reverse mapping (i.e. from
     * each constant to its index in the table) is placed.
     */
    private Object[] buildConstants(Type type, ArrayHashtable reverseMapping) {
        // Compute a ref count for each constant referred to from within this class's bytecode
        ArrayHashtable refCount = new ArrayHashtable();
        Enumeration e = type.methods();
        while (e.hasMoreElements()) {
            Method m = (Method)e.nextElement();
            Assert.that(m != null);
            if (!m.hasIR()) {
                continue;
            }
            InstructionList ir = m.getIR();
            Assert.that(ir != null);
            for (Instruction inst = ir.head(); inst != null; inst = inst.getNext()) {
                Object constant = inst.getConstantObject();
                if (constant != null) {
                    CountedObject co = (CountedObject)refCount.get(constant);
                    if (co == null) {
                        co = new CountedObject(constant);
                    }
                    else {
                        co.inc();
                    }
                    refCount.put(constant, co);
                }
            }
        }

        // Sort the constants by ref count and then by name of their class
        Object[] constants;
        boolean empty = refCount.isEmpty();
        if (!empty) {
            CountedObject[] entries = CountedObject.toArray(refCount.elements(), refCount.size());
            Arrays.sort(entries, new Comparer() {
                public int compare(Object a, Object b) {
                    int result = CountedObject.COMPARER.compare(a, b);
                    if (result == 0) {
                        return ((CountedObject)a).object.getClass().getName().compareTo(((CountedObject)b).object.getClass().getName());
                    } else {
                        return result;
                    }
                }
            });
            constants = new Object[entries.length];
            CountedObject.copyInto(entries, constants, false);
        }
        else {
            constants = new Object[0];
        }
        // Emit the sorted references
        limits.checkNumConstantObjects(constants.length, type);

        // Build the reverse mapping of references to their indexes for this type
        for (int i = 0; i != constants.length; i++) {
            reverseMapping.put(constants[i], new Integer(i));
        }

        return constants;
    }

    /**
     * Emit the method bodies for the suite classes.
     *
     * @param classes The suite classes
     */
    private void emitMethodBodies(Type[] classes) {
        int methodsCount = 0;
        for (Enumeration e = new MethodsEnumerator(classes); e.hasMoreElements();) {
            Method m = (Method)e.nextElement();
            Assert.that(!vm.pruneSuite() || m.includeInSuite());
//            if (m.hasIR()) {
                methodsCount++;
//            }
        }

        if (emitter.emitMethodBodiesStart(methodsCount)) {
            for (Enumeration e = new MethodsEnumerator(classes); e.hasMoreElements();) {
                Method method = (Method)e.nextElement();
//                if (!method.hasIR()) {
//                    continue;
//                }
                Type type = method.parent();
                if (emitter.emitMethodBodyStart(method)) {

                    MethodDebugAttribute debugAttribute = null;
                    SuiteAttribute[] attributes = null;
                    if (emitter.isEmittingMethodDebugAttribute()) {
                        String filePath = method.parent().getSourceFilePath();
                        debugAttribute = new MethodDebugAttribute(filePath, method.toString());
                        attributes = new SuiteAttribute[] {debugAttribute};
                    }

                    // Emit the class owning the method
                    emitter.emitMethodClass(method.parent());

                    // Emit the method's index
                    emitter.emitMethodSlot(method.slot());

                    // Emit the method's implementation flags.
                    int flags = method.suiteFlags(method.name() == vm.INIT, false);
                    if (method.name() == vm.INIT) {
                        flags |= SquawkConstants.ACC_STATIC;
                    }
                    if (attributes != null) {
                        flags |= SquawkConstants.ACC_ATTRIBUTES;
                    }
                    emitter.emitMethodImplAccessFlags(flags);

                    if (method.hasIR()) {
                        // Emit the local variable types (in order)
                        InstructionList ir = method.getIR();
                        Local[] locals = ir.getLocals();
                        limits.checkNumLocalVariables(locals.length, method);
                        emitter.emitMethodLocals(locals);

                        // Do the first pass through the bytecode to gather information for
                        // calculating exact offsets for branch instructions and assigning
                        // IP addresses to each instruction.
                        BytecodeProducer bytecodeEmitter = new BytecodeProducer(method, locals, emitter, this, debugAttribute);
                        bytecodeEmitter.emit(true);
                        if (   (!method.getSquawkMetrics().usesLong || vm.statsLong())
/*if[FLOATS]*/
                               && (!method.getSquawkMetrics().usesFloatOrDouble || vm.statsFloatDouble())
/*end[FLOATS]*/
                               )
                        {
                            stats.addTranslatedMethod(method);
                        }

                        // Emit the max stack value
                        Method.CodeMetrics metrics = method.getSquawkMetrics();
                        emitter.emitMethodMaxStack(metrics.maxStack);

                        // Emit the exception handler table now that IP addresses have been fixed.
                        ExceptionHandlerTable handlerTable = ir.getHandlerTable();
                        if (handlerTable != null) {
                            ExceptionHandlerTable.Entry[] entries = handlerTable.entries();
                            emitter.emitExceptionHandlersStart(entries.length);
                            for (int j = 0; j != entries.length; j++) {
                                ExceptionHandlerTable.Entry entry = entries[j];
                                emitter.emitExceptionHandlerStart();
                                emitter.emitExceptionHandlerFrom(entry.tryStart().getRelocIP());
                                emitter.emitExceptionHandlerTo(entry.tryEnd().getRelocIP());
                                emitter.emitExceptionHandlerEntry(entry.handlerEntry().getRelocIP());
                                emitter.emitExceptionHandlerCatchType(entry.catchType());
                                emitter.emitExceptionHandlerEnd();
                            }
                            emitter.emitExceptionHandlersEnd();
                        } else {
                            emitter.emitExceptionHandlersStart(0);
                            emitter.emitExceptionHandlersEnd();
                        }

                        // Do the second pass through the bytecode for actually
                        // emitting the instructions.
                        bytecodeEmitter.emit(false);
                    }

                    // Emit the method body attributes (if any)
                    if (attributes != null) {
                        emitter.emitMethodBodyAttributes(method, attributes);
                    }

                    // Notify the emitter that the method body has been emitted
                    emitter.emitMethodBodyEnd();
                }
            }
            emitter.emitMethodBodiesEnd();
        }
    }

/*---------------------------------------------------------------------------*\
 *                             MethodsEnumerator                             *
\*---------------------------------------------------------------------------*/

}

class MethodsEnumerator implements Enumeration {
    private final Type[] classes;
    private Enumeration methods;
    private int count = 0;

    MethodsEnumerator(Type[] classes) {
        this.classes = classes;
        if (classes.length > 0) {
            methods = classes[0].methods();
            advance();
        }
    }

    private void advance() {
        Assert.that(methods != null);
        while (!methods.hasMoreElements()) {
            count++;
            if (count < classes.length) {
                methods = classes[count].methods();
            }
            else {
                methods = null;
                return;
            }
        }
    }

    public Object nextElement() {
        if (methods != null) {
            Object o = methods.nextElement();
            advance();
            return o;
        }
        throw new NoSuchElementException("MethodsEnumerator");
    }

    public boolean hasMoreElements() {
        Assert.that(methods == null || methods.hasMoreElements());
        return methods != null;
    }
}
