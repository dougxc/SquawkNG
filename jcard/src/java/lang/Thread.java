//if[SQUAWK]
/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

public class Thread {
    private   Object context;           /* Execution context *** MUST BE FIRST ***                           */
    static Monitor dummy;

}

class Monitor {
    Thread  owner;                  /* Owning thread of the monitor                            */
    Thread  monitorQueue;           /* Queue of threads waiting to claim the monitor           */
    Thread  condvarQueue;           /* Queue of threads waiting to claim the object            */
    short   depth;                  /* Nesting depth                                           */
    boolean isInUse;                /* Flag to say the monitor is in use                       */
}
