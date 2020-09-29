package com.sun.squawk.translator.suite.impl;

import java.io.*;
import java.util.*;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.Assert;
import com.sun.squawk.translator.ir.*;
import com.sun.squawk.vm.*;

import com.sun.squawk.translator.suite.SuiteEmitter;
import com.sun.squawk.translator.suite.SuiteAttribute;
import com.sun.squawk.translator.suite.VMAccessedAttribute;
import com.sun.squawk.translator.suite.MethodDebugAttribute;

import java.io.IOException;

/**
 * This class implements a SuiteEmitter which emits an binary
 * representation of a suite conformant with the Suite File
 * Format described in the The Squawk System specification.
 */
public class BinarySuiteEmitter implements SuiteEmitter {

    /** The SuiteWriter to use. */
    private SuiteWriter out;
    /** Flags controlling behaviour of emitter. */
    private int flags;

    boolean debug;
    int maxType=0;

    /*---------------------------------------------------------------------------*\
     *           Implementation of initialization methods in SuiteEmitter        *
    \*---------------------------------------------------------------------------*/

    public void init(OutputStream out, int flags, Hashtable properties) {
        Assert.that(this.out == null);

        // Configure the debug variable from the passed properties.
        if (properties != null) {
            String debugProp = (String)properties.get("debug");
            debug = debugProp != null && debugProp.equals("true");
        }

        if(!debug) {
            this.out = new BinarySuiteWriter(out);
        }
        else {
            this.out = new DebugBinarySuiteWriter(out);
        }
        this.flags = flags;
    }

    public void close() throws IOException {
        out.close();
    }

    public boolean isEmittingStackComments() { return false; }
    public boolean isEmittingComments()      { return false; }
    public boolean isEmittingMethodDebugAttribute() {
        return (flags & EMIT_METHOD_DEBUG) != 0;
    }

    /*---------------------------------------------------------------------------*\
     *           Helpers for implementation of emitting methods in SuiteEmitter  *
    \*---------------------------------------------------------------------------*/

    private String comment(Type t) {
        if (out instanceof BinarySuiteWriter) {
            return null;
        }
        return t.toSignature();
    }

    private String comment(Member m) {
        if (out instanceof BinarySuiteWriter) {
            return null;
        }
        int slot = m.slot();
        return (m instanceof Method && ((Method)m).isSynthetic() ? "**synthetic** " : "") +
                       (m.isStatic() ? "static " : "") + m + "  slot="+slot;
    }

    /**
     * Determine if a given number of bits is sufficent to encode a given unsigned value.
     * @param bits
     * @param operand The operand value.
     * @return
     */
    private boolean fits(int bits, long operand) {
        Assert.that(operand >= 0);
        long lo = 0;
        long hi = (1 << bits) - 1;
        return (operand >= lo && operand <= hi);
    }

    /*---------------------------------------------------------------------------*\
     *           Implementation of emitting methods in SuiteEmitter              *
    \*---------------------------------------------------------------------------*/

    /**
     * Optionally emit a comment.
     * @param prefix Optional prefix.
     * @param comment
     */
    public void emitComment(String prefix, String comment, boolean nl) {
        out.writeComment(comment);
    }

    /**
     * Emit a physical representation of a Suite.
     * @param name The name of the suite
     * @param isFinal Indicates if this is a final suite or not.
     * @param proxies The proxy classes of the suite.
     * @param classes The non-proxy classes of the suite.
     * @return true if the suite producer should traverse the subelements of the suite.
     */
    public boolean emitSuite(String name, boolean isFinal, Type[] proxies, Type[] classes) {
        for (int i = 0; i < proxies.length; i++){
            if (proxies[i].suiteID() > maxType){
                maxType = proxies[i].suiteID();
            }
        }
        for (int i = 0; i < classes.length; i++){
            if (classes[i].suiteID() > maxType){
                maxType = classes[i].suiteID();
            }
        }
        return true;
    }

    /**
     * Emit the suite header.
     *
     * @param name The name of the suite
     * @param isFinal Indicates if this is a final suite or not.
     * @param binds The list of suites to which this one is bound.
     */
    public void emitSuiteStart(String name, boolean isFinal, String[] binds) {
        out.start("SuiteFile");

        out.writeUnsignedInt(0xCAFEFACE, "magic");
        out.writeUnsignedShort(0, "minor_version");
        out.writeUnsignedShort(45, "major_version");
        if (isFinal) {
            out.writeUnsignedByte(SquawkConstants.ACC_FINAL, "flags");
        }
        else {
            out.writeUnsignedByte(0, "flags");
        }
        out.writeUTF8(name, "name");
        out.writeType(maxType, "max_type");
        out.writeUnsignedShort(binds.length, "binds_count");
        for (int i = 0; i < binds.length; i++){
            out.writeUTF8(binds[i], "binds");
        }
    }

    public void emitSuiteEnd() {
        out.end("SuiteFile");
    }

    /**
     * Start emitting meta info for all classes (i.e. both proxy and non-proxy classes).
     * @param numClasses The total number of classes about to be emitted.
     */
    public void emitAllClassesMetaInfoStart(int numClasses) {
        out.writeUnsignedShort(numClasses, "type_count");
    }
    public void emitAllClassesMetaInfoEnd() {
    }

    /**
     * Emit the meta-info for a list of classes.
     * @param classes The list of classes.
     * @param boolean isProxy true if classes is a list of proxy classes, false otherwise
     * @return true if the suite producer should traverse the children of this suite element.
     */
    public boolean emitClassesMetaInfoStart(Type[] classes, boolean isProxy) {
        return true;
    }
    public void emitClassesMetaInfoEnd() {
    }

    /**
     * Emit the meta-info for a class.
     * @param type The class.
     * @param boolean isProxy true if classes is a list of proxy classes, false otherwise
     * @param isLast true if this is the last class in the list of classes.
     * @return true if the suite producer should traverse the children of this suite element.
     */
    public boolean emitClassMetaInfoStart(Type type, boolean isProxy, boolean isLast) {
        out.start("Type_info", comment(type));
        return true;
    }

    public void emitClassMetaInfoEnd() {
        out.end("Type_info");
    }

    /**
     * Emit the suite ID for class.
     * @param type The class.
     */
    public void emitClassType(Type type) {
        out.writeType(type.suiteID(), "this_type");
    }
    /**
     * Emit the name of a class.
     * @param name The name of the class.
     */
    public void emitClassName(String name) {
        out.writeUTF8(name, "name");
    }
    /**
     * Emit the access flags for a class.
     * @param accessFlags The class access flags.
     */
    public void emitClassAccessFlags(int accessFlags) {
        out.writeUnsignedShort(accessFlags, "access_flags");
    }

    /**
     * Emit the super class for a class.
     * @param superClassNumber The super class (null if this is Object).
     */
    public void emitClassExtends(Type superClass) {
        if (superClass == null) {
            out.writeType(0, "super_class");
        }
        else {
            out.writeType(superClass.suiteID(), "super_class");
        }
    }
    /**
     * Emit the interfaces implemented by a class.
     * @param numInterfaces The number of interfaces implemented.
     */
    public void    emitClassImplementsStart(int numInterfaces) {
        out.writeUnsignedShort(numInterfaces, "interfaces_count");
        out.start("interfaces");
    }
    public void    emitClassImplementsEnd() {
        out.end("interfaces");
    }

    public void    emitInterfaceStart() {
        out.start("Interface_info");
    }
    public void    emitInterfaceEnd() {
        out.end("Interface_info");
    }
    public void    emitInterfaceType(Type type) {
        out.writeType(type.suiteID(), "type");
    }
    public void emitInterfaceMethodImplementationSlotsStart(int numMethods) {
        out.writeUnsignedShort(numMethods, "implementation_methods_count");
        out.start("implementation_methods");
    }
    public void    emitInterfaceMethodImplementationSlotsEnd() {
        out.end("implementation_methods");
    }
    public void emitInterfaceMethodImplementationSlot(int slot) {
        out.writeUnsignedShort(slot, null);
    }

    /**
     * Emit the name for a method or field.
     * @param member The method or field.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMemberName(String member) {
        out.writeUTF8(member, "name");
    }

    /**
     * Emit the meta info for a list of fields.
     * @param fields The list of fields.
     * @param fieldsCount The number of fields to emit from the start of the list.
     * @param isStatic Emitting static fields.
     * @param isProxy true if this is a list of fields for a proxy class, false otherwise.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitFieldsStart(Field[] fields,
                                   int fieldsCount,
                                   boolean isStatic,
                                   boolean isProxy) {
        out.writeUnsignedShort(fieldsCount, isStatic ? "static_fields_count" : "instance_fields_count");
        out.start(isStatic ? "static_fields" : "instance_fields");
        return true;
    }
    public void emitFieldsEnd(boolean isStatic) {
        out.end(isStatic ? "static_fields" : "instance_fields");
    }
    /**
     * Emit the meta info for a field.
     * @param field The field.
     * @param isProxy true if this is a field of a proxy class, false otherwise.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitFieldStart(Field field, boolean isProxy) {
        out.start("Field_info", comment(field));
        return true;
    }
    public void    emitFieldEnd() {
        out.end("Field_info");
    }

    /**
     * Emit the type of a field.
     * @param type The field's type.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitFieldType(Type type) {
        out.writeType(type.suiteID(), "type");
    }

    /**
     * Emit the access flags for a field.
     * @param accessFlags The field access flags.
     */
    public void emitFieldAccessFlags(int accessFlags) {
        out.writeUnsignedShort(accessFlags, "access_flags");
    }

    /**
     * Emit the meta info for a list of methods.
     * @param methods The list of methods.
     * @param methodsCount The number of methods to emit from the start of the list.
     * @param isStatic if true, then methods are static methods.
     * @param isProxy true if this is a list of methods for a proxy class, false otherwise.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitMethodsStart(Method[] methods,
                                    int methodsCount,
                                    boolean isStatic,
                                    boolean isProxy) {
        out.writeUnsignedShort(methodsCount, isStatic ? "static_methods_count" : "virtual_methods_count");
        out.start(isStatic ? "static_methods" : "virtual_methods");
        return true;
    }
    public void emitMethodsEnd(boolean isStatic) {
        out.end(isStatic ? "static_methods" : "virtual_methods");
    }

    /**
     * Emit the meta info for a method.
     * @param method The method.
     * @param isProxy true if this is a method of a proxy class, false otherwise.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitMethodStart(Method method, boolean isProxy) {
        out.start("Method_info", comment(method));
        return true;
    }
    public void    emitMethodEnd()                                 {
        out.end("Method_info");
    }

    /**
     * Emit the access flags for a method.
     * @param accessFlags The method access flags.
     */
    public void emitMethodAccessFlags(int accessFlags) {
        out.writeUnsignedShort(accessFlags, "access_flags");
    }
    /**
     * Emit the return type of a method. A method's type is composed of it's parameters
     * and return type.
     * @param the receiver type of the method or null if the method has no receiver.
     * @param parameters The method's parameters.
     * @param returnType The method's return type.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMethodType(Type receiver, Type[] parameters, Type returnType) {
        out.writeType(returnType.suiteID(), "type");
        //params
        if(receiver == null) {
            out.writeUnsignedShort(parameters.length, "parameters_count");
            out.start("parameters");
        }
        else {
            out.writeUnsignedShort(parameters.length+1, "parameters_count");
            out.start("parameters");
            out.writeType(receiver.suiteID(), null);
        }

        for (int i = 0; i < parameters.length; i++) {
            out.writeType(parameters[i].suiteID(), null);
        }
        out.end("parameters");
    }
    public void    emitOverriddenAccessStart(int numOverrides) {
        out.writeUnsignedShort(numOverrides, "overriding_count");
        out.start("overriding");
    }

    public void emitOverriddenAccessEnd() {
        out.end("overriding");
    }
    public void emitOverriddenAccessMethodStart(Method method) {
        out.start("Overriding_info");
    }
    public void emitOverriddenAccessMethodEnd() {
        out.end("Overriding_info");
    }
    public void emitOverriddenAccessMethodSlot(int slot) {
        out.writeUnsignedShort(slot, "vindex");
    }
    public void    emitOverriddenAccessMethodAccess(int flags) {
        out.writeUnsignedShort(flags, "access_flags");
    }

    /**
     * Emit the list of classes referenced by a class.
     * @param classes The list of classes.
     */
    public void emitClassReferences(Type[] classes) {
        out.writeUnsignedShort(classes.length, "class_refs_count");
        out.start("class_refs");
        for (int i = 0; i < classes.length; i++){
            out.commentNext(comment(classes[i]));
            out.writeType(classes[i].suiteID(), null);
        }
        out.end("class_refs");
    }
    /**
     * Emit the list immutable constant objects the methods of a class may refer to.
     * @param constants The list of immutable constant objects.
     * @return true if the suite producer should traverse the children of this suite element.
     */
    public boolean emitConstantsStart(Object[] constants) {
        out.writeUnsignedShort(constants.length, "objects_count");
        out.start("objects");
        return true;
    }
    public void    emitConstantsEnd()                     {
        out.end("objects");
    }

    /**
     * Emit an immutable String constant object.
     * @param index The index of the constant in the class's constant object pool.
     * @param constant The immutable String constant object.
     */
    public void emitStringConstant(int index, String constant) {
        out.start("CONSTANT_String_info");
        out.writeUnsignedByte(SquawkConstants.CONSTANT_String, "tag");
        out.writeUTF8(constant, "value");
        out.end("CONSTANT_String_info");
    }

    /**
     * Emit an immutable int array constant object.
     * @param index The index of the constant in the class's constant object pool.
     * @param constant The immutable int array constant object.
     */
    public void emitIntArrayConstant(int index, int[] constant) {
        out.start("CONSTANT_Int_array_info");
        out.writeUnsignedByte(SquawkConstants.CONSTANT_Int_array, "tag");
        out.writeUnsignedShort(constant.length, "length");
        out.start("elements");
        for (int i = 0; i < constant.length; i++){
            out.writeInt(constant[i], null);
        }
        out.end("elements");
        out.end("CONSTANT_Int_array_info");
    }
    /**
     * Emit an immutable short array constant object.
     * @param index The index of the constant in the class's constant object pool.
     * @param constant The immutable short array constant object.
     */
    public void emitShortArrayConstant(int index, short[] constant) {
        out.start("CONSTANT_Short_array_info");
        out.writeUnsignedByte(SquawkConstants.CONSTANT_Short_array, "tag");
        out.writeUnsignedShort(constant.length, "length");
        out.start("elements");
        for (int i = 0; i < constant.length; i++){
            out.writeShort(constant[i], null);
        }
        out.end("elements");
        out.end("CONSTANT_Short_array_info");
    }
    /**
     * Emit an immutable char array constant object.
     * @param index The index of the constant in the class's constant object pool.
     * @param constant The immutable char array constant object.
     */
    public void emitCharArrayConstant(int index, char[] constant) {
        out.start("CONSTANT_Char_array_info");
        out.writeUnsignedByte(SquawkConstants.CONSTANT_Char_array, "tag");
        out.writeUnsignedShort(constant.length, "length");
        out.start("elements");
        for (int i = 0; i < constant.length; i++){
            out.writeChar(constant[i], null);
        }
        out.end("elements");
        out.end("CONSTANT_Char_array_info");
    }

    /**
     * Emit an immutable byte array constant object.
     * @param index The index of the constant in the class's constant object pool.
     * @param constant The immutable byte array constant object.
     */
    public void emitByteArrayConstant(int index, byte[] constant) {
        out.start("CONSTANT_Byte_array");
        out.writeUnsignedByte(SquawkConstants.CONSTANT_Byte_array, "tag");
        out.writeUnsignedShort(constant.length, "length");
        out.start("elements");
        for (int i = 0; i < constant.length; i++){
            out.writeByte(constant[i], null);
        }
        out.end("elements");
        out.end("CONSTANT_Byte_array_info");
    }

    /**
     * Emit the attributes (if any) for a class.
     *
     * @param type The class to which the atributes pertain.
     * @param attributes The attributes.
     */
    public void emitClassAttributes(Type type, SuiteAttribute[] attributes) {
        VMAccessedAttribute attribute = (VMAccessedAttribute)findAttribute(attributes, "VMAccessed");
        if (attribute != null) {
            int attributeLength = getVMAccessedAttributeLength(attribute);
            out.writeUnsignedShort(1, "attributes_count");
            out.start("attributes");
            out.start("attribute");
            out.writeUTF8(attribute.getAttributeName(), "attribute_name");
            out.writeUnsignedInt(attributeLength, "attribute_length");
            out.writeUnsignedShort(attribute.classAccessFlags, "class_access_flags");

            // Write the instance fields table
            out.writeUnsignedShort(attribute.getInstanceFieldsTableSize(), "instance_fields_table_length");
            out.start("instance_fields_table");
            for (Enumeration e = attribute.getInstanceFieldsTableEntries(); e.hasMoreElements();) {
                VMAccessedAttribute.Item item = (VMAccessedAttribute.Item)e.nextElement();
                out.writeUnsignedShort(item.slot, "slot");
                out.writeUnsignedShort(item.accessFlags, "access_flags");
            }
            out.end("instance_fields_table");

            // Write the static methods table
            out.writeUnsignedShort(attribute.getStaticMethodsTableSize(), "static_methods_table_length");
            out.start("static_methods_table");
            for (Enumeration e = attribute.getStaticMethodsTableEntries(); e.hasMoreElements();) {
                VMAccessedAttribute.Item item = (VMAccessedAttribute.Item)e.nextElement();
                out.writeUnsignedShort(item.slot, "slot");
                out.writeUnsignedShort(item.accessFlags, "access_flags");
            }
            out.end("static_methods_table");

            out.end("attribute");
            out.end("attributes");
        } else {
            out.writeUnsignedShort(0, "attributes_count");
        }
    }

    /**
     * Emit the method bodies for a list of methods.
     * @param methodCount The number of methods.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitMethodBodiesStart(int methodCount)   {
        out.writeUnsignedShort(methodCount, "methods_count");
        return true;
    }
    public void emitMethodBodiesEnd() {
    }

    /**
     * Emit the method body for a method.
     * @param method The method.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitMethodBodyStart(Method method) {
        out.start("MethodImpl_info", comment(method));
        return true;
    }

    public void  emitMethodBodyEnd() {
        out.end("MethodImpl_info");
    }
    /**
     * Emit the class of a method.
     * @param ofClass The method's class.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMethodClass(Type ofClass) {
        out.writeType(ofClass.suiteID(), "ofClass");
    }

    /**
     * Emit the index of a method in it's table. There is one table for static
     * methods and one for virtual methods so method indexes are not necessarily
     * unique within a class.
     * @param index The method's index.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMethodSlot(int index) {
        out.writeUnsignedShort(index, "index");
    }

    /**
     * Emit the implementation access flags for a method.
     * @param flags the access flags
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMethodImplAccessFlags(int flags) {
        out.writeUnsignedShort(flags, "access_flags");
    }

    /**
     * Emit the local variable types of a method.
     * @param locals The local variable types of a method.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMethodLocals(Local[] locals) {
        out.writeUnsignedShort(locals.length, "locals_count");
        out.start("locals");
        for (int i = 0; i < locals.length; i++){
            out.writeType(locals[i].type().suiteID(), null);
        }
        out.end("locals");
    }

    /**
     * Emit the max stack value.
     * @param maxStack
     */
    public void emitMethodMaxStack(int maxStack) {
        out.writeUnsignedShort(maxStack, "stack_size");
    }

    /**
     * Start emitting the exception handler table of a method.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlersStart(int numHandlers) {
        out.writeUnsignedByte(numHandlers, "exception_table_length");
        out.start("exception_table");
    }
    public void emitExceptionHandlersEnd()                  {
        out.end("exception_table");
    }

    /**
     * Start emitting a single exception handler table entry of a method.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlerStart() {
        out.start("exception_handler");
    }

    public void emitExceptionHandlerEnd()   {
        out.end("exception_handler");
    }

    /**
     * Emit the start address of the code range protected by an exception handler table entry.
     * @param from The start address.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlerFrom(int from) {
        out.writeUnsignedInt(from, "start_pc");
    }

    /**
     * Emit the end address of the code range protected by an exception handler table entry.
     * @param to The end address.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlerTo(int to) {
        out.writeUnsignedInt(to, "end_pc");
    }

    /**
     * Emit the entry address of the code of an exception handler.
     * @param entry The entry address.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlerEntry(int entry) {
        out.writeUnsignedInt(entry, "handler_pc");
    }

    /**
     * Emit the exception type caught by an exception handler.
     * @param catchType The exception type caught by an exception handler.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlerCatchType(Type catchType) {
        out.writeType(catchType.suiteID(), "catch_type");
    }


    /**
     * Emit the bytecode of a method.
     * @param firstPass  If true, this notifys the emitter that this is pass 1 through the
     * instructions. During this pass, each instruction emitting method should not actually emit
     * any content but merely return the number of bytes it will emit when
     * invoked during pass 2. This pass is used by the SuiteProducer to calculate
     * the final code layout and hence the correct offsets for each branch.
     * During pass 2, each emitting method should just emit the content and
     * return anything as that value is ignored by the SuiteProducer.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitBytecodeStart(boolean firstPass, int length) {
        isFirstPass = firstPass;
        if (isFirstPass) {
            return true;
        }
        out.writeUnsignedInt(length, "code_length");
        out.start("code");
        return true;
    }

    public void emitBytecodeEnd() {
        if (!isFirstPass) {
            out.end("code");
        }
    }
     /*---------------------------------------------------------------------------*\
      *                Instruction emitting methods                               *
     \*---------------------------------------------------------------------------*/

    private boolean isFirstPass;
    /**
     * Helper function for emitting instructions that have exactly one format:
     *
     *    <opcode>
     *
     * @param inst
     * @param opcode The opcode of the instruction.
     * @return the number of bytes emitted which will be 1.
     */
    private int emitInstructionWithNoOperand(IContext ctx, int opcode) {
        if (!isFirstPass) {
            out.writeBytecode(opcode);
        }
        return 1;
    }

    /**
     * Helper function for emitting instructions that take a single operand that can
     * be up to 32-bits in magnitude.
     * @param inst
     * @param opcode The opcode of the instruction
     * @param isImm4BitOpcode If true, then opcode includes an immediate 4-bit operand.
     * @param operand The value of the operand.
     * @param debug
     * @return the number of bytes required for the encoding
     */
    private int emitInstructionWithVariableSizeIndexOperand(IContext ctx, int opcode, boolean isImm4BitOpcode, long operand, Object debug) {
        Assert.that(opcode >= 0 && opcode < Mnemonics.OPCODES.length);
        String mnemonic = Mnemonics.OPCODES[opcode];
        String comment = "";
        int size;
        // The *_<0..15> immediate operand format
        if (isImm4BitOpcode) {
            Assert.that(fits(4, operand), "operand="+operand);
            size = 1;
            if (!isFirstPass) {
                out.writeBytecode(opcode);
            }
        }
        // The single byte operand format
        else if (fits(8, operand)) {
            size = 2;
            if(!isFirstPass) {
                out.writeBytecode(opcode);
                out.writeUnsignedByte((int)operand, null);
            }
        }
        // The wide_<0-15> format
        else if (fits(12, operand)) {
            size = 3;
            if(!isFirstPass) {
                int wideImm = OPC.WIDE_0 + (int)((operand >> 8) & 0xF);
                Assert.that(wideImm >= OPC.WIDE_0 && wideImm <= OPC.WIDE_15);
                out.writeBytecode(wideImm);
                out.writeBytecode(opcode);
                out.writeUnsignedByte((int)(operand & 0xFF), null);
            }
        }
        // The wide_half format
        else if (fits(16, operand)) {
            size = 4;
            if(!isFirstPass) {
                out.writeBytecode(OPC.WIDE_HALF);
                out.writeBytecode(opcode);
                out.writeShort((short)operand, null);
            }
        }
        // The wide_int or wide_float format
        else {
            Assert.that(fits(32, operand));
            size = 6;
            if(!isFirstPass) {
                out.writeBytecode(OPC.WIDE_FULL);
                out.writeBytecode(opcode);
                out.writeInt((int)operand, null);
            }
        }

        return size;
    }

    /**
     * Emits either a long of float opcode.
     */
    private int emitLongFloatOpcode(IContext ctx, int opcode, boolean isFloat) {
        int size = (opcode == -1) ? 0 : 1;
        if (!isFirstPass) {
            // 2nd pass
            if (opcode != -1) {
                if (isFloat) {
/*if[FLOATS]*/
                    Assert.that(opcode >= 0 && opcode < Mnemonics.FLOAT_OPCODES.length);
                    out.writeFloatBytecode(opcode);
/*end[FLOATS]*/
                }
                else {
                    Assert.that(opcode >= 0 && opcode < Mnemonics.LONG_OPCODES.length);
                    out.writeLongBytecode(opcode);
                }
            }
        }
        return size;
    }

    /**
     * Emit an arithmetic instruction.
     * @param opcode One of the following bytecodes:
     *
     *   OPC_IADD
     *   OPC_ISUB
     *   OPC_IAND
     *   OPC_IOR
     *   OPC_IXOR
     *   OPC_ISHL
     *   OPC_ISHR
     *   OPC_IUSHR
     *   OPC_IMUL
     *   OPC_IDIV
     *   OPC_IREM
     * @return the number of bytes emitted for this instruction.
     */
    public int emitArithmeticOp(IContext ctx, int opcode, int longFloatOpcode, boolean isFloat) {
        return emitInstructionWithNoOperand(ctx, opcode) + emitLongFloatOpcode(ctx, longFloatOpcode, isFloat);
    }

    /**
     * Emit an array load instruction.
     * @param opcode One of the following bytecodes:
     *    OPC_ALOAD
     * @return the number of bytes emitted for this instruction.
     */
    public int emitArrayLoad(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }

    /**
     * Emit an array store instruction.
     * @param opcode One of the following bytecodes:
     *
     *    OPC_ASTORE
     * @return the number of bytes emitted for this instruction.
     */
    public int emitArrayStore(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }

    /**
     * Emit the array length instruction.
     * @return the number of bytes emitted for this instruction.
     */
    public int emitArrayLength(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }

    /**
     * The branch encoding table for IF and GOTO instructions.
     *
     *  Offset range  |  Encoding size
     *  --------------+----------------
     *   -(2^8)
     *
     *  [ 8, 2, 12, 3, 16 , 4 ]
     *
     * This specifies that a signed 8-bit offset can be encoded in a 2 byte instruction,
     * a signed 12-bit offset can be encoded in a 3 byte instruction and a 16-bit
     * offset can be encoded in a 4 byte instruction.
     */
    private BranchEncoding[] branchEncodingTable = new BranchEncoding[] {
        new BranchEncoding(8, 2, 2),
        new BranchEncoding(12, 3, 3),
        new BranchEncoding(16 , 4, 4)
    };

    /**
    * Emit a branch instruction.
    * @param opcode One of the following bytecodes:
    *
    *     OPC_IFEQ
    *     OPC_IFNE
    *     OPC_IFLT
    *     OPC_IFLE
    *     OPC_IFGT
    *     OPC_IFGE
    *
    *     OPC_IF_ICMPEQ
    *     OPC_IF_ICMPNE
    *     OPC_IF_ICMPLT
    *     OPC_IF_ICMPLE
    *     OPC_IF_ICMPGT
    *     opc_IF_ICMPGE
    *
    *     OPC_GOTO
    *
    * @param offset If this is pass 1, then this will be 0 and should be ignored.
    * If this is pass 2, then this is the offset to the target instruction.
    * @param bitSize This will be null during the first pass. On the second pass,
    * this will be one of the BranchEncoding values returned in the first pass.
    * @return null if this is pass 2. If this is pass 1, then
    * return a table mapping upper limits of offsets (in terms
    * their bit sizes) to the number of bytes required to encode these limits.
    * This table is used by the SuiteProducer to calculate the final code
    * layout and hence the correct offsets for each branch.
    */
    public BranchEncoding[] emitBranch(IContext ctx, int opcode, int offset, BranchEncoding enc) {
        if(isFirstPass){
            return branchEncodingTable;
        }

        // Prepend "wide" prefix (if necessary).
        int bitSize = enc.offsetBits;
        switch (bitSize) {
            case 8:                                            break;
            case 12: out.writeBytecode(OPC.WIDE_0+((offset >> 8) & 0xF)); break;
            case 16: out.writeBytecode(OPC.WIDE_HALF);                   break;
            default: System.out.println("ERROR: Unexpected bitSize for branch instruction offset: "+bitSize);
        }

        // Emit opcode
        if (opcode < 0 || opcode >= Mnemonics.OPCODES.length) {
            System.out.println("ERROR: invalid branch opcode: " + opcode);
            return null;
        }
        else {
            out.writeBytecode(opcode);
        }

        // Emit offset
        switch (bitSize) {
            case 8:  out.writeUnsignedByte(offset, null);       break;
            case 12: out.writeUnsignedByte(offset&0xFF, null);  break;
            case 16: out.writeShort((short)offset, null);       break;
        }
        return null;
    }

    /**
     * Emit the checkcast instruction.
     * @param opcode OPC_CHECKCAST
     * @return the number of bytes emitted for this instruction.
     */
    public int emitCheckcast(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }

    /**
     * Emit a primitive type conversion instruction.
     * @param opcode One of the following bytecodes:
     *
     *     OPC_I2B
     *     OPC_I2C
     *     OPC_I2S
     *     OPC_NEG
     * @return the number of bytes emitted for this instruction.
     */
    public int emitConversion(IContext ctx, int opcode, int longFloatOpcode, boolean isFloat) {
        return emitInstructionWithNoOperand(ctx, opcode) + emitLongFloatOpcode(ctx, longFloatOpcode, isFloat);
    }

    /**
     * Emit an instruction to load a class reference to the stack.
     * @param opcode One of the following bytecodes:
     *
     *     OPC_CLASS
     *     OPC_CLASS_<i>   [ 0 <= i <= 15 ]
     *
     * Note that the given opcode need not necessarily be emitted. For example,
     * if the given opcode is OPC_CLASS_12, the emitter can choose to emit
     * OPC_CLASS with the operand of 12 instead.
     * @param index The index of the class in the pool of class references for the
     * current class.  This index is in terms of the ordering in the array passed
     * to emitClassReferences.
     * @return the number of bytes emitted for this instruction.
     * @see #emitClassReferences(Type[])
     */
    public int emitLoadClass(IContext ctx, int opcode, int index) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, opcode != OPC.CLASS, index, null);
    }

    /**
     * Emit a load constant value instruction.
     * @param opcode One of the following bytecodes:
     *
     *     OPC_CONST_M1
     *     OPC_CONST_<i>    [ 0 <= i <= 15 ]
     *     OPC_CONST_BYTE
     *     OPC_CONST_SHORT
     *     OPC_CONST_CHAR
     *     OPC_CONST_INT
     *     OPC_CONST_FLOAT
     *     OPC_CONST_DOUBLE
     *
     * @param value the constant value to load.
     * @param operandBytes the number of bytes worth of value to emit.
     * @return the number of bytes emitted for this instruction.
     */
    public int emitLoadConstant(IContext ctx, int opcode, long value) {
        int size;
        if (opcode == OPC.CONST_M1 || (opcode >= OPC.CONST_0 && opcode <= OPC.CONST_15)) {
            size = 1;
        }else {
            switch (opcode) {
                case OPC.CONST_BYTE:   size = 2; break;
                case OPC.CONST_SHORT:  size = 3; break;
                case OPC.CONST_CHAR:   size = 3; break;
                case OPC.CONST_INT:    size = 5; break;
                case OPC.CONST_LONG:   size = 9; break;
/*if[FLOATS]*/
                case OPC.CONST_FLOAT:  size = 5; break;
                case OPC.CONST_DOUBLE: size = 9; break;
/*end[FLOATS]*/
                default:
                    throw new RuntimeException("Error: unknown load constant opcode: " + opcode);
            }
        }
        if (!isFirstPass) {
            //2nd pass
            if (opcode == OPC.CONST_M1 || (opcode >= OPC.CONST_0 && opcode <= OPC.CONST_15)) {
                out.writeBytecode(opcode);
            }else {
                out.writeBytecode(opcode);
                switch (opcode) {
                    case OPC.CONST_BYTE:   out.writeByte((byte)value, null);            break;
                    case OPC.CONST_SHORT:  out.writeShort((short) value, null);         break;
                    case OPC.CONST_CHAR:   out.writeChar((char)(value & 0xFFFF), null); break;
                    case OPC.CONST_INT:    out.writeInt((int)value, null);              break;
                    case OPC.CONST_LONG:   out.writeLong(value, null);                  break;
/*if[FLOATS]*/
                    case OPC.CONST_FLOAT:  out.writeFloat((int) value, null);           break;
                    case OPC.CONST_DOUBLE: out.writeDouble(value, null);                break;
/*end[FLOATS]*/
                    default:
                        throw new RuntimeException("Error: unknown load constant opcode: " + opcode);
                }
            }
        }
        return size;
    }

    /**
     * Emit a load constant null instruction.
     * @param opcode OPC_CONST_NULL
     * @return
     * @return the number of bytes emitted for this instruction.
     */
    public int emitLoadConstantNull(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }


    /**
     * Emit a load constant object instruction.
     * @param opcode One of the following bytecodes:
     *
     *     OPC_OBJECT_<n>    [ 0 <= n <= 15 ]
     *     OPC_OBJECT
     *
     * @param index The index of the constant object in the pool of constant
     * objects for the current class. This index is in terms of the ordering
     * in the array passed to emitConstants.
     * @return the number of bytes emitted for this instruction.
     * @see #emitConstants(Object[])
     */
    public int emitLoadObject(IContext ctx, int opcode, int index) {
        String constant = null;
        if (ctx.inst instanceof LoadConstantObject) {
            LoadConstantObject ldc = (LoadConstantObject)ctx.inst;
            Object value = ldc.value();
            if (value instanceof String) {
                constant = (String)value;
            }
        }
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, opcode != OPC.OBJECT, index, constant);
    }


    /**
     * Emit a get instance field instruction.
     * @param opcode One of the following bytecodes:
     *
     *     OPC_GETFIELD
     *
     * @param slot The slot number of the field.
     */
    public int emitGetField(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((LoadField)ctx.inst).field());
    }

    /**
     * Emit a get static field instruction.
     * @param opcode One of the following bytecodes:
     *
     *     OPC_GETSTATIC
     *
     * @param slot The slot number of the field.
     * @return the number of bytes emitted for this instruction.
     */
    public int emitGetStatic(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((LoadField)ctx.inst).field());
    }

    public int emitIncDecLocal(IContext ctx, int opcode, int index) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, index, ((IncDecLocal)ctx.inst).local());
    }

    public int emitInstanceOf(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }

    public int emitInvokeVirtual(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((Invoke)ctx.inst).method());
    }
    public int emitInvokeSuper(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((Invoke)ctx.inst).method());
    }
    public int emitInvokeStatic(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((Invoke)ctx.inst).method());
    }
    public int emitInvokeInit(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((Invoke)ctx.inst).method());
    }
    public int emitInvokeInterface(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((Invoke)ctx.inst).method());
    }
    public int emitLoadLocal(IContext ctx, int opcode, int index) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, opcode != OPC.LOAD, index, ((LoadLocal)ctx.inst).local());
    }
    public int emitLookup(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }
    public int emitMonitorEnter(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }
    public int emitMonitorExit(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }
    public int emitNew(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }
    public int emitNewArray(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }
    public int emitNewDimension(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }
    public int emitPop(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }
    public int emitPutField(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((StoreField)ctx.inst).field());
    }
    public int emitPutStatic(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((StoreField)ctx.inst).field());
    }
    public int emitStoreLocal(IContext ctx, int opcode, int index) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, opcode != OPC.STORE, index, ((StoreLocal)ctx.inst).local());
    }
    public int emitReturn(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }

    /**
     * This will have to be modified
     * @param opcode
     * @param is16Bit
     * @param defaultOffset
     * @param lowMatch
     * @param highMatch
     * @param offsets
     * @param padding
     * @return
     */
    public BranchEncoding emitTableSwitch(IContext ctx, int opcode, boolean is16Bit, int defaultOffset, int lowMatch, int highMatch, int[] offsets, int padding) {
        if (!is16Bit) {
            throw new RuntimeException("This SuiteEmitter implementation cannot emit 32-bit switch instructions");
        }
        int ipAdjust = 1 + // opcode
                       2 + // default offset
                       4 + // low
                       4;  // high
        if (isFirstPass) {
            return new BranchEncoding(16, ipAdjust+(2*(highMatch-lowMatch+1)), ipAdjust);
        }
        int size;
        if (!isFirstPass) {
            //2nd pass
            out.writeBytecode(opcode);
            if (padding == 1) {
                out.writeUnsignedByte(0, null);
            }
            int abs = ctx.ip + ipAdjust + padding + defaultOffset;
            out.writeUnsignedShort(defaultOffset, null);
            out.writeUnsignedInt(lowMatch, null);
            out.writeUnsignedInt(highMatch, null);
            for (int i = 0; i < highMatch-lowMatch+1; i++) {
                int rel = offsets[i];
                abs = ctx.ip +ipAdjust + padding + rel;
                out.writeUnsignedShort(rel, null);
            }
        }
        return null;
    }
    public int emitThrow(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }

    /**
     * Emit the attributes (if any) for a method body. Only the MethodDebug
     * attribute is recognised and emitted by this emitter.
     *
     * @param method The method to which the attributes pertain.
     * @param attributes The attributes.
     */
    public void emitMethodBodyAttributes(Method method, SuiteAttribute[] attributes) {
        MethodDebugAttribute attribute = (MethodDebugAttribute)findAttribute(attributes, "MethodDebug");
        if (attribute != null) {
            int attributeLength = getMethodDebugAttributeLength(attribute);
            out.writeUnsignedShort(1, "attributes_count");
            out.start("attributes");
            out.start("attribute");
            out.writeUTF8(attribute.getAttributeName(), "attribute_name");
            out.writeUnsignedInt(attributeLength, "attribute_length");
            out.writeUTF8(attribute.filePath, "file_path");
            out.writeUTF8(attribute.signature, "signature");
            out.writeUnsignedShort(attribute.getLineNumberTableSize(), "line_number_table_length");
            out.start("line_number_table");
            for (Enumeration e = attribute.getLineNumberTableEntries(); e.hasMoreElements();) {
                MethodDebugAttribute.LNTItem item = (MethodDebugAttribute.LNTItem)e.nextElement();
                out.writeUnsignedShort(item.startPc, "ip");
                out.writeUnsignedShort(item.lineNumber, "source_line_number");
            }
            out.end("line_number_table");
            out.end("attribute");
            out.end("attributes");
        } else {
            out.writeUnsignedShort(0, "attributes_count");
        }
    }

    /*------------------------------------------------------------------------*\
     *           Helpers for clsss and method attributes                      *
    \*------------------------------------------------------------------------*/

    private SuiteAttribute findAttribute(SuiteAttribute[] attributes, String name) {
        if (attributes != null) {
            for (int i = 0; i != attributes.length; ++i) {
                if (attributes[i].getAttributeName().equals(name)) {
                    return attributes[i];
                }
            }
        }
        return null;
    }

    /**
     * Calculate and return the number of bytes that will be written to a
     * DataOutputStream for a given VMAccessedAttribute.
     *
     * @param attribute
     * @return
     */
    private int getVMAccessedAttributeLength(VMAccessedAttribute attribute) {
        final int[] counter = new int[1];
        OutputStream counterOS = new OutputStream() {
            public void write(int b) {
                counter[0]++;
            }
        };
        try {
            DataOutputStream out = new DataOutputStream(counterOS);
            out.writeShort(attribute.classAccessFlags);
            out.writeShort(attribute.getInstanceFieldsTableSize());
            for (Enumeration e = attribute.getInstanceFieldsTableEntries(); e.hasMoreElements(); ) {
                VMAccessedAttribute.Item item = (VMAccessedAttribute.Item)e.nextElement();
                out.writeShort(item.slot);
                out.writeShort(item.accessFlags);
            }
            out.writeShort(attribute.getStaticMethodsTableSize());
            for (Enumeration e = attribute.getStaticMethodsTableEntries(); e.hasMoreElements(); ) {
                VMAccessedAttribute.Item item = (VMAccessedAttribute.Item)e.nextElement();
                out.writeShort(item.slot);
                out.writeShort(item.accessFlags);
            }
        } catch (IOException ioe) {
            Assert.shouldNotReachHere();
        }
        return counter[0];
    }

    /**
     * Calculate and return the number of bytes that will be written to a
     * DataOutputStream for a given MethodDebugAttribute.
     *
     * @param attribute
     * @return
     */
    private int getMethodDebugAttributeLength(MethodDebugAttribute attribute) {
        final int[] counter = new int[1];
        OutputStream counterOS = new OutputStream() {
            public void write(int b) {
                counter[0]++;
            }
        };

        try {
            DataOutputStream out = new DataOutputStream(counterOS);
            out.writeUTF(attribute.filePath);
            out.writeUTF(attribute.signature);
            out.writeShort(attribute.getLineNumberTableSize());
            for (Enumeration e = attribute.getLineNumberTableEntries(); e.hasMoreElements(); ) {
                MethodDebugAttribute.LNTItem item = (MethodDebugAttribute.LNTItem)e.nextElement();
                out.writeShort(item.startPc);
                out.writeShort(item.lineNumber);
            }
        } catch (IOException ioe) {
            Assert.shouldNotReachHere();
        }
        return counter[0];
    }

}

    /*------------------------------------------------------------------------*\
     *           Helper classes for BinarySuiteEmitter                        *
    \*------------------------------------------------------------------------*/

interface SuiteWriter {
    public void writeUnsignedByte(int b, String tag);
    public void writeBytes(byte[] b, String tag);
    public void writeUnsignedShort(int b, String tag);
    public void writeUnsignedInt(int b, String tag);
    public void writeInt(int i, String tag);
    public void writeShort(short i, String tag);
    public void writeByte(int i, String tag);
    public void writeChar(char i, String tag);
    public void writeLong(long v, String tag);
    public void writeLongBytecode(int op);
/*if[FLOATS]*/
    public void writeFloat(int f, String tag);
    public void writeDouble(long v, String tag);
    public void writeFloatBytecode(int op);
/*end[FLOATS]*/
    public void writeUTF8(String s, String tag);
    public void writeType(int i, String tag);
    public void writeBytecode(int op);

    public void writeComment(String comment);
    public void commentNext(String comment);
    public void start(String tag);
    public void start(String tag, String comment);
    public void end(String tag);
    public void close() throws IOException;
}

class BinarySuiteWriter implements SuiteWriter {
    private final DataOutputStream out;
    BinarySuiteWriter(OutputStream out) {
        if (out instanceof DataOutputStream) {
            this.out = (DataOutputStream)out;
        }
        else {
            this.out = new DataOutputStream(out);
        }
    }

    public void close() throws IOException {
        out.close();
    }

    public void writeUnsignedByte(int b, String tag)  { try { out.write(b);      } catch (IOException e) { ioe(e); } }
    public void writeBytes(byte[] b, String tag)      { try { out.write(b);      } catch (IOException e) { ioe(e); } }
    public void writeUnsignedShort(int s, String tag) { try { out.writeShort(s); } catch (IOException e) { ioe(e); } }
    public void writeUnsignedInt(int i, String tag)   { try { out.writeInt(i);   } catch (IOException e) { ioe(e); } }
    public void writeInt(int i, String tag)           { try { out.writeInt(i);   } catch (IOException e) { ioe(e); } }
    public void writeShort(short s, String tag)       { try { out.writeShort(s); } catch (IOException e) { ioe(e); } }
    public void writeByte(int b, String tag)          { try { out.writeByte(b);  } catch (IOException e) { ioe(e); } }
    public void writeChar(char c, String tag)         { try { out.writeChar(c);  } catch (IOException e) { ioe(e); } }
    public void writeLong(long l, String tag)         { try { out.writeLong(l);  } catch (IOException e) { ioe(e); } }
    public void writeLongBytecode(int op)             { writeUnsignedByte(op, null); }
/*if[FLOATS]*/
    public void writeFloat(int f, String tag)         { try { out.writeInt(f);   } catch (IOException e) { ioe(e); } }
    public void writeDouble(long d, String tag)       { try { out.writeDouble(d);} catch (IOException e) { ioe(e); } }
    public void writeFloatBytecode(int op)            { writeUnsignedByte(op, null); }
/*end[FLOATS]*/
    public void writeUTF8(String s, String tag)       { try { out.writeUTF(s);   } catch (IOException e) { ioe(e); } }
    public void writeType(int t, String tag) { writeUnsignedShort(t, null); }
    public void writeBytecode(int op)        { writeUnsignedByte(op, null); }

    public void writeComment(String comment)       {}
    public void commentNext(String comment)       {}
    public void start(String tag)                 {}
    public void start(String tag, String comment) {}
    public void end(String tag)                   {}

    private void ioe(IOException ioe) {
        ioe.printStackTrace();
        Assert.shouldNotReachHere();
    }
}

class DebugBinarySuiteWriter implements SuiteWriter {

    private final OutputStreamWriter out;
    private String indent = "";
    private String nextComment;
    private int ip;

    DebugBinarySuiteWriter(OutputStream out) {
        this.out = new OutputStreamWriter(out);
    }

    public void close() throws IOException {
        out.close();
    }

    private String indent(boolean showIp) {
        String pad = "                   ";
        if (showIp) {
            StringBuffer buf = new StringBuffer(pad.length());
            String hex = Integer.toHexString(ip).toUpperCase();
            buf.append("<!-- 0x");
            int hpad = 8-hex.length();
            while (hpad != 0) {
                buf.append('0');
                hpad--;
            }
            pad = buf.append(hex).append(" -->").toString();
        }
        return pad + indent;
    }

    private void writeOpcode(String opcode) {
        try {
            out.write(indent(true) + opcode);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.shouldNotReachHere();
        }
        ip ++;
    }
    private void write(String type, String tag, String item, int size) {
        try {
            out.write(indent(true) + "<" + type);
            if (tag != null && tag.length() > 0) {
                out.write(" name=\"" + tag + "\"");
            }
            out.write(">" + item + "</" + type + ">");
            if (nextComment != null) {
                out.write("  <!-- " + nextComment + " -->");
                nextComment = null;
            }
            out.write(Translator.lineSeparator);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.shouldNotReachHere();
        }
        ip += size;
    }

    public void writeBytes(byte[] b, String tag) {
        StringBuffer buf = new StringBuffer((b.length * 5)+10);
        buf.append("{ ");
        for (int i = 0; i != b.length; i++) {
            buf.append("0x"+Integer.toHexString(b[i]&0xFF)).append(' ');
        }
        buf.append("}");
        write("u1["+b.length+"]", tag, buf.toString(), b.length);
    }


    public void writeUnsignedByte(int b, String tag)  { write("u1",  tag, "0x"+Integer.toHexString(b&0xFF), 1);   }
    public void writeUnsignedShort(int s, String tag) { write("u2",  tag, "0x"+Integer.toHexString(s&0xFFFF), 2); }
    public void writeUnsignedInt(int i, String tag)   { write("u4",  tag, "0x"+Integer.toHexString(i), 4);        }
    public void writeInt(int i, String tag)           { write("int",    tag, ""+i, 4); }
    public void writeShort(short s, String tag)       { write("short",  tag, ""+s, 2); }
    public void writeByte(int b, String tag)          { write("byte",   tag, ""+b, 1); }
    public void writeChar(char c, String tag)         { write("char",   tag, ""+c, 2); }
    public void writeLong(long l, String tag)         { write("long",   tag, ""+l, 8);         }
    public void writeLongBytecode(int op)             { writeOpcode(Mnemonics.LONG_OPCODES[op]);  }
/*if[FLOATS]*/
    public void writeFloat(int f, String tag)         { write("float",  tag, ""+(float)f, 4);  }
    public void writeDouble(long d, String tag)       { write("double", tag, ""+(double)d, 8); }
    public void writeFloatBytecode(int op)            { writeOpcode(Mnemonics.FLOAT_OPCODES[op]); }
/*end[FLOATS]*/
    public void writeType(int t, String tag)          { write("type",   tag, ""+t, 2);         }
    public void writeBytecode(int op)                 { writeOpcode(Mnemonics.OPCODES[op]);      }

    public void writeUTF8(String s, String tag)       {
        int utfLen = Translator.utf8Size(s);
        write("UTF", tag+"\" utfLength=\""+utfLen, XMLSuiteEmitter.XMLEncodeString(s), utfLen+2);
    }

    public void writeComment(String comment) {
        try {
            out.write(indent(false)+"<!-- "+comment+" -->"+Translator.lineSeparator);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.shouldNotReachHere();
        }
    }
    public void commentNext(String comment) {
        nextComment = comment;
    }

    public void start(String tag, String comment) {
        if (tag != null && tag.length() > 0) {
            try {
                out.write(indent(false)+"<"+tag+">");
                if (comment != null) {
                    out.write("  <!-- "+comment+" -->");
                }
                out.write(Translator.lineSeparator);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Assert.shouldNotReachHere();
            }
        }
        indent += "  ";
    }
    public void start(String tag) {
        start(tag, null);
    }
    public void end(String tag) {
        indent = indent.substring(2);
        if (tag != null && tag.length() > 0) {
            try {
                out.write(indent(false)+"</"+tag+">"+Translator.lineSeparator);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Assert.shouldNotReachHere();
            }
        }
    }
}

