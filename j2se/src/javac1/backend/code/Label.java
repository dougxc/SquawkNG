/*
 * @(#)Label.java                       1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.code;

import javac1.Assert;

/**
 * Represents known or yet unknown target destinations for jumps and calls.
 *
 * @see      Assembler#bind(Label)
 * @see      Assembler#jcc(int, Label)
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Label {
    /**
     * Encodes both the binding state and the binding position of this label.
     */
    private int pos;

    /**
     * Constructs a new unused label.
     */
    public Label() {
        pos = 0;
    }

    /**
     * Returns the target position or the last displacement in the chain. The
     * meaning of the actual result depends on whether the label is bound or
     * unbound.
     *
     * @return  target position or last displacement
     */
     public int getPos() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(pos != 0, "label is unused");
        }
        if (pos < 0) {
            return -pos - 1;
        } else {
            return pos - 1;
        }
    }

    /**
     * Binds this label to the specified code position. The position is stored
     * in this label for future backward jumps.
     *
     * @param  pos  target code position
     */
    public void bindTo(int pos) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(pos >= 0, "illegal position");
        }
        this.pos = -pos - 1;
    }

    /**
     * Links this label to the specified displacement. This method is used to
     * create a chain of forward jumps that will be fixed up as soon as the
     * target position is known.
     *
     * @param  pos  next displacement in the chain
     */
    public void linkTo(int pos) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(pos >= 0, "illegal position");
        }
        this.pos = pos + 1;
    }

    /**
     * Clears this label so that it is unused again.
     */
    public void clear() {
        pos = 0;
    }

    /**
     * Returns whether or not this label is bound.
     *
     * @return  whether or not this label is bound
     */
    public boolean isBound() {
        return pos < 0;
    }

    /**
     * Returns whether or not this label is unbound.
     *
     * @return  whether or not this label is unbound
     */
    public boolean isUnbound() {
        return pos > 0;
    }

    /**
     * Returns whether or not this label is unused.
     *
     * @return  whether or not this label is unused
     */
    public boolean isUnused() {
        return pos == 0;
    }
}
