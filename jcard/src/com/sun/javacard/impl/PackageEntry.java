/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)PackageEntry.java	1.11
// Version:1.11
// Date:02/06/02
//
// Archive:  /Products/Europa/api21/com/sun/javacard/impl/PackageEntry.java
// Modified:02/06/02 16:38:57
// Original author: Joe Chen
// Modified by: Saqib Ahmad
//-

package com.sun.javacard.impl;

import javacard.framework.AID;

/*
 * This class implements the resident package data structure
 */
public class PackageEntry {

    AID pkgAID;                    // reference to AID of this package
    byte pkgMinor;                 // package minor version number
    byte pkgMajor;                 // package major version number
    byte pkgFlags;                 // package modifier bit fields
    short pkgStaticReferenceCount; // static reference count
    byte importCount;               //number of packages imported
    byte[] importedPackages;        //information regarding imported packages
    ComponentInfo[] compInfo;       //component sizes and addresses
    byte appletCount;               //applet count
    AppletInfo[] applets;           //static applet information
    byte pkgNamelength;             //package name length for 2.2 packages
    byte[] pkgName;                 //package name


    private static byte appCount = 0;

    /**
     * Constructor
	 * @param AID of the new package
	 * @param id of new package (which is the index in the package table)
	 * @param major is the major version of the package
	 * @param minor is the minor version of the package
	 * @param flags is the package flags
     */
    public PackageEntry(AID aid, byte major, byte minor, byte flags) {
        pkgAID = aid;
        pkgMajor = major;
        pkgMinor = minor;
        pkgFlags = flags;
        compInfo = new ComponentInfo[PackageMgr.COMPONENT_COUNT];
        appCount = 0;
        appletCount = 0;
    }

    /**
     * sets the count for static reference type fields
	 * @param count of static reference type fields
     */
    public void setStaticCount(short count){
        pkgStaticReferenceCount = count;
    }

    /**
     * Adds a component's info to the compInfo array
     * @param component size
     * @param component address
	 * @param compIndex is the index of the component in the components array
     */
    public void setComponentInfo(short compSize, short compAddr, byte compIndex){
        compInfo[compIndex] = new ComponentInfo(compSize, compAddr);
    }

    /**
     * Sets information regarding imported packages in the importedPackages array
     * @param info array that contains the info regarding imported packages
     * @param offset into the info array where the required information starts
     * @param count is the number of entries of interest in the array
     */
    public void setImportInfo(byte info[], byte offset, byte count){
        importCount = count;

        //initialize the array
        importedPackages = new byte[importCount];

        //set the values in the array
        for(byte i = 0; i < count; i++){
            importedPackages[i] = info[offset + i];
        }
    }

    /**
     * Initializes the applets array with the applet count
     * @param applet count in the package being installed
     */
    public void initializeAppletArray(byte count){
        appletCount = count;
        applets = new AppletInfo[count];
    }

    /**
     * Adds an applet's infomration to the applets array
     * @param class AID
     * @param install method address
     */
    public void addAppletInfo(AID aid, short address){
        applets[appCount++] = new AppletInfo(aid, address);
    }

    /**
     * Tries to find an applet with the specific AID . If the applet
     * is found, it's install method address is returned. -1 is returned otherwise
     * @param bArray contains the applet class AID
     * @param offset in bArray from where the AID starts
     * @param length of AID
     * @return applet's install method address if the applet is found, -1 otherwise
     */

    public short getAppletInstallMethodAddress(byte[] bArray, short offset, byte length){
        for(byte i = 0; i < appletCount; i++){
            if(applets[i] != null){
                if(applets[i].theClassAID.equals(bArray, offset, length)){
                    return applets[i].installMethodAddr;
                }
            }
        }
        //If the information was not found, return -1
        return -1;
    }

    public void setPkgNameAndLength(byte[] name, byte length){
        pkgNamelength = length;
        this.pkgName = name;
    }

    public void getPackageName(byte[] buffer, byte offset){
        for(byte i = 0; i < pkgNamelength; i++){
    			buffer[offset+i] = pkgName[i];
		    }
    }
}
