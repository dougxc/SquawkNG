/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)RuntimeException.java	1.5
// Version:1.5
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/java/lang/RuntimeException.java 
// Modified:02/01/02 16:38:58
// Original author:  Ravi
// */

package java.lang;

/**
 * <code>RuntimeException</code> is the superclass of those exceptions that can be thrown
 * during the normal operation of the Java Card Virtual Machine.<p> 
 * A method is not required to declare in its throws clause any subclasses of
 * <code>RuntimeException</code> that might be thrown during the execution of the
 * method but not caught.
 * <p>This Java Card class's functionality is a strict subset of the definition in the 
 * <em>Java Platform Core API Specification</em>.<p> 
 */

public class RuntimeException extends Exception {

  /**
   * Constructs a <code>RuntimeException</code> instance.
   */
  public RuntimeException() {}

}
