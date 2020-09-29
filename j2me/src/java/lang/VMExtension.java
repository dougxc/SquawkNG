/*
 * Copyright 1994-2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

import java.io.*;
import java.util.Hashtable;
import com.sun.squawk.vm.SquawkConstants;

/**
 * This class is a repository of methods that are invoked directly by the VM to
 * do things that done more easily in Java code.
 */
public class VMExtension {

    /*@vmaccessed: */

    /**
     * The global pre-allocated exception thrown when the VM runs out of memory.
     */
    static OutOfMemoryError outOfMemoryError;

    /**
     * A global flag used to indicate when the start up process has reached a
     * state when complete threading functionality is enabled.
     */
    static boolean systemUp;

    /**
     * A global flag set to true when the VM is running a set of TCK tests.
     * This is simply used to change the exit value of the VM to the value
     * expected by the shell script running the tests when a negative test
     * is being run.
     */
    static boolean negativeTckTest;

    /**
     * A global flag used to modify the behaviour of the VM when running the
     * TCK in the following way:
     *
     * 1. Instantiating a LinkageError instance with a message that starts with
     *    "UntranslatableCodeError" will cause the VM to exit with status 105.
     *    This is for tests that the translator cannot yet handle.
     * 2. If the VM executes an extremely long or infinite Thread.wait, the
     *    VM will exit with status 99.
     */
    static boolean tckMode;

    /**
     * Must be here so that it is the zero'th static method
     */
    private VMExtension() {}

    /**
     * The first method invoked when the romized image is started.
     */
    static void vmstart() /*@vmaccessed: call */ {
        try {
            new VMExtensionStarter().startPrimaryThread();
        } catch(Throwable ex) {
            Native.println(ex.toString());
            ex.printStackTrace();
        }
        Native.fatalVMError();
    }

    /**
     * Cause a specified Class instance to be initialized. This involves
     * creating the structure to hold the static state of the class as
     * well as running its <clinit> (if necessary).
     *
     * @param cls
     */
    static void initializeClass(Klass cls) /*@vmaccessed: call */ {
        cls.initializeClass();
    }

    /**
     * Raised by the VM when an exceptional arithmetic condition has
     * occurred. For example, an integer "divide by zero" throws an
     * instance of this class.
     */
    static void arithmeticException() /*@vmaccessed: call */ {
       throw new ArithmeticException();
    }

    /**
     * Raised by the VM when an array has been accessed with an
     * illegal index. The index is either negative or greater than
     * or equal to the size of the array.
     */
    static void arrayBoundsException() /*@vmaccessed: call */ {
       throw new ArrayIndexOutOfBoundsException();
    }

    /**
     * Raised by the VM when an application attempts to use <code>null</code>
     * in a case where an object is required.
     */
    static void nullPointerException() /*@vmaccessed: call */ {
       throw new NullPointerException();
    }

    /**
     * Perform the necessary runtime check when casting an object to a given
     * class.
     *
     * @param obj
     * @param cls
     * @return
     */
    static Object checkcast(Object obj, Klass cls) /*@vmaccessed: call */ {
        Native.assume(cls != null);
        if (obj != null && !obj.getKlass().isAssignableTo(cls)) {
            throw new ClassCastException();
        }
        return obj;
    }

    /**
     * instanceOf
     */
    static boolean instanceOf(Object obj, Klass cls) /*@vmaccessed: call */ {
        return obj != null && cls != null && obj.getKlass().isAssignableTo(cls);
    }

    /*
     * astoreOop
     */
    static void astoreOop(Object array, int index, Object value) /*@vmaccessed: call */ {
        if (value != null) {
            Klass acls = array.getKlass();
            Klass vcls = value.getKlass();
            if (!vcls.isAssignableTo(acls.getElementClass())) {
                throw new ArrayStoreException();
            }
        }
        Native.primAstoreOop(array, index, value);
    }

    /*
     * yeild
     */
    static void yield() /*@vmaccessed: call */ {
        Thread.yield();
    }

    /*
     * getStatic
     */
    static int getStatic(int offset, Klass cls) /*@vmaccessed: call */ {
        Object classState = cls.initializeClass();
        return Native.primGetStatic(classState, offset);
    }

    /*
     * getStaticLong
     */
    static long getStaticLong(int offset, Klass cls) /*@vmaccessed: call */ {
        Object classState = cls.initializeClass();
        return Native.primGetStaticLong(classState, offset);
    }

    /*
     * putStatic
     */
    static void putStatic(int value, int offset, Klass cls) /*@vmaccessed: call */ {
        Object classState = cls.initializeClass();
        Native.primPutStatic(classState, offset, value);
    }

    /*
     * putStaticLong
     */
    static void putStaticLong(long value, int offset, Klass cls) /*@vmaccessed: call */ {
        Object classState = cls.initializeClass();
        Native.primPutStaticLong(classState, offset, value);
    }

    /*
     * putStaticObj
     */
    static void putStaticObj(Object value, int offset, Klass cls) /*@vmaccessed: call */ {
        Object classState = cls.initializeClass();
        Native.primPutStaticObj(classState, offset, value);
    }

    /*
     * monitorEnter
     */
    static void monitorEnter(Object obj) /*@vmaccessed: call */ {
        Thread.monitorEnter(obj);
    }

    /*
     * monitorExit
     */
    static void monitorExit(Object obj) /*@vmaccessed: call */ {
        Thread.monitorExit(obj);
    }

    /*
     * newArray
     */
    static Object newArray(int size, Klass cls) /*@vmaccessed: call */ throws InstantiationException {
        if (size < 0) {
            throw new NegativeArraySizeException();
        }
        if (cls.getElementType() == 0) {
            throw new InstantiationException();
        }
        //if (!cls.callerHasAccessToClass()) {
        //    throw new IllegalAccessException();
        //}
        Object res = Native.primNewArray(cls, size);
        if (res == null) {
            Native.gc();
            res = Native.primNewArray(cls, size);
            if (res == null) {
                throw VMExtension.outOfMemoryError;
            }
        }
        return res;
    }

    /*
     * newDimension
     */
    static Object newDimension(Object[] array, int nextDimention) /*@vmaccessed: call */ throws InstantiationException {
        Klass arrayClass = ((Object)array).getKlass();
        Klass elementClass = arrayClass.getElementClass();
        for (int i = 0 ; i < array.length ; i++) {
            if (array[i] == null) {
                array[i] = newArray(nextDimention, elementClass);
            } else {
                newDimension((Object[])array[i], nextDimention);
            }
        }
        return array;
    }

    /*
     * breakpoint
     */
    static void breakpoint() /*@vmaccessed: call */ {
        throw new RuntimeException("Breakpoint!");
    }

    /*
     * callrun
     */
    static void callrun(Thread t) /*@vmaccessed: call */ {
        t.callRun();
        Native.fatalVMError();
    }

    /**
     * Raise an error indicating that an application tried to
     * call an abstract method.
     *
     * Note: this method must be made public as it can be invoked from anywhere.
     */
    public static void abstractMethodError() /*@vmaccessed: call */ {
//       throw new AbstractMethodError();
       throw new LinkageError("AbstractMethodError");
    }

    /**
     * Raise an error indicating that a non-implemented native method was
     * invoked.
     *
     * Note: this method must be made public as it can be invoked from anywhere.
     */
    public static void unsatisfiedLinkError() /*@vmaccessed: call */ {
//       throw new UnsatisfiedLinkError();
       throw new LinkageError("UnsatisfiedLinkError");
    }

    /*
     * illegalStoreException
     */
    static void illegalStoreException() /*@vmaccessed: call */ {
       throw new IllegalStoreException();
    }

    /*
     * registerForFinalization
     */
    static void registerForFinalization(Object object) /*@vmaccessed: call */ {
/*if[FINALIZATION]*/
        ObjectAssociation.hasFinalizer(object);
/*end[FINALIZATION]*/
    }

/*if[TRANSIENT]*/
    static Hashtable transientFields;
    /**
     * Get the object contain the transient field of a specified object,
     * creating it first if necessary.
     * @param object
     * @return
     */
    static Object getTransientFields(Object object) /*@vmaccessed: call */ {
        Object transients = transientFields.get(object);
        if (transients == null) {
            try {
                Class transientsClass = Class.forName(object.getClass().getName() +
                    "$Transients");
                transients = transientsClass.newInstance();
                transientFields.put(object, transients);
            } catch (InstantiationException ex) {
                Native.fatalVMError();
            } catch (IllegalAccessException ex) {
                Native.fatalVMError();
            } catch (ClassNotFoundException ex) {
                Native.fatalVMError();
            }
        }
        return transients;
    }
/*end[TRANSIENT]*/

    /*
     * stopVM
     */
    static void stopVM(int status) /*@vmaccessed: call */ {
        if (negativeTckTest && status != 95) {
            status = 95;
        }
        Native.exit(status);
    }

}

/**
 * This is the VM startup thread.
 */
class VMExtensionStarter extends Thread {

    String[] args;

    public void run() {
        // Run the main method.
        try {
            getMainClass().main(args);
            args = null;
        } catch (Throwable ex) {
            Native.print("Uncaught exception in the main thread: ");
            ex.printStackTrace();
            VMExtension.stopVM(-1);
        }
    }

    Klass getMainClass() {
        Klass result = null;

        // Setup the out of memory object
        VMExtension.outOfMemoryError = new OutOfMemoryError();
        Native.setOutOfMemoryError(VMExtension.outOfMemoryError);

/*if[TRANSIENT]*/
        // Set up the transient fields association map
        VMExtension.transientFields = new Hashtable();
/*end[TRANSIENT]*/

        // Garbage collect persistent memory if the VM was shutdown during
        // such a collection (which leaves the free list in an inconsistent state).
        if (Native.wasPersistentMemoryGCInterrupted()) {
            PersistentMemory.gc();
            Native.assume(!Native.wasPersistentMemoryGCInterrupted());
        }

        // Setup the interpreter's object association hashtable
        Native.setAssociationHashtable(new Object[SquawkConstants.ASSN_hashtableSize]);

/*if[FAST.MONITORS]*/
        // Setup the interpreter's fast lock stack
        Native.setFastLockStack(new Object[16]);
/*end[FAST.MONITORS]*/

        // Get the command line arguments
        args = Native.getCommandLineArgs();

        // Strip off and process -XX... args
        int mainClassIndex = 0;
        while (mainClassIndex != args.length) {
            String arg = args[mainClassIndex];
            if (!arg.startsWith("-XX")) {
                break;
            }
            else if (arg.startsWith("-XXnogc")) {
                Runtime.allowUserGC = false;
            } else if (arg.startsWith("-XXD")) {
                int equalsIndex = arg.indexOf('=', 4);
                if (equalsIndex != -1) {
                    String name = arg.substring(4, equalsIndex);
                    String value = arg.substring(equalsIndex+1);
                    Native.setProperty(name, value);
                } else {
                    Native.println("Badly formed -XXD<name>=<value> options: " + arg);
                    System.exit(-1);
                }
/*if[RESOURCE.CONNECTION]*/
            } else if (arg.startsWith("-XXresourcepath:")) {
                com.sun.squawk.io.j2me.resource.Protocol.setResourcePath(arg.substring("-XXresourcepath:".length()));
/*end[RESOURCE.CONNECTION]*/
            } else {
                Native.println("Unknown switch " + arg);
                System.exit(-1);
            }
            mainClassIndex++;
        }

        if (mainClassIndex != 0) {
            String[] old = args;
            args = new String[args.length - mainClassIndex];
            System.arraycopy(old, mainClassIndex, args, 0, args.length);
        }

        // Do a monitorEnter before allowing excessive GC
        synchronized(this) {
            Native.enableExcessiveGC();
            VMExtension.systemUp = true;
        }

        // Check that there is a class name
        if (args.length == 0) {
            Native.println("usage: <className> [parms...]");
            System.exit(-1);
        }

        // Strip off the first argument
        String mainClassName = args[0];
        Object old = args;
        args = new String[args.length - 1];
        System.arraycopy(old, 1, args, 0, args.length);

        // Get the system properties from the hosting JVM (if any)
        Native.updateSystemPropertiesFromHostJVM();

        // Lookup the class
        try {
            result = Klass.forName(mainClassName);
        } catch(ClassNotFoundException ex) {
            Native.println("Cannot find class " + mainClassName);
            System.exit(-1);
        }

        return result;
    }
}
