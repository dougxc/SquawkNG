
package com.sun.squawk.translator.ir;
import com.sun.squawk.translator.*;

/**
 * This is the base class for the LoadField and StoreField instructions.
 */
public abstract class FieldInstruction extends Trappable {

    /** The field referred to by this instruction. */
    private Field field;
    /** The reference for a non-static field. */
    private Instruction ref;
    /** For static fields, this flag indicates if the field belongs to the class of the enclosing method.
        For instance fields, this flag indicates if the field belongs to the receiver of enclosing virtual method. */
    private boolean isFieldOfReceiver;

    FieldInstruction(Type type, Field field, Instruction ref, boolean isFieldOfReceiver) {
        super(type);
        this.field = field;
        this.ref   = ref;
        this.isFieldOfReceiver = isFieldOfReceiver;
    }

    public Field field()       { return field; }
    public Instruction ref()   { return ref; }
    public void removeRef()    { ref = null; }
    public boolean isFieldOfReceiver() { return isFieldOfReceiver; }

    public boolean constrainsStack() {
        return field.isStatic();
    }

    public Type getReferencedType() {
        if (field.isStatic() && !isFieldOfReceiver) {
            return field.parent();
        }
        return null;
    }

    public void visit(ParameterVisitor visitor) {
        if (ref != null) {
            ref = visitor.doParameter(this, ref);
        }
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    protected String toString(String prefix) {
        return prefix + " [" +field+"]";
    }
}
