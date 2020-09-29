/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)ArrayIndexOutOfBoundsException.java	1.6
// Version:1.6
// Date:05/01/02
// 
// Archive:  /Products/Europa/api21/java/lang/ArrayIndexOutOfBoundsException.java 
// Modified:05/01/02 16:38:58
// Original author:  Ravi
// */

package java.lang;

/**
 * A JCRE owned instance of <code>ArrayIndexOutOfBoundsException</code> is thrown to indicate that
 * an array has been accessed with an illegal index.
 * The index is either negative or greater than or equal to the size of the array.
 * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
 * and can be accessed from any applet context. References to these temporary objects
 * cannot be stored in class variables or instance variables or array components.
 * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
 * <p>This Java Card class's functionality is a strict subset of the definition in the 
 * <em>Java Platform Core API Specification</em>.<p>
 */

public class ArrayIndexOutOfBoundsException extends IndexOutOfBoundsException{
  /**
   * Constructs an <code>ArrayIndexOutOfBoundsException</code>.
   */
  public ArrayIndexOutOfBoundsException() {}
  
}
