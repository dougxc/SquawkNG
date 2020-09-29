package com.sun.squawk.loader;

import java.io.*;
import com.sun.squawk.suite.*;
import com.sun.squawk.vm.CNO;
import com.sun.squawk.vm.SquawkConstants;

/**
 * This is a relocator for a class in a suite that corresponds to an
 * existing internal class.
 */
class ProxyRelocator extends Relocator {

    /*
     * Static Constructor
     */
    static ProxyRelocator create(SuiteClass sc, Klass k, int cno) {
        if (cno >= CNO.PRIMITIVE && cno <= CNO.LAST_PRIMITIVE_TYPE) {
            return new PrimitiveProxyRelocator(sc, k);
        } else {
            return new ProxyRelocator(sc, k);
        }
    }

    /**
     * The internally linked class for which this is a proxy.
     */
    private final Klass k;

    /**
     * Initially the SuiteClass but later a short[][] with the member IDs
     */
    private Object something;

    /**
     * Constructor.
     */
    ProxyRelocator(Object sc, Klass k) {
        this.k = k;
        this.something = sc;
    }

   /* ------------------------------------------------------------------------ *\
    *                       Internal class methods                             *
    *                                                                          *
    * All methods in this section refer to the internals of the class as they  *
    * would be once the class is linked into the system.                       *
   \* ------------------------------------------------------------------------ */

   /**
    * Get the unique 16-bit identifier for the internally linked class.
    * @return the 16-bit identifier for the internally linked class.
    */
    final char internal_getType() {
        return k.getType();
    }

    /**
     * Get the number of static methods in this class.
     * @return number of static or virtual methods in this class.
     */
    final int internal_getStaticMethodCount() {
        return k.getStaticMethodCount();
    }

    /**
     * Get the number of virtual methods in this class's hierarchy.
     * @param superOnly If false, return the number of virtual methods declared
     * by this class and its super classes otherwise return only the
     * number of virtual methods declared by its super classes.
     * @return number of virtual methods in this class's hierarchy.
     */
    final int internal_getVirtualMethodCount() {
        return k.getVirtualMethodCount();
    }

    /**
     * Get the size in words required by the static fields of this class.
     * @param onlyReferenceFields If true, only count the non-primitive typed
     * static fields.
     * @return size in words of the static fields declared by this class.
     */
    final int internal_getStaticFieldsSize(boolean onlyReferenceFields) {
        return (onlyReferenceFields ? k.getPointerStaticFieldsLength() : k.getStaticFieldsLength());
    }

    /**
     * Get the size in words required by the instance fields of this
     * class's hierachy.
     * @param superOnly If false, return the size of the instance fields declared
     * by this class and its super classes otherwise return only the
     * size of the instance fields declared by its super classes.
     * @return size in words of the instance fields declared by this
     * class's hierachy.
     */
    final int internal_getInstanceFieldsSize() {
        return k.getInstanceFieldsLength();
    }

    /**
     * Determine if this class must be initialized before use. A class must
     * be initialized before use it it or any of its super classes
     * define a <clinit> method.
     *
     * @return true if the class must be initialized before use
     */
    final boolean internal_mustClinit() {
        return k.mustClinit();
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
    final Relocator suite_superType() {
        Klass sk = k;
        while (true) {
            sk = sk.getSuperClass();
            if (sk == null) {
                return null;
            }
            Relocator r = SuiteLoader.theLoader.findRelocatorForSystemType(sk.getType());
            if (r != null) {
                return r;
            }
           /*
            * There is no relocator for the super class of this class.
            * This means that the translator decided not to include it
            * the program must therefore look for it's superclass...
            */
        }
    }

    /**
     * If this relocator represents an array class, return the unique 16-bit
     * identifier (in the suite file namespace) its element type. Otherwise
     * return 0.
     * @return the 16-bit identifier for element type of the array class
     * represented by this relocator or 0 if this relocator does not represent
     * an array class.
     */
    final char suite_getElementType() {
        char systemType = k.getElementType();
        if (systemType == 0) {
            return 0;
        }
        return SuiteLoader.theLoader.findSuiteTypeForSystemType(systemType);
    }

    /**
     * Look up a member in this class and fill in the given Member container
     * with the details of the member.
     * @param minfo The container to fill with the member's details (if found).
     * @param mtype The member category.
     * @param index The suite index of the member.
     * @return true if the member was found, else false.
     */
    final Member getSuiteMember(int mtype, int slot) {
        short[][] mids = (short[][])something;

        Romizer.assume(mids != null);
        Romizer.assume(mids[mtype] != null);
        Romizer.assume(slot < mids[mtype].length);
        Romizer.assume(mids[mtype][slot] != -1);

        Member m = k.symbols.getMember(mids[mtype][slot]);
        m.convertSystemTypesToSuiteClasss();
        return m;
    }

    /**
     * Get the number of members in a specified category declared by this class
     * in a suite file.
     * @param mtype The category of members to consider.
     * @return the number of members in the category specified by <code>mtype</code>
     * declared by this class in a suite file.
     */
    int suite_getSlotsCount(int mtype) {
        short[][] mids = (short[][])something;
        Romizer.assume(mids != null);
        return mids[mtype] == null ? 0 : mids[mtype].length;
    }


   /* ------------------------------------------------------------------------ *\
    *                             Other methods                                *
   \* ------------------------------------------------------------------------ */

    /**
     * Get the fully qualified internal name of this class. The format of
     * the name is equivalent to that specified by Klass.getNameInternal().
     *
     * @return the fully qualified internal name of this class.
     */
    final String getName() {
        return k.getNameInternal();
    }

    /**
     * Get the symbols of this class.
     *
     * @return
     */
    final StringOfSymbols getSymbols() {
        return k.symbols;
    }

    /*
     * fixup
     */
    void fixup() {
        if (something instanceof SuiteClass) {
            StringOfSymbols proxy = ((SuiteClass)something).symbols;
            something = StringOfSymbols.getMemberIDTable(proxy, k);
            PersistentMemory.allowMigration(this);
        }
    }

    /*
     * isInterface
     */
    boolean isInterface() {
        return k.isInterface();
    }

    /*
     * isSubclassOf
     */
    boolean isSubclassOf(Relocator aClass) {
        if (aClass instanceof SuiteRelocator) {
            return false;
        }
        return k.isSubclassOf(((ProxyRelocator)aClass).k);
    }

    /*
     * isImplementorOf
     */
    boolean isImplementorOf(Relocator aClass) {
        if (aClass instanceof SuiteRelocator) {
            return false;
        }
        return k.isImplementorOf(((ProxyRelocator)aClass).k);
    }

    /*
     * isElementAssignableTo
     */
    boolean isElementAssignableTo(Relocator aClass) {
        if (aClass instanceof SuiteRelocator) {
            return false;
        }
        return k.isElementAssignableTo(((ProxyRelocator)aClass).k);
    }

    /**
     * Get the internally linked class represented by this proxy.
     * @return the internally linked class represented by this proxy.
     */
    Klass getKlass() {
        return k;
    }

//=============================================================================================================================
// TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP TEMP
//=============================================================================================================================

/*if[DEBUG.LOADER]*/

    void printClass(int n, SuiteLoader loader) {
        SuiteLoader.debugOut.println();
        SuiteLoader.debugOut.print("Proxy class "+n+" = "+getName()+" (system type "+(int)internal_getType()+")");
        Klass sk = k.getSuperClass();
        if (sk != null) {
            SuiteLoader.debugOut.print(" extends "+sk.getName());
        }
        SuiteLoader.debugOut.println();

        printMembers(SquawkConstants.MTYPE_INSTANCEFIELDS, loader);
        printMembers(SquawkConstants.MTYPE_STATICFIELDS,   loader);
        printMembers(SquawkConstants.MTYPE_VIRTUALMETHODS, loader);
        printMembers(SquawkConstants.MTYPE_STATICMETHODS,  loader);
    }

    void printMembers(int mtype, SuiteLoader loader) {
        String label = StringOfSymbols.MTYPE_NAMES[mtype];
        for (int slot = 0 ;; slot++) {
            try {
                Member m = getMember(mtype, slot);
                String name = m.toString(null, label.charAt(0) == 'S', label.charAt(1) == 'M', loader);
                SuiteLoader.debugOut.println(""+slot+"\t"+label+" ["+m.offset()+"] "+name+" access="+StringOfSymbols.intToHex(m.access()));
            } catch (LinkageError le) {
                break;
            }
        }
    }

/*end[DEBUG.LOADER]*/

}

/**
 * This is a very lightweight relocator for a proxy class representing a
 * primitive type.
 */
final class PrimitiveProxyRelocator extends ProxyRelocator {

    /**
     * Constructor
     */
    PrimitiveProxyRelocator(SuiteClass sc, Klass k) {
        super(PersistentMemory.get("new short[MTYPE_LAST+1][]"), k);
    }

    /**
     * There's no need to fix up a proxy for a primitive class.
     */
    void fixup() {
    }

}
