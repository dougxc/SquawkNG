/*
 * @(#)Method.java                      1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ci;

import java.util.ArrayList;
import java.util.List;
import javac1.Assert;
import javac1.BasicType;

/**
 * Represents a method in the compiler interface.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Method extends Obj {
    /**
     * The access flags of this method.
     */
    private AccessFlags flags;
    
    /**
     * The maximum size of the expression stack.
     */
    private int maxStack;
    
    /**
     * The size of the local variable array.
     */
    private int maxLocals;
    
    /**
     * The size of the bytecodes.
     */
    private int codeSize;
    
    /**
     * The class that holds this method.
     */
    private InstanceKlass holder;
    
    /**
     * The bytecodes of this method.
     */
    private byte[] code;
    
    /**
     * The list of exception handlers in this method.
     */
    private List xhandlers;
    
    /**
     * The number of exception handlers in this method.
     */
    private int handlerCount;
    
    /**
     * The constant pool associated with this method.
     */
    private ConstantPool cpool;
    
    /**
     * The name of this method.
     */
    private Symbol name;
    
    /**
     * The type signature of this method.
     */
    private Symbol signature;
    
    /**
     * The types of the arguments.
     */
    private List argTypes;
    
    /**
     * The return type of this method.
     */
    private int returnType;
    
    /**
     * Constructs a new object representing a method.
     *
     * @param  oop  the method
     */
    protected Method(Object oop) {
        super(oop);
        checkIsLoaded();
        this.flags = new AccessFlags(JVM.getMethodFlags(oop));
        this.maxStack = JVM.getMaxStack(oop);
        this.maxLocals = JVM.getMaxLocals(oop);
        this.codeSize = JVM.getCodeSize(oop);
        this.holder = new InstanceKlass(JVM.getMethodHolder(oop));
        this.handlerCount = JVM.getExceptionTable(oop).length / 4;
        this.name = new Symbol(JVM.getMethodName(oop));
        this.signature = new Symbol(JVM.getMethodSignature(oop));
        this.argTypes = parseSignature(signature);
        this.code = null;
    }
    
    /**
     * Constructs a new object representing an unloaded method.
     *
     * @param  holder     class holding the method
     * @param  name       name of the method
     * @param  signature  signature of the method
     */
    protected Method(InstanceKlass holder, Symbol name, Symbol signature) {
        super(null);
        this.holder = holder;
        this.name = name;
        this.signature = signature;
        this.argTypes = parseSignature(signature);
        this.code = null;
    }
    
    /**
     * Looks up the method with the specified name and signature.
     *
     * @param   klass      name of the declaring class
     * @param   name       name of the method
     * @param   signature  signature of the method
     * @return  the method
     */
    public static Method lookup(String klass, String name, String signature) {
        Object method = JVM.lookupMethod(klass, name, signature);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(method != null, "intended for loaded methods only");
        }
        return new Method(method);
    }
    
    /**
     * Converts the signature string into an array of types.
     */
    private static List parseSignature(Symbol signature) {
        List argTypes = new ArrayList();
        char[] str = signature.toString().toCharArray();
        int i = 1;
        while (str[i] != ')') {
            int type = BasicType.valueOf(str[i]);
            argTypes.add(new Integer(type));
            while (str[i] == '[') {
                i++;
                while ((str[i] >= '0') && (str[i] <= '9')) i++;
            }
            if (str[i] == 'L') {
                while (str[i] != ';') i++;
            }
            i++;
        }
        argTypes.add(new Integer(BasicType.valueOf(str[i + 1])));
        return argTypes;
    }
    
    /**
     * Returns the class that holds this method.
     *
     * @return  the class holding this method
     */
    public InstanceKlass getHolder() {
        return holder;
    }
    
    /**
     * Returns the name of this method.
     *
     * @return  name of this method
     */
    public Symbol getName() {
        return name;
    }
    
    /**
     * Returns the signature of this method.
     *
     * @return  signature of this method
     */
    public Symbol getSignature() {
        return signature;
    }
    
    /**
     * Returns the access flags of this method.
     *
     * @return  access flags of this method
     */
    public AccessFlags getFlags() {
        checkIsLoaded();
        return flags;
    }
    
    /**
     * Loads the bytecodes into the code array.
     */
    private void loadCode() {
        checkIsLoaded();
        code = JVM.getCode(getOop());
        int[] table = JVM.getExceptionTable(getOop());
        xhandlers = new ArrayList();
        for (int i = 0; i < table.length; i += 4) {
            xhandlers.add(new ExceptionHandler(holder, table[i],
                table[i + 1], table[i + 2], table[i + 3]));
        }
        cpool = new ConstantPool(JVM.getMethodConstants(getOop()));
    }
    
    /**
     * Returns the size of the bytecodes.
     *
     * @return  the code size
     */
    public int getCodeSize() {
        return codeSize;
    }
    
    /**
     * Loads the bytecodes on demand and returns them.
     *
     * @return  the code array
     */
    public byte[] getCode() {
        if (code == null) {
            loadCode();
        }
        return code;
    }
    
    /**
     * Returns the list of exception handlers of this method.
     *
     * @return  list of exception handlers
     */
    public List getExceptionHandlers() {
        if (code == null) {
            loadCode();
        }
        return xhandlers;
    }
    
    /**
     * Tests if this method has exception handlers.
     *
     * @return  whether the method has handlers or not
     */
    public boolean hasExceptionHandlers() {
        checkIsLoaded();
        return handlerCount > 0;
    }
    
    /**
     * Returns the constant pool associated with this method.
     *
     * @return  the constant pool
     */
    public ConstantPool getConstants() {
        if (code == null) {
            loadCode();
        }
        return cpool;
    }
    
    /**
     * Returns whether or not the method is abstract. An abstract declaration
     * merely defines the calling signature and return type.
     *
     * @return  whether this method is abstract or not
     */
    public boolean isAbstract() {
        return getFlags().isAbstract();
    }

    /**
     * Returns whether the method itself is declared final or not.
     *
     * @return  whether this method is declared final or not
     * @see     #isFinalMethod()
     */
    public boolean isFinal() {
        return getFlags().isFinal();
    }

    /**
     * Returns whether or not the method is native. In this case its
     * implementation is not provided by Java language code.
     *
     * @return  whether this method is native or not
     */
    public boolean isNative() {
        return getFlags().isNative();
    }

    /**
     * Returns whether or not the method is static. Static methods are regarded
     * as belonging to the class itself rather than operating within instances
     * of the class.
     *
     * @return  whether this method is static or not
     */
    public boolean isStatic() {
        return getFlags().isStatic();
    }

    /**
     * Returns whether the method was compiled strictfp, which changes the
     * floating-point rounding behavior in certain situations on x86 processors.
     *
     * @return  whether this method is strict or not
     */
    public boolean isStrict() {
        return getFlags().isStrict();
    }

    /**
     * Returns whether or not the method is synchronized. Such a method will
     * acquire a monitor lock before it executes.
     *
     * @return  whether this method is synchronized or not
     */
    public boolean isSynchronized() {
        return getFlags().isSynchronized();
    }

    /**
     * Tests if the method or its holder is declared final. A final method
     * cannot be overridden.
     *
     * @return  whether the method is final or not
     */
    public boolean isFinalMethod() {
        return isFinal() || holder.isFinal();
    }
    
    /**
     * Tests if the method contains loops.
     *
     * @return  whether the method has loops or not
     */
    public boolean hasLoops() {
        checkIsLoaded();
        return JVM.hasLoops(getOop());
    }
    
    /**
     * Returns the size of the arguments without any receiver.
     *
     * @return  size of arguments without the receiver
     */
    public int getArgSizeNoReceiver() {
        int argSize = 0;
        for (int i = 0; i < argTypes.size() - 1; i++) {
            Integer type = (Integer) argTypes.get(i);
            argSize += BasicType.TYPE_TO_SIZE[type.intValue()];
        }
        return argSize;
    }
    
    /**
     * Returns the size of the arguments including the receiver if any.
     *
     * @return  size of arguments including the receiver
     */
    public int getArgSize() {
        return getArgSizeNoReceiver() + (isStatic() ? 0 : 1);
    }
    
    /**
     * Returns the types of the arguments of this method.
     *
     * @return  list of argument types
     */
    public List getArgTypes() {
        return argTypes;
    }
    
    /**
     * Returns the return type of the method.
     *
     * @return  the return type
     */
    public int getReturnType() {
        return ((Integer) argTypes.get(argTypes.size() - 1)).intValue();
    }
    
    /**
     * Returns the size of the local variable array of this method.
     *
     * @return  total size of local variables
     */
    public int getMaxLocals() {
        checkIsLoaded();
        return JVM.getMaxLocals(getOop());
    }

    /**
     * Returns the maximum size of the expression stack.
     *
     * @return  maximum size of the expression stack
     */
    public int getMaxStack() {
        checkIsLoaded();
        return JVM.getMaxStack(getOop());
    }
    
    /**
     * Returns the intrinsic identification number of this method.
     *
     * @return  the intrinsic identification number
     * @see     javac1.ir.instr.Intrinsic
     */
    public int getIntrinsicId() {
        checkIsLoaded();
        return JVM.getIntrinsicId(getOop());
    }
    
    /**
     * Returns the address of this method's native code.
     *
     * @return  address of the native code
     */
    public int getNativeEntry() {
        checkIsLoaded();
        return JVM.getNativeEntry(getOop());
    }
    
    /**
     * Returns a string describing this method. The string is composed of the
     * class declaring the method, the method name and its signature.
     *
     * @return  string representation of the method
     */
    public String toString() {
        return holder.toString() + "." + name.toString() + signature.toString();
    }
}
