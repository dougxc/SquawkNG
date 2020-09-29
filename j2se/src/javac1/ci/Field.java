/*
 * @(#)ConstantPool.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ci;

/**
 * Represents a field in the compiler interface.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Field {
    /**
     * The class that holds this field.
     */
    private InstanceKlass holder;
    
    /**
     * The basic type of the field.
     */
    private int fieldType;
    
    /**
     * The offset of the field within an object.
     */
    private int offset;
    
    /**
     * Constructs a new field with the specified holder and offset.
     *
     * @param  holder     class that holds the field
     * @param  fieldType  basic type of the field
     * @param  offset     offset of the field
     */
    protected Field(InstanceKlass holder, int fieldType, int offset) {
        this.holder = holder;
        this.fieldType = fieldType;
        this.offset = offset;
        
    }
    
    /**
     * Returns the class that holds the field.
     *
     * @return  class that holds the field
     */
    public InstanceKlass getHolder() {
        return holder;
    }
    
    /**
     * Returns the basic type of the field.
     *
     * @return  basic type of the field
     */
    public int getFieldType() {
        return fieldType;
    }
    
    /**
     * Returns the offset of the field within an object.
     *
     * @return  offset of the field
     */
    public int getOffset() {
        return offset;
    }
    
    /**
     * Returns whether this field is loaded or not.
     *
     * @return  whether the field is loaded or not
     */
    public boolean isLoaded() {
        return offset != -1;
    }
    
    /**
     * Tests if this field has been initialized.
     *
     * @return  whether the field is initialized or not
     */
    public boolean isInitialized() {
        return isLoaded() && holder.isInitialized();
    }
}
