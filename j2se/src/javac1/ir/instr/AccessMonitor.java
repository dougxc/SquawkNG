/*
 * @(#)AccessMonitor.java               1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.types.ValueType;

/**
 * The abstract base class for instructions accessing the monitor associated
 * with an object. The concrete subclasses are used for synchronization.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public abstract class AccessMonitor extends StateSplit {
    /**
     * The object that the monitor is associated with.
     */
    private Instruction obj;

    /**
     * The number of the monitor to be accessed.
     */
    private int monitorNo;

    /**
     * Initializes the attributes declared in this class.
     *
     * @param  scope      scope containing this instruction
     * @param  obj        the object
     * @param  monitorNo  number of the monitor
     */
    protected AccessMonitor(IRScope scope, Instruction obj, int monitorNo) {
        super(scope, ValueType.illegalType);
        this.obj = obj;
        this.monitorNo = monitorNo;
    }

    /**
     * Returns the object that the monitor is associated with.
     *
     * @return  the object
     */
    public Instruction getObj() {
        return obj;
    }

    /**
     * Returns the number of the monitor to be accessed.
     *
     * @return  number of the monitor
     */
    public int getMonitorNo() {
        return monitorNo;
    }

    public boolean canTrap() {
        return true;
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        obj = vc.doValue(obj);
    }
}
