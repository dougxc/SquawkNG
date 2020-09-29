/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)Object.java 1.5
// Version:1.5
// Date:02/01/02
//
// Archive:  /Products/Europa/api21/java/lang/Object.java
// Modified:02/01/02 16:38:58
// Original author:  Ravi
// */

package java.lang;

  /**
   * Class <code>Object</code> is the root of the Java Card class hierarchy.
   * Every class has <code>Object</code> as a superclass.
   * All objects, including arrays, implement the methods of this
   * class.
   * <p>This Java Card class's functionality is a strict subset of the definition in the
   * <em>Java Platform Core API Specification</em>.<p>
   */
public class Object {

  public Object() {}

 /**
  * Compares two Objects for equality. <p>
  * The <code>equals</code> method implements an equivalence relation:
  * <ul>
  * <li>It is <i>reflexive</i>: for any reference value <code>x</code>,
  * <code>x.equals(x)</code> should return <code>true</code>.
  * <li>It is <i>symmetric</i>: for any reference values <code>x</code> and
  * <code>y</code>, <code>x.equals(y)</code> should return
  * <code>true</code> if and only if <code>y.equals(x)</code> returns
  * <code>true</code>.
  * <li>It is <i>transitive</i>: for any reference values <code>x</code>,
  * <code>y</code>, and <code>z</code>, if <code>x.equals(y)</code>
  * returns  <code>true</code> and <code>y.equals(z)</code> returns
  * <code>true</code>, then <code>x.equals(z)</code> should return
  * <code>true</code>.
  * <li>It is <i>consistent</i>: for any reference values <code>x</code>
  * and <code>y</code>, multiple invocations of <code>x.equals(y)</code>
  * consistently return <code>true</code> or consistently return
  * <code>false</code>.
  * <li>For any reference value <code>x</code>, <code>x.equals(null)</code>
  * should return <code>false</code>.
  * </ul>
  * <p>
  * The equals method for class <code>Object</code> implements the most discriminating possible equivalence
  * relation on objects; that is, for any reference values <code>x</code> and <code>y</code>,
  * this method returns <code>true</code> if and only if <code>x</code> and <code>y</code>
  * refer to the same object (<code>x==y</code> has the value <code>true</code>).
  * @param obj the reference object with which to compare.
  * @return <code>true</code> if this object is the same as the obj argument; <code>false</code> otherwise.
  */
  public boolean equals(Object obj){ return (this==obj); }


/*if[EXCLUDE]*/

    public final void notify()                      { throw new Error(); }
    public final void notifyAll()                   { throw new Error(); }
    public final void wait(long timeout)            { throw new Error(); }
    public final void wait(long timeout, int nanos) { throw new Error(); }
    public final void wait()                        { throw new Error(); }
    public final Class getClass()                   { throw new Error(); }
    public int hashCode()                           { return 1;  }
    public String toString()                        { return ""; }
    protected void finalize()                       {}

/*end[EXCLUDE]*/
}
