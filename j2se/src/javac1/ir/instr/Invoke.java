/*
 * @(#)Invoke.java                      1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir.instr;

import java.util.List;
import javac1.Assert;
import javac1.ir.InstructionVisitor;
import javac1.ir.IRScope;
import javac1.ir.ValueClosure;
import javac1.ir.types.ValueType;

/**
 * The instruction node for the invocation of a method.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Invoke extends StateSplit {
    /**
     * The invocation instruction code.
     */
    private int code;

    /**
     * The object receiving the method call.
     */
    private Instruction recv;

    /**
     * The values that are passed as parameters to the method.
     */
    private List args;

    /**
     * Whether or not the called method is final.
     */
    private boolean targetFinal;

    /**
     * Whether or not the called method is loaded.
     */
    private boolean targetLoaded;

    /**
     * Constructs a new instruction node for a method invocation.
     *
     * @param  scope         scope containing this instruction
     * @param  code          the invocation instruction code
     * @param  resultType    type of the return value
     * @param  recv          the receiver
     * @param  args          the list of arguments
     * @param  targetFinal   whether or not the target method is final
     * @param  targetLoaded  whether or not the target method is loaded
     */
    public Invoke(IRScope scope, int code, ValueType resultType, Instruction recv,
            List args, boolean targetFinal, boolean targetLoaded) {
        super(scope, resultType);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(args != null, "argument list must exist");
        }
        this.code = code;
        this.recv = recv;
        this.args = args;
        this.targetFinal = targetFinal;
        this.targetLoaded = targetLoaded;
    }

    /**
     * Returns the invocation instruction code.
     *
     * @return  the instruction code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the object receiving the method call.
     *
     * @return  the receiver
     */
    public Instruction getReceiver() {
        return recv;
    }

    /**
     * Tests whether this method invocation has a receiver or the target method
     * is static.
     *
     * @return  whether or not the invocation has a receiver
     */
    public boolean hasReceiver() {
        return recv != null;
    }

    /**
     * Returns the argument that is passed to the method at the specified index.
     *
     * @param   index  index into the argument list
     * @return  the argument at the specified index
     */
    public Instruction argumentAt(int index) {
        return (Instruction) args.get(index);
    }

    /**
     * Returns the number of arguments of the method.
     *
     * @return  number of arguments
     */
    public int countArguments() {
        return args.size();
    }

    /**
     * Returns the total size of the arguments of the method.
     *
     * @return  size of the arguments
     */
    public int getArgSize() {
        int size = hasReceiver() ? 1 : 0;
        for (int i = args.size() - 1; i >= 0; i--) {
            size += argumentAt(i).getType().getSize();
        }
        return size;
    }

    /**
     * Returns whether or not the called method is final.
     *
     * @return  whether or not the method is final
     */
    public boolean isTargetFinal() {
        return targetFinal;
    }

    /**
     * Returns whether or not the called method is loaded.
     *
     * @return  whether or not the method is loaded
     */
    public boolean isTargetLoaded() {
        return targetLoaded;
    }

    public boolean canTrap() {
        return true;
    }

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        if (hasReceiver()) {
            recv = vc.doValue(recv);
        }
        for (int i = 0; i < args.size(); i++) {
            args.set(i, vc.doValue((Instruction) args.get(i)));
        }
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doInvoke(this);
    }
}
