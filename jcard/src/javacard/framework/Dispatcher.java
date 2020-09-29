/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)Dispatcher.java 1.20
// Version:1.20
// Date:11/14/01
//
// Archive:  /Products/Europa/api21/javacard/framework/Dispatcher.java
// Modified:11/14/01 15:27:22
// Original author:  Mitch Butler
// */
package javacard.framework;

import com.sun.javacard.impl.PrivAccess;
import com.sun.javacard.impl.AppletMgr;
import com.sun.javacard.impl.PackageMgr;
import com.sun.javacard.impl.NativeMethods;
import javacard.security.CryptoException;
import javacard.framework.service.ServiceException;
import com.sun.javacard.impl.GarbageCollector;
import com.sun.javacard.impl.CryptoInit;

/**
 * This class is the Dispatcher, which contains the main entry point and
 * the main loop of the card. It dispatches APDUs to applets.
 */
class Dispatcher
{
    // Constants
    private static final byte INS_SELECT   = (byte)0xA4;
    private static final byte INS_MANAGECHANNEL = (byte) 0x70;
    private static final byte P1_SELECT_DFBYNAME = (byte) 0x04;

    private static final byte P2_SELECT_OPTIONS = (byte) 0xE3;
    private static final byte P2_SELECT_OPTIONS_ONLY = 0x00;

    private static final byte P1_OPEN_CHANNEL = (byte)0x00;
    private static final byte P1_CLOSE_CHANNEL = (byte)0x80;
    private static final byte P2_AUTOSELECT_CHANNEL = (byte)0x00;
    private static final byte ERR_NO_CHANNEL_AVAILABLE = (byte)0xFF;
    private static final byte BASIC_CHANNEL = (byte)0x00;

    //Exceptions for system
    private static ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException;
    private static NegativeArraySizeException negativeArraySizeException;
    private static NullPointerException nullPointerException;
    private static ClassCastException classCastException;
    private static ArithmeticException arithmeticException;
    static SecurityException securityException; // used by APDU to flag security exception
    private static ArrayStoreException arrayStoreException;
    private static SystemException systemException;
    private static TransactionException transactionException;
    private static ServiceException serviceException;

    // Logical channel operations
    private static final byte OP_CHANNEL_CLOSE = (byte)0x00;
    private static final byte OP_CHANNEL_OPEN = (byte)0x01;
    private static final byte OP_CHANNEL_OPEN_AUTOSELECT = (byte)0x02;

    // Channel status constants
    private static final byte CHANNEL_CLOSED = (byte)0x00;
    private static final byte CHANNEL_DISABLED = (byte)0x01;
    private static final byte CHANNEL_OPEN = (byte)0x02;
    private static final byte CHANNEL_OPEN_MS = (byte)0x03;

    // Useful constants
    private static final byte CHANNEL_MS_MASK = (byte)0x01;
    private static final byte CHANNEL_OPEN_MASK = (byte)0x02;

    // Static class fields, with initialization done in init()
    private static APDU theAPDU;
    private static byte[] theAPDUBuffer;
    private static PrivAccess thePrivAccess;


    // other static fields
    static Dispatcher theDispatcher;

    /**
     * Main entry point invoked by VM when card is reset.
     */
    public static void main()
    {
        if (!NativeMethods.isCardInitialized()) cardInit();      // card initialization (first time only)
        cardReset();                    // session initialization (each card reset)
        short sw = 0;

        // main loop
        while (true) {
            PrivAccess.resetSelectingAppletFlag();
            PrivAccess.resetProcessMethodFlag();
            theAPDU.complete(sw); // respond to previous APDU and get next
            try {
                // Process channel information
                if (processAndForward()) {  // Dispatcher handles the SELECT APDU
                    // dispatch to the currently selected applet
                    byte commandChannel = NativeMethods.getCurrentlySelectedChannel();
                    if (PrivAccess.getSelectedAppID(commandChannel)==PrivAccess.APP_NULL) {
                        // if no applet selected
                        ISOException.throwIt(ISO7816.SW_APPLET_SELECT_FAILED);
                    }
                    PrivAccess.setProcessMethodFlag();
                    PrivAccess.getSelectedApplet(commandChannel).process(theAPDU);
                    // abort transaction if applet forgot to commit it
                    if (JCSystem.getTransactionDepth() != 0) {
                        TransactionException.throwIt(TransactionException.IN_PROGRESS);
                    }
                }
                sw = ISO7816.SW_NO_ERROR;
            } catch (ISOException ex) {
                // get SW from ISOException reason
                sw = ex.getReason();
            } catch (Throwable e) {
                // any other exception is unknown reason
                sw = ISO7816.SW_UNKNOWN;
            }

            // abort transaction if still in progress
            if (JCSystem.getTransactionDepth() != 0) {
                JCSystem.abortTransaction();
            }

            //did the applet request garbage collection? If it did, call the garbage collector
            if(thePrivAccess.isGarbageCollectionRequested()){
                GarbageCollector.startGC();
            }
        }
    }


    private static void setAPDUChannel()
        throws SystemException {

        // Analyze APDU CLA byte
        byte theAPDUChannel = APDU.getCLAChannel();

        // If channel > max channel, throw exception
        if (theAPDUChannel >= NativeMethods.getMaxChannels()) {
            ISOException.throwIt(ISO7816.SW_LOGICAL_CHANNEL_NOT_SUPPORTED);
        }

        // Set the APDU channel according  to channel encoding
        NativeMethods.setCurrentlySelectedChannel(theAPDUChannel);

    }

    /**
     * Process APDUs handled by the Dispatcher.
     * Also determines if APDU shall be forwarded to the applet or not.
     * @return true if APDU is to be forwarded to applet, false if not.
     * @exception ISOException if an ISO 7816 error code is generated
     * during command processing.
     */
    private static boolean processAndForward()
        throws ISOException {
        // Filter channel information to detect ISO APDUs
        if ((byte)(theAPDUBuffer[0] & (byte)(APDU.APDU_CMD_MASK))
            == (byte)(ISO7816.CLA_ISO7816)) {

            // SM bits are equal to 00
            setAPDUChannel();
            switch (theAPDUBuffer[1]) {
                // ISO 7816-4 SELECT FIlE command
                case (byte)INS_SELECT:
                    theDispatcher.selectAPDU(theAPDU);
                    return true;
                // ISO 7816-4 MANAGE CHANNEL command
                case (byte)INS_MANAGECHANNEL:
                    theDispatcher.manageChannelAPDU(theAPDU);
                    return false;
            }
        } else if ((byte)(theAPDUBuffer[0] & (byte)(APDU.APDU_TYPE_MASK))
            == (byte)(ISO7816.CLA_ISO7816)) {
            // Check for secure messaging bits - if set, return error
            if (((byte)(theAPDUBuffer[0] & (byte)(APDU.APDU_SM_MASK))
                != 0) && (theAPDUBuffer[1] == (byte)INS_MANAGECHANNEL)) {
                ISOException.throwIt(ISO7816.SW_SECURE_MESSAGING_NOT_SUPPORTED);
            }
        }

        setAPDUChannel();
        return true;
    }

    void manageChannelAPDU(APDU theAPDU) throws ISOException {

        byte newChannel = (byte)(0xFF);
        byte cmdChannel = NativeMethods.getCurrentlySelectedChannel();
        byte channelStatus;
        byte maxChannels = NativeMethods.getMaxChannels();

        // If card has only one channel, functionality is not
        // supported.
        if (maxChannels == (byte)1) {
            ISOException.throwIt(ISO7816.SW_LOGICAL_CHANNEL_NOT_SUPPORTED);
        }

        // If managing channel is closed, return error
        if (NativeMethods.getChannelStatus(cmdChannel) == CHANNEL_CLOSED) {
            ISOException.throwIt(ISO7816.SW_LOGICAL_CHANNEL_NOT_SUPPORTED);
        }

        byte opType = theAPDUBuffer[ISO7816.OFFSET_P1];
        byte managedChannel = theAPDUBuffer[ISO7816.OFFSET_P2];

        // Check if managed channel is within range of legal channels
        // based on ISO7816-4.
        if ((managedChannel > (byte)3) || (managedChannel < (byte)0)) {
            ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
        }

        channelStatus = NativeMethods.getChannelStatus(managedChannel);

        switch (opType) {
            case (byte)P1_CLOSE_CHANNEL:
                // Basic channel cannot be closed
                if ((managedChannel == BASIC_CHANNEL)) {
                    ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
                }

                // If channel is open, close it
                if ((channelStatus & CHANNEL_OPEN_MASK) == CHANNEL_OPEN) {

                    // Deselect applet in channel
                    PrivAccess.deselectOnly(managedChannel);

                    // Close channel
                    NativeMethods.channelManage(managedChannel, OP_CHANNEL_CLOSE);
                } else {
                    // If channel is closed, throw a warning
                    ISOException.throwIt(ISO7816.SW_WARNING_STATE_UNCHANGED);
                }
                break;
            case (byte)P1_OPEN_CHANNEL:
                if (managedChannel == P2_AUTOSELECT_CHANNEL) {

                    // First, check outbound data length to ensure that 1 byte is expected
                    // Expected: 1 byte of data
                    short le = theAPDU.setOutgoing();
                    if ( le != 1 )
                       ISOException.throwIt((short)(ISO7816.SW_CORRECT_LENGTH_00 + (short)0x01));

                    // In autoselect, let JCRE choose channel automatically
                    newChannel =
                         NativeMethods.channelManage((byte)(-1), OP_CHANNEL_OPEN_AUTOSELECT);
                    if (newChannel == ERR_NO_CHANNEL_AVAILABLE) {
                        ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
                    }
                } else {
                    // Check channel availability and open it
                    if ((NativeMethods.channelManage(managedChannel, OP_CHANNEL_OPEN)) != (byte)(0xFF)) {
                        newChannel = managedChannel;
                    } else {
                        ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
                    }
                }

                try {
                    // Select applets in selected channel.
                    if (cmdChannel == BASIC_CHANNEL) {
                        // If managing channel is the basic channel, select the
                        // default applet in the newly open channel.
                        // The command channel is passed to allow other implementations
                        // to select a different default applet according to the
                        // selection channel.
                        PrivAccess.selectDefaultApplet(cmdChannel, newChannel);
                    } else {
                        // If managing channel is not the basic channel,
                        // select the currently selected applet in the managing
                        // channel.
                        byte selAppletID =
                            PrivAccess.getSelectedAppID(cmdChannel);
                        // If no applet selected on origin channel, skip
                        // selection process.
                        if (selAppletID != (byte)(-1)) {
                            PrivAccess.selectOnly( newChannel, selAppletID);
                        }
                    }
                } catch (ISOException isoEx) {
                    // An ISO exception will happen if applet fails to be selected.
                    // Close the channel if applet rejects selection or cannot be selected.
                    NativeMethods.channelManage(newChannel, OP_CHANNEL_CLOSE);
                    // Rethrow exception so that it can be caught in main
                    // Dispatcher loop
                    ISOException.throwIt(isoEx.getReason());
                }
                // In case of open auto-select successful, return channel information
                if (managedChannel == P2_AUTOSELECT_CHANNEL) {

                    // Write channel info to APDU buffer
                    theAPDU.setOutgoingLength((byte)1);
                    theAPDUBuffer[0] = (byte)(newChannel);
                    theAPDU.sendBytes((short)0, (short)1);
                }

                break;
            default:
                ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
        }
    }


    /**
     * Select commands are handled by the Dispatcher.
     * This method checks for the supported SELECT options. If it is a supported applet selection command,
     * and the applet is identified, deselect the currently selected applet and select the new one.
     * If applet selection fails, or the identified applet state is not selectable, deselect the
     * the currently selected applet and leave in "no applet selected" state.
     * This method rewinds the APDU if setIncomingAndReceive() is called.
     */
    void selectAPDU(APDU theAPDU) throws ISOException {
        byte selectChannel = NativeMethods.getCurrentlySelectedChannel();

        // If managing channel is closed, open a new channel
        if (NativeMethods.getChannelStatus(selectChannel) == CHANNEL_CLOSED) {
             byte openOK = NativeMethods.channelManage(selectChannel, OP_CHANNEL_OPEN);
             if (openOK == (byte)(-1)) {
                 ISOException.throwIt(ISO7816.SW_LOGICAL_CHANNEL_NOT_SUPPORTED);
             }
        }

        // ensure that P1==4 and (P2 & 0xF3)==0
        if (theAPDUBuffer[2] == P1_SELECT_DFBYNAME) {

            // check P2 = 0x0, 0x04, 0x08 or 0x0C
            if ((theAPDUBuffer[3] & P2_SELECT_OPTIONS) != P2_SELECT_OPTIONS_ONLY) {
                return;
            }

            // read AID and check APDU data length
            byte len = (byte)(theAPDU.setIncomingAndReceive());

            if ( len == theAPDUBuffer[4]) {

                // search for applet with AID
                byte i = AppletMgr.findApplet(theAPDUBuffer, (short)5, len);

                // find AID in AppletMgr
                // if no match of AID found in AppletMgr. JCRE assumes that the command is
                // not being used to select an applet and will invoke process() method
                // of the currently select applet (if there is one)
                if ( i != AppletMgr.APP_NULL) {
                  // is applet selectable?
                  if ( PrivAccess.getAppState( thePrivAccess.getAID(i) ) >= PrivAccess.APP_STATE_SELECTABLE ){
                    PrivAccess.selectApplet(selectChannel, i);
                  } else {
                    // if applet is not selectable, no applet selected
                    PrivAccess.deselectOnly(selectChannel);
                    NativeMethods.setChannelContext(selectChannel, PrivAccess.JCRE_CONTEXTID, false);
                  }
                }
            }
            // "undo" the receive so that the applet can do it
            undoReceive();
        }
    }

    void undoReceive() {
        // "undo" the receive so that the applet can do it
        theAPDU.undoIncomingAndReceive();
    }

    /**
     * This method is executed once each time the card is reset, when
     * the Dispatcher is initialized. All reset-time-only Dispatcher,
     * PrivAccess, and "system" initializations are done here.
     */
    static void cardReset() {
          // this implementation selects a default applet on card reset.
        PrivAccess.selectDefaultApplet(BASIC_CHANNEL, BASIC_CHANNEL);

        // ensure that there was no package table state is consistent and there
        // is no package in progress.
        PackageMgr.restore();

        // other card reset time initialization such as initializing
        // transient objects not shown here.
    }

    /**
     * This method is executed exactly once in the lifetime of the card, when
     * the Dispatcher is initialized for the first time. All first-time-only Dispatcher,
     * PrivAccess, and "system" initializations are done here.
     */
    static void cardInit() {
        initSystemExceptions();

        if (theDispatcher==null) theDispatcher = new Dispatcher();

        // create JCRE owned instances of exceptions
        // and designate as temporary Entry Point Objects
        // Exception classes init their own static systemInstance reference
        Exception ex;
        ex = new CardException( (short) 0 );
        NativeMethods.setJCREentry( ex, true );
        ex = new APDUException ( (short) 0 );
        NativeMethods.setJCREentry( ex, true );
        ex = new ISOException ( (short) 0 );
        NativeMethods.setJCREentry( ex, true );
        ex = new PINException ( (short) 0 );
        NativeMethods.setJCREentry( ex, true );
        ex = new UserException ( (short) 0 );
        NativeMethods.setJCREentry( ex, true );
        ex = new CryptoException ( (short) 0 );
        NativeMethods.setJCREentry( ex, true );

        // create JCRE owned instance of the APDU and designate as temporary Entry Point Object
        theAPDU = new APDU();
        NativeMethods.setJCREentry( theAPDU, true );

        // create JCRE owned instance of PrivAccess and designate as permanent Entry Point Object
        thePrivAccess = JCSystem.thePrivAccess = new PrivAccess();
        NativeMethods.setJCREentry( thePrivAccess, false );

        //get the GCRequested packed boolean value and set it false
        GarbageCollector.GCRequested = PrivAccess.getPackedBoolean().allocate();

        // get APDU buffer and mark as (temporary) Global Array.
        theAPDUBuffer = theAPDU.getBuffer();
        NativeMethods.setJCREentry( theAPDUBuffer, true );

        CryptoInit.initCryptoStorage();
        PrivAccess.initialize( theAPDU );
        NativeMethods.setCardInitialized();
    }


    /**
     * Initialize the java.lang exceptions and exceptions that the systems needs to hold
     * a pointer to. This is done exactly once.
     */
    private static void initSystemExceptions(){
        arrayIndexOutOfBoundsException = new ArrayIndexOutOfBoundsException();
        NativeMethods.setJCREentry( arrayIndexOutOfBoundsException, true );

        negativeArraySizeException = new NegativeArraySizeException();
        NativeMethods.setJCREentry( negativeArraySizeException, true );

        nullPointerException = new NullPointerException();
        NativeMethods.setJCREentry( nullPointerException, true );

        classCastException = new ClassCastException();
        NativeMethods.setJCREentry( classCastException, true );

        arithmeticException = new ArithmeticException();
        NativeMethods.setJCREentry( arithmeticException, true );

        securityException = new SecurityException();
        NativeMethods.setJCREentry( securityException, true );

        arrayStoreException = new ArrayStoreException();
        NativeMethods.setJCREentry( arrayStoreException, true );

        //CardRuntimeException exception needs to be initialized before
        //systemException and TransactionException are initialized
        //because this is the parent of the later 2.
        Exception ex = new CardRuntimeException ( (short) 0 );
        NativeMethods.setJCREentry( ex, true );

        systemException = new SystemException((short) 0);
        NativeMethods.setJCREentry( systemException, true );

        transactionException = new TransactionException((short) 0);
        NativeMethods.setJCREentry( transactionException, true );

        serviceException = new ServiceException((short) 0);
        NativeMethods.setJCREentry( serviceException, true );
    }

    /**
     * Only JCRE can use constructor.
     * No need to construct this class anyway. No instance methods/fields.
     */
    Dispatcher(){}

}
