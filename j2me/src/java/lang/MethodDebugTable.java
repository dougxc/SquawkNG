//if[DEBUG.METHODDEBUGTABLE]
package java.lang;

import java.io.*;
import java.util.*;
import com.sun.squawk.util.IntHashtable;

/**
 * This is the repository of debug info for the methods in the image.
 */
public class MethodDebugTable {

    private MethodDebugTable() {
    }

    /**
     * The system wide table storing the debug info for all the romized
     * methods.
     */
    static IntHashtable table = Native.getMethodDebugTable();

    /**
     * Return the next available method identifier.
     *
     * @return the next available method identifier.
     */
    public static int getNextAvailableMethodID() {
        return table.size();
    }

    /**
     * Get the debug info for a method based on a given method ID.
     *
     * @param methodID
     * @return
     */
    public static Method getMethodDebug(int methodID) {
        return (Method)table.get(methodID);
    }

    /**
     * Set the debug info for a method.
     *
     * @param methodID
     * @param method
     */
    public static void putMethodDebug(int methodID, Method method) {
        Method m = (Method)table.put(methodID, method);
        if (m != null) {
            throw new RuntimeException("Can't overwrite debug info for method: "+m.signature);
        }
    }

/*if[SYSTEM.STREAMS]*/
    /**
     * Dump the table of debug info entries to a given print stream.
     *
     * @param out
     */
    public static void dump(PrintStream out) {
        for (int methodID = 0; methodID != table.size(); ++methodID) {
            out.println(methodID);
            ((Method)table.get(methodID)).dump(out);
        }
    }
/*end[SYSTEM.STREAMS]*/

    /**
     * This class enscapsulates the debug info for a single method.
     */
    public final static class Method {

        /**
         * The file path for the source file declaring the method. This is the
         * path corresponding to the fully qualified name of the method's class.
         */
        final public String filePath;

        /**
         * This is the signature of the method. It's in a source code like format.
         */
        final public String signature;

        final private int[] startPCs;
        final private int[] lineNumbers;

        /**
         * Create a new attribute that contains the bytecode address to source
         * line number mapping for a method.
         *
         * @param filePath
         * @param signature
         * @param lineNumberTableLength
         */
        public Method(String filePath, String signature, int lineNumberTableLength) {
            this.filePath = filePath == null ? "" : filePath;
            this.signature = signature;
            this.startPCs = new int[lineNumberTableLength];
            this.lineNumbers = new int[lineNumberTableLength];
        }

        /**
         * Add an entry to the line number table.
         *
         * @param index
         * @param startPc
         * @param lineNumber
         */
        public void addLineNumberTableItem(int index, int startPc,
                                           int lineNumber) {
            startPCs[index] = startPc;
            lineNumbers[index] = lineNumber;
        }

        /**
         * Get the source line number for a given instruction address.
         *
         * @param pc an instruction address
         * @return the source line number for 'pc' or -1 if there is no
         * source line number recorded for this address.
         */
        public int getSourceLineFor(int pc) {
            int index = 0;
            int lineNumber = -1;
            for (int i = 0; i != startPCs.length; ++i) {
                int startPc = startPCs[i];
                if (pc < startPc) {
                    break;
                }
                lineNumber = lineNumbers[i];
            }
            return lineNumber;
        }

/*if[SYSTEM.STREAMS]*/
        /**
         * Dump a representation of this object to a print stream.
         *
         * @param out
         */
        public void dump(PrintStream out) {
            out.println(filePath);
            out.println(signature);
            for (int i = 0; i != startPCs.length; i++) {
                out.print(startPCs[i]+" "+lineNumbers[i]+" ");
            }
            out.println();
        }
/*end[SYSTEM.STREAMS]*/

    }
}