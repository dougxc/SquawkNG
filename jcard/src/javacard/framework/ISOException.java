/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)ISOException.java	1.6
// Version:1.6
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/javacard/framework/ISOException.java 
// Modified:02/01/02 16:39:00
// Original author:  Zhiqun
// */

package javacard.framework;

/**
 * <code>ISOException</code> class encapsulates an ISO 7816-4 response status word as
 * its <code>reason</code> code.
 * <p>The <code>APDU</code> class throws JCRE owned instances of <code>ISOException</code>.
 * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
 * and can be accessed from any applet context. References to these temporary objects
 * cannot be stored in class variables or instance variables or array components.
 * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
 */

public class ISOException extends CardRuntimeException {

  // initialized when created by Dispatcher
  private static ISOException systemInstance;
  
  /**
   * Constructs an ISOException instance with the specified status word.
   * To conserve on resources use <code>throwIt()</code>
   * to use the JCRE owned instance of this class.
   * @param sw the ISO 7816-4 defined status word
   */
  public ISOException(short sw){
    super(sw);
    if (systemInstance==null) // created by Dispatcher
        systemInstance = this;
  }

  /**
   * Throws the JCRE owned instance of the ISOException class with the specified status word.
   * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
   * and can be accessed from any applet context. References to these temporary objects
   * cannot be stored in class variables or instance variables or array components.
   * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
   * @param sw ISO 7816-4 defined status word
   * @exception ISOException always.
   */
  public static void throwIt(short sw){
    systemInstance.setReason(sw);
    throw systemInstance;
  }
}
