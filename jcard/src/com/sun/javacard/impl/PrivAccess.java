/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)PrivAccess.java	1.24
// Version:1.24
// Date:02/01/02
//
// Archive:  /Products/Europa/api21/com/sun/javacard/impl/PrivAccess.java
// Modified:02/01/02 16:38:57
// Original author:  Mitch Butler
// */

package com.sun.javacard.impl;

import javacard.framework.AID;
import javacard.framework.Applet;
import javacard.framework.APDU;
import javacard.framework.JCSystem;
import javacard.framework.Shareable;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.MultiSelectable;

/**
 * This static class (no object instances) contains native and non-native methods
 * for the Runtime Environment to control and select applets.
 */
public class PrivAccess
{
    public static final byte APP_FIRST = AppletMgr.APP_FIRST;
    public static final byte APPS_MAX = AppletMgr.APPS_MAX;
    public static final byte APP_NULL = AppletMgr.APP_NULL;

    public static final byte JCRE_CONTEXTID = (byte) 0x00;
    public static final byte NULL_CONTEXTID = (byte) 0x0F;
    public static final byte APPID_BITMASK = (byte)0xF;

    private static byte defaultAppID = APP_FIRST;

    private static final byte BASIC_CHANNEL = (byte)0x00;
    
    // Context specific constants
    static final byte APPLET_MULTISELECTED = (byte)(0x08);
    static final byte APPLET_ACTIVE = (byte)(0x04);
    static final byte PACKAGE_MULTISELECTED = (byte)0x02;
    static final byte PACKAGE_ACTIVE = (byte)0x01;

    
    private static PackedBoolean thePackedBoolean;
    private static PrivAccess thePrivAccess;
    private static final byte NUMBER_SYSTEM_BOOLS = 10;

    private static byte selectingAppletFlag;
    private static byte processMethodFlag;

    /**
     * Singleton constructor. Dispatcher creates
     */
    public PrivAccess()
    {
        thePrivAccess = this;
    }

    /**
     */
    public static PackedBoolean getPackedBoolean()
    {
        if ( thePackedBoolean==null )
            thePackedBoolean = new PackedBoolean( (byte)(((NUMBER_SYSTEM_BOOLS-1)>>3)+1) );
        return thePackedBoolean;
    }

    /**
     */
    public static PrivAccess getPrivAccess()
    {
        return thePrivAccess;
    }

    // selection related methods

    public static void setSelectingAppletFlag()
    { thePackedBoolean.set( selectingAppletFlag ); }

    public static void resetSelectingAppletFlag()
    { thePackedBoolean.reset( selectingAppletFlag ); }
    
    // process() method related methods

    public static void setProcessMethodFlag()
    { thePackedBoolean.set( processMethodFlag ); }

    public static void resetProcessMethodFlag()
    { thePackedBoolean.reset( processMethodFlag ); }

    /**
     * This method returns the currently selected appId from the currently selected contextId.
     * @param channelId Logical channel from which we wish to get information from
     * @return the currently selected appId
     */
    public static byte getSelectedAppID(byte channelId)
    {
        byte contextId = NativeMethods.getChannelContext(channelId);
        if (contextId==NULL_CONTEXTID) return APP_NULL;
        return (byte) ( contextId & APPID_BITMASK );
    }

    /**
     * This method returns the applet instance of the currently
     * selected applet.
     * @param channelId Logical channel from which we wish to get information from
     * @return the selected applet instance
     */
    public static Applet getSelectedApplet(byte channelId)
     { return AppletMgr.theAppTable[getSelectedAppID(channelId)].theApplet; }

	/**
     * This method is executed each time the card is reset.
     * @param channelId Logical channel in which we wish to perfrom the selection
     */
	public static void selectDefaultApplet(byte cmdChannel, byte channelId)
	    throws ISOException {
	    deselectOnly(channelId);
	    selectOnly(channelId, defaultAppID);
    }

	/**
	 * Select the specified applet.
	 * Select APP_NULL on selection failure.
     * @param channelId Logical channel in which we wish to perfrom the selection
     * @param appID applet ID to select
     */
	public static void selectOnly(byte channelId, byte theAppID) throws ISOException {
        boolean success = false;
        byte appContextId = getContextId( theAppID );
        boolean multiSelectFailed = false;
        byte contextStatus = 0;

        setSelectingAppletFlag();
	    try {
	        Applet appToSelect = AppletMgr.theAppTable[theAppID].theApplet;
            contextStatus = NativeMethods.getContextStatus(appContextId);

            // Select applet at the specified channel.
            // VM will allocate COD space underneath.
            if (NativeMethods.setChannelContext(channelId, appContextId,
              (appToSelect instanceof MultiSelectable))) { 

                // Perform an execution context switch on the new channel
                NativeMethods.setCurrentlySelectedChannel(channelId);

                   // Check for multiselection and call respective select method.
                if ((byte)(contextStatus & PACKAGE_ACTIVE) != (byte)0) {
                     MultiSelectable msApp =
                         (MultiSelectable)(getSelectedApplet(channelId));
                     success = msApp.select(((byte)(contextStatus & APPLET_ACTIVE) != (byte)0));
                } else {
                     success = getSelectedApplet(channelId).select();
                }
            } else {
    	        ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
            }
        } catch (ISOException isoEx) {
            // Rethrow exception so that it can be caught in main 
            // Dispatcher loop
            if (isoEx.getReason() == ISO7816.SW_CONDITIONS_NOT_SATISFIED) {
                ISOException.throwIt(isoEx.getReason());
            }                
            // Rethrow exception so that it can be caught in main 
            // Dispatcher loop
        } catch (Throwable e){
            // success==false
        }

        // Restore execution context to managing channel's context
        NativeMethods.setCurrentlySelectedChannel(APDU.getCLAChannel());
        // abort transaction if applet forgot to commit it
        if (JCSystem.getTransactionDepth() != 0) {
            success = false;
            JCSystem.abortTransaction();
        }
        if (success) return;

        // no selected applet if select returns false or throws exception.
        NativeMethods.setChannelContext(channelId, NULL_CONTEXTID, false);
        ISOException.throwIt(ISO7816.SW_APPLET_SELECT_FAILED);
	}

	/**
	 * Deselect the currently selected applet.
     * @param channelId Logical channel in which we wish to perfrom the deselection
     */
	public static void deselectOnly(byte channelId)
	{
	    // Return if deselect on null context ID attempted
	    if (NativeMethods.getChannelContext(channelId) == NULL_CONTEXTID) {
	        return;
	    }

        Applet theApp = getSelectedApplet(channelId);
        if (theApp==null) return;

        // Check if applet has to be multiselected-deselected
        byte appContextId = NativeMethods.getChannelContext(channelId);
        // Returns true if context selected more than once
	    byte contextStatus = NativeMethods.getContextStatus(appContextId);

        try {
	        // Perform an execution context switch on the deselect channel
            NativeMethods.setCurrentlySelectedChannel(channelId);

            // deselect the currently selected applet
	        // Check for multiselection and call respective deselect method.
            if ((byte)(contextStatus & PACKAGE_MULTISELECTED) != (byte)0) {
                MultiSelectable msApp =
                    (MultiSelectable)(getSelectedApplet(channelId));
                msApp.deselect((byte)(contextStatus & APPLET_MULTISELECTED) != (byte)0);
        	} else {
        	    theApp.deselect();
        	}
        } catch (Throwable e){
            // ignore all exceptions from the deselect
        }

        // Restore execution context to managing channel's context
        NativeMethods.setCurrentlySelectedChannel(APDU.getCLAChannel());

        //clear CLEAR_ON_DESELECT transient data
        if ((byte)(contextStatus & PACKAGE_MULTISELECTED) == (byte)0) {
            NativeMethods.clearTransientObjs(channelId,
                JCSystem.CLEAR_ON_DESELECT);
        }
        // no selected applet if select returns false or throws exception.
        NativeMethods.setChannelContext(channelId, NULL_CONTEXTID, false);
        // abort transaction if applet forgot to commit it
        if (JCSystem.getTransactionDepth() != 0) {
            JCSystem.abortTransaction();
        }
	}

	/**
	 * Deselect the currently selected applet and select the
	 * specified applet as the new selected applet.
     * @param channelId Logical channel in which we wish to perfrom the deselection
     * @param appID applet ID to select
     */
	public static void selectApplet(byte channelID, byte theAppID) throws ISOException {
        deselectOnly(channelID);
        selectOnly(channelID, theAppID);
	}

    // applet state methods

    public static final byte APP_STATE_NONE = (byte)0;
    public static final byte APP_STATE_REGISTERED = (byte)1;
    public static final byte APP_STATE_SELECTABLE = (byte)2;

    /**
     * Get the applet life cycle state of the applet.
     * @param theAID the AID of the applet.
     * @return state of the applet. See APP_STATE..
     */
    public static byte getAppState( AID theAID ) {
        byte appID = AppletMgr.findApplet( theAID );
        if ( appID != AppletMgr.APP_NULL ) return AppletMgr.theAppState[appID];
        return APP_STATE_NONE;
    }

    /**
     * Set the applet life cycle state of the applet.
     * @param theAID the AID of the applet
     * @param theState the new state of the applet.
     */
    public static void setAppState( AID theAID, byte theState )
    {
        byte appID = AppletMgr.findApplet( theAID );
        AppletMgr.theAppState[appID] = theState;
    }

    // context management utility methods

    /**
     * This method returns the currently active appId from the currently active contextId.
     * @return the currently active appId
     */
    public static byte getCurrentAppID()
    {
        byte contextId = NativeMethods.getCurrentContext();

        return (byte) ( contextId & APPID_BITMASK );
    }

    /**
     * This method returns the previously active appId from the previous active contextId.
     * @return the previously active appId
     */
    public static byte getPreviousAppID()
    {
        byte contextId = NativeMethods.getPreviousContext();

        return (byte) ( contextId & APPID_BITMASK );
    }

    /**
     * Get the ContextId of the applet using the App ID
     * @return ContextId of the applet.
     */
    public static byte getContextId( byte appID ) {
        return (byte)(AppletMgr.theAppTable[appID].theContext<<4 | appID);
    }

    // install related methods
	// card initialization

	/**
	 * Called by the Dispatcher during card initialization. This method creates the
	 * installer applet. Assumption is that installer applet is the first applet in the
	 * applets array in the mask.
	 */
	public static void initialize( APDU theAPDU )
	{
    PackageMgr.init();
    // initialize System flag
    selectingAppletFlag = getPackedBoolean().allocate();
    processMethodFlag = getPackedBoolean().allocate();

    // borrow the APDU buffer for temporary scratch pad array
    byte [] bArray = theAPDU.getBuffer();
    short bOffset = (short)0;

    byte aidLength = NativeMethods.getAppletAID(bArray, (byte)0);
    if (aidLength == (short)-1) return;
    AppletMgr.currentAppletIndex = 0;
    AID theAID = AppletMgr.createApplet(bArray, bOffset, aidLength, (byte)0);
    if (theAID!=null) setAppState( theAID, PrivAccess.APP_STATE_SELECTABLE );
  }

    // instance methods. The following are JCRE entry points.
    // The JCRE entry point methods are called from static API methods.

    /**
     * @return true if the system if an applet is begin selected.
     */
    public boolean selectingApplet()
    { return thePackedBoolean.get( selectingAppletFlag ); }
    
    /**
     * @return true if executing in the Applet.process() method.
     */
    public boolean inProcessMethod()
    { return thePackedBoolean.get( processMethodFlag ); }

    /**
     * Register the applet object with the Class AID as instance AID.
     * @param theApplet the applet instance
     */
    public final void register(Applet theApplet){
        AppletMgr.register(theApplet, null );
    }

    /**
     * Register the applet object with with the specified AID as the instance AID.
     * @param theApplet the applet instance.
     * @param bArray the byte array containing the AID bytes.
     * @param bOffset offset in bArray where AID bytes begin
     * @param bLength length of AID bytes.
     * @return applet instance of the AID object.
     */
    public final void register(Applet theApplet, byte[] bArray, short bOffset, byte bLength ){

        // create new AID to represent applet instance.
        // Mark as permanent JCRE Entry Point object
        AID theAID = new AID(bArray, bOffset, bLength);
        NativeMethods.setJCREentry( theAID, false );

        AppletMgr.register(theApplet, theAID );
    }

    /**
     * Get the servers Shareable interface on behalf of the server applet.
     * @param serverAID AID of the server applet.
     * @param clientAID AID of the client applet
     * @return Shareable instance from the client or null.
     */
    public Shareable getSharedObject( AID serverAID, byte param ){
        AID clientAID = getAID( (byte)(NativeMethods.getPreviousContext() & APPID_BITMASK));
        if (clientAID==null) return null; // caller is not registered correctly
        byte appID = AppletMgr.findApplet( serverAID );
        if (appID!=APP_NULL){
            try {
            return ( AppletMgr.theAppTable[appID].theApplet.getShareableInterfaceObject(clientAID,param)) ;
            } catch ( Exception ie ){}
        }
        return null;
     }

    /**
     * Get the applet AID using the applet instance AID byte array.
     * @param aidArray the array containing the AID bytes of the applet class.
     * @param aidOff the offset within aidArray to start
     * @param aidLength the length of the AID array.
     * @return AID of the applet.
     */
    public AID getAID( byte[] aidArray, short aidOff, byte aidLength) {
        byte appID = AppletMgr.findApplet( aidArray, aidOff, aidLength );
        if (appID != AppletMgr.APP_NULL )
            return AppletMgr.theAppTable[appID].theAID;

        return null;
    }

    /**
     * Get the AID of the applet using the App ID
     * @return AID of the applet.
     */
    public AID getAID( byte appID ) {
        return AppletMgr.theAppTable[appID].theAID;
    }

    /**
     * Gets the package Id for the package that covers a range
     * of addresses that include the address passed to this
     * method as the parameter.
     * @param address
     * @return package Id
     */
    public byte getPkgIDForAddress(short addr){
         return (PackageMgr.getPkgIDForAddress(addr));
    }

    /**
     * Gets the package name for the package that covers a range
     * of addresses that include the address passed to this
     * method as the parameter. If such a package is found, the package
     * manager sets it's length and name bytes in the buffer and returns
     * the total amount of data set.
     * @param classAddress
     * @param buffer
     * @param offset is the offset from where the data needs to be set in the
     * buffer
     * @return Total amount of data set in the buffer
     */
    public byte getPkgNameForClass(short classAddress, byte[] buffer, byte offset){
        return (PackageMgr.getPkgNameForClass(classAddress, buffer, offset));
    }
    
    /**
     * Sets the garbage collection requested flag to the value passed as the parameter
     * @param flag which can be either true or false.
     */
    public void setGCRequestedFlag(boolean flag){
        if(flag){
            thePackedBoolean.set(GarbageCollector.GCRequested);
        }else{
            thePackedBoolean.reset(GarbageCollector.GCRequested);
        }
    }

    /**
     * Gets the garbage collection requested flag to the value passed as the parameter
     * @return either true or false depending on the flag value in Packed Boolean
     */
    public boolean isGarbageCollectionRequested(){
        return thePackedBoolean.get(GarbageCollector.GCRequested);
    }
}
