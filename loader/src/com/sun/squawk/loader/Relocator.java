package com.sun.squawk.loader;

import java.io.*;
import java.util.NoSuchElementException;
import com.sun.squawk.suite.*;
import com.sun.squawk.vm.CNO;
import com.sun.squawk.vm.SquawkConstants;

/**
 * A Relocator instance relates a class in a suite file to its internally
 * linked representation. A proxy class in a suite will be represented by
 * a ProxyRelocator instance and a non-proxy class wiill be represented by
 * a SuiteRelocator instance.
 */
abstract class Relocator {

   /* ------------------------------------------------------------------------ *\
    *                 Internally linked class methods                          *
    *                                                                          *
    * All methods in this section refer to the details of the class as they    *
    * would be once the class is internally linked into the system.            *
   \* ------------------------------------------------------------------------ */

    /**
     * Get the unique 16-bit identifier for the internally linked class.
     * @return the 16-bit identifier for the internally linked class.
     */
    abstract char internal_getType();

    /**
     * Get the number of static methods in this class.
     * @return number of static or virtual methods in this class.
     */
    abstract int internal_getStaticMethodCount();

    /**
     * Get the number of virtual methods in this class's hierarchy.
     * @param superOnly If false, return the number of virtual methods declared
     * by this class and its super classes otherwise return only the
     * number of virtual methods declared by its super classes.
     * @return number of virtual methods in this class's hierarchy.
     */
    abstract int internal_getVirtualMethodCount();

    /**
     * Get the size in words required by the static fields of this class.
     * @param onlyReferenceFields If true, only count the non-primitive typed
     * static fields.
     * @return size in words of the static fields declared by this class.
     */
    abstract int internal_getStaticFieldsSize(boolean onlyReferenceFields);

    /**
     * Get the size in words required by the instance fields of this
     * class's hierachy.
     * @param superOnly If false, return the size of the instance fields declared
     * by this class and its super classes otherwise return only the
     * size of the instance fields declared by its super classes.
     * @return size in words of the instance fields declared by this
     * class's hierachy.
     */
    abstract int internal_getInstanceFieldsSize();

    /**
     * Determine if this class must be initialized before use. A class must
     * be initialized before use it it or any of its super classes
     * define a <clinit> method.
     *
     * @return true if the class must be initialized before use
     */
    abstract boolean internal_mustClinit();

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
    abstract Relocator suite_superType();

    /**
     * If this relocator represents an array class, return the unique 16-bit
     * identifier (in the suite file namespace) its element type. Otherwise
     * return 0.
     * @return the 16-bit identifier for element type of the array class
     * represented by this relocator or 0 if this relocator does not represent
     * an array class.
     */
    abstract char suite_getElementType();

    /**
     * Look up a member in this class and return a Member instance
     * with the details of the member.
     * @param mtype The member's category.
     * @param slot The member's logical slot number with in a suite.
     * @return the member or null.
     */
    abstract Member getSuiteMember(int mtype, int slot);

    /**
     * Get the number of members in a specified category declared by this class
     * in a suite. This does not include member inherited from or declared by
     * super classes.
     * @param mtype The category of members to consider.
     * @return the number of members in the category specified by <code>mtype</code>
     * declared by this class in a suite file.
     */
    abstract int suite_getSlotsCount(int mtype);

   /* ------------------------------------------------------------------------ *\
    *            Other methods that must be defined by subclasses              *
   \* ------------------------------------------------------------------------ */

    /**
     * Get the fully qualified internal name of this class. The format of
     * the name is equivalent to that specified by Klass.getNameInternal().
     *
     * @return the fully qualified internal name of this class.
     */
    abstract String getName();

    /**
     * Get the symbols of this class.
     *
     * @return
     */
    abstract StringOfSymbols getSymbols();

    /*
     * isInterface
     */
    abstract boolean isInterface();

    /*
     * isSubclassOf
     */
    abstract boolean isSubclassOf(Relocator aClass);

    /*
     * isImplementorOf
     */
    abstract boolean isImplementorOf(Relocator aClass);

    /*
     * isElementAssignableTo
     */
    abstract boolean isElementAssignableTo(Relocator aClass);


/*if[DEBUG.LOADER]*/
    abstract void printClass(int i, SuiteLoader loader);
/*end[DEBUG.LOADER]*/

   /* ------------------------------------------------------------------------ *\
    *                          Non-overidable methods                          *
   \* ------------------------------------------------------------------------ */

   /**
    * Get the symbolic details for a specified suite member in this class.
    * @param mtype The category of members to search.
    * @param slot The slot identifier for the member.
    * @return the found member
    * @throws LinkageError if the member was not found.
    */
    public final Member getMember(int mtype, int slot) {
        Romizer.assume(slot >= 0);
        Member member = getMemberInternal(mtype, slot);
        if (member == null) {
            throw new LinkageError((StringOfSymbols.isFieldMember(mtype) ? "NoSuchFieldError" : "NoSuchMethodError")+ ": "+
                                   (StringOfSymbols.isStaticMember(mtype) ? "static " : "") +
                                   getName()+" slot:"+slot);
        }
        return member;
    }


   /**
    * Iterate through the suite fields defined by classes in this
    * class's hierarchy, setting the oop map bits for reference fields.
    */
    void setOopMap(byte[] oopMap) {
        for (int slot = 0 ;; slot++) {
            Member m = getMemberInternal(SquawkConstants.MTYPE_INSTANCEFIELDS, slot);
            if (m == null) {
                return;
            }
            switch (m.type()) {
                case CNO.BOOLEAN:
                case CNO.BYTE:
                case CNO.SHORT:
                case CNO.CHAR:
                case CNO.INT:
                case CNO.LONG:
                case CNO.LONG2:
                case CNO.FLOAT:
                case CNO.DOUBLE:
                case CNO.DOUBLE2:
                    break;

                case CNO.NULL:
                case CNO.PRIMITIVE:
                case CNO.VOID:
                    throw new LinkageError("Bad field type "+(int)m.type());

                default:
                    if ((m.offset() % 4) != 0) {
                        throw new LinkageError("Bad pointer offset "+m.offset());
                    }
                    int wordoffset = m.offset() / 4;
                    int bitoffset  = wordoffset % 8;
                    int byteoffset = wordoffset / 8;
                    oopMap[byteoffset] |= (1 << bitoffset);
                    break;
            }
        }
    }


    /**
     * getMemberInternal
     *
     * @param mtype
     * @param slot
     * @param isStatic
     * @return
     */
    private Member getMemberInternal(int mtype, int slot) {
        Romizer.assume(lookupSlot == -9999);
        lookupSlot = slot;
        Member m = getMemberInternalPrim(mtype, StringOfSymbols.isStaticMember(mtype));
        lookupSlot = -9999;
        return m;
    }
    private static int lookupSlot = -9999;


    /**
     * getMemberInternalPrim
     *
     * @param mtype
     * @param slot
     * @return
     */
    private Member getMemberInternalPrim(int mtype, boolean isStatic) {
        if (!isStatic) {
            Relocator superType = suite_superType();
            if (superType != null) {
                Member m = superType.getMemberInternalPrim(mtype, false);
                if (m != null) {
                    return m;
                }

               /*
                * Adjust the slot to account for slots defined by this suite's super class
                */
                lookupSlot -= superType.suite_getSlotsCount(mtype);
            }
        }

        if (lookupSlot >= suite_getSlotsCount(mtype)) {
            return null;
        } else {
            return getSuiteMember(mtype, lookupSlot);
        }
    }


    /*
     * Work out if this type can be assigned to another type.
     */
    final boolean isAssignableTo(Relocator aClass) {
       /*
        * Quickly check for equalty, the most common case.
        */
        if (this == aClass) {
           return true;
        }

       /*
        * Check to see of this class is somewhere in aType's hierarchy
        */
        if (isSubclassOf(aClass)) {
            return true;
        }

       /*
        * If aClass is an interface see if this class implements it
        */
        if (aClass.isInterface() && isImplementorOf(aClass)) {
             return true;
        }

       /*
        * This is needed to cast arrays of classes into arrays of interfaces
        */
        if (isElementAssignableTo(aClass)) {
            return true;
        }

       /*
        * Otherwise there is no match
        */
        return false;
    }
}

