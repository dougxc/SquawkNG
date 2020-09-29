package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.vm.*;

/**
 * This IR instruction represents all the INVOKE* Java bytecodes.
 */
public final class Invoke extends Trappable {

    /**
     * This is an enumerated type representing the different forms of invoke instructions.
     */
    public static class Form {
        private Form(String name, int opcode) {
            this.name   = name;
            this.opcode = opcode;
        }
        public String toString() {
            return name;
        }

        private final String name;
        private final int opcode;

        public int opcode() { return opcode; }

        // The enumerated values.
        /** Denotes a virtual method invocation. */
        public static final Form VIRTUAL   = new Form("virtual", OPC.INVOKEVIRTUAL);
        /** Denotes a virtual method invocation where the lookup starts in the receiver's superclass's vtable. */
        public static final Form SUPER     = new Form("super", OPC.INVOKESUPER);
        /** Denotes a static method invocation. */
        public static final Form STATIC    = new Form("static", OPC.INVOKESTATIC);
        /** Denotes an interface method invocation. */
        public static final Form INTERFACE = new Form("interface", OPC.INVOKEINTERFACE);
        /** Denotes a constructor invocation. */
        public static final Form INIT      = new Form("init", OPC.INVOKEINIT);
    }

    /** The method invoked. */
    private final Method method;
    /** The list of instructions that pushed the parameters for the invoke.*/
    private final Instruction[] parms;
    /** Specifies the form of the invocation. */
    private final Form form;

    Invoke(Method method, Instruction[] parms, Form form) {
        super(form == Form.INIT ? parms[0].type() : method.type());
        Assert.that(method != null);
        this.method     = method;
        this.parms      = parms;
        this.form       = form;
    }



    public Method method()          { return method; }
    public int    offset()          { return method.slot(); }
    public Instruction[] parms()    { return parms;  }
    public Form form()              { return form;   }

    public boolean constrainsStack() {
        return true;
    }

    public Type getReferencedType() {
        if (form == Form.VIRTUAL) {
            return null;
        }
        return method.parent();
    }

    /**
     * Entry point for an InstructionVisitor.
     * @param visitor The InstructionVisitor.
     */
    public void visit(InstructionVisitor visitor) throws LinkageException {
        visitor.doInvoke(this);
    }

    public void visit(ParameterVisitor visitor) {
        for (int i = 0 ; i < parms.length ; i++) {
            parms[i] = visitor.doParameter(this, parms[i]);
        }
    }

    /**
     * Return a String representation of this instruction.
     * @return a String representation of this instruction.
     */
    public String toString() {
        return Mnemonics.OPCODES[form.opcode()] + " [" +method+"]";
    }
}
