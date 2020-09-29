//J2C:objassoc.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/import java.io.*;
/*IFJ*/import java.util.*;

/*IFJ*/abstract public class ObjectAssociations extends MemoryAllocator {


/*---------------------------------------------------------------------------*\
 *                             Forward references                            *
\*---------------------------------------------------------------------------*/

        abstract boolean IsMarked(int obj);
        abstract boolean IsAlive(int obj);
        abstract int     KeepObject(int obj);
        abstract int     GetPossiblyForwarded(int obj);

/*---------------------------------------------------------------------------*\
 *                                 Variables                                 *
\*---------------------------------------------------------------------------*/

/*IFJ*/ private int[] newAssociationTable = new int[ASSN_hashtableSize];
//IFC// private int   newAssociationTable[ASSN_hashtableSize];


//IFC//#ifndef PRODUCTION
        private int associationCno;
        void setAssociationCno(int cno) {
            associationCno = cno;
        }
        int getAssociationCno() {
            return associationCno;
        }
//IFC//#else
//IFC//#define setAssociationCno(x) /**/
//IFC//#define getAssociationCno()  0
//IFC//#endif

/*---------------------------------------------------------------------------*\
 *                           Get association entry                           *
\*---------------------------------------------------------------------------*/

    /**
     * Find the next association for an object that has been marked
     * migratable (if any).
     */
    int lookupNextMigratable() {
        int i;
        for (i = 0 ; i < getAssociationHashtableSize() ; i++) {
            int assn = getAssociationQueue(i);
            while (assn != 0) {
                if (ObjectAssociation_isMigratable(assn)) {
                    return assn;
                }
                assn = ObjectAssociation_getNext(assn);
            }
        }
        return 0;
    }

/*---------------------------------------------------------------------------*\
 *                           New Association Table                           *
\*---------------------------------------------------------------------------*/


        /*
         * getAssociationTable
         */
/*MAC*/ int getNewAssociationTable(int $queue) {
            return newAssociationTable[$queue];
        }

        /*
         * setNewAssociationTable
         */
/*MAC*/ void setNewAssociationTable(int $queue, int $assn) {
            newAssociationTable[$queue] = $assn;
        }

        /*
         * clearNewAssociationTable
         */
/*MAC*/ void clearNewAssociationTable() {
            int i;
            for (i = 0 ; i < getAssociationHashtableSize() ; i++) {
                newAssociationTable[i] = 0;
            }
        }

        /*
         * copyNewAssociationTable
         */
/*MAC*/ void copyNewAssociationTable() {
            int i;
            for (i = 0 ; i < getAssociationHashtableSize() ; i++) {
                setAssociationQueue(i, newAssociationTable[i]);
            }
        }

        /**
         * traceAssociationQueue
         */
/*MAC*/ void traceAssociationQueue(String $msg) {
            if (getTraceAssociations())  {
                int count = getAssociationHashtableSize();
                int i;
                traceln($msg);
                for (i = 0 ; i < count ; i++) {
                    int mq = getAssociationQueue(i);
                    int m  = GetPossiblyForwarded(mq);
                    if (m != 0) {
                        trace("Queue ");
                        traceInt(i);
                        traceln(":");
                    }
                    while (m != 0) {
                        traceInstance(m);
                        traceln("");
                        m = GetPossiblyForwarded(ObjectAssociation_getNext(m));
                    }
                }
            }
        }

        /**
         * insertAssociation
         */
/*MAC*/ void insertAssociation(int $assn, int $object) {
            int queue = getAssociationHashEntry($object);
            int first = getNewAssociationTable(queue);
            if (getTraceAssociations())  {
                 trace("insertAssociation assn=");
                 traceInt($assn);
                 trace(" object=");
                 traceInt($object);
                 trace(" first=");
                 traceInt(first);
                 trace(" into queue ");
                 traceInt(queue);
                 traceln("");
            }
            assume($assn != first);
            ObjectAssociation_setNext($assn, first);
            setNewAssociationTable(queue, $assn);
        }


        /**
         * insertAssociationAsserting
         */
/*MAC*/ void insertAssociationAsserting(int $assn, int $object) {
            insertAssociation($assn, $object);
            assume(IsAlive($assn));
            assume(IsAlive($object));
        }


        /**
         * addToFinalizeQueue
         */
/*MAC*/ void addToFinalizerQueue(int $assn) {
            int first = getFinalizerQueue();
            if (getTraceAssociations())  {
                 trace("addToFinalizeQueue assn=");
                 traceInt($assn);
                 trace(" first=");
                 traceInt(first);
                 traceln("");
            }
            assume(IsAlive($assn));
            assume(first == 0 || IsAlive(first));
            ObjectAssociation_setNext_safe($assn, first);
            setFinalizerQueue($assn);
        }


        /**
         * markAssociationQueue
         */
        void markAssociationQueue(int queue) {

           /*
            * Get the queue of associations and zero the pointer
            */
            int assn = getAssociationQueue(queue);
            setAssociationQueue(queue, 0); // safety

            while (assn != 0) {

               /*
                * Get the next association before it is overwritten
                */
                int next    = ObjectAssociation_getNext(assn);
                int object  = GetPossiblyForwarded(ObjectAssociation_getObject(assn));

               /*
                * Check to see if the monitor is in use
                */
                int monitor = GetPossiblyForwarded(ObjectAssociation_getMonitor(assn));
                boolean monitorInUse = (monitor == 0) ? false : Monitor_isInUse(monitor);

                /*
                 * Clear the pointer to the next association so it will not get marked later
                 */
                ObjectAssociation_setNext_safe(assn, 0);

                /*
                 * Trace
                 */
                if (getTraceAssociations())  {
                    traceln("");
                    trace("markAssociationQueue assn=");
                    traceInt(assn);
                    trace(" next=");
                    traceInt(next);
                    trace(" object=");
                    traceInt(object);
                    trace(IsAlive(object) ? " ALIVE" : " DEAD");
                    trace(ObjectAssociation_hasFinalizer(assn) ? " HASFINALIZER" : "");
                    trace(ObjectAssociation_isMigratable(assn) ? " ISMIGRATABLE" : "");
                    if (ObjectAssociation_getHashCode(assn) != 0) {
                        trace(" hashcode=");
                        traceInt(ObjectAssociation_getHashCode(assn));
                    }
                    trace(" monitor=");
                    traceInt(monitor);
                    trace(monitorInUse ? " INUSE" : " NOTINUSE");
                    trace(" queue=");
                    traceInt(queue);
                    traceln("");
                }

                /*
                 * ObjectAssociations should not be in the old space. This is
                 * not an absolute requirement but it is better if this is
                 * always true because it prevents normally unreferenced objects
                 * being marked because a whole chain of associations were
                 * marked. If this assertion is true then the most likely reason
                 * is that gc was called when some code in ObjectAssociation.java
                 * has an association in the stack frame somewhere.
                 *
                 * Update: Unfortuately this assertion cannot be guaranteed as
                 * ObjectAssociations can creep into old space when an object
                 * in old space has been given a native hashcode or has a
                 * monitor.
                 */
//                assume(!IsMarked(assn));

                /*
                 * Keep the association if it's monitor in use or it's object
                 * is alive and the association has a finalizer, a hashcode,
                 * or is migratable.
                 */
                if (monitorInUse || (IsAlive(object) &&
                                     ObjectAssociation_getHashCode(assn) != 0 ||
                                     ObjectAssociation_hasFinalizer(assn) ||
                                     ObjectAssociation_isMigratable(assn))) {
                    /*
                     * Insert association back into the new hash table
                     */
                    insertAssociationAsserting(KeepObject(assn), object);
                } else {
                    /*
                     * Otherwize the object is dead and it needs to be queued for finalization
                     * if it is for a finalizable object
                     */
                    if (ObjectAssociation_hasFinalizer(assn)) {
/*if[FINALIZATION]*/
                        addToFinalizerQueue(KeepObject(assn));
/*end[FINALIZATION]*/
                    } else {
                        if (getTraceAssociations())  {
                            traceln("Association LOST");
                        }
                    }
                }

                /*
                 * Get the next association
                 */
                assn = next;
            }
        }

        /**
         * Copy all the entries in the side-list of an association table to
         * the new association table.
         */
        void copyAssociationQueue(int queue) {

           /*
            * Get the queue of associations and zero the pointer
            */
            int assn = getAssociationQueue(queue);
            setAssociationQueue(queue, 0);

            while (assn != 0) {

                /*
                 * Get the next association before it is overwritten
                 */
                int next    = ObjectAssociation_getNext(assn);
                int object  = ObjectAssociation_getObject(assn);
                ObjectAssociation_setNext(assn, 0);
                insertAssociation(assn, object);
                assn = next;
            }
        }

        /*
         * markAssociationQueues
         */
/*MAC*/ void markAssociationQueues() {
            int i;
            for (i = 0 ; i < getAssociationHashtableSize() ; i++) {
                markAssociationQueue(i);
            }
        }


        /*
         * checkAssociationQueues
         */
        void checkAssociationQueues() {
//IFC//#ifndef PRODUCTION
            if (getAssociationHashtable() != 0) {
                int queue, count;
                traceAssociationQueue("Association queue after:");
                count = getAssociationHashtableSize();
                for (queue = 0 ; queue < count ; queue++) {
                    int assn = getAssociationQueue(queue);
                    while (assn != 0) {
                        int object, hash;
                        assume(IsAlive(assn));
                        object = ObjectAssociation_getObject(assn);
                        assume(IsAlive(object));
                        hash = getAssociationHashEntry(object);
                        assume(hash == queue);
                        assn = ObjectAssociation_getNext(assn);
                    }
                }
            }
//IFC//#endif
        }


        /**
         * processAssociationQueues()
         */
        void processAssociationQueues() {

            /*
             * Get the hashtable.
             */
            int table = getAssociationHashtable();

            /*
             * Trace
             */
            if (getTraceAssociations())  {
                traceln("About to copy association hash table");
            }

            /*
             * The table is zero when gc is called from the romozer.
             */
            if (table != 0) {

               /*
                * Check that the hashtable is in old space and zero the
                * pointers in the target hashtable.
                */
/*if[CHENEY.COLLECTOR]*/
                assume(inCurrentObjectPartition(table));
/*end[CHENEY.COLLECTOR]*/
                clearNewAssociationTable();

               /*
                * Copy all the object associations
                */
                traceAssociationQueue("Association queue before:");
                markAssociationQueues();

               /*
                * Copy the hashtable into target space and copy the new contents
                */
                setAssociationHashtable(KeepObject(table));
                copyNewAssociationTable();
            }
        }


        /**
         * Update the association table after an image containing a
         * non-empty association table has been relocated. This is necessary
         * as the objects are found in the table by hashing on their
         * address which will (most likely) have changed after relocation.
         */
        void processAssociationQueuesAfterRelocation() {

            /*
             * Get the hashtable.
             */
            int table = getAssociationHashtable();

            if (table != 0) {
                int i;

                clearNewAssociationTable();

                /*
                 * Copy all the object associations
                 */
                for (i = 0 ; i < getAssociationHashtableSize() ; i++) {
                    copyAssociationQueue(i);
                }

               /*
                * Copy the hashtable into target space and copy the new contents
                */
                setAssociationHashtable(table);
                copyNewAssociationTable();
            }
        }


/*if[LISP2.COLLECTOR]*/

        abstract void updateObjectIfNotMarked(int oop);


        /**
         * updateAssociationQueue
         */
/*MAC*/ void updateAssociationQueue(int $queue) {

           /*
            * Get the queue of associations
            */
            int assn = getAssociationQueue($queue);

           /*
            * Interate through the associations and update their pointers if they are not marked.
            * (Marked objects are processed in a separate iteration over the mark bits.)
            */
            while (assn != 0) {
               /*
                * Get the next association before it is overwritten
                */
                int next = ObjectAssociation_getNext(assn);

               /*
                * Update the pointers
                */
                updateObjectIfNotMarked(assn);

               /*
                * Get the next association
                */
                assn = next;
            }
        }

        /*
         * updateAssociationPointers
         */
/*MAC*/ void updateAssociationPointers() {
            int i;
            if (getTraceAssociations())  {
                 traceln("updateAssociationQueues");
            }
            for (i = 0 ; i < getAssociationHashtableSize() ; i++) {
                updateAssociationQueue(i);
            }
            updateObjectIfNotMarked(getAssociationHashtable());
        }

        /**
         * rebuildAssociationQueue
         */
/*MAC*/ void rebuildAssociationQueue(int $queue) {

           /*
            * Get the queue of associations and zero the pointer
            */
            int assn = getAssociationQueue($queue);
            setAssociationQueue($queue, 0);

            while (assn != 0) {
               /*
                * Get the next association before it is overwrtten
                */
                int next    = ObjectAssociation_getNext(assn);
                int object  = GetPossiblyForwarded(ObjectAssociation_getObject(assn));

               /*
                * Clear the pointer to the next association so it will not get marked later
                */
                ObjectAssociation_setNext_safe(assn, 0);

               /*
                * Insert association back into the new hash table
                */
                insertAssociation(assn, object);

               /*
                * Get the next association
                */
                assn = next;
            }
        }

        /*
         * rebuildAssociationQueues
         */
/*MAC*/ void rebuildAssociationQueues() {
            int i;
            if (getTraceAssociations())  {
                 traceln("rebuildAssociationQueues");
            }
            clearNewAssociationTable();
            for (i = 0 ; i < getAssociationHashtableSize() ; i++) {
                rebuildAssociationQueue(i);
            }
            copyNewAssociationTable();
        }

/*end[LISP2.COLLECTOR]*/


/*IFJ*/}
