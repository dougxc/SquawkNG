/*
 * @(#)Displacement.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.code;

import javac1.Assert;

/**
 * Chains a list of instructions using the same label. Sometimes the back end
 * has to emit instructions that refer to addresses in the code buffer before
 * they are actually known. In these cases the assembler temporarily emits a
 * displacement that is used together with a label in order to refer to the yet
 * unknown code position. It stores the instruction type and instruction
 * specific information as well as the position of the next displacement in the
 * chain. These values later allow the assembler to iterate over the incomplete
 * instructions and replace the displacements by the correct address.
 *
 * @see      Label
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Displacement {
    /**
     * The type constant for procedure calls.
     */
    public static final int CALL = 0;

    /**
     * The type constant for absolute jumps.
     */
    public static final int ABSOLUTE_JUMP = 1;

    /**
     * The type constant for conditional jumps.
     */
    public static final int CONDITIONAL_JUMP = 2;

    /**
     * The number of bits reserved for instruction specific information.
     */
    private static final int INFO_SIZE = 8;

    /**
     * The number of bits that specify the instruction type.
     */
    private static final int TYPE_SIZE = 2;

    /**
     * The number of bits that store the position of the next displacement.
     */
    private static final int NEXT_SIZE = 32 - (TYPE_SIZE + INFO_SIZE);

    /**
     * The index of the first bit of instruction specific information.
     */
    private static final int INFO_POS = 0;

    /**
     * The index of the first bit of the instruction type.
     */
    private static final int TYPE_POS = INFO_POS + INFO_SIZE;

    /**
     * The index of the first bit of the reference to the next displacement.
     */
    private static final int NEXT_POS = TYPE_POS + TYPE_SIZE;

    /**
     * The bit mask that selects any instruction specific information.
     */
    private static final int INFO_MASK = (1 << INFO_SIZE) - 1;

    /**
     * The bit mask that selects the instruction type.
     */
    private static final int TYPE_MASK = (1 << TYPE_SIZE) - 1;

    /**
     * The bit mask that selects the position of the next displacement.
     */
    private static final int NEXT_MASK = (1 << NEXT_SIZE) - 1;

    /**
     * The bit field that encodes all of the displacement data.
     */
    private int data;

    /**
     * Constructs a new displacement for the specified label.
     *
     * @param  label  the yet unknown code position
     * @param  type   the instruction type
     * @param  info   instruction specific information
     */
    public Displacement(Label label, int type, int info) {
        init(label, type, info);
    }

    /**
     * Constructs a new displacement from the specified bit field.
     *
     * @param  data  the displacement data
     */
    public Displacement(int data) {
        this.data = data;
    }

    /**
     * Initializes this displacement according to the specified arguments.
     *
     * @param  label  the yet unknown code position
     * @param  type   the instruction type
     * @param  info   instruction specific information
     */
    private void init(Label label, int type, int info) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!label.isBound(), "label is bound");
        }
        int next = 0;
        if (label.isUnbound()) {
            next = label.getPos();
        }
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((next & ~NEXT_MASK) == 0, "next field too small");
            Assert.that((type & ~TYPE_MASK) == 0, "type field too small");
            Assert.that((info & ~INFO_MASK) == 0, "info field too small");
        }
        data = (next << NEXT_POS) | (type << TYPE_POS) | (info << INFO_POS);
    }

    /**
     * Returns the bit field that encodes all of the displacement data.
     *
     * @return  the displacement data
     */
    public int getData() {
        return data;
    }

    /**
     * Returns any instruction specific information.
     *
     * @return  instruction specific information
     */
    public int getInfo() {
        return (data >>> INFO_POS) & INFO_MASK;
    }

    /**
     * Returns the type of the instructions that this displacement is used with.
     *
     * @return  the instruction type
     */
    public int getType() {
        return (data >>> TYPE_POS) & TYPE_MASK;
    }

    /**
     * Binds the specified label to the next displacement. If the end of the
     * chain is reached then the label will be cleared.
     *
     * @param  label  label to be linked to the next position
     */
    public void next(Label label) {
        int pos = (data >>> NEXT_POS) & NEXT_MASK;
        if (pos > 0) {
            label.linkTo(pos);
        } else {
            label.clear();
        }
    }

    /**
     * Links this displacement to the specified label.
     *
     * @param  label  the yet unknown code position
     */
    public void linkTo(Label label) {
        init(label, getType(), getInfo());
    }
}
