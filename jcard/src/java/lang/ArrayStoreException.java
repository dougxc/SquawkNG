/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)ArrayStoreException.java	1.5
// Version:1.5
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/java/lang/ArrayStoreException.java 
// Modified:02/01/02 16:38:58
// Original author:  Ravi
// */

package java.lang;

/**
 * A JCRE owned instance of <code>ArrayStoreException</code> is thrown to indicate that an attempt
 * has been made to store the wrong type of object into an array of objects.
 * For example, the following code generates an <code>ArrayStoreException</code>:<p>
 *
 * <pre>
 *        Object x[] = new AID[3];
 *        x[0] = new OwnerPIN( (byte) 3, (byte) 8);
 * </pre>
 * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
 * and can be accessed from any applet context. References to these temporary objects
 * cannot be stored in class variables or instance variables or array components.
 * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
 * <p>This Java Card class's functionality is a strict subset of the definition in the 
 * <em>Java Platform Core API Specification</em>.<p>
 */

public class ArrayStoreException extends RuntimeException{
  /**
   * Constructs an <code>ArrayStoreException</code>.
   */
  public ArrayStoreException() {}
  
}
