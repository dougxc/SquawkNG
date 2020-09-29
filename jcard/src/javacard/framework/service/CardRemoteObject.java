/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

 /*
  * @(#)CardRemoteObject.java	1.3 02/04/22
  */

package javacard.framework.service;

import javacard.framework.*;
import java.rmi.Remote;
import com.sun.javacard.impl.NativeMethods;
import com.sun.javacard.impl.GarbageCollector;
/**
 * A convenient base class for remote objects in Java Card. An instance of
 * a subclass of this <CODE>CardRemoteObject</CODE> class will
 * automatically be exported upon construction.
 *
 * @version 1.0
 *
 */
public class CardRemoteObject extends Object implements java.rmi.Remote{

    private static final short MAX_OBJECTS = (short)16;

    private static short[] array;
    /** Creates a new <CODE>CardRemoteObject</CODE> and automatically exports it. When
     * exported, the object is enabled for remote access from outside the card
     * until unexported. Only when the object is enabled for remote access can it be
     * returned as the initial reference during selection or returned by a remote method.
     * In addition, remote methods can be invoked only on objects enabled for remote
     * access.
     */
    public CardRemoteObject() {
        export(this);
    }

    /**
     * Exports the specified remote object. The object is now enabled for
     * remote access from outside the card until unexported. In order to remotely
     * access the remote object from the terminal client, it must either be
     * set as the initial reference or be returned by a remote method.
     * @param obj the remotely accessible object.
     * @throws SecurityException if the specified obj parameter is not owned by the
     * caller context.
     */
    public static void export( Remote obj ) throws SecurityException {
        if(obj==null) return;

        short id = NativeMethods.getObjectID(obj);
        export(id);
    }

    /**
     * Unexports the specified remote object. The object cannot be remotely
     * accessed any more from outside the card, until it is exported again.
     * <p>Note:<ul>
     * <li><em>If this method is called during the session in which the specified
     * remote object parameter is the initial reference object or has been
     * returned by a remote method,
     * the specified remote object will continue to be remotely accessible until the end of
     * the associated selection session(s).</em>
     * </ul>
     *
     * @param obj the remotely accessible object.
     * @throws SecurityException if the specified obj parameter is not owned by the
     * caller context.
     */
    public static void unexport( Remote obj ) throws SecurityException {

        if(obj==null) return;

        short objID = NativeMethods.getObjectID(obj);
        unexport(objID);
    }

    private static void unexport( short objID ) throws SecurityException {
        NativeMethods.checkAccess(objID);

        if(array == null) return;

        for(short i=0; i<MAX_OBJECTS; ++i)
        {
            if(array[i] == objID)
            {
                array[i] = 0;
            }
        }

    }

    private static void export( short objID ) throws SecurityException {
        NativeMethods.checkAccess(objID);

        if(array == null) {
            array = new short[MAX_OBJECTS];
            NativeMethods.setJCREentry(array, false);   //  false = not temporary
            GarbageCollector.setExpObjArray(array);
        }

        for(short i=0; i<MAX_OBJECTS; ++i)
        {
            if(array[i] == 0)
            {
                array[i] = objID;
                return;
            }
        }

        SystemException.throwIt(SystemException.NO_RESOURCE); // no slots
        return; // never reached
    }


    static boolean isExported( Remote obj ) throws SecurityException {

        if(obj==null) return true;

        short objID = NativeMethods.getObjectID(obj);
        return isExported(objID);
    }

    private static boolean isExported( short objID ) throws SecurityException {
        NativeMethods.checkAccess(objID);

        if(array == null) return false;

        for(short i=0; i<MAX_OBJECTS; ++i)
        {
            if(array[i] == objID)
            {
                return true;
            }
        }

        return false;
    }

}
