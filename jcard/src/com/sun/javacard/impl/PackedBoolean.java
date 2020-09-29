/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)PackedBoolean.java	1.6
// Version:1.6
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/com/sun/javacard/impl/PackedBoolean.java 
// Modified:02/01/02 16:38:57
// Original author:  Ravi
// */

package com.sun.javacard.impl;

import javacard.framework.JCSystem;

/**
 * The <code>PackedBoolean</code> class manages booleans
 * in volatile storage space efficiently.
 * Multiple classes may share the same instance
 * of this class for efficient volatile usage.
 */

public class PackedBoolean{

  protected byte[] container;
  protected byte nextId;

 /**
   * Constructor. Allocates an instance of PackedBoolean.
   * @param maximum bytes for boolean storage.
   */
  public PackedBoolean(byte maxBytes) {
    container = JCSystem.makeTransientByteArray((short)(maxBytes), JCSystem.CLEAR_ON_RESET);
    nextId = 0;
    }

 /**
   * Allocates a new boolean and returns the associated byte identifier.
   */
  public byte allocate() {
    return (byte) nextId++;
    }

 /**
   * Returns the state of identified boolean.
   * @param boolean identifier
   */
  public boolean get( byte identifier ) {
    return (access(identifier, (byte)0));
    }

 /**
   * Changes the state of the identified boolean to the
   * specified value or simply queries
   * @param identifier of boolean flag
   * @param type 1 set, -1 reset, 0 no change
   * @return value boolean value of specified flag
   */
  public boolean access ( byte identifier,  byte type ) {
    byte bOff = (byte) (identifier >> 3);
    byte bitNum = (byte)(identifier & 0x7);
    byte bitMask = (byte) ( (short)0x80 >> bitNum );
    switch (type){
        case 1: container[bOff] |= bitMask; break;
        case -1: container[bOff] &= (~bitMask); break;
    }
    return ((container[bOff] & bitMask)!=0);
  }
    
    
    /**
   * Sets the state of the identified boolean to true.
   * @param boolean identifier
   */
  public void set ( byte identifier ) {
    access( identifier, (byte)1 );
    }

 /**
   * Resets the state of the identified boolean to false.
   * @param boolean identifier
   */
  public void reset ( byte identifier ) {
    access( identifier, (byte)-1 );
    }
}
