package com.sun.squawk.translator.suite;

import java.util.Vector;
import java.util.Enumeration;
import com.sun.squawk.translator.Assert;

/**
 * This class represents the optional attribute for a method body in a
 * suite that can be used by a VM to correlate an instruction with a
 * line in an original source file.
 *
 * The serialized binary format for the attribute is:
 *
 * MethodDebug_attribute {
 *     CONSTANT_Utf8_info attribute_name; // "MethodDebug"
 *     u4 attribute_length;
 *     CONSTANT_Utf8_info file_path;
 *     CONSTANT_Utf8_info signature;
 *     u2 line_number_table_length;
 *     {
 *         u2 start_pc;
 *         u2 line_number;
 *     } line_number_table[line_number_table_length];
 * }
 *
 * The 'start_pc' item indicates the instruction address at which the code
 * for a new source line begins. The 'line_number' item is the line
 * number for the new source line.
 */
public final class MethodDebugAttribute extends SuiteAttribute {

    /**
     * This class encapsulates a single entry in the 'line_number_table'.
     */
    public static final class LNTItem {
        /**
         * The instruction address at which the code for the line at
         * 'lineNumber' in the source file begins
         */
        public final int startPc;

        /**
         * The source line number corresponding to 'startPc'.
         */
        public final int lineNumber;

        private LNTItem(int startPc, int lineNumber) {
            this.startPc = startPc;
            this.lineNumber = lineNumber;
        }
    }

    /**
     * The file path for the source file declaring the method. This is the
     * path corresponding to the fully qualified name of the method's class.
     */
    final public String filePath;

    /**
     * This is the signature of the method. It's in a source code like format.
     */
    final public String signature;

    /**
     * Create a new attribute that contains the bytecode address to source
     * line number mapping for a method.
     *
     * @param filePath
     * @param signature
     */
    public MethodDebugAttribute(String filePath, String signature) {
        this.filePath = filePath == null ? "" : filePath;
        this.signature = signature;
        this.lineNumberTable = new Vector();
    }

    /**
     * Return the name of the attribute.
     *
     * @return "MethodDebug".
     */
    public String getAttributeName() {
        return "MethodDebug";
    }

    /**
     * Add a new entry to the line number table. The entry is only added if
     * the table is empty or if 'lineNumber' is greater than the line number
     * of the last entry.
     *
     * @param startPc
     * @param lineNumber
     */
    public void addLineNumberTableItem(int startPc, int lineNumber) {
        int lastLineNumber = -1;
        if (!lineNumberTable.isEmpty()) {
            LNTItem item = (LNTItem)lineNumberTable.lastElement();
            lastLineNumber = item.lineNumber;
            Assert.that(item.startPc < startPc);
        }
        if (lastLineNumber < lineNumber) {
            lineNumberTable.addElement(new LNTItem(startPc, lineNumber));
        }
    }

    /**
     * Return the size of the line number table. This is the number of entries
     * in the table.
     *
     * @return the size of the line number table
     */
    public int getLineNumberTableSize() {
        return lineNumberTable.size() ;
    }

    /**
     * Return an enumeration of the entries in the line number table.
     *
     * @return an enumeration of the entries in the line number table.
     */
    public Enumeration getLineNumberTableEntries() {
        return lineNumberTable.elements();
    }

    final private Vector lineNumberTable;
}