/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)NullPointerException.java	1.6
// Version:1.6
// Date:05/01/02
// 
// Archive:  /Products/Europa/api21/java/lang/NullPointerException.java 
// Modified:05/01/02 16:38:58
// Original author:  Ravi
// */

package java.lang;

/**
 * A JCRE owned instance of <code>NullPointerException</code> is thrown when an applet attempts
 * to use <code>null</code> in a case where an object is required. These include:<p><ul>
 * <li> Calling the instance method of a <code>null</code> object. 
 * <li> Accessing or modifying the field of a <code>null</code> object. 
 * <li> Taking the length of <code>null</code> as if it were an array. 
 * <li> Accessing or modifying the slots of <code>null</code> as if it were an array. 
 * <li> Throwing <code>null</code> as if it were a <code>Throwable</code> value.
 * </ul>
 * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
 * and can be accessed from any applet context. References to these temporary objects
 * cannot be stored in class variables or instance variables or array components.
 * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
 * <p>This Java Card class's functionality is a strict subset of the definition in the 
 * <em>Java Platform Core API Specification</em>.<p>
 */

public class NullPointerException extends RuntimeException{
  /**
   * Constructs a <code>NullPointerException</code>.
   */
  public NullPointerException() {}
    
}
