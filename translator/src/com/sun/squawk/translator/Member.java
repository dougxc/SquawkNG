
package com.sun.squawk.translator;
import com.sun.squawk.translator.util.*;
import com.sun.squawk.translator.loader.*;

/**
 * This is the base class for Methods and Fields.
 */
public abstract class Member extends AccessFlags {

    /**
     * The type in which the member was defined. A member
     * can belong to a number of classes by inheritance, but only one
     * class can have defined it.
     */
    final private Type parent;

    /*** The name of the member (e.g. "main"). */
    final private String name;
    /** The (result) type of this field or method. */
    final private Type type;
    /** The slot offset of this member in its enclosing table. */
    private int slot;
    /** Flags whether or not the signature types of this member have been loaded. */
    private boolean typesLoaded;

    /**
     * Constructor for subclasses.
     * @param parent The type in which the member was defined.
     * @param name The name of the member.
     * @param type The (result) type of this field or method.
     * @param flags Attribute/access flags.
     */
    protected Member(Type parent, String name, Type type, int flags) {
        this.parent = parent;
        this.name   = name;
        this.type   = type;
        this.slot   = -1;
        setFlags(flags);
    }

    /**
     * Get the type in which the member was defined.
     * @return the type in which the member was defined.
     */
    public Type parent() {
        return parent;
    }

    /**
     * Get the name of the member.
     * @return the name of the member.
     */
    public String name() {
        return name;
    }

    /**
     * Get the slot offset of this member in its enclosing table. If the
     * parent of this member has a LinkageError, then 0 is returned.
     * @return the slot offset of this member in its enclosing table.
     */
    public int slot() {
        if (slot != -1) {
            return slot;
        }
        Assert.that(parent.getLinkageError() != null);
        return 0;
    }

    /**
     * Load the types implied by the signature of this member.
     */
    abstract public void loadSignatureTypes() throws LinkageException;

    /**
     * Set the slot offset of this member in its enclosing table.
     * @param slot the slot offset of this member in its enclosing table.
     */
    public void setSlot(int slot) {
        this.slot = slot;
    }

    /**
     * Get the type in which the member was defined.
     * @return the type in which the member was defined.
     */
    public Type type() {
        return type;
    }

    /**
     * Get the Translator context of the member.
     * @return the Translator context of the member.
     */
    public Translator vm() {
        return type.vm();
    }

    protected boolean areTypesLoaded() {
        return typesLoaded;
    }

    /**
     * Mark this member as having been loaded.
     */
    protected void setTypesLoaded() {
        Assert.that(typesLoaded == false);
        typesLoaded = true;
    }

    /**
     * Determine whether or not this member is accessible by a specified class.
     * @param other A class that refers to this class.
     * @return true if 'other' is null or has access to this member.
     */
    public final boolean isAccessibleBy(Type other) {
        if (other == null ||
                parent == other ||
                isPublic()) {
            return true;
        }
        if (isPrivate()) {
            return false;
        }
        if (parent.inSamePackageAs(other)) {
            return true;
        }
        if (isProtected()) {
            for (other = other.superClass(); other != null; other = other.superClass()) {
                if (other == parent) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return the signature of the member in internal format. E.g. "(II)V" for a method
     * or "Ljava/io/PrintStream;" for a field.
     * @param includePackageNames If true, then use fully qualified class names.
     * @param asSourceDecl If true, then use a format corresponding with the source level
     * declaration of this member.
     * @return the signature of the member in internal format.
     */
    public abstract String signature(boolean includePackageNames, boolean asSourceDecl);

    /**
     * Return a String representation of this member.
     * @param includePackageNames If true, then use fully qualified class names.
     * @param asSourceDecl If true, then use a format corresponding with the source level
     * declaration of this member.
     * @param includeParent If true, include the class owning this member in the string.
     * @return a String representation of the member.
     */
    public abstract String toString(boolean includePackageNames, boolean asSourceDecl, boolean includeParent);

    /**
     * Get a String representation of the member.
     * @return a String representation of the member.
     */
    final public String toString() {
        return toString(vm().namesUseFqn(), vm().namesUseSrc(), vm().namesUseParent());
    }

   /* ------------------------------------------------------------------------ *\
    *                         Suite pruning                                    *
   \* ------------------------------------------------------------------------ */

    public abstract void mark();
}
