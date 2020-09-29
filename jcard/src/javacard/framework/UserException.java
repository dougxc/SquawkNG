/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)UserException.java	1.6
// Version:1.6
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/javacard/framework/UserException.java 
// Modified:02/01/02 16:39:00
// Original author:  Ravi
// */

package javacard.framework;

/**
 * <code>UserException</code> represents a User exception.
 * This class also provides a resource-saving mechanism (the <code>throwIt()</code> method) for user
 * exceptions by using a JCRE owned instance.
 * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
 * and can be accessed from any applet context. References to these temporary objects
 * cannot be stored in class variables or instance variables or array components.
 * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
 */

public class UserException extends CardException{

  // initialized when created by Dispatcher
  private static UserException systemInstance;

  /**
   * Constructs a <code>UserException</code> with reason = 0.
   * To conserve on resources use <code>throwIt()</code>
   * to use the JCRE owned instance of this class.
   */
  public UserException() {
    this((short)0);
  }

  /**
   * Constructs a <code>UserException</code> with the specified reason.
   * To conserve on resources use <code>throwIt()</code>
   * to use the JCRE owned instance of this class.
   * @param reason the reason for the exception.
   */
  public UserException(short reason) {
    super(reason);
    if (systemInstance==null) // created by Dispatcher
        systemInstance = this; 
    }

  /**
   * Throws the JCRE owned instance of <code>UserException</code> with the specified reason.
   * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
   * and can be accessed from any applet context. References to these temporary objects
   * cannot be stored in class variables or instance variables or array components.
   * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
   * @param reason the reason for the exception.
   * @exception UserException always.
   */
  public static void throwIt(short reason) throws UserException{
    systemInstance.setReason(reason);
    throw systemInstance;
  }
}
