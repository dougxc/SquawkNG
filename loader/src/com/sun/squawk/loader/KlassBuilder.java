package com.sun.squawk.loader;

import java.io.*;
import com.sun.squawk.suite.*;
import com.sun.squawk.vm.CNO;
import com.sun.squawk.vm.SquawkConstants;
import com.sun.squawk.vm.SquawkConstants;

/**
 * A KlassBuilder instance is used to convert a SuiteClass into a Klass.
 */
class KlassBuilder implements SymbolEditor {

    private static final int ACCESS_NONE       = 0;
    private static final int ACCESS_PROTECTED  = 1;
    private static final int ACCESS_PACKAGE    = 2;

    private int accessFilter;

    /**
     * The enclosing SuiteLoader.
     */
    private final SuiteLoader loader;

    /**
     * The Relocator for the class being built.
     */
    private final SuiteRelocator relocator;

    /**
     * The suite file components of the class being built.
     */
    private final SuiteClass sc;

    static final char[][] NO_INTERFACE_SLOT_TABLES = (char[][]) PersistentMemory.get("new char[0][]");
    static final byte[][] NO_METHODS               = (byte[][]) PersistentMemory.get("new byte[0][]");
    static final char[]   NO_TYPES                 = (char[])   PersistentMemory.get("new char[0]");
    static final Object[] NO_OBJECTS               = (Object[]) PersistentMemory.get("new Object[0]");
    static final int[]    NO_INTS                  = (int[])    PersistentMemory.get("new int[0]");

    private byte     firstVirtualMethod = -1; // Default value when there are no virtual methods
    private byte[][] staticMethods;
    private byte[][] virtualMethods;

    private byte slotForInit   = SquawkConstants.SLOT_UNDEFINED;
    private byte slotForClinit = SquawkConstants.SLOT_UNDEFINED;
    private byte slotForMain   = SquawkConstants.SLOT_UNDEFINED;

    private boolean hasFinalizer = false;
    private boolean hasAbstractOrNativeMethod = false;

    /**
     * Constructor
     */
    KlassBuilder(SuiteLoader loader, SuiteRelocator relocator) {
        Romizer.assume(relocator != null);
        this.loader    = loader;
        this.relocator = relocator;
        this.sc        = relocator.getSuiteClass();
    }

    /**
     * Record the slot of a given method if it is the main method,
     * the class initializer or the default constructor.
     *
     * @param methodImpl
     * @param method
     * @param slot
     */
    private void recordSpecialSlot(boolean isStatic, int access, int slot) {
        if (isStatic) {
            if ((access & SquawkConstants.ACC_MAIN) != 0) {
                slotForMain = (byte)slot;
            }
            if ((access & SquawkConstants.ACC_CLINIT) != 0) {
                slotForClinit = (byte)slot;
            }
            if ((access & SquawkConstants.ACC_DEFAULTINIT) != 0) {
                slotForInit = (byte)slot;
            }
        }
    }

    /**
     * Get the method declaration info for a given method implementation.
     *
     * @param methodImpl
     * @return
     */
    private Member getMethodForImpl(SuiteMethodImpl methodImpl) {
        int entry = methodImpl.entry;
        if (methodImpl.isStatic()) {
            return loader.getMember(methodImpl.parent, SquawkConstants.MTYPE_STATICMETHODS, entry);
        } else {
            return loader.getMember(methodImpl.parent, SquawkConstants.MTYPE_VIRTUALMETHODS, entry);

        }
    }

    /**
     * Get the table of method implementations into which the implementation
     * currently being added will be inserted. The table is created if
     * it has not already been created.
     *
     * @param isStatic Indicates if the current method implementation is static.
     * @param slot The offset of the current method implementation.
     * @return
     */
    private byte[][] getMethodTable(boolean isStatic, int slot) {
        if (isStatic) {
            if (staticMethods == null) {
                staticMethods = new byte[relocator.internal_getStaticMethodCount()][];
            }
            return staticMethods;
        } else {
            // Sanity test on the method offset
            if (slot < 0) {
                throw new LinkageError("ClassFormatError: method slot is out of range: "+slot);
            }

            // Create the vtable if necessary
            if (virtualMethods == null) {
                firstVirtualMethod = (byte)slot;
                int rcount = relocator.internal_getVirtualMethodCount();
                int size = rcount-firstVirtualMethod;
                if (size <= 0) {
                    throw new LinkageError("ClassFormatError: method slot is out of range: "+slot);
                }
                virtualMethods = new byte[size][];
            }
            return virtualMethods;
        }
    }

    /**
     * Adjust the vtable index for a virtual method so that it is relative
     * to the first entry in the vtable for this class.
     *
     * @param slot
     * @return
     */
    private int adjustVirtualMethodSlot(int slot) {
/*if[FINALIZATION]*/
        if (slot == SuiteLoader.getSlotForFinalize()) {
            hasFinalizer = true;
        }
/*end[FINALIZATION]*/
        return slot - firstVirtualMethod;
    }

    /**
     * Add a method body to the class currently under construction. The relevant
     * table of methods is also created here if it has not already been created.
     * This will be the table of static methods if 'methodImpl' is a static
     * method and it will be the vtable if 'methodImpl' is a virtual method.
     *
     * @param methodImpl
     */
    void addMethodImpl(SuiteMethodImpl methodImpl) {
        int symbolPosition;

        Member method = getMethodForImpl(methodImpl);
        int slot = method.offset();
        byte[][] methodTable = getMethodTable(methodImpl.isStatic(), slot);

        // Adjust the slot for a virtual method
        if (!methodImpl.isStatic()) {
            slot = adjustVirtualMethodSlot(slot);
        }

/*if[DEBUG.LOADER]*/
        if (loader.traceLoading()) {
            loader.traceOut.println("Loading method: "+method.toString(sc.getName(), methodTable == staticMethods, true, loader));
        }
/*end[DEBUG.LOADER]*/

        // Record the slot if this method is the main method,
        // the class initializer or the default constructor.
        recordSpecialSlot(methodImpl.isStatic(), method.access(), slot);

        // Verify that method's slot number is within range of the method table.
        if (slot < 0 || slot >= methodTable.length) {
            throw new LinkageError("ClassFormatError: method slot is out of range: "+slot);
        }

        // Verify that another definition is not being overwritten
        if (methodTable[slot] != null) {
            throw new LinkageError("ClassFormatError: redefinition of method slot "+slot);
        }

        // Verify the code and add it to the table
        Verifier.verify(methodImpl);
        methodTable[slot] = methodImpl.bytecodes;

        if (methodImpl.isAbstract() || methodImpl.isNative()) {
            hasAbstractOrNativeMethod = true;
        }

/*if[DEBUG.LOADER]*/
        if (SuiteLoader.debugOut != null) {
            int mtype = methodImpl.isStatic() ? SquawkConstants.MTYPE_STATICMETHODS : SquawkConstants.MTYPE_VIRTUALMETHODS;
            method = relocator.getMember(mtype, methodImpl.entry);
            SuiteLoader.debugOut.println("------------ "+method.toString(sc.getName(), methodImpl.isStatic(), true, loader)+" -----------");
            SuiteLoader.debugOut.println(new Disassembler().disassemble(methodImpl.bytecodes));
        }
/*end[DEBUG.LOADER]*/

    }

    /**
     * Change all type references in the symbols of a class to be in terms of
     * system type identifiers as opposed to suite file type identifiers.
     * @param symbols
     * @param accessFilter
     * @return the fixed up sysmbols string.
     */
    private StringOfSymbols fixup(StringOfSymbols symbols, int accessFilter) {
        if (accessFilter == ACCESS_NONE) {
            return symbols.removeMembers();
        } else {
            this.accessFilter = accessFilter;
            return symbols.edit(this); // Edit the symbols using the following editMember() function.
        }
    }

    /**
     * editMember
     * @param mtype
     * @param member
     * @param index
     * @return a member
     */
    public Member editMember(int mtype, Member member, int index) {
        int access = member.access();
        if (
               ((accessFilter == ACCESS_PROTECTED) && (access & (SquawkConstants.ACC_PUBLIC|SquawkConstants.ACC_PROTECTED)) != 0)            ||
               ((accessFilter == ACCESS_PACKAGE)   && (access & (SquawkConstants.ACC_PUBLIC|SquawkConstants.ACC_PROTECTED|SquawkConstants.ACC_PRIVATE)) != SquawkConstants.ACC_PRIVATE) ||
               (member.nameLength() > 0 && member.nameAt(0) == '<')
           ) {
            member.convertSuiteTypesToSystemTypes();
            return member;
        }
        return null;
    }


    /**
     * Get the Klass object that is the linked super class for this class.
     * @param suiteNumber
     * @param klassList
     * @return
     */
    private Klass getSystemSuperClass(int suiteNumber, Klass[] klassList) {
        char stype = sc.getSuperType();
        if (stype != 0) {
            stype = loader.getSystemTypeFor(stype);
            int sno = (stype>>8) & 0xFF;
            if (sno == suiteNumber) {
                int cno = stype & 0xFF;
                return klassList[cno];
            } else {
                return SuiteManager.lookup(stype);
            }
        }
        return null;
    }

    /**
     * Fix up the class references for this class to be in terms of system
     * types, not suite types. In addition, an extra class reference is added
     * for java.lang.VMExtension if this class has any abstract or native
     * methods as these methods have been given a body (by the Verifier) that
     * calls a static method in java.lang.VMExtension.
     *
     * @param classRefs
     * @param isSuite0
     * @return
     */
    private char[] fixupClassReferences(char[] classRefs, boolean isSuite0) {
        classRefs = loader.getSystemTypesFor(classRefs);

        if (hasAbstractOrNativeMethod) {
            int length = classRefs.length;
            Object old = classRefs;
            classRefs = new char[length + 1];
            System.arraycopy(old, 0, classRefs, 0, length);

            int cno = -1;
            if (isSuite0) {

                // We must be loading suite 0 - look up the relocators
                Relocator relocator = loader.findRelocatorForName("java.lang.VMExtension");
                if (relocator != null) {
                    cno = relocator.internal_getType();
                }
            } else {
                Klass klass = SuiteManager.forName("java.lang.VMExtension");
                if (klass != null) {
                    cno = klass.type;
                }
            }

            if (cno != -1) {
                classRefs[length] = (char)cno;
            } else {
                throw new InternalError("Cannot find java.lang.VMExtension");
            }

        }
        return classRefs;
    }

    /**
     * Finalise construction of the current class and add it to a specified
     * system table of classes for the suite in which this class will be placed.
     *
     * @param isFinal
     * @param suiteNumber
     * @param klassList
     */
    void finishKlass(boolean isFinal, int suiteNumber, Klass[] suiteClasses) {

        byte instanceFieldsLength = (byte)relocator.internal_getInstanceFieldsSize();
        byte[] oopMap = (byte[])PersistentMemory.get("new byte[0]");

        // Work out the oopmap
        int oopMapSize = (instanceFieldsLength+7)/8;
        if (oopMapSize > 0) {
            oopMap = new byte[oopMapSize];

            // If the super type is represented by a proxy class then its oop map
            // must be copied into the oop map of this class first.
            Relocator superReloc = relocator.suite_superType();
            if (superReloc != null && superReloc instanceof ProxyRelocator) {
                byte[] soop = ((ProxyRelocator)superReloc).getKlass().getOopMap();
                if (soop != null) {
                    System.arraycopy(soop, 0, oopMap, 0, soop.length);
                }
            }

            // Iterate through the suite fields defined by classes in this
            // class's hierarchy, setting the oop map bits for reference fields.
            relocator.setOopMap(oopMap);

            // Use some simple sentinel oopmaps if available
            if (oopMapSize == 1) {
                boolean shared = true;
                if      (oopMap[0] == 0)  oopMap = (byte[]) PersistentMemory.get("new byte[] {0}");
                else if (oopMap[0] == 1)  oopMap = (byte[]) PersistentMemory.get("new byte[] {1}");
                else if (oopMap[0] == 3)  oopMap = (byte[]) PersistentMemory.get("new byte[] {3}");
                else if (oopMap[0] == 7)  oopMap = (byte[]) PersistentMemory.get("new byte[] {7}");
                else if (oopMap[0] == 15) oopMap = (byte[]) PersistentMemory.get("new byte[] {15}");
                else if (oopMap[0] == 31) oopMap = (byte[]) PersistentMemory.get("new byte[] {31}");
                else {
                    shared = false;
                }
                if (shared) {
                    Romizer.logOopMapCreation(shared);
                }
            } else {
                Romizer.logOopMapCreation(false);
            }
        }


        char type = (char)(suiteNumber<<8 | relocator.finalClassNumber());
        char access = sc.getAccess();
        Klass superClass = getSystemSuperClass(suiteNumber, suiteClasses);

        if (type != CNO.OBJECT) {
            // If a superclass requires initialization then this class
            // must also be set to require it
            if ((superClass.access & SquawkConstants.ACC_MUSTCLINIT) != 0) {
                access |= SquawkConstants.ACC_MUSTCLINIT;
            }

/*if[FINALIZATION]*/
            if (hasFinalizer || (superClass.access & SquawkConstants.ACC_HASFINALIZER) != 0) {
                access |= SquawkConstants.ACC_HASFINALIZER;
            }
/*end[FINALIZATION]*/
        } else {
            Romizer.assume((access & SquawkConstants.ACC_HASFINALIZER) == 0);
            // java.lang.Object should never have a <clinit>
            Romizer.assume((access & SquawkConstants.ACC_MUSTCLINIT) == 0);
        }



        char[] classRefs = fixupClassReferences(sc.classReferences, suiteNumber == 0);

        // Create the Klass
        Klass result = new Klass(
                            fixup(sc.symbols, isFinal ? ACCESS_NONE : (Romizer.isTCK() ? ACCESS_PACKAGE : ACCESS_PROTECTED)),
                            type,
                            access,
                            getSystemSuperClass(suiteNumber, suiteClasses),
                            loader.getSystemTypeFor(relocator.suite_getElementType()),
                            loader.getSystemTypesFor(sc.interfaceTypes),
                            sc.interfaceSlotTables,
                            classRefs,
                            sc.objectReferences,
                            instanceFieldsLength,
                            (byte)relocator.internal_getStaticFieldsSize(false),
                            (byte)relocator.internal_getStaticFieldsSize(true),
                            firstVirtualMethod,
                            oopMap,
                            (staticMethods  == null) ? NO_METHODS : staticMethods,
                            (virtualMethods == null) ? NO_METHODS : virtualMethods,
                            sc.getOverriddenAccess(),
                            slotForInit,
                            slotForClinit,
                            slotForMain
                        );


        int n = relocator.finalClassNumber();
        Romizer.assume(n < suiteClasses.length);
        Romizer.assume(suiteClasses[n] == null);
        suiteClasses[n] = (Klass)PersistentMemory.makePersistentCopy(result);

/*if[DEBUG.LOADER]*/
        if (loader.traceLoading()) {
            loader.traceOut.println("Built class: "+sc.getName());
        }
/*end[DEBUG.LOADER]*/
    }
}
