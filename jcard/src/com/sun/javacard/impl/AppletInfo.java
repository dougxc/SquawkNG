/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:%W%
// Version:%I%
// Date:%G%
//
// Archive:  com/sun/javacard/impl/AppletInfo.java
// Modified:%G% %T%
// Original author:  Saqib
// */

package com.sun.javacard.impl;

import javacard.framework.AID;

class AppletInfo{
    public AID theClassAID;
    public short installMethodAddr;
    /**
     * Constructor
	 * @param aid is the class AID of the applet
	 * @param methodAddr is the address of the applet's install method
     */
    public AppletInfo(AID aid, short methodAddr){
        theClassAID = aid;
        installMethodAddr = methodAddr;
    }
}