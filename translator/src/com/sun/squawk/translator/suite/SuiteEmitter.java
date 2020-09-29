package com.sun.squawk.translator.suite;

import java.util.*;
import java.io.OutputStream;
import com.sun.squawk.translator.Assert;
import com.sun.squawk.translator.ir.Instruction;
import com.sun.squawk.translator.ir.InstructionVisitor;
import com.sun.squawk.translator.ir.Local;
import com.sun.squawk.translator.*;

import java.io.IOException;

/**
 * This class is the interface used by a SuiteProducer to emit a serialized
 * representation of a suite. A suite can be thought of as a hierachial
 * collection of elements. A suite producer traverses each of the elements
 * in this hierarchy, calling a method in the SuiteEmitter for each element
 * it traverses. The SuiteEmitter is responsible for emitting a representation
 * for that element (which may be nothing depending on the emitter). For elements
 * that have children, the emit method returns a boolean which informs
 * the suite producer whether or not to continue traversing the
 * children of the current element. This gives the SuiteEmitter fine grain
 * control of how much of the standard suite layout it wants to conform to.
 *
 * For example, a SuiteEmitter used for generating standard class files for the
 * classes in a suite will only override the 'emitSuite' method and return
 * false when it completes.
 *
 * The convention used for the method names in this interface is as follows.
 * Elements that have children will have two methods:
 *
 *    emit<element_name>Start
 *    emit<element_name>End
 *
 * Elements that have no children will only have the following method:
 *
 *    emit<element_name>
 *
 * Calling one of the emit methods in this interface is never expected
 * to fail and so any exceptions occuring during one of these methods
 * should be converted to an instance of RuntimeException (or a subclass of
 * RuntimeException).
 */
public interface SuiteEmitter {

/*---------------------------------------------------------------------------*\
 *                      BranchEncoding helper class                          *
\*---------------------------------------------------------------------------*/

    /**
     * Instances of this class are a pair of numbers representing the magnitude
     * (in number of bits) of a signed branch offset and the size of the
     * corresponding branch instruction (in bytes).
     */
    public static class BranchEncoding {
        /** The magnitude of the signed offsets encodable by this encoding. */
        public final int offsetBits;
        /** The size of the encoded branch instruction. */
        public final int encBytes;
        /** The absolute offset of the encoded branch instruction is calculated by
            adding the relative offset to IP of the branch instruction adjusted by
            ipAdjust. */
        public final int ipAdjust;

        public BranchEncoding(int offsetBits, int encBytes, int ipAdjust) {
            this.offsetBits = offsetBits;
            this.encBytes   = encBytes;
            this.ipAdjust   = ipAdjust;
        }
    }

/*---------------------------------------------------------------------------*\
 *                      IContext helper class                                *
\*---------------------------------------------------------------------------*/

    /**
     * This class encapsulates the context of an instruction being emitted.
     */
    public static class IContext {
        /** The original IR instruction to which this instruction corresponds. */
        public final Instruction inst;
        /** The IP address of the instruction after relocation. */
        public final int ip;
        /** The state of the stack just prior to executing this instruction. */
        public final Type[] stackState;
        public final int stackDepth;

        public IContext(Instruction inst, int ip, Type[] stackState, int stackDepth) {
            this.inst = inst;
            this.ip   = ip;
            this.stackState = stackState;
            this.stackDepth = stackDepth;
        }
    }

/*---------------------------------------------------------------------------*\
 *                 Initialization and misc. methods                          *
\*---------------------------------------------------------------------------*/

    /**
     * Optionally emit a comment.
     * @param prefix Optional prefix.
     * @param comment
     * @throws IOException
     */
    public void emitComment(String prefix, String comment, boolean nl);

    /** Constant specifying stack tracing. */
    public final static int EMIT_STACK_COMMENTS = 1<<0;
    /** Constant specifying that comments are to be emitted. */
    public final static int EMIT_COMMENTS       = 1<<1;
    /** Constant specifying that <meta> attributes are to be emitted. */
    public final static int EMIT_METHOD_DEBUG   = 1<<2;

    /**
     * Set the output stream and flags for the emitter. This should be called
     * after construction and emitter implementations should ensure that it is
     * only called once.
     *
     * @param out The output stream to which the emitter should emit.
     * @param flags a mask of the constant values defined in this interface
     * @param properties a set of properties specific to the emitter
     * implementation class. This may be null.
     */
    public void init(OutputStream out, int flags, Hashtable properties);

    public void close() throws IOException;

    public boolean isEmittingStackComments();
    public boolean isEmittingComments();
    public boolean isEmittingMethodDebugAttribute();

/*---------------------------------------------------------------------------*\
 *                Meta-info emitting methods                                 *
\*---------------------------------------------------------------------------*/

    /**
     * Emit a physical representation of a Suite.
     *
     * @param name The name of the suite.
     * @param isFinal Indicates if this is a final suite or not.
     * @param proxies The proxy classes of the suite.
     * @param classes The non-proxy classes of the suite.
     * @return true if the suite producer should traverse the subelements of the suite.
     */
    public boolean emitSuite(String name, boolean isFinal, Type[] proxies, Type[] classes);

    /**
     * Emit the suite header.
     *
     * @param name The name of the suite.
     * @param isFinal Indicates if this is a final suite or not.
     * @param binds The list of suites to which this one is bound.
     */
    public void emitSuiteStart(String name, boolean isFinal, String[] binds);

    /**
     * This method is called when the last suite element has been emitted.
     */
    public void emitSuiteEnd();

    /**
     * Start emitting meta info for all classes (i.e. both proxy and non-proxy classes).
     *
     * @param numClasses The total number of classes about to be emitted.
     * @throws IOException if there is an IO problem.
     */
    public void emitAllClassesMetaInfoStart(int numClasses);
    public void emitAllClassesMetaInfoEnd();

    /**
     * Emit the meta-info for a list of classes.
     *
     * @param classes The list of classes.
     * @param boolean isProxy true if classes is a list of proxy classes, false otherwise
     * @return true if the suite producer should traverse the children of this suite element.
     */
    public boolean emitClassesMetaInfoStart(Type[] classes, boolean isProxy);
    public void    emitClassesMetaInfoEnd();

    /**
     * Emit the meta-info for a class.
     *
     * @param type The class.
     * @param boolean isProxy true if classes is a list of proxy classes, false otherwise
     * @param isLast true if this is the last class in the list of classes.
     * @return true if the suite producer should traverse the children of this suite element.
     */
    public boolean emitClassMetaInfoStart(Type type, boolean isProxy, boolean isLast);
    public void    emitClassMetaInfoEnd();

    /**
     * Emit the suite ID for class.
     * @param type The class.
     */
    public void emitClassType(Type type);

    /**
     * Emit the access flags for a class.
     * @param accessFlags The class access flags.
     */
    public void emitClassAccessFlags(int accessFlags);

    /**
     * Emit the name of a class.
     * @param name The name of the class.
     */
    public void emitClassName(String name);

    /**
     * Emit the super class for a class.
     * @param superClassNumber The super class (null if this is Object).
     */
    public void emitClassExtends(Type superClass);

    /**
     * Emit the interfaces implemented by a class.
     * @param numInterfaces The number of interfaces implemented.
     */
    public void emitClassImplementsStart(int numInterfaces);
    public void emitClassImplementsEnd();
    public void emitInterfaceStart();
    public void emitInterfaceEnd();
    public void emitInterfaceType(Type type);
    public void emitInterfaceMethodImplementationSlotsStart(int numMethods);
    public void emitInterfaceMethodImplementationSlotsEnd();
    public void emitInterfaceMethodImplementationSlot(int slot);

    /**
     * Emit the name for a method or field.
     * @param name The name of the method or field.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMemberName(String name);

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
                            boolean isProxy);
    public void    emitFieldsEnd(boolean isStatic);

    /**
     * Emit the meta info for a field.
     * @param field The field.
     * @param isProxy true if this is a field of a proxy class, false otherwise.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitFieldStart(Field field, boolean isProxy);
    public void    emitFieldEnd();

    /**
     * Emit the type of a field.
     * @param type The field's type.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitFieldType(Type type);

    /**
     * Emit the access flags for a field.
     * @param accessFlags The field access flags.
     */
    public void emitFieldAccessFlags(int accessFlags);

    /**
     * Emit the meta info for a list of methods.
     * @param methods The list of methods.
     * @param methodsCount The number of methods to emit from the start of the list.
     * @param isStatic Emitting static methods.
     * @param isProxy true if this is a list of methods for a proxy class, false otherwise.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitMethodsStart(Method[] methods,
                            int methodsCount,
                            boolean isStatic,
                            boolean isProxy);
    public void    emitMethodsEnd(boolean isStatic);

    /**
     * Emit the meta info for a method.
     * @param method The method.
     * @param isProxy true if this is a method of a proxy class, false otherwise.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitMethodStart(Method method, boolean isProxy);
    public void    emitMethodEnd();

    /**
     * Emit the return type of a method. A method's type is composed of it's parameters
     * and return type.
     * @param the receiver type of the method or null if the method has no receiver.
     * @param parameters The method's parameters.
     * @param returnType The method's return type.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMethodType(Type receiver, Type[] parameters, Type returnType);

    /**
     * Emit the access flags for a method.
     * @param accessFlags The method access flags.
     */
    public void emitMethodAccessFlags(int accessFlags);

    public void emitOverriddenAccessStart(int numOverrides);
    public void emitOverriddenAccessEnd();
    public void emitOverriddenAccessMethodStart(Method method);
    public void emitOverriddenAccessMethodEnd();
    public void emitOverriddenAccessMethodSlot(int slot);
    public void emitOverriddenAccessMethodAccess(int flags);

   /**
    * Emit the list of classes referenced by a class.
    * @param classes The list of classes.
    * @throws IOException if there is an IO problem.
    */
    public void emitClassReferences(Type[] classes);

    /**
     * Emit the list immutable constant objects the methods of a class may refer to.
     * @param constants The list of immutable constant objects.
     * @return true if the suite producer should traverse the children of this suite element.
     * @throws IOException if there is an IO problem.
     */
    public boolean emitConstantsStart(Object[] constants);
    public void    emitConstantsEnd();

    /**
     * Emit an immutable String constant object.
     * @param index The index of the constant in the class's constant object pool.
     * @param constant The immutable String constant object.
     * @throws IOException if there is an IO problem.
     */
    public void emitStringConstant(int index, String constant);

    /**
     * Emit an immutable int array constant object.
     * @param index The index of the constant in the class's constant object pool.
     * @param constant The immutable int array constant object.
     * @throws IOException if there is an IO problem.
     */
    public void emitIntArrayConstant(int index, int[] constant);

    /**
     * Emit an immutable short array constant object.
     * @param index The index of the constant in the class's constant object pool.
     * @param constant The immutable short array constant object.
     * @throws IOException if there is an IO problem.
     */
    public void emitShortArrayConstant(int index, short[] constant);

    /**
     * Emit an immutable char array constant object.
     * @param index The index of the constant in the class's constant object pool.
     * @param constant The immutable char array constant object.
     * @throws IOException if there is an IO problem.
     */
    public void emitCharArrayConstant(int index, char[] constant);

    /**
     * Emit an immutable byte array constant object.
     * @param index The index of the constant in the class's constant object pool.
     * @param constant The immutable byte array constant object.
     * @throws IOException if there is an IO problem.
     */
    public void emitByteArrayConstant(int index, byte[] constant);

    /**
     * Emit the attributes (if any) for a class.
     *
     * @param type The class to which the atributes pertain.
     * @param attributes The attributes.
     */
    public void emitClassAttributes(Type type, SuiteAttribute[] attributes);

    /**
     * Emit the method bodies for a list of methods.
     * @param methodCount The number of methods.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitMethodBodiesStart(int methodCount);
    public void    emitMethodBodiesEnd();

    /**
     * Emit the method body for a method.
     * @param method The method.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitMethodBodyStart(Method method);
    public void emitMethodBodyEnd();

    /**
     * Emit the class of a method.
     * @param ofClass The method's class.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMethodClass(Type ofClass);

    /**
     * Emit the index of a method in it's table. There is one table for static
     * methods and one for virtual methods so method indexes are not necessarily
     * unique within a class.
     * @param index The method's index.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMethodSlot(int index);

    /**
     * Emit the implementation access flags for a method.
     * @param flags the access flags
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMethodImplAccessFlags(int flags);

    /**
     * Emit the local variable types of a method.
     * @param locals The local variables of a method.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMethodLocals(Local[] locals);

    /**
     * Emit the max stack value.
     * @param maxStack
     * @throws IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMethodMaxStack(int maxStack);

    /**
     * Start emitting the exception handler table of a method.
     * @param numHandlers The number of handlers about to be emitted.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlersStart(int numHandlers);
    public void emitExceptionHandlersEnd();

    /**
     * Start emitting a single exception handler table entry of a method.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlerStart();
    public void emitExceptionHandlerEnd();

    /**
     * Emit the start address of the code range protected by an exception handler table entry.
     * @param from The start address.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlerFrom(int from);

    /**
     * Emit the end address of the code range protected by an exception handler table entry.
     * @param to The end address.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlerTo(int to);

    /**
     * Emit the entry address of the code of an exception handler.
     * @param entry The entry address.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlerEntry(int entry);

    /**
     * Emit the exception type caught by an exception handler.
     * @param catchType The exception type caught by an exception handler.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlerCatchType(Type catchType);

    /**
     * Start the emitting of the bytecode of a method.
     * @param firstPass  If true, this notifys the emitter that this is pass 1 through the
     * instructions. During this pass, each instruction emitting method should not actually emit
     * any content but merely return the number of bytes it will emit when
     * invoked during pass 2. This pass is used by the SuiteProducer to calculate
     * the final code layout and hence the correct offsets for each branch.
     * During pass 2, each emitting method should just emit the content and
     * return anything as that value is ignored by the SuiteProducer.
     * @param length the total length of the bytecode about to be emitted in bytes. This
     * value is meaningless if firstPass is true.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitBytecodeStart(boolean firstPass, int length);
    public void    emitBytecodeEnd();

/*---------------------------------------------------------------------------*\
 *                Instruction emitting methods                               *
\*---------------------------------------------------------------------------*/

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
    public int emitArithmeticOp(IContext ctx, int opcode, int longFloatOpcode, boolean isFloat);

    /**
     * Emit an array load instruction.
     * @param opcode One of the following bytecodes:
     *
     *    OPC_ALOAD
     * @return the number of bytes emitted for this instruction.
     */
    public int emitArrayLoad(IContext ctx, int opcode);

    /**
     * Emit an array store instruction.
     * @param opcode One of the following bytecodes:
     *
     *    OPC_ASTORE
     * @return the number of bytes emitted for this instruction.
     */
    public int emitArrayStore(IContext ctx, int opcode);

    /**
     * Emit the array length instruction.
     * @return the number of bytes emitted for this instruction.
     */
    public int emitArrayLength(IContext ctx, int opcode);

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
    public BranchEncoding[] emitBranch(IContext ctx, int opcode, int offset, BranchEncoding enc);

    /**
     * Emit the checkcast instruction.
     * @param opcode OPC_CHECKCAST
     * @return the number of bytes emitted for this instruction.
     */
    public int emitCheckcast(IContext ctx, int opcode);

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
    public int emitConversion(IContext ctx, int opcode, int longFloatOpcode, boolean isFloat);

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
    public int emitLoadClass(IContext ctx, int opcode, int index);

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
     * @return the number of bytes emitted for this instruction.
     */
    public int emitLoadConstant(IContext ctx, int opcode, long value);

    /**
     * Emit a load constant null instruction.
     * @param opcode OPC_CONST_NULL
     * @return
     * @return the number of bytes emitted for this instruction.
     */
    public int emitLoadConstantNull(IContext ctx, int opcode);

    /**
     * Emit a load constant object instruction.
     * @param opcode One of the following bytecodes:
     *
     *     OPC_OCONST_<n>    [ 0 <= n <= 15 ]
     *     OPC_OCONST
     *
     * @param index The index of the constant object in the pool of constant
     * objects for the current class. This index is in terms of the ordering
     * in the array passed to emitConstants.
     * @return the number of bytes emitted for this instruction.
     * @see #emitConstants(Object[])
     */
    public int emitLoadObject(IContext ctx, int opcode, int index);


    /**
     * Emit a get instance field instruction.
     * @param opcode One of the following bytecodes:
     *
     *     OPC_GETFIELD
     *
     * @param slot The slot number of the field.
     */
    public int emitGetField(IContext ctx, int opcode, int slot);

    /**
     * Emit a get static field instruction.
     * @param opcode One of the following bytecodes:
     *
     *     OPC_GETSTATIC
     *
     * @param slot The slot number of the field.
     * @return the number of bytes emitted for this instruction.
     */
    public int emitGetStatic(IContext ctx, int opcode, int slot);

    public int emitInstanceOf(IContext ctx, int opcode);
    public int emitIncDecLocal(IContext ctx, int opcode, int index);
    public int emitInvokeVirtual(IContext ctx, int opcode, int slot);
    public int emitInvokeSuper(IContext ctx, int opcode, int slot);
    public int emitInvokeStatic(IContext ctx, int opcode, int slot);
    public int emitInvokeInit(IContext ctx, int opcode, int slot);
    public int emitInvokeInterface(IContext ctx, int opcode, int slot);
    public int emitLoadLocal(IContext ctx, int opcode, int index);
    public int emitLookup(IContext ctx, int opcode);
    public int emitMonitorEnter(IContext ctx, int opcode);
    public int emitMonitorExit(IContext ctx, int opcode);
    public int emitNew(IContext ctx, int opcode);
    public int emitNewArray(IContext ctx, int opcode);
    public int emitNewDimension(IContext ctx, int opcode);
    public int emitPop(IContext ctx, int opcode);
    public int emitPutField(IContext ctx, int opcode, int slot);
    public int emitPutStatic(IContext ctx, int opcode, int slot);
    public int emitStoreLocal(IContext ctx, int opcode, int index);
    public int emitReturn(IContext ctx, int opcode);

    /**
     * Emit a tableswitch instruction.
     * @param opcode One of the following bytecodes:
     *
     *     OPC_TABLESWITCH
     *     OPC_STABLESWITCH
     *
     * @param is16Bit If true, then all the following parameters are signed 16-bit values and
     * should be encoded as such otherwise they are signed 32-bit values.
     * @param defaultOffset
     * @param lowMatch
     * @param highMatch
     * @param offsets
     * @param padding
     * @return relocation information to do with padding. This interface will have to be
     * modified to support 32-bit tableswitch instruction emitting.
     */
    public BranchEncoding emitTableSwitch(IContext ctx, int opcode, boolean is16Bit, int defaultOffset, int lowMatch, int highMatch, int[] offsets, int padding);
    public int emitThrow(IContext ctx, int opcode);

    /**
     * Emit the attributes (if any) for a method body.
     *
     * @param method The method to which the attributes pertain.
     * @param attributes The attributes.
     */
    public void emitMethodBodyAttributes(Method method, SuiteAttribute[] attributes);
}
