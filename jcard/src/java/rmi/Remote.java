/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)Shareable.java	1.3
// Version:1.3
// Date:12/19/00
// 
// Archive:  /Products/Europa/api21/javacard/framework/Shareable.java 
// Modified:12/19/00 15:03:06
// Original author:  Mitch
// */

package java.rmi;

/**
 * The Remote interface serves to identify interfaces whose methods may be invoked
 * from a CAD client application. An object that is a remote object must directly
 * or indirectly implement this interface. Only those methods specified in a
 * "remote interface", an interface that extends <CODE>java.rmi.Remote</CODE> are available remotely.
 *
 * Implementation classes can implement any number of remote interfaces and can extend
 * other remote implementation classes. Java Card RMI provides a convenience class
 * called <CODE>javacard.framework.service.CardRemoteObject</CODE> that remote object
 * implementations can extend which facilitates remote object creation.
 * For complete details on Java Card RMI, see the <I>Java Card Runtime Environment
 * Specification</I> and the <CODE>javacard.framework.service</CODE> API package.
 */



public interface Remote {}
