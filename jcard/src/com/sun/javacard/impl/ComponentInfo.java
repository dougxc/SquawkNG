/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:%W%
// Version:%I%
// Date:%G%
//
// Archive:  com/sun/javacard/impl/ComponentInfo.java
// Modified:%G% %T%
// Original author:  Saqib
// */

package com.sun.javacard.impl;
class ComponentInfo{
    public short size;
    public short address;
    /**
     * Constructor
	 * @param compSize is the component size in memory
	 * @param compAddr is the component start address in memory
     */
    public ComponentInfo(short compSize, short compAddr){
        size = compSize;
        address = compAddr;
    }
}