package com.sun.squawk.translator.ir;

import com.sun.squawk.translator.Type;
import com.sun.squawk.translator.Assert;

/**
 * This class represents a local variable that was created to denote a unique
 * usage of a stack slot.
 */
public final class TemporaryLocal extends Local {

    /** This keeps track of the number of times the value on the stack was duped. */
    private int referenceCount;
    /** The stack slot represented. */
    private final int slot;
    /** Flags whether or not this variable represents a merged stack slot. */
    private boolean merged;

    /**
     * Construct a TemporaryLocal variable that will be used to store values of
     * a given type.
     */
    public TemporaryLocal(Type type, int slot) {
        super(type);
        Assert.that(type == type.localType());
        this.slot = slot;
    }

    public int slot() {
        return slot;
    }

    public void incrementReferenceCount() {
        referenceCount++;
    }

    public boolean decrementReferenceCount() {
        return --referenceCount <= 0;
    }

    public int referenceCount() {
        return referenceCount;
    }

    public String toString() {
        return toString("tmp"+(squawkIndex() == -1 ? id() : squawkIndex()));
    }

    /**
     * Set the flag indicating that this variable represents a merged stack slot.
     */
    public void setMerged() {
        merged = true;
    }

    /**
     * Return true if this variable represents a merged stack slot.
     * @return
     */
    public boolean isMerged() {
        return merged;
    }
}
