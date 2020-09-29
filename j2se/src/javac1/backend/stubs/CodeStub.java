/*
 * @(#)CodeStub.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.stubs;

import javac1.Assert;
import javac1.backend.code.CodeEmitter;
import javac1.backend.code.Label;

/**
 * The abstract base class for all code stubs.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public abstract class CodeStub {
    /**
     * The label at the stub entry point.
     */
    private Label entry;

    /**
     * The label where the stub continues.
     */
    private Label continuation;

    /**
     * Initializes the attributes declared in this class.
     */
    protected CodeStub() {
        entry = new Label();
        continuation = new Label();
    }

    /**
     * Returns the label at the stub entry point.
     *
     * @return  label at the entry point
     */
    public Label getEntry() {
        return entry;
    }

    /**
     * Returns the label where the stub continues.
     *
     * @return  label where the stub continues
     */
    public Label getContinuation() {
        return continuation;
    }

    /**
     * Asserts that none of the labels is unbound.
     */
    public void assertNoUnboundLabels() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!entry.isUnbound() && !continuation.isUnbound(), "unbound label");
        }
    }

    /**
     * Emits the machine instructions for this code stub. During code generation
     * all code stubs are collected in a list at first. At the end, this list is
     * traversed and the machine instructions are emitted following the regular
     * code. This approach improves instruction cache usage for the common case.
     *
     * @param  ce  reference to the current code emitter
     */
    public abstract void emitCode(CodeEmitter ce);
}
