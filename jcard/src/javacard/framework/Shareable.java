/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)Shareable.java	1.5
// Version:1.5
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/javacard/framework/Shareable.java 
// Modified:02/01/02 16:39:00
// Original author:  Mitch
// */

package javacard.framework;

/**
 * The Shareable interface serves to identify all shared objects.
 * Any object that needs to be shared through the applet firewall
 * must directly or indirectly implement this interface. Only those
 * methods specified in a shareable interface are available through
 * the firewall.
 *
 * Implementation classes can implement any number of shareable
 * interfaces and can extend other shareable implementation classes.
 */

public interface Shareable {}
