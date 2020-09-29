/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)ArithmeticException.java	1.3
// Version:1.3
// Date:12/19/00
// 
// Archive:  /Products/Europa/api21/java/lang/ArithmeticException.java 
// Modified:12/19/00 15:03:00
// Original author:  Ravi
// */

package java.rmi;
import java.io.IOException;

/**
 * A JCRE owned instance of <code>RemoteException</code> is thrown to
 * indicate that a communication-related exception has occurred
 * during the execution of a remote method call.
 * Each method of a remote interface, an interface that extends
 * <CODE>java.rmi.Remote</CODE>, must list <CODE>RemoteException</CODE> or a superclass
 * in its <CODE>throws</CODE> clause.
 * <p>JCRE owned instances of exception classes are temporary JCRE Entry Point Objects
 * and can be accessed from any applet context. References to these temporary objects
 * cannot be stored in class variables or instance variables or array components.
 * See <em>Java Card Runtime Environment (JCRE) Specification</em>, section 6.2.1 for details.
 * <p>This Java Card class's functionality is a strict subset of the definition in the
 * <em>Java 2 Platform Standard Edition API Specification</em>.<p>
 */

public class RemoteException extends IOException{
    /**
     * Constructs a <code>RemoteException</code>.
     */
  public RemoteException() {}
}
