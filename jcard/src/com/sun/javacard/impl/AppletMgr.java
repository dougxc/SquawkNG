/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)AppletMgr.java  1.13
// Version:1.13
// Date:11/04/01
//
// Archive:  /Products/Europa/api21/com/sun/javacard/impl/AppletMgr.java
// Modified:11/04/01 18:54:23
// Original author:  Mitch Butler
// */

package com.sun.javacard.impl;
import javacard.framework.ISO7816;
import javacard.framework.APDU;
import javacard.framework.ISOException;
import javacard.framework.AID;
import javacard.framework.Applet;
import javacard.framework.SystemException;
import javacard.framework.JCSystem;
import javacard.framework.Util;


/**
 * This class manages the Applet Table; the list of applets
 * installed on the card.
 */
public class AppletMgr
{
    // constants
    //Delete Applets instruction
    public static final byte DELETE_APPLETS = (byte)0xc4;
    public static final byte APP_NULL = (byte) 0xFF;// invalid applet ID
    public static final byte APP_FIRST = (byte)0;         // ID of the first applet
    public static final byte APPS_MAX = (byte)16;         // maximum number of applets
      public static final byte NO_APPLET_BEING_PROCESSED = (byte)(-1);
      public static final byte APPLET_INDEX = (byte)3;
    //maximum number of applets that can be deleted in one command
    static final byte MAX_APPLETS_TO_BE_DELETED = (byte)8;

    // Logical Channels constants
    // Channel status constants
    private static final byte CHANNEL_CLOSED = 0x00;
    private static final byte CHANNEL_DISABLED = 0x01;
    private static final byte CHANNEL_OPEN = 0x02;
    private static final byte CHANNEL_OPEN_MS = 0x03;

    private static final byte CHANNEL_MS_MASK = 0x01;
    private static final byte CHANNEL_OPEN_MASK = 0x02;

    // Static class field, initialization done in addApplet
    static AppTableEntry[] theAppTable;
    static byte[] theAppState; // life cycle state of applet instance. See PrivAccess.APP_STATE_..

    //Applet index of the applet in the package entry or in applet table in ROM,
    //of the applet being created
      public static byte currentAppletIndex = NO_APPLET_BEING_PROCESSED;
      //buffer to hold applet deletion AID data. This buffer is created in installer's
      //CLEAR_ON_DESELECT space.
      private static byte[] AppletMgrBuffer;
      //If a ROM applet tries to register with default AID, this buffer is used to get its
      //AID from ROM.
      private static byte[] currentAppAID;

      //ID of the applet being created
    private static byte appInProcess = NO_APPLET_BEING_PROCESSED;

    //flag to indicate that the there is an installation in progress and calls to begin, commit
    //or abort transaction are to be ignored.
    public static boolean installTransactionFlag = false;



    /**
     * Empty constructor
     */
    AppletMgr() {}

    /**
     * Add an applet entry to the applet table and return the corresponding applet ID.
     * @param pkgContext is the package context of the applet being created.
       * @return the index in the applet table for the new applet which is used as applet ID
     */
     static byte addAppletEntry(byte pkgContext){
        if(theAppTable == null){
            //this is the first time we are getting in here, so initialize whatever needs
            //to be initialized for applet manager.
            theAppTable = new AppTableEntry[APPS_MAX];
            theAppState = new byte[APPS_MAX];
            AppletMgrBuffer = JCSystem.makeTransientByteArray((short)25, JCSystem.CLEAR_ON_DESELECT);
            currentAppAID = new byte[16];
            NativeMethods.setJCREentry( currentAppAID, false );
        }
        for(byte i = APP_FIRST; i < APPS_MAX; i++){
            if(theAppTable[i] == null){
                theAppTable[i] = new AppTableEntry();
                theAppTable[i].theContext = pkgContext;
                appInProcess = i;
                return i;
            }
        }
        return APP_NULL;
     }

    /**
     * Calls the install method on an applet that has just been loaded or was
     * masked in ROM
     * @param bArray to pass to the applet's install method
     * @param bOffset to pass to the applet's install method
     * @param bLength to pass to the applet's install method
     * @param the package ID of the package to which the applet being created belongs
     * @return the instance AID of the installed applet.
     */
    public static AID createApplet(byte[] bArray, short bOffset, byte bLength, byte pkgContext){
        byte appID = 0;
        //offset to applet parameters in bAarray
        short m_paramOffset;
        //applet parameters length
        byte m_paramLength;
        byte currentChannel = 0;
        byte myContextId = 0;
        byte contextId;
        boolean isMyContextMultiSelected = false;

        try {
            //the transaction started here will be commited in the AppletMgr's commit method
            //or if the installation wasn't successful, it'll be aborted by the Dispatcher.
            JCSystem.beginTransaction();
            installTransactionFlag = true;
            //add a new entry to the applet table.
            appID = addAppletEntry(pkgContext);
            if(appID == APP_NULL) {
                //error
                return null;
            }
           // Logical channels multiselectability checks
            currentChannel = NativeMethods.getCurrentlySelectedChannel();
            contextId = PrivAccess.getContextId( appID );
            myContextId = NativeMethods.getChannelContext(currentChannel);
            isMyContextMultiSelected =
                (NativeMethods.getChannelStatus(currentChannel) == CHANNEL_OPEN_MS);

            //check if any other applet from the same package as the applet being created, is active
            //at this time
            if((NativeMethods.getContextStatus(contextId) & PrivAccess.PACKAGE_ACTIVE) ==
                                                            PrivAccess.PACKAGE_ACTIVE){
                //check if this is the installer, if it is not, then we have to throw an error.
                //In case of installer, since the context is the same as JCRE context, it will come
                //come back saying that the package is active.
                if(contextId != 0)
                    ISOException.throwIt(Errors.ACTIVE_APPLET_FROM_SAME_PACKAGE);
            }


            m_paramOffset = (short)(bOffset + (short)bLength);
            m_paramLength = bArray[m_paramOffset++];

            /*For the new create command APDU format */
            if(m_paramLength == (byte)0){
                m_paramLength=(byte)3;
                bArray[m_paramOffset] = bArray[(byte)(m_paramOffset+1)] = bArray[(byte)(m_paramOffset+2)] = (byte)0;
            }

            //how to get the install method address?
            short theAddress = PackageMgr.getAppletInfo(bArray, bOffset,
                bLength, PackageMgr.INSTALL_METHOD_ADDRESS);

            // If applet is uniquely selected from package, it will be treated as
            // non-multiselectable for the time being.  Minimal impact, since always the
            // context is reset to the installer context at the end of the method.
            boolean isMultiSelectable = ((NativeMethods.getContextStatus(contextId) & PrivAccess.PACKAGE_ACTIVE) == (byte)0);

            NativeMethods.clearTransientObjs(currentChannel, JCSystem.CLEAR_ON_DESELECT);

            // By calling setChannelContext() twice, the space is deallocated and reallocated dynamically.
            if (myContextId != PrivAccess.NULL_CONTEXTID) {
                NativeMethods.setChannelContext(currentChannel, PrivAccess.NULL_CONTEXTID, false);
            }
            NativeMethods.setChannelContext(currentChannel, contextId, isMultiSelectable);

            /**
             * Call the install method within a transaction, so that if applet creation fails,
             * everything can be brought back to the old state.
             * According to the JCRE specifications "If the installation is
             * unsuccessful, the JCRE shall perform all cleanup when it regains control.
             * That is, all persistent objects shall be returned to the state they had
             * prior to calling the install method."
             */
            NativeMethods.callInstall(theAddress, contextId, bArray, m_paramOffset, m_paramLength);
        }finally {
            boolean wasCreationSuccessful = true;
            /**
             * For the cleanup in case of applet creation failure, since everything was being transacted
             * the transaction mechanism will take care of it and we do not need to do anything special here.
             * Only thing we do need to tell the transaction mechanism is that the installer instantiated
             * transaction is over. And we do need to restore the JCRE context and everything else which
             * needs to be done in any case.
             */
            installTransactionFlag = false;
            //we need to nullify any references to objects created during applet creation,
            //from Deselect Transient RAM if applet creation failed.
            if(appInProcess != NO_APPLET_BEING_PROCESSED){
                JCSystem.abortTransaction();
                NativeMethods.clearInvalidTransientReferences();
                wasCreationSuccessful = false;
            }

            NativeMethods.clearTransientObjs(currentChannel, JCSystem.CLEAR_ON_DESELECT);
            NativeMethods.setChannelContext(currentChannel, PrivAccess.NULL_CONTEXTID, false);
            if (myContextId != PrivAccess.NULL_CONTEXTID) {
                NativeMethods.setChannelContext(currentChannel, myContextId,
                                                isMyContextMultiSelected);
            }
            if(wasCreationSuccessful){
                return PrivAccess.getPrivAccess().getAID( appID );
            }
//            return null;
        }
        return null;
    }


    /**
     * Lookup an applet in the Applet table by AID in the form of byte array.
     * @param bArray contains the AID
     * @param offset of AID in bArray
     * @param length of AID in bArray
     * @return the index of the applet in the table (or APP_NULL)
     */
    public static byte findApplet(byte[] bArray, short offset, byte length)
    {
        for (byte i = APP_FIRST; i < APPS_MAX; i++) {
            AppTableEntry theEntry = theAppTable[i];
            if ( theEntry != null
              && theEntry.theAID != null
              && theEntry.theAID.equals(bArray, offset, length)) {
                return i;
            }
        }
        return APP_NULL;
    }

    /**
     * Same as findApplet(byte[],short,byte), except takes an AID as input parameter.
     * @param theAID to locate in the Applet Table
     * @return the index of the applet in the table (or APP_NULL)
     */
    static byte findApplet(AID theAID)
    {
        for (byte i = APP_FIRST; i < APPS_MAX; i++) {
            AppTableEntry theEntry = theAppTable[i];
            if ( theEntry != null
              && theEntry.theAID != null
              && theEntry.theAID.equals( theAID )) {
                return i;
            }
        }
        return APP_NULL;
    }

    /**
     * Put the applet's object reference in the Applet Table. Called
     * only from Applet.register. This method must only be called in the JCRE context.
     * @param theApplet object reference
     * @param theAID to assign the applet. ( null => classAID ).
     */
      static void register(Applet theApplet, AID theAID)
      {
        byte appID = PrivAccess.getPreviousAppID();

        if(theAppTable[appID].theAID != null){
            SystemException.throwIt( SystemException.ILLEGAL_AID );
        }

        if (theAID==null) {
            byte pkgId = 0;
            byte pkgContext = theAppTable[appID].theContext;
            //special case for installer applet
            if(pkgContext != 0)
                pkgId = PackageMgr.getPkgIdForContext(pkgContext);

            if(pkgId >= PackageMgr.f_firstEEPkgID){
                if(pkgId == PackageMgr.g_packageInProcess){
                    theAID = PackageMgr.g_newPackage.applets[currentAppletIndex].theClassAID;;
                }else{
                    theAID = PackageMgr.f_pkgTable[pkgId].applets[currentAppletIndex].theClassAID;
                }
            }else{
                byte length = NativeMethods.getAppletAID(currentAppAID, currentAppletIndex);
                theAID = new AID(currentAppAID, (short)0, length);
                NativeMethods.setJCREentry( theAID, false );
            }
        }

        // check if the specified AID is in use as an instance AID
        //and the applet has not already registered itself
        if (findApplet(theAID)!=APP_NULL)
            SystemException.throwIt( SystemException.ILLEGAL_AID );

        theAppTable[appID].theApplet = theApplet;
        theAppTable[appID].theAID = theAID;
        commit(appID);
    }


    /**
     * Sets the current applet ID to the index of applet in the package entry or index
     * of applet in applet table in ROM
     * @param bArray is the array containing the class AID
       * @param offset in the bArray where the AID starts
       * @param length of AID
       * @param pkgId is the id of the package containing the applet being processed
     */
     public static void setCurrentAppletIndex(byte[] bArray, short offset, byte length, byte pkgId){
        PackageEntry pe;
        currentAppletIndex = NO_APPLET_BEING_PROCESSED;
        if((PackageMgr.g_packageInProcess == pkgId) && (PackageMgr.g_newPackage != null)){
            pe = PackageMgr.g_newPackage;
        }else if(pkgId >= PackageMgr.f_firstEEPkgID){
            pe = PackageMgr.f_pkgTable[pkgId];
        }else{
            currentAppletIndex = (byte)(NativeMethods.getAppletInfo(bArray, offset, length, APPLET_INDEX) & 0xFF);
            return;
        }

        for(byte i = 0; i < pe.appletCount; i++){
            AppletInfo ai = pe.applets[i];
            if(ai.theClassAID.equals(bArray, offset, length)) {
                currentAppletIndex  = i;
                return;
            }
        }
        }

    /**
     * After successful creation of the applet, we have to reset the intermediate
     * variables.
     */
    public static void commit(byte appID) {
        // mark the entry as successfully registered
        theAppState[appID] = PrivAccess.APP_STATE_REGISTERED;
        // commit applet table entries
        appInProcess = NO_APPLET_BEING_PROCESSED;

        //commit the package. This is for the support of combination
        //install (package installation + applet creation). In this case
        //we say since an applet has been commited, the package also needs
        //to be commited. This method will not do anything if there is
        //no package installation in progress.
        PackageMgr.commit();

        installTransactionFlag = false;
        JCSystem.commitTransaction();
    }

    /**
     * get list of applets for a package
     * @param appIds is array of applet ids that needs to be set if there are
     * any applets belonging to this package.
     * @param offset from where the ids are supposed to set in the appIds array
     * @param package context
     * @return number of applets
     */
     public static byte getAppletsForPackage(byte[] appIds, byte offset, byte pkgId){
        byte pkgContext = PackageMgr.getPkgContext(pkgId);
        //number of applets belonging to the selected context
        byte count = 0;
        for(byte i = 0; i < APPS_MAX; i++){
            if(theAppTable[i] != null){
                //if the applet belongs to this context
                if(theAppTable[i].theContext == pkgContext){
                    //if the calling method required the applet ids
                    if(appIds != null){
                        appIds[(byte)offset++] = (byte)((pkgContext << 4)|i);
                    }
                    //increment the count
                    count++;
                }
            }
        }
        return count;
     }

    /**
     * This method processes the APDU for AIDs of the applets that are to be deleted.
     * @param APDU which is used as a temporary buffer for applet deletion
     */
    public static void handleAppletDeletion(APDU apdu){
        byte aidCount = 0;
        byte aidLengthRead = 0;
        byte aidlength = -1;
        short aidAndIdBufferOffset = 0;
        byte[] buffer = apdu.getBuffer();
        byte count = buffer[ISO7816.OFFSET_P1];

        try{
            /*
            If the requested number of applets to be deleted is less than 0 or greater than 8
            it's an error
            */
            if(count < 0 || count > MAX_APPLETS_TO_BE_DELETED){
                //error: Invalid # of applets requested to be deleted
                ISOException.throwIt(javacard.framework.ISO7816.SW_INCORRECT_P1P2);
            }
            //read data into the APDU buffer
            short bytesRead = apdu.setIncomingAndReceive();
            //initialize the whole array to 0s so that there isn't any invalid entry in there
            Util.arrayFillNonAtomic(AppletMgrBuffer, (short)0, (short)AppletMgrBuffer.length, (byte)0);

            //copy all the data received into the AID data and then do processing on that
            while(bytesRead > 0){
                //process the data received
                //get the contexts for all applets, put them in buffer and call garbage collector's
                //deleteApplets method
                for(byte i = ISO7816.OFFSET_CDATA; i <= (byte)(bytesRead + ISO7816.OFFSET_CDATA); i++){
                    if(aidlength == -1){
                        aidlength = buffer[i];
                    }
                    else if(aidLengthRead == aidlength){
                        //complete AID has been read. Now to do the processing part
                        byte appContext = (byte)findApplet(AppletMgrBuffer, (short)aidCount, aidlength);
                        if(appContext == APP_NULL){
                            //error: Applet not found
                            ISOException.throwIt(Errors.APPLET_NOT_FOUND);
                        }
                        //get the complete applet context including the package
                        appContext = (byte)((byte)(theAppTable[appContext].theContext<<4) | appContext);
                        AppletMgrBuffer[aidCount] = appContext;
                        aidLengthRead = 0;
                        aidAndIdBufferOffset = ++aidCount;
                        aidlength = -1;
                        i--;
                    }else{
                        AppletMgrBuffer[aidAndIdBufferOffset++] = buffer[i];
                        aidLengthRead++;
                    }
                }
                bytesRead = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
            }
            //start a transaction
            JCSystem.beginTransaction();

            GarbageCollector.deleteApplets(AppletMgrBuffer, count);

            //commit the transaction if no exception occured
            JCSystem.commitTransaction();
        } catch (ISOException e) {
            //error. Either memory constraints or dependencies
            if (JCSystem.getTransactionDepth() != 0){
                JCSystem.abortTransaction();
            }
            ISOException.throwIt(e.getReason());
        } catch (Exception e) {
            if (JCSystem.getTransactionDepth() != 0){
                JCSystem.abortTransaction();
            }
            ISOException.throwIt(Errors.EXCEPTION);
        }
    }

     /**
      * Removes the applet from the applet table
      * @param appletIndex is the index in the applet table
      */
     public static void removeApplet(byte appletIndex){
        theAppState[appletIndex] = PrivAccess.APP_STATE_NONE;
        theAppTable[appletIndex] = null;
     }
}
