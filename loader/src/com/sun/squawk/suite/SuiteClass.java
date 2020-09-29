package com.sun.squawk.suite;

import com.sun.squawk.vm.CNO;
import com.sun.squawk.vm.SquawkConstants;

/**
 * A SuiteClass instance is a container for the information present in a
 * suite file for a single class. It corresponds with the 'Type_info'
 * structure described in the Suite File format.
 */
public class SuiteClass {

    static final char[]   NO_INTERFACE_TYPES       = (char[])   PersistentMemory.get("new char[0]");
    static final char[]   NO_CLASS_REFERENCES      = (char[])   PersistentMemory.get("new char[0]");
    static final byte[][] NO_INTERFACE_SLOT_TABLES = (byte[][]) PersistentMemory.get("new byte[0][]");
    static final Object[] NO_OBJECT_REFERENCES     = (Object[]) PersistentMemory.get("new Object[0]");
    static final int[]    NO_OVERRIDDEN_ACCESS     = (int[])    PersistentMemory.get("new int[0]");

    public  /*final*/   StringOfSymbols symbols;
    private final       char            access;
    private final       byte            type_in_a_byte;
    private final       byte            superType_in_a_byte;
    public  final       char[]          interfaceTypes;
    public  final       byte[][]        interfaceSlotTables;
    public  final       char[]          classReferences;
    public  final       Object[]        objectReferences;
/*if[OVERRIDDENACCESS]*/
    private final       int[]           overriddenAccess;
/*end[OVERRIDDENACCESS]*/

    /*
     * SuiteClass (new)
     */
    public SuiteClass(
                         StringOfSymbols    symbols,
                         char               type,
                         char               access,
                         char               superType,
                         char[]             interfaceTypes,
                         char[][]           interfaceSlotTables,
                         char[]             classReferences,
                         Object[]           objectReferences,
                         int[]              overriddenAccess
                     ) {
        this.symbols             = symbols;
        this.type_in_a_byte      = (byte)type;
        this.access              = access;
        this.superType_in_a_byte = (byte)superType;
        this.interfaceTypes      = (interfaceTypes == null      || interfaceTypes.length == 0)   ?    NO_INTERFACE_TYPES      : interfaceTypes;
        this.classReferences     = (classReferences == null     || classReferences.length == 0)  ?    NO_CLASS_REFERENCES     : classReferences;
        this.objectReferences    = (objectReferences == null    || objectReferences.length == 0) ?    NO_OBJECT_REFERENCES    : objectReferences;
/*if[OVERRIDDENACCESS]*/
        this.overriddenAccess    = (overriddenAccess == null    || overriddenAccess.length == 0) ?    NO_OVERRIDDEN_ACCESS    : overriddenAccess;
/*end[OVERRIDDENACCESS]*/

        // Re-cast interface slot tables to be byte sized slot offsets
        int ifaceCount = this.interfaceTypes.length;
        if (isInterface() || ((access & SquawkConstants.ACC_ABSTRACT) != 0) || ifaceCount == 0) {
            this.interfaceSlotTables = NO_INTERFACE_SLOT_TABLES;
        } else {
            this.interfaceSlotTables = new byte[ifaceCount][];
        }

        for (int i = 0; i != ifaceCount; i++) {
            char[] slotsChar = interfaceSlotTables[i];
            // This must not be an interface or abstract class
            if (isInterface() || isAbstract()) {
                if (slotsChar.length != 0) {
                    throw new LinkageError("ClassFormatError: abstract/interface class cannot have interface method implementations table: "+getName()+" interfaceSlotTables.length="+interfaceSlotTables.length);
                }
            } else {
                byte[] slots = new byte[slotsChar.length];
                for (int j = 0; j != slots.length; j++) {
                    char slot = slotsChar[j];
                    if (slot > 0xFF) {
                        throw new LinkageError(
                            "ClassFormatError: interface method implementation slot too large");
                    }
                    slots[j] = (byte)(slot & 0xFF);
                }
                this.interfaceSlotTables[i] = slots;
            }
        }

        // Ensure that the minimal VM constraints are not exceeded
        Romizer.assume(symbols.getMemberCount(SquawkConstants.MTYPE_STATICFIELDS)   <= 256);
        Romizer.assume(symbols.getMemberCount(SquawkConstants.MTYPE_STATICMETHODS)  <= 256);
    }

    /*
     * getName
     */
    public String getName() {
        return symbols.getClassName();
    }

    /**
     * Test the class.
     * @param str the class name to test
     * @return true if the class name is the same
     */
    public boolean equalsClassName(String str) {
        return symbols.equalsClassName(str);
    }


    /*
     * getAccess
     */
    public char getAccess() {
        return access;
    }

    /*
     * getType
     */
    public char getType() {
        return (char)(type_in_a_byte & 0xFF);
    }

    /*
     * getSuperType
     */
    public char getSuperType() {
        return (char)(superType_in_a_byte & 0xFF);
    }

    /*
     * isInterface
     */
    public boolean isInterface() {
        return (access & SquawkConstants.ACC_INTERFACE) != 0;
    }

    /*
     * isAbstract
     */
    public boolean isAbstract() {
        return (access & SquawkConstants.ACC_ABSTRACT) != 0;
    }

    /*
     * isArray
     */
    public boolean isArray() {
        return symbols.isArray();
    }

    /**
     * Determine if a given type number represents a primitive type.
     *
     * @param t The type number.
     * @return true if t represents a primitive type.
     */
    public static boolean isPrimitiveType(int t) {
        return (t >= CNO.FIRST_PRIMITIVE_TYPE && t <= CNO.LAST_PRIMITIVE_TYPE);
    }

    public int[] getOverriddenAccess() {
        int[] res = NO_OVERRIDDEN_ACCESS;
/*if[OVERRIDDENACCESS]*/
        res = overriddenAccess;
/*end[OVERRIDDENACCESS]*/
        return res;
    }

}


