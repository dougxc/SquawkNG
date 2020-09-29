/*
 * @(#)Return.java                      1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.types.ValueType;

/**
 * The instruction node for returning control from a method to its invoker.
 *
 * @see      Invoke
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Return extends BlockEnd {
    /**
     * The value that is passed back to the invoker.
     */
    private Instruction result;

    /**
     * The number of the monitor for synchronized methods.
     */
    private int monitorNo;

    /**
     * Constructs a new instruction node for returning from a method.
     *
     * @param  scope      scope containing this instruction
     * @param  result     the return value if any
     * @param  monitorNo  monitor number or -1 if the method is not synchronized
     */
    public Return(IRScope scope, Instruction result, int monitorNo) {
        super(scope, (result == null) ? ValueType.voidType : result.getType());
        this.result = result;
        this.monitorNo = monitorNo;
    }

    /**
     * Returns the value that is passed back to the invoker.
     *
     * @return  the return value if any
     */
    public Instruction getResult() {
        return result;
    }

    /**
     * Tests if the method has a return value or has been declared void.
     *
     * @return  whether or not the method has a return value
     */
    public boolean hasResult() {
        return result != null;
    }

    /**
     * Returns the number of the monitor for synchronized methods.
     *
     * @return  number of the monitor
     */
    public int getMonitorNo() {
        return monitorNo;
    }

    /**
     * Tests if the method to return from is synchronized.
     *
     * @return  whether or not the method is synchronized
     */
    public boolean isSynchronized() {
        return monitorNo >= 0;
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        if (hasResult()) {
            result = vc.doValue(result);
        }
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doReturn(this);
    }
}
