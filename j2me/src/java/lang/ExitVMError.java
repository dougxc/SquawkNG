/*
 * Copyright 1995-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

/**
 * ExitVMException
 */
public class ExitVMError extends VirtualMachineError {
    /**
     * Constructs a <code>RuntimeException</code> with no detail  message.
     */
    public ExitVMError() {
        super();
    }

    /**
     * Constructs a <code>RuntimeException</code> with the specified
     * detail message.
     *
     * @param   s   the detail message.
     */
    public ExitVMError(String s) {
        super(s);
    }
}


