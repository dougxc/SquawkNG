/*
 * @(#)ConstantPool.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ci;

import javac1.Assert;
import javac1.BasicType;

/**
 * Represents the constant pool of a class.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class ConstantPool extends Obj {
    /**
     * A constant pool entry type constant.
     */
    public final static int
            JVM_CONSTANT_INVALID     =  0, JVM_CONSTANT_UTF8        =  1,
            JVM_CONSTANT_UNICODE     =  2, JVM_CONSTANT_INTEGER     =  3,
            JVM_CONSTANT_FLOAT       =  4, JVM_CONSTANT_LONG        =  5,
            JVM_CONSTANT_DOUBLE      =  6, JVM_CONSTANT_CLASS       =  7,
            JVM_CONSTANT_STRING      =  8, JVM_CONSTANT_FIELDREF    =  9,
            JVM_CONSTANT_METHODREF   = 10, JVM_CONSTANT_IMETHODREF  = 11,
            JVM_CONSTANT_NAMEANDTYPE = 12;
    
    /**
     * A constant pool entry type constant.
     */
    public final static int JVM_CONSTANT_UNRESOLVED_STRING = 102;
    
    /**
     * Constructs a new object representing the constant pool of a class.
     *
     * @param  cpool  the constant pool
     */
    protected ConstantPool(Object cpool) {
        super(cpool);
    }
    
    /**
     * Retrieves a class from the constant pool.
     *
     * @param   index  index of the constant pool entry
     * @return  the class at the specified index
     */
    public Klass getKlassAt(int index) {
        Object klass = JVM.getKlassAt(getOop(), index);
        if (klass != null) {
            if (JVM.isArrayKlass(klass)) {
                return new ArrayKlass(klass);
            } else if (JVM.isInstanceKlass(klass)) {
                return new InstanceKlass(klass);
            } else {
                Assert.shouldNotReachHere();
                return new Klass(klass);
            }
        } else {
            Symbol name = new Symbol(JVM.getKlassNameAt(getOop(), index));
            String str = name.toString();
            if (str.startsWith("[")) {
                return new ArrayKlass(name);
            } else {
                return new InstanceKlass(name);
            }
        }
    }
    
    /**
     * Retrieves a method from the constant pool.
     *
     * @param   accessor  the accessing class
     * @param   index     index of the constant pool entry
     * @param   bc        the bytecode
     * @return  the method at the specified index
     */
    public Method getMethodAt(InstanceKlass accessor, int index, int bc) {
        Object oop = getOop();
        int holderIndex = JVM.getHolderIndexAt(oop, index);
        InstanceKlass holder = (InstanceKlass) getKlassAt(holderIndex);
        Object method = holder.isLoaded() ?
            JVM.getMethodAt(oop, accessor.getOop(), holder.getOop(), index, bc) : null;
        if (method != null) {
            return new Method(method);
        } else {
            Symbol name = new Symbol(JVM.getMemberNameAt(oop, index));
            Symbol signature = new Symbol(JVM.getSignatureAt(oop, index));
            return new Method(holder, name, signature);
        }
    }
    
    /**
     * Retrieves a field from the constant pool.
     *
     * @param   index     index of the constant pool entry
     * @param   bc        the bytecode
     * @return  the field at the specified index
     */
    public Field getFieldAt(int index, int bc) {
        Object oop = getOop();
        int holderIndex = JVM.getHolderIndexAt(oop, index);
        InstanceKlass holder = (InstanceKlass) getKlassAt(holderIndex);
        Object name = JVM.getMemberNameAt(oop, index);
        Object signature = JVM.getSignatureAt(oop, index);
        int fieldType = BasicType.valueOf(JVM.getSymbolString(signature).charAt(0));
        Object klass = holder.isLoaded() ?
            JVM.getCanonicalHolder(holder.getOop(), name, signature) : null;
        if ((klass != null) && JVM.willLink(getOop(), index, bc)) {
            int offset = JVM.getFieldOffset(holder.getOop(), name, signature);
            return new Field(new InstanceKlass(klass), fieldType, offset);
        } else {
            return new Field(holder, fieldType, -1);
        }
    }
    
    /**
     * Returns the constant pool entry type for the specified index.
     *
     * @param   index  index of the constant pool entry
     * @return  type of the constant pool entry
     */
    public int getTagAt(int index) {
        return JVM.getTagAt(getOop(), index);
    }
    
    /**
     * Retrieves an integer value from the constant pool.
     *
     * @param   index  index of the constant pool entry
     * @return  integer value at the specified index
     */
    public int getIntAt(int index) {
        return JVM.getIntAt(getOop(), index);
    }
    
    /**
     * Retrieves a long integer value from the constant pool.
     *
     * @param   index  index of the constant pool entry
     * @return  long integer value at the specified index
     */
    public long getLongAt(int index) {
        return JVM.getLongAt(getOop(), index);
    }
    
    /**
     * Retrieves a single-precision floating-point value from the constant pool.
     *
     * @param   index  index of the constant pool entry
     * @return  floating-point value at the specified index
     */
    public float getFloatAt(int index) {
        return JVM.getFloatAt(getOop(), index);
    }
    
    /**
     * Retrieves a double-precision floating-point value from the constant pool.
     *
     * @param   index  index of the constant pool entry
     * @return  floating-point value at the specified index
     */
    public double getDoubleAt(int index) {
        return JVM.getDoubleAt(getOop(), index);
    }
    
    /**
     * Retrieves a string constant from the constant pool.
     *
     * @param   index  index of the constant pool entry
     * @return  string constant at the specified index
     */
    public Instance getStringAt(int index) {
        return new Instance(JVM.getStringAt(getOop(), index));
    }
    
    /**
     * Retrieves the signature of a method or field from the constant pool.
     *
     * @param   index  index of the constant pool entry
     * @return  signature at the specified index
     */
    public Symbol getSignatureAt(int index) {
        return new Symbol(JVM.getSignatureAt(getOop(), index));
    }
}
