/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)ClassCastException.java	1.5
// Version:1.5
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/java/lang/ClassCastException.java 
// Modified:02/01/02 16:38:58
// Original author:  Ravi
// */

package java.lang;

/**
  * A JCRE owned instance of <code>ClassCastException</code> is thrown to indicate that
  * the code has attempted to cast an object to a subclass of which it is not an instance.
  * For example, the following code generates a <code>ClassCastException</code>:<p>
  *
  * <pre>
  *        Object x = new OwnerPIN( (byte)3, (byte)8);
  *        JCSystem.getAppletShareableInterfaceObject( (AID)x, (byte)5 );
  * </pre>
  * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
  * and can be accessed from any applet context. References to these temporary objects
  * cannot be stored in class variables or instance variables or array components.
  * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
  * <p>This Java Card class's functionality is a strict subset of the definition in the 
  * <em>Java Platform Core API Specification</em>.<p>
  */

public class ClassCastException extends RuntimeException{
  /**
   * Constructs a <code>ClassCastException</code>.
   */
  public ClassCastException() {}
    
}
