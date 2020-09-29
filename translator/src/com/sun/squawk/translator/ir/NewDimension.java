package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

/**
 * This instruction implements the latter half of the standard Java
 * MULTIANEWARRAY bytecode.
 */
public class NewDimension extends Trappable {

    /** The size of the dimension this instruction adds to an array. */
    private Instruction dimension;
    /** The array ref that has another dimension intialized. */
    private Instruction array;

    NewDimension(Instruction array, Instruction dimension) {
        super(array.type());
        this.array     = array;
        this.dimension = dimension;
    }

    /** Return the array. */
    public Instruction array() { return array;   }
    /** Return the dimension. */
    public Instruction dimension() { return dimension;   }

    public boolean constrainsStack() {
        return true;
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doNewDimension(this);
    }

    public void visit(ParameterVisitor visitor) {
        array = visitor.doParameter(this, array);
        dimension = visitor.doParameter(this, dimension);
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return Mnemonics.OPCODES[OPC.NEWDIMENSION];
    }
}
