/*
 * @(#)AccessField.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.types.ValueType;

/**
 * The abstract base class for instructions loading or storing a static field
 * or an instance variable.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public abstract class AccessField extends Instruction {
    /**
     * The object containing the field.
     */
    private Instruction obj;

    /**
     * The offset of the field in the object.
     */
    private int offset;

    /**
     * The basic type of the field.
     */
    private int fieldType;

    /**
     * Whether or not the field is static.
     */
    private boolean isStatic;

    /**
     * Whether or not the field is loaded.
     */
    private boolean loaded;

    /**
     * Whether or not the field is initialized.
     */
    private boolean initialized;

    /**
     * Initializes the attributes declared in this class.
     *
     * @param  scope        scope containing this instruction
     * @param  obj          the parent object
     * @param  offset       offset of the field
     * @param  fieldType    <strong>basic</strong> field type
     * @param  isStatic     whether it is a static or an instance field
     * @param  loaded       whether or not the field is loaded
     * @param  initialized  whether or not the field is initialized
     */
    protected AccessField(IRScope scope, Instruction obj, int offset,
            int fieldType, boolean isStatic, boolean loaded,
            boolean initialized) {
        super(scope, ValueType.valueOf(fieldType));
        this.obj = obj;
        this.offset = offset;
        this.fieldType = fieldType;
        this.isStatic = isStatic;
        this.loaded = loaded;
        this.initialized = initialized;
        if (!loaded || !initialized) {
            setPinned(true);
        }
    }

    /**
     * Returns the object that contains the field.
     *
     * @return  the parent object
     */
    public Instruction getObj() {
        return obj;
    }

    /**
     * Returns the offset of the field in the object.
     *
     * @return  the offset of the field
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Returns the basic type of the field.
     *
     * @return  the field type
     */
    public int getFieldType() {
        return fieldType;
    }

    /**
     * Tests if the field to be accessed is a static or an instance field.
     *
     * @return  whether or not the field is static
     */
    public boolean isStatic() {
        return isStatic;
    }

    /**
     * Returns whether or not the field is loaded.
     *
     * @return  whether or not the field is loaded
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Returns whether or not the field is initialized.
     *
     * @return  whether or not the field is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    public boolean canTrap() {
        return true;
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        obj = vc.doValue(obj);
    }
}
