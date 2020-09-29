/*
 * @(#)BitMapClosure.java	1.2 01/11/27 19:37:23
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.oops;

public interface BitMapClosure {
  /** Called when specified bit in map is set */
  public void doBit(int offset);
}
