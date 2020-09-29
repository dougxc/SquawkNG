/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * @(#)APDU.java	1.33 02/05/01
 */

package javacard.framework;

import com.sun.javacard.impl.PrivAccess;
import com.sun.javacard.impl.PackedBoolean;
import com.sun.javacard.impl.NativeMethods;
import com.sun.javacard.impl.Constants;

/**
 * Application Protocol Data Unit (APDU) is
 * the communication format between the card and the off-card applications.
 * The format of the APDU is defined in ISO specification 7816-4.<p>
 *
 * This class only supports messages which conform to the structure of
 * command and response defined in ISO 7816-4. The behavior of messages which
 * use proprietary structure of messages ( for example with header CLA byte in range 0xD0-0xFE ) is
 * undefined. This class does not support extended length fields.<p>
 *
 * The <code>APDU</code> object is owned by the JCRE. The <code>APDU</code> class maintains a byte array
 * buffer which is used to transfer incoming APDU header and data bytes as well as outgoing data.
 * The buffer length must be at least 133 bytes ( 5 bytes of header and 128 bytes of data ).
 * The JCRE must zero out the APDU buffer before each new message received from the CAD.<p>
 *
 * The JCRE designates the <code>APDU</code> object as a temporary JCRE Entry Point Object
 * (See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details).
 * A temporary JCRE Entry Point Object can be accessed from any applet context. References
 * to these temporary objects cannot be stored in class variables or instance variables
 * or array components.
 * <p>The JCRE similarly marks the APDU buffer as a global array
 * (See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.2 for details).
 * A global array
 * can be accessed from any applet context. References to global arrays
 * cannot be stored in class variables or instance variables or array components.
 * <p>
 *
 * The applet receives the <code>APDU</code> instance to process from
 * the JCRE in the <code>Applet.process(APDU)</code> method, and
 * the first five bytes [ CLA, INS, P1, P2, P3 ] are available
 * in the APDU buffer.<p>
 *
 * The <code>APDU</code> class API is designed to be transport protocol independent.
 * In other words, applets can use the same APDU methods regardless of whether
 * the underlying protocol in use is T=0 or T=1 (as defined in ISO 7816-3).<p>
 * The incoming APDU data size may be bigger than the APDU buffer size and may therefore
 * need to be read in portions by the applet. Similarly, the
 * outgoing response APDU data size may be bigger than the APDU buffer size and may
 * need to be written in portions by the applet. The <code>APDU</code> class has methods
 * to facilitate this.<p>
 *
 * For sending large byte arrays as response data,
 * the <code>APDU</code> class provides a special method <code>sendBytesLong()</code> which
 * manages the APDU buffer.<p>
 *
 * <pre>
 * // The purpose of this example is to show most of the methods
 * // in use and not to depict any particular APDU processing
 *
 *public void process(APDU apdu){
 *  // ...
 *  byte[] buffer = apdu.getBuffer();
 *  byte cla = buffer[ISO7816.OFFSET_CLA];
 *  byte ins = buffer[ISO7816.OFFSET_INS];
 *  ...
 *  // assume this command has incoming data
 *  // Lc tells us the incoming apdu command length
 *  short bytesLeft = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
 *  if (bytesLeft < (short)55) ISOException.throwIt( ISO7816.SW_WRONG_LENGTH );
 *
 *  short readCount = apdu.setIncomingAndReceive();
 *  while ( bytesLeft > 0){
 *      // process bytes in buffer[5] to buffer[readCount+4];
 *      bytesLeft -= readCount;
 *      readCount = apdu.receiveBytes ( ISO7816.OFFSET_CDATA );
 *      }
 *  //
 *  //...
 *  //
 *  // Note that for a short response as in the case illustrated here
 *  // the three APDU method calls shown : setOutgoing(),setOutgoingLength() & sendBytes()
 *  // could be replaced by one APDU method call : setOutgoingAndSend().
 *
 *  // construct the reply APDU
 *  short le = apdu.setOutgoing();
 *  if (le < (short)2) ISOException.throwIt( ISO7816.SW_WRONG_LENGTH );
 *  apdu.setOutgoingLength( (short)3 );
 *
 *  // build response data in apdu.buffer[ 0.. outCount-1 ];
 *  buffer[0] = (byte)1; buffer[1] = (byte)2; buffer[3] = (byte)3;
 *  apdu.sendBytes ( (short)0 , (short)3 );
 *  // return good complete status 90 00
 *  }
 * </pre>
 *
 * The <code>APDU</code> class also defines a set of <code>STATE_..</code> constants
 * which represent the various processing states of the <code>APDU</code>object based
 * on the methods invoked and the state of the data transfers. The
 * <code>getCurrentState()</code> method returns the current state.
 * <p>
 * Note that the state number assignments are ordered as follows:
 * STATE_INITIAL < STATE_PARTIAL_INCOMING < STATE_FULL_INCOMING <
 * STATE_OUTGOING < STATE_OUTGOING_LENGTH_KNOWN < STATE_PARTIAL_OUTGOING <
 * STATE_FULL_OUTGOING.
 * <p>
 * The following are processing error states and have negative state number
 * assignments :
 * STATE_ERROR_NO_T0_GETRESPONSE, STATE_ERROR_T1_IFD_ABORT, STATE_ERROR_IO and
 * STATE_ERROR_NO_T0_REISSUE.
 *
 * returns the current processing state of the
 * @see APDUException
 * @see ISOException
 */

public final class APDU  {
    
  // This APDU class implements the T=0 transport protocol.
    
  private static final short BUFFERSIZE = Constants.APDU_BUFFER_LENGTH;
  private static final byte IFSC = 1;//information field size for ICC i.e Maximum Incoming BlockSize in T=1
  private static final short IFSD = 258;//information field size for IFD i.e Maximum Outgoing BlockSize in T=1
    
  private static APDU theAPDU;
  /**
   * The APDU will use the buffer byte[]
   * to store data for input and output.
   */
  private byte[] buffer;
    
  private static byte[] ramVars;
  private static final byte LE  = (byte) 0;
  private static final byte LR  = (byte)(LE+1);
  private static final byte LC  = (byte)(LR+1);
  private static final byte PRE_READ_LENGTH = (byte)(LC+1);
  private static final byte CURRENT_STATE = (byte) (PRE_READ_LENGTH+1);
  private static final byte LOGICAL_CHN = (byte)(CURRENT_STATE+1);
  private static final byte RAM_VARS_LENGTH = (byte) (LOGICAL_CHN+1);
    
    
  // status code constants
  private static final short BUFFER_OVERFLOW = (short) 0xC001;
  private static final short READ_ERROR = (short) 0xC003;
  private static final short WRITE_ERROR = (short) 0xC004;
  private static final short INVALID_GET_RESPONSE = (short) 0xC006;
    
  // procedure byte type constants
  private static final byte ACK_NONE = (byte) 0;
  private static final byte ACK_INS = (byte)1;
  private static final byte ACK_NOT_INS = (byte)2;
    
  // Logical channels mask
  static final byte LOGICAL_CHN_MASK = (byte)0x03;
  static final byte APDU_TYPE_MASK = (byte)0xF0;
  static final byte APDU_CMD_MASK = (byte)0xFC;
  static final byte APDU_SM_MASK = (byte)0x0C;
    
  // Le = terminal expected length
  private short getLe() {
    if ( ramVars[LE]==(byte)0 ) return (short) 256;
    else return (short)(ramVars[LE] & 0xFF);
  }
  private void setLe( byte data ) { ramVars[LE] = data; }
    
  // Lr = our response length
  private short getLr() {
    if (getLrIs256Flag()) return 256;
    else return (short)(ramVars[LR] & 0xFF);
  }
  private void setLr( byte data ) { ramVars[LR] = data; }
    
  // Lc = terminal incoming length
  private byte getLc() { return ramVars[LC]; }
  private void setLc( byte data ) { ramVars[LC] = data; }
    
  // PreReadLength = length already received via setIncommingAndReceive(). Used for undo operation.
  private byte getPreReadLength() { return ramVars[PRE_READ_LENGTH]; }
  private void setPreReadLength( byte data ) { ramVars[PRE_READ_LENGTH] = data; }
    
  // currentState = APDU processing state
  private byte getCurrState() { return ramVars[CURRENT_STATE]; }
  private void setCurrState( byte data ) { ramVars[CURRENT_STATE] = data; }
  private void resetCurrState() { ramVars[CURRENT_STATE] = STATE_INITIAL; }
    
  // Logical Channel to whom the APDU is intended to (CLA byte)
  byte getLogicalChannel() { return ramVars[LOGICAL_CHN]; }
  private void setLogicalChannel( byte data ) { ramVars[LOGICAL_CHN] = data; }
    
  private PackedBoolean thePackedBoolean;
  private byte incomingFlag, outgoingFlag, outgoingLenSetFlag,
    lrIs256Flag, sendInProgressFlag, noChainingFlag, noGetResponseFlag;
    
  // IncomingFlag = setIncoming() has been invoked.
  private boolean getIncomingFlag() { return thePackedBoolean.get( incomingFlag ); }
  private void setIncomingFlag() { thePackedBoolean.set( incomingFlag ); }
  private void resetIncomingFlag() { thePackedBoolean.reset( incomingFlag ); }
    
  // SendInProgressFlag = No procedure byte needs to be sent.
  private boolean getSendInProgressFlag() { return thePackedBoolean.get( sendInProgressFlag ); }
  private void setSendInProgressFlag() { thePackedBoolean.set( sendInProgressFlag );}
  private void resetSendInProgressFlag() { thePackedBoolean.reset( sendInProgressFlag ); }
    
  // OutgoingFlag = setOutgoing() has been invoked.
  private boolean getOutgoingFlag() { return thePackedBoolean.get( outgoingFlag ); }
  private void setOutgoingFlag() { thePackedBoolean.set( outgoingFlag ); }
  private void resetOutgoingFlag() { thePackedBoolean.reset( outgoingFlag ); }
    
  // OutgoingLenSetFlag = setOutgoingLength() has been invoked.
  private boolean getOutgoingLenSetFlag() { return thePackedBoolean.get( outgoingLenSetFlag ); }
  private void setOutgoingLenSetFlag() { thePackedBoolean.set( outgoingLenSetFlag ); }
  private void resetOutgoingLenSetFlag() { thePackedBoolean.reset( outgoingLenSetFlag ); }
    
  // LrIs256Flag = Lr is not 0. It is actually 256. Saves 1 byte of RAM.
  private boolean getLrIs256Flag() { return thePackedBoolean.get( lrIs256Flag ); }
  private void setLrIs256Flag() { thePackedBoolean.set( lrIs256Flag ); }
  private void resetLrIs256Flag() { thePackedBoolean.reset( lrIs256Flag ); }
    
  // noChainingFlag = do not use <61,xx> chaining for outbound data transfer.
  // Note that the "get" method is package visible. This ensures that it is
  // entered via an "invokevirtual" instruction and not an "invokestatic"
  // instruction thereby ensuring a context switch
  boolean getNoChainingFlag() { return thePackedBoolean.get( noChainingFlag ); }
  private void setNoChainingFlag() { thePackedBoolean.set( noChainingFlag ); }
  private void resetNoChainingFlag() { thePackedBoolean.reset( noChainingFlag ); }
    
  // noGetResponseFlag = GET RESPONSE command was not received from CAD.
  private boolean getNoGetResponseFlag() { return thePackedBoolean.get( noGetResponseFlag ); }
  private void setNoGetResponseFlag() { thePackedBoolean.set( noGetResponseFlag ); }
  private void resetNoGetResponseFlag() { thePackedBoolean.reset( noGetResponseFlag ); }
    
  // This private method implements ISO 7816-4 logic to determine
  // the logical channel to whom this APDU command is intended to.
  private byte getChannelInfo() {
    byte theAPDUChannel = (byte)(buffer[ISO7816.OFFSET_CLA] & (byte)LOGICAL_CHN_MASK);
    byte theAPDUType = (byte) (buffer[ISO7816.OFFSET_CLA] & (byte)APDU_TYPE_MASK);
        
    // Verify if CLA byte contains channel information, according
    // to ISO 7816-4 section 5.4
    if ((theAPDUType != (byte)0x00) && (theAPDUType != (byte)0x80)
        && (theAPDUType != (byte)0x90) && (theAPDUType != (byte)0xA0)) {
      // All CLA bytes whose most significant nibble is not
      // '0X', '8X', '9X' or 'AX' do not encode any logical
      // channel information, and must be forwarded to the
      // basic channel.
      return (byte)0;
    }
    return theAPDUChannel;
  }
    
  // Current State Constants
  /** This is the state of a new <CODE>APDU</CODE> object when only the command
   * header is valid.
   */
  public static final byte STATE_INITIAL = 0;
    
  /** This is the state of a <CODE>APDU</CODE> object when incoming data
   * has partially been received.
   */
  public static final byte STATE_PARTIAL_INCOMING = 1;
    
  /** This is the state of a <CODE>APDU</CODE> object when all the
   * incoming data been received.
   */
  public static final byte STATE_FULL_INCOMING = 2;
    
  /** This is the state of a new <CODE>APDU</CODE> object when data transfer
   * mode is outbound but length is not yet known.
   */
  public static final byte STATE_OUTGOING = 3;
    
  /** This is the state of a <CODE>APDU</CODE> object when data transfer
   * mode is outbound and outbound length is known.
   */
  public static final byte STATE_OUTGOING_LENGTH_KNOWN = 4;
    
  /** This is the state of a <CODE>APDU</CODE> object when some outbound
   * data has been transferred but not all.
   */
  public static final byte STATE_PARTIAL_OUTGOING = 5;
    
  /** This is the state of a <CODE>APDU</CODE> object when all outbound data
   * has been transferred.
   */
  public static final byte STATE_FULL_OUTGOING = 6;
    
  /** This error state of a <CODE>APDU</CODE> object occurs when an <CODE>APDUException</CODE>
   * with reason code <CODE>APDUException.NO_T0_GETRESPONSE</CODE> has been
   * thrown.
   */
  public static final byte STATE_ERROR_NO_T0_GETRESPONSE = (byte) -1;
    
  /** This error state of a <CODE>APDU</CODE> object occurs when an <CODE>APDUException</CODE>
   * with reason code <CODE>APDUException.T1_IFD_ABORT</CODE> has been
   * thrown.
   */
  public static final byte STATE_ERROR_T1_IFD_ABORT = (byte) -2;
    
  /** This error state of a <CODE>APDU</CODE> object occurs when an <CODE>APDUException</CODE>
   * with reason code <CODE>APDUException.IO_ERROR</CODE> has been
   * thrown.
   */
  public static final byte STATE_ERROR_IO = (byte) -3;
    
  /** This error state of a <CODE>APDU</CODE> object occurs when an <CODE>APDUException</CODE>
   * with reason code <CODE>APDUException.NO_T0_REISSUE</CODE> has been
   * thrown.
   */
  public static final byte STATE_ERROR_NO_T0_REISSUE = (byte) -4;
    
  /**
   * Only JCRE should create an <code>APDU</code>.
   * <p>
   * @no params
   */
  APDU(){
        
    /*
      buffer = JCSystem.makeTransientByteArray(BUFFERSIZE, JCSystem.CLEAR_ON_RESET);
      */
    buffer = NativeMethods.t0InitAPDUBuffer();
    ramVars = JCSystem.makeTransientByteArray(RAM_VARS_LENGTH, JCSystem.CLEAR_ON_RESET);
        
    thePackedBoolean = PrivAccess.getPackedBoolean();
    incomingFlag = thePackedBoolean.allocate();
    sendInProgressFlag = thePackedBoolean.allocate();
    outgoingFlag = thePackedBoolean.allocate();
    outgoingLenSetFlag = thePackedBoolean.allocate();
    lrIs256Flag = thePackedBoolean.allocate();
    noChainingFlag = thePackedBoolean.allocate();
    noGetResponseFlag = thePackedBoolean.allocate();
    theAPDU = this;
  }
    
  /**
   * Returns the APDU buffer byte array.
   * <p>Note:<ul>
   * <li><em>References to the APDU buffer byte array
   * cannot be stored in class variables or instance variables or array components.
   * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.2 for details.</em>
   * </ul>
   * @return byte array containing the APDU buffer
   */
  public byte[] getBuffer() {
    return buffer;
  }
    
  /**
   * Returns the configured incoming block size.&nbsp;
   * In T=1 protocol, this corresponds to IFSC (information field size for ICC),
   * the maximum size of incoming data blocks into the card.&nbsp; In T=0 protocol,
   * this method returns 1.
   * IFSC is defined in ISO 7816-3.<p>
   * This information may be used to ensure that there is enough space remaining in the
   * APDU buffer when <code>receiveBytes()</code> is invoked.
   * <p>Note:<ul>
   * <li><em>On </em><code>receiveBytes()</code><em> the </em><code>bOff</code><em> param
   * should account for this potential blocksize.</em>
   * </ul>
   * @return incoming block size setting.
   * @see #receiveBytes(short)
   */
  public static short getInBlockSize() {
    return IFSC;
  }
    
  /**
   * Returns the configured outgoing block size.&nbsp;
   * In T=1 protocol, this corresponds to IFSD (information field size for interface device),
   * the maximum size of outgoing data blocks to the CAD.&nbsp;
   * In T=0 protocol, this method returns 258 (accounts for 2 status bytes).
   * IFSD is defined in ISO 7816-3.
   * <p>This information may be used prior to invoking the <code>setOutgoingLength()</code> method,
   * to limit the length of outgoing messages when BLOCK CHAINING is not allowed.
   * <p>Note:<ul>
   * <li><em>On </em><code>setOutgoingLength()</code><em> the </em><code>len</code><em> param
   * should account for this potential blocksize.</em>
   * </ul>
   * @return outgoing block size setting.
   * @see #setOutgoingLength(short)
   */
  public static short getOutBlockSize() {
    return IFSD;
  }
    
  /**
   * Media nibble mask in protocol byte
   */
  public static final byte PROTOCOL_MEDIA_MASK  = (byte)0xF0;
    
  /**
   * Type nibble mask in protocol byte
   */
  public static final byte PROTOCOL_TYPE_MASK  = (byte)0x0F;
    
  /**
   * ISO 7816 transport protocol type T=0.
   */
  public static final byte PROTOCOL_T0   = 0;
    
  /**
   * ISO 7816 transport protocol type T=1.&nbsp;This constant is also used to
   * denote the variant for contactless cards defined in ISO 14443-4.
   */
  public static final byte PROTOCOL_T1   = 1;
    
  /**
   * Transport protocol Media - Contacted Asynchronous Half Duplex 
   */
  public static final byte PROTOCOL_MEDIA_DEFAULT  = (byte)0x00;
    
  /**
   * Transport protocol Media - Contactless Type A 
   */
  public static final byte PROTOCOL_MEDIA_CONTACTLESS_TYPE_A  = (byte)0x80;
    
  /**
   * Transport protocol Media - Contactless Type B 
   */
  public static final byte PROTOCOL_MEDIA_CONTACTLESS_TYPE_B  = (byte)0x90;
    
  /**
   * Transport protocol Media - USB 
   */
  public static final byte PROTOCOL_MEDIA_USB                 = (byte)0xA0;
    
    
  /**
   * Returns the ISO 7816 transport protocol type, T=1 or T=0 in the low nibble
   * and the transport media in the upper nibble in use.
   * @return the protocol media and type in progress.
   * Valid nibble codes are listed in PROTOCOL_ .. constants above. See {@link #PROTOCOL_T0}
   */
  public static byte getProtocol() {
    return (byte) (PROTOCOL_MEDIA_DEFAULT+PROTOCOL_T0);
  }
    
  /**
   * In T=1 protocol, this method returns the Node Address byte, NAD.&nbsp;
   * In T=0 protocol, this method returns 0.
   * This may be used as additional information to maintain multiple contexts.
   * @return NAD transport byte as defined in ISO 7816-3.
   */
  public byte getNAD() {
    return (byte) 0;
  }
    
  /**
   * This method is used to set the data transfer direction to
   * outbound and to obtain the expected length of response (Le).
   * <p>Notes. <ul><li><em>Any remaining incoming data will be discarded.</em>
   * <li><em>In T=0 (Case 4) protocol, this method will return 256.</em>
   * <li><em>This method sets the state of the <code>APDU</code> object to
   * <code>STATE_OUTGOING</code>.</em>
   * </ul>
   * @return Le, the expected length of response.
   * @exception APDUException with the following reason codes:<ul>
   * <li><code>APDUException.ILLEGAL_USE</code> if this method or <code>setOutgoingNoChaining()</code> method already invoked.
   * <li><code>APDUException.IO_ERROR</code> on I/O error.</ul>
   */
  public short setOutgoing() throws APDUException {
    // if we've previously called this method, then throw an exception
    if ( getOutgoingFlag() )  APDUException.throwIt( APDUException.ILLEGAL_USE );
    setOutgoingFlag();
    setCurrState(STATE_OUTGOING);
    return getLe();
  }
    
  /**
   * This method is used to set the data transfer direction to
   * outbound without using BLOCK CHAINING (See ISO 7816-3/4) and to obtain the expected length of response (Le).
   * This method should be used in place of the <code>setOutgoing()</code> method by applets which need
   * to be compatible with legacy CAD/terminals which do not support ISO 7816-3/4 defined block chaining.
   * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 9.4 for details.
   * <p>Notes. <ul>
   * <li><em>Any remaining incoming data will be discarded.</em>
   * <li><em>In T=0 (Case 4) protocol, this method will return 256.</em>
   * <li><em>When this method is used, the </em><code>waitExtension()</code><em> method cannot be used.</em>
   * <li><em>In T=1 protocol, retransmission on error may be restricted.</em>
   * <li><em>In T=0 protocol, the outbound transfer must be performed
   * without using <code>(ISO7816.SW_BYTES_REMAINING_00+count)</code> response status chaining.</em>
   * <li><em>In T=1 protocol, the outbound transfer must not set the More(M) Bit in the PCB of the I block. See ISO 7816-3.</em>
   * <li><em>This method sets the state of the <code>APDU</code> object to
   * <code>STATE_OUTGOING</code>.</em>
   * </ul>
   * @return Le, the expected length of response data.
   * @exception APDUException with the following reason codes:<ul>
   * <li><code>APDUException.ILLEGAL_USE</code> if this method or <code>setOutgoing()</code> method already invoked.
   * <li><code>APDUException.IO_ERROR</code> on I/O error.</ul>
   */
  public short setOutgoingNoChaining() throws APDUException {
    // if we've previously called this method, then throw an exception
    if ( getOutgoingFlag() )  APDUException.throwIt( APDUException.ILLEGAL_USE );
    setOutgoingFlag();
    setNoChainingFlag();
    setCurrState(STATE_OUTGOING);
    return getLe();
  }
    
  /**
   * Sets the actual length of response data. Default is 0.
   * <p>Note:<ul>
   * <li><em>In T=0 (Case 2&4) protocol, the length is used by the JCRE to prompt the CAD for GET RESPONSE commands.</em>
   * <li><em>This method sets the state of the <code>APDU</code> object to
   * <code>STATE_OUTGOING_LENGTH_KNOWN</code>.</em>
   * </ul>
   * @param len the length of response data.
   * @exception APDUException with the following reason codes:<ul>
   * <li><code>APDUException.ILLEGAL_USE</code> if <code>setOutgoing()</code> not called or this method already invoked.
   * <li><code>APDUException.BAD_LENGTH</code> if <code>len</code> is greater than 256 or
   * if non BLOCK CHAINED data transfer is requested and <code>len</code> is greater than
   * (IFSD-2), where IFSD is the Outgoing Block Size. The -2 accounts for the status bytes in T=1.
   * <li><code>APDUException.NO_GETRESPONSE</code> if T=0 protocol is in use and
   * the CAD does not respond to <code>(ISO7816.SW_BYTES_REMAINING_00+count)</code> response status
   * with GET RESPONSE command on the same origin logical channel number as that of the current
   * APDU command.
   * <li><code>APDUException.NO_T0_REISSUE</code> if T=0 protocol is in use and
   * the CAD does not respond to <code>(ISO7816.SW_CORRECT_LENGTH_00+count)</code> response status
   * by re-issuing same APDU command on the same origin logical channel number as that of the current
   * APDU command with the corrected length.
   * <li><code>APDUException.IO_ERROR</code> on I/O error.</ul>
   * @see #getOutBlockSize()
   */
  public void setOutgoingLength(short len) throws APDUException{
    if ( !getOutgoingFlag() )  APDUException.throwIt(APDUException.ILLEGAL_USE);
    // if we've previously called this method, then throw an exception
    if ( getOutgoingLenSetFlag() )  APDUException.throwIt( APDUException.ILLEGAL_USE );
    if ( len>256 || len<0 )  APDUException.throwIt(APDUException.BAD_LENGTH);
    setOutgoingLenSetFlag();
    setCurrState(STATE_OUTGOING_LENGTH_KNOWN);
    setLr((byte)len);
    if ( len==256 ) setLrIs256Flag();
  }
    
  /**
   * Indicates that this command has incoming data.
   * <p>Note.<ul>
   * <li><em>In T=0 ( Case 3&4 ) protocol, the P3 param is assumed to be Lc.</em></ul>
   * @exception APDUException with the following reason codes:<ul>
   * <li><code>APDUException.ILLEGAL_USE</code> if this method already invoked or
   * if <code>setOutgoing()</code> or <code>setOutgoingNoChaining()</code> previously invoked.
   * <li><code>APDUException.IO_ERROR</code> on I/O error.</ul>
   */
  private void setIncoming() throws APDUException{
    // if JCRE has undone a previous setIncomingAndReceive ignore
    if ( getPreReadLength() != 0 ) return;
    // if we've previously called this or setOutgoing() method, then throw an exception
    if ( getIncomingFlag() || getOutgoingFlag() ) APDUException.throwIt( APDUException.ILLEGAL_USE );
    setIncomingFlag(); // indicate that this method has been called
    byte Lc = (byte) getLe(); // what we stored in Le was really Lc
    setLc( Lc );
    setLe((byte)0);    // in T=1, the real Le is now unknown (assume 256)
  }
    
  /**
   * Gets as many data bytes as will fit without APDU buffer overflow,
   * at the specified offset <code>bOff</code>.&nbsp; Gets all the remaining bytes if they fit.
   * <p>Notes:<ul>
   * <li><em>The space in the buffer must allow for incoming block size.</em>
   * <li><em>In T=1 protocol, if all the remaining bytes do not fit in the buffer, this method may
   * return less bytes than the maximum incoming block size (IFSC).</em>
   * <li><em>In T=0 protocol, if all the remaining bytes do not fit in the buffer, this method may
   * return less than a full buffer of bytes to optimize and reduce protocol overhead.</em>
   * <li><em>In T=1 protocol, if this method throws an </em><code>APDUException</code><em>
   * with </em><code>T1_IFD_ABORT</code><em> reason code, the JCRE will restart APDU command processing using the newly
   * received command. No more input data can be received.
   * No output data can be transmitted. No error status response can be returned.</em>
   * <li><em>This method sets the state of the <code>APDU</code> object to
   * <code>STATE_PARTIAL_INCOMING</code> if all incoming bytes are not received.</em>
   * <li><em>This method sets the state of the <code>APDU</code> object to
   * <code>STATE_FULL_INCOMING</code> if all incoming bytes are received.</em>
   * </ul>
   * @param bOff the offset into APDU buffer.
   * @return number of bytes read. Returns 0 if no bytes are available.
   * @exception APDUException with the following reason codes:<ul>
   * <li><code>APDUException.ILLEGAL_USE</code> if <code>setIncomingAndReceive()</code> not called or
   * if <code>setOutgoing()</code> or <code>setOutgoingNoChaining()</code> previously invoked.
   * <li><code>APDUException.BUFFER_BOUNDS</code> if not enough buffer space for incoming block size.
   * <li><code>APDUException.IO_ERROR</code> on I/O error.
   * <li><code>APDUException.T1_IFD_ABORT</code> if T=1 protocol is in use and the CAD sends
   * in an ABORT S-Block command to abort the data transfer.
   * </ul>
   * @see #getInBlockSize()
   */
  public short receiveBytes(short bOff) throws APDUException {
    if ( !getIncomingFlag() || getOutgoingFlag() ) APDUException.throwIt( APDUException.ILLEGAL_USE );
    short Lc = (short)(getLc()&0xFF);
    // T=1 check bOff against blocksize and Lc.
    if ( (bOff<0) || ((Lc>=IFSC) && (((short)(bOff+IFSC))>=BUFFERSIZE)) ) APDUException.throwIt( APDUException.BUFFER_BOUNDS);
        
    short pre =  (short)( getPreReadLength() & 0x00FF ) ;
    if ( pre != 0 ){
      setPreReadLength( (byte) 0 );
      if(Lc == 0) setCurrState(STATE_FULL_INCOMING);
      else setCurrState(STATE_PARTIAL_INCOMING);
      return pre;
    }
        
    if ( Lc!=0 ){
      short len = NativeMethods.t0RcvData( bOff );
      if (len<0) APDUException.throwIt( APDUException.IO_ERROR );
      Lc = (short)(Lc - len);
      setLc((byte)Lc);   // update RAM copy of Lc, the count remaining
      if(Lc == 0) setCurrState(STATE_FULL_INCOMING);
      else setCurrState(STATE_PARTIAL_INCOMING);
      return len;
    }
        
    setCurrState(STATE_FULL_INCOMING);
    return (short)0;
  }
    
  /**
   * This is the primary receive method.
   * Calling this method indicates that this APDU has incoming data. This method gets as many bytes
   * as will fit without buffer overflow in the APDU buffer following the header.&nbsp;
   * It gets all the incoming bytes if they fit.
   * <p>Notes:<ul>
   * <li><em>In T=0 ( Case 3&4 ) protocol, the P3 param is assumed to be Lc.</em>
   * <li><em>Data is read into the buffer at offset 5.</em>
   * <li><em>In T=1 protocol, if all the incoming bytes do not fit in the buffer, this method may
   * return less bytes than the maximum incoming block size (IFSC).</em>
   * <li><em>In T=0 protocol, if all the incoming bytes do not fit in the buffer, this method may
   * return less than a full buffer of bytes to optimize and reduce protocol overhead.</em>
   * <li><em>This method sets the transfer direction to be inbound
   * and calls </em><code>receiveBytes(5)</code><em>.</em>
   * <li><em>This method may only be called once in a </em><code>Applet.process()</code><em> method.</em>
   * <li><em>This method sets the state of the <code>APDU</code> object to
   * <code>STATE_PARTIAL_INCOMING</code> if all incoming bytes are not received.</em>
   * <li><em>This method sets the state of the <code>APDU</code> object to
   * <code>STATE_FULL_INCOMING</code> if all incoming bytes are received.</em>
   * </ul>
   * @return number of data bytes read. The Le byte, if any, is not included in the count.
   * Returns 0 if no bytes are available.
   * @exception APDUException with the following reason codes:<ul>
   * <li><code>APDUException.ILLEGAL_USE</code> if <code>setIncomingAndReceive()</code> already invoked or
   * if <code>setOutgoing()</code> or <code>setOutgoingNoChaining()</code> previously invoked.
   * <li><code>APDUException.IO_ERROR</code> on I/O error.
   * <li><code>APDUException.T1_IFD_ABORT</code> if T=1 protocol is in use and the CAD sends
   * in an ABORT S-Block command to abort the data transfer.
   * </ul>
   */
  public short setIncomingAndReceive() throws APDUException {
    setIncoming();
    return receiveBytes( (short) 5 );
  }
    
  /**
   * Send 61 <len> status bytes to CAD and
   * process resulting GET RESPONSE command in the ramVars buffer.
   * **NOTE** : This method overwrites ramVars[0..4]. **
   * @param len the length to prompt CAD with. ( 0<len<=256)
   * @return Expected Length (Le) in GET RESPONSE command.
   * @exception APDUException with the following reason codes:<ul>
   * <li><code>APDUException.NO_T0_GETRESPONSE</code> if GET RESPONSE command not received.
   * </ul>
   */
  private short send61xx( short len ) {
        
    byte originChannel = getCLAChannel();
    short expLen = len;
    do {
      NativeMethods.t0SetStatus( (short)(ISO7816.SW_BYTES_REMAINING_00+(len&0xFF)) );
      short newLen = NativeMethods.t0SndGetResponse(originChannel);
      if ( newLen == INVALID_GET_RESPONSE ) { // Get Response not received
	setNoGetResponseFlag();
	APDUException.throwIt(APDUException.NO_T0_GETRESPONSE);
      } else if ( newLen > 0 ) {
	ramVars[LE] = (byte) newLen;
	expLen = getLe();
      }
      else APDUException.throwIt( APDUException.IO_ERROR );
    } while ( expLen>len );
        
    resetSendInProgressFlag();
    return expLen;
  }
    
  /**
   * Sends <code>len</code> more bytes from APDU buffer at specified offset <code>bOff</code>.
   * <p>If the last part of the response is being sent by the invocation
   * of this method, the APDU buffer must not be altered. If the data is altered, incorrect output may be sent to
   * the CAD.
   * Requiring that the buffer not be altered allows the implementation to reduce protocol overhead
   * by transmitting the last part of the response along with the status bytes.
   * <p>Notes:<ul>
   * <li><em>If </em><code>setOutgoingNoChaining()</code><em> was invoked, output block chaining must not be used.</em>
   * <li><em>In T=0 protocol, if </em><code>setOutgoingNoChaining()</code><em> was invoked, Le bytes must be transmitted
   * before </em><code>(ISO7816.SW_BYTES_REMAINING_00+remaining bytes)</code><em> response status is returned.</em>
   * <li><em>In T=0 protocol, if this method throws an </em><code>APDUException</code><em>
   * with </em><code>NO_T0_GETRESPONSE</code><em> or </em><code>NO_T0_REISSUE</code><em> reason code,
   * the JCRE will restart APDU command processing using the newly
   * received command. No more output data can be transmitted. No error status response can be returned.</em>
   * <li><em>In T=1 protocol, if this method throws an </em><code>APDUException</code><em>
   * with </em><code>T1_IFD_ABORT</code><em> reason code, the JCRE will restart APDU command processing using the newly
   * received command. No more output data can be transmitted. No error status response can be returned.</em>
   * <li><em>This method sets the state of the <code>APDU</code> object to
   * <code>STATE_PARTIAL_OUTGOING</code> if all outgoing bytes have not been sent.</em>
   * <li><em>This method sets the state of the <code>APDU</code> object to
   * <code>STATE_FULL_OUTGOING</code> if all outgoing bytes have been sent.</em>
   * </ul>
   * @param bOff the offset into APDU buffer.
   * @param len the length of the data in bytes to send.
   * @exception APDUException with the following reason codes:<ul>
   * <li><code>APDUException.ILLEGAL_USE</code> if <code>setOutgoingLength()</code> not called
   * or <code>setOutgoingAndSend()</code> previously invoked
   * or response byte count exceeded or if <code>APDUException.NO_T0_GETRESPONSE</code> or
   * <code>APDUException.NO_T0_REISSUE</code> or <code>APDUException.T1_IFD_ABORT</code>
   * previously thrown.
   * <li><code>APDUException.BUFFER_BOUNDS</code> if <code>bOff</code> is negative or
   * <code>len</code> is negative or <code>bOff+len</code> exceeds the buffer size.
   * <li><code>APDUException.IO_ERROR</code> on I/O error.
   * <li><code>APDUException.NO_GETRESPONSE</code> if T=0 protocol is in use and
   * the CAD does not respond to <code>(ISO7816.SW_BYTES_REMAINING_00+count)</code> response status
   * with GET RESPONSE command on the same origin logical channel number as that of the current
   * APDU command.
   * <li><code>APDUException.NO_T0_REISSUE</code> if T=0 protocol is in use and
   * the CAD does not respond to <code>(ISO7816.SW_CORRECT_LENGTH_00+count)</code> response status
   * by re-issuing same APDU command on the same origin logical channel number as that of the current
   * APDU command with the corrected length.
   * <li><code>APDUException.T1_IFD_ABORT</code> if T=1 protocol is in use and the CAD sends
   * in an ABORT S-Block command to abort the data transfer.
   * </ul>
   * @see #setOutgoing()
   * @see #setOutgoingNoChaining()
   */
  public void sendBytes(short bOff, short len) throws APDUException {
        
    short result;
    if ( (bOff<0) || (len<0) || ((short)((bOff+len))>BUFFERSIZE) ) APDUException.throwIt( APDUException.BUFFER_BOUNDS );
    if ( !getOutgoingLenSetFlag() || getNoGetResponseFlag() )  APDUException.throwIt( APDUException.ILLEGAL_USE );
    if (len==0) return;
    short Lr = getLr();
    if (len>Lr) APDUException.throwIt( APDUException.ILLEGAL_USE );
        
    short Le = getLe();
        
    if ( getNoChainingFlag() ) {
            
      // Need to force GET RESPONSE for
      // Case 4 or
      // Case 2 but sending less than CAD expects
      if ( getIncomingFlag() || (Lr<Le) ) {
	Le = send61xx( Lr ); // expect Le==Lr, resets sendInProgressFlag.
	resetIncomingFlag(); // no more incoming->outgoing switch.
      }
            
      while ( len > Le ) { //sending more than Le
	if ( !getSendInProgressFlag() ) {
	  result = NativeMethods.t0SndData( buffer, bOff, Le, ACK_INS );
	  setSendInProgressFlag();
	}
	else result = NativeMethods.t0SndData(  buffer, bOff, Le, ACK_NONE );
                
	if ( result != 0 ) APDUException.throwIt( APDUException.IO_ERROR );
                
	bOff+=Le;
	len-=Le;
	Lr-=Le;
	Le = send61xx( Lr); // resets sendInProgressFlag.
      }
            
      if ( !getSendInProgressFlag() ) {
	result = NativeMethods.t0SndData( buffer, bOff, len, ACK_INS );
	setSendInProgressFlag();
      }
            
      else result = NativeMethods.t0SndData( buffer, bOff, len, ACK_NONE );
            
      if ( result != 0 ) APDUException.throwIt( APDUException.IO_ERROR );
      Lr-=len;
      Le-=len;
    }
    else { // noChainingFlag = FALSE
      while (len>0) {
	short temp=len;
	// Need to force GET RESPONSE for Case 4  & for partial blocks
	if ( (len!=Lr) || getIncomingFlag() || (Lr!=Le) || getSendInProgressFlag() ){
	  temp = send61xx( len ); // resets sendInProgressFlag.
	  resetIncomingFlag(); // no more incoming->outgoing switch.
	}
	result = NativeMethods.t0SndData( buffer, bOff, temp, ACK_INS );
	setSendInProgressFlag();
                
	if ( result != 0 ) APDUException.throwIt( APDUException.IO_ERROR );
	bOff+=temp;
	len-=temp;
	Lr-=temp;
	Le=Lr;
      }
    }
        
    if (Lr==0){
      setCurrState(STATE_FULL_OUTGOING);
    } else
      setCurrState(STATE_PARTIAL_OUTGOING);
    setLe((byte)Le);   // update RAM copy of Le, the expected count remaining
    setLr((byte)Lr);   // update RAM copy of Lr, the response count remaining
  }
    
  /**
   * Sends <code>len</code> more bytes from <code>outData</code> byte array starting at specified offset
   * <code>bOff</code>. <p>If the last of the response is being sent by the invocation
   * of this method, the APDU buffer must not be altered. If the data is altered, incorrect output may be sent to
   * the CAD.
   * Requiring that the buffer not be altered allows the implementation to reduce protocol overhead
   * by transmitting the last part of the response along with the status bytes.
   * <p>The JCRE may use the APDU buffer to send data to the CAD.
   * <p>Notes:<ul>
   * <li><em>If </em><code>setOutgoingNoChaining()</code><em> was invoked, output block chaining must not be used.</em>
   * <li><em>In T=0 protocol, if </em><code>setOutgoingNoChaining()</code><em> was invoked, Le bytes must be transmitted
   * before </em><code>(ISO7816.SW_BYTES_REMAINING_00+remaining bytes)</code><em> response status is returned.</em>
   * <li><em>In T=0 protocol, if this method throws an </em><code>APDUException</code><em> with
   * </em><code>NO_T0_GETRESPONSE</code><em> or </em><code>NO_T0_REISSUE</code><em> reason code,
   * the JCRE will restart APDU command processing using the newly received command. No more output
   * data can be transmitted. No error status response can be returned.</em>
   * <li><em>In T=1 protocol, if this method throws an </em><code>APDUException</code><em>
   * with </em><code>T1_IFD_ABORT</code><em> reason code, the JCRE will restart APDU command processing using the newly
   * received command. No more output data can be transmitted. No error status response can be returned.</em>
   * <li><em>This method sets the state of the <code>APDU</code> object to
   * <code>STATE_PARTIAL_OUTGOING</code> if all outgoing bytes have not been sent.</em>
   * <li><em>This method sets the state of the <code>APDU</code> object to
   * <code>STATE_FULL_OUTGOING</code> if all outgoing bytes have been sent.</em>
   * </ul>
   * <p>
   * @param outData the source data byte array.
   * @param bOff the offset into OutData array.
   * @param len the byte length of the data to send.
   * @exception SecurityException if the <code>outData</code> array is not accessible in the caller's context.
   * @exception APDUException with the following reason codes:<ul>
   * <li><code>APDUException.ILLEGAL_USE</code> if <code>setOutgoingLength()</code> not called
   * or <code>setOutgoingAndSend()</code> previously invoked
   * or response byte count exceeded or if <code>APDUException.NO_T0_GETRESPONSE</code> or
   * <code>APDUException.NO_T0_REISSUE</code> or <code>APDUException.NO_T0_REISSUE</code>
   * previously thrown.
   * <li><code>APDUException.IO_ERROR</code> on I/O error.
   * <li><code>APDUException.NO_T0_GETRESPONSE</code> if T=0 protocol is in use and
   * CAD does not respond to <code>(ISO7816.SW_BYTES_REMAINING_00+count)</code> response status
   * with GET RESPONSE command on the same origin logical channel number as that of the current
   * APDU command.
   * <li><code>APDUException.T1_IFD_ABORT</code> if T=1 protocol is in use and the CAD sends
   * in an ABORT S-Block command to abort the data transfer.
   * </ul>
   * @see #setOutgoing()
   * @see #setOutgoingNoChaining()
   */
  public void sendBytesLong(byte[] outData, short bOff, short len) throws APDUException, SecurityException {
    NativeMethods.checkArrayArgs( outData, bOff, len );
    NativeMethods.checkPreviousContextAccess(outData); 
    short sendLength = (short)buffer.length;
    while ( len>0 ) {
      if ( len<sendLength ) sendLength = len;
      Util.arrayCopy( outData, bOff, buffer, (short)0, sendLength );
      sendBytes( (short)0, sendLength );
      len-=sendLength;
      bOff+=sendLength;
    }
  }
    
  /**
   * This is the "convenience" send method. It provides for the most efficient way to send a short
   * response which fits in the buffer and needs the least protocol overhead.
   * This method is a combination of <code>setOutgoing(), setOutgoingLength( len )</code> followed by
   * <code>sendBytes ( bOff, len )</code>. In addition, once this method is invoked, <code>sendBytes()</code> and
   * <code>sendBytesLong()</code> methods cannot be invoked and the APDU buffer must not be altered.<p>
   * Sends <code>len</code> byte response from the APDU buffer starting at the specified offset <code>bOff</code>.
   * <p>Notes:<ul>
   * <li><em>No other </em><code>APDU</code><em> send methods can be invoked.</em>
   * <li><em>The APDU buffer must not be altered. If the data is altered, incorrect output may be sent to
   * the CAD.</em>
   * <li><em>The actual data transmission may only take place on return from </em><code>Applet.process()</code>
   * <li><em>This method sets the state of the <code>APDU</code> object to
   * <code>STATE_FULL_OUTGOING</code>.</em>
   * </ul>
   * <p>
   * @param bOff the offset into APDU buffer.
   * @param len the bytelength of the data to send.
   * @exception APDUException with the following reason codes:<ul>
   * <li><code>APDUException.ILLEGAL_USE</code> if <code>setOutgoing()</code>
   * or <code>setOutgoingAndSend()</code> previously invoked
   * or response byte count exceeded.
   * <li><code>APDUException.IO_ERROR</code> on I/O error.</ul>
   */
  public void setOutgoingAndSend( short bOff, short len) throws APDUException {
    setOutgoing();
    setOutgoingLength(len);
    sendBytes( bOff, len );
  }
    
  /** This method returns the current processing state of the
   * <CODE>APDU</CODE> object. It is used by the <CODE>BasicService</CODE> class to help
   * services collaborate in the processing of an incoming APDU command.
   * Valid codes are listed in STATE_ .. constants above. See {@link #STATE_INITIAL}
   * @return the current processing state of the APDU
   * @see javacard.framework.service.BasicService
   */
  public byte getCurrentState() {
    return getCurrState();
  }
    
  private static void checkAPDUObjectAccess() throws SecurityException {
    // check if the caller context is selected context
    // check if process() method is in progress
        
    byte commandChannel = NativeMethods.getCurrentlySelectedChannel();
    if ( ( PrivAccess.getSelectedAppID(commandChannel) != PrivAccess.getCurrentAppID() ) ||
	 ( ! PrivAccess.getPrivAccess().inProcessMethod() ) )
      {
	throw Dispatcher.securityException;
      }
  }
    
  /** This method is called to obtain a reference to the current <CODE>APDU</CODE> object.
   * This method can only be called in the context of the currently
   * selected applet.
   * <p>Note:<ul>
   * <li><em>Do not call this method directly or indirectly from within a method
   * invoked remotely via Java Card RMI method invocation from the client. The
   * APDU object and APDU buffer are reserved for use by RMIService. Remote
   * method parameter data may become corrupted.</em>
   *</ul>
   * @return the current <CODE>APDU</CODE> object being processed
   * @exception SecurityException if <ul>
   * <li>the current context is not the context of the currently selected applet instance or
   * <li>the method is not called, directly or indirectly, from the applet's
   * process method.
   * <li>the method is called during applet installation.
   * </ul>
   */
  public static APDU getCurrentAPDU() throws SecurityException {
    checkAPDUObjectAccess();
    return theAPDU;
  }
    
  /** This method is called to obtain a reference to the current
   * APDU buffer.
   * This method can only be called in the context of the currently
   * selected applet.
   * <p>Note:<ul>
   * <li><em>Do not call this method directly or indirectly from within a method
   * invoked remotely via Java Card RMI method invocation from the client. The
   * <CODE>APDU</CODE> object and APDU buffer are reserved for use by <CODE>RMIService</CODE>. Remote
   * method parameter data may become corrupted.</em>
   * </ul>
   * @exception SecurityException if <ul>
   * <li>the current context is not the context of the currently selected applet or
   * <li>the method is not called, directly or indirectly, from the applet's
   * process method.
   * <li>the method is called during applet installation.
   * </ul>
   * @return the APDU buffer of the <CODE>APDU</CODE> object being processed
   */
  public static byte[] getCurrentAPDUBuffer() throws SecurityException {
    checkAPDUObjectAccess();
    return theAPDU.getBuffer();
  }
    
  /**
   * Returns the logical channel number associated with the current <CODE>APDU</CODE> command
   * based on the CLA byte. A number in the range 0-3 based on the least
   * significant two bits of the CLA byte is returned if the command contains
   * logical channel encoding. If the command does not contain logical channel
   * information, 0 is returned.
   * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section
   * 4.3 for encoding details.<p>
   * @return logical channel number, if present, within the CLA byte, 0 otherwise.
   */
  public static byte getCLAChannel() {
    return theAPDU.getLogicalChannel();
  }
    
  void resetAPDU() {
    // as described earlier, we assume case 1 or 2, so Le=P3 and Lc=0
    setLe( buffer[ISO7816.OFFSET_LC] );
    setLc( (byte)0 );
    resetCurrState();
        
    // set logical channel information as specified in the APDU CLA byte
    setLogicalChannel(getChannelInfo());
        
    // save Lr=0, reset flags
    setLr((byte)0);
    resetIncomingFlag();
    resetOutgoingFlag();
    resetOutgoingLenSetFlag();
    resetSendInProgressFlag();
    resetLrIs256Flag();
    resetNoChainingFlag();
    resetNoGetResponseFlag();
    setPreReadLength( (byte)0 );
  }
    
  /**
   * Only JCRE should call this method. <p>
   * Sends 0s for any remaining data to be sent based on the
   * promised responseLength (default=0/Le) and status bytes to complete this APDUs
   * response and initiates new APDU exchange.
   * <p> Note : <ul>
   * <li><em>This method sets the state of the <code>APDU</code> object to
   * <code>STATE_INITIAL</code>.</em>
   * </ul>
   * @param status the ISO R-Apdu status bytes sw1 and sw2 response
   * @exception APDUException with the following reason codes:<ul>
   * <li><code>APDUException.IO_ERROR</code> on I/O error.</ul>
   */
  void complete(short status) throws APDUException{
        
    short result;
        
    // Zero out APDU buffer
    Util.arrayFillNonAtomic( buffer, (short)0, BUFFERSIZE, (byte)0 );
        
    if ( ( !getNoGetResponseFlag() ) && ( getSendInProgressFlag() ) ) {
      short Le = (byte)getLe();
      short sendLen = (short)32;
      while ( Le > 0 ) {
	if (Le<32) sendLen=Le;
	result = NativeMethods.t0SndData( buffer, (byte) 0, sendLen, ACK_NONE );
	if ( result != 0 ) APDUException.throwIt( APDUException.IO_ERROR );
	Le-=sendLen;
      }
    }
        
    buffer[0] = (byte) ( status >> 8);
    buffer[1] = (byte) status;
        
    if (status==0) result = NativeMethods.t0RcvCommand();
    else {
      NativeMethods.t0SetStatus( status );
      result = NativeMethods.t0SndStatusRcvCommand();
    }
        
    if ( result != 0 ) APDUException.throwIt( APDUException.IO_ERROR );
        
    resetAPDU();
  }
    
  /**
   * Only JCRE should call this method. <p>
   * This method puts the externally visible state of the apdu back as if
   * the <code>setIncomingAndReceive</code> has not yet been invoked. It needs to
   * ensure, though that no real I/O is performed when <code>setIncomingAndReceive</code> is
   * invoked next. This method must only be called it <code>setIncomingAndReceive</code> has
   * been invoked by the JCRE. This method must not be invoked however, if <code>receiveBytes</code>
   * has also been invoked.
   */
  void undoIncomingAndReceive() {
    setPreReadLength( (byte)(buffer[ISO7816.OFFSET_LC] - getLc()) );
    setCurrState(STATE_INITIAL);
  }
    
  /**
   * Requests additional processing time from CAD. The implementation should ensure that this method
   * needs to be invoked only under unusual conditions requiring excessive processing times.
   * <p>Notes:<ul>
   * <li><em>In T=0 protocol, a NULL procedure byte is sent to reset the work waiting time (see ISO 7816-3).</em>
   * <li><em>In T=1 protocol, the implementation needs to request the same T=0 protocol work waiting time quantum
   * by sending a T=1 protocol request for wait time extension(see ISO 7816-3).</em>
   * <li><em>If the implementation uses an automatic timer mechanism instead, this method may do nothing.</em>
   * </ul>
   * <p>
   * @exception APDUException with the following reason codes:<ul>
   * <li><code>APDUException.ILLEGAL_USE</code> if <code>setOutgoingNoChaining()</code> previously invoked.
   * <li><code>APDUException.IO_ERROR</code> on I/O error.</ul>
   */
  public static void waitExtension() throws APDUException{
        
    if ( theAPDU.getNoChainingFlag() ) APDUException.throwIt( APDUException.ILLEGAL_USE );
    //send a procedure byte of 0x60 to request additional waiting time.
    short result = NativeMethods.t0Wait();
    if ( result != 0 ) APDUException.throwIt( APDUException.IO_ERROR );
  }
}

