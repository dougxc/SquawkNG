package java.lang;

import java.io.*;
//import java.lang.reflect.*;
import java.util.*;
import com.sun.squawk.loader.*;
import com.sun.squawk.suite.*;
import com.sun.squawk.vm.*;
import com.sun.squawk.translator.suite.VMAccessedAttribute;

/**
 * This class extends the BinarySuiteParser to enables parsing of
 * "VMAccessed" class attributes and "MethodDebug" method implementation
 * attributes.
 */
public final class RomizingBinarySuiteParser extends com.sun.squawk.suite.BinarySuiteParser {

    private final GeneratedObjectLayoutVerifier golv;

    /**
     * Create a new binary attribute parser.
     *
     * @param in The input stream from which to load the suite(s).
     * @param golv The GeneratedObjectLayoutVerifier instance used to process the
     * classes with a VMAccessed attribute.
     */
    RomizingBinarySuiteParser(InputStream in, GeneratedObjectLayoutVerifier golv) {
        super(in);
        this.golv = golv;
    }

    /**
     * Read a 'Type_info' structure from the stream.
     *
     * @return a SuiteClass representing the data in the structure read or
     * or null if there is no 'Type_info' structure at the current
     * position in the input stream.
     * @throws IOException
     */
    public SuiteClass readClass() throws IOException {
        SuiteClass sc = super.readClass();
        if (sc != null) {
            golv.addClass(sc);
        }
        return sc;
    }

    /**
     * Read a sequence of 'Attribute_info' structures from the stream that are
     * associated with a given class. This parser recognizes the "VMAccessed"
     * class attribute.
     *
     * @param sc The class to which the attributes pertain.
     * @throws IOException
     */
    public void readClassAttributes(SuiteClass sc) throws IOException {
        // Read 'attributes_count'
        int attributes_count = in.readUnsignedShort();
        while (attributes_count-- != 0) {
            String attribute_name = in.readUTF();
            int attribute_length = in.readInt();
            if (attribute_name.equals("VMAccessed")) {
                golv.addClassWithVMAccessedAttribute(sc, parseVMAccessedAttribute(in));
            } else {
                in.skipBytes(attribute_length);
            }
        }
    }

    /**
     * Read a sequence of 'Attribute_info' structures from the stream that are
     * associated with a given method implementation. This parser recognizes
     * the "MethodDebug" method implementation attribute.
     *
     * @param methodImpl The method implementation to which the
     * attributes pertain.
     * @throws IOException
     */
    public void readMethodImplAttributes(SuiteMethodImpl methodImpl) throws IOException {
        if ((methodImpl.flags & SquawkConstants.ACC_ATTRIBUTES) != 0) {
            int attributes_count = in.readUnsignedShort();
            while (attributes_count-- != 0) {
                String attribute_name = in.readUTF();
                int attribute_length = in.readInt();
/*if[DEBUG.METHODDEBUGTABLE]*/
                if (attribute_name.equals("MethodDebug")) {
                    parseMethodDebugAttribute(in);
                } else
/*end[DEBUG.METHODDEBUGTABLE]*/
                {
                    in.skipBytes(attribute_length);
                }
            }
        }
    }

/*if[DEBUG.METHODDEBUGTABLE]*/
    /**
     * Parse the 'MethodDebug' method attribute which has the following format:
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
     *
     * @param method
     * @return
     */
    private void parseMethodDebugAttribute(DataInputStream in) throws IOException {
        String filePath = in.readUTF();
        String signature = in.readUTF();
        int lineNumberTableLength = in.readUnsignedShort();
        MethodDebugTable.Method method = new MethodDebugTable.Method(filePath, signature, lineNumberTableLength);
        for (int i = 0; i != lineNumberTableLength; ++i) {
            int startPc = in.readUnsignedShort();
            int lineNumber = in.readUnsignedShort();
            method.addLineNumberTableItem(i, startPc, lineNumber);
        }

        int methodID = MethodDebugTable.getNextAvailableMethodID();
        MethodDebugTable.putMethodDebug(methodID, method);
    }
/*end[DEBUG.METHODDEBUGTABLE]*/

    /**
     * Parse the 'VMAccessed' class attribute which has the following format:
     *
     * VMAccessed_attribute {
     *     CONSTANT_Utf8_info attribute_name; // "VMAccessed"
     *     u4 attribute_length;
     *     u2 class_access_flags;
     *     u2 instance_fields_table_length;
     *     {
     *         u2 slot;
     *         u2 access_flags;
     *     } instance_fields_table[instance_fields_table_length];
     *     u2 static_methods_table_length;
     *     {
     *         u2 slot;
     *         u2 access_flags;
     *     } static_methods_table[static_methods_table_length];
     * }
     *
     * @param method
     * @return
     */
    private VMAccessedAttribute parseVMAccessedAttribute(DataInputStream in) throws IOException {
        int classAccessFlags = in.readUnsignedShort();
        VMAccessedAttribute attribute = new VMAccessedAttribute(classAccessFlags);
        int instanceFieldsTableLength = in.readUnsignedShort();
        for (int i = 0; i != instanceFieldsTableLength; ++i) {
            attribute.addInstanceFieldsTableItem(in.readUnsignedShort(), in.readUnsignedShort());
        }
        int staticMethodsTableLength = in.readUnsignedShort();
        for (int i = 0; i != staticMethodsTableLength; ++i) {
            attribute.addStaticMethodsTableItem(in.readUnsignedShort(), in.readUnsignedShort());
        }
        return attribute;
    }
}