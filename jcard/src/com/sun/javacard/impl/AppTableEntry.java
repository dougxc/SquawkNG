/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)AppTableEntry.java	1.7
// Version:1.7
// Date:02/01/02
// 
// Archive:  /Products/Europa/api21/com/sun/javacard/impl/AppTableEntry.java 
// Modified:02/01/02 16:38:57
// Original author:  Mitch Butler
// */

package com.sun.javacard.impl;

import javacard.framework.AID;
import javacard.framework.Applet;

/**
 * This class contains the data structure used by the AppletMgr class to creat Applet Table.
 */
class AppTableEntry
{
	Applet theApplet;        // applet instance.
	AID theAID;              // AID of applet instance.
	byte theContext;         // packageId of applet class
}
