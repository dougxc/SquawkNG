package java.lang;

import java.util.*;
import java.io.*;

import com.sun.squawk.vm.CNO;
import com.sun.squawk.vm.SquawkConstants;

//import com.sun.squawk.loader.*;
//import com.sun.squawk.suite.*;

/**
 * The Klass class is a Squawk VM specific version of the class Class. It is
 * transformed by the translator such that it replaces Class and the translator
 * tranforms all reference to Class to refer to this class instead.
 *
 * This class is API compatible with Class but also contains extra fields and
 * methods specific to the Squawk system.
 */
public final class Klass {

    /*@vmaccessed: eeprom */

    /*
     * These are the instance fields of Klass that encapsulate all
     * the meta-info for a Klass. This class can represent any class
     * described in a valid suite file with the added constraint that
     * it complies with the "Minimal Virtual Machine" appendix to the
     * "The Squawk System" specification. The constraints imposed by
     * this appendix are reproduced below for ease of reference:
     *
     *   1.  Each method can use no more than 256 local variables.
     *   2.  Arrays can be no larger than 65536 elements long.
     *   3.  Methods can be no larger than 65536 bytes long.
     *   4.  There may be no more than 256 classes in a suite.
     *   5.  There may be no more than 256 suites loaded on a card.
     *   6.  There may be no more than 256 virtual methods in a single class's hierarchy.
     *   7.  There may be no more than 256 static and <init> methods in a single class.
     *   8.  There may be no more than 256 non-static fields in a single class's hierarchy.
     *   9.  There may be no more than 256 static fields in a single class.
     *   10. There may be no more than 256 methods in an interface's hierarchy.
     *
     */

    /**
     * The super class of this class.
     */
    public final Klass superClass; /*@vmaccessed: read */

    /**
     * The symbolic information for the class. This includes the signatures
     * of all the public and protected fields and methods of the class. It
     * also includes the name of the class.
     * @reflected
     */
    public final StringOfSymbols symbols; /*@vmaccessed: read */

    /**
     * The ordered set of interface types implemented by this class.
     */
    public final char[] interfaceTypes; /*@vmaccessed: read */

    /**
     * The mapping from the logical offset of each interface method
     * implemented to the virtual methods that implement them.
     */
    public final byte[][] interfaceSlotTables; /*@vmaccessed: read */

    /**
     * The pool of class references used by the bytecode of
     * this class to refer to other class.
     */
    public final char[] classReferences; /*@vmaccessed: read */

    /**
     * The pool of object constants used by the bytecode of
     * this class.
     */
    public final Object[] objectReferences; /*@vmaccessed: read */

    /**
     * The bit map for an instance of this class describing which
     * words of the instance contain pointer values.
     */
    public final byte[] oopMap; /*@vmaccessed: read */

    /**
     * The table of static methods declared by this class.
     */
    public final byte[][] staticMethods; /*@vmaccessed: read */

    /**
     * The vtable for this class.
     *
     * The vtable for a class includes slots for all the virtual methods
     * defined by this class. It will also contain slots for methods
     * defined in the super class if any of these methods are overridden.
     * As the vtable entries represent a contiguous range of vtable indexes,
     * a vtable slot that corresponds to a super class's method that is *not*
     * overridden will be null in this class's vtable. This is so the
     * VM can efficiently set the current class pointer when resolving
     * a method executing an INVOKEVIRTUAL instruction.
     */
    public final byte[][] virtualMethods; /*@vmaccessed: read */

    /**
     * The 16 bit identifier for this class. The high 8 bits denote the
     * suite to which this class belongs and the low 8 bits is the
     * unique identifier for this class within its suite.
     */
    public final char type; /*@vmaccessed: read */

    /**
     * The access flags for this class.
     */
    public final char access; /*@vmaccessed: read */

    /**
     * The 16 bit identifier for the class of the elements of this class
     * if it represents an array. Otherwise 0.
     */
    public final char elementType; /*@vmaccessed: read */

    /**
     * The size (in words) of an instance of this class. This includes the
     * size of any fields declared by the superclass(es).
     */
    public final byte instanceFieldsLength; /*@vmaccessed: read unsigned */

    /**
     * The total size (in words) of the static fields of this class. As
     * static fields are not packed by the VM, each static field occupies
     * one word except for doubles and longs which occupy two words.
     */
    public final byte staticFieldsLength; /*@vmaccessed: read unsigned */

    /**
     * The total size (in words) of the pointer static fields of this class.
     */
    public final byte pointerStaticFieldsLength; /*@vmaccessed: read unsigned */

    /**
     * The index of the first virtual method contained in the vtable for this
     * class. Any virtual method whose index is lower than this value is
     * accessed from the vtable of the superclass.
     */
    public final byte firstVirtualMethod; /*@vmaccessed: read unsigned */

    /**
     * The vtable index for the default constructor of this class or
     * -1 if no such method exists.
     */
    public final byte slotForInit; /*@vmaccessed: read unsigned */

    /**
     * The vtable index for this class's <clinit> method or
     * -1 if no such method exists.
     */
    public final byte slotForClinit; /*@vmaccessed: read unsigned */

    /**
     * The vtable index for this class's 'public static void main(String[])'
     * method or -1 if no such method exists.
     */
    public final byte slotForMain; /*@vmaccessed: read unsigned */

    public Klass(
        StringOfSymbols symbols,
        char            type,
        char            access,
        Klass           superClass,
        char            elementType,
        char[]          interfaceTypes,
        byte[][]        interfaceSlotTables,
        char[]          classReferences,
        Object[]        objectReferences,
        byte            instanceFieldsLength,
        byte            staticFieldsLength,
        byte            pointerStaticFieldsLength,
        byte            firstVirtualMethod,
        byte[]          oopMap,
        byte[][]        staticMethods,
        byte[][]        virtualMethods,
        int[]           overriddenAccess,
        byte            slotForInit,
        byte            slotForClinit,
        byte            slotForMain
    ) {

        this.symbols                      = symbols;
        this.type                         = type;
        this.access                       = access;
        this.superClass                   = superClass;
        this.elementType                  =
/*if[NEWSTRING]*/
                                            (type == CNO.STRING) ? CNO.CHAR :
                                            (type == CNO.STRING_OF_BYTES) ? CNO.BYTE :
/*if[NEWSYMBOLS]*/
                                            (type == CNO.STRING_OF_SYMBOLS) ? CNO.BYTE :
/*end[NEWSYMBOLS]*/
/*end[NEWSTRING]*/
                                            elementType;
        this.interfaceTypes            = interfaceTypes;
        this.interfaceSlotTables       = interfaceSlotTables;
        this.classReferences           = classReferences;
        this.objectReferences          = objectReferences;
        this.instanceFieldsLength      = instanceFieldsLength;
        this.staticFieldsLength        = staticFieldsLength;
        this.pointerStaticFieldsLength = pointerStaticFieldsLength;
        this.firstVirtualMethod        = firstVirtualMethod;
        this.oopMap                    = oopMap;
        this.staticMethods             = staticMethods;
        this.virtualMethods            = virtualMethods;
        this.slotForInit               = slotForInit;
        this.slotForClinit             = slotForClinit;
        this.slotForMain               = slotForMain;
    }

//=============================================================================================================================
//                                                       General things
//=============================================================================================================================

    /**
     * Returns the super class of this class. This will be <code>null</code>
     * in the case where this class represents <code>java.lang.Object</code>.
     * @return     the super class of this class.
     */
    public Klass getSuperClass() {
        return superClass;
    }

    /**
     * Return the unique identifier for this class.
     * @return the unique identifier for this class.
     */
    public char getType() {
        return type;
    }

    /**
     * If this class represents an array type, then return the unique identifier
     * for the type of the elements of the array. Otherwise, return 0.
     * @return the unique identifier for the element type of this array class or
     * 0 if this is not an array class.
     */
    public char getElementType() {
        return elementType;
    }

    /**
     * Return the Klass instance for the type of the elements of this array class.
     * @return the Klass instance for the type of the elements of this array class.
     * @throws InternalError if this is not an array class.
     *
     */
    public Klass getElementClass() {
        if (elementType == 0) {
            throw new InternalError(getNameInternal()+" is not an array class");
        }
        return SuiteManager.lookup(elementType);
    }

    /**
     * Get the number of virtual methods in this class's hierarchy.
     * @return the number of virtual methods in this class's hierarchy.
     */
    public int getVirtualMethodCount() {
        if (firstVirtualMethod == -1) {
            return getSuperClass().getVirtualMethodCount();
        }
        return firstVirtualMethod + virtualMethods.length;

    }

    /**
     * Get the number of static methods in this class.
     * @return the number of static methods in this class.
     */
    public int getStaticMethodCount() {
        return staticMethods.length;
    }

    /**
     * Get the number of words required for the static fields in this class.
     * @return the number of words required for the static fields in this class.
     */
    public int getStaticFieldsLength() {
        return staticFieldsLength;
    }

    /**
     * Get the number of words required for the non-primitive static fields in this class.
     * @return the number of words required for the non-primitive static fields in this class.
     */
    public int getPointerStaticFieldsLength() {
        return pointerStaticFieldsLength;
    }

    /**
     * Get the number of words required for the instance fields in this class's hierarchy.
     * @return the number of words required for the instance fields in this class's hierarchy.
     */
    public int getInstanceFieldsLength() {
        return instanceFieldsLength;
    }

    /**
     * Work out if this class can be assigned to another class.
     * @param klass The class to test against.
     * @return true if an instance of this class can be assigned to an instance
     * of 'klass'.
     */
    public boolean isAssignableTo(Klass klass) {
       /*
        * Quickly check for equalty, the most common case.
        */
        if (this == klass) {
           return true;
        }

       /*
        * Check to see of this class is somewhere in aType's hierarchy
        */
        if (isSubclassOf(klass)) {
            return true;
        }

       /*
        * If aClass is an interface see if this class implements it
        */
        if (klass.isInterface() && isImplementorOf(klass)) {
             return true;
        }

       /*
        * This is needed to cast arrays of classes into arrays of interfaces
        */
        if (isElementAssignableTo(klass)) {
            return true;
        }

       /*
        * Otherwise there is no match
        */
        return false;
    }

    /**
     * Work out if this class is a subclass of a specified class.
     *
     * @param klass The class to test against.
     * @return true if this class is a subclass of 'klass'.
     */
    public boolean isSubclassOf(Klass klass) {
        // Primitives never match non-primitives
        if (isPrimitive() != klass.isPrimitive()) {
            return false;
        }

        for (Klass thisClass = this ; thisClass != null ; thisClass = thisClass.getSuperClass()) {
            if (thisClass == klass) {
                return true;
            }
        }
        return false;
    }

    /**
     * Work out if this class implements a specified class.
     *
     * @param klass The class to test against.
     * @return true if 'klass' is an interface type and this class implements it.
     */
    public boolean isImplementorOf(Klass klass) {
        if (klass.isInterface()) {
            for (Klass thisClass = this ; thisClass != null ; thisClass = thisClass.getSuperClass()) {
               /*
                * The interface list in each class is a transitive closure of all the
                * interface types specified in the class file less those defined in the
                * superclass hierarchy. Therefore it is only necessary to check this list
                * and not the interfaces implemented by the interfaces, and then to do this
                * to the superclasses until it is matched.
                */
                for (int i = 0 ; i < thisClass.interfaceTypes.length ; i++) {
                    Klass k = SuiteManager.lookup(thisClass.interfaceTypes[i]);
                    if (k == klass) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /*
     * isElementAssignableTo
     */
    public boolean isElementAssignableTo(Klass aClass) {
        if (this.isArray() && aClass.isArray()) {
            Klass a = SuiteManager.lookup(this.elementType);
            Klass b = SuiteManager.lookup(aClass.elementType);
            return a.isAssignableTo(b);
        }
        return false;
    }

    /*
     * mustClinit
     * @return true if the class must be initialized before use
     */
    public boolean mustClinit() {
        return (access & SquawkConstants.ACC_MUSTCLINIT) != 0;
    }

/*if[FINALIZATION]*/
    /*
     * hasFinalizer
     * @return true if the class define a finalise() method
     */
    public boolean hasFinalizer() {
        return (access & SquawkConstants.ACC_HASFINALIZER) != 0;
    }
/*end[FINALIZATION]*/

    /**
     * isPrimitive
     */
    boolean isPrimitive() {
        return superClass != null && superClass.type == CNO.PRIMITIVE;
    }

    boolean isAbstract() {
        return (access & SquawkConstants.ACC_ABSTRACT) != 0;
    }

    /**
     * Returns the internal class name
     */
    public String getNameInternal() {
        return symbols.getClassName();
    }

    /**
     * Returns the internal class name
     */
    public boolean equalsInternalName(String name) {
        return symbols.equalsClassName(name);
    }

//=============================================================================================================================
//                                                     Class initialization
//=============================================================================================================================

    static KlassInitializationState initqueue;

    private final static int NOTINITIALIZED  = 0;
    private final static int INITIALIZING    = 1;
    private final static int INITIALIZED     = 2;
    private final static int FAILED          = 3;

    /*
     * Get the initialzation state (one of the above values)
     */
    private int getInitializationState() {
        if (Native.getClassState(this) != null) {
            return INITIALIZED;
        }
        KlassInitializationState p = initqueue;
        while (p != null && p.klass != this) {
            p = p.next;
        }
        if (p == null) {
            return NOTINITIALIZED;
        }
        if (p.thread == null) {
            return FAILED;
        }
        return INITIALIZING;
    }

    /*
     * Set the initialzation state to either INITIALIZING or FAILED
     */
    private void setInitializationState(Thread t) {
        KlassInitializationState first = initqueue;
        KlassInitializationState p = first;
        while (p != null && p.klass != this) {
            p = p.next;
        }
        if (p == null) {
            p = new KlassInitializationState();
            p.next = first;
            p.thread = t;
            p.klass = this;
            p.classState = createClassState();
            initqueue = p;
        } else {
            p.thread = t;
        }
    }

    /*
     * Get the initialzation thread
     */
    private Thread getInitializationThread() {
        KlassInitializationState p = initqueue;
        KlassInitializationState prev = null;
        while (p.klass != this) {
            prev = p;
            p = p.next;
            Native.assume(p != null);
        }
        return p.thread;
    }

    /*
     * Get the initialzation class state
     */
    private Object getInitializationClassState() {
        KlassInitializationState p = initqueue;
        KlassInitializationState prev = null;
        while (p.klass != this) {
            prev = p;
            p = p.next;
            Native.assume(p != null);
        }
        return p.classState;
    }

    /*
     * Remove the initialzation state (when state is INITIALIZED)
     */
    private void removeInitializationState() {
        KlassInitializationState p = initqueue;
        KlassInitializationState prev = null;
        while (p.klass != this) {
            prev = p;
            p = p.next;
            Native.assume(p != null);
        }
        if (prev == null) {
            initqueue = p.next;
        } else {
            prev.next = p.next;
        }
    }

    /*
     * The internal class initializion function. (See page 53 of the VM Spec.)
     */
    final Object initializeClass() {

       /*
        * Step 1
        */
        synchronized(this) {

           /*
            * Step 2
            */
            if (getInitializationState() == INITIALIZING) {
                if (getInitializationThread() != Thread.currentThread()) {
                    do {
                        try {
                            wait();
                        } catch (InterruptedException e) {}
                    } while (getInitializationState() == INITIALIZING);
                } else {
                   /*
                    * Step 3
                    */
                    return getInitializationClassState();
                }
            }

           /*
            * Step 4
            */
            if (getInitializationState() == INITIALIZED) {
                return Native.getClassState(this);
            }

           /*
            * Step 5
            */
            if (getInitializationState() == FAILED) {
                throw new NoClassDefFoundError();
            }

           /*
            * Step 6
            */
            setInitializationState(Thread.currentThread()); // state = INITIALIZING);
        }

       /*
        * Step 7
        */
        if (!isInterface()) {
            if (superClass != null && superClass.mustClinit() && superClass.getInitializationState() != INITIALIZED) {
                try {
                    superClass.initializeClass();
                } catch(Error ex) {
                    synchronized(this) {
                        setInitializationState(null); // state = FAILED;
                        notifyAll();
                    }
                    throw ex;
                } catch(Throwable ex) {
                    Native.fatalVMError();
                }
            }
        }

       /*
        * Step 8
        */
        try {
            clinit();

           /*
            * Step 9
            */
            synchronized(this) {
                Object cs = getInitializationClassState();
                Native.setClassState(this, cs);
                removeInitializationState(); // state = INITIALIZED;
                notifyAll();
                return cs;
            }
        } catch(Throwable ex) {
           /*
            * Step 10
            */
            if (!(ex instanceof Error)) {
                ex = new ExceptionInInitializerError(ex);
            }

           /*
            * Step 11
            */
            synchronized(this) {
                setInitializationState(null); // state = FAILED;
                notifyAll();
            }
            throw (Error)ex;
        }
    }


    private void clinit() {
        int slot = slotForClinit;
        if (slot != SquawkConstants.SLOT_UNDEFINED) {
            Native.callStaticNoParm(this, slot);
        }
    }


    void main(String[] args) {
        int slot = slotForMain & 0xFF;
        if (slot != SquawkConstants.SLOT_UNDEFINED) {
            Native.callStaticOneParm(args, this, slot);
        } else {
            throw new Error("Class "+getNameInternal()+" has no main() method");
        }
    }


    private Object createClassState() {
        Object cs = Native.createClassState(this);
        if (cs == null) {
            Native.gc();
            cs = Native.createClassState(this);
            if (cs == null) {
                throw VMExtension.outOfMemoryError;
            }
        }
        return cs;
    }





//=============================================================================================================================
//                                                "Normal" java.lang.Class methods
//=============================================================================================================================

    /**
     * Converts the object to a string. The string representation is the
     * string "class" or "interface", followed by a space, and then by the
     * fully qualified name of the class in the format returned by
     * <code>getName</code>.  If this <code>Class</code> object represents a
     * primitive type, this method returns the name of the primitive type.  If
     * this <code>Class</code> object represents void this method returns
     * "void".
     *
     * @return a string representation of this class object.
     */
    public String toString() {
        return (isInterface() ? "interface " :  "class ") + getName();
    }

    /**
     * Returns the <code>Class</code> object associated with the class
     * with the given string name.
     * Given the fully-qualified name for a class or interface, this
     * method attempts to locate, load and link the class.  If it
     * succeeds, returns the Class object representing the class.  If
     * it fails, the method throws a ClassNotFoundException.
     * <p>
     * For example, the following code fragment returns the runtime
     * <code>Class</code> descriptor for the class named
     * <code>java.lang.Thread</code>:
     * <ul><code>
     *   Class&nbsp;t&nbsp;= Class.forName("java.lang.Thread")
     * </code></ul>
     *
     * @param      className   the fully qualified name of the desired class.
     * @return     the <code>Class</code> descriptor for the class with the
     *             specified name.
     * @exception  ClassNotFoundException  if the class could not be found.
     * @since      JDK1.0
     */
    public static Klass forName(String className) throws ClassNotFoundException {
        Klass k = SuiteManager.forName(className);
        if (k != null) {
            if (k.mustClinit()) {
                k.initializeClass();
            }
            return k;
        }
        throw new ClassNotFoundException(className);
    }

    /**
     * Creates a new instance of a class.
     *
     * @return     a newly allocated instance of the class represented by this
     *             object. This is done exactly as if by a <code>new</code>
     *             expression with an empty argument list.
     * @exception  IllegalAccessException  if the class or initializer is
     *               not accessible.
     * @exception  InstantiationException  if an application tries to
     *               instantiate an abstract class or an interface, or if the
     *               instantiation fails for some other reason.
     * @since     JDK1.0
     */
    public Object newInstance() throws InstantiationException, IllegalAccessException {
        if (isInterface() || isAbstract() || isSquawkArray()) {
            throw new InstantiationException();
        }
        if (slotForInit == SquawkConstants.SLOT_UNDEFINED) {
            throw new IllegalAccessException();
        }
        Object res = Native.primNewObject(this, slotForInit & 0xFF);
        if (res == null) {
            Native.gc();
            res = Native.primNewObject(this, slotForInit & 0xFF);
            if (res == null) {
                throw VMExtension.outOfMemoryError;
            }
        }
        return res;
    }

    /**
     * Determines if the specified <code>Object</code> is assignment-compatible
     * with the object represented by this <code>Class</code>.  This method is
     * the dynamic equivalent of the Java language <code>instanceof</code>
     * operator. The method returns <code>true</code> if the specified
     * <code>Object</code> argument is non-null and can be cast to the
     * reference type represented by this <code>Class</code> object without
     * raising a <code>ClassCastException.</code> It returns <code>false</code>
     * otherwise.
     *
     * <p> Specifically, if this <code>Class</code> object represents a
     * declared class, this method returns <code>true</code> if the specified
     * <code>Object</code> argument is an instance of the represented class (or
     * of any of its subclasses); it returns <code>false</code> otherwise. If
     * this <code>Class</code> object represents an array class, this method
     * returns <code>true</code> if the specified <code>Object</code> argument
     * can be converted to an object of the array class by an identity
     * conversion or by a widening reference conversion; it returns
     * <code>false</code> otherwise. If this <code>Class</code> object
     * represents an interface, this method returns <code>true</code> if the
     * class or any superclass of the specified <code>Object</code> argument
     * implements this interface; it returns <code>false</code> otherwise. If
     * this <code>Class</code> object represents a primitive type, this method
     * returns <code>false</code>.
     *
     * @param   obj the object to check
     * @return  true if <code>obj</code> is an instance of this class
     *
     * @since JDK1.1
     */
    public boolean isInstance(Object obj) {
        return obj != null && obj.getKlass().isAssignableTo(this);
    }

    /**
     * Determines if the class or interface represented by this
     * <code>Class</code> object is either the same as, or is a superclass or
     * superinterface of, the class or interface represented by the specified
     * <code>Class</code> parameter. It returns <code>true</code> if so;
     * otherwise it returns <code>false</code>. If this <code>Class</code>
     * object represents a primitive type, this method returns
     * <code>true</code> if the specified <code>Class</code> parameter is
     * exactly this <code>Class</code> object; otherwise it returns
     * <code>false</code>.
     *
     * <p> Specifically, this method tests whether the type represented by the
     * specified <code>Class</code> parameter can be converted to the type
     * represented by this <code>Class</code> object via an identity conversion
     * or via a widening reference conversion. See <em>The Java Language
     * Specification</em>, sections 5.1.1 and 5.1.4 , for details.
     *
     * @param cls the <code>Class</code> object to be checked
     * @return the <code>boolean</code> value indicating whether objects of the
     * type <code>cls</code> can be assigned to objects of this class
     * @exception NullPointerException if the specified Class parameter is
     *            null.
     * @since JDK1.1
     */
    public boolean isAssignableFrom(Klass cls) {
        return cls.isAssignableTo(this);
    }

    /**
     * Determines if the specified <code>Class</code> object represents an
     * interface type.
     *
     * @return  <code>true</code> if this object represents an interface;
     *          <code>false</code> otherwise.
     */
    public boolean isInterface() {
        return (access & SquawkConstants.ACC_INTERFACE) != 0;
    }

    /**
     * Determines if this <code>Class</code> object represents an array class.
     *
     * @return  <code>true</code> if this object represents an array class;
     *          <code>false</code> otherwise.
     * @since   JDK1.1
     */
    public boolean isArray() {
        return     elementType != 0
                && type != CNO.STRING
                && type != CNO.STRING_OF_BYTES
/*if[NEWSYMBOLS]*/
                && type != CNO.STRING_OF_SYMBOLS
/*end[NEWSYMBOLS]*/
        ;
    }

    /**
     * Determines if this <code>Class</code> object represents an array class.
     *
     * @return  <code>true</code> if this object represents an array class;
     *          <code>false</code> otherwise.
     * @since   JDK1.1
     */
    public boolean isSquawkArray() {
        return elementType != 0;
    }


    /**
     * Returns the fully-qualified name of the entity (class, interface, array
     * class, primitive type, or void) represented by this <code>Class</code>
     * object, as a <code>String</code>.
     *
     * <p> If this <code>Class</code> object represents a class of arrays, then
     * the internal form of the name consists of the name of the element type
     * in Java signature format, preceded by one or more "<tt>[</tt>"
     * characters representing the depth of array nesting. Thus:
     *
     * <blockquote><pre>
     * (new Object[3]).getClass().getName()
     * </pre></blockquote>
     *
     * returns "<code>[Ljava.lang.Object;</code>" and:
     *
     * <blockquote><pre>
     * (new int[3][4][5][6][7][8][9]).getClass().getName()
     * </pre></blockquote>
     *
     * returns "<code>[[[[[[[I</code>". The encoding of element type names
     * is as follows:
     *
     * <blockquote><pre>
     * B            byte
     * C            char
     * D            double
     * F            float
     * I            int
     * J            long
     * L<i>classname;</i>  class or interface
     * S            short
     * Z            boolean
     * </pre></blockquote>
     *
     * The class or interface name <tt><i>classname</i></tt> is given in fully
     * qualified form as shown in the example above.
     *
     * @return  the fully qualified name of the class or interface
     *          represented by this object.
     */
    public String getName() {
        Klass base = this;
        int dims   = 0;
        while (base.isArray()) {
            base = base.getElementClass();
            dims++;
        }
        String name;
        if (base.isPrimitive()   &&
            base.type != CNO.VOID    &&
/*if[FLOATS]*/
            base.type != CNO.DOUBLE2 &&
/*end[FLOATS]*/
            base.type != CNO.LONG2) {
            switch (base.type) {
                case CNO.BOOLEAN: name = "Z"; break;
                case CNO.BYTE:    name = "B"; break;
                case CNO.CHAR:    name = "C"; break;
                case CNO.SHORT:   name = "S"; break;
                case CNO.INT:     name = "I"; break;
                case CNO.LONG:    name = "J"; break;
/*if[FLOATS]*/
                case CNO.FLOAT:   name = "F"; break;
                case CNO.DOUBLE:  name = "D"; break;
/*end[FLOATS]*/
                default: {
                    throw new RuntimeException("Unknown primitive class: "+base.getNameInternal());
                }
            }
        } else {
            name = base.getNameInternal();
            if (dims != 0) {
                name = "L"+name+";";
            }
        }
        while (dims-- != 0) {
            name = "["+name;
        }
        return name;
    }

    /**
     * Finds a resource with a given name.  This method returns null if no
     * resource with this name is found.  The rules for searching
     * resources associated with a given class are profile
     * specific.
     *
     * @param name  name of the desired resource
     * @return      a <code>java.io.InputStream</code> object.
     * @since JDK1.1
     */
    public java.io.InputStream getResourceAsStream(String name) {
        try {
            if (name.length() > 0 && name.charAt(0) == '/') {
                name = name.substring(1);
            } else {
                String className = this.getName();
                int dotIndex = className.lastIndexOf('.');
                if (dotIndex >= 0) {
                    name = className.substring(0, dotIndex + 1).replace('.', '/') + name;
                }
            }
            return javax.microedition.io.Connector.openInputStream("resource:"+name);
        } catch (java.io.IOException x) {
            return null;
        }
    }


    public byte[] getOopMap() {
        return oopMap;
    }
}

class KlassInitializationState {
    KlassInitializationState next;
    Thread thread;
    Klass klass;
    Object classState;
}