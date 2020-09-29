/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)GarbageCollector.java	1.16
// Version:1.16
// Date:04/12/02
//
// Modified:04/12/02 16:38:57
// Original author:  Saqib Ahmad
// */

package com.sun.javacard.impl;

import javacard.framework.SystemException;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;

/**
 * This class is responsible for driving garbage collection, applet deletion and package deletion.
 *
 */
public class GarbageCollector
{
    /**
     * Flag that shows if GC is requested
     */
    public static byte GCRequested = (byte)-1;
    /**
     * Flag that shows that package deletion is in progress
     */
    public static boolean deletingPackage = false;

    /**
     * In case package deletion is in progress, this variable contains
     * the index of the package being deleted.
     */
    public static byte packageBeingDeleted = PackageMgr.ILLEGAL_ID;
    
    /**
     * RMIs exported objects array
     */
    public static short[] RMIExpObjArray = null;
    
    /**
     * Set the exported objects array for RMI. This need to be cleaned up after every
     * successful GC operation.
     * @param exported objects array which contains object IDs
     */
     public static void setExpObjArray(short[] expArray){
        RMIExpObjArray = expArray;
     }

    /**
     * Starting point for garbage collection procedure.
     * @return the status for gc
     */
    public static boolean startGC(){
        boolean doCommit = false;
        boolean gcRequested = PrivAccess.getPrivAccess().isGarbageCollectionRequested();
        PrivAccess.getPrivAccess().setGCRequestedFlag(false);
        try{
            /*
            This method takes care of starting garbage collection.
            It first calls the native method initializeGC().
            */
            if(!NativeMethods.initializeGC()){
                //if no exception was thrown, but initialization failed, this method
                //was called during applet deletion and should return false because
                //the applet had dependencies on it because only in that case is
                //false returned from intialize GC
                return false;
            }
        }catch(SystemException e){
            //check if this method was called as a result of requested object deletion.
            //if this was the case, we cannot do anything with the exception since there
            //is no entity that would catch it and deal with it which is why we suppress
            //it right here and just return false. Otherwise, we throw an exception
            if(!gcRequested){
                /*there was not enough memory available to complete operation*/
                ISOException.throwIt(Errors.MEMORY_CONSTRAINTS);
            }else{
                return false;
            }
        }

        /*
        Then call the native method to mark the applet table as the root of roots :)
        passing it the address to the applet table. What garbage collector does with
        it is upto it
        */
        NativeMethods.markAppletRoots(AppletMgr.theAppTable);

        //Do the following step only if the package table has been initialized.
        if(PackageMgr.f_pkgTable != null){
            /*
            For all the packages in the memory, call the native method markStaticRoots()
            to mark the objects pointed to by statics as roots
            */
            for(byte i = PackageMgr.f_firstEEPkgID ; i < PackageMgr.ON_CARD_PKG_MAX ; i++){
                if(PackageMgr.f_pkgTable[i] != null){
                    if(deletingPackage && packageBeingDeleted == i)continue;
                    short refCount = PackageMgr.f_pkgTable[i].pkgStaticReferenceCount;
                    short staticCompAddr = PackageMgr.f_pkgTable[i].compInfo[PackageMgr.STATIC_FIELD_COMPONENT_INDEX].address;
                    if(NativeMethods.markStaticRoots(staticCompAddr, refCount) == 0){
                        //error
                        return false;
                    }
                }
            }
        }

        /*
        Now that the roots have been marked call doGC() and let it do it's work
        */
        if (JCSystem.getTransactionDepth() == 0) {
            JCSystem.beginTransaction();
            doCommit = true;
        }
        
        if(NativeMethods.doGC()==0){
            //error
		        if(doCommit){
			          JCSystem.abortTransaction();
		        }
            return false;
        }
        cleanupExportedObjectsArray();
        
		    if(doCommit){
			      JCSystem.commitTransaction();
		    }
        return true;
    }
    
    /**
     * This method calls the corresponding native method which goes through the 
     * complete exported objects arrays and removes any entries from it that do 
     * not point to objects existing on the card.
     */
    private static void cleanupExportedObjectsArray(){
        if(RMIExpObjArray != null){
            NativeMethods.cleanupExportedObjectsArray(RMIExpObjArray);
        }
    }

    /**
    * Used to delete one or more than one applet instances from EEPROM
    * @param contexts array is the one that contains the ids or applet contexts of all
	  * the applets being deleted.
	  * @param count is the count of applets being deleted.
    * @return status
    */
    public static boolean deleteApplets(byte[] contexts, byte count) throws ISOException{
        /*
        pass the context information about the applets to be deleted to garbage
        collector
        */
        if(NativeMethods.initializeAppletDeletion(contexts, count) == 0){
            ISOException.throwIt(Errors.APPLET_ACTIVE);
        }

        /*
        Remove the applet(s) from the applet table
        */
        for(byte i = 0; i < count; i++){
            AppletMgr.removeApplet((byte)(contexts[i] & 0x0F));
        }

        /*
        rest of the stuff is same as garbage collection, so just call that method again.
        */
        try{
            if(!startGC()){
                //error
                ISOException.throwIt(Errors.DEPENDENCIES_ON_APPLET);
            }
        }catch(javacard.framework.TransactionException e){
            /*More garbage out there than the transaction buffer can handle.*/
            ISOException.throwIt(Errors.MEMORY_CONSTRAINTS);
        }
        return true;
    }

    /**
     * This method initializes the package deletion so that while doing
     * applet deletion (in case of applet and package deletion) we don't mark
     * the objects being pointed to by the static fields of the package being
     * deleted.
     * @param index of the package being deleted in the package table.
    */
    public static void initPackageDeletion(byte index){
        deletingPackage = true;
        packageBeingDeleted = index;
    }

    /**
     * delete a package
     * @param index is the is the index of the package in the package table.
     * @return status
     */
    public static byte deletePackage(byte index)  throws ISOException{
        //remove all the components of the package from the memory
        ComponentInfo cinf;
        //check that entry in the package table at the given index is not null
        //if the entry is null the package has nothing. Just return.
        if(PackageMgr.f_pkgTable[index] == null) {
            return (byte)1;
        }
        //remove all the components of the package from the memory
        for(byte i = 0; i < PackageMgr.COMPONENT_COUNT; i++){
            cinf = PackageMgr.f_pkgTable[index].compInfo[i];
            if(cinf != null)
                NativeMethods.freeHeap(cinf.address, cinf.size);
        }
         /**
         * If the package is an applet package, we need to remove it from the contexts
         * table as well to make room for new applet packages.
         */
        if(PackageMgr.f_pkgTable[index].appletCount > 0){
           PackageMgr.packageContextTable[PackageMgr.getPkgContext(index)] = PackageMgr.ILLEGAL_ID;
           PackageMgr.appletPkgCount--;
        }
        //remove the exception table entry corresponding to this package. 
        //the native method just returns if the entry does not exist.
        NativeMethods.removeExcTableEntry(index);
        PackageMgr.f_pkgTable[index] = null;
        packageBeingDeleted = PackageMgr.ILLEGAL_ID;
        deletingPackage = false;
        startGC();
        return (byte)1;
    }

    /**
     * delete a package along with it's applets
     * @param index is the is the index of the package in the package table.
	 * @param buffer will be used to store the IDs of the applets belonging to the
	 * package being deleted.
     * @return status
     */
    public static byte deletePackageAndApplets(byte index, byte[] buffer) throws ISOException{
        byte appCount;
        ComponentInfo cinf;

        initPackageDeletion(index);
        appCount = AppletMgr.getAppletsForPackage(buffer, (byte)0, index);
        if(appCount!= 0){
            //delete all the applets first. This may throw exceptions if applets cannot be deleted
            //because of dependencies or memory constraints. If an exception is thrown, we do not need
            //to catch it here. Since this method throws the exception back to the calling method
            //automatically, that will be caught by the calling method. And since an exception will result
            //in aborted transaction, all the variables that were set for package deletion will be reset
            //automatically.
            deleteApplets(buffer, appCount);
        }
        deletePackage(index);
        return (byte)1;
    }
}
