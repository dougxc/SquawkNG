/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)Throwable.java  1.5
// Version:1.5
// Date:02/01/02
//
// Archive:  /Products/Europa/api21/java/lang/Throwable.java
// Modified:02/01/02 16:38:58
// Original author:  Ravi
// */

package java.lang;

  /**
   * The Throwable class is the superclass of all errors and exceptions in the Java Card subset
   * of the Java language.
   * Only objects that are instances of this class (or of one of its
   * subclasses) are thrown by the Java Card Virtual Machine
   * or can be thrown by the Java <code>throw</code> statement.
   * Similarly, only this class or one of its subclasses
   * can be the argument type in a <code>catch</code> clause.
   * <p>This Java Card class's functionality is a strict subset of the definition in the
   * <em>Java Platform Core API Specification</em>.<p>
   */
public class Throwable {

  /**
   * Constructs a new <code>Throwable</code>.
   */
  public Throwable() {}

/*if[SQUAWK]*/
    public Throwable(String message)        { throw new Error(); }
    public String getMessage()              { throw new Error(); }
    public String toString()                { throw new Error(); }
    public void printStackTrace()           { throw new Error(); }
/*end[SQUAWK]*/
}
