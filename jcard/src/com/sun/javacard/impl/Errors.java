/*
 * Copyright © 2002 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// /*
// Workfile:@(#)Errors.java	1.9
// Version:1.9
// Date:03/20/02
//
// Archive:  com/sun/javacard/impl/ErrorsInfo.java
// Modified:03/20/02 16:38:57
// Original author:  Saqib
// */

package com.sun.javacard.impl;

/**
 * This interface collects Error constants
 */
public interface Errors {
    // error codes

    /**
     * Response status : Invalid CAP file magic number = 0x6402
     */
    static final short CAP_MAGIC = 0x6402;

    /**
     * Response status : Invalid CAP file minor number = 0x6403
     */
    static final short CAP_MINOR = 0x6403;

    /**
     * Response status : Invalid CAP file major number = 0x6404
     */
    static final short CAP_MAJOR = 0x6404;

    /**
     * Response status : Integer not supported = 0x640b
     */
    static final short INTEGER_UNSUPPORTED = 0x640b;

    /**
     * Response status : Duplicate package AID found = 0x640c
     */
    static final short DUP_PKG_AID = 0x640c;

    /**
     * Response status : Duplicate Applet AID found = 0x640d
     */
    static final short DUP_APPLET_AID = 0x640d;

    /**
     * Response status : Installation aborted = 0x640f
     */
    static final short ABORTED = 0x640f;

    /**
     * Response status : Installer in error state = 0x6421
     */
    static final short ERROR_STATE = 0x6421;

    /**
     * Response status : CAP file component out of order = 0x6422
     */
    static final short COMP_ORDER = 0x6422;

    /**
     * Response status : Exception occurred = 0x6424
     */
    static final short EXCEPTION = 0x6424;

    /**
     * Response status : Install APDU command out of order = 0x6425
     */
    static final short COMMAND_ORDER = 0x6425;

    /**
     * Response status : Invalid component tag number = 0x6428
     */
    static final short COMP_TAG = 0x6428;

    /**
     * Response status : Invalid install instruction = 0x6436
     */
    static final short INSTRUCTION = 0x6436;

    /**
     * Response status : On-card package max exceeded
     */
    static final short ON_CARD_PKG_MAX_EXCEEDED = 0x6437;

    /**
     * Response status : Import package not found = 0x6438
     */
    static final short IMPORT_NOT_FOUND = 0x6438;

    /**
     * Response status : Illegal package identifier = 0x6439
     */
    static final short PKG_ID = 0x6439;

    /**
     * Response status: On-card applet package max exceeded = 0x643a
     */
    static final short ON_CARD_APPLET_PKG_MAX_EXCEEDED = 0x643a;

    /**
     * Response status : Maximum allowable package methods exceeded  = 0x6442
     */
    static final short PKG_METHOD_MAX_EXCEEDED = 0x6442;

    /**
     * Response status : Applet not found = 0x6443
     */
    static final short APPLET_NOT_FOUND = 0x6443;

    /**
     * Response status : Applet creation failed = 0x6444
     */
    static final short APPLET_CREATION = 0x6444;

    /**
     * Response status : Maximum allowable instances exceeded  = 0x6445
     */
    static final short INSTANCE_MAX_EXCEEDED = 0x6445;

    /**
     * Response status : Memory allocation failed = 0x6446
     */
    static final short ALLOCATE_FAILURE = 0x6446;

    /**
     * Response status : Import class not found = 0x6447
     */
    static final short IMPORT_CLASS_NOT_FOUND = 0x6447;


    //Error codes for applet and package deletion
    /**
     * Response status: Dependenices on applet = 0x6448
     */
    static final short DEPENDENCIES_ON_APPLET = 0x6448;

    /**
     * Response status: internal memory constraints = 0x6449
     */
    static final short MEMORY_CONSTRAINTS = 0x6449;

    /**
     * Response status: Package not found = 0x644b
     */
    static final short PACKAGE_NOT_FOUND = 0x644b;

    /**
     * Response status: Dependencies on package = 0x644c
     */
    static final short DEPENDENCIES_ON_PACKAGE = 0x644c;

    /**
     * Response status: Applet's present = 0x644d
     */
    static final short APPLETS_PRESENT = 0x644d;

    /**
     * Response status: package is rom package = 0x644e
     */
    static final short PACKAGE_IS_ROM_PACKAGE = 0x644e;

    /**
     * Response status: Package name exceeds name length = 0x644f
     */
    static final short PACKAGE_NAME_LENGTH_EXCEEDED = 0x644f;

    /**
     * Response status: Cannot delete active applet = 0x6451
     */
    static final short APPLET_ACTIVE = 0x6451;

    /**
     * Response status: Another Applet active from the same context = 0x6452
     */
    static final short ACTIVE_APPLET_FROM_SAME_PACKAGE = 0x6452;
}
