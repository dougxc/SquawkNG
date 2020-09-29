package java.lang;


class ObjectAssociation implements
/*if[FINALIZATION]*/
                                    Runnable
/*end[FINALIZATION]*/
                                {
   /*
    * NOTE - There is an assertion in the garbage collector that
    * ObjectAssociations that are in the hashtable must never be
    * marked after the marking phase.
    *
    * For this reason it is important to make sure that the
    * garbage collector is not run whenever there is an
    * ObjectAssociation on a local variable or on the stack.
    *
    * The run() method does not matter because the ObjectAssociation
    * is not in the hashtable when this is executed.
    */

    /*@vmaccessed: */

    /**
     * The size of the monitor cache
     */
    private final static int ASSOCIATION_CACHE_SIZE = 6;

    /**
     * The next hashcode to be allocated
     */
    private static short nextHashcode;

    /**
     * The object for which this association belongs
     */
    private final Object object; /*@vmaccessed: read write wbopaque */

    /**
     * The next association in the queue.
     */
    private ObjectAssociation next; /*@vmaccessed: read write wbopaque */

    /**
     * The monitor for the object
     */
    private Monitor monitor; /*@vmaccessed: read wbopaque */

    /**
     * The hashcode the object
     */
    private short hashCode; /*@vmaccessed: read */

    /**
     * Flag to say the object has a finalizer
     */
    private boolean hasFinalizer; /*@vmaccessed: read */

    /**
     * Flag to say the object can be migrated into eeprom
     */
    private boolean isMigratable; /*@vmaccessed: read write */

    /**
     * Constructor
     */
    private ObjectAssociation(Object object) {
        this.object       = object;
        this.next         = Native.getAssociationQueue(object);
        Native.setAssociationQueue(object, this);
    }

    /**
     * getAssociation
     */
    private static ObjectAssociation getAssociationPrim(Object object) {
        ObjectAssociation first = Native.getAssociationQueue(object);

       /*
        * Quick test for empty queue
        */
        if (first == null) {
            return null;
        }

       /*
        * Quick test for object at head of the queue
        */
        if (first.object == object) {
            return first;
        }

       /*
        * Search for the object
        */
        ObjectAssociation prev = first;
        ObjectAssociation assn = first.next;
        while (assn != null && (assn.object != object)) {
            prev = assn;
            assn = assn.next;
        }

       /*
        * If not found then return null
        */
        if (assn == null) {
            return null;
        }

       /*
        * Unlink association and then link to head of queue
        */
        prev.next = assn.next;
        assn.next = first;
        Native.setAssociationQueue(object, assn);
        return assn;
    }

    /**
     * getAssociation
     */
    private static ObjectAssociation getAssociation(Object object) {
        ObjectAssociation assn = getAssociationPrim(object);
        if (assn == null) {
            assn = new ObjectAssociation(object);
        }
        return assn;
    }

    /**
     * getMonitorPrim
     */
    private static Monitor getMonitorPrim(Object object) {
        ObjectAssociation assn = getAssociationPrim(object);
        if (assn == null) {
            return null;
        }
        return assn.monitor;
    }


   /*
    * Public methods
    */


    /**
     * getMonitor
     */
    static Monitor getMonitor(Object object) {
        Monitor monitor = getMonitorPrim(object);
        if (monitor == null) {
           /*
            * Allocate the monitor before (possibly) allocating
            * the association. This is important because the association
            * might be collected when the monitor was allocated.
            */
            monitor = new Monitor();
            ObjectAssociation assn = getAssociation(object);
            assn.monitor = monitor;
        }
        return monitor;
    }


    /**
     * getHashcode
     */
    static int getHashCode(Object object) {
        ObjectAssociation assn = getAssociation(object);
        while (assn.hashCode == 0) {
            assn.hashCode = nextHashcode++;
        }
        return assn.hashCode;
    }

    /**
     * hasFinalizer
     */
    static void hasFinalizer(Object object) {
        ObjectAssociation assn = getAssociation(object);
        assn.hasFinalizer = true;
    }

    /**
     * isMigratable
     */
    static void markMigratable(Object object) {
        ObjectAssociation assn = getAssociation(object);
        assn.isMigratable = true;
    }

    /**
     * purgeAssociations
     */
    static void purgeAssociations(Object object) {

       /*
        * Avoid the monitor queue becoming too large due to unused entries.
        */
        ObjectAssociation assn = Native.getAssociationQueue(object);
        ObjectAssociation prev = null;

       /*
        * Skip the first six entries
        */
        for (int count = 0 ; assn != null && count < ASSOCIATION_CACHE_SIZE ; count++) {
            prev = assn;
            assn = assn.next;
        }

       /*
        * Unlink the unused monitors after this
        */
        while (assn != null) {
            if (assn.hashCode == 0 && !assn.hasFinalizer && !assn.isMigratable && (assn.monitor == null || !assn.monitor.isInUse)) {
                prev.next = assn.next;
            } else {
                prev = assn;
            }
            assn = assn.next;
        }
    }

/*if[FINALIZATION]*/

    /**
     * This is split into a separate function so that the association is
     * not referred to by a stack frame if the finalizer causes a gc.
     */
    private void recreateHashcode() {
        ObjectAssociation assn = new ObjectAssociation(object);
        assn.hashCode = hashCode;
    }

    /**
     * One of the uses of an ObjectAssociation is to finalize objects.
     * In this case the ObjectAssociation is used as the execution context
     * for a new Thread. See VMExtension.registerForFinalization();
     */
    public void run() {
        Native.assume(hasFinalizer);
        try {
           /*
            * If this association (which is now removed from the hashtable) had a
            * hashcode then another association must be created to remeneber this
            * before the finalizer is called
            */
            if (hashCode != 0) {
                recreateHashcode();
            }
            object.finalize();
        } catch(Throwable ex) {
        }
    }

/*end[FINALIZATION]*/

}