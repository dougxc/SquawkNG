package javacard.framework;

/**
 * Place holder for official checkpoint API.
 */
public final class JCSystem {

    JCSystem() {
    }

    /**
     * Make a specified object persistent and return a unique identifier to it.
     * @throws Exception if 'obj' is already persistent before
     */
    public static String makePersistent(Object obj) throws Exception { return null; }


    /**
     * Return the persistence-capable instance that corresponds to a
     * specified object identifier or null if doesn't exist.
     * @note the only way to get an object identifier is via the
     * makePersistent() method
     * @param objId
     */
    public static Object getObject(String objId) { return null; }

    /**
     * Checkpoint an object on which makePersistent has already been called.
     * @throws Exception if not makePersistent() was not invoked on
     * 'obj'.
     */
    public static void checkpoint(Object obj) throws Exception {}

    /**
     * Rollsback parameter instance.
     * @note Parameter instance must have been the parameter to
     * the makePersistent() methods or else this method will throw
     * an exception
     */
    public static void rollback(Object obj) throws Exception {}

    /**
     * method returns the ObjectID for a persistent object. The only way to
     * make an object persistent is by calling makePersistent() on it
     */
    public static String getObjectID(Object obj) { return null; }

}