package com.sun.squawk.translator;

import com.sun.squawk.translator.util.JVMConst;
import com.sun.squawk.vm.SquawkConstants;

/**
 * This class is subclassed by any class that uses the ACC_* access flags
 * defined by the Java VM spec and the extra ones defined for the translator.
 */

public abstract class AccessFlags {

    /** A mask to enforce setting of only valid bits. */
    private final int validMask;
    private int flags;
    private final int validSuiteMask;

    protected AccessFlags() {
        if (this instanceof Type) {
            this.validMask = JVMConst.VALID_CLASS_FLAGS_MASK |
                             JVMConst.TRANSLATOR_FLAGS_MASK;
            this.validSuiteMask = SquawkConstants.ACC_PUBLIC    |
                                  SquawkConstants.ACC_FINAL     |
                                  SquawkConstants.ACC_INTERFACE |
                                  SquawkConstants.ACC_ABSTRACT  |
                                  SquawkConstants.ACC_SYMBOLIC;

        }
        else if(this instanceof Method) {
            this.validMask = JVMConst.VALID_METHOD_FLAGS_MASK |
                             JVMConst.TRANSLATOR_FLAGS_MASK;
            this.validSuiteMask = SquawkConstants.ACC_PUBLIC    |
                                  SquawkConstants.ACC_PRIVATE   |
                                  SquawkConstants.ACC_PROTECTED |
                                  SquawkConstants.ACC_FINAL     |
                                  SquawkConstants.ACC_NATIVE    |
                                  SquawkConstants.ACC_ABSTRACT  |
                                  SquawkConstants.ACC_SYMBOLIC  |
                                  SquawkConstants.ACC_STATIC    |
                                  SquawkConstants.ACC_INIT;
        }
        else {
            Assert.that(this instanceof Field);
            this.validMask = JVMConst.VALID_FIELD_FLAGS_MASK |
                             JVMConst.TRANSLATOR_FLAGS_MASK;
            this.validSuiteMask = SquawkConstants.ACC_PUBLIC    |
                                  SquawkConstants.ACC_PRIVATE   |
                                  SquawkConstants.ACC_PROTECTED |
                                  SquawkConstants.ACC_FINAL     |
                                  SquawkConstants.ACC_VOLATILE  |
                                  SquawkConstants.ACC_TRANSIENT |
//                                  SquawkConstants.ACC_SYNCHRONIZED |
                                  SquawkConstants.ACC_SYMBOLIC  |
                                  SquawkConstants.ACC_STATIC;
        }
    }

    public final boolean isPublic()       {  return isSet(JVMConst.ACC_PUBLIC);       }
    public final boolean isPrivate()      {  return isSet(JVMConst.ACC_PRIVATE);      }
    public final boolean isProtected()    {  return isSet(JVMConst.ACC_PROTECTED);    }
    public final boolean isStatic()       {  return isSet(JVMConst.ACC_STATIC);       }
    public final boolean isFinal()        {  return isSet(JVMConst.ACC_FINAL);        }
    public final boolean isSynchronized() {  return isSet(JVMConst.ACC_SYNCHRONIZED); }
    public final boolean isNative()       {  return isSet(JVMConst.ACC_NATIVE);       }
    public final boolean isInterface()    {  return isSet(JVMConst.ACC_INTERFACE);    }
    public final boolean isAbstract()     {  return isSet(JVMConst.ACC_ABSTRACT);     }
    public final boolean isStrict()       {  return isSet(JVMConst.ACC_STRICT);       }
    public final boolean isVolatile()     {  return isSet(JVMConst.ACC_VOLATILE);     }
    public final boolean isTransient()    {  return isSet(JVMConst.ACC_TRANSIENT);    }

    public final boolean isProxy()        {  return isSet(JVMConst.ACC_PROXY);        }
    public final boolean includeInSuite() {  return isSet(JVMConst.ACC_INCLUDE);      }


    public final boolean isSet(int bit) {
        Assert.that((bit & ~validMask) == 0);
        return ((flags & bit) != 0);
    }

    /**
     * Update the access flags with a single flag.
     * @bit the bit to set in the flags.
     */
    public final void setFlag(int bit) {
        // Ensure only one bit is being set
        Assert.that((bit & (bit-1)) == 0);
        Assert.that((bit & ~validMask) == 0);
        flags |= bit;
    }


    /**
     * Update the access flags with a group of flags.
     * @bit the bit to set in the flags.
     */
    public final void setFlags(int flags) {
        Assert.that((flags & ~validMask) == 0);
        this.flags |= flags;
    }

    /**
     * Unset a single flag.
     * @bit the bit to unset in the flags.
     */
    public final void unsetFlag(int bit) {
        // Ensure only one bit is being unset
        Assert.that((bit & (bit-1)) == 0);
        Assert.that((bit & ~validMask) == 0);
        flags &= ~bit;
    }

    /**
     * Unset a group of flags.
     * @bit the bit to unset in the flags.
     */
    public final void unsetFlags(int flags) {
        Assert.that((flags & ~validMask) == 0);
        this.flags &= ~flags;
    }

    /**
     * Get the access flags.
     * @return the access flags.
     */
    public final int flags() {
        return flags;
    }

    /**
     * Translate Java VM flags into the equivalent Squawk flags.
     * @param isInit If true, add the Squawk specific flag indicating that the
     * enclosing method is a constructor.
     * @param isSymbolic If true, add the Squawk specific flag indicating that
     * the enclosing method or field is transmitted in a Suite file with its
     * symbolic info (i.e. it name).
     * @return
     */
    public final int suiteFlags(boolean isInit, boolean isSymbolic) {
        int sflags = 0;
        if ((flags & JVMConst.ACC_PUBLIC) != 0)       sflags |= SquawkConstants.ACC_PUBLIC;
        if ((flags & JVMConst.ACC_PRIVATE) != 0)      sflags |= SquawkConstants.ACC_PRIVATE;
        if ((flags & JVMConst.ACC_PROTECTED) != 0)    sflags |= SquawkConstants.ACC_PROTECTED;
        if ((flags & JVMConst.ACC_STATIC) != 0)       sflags |= SquawkConstants.ACC_STATIC;
        if ((flags & JVMConst.ACC_FINAL) != 0)        sflags |= SquawkConstants.ACC_FINAL;
        if ((flags & JVMConst.ACC_SYNCHRONIZED) != 0) sflags |= SquawkConstants.ACC_SYNCHRONIZED;
        if ((flags & JVMConst.ACC_NATIVE) != 0)       sflags |= SquawkConstants.ACC_NATIVE;
        if ((flags & JVMConst.ACC_INTERFACE) != 0)    sflags |= SquawkConstants.ACC_INTERFACE;
        if ((flags & JVMConst.ACC_ABSTRACT) != 0)     sflags |= SquawkConstants.ACC_ABSTRACT;
        if ((flags & JVMConst.ACC_VOLATILE) != 0)     sflags |= SquawkConstants.ACC_VOLATILE;
        if ((flags & JVMConst.ACC_TRANSIENT) != 0)    sflags |= SquawkConstants.ACC_TRANSIENT;
        if ((flags & JVMConst.ACC_PROXY) != 0)        sflags |= SquawkConstants.ACC_PROXY;
        if (isInit)                          sflags |= SquawkConstants.ACC_INIT;
        if (isSymbolic)                      sflags |= SquawkConstants.ACC_SYMBOLIC;
        return sflags & validSuiteMask;
    }
}
