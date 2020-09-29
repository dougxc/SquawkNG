/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//+
// Workfile:@(#)PackageMgr.java	1.25
// Version:1.25
// Date:03/20/02
//
// Archive:  /Products/Europa/api21/com/sun/javacard/impl/PackageMgr.java
// Modified:03/20/02 16:38:57
// Original author: Joe Chen

package com.sun.javacard.impl;

import javacard.framework.*;

/*
 * This class implements methods to manage resident packages
 */
public class PackageMgr {

    // constants
    public static final byte ILLEGAL_ADDRESS = (byte)0;
    public static final byte ILLEGAL_ID = -1;
    static final short ON_CARD_PKG_MAX = 32;
    static final short COMP_HEADER_SIZE = (short)3;
    static final byte MASK_PKG_HAS_APPLET = (byte)0x4;

    /*New Constants*/
    public static final byte CLASS_COMPONENT_INDEX = (byte)0;
    public static final byte METHOD_COMPONENT_INDEX = (byte)1;
    public static final byte STATIC_FIELD_COMPONENT_INDEX = (byte)2;
    public static final byte EXPORT_COMPONENT_INDEX = (byte)3;
    public static final byte COMPONENT_COUNT = (byte)4;

    public static final byte DELETE_PACKAGE = (byte)0xc0;
    public static final byte DELETE_PACKAGE_AND_APPLETS = (byte)0xc2;


    public static final byte INSTALL_METHOD_ADDRESS = (byte)1;
    public static final byte PACKAGE_ID = (byte)2;

    public static final byte FAILURE = (byte)0;
    public static final byte SUCCESS = (byte)1;

    public static final byte NO_DEPENDENCIES = (byte)2;
    public static final byte DEPENDENCIES_PRESENT = (byte)3;
    public static final byte APPLETS_PRESENT = (byte)4;
    public static final byte DEPENDENCIES_ON_APPLETS = (byte)5;

    /*New Variables to be used*/
    public static byte g_packageInProcess = ILLEGAL_ID;
    public static short g_tempMemoryAddress = 0;
    public static short g_tempMemorySize = 0;


    // class variables
    static PackageEntry[] f_pkgTable;
    public static byte f_firstEEPkgID;

    public static short g_excTableEntry;
    public static byte g_newPackageIdentifier;
    public static PackageEntry g_newPackage;

    /***********************************************************************************************/
    //new variables and constants for supporting more than 16 packages (upto 255, but right now, 32)
    //on the card
    //on card applet package max is 16
    public static final byte ON_CARD_APPLET_PKG_MAX = (byte)16;

    public static byte appletPkgCount = (byte)0;

    //this is used to contain pkgIds of applet packages. Size of this array is going to be
    //ON_CARD_APPLET_PKG_MAX which is how many applet packages we are going to allow on the card.
    static byte[] packageContextTable;


    /*******************************************************************************************/
    /**
     * initialize once per JCRE lifetime
     */
    public static void init() {
        f_pkgTable = new PackageEntry[ON_CARD_PKG_MAX];
        packageContextTable = new byte[ON_CARD_APPLET_PKG_MAX];
        g_packageInProcess = ILLEGAL_ID;
        for(byte i = 0; i < ON_CARD_APPLET_PKG_MAX; i++){
            packageContextTable[i]= ILLEGAL_ID;
        }
        appletPkgCount = NativeMethods.initAppPkgContextTable(packageContextTable);
        f_firstEEPkgID = NativeMethods.getMaxPackageIdentifier();
    }

    /**
     * reset once per CAP file
     */
    public static void reset() {
        //Needs to restore the package manager to it's original state.
        //Needs to check if any CAP file was already being processed
        //it should be commited before the new installation starts.
        //if the changes for the previous one had not already been
        //commited, we have to remove it from the card before
        //we attempt anything else.
        restore();
        g_newPackage = null;
        g_newPackageIdentifier = ILLEGAL_ID;
        for(byte i = f_firstEEPkgID; i < ON_CARD_PKG_MAX; i++){
            if(f_pkgTable[i]==null){
                g_newPackageIdentifier = i;
                break;
            }
        }
        g_excTableEntry = ILLEGAL_ADDRESS;
        g_packageInProcess = g_newPackageIdentifier;
        g_tempMemoryAddress = 0;
        g_tempMemorySize = 0;
    }


    /**
     * conditionally calls Package Deletion in case of failure or tear
     * This method also is called once per "select" APDU command i.e.
     * this is done when the installer applet is selected.
     * just to make sure that nothing was being processed
     * during the last invokation of the installer, we call
     * the restore() method which takes care of everything.
     * Also does the work of the cleanup() method which has been removed.
     */
    public static void restore() {
        if(g_packageInProcess!= ILLEGAL_ID){
            /*
             We need to remove this package from the memory
             to do that we use the already existing package removal mechanism.
             To use the existing mechansim we have to put the package table entry
             in the package table because it relies on the entry being there.
             The GarbageCollector.deletePackage() method then puts null in it's place
            */
            if(f_pkgTable[g_packageInProcess] == null){
                f_pkgTable[g_packageInProcess] = g_newPackage;
                /*The new entry also needs to be garbage collected*/
                g_newPackage = null;
            }
            /*Remove the package from memory*/
            removePackage(g_packageInProcess);
            /*Reclaim the temporarily allocated space*/
            if(g_tempMemoryAddress != ILLEGAL_ADDRESS){
                freeTempMemory();
            }
            /*There isn't any installation in progress anymore*/
            g_packageInProcess = ILLEGAL_ID;
        }
    }

    /**/
    public static void freeTempMemory(){
        /*Reclaim the temporarily allocated space*/
        JCSystem.beginTransaction();
        NativeMethods.freeHeap(g_tempMemoryAddress, g_tempMemorySize);
        g_tempMemoryAddress = ILLEGAL_ADDRESS;
        g_tempMemorySize = 0;
        JCSystem.commitTransaction();
    }

    /**
     * finalize a CAP file download in an atomic transaction
     * (should be called within a transaction block)
     */
    public static void commit() {

        /*
         * add the new package to the package table
         * (newPackage == null if in "create only" scenario
         */
        if (g_newPackage != null) {
            f_pkgTable[g_newPackageIdentifier] = g_newPackage;
            //since the installation is successfully completed, reset this variable
            g_packageInProcess = ILLEGAL_ID;
            g_newPackage = null;
        }
    }

    public static void addAppletPackage(byte pkgId){
        for(byte i = 0; i < ON_CARD_APPLET_PKG_MAX; i++){
            if(packageContextTable[i]==ILLEGAL_ID){
                packageContextTable[i] = g_newPackageIdentifier;
                appletPkgCount++;
                break;
            }
        }
    }

    public static void handlePackageDeletion(APDU apdu){
        byte requestStatus;
        byte pkgId = 0;
        byte AIDLength = 0;
        byte[] buffer =apdu.getBuffer();
        byte command = buffer[ISO7816.OFFSET_INS];
        try{
             //read data into the APDU buffer
            apdu.setIncomingAndReceive();

            if((buffer[ISO7816.OFFSET_LC] < 6) || (buffer[ISO7816.OFFSET_LC] > 17)){
                //error: AID and length of AID cannot be less than 6 bytes or greater than 17 bytes
                ISOException.throwIt(javacard.framework.ISO7816.SW_WRONG_LENGTH);
            }

            AIDLength = buffer[ISO7816.OFFSET_CDATA];
            pkgId = findPkgID(buffer,(short)( ISO7816.OFFSET_CDATA + 1), AIDLength);

            //Possible errors:
            if(pkgId < 0){
                //error: package not found
                ISOException.throwIt(Errors.PACKAGE_NOT_FOUND);
            }
            else if(pkgId < f_firstEEPkgID){
                //error: package is ROM package
                ISOException.throwIt(Errors.PACKAGE_IS_ROM_PACKAGE);
            }

            //start the transaction
            JCSystem.beginTransaction();

            //the package being processed is the package being deleted
            g_packageInProcess = pkgId;
            //call the proper package deletion method
            if(command == DELETE_PACKAGE){
                requestStatus = removePackage(pkgId);
            }
            else{
                requestStatus = removePackageAndApplets(pkgId, buffer);
            }
            g_packageInProcess = ILLEGAL_ID;
            JCSystem.commitTransaction();
        }catch (ISOException e) {
            if (JCSystem.getTransactionDepth() != 0){
                JCSystem.abortTransaction();
            }
            ISOException.throwIt(e.getReason());
        }
    }

    /**
     * look for the package with the matching AID and matching version number
     *
     * @aid the byte array that contains the package AID bytes
     * @offset the offset of AID bytes in aid
     * @length the length of AID starting from offset
     * @major the package major version number
     * @minor the package minor version number
     * @return the package identifier found in ROM or EEPROM, -1 otherwise
     */
    public static byte getPkgID(byte[] aid, short offset, byte length,
                                            byte major, byte minor){
        // search in ROM first
        byte pkgID = NativeMethods.getPackageIdentifier(aid, offset,
                length, major, minor);
        if (pkgID != ILLEGAL_ID) {
            return pkgID;
        }

        // search EEPROM next
        for (pkgID = f_firstEEPkgID; pkgID < ON_CARD_PKG_MAX; pkgID++) {
            if(f_pkgTable[pkgID] != null){
                if (f_pkgTable[pkgID].pkgAID.equals(aid, offset, length) &&
                        major == f_pkgTable[pkgID].pkgMajor &&
                        (minor & 0xFF) <= (f_pkgTable[pkgID].pkgMinor & 0xFF)) {
                    return pkgID;
                }
            }
        }
        return ILLEGAL_ID;
    }

    /**
     * look for the package with the matching AID (without matching version #s)
     *
     * @aid the byte array that contains the package AID bytes
     * @offset the offset of AID bytes in aid
     * @length the length of AID starting from offset
     * @return the package identifier found in ROM or EEPROM, -1 otherwise
     */
    public static byte findPkgID(byte[] aid, short offset, byte length) {
        // search in ROM first
        byte pkgID = NativeMethods.findPackageIdentifier(aid, offset, length);
        if (pkgID != ILLEGAL_ID) {
            return pkgID;
        }

        // search EEPROM next
        for (pkgID = f_firstEEPkgID; pkgID < ON_CARD_PKG_MAX; pkgID++) {
            if(f_pkgTable[pkgID] !=null){
                if (f_pkgTable[pkgID].pkgAID.equals(aid, offset, length)) {
                    return pkgID;
                }
            }
        }
        return ILLEGAL_ID;
    }

    /**
     * return reference to the export component of a package
     * @param pkgID the package identifier
     * @return short the reference to the export component
     */
    public static short getExportAddress(byte pkgID) {

        short exportAddr = ILLEGAL_ADDRESS;

        // search in ROM first
        if (pkgID < f_firstEEPkgID) {
            exportAddr = NativeMethods.getPackageExportComponent(pkgID);
            if (exportAddr == ILLEGAL_ADDRESS) {
                return exportAddr;
            }

            /*
             * special case: ROM package's export component has a 3-byte
             *               header (tag, count) while a downloaded
             *               package does not.
             */
            exportAddr += COMP_HEADER_SIZE;
        } else {
            // return from EEPROM package table
            exportAddr = f_pkgTable[pkgID].compInfo[EXPORT_COMPONENT_INDEX].address;
        }
        return exportAddr;
    }

	/**
	 * This method tries to find a package that has the class component that covers
	 * the range of addresses including the address provided to this method as a parameter
	 * @param address that needs to be convered
	 */
	static byte findDownloadedPkgForAddr(short address){
        for (byte i=(byte)f_firstEEPkgID; i<(byte)ON_CARD_PKG_MAX; i++) {
            /*
             since package table may have holes in it once package
             deletion is implemented.
            */
            if(f_pkgTable[i] != null){
                short range = (short)(f_pkgTable[i].compInfo[CLASS_COMPONENT_INDEX].size +
                              f_pkgTable[i].compInfo[CLASS_COMPONENT_INDEX].address);
                //if the given address is within the range
                if(address >= f_pkgTable[i].compInfo[CLASS_COMPONENT_INDEX].address
                   && address <= range){
                    //set the package AID in the given array starting from the
                    //given offset
                    return i;
                }
            }
		}
		return (byte)-1;
	}

    /**
     * get the ID of the package with the class component covering the
     * range of addresses in which the given address lies.
     * @param address that needs to be covered
     * @return the package ID
    */
    static byte getPkgIDForAddress(short address){
		byte requiredId = findDownloadedPkgForAddr(address);
		if(requiredId != (byte)-1){
			return requiredId;
		}
        //if no such package is found in EEPROM find the package in ROM.
        return NativeMethods.getPkgIDForAddress(address);
    }


    /**
     * get the AID of the package with the class component covering the
     * range of addresses in which the given address lies.
     * @param address that needs to be covered
     * @param buffer to set the AID in once a package is found
     * @param offset in the buffer from where the AID should start
     * @return the size of AID
    */
    static byte getPkgNameForClass(short classAddress, byte[] buffer, byte offset){
		byte requiredId = findDownloadedPkgForAddr(classAddress);
		if(requiredId != -1){
			//set the package name length at the offset
			buffer[offset++] = f_pkgTable[requiredId].pkgNamelength;
//			buffer[offset++] = f_pkgTable[requiredId].pkgNamelength;
			//set the package Name in the given array starting from the
			//given offset + 1
			f_pkgTable[requiredId].getPackageName(buffer, offset);
			//return the number of bytes contained in the package name
			//plus one byte for it's size data.
			return (byte)(f_pkgTable[requiredId].pkgNamelength + offset);
		}
		//else call the corresponding native method to find the package in ROM and return
		//the data returned by it
		return NativeMethods.getPkgNameForClass(classAddress, buffer, offset);
    }


    /**
     * Check for packages dependent on this package
     * @param index in package table where the package is
     * @return status NO_DEPENDENCIES or DEPENDECIES_PRESENT
    */
    public static byte checkDependencies(byte index){
        for (byte i=(byte)f_firstEEPkgID; i<(byte)ON_CARD_PKG_MAX; i++) {
            // since package table may have holes in it once package
            // deletion is implemented go through the entire table.
            if(i == index)continue;
            if(f_pkgTable[i] != null){
                for (byte j = 0; j < f_pkgTable[i].importCount; j++){
                    if(f_pkgTable[i].importedPackages[j] == index)
                        return DEPENDENCIES_PRESENT;
                }
            }
        }
        return NO_DEPENDENCIES;
    }

    /**
     * Remove the package from the card memory
     * @param index in package table where the package is
     * @return status SUCCESS or FAILURE
    */
    public static byte removePackage(byte index) throws ISOException{
        //check dependencies on the package to be deleted
        if(checkDependencies(index) == NO_DEPENDENCIES){
            //check if there are any applet instances belonging to this package
            if(AppletMgr.getAppletsForPackage(null, (byte)0, index) == 0){
                GarbageCollector.deletePackage(index);
                return SUCCESS;
            }else{
                ISOException.throwIt(Errors.APPLETS_PRESENT);
            }
        }else{
            ISOException.throwIt(Errors.DEPENDENCIES_ON_PACKAGE);
        }
        return SUCCESS;
    }


    /**
     * Remove a package and it's applets from the card memory
     * @param index in package table where the package is
     * @return status SUCCESS or FAILURE
    */
    public static byte removePackageAndApplets(byte index, byte[] buffer) throws ISOException{
        byte result = 0;
        //check dependencies on the package to be deleted
        if(checkDependencies(index) != NO_DEPENDENCIES){
            ISOException.throwIt(Errors.DEPENDENCIES_ON_PACKAGE);
        }
        result = GarbageCollector.deletePackageAndApplets(index, buffer);
        return result;
    }

    /**
     * Returns the package context for the pkgId provided
     * @param package Id
     * @return package context
     */
    public static byte getPkgContext(byte pkgId){
        for(byte i = 0; i < ON_CARD_APPLET_PKG_MAX; i++){
            if(packageContextTable[i] == pkgId){
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the package Id for a given context
     * @param package context
     * @return package Id
     */
    public static byte getPkgIdForContext(byte pkgContext){
        return packageContextTable[pkgContext];
    }


    /**
     * This method tries to find the required applet in the package table, and once it's found
     * it's install method address or the package id of the package to which this applet belongs
     * is returned. Return type depends on the parameter requiredInfoType
     * @param bArray containing the applet's AID bytes
     * @param offset in bArray from where the applet's AID starts
     * @param length of applet's AID
     * @param requiredINfoType based on which the return value is decided
     * @return either package ID of the package to which this package belongs or applet's install
     * method address depending on the requiredInfoType parameter
     */
    public static short getAppletInfo(byte[] bArray, short offset, byte length, byte requiredInfoType){
        short appletInfo = -1;
        byte pkgID = 0;

        /**
         * First check if a package was being installed and the call to create applet was made
         * without really committing the package to the package table. If that is the case
         * we have to check applet info in the new package entry first.
         */
        if(g_packageInProcess > 0 && g_newPackage != null){
            pkgID = g_packageInProcess;
            appletInfo = g_newPackage.getAppletInstallMethodAddress(bArray, offset, length);
        }
        if(appletInfo == -1){
            for(byte i=(byte)f_firstEEPkgID; i<(byte)ON_CARD_PKG_MAX; i++) {
                /*
                Just in case even the package table has not been initialized as yet
                no need to look in EEPROM, let the native method handle this call.
                */
                if(f_pkgTable == null) break;
                if(f_pkgTable[i] !=null){
                    appletInfo = f_pkgTable[i].getAppletInstallMethodAddress(bArray, offset, length);
                }
                if(appletInfo != -1){
                    pkgID = i;
                    break;
                }
            }
        }
        if(appletInfo != -1){
            if(requiredInfoType == PACKAGE_ID){
                return pkgID;
            }else{
                return appletInfo;
            }
        }
        /**
         * If the required applet is still not found, try to find it in ROM
         */
        return NativeMethods.getAppletInfo(bArray, offset, length, requiredInfoType);
    }
}
