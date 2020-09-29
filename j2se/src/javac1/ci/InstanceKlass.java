/*
 * @(#)InstanceKlass.java               1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ci;

/**
 * Represents an instance class in the compiler interface.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class InstanceKlass extends Klass {
    /**
     * The access flags of this class.
     */
    private AccessFlags flags;
    
    /**
     * The size helper of this class.
     */
    private int sizeHelper;
    
    /**
     * Whether or not this class has been initialized.
     */
    private boolean initialized;
    
    /**
     * Constructs a new object representing an instance class.
     *
     * @param  oop  the ordinary object pointer
     */
    protected InstanceKlass(Object oop) {
        super(oop);
        flags = new AccessFlags(JVM.getKlassFlags(oop));
        sizeHelper = JVM.getSizeHelper(oop);
        initialized = JVM.isKlassInitialized(oop);
    }
    
    /**
     * Constructs a new object representing an unloaded instance class.
     *
     * @param  name  the name of the class
     */
    protected InstanceKlass(Symbol name) {
        super(name);
        this.initialized = false;
    }
    
    /**
     * Tests if the class has already been initialized.
     *
     * @return  whether the class is initialized or not
     */
    public boolean isInitialized() {
        checkIsLoaded();
        return JVM.isKlassInitialized(getOop());
    }
    
    /**
     * Returns the access flags of this instance class.
     *
     * @return  access flags of this class
     */
    public AccessFlags getFlags() {
        checkIsLoaded();
        return flags;
    }
    
    /**
     * Tests if the class has been declared final and thus cannot be subclassed.
     *
     * @return  whether the class is declared final or not
     */
    public boolean isFinal() {
        return getFlags().isFinal();
    }
    
    /**
     * Tests if the class contains a finalizer.
     *
     * @return  whether the class has a finalizer or not
     */
    public boolean hasFinalizer() {
        return getFlags().hasFinalizer();
    }
    
    /**
     * Returns the size helper of this class.
     *
     * @return  the size helper
     */
    public int getSizeHelper() {
        return sizeHelper;
    }
    
    /**
     * Returns the constant pool of this class.
     *
     * @return  the constant pool
     */
    public ConstantPool getConstants() {
        checkIsLoaded();
        return new ConstantPool(JVM.getKlassConstants(getOop()));
    }
    
    /**
     * Returns the array of all the member methods in this class.
     *
     * @return  array of member methods
     */
    public Method[] getMethods() {
        checkIsLoaded();
        Object[] oops = JVM.getMethods(getOop());
        int count = oops.length;
        Method[] methods = new Method[count];
        for (int i = 0; i < count; i++) {
            methods[i] = new Method(oops[i]);
        }
        return methods;
    }
}
