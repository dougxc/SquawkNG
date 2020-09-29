/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * @(#)RMINativeMethods.java	1.7 02/04/18
 */

package javacard.framework.service;

import java.rmi.Remote;

class RMINativeMethods {

   /**
    * This method is used to copy a string in the format { u1 length, u1[]} into a given byte array.
    * @param pointer to the string to be copied
    * @param byte array to copy the string into.
    * @param offset into the byte array to copy to.
    * @return the number of bytes copied including the length field.
    */
   public static native short copyStringIntoBuffer(short ptr_to_string, byte[] buffer, short offset);

   public static native short getClassNameAddress(Remote rem_object);

   /**
    * Used to get the address of the remote method information found in the class_info structure
    * @param objID the object ID of the object from which the remote method information is needed.
    * @param methodID the unique hash for the remote method ultimately to be invoked.
    * @return the address of the remote method information.
    */
   public static native short getRemoteMethodInfo(short objID, short methodID);

    /**
    * Returns the return type of a remote method from a given object. Used by RMI to
    * determine which remote method invokation to use. Also does some validation that
    * this is a real and remote object.
    * @param address of the remote method information of the given object ID
    * @param the object ID for the object which has the remote method to be invoked
    * @return the return type of the remote method
    */
   public static native short getReturnType(short remote_method_info);

   /**
    * a cleanup method used to delete any temporary arrays used to pass parameters to an invoked
    * remote method. No parameters or return.
    */

   public static native void deleteAllTempArrays();

   /**
    * used to check if an exception is a Java Card api exception.
    * @param ex the exception to be checked
    * @return true if exception is Java Card api exception, otherwise false.
    */

   public static native boolean isAPIException(Throwable ex);

   /**
    * Copy anti-collision string into apdu buffer.
    * @param RemoteObj The remote object to get the anti-collision string for.
    * @param buffer The APDU buffer.
    * @param offset The starting offset in the APDU buffer to which the collision string is to be copied.
    */
   public static native short getAnticollisionString(Remote RemoteObj, byte[] buffer, byte offset);

   /**
    * This method is used to obtain the number of remote interfaces of the given remote object.
    * @param RemoteObj The remote object to get the number of remote interfaces for.
    * @return noOfRemInterfaces the returned number of remote interfaces.
    */
   public static native byte getRemoteInterfaceNumber(Remote RemoteObj);

   /**
    * Returns the remote interface address from the class structure for the given interface index.
    * @param remoteObject The remote object that is to have it's class structure quiried.
    * @param interfaceIndex The interface index used to find the remote interface address.
    * @return the interface address for the given interface index and remote object.
    */
   public static native short getRemoteInterfaceAddress(Remote remoteObj, byte interfaceIndex);

   /**
    * copies interface name into a buffer in the format {u1 length, u1[] bytes}.
    * @param ptr_to_interface pointer to the interface name to be copied.
    * @param buffer The buffer into which the interface name is to be copied.
    * @param offset The offset in the buffer for the point at which the interface name is to be copied.
    */

   public static native short copyInterfaceNameIntoBuffer(short ptr_to_interface, byte[] buffer, short offset);

   /**
    * all remaining native methods are used to invoke remote methods. The only difference in them
    * is the return type. Most of the code here is common, but separate methods are needed so that
    * the return type is handled correctly by the VM.
    * @param the address of the remote method information
    * @param the apdu buffer with the parameter data to be used when invoking the remote method.
    * @param offset in the apdu buffer where the parameter data starts.
    */

   public static native void rmi_invoker_void(short remote_method_info, short objID, byte[] buffer, byte offset);

   public static native boolean rmi_invoker_boolean(short remote_method_info, short objID, byte[] buffer, byte offset);

   public static native byte rmi_invoker_byte(short remote_method_info, short objID, byte[] buffer, byte offset);

   public static native short rmi_invoker_short(short remote_method_info, short objID, byte[] buffer, byte offset);

   public static native int rmi_invoker_int(short remote_method_info, short objID, byte[] buffer, byte offset);

   public static native Object rmi_invoker_reference(short remote_method_info, short objID, byte[] buffer, byte offset);

   public static native Object rmi_invoker_array(short remote_method_info, short objID, byte[] buffer, byte offset);

   public static native short getRMIErrorCode();

   public static native short writeArray(Object data, byte[] buffer, short offset);

}

