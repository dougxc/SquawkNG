
package com.sun.squawk.translator.ir;
import com.sun.squawk.translator.*;

public abstract class LocalVariableInstruction extends Instruction {

    private Local local;

    LocalVariableInstruction(Local local, Type type) {
        super(type);
        Assert.that(local != null);
        updateLocal(local, true);
    }

    /**
     * Return true if this instruction defines (i.e. writes to) its local variable.
     */
    public abstract boolean isDefinition();

    /**
     * Return true if this instruction uses (i.e. loads from) its local variable.
     */
    public abstract boolean isUse();

//    public Type type()         { return local.type(); }
    final public Local local() { return local; }

    public abstract void updateLocal(Local local, boolean updateUseDef);

    final protected void updateLocal(Local local) {
        this.local = local;
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    protected String toString(String opcode, Type type) {
        int index = local.squawkIndex();
        if (index == -1) {
            index = local.javacIndex();
        }
        return opcode + " " + local;
    }

}
