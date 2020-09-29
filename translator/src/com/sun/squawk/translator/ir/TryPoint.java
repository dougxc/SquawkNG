package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.*;

/**
 * This class represents the start or end of a range of code protected
 * by a single exception handler. That is, where the opening '{' or
 * closing '}' of a "try" block would be in the source code.
 */
public abstract class TryPoint extends BytecodeAddress {

    /**
     * The entry point of the exception handler corresponding to this try point.
     */
    private final Target handler;

    /**
     * The index of the handler in the handler table. This is used for nesting
     * try points correctly.
     */
    protected final int handlerTableIndex;

    /**
     * Constructor.
     * @param ip The address of the start/end of the protected range.
     * @param handler The exception handler entry point.
     * @param handlerTableIndex The index of the exception handler in the handler table.
     */
    public TryPoint(int ip, Target handler, int handlerTableIndex) {
        super(ip);
        this.handler           = handler;
        this.handlerTableIndex = handlerTableIndex;
    }

    /**
     * Return the handler.
     */
    public Target handler() {
        return handler;
    }
}
