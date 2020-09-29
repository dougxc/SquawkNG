
package com.sun.squawk.translator.loader;

import java.util.Vector;
import java.util.Enumeration;
import com.sun.squawk.util.*;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.ir.*;
import com.sun.squawk.translator.util.*;

import java.io.IOException;

/**
 * The Liveness class represents the Code attribute describing the liveness of
 * variables at basic block boundaries. It has the following format:
 *
 * Liveness_attribute {
 *     u2 attribute_name_index
 *     u4 attribute_length
 *     u2 number_of_entries // is always even = # of basic blocks * 2
 *     {
 *         u2 address                     // the BB entry/exit instruction to which this annotation is attached
 *         u1 liveness_bitset[max_locals] // a bitset representing liveness info. LSBs are smaller numbers,
 *                                        // i.e. bit 0 is in the LSB, bit 7 is in the MSB.
 *     } entries[number_of_entries]
 */
public class Liveness {

    /**
     * This class represents the type maps for a single instruction address.
     */
    public static class Entry {
        /** Indicates if this is the entry for the start of a BB or end. */
        public final boolean isBBEntry;
        /** The bytecode offset. */
        public final int address;
        /** The types on the locals stack where all types are one logical entry. */
        public final BitSet livenessSet;

        /**
         * Constructor.
         */
        public Entry(boolean isBBEntry, int address, BitSet livenessSet) {
            this.isBBEntry   = isBBEntry;
            this.address     = address;
            this.livenessSet = livenessSet;
        }

        public boolean isLive(int javacIndex) {
            return (livenessSet.get(javacIndex));
        }
    }

    /** The enclosing method. */
    private final Method method;
    /** Enclosing translator (convenient short-cut for method.parent().getVM()). */
    final private Translator vm;
    /** A map of BB entry addresses to the liveness map indicating the live locals on entry to the BB. */
    private IntHashtable bbEntries;
    private Vector       bbEntriesSorted;
    /** A map of BB exit addresses to the liveness map indicating the live locals on exit from the BB. */
    private IntHashtable bbExits;
    private Vector       bbExitsSorted;

/*==============================================================================
 * Constructors
 *============================================================================*/

    /**
     * Construct a Liveness from a classfile input stream.
     * @param in The classfile input stream at the point where the liveness atribute starts.
     * @param method
     */
    public Liveness(ClassFileInputStream in, Method method, int maxLocals) throws LinkageException {
        this.method  = method;
        this.vm      = in.getVM();

        // Read number_of_entries
        int nentries  = in.readUnsignedShort("liv-nentries");
        if ((nentries % 2) != 0) {
            throw in.classFormatError("Liveness number_of_entries must be even");
        }
        bbEntries       = new IntHashtable(nentries/2);
        bbEntriesSorted = new Vector(nentries/2);
        bbExits         = new IntHashtable(nentries/2);
        bbExitsSorted   = new Vector(nentries/2);

        int lastAddress = -1;
        for (int i = 0 ; i < nentries ; i++) {
            int address = in.readUnsignedShort("liv-address");
            if (address < lastAddress) {
                throw in.classFormatError("Liveness ip addresses not in order. address="+
                                                     address+" lastAddress="+lastAddress);
            }

            byte[] livenessSetData = new byte[(maxLocals+7)/8];
            for (int j = 0; j != livenessSetData.length; j++) {
                livenessSetData[j] = (byte)in.readUnsignedByte("liv-liveness_bitset");
            }
            BitSet livenessSet = BitSet.fromByteArray(livenessSetData);

            if ((i % 2) == 0) {
                Entry entry = new Entry(true, address, livenessSet);
                bbEntries.put(address, entry);
                bbEntriesSorted.addElement(entry);
            }
            else {
                Entry entry = new Entry(false, address, livenessSet);
                bbExits.put(address, entry);
                bbExitsSorted.addElement(entry);
            }
        }
    }

/*==============================================================================
 * Accessor methods
 *============================================================================*/

    /**
     * Return the number of entries.
     */
    public int size() {
        return bbEntries.size();
    }

    /**
     * Return the liveness set for a BB entry.
     */
    public Entry lookupBBEntry(int ip) {
        return (Entry)bbEntries.get(ip);
    }

    /**
     * Return the liveness set for a BB exit.
     */
    public Entry lookupBBExit(int ip) {
        return (Entry)bbExits.get(ip);
    }

    public boolean isLive(int ip, int javacIndex, boolean isBBEntry) {
        Entry entry = isBBEntry ? lookupBBEntry(ip) : lookupBBExit(ip);
        Assert.that(entry != null);
        return entry.isLive(javacIndex);
    }

}
