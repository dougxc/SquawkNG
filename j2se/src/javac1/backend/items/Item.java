/*
 * @(#)Item.java                        1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.items;

import javac1.Assert;
import javac1.backend.reg.RInfo;
import javac1.ci.Obj;
import javac1.ir.instr.Instruction;
import javac1.ir.types.*;

/**
 * This class represents an operand of code generation. It describes the value's
 * location and its access mode. The value can be a constant, or it can be in a
 * register, on the stack or in memory.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Item {
    /**
     * The mode constant for items that do not represent any value.
     */
    public static final int NO_MODE = 0;

    /**
     * The mode constant for register items.
     */
    public static final int REG_MODE = 1;

    /**
     * The mode constant for stack items.
     */
    public static final int STACK_MODE = 2;

    /**
     * The mode constant for constant items.
     */
    public static final int CONST_MODE = 3;

    /**
     * The value of the spill index if the item is not spilled.
     */
    public static final int NOT_SPILLED = -1;

    /**
     * The item mode that describes the location of the value.
     */
    private int mode;

    /**
     * The spill slot index or a value less than 0 if the item is not spilled.
     */
    private int spillIx;

    /**
     * The instruction that specifies the value type of the item.
     */
    private Instruction instr;

    /**
     * Whether or not the register specified by this item will be destroyed.
     */
    private boolean destroysRegister;

    /**
     * The register information number or stack slot index respectively.
     */
    private int loc;

    /**
     * Whether or not the item must be in 32-bit precision.
     */
    private boolean round32;

    /**
     * Whether or not the item contains a cached local variable.
     */
    private boolean cached;

    /**
     * Whether or not the item must be in register mode.
     */
    private boolean inreg;

    /**
     * Initializes the attributes declared in this class.
     */
    protected Item() {
        this.instr = null;
        this.mode = NO_MODE;
        this.spillIx = NOT_SPILLED;
        this.destroysRegister = false;
        this.loc = 0;
        this.round32 = false;
        this.cached = false;
        this.inreg = false;
    }

    /**
     * Constructs a new item and links it up with the specified instruction.
     *
     * @param  instr  the instruction to construct item for
     */
    public Item(Instruction instr) {
        this();
        this.instr = instr;
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(instr != null, "instruction must not be null");
        }
        if (!instr.isRoot()) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(instr.getItem() == null, "item already set");
            }
            instr.setItem(this);
        }
    }

    /**
     * Assigns the specified register to this item.
     *
     * @param  reg     register to be assigned
     * @param  cached  whether or not the item is cached
     */
    public void setRInfo(RInfo reg, boolean cached) {
        mode = REG_MODE;
        loc = reg.getNumber();
        spillIx = NOT_SPILLED;
        this.cached = cached;
    }

    /**
     * Assigns the specified register to this item.
     *
     * @param  reg  register to be assigned
     */
    public void setRInfo(RInfo reg) {
        setRInfo(reg, false);
    }

    /**
     * Returns the register specified by this item.
     *
     * @return  register information
     */
    public RInfo getRInfo() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isRegister(), "item does not represent register");
        }
        return new RInfo(loc);
    }

    /**
     * Tests if this item represesents a register that is not spilled.
     *
     * @return  whether or not this is a register item
     */
    public boolean isRegister() {
        return (mode == REG_MODE) && (spillIx == NOT_SPILLED);
    }

    /**
     * Assigns the specified spill slot to this item.
     *
     * @param  spillIx  index of the spill slot to be assigned
     */
    public void setSpillIx(int spillIx) {
        mode = REG_MODE;
        this.spillIx = spillIx;
    }

    /**
     * Returns the spill slot index specified by this item.
     *
     * @return  index of the spill slot
     */
    public int getSpillIx() {
        return spillIx;
    }

    /**
     * Tests if this item represents a spilled register.
     *
     * @return  whether or not this item is spilled
     */
    public boolean isSpilled() {
        return (spillIx != NOT_SPILLED);
    }

    /**
     * Assigns the specified stack slot to this item.
     *
     * @param  index  index of the stack slot to be assigned
     */
    public void setStack(int index) {
        mode = STACK_MODE;
        loc = index;
        spillIx = NOT_SPILLED;
    }

    /**
     * Returns the stack slot index specified by this item.
     *
     * @return  index of the stack slot
     */
    public int getStack() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isStack(), "item does not represent stack slot");
        }
        return loc;
    }

    /**
     * Tests if this item represents a stack slot or a spilled register.
     *
     * @return  whether or not this is a stack item
     */
    public boolean isStack() {
        return (mode == STACK_MODE) || (spillIx != NOT_SPILLED);
    }

    /**
     * Sets that the item represents a constant. The value of the constant is
     * specified by the referenced instruction.
     */
    public void setConstant() {
        mode = CONST_MODE;
        spillIx = NOT_SPILLED;
    }

    /**
     * Tests if this item represents a constant.
     *
     * @return  whether or not this is a constant item
     */
    public boolean isConstant() {
        return (mode == CONST_MODE);
    }

    /**
     * Sets this item to represent no result.
     */
    public void setNoResult() {
        mode = NO_MODE;
    }

    /**
     * Returns whether or not this item has a result.
     *
     * @return  whether or not this item has a result
     */
    public boolean hasResult() {
        return (mode != NO_MODE);
    }

    /**
     * Returns whether or not this item has a hint.
     *
     * @return  whether or not this item has a hint
     */
    public boolean hasHint() {
        return (mode != NO_MODE);
    }

    /**
     * Returns whether or not this item is valid.
     *
     * @return  whether or not this item is valid
     */
    public boolean isValid() {
        return (mode != NO_MODE);
    }

    /**
     * Returns the mode of this item.
     *
     * @return  the item mode
     */
    public int getMode() {
        return mode;
    }

    /**
     * Returns the register information number or stack slot index of this item.
     *
     * @return  location of this item
     */
    public int getLoc() {
        return loc;
    }

    /**
     * Sets whether or not this item must be in 32-bit precision.
     *
     * @param  round32  whether or not the item must be in 32-bit precision
     */
    public void setRound32(boolean round32) {
        this.round32 = round32;
    }

    /**
     * Returns whether or not this item must be in 32-bit precision.
     *
     * @return  whether or not the item must be in 32-bit precision
     */
    public boolean isRound32() {
        return round32;
    }

    /**
     * Sets whether or not this item must be in register mode.
     *
     * @param  inreg  whether or not the item must be in register mode
     */
    public void setInreg(boolean inreg) {
        this.inreg = inreg;
    }

    /**
     * Returns whether or not this item must be in register mode.
     *
     * @return  whether or not the item must be in register mode
     */
    public boolean isInreg() {
        return inreg;
    }

    /**
     * Returns whether or not this item contains a cached local variable.
     *
     * @return  whether or not this item is cached
     */
    public boolean isCached() {
        return cached;
    }

    /**
     * Returns the value type of this item.
     *
     * @return  type of this item
     */
    public ValueType getType() {
        return instr.getType();
    }

    /**
     * Returns the instruction that this item is linked to.
     *
     * @return  the value of this item
     */
    public Instruction getValue() {
        return instr;
    }

    /**
     * Sets whether or not the register specified by this item will be
     * destroyed.
     *
     * @param  destroysRegister  whether or not the register will be destroyed
     */
    public void setDestroysRegister(boolean destroysRegister) {
        this.destroysRegister = destroysRegister;
    }

    /**
     * Tests if the register specified by this item will be destroyed.
     *
     * @return  whether or not the register will be destroyed
     */
    public boolean destroysRegister() {
        return destroysRegister;
    }

    /**
     * Marks floating-point items as destroying the specified register. The
     * reason is that the registers are popped off the floating-point stack
     * after finishing the operation.
     */
    public void handleFloatKind() {
        if (getType().isFloatKind()) {
            destroysRegister = true;
        }
    }

    /**
     * Returns the value of the object constant specified by this item.
     *
     * @return  the value of the constant
     */
    public Obj getObjectConstant() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isConstant() && (instr != null), "should not reach here");
        }
        if (getType() instanceof ObjectConstant) {
            return ((ObjectConstant) getType()).getValue();
        } else if (getType() instanceof InstanceConstant) {
            return ((InstanceConstant) getType()).getValue();
        } else if (getType() instanceof ClassConstant) {
            return ((ClassConstant) getType()).getValue();
        } else if (getType() instanceof ArrayConstant) {
            return ((ArrayConstant) getType()).getValue();
        } else {
            Assert.shouldNotReachHere();
            return null;
        }
    }

    /**
     * Returns the value of the integer constant specified by this item.
     *
     * @return  the value of the constant
     */
    public int getIntConstant() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isConstant() && (instr != null) && (getType() instanceof IntConstant), "should not reach here");
        }
        return ((IntConstant) getType()).getValue();
    }

    /**
     * Returns the value of the long integer constant specified by this item.
     *
     * @return  the value of the constant
     */
    public long getLongConstant() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isConstant() && (instr != null) && (getType() instanceof LongConstant), "should not reach here");
        }
        return ((LongConstant) getType()).getValue();
    }

    /**
     * Returns the value of the address constant specified by this item.
     *
     * @return  the value of the constant
     */
    public int getAddressConstant() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isConstant() && (instr != null) && (getType() instanceof AddressConstant), "should not reach here");
        }
        return ((AddressConstant) getType()).getValue();
    }

    /**
     * Returns the value of the single-precision floating-point constant
     * specified by this item.
     *
     * @return  the value of the constant
     */
    public float getFloatConstant() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isConstant() && (instr != null) && (getType() instanceof FloatConstant), "should not reach here");
        }
        return ((FloatConstant) getType()).getValue();
    }

    /**
     * Returns the value of the double-precision floating-point constant
     * specified by this item.
     *
     * @return  the value of the constant
     */
    public double getDoubleConstant() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(isConstant() && (instr != null) && (getType() instanceof DoubleConstant), "should not reach here");
        }
        return ((DoubleConstant) getType()).getValue();
    }

    /**
     * Sets the attributes of this item from the specified one.
     *
     * @param  item  item to copy attributes from
     */
    public void setFromItem(Item item) {
        mode = item.getMode();
        loc = item.getLoc();
        spillIx = item.getSpillIx();
        round32 = item.isRound32();
        inreg = item.isInreg();
        instr = item.getValue();
        cached = item.isCached();
    }

    /**
     * Clears the item in the corresponding instruction.
     */
    public void dispose() {
        if ((instr != null) && !instr.isRoot()) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(instr.getItem() == this, "wrong pairing");
            }
            instr.clearItem();
            instr = null;
        }
    }

    /**
     * Updates the attributes of this item. This method is used to keep the
     * values of root items synchronized with the instructions. If this item
     * points to a root but the root does not point to this item then the
     * content of the root item is copied into this one.
     */
    public void update() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!(this instanceof RootItem), "should not reach here");
        }
        if (instr.isRoot() && (instr.getItem() != this)) {
            setFromItem(instr.getItem());
        }
    }

    /**
     * Compares this item with the specified object for equality.
     *
     * @param   obj  the reference object with which to compare
     * @return  whether or not the items are equal
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Item)) {
            return false;
        }
        Item item = (Item) obj;
        return (mode == item.getMode()) && (loc == item.getLoc())
            && (spillIx == item.getSpillIx()) && (instr == item.getValue())
            && (round32 == item.isRound32()) && (cached == item.isCached())
            && (inreg == item.isInreg());
    }
}
