/*
 * @(#)Intrinsic.java                   1.10 02/11/27
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
 * The instruction node for instrinsic method invocations. During method
 * inlining the invocation of special methods may be replaced by instructions
 * of this kind, so that the back end can generate very efficient code for the
 * desired operation.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Intrinsic extends StateSplit {
    /**
     * The constant used to state that a method is not an instrinsic one.
     */
    public static final int NONE = -1;

    /**
     * The identification number for the method <code>Object.hashCode</code>.
     */
    public static final int HASH_CODE = 0;

    /**
     * The identification number for the method <code>Math.sin</code>.
     */
    public static final int DSIN = 1;

    /**
     * The identification number for the method <code>Math.cos</code>.
     */
    public static final int DCOS = 2;

    /**
     * The identification number for the method <code>Math.sqrt</code>.
     */
    public static final int DSQRT = 3;

    /**
     * The identification number for the method <code>System.arraycopy</code>.
     */
    public static final int ARRAYCOPY = 4;

    /**
     * The identification number for the method <code>String.compareTo</code>.
     */
    public static final int COMPARE_TO = 5;

     /**
      * The identification number of the intrinsic method to be called.
      */
     private int intrinsicId;

    /**
     * The values that are passed as parameters to the method.
     */
     private List args;

    /**
     * Constructs a new instruction node for an instrinsic method invocation.
     *
     * @param  scope        scope containing this instruction
     * @param  resultType   type of the return value
     * @param  intrinsicId  the intrinsic identification number
     * @param  args         the list of arguments
     */
     public Intrinsic(IRScope scope, ValueType resultType, int intrinsicId,
            List args) {
        super(scope, resultType);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(args != null, "argument list must exist");
        }
        this.intrinsicId = intrinsicId;
        this.args = args;
     }

     /**
      * Returns the identification number of the intrinsic method to be called.
      *
      * @return  the intrinsic identification number
      */
     public int getIntrinsicId() {
        return intrinsicId;
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

    public void doInputValues(ValueClosure vc) {
        super.doInputValues(vc);
        for (int i = 0; i < args.size(); i++) {
            args.set(i, vc.doValue((Instruction) args.get(i)));
        }
    }

    public void visit(InstructionVisitor visitor) {
        visitor.doIntrinsic(this);
    }
}
