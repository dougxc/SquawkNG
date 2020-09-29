/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)ServiceException.java	1.10
// Version:1.10
// Date:05/01/02
// 
// Modified:05/01/02 16:39:00
// */

package javacard.framework.service;

import javacard.framework.* ;
/**
 * <code>ServiceException</code> represents a service framework related exception.
 * <p>The service framework classes throw JCRE owned instances of <code>ServiceException</code>.
 * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
 * and can be accessed from any applet context. References to these temporary objects
 * cannot be stored in class variables or instance variables or array components.
 * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details. 
 *
 * @version 1.0 
 */
public class ServiceException extends CardRuntimeException {

  // initialized when created by Dispatcher
  private static ServiceException systemInstance;

  // ServiceException reason code
  
  /**
  *This reason code is used to indicate that an input parameter is not
  *allowed.
  */
  public static final short ILLEGAL_PARAM = 1;

  /**
  *This reason code is used to indicate that a dispatch table is full
  */
  public static final short DISPATCH_TABLE_FULL = 2;
  
  /**
   * This reason code is used to indicate that the incoming data for a
   * command in the <CODE>APDU</CODE> object does not fit in the APDU buffer.
   */
  public static final short COMMAND_DATA_TOO_LONG = 3;

  /**
   * This reason code is used to indicate that the command in the <CODE>APDU</CODE>
   * object cannot be accessed for input processing.
   */
  public static final short CANNOT_ACCESS_IN_COMMAND = 4 ;
  
 /**
   * This reason code is used to indicate that the command in the <CODE>APDU</CODE> object
   * cannot be accessed for output processing.
   */
  public static final short CANNOT_ACCESS_OUT_COMMAND =  5;
  
  /**
   * This reason code is used to indicate that the command in the <CODE>APDU</CODE> object
   * has been completely processed.
   */
  public static final short COMMAND_IS_FINISHED = 6;
  
  /**
   * This reason code is used by RMIService to indicate that the remote
   * method returned a remote object which has not been exported.
   */
  public static final short REMOTE_OBJECT_NOT_EXPORTED = 7;
  
  /**
   * Constructs a <CODE>ServiceException</CODE>.
   * To conserve on resources use <code>throwIt()</code>
   * to use the JCRE owned instance of this class.
   * @param reason the reason for the exception.
   */
  public ServiceException(short reason) {
    super(reason);
    if (systemInstance==null) // created by Dispatcher
        systemInstance = this; 
    }

  /**
   * Throws the JCRE owned instance of <code>ServiceException</code> with the
   * specified reason.
   * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
   * and can be accessed from any applet context. References to these temporary objects
   * cannot be stored in class variables or instance variables or array components.
   * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
   * @param reason the reason for the exception.
   * @exception ServiceException always.
   */
  public static void throwIt (short reason) throws ServiceException {    
    systemInstance.setReason(reason);
    throw systemInstance;
  }


  

}

