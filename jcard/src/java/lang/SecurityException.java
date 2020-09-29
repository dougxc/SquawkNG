/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)SecurityException.java	1.6
// Version:1.6
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/java/lang/SecurityException.java 
// Modified:02/01/02 16:38:58
// Original author:  Ravi
// */

package java.lang;

/**
 * A JCRE owned instance of <code>SecurityException</code> is thrown by the Java Card Virtual Machine
 * to indicate a security violation.<p>
 * This exception is thrown when an attempt is made to illegally access an object
 * belonging to another applet. It may optionally be thrown by a Java Card VM
 * implementation to indicate fundamental language restrictions,
 * such as attempting to invoke a private method in another class. 
 * <p> For security reasons, the JCRE implementation may mute the card instead
 * of throwing this exception.
 * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
 * and can be accessed from any applet context. References to these temporary objects
 * cannot be stored in class variables or instance variables or array components.
 * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
 * <p>This Java Card class's functionality is a strict subset of the definition in the 
 * <em>Java Platform Core API Specification</em>.<p>
 */

public class SecurityException extends RuntimeException{
  /**
   * Constructs a <code>SecurityException</code>.
   */
  public SecurityException() {}

}
