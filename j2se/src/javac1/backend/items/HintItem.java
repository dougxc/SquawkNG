/*
 * @(#)HintItem.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.items;

import javac1.Assert;
import javac1.backend.reg.RInfo;
import javac1.ir.types.ValueType;

/**
 * Describes what the result should look like. Hint items represent attributes
 * flowing down during evaluation as opposed to up towards the root of the tree.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class HintItem extends Item {
    /**
     * A constant used to state that no hint item is needed.
     */
    public static final HintItem NO_HINT = new HintItem(null);

    /**
     * The value type of this hint item.
     */
    private ValueType type;

    /**
     * Constructs a new hint item with the specified type.
     *
     * @param  type  the type of this item
     */
    public HintItem(ValueType type) {
        this.type = type;
    }

    /**
     * Constructs a new hint item that describes the specified register.
     *
     * @param  type    the type of this item
     * @param  reg     register to be described
     * @param  cached  whether or not the item is cached
     */
    public HintItem(ValueType type, RInfo reg, boolean cached) {
        this(type);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(reg.isValid(), "invalid register");
        }
        setRInfo(reg, cached);
    }

    /**
     * Constructs a new hint item that describes the specified register.
     *
     * @param  type  the type of this item
     * @param  reg   register to be described
     */
    public HintItem(ValueType type, RInfo reg) {
        this(type, reg, false);
    }

    public ValueType getType() {
        return type;
    }

    public void setFromItem(Item item) {
        super.setFromItem(item);
        type = item.getType();
    }

    public boolean equals(Object obj) {
        return super.equals(obj) && (obj instanceof HintItem)
            && (((HintItem) obj).getType() == type);
    }
}
