/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)SystemException.java	1.9
// Version:1.9
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/javacard/framework/SystemException.java 
// Modified:02/01/02 16:39:00
// Original author:  Ravi
// */

package javacard.framework;

/**
 * <code>SystemException</code> represents a <code>JCSystem</code> class related exception.
 * It is also thrown by the <code>javacard.framework.Applet.register()</code> methods and by
 * the <code>AID</code> class constructor.
 * <p>These API classes throw JCRE owned instances of <code>SystemException</code>. 
 * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
 * and can be accessed from any applet context. References to these temporary objects
 * cannot be stored in class variables or instance variables or array components.
 * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
 * @see JCSystem
 * @see Applet
 * @see AID
 */

public class SystemException extends CardRuntimeException{

  // initialized when created by Dispatcher
  private static SystemException systemInstance;

   // SystemException reason code
 /**
  * This reason code is used to indicate that one or more input parameters
  * is out of allowed bounds.
  */
  public static final short ILLEGAL_VALUE = 1;
  
 /**
  * This reason code is used by the <code>makeTransient..()</code> methods 
  * to indicate that no room is available in volatile memory for the requested object.
  */
  public static final short NO_TRANSIENT_SPACE = 2;
  
 /**
  * This reason code is used to indicate that the request to create
  * a transient object is not allowed in the current applet context.
  * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details. 
  */
  public static final short ILLEGAL_TRANSIENT = 3;
  
 /**
  * This reason code is used by the <code>javacard.framework.Applet.register()</code> method
  * to indicate that the input AID parameter is not a legal AID value.
  */
  public static final short ILLEGAL_AID = 4;
 
 /**
  * This reason code is used to indicate that there is insufficient resource
  * in the Card for the request. 
  * <p>For example, the Java Card Virtual Machine may <code>throw</code>
  * this exception reason when there is insufficient heap space to create a new instance.
  */
  public static final short NO_RESOURCE = 5;
  
  /**
   * This reason code is used to indicate that the requested function is not
   * allowed. For example, <CODE>JCSystem.requestObjectDeletion()</CODE> method throws this exception if
   * the object deletion mechanism is not implemented.
   */
  public static final short ILLEGAL_USE = 6;

  /**
   * Constructs a SystemException.
   * To conserve on resources use <code>throwIt()</code>
   * to use the JCRE owned instance of this class.
   * @param reason the reason for the exception.
   */

  public SystemException(short reason) {
    super(reason);
    if (systemInstance==null) // created by Dispatcher
        systemInstance = this; 
    }

  /**
   * Throws the JCRE owned instance of <code>SystemException</code> with the specified reason.
   * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
   * and can be accessed from any applet context. References to these temporary objects
   * cannot be stored in class variables or instance variables or array components.
   * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
   * @param reason the reason for the exception.
   * @exception SystemException always.
   */
  public static void throwIt(short reason) throws SystemException{
    systemInstance.setReason(reason);
    throw systemInstance;
  }
}
