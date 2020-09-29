package com.sun.squawk.loader;

import java.io.*;
import com.sun.squawk.suite.*;
import com.sun.squawk.vm.SquawkConstants;

final class SuiteRelocator extends Relocator implements SymbolEditor {

    private static int primitiveStatics = -9999;

    private final int finalClassNumber;
    private final SuiteClass sc;

    /*
     * Constructor
     */
    SuiteRelocator(SuiteClass sc, int finalClassNumber) {
        this.finalClassNumber = finalClassNumber;
        this.sc = sc;

       /*
        * Edit the symbols of this suite class to relocate the offsets to their
        * physical offsets from their logical suite file offsets.
        */
        Romizer.assume(primitiveStatics == -9999);
        primitiveStatics = 0;

        sc.symbols = sc.symbols.edit(this); // Edit the symbols using the following editMember() function.
        primitiveStatics = -9999;

       /*
        * Fixup the interface slots if this is not an interface
        */
        if (!isInterface() && !sc.isAbstract()) {
            fixupInterfacesSlots(SuiteLoader.theLoader);
        }

       /*
        * Allow this relocator to be migrated to EEPROM memory.
        */
        PersistentMemory.allowMigration(this);
    }

    /**
     * editMember
     * @param mtype
     * @param member
     * @param index
     * @return a member
     */
    public Member editMember(int mtype, Member m, int index) {
        editMember0(mtype, m, index);
        return m;
    }

    private void editMember0(int mtype, Member m, int index) {
        if (mtype == SquawkConstants.MTYPE_INSTANCEFIELDS) {
            // The member's offset need to be adjusted if it is an instance
            // field from a logical offset in terms of slots to a physical
            // offset in terms of bytes.
            int offset = localInstanceFieldsByteCount(index);

            // Align the field according to its size
            int size = SuiteLoader.theLoader.getPackedSize(m.type());
            offset = SuiteLoader.roundup(offset, size);

            // Also need to add the offsets used by super classes
            Relocator superClass = suite_superType();
            if (superClass != null) {
                offset += superClass.internal_getInstanceFieldsSize()*4;
            }
            m.setOffset(offset);
        } else if (mtype == SquawkConstants.MTYPE_VIRTUALMETHODS) {
            // Add the offsets used by super classes
            Relocator superClass = suite_superType();
            int offset = index;
            if (superClass != null) {
                offset += superClass.internal_getVirtualMethodCount();
            }
            m.setOffset(offset);
            // If this is a native method in suite 0, then get the next
            // globally unique native method number.
            if (m.isNative() && SuiteLoader.theLoader.suite().number() == 0) {
                if ((m.access() & SquawkConstants.ACC_FINAL) == 0) {
                    // Native methods in suite 0 must be final as calls to
                    // them are converted into 'invokenative' calls which
                    // are not polymorphic.
                    throw new LinkageError("Native methods in suite 0 must be final: "+sc.symbols.getClassName()+"."+m.name());
                }
                m.setNativeMethodIdentifier(getNextNativeMethodIdentifier());
            }
        } else if (mtype == SquawkConstants.MTYPE_STATICFIELDS) {
            // The member's offset need to be adjusted if it is a static
            // field from a logical offset in terms of all static fields
            // to a physical offset in terms of the primitive static fields
            // if the member is a primitive typed field otherwise in terms
            // of the non-primitive typed fields.
            boolean isPrimitive = SuiteClass.isPrimitiveType(m.type());
            if (isPrimitive) {
                m.setOffset(primitiveStatics++);
            } else {
                m.setOffset(index - primitiveStatics);
            }
        } else {
            Romizer.assume(index == m.offset());
            // If this is a native method in suite 0, then get the next
            // globally unique native method number.
            if (m.isNative() && SuiteLoader.theLoader.suite().number() == 0) {
                m.setNativeMethodIdentifier(getNextNativeMethodIdentifier());
            }
        }
    }

    /**
     * Allocate and return a globally unique native method identifier.
     *
     * @return
     */
    private static int getNextNativeMethodIdentifier() {
        int id = Romizer.getNextNativeMethodNumber();
        if ((id & ~0xFFFF) != 0) {
            throw new LinkageError("Native method limit exceeded");
        }
        return id;
    }

   /* ------------------------------------------------------------------------ *\
    *                       Internal class methods                             *
    *                                                                          *
    * All methods in this section refer to the internals of the class as they  *
    * would be once the class is linked into the system.                       *
   \* ------------------------------------------------------------------------ */


   /**
    * Get the unique 16-bit identifier for the internally linked class.
    *
    * @return the 16-bit identifier for the internally linked class.
    */
   char internal_getType() {
        return (char)(SuiteLoader.theLoader.suite().number() << 8 | finalClassNumber);
    }

    /**
     * Get the number of static methods in this class.
     *
     * @return number of static or virtual methods in this class.
     */
    int internal_getStaticMethodCount() {
        return sc.symbols.getMemberCount(SquawkConstants.MTYPE_STATICMETHODS);
    }

    /**
     * Get the number of virtual methods in this class's hierarchy.
     *
     * @param superOnly If false, return the number of virtual methods declared
     * by this class and its super classes otherwise return only the
     * number of virtual methods declared by its super classes.
     * @return number of virtual methods in this class's hierarchy.
     */
    int internal_getVirtualMethodCount() {
        Relocator stype = suite_superType();
        int count = sc.symbols.getMemberCount(SquawkConstants.MTYPE_VIRTUALMETHODS);
        if (stype != null) {
           count += stype.internal_getVirtualMethodCount();
        }
        return count;
    }

    /**
     * Get the size in words required by the static fields of this class.
     *
     * @param onlyReferenceFields If true, only count the non-primitive typed
     * static fields.
     * @return size in words of the static fields declared by this class.
     */
    int internal_getStaticFieldsSize(boolean onlyReferenceFields) {
        if (!onlyReferenceFields) {
            return sc.symbols.getMemberCount(SquawkConstants.MTYPE_STATICFIELDS);
        } else {
            int count = 0;
            for (int slot = sc.symbols.getMemberCount(SquawkConstants.MTYPE_STATICFIELDS) - 1; slot >= 0; --slot) {
                int memberID = sc.symbols.lookupMemberID(SquawkConstants.MTYPE_STATICFIELDS, slot);
                if (!SuiteClass.isPrimitiveType(sc.symbols.getMemberType(memberID))) {
                    count++;
                }
            }
            return count;
        }
    }

    /**
     * Get the size in words required by the instance fields of this
     * class's hierachy.
     *
     * @return size in words of the instance fields declared by this
     * class's hierachy.
     */
    int internal_getInstanceFieldsSize() {
        Relocator stype = suite_superType();
        int count = (localInstanceFieldsByteCount(-1)+3)/4;
        if (stype != null) {
           count += stype.internal_getInstanceFieldsSize();
        }
        return count;
    }

    /**
     * Determine if this class must be initialized before use. A class must
     * be initialized before use it it or any of its super classes
     * define a <clinit> method.
     *
     * @return true if the class must be initialized before use
     */
    boolean internal_mustClinit() {
        if ((sc.getAccess() & SquawkConstants.ACC_MUSTCLINIT) != 0) {
            return true;
        }
        Relocator stype = suite_superType();
        if (stype != null) {
            return stype.internal_mustClinit();
        }
        return false;
    }

    /**
     * Get the length in bytes of memory needed for the (0 .. end-1] instance
     * fields defined by this class.
     *
     * @param end The instance field slot to stop counting at. If this is -1,
     * then all instance field slots are counted.
     * @return the length in bytes of memory needed for (0 .. end-1] instance
     * fields defined by this class.
     */
    private int localInstanceFieldsByteCount(int end) {
        if (end == -1) {
            end = sc.symbols.getMemberCount(SquawkConstants.MTYPE_INSTANCEFIELDS);
        }
        int count = 0;
        for (int slot = 0; slot != end; slot++) {
            int memberID = sc.symbols.lookupMemberID(SquawkConstants.MTYPE_INSTANCEFIELDS, slot);
            int type = sc.symbols.getMemberType(memberID);
            int size = SuiteLoader.theLoader.getPackedSize(type);
            count = SuiteLoader.roundup(count, size);
            count += size;
        }
        return count;
    }

   /* ------------------------------------------------------------------------ *\
    *                       Suite class methods                                *
    *                                                                          *
    * All methods in this section refer to the internals of the class as       *
    * presented in a suite file.                                               *
   \* ------------------------------------------------------------------------ */


   /**
    * Get the relocator for the super class (as specified in a suite file)
    * of this class. For a proxy class, the returned class may not be the
    * direct super class of the real class if the direct super class was
    * not included in the suite.
    * @return the relocator for the super class (as specified in a suite file)
    * of this class.
    */
   Relocator suite_superType() {
       return SuiteLoader.theLoader.getRelocator(sc.getSuperType());
   }

   /**
    * If this relocator represents an array class, return the unique 16-bit
    * identifier (in the suite file namespace) its element type. Otherwise
    * return 0.
    * @return the 16-bit identifier for element type of the array class
    * represented by this relocator or 0 if this relocator does not represent
    * an array class.
    */
   char suite_getElementType() {
       if (!sc.symbols.isArray()) {
           return 0;
       } else {
           return SuiteLoader.theLoader.getElementTypeFor(sc.symbols);
       }
   }

   /**
    * Look up a member in this class and fill in the given Member container
    * with the details of the member.
    * @param minfo The container to fill with the member's details (if found).
    * @param mtype The member category.
    * @param index The member's slot.
    * @return The
    */
   Member getSuiteMember(int mtype, int slot) {
       return sc.symbols.getMember(sc.symbols.lookupMemberID(mtype, slot));
    }

    /**
     * Get the number of members in a specified category declared by this class
     * in a suite file.
     * @param mtype The category of members to consider.
     * @return the number of members in the category specified by <code>mtype</code>
     * declared by this class in a suite file.
     */
    int suite_getSlotsCount(int mtype) {
        return sc.symbols.getMemberCount(mtype);
    }

   /* ------------------------------------------------------------------------ *\
    *                             Other methods                                *
   \* ------------------------------------------------------------------------ */

    /*
     * getSuiteClass
     */
    SuiteClass getSuiteClass() {
        return sc;
    }

    /**
     * Get the fully qualified internal name of this class. The format of
     * the name is equivalent to that specified by Klass.getNameInternal().
     *
     * @return the fully qualified internal name of this class.
     */
    String getName() {
        return sc.getName();
    }

    /**
     * Get the symbols of this class.
     *
     * @return
     */
    StringOfSymbols getSymbols() {
        return sc.symbols;
    }

    /*
     * finalClassNumber
     * @return class number
     */
    int finalClassNumber() {
        return finalClassNumber;
    }
    /*
     * isInterface
     */
    boolean isInterface() {
        return sc.isInterface();
    }

    /*
     * isSubclassOf
     */
    boolean isSubclassOf(Relocator aClass) {
        if (this == aClass) {
            return true;
        }
        Relocator stype = suite_superType();
        return stype == null ? false : stype.isSubclassOf(aClass);
    }

    /*
     * isImplementorOf
     */
    boolean isImplementorOf(Relocator aClass) {
       /*
        * The interface list in each class is a transitive closure of all the
        * interface types specified in the class file less those defined in the
        * superclass hierarchy. Therefore it is only necessary to check this list
        * and not the interfaces implemented by the interfaces, and then to do this
        * to the superclasses if until it is matched.
        */
        for (int i = 0 ; i < sc.interfaceTypes.length ; i++) {
            Relocator r = SuiteLoader.theLoader.getRelocator(sc.interfaceTypes[i]);
            if (aClass == r) {
                return true;
            }
        }

        Relocator stype = suite_superType();
        return stype == null ? false : stype.isImplementorOf(aClass);
    }

    /*
     * isElementAssignableTo
     */
    boolean isElementAssignableTo(Relocator aClass) {
        char a = this.suite_getElementType();
        char b = aClass.suite_getElementType();
        if (a != 0 && b != 0) {
            return SuiteLoader.theLoader.isAssignable(a, b);
        }
        return false;
    }

    /*
     * fixupInterfacesSlots
     */
    private void fixupInterfacesSlots(SuiteLoader loader) {
        char[] interfaces = sc.interfaceTypes;
        byte[][] ifaceSlots    = sc.interfaceSlotTables;
        if (interfaces != null) {
            for (int i = 0; i != interfaces.length; i++) {
                int cno = interfaces[i]&0xFF;
                //SuiteLoader.debugOut.print("INTERFACE["+i+"] "+loader.suiteClasses[cno].getName()+" ");
                byte[] slots = ifaceSlots[i];
                for (int j = 0; j != slots.length; j++) {
                    byte impl = slots[j];
                    Member m = getMember(SquawkConstants.MTYPE_VIRTUALMETHODS, impl);
                    slots[j] = (byte)m.offset();
                }
            }
        }
    }

    /**
     * Get the unique 16-bit identifier (in the suite file namespace) of the
     * class at a specified index in the class references table of this class.
     * This only applies to relocators for non-proxy classes.
     * @param index The index of the class reference.
     * @return unique 16-bit identifier of a class referred to by this class
     * @throws LinkageError if this is a relocator for a proxy class or if the
     * index is out of range.
     */
    char getClassReference(int index) {
        if (index >= sc.classReferences.length) {
            throw new LinkageError("ClassFormatError: invalid class reference");
        }
        return sc.classReferences[index];
    }

    /**
     * Get the object at a specified index from the object references pool of
     * this class.
     * This only applies to relocators for non-proxy classes.
     * @param index The index of the object reference.
     * @return the object at index <code>index</code> in the object references
     * table of this class.
     * @throws LinkageError if this is a relocator for a proxy class or if the
     * index is out of range.
     */
    Object getObjectReference(int index) {
         if (index >= sc.objectReferences.length) {
             throw new LinkageError("ClassFormatError: invalid object reference");
         }
         return sc.objectReferences[index];
     }

//=============================================================================================================================
// TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP
//=============================================================================================================================

/*if[DEBUG.LOADER]*/
    void printClass(int n, SuiteLoader loader) {
        SuiteLoader.debugOut.println();
        SuiteLoader.debugOut.print("Real class  "+n+" = "+getName());

        Relocator stype = suite_superType();
        if (stype != null) {
            SuiteLoader.debugOut.print(" extends "+stype.getName());
        }
        char etype = suite_getElementType();
        if (etype != 0) {
            SuiteLoader.debugOut.print(" array_of "+SuiteLoader.theLoader.getRelocator(etype).getName());
        }
        SuiteLoader.debugOut.print(" access="+StringOfSymbols.intToHex(sc.getAccess())+" mustClinit="+internal_mustClinit());
        SuiteLoader.debugOut.println();

        printMembers(SquawkConstants.MTYPE_INSTANCEFIELDS, loader);
        printMembers(SquawkConstants.MTYPE_STATICFIELDS,   loader);
        printMembers(SquawkConstants.MTYPE_VIRTUALMETHODS, loader);
        printMembers(SquawkConstants.MTYPE_STATICMETHODS,  loader);

        printInterfaces(loader);
        printClassReferences(loader);
        printObjects();
    }

    void printMembers(int mtype, SuiteLoader loader) {
        String label = StringOfSymbols.MTYPE_NAMES[mtype];
        for (int slot = 0 ;; slot++) {
            try {
                Member m = getMember(mtype, slot);
                String name = m.toString(null, label.charAt(0) == 'S', label.charAt(1) == 'M', loader);
                SuiteLoader.debugOut.print(""+slot+"\t"+label+" ["+m.offset());
                if (mtype == SquawkConstants.MTYPE_STATICFIELDS) {
                    if (!SuiteClass.isPrimitiveType(m.type())) {
                        SuiteLoader.debugOut.print("+");
                    }
                }
                SuiteLoader.debugOut.println("] "+name+" access="+StringOfSymbols.intToHex(m.access()));
            } catch (LinkageError le) {
                break;
            }
        }
    }

    void printInterfaces(SuiteLoader loader) {
        char[] interfaces = sc.interfaceTypes;
        byte[][] ifaceSlots = sc.interfaceSlotTables;
        if (!isInterface() && !sc.isAbstract()) {
            if (interfaces != null) {
                for (int i = 0; i != interfaces.length; i++) {
                    int cno = interfaces[i] & 0xFF;
                    SuiteLoader.debugOut.print("INTERFACE[" + i + "] " +
                                               loader.getRelocator(cno).getName() +
                                               " ");
                    byte[] slots = ifaceSlots[i];
                    for (int j = 0; j != slots.length; j++) {
                        byte impl = slots[j];
                        SuiteLoader.debugOut.print(j + "->" + impl + " ");
                    }
                    SuiteLoader.debugOut.println();
                }
            }
        }
    }

    void printObjects() {
        Object[] objs = sc.objectReferences;
        if (objs != null) {
            for (int i = 0; i != objs.length; i++) {
                Object o = objs[i];
                if (o instanceof String) {
                    SuiteLoader.debugOut.println("CONST["+i+"] String: \""+o+"\"");
                }
                else if (o instanceof int[]) {
                    int[] arr = (int[])o;
                    SuiteLoader.debugOut.print("CONST["+i+"] int["+arr.length+"]: ");
                    for (int j = 0; j != arr.length; j++) {
                        SuiteLoader.debugOut.print(arr[j]+" ");
                    }
                    SuiteLoader.debugOut.println();
                }
                else {
                    throw new RuntimeException("Unknown constant object type: "+o.getClass().getName());
                }
            }
        }
    }

    void printClassReferences(SuiteLoader loader) {
        char[] refs = sc.classReferences;
        if (refs != null) {
            for (int i = 0; i != refs.length; i++) {
                int cno = refs[i]&0xFF;
                SuiteLoader.debugOut.println("CLASSREF["+i+"] "+loader.getRelocator(cno).getName());
            }
        }
    }
/*end[DEBUG.LOADER]*/

}

