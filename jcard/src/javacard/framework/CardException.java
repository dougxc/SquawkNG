/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)CardException.java	1.9
// Version:1.9
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/javacard/framework/CardException.java 
// Modified:02/01/02 16:39:00
// Original author:  Zhiqun
// */

package javacard.framework;
import com.sun.javacard.impl.PrivAccess;

/**
 * The <code>CardException</code> class
 * defines a field <code>reason </code>and two accessor methods <code>
 * getReason()</code> and <code>setReason()</code>. The <code>reason</code>
 * field encapsulates exception cause identifier in Java Card.
 * All Java Card checked Exception classes should extend
 * <code>CardException</code>. This class also provides a resource-saving mechanism
 * (<code>throwIt()</code> method) for using a JCRE owned instance of this class.
 * <p> Even if a transaction is in progress, the update of the internal <code>reason</code>
 * field shall not participate in the transaction. The value of the internal <code>reason</code>
 * field of JCRE owned instance is reset to 0 on a tear or reset.
 */

public class CardException extends Exception {

  private byte[] theSw;
  
  // initialized when created by Dispatcher
  private static CardException systemInstance;

  /**
   * Construct a CardException instance with the specified reason.
   * To conserve on resources, use the <code>throwIt()</code> method
   * to use the JCRE owned instance of this class.
   * @param reason the reason for the exception
   */
  public CardException(short reason){
    if (PrivAccess.getCurrentAppID()==PrivAccess.JCRE_CONTEXTID) {// created by Dispatcher  
        if (systemInstance==null) systemInstance = this; // this must be created before the subclasses
        theSw = JCSystem.makeTransientByteArray( (short)2, (byte)JCSystem.CLEAR_ON_RESET );
    } else
        theSw = new byte[2];
    Util.setShort(theSw, (short)0, reason);
  }

  /** Get reason code
   * @return the reason for the exception
   */
  public short getReason() {
    return Util.getShort(theSw, (short)0);
  }

  /** Set reason code
   * @param reason the reason for the exception
   */
  public void setReason(short reason) {
      Util.arrayFillNonAtomic( theSw, (short)0, (short)1, (byte) (reason >>> 8) );
      Util.arrayFillNonAtomic( theSw, (short)1, (short)1, (byte)reason );
  }

  /**
   * Throw the JCRE owned instance of <code>CardException</code> class with the
   * specified reason.
   * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
   * and can be accessed from any applet context. References to these temporary objects
   * cannot be stored in class variables or instance variables or array components.
   * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
   * @param reason the reason for the exception
   * @exception CardException always.
   */
  public static void throwIt(short reason) throws CardException{   
    systemInstance.setReason(reason);
    throw systemInstance;
  }
}
