/*
 * Copyright 1995-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */
package java.lang;

import com.sun.squawk.loader.Member;
import com.sun.squawk.loader.SuiteLoader;
import com.sun.squawk.vm.CNO;
import com.sun.squawk.vm.SquawkConstants;

public class StringOfSymbols
/*if[NEWSYMBOLS]*/
/*if[NOTROMIZER]*/
                              extends StringOfBytes
/*end[NOTROMIZER]*/
/*end[NEWSYMBOLS]*/
                                                     {

/*if[ROMIZER]*/
    private String _symbols;

    int length() {
        return _symbols.length();
    }

    char at(int i) {
        return _symbols.charAt(i);
    }

    public String substring(int from, int to) {
        return _symbols.substring(from, to);
    }

    int indexOf(char ch, int from) {
        return _symbols.indexOf(ch, from);
    }

    private StringOfSymbols(byte[] value, int count) {
        char[] chars = new char[count];
        for (int i = 0 ; i < count ; i++) {
            int ch = value[i] & 0xFF;
            chars[i] = (char)ch;
        }
        _symbols = new String(chars, 0, count);
    }

    public static StringOfSymbols create(byte[] value, int count) {
        return new StringOfSymbols(value, count);
    }

    public String toString() {
        return _symbols;
    }
/*end[ROMIZER]*/

/*if[NOTROMIZER]*/
/*if[OLDSYMBOLS]*/
    private String _symbols;

    int length() {
        return _symbols.length();
    }

    char at(int i) {
        return _symbols.charAt(i);
    }

    String substring(int from, int to) {
        return _symbols.substring(from, to);
    }

    int indexOf(char ch, int from) {
        return _symbols.indexOf(ch, from);
    }

    private StringOfSymbols(byte[] value, int count) {
        char[] chars = new char[count];
        for (int i = 0 ; i < count ; i++) {
            int ch = value[i] & 0xFF;
            chars[i] = (char)ch;
        }
        _symbols = new String(chars, 0, count);
    }

    public static StringOfSymbols create(byte[] value, int count) {
        return new StringOfSymbols(value, count);
    }

    public String toString() {
        return _symbols;
    }
/*end[OLDSYMBOLS]*/
/*if[NEWSYMBOLS]*/
    public static StringOfSymbols create(byte[] value, int count) {
        String s = new String(value, count);
        return Native.makeStringOfSymbols(s);
    }
/*end[NEWSYMBOLS]*/
/*end[NOTROMIZER]*/


   /* ------------------------------------------------------------------------ *\
    *                                  Editing                                 *
   \* ------------------------------------------------------------------------ */

    /**
     * Remove all the members from the symbol string.
     *
     * @param editor
     * @return
     */
    public StringOfSymbols removeMembers() {
        return new SymbolBuilder(this).toStringOfSymbols();
    }

    /**
     * Edit the symbols with an Editor and return the resulting symbols string.
     * The symbols string of this instance is not modified.
     *
     * @param editor
     * @return new symbol string
     */
    public StringOfSymbols edit(SymbolEditor editor) {
        SymbolBuilder buf = new SymbolBuilder(this);
        editMembers(buf, SquawkConstants.MTYPE_VIRTUALMETHODS, editor);
        editMembers(buf, SquawkConstants.MTYPE_STATICMETHODS,  editor);
        editMembers(buf, SquawkConstants.MTYPE_INSTANCEFIELDS, editor);
        editMembers(buf, SquawkConstants.MTYPE_STATICFIELDS,   editor);
        return buf.toStringOfSymbols();
    }

    /**
     * editMembers
     *
     * @param buf
     * @param mtype
     * @param editor
     */
    private void editMembers(SymbolBuilder buf, int mtype, SymbolEditor editor) {
        int count = getMemberCount(mtype);
        boolean firstTime = true;
        int slot = 0;
        ByteBuffer byteBuf = new ByteBuffer();
        while (slot < count) {
            Member m = getMember(lookupMemberID(mtype, slot));
            m = editor.editMember(mtype, m, slot);
            if (m != null) {
                if (firstTime) {
                    buf.addSegmentMarker(mtype);
                    firstTime = false;
                }
                int access = m.access();
                int type   = m.type();
                int offset = m.offset() | (m.nativeMethodIdentifier() << 16);
                int nlth   = m.nameLength();
                int pcnt   = m.parmCount();
                byteBuf.reset();
                byteBuf.addAccess(access);
                byteBuf.addOffset(offset);
                for (int i = 0 ; i < nlth ; i++) {
                    byteBuf.addByte(m.nameAt(i));
                }
                byteBuf.addDash();
                byteBuf.addType(type);
                for (int i = 0 ; i < pcnt ; i++) {
                    byteBuf.addParm(m.parmAt(i));
                }
                buf.addBuffer(byteBuf);
            }
            slot++;
        }
    }


   /* ------------------------------------------------------------------------ *\
    *                          Member type functions                           *
   \* ------------------------------------------------------------------------ */

    public static boolean isStaticMember(int mtype) {
        return mtype == SquawkConstants.MTYPE_STATICFIELDS || mtype == SquawkConstants.MTYPE_STATICMETHODS;
    }

    public static boolean isFieldMember(int mtype) {
        return mtype == SquawkConstants.MTYPE_STATICFIELDS || mtype == SquawkConstants.MTYPE_INSTANCEFIELDS;
    }

    /**
     * Test the class.
     * @param str the class name to test
     * @return true if the class name is the same
     */
    public boolean equalsClassName(String str) {
        return equalsClassName(str, 0);
    }

    /**
     * Test the class.
     * @param str the class name to test
     * @return true if the class name is array class of the parameter
     */
    public boolean isArrayTypeFor(String str) {
        return equalsClassName(str, 1);
                                                                            }

    /**
     * Test the class.
     * @param str the class name to test
     * @param skip the number of initial characters to skip
     * @return true if the class name is the same
     */
    private boolean equalsClassName(String str, int skip) {
        SymbolParser p = SymbolParser.create(this);
        return p.equalsClassName(str, skip);
    }

    /**
     * Test the class.
     * @return true if it is an array
     */
    public boolean isArray() {
        SymbolParser p = SymbolParser.create(this);
        return p.isArray();
    }

    /**
     * Get the class name from the symbols of a class.
     * @return the class name.
     */
    public String getClassName() {
        SymbolParser p = SymbolParser.create(this);
        return p.getClassName();
    }

    /**
     * Get the number of characters in the class name
     * @return the length
     */
    public int getClassNameLength() {
        SymbolParser p = SymbolParser.create(this);
        return p.getClassNameLength();
    }

    /**
     * Get a specific character in the class name
     * @return the char
     */
    public char getClassNameChar(int index) {
        SymbolParser p = SymbolParser.create(this);
        return p.getClassNameChar(index);
    }

    /**
     * Count the number of members in a given section of members.
     * @param mtype The section of members to search.
     * @return the number of entries in a given section of members
     */
    public int getMemberCount(int mtype) {
        SymbolParser p = SymbolParser.create(this);
        return p.getMemberCount(mtype);
    }

    /**
     * Find the identifier of the meta-info for a member.
     *
     * @param mtype The category of members to be searched.
     * @param slot The slot of the required member.
     * @return The member identifer or -1 if it wasn't found.
     */
    public int lookupMemberID(int mtype, int slot) {
        SymbolParser p = SymbolParser.create(this);
        return p.lookupMemberID(mtype, slot);
    }

    /**
     * Construct a member ID table.
     *
     * @param proxySymbols The proxy relocator symbols
     * @param mtklass The class
     * @return The member ID table.
     */
    public static short[][] getMemberIDTable(StringOfSymbols proxySymbols, Klass klass) {
        StringOfSymbols symbols = klass.symbols;

        int count0 = proxySymbols.getMemberCount(SquawkConstants.MTYPE_FIRST);
        int count1 = proxySymbols.getMemberCount(SquawkConstants.MTYPE_FIRST+1);
        int count2 = proxySymbols.getMemberCount(SquawkConstants.MTYPE_FIRST+2);
        int count3 = proxySymbols.getMemberCount(SquawkConstants.MTYPE_FIRST+3);
        Romizer.assume(SquawkConstants.MTYPE_FIRST+3 == SquawkConstants.MTYPE_LAST);

       /*
        * If they are all zero then return a common empty array
        */
        if ((count0 + count1 + count2 + count3) == 0) {
            return (short[][])PersistentMemory.get("new short[MTYPE_LAST+1][]");
        }

        short[][] memberIDs = new short[SquawkConstants.MTYPE_LAST+1][];
        for (int mtype = SquawkConstants.MTYPE_FIRST ; mtype <= SquawkConstants.MTYPE_LAST ; mtype++) {
            int count = proxySymbols.getMemberCount(mtype);
            short[] entry;
            if (count == 0) {
                entry = (short[])PersistentMemory.get("new short[0]");
            } else {
                entry = new short[count];
            }
            memberIDs[mtype] = entry;
            for (int i = 0 ; i < count; i++) {
                memberIDs[mtype][i] = (short)symbols.lookupMemberID(proxySymbols, mtype, i, klass);
            }
        }
        return memberIDs;
    }

    /**
     * Find the identifier of the meta-info for a member.
     *
     * @param proxySymbols The proxy symbols
     * @param mtype The category of members to be searched.
     * @param slot The slot.
     * @return The member identifer or -1 if it wasn't found.
     */
    private int lookupMemberID(StringOfSymbols proxySymbols, int mtype, int slot, Klass klass) {
        SymbolParser proxyParser = SymbolParser.create(proxySymbols);
        SymbolParser thisParser  = SymbolParser.create(this);
        Romizer.assume(thisParser != proxyParser);

        int count = getMemberCount(mtype);
        int proxyMemberID = proxyParser.lookupMemberID(mtype, slot);
        Romizer.assume(proxyMemberID > 0);
        proxyParser.select(proxyMemberID);

        for (int i = 0 ; i < count ; i++) {
            int thisMemberID = thisParser.lookupMemberID(mtype, i);
            if (thisMemberID < 0) {
                break;
            }
            thisParser.select(thisMemberID);
            if (thisParser.equalTranslatedSelection(proxyParser)) {
                return thisMemberID;
            }
        }
        throw new LinkageError("NoSuch"+(isFieldMember(mtype) ? "Field" : "Method")+"Error: " + klass.getName()+'.'+proxyParser.getMemberName());
    }

    /**
     * Get the access flags of a member.
     * @param memberID The member's identifier as returned from lookupMemberID.
     * @return the access flags for the denoted member.
     */
    public int getMemberAccess(int memberID) {
        SymbolParser p = SymbolParser.create(this, memberID);
        return p.getMemberAccess();
    }

    /**
     * Get the offset of a member.
     *
     * @param memberID The member's identifier as returned from lookupMemberID.
     * @return the access flags for the denoted member.
     */
    public int getMemberOffset(int memberID) {
        SymbolParser p = SymbolParser.create(this, memberID);
        return p.getMemberOffset();
    }

    /**
     * Get the native method identifier of a native method.
     *
     * @param memberID The member's identifier as returned from lookupMemberID.
     * @return the native method identifier of this native method or 0 if this
     * is not a native method.
     */
    public int getMemberNativeMethodIdentifier(int memberID) {
        SymbolParser p = SymbolParser.create(this, memberID);
        return p.getMemberNativeMethodIdentifier();
    }

    /**
     * Get the name of a member.
     *
     * @param memberID The offset of the member in the symbols.
     * @return the member name
     */
    public String getMemberName(int memberID) {
        SymbolParser p = SymbolParser.create(this, memberID);
        return p.getMemberName();
    }

    /**
     * Get the length of name of a member.
     * @param memberID The offset of the member in the symbols.
     * @return the length of the name
     */
    public int getMemberNameLength(int memberID) {
        SymbolParser p = SymbolParser.create(this, memberID);
        return p.getMemberNameLength();
    }

    /**
     * Get a character in the name of a member.
     * @param memberID The offset of the member in the symbols.
     * @return the character.
     */
    public char getMemberNameChar(int memberID, int index) {
        SymbolParser p = SymbolParser.create(this, memberID);
        return p.getMemberNameChar(index);
    }

    /**
     * Get the type of a member.
     * @param memberID The offset of the member in the symbols.
     * @return the type of the denoted member.
     */
    public char getMemberType(int memberID) {
        SymbolParser p = SymbolParser.create(this, memberID);
        return (char)p.getMemberParm(0);
    }

    /**
     * Get a parameter of a member.
     * @param memberID The offset of the member in the symbols.
     * @return the type of the denoted member.
     */
    public char getMemberParm(int memberID, int index) {
        SymbolParser p = SymbolParser.create(this, memberID);
        return (char)p.getMemberParm(index+1);
    }

    /**
     * Get the number of parameters in a member.
     * @param memberID The offset of the member in the symbols.
     * @return the number of paramaters
     */
    public int getMemberParmLength(int memberID) {
        SymbolParser p = SymbolParser.create(this, memberID);
        return p.getMemberParmLength() - 1;
    }

    /**
     * Return a Member object containing the symbolic information for a member.
     * @param memberID The member's identifier as returned from lookupMemberID.
     */
    public Member getMember(int memberID) {
        SymbolParser p = SymbolParser.create(this, memberID);
        return p.getMember();
    }

    /**
     * Flush the cached segment parser objects
     */
    public static void flush() {
        SymbolParser.flush();
    }


/*if[DEBUG.LOADER]*/

    public static void printStats() {
        System.out.println();
        System.out.println();
        System.out.println("SymbolParser stats:");
        System.out.println();
        System.out.println("Creates "+SymbolParser.creates);
        System.out.println("Misses  "+SymbolParser.createMisses);
        System.out.println();
        System.out.println("Parses  "+SymbolParser.parses);
        System.out.println("Misses  "+SymbolParser.parseMisses);
        System.out.println();
        System.out.println("Selects "+SymbolParser.selects);
        System.out.println("Misses  "+SymbolParser.selectMisses);
    }

    /**
     * formatMembers
     */
    public static void dumpSymbols(StringOfSymbols symbols) {
        StringBuffer buf = new StringBuffer();
        buf.append("symbols: (class name: ").append(symbols.getClassName()).append(')');
        formatMembers(buf, symbols);
        System.err.println(buf.toString());
    }

    /**
     * formatMembers
     */
    public static void formatMembers(StringBuffer buf, StringOfSymbols symbols) {
        for (int mtype = SquawkConstants.MTYPE_FIRST ; mtype <= SquawkConstants.MTYPE_LAST ; mtype++) {
            buf.append("\n  section: " + mtype + " (" + MTYPE_NAMES[mtype] + ")");
            for (int slot = 0 ;; slot++) {
                int id = symbols.lookupMemberID(mtype, slot);
                if (id == -1) {
                    break;
                }
                buf.append("\n    (member " + slot + ", memberID=" + id + ")");
                symbols.formatMember(id, buf);
            }
        }
    }

    /**
     * formatMember
     */
    public void formatMember(int memberID, StringBuffer buf) {
        SymbolParser p = SymbolParser.create(this, memberID);
        p.formatMember(buf);
    }

    /**
     * formatMember
     */
    public String formatMember(int memberID) {
        SymbolParser p = SymbolParser.create(this, memberID);
        return p.formatMember();
    }

    /**
     * charToHex
     */
    public static String charToHex(char c) {
        return "0x" +
                hexDigit(c>>12) +
                hexDigit(c>>8) +
                hexDigit(c>>4) +
                hexDigit(c);
    }

    /**
     * intToHex
     */
    public static String intToHex(int i) {
        return "0x" +
                hexDigit(i>>28) +
                hexDigit(i>>24) +
                hexDigit(i>>20) +
                hexDigit(i>>16) +
                hexDigit(i>>12) +
                hexDigit(i>>8) +
                hexDigit(i>>4) +
                hexDigit(i);
    }

    /**
     * hexDigit
     */
    public static char hexDigit(int digit) {
        String hextable = "0123456789ABCDEF";
        return (char)hextable.charAt(digit&0xF);
    }

    /**
     * MTYPE_NAMES
     */
    public final static String[] MTYPE_NAMES = {
        "IF",
        "SF",
        "VM",
        "SM"
    };


    public void dump() {
        System.out.println("StringOfSymbols");
        for (int i = 0 ; i < length() ; i++) {
            int b = at(i) & 0xFF;
            String s = "    "+i;
            while (s.length() < 15) s += " ";
            s += ""+b;
            if (b > ' ' && b < '~') {
                while (s.length() < 25) s += " ";
                s += ""+(char)b;
            }
            System.out.println(s);
        }
    }


/*end[DEBUG.LOADER]*/


}


/*---------------------------------------------------------------------------*\
 *                              Symbol Parser                                *
\*---------------------------------------------------------------------------*/

class SymbolParser {

    /*-------------------------------*\
     *        Static cache           *
    \*-------------------------------*/

    static SymbolParser p1, p2;

/*if[DEBUG.LOADER]*/
    static int creates, createMisses, parses, parseMisses, selects, selectMisses;
/*end[DEBUG.LOADER]*/

    /**
     * Create a Segemnt parser
     */
    static SymbolParser create(StringOfSymbols sos) {
/*if[DEBUG.LOADER]*/
        creates++;
/*end[DEBUG.LOADER]*/
        if (p1 != null && p1.sos == sos) {
            return p1;
        }
        if (p2 == null) {
            p2 = new SymbolParser();
        }
        if (p2.sos != sos) {
/*if[DEBUG.LOADER]*/
            createMisses++;
/*end[DEBUG.LOADER]*/
            p2.setup(sos);
        }

       /*
        * Swap p1 and p2 so that the other one is replaced next time
        */
        SymbolParser temp = p2;
        p2 = p1;
        p1 = temp;
        return p1;
    }

    /**
     * Create a Segemnt parser and select member
     */
    static SymbolParser create(StringOfSymbols sos, int id) {
        SymbolParser res = create(sos);
        res.select(id);
        return res;
    }

    /**
     * Flush the cached segment parser objects
     */
    static void flush() {
        p1 = p2 = null;
    }


    /*-------------------------------*\
     *         Instance Data         *
    \*-------------------------------*/

    StringOfSymbols sos;
    short           pos;
    short           classNameLength;
    short           classNameStart;
    boolean         segmentsParsed;
    short[]         segmentStart = new short[SquawkConstants.MTYPE_LAST+1];
    short[]         segmentCount = new short[SquawkConstants.MTYPE_LAST+1];

    short           selection;
    int             access;
    int             offset;
    short           nameStart;
    short           parmStart;
    short           parmCount;


    /*-------------------------------*\
     *            Setup              *
    \*-------------------------------*/

    /**
     * Setup the parser for a string of symbols
     */
    private void setup(StringOfSymbols sos) {
        this.sos = sos;
        this.pos = 0;
        classNameLength = getShort();
        classNameStart  = pos;
        segmentsParsed  = false;
        selection       = -1;
    }

    /*-------------------------------*\
     *       Class name access       *
    \*-------------------------------*/

    /**
     * Test for class name equality
     * @return true if it is equal
     */
    boolean equalsClassName(String str, int skip) {
        int lth = classNameLength - skip;
        if (str.length() != lth) {
            return false;
        }
        int pos = classNameStart + skip;
        for (int i = 0 ; i < lth ; i++) {
            if (sos.at(pos++) != str.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test the class.
     * @return true if it is an array
     */
    boolean isArray() {
        return sos.at(classNameStart) == '[';
    }

    /**
     * Get the class name as a string
     * @return the name
     */
    String getClassName() {
        return sos.substring(classNameStart, classNameLength+1);
    }

    /**
     * Get the number of characters in the class name
     * @return the length
     */
    int getClassNameLength() {
        return classNameLength;
    }

    /**
     * Get a specific character in the class name
     * @return the char
     */
    char getClassNameChar(int pos) {
        Romizer.assume(pos < getClassNameLength());
        return sos.at(classNameStart + pos);
    }


    /*-------------------------------*\
     *        Integer parsing        *
    \*-------------------------------*/

    /**
     * Decode an unsigned integer from the current position
     * whose value can be encoded in 28 bits or less.
     * The value was encoded in as few bytes as possible with 7 bits of
     * value and one 1 bit of encoding over head per byte. All
     * encoded bytes but the last have a 1 in their most significant bit
     * to signify there are more bytes to follow. The last encoded
     * byte has a 0 in this bit.
     *
     * @return the decoded value.
     */
    int getInt() {
        int lo = sos.at(pos++);
        if (lo < 128) {
            return lo;
        }
        lo &= 0x7f;
        int mid = sos.at(pos++);
        if (mid < 128) {
            return ((mid << 7) + lo);
        }
        mid &= 0x7f;
        int hi = sos.at(pos++);
        if (hi < 128) {
            return ((hi << 14) + (mid << 7) + lo);
        }
        hi &= 0x7f;
        int last = sos.at(pos++);
        if (last < 128) {
            return ((last << 21) + (hi << 14) + (mid << 7) + lo);
        }
        throw new Error();
    }

    /**
     * Read an encoded short from the current position
     */
    short getShort() {
        int x = getInt();
        Romizer.assume(x >= 0 && x < 65536);
        return (short)x;
    }

    /**
     * Read an encoded type
     */
    int getType() {
/*
        int lo = sos.at(pos++);
        int hi = sos.at(pos++);
        return (hi << 8) + lo;
*/
        return getInt();
    }


    /*-------------------------------*\
     *        Segment parsing        *
    \*-------------------------------*/

    /**
     * Parse the member segments
     */
    private void parseSegments() {
/*if[DEBUG.LOADER]*/
        parses++;
/*end[DEBUG.LOADER]*/
        if (!segmentsParsed) {
/*if[DEBUG.LOADER]*/
            parseMisses++;
/*end[DEBUG.LOADER]*/
            segmentStart[SquawkConstants.MTYPE_INSTANCEFIELDS] = 0;
            segmentCount[SquawkConstants.MTYPE_INSTANCEFIELDS] = 0;
            segmentStart[SquawkConstants.MTYPE_STATICFIELDS]   = 0;
            segmentCount[SquawkConstants.MTYPE_STATICFIELDS]   = 0;
            segmentStart[SquawkConstants.MTYPE_VIRTUALMETHODS] = 0;
            segmentCount[SquawkConstants.MTYPE_VIRTUALMETHODS] = 0;
            segmentStart[SquawkConstants.MTYPE_STATICMETHODS]  = 0;
            segmentCount[SquawkConstants.MTYPE_STATICMETHODS]  = 0;
            int soslength = sos.length();
            pos = (short)(classNameStart + classNameLength);
            while (pos < soslength) {
                int seg = sos.at(pos);
                Romizer.assume(seg >= SquawkConstants.MTYPE_FIRST && seg <= SquawkConstants.MTYPE_LAST);
                pos++;
                segmentStart[seg] = pos;
                while(pos < soslength) {
                    int lth = sos.at(pos);
                    if (lth <= SquawkConstants.MTYPE_LAST) {
                        break;
                    }
                    lth = getShort();
                    pos += lth;
                    segmentCount[seg]++;
                }
            }
            segmentsParsed = true;
        }
    }

    /**
     * Count the number of members in a given section of members.
     * @param mtype The section of members to search.
     * @return the number of entries in a given section of members
     */
    public int getMemberCount(int mtype) {
        parseSegments();
        return segmentCount[mtype];
    }

    /**
     * Find the identifier of the meta-info for a member.
     * @param mtype The category of members to be searched.
     * @param slot The slot of the required member.
     * @return The member identifer or -1 if it wasn't found.
     */
    public int lookupMemberID(int mtype, int slot) {
        parseSegments();
        if (segmentCount[mtype] <= slot) {
            return -1;
        }
        pos = segmentStart[mtype];
        while (slot-- > 0) {
            int lth = getShort();
            pos += lth;
        }
        return pos;
    }


    /*-------------------------------*\
     *         Member access         *
    \*-------------------------------*/

    /**
     * Select a member
     * @param mtype The category of members to be searched.
     * @param slot The slot of the required member.
     */
    public void select(int mtype, int slot) {
        int id = lookupMemberID(mtype, slot);
        Romizer.assume(id > 0);
        select(id);
    }

    /**
     * Select a member
     *
     * @param pos member id.
     */
    public void select(int id) {
/*if[DEBUG.LOADER]*/
        selects++;
/*end[DEBUG.LOADER]*/
        if (selection != id) {
/*if[DEBUG.LOADER]*/
            selectMisses++;
/*end[DEBUG.LOADER]*/
            selection  = (short)id;
            pos        = (short)id;
            int lth    = getShort();
            int end    = pos + lth;
            access     = getInt();
            offset     = getInt();
            nameStart  = pos;
            int dash   = sos.indexOf('-', pos);
            parmStart  = (short)(dash + 1);
            pos = parmStart;
            parmCount = 0;
            while (pos < end) {
                getType();
                parmCount++;
                Romizer.assume(pos <= end);
            }

            pos = nameStart;
            int ch = sos.at(pos); // First character of name
            switch (ch) {
                case '<': {
                    if(posEquals("<init>-")) {
                        if (parmCount == 2) {
                            access |= SquawkConstants.ACC_DEFAULTINIT;
                        }
                    } else if (posEquals("<clinit>-")) {
                        if (getMemberParm(0) == CNO.VOID && parmCount == 1) {
                            access |= SquawkConstants.ACC_CLINIT;
                        }
                    }
                    break;
                }
                case 'm': {
                    if (posEquals("main-") &&
                        getMemberParm(0) == CNO.VOID &&
                        parmCount == 2 &&
                        getMemberParm(1) == CNO.STRING_ARRAY)
                    {
                        access |= SquawkConstants.ACC_MAIN;
                    }
                    break;
                }
            }
        }
    }

    /**
     * Check member is same as string
     * @param s string
     */
    boolean posEquals(String s) {
        int lth = s.length();
        int p = pos;
        for (int i = 0 ; i < lth ; i++) {
            if (s.charAt(i) != sos.at(p++)) {
                return false;
            }
        }
        return true;
    }

    /**
     * getMemberAccess
     */
    int getMemberAccess() {
        return access;
    }

    /**
     * getMemberOffset
     */
    int getMemberOffset() {
        return offset & 0xFFFF;
    }

    int getMemberNativeMethodIdentifier() {
        return offset >>> 16;
    }

    /**
     * getMemberName
     */
    String getMemberName() {
        return sos.substring(nameStart, parmStart - 1);
    }

    /**
     * getMemberNameLength
     */
    public int getMemberNameLength() {
        return parmStart - nameStart - 1;
    }

    /**
     * getMemberNameChar
     */
    public char getMemberNameChar(int index) {
        Romizer.assume(index < getMemberNameLength());
        return sos.at(nameStart + index);
    }

    /**
     * getMemberParmLength
     */
    public int getMemberParmLength() {
        return parmCount;
    }

    /**
     * getMemberParm
     */
    public int getMemberParm(int index) {
        Romizer.assume(index >= 0);
        Romizer.assume(index < parmCount);
        pos = parmStart;
        int res = -1;
        while (index-- >= 0) {
            res = getType();
        }
        return res;
    }


    /**
     * equalTranslatedSelection
     */
    boolean equalTranslatedSelection(SymbolParser proxyParser) {
        return sameName(proxyParser) && sameParms(proxyParser);
    }

    /**
     * sameName
     */
    private boolean sameName(SymbolParser pp) {
        int lth = getMemberNameLength();
        if (lth != pp.getMemberNameLength()) {
            return false;
        }
        for (int i = 0 ; i < lth ; i++) {
            int ch = getMemberNameChar(i);
            if (ch != pp.getMemberNameChar(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * sameParms
     */
    private boolean sameParms(SymbolParser pp) {
        SuiteLoader loader = SuiteLoader.theLoader;
        int lth = getMemberParmLength();
        if (lth != pp.getMemberParmLength()) {
            return false;
        }
        for (int i = 0 ; i < lth ; i++) {
            int t1 =    getMemberParm(i);
            int t2 = pp.getMemberParm(i);
            if (t2 != loader.findSuiteTypeForSystemType(t1)) {
                return false;
            }
        }
        return true;
    }

    /**
     * getMember
     */
    Member getMember() {
        int type = getMemberParm(0);
        return Member.create(sos, selection, type, access, offset&0xFFFF, offset >>> 16);
    }


/*if[DEBUG.LOADER]*/
    /**
     * formatMember
     */
    public void formatMember(StringBuffer buf) {
        buf.append("\n      access: 0x" + Integer.toHexString(getMemberAccess()));
        buf.append("\n      offset: " + getMemberOffset());
        buf.append("\n      name:   " + getMemberName());
        buf.append("\n      type:   " + getMemberParm(0));
        if (getMemberParmLength() > 1) {
            buf.append("\n      parms:  ");
            for (int i = 1 ; i < getMemberParmLength() ; i++) {
                buf.append("\n                 " + getMemberParm(i));
            }
        }

    }

    /**
     * formatMember
     */
    public String formatMember() {
        StringBuffer buf = new StringBuffer();
        formatMember(buf);
        return buf.toString();
    }
/*end[DEBUG.LOADER]*/

}
