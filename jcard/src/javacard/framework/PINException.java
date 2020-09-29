/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)PINException.java	1.5
// Version:1.5
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/javacard/framework/PINException.java 
// Modified:02/01/02 16:39:00
// Original author:  Ravi
// */

package javacard.framework;

/**
 * <code>PINException</code> represents a <code>OwnerPIN</code> class access-related exception.
 * <p>The <code>OwnerPIN</code> class throws JCRE owned instances of <code>PINException</code>.
 * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
 * and can be accessed from any applet context. References to these temporary objects
 * cannot be stored in class variables or instance variables or array components.
 * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
 * @see OwnerPIN
 */

public class PINException extends CardRuntimeException{

  // initialized when created by Dispatcher
  private static PINException systemInstance;

  // PINException reason codes
 /**
  * This reason code is used to indicate that one or more input parameters
  * is out of allowed bounds.
  */
  public static final short ILLEGAL_VALUE = 1;

  /**
   * Constructs a PINException.
   * To conserve on resources use <code>throwIt()</code>
   * to use the JCRE owned instance of this class.
   * @param reason the reason for the exception.
   */
  public PINException(short reason) {
    super(reason);
    if (systemInstance==null) // created by Dispatcher
        systemInstance = this; 
    }

  /**
   * Throws the JCRE owned instance of <code>PINException</code> with the specified reason.
   * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
   * and can be accessed from any applet context. References to these temporary objects
   * cannot be stored in class variables or instance variables or array components.
   * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
   * @param reason the reason for the exception.
   * @exception PINException always.
   */
  public static void throwIt (short reason){    
    systemInstance.setReason(reason);
    throw systemInstance;
  }
}
