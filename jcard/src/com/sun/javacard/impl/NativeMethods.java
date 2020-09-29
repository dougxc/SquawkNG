/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * @(#)NativeMethods.java	1.40 02/04/12
 */

/**
 * This static class (no object instances) contains all the native methods
 * of the JCRE except for those methods defined in the Java Card 2.1 API itself.
 * The JCWDE environment uses a variant of this class with non-native methods
 * but with identical signatures and JCWDE Java implementations.
 */

package com.sun.javacard.impl;

import java.rmi.Remote;

public class NativeMethods  {
  //
  // T=0 I/O Methods
  //

  /**
   * This method receives a five byte T=0 protocol command APDU
   * into the APDU buffer.
   * This method is invoked following the ATR before starting
   * the send and receive cycle.
   * Only JCRE can invoke this method.
   * @return 0 on success; 0xC0xx on error.
   */
  public static native short t0RcvCommand();

  /**
   * This method sends the previously logged status bytes and
   * receives a five byte T=0 protocol command APDU into the APDU buffer.
   * Only JCRE can invoke this method.
   * @param command the byte array to receive into. Must be >= 5 bytes long.
   * @return 0 on success; 0xC0xx on error.
   */
  public static native short t0SndStatusRcvCommand();

  /**
   * This method is used to receive as much data portion of a T=0 protocol APDU as
   * will fit into the APDU buffer safely.
   * Only JCRE can invoke this method.
   * @param offset the offset within the APDU buffer array to start.
   * @return byte count < 256 on success; 0xC0xx on error.
   */
  public static native short t0RcvData( short offset );

  /**
   * This method is used to send the data portion of a T=0 protocol APDU. The procedure
   * byte to be used prior to sending the data is passed as a parameter.
   * The length parameter should be consistent with the procedure byte parameter used.
   * Only JCRE can invoke this method.
   * @param data the byte array to send from.
   * @param offset the offset within the array to start.
   * @param length the length of the data bytes to be sent.
   * @param procByteType = 0 (no procByte), 1 (INS), 2 (~INS)
   * @return 0 on success; 0xC0xx on error.
   */
  public static native short t0SndData( byte[] data, short offset, short length, byte procByteType );

  /**
   * This method is used to send the previously logged 61xx status, followed by
   * reception of the subsequent GET RESPONSE APDU from the IFD.
   * Only JCRE can invoke this method.
   * @return GET RESPONSE P3 value; 0xC006 if no GET RESPONSE; 0xC0xx on error.
   */
  public static native short t0SndGetResponse(byte channelId);

  /**
   * This method is used to request a wait time extension from the IFD.
   * Only JCRE can invoke this method.
   * @return 0 on success; 0xC0xx on error.
   */
  public static native short t0Wait();

  /**
   * This method returns a APDU buffer byte array object.
   * Only JCRE can invoke this method.
   * @return APDU buffer byte array on success; null on error.
   */
  public static native byte[] t0InitAPDUBuffer();

  /**
   * This method is called to set the APDU response status to be sent.
   * Only JCRE can invoke this method.
   * @param sw1Sw2 the 61xx status bytes to be sent.
   * @return 0 on success; 0xC0xx on error.
   */
  public static native short t0SetStatus( short sw1Sw2);

  //
  // Transience management methods
  //

  /**
   * Called from Dispatcher to clear transient objects.
   * @param channelId the logical channel to which the operation will apply.
   * @param event the event which caused the array elements to be cleared.
   */
  public static native void clearTransientObjs( byte channelId, byte event );

  /**
   * Called from AppletMgr when an applet creation fails. This method removes
   * references to objects that were created during applet creation from RTR
   * arrays since those objects no longer exist (because of applet creation
   * failure)
   */
  public static native void clearInvalidTransientReferences();

  //
  // Firewall Management methods
  // Format of contextId byte = 4 bits of context | 4 bits of applet id.
  //

  /**
   * This method is called to set the current selected applet's contextId
   * at the logical channel specified as parameter. This contextId is used
   * by the VM to check access to CLEAR_ON_DESELECT transient objects. It is
   * also used to restrict the creation of CLEAR_ON_DESELECT transient objects.
   * CLEAR_ON_DESELECT objects can only be created or accessed when the currently
   * selected applet's context is the currently active context.
   * The multi-selection information is used by the JCRE to enforce selection
   * rules regarding applets from the same package being active on different
   * logical channels.
   * @param channelId the logical channel where the applet is being selected
   * @param contextId the contextId of selected applet
   * @param isMultiSelectable flag that specifies whether this applet belongs
   * to a multiselectable package or not.
   * @return <b>true</b> if channel context set successfully, <b>false</b> otehrwise.
   */
  public static native boolean setChannelContext ( byte channelId, byte contextId,
						   boolean isMultiSelectable );

  /**
   * This method returns the current selected applet's contextId, at the logical
   * channel specified as parameter. This contextId is used by the VM to check
   * access to CLEAR_ON_DESELECT transient objects. It is also used to restrict
   * the creation of CLEAR_ON_DESELECT transient objects.
   * @param channelId the desired logical channel
   * @return the selected applet's contextId
   */
  public static native byte getChannelContext ( byte channelId );

  /**
   * This method returns the currently active contextId. ContextId is used by the
   * VM to enforce the firewall access checking. This method is used by JCSystem to
   * implement the <code>getAID()</code> method. Note that this method is called from the
   * JCRE context and must return the contextId in the last stack frame.
   * @return the currently active contextId
   */
  public static native  byte getCurrentContext();

  /**
   * This method returns the previously active contextId. The contextId that is returned
   * is the contextId value at the time of the last context switch.  This method is
   * used by JCSystem to implement the <code>getPreviousContextAID()</code> and the
   * <code>getAppletShareableInterface()</code> methods.
   * Note that this method is called from the
   * JCRE context and must begin searching for a change in the context nibble starting with
   * the last stack frame.
   * @return the previously active contextId
   */
  public static native  byte getPreviousContext();

  /**
   * This method checks if access is to be allowed to the object being passed to it from 
   * the previous context. This method just returns if the access to the object from the 
   * previous context (this can be if the previous context is either JCRE or object owner's
   * context) is okay, otherwise it throws a security exception.
   * It is assumed that the object being passed to this method is not null and proper 
   * precautions have been taken regarding this before this method is called. This method's
   * behaviour is non-deterministic if the object passed to it is null.
   * @param theObject for which it is to be determined if the access from the previous context
   * should be allowed.
   */
  public static native void checkPreviousContextAccess(Object theObject);
   
  /**
   * This method is called to mark the specified object as being a JCRE entry point object.
   * JCRE entry point objects' methods are accessible from any applet context. In addition,
   * the the JCRE Entry point object is marked as temporary or permanent based on the
   * boolean parameter. User applets cannot store temporary Entry Point Objects in
   * class variables or array components. This method is also used for global arrays.
   * @param theObject the object to be marked as a JCRE Entry point object
   * @param temporary <code>true<code> if the JCRE Entry point object is a
   * temporary JCRE EP object.
   */
  public static native  void setJCREentry(Object theObject, boolean temporary);

  /**
   * This method is called to set the owner context of the object referenced by the
   * parameter to the specified contextId parameter. This method is used by the installer
   * to stamp the owner context on <clinit> initialized arrays created by the Installer
   * on behalf of the applet package.
   * @param clinitArray the object on which to set owner contextId
   * @param contextId the contextId to assign to the the <code>clinitArray<code> object
   */
  public static native void setObjectContext ( Object clinitArray, byte contextId );

  // Logical channels support native methods


  /**
   * This method is used to open and close logical channels in the card.
   * Operations can be open channel, close channel open autoselect channel.
   * @param channelId the channel to be managed, or ignored for open autoselect.
   * @param operation the operation to perform on the target channel.
   * @return 0 or -1 in case of success/failure, or channelId in case of autoselect.
   */
  public static native byte channelManage(byte channelId, byte operation);
  
  /**
   * This method is used to query the status of a logical channel in the card.
   * Channel status can be open, open multiselected, closed, or deactivated.
   * @param channelId the channel whose status is desired.
   * @return logical channel status code.
   */
  public static native byte getChannelStatus(byte channelId);

  /**
   * This method returns the maximum number of ISO 7816-4 compliant
   * logical channels supported in the smart card.
   * @return maximum number of channels supported.
   */
  public static native byte getMaxChannels ();

  /**
   * This method is used to query the status of a particular applet context.
   * Applet contexts can be package active, package active multiselected,
   * applet active, applet active multiselected, or inactive.
   * @param contextId the applet context whose status is desired.
   * @return logical channel status code.
   */
  public static native byte getContextStatus(byte contextId);

  /**
   * This method sets the currently selected logical channel in the card.
   * The currently selected channel is the channel where the current APDU
   * command is targeted to.  All CLEAR_ON_DESELECT transient array accesses
   * are only allowed to the active package in the currently selected channel.
   * @param channelId the logical channel to be specified as currently selected
   */
  public static native void setCurrentlySelectedChannel ( byte channelId );

  /**
   * This method returns the currently selected logical channel in the card.
   * The currently selected channel is the channel where the current APDU
   * command is targeted to.
   * @return the currently selected channel in the card.
   */
  public static native byte getCurrentlySelectedChannel ();

  //
  // Installer Mask Access methods
  //

  /**
   * This method returns the highest package identifier allocated to the packages
   * in the ROM mask. Package ids are used for context assignment. This method is used
   * by the Installer to manage its Package management tables and for
   * package id assignment for installed packages. Package ids are used as contexts.
   * @return the highest package identifier in ROM.
   */
  public static native byte getMaxPackageIdentifier();
  
  /**
   * This method sets the the packageIds of all the applet packages in ROM in 
   * contextsTable. The index of a particular applet package in the context will
   * be used for context assignment. Once it's completed it's work, the method returns
   * the count of entries set in the table.
   * @param contextsTable is the table containing the Ids of applet packages.
   * @return number of entries set in the table.
   */
  static native byte initAppPkgContextTable(byte[] contextsTable);

  /**
   * This method returns the package identifier in ROM, if any associated
   * with specified package AID. Used by the installer to resolve packages
   * imported by a package being installed.
   * @param aid the byte array containing the package AID bytes.
   * @param aidOff the offset within <code>aid</code> where the package AID start.
   * @param aidlen the byte length of the package AID.
   * @param major the major version number of the specified package.
   * @param minor the minor version number of the specified package.
   * @return the package identifier for the specified package or 0 if not found.
   */
  public static native byte getPackageIdentifier(byte[] aid, 
						 short aidOff, 
						 byte aidLen, 
						 byte major, 
						 byte minor);

  /**
   * This method returns the package identifier in ROM, if any associated
   * with specified package AID. Used by the installer to resolve packages
   * imported by a package being installed.
   * @param aid the byte array containing the package AID bytes.
   * @param aidOff the offset within <code>aid</code> where the package AID start.
   * @param aidlen the byte length of the package AID.
   * @return the package identifier for the specified package or 0 if not found.
   */
  public static native byte findPackageIdentifier(byte[] aid, 
						  short aidOff, 
						  byte aidLen);

  /**
   * This method returns the address to the export component associated with
   * the specified pacakge identifier. This method is used by the installer to resolve
   * classes, methods and fields accessed in the ROM package.
   * @param packageId the package identifier in ROM
   * @return the address of associated export component or -1 if not found.
   */
  public static native short getPackageExportComponent(byte packageId );


  /**
   * Method corresponding to the Package Manager's getAppletInfo. It get's the applet class's
   * information corresponding to the class AID provided to it. The information
   * returned is dependent on the parameter <code>requiredInfoType</code>. If the required
   * applet class is found the required information, which can be package id of the package
   * containing this applet class or applet class's installe method address, is returned. If
   * the required applet class is not found <code>-1</code> is returned.
   * @param bArray contains the AID of the applet class
   * @param offset in the bArray where the AID starts
   * @param AID length
   * @param requiredInfoType which can be either <code>PackageMgr.INSTALL_METHOD_ADDRESS</code>
   * or <code>PackageMgr.PACKAGE_ID</code>
   * @return install method address, package Id or -1 if the required class is not found.
   */
  public static native short getAppletInfo(byte[] bArray, short offset, byte length, byte requiredInfoType);


  /**
   * This method sets the AID of the applet corresponding to the parameter appIndex in the array provided
   * to it as a paramater and returns the size of the AID. If the required applet class is not found, 
   * this method returns -1.
   * @param bArray is the array in which the AID is set
   * @param appIndex is the index in the applet table in ROM.
   * @return AID length if the applet class is found, -1 otherwise.
   */
  public static native byte getAppletAID(byte[] bArray, byte appIndex);


  //
  // Installer Memory Management Methods
  //
  
  /**
   * This method starts a transaction once it has been made sure that no 
   * transaction is started  by the installer to call the install method of an applet.
   */
  public static native void beginTransactionNative() 
    throws javacard.framework.TransactionException;
  
  /**
   * This method commits a transaction once it has been made sure that no 
   * transaction is started by the installer to call the install method of an applet.
   */
  public static native void commitTransactionNative()
    throws javacard.framework.TransactionException;
  
  /**
   * This method aborts a transaction once it has been made sure that no 
   * transaction is started by the installer to call the install method of an applet.
   */
  public static native void abortTransactionNative()
    throws javacard.framework.TransactionException;

  /**
   * This method allocates EEPROM heap space for the specified number of bytes and
   * returns the associated address. The address returned is used
   * in the read, write, copy methods by the Installer to download and install a CAP
   * file. The Installer may use the returned address in a later call to
   * the <code>restore()</code> method to restore heap to the state at the time of
   * invoking this method.
   * @param chunkSize the byte length of heap space to allocate
   * @return the address of allocated heap space or 0 on failure.
   */
  public static native short allocate (short chunkSize);

  /**
   * This method frees EEPROM heap space for the specified number of bytes and
   * the specified address. The space is then added to the heap table for later
   * allocations.
   * @param address the address of the memory to be freed.
   * @param length the length in bytes of the memory to be returned.
   */
  public static native void freeHeap(short address, short length);

  /**
   * the next three methods will return the amount of memory left in the three 
   * memory pools E2P_Available returns the amount of available E2P memory
   * rtr_Available() returns the amount of clear on reset transient memory available
   * dtr_Available() returns the amount of clear on deselect transient memory available
   * @retun the amount of memory avaiable in bytes.
   */
  public static native short E2P_Available();
  public static native short dtr_Available();
  public static native short rtr_Available();

  /**
   * This method writes the specified <code>data</code> byte at the specified byte
   * <code>offset</code> relative the the specified <code>address</code>.
   * @param address the address to the heap space to reference
   * @param offset the byte offset relative to the specified address to write.
   * @param data the byte value to deposit at that specified offset.
   */
  public static native void writeByte ( short address, short offset, byte data);

  /**
   * This method writes the bytes comprising the high and low byte respectively
   * of the specified short <code>data</code> parameter value at the specified byte
   * <code>offset</code> relative the the specified <code>address</code>.
   * @param address the address to the heap space to reference
   * @param offset the byte offset relative to the specified address to write.
   * @param data the short value to deposit at that specified offset.
   */
  public static native void writeShort ( short address, short offset, short data);

  /**
   * Copies the specified number of bytes, from the specified source array,
   * beginning at the specified position, to the destination location specified by
   * the address and the address relative byte offset. This method is used by
   * the Installer to deposit CAP file data into the heap area allocated using the
   * <code>allocate()</code> method.
   * @param src source byte array.
   * @param srcOff offset within source byte array to start copy from.
   * @param address the destination address to the heap space to reference.
   * @param offset the byte offset relative to the specified address to start copy into.
   * @param length byte length to be copied.
   * @return <code>offset+length</code>
   */
  public static native short copyBytes( byte[] src, 
					short srcOff,  
					short address,
					short offset, 
					short length);

  /**
   * This method returns the byte at the specified byte
   * <code>offset</code> relative the the specified <code>address</code>.
   * @param address the address to the heap space to reference
   * @param offset the byte offset relative to the specified address to read.
   * @return the byte value at that specified offset.
   */
  public static native byte readByte(short address, short offset );

  /**
   * This method returns the short value at the specified byte
   * <code>offset</code> relative to the specified <code>address</code>.
   * @param address the address to the heap space to reference
   * @param offset the byte offset relative to the specified address to read.
   * @return the short value at that specified offset.
   */
  public static native short readShort(short address, short offset );

  //
  // Installer Exception Table Management Methods
  //

  /* This method is used by the installer to pass the address of a partially built
   * Exception Table structure to be inserted into the VM Exception table
   * linked list.<p>
   * The address is an address to a 5 byte exception table structure as follows :<p>
   * Bytes 0,1 is the link slot; Bytes 2,3 contain an address to
   * the <code>handler_count</code> item of the method component of the installed
   * package.
   * The last byte contains the package ID to which this exception table belongs. 
   * This method inserts this new Exception Table structure at  the
   * head of the exception table linked list. 
   * @param address the address to the new exception table entry in heap space
   * @param package Id of the package to which this exception list belongs
   */
  public static native void addExcTable ( short address, byte pkgId );

  /**
   * This method is used to remove the Exception Table structure from the 
   * VM Exception table linked list.
   * @param package Id of the package whose entry needs to be removed.
   */
  public static native void removeExcTableEntry(byte pkgId);

  //
  // Installer  Create Method
  //

  /**
   *  This method is used by the installer to create a new applet instance. This method
   *  invokes the specified <code>install()</code> method
   * of the Applet class in the specified execution context.
   * @param address the address to the Applet's install() method in heap space
   * @param contextId the contextId to set as current active context for the 
   * <code>install()</code> method.
   * @param bArray the byte array parameter to pass to the <code>install()</code> method.
   * @param bOffset the byte array offset parameter to pass to the <code>install()</code> method.
   * @param bLength the length parameter to pass to the <code>install()</code> method.
   */
  public static native void callInstall( short address,
					 byte contextId, 
					 byte[] bArray, 
					 short bOffset, 
					 byte bLength );
  
  /**
   * This method writes the reference of
   * of the specified Object <code>obj</code> parameter value at the specified byte
   * <code>offset</code> relative the the specified <code>address</code>.
   * @param address the address to the heap space to reference
   * @param offset the byte offset relative to the specified address to write.
   * @param obj the Object whose reference to deposit at that specified offset.
   */
  public static native void writeObjectAddress (short address,
						short offset, 
						Object obj);

  /**
   * This method converts the block handle returned by allocate to a read/write address.
   * @param handle the block handle returned by allocate.
   * @return the short value as a read/write address or 0 on failure.
   */
  public static native short unhand ( short handle);

  /**
   * This method passes debug address information on a package downloaded via 
   * the installer. For internal JavaCard VM debug purposes only.
   * @param aidLength the length of the package AID.
   * @param aid the AID of the package being installed, starting at index 0.
   * @param addresses the memory addresses where the CAP file components were installed.
   * Component addresses are stored in the following order, starting at index 1: header,
   * directory, applet, import, constantpool, class, method, static field, 
   * reference location and export.
   */
  public static native void installDebugInfo (byte aidLength,
					      byte[] aid,
					      short[] addresses );
  
  /**
   * This method is used to tell the VM that the card initialization has been completed
   * successfully.
   */
  public static native void setCardInitialized();
  
  /**
   * This method is used to check if card has been already initialized.
   */
  public static native boolean isCardInitialized();
  
  /**
   * Garbage Collection Method responsible for initializing the state for garbage
   * collection.
   */
  public static native boolean initializeGC();
  
  /**
   * For garbage collection this method marks the root of all applets, the applet table.
   * @param reference to applet table as an object
   * @return 0 if failed a non-zero value if successful
   */
  public static native byte markAppletRoots(Object applets);

  /**
   * This method is called for all the packages to mark the objects being pointed 
   * to by static reference type fields.
   * @param address of the static component
   * @param number of reference fields in the the static field component
   * @return 0 if failed and non-zero value if succesful.
   */
  public static native byte markStaticRoots(short staticCompAddr, short refCount);

  /**
   * This method starts the garbage collection cycle.
   * @return 0 if failed and non-zero value if succesful.
   */
  public static native byte doGC();

  /**
   * This method initializes the underlying garbage collection module for applet 
   * deletion which is basically a special case of applet deletion. This method 
   * sets up proper variables which will be checked during marking phase to 
   * check if an object owned by the applet being deleted is being marked.
   * @param contextsArray is the array of contexts of applets that are to be deleted.
   * @param count is the number of applets being deleted.
   * @return 0 if failed and non-zero value if succesful.
   */
  public static native byte initializeAppletDeletion(byte[] contextsArray, byte count);

  /**
   * The remaining native methods are used to support remote RMI.
   */
    
  /**
   * This method goes through the complete exported objects arrays and removes any 
   * entries from it that do not point to objects existing on the card.
   * @param RMIExpObjArray containing the object IDs of the objects exported.
   */
  public static native void cleanupExportedObjectsArray(short[] RMIExpObjArray);

  public static native short getObjectID(Object obj);

  /**
   * Used to check if the object ID being placed in the exported RMI object array is
   * owned by the caller. If it is not, will throw a security exception.
   * @param objectID the object ID to be checked
   */
  public static native void checkAccess(short objectID);

  /**
   * This method is used to find a ROM package that has the class component covering
   * the address provided to this method as the parameter.
   * Once such package is found, it's name size and name is set in the given buffer starting from
   * the given offset and the total size of data added to the buffer is returned
   * @param address which is to be in the range of the class component
   * @param buffer in which the name length and name of the package are to be set
   * @param offset in the buffer from where the data needs to be set
   * @return the size of data set in the buffer
   */
  public static native byte getPkgNameForClass(short address,
					       byte[] buffer, 
					       byte offset);
  
  /**
   * This method is used to find a ROM package that has the class component covering
   * the address containing the address provided to this method as the parameter
   * once such package is found, it's ID is returned
   * @param address which is to be in the range of the class component
   * @return package Id
   */
  public static native byte getPkgIDForAddress(short address);

  /**
   * Array arguments range checking is the main purpose of this class
   * at this time.  Calls to the native method are made to check of 
   * array arguments and thus generating the right kind of exceptions.
   * 
   * This does not check for zero length arrays as in some cases,
   * zero length is a special case requiring a special exception, 
   * say, ILLEGAL_USE.
   *
   * Note that this method could throw the following exceptions for errors:
   * ArrayIndexOutOfBoundsException, NullPointerException
   * @param bufin Input buffer
   * @param inoffset Input buffer offset
   * @param inlen Lenght of the input buffer under consideration.
   */
  
  public static native void checkArrayArgs(byte[] bufin,
					   short inoffset,
					   short inlen);
  
}
