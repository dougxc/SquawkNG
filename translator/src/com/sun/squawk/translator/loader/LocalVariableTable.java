package com.sun.squawk.translator.loader;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.*;

/**
 * The LocalVariableTable class represents a LocalVariableTable attribute.
 */

public class LocalVariableTable {

    /** A single entry in the table. */
    public static class Entry {
        public final int startPC;
        public final int length;
        public final String name;
        public final Type type;
        public final int index;
        public Entry(int startPC, int length, String name, Type type, int index) {
            this.startPC = startPC;
            this.length  = length;
            this.name    = name;
            this.type    = type;
            this.index   = index;
        }
        public static final Comparer COMPARER = new Comparer() {
            private boolean encloses(Entry e1, Entry e2) {
                return (e1.startPC <= e2.startPC) && (e1.startPC+e1.length >= e2.startPC+e2.length);
            }
            public int compare(Object o1, Object o2) {
                if (o1 == o2) {
                    return 0;
                }
                Entry e1 = (Entry)o1;
                Entry e2 = (Entry)o2;
                if (e1.index < e2.index) {
                    return -1;
                }
                else if (e1.index > e2.index) {
                    return 1;
                }
                else {
                    if (encloses(e1, e2) || encloses(e2, e1)) {
                        return 0;
                    }
                    else if (e1.startPC < e2.startPC) {
                        return -1;
                    }
                    else {
                        Assert.that(e1.startPC > e2.startPC);
                        return 1;
                    }
                }
            }
        };
    }

    public static final Entry NO_ENTRY = new Entry(-1, -1, null, null, -1);

    private final Entry[] entries;

    public LocalVariableTable(Entry[] entries) {
        this.entries = entries;
        // Sort the entries
        Arrays.sort(entries, Entry.COMPARER);
    }

    public LocalVariableTable(ClassFileInputStream in, ConstantPool pool, int codeLength) throws LinkageException {
        Translator vm = in.getVM();
        int localVariableTableLength = in.readUnsignedShort("lvt-localVariableTableLength");
        entries = new LocalVariableTable.Entry[localVariableTableLength];
        for (int k = 0 ; k < localVariableTableLength ; k++) {
            int start_pc = in.readUnsignedShort("lvt-startPC");
            if (start_pc >= codeLength) {
                throw in.classFormatError("start_pc of LocalVariableTable is out of range");
            }
            int length = in.readUnsignedShort("lvt-length");
            if (start_pc+length > codeLength) {
                throw in.classFormatError("start_pc+length of LocalVariableTable is out of range");
            }
            String name = pool.getUtf8(in.readUnsignedShort("lvt-nameIndex"));
            Type type   = pool.findOrCreateType(pool.getUtf8(in.readUnsignedShort("lvt-descriptorIndex")));
            int index   = in.readUnsignedShort("lvt-index");
            entries[k] = new LocalVariableTable.Entry(start_pc, length, name, type, index);
        }
    }

    public Entry findEntry(int ip, int javacIndex) {
        Entry key = new Entry(ip, 0, null, null, javacIndex);
        int index = Arrays.binarySearch(entries, key, Entry.COMPARER);
        if (index < 0) {
            return NO_ENTRY;
        }
        return entries[index];
    }

    public static Entry getEntry(LocalVariableTable lvt, int ip, int javacIndex) {
        if (lvt == null) {
            return NO_ENTRY;
        }
        return lvt.findEntry(ip, javacIndex);
    }
}
