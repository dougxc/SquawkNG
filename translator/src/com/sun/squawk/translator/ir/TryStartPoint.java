
package com.sun.squawk.translator.ir;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.*;

/**
 * This class represents the start a range of code protected
 * by a single exception handler. That is, where the opening '{'
 * of a "try" block would be in the source code.
 */
public final class TryStartPoint extends TryPoint {

    /**
     * Constructor.
     * @param ip The address of the start of the protected range.
     * @param handler The exception handler entry point.
     * @param handlerSlot The index of the exception handler in the handler table.
     */
    public TryStartPoint(int ip, Target handler, int handlerSlot) {
        super(ip, handler, handlerSlot);
    }

    /**
     * Return the secondary key used for sorting when the primary key for two elements matches.
     * This sorts try start points *inversely* by the index of their corresponding handler in the handler table.
     * The reason for the inversion is so handlers earlier in the table are nested deeper than
     * tjose later in the table.
     */
    public int secondaryKey() {
        // The maximum index of a handler table index is 0xFFFF (i.e. an unsigned short).
        return 0xFFFF - handlerTableIndex;
    }

    /**
     * Return the pseudo-opcode representing this point in an instruction stream.
     */
    public int opcode() {
        return  JVMConst.opc_handlerstart;
    }
}
