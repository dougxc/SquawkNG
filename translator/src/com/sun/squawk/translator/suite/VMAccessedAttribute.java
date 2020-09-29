package com.sun.squawk.translator.suite;

import com.sun.squawk.translator.suite.SuiteAttribute;
import java.util.Vector;
import java.util.Enumeration;

/**
 * This class represents the optional attribute for a class in a
 * suite that denotes the associated class is accessed internally
 * by the VM and describes exactly which fields and methods
 * need to be exposed to the VM.
 *
 * The serialized binary format for the attribute is:
 *
 * VMAccessed_attribute {
 *     CONSTANT_Utf8_info attribute_name; // "VMAccessed"
 *     u4 attribute_length;
 *     u2 class_access_flags;
 *     u2 instance_fields_table_length;
 *     {
 *         u2 slot;
 *         u2 access_flags;
 *     } instance_fields_table[instance_fields_table_length];
 *     u2 static_methods_table_length;
 *     {
 *         u2 slot;
 *         u2 access_flags;
 *     } static_methods_table[static_methods_table_length];
 * }
 *
 * The value of the class_access_flags and field_access_flags item
 * is a mask of the following flags:
 *
 *   Flag Name       | Value  | Applies To     | Meaning
 *   ----------------+--------+----------------+---------------------------------
 *   VMACC_EEPROM    | 0x0001 | class          | The VM only accesses instances of
 *                   |        |                | this class that are in EEPROM.
 *   VMACC_UNSIGNED  | 0x0002 | instance field | The field is accessed by the Squawk
 *                   |        |                | VM as an unsigned value.
 *   VMACC_WBOPQAUE  | 0x0004 | instance field | The write barrier (if any) is not
 *                   |        |                | updated when the field is updated.
 *   VMACC_READ      | 0x0008 | instance field | The VM reads the field.
 *   VMACC_WRITE     | 0x0010 | instance field | The VM writes to the field.
 *   VMACC_CALL      | 0x0010 | static method  | The VM calls the method.
 */
public final class VMAccessedAttribute extends SuiteAttribute {

    /**
     * This class encapsulates a single entry in the 'instance_fields_table' or
     * the 'static_methods_table'.
     */
    public static final class Item {
        /**
         * The logical slot number of a field or method in the enclosing class's
         * 'instance_fields' or 'static_methods_table' array.
         */
        public final int slot;

        /**
         * The access flags for the corresponding field or method that
         * describes how it is accessed by the VM.
         */
        public final int accessFlags;

        private Item(int slot, int accessFlags) {
            this.slot = slot;
            this.accessFlags = accessFlags;
        }
    }

    /**
     * The access flags for the corresponding class that describes how it
     * is accessed by the VM.
     */
    final public int classAccessFlags;


    /**
     * Create a new class attribute that describes how the associated class
     * and its field are accessed directly by the VM.
     *
     * @param classAccessFlags.
     */
    public VMAccessedAttribute(int classAccessFlags) {
        this.classAccessFlags = classAccessFlags;
        this.instanceFieldsTable = new Vector();
        this.staticMethodsTable = new Vector();
    }

    /**
     * Return the name of the attribute.
     *
     * @return "VMAccessed".
     */
    public String getAttributeName () {
        return "VMAccessed";
    }

    /**
     * Add a new entry to the instance fields table.
     *
     * @param slot
     * @param accessFlags
     */
    public void addInstanceFieldsTableItem(int slot, int accessFlags) {
        instanceFieldsTable.addElement(new Item(slot, accessFlags));
    }

    /**
     * Return the size of the instance fields table. This is the number
     * of entries in the table.
     *
     * @return the size of the instance fields table
     */
    public int getInstanceFieldsTableSize() {
        return instanceFieldsTable.size() ;
    }

    /**
     * Return an enumeration of the entries in the instance fields table.
     *
     * @return an enumeration of the entries in the instance fields table.
     */
    public Enumeration getInstanceFieldsTableEntries() {
        return instanceFieldsTable.elements();
    }

    /**
     * Add a new entry to the static methods table.
     *
     * @param slot
     * @param accessFlags
     */
    public void addStaticMethodsTableItem(int slot, int accessFlags) {
        staticMethodsTable.addElement(new Item(slot, accessFlags));
    }

    /**
     * Return the size of the static methods table. This is the number
     * of entries in the table.
     *
     * @return the size of the static methods table
     */
    public int getStaticMethodsTableSize() {
        return staticMethodsTable.size() ;
    }

    /**
     * Return an enumeration of the entries in the static methods table.
     *
     * @return an enumeration of the entries in the static methods table.
     */
    public Enumeration getStaticMethodsTableEntries() {
        return staticMethodsTable.elements();
    }

    private final Vector instanceFieldsTable;
    private final Vector staticMethodsTable;
}