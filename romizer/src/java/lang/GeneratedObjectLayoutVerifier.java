package java.lang;

import java.util.Vector;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.SortedMap;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Comparator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.sun.squawk.suite.SuiteClass;
import com.sun.squawk.loader.Member;
import com.sun.squawk.translator.suite.VMAccessedAttribute;
import com.sun.squawk.vm.CNO;
import com.sun.squawk.vm.SquawkConstants;
import com.sun.squawk.vm.GeneratedObjectLayout;

/**
 * This class verifies that the VM source file "GeneratedObjectLayout.java"
 * generated statically is still valid.
 */
public class GeneratedObjectLayoutVerifier {

    /**
     * A Comparator that orders SuiteClass's by their names.
     */
    private static Comparator SUITE_CLASS_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            if (o1 == o2) {
                return 0;
            }
            SuiteClass sc1 = (SuiteClass)o1;
            SuiteClass sc2 = (SuiteClass)o2;
            return sc1.getName().compareTo(sc2.getName());
        }
    };

    private void verify(boolean condition, String msg) {
        if (!condition) {
            System.err.println("********* WARNING *************");
            System.err.println("Verification of GeneratedObjectLayout.java failed:");
            System.err.println();
            System.err.println("    "+msg);
            System.err.println();
            System.err.println("This most likely means that a conditional feature");
            System.err.println("(such as FLOATS) has been enabled/disable since the");
            System.err.println("last complete build. A complete rebuild should fix");
            System.err.println("the inconsistency.");
            System.err.println("*******************************");
            throw new RuntimeException();
        }
    }

    /**
     * This is the path to the VM source file that will be generated.
     */
    static private final String GEN_FILE_PATH = "vm/src/com/sun/squawk/vm/GeneratedObjectLayout.java";

    /**
     * The set of suite classes parsed.
     */
    private SortedSet suiteClasses = new TreeSet(SUITE_CLASS_COMPARATOR);

    /**
     * The sorted map of suite classes that have "VMAccessed" attribute to
     * those attributes.
     */
    private SortedMap suiteClassesWithVMAccessedAttributes = new TreeMap(SUITE_CLASS_COMPARATOR);

    /**
     * Add a suite class to the set of parsed suite classes. The class is
     * only added if it is a non-proxy class.
     *
     * @param sc a non-proxy class.
     */
    void addClass(SuiteClass sc) {
        if ((sc.getAccess() & SquawkConstants.ACC_PROXY) == 0) {
            suiteClasses.add(sc);
        }
    }

    /**
     * Add a <SuiteClass, VMAccessedAttribute> pair to the classes for which
     * accessors will be generated.
     *
     * @param sc
     * @param attribute
     */
    void addClassWithVMAccessedAttribute(SuiteClass sc, VMAccessedAttribute attribute) {
        suiteClassesWithVMAccessedAttributes.put(sc, attribute);
    }

    /**
     * Return a map that maps Klass instances to the corresponding SuiteClass
     * instances for all the suite classes registered with this object via the
     * 'addClass' method.
     *
     * @return
     */
    Map getSuiteClasses() {
        Map klasses = new HashMap();
        for (Iterator iterator = suiteClasses.iterator(); iterator.hasNext();) {
            SuiteClass sc = (SuiteClass)iterator.next();
            Klass klass = SuiteManager.forName(sc.getName());
            klasses.put(klass, sc);
        }
        return klasses;
    }

    /**
     * Verify the generated accessor methods and native method identifiers.
     */
    void verify() {
        verifyNativeMethodIdentifiers();
        verifyFieldAccessors();
        verifyVMExtensionOffsets();
    }

    /**
     * Verify the declarations of the native method identifiers.
     */
    private void verifyNativeMethodIdentifiers() {

        for (Iterator iterator = suiteClasses.iterator(); iterator.hasNext();) {
            SuiteClass sc = (SuiteClass)iterator.next();

            StringOfSymbols symbols = sc.symbols;
            String className = sc.getName();

            // Ignore non-system classes.
            if (!isSystemClass(className)) {
                continue;
            }

            verifyNativeMethodIdentifiers(className, symbols, SquawkConstants.MTYPE_VIRTUALMETHODS);
            verifyNativeMethodIdentifiers(className, symbols, SquawkConstants.MTYPE_STATICMETHODS);

        }
    }

    /**
     * Verify the declarations of the native method identifiers
     * for a given class.
     *
     * @param className
     * @param symbols
     * @param mtype
     */
    private void verifyNativeMethodIdentifiers(String className,
                                               StringOfSymbols symbols,
                                               int mtype) {

        int count = symbols.getMemberCount(mtype);
        if (count != 0) {
            for (int i = 0; i != count; ++i) {
                int id = symbols.lookupMemberID(mtype, i);
                Member method = symbols.getMember(id);
                String methodName = method.name();
                int identifier = method.nativeMethodIdentifier();
                if (method.isNative()) {
                    int oldIdentifier = GeneratedObjectLayout.getNativeMethodIdentifierFor(className, methodName);
                    verify(oldIdentifier == identifier,
                        "Native method identifier for "+className+'.'+methodName+
                        " has changed (old="+oldIdentifier+", new="+identifier+")");
                }
            }
        }
    }

    /**
     * Verify the declarations of the field accessors.
     */
    private void verifyFieldAccessors() {
        for (Iterator iterator = suiteClassesWithVMAccessedAttributes.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry)iterator.next();
            SuiteClass sc = (SuiteClass)entry.getKey();
            VMAccessedAttribute attribute = (VMAccessedAttribute)entry.getValue();

            String className = sc.getName();

            // Ignore non-system classes.
            if (!isSystemClass(className)) {
                continue;
            }

            StringOfSymbols symbols = sc.symbols;

            int fieldCount = attribute.getInstanceFieldsTableSize();
            if (fieldCount != 0) {

                for (Enumeration e = attribute.getInstanceFieldsTableEntries(); e.hasMoreElements(); ) {
                    VMAccessedAttribute.Item item = (VMAccessedAttribute.Item)e.nextElement();
                    int id = symbols.lookupMemberID(SquawkConstants.MTYPE_INSTANCEFIELDS, item.slot);
                    int type = symbols.getMemberType(id);

                    // Skip the field representing the second word of a long or double
                    if (type == CNO.DOUBLE2 || type == CNO.LONG2) {
                        continue;
                    }

                    String fieldName = symbols.getMemberName(id);
                    int offset = symbols.getMemberOffset(id);
                    int oldAttributesAndOffset = GeneratedObjectLayout.getInstanceFieldAttributesAndOffset(className, fieldName);
                    int oldAttributes = oldAttributesAndOffset >>> 16;
                    int oldOffset = oldAttributesAndOffset & 0xFFFF;

                    verify(oldAttributes == item.accessFlags,
                           "VMAccessed flags for "+className+'.'+fieldName+
                           " have changed (old=0x"+Integer.toHexString(oldAttributes)+
                           ", new=0x"+Integer.toHexString(item.accessFlags)+")");


                    verify(oldOffset == offset,
                           "Offset for "+className+'.'+fieldName+
                           " has changed (old="+oldOffset+", new="+offset+")");
                }
            }
        }
    }

    /**
     * Verify the declarations of the static method offsets for the methods
     * in java.lang.VMExtension called by the VM.
     */
    private void verifyVMExtensionOffsets() {

        for (Iterator iterator = suiteClassesWithVMAccessedAttributes.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry)iterator.next();
            SuiteClass sc = (SuiteClass)entry.getKey();
            VMAccessedAttribute attribute = (VMAccessedAttribute)entry.getValue();

            String className = sc.getName();
            StringOfSymbols symbols = sc.symbols;

            int methodCount = attribute.getStaticMethodsTableSize();
            if (methodCount != 0) {

                Romizer.assume(className.equals("java.lang.VMExtension"),
                               "Class other than java.lang.VMExtension cannot have '@vmaccessed: call' attribute: "+className);

                for (Enumeration e = attribute.getStaticMethodsTableEntries(); e.hasMoreElements(); ) {
                    VMAccessedAttribute.Item item = (VMAccessedAttribute.Item)e.nextElement();
                    if ((item.accessFlags & SquawkConstants.VMACC_CALL) != 0) {
                        int id = symbols.lookupMemberID(SquawkConstants.MTYPE_STATICMETHODS, item.slot);

                        String methodName = symbols.getMemberName(id);
                        int offset = symbols.getMemberOffset(id);
                        int oldOffset = GeneratedObjectLayout.getOffsetForStaticMethodInVMExtension(methodName);

                        verify(oldOffset == offset,
                               "Offset for "+className+'.'+methodName+
                               " has changed (old="+oldOffset+", new="+offset+")");
                    }
                }
            }
        }
    }

    /**
     * Determine whether a class corresponding to a given name is a system
     * class. That is, is the class in suite 0.
     *
     * @param name
     * @return
     */
    private boolean isSystemClass(String name) {
        Klass klass = SuiteManager.forName(name);
        return klass != null && ((klass.type >> 8) == 0);
    }
}