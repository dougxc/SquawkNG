package com.sun.squawk.translator;

import java.util.*;

import com.sun.squawk.translator.util.*;
import com.sun.squawk.translator.loader.*;
import com.sun.squawk.translator.ir.*;


/**
 * The base class for implementations which represent a method in its various
 * stages of resolution.
 */
public final class Method extends Member {

    /**
     * This class represents a method's signature as its parameter types and return Type.
     */
    public static class SignatureTypes {
        /** The return type of a method. */
        public final Type returnType;
        /** The declared parameter types of a method. */
        public final Type[] parmTypes;

        /**
         * Construct a method signature.
         */
        public SignatureTypes(Type returnType, Type[] parmTypes) {
            this.returnType = returnType;
            this.parmTypes  = parmTypes;
        }

        /** Sentinel object representing an invalid signature. */
        public static final SignatureTypes INVALID = new SignatureTypes(null, null);

        /**
         * Change the return type of a signature. If the new return type is different
         * from the existing one, a new object is created and returned.
         */
        public SignatureTypes modifyReturnType(Type type) {
            if (type == returnType) {
                return this;
            }
            else {
                return new SignatureTypes(type, parmTypes);
            }
        }
    }

    public static class CodeMetrics {
        public int length;
        public int maxStack;
        public int maxLocals;
        public boolean usesLong;
/*if[FLOATS]*/
        public boolean usesFloatOrDouble;
/*end[FLOATS]*/
        public int instructionCount;
        public Vector oopMapInstructions;
        public CodeMetrics(int length, int maxStack, int maxLocals, int instructionCount) {
            this.length    = length;
            this.maxStack  = maxStack;
            this.maxLocals = maxLocals;
            this.instructionCount = instructionCount;
        }
    }

    public static CodeMetrics NO_CODE = new CodeMetrics(0,0,0,0);

    /** A list of the declared parameters. This does not include the implicit receiver for non-static methods. */
    private final Type[] parms;
    /** Flags whether or not this is a stub for a method corresponding to a well known slot offset. */
    private boolean synthetic;
    /** The overridden superclass method (if any). */
    private Method overridden;
    /** The BytecodeHolder containing the unparsed Java bytecode. */
    private BytecodeHolder holder;
    /** The IR. */
    private InstructionList ir;
    /** The metrics of the javac generated code. */
    private CodeMetrics javacMetrics;
    /** The metrics of the generated Squawk code. */
    private CodeMetrics squawkMetrics;

    /**
     * Public constructor.
     * @param parent The class in which this method is defined.
     * @param name The method's name.
     * @param flags The method's flags.
     * @param type The method's return type.
     * @param parms The method's parameter types.
     * @return the newly constructed Method
     */
    public static Method create(Type parent, String name, int flags, Type returnType, Type[] parmTypes) {
        return new Method(parent, name, flags, returnType, parmTypes);
    }

    /**
     * Set the overridden superclass method.
     * @param overridden the overridden superclass method.
     */
    public void overrides(Method overridden) {
        Assert.that(!isStatic());
        Assert.that(overridden != null);
        this.overridden = overridden;
    }

    /**
     * Get the overridden superclass method (if any).
     * @return the overridden superclass method or null if this method does
     * not override a superclass's method.
     */
    public Method overridden() {
        Assert.that(!isStatic());
        return overridden;
    }

    /**
     * Get the slot of this method.
     * @return the slot of this method.
     */
    public int slot() {
        if (overridden != null && (!vm().pruneSuite() || overridden.includeInSuite())) {
            return overridden.slot();
        }
        return super.slot();
    }

    /**
     * Load the types implied by the signature of this method. That is, load
     * the parameter types, the return type and the declaring class.
     */
    public void loadSignatureTypes() throws LinkageException {
        if (!areTypesLoaded()) {
            parent().load();
            type().load();
            for (int i = 0 ; i < getParms().length ; i++) {
                getParms()[i].load();
            }
            setTypesLoaded();
        }
    }

    /**
     * Return the parameters of the method as a String.
     *
     * @param includePackageNames If true, then use fully qualified class names.
     * @param asSourceDecl If true, then use a format corresponding with the source level
     * declaration of this method.
     * @return the parameters of the method as a String.
     */
    public String parmsToString(boolean includePackageNames, boolean asSourceDecl) {
        return "("+Type.toSignature(parms, ",")+")";
    }

    /**
     * Return the signature of the method as a String. Depending on the values of the
     * parameters, the returned String for method named "equals" that takes a parameter
     * of type java.lang.Object can be:
     *
     *     "(Ljava/lang/Object;)V"
     *     "(LObject;)V"
     *     "void (Object);"
     *
     * @param includePackageNames If true, then use fully qualified class names.
     * @param asSourceDecl If true, then use a format corresponding with the source level
     * declaration of this method.
     * @return the signature of the method as a String.
     */
    public String signature(boolean includePackageNames, boolean asSourceDecl) {
        String sig = parmsToString(includePackageNames, asSourceDecl);
        if (asSourceDecl) {
            sig = type().toSignature(includePackageNames, asSourceDecl) + " " + sig;
        }
        else {
            sig += type().toSignature(includePackageNames, asSourceDecl);
        }
        return sig;
    }

    /**
     * Return a String representation of this member.
     * @param includePackageNames If true, then use fully qualified class names.
     * @param asSourceDecl If true, then use a format corresponding with the source level
     * declaration of this member.
     * @param includeParent If true, include the class owning this member in the string.
     * @return a String representation of the member.
     */
    public String toString(boolean includePackageNames, boolean asSourceDecl, boolean includeParent) {
        String result  = parmsToString(includePackageNames, asSourceDecl);
        String name    = name();
        String retType = type().toSignature(includePackageNames, asSourceDecl);
        if (includeParent) {
            name = parent().toSignature(includePackageNames, asSourceDecl)+"."+name;
        }
        if (asSourceDecl) {
            result = retType+" "+name+result;
        }
        else {
            result = name+result+retType;
        }
        return result;
    }

    /**
     * Return the list of the explicit parameters. This does not include the implicit
     * receiver for non-static methods.
     */
    public Type[] getParms() {
        return parms;
    }

    /**
     * Flag this method as one generated by the translator.
     * @param flag
     */
    public void setSynthetic() {
        this.synthetic = true;
    }

    /**
     * Return true if this is a method generated by the translator.
     * @return
     */
    public boolean isSynthetic() {
        return synthetic;
    }

    /**
     * Return the metrics of the original javac generated bytecode for this method.
     */
    public CodeMetrics getJavacMetrics() {
        return javacMetrics;
    }

    /**
     * Return the metrics of the generated Squawk bytecode for this method.
     */
    public CodeMetrics getSquawkMetrics() {
        return squawkMetrics;
    }

    /**
     * Return whether or not this method has a body. This will only be true
     * if the method is abstract or native.
     * @return
     */
    public boolean hasIR() {
//        return !(isAbstract() || isNative());
        return ir != null;
    }

    /**
     * Get the IR of the method.
     * @return the IR of the method.
     */
    public InstructionList getIR() {
        return ir;
    }

    /**
     * Replace the IR for this method with the IR from another method.
     * @param other
     */
    public void replaceIR(Method other) {
        this.ir = other.ir;
    }

   /* ------------------------------------------------------------------------ *\
    *             Classfile conversion methods                                 *
   \* ------------------------------------------------------------------------ */

   /**
    * Set the BytecodeHolder containing the unparsed Java bytecode.
    * @param holder the BytecodeHolder containing the unparsed Java bytecode.
    */
   public void setHolder(BytecodeHolder holder) {
       this.holder = holder;
   }

   /**
    * Convert the unparsed Java bytecode into a graph of intermediate instructions.
    * @return the built IR.
    * @exception LinkageException
    */
   private InstructionList buildIR() throws LinkageException {
       InstructionList ir = null;
       if (holder != null) {
           try {
               Assert.that(vm().verifyCount++ == 0); // check that serialization is working
               ir = holder.buildIR();
           } finally {
               Assert.that(vm().verifyCount-- > 0);
           }
       }
       return ir;
   }

   /**
    * Convert the method into its intermediate form and return the result.
    * @return the intermediate form of this method.
    */
   public void convert() throws LinkageException {
       if (isSynthetic()) {
           Assert.that(ir != null);
           Assert.that(holder == null);
       } else {
           Assert.that(ir == null);
           Assert.that(holder != null);
           ir = buildIR();
           javacMetrics = holder.getJavacMetrics();

           if (vm().verifyingOnly()) {
               ir = null;
           }

           // Clear the bytecode holder so that it can be gc'ed
           holder = null;
       }
   }

   /**
    * Convert the body of this method to return the receiver. This is used to
    * create default constructors for classes that have no constructors.
    * @param vm The Translator context.
    */
   public void convertToDefaultConstructorMethod(Translator vm) throws LinkageException {
       int flags = ExecutionFrame.FLAG_ALLOCATE_STACK_VARIABLES |
                   ExecutionFrame.FLAG_REUSABLE_LOCAL_VARIABLES |
                   ExecutionFrame.FLAG_MERGE_LOCAL_VARIABLES;
       InstructionFactory ifactory = new InstructionFactory(vm, this);
       ExecutionFrame frame = new ExecutionFrame(vm,
                                                 this,
                                                 false,
                                                 null,
                                                 ifactory,
                                                 1,
                                                 new Local[1],
                                                 flags,
                                                 null);
       InstructionList ilist       = new InstructionList(frame, true);

       // Change the receiver type from INITOBJECT if this is an <init> method
       if (name() == vm.INIT) {
           frame.renameInitTo(parent());
       }

       Type sclass = parent().superClass();

       if (sclass != null) {
           // Call parent's <init> - there had better be a default constructor
           Method sinit = sclass.lookupMethod(vm.INIT, vm.ZEROTYPES, sclass, null, true);
           Assert.that(sinit != null);

           // 'load_0'
           ilist.appendPush(ifactory.createLoadLocal(frame.getLocal(parent(), 0), parent()));

           // 'invokeinit'
           ilist.appendPush(ifactory.createInvoke(sinit, new Instruction[] { frame.pop(parent()) }, Invoke.Form.INIT));

           // 'pop'
           ilist.append(ifactory.createPop(frame.pop(vm.OBJECT)));
       }

       // 'load_0'
       ilist.appendPush(ifactory.createLoadLocal(frame.getLocal(parent(), 0), parent()));

       // 'return'
       Instruction returnValue = frame.pop(parent());
       ilist.append(ifactory.createReturn(returnValue));

       GraphTransformer.transform(this, ilist);
       ir = ilist;
   }

   /**
    * Convert the body of this method to throw an exception.
    * @param vm The Translator context.
    * @param throwable The subclass of java.lang.Throwable to be thrown.
    * @param initString An optional string argument that will be passed to the
    * constructor of the exception.
    */
   public void convertToThrowsExceptionMethod(Translator vm, Type throwable, String initString) throws LinkageException {
       // Ensure the exception class is loaded
       throwable.load();

       // Get the relevant constructor for the exception.
       Method init;
       if (initString == null) {
           init = throwable.lookupMethod(vm.INIT, vm.ZEROTYPES, throwable, null, false);
           Assert.that(init != null, "LinkageError must include \"<init>();\"");
       }
       else {
           init = throwable.lookupMethod(vm.INIT, vm.internTypeList(new Type[] { vm.STRING }), throwable, null, false);
           Assert.that(init != null, "LinkageError must include \"<init>(String message);\" (class = "+throwable.name()+")");
       }

       int maxStack  = (initString == null ? 1 : 2);
       int maxLocals = 2 + (parms.length * 2);
       InstructionFactory ifactory = new InstructionFactory(vm, this);
       int flags = ExecutionFrame.FLAG_ALLOCATE_STACK_VARIABLES |
                   ExecutionFrame.FLAG_REUSABLE_LOCAL_VARIABLES |
                   ExecutionFrame.FLAG_MERGE_LOCAL_VARIABLES;
       ExecutionFrame frame = new ExecutionFrame(vm,
                                                 this,
                                                 false,
                                                 null,
                                                 ifactory,
                                                 maxStack,
                                                 new Local[maxLocals],
                                                 flags,
                                                 null);
       InstructionList ilist = new InstructionList(frame, true);

       // Change the receiver type from INITOBJECT if this is an <init> method
       if (name() == vm.INIT) {
           frame.renameInitTo(parent());
       }

       // 'new'
       ilist.appendPush(ifactory.createNewObject(throwable));
       if (initString != null) {
           // 'object "<initString>"'
           ilist.appendPush(ifactory.createLoadConstantObject(vm.STRING, initString));
       }
       Instruction[] parms;
       int ip = ilist.getLogicalIP();
       if (initString == null) {
           parms = new Instruction[] { frame.pop(vm.NULLOBJECT, ip) };
       }
       else {
           parms = new Instruction[2];
           parms[1] = frame.pop(vm.STRING, ip);
           parms[0] = frame.pop(vm.NULLOBJECT, ip);
       }
       // 'invokeinit'
       ilist.appendPush(ifactory.createInvoke(init, parms, Invoke.Form.INIT));
       // 'throw'
       ilist.append(ifactory.createThrow(frame.pop(vm.THROWABLE, ilist.getLogicalIP())));
       GraphTransformer.transform(this, ilist);
       ir = ilist;
   }


    /**
     * Private constructor.
     * @param parent The class in which this method is defined.
     * @param name The method's name.
     * @param flags The method's flags.
     * @param type The method's return type.
     * @param parms The method's parameter types.
     */
    private Method(Type parent, String name, int flags, Type returnType, Type[] parmTypes) {
        super(parent, name, returnType, flags);
        this.parms = parmTypes;

        this.squawkMetrics = new CodeMetrics(0, 0, 0, 0);
        Translator vm = parent.vm();
        squawkMetrics.usesLong = returnType.isLong();
/*if[FLOATS]*/
        squawkMetrics.usesFloatOrDouble = returnType.isFloatOrDouble();
/*end[FLOATS]*/
        for (int i = 0; i != parms.length; i++) {
            Type parm = parms[i];
            squawkMetrics.usesLong |= parm.isLong();
/*if[FLOATS]*/
            squawkMetrics.usesFloatOrDouble |= parm.isFloatOrDouble();
/*end[FLOATS]*/
        }
    }

   /* ------------------------------------------------------------------------ *\
    *                         Suite pruning                                    *
   \* ------------------------------------------------------------------------ */

    public void mark() {
        if (!includeInSuite()) {
            vm().traceMark(this);
            vm().markDepth++;
            setFlag(JVMConst.ACC_INCLUDE);

            // Mark parent
            parent().mark();

            // Return now if the parent has a LinkageError and this is not
            // the <clinit> method
//            if (name() != vm().CLINIT && parent().getLinkageError() != null) {
//                vm().markDepth--;
//                return;
//            }

            // Mark parameter types
            for (int i = 0; i != parms.length; i++) {
                parms[i].mark();
            }

            // Mark return type
            type().mark();

            // Mark overridden method (if any)
            if (overridden != null) {
                overridden.mark();
            }

            if (!vm().pruneSuite() || !parent().isProxy()) {

//                if (!parent().isInterface()) {
//                    // Creates a stub for each abstract and native method
//                    // that simply throws an AbstractMethodError or UnsatisfiedLinkError.
//                    try {
//                        if (isAbstract()) {
//                            convertToThrowsExceptionMethod(vm(), vm().ABSTRACTMETHODERROR, name());
//                        } else if (isNative()) {
//                            convertToThrowsExceptionMethod(vm(), vm().UNSATISFIEDLINKERROR, name());
//                        }
//                    } catch (LinkageException le) {
//                        le.printStackTrace();
//                        Assert.shouldNotReachHere();
//                    }
//                }

                // Go through instructions...
                if (hasIR()) {
                    Assert.that(ir != null);
                    for (Instruction inst = ir.head(); inst != null; inst = inst.getNext()) {
                        Type refType = inst.getReferencedType();
                        if (refType != null) {
                            refType.mark();
                        }

                        if (inst instanceof FieldInstruction) {
                            ((FieldInstruction)inst).field().mark();
                        }
                        else if (inst instanceof Invoke) {
                            ((Invoke)inst).method().mark();
                        }
                        else if (inst instanceof NewObject) {
                            // This is for a TCK test that allocates an object
                            // but does not call its constructor:
                            //    javasoft.sqe.tests.vm.instr.newX.new010.new01001m1.new01001m1.run
                            if (inst.type() instanceof TypeProxy) {
                                TypeProxy proxy = (TypeProxy)inst.type();
                                inst.changeType(proxy.getProxy());
                                inst.type().mark();
                            }
                        }
                    }
                }
            }

            vm().markDepth--;
        }
    }
}
