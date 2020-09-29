package java.lang;

import java.io.*;
//import java.lang.reflect.*;
import java.util.StringTokenizer;
import com.sun.squawk.suite.SuiteClass;
import com.sun.squawk.suite.SuiteMethodImpl;
import com.sun.squawk.xml.Tag;
import com.sun.squawk.translator.suite.VMAccessedAttribute;
import com.sun.squawk.vm.SquawkConstants;

/**
 * This class extends the XMLSuiteParser to enables parsing of
 * "VMAccessed" class attributes and "MethodDebug" method implementation
 * attributes.
 */
public final class RomizingXMLSuiteParser extends com.sun.squawk.suite.XMLSuiteParser {

    private final GeneratedObjectLayoutVerifier golv;

    /**
     * Create a new XML attribute parser.
     *
     * @param in The input stream from which to load the suite(s).
     * @param golv The GeneratedObjectLayoutVerifier instance used to process the
     * classes with a VMAccessed attribute.
     */
    RomizingXMLSuiteParser(InputStream in, GeneratedObjectLayoutVerifier golv) {
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
        Tag[] attributes = super.getClassAttributes();
        if (attributes != null) {
            for (int i = 0; i != attributes.length; ++i) {
                Tag tag = attributes[i];
                String name = parseName(tag);
                tag = tag.getNext();
                if (name.equals("VMAccessed")) {
                    golv.addClassWithVMAccessedAttribute(sc, parseVMAccessedAttribute(tag));
                }
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
/*if[DEBUG.METHODDEBUGTABLE]*/
        Tag[] attributes = super.getMethodImplAttributes();
        if (attributes != null) {
            for (int i = 0; i != attributes.length; ++i) {
                Tag tag = attributes[i];
                String name = parseName(tag);
                tag = tag.getNext();
                if (name.equals("MethodDebug")) {
                    parseMethodDebugAttribute(tag);
                }
            }
        }
/*end[DEBUG.METHODDEBUGTABLE]*/
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
    private void parseMethodDebugAttribute(Tag tag) throws IOException {
        MethodDebugTable.Method method;

        tag.checkName("filePath");
        String filePath = Tag.unescape(tag.getContent().getData());
        tag = tag.getNext();

        tag.checkName("signature");
        String signature = Tag.unescape(tag.getContent().getData());
        tag = tag.getNext();

        tag.checkName("lineNumberTable");
        Tag content = tag.getContent();
        if (content != null) {
            int length = Tag.countTags(content, "item");
            method = new MethodDebugTable.Method(filePath, signature, length);
            for (int i = 0; i != length; i++) {
                Tag item = content.getContent();
                int startPc = parseInt(item, "startPc");
                item = item.getNext();
                int lineNumber = parseInt(item, "lineNumber");
                method.addLineNumberTableItem(i, startPc, lineNumber);
                content = content.getNext();
            }
        } else {
            method = new MethodDebugTable.Method(filePath, signature, 0);
        }
        tag = tag.getNext();

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
    private VMAccessedAttribute parseVMAccessedAttribute(Tag tag) throws IOException {
        tag.checkName("access");
        char classAccessFlags = 0;
        Tag acc = tag.getContent();
        while (acc != null) {
            if (acc.getName().equals("eeprom")) {
                classAccessFlags |= SquawkConstants.VMACC_EEPROM;
            }
            acc = acc.getNext();
        }
        tag = tag.getNext();

        VMAccessedAttribute attribute = new VMAccessedAttribute(classAccessFlags);
        parseVMAccessedInstanceFieldsTable(tag, attribute);
        tag = tag.getNext();
        parseVMAccessedStaticMethodsTable(tag, attribute);
        tag = tag.getNext();
        return attribute;
    }

    /**
     * Parse the 'instance_fields_table' component of a VMAccessed attribute.
     *
     * @param tag
     * @param attribute
     */
    private void parseVMAccessedInstanceFieldsTable(Tag tag, VMAccessedAttribute attribute) {
        tag.checkName("instanceFieldsTable");
        Tag fields = tag.getContent();
        int instanceFieldsTableLength = Tag.countTags(fields, "field");
        for (int i = 0; i != instanceFieldsTableLength; ++i) {
            Tag content = fields.getContent();
            int slot = parseInt(content, "slot");
            content = content.getNext();

            content.checkName("access");
            Tag acc = content.getContent();
            int accessFlags = 0;
            while (acc != null) {
                if (acc.getName().equals("unsigned")) {
                    accessFlags |= SquawkConstants.VMACC_UNSIGNED;
                } else if (acc.getName().equals("wbopaque")) {
                    accessFlags |= SquawkConstants.VMACC_WBOPAQUE;
                } else if (acc.getName().equals("read")) {
                    accessFlags |= SquawkConstants.VMACC_READ;
                } else if (acc.getName().equals("write")) {
                    accessFlags |= SquawkConstants.VMACC_WRITE;
                }
                acc = acc.getNext();
            }

            attribute.addInstanceFieldsTableItem(slot, accessFlags);
            fields = fields.getNext();
        }
    }

    /**
     * Parse the 'static_methods_table' component of a VMAccessed attribute.
     *
     * @param tag
     * @param attribute
     */
    private void parseVMAccessedStaticMethodsTable(Tag tag, VMAccessedAttribute attribute) {
        tag.checkName("staticMethodsTable");
        Tag methods = tag.getContent();
        int staticMethodsTableLength = Tag.countTags(methods, "method");
        for (int i = 0; i != staticMethodsTableLength; ++i) {
            Tag content = methods.getContent();
            int slot = parseInt(content, "slot");
            content = content.getNext();

            content.checkName("access");
            Tag acc = content.getContent();
            int accessFlags = 0;
            while (acc != null) {
                if (acc.getName().equals("call")) {
                    accessFlags |= SquawkConstants.VMACC_CALL;
                }
                acc = acc.getNext();
            }

            attribute.addStaticMethodsTableItem(slot, accessFlags);
            methods = methods.getNext();
        }
    }

}