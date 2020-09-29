/*
 * @(#)JVM.java                         1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ci;

/**
 * Represents the interface between the JVM and the compiler. The native methods
 * declared by this class are linked to the JVM and registered during start-up.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class JVM {
    /**
     * Compiles the bytecodes of the specified method. This method is called
     * from within the JVM when the invocation counter of a method overflows
     * and induces JavaC1 to compile that method.
     *
     * @param  method  the method to be compiled
     * @param  osrBci  bytecode index for OSR
     * @see    javac1.JavaC1#compileMethod(Method, int)
     */
    public static void compileMethod(Object method, int osrBci) {
        javac1.JavaC1.compileMethod(new Method(method), osrBci);
    }
    
    /**
     * Returns the canonical holder of the specified field.
     *
     * @param   holder     the declared holder
     * @param   name       name of the field
     * @param   signature  signature of the field
     * @return  the canonical holder
     */
    public static native Object getCanonicalHolder(Object holder, Object name,
            Object signature);

    /**
     * Returns the bytecodes of the specified method.
     *
     * @param   method  the method
     * @return  bytecodes of the method
     */
    public static native byte[] getCode(Object method);

    /**
     * Returns the size of the bytecodes of the specified method.
     *
     * @param   method  the method
     * @return  size of the bytecodes
     */
    public static native int getCodeSize(Object method);

    /**
     * Retrieves a double-precision floating-point number from the constant
     * pool.
     *
     * @param   cpool  the constant pool
     * @param   index  index of the entry
     * @return  the value at the specified index
     */
    public static native double getDoubleAt(Object cpool, int index);

    /**
     * Returns the exception table for the specified method.
     *
     * @param   method  the method
     * @return  the exception table
     */
    public static native int[] getExceptionTable(Object method);

    /**
     * Returns the offset of the specified field.
     *
     * @param   holder     class that contains the field
     * @param   name       name of the field
     * @param   signature  signature of the field
     * @return  offset of the field
     */
    public static native int getFieldOffset(Object holder, Object name,
            Object signature);

    /**
     * Retrieves a single-precision floating-point number from the constant
     * pool.
     *
     * @param   cpool  the constant pool
     * @param   index  index of the entry
     * @return  the value at the specified index
     */
    public static native float getFloatAt(Object cpool, int index);

    /**
     * Returns the index of the holder class referenced at the specified index.
     *
     * @param   cpool  the constant pool
     * @param   index  index of the entry
     * @return  index of the holder class
     */
    public static native int getHolderIndexAt(Object cpool, int index);

    /**
     * Retrieves an integer value from the constant pool.
     *
     * @param   cpool  the constant pool
     * @param   index  index of the entry
     * @return  the value at the specified index
     */
    public static native int getIntAt(Object cpool, int index);

    /**
     * Returns the intrinsic identification number of the specified method.
     *
     * @param   method  the method
     * @return  the instrinsic identification number
     */
    public static native int getIntrinsicId(Object method);

    /**
     * Retrieves a class from the constant pool.
     *
     * @param   cpool  the constant pool
     * @param   index  index of the entry
     * @return  the class at the specified index
     */
    public static native Object getKlassAt(Object cpool, int index);
    
    /**
     * Returns the constant pool of the specified class.
     *
     * @param   method  the class
     * @return  the constant pool
     */
    public static native Object getKlassConstants(Object method);
    
    /**
     * Returns the access flags of the specified class.
     *
     * @param   klass  the class
     * @return  access flags of the specified class
     */
    public static native int getKlassFlags(Object klass);
    
    /**
     * Returns the name of the specified class.
     *
     * @param   klass  the class
     * @return  name of the specified class
     */
    public static native Object getKlassName(Object klass);

    /**
     * Retrieves the name of a class from the constant pool.
     *
     * @param   cpool  the constant pool
     * @param   index  index of the entry
     * @return  name of the class at the specified index
     */
    public static native Object getKlassNameAt(Object cpool, int index);

    /**
     * Retrieves a long integer value from the constant pool.
     *
     * @param   cpool  the constant pool
     * @param   index  index of the entry
     * @return  the value at the specified index
     */
    public static native long getLongAt(Object cpool, int index);

    /**
     * Returns the maximum size of the local variables of the specified method.
     *
     * @param   method  the method
     * @return  maximum size of the local variables
     */
    public static native int getMaxLocals(Object method);

    /**
     * Returns the maximum size of the expression stack of the specified method.
     *
     * @param   method  the method
     * @return  maximum size of the expression stack
     */
    public static native int getMaxStack(Object method);

    /**
     * Retrieves the name of a method or field from the constant pool.
     *
     * @param   cpool  the constant pool
     * @param   index  index of the entry
     * @return  name of the member at the specified index
     */
    public static native Object getMemberNameAt(Object cpool, int index);
    
    /**
     * Returns the array of all the member methods of the specified class.
     *
     * @param   klass  the class
     * @return  array of member methods
     */
    public static native Object[] getMethods(Object klass);

    /**
     * Retrieves a method from the constant pool.
     *
     * @param   cpool     the constant pool
     * @param   accessor  the accessing class
     * @param   holder    holder of the method
     * @param   index     index of the entry
     * @param   bc        the bytecode
     * @return  the method at the specified index
     */
    public static native Object getMethodAt(Object cpool, Object accessor,
            Object holder, int index, int bc);

    /**
     * Returns the constant pool of the class that declares the specified
     * method.
     *
     * @param   method  the method
     * @return  the constant pool
     */
    public static native Object getMethodConstants(Object method);

    /**
     * Returns the access flags of the specified method.
     *
     * @param   method  the method
     * @return  access flags of the method
     */
    public static native int getMethodFlags(Object method);

    /**
     * Returns the class that contains the specified method.
     *
     * @param   method  the method
     * @return  the holder of the method
     */
    public static native Object getMethodHolder(Object method);

    /**
     * Returns the name of the specified method.
     *
     * @param   method  the method
     * @return  name of the specified method
     */
    public static native Object getMethodName(Object method);

    /**
     * Returns the signature of the specified method.
     *
     * @param   method  the method
     * @return  signature of the specified method
     */
    public static native Object getMethodSignature(Object method);
    
    /**
     * Returns the address of the native code of the specified method.
     *
     * @return  address of the native code
     */
    public static native int getNativeEntry(Object method);

    /**
     * Retrieves the signature of a method or field from the constant pool.
     *
     * @param   cpool  the constant pool
     * @param   index  index of the entry
     * @return  signature at the specified index
     */
    public static native Object getSignatureAt(Object cpool, int index);

    /**
     * Returns the size helper of the specified class.
     *
     * @param   klass  the class
     * @return  size helper of the class
     */
    public static native int getSizeHelper(Object klass);

    /**
     * Retrieves a string constant from the constant pool.
     *
     * @param   cpool  the constant pool
     * @param   index  index of the entry
     * @return  string at the specified index
     */
    public static native Object getStringAt(Object cpool, int index);
    
    /**
     * Returns the string representation of the specified symbol.
     *
     * @param   symbol  the symbol
     * @return  the string representation
     */
    public static native String getSymbolString(Object symbol);

    /**
     * Returns the tag of the specified constant pool entry.
     *
     * @param   cpool  the constant pool
     * @param   index  index of the entry
     * @return  the tag of the entry
     */
    public static native int getTagAt(Object cpool, int index);

    /**
     * Tests if the specified method contains any loops.
     *
     * @param   method  the method
     * @return  whether the method has loops or not
     */
    public static native boolean hasLoops(Object method);
    
    /**
     * Tests if the specified class is an array class.
     *
     * @param   klass  the class
     * @return  whether the class is an array class or not
     */
    public static native boolean isArrayKlass(Object klass);
    
    /**
     * Tests if the specified class is an instance class.
     *
     * @param   klass  the class
     * @return  whether the class is an instance class or not
     */
    public static native boolean isInstanceKlass(Object klass);

    /**
     * Tests if the specified class has already been initialized.
     *
     * @param   klass  the class
     * @return  whether the class is initialized or not
     */
    public static native boolean isKlassInitialized(Object klass);
    
    /**
     * Looks up the class with the specified name.
     *
     * @param   name  name of the class
     * @return  the class
     */
    public static native Object lookupKlass(String name);

    /**
     * Looks up the method with the specified name and signature.
     *
     * @param   klass      name of the declaring class
     * @param   name       name of the method
     * @param   signature  signature of the method
     * @return  the method
     */
    public static native Object lookupMethod(String klass, String name,
            String signature);
    
    /**
     * Constructs an object array class from the specified element class.
     *
     * @param   elemKlass  the element class
     * @return  the object array class
     */
    public static native Object makeObjectArrayKlass(Object elemKlass);
    
    /**
     * Constructs an type array class from the specified element type.
     *
     * @param   elemType  the basic element type
     * @return  the type array class
     */
    public static native Object makeTypeArrayKlass(int elemType);
    
    /**
     * Tests if this field can be accessed without causing link errors.
     *
     * @param   cpool  the constant pool
     * @param   index  index into the constant pool
     * @param   bc     the bytecode
     * @return  whether field can be accessed or not
     */
    public static native boolean willLink(Object cpool, int index, int bc);

    /**
     * Don't let anyone instantiate this class.
     */
    private JVM() {}
}
