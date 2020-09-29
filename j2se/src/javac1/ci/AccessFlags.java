/*
 * @(#)AccessFlags.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ci;

/**
 * Encapsulates the access flags of a method or a class.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class AccessFlags {
    /**
     * The flag constant for items visible to everyone.
     */
    private static final int JVM_ACC_PUBLIC = 0x0001;
    
    /**
     * The flag constant for items visible only to the defining class.
     */
    private static final int JVM_ACC_PRIVATE = 0x0002;
    
    /**
     * The flag constant for items visible to subclasses.
     */
    private static final int JVM_ACC_PROTECTED = 0x0004;
    
    /**
     * The flag constant for static items.
     */
    private static final int JVM_ACC_STATIC = 0x0008;
    
    /**
     * The flag constant for items that must not be subclassed or overridden.
     */
    private static final int JVM_ACC_FINAL = 0x0010;
    
    /**
     * The flag constant for synchronized methods.
     */
    private static final int JVM_ACC_SYNCHRONIZED = 0x0020;
    
    /**
     * The flag constant for volatile variables.
     */
    private static final int JVM_ACC_VOLATILE = 0x0040;
    
    /**
     * The flag constant for transient variables.
     */
    private static final int JVM_ACC_TRANSIENT = 0x0080;
    
    /**
     * The flag constant for native methods.
     */
    private static final int JVM_ACC_NATIVE = 0x0100;
    
    /**
     * The flag constant for interfaces.
     */
    private static final int JVM_ACC_INTERFACE = 0x0200;
    
    /**
     * The flag constant for abstract classes or methods.
     */
    private static final int JVM_ACC_ABSTRACT = 0x0400;
    
    /**
     * The flag constant for methods with strict floating point operations.
     */
    private static final int JVM_ACC_STRICT  = 0x0800;
    
    /**
     * The flag constant for classes that define a finalizer.
     */
    private static final int JVM_ACC_HAS_FINALIZER = 0x40000000;
    
    /**
     * The integer representation of the access flags.
     */
    private int flags;
    
    /**
     * Constructs a new object encapsulating the specified flags.
     *
     * @param  flags  integer representation of the flags
     */
    protected AccessFlags(int flags) {
        this.flags = flags;
    }
    
    /**
     * Tests if the item is visible to everyone.
     *
     * @return  whether the item is public or not
     */
    public boolean isPublic() {
        return (flags & JVM_ACC_PUBLIC) != 0;
    }
    
    /**
     * Tests if the item is visible to the defining class only.
     *
     * @return  whether the item is private or not
     */
    public boolean isPrivate() {
        return (flags & JVM_ACC_PRIVATE) != 0;
    }
    
    /**
     * Tests if the item is visible to subclasses.
     *
     * @return  whether the item is protected or not
     */
    public boolean isProtected() {
        return (flags & JVM_ACC_PROTECTED) != 0;
    }
    
    /**
     * Tests if the method or field is static.
     *
     * @return  whether the item is static or not
     */
    public boolean isStatic() {
        return (flags & JVM_ACC_STATIC) != 0;
    }
    
    /**
     * Tests if the class or method is declared final.
     *
     * @return  whether this method is final or not
     */
    public boolean isFinal() {
        return (flags & JVM_ACC_FINAL) != 0;
    }
    
    /**
     * Tests if the method is synchronized.
     *
     * @return  whether the method is synchronized or not
     */
    public boolean isSynchronized() {
        return (flags & JVM_ACC_SYNCHRONIZED) != 0;
    }
    
    /**
     * Tests if the variable is volatile.
     *
     * @return  whether the variable is volatile or not
     */
    public boolean isVolatile() {
        return (flags & JVM_ACC_VOLATILE) != 0;
    }
    
    /**
     * Tests if the variable is transient.
     *
     * @return  whether the variable is transient or not
     */
    public boolean isTransient() {
        return (flags & JVM_ACC_TRANSIENT) != 0;
    }
    
    /**
     * Tests if the method is native.
     *
     * @return  whether the method is native or not
     */
    public boolean isNative() {
        return (flags & JVM_ACC_NATIVE) != 0;
    }
    
    /**
     * Tests if the class is an interface.
     *
     * @return  whether or not the class is an interface.
     */
    public boolean isInterface() {
        return (flags & JVM_ACC_INTERFACE) != 0;
    }
    
    /**
     * Tests if the class or method is abstract.
     *
     * @return  whether the class or method is abstract or not
     */
    public boolean isAbstract() {
        return (flags & JVM_ACC_ABSTRACT) != 0;
    }
    
    /**
     * Tests if the method was compiled strictfp.
     *
     * @return  whether the method is strict or not
     */
    public boolean isStrict() {
        return (flags & JVM_ACC_STRICT) != 0;
    }
    
    /**
     * Tests if the class defines a finalizer.
     *
     * @return  whether the class has a finalizer or not
     */
    public boolean hasFinalizer() {
        return (flags & JVM_ACC_HAS_FINALIZER) != 0;
    }
}
