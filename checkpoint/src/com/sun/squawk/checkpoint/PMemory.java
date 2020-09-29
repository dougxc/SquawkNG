package com.sun.squawk.checkpoint;

import java.util.*;
import java.lang.reflect.*;
import java.io.*;
import sun.misc.Unsafe;

/**
 * This class provides facilities for interacting with an object store.
 *
 * To use this class, the '-Xbootclasspath/a:' command line option must be
 * used to append the classpath of this class to the bootstrap classpath.
 * Otherwise, a SecurityException will occur when the sun.misc.Unsafe class
 * is accessed.
 */
public class PMemory {

    /* ------------------------------------------------------------------------ *\
     *                     Public API                                           *
    \* ------------------------------------------------------------------------ */

    /**
     * Copy an object into persistent memory and designate it as a persistent
     * root identified by a specified identifier. This operation will traverse
     * the graph of objects reachable by 'object'. For each object that does
     * not already have a persistent copy, a persistent copy is made. For each
     * object that does have a persistent copy, the state of the persistent copy
     * is updated to match the state of the non-persistent copy.
     *
     * <b>This is the "makePersistent" operation.</b>
     *
     * @param id The identifier for the root.
     * @param object The object to copy and designate as a persistent root. If
     * the value of 'object' is null, then 'id' is removed as a persistent root
     * identifier.
     */
    public synchronized static void put(String id, Object object) {
        instance.putImpl(id, object);
    }

    /**
     * Retrieve a non-persistent copy of the persistent object corresponding to
     * a specified identifier. This operation will traverse the graph of
     * persistent objects denoted by 'id'. For each object that does
     * not already have a non-persistent copy, a non-persistent copy is made.
     * For each object that does have a non-persistent copy, the state of the
     * non-persistent copy is left unchanged. The latter point is the primary
     * difference between get() and restore().
     *
     * <b>This is the "getObject" operation.</b>
     *
     * @param id The identifier of the persistent root to retrieve a
     * non-persistent copy of.
     * @return The non-persistent copy of the persistent object corresponding
     * to'id' or null if 'id' does not denote a persistent root.
     */
    public synchronized static Object get(String id) {
        return instance.getImpl(id);
    }


    /**
     * Commit all the changes that have been made to a graph of non-persistent
     * objects to the persistent store.
     *
     * <b>This is the "checkpoint" operation.</b>
     *
     * @param id This identifies a persistent root whose corresponding non-
     * persistent graph will be committed.
     */
    public synchronized static void save(String id) {
        instance.saveImpl(id);
    }

    /**
     * Undo all the changes that have been made to a graph of non-persistent
     * objects since their last commit. This is the "rollback" operation.
     * The object states are rolled back to their last commit state. Note that
     * this may be a later state than when they were last committed via 'id'.
     * This will occur for objects that are reachable by another persistent
     * root.
     *
     * <b>This is the "rollback" operation.</b>
     *
     * @param id This identifies a persistent root whose corresponding non-
     * persistent graph will be rolled back.
     */
    public synchronized static void restore(String id) {
        instance.restoreImpl(id);
    }

    /**
     * This is not really part of the public API but is provided so that
     * a power cycle can be simulated and the resulting state changes be
     * tested.
     */
    public synchronized static void reset() {
        instance.resetImpl();
    }

    /* ------------------------------------------------------------------------ *\
     *                     Private implementation                               *
    \* ------------------------------------------------------------------------ */

    /**
     * The implementation of the public 'put' method.
     *
     * @param id
     * @param object
     */
    private void putImpl(String id, Object object) {
        assume(id != null);
        if (object == null) {
            roots.remove(id);
        } else {
            assume(!isPersistent(object));
            roots.put(id, getPersistentCopy(object, true, new Hashtable()));
        }
    }

    /**
     * The implementation of the public 'get' method.
     *
     * @param id
     * @return
     */
    private Object getImpl(String id) {
        Object persistentObject = roots.get(id);
        if (persistentObject == null) {
            return null;
        } else {
            return getNonPersistentCopy(persistentObject, false, new Hashtable());
        }
    }

    /**
     * The implementation of the public 'save' method.
     *
     * @param id
     */
    private void saveImpl(String id) {
        assume(id != null);
        Object persistentObject = roots.get(id);
        if (persistentObject != null) {
            Object object = getNonPersistentObject(persistentObject);
            if (object != null) {
                assume(!isPersistent(object));
                getPersistentCopy(object, true, new Hashtable());
            }
        }
    }

    /**
     * The implementation of the public 'restore' method.
     *
     * @param id
     */
    private void restoreImpl(String id) {
        Object persistentObject = roots.get(id);
        if (persistentObject != null) {
            Object object = getNonPersistentObject(persistentObject);
            if (object != null) {
                getNonPersistentCopy(persistentObject, true, new Hashtable());
            }
        }

    }

    /* ------------------------------------------------------------------------ *\
     *                     Store state and helper methods                       *
    \* ------------------------------------------------------------------------ */

    /**
     * Prevent external construction.
     */
    private PMemory() {
    }

    /**
     * Singleton instance.
     */
    private final static PMemory instance = new PMemory();

    /**
     * The roots of the persistent store. That is, the objects that can be
     * accessed via a String identifier.
     */
    private Hashtable roots = new Hashtable();

    /**
     * The mapping from persistent objects to their corresponding non-persistent
     * copies. Every persistent object in the system has an entry in this table.
     */
    private IdentityHashtable store = new IdentityHashtable();

    /**
     * A sentinel value representing the absence of a non-persistent copy of
     * a persistent object.
     */
    private static final Object NULL = new Object();

    /**
     * Return the persistent object corresponding to a specifed non-persistent
     * object or null if no such persistent object exists.
     *
     * @param object A non-persistent object.
     * @returm
     */
    private Object getPersistentObject(Object object) {
        assume(!isPersistent(object), idString(object));
        Enumeration keys = store.keys();
        Enumeration values = store.elements();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = values.nextElement();
            if (value == object) {
                return key;
            }
        }
        return null;
    }

    /**
     * Return the non-persistent object corresponding to a specifed persistent
     * object or null if no such non-persistent object exists.
     *
     * @param persistentObject A persistent object.
     * @returm The non-persistent object corresponding to 'persistentObject' or
     * null if no such non-persistent object exists.
     */
    private Object getNonPersistentObject(Object persistentObject) {
        assume(isPersistent(persistentObject), idString(persistentObject));
        Object object = store.get(persistentObject);
        assume(persistentObject != object, "persistentObject="+idString(persistentObject)+", object="+idString(object));
        if (object == NULL) {
            object = null;
        }
        return object;
    }

    /**
     * Make a clone of a specified object.
     *
     * @param object
     * @return
     */
    private Object deepClone(Object source, Class clazz) {
        if (clazz.isArray()) {
            int length = Array.getLength(source);
            Object target = Array.newInstance(clazz.getComponentType(), length);
            System.arraycopy(source, 0, target, 0, length);
            return target;
        } else if (source instanceof Serializable) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(source);
                oos.close();
                byte[] serializedSource = baos.toByteArray();
                ByteArrayInputStream bais = new ByteArrayInputStream(serializedSource);
                ObjectInputStream ois = new ObjectInputStream(bais);
                Object target = ois.readObject();
                ois.close();
                return target;
            } catch (ClassNotFoundException ex) {
                throw new InternalError();
            } catch (IOException ex) {
                throw new InternalError();
            }
        } else {
            throw new RuntimeException("Could not clone object of type: " + clazz.getName());
        }
    }

    /**
     * Helper method to catch exceptions thrown by Field.get();
     *
     * @param obj
     * @param field
     * @return
     */
    private Object getFieldValue(Object obj, Field field) {
        try {
            return field.get(obj);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("unexpected: "+ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("unexpected: "+ex);
        }
    }

    private static final Unsafe unsafe = Unsafe.getUnsafe();

    /**
     * Helper method to catch exceptions thrown by Field.set();
     *
     * @param obj
     * @param field
     * @return
     */
    private void setFieldValue(Object obj, Field field, Object value) {
        try {
///*
            int key = unsafe.fieldOffset(field);
            Class fieldType = field.getType();
            if (fieldType.isPrimitive()) {
                String fieldTypeName = fieldType.getName();
                if (fieldType == Boolean.TYPE) {
                    if (value == null) {
                        unsafe.putBoolean(obj, key, false);
                    } else {
                        unsafe.putBoolean(obj, key, ((Boolean)value).booleanValue());
                    }
                } else if (fieldType == Byte.TYPE) {
                    if (value == null) {
                        unsafe.putByte(obj, key, (byte)0);
                    } else {
                        unsafe.putByte(obj, key, ((Byte)value).byteValue());
                    }
                } else if (fieldType == Character.TYPE) {
                    if (value == null) {
                        unsafe.putChar(obj, key, (char)0);
                    } else {
                        unsafe.putChar(obj, key, ((Character)value).charValue());
                    }
                } else if (fieldType == Short.TYPE) {
                    if (value == null) {
                        unsafe.putShort(obj, key, (short)0);
                    } else {
                        unsafe.putShort(obj, key, ((Short)value).shortValue());
                    }
                } else if (fieldType == Integer.TYPE) {
                    if (value == null) {
                        unsafe.putInt(obj, key, 0);
                    } else {
                        unsafe.putInt(obj, key, ((Integer)value).intValue());
                    }
                } else if (fieldType == Float.TYPE) {
                    if (value == null) {
                        unsafe.putFloat(obj, key, 0.0F);
                    } else {
                        unsafe.putFloat(obj, key, ((Float)value).floatValue());
                    }
                } else if (fieldType == Long.TYPE) {
                    if (value == null) {
                        unsafe.putLong(obj, key, 0);
                    } else {
                        unsafe.putLong(obj, key, ((Long)value).longValue());
                    }
                } else if (fieldType == Double.TYPE) {
                    if (value == null) {
                        unsafe.putDouble(obj, key, 0.0);
                    } else {
                        unsafe.putDouble(obj, key, ((Double)value).doubleValue());
                    }
                } else {
                    throw new InternalError();
                }
            } else {
                unsafe.putObject(obj, key, value);
            }
//*/
/*
            field.set(obj, value);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("unexpected: "+ex);
*/
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("unexpected: "+ex);
        }
    }

    /**
     * A cache of the field descriptor arrays for each class. This simply speeds
     * up the use of reflection which is otherwise quite slow.
     */
    private Hashtable cachedPersistentFields = new Hashtable();
    private Hashtable cachedTransientFields  = new Hashtable();
    private static Field[] NO_FIELDS = new Field[0];


    /**
     * Retrieve the array of field descriptors for the non-transient,
     * non-static fields of a specified class.
     *
     * @param clazz The class for which the field descriptor array is requested.
     * @return
     */
    private Field[] getPersistentFields(Class clazz) {
        Field[] fields = (Field[])cachedPersistentFields.get(clazz);
        if (fields == null) {
            Field[] flds = clazz.getDeclaredFields();
            Vector all = new Vector(flds.length);
            if (flds.length != 0) {
                for (int i = 0; i != flds.length; ++i) {
                    Field field = flds[i];
                    int modifiers = field.getModifiers();
                    // We're not interested in static or transient fields
                    if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
                        field.setAccessible(true);
                        all.addElement(field);
                    }
                }
            }
            fields = all.size() != 0 ? new Field[all.size()] : NO_FIELDS;
            all.copyInto(fields);
            cachedPersistentFields.put(clazz, fields);
        }
        return fields;
    }

    /**
     * Retrieve the array of field descriptors for the transient,
     * non-static fields of a specified class. This does a special
     * check for fields in certain classes that are declared transient
     * but are not really transient under the checkpoint model. These
     * are exactly the classes that are special cased in copy() and
     * update().
     *
     * @param clazz The class for which the field descriptor array is requested.
     * @return
     */
    private Field[] getTransientFields(Class clazz) {
        Field[] fields = (Field[])cachedTransientFields.get(clazz);
        if (fields == null) {
            Field[] flds = clazz.getDeclaredFields();
            Vector all = new Vector(flds.length);
            if (flds.length != 0) {
                for (int i = 0; i != flds.length; ++i) {
                    Field field = flds[i];
                    int modifiers = field.getModifiers();
                    // We're not interested in static or non-transient fields
                    if (!Modifier.isStatic(modifiers) && Modifier.isTransient(modifiers)) {
                        field.setAccessible(true);
                        all.addElement(field);
                    }
                }
            }
            fields = all.size() != 0 ? new Field[all.size()] : NO_FIELDS;
            all.copyInto(fields);
            cachedTransientFields.put(clazz, fields);
        }
        return fields;
    }

    /**
     * Test whether or not a specified object is persistent.
     *
     * @param object The object to test.
     * @return true if 'object' is persistent.
     */
    private boolean isPersistent(Object object) {
        return store.containsKey(object);
    }

    /* ------------------------------------------------------------------------ *\
     *                   Putting/saving objects into store                      *
    \* ------------------------------------------------------------------------ */

    /**
     * Given a non-persistent object, get the corresponding persistent object,
     * creating it first if necessary.
     *
     * @param object A non-persistent object.
     * @param update If true and there already exists a persistent object
     * for 'object', then update its state from 'object'.
     * @param marked The remembered set of objects that have already
     * been traversed.
     * @return the persistent object corresponding to 'object'.
     */
    private Object getPersistentCopy(Object object, boolean update, Hashtable marked) {
        return copy(object, true, marked, NONPERSISTENT_TO_PERSISTENT);
    }

    /**
     * Update the persistent copy of an object graph based on its corresponding
     * non-persistent copy.
     *
     * @param object A non-persistent object.
     * @param persistentObject The persistent object corresponding to 'object'.
     * @param marked The remembered set of objects that have already
     * been traversed.
     */
    private void updatePersistentCopy(Object object, Object persistentObject, Hashtable marked) {
        update(object, persistentObject, marked, NONPERSISTENT_TO_PERSISTENT);
    }

    /* ------------------------------------------------------------------------ *\
     *                   Getting/restoring objects from store                   *
    \* ------------------------------------------------------------------------ */

    /**
     * Given a persistent object, get the corresponding non-persistent object,
     * creating it first if necessary.
     *
     * @param object A persistent object.
     * @param update If true and there already exists a non-persistent object
     * for 'persistentObject', then update its state from 'persistentObject'.
     * @param marked The remembered set of objects that have already
     * been traversed.
     * @return the non-persistent object corresponding to 'persistentObject'.
     */
    private Object getNonPersistentCopy(Object persistentObject, boolean update, Hashtable marked) {
        return copy(persistentObject, update, marked, PERSISTENT_TO_NONPERSISTENT);
    }

    /**
     * Update the non-persistent copy of an object graph based on its corresponding
     * persistent copy.
     *
     * @param persistentObject A persistent object.
     * @param object The non-persistent object corresponding to 'persistentObject'.
     * @param marked The remembered set of objects that have already
     * been traversed.
     */
    private void updateNonPersistentCopy(Object persistentObject, Object object, Hashtable marked) {
        update(object, persistentObject, marked, PERSISTENT_TO_NONPERSISTENT);
    }

    /* ------------------------------------------------------------------------ *\
     *                     Assertion checking routines                          *
    \* ------------------------------------------------------------------------ */

    /**
     * Test a specified boolean condition and throw a runtime exception if it is
     * false.
     *
     * @param b The boolean condition to test
     * @param msg The message sent to the RuntimeException constructor.
     */
    private void assume(boolean b, String msg) {
        if (!b) {
            throw new RuntimeException("Assume failure: "+msg);
        }
    }

    /**
     * Test a specified boolean condition and throw a runtime exception if it is
     * false.
     *
     * @param b The boolean condition to test
     */
    private void assume(boolean b) {
        assume(b, "no message");
    }

    private static String idString(Object o) {
        return o == null ? "null" : System.identityHashCode(o) + ":" + o.getClass().getName();
    }

    /* ------------------------------------------------------------------------ *\
     *                   Moving objects between memory spaces                   *
    \* ------------------------------------------------------------------------ */

    /**
     * An object that implements the Association interface provides a mapping
     * between objects on either side of disjoint persistent/non-persistent
     * object memories.
     */
    interface Association {
        /**
         * Return the object (if any) corresponding to a specified object.
         *
         * @param source
         * @return
         */
        Object get(Object source);

        void put(Object source, Object target);
    }

    /**
     * Checks for existence of a non-static private method with a specified signature
     * defined by a specified class.
     */
    private static boolean hasPrivateMethod(Class cl, String name,
                       Class[] argTypes,
                       Class returnType)
    {
        try {
            Method meth = cl.getDeclaredMethod(name, argTypes);
            int mods = meth.getModifiers();
            return ((meth.getReturnType() == returnType) &&
                    ((mods & Modifier.STATIC) == 0) &&
                    ((mods & Modifier.PRIVATE) != 0));
        } catch (NoSuchMethodException ex) {
            return false;
        }
    }


    private static Hashtable specializedSerializationFilter = new Hashtable();

    /**
     * Determine if a specified class specializes its serialization.
     * @param cl
     * @return
     */
    private boolean hasSpecializedSerialization(Class cl) {
        Boolean b = (Boolean)specializedSerializationFilter.get(cl);
        if (b == null) {
            if (hasPrivateMethod(cl, "readObject", new Class[] { ObjectInputStream.class }, Void.TYPE) ||
                hasPrivateMethod(cl, "writeObject", new Class[] { ObjectInputStream.class }, Void.TYPE) ||
                hasPrivateMethod(cl, "readObjectNoData", new Class[0], Void.TYPE)) {
                b = Boolean.TRUE;
            } else {
                b = Boolean.FALSE;
            }
            specializedSerializationFilter.put(cl, b);
        }
        return b.booleanValue();
    }

    /**
     * Given an object in a source memory, get the corresponding object in a
     * target memory, creating it first if necessary.
     *
     * @param source An object in source memory.
     * @param update If true and there already exists an object in target
     * memory for 'source', then update its state from 'source'.
     * @param marked The remembered set of objects that have already
     * been traversed.
     * @param map The mapping between objects in source memory and target memory.
     * @return the object in target memory corresponding to 'source'.
     */
    private Object copy(Object source, boolean update, Hashtable marked, Association map) {
        assume(source != null);
        Object target = map.get(source);
        if (!marked.containsKey(source)) {
            if (target == null) {
                marked.put(source, source);

                Class clazz = source.getClass();
                Class elementType = clazz.getComponentType();

                // Handle arrays differently from non-array instances.
                if (elementType != null) {
                    // Clone the array
                    target = deepClone(source, clazz);

                    // Recurse on the elements of a non-primitive array
                    if (!elementType.isPrimitive()) {
                        Object[] destArray = (Object[])target;
                        Object[] sourceArray = (Object[])source;
                        for (int j = 0; j != sourceArray.length; ++j) {
                            Object sourceElement = sourceArray[j];
                            if (sourceElement != null) {
                                Object destElement = copy(sourceElement, update, marked, map);
                                destArray[j] = destElement;
                            }
                        }
                    }

                    // Register the mapping from source to target
                    map.put(source, target);

                } else {
                    target = deepClone(source, clazz);

                    // Iterate up the class hierachy of the object, copying all
                    // the non-transient fields.
                    while (clazz != null) {

                        // Handle classes that do a specialized form of serialization
                        if (hasSpecializedSerialization(clazz)) {
                            if (clazz == Hashtable.class) {
                                Hashtable sourceHT = (Hashtable)source;
                                Hashtable targetHT = (Hashtable)target;
                                Enumeration keys = sourceHT.keys();
                                Enumeration values = sourceHT.elements();
                                targetHT.clear();
                                while (keys.hasMoreElements()) {
                                    targetHT.put(copy(keys.nextElement(),   update, marked, map),
                                                 copy(values.nextElement(), update, marked, map));
                                }
                            } else {
                                throw new RuntimeException("Need special copy semantics for "+clazz.getName());
                            }
                        }

                        // Iterate over the non-transient fields of the object
                        Field[] fields = getPersistentFields(clazz);
                        for (int i = 0; i != fields.length; ++i) {
                            Field field = fields[i];
                            Class fieldType = field.getType();
                            int modifiers = field.getModifiers();
                            boolean isFinal = Modifier.isFinal(modifiers);

                            if (!fieldType.isPrimitive()) {
                                Object sourceValue = getFieldValue(source, field);
                                if (sourceValue != null) {
                                    Object destValue = copy(sourceValue, update, marked, map);
                                    setFieldValue(target, field, destValue);
                                }
                            }
                        }
                        clazz = clazz.getSuperclass();
                    }

                    // Register the mapping from source to target
                    map.put(source, target);
                }
            } else {
                if (update) {
                    update(source, target, marked, map);
                } else {
                    marked.put(source, source);
                }
            }
        } else {
            assume(target != null);
        }
        assume(marked.containsKey(source));
//System.out.println("=== copied(source="+idString(source)+", "+
//                              "target="+idString(target));
        return target;
    }


    /**
     * Update an object in target memory based on its corresponding object in
     * source memory.
     *
     * @param source A memory in source memory.
     * @param target The object in target memory corresponding to 'source' that
     * is to be updated from 'source'.
     * @param marked The remembered set of objects that have already
     * been traversed.
     * @param map The mapping between objects in source memory and target memory.
     */
    private void update(Object source, Object target, Hashtable marked, Association map) {
        assume(source != null);
        assume(target != null);

        if (!marked.containsKey(source)) {
//System.out.println("=== update(source="+idString(source)+", "+
//                              "target="+idString(target));
            marked.put(source, source);

            Class clazz = source.getClass();
            Class elementType = clazz.getComponentType();

            // Handle arrays differently from non-array instances.
            if (elementType != null) {
                // Copy the state of the source object into the target object
                System.arraycopy(source, 0, target, 0, Array.getLength(source));

                // Recurse on the elements of a non-primitive array
                if (!elementType.isPrimitive()) {
                    Object[] sourceArray = (Object[])source;
                    Object[] targetArray = (Object[])target;
                    for (int j = 0; j != sourceArray.length; ++j) {
                        Object sourceElement = sourceArray[j];
                        if (sourceElement != null) {
                            Object targetElement = map.get(sourceElement);
                            update(sourceElement, targetElement, marked, map);
                            targetArray[j] = targetElement;
                        }
                    }
                }
            } else {
                // Iterate up the class hierachy of the object, updating all
                // the non-transient fields.
                while (clazz != null) {

                    // Handle classes that do a specialized form of serialization
                    if (hasSpecializedSerialization(clazz)) {
                        if (clazz == Hashtable.class) {
                            Hashtable sourceHT = (Hashtable)source;
                            Hashtable targetHT = (Hashtable)target;
                            Enumeration keys = sourceHT.keys();
                            Enumeration values = sourceHT.elements();
                            targetHT.clear();
                            while (keys.hasMoreElements()) {
                                Object key = keys.nextElement();
                                Object value = values.nextElement();
                                Object targetKey = map.get(key);
                                Object targetValue = map.get(value);
                                targetHT.put(targetKey, targetValue);
                                update(key, targetKey, marked, map);
                                update(value, targetValue, marked, map);
                            }
                        } else {
                            throw new RuntimeException("Need special update semantics for "+clazz.getName());
                        }
                    }
                    // Iterate over the fields of the object
                    Field[] fields = getPersistentFields(clazz);
                    for (int i = 0; i != fields.length; ++i) {
                        Field field = fields[i];
                        Class fieldType = field.getType();
                        Object sourceValue = getFieldValue(source, field);
                        Object targetValue = null;
                        int modifiers = field.getModifiers();
                        boolean isFinal = Modifier.isFinal(modifiers);
                        boolean changed = false;
                        if (sourceValue != null) {
                            if (!fieldType.isPrimitive()) {
                                targetValue = map.get(sourceValue);
                                Object currentTargetValue = getFieldValue(target, field);
                                if (isFinal) {
                                    assume(currentTargetValue.equals(sourceValue) &&
                                           currentTargetValue == targetValue,
                                        "the value of a final field was changed: " + field);
                                } else {
                                    changed = (targetValue != currentTargetValue);
                                }

                                // Recursively update the fields of this field
                                update(sourceValue, targetValue, marked, map);
                            } else {
                                Object currentTargetValue = getFieldValue(
                                    target, field);
                                if (isFinal) {
                                    assume(currentTargetValue.equals(sourceValue), "the value of a final field was changed");
                                } else {
                                    targetValue = sourceValue;
                                    changed = (!targetValue.equals(currentTargetValue));
                                }
                            }
                        }

                        // Only update the field in the target if the new value
                        // does not match its current value
                        if (changed) {
                            setFieldValue(target, field, targetValue);
                        }
                    }
                    clazz = clazz.getSuperclass();
                }
            }
        }
    }

    /**
     * A mapping from objects in persistent memory to objects in
     * non-persistent memory.
     */
    private final Association PERSISTENT_TO_NONPERSISTENT = new Association() {

        /**
         * Get the non-persistent object from the store corresponding to a
         * specified persistent object.
         * @param object The persistent object key.
         * @return The non-persistent object corresponding to 'persistentObject'.
         */
        public Object get(Object persistentObject) {
            assume(isPersistent(persistentObject), "should be persistent: "+idString(persistentObject));
            return getNonPersistentObject(persistentObject);
        }

        /**
         * Add the mapping between a specified persistent object and a specified
         * non-persistent object to the store.
         * @param persistentObject
         * @param object
         */
        public void put(Object persistentObject, Object object) {
            assume(persistentObject != object);
            store.put(persistentObject, object);
        }
    };

    /**
     * A mapping from objects in non-persistent memory to objects in
     * persistent memory.
     */
    private final Association NONPERSISTENT_TO_PERSISTENT = new Association() {

        /**
         * Get the persistent object from the store corresponding to a
         * specified non-persistent object.
         * @param object The non-persistent object key.
         * @return The persistent object corresponding to 'object'.
         */
        public Object get(Object object) {
            assume(!isPersistent(object));
            return getPersistentObject(object);
        }

        /**
         * Add the mapping between a specified non-persistent object and a specified
         * persistent object to the store.
         * @param object
         * @param persistentObject
         */
        public void put(Object object, Object persistentObject) {
            assume(persistentObject != object);
            store.put(persistentObject, object);
        }
    };

    /* ------------------------------------------------------------------------ *\
     *                                  Testing                                 *
    \* ------------------------------------------------------------------------ */

    /**
     * Simulate a power cycle. This effectively resets all the transient
     * fields to their default values.
     */
    private synchronized void resetImpl() {
        for (Iterator iterator = store.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry)iterator.next();

            // Reset all the transient fields to their defaults
            Object obj = entry.getValue();
            Class clazz = obj.getClass();
            if (!clazz.isArray()) {
                while (clazz != null) {
                    Field[] fields = getTransientFields(clazz);
                    for (int i = 0; i != fields.length; ++i) {
                        Field field = fields[i];
                        if (!isSpecialTransientField(field)) {
                            setFieldValue(obj, field, null);
                        }
                    }
                    clazz = clazz.getSuperclass();
                }
            }
        }
    }

    /**
     * Determine whether a transient field is really transient under the
     * checkpoint model.
     * @param field
     * @return
     */
    private boolean isSpecialTransientField(Field field) {
        Class clazz = field.getDeclaringClass();
        if (clazz == Hashtable.class) {
            return (field.getName().equals("table") ||
                    field.getName().equals("count") );
        } else {
            return false;
        }
    }


    static void dump(Dictionary table, PrintStream out) {
        if (table instanceof TestHashtable) {
            TestHashtable ttable = (TestHashtable)table;
            out.println("  aPersistentPrimitive: "+ttable.aPersistentPrimitive);
            out.println("  aPersistentReference: "+idString(ttable.aPersistentReference));
            out.println("  aTransientPrimitive: "+ttable.aTransientPrimitive);
            out.println("  aTransientReference: "+idString(ttable.aTransientReference));
            out.println("  aFinalTransientPrimitive: "+ttable.aFinalTransientPrimitive);
            out.println("  aFinalPersistentReference: "+idString(ttable.aFinalPersistentReference));
        }
        out.println("  entries: ");
        Enumeration keys = table.keys();
        Enumeration values = table.elements();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = values.nextElement();
            StringBuffer buf = new StringBuffer("    ");
            buf.append(idString(key)).
                append('=').
                append(idString(value)).
                append("    (").
                append(key == table ? "(this map)" : key).
                append("=").
                append(value == table ? "(this map)" : value).
                append(")");


            out.println(buf);
        }
    }

    private void dump(PrintStream out) {
        out.println("PMemory.roots: ");
        dump(roots, out);
        out.println("PMemory.store: ");
        dump(store, out);
    }

//    private static final boolean TRACE = true;
    private static final boolean TRACE = true;

    /**
     * This is a simple harness for testing the correctness of the
     * implementation with respect to the semantics specified for the
     * four public methods that compose the persistent memory
     * internal API.
     * @param args
     */
    public static void main(String[] args) {
        PrintStream out = System.out;

        // Test 1: transient fields should be ignored. This uses a TestHashtable
        // which has some state in transient fields and some in non-transient
        // fields
        TestHashtable table = new TestHashtable(55,
                                                "persistentReference1",
                                                66,
                                                "transientReference1",
                                                77,
                                                "finalPersistentReference1");
        table.put("key1", "value1");
        table.put("key2", "value2");
        table.put("key3", "value3");

// Time 1

        // Commit the hash table to persistent memory under the identifier "table".
        PMemory.put("table", table);

// Time 2

        if (TRACE) {
            out.println("============= after init: ");
            out.println("table: ("+idString(table)+")");
            dump(table, out);
            instance.dump(out);
        }

        // Update the non-final non-transient values in the table
        table.put("key1", "valueA");
        table.put("key2", "valueB");
        table.put("key3", "valueC");
        table.aPersistentPrimitive = 155;
        table.aPersistentReference = "persistentReference2";

// Time 3

        // Update the non-final non-transient values in the table
        table.aTransientPrimitive = 166;
        table.aTransientReference = "transientReference2";

// Time 4

        if (TRACE) {
            out.println("============= before restore: ");
            out.println("table: ("+idString(table)+")");
            dump(table, out);
            instance.dump(out);
        }

        // Roll back the state of the table to just after the last time
        // it was committed (i.e. at Time 2)
        PMemory.restore("table");

// Time 5

        if (TRACE) {
            out.println("============= after restore: ");
            out.println("table: ("+idString(table)+")");
            dump(table, out);
            instance.dump(out);
        }

        // Test the state of the table
        String msg = "Incorrect state after restore()";
        instance.assume(table.get("key1") == "value1" &&
                        table.get("key2") == "value2" &&
                        table.get("key3") == "value3", msg);
        instance.assume(table.aPersistentPrimitive == 55, msg+": aPersistentPrimitive="+table.aPersistentPrimitive);
        instance.assume(table.aPersistentReference == "persistentReference1", msg+": aPersistentReference="+table.aPersistentReference);
        instance.assume(table.aTransientPrimitive == 166, msg+": aTransientPrimitive="+table.aTransientPrimitive);
        instance.assume(table.aTransientReference == "transientReference2", msg+": aTransientReference="+table.aTransientReference);
        instance.assume(table.aFinalTransientPrimitive == 77, msg+": aFinalTransientPrimitive="+table.aFinalTransientPrimitive);
        instance.assume(table.aFinalPersistentReference == "finalPersistentReference1", msg+": aFinalPersistentReference="+table.aFinalPersistentReference);

        // Test 2: power cycling
        reset();

// Time 6

        if (TRACE) {
            out.println("============= after reset: ");
            instance.dump(out);
        }

        Object oldTable = table;
        table = (TestHashtable)PMemory.get("table");

// Time 7

        if (TRACE) {
            out.println("============= after get: ");
            out.println("table: ("+idString(table)+")");
            dump(table, out);
            instance.dump(out);
            out.println("System.identityHashCode(table): "+System.identityHashCode(table));
            out.println("System.identityHashCode(oldTable): "+System.identityHashCode(oldTable));
        }

        msg = "Incorrect state after power cycle";
        instance.assume(table.get("key1").equals("value1") &&
                        table.get("key2").equals("value2") &&
                        table.get("key3").equals("value3"), msg);
        instance.assume(table.aPersistentPrimitive == 55, msg+": aPersistentPrimitive="+table.aPersistentPrimitive);
        instance.assume(table.aPersistentReference == "persistentReference1", msg+": aPersistentReference="+table.aPersistentReference);
        instance.assume(table.aTransientPrimitive == 0, msg+": aTransientPrimitive="+table.aTransientPrimitive);
        instance.assume(table.aTransientReference == null, msg+": aTransientReference="+table.aTransientReference);
        instance.assume(table.aFinalTransientPrimitive == 0, msg+": aFinalTransientPrimitive="+table.aFinalTransientPrimitive);
        instance.assume(table.aFinalPersistentReference == "finalPersistentReference1", msg+": aFinalPersistentReference="+table.aFinalPersistentReference);
    }

    static class TestHashtable extends Hashtable implements PersistentCapable {
        TestHashtable(int aPersistentPrimitive,
                      String aPersistentReference,
                      int aTransientPrimitive,
                      String aTransientReference,
                      int aFinalTransientPrimitive,
                      String aFinalPersistentReference) {
            super();
            this.aPersistentPrimitive = aPersistentPrimitive;
            this.aPersistentReference = aPersistentReference;
            this.aTransientPrimitive  = aTransientPrimitive;
            this.aTransientReference  = aTransientReference;
            this.aFinalTransientPrimitive  = aFinalTransientPrimitive;
            this.aFinalPersistentReference = aFinalPersistentReference;
        }

        int aPersistentPrimitive;
        String aPersistentReference;

        transient int aTransientPrimitive;
        transient String aTransientReference;

        final transient int aFinalTransientPrimitive;
        final String aFinalPersistentReference;

    }
}