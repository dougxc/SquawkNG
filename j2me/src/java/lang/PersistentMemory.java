package java.lang;

import java.util.*;
import com.sun.squawk.util.IntHashtable;
import com.sun.squawk.vm.SquawkConstants;

/**
 * This class contains a number of static methods that provide an interface
 * to the underlying persistent object memory of the VM.
 */
public final class PersistentMemory {

    private static final int GC_MARK_STACK_SIZE = 20;

    /**
     * Get the array of suites loaded in the system.
     * @return the array of suites loaded in the system.
     */
    public static Suite[] getSuites() {
        return Native.getSuiteList();
    }

    /**
     * Update the array of suites loaded in the system.
     * @param suites the new array of suites loaded in the system.
     */
    public static void setSuites(Suite[] suites) {
        Native.setSuiteList(suites);
    }

    /**
     * Get an object from persistent memory by name.
     * @param name The name of the persistent object.
     * @return the object in persistent memory corresponding to 'name'.
     * @throws NoSuchElementException if the object does not exist.
     */
    public static Object get(String name) throws NoSuchElementException {
        Object object = null;
        PersistentRootEntry entry = (PersistentRootEntry)Native.getPersistentMemoryTable();
        while (entry != null) {
            String ename = entry.name;
            if (ename != null && ename.equals(name)) {
                object = entry.value;
                break;
            }
            entry = entry.next;
        }

        if (object != null) {
             return object;
        }
        if        (name.equals("new byte[0]"))                  { object = new byte[0];
        } else if (name.equals("new byte[0][]"))                { object = new byte[0][];
        } else if (name.equals("new char[0]"))                  { object = new char[0];
        } else if (name.equals("new char[0][]"))                { object = new char[0][];
        } else if (name.equals("new int[0]"))                   { object = new int[0];
        } else if (name.equals("new Object[0]"))                { object = new Object[0];
        } else if (name.equals("new byte[] {0}"))               { object = new byte[] {0};
        } else if (name.equals("new byte[] {1}"))               { object = new byte[] {1};
        } else if (name.equals("new byte[] {3}"))               { object = new byte[] {3};
        } else if (name.equals("new byte[] {7}"))               { object = new byte[] {7};
        } else if (name.equals("new byte[] {15}"))              { object = new byte[] {15};
        } else if (name.equals("new byte[] {31}"))              { object = new byte[] {31};
        } else if (name.equals("new short[0]"))                 { object = new short[0];
        } else if (name.equals("new short[MTYPE_LAST+1][]"))    { object = new short[SquawkConstants.MTYPE_LAST+1][];
        } else {
            throw new NoSuchElementException(name);
        }
        put(name, object);
        return object;
    }

    /**
     * Add or update a persistent object as a root in persistent memory.
     * @param name The name under which the object is to be stored.
     * @param object The persistent object to add. If the
     * @return the persistent object which was originally stored under 'name'
     * or null if there was no such object.
     * @throws IllegalStoreException if 'object' is not a persistent object.
     */
    public static Object put(String name, Object object) {
        // Make sure the name and object are both in persistent memory first
        object = makePersistentCopy(object);
        name   = (String)makePersistentCopy(name);

        PersistentRootEntry entry = (PersistentRootEntry)Native.getPersistentMemoryTable();
        PersistentRootEntry unusedEntry = null;
        while (entry != null) {
            String ename = entry.name;
            if (ename != null) {
                if (ename.equals(name)) {
                    Object o = entry.value;
                    entry.value = object;
                    return o;
                }
            } else if (unusedEntry == null) {
                // Re-use first available unused entry
                unusedEntry = entry;
            }
            entry = entry.next;
        }

        if (unusedEntry == null) {
            unusedEntry = new PersistentRootEntry();
            unusedEntry.value = object;
            unusedEntry.name  = name;

            PersistentRootEntry table = (PersistentRootEntry)Native.getPersistentMemoryTable();
            if (table != null) {
                unusedEntry.next = table;
            }
            table = (PersistentRootEntry)makePersistentCopy(unusedEntry);
            Native.setPersistentMemoryTable(table);
        } else {
            // Must write object first as writing the name essentially
            // links this entry into the roots and therefore must be
            // atomic.
            unusedEntry.value = object;
            unusedEntry.name  = name;
        }
        return null;
    }

    /**
     * Removes an entry from the persistent objects table.
     * @param name The name of the object to be removed.
     * @return  the object to which the name had been mapped in the table,
     *          or <code>null</code> if the name did not have a mapping.
     */
    public static Object remove(String name) {
        PersistentRootEntry entry = (PersistentRootEntry)Native.getPersistentMemoryTable();
        while (entry != null) {
            String ename = entry.name;
            if (ename != null && ename.equals(name)) {
                Object o = entry.value;
                entry.name = null;
                entry.value = null;
                return o;
            }
            entry = entry.next;
        }
        return null;
    }

    /**
     * Mark the object grapg as being migratable into eeprom
     * @param object root of the graph
     */
    public static void allowMigration(Object object) {
        if (Native.inRam(object)) {
            ObjectAssociation.markMigratable(object);
        }
    }

    /**
     * If <code>root</code> is null or is not in temporary memory, then
     * this method does nothing and returns immediately.
     * Otherwise, the graph of objects reachable from <code>root</code>
     * are copied to persistent memory. All references
     * to any of the objects copied are automatically updated. That is,
     * once the operation is complete, there will be no references to the
     * objects in the graph and therefore they will have been garbage collected
     * @param root The root of an object graph to be copied from temporary
     * memory to persistent memory.
     */
    public static void makePersistent(Object root) {
        root = Native.makePersistent(root);
        if (root == null) {
            throw VMExtension.outOfMemoryError;
        } else {
            Native.gc();
        }
    }

    /**
     * If <code>root</code> is null or is not in temporary memory, then
     * this method does nothing and returns <code>root</code> immediately.
     * Otherwise, the graph of objects reachable from <code>root</code>
     * are copied to persistent memory. All references in objects external to
     * the graph to any of the objects copied are not updated
     * and will therefore keep the original objects alive in temporary
     * memory until they are null'ed or go out of scope.
     * @param root The root of an object graph to be copied from temporary
     * memory to persistent memory.
     * @return the pointer to the copy of the graph in persistent memory or
     * the <code>root</code> if it is null or not in temporary memory.
     */
    public static Object makePersistentCopy(Object object) {
        object = Native.makePersistentCopy(object);
        if (object == null) {
            throw VMExtension.outOfMemoryError;
        } else {
            return object;
        }
    }

    /**
     * Returns the amount of free persistent memory in the system. Calling the
     * <code>gc</code> method may result in increasing the value returned
     * by <code>freeMemory.</code>
     *
     * @return  an approximation to the total amount of memory currently
     *          available for future allocated objects, measured in bytes.
     */
    public static long freeMemory() {
        return Native.freeMemory(false);
    }

    /**
     * Returns the total amount of memory in the Java Virtual Machine.
     * The value returned by this method may vary over time, depending on
     * the host environment.
     * <p>
     * Note that the amount of memory required to hold an object of any
     * given type may be implementation-dependent.
     *
     * @return  the total amount of memory currently available for current
     *          and future objects, measured in bytes.
     */
    public static long totalMemory() {
        return Native.totalMemory(false);
    }


    /**
     * Run the garbage collector on the persistent memory.
     */
    public static void gc() {
        // Collecting RAM before collecting EEPROM means that only live objects
        // in RAM with pointers to EEPROM will be considered as roots while
        // marking EEPROM objects.
        Native.gc();

        int wordSize = Native.getPersistentMemorySize()/4;
        try {
            int[] bitVector = new int[(wordSize / 32) + 2];
            int[] markingStack = new int[GC_MARK_STACK_SIZE];
            Native.gcPersistentMemory(bitVector, markingStack);
        } catch (OutOfMemoryError oome) {
            throw new OutOfMemoryError("Cannot allocate auxillary data structures for persistent gc");
        }
    }
}

class PersistentRootEntry {
    String name;
    Object value;
    PersistentRootEntry next;
}
