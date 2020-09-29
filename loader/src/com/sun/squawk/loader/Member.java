package com.sun.squawk.loader;

import com.sun.squawk.suite.*;
import com.sun.squawk.vm.SquawkConstants;

/**
 * The Member class is a container for symbolic information of a field or
 * method. It is returned in StringOfSymbols.getMember() to enable simple access
 * to the symbolic information.
 */
public class Member {

    /**
     * Prevent external creation.
     */
    private Member() {}

    /**
     * A constant denoting zero length array of parameters.
     */
    public static char[] NO_PARMS = new char[0];

    /**
     * The shared instance.
     */
    private static Member sharedInstance = new Member();

    /* Instance data */
    private StringOfSymbols symbols;
    private int             memberID;
    private char            type;
    private int             access;
    private char            offset;
    private char            nativeMethodIdentifier;
    private char[]          parms = new char[16];
    private int             parmCount;

    /**
     * Get an member instance to hold the details of a member.
     *
     * @param symbols
     * @param memberID
     * @param type
     * @param access
     * @param offset
     * @return a member
     */
    public static Member create(StringOfSymbols symbols,
                                int memberID,
                                int type,
                                int access,
                                int offset,
                                int nativeMethodIdentifier) {
        Member member   = sharedInstance;
        member.symbols  = symbols;
        member.memberID = memberID;
        member.type     = (char)type;
        member.access   = access;
        member.offset   = (char)offset;
        member.nativeMethodIdentifier = (char)nativeMethodIdentifier;
        int lth = symbols.getMemberParmLength(memberID);
        if (member.parms.length < lth) {
            member.parms = new char[lth];
        }
        member.parmCount = lth;
        for (int i = 0 ; i < lth ; i++) {
            member.parms[i] = symbols.getMemberParm(memberID, i);
        }
        return member;
    }
    /**
     * Return the member's access flags.
     *
     * @return the member's access flags.
     */
    public int access() {
        return access;
    }

    /**
     * Return the member's type. This is the type of a field or
     * the return type of a method.
     *
     * @return the member's type
     */
    public char type() {
        return type;
    }

    /**
     * Return the offset for the member.
     *
     * @return the offset for the member.
     */
    public int offset() {
        return offset;
    }

    /**
     * Return the globally unique identifier that was assigned to this
     * native method.
     *
     * @return this native method's globally unique identifier.
     */
    public int nativeMethodIdentifier() {
        return nativeMethodIdentifier;
    }

    /**
     * Return the length of this member's name.
     *
     * @return the length of this member's name.
     */
    public int nameLength() {
        return symbols.getMemberNameLength(memberID);
    }

    /**
     * Return the character at a given index in this member's name.
     *
     * @param i the index
     * @return the character at 'i' in this member's name.
     */
    public char nameAt(int i) {
        return symbols.getMemberNameChar(memberID, i);
    }

    /**
     * Return the number of parameters this method has.
     *
     * @return the number of parameters this method has.
     */
    public int parmCount() {
        return parmCount;
    }

    /**
     * Return the type of the parameter at specified index.
     *
     * @param i the index of the parameter.
     * @return the type of the 'i'th parameter.
     */
    public char parmAt(int i) {
        return parms[i];
    }

    /**
     * Return the name of this member or "**NONAME**" if it doesn't have one.
     *
     * @return the name of this member or "**NONAME**" if it doesn't have one.
     */
    public String name() {
        String name = symbols.getMemberName(memberID);
        if (name.length() == 0) {
            name = "**NONAME**";
        }
        return name;
    }

    /**
     * Determine whether or not this method is native.
     *
     * @return true if this method is native.
     */
    public boolean isNative() {
        return ((access & SquawkConstants.ACC_NATIVE) != 0);
    }

    /**
     * Update the offset for the member.
     *
     * @param offset the new offset.
     */
    public void setOffset(int offset) {
        this.offset = (char)offset;
    }

    /**
     * Update the globally unique identifier assigned to this
     * native method.
     *
     * @param offset the native method identifier.
     */
    public void setNativeMethodIdentifier(int nativeMethodIdentifier) {
        Romizer.assume(this.nativeMethodIdentifier == 0); // Cannot be set again
        this.nativeMethodIdentifier = (char)nativeMethodIdentifier;
    }

    /**
     * Convert the type identifier components of this member from the
     * internally linked class namespace to the namespace of the suite
     * currently being loaded by the loader.
     */
    public void convertSystemTypesToSuiteClasss() {
        SuiteLoader loader = SuiteLoader.theLoader;
        type = loader.findSuiteTypeForSystemType(type);
        if (parms != null) {
            for (int i = 0; i < parmCount; i++) {
                parms[i] = loader.findSuiteTypeForSystemType(parms[i]);
            }
        }
    }

    /**
     * Convert the type identifier components of this member from the
     * the namespace of the suite currently being loaded by the suite loader.
     * to the internally linked class namespace.
     */
    public void convertSuiteTypesToSystemTypes() {
        SuiteLoader loader = SuiteLoader.theLoader;
        type = loader.getSystemTypeFor(type);
        if (parms != null) {
            for (int i = 0; i < parmCount; i++) {
                parms[i] = loader.getSystemTypeFor(parms[i]);
            }
        }
    }

/*if[DEBUG.LOADER]*/
    /**
     * Return the signature of this member as a String.
     * @param className
     * @param isStatic
     * @param isMethod
     * @param loader
     * @return
     */
    public String toString(String className,
                           boolean isStatic,
                           boolean isMethod,
                           SuiteLoader loader) {
        StringBuffer buf = new StringBuffer();
        int cno = type&0xFF;
        buf.append(loader.getRelocator(cno).getName()).append(" ");
        if (className != null) {
            buf.append(className).append(".");
        }
        buf.append(name());
        if (isMethod) {
            buf.append("(");
            for (int i = 0; i < parmCount; i++) {
                cno = parms[i]&0xFF;
                buf.append(loader.getRelocator(cno).getName());
                if (i != (parmCount - 1)) {
                    buf.append(", ");
                }
            }
            buf.append(")");
        }
        if (isStatic) {
            buf.append(" static");
        }
        return buf.toString();
    }

/*end[DEBUG.LOADER]*/
}

