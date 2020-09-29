/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)APDUException.java	1.8
// Version:1.8
// Date:05/01/02
// 
// Archive:  /Products/Europa/api21/javacard/framework/APDUException.java 
// Modified:05/01/02 16:39:00
// Original author:  Ravi
// */

package javacard.framework;

/**
 * <code>APDUException</code> represents an <code>APDU</code> related exception.
 * <p>The <code>APDU</code> class throws JCRE owned instances of <code>APDUException</code>.
 * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
 * and can be accessed from any applet context. References to these temporary objects
 * cannot be stored in class variables or instance variables or array components.
 * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
 * @see APDU
 */

public class APDUException extends CardRuntimeException{

  // initialized when created by Dispatcher
  private static APDUException systemInstance;

  // APDUException reason code 
 /**
  * This APDUException reason code indicates that the method should not be invoked 
  * based on the current state of the APDU. 
  */
  public static final short ILLEGAL_USE = 1;
  
 /**
  * This reason code is used by the <code>APDU.sendBytes()</code> method to indicate
  * that the sum of buffer offset parameter and the byte length parameter exceeds the APDU
  * buffer size.
  */
  public static final short BUFFER_BOUNDS = 2;
  
 /**
  * This reason code is used by the <code>APDU.setOutgoingLength()</code> method to indicate
  * that the length parameter is greater that 256 or
  * if non BLOCK CHAINED data transfer is requested and <code>len</code> is greater than
  * (IFSD-2), where IFSD is the Outgoing Block Size. 
  */
  public static final short BAD_LENGTH = 3;
  
 /**
  * This reason code indicates that an unrecoverable error occurred in the
  * I/O transmission layer.
  */
  public static final short IO_ERROR = 4;
  
 /**
  * This reason code indicates that during T=0 protocol, the CAD did not return a GET RESPONSE 
  * command in response to a <61xx> response status to send additional data. The outgoing
  * transfer has been aborted. No more data or status can be sent to the CAD 
  * in this <code>Applet.process()</code> method.
  */
  public static final short NO_T0_GETRESPONSE = 0xAA;
  
 /**
  * This reason code indicates that during T=1 protocol, the CAD returned an ABORT S-Block
  * command and aborted the data transfer. The incoming or outgoing
  * transfer has been aborted. No more data can be received from the CAD.
  * No more data or status can be sent to the CAD 
  * in this <code>Applet.process()</code> method.
  */
  public static final short T1_IFD_ABORT = 0xAB;
  
  /**
  * This reason code indicates that during T=0 protocol, the CAD did not reissue the
  * same APDU command with the corrected length in response to a <6Cxx> response status
  * to request command reissue with the specified length. The outgoing
  * transfer has been aborted. No more data or status can be sent to the CAD 
  * in this <code>Applet.process()</code> method.
  */
  public static final short NO_T0_REISSUE = 0xAC;

  /**
   * Constructs an APDUException.
   * To conserve on resources use <code>throwIt()</code>
   * to use the JCRE owned instance of this class.
   * @param reason the reason for the exception.
   */
  public APDUException(short reason) {
    super(reason);
    if (systemInstance==null) // created by Dispatcher
        systemInstance = this;
    }

  /**
   * Throws the JCRE owned instance of <code>APDUException</code> with the specified reason.
   * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
   * and can be accessed from any applet context. References to these temporary objects
   * cannot be stored in class variables or instance variables or array components.
   * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
   * @param reason the reason for the exception.
   * @exception APDUException always.
   */
  public static void throwIt(short reason){    
    systemInstance.setReason(reason);
    throw systemInstance;
  }
}
