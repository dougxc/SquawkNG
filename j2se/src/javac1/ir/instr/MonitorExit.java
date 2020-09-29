/*
 * @(#)MonitorExit.java                 1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;

/**
 * The instruction node for exiting the monitor associated with an object. The
 * <code>MonitorEnter</code> is used together with <code>MonitorExit</code> to
 * provide synchronization.
 *
 * @see      MonitorEnter
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class MonitorExit extends AccessMonitor {
    /**
     * Constructs a new instruction node for exiting the monitor.
     *
     * @param  scope      scope containing this instruction
     * @param  obj        the object
     * @param  monitorNo  number of the monitor
     */
    public MonitorExit(IRScope scope, Instruction obj, int monitorNo) {
        super(scope, obj, monitorNo);
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doMonitorExit(this);
    }
}
