
package com.sun.squawk.translator.ir;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.*;

/**
 * This class represents the address just past the last instruction in a range of code protected
 * by a single exception handler. That is, just before the first "catch"
 * block of a "try" statement in the source code.
 */
public final class TryEndPoint extends TryPoint {

    /**
     * Constructor.
     * @param ip The address of the start of the protected range.
     * @param handler The exception handler entry point.
     * @param handlerTableIndex The index of the exception handler in the handler table.
     */
    public TryEndPoint(int ip, Target handler, int handlerTableIndex) {
        super(ip, handler, handlerTableIndex);
    }


    /**
     * Return the secondary key used for sorting when the primary key for two elements matches.
     * This sorts try end points by the index of their corresponding handler in the handler table.
     */
    public int secondaryKey() {
        return handlerTableIndex;
    }

    /**
     * Return the pseudo-opcode representing this point in an instruction stream.
     */
    public int opcode() {
        return JVMConst.opc_handlerend;
    }
}
