/*
 * @(#)CellTypeStateList.java	1.2 01/11/27 19:36:07
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.oops;

import java.util.*;

/** Auxiliary class for GenerateOopMap */
public class CellTypeStateList {
  public CellTypeStateList(int size) {
    list = new ArrayList(size);
    for (int i = 0; i < size; i++) {
      list.add(i, CellTypeState.makeBottom());
    }
  }
    
  public int size() {
    return list.size();
  }

  public CellTypeState get(int i) {
    return (CellTypeState) list.get(i);
  }

  public CellTypeStateList subList(int fromIndex, int toIndex) {
    return new CellTypeStateList(list.subList(fromIndex, toIndex));
  }

  //----------------------------------------------------------------------
  private List list;
  private CellTypeStateList(List list) {
    this.list = list;
  }
}
