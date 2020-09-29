package com.sun.squawk.translator.suite.impl;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.JVMConst;
import com.sun.squawk.translator.loader.LinkageException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import javax.microedition.io.*;

import com.sun.squawk.suite.*;
import com.sun.squawk.xml.Tag;
import com.sun.squawk.vm.SquawkConstants;

/**
 * This class enables loading of a suite file in XML format.
 */
public class XMLSuiteLoader extends BinarySuiteLoader {

    /**
     * Load a suite from a specified file.
     * @param fileName
     * @return
     * @throws LinkageException
     */
    public String loadSuite(Translator vm, String fileName) throws LinkageException {
        InputStream in = null;
        this.vm = vm;
        try {
            in = Connector.openInputStream("file://"+fileName);
            Suite suite = parseSuite(in);
            suite.resolve();
            return suite.getName();
        } catch (IOException ioe) {
            throw new LinkageException(vm.LINKAGEERROR, "Error opening/reading suite from '"+fileName+"': "+ioe.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    /** These variables hold the current state of the suite being parsed. */
    private String suiteName;
    private int suiteAccess;
    private int maxType;
    private String[] bindList;
    private Suite suite;

    private Suite parseSuite(InputStream in) throws IOException {

        // skip leading whitespace
        int ch;
        while ((ch = in.read()) != '<' && ch != -1) {
            if (ch != ' ' && ch != '\t') {
                throw new RuntimeException("Found bad character while looking for '<' in XML file: " + ch);
            }
        }

        Tag tag = Tag.create(in, '<');
        Assert.that(tag != null);

        tag = parseSuiteTag(tag);
        tag = parseSuiteAccess(tag);
        tag = parseSuiteName(tag);
        tag = parseHighest(tag);
        tag = parseBindList(tag);
        suite = new Suite(vm, suiteName, suiteAccess, bindList, maxType);
        tag = parseClasses(tag, suite);
        return suite;
    }

    /*
     * parseName
     */
    private String parseName(Tag nam) {
        nam.checkName("name");
        Tag cont = nam.getContent();
        if (cont == null) {
            return "";
        }
        return Tag.unescape(cont.getData());
    }

    /*
     * parseNameList
     */
    private String[] parseNameList(Tag nam) {
        int count = Tag.countTags(nam, "name");
        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            result[i] = parseName(nam);
            nam = nam.getNext();
        }
        return result;
    }

    /*
     * parseSuiteTag
     */
    private Tag parseSuiteTag(Tag tag) {
        tag.checkName("suite");
        return tag.getContent();
    }

    /*
     * parseSuiteName
     */
    private Tag parseSuiteName(Tag tag) {
        suiteName = parseName(tag);
        return tag.getNext();
    }

    /*
     * parseSuiteAccess
     */
    private Tag parseSuiteAccess(Tag tag) {
        tag.checkName("access");
        suiteAccess = parseAccess(tag);
        return tag.getNext();
    }

    /*
     * parseHighest
     */
    private Tag parseHighest(Tag tag) {
        maxType = parseInt(tag, "highest");
        return tag.getNext();
    }

    /*
     * parseBindList
     */
    private Tag parseBindList(Tag tag) {
        tag.checkName("bind");
        bindList = parseNameList(tag.getContent());
        return tag.getNext();
    }

    /*
     * parseClasses
     */
    private Tag parseClasses(Tag tag, Suite suite) {
        tag.checkName("classes");
        Tag content = tag.getContent();
        int count = Tag.countTags(content, "class");
        int lastClassNumber = 0;
        for (int i = 0; i < count; i++) {
            parseClass(content.getContent(), suite);
            content = content.getNext();
        }
        return tag.getNext();
    }

    /*
     * parseClass
     */
    private void parseClass(Tag cls, Suite suite) {

        char[] classReferences;
        Object[] objectReferences;
        int[] overriddenAccess;

        String name = parseName(cls);
        cls = cls.getNext();
        Assert.that(!name.endsWith(";"));

        cls.checkName("access");
        int accessFlags = parseAccess(cls);
        cls = cls.getNext();

        cls.checkName("type");
        int thisIndex = parseType(cls);
        cls = cls.getNext();

        cls.checkName("extends");
        int superIndex = parseType(cls.getContent());
        cls = cls.getNext();

        cls.checkName("implements");
        int[] interfaces = new int[parseInterfaceCount(cls.getContent())];
        parseInterfaceList(interfaces, cls.getContent());
        cls = cls.getNext();

        name = convertSuiteNameToInternalName(name);

        SuiteType suiteType = new SuiteType(thisIndex, superIndex, name, accessFlags);
        suite.addSuiteType(thisIndex, suiteType);

        cls.checkName("static_fields");
        parseFieldList(cls.getContent(), suiteType, true);
        cls = cls.getNext();

        cls.checkName("instance_fields");
        parseFieldList(cls.getContent(), suiteType, false);
        cls = cls.getNext();

        SuiteType superSuiteType = suiteType.getSuperClass(suite);

        cls.checkName("static_methods");
        parseMethodList(cls.getContent(), suiteType, null, true);
        cls = cls.getNext();

        cls.checkName("virtual_methods");
        parseMethodList(cls.getContent(), suiteType, superSuiteType, false);
        cls = cls.getNext();

        // overridden access
        cls.checkName("overridden_access");
        parseOverriddenList(cls.getContent(), suite, suiteType);
        cls = cls.getNext();

        // ignore class references
        cls.checkName("class_references");
        cls = cls.getNext();

        // ignore class references
        cls.checkName("object_references");
        cls = cls.getNext();
    }

    /*
     * parseInt
     */
    private int parseInt(Tag data) {
        try {
            return Integer.parseInt(data.getData());
        }
        catch (Exception ex) {
            throw new RuntimeException("Number format error");
        }
    }

    /*
     * parseInt
     */
    private int parseInt(Tag typ, String name) {
        typ.checkName(name);
        return parseInt(typ.getContent());
    }

    /*
     * parseIntArray
     */
    private int[] parseIntArray(Tag lst, String name) {
        int count = Tag.countTags(lst, name);
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            result[i] = parseInt(lst, name);
            lst = lst.getNext();
        }
        return result;
    }

    /*
     * parseByteArray
     */
    private byte[] parseByteArray(Tag lst, String name) {
        int count = Tag.countTags(lst, name);
        byte[] result = new byte[count];
        for (int i = 0; i < count; i++) {
            result[i] = (byte) parseInt(lst, name);
            lst = lst.getNext();
        }
        return result;
    }

    /*
     * parseType
     */
    private int parseType(Tag typ) {
        return parseInt(typ, "type");
    }

    /*
     * parseTypeList
     */
    private int[] parseTypeList(Tag typ) {
        return parseIntArray(typ, "type");
    }

    /*
     * parseAccess
     */
    private char parseAccess(Tag acc) {
        acc.checkName("access");
        char result = 0;
        acc = acc.getContent();
        while (acc != null) {
            result |= (char)TagLookup.getAccessCode(acc.getName());
            acc = acc.getNext();
        }
        return result;
    }

    /*
     * parseInterfaceCount
     */
    private int parseInterfaceCount(Tag lst) {
        return Tag.countTags(lst, "interface");
    }

    /*
     * parseInterfaceList
     */
    private void parseInterfaceList(int[] interfaces, Tag lst) {
        int count = interfaces.length;
        for (int i = 0; i < count; i++) {
            parseInterface(interfaces, i, lst.getContent());
            lst = lst.getNext();
        }
    }

    /*
     * parseInterface
     */
    private void parseInterface(int[] interfaces, int index, Tag lst) {
        interfaces[index] = parseType(lst);
        lst = lst.getNext();
    }

    /*
     * parseFieldList
     */
    private void parseFieldList(Tag lst, SuiteType suiteType, boolean isStatic) {
        int count = Tag.countTags(lst, "field");
        int actualCount = 0;
        SuiteField[] fields = new SuiteField[count];
        for (int i = 0; i < count; i++) {
            Tag fld         = lst.getContent();
            int accessFlags = parseAccess(fld);    fld = fld.getNext();
            int fieldType   = parseType(fld);        fld = fld.getNext();
            if ((accessFlags & SquawkConstants.ACC_SYMBOLIC) != 0) {
                String name = parseName(fld);
                if (isStatic) {
                    accessFlags |= JVMConst.ACC_STATIC;
                }
                fields[actualCount++] = new SuiteField(accessFlags & JVMConst.VALID_FIELD_FLAGS_MASK, name, fieldType);
            }
            lst = lst.getNext();
        }
        suiteType.setFields(fields, actualCount, isStatic);
    }

    /*
     * parseMethodList
     */
    private void parseMethodList(Tag lst, SuiteType suiteType, SuiteType superSuiteType, boolean isStatic) {
        int count = Tag.countTags(lst, "method");
        SuiteMethod[] methods = new SuiteMethod[count];
        int actualCount = 0;
        int vtableIndex = superSuiteType == null ? 0 : superSuiteType.getMaxVtableIndex() + 1;
        for (int i = 0; i < count; i++) {
            Tag mth         = lst.getContent();
            int accessFlags = parseAccess(mth);            mth = mth.getNext();
            int methodType  = parseType(mth);              mth = mth.getNext();
            int[] parms     = parseTypeList(mth.getContent()); mth = mth.getNext();
            boolean isInit  = (accessFlags & SquawkConstants.ACC_INIT) != 0;

            // Make receiver of virtual methods and <init> implicit
            if (!isStatic || isInit) {
                Assert.that(parms.length > 0);
                Object old = parms;
                parms = new int[parms.length - 1];
                System.arraycopy(old, 1, parms, 0, parms.length);
            }

            if ((accessFlags & SquawkConstants.ACC_SYMBOLIC) != 0) {
                String name = parseName(mth);
                if (isStatic && !isInit) {
                    accessFlags |= JVMConst.ACC_STATIC;
                }
                methods[actualCount++] = new SuiteMethod(accessFlags & JVMConst.VALID_METHOD_FLAGS_MASK, name, methodType, parms, vtableIndex);
            }
            lst = lst.getNext();
            vtableIndex++;
        }
        suiteType.setMethods(methods, actualCount, isStatic);
    }

    private void parseOverriddenList(Tag lst, Suite suite, SuiteType suiteType) {
        int overridingCount = Tag.countTags(lst, "method");
        if (overridingCount != 0) {
            SuiteMethod[] methods = new SuiteMethod[overridingCount];
            for (int i = 0; i != overridingCount; ++i) {
                Tag mth = lst.getContent();
                int vtableIndex = parseInt(mth, "slot");       mth = mth.getNext();
                int accessFlags = parseAccess(mth);            mth = mth.getNext();

                SuiteMethod overridden = suiteType.getSuperClass(suite).lookupMethod(suite, vtableIndex);
                Assert.that(overridden != null);
                SuiteMethod method = new SuiteMethod(accessFlags, overridden.name, overridden.type, overridden.parms, vtableIndex);
                methods[i] = method;
                lst = lst.getNext();
            }
            suiteType.setOverridingMethods(methods);
        }
    }
}
