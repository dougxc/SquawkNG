package com.sun.squawk.translator.suite.impl;

import java.io.*;
import java.util.*;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.Assert;
import com.sun.squawk.translator.ir.*;
import com.sun.squawk.translator.suite.SuiteEmitter;
import com.sun.squawk.translator.suite.SuiteAttribute;
import com.sun.squawk.translator.suite.VMAccessedAttribute;
import com.sun.squawk.translator.suite.MethodDebugAttribute;
import com.sun.squawk.vm.*;

import java.io.IOException;

/**
* This class implements a SuiteEmitter which emits an XML representation of a suite.
*/
public class XMLSuiteEmitter implements SuiteEmitter {

    /** The output stream. */
    private OutputStreamWriter out;
    /** Flags controlling behaviour of emitter. */
    private int flags;
    /** Buffer for building <meta> node for method bodies. */
    private ByteArrayOutputStream methodDebugInfo;
    private PrintStream methodDebugInfoOut;
    private int maxType=0;

    public void init(OutputStream out, int flags, Hashtable properties) {
        Assert.that(this.out == null);
        this.out = new OutputStreamWriter(out);
        this.flags = flags;
    }

    public void close() throws IOException {
        out.close();
    }

    private void print(String s) {
        try {
            if (s == null) {
                s = "null";
            }
            out.write(s);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            Assert.shouldNotReachHere();
        }
    }

    private void println(String s) {
        print(s + Translator.lineSeparator);
    }
    private void println() {
        print(Translator.lineSeparator);
    }

    public boolean isEmittingStackComments() { return (flags & EMIT_STACK_COMMENTS) != 0; }
    public boolean isEmittingComments()      { return (flags & EMIT_COMMENTS) != 0; }
    public boolean isEmittingMethodDebugAttribute() {
        return (flags & EMIT_METHOD_DEBUG) != 0;
    }

    /**
     * Wrap a given string in "<" and "/>" and print it.
     * @param str The string to wrap and print.
     * @exception IOException if there is an IO problem.
     */
    private void wrapPrint(String str) {
        print("<"+str+"/>");
    }

    /**
     * Wrap a given string in "<" and "/>" and print it followed by a newline.
     * @param str The string to wrap and print.
     * @exception IOException if there is an IO problem.
     */
    private void wrapPrintln(String str) {
        println("<"+str+"/>");
    }

    /**
     * Wrap a string in XML comment tags and return it.
     * @param s The string to wrap.
     * @return the wrapped string.
     */
    private String comment(String s) {
        if (!isEmittingComments()) {
            return "";
        }
        return "   <!-- "+XMLEncodeString(s)+" -->";
    }
    private String comment(Type t) {
        return comment(t.toSignature());
    }
    private String comment(Member m) {
        int slot = m.slot();
        return comment((m instanceof Method && ((Method)m).isSynthetic() ? "**synthetic** " : "") +
                       (m.isStatic() ? "static " : "") + m + "  slot="+slot);
    }

    public static String XMLEncodeString(String s) {

       /*
        * Tag parser cannot cope with data that is just spaces
        */
        if (s.length() > 0 && s.charAt(0) == ' ') {
            boolean allSpaces = true;
            for (int i = 0 ; i < s.length() ; i++) {
                if (s.charAt(i) != ' ') {
                    allSpaces = false;
                }
            }
            if (allSpaces) {
                StringBuffer sb = new StringBuffer();
                for (int i = 0 ; i < s.length() ; i++) {
                    sb.append("&#32;");
                }
                return sb.toString();
            }
        }

        StringBuffer buf = new StringBuffer(s.length() * 2);
        for (int j = 0 ; j < s.length() ; j++) {
            char ch = s.charAt(j);
            if (ch < ' ' || ch >= 0x7F || ch == '<' || ch == '>' || ch == '&' || ch == '"') {
                buf.append("&#");
                buf.append((int)ch);
                buf.append(';');
            } else {
                buf.append(ch);
            }
        }
        return buf.toString();
    }


/*---------------------------------------------------------------------------*\
 *                Meta-info emitting methods                                 *
\*---------------------------------------------------------------------------*/

    /**
     * Optionally emit a comment.
     * @param prefix Optional prefix.
     * @param comment
     */
    public void emitComment(String prefix, String comment, boolean nl) {
        if (prefix != null) {
            print(prefix);
        }
        if (nl) {
            println(comment(comment));
        }
        else {
            print(comment(comment));
        }
    }



    /**
     * Emit a physical representation of a Suite.
     * @param name The name of the suite
     * @param isFinal Indicates if this is a final suite or not.
     * @param proxies The proxy classes of the suite.
     * @param classes The non-proxy classes of the suite.
     * @return true if the suite producer should traverse the subelements of the suite.
     * @exception IOException if there is an IO problem.
     */
    public boolean emitSuite(String name, boolean isFinal, Type[] proxies, Type[] classes) {
        for (int i = 0; i < proxies.length; i++){
            if (proxies[i].suiteID() > maxType){
                maxType = proxies[i].suiteID();
            }
        }
        for (int i = 0; i < classes.length; i++){
            if (classes[i].suiteID() > maxType){
                maxType = classes[i].suiteID();
            }
        }
        return true;
    }

    /**
     * Emit the suite header.
     *
     * @param name The name of the suite
     * @param isFinal Indicates if this is a final suite or not.
     * @param binds The list of suites to which this one is bound.
     * @exception IOException if there is an IO problem.
     */
    public void emitSuiteStart(String name, boolean isFinal, String[] binds) {
        println("<suite>");
        println("  <access>"+(isFinal ? "<final/>" : "")+"</access>");
        println("  <name>"+name+"</name>");
        println("  <highest>"+maxType+"</highest>");
        println("  <bind>");
        for (int i = 0; i != binds.length; i++) {
            println("    <name>"+binds[i]+"</name>");
        }
        println("  </bind>");
    }

    public void emitSuiteEnd() {
        println("</suite>");
    }

    /**
     * Start emitting meta info for all classes (i.e. both proxy and non-proxy classes).
     * @param numClasses The total number of classes about to be emitted.
     */
    public void emitAllClassesMetaInfoStart(int numClasses) { println("  <classes>"+comment("num classes="+numClasses)); }
    public void emitAllClassesMetaInfoEnd()                 { println("  </classes>"); }

    /**
     * Emit the meta-info for a list of classes.
     * @param classes The list of classes.
     * @param boolean isProxy true if classes is a list of proxy classes, false otherwise
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if there is an IO problem.
     */
    public boolean emitClassesMetaInfoStart(Type[] classes, boolean isProxy) {
        if (isProxy) {
            println();
            println("    "+comment(" **** Proxy Classes ****"));
            println();
        }
        else {
            println();
            println("    "+comment(" **** Suite Classes ****"));
            println();
        }
        return true;
    }
    public void    emitClassesMetaInfoEnd()                                  {}

    /**
     * Emit the meta-info for a class.
     * @param type The class.
     * @param boolean isProxy true if classes is a list of proxy classes, false otherwise
     * @param isLast true if this is the last class in the list of classes.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if there is an IO problem.
     */
    public boolean emitClassMetaInfoStart(Type type, boolean isProxy, boolean isLast) {
        println("    <class>"+comment(type)+comment("SuiteID="+type.suiteID()));
        return true;
    }
    public void emitClassMetaInfoEnd() {
        println("    </class>");
    }

    private void emitGenericAccessFlags(String indent, int accessFlags, String extraFlags) {
        print(indent + "<access>");
        if ((accessFlags & SquawkConstants.ACC_PUBLIC) != 0) {
            print("<public/>");
        }
        else if ((accessFlags & SquawkConstants.ACC_PRIVATE) != 0) {
            print("<private/>");
        }
        else if ((accessFlags & SquawkConstants.ACC_PROTECTED) != 0) {
            print("<protected/>");
        }
        if ((accessFlags & SquawkConstants.ACC_FINAL) != 0) {
            print("<final/>");
        }
        if ((accessFlags & SquawkConstants.ACC_VOLATILE) != 0) {
            print("<volatile/>");
        }
        if ((accessFlags & SquawkConstants.ACC_TRANSIENT) != 0) {
            print("<transient/>");
        }
        if ((accessFlags & SquawkConstants.ACC_ABSTRACT) != 0) {
            print("<abstract/>");
        }
        if ((accessFlags & SquawkConstants.ACC_NATIVE) != 0) {
            print("<native/>");
        }
        if ((accessFlags & SquawkConstants.ACC_INTERFACE) != 0) {
            print("<interface/>");
        }
        if ((accessFlags & SquawkConstants.ACC_PROXY) != 0) {
            print("<proxy/>");
        }
        if ((accessFlags & SquawkConstants.ACC_SYMBOLIC) != 0) {
            print("<symbolic/>");
        }
        if ((accessFlags & SquawkConstants.ACC_INIT) != 0) {
            print("<init/>");
        }
        if ((accessFlags & SquawkConstants.ACC_STATIC) != 0) {
            print("<static/>");
        }
        if (extraFlags != null) {
            print(extraFlags);
        }

        println("</access>");
    }

    /**
     * Emit the suite ID for class.
     * @param type The class.
     * @exception IOException if there is an IO problem.
     */
    public void emitClassType(Type type) {
        println("      <type>"+type.suiteID()+"</type>");
    }

    /**
     * Emit the access flags for a class.
     * @param accessFlags The class access flags.
     * @exception IOException if there is an IO problem.
     */
    public void emitClassAccessFlags(int accessFlags) {
        emitGenericAccessFlags("      ", accessFlags, null);
    }

    /**
     * Emit the name of a class.
     * @param name The name of the class.
     * @exception IOException if there is an IO problem.
     */
    public void emitClassName(String name) {
        println("      <name>"+name+"</name>");
    }

    /**
     * Emit the super class for a class.
     * @param superClassNumber The super class (null if this is Object).
     * @exception IOException if there is an IO problem.
     */
    public void emitClassExtends(Type superClass) {
        if (superClass!=null){
            println("      <extends><type>"+superClass.suiteID()+"</type></extends>"+comment(superClass));
        }
        else {
            println("      <extends><type>"+0+"</type></extends>");
        }
    }

    /**
     * Emit the interfaces implemented by a class.
     * @param numInterfaces The number of interfaces implemented.
     * @exception IOException if there is an IO problem.
     */
    public void    emitClassImplementsStart(int numInterfaces) {
        println("      <implements>");
    }
    public void    emitClassImplementsEnd() {
        println("      </implements>");
    }
    public void    emitInterfaceStart() {
        println("        <interface>");
    }
    public void    emitInterfaceEnd() {
        println("        </interface>");
    }
    public void    emitInterfaceType(Type type) {
        println("          <type>"+type.suiteID()+"</type>"+comment(type));
    }
    public void    emitInterfaceMethodImplementationSlotsStart(int numMethods) {
        println("          <slots>");
    }
    public void    emitInterfaceMethodImplementationSlotsEnd() {
        println("          </slots>");
    }
    public void    emitInterfaceMethodImplementationSlot(int slot) {
        println("            <slot>"+slot+"</slot>");
    }

    /**
     * Emit the name for a method or field.
     * @param name The name of the method or field.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMemberName(String name) {
        println("          <name>"+XMLEncodeString(name)+"</name>");
    }

    /**
     * Emit the meta info for a list of fields.
     * @param fields The list of fields.
     * @param fieldsCount The number of fields to emit from the start of the list.
     * @param isStatic Emitting static fields.
     * @param isProxy true if this is a list of fields for a proxy class, false otherwise.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitFieldsStart(Field[] fields,
                            int fieldsCount,
                            boolean isStatic,
                            boolean isProxy)
    {
        if (isStatic){
            println("      <static_fields>");
        }
        else {
            println("      <instance_fields>");
        }
        return true;
    }
    public void emitFieldsEnd(boolean isStatic) {
        if (isStatic) {
            println("      </static_fields>");
        }
        else {
            println("      </instance_fields>");
        }
    }

    /**
     * Emit the meta info for a field.
     * @param field The field.
     * @param isProxy true if this is a field of a proxy class, false otherwise.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitFieldStart(Field field, boolean isProxy) { println("        <field>"+comment(field)); return true; }
    public void    emitFieldEnd()                               { println("        </field>");   }

    /**
     * Emit the type of a field.
     * @param type The field's type.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitFieldType(Type type) {
        println("          <type>"+type.suiteID()+"</type>"+comment(type));
    }

    /**
     * Emit the access flags for a field.
     * @param accessFlags The field access flags.
     * @exception IOException if there is an IO problem.
     */
    public void emitFieldAccessFlags(int accessFlags) {
        emitGenericAccessFlags("          ", accessFlags, null);
    }

    /**
     * Emit the meta info for a list of methods.
     * @param methods The list of methods.
     * @param methodsCount The number of methods to emit from the start of the list.
     * @param isStatic if true, then methods are static methods.
     * @param isProxy true if this is a list of methods for a proxy class, false otherwise.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitMethodsStart(Method[] methods,
                            int methodsCount,
                            boolean isStatic,
                            boolean isProxy) {
        if (isStatic) {
            println("      <static_methods>"+comment("num methods="+methodsCount));
        }
        else {
            println("      <virtual_methods>");
        }
        return true;
    }

    public void emitMethodsEnd(boolean isStatic) {
        if (isStatic) {
            println("      </static_methods>");
        }
        else {
            println("      </virtual_methods>");
        }
    }

    /**
     * Emit the meta info for a method.
     * @param method The method.
     * @param isProxy true if this is a method of a proxy class, false otherwise.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitMethodStart(Method method, boolean isProxy) { println("        <method>"+comment(method)); return true; }
    public void    emitMethodEnd()                                 { println("        </method>");             }

    /**
     * Emit the return type of a method. A method's type is composed of it's parameters
     * and return type.
     * @param the receiver type of the method or null if the method has no receiver.
     * @param parameters The method's parameters.
     * @param returnType The method's return type.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMethodType(Type receiver, Type[] parameters, Type returnType) {
        println("          <type>"+returnType.suiteID()+"</type>");
        println("          <parameters>");
        if (receiver != null) {
            println("            <type>"+receiver.suiteID()+"</type>"+comment(receiver));
        }
        for (int i = 0; i < parameters.length; i++){
            println("            <type>"+parameters[i].suiteID()+"</type>"+comment(parameters[i]));
        }
        println("          </parameters>");
    }

    /**
     * Emit the access flags for a method.
     * @param accessFlags The method access flags.
     * @exception IOException if there is an IO problem.
     */
    public void emitMethodAccessFlags(int accessFlags) {
        emitGenericAccessFlags("          ", accessFlags, null);
    }

    public void    emitOverriddenAccessStart(int numOverrides) {
        println("      <overridden_access>");
    }
    public void    emitOverriddenAccessEnd() {
        println("      </overridden_access>");
    }
    public void    emitOverriddenAccessMethodStart(Method method) {
        println("        <method>"+comment(method));
    }
    public void    emitOverriddenAccessMethodEnd() {
        println("        </method>");
    }
    public void    emitOverriddenAccessMethodSlot(int slot) {
        println("          <slot>"+slot+"</slot>");
    }
    public void    emitOverriddenAccessMethodAccess(int flags) {
        emitGenericAccessFlags("          ", flags, null);
    }

   /**
    * Emit the list of classes referenced by a class.
    * @param classes The list of classes.
    */
    public void emitClassReferences(Type[] classes) {
        println("      <class_references>");
        for (int i = 0; i < classes.length; i++) {
            println("     "+comment(""+i)+" <type>"+classes[i].suiteID()+"</type>"+comment(classes[i]));
        }
        println("      </class_references>");
    }

    /**
     * Emit the list immutable constant objects the methods of a class may refer to.
     * @param constants The list of immutable constant objects.
     * @return true if the suite producer should traverse the children of this suite element.
     */
    public boolean emitConstantsStart(Object[] constants) { println("      <object_references>"); return true; }
    public void    emitConstantsEnd()                     { println("      </object_references>");             }

    /**
     * Emit an immutable String constant object.
     * @param index The index of the constant in the class's constant object pool.
     * @param constant The immutable String constant object.
     */
    public void emitStringConstant(int index, String constant) {
        println("     "+comment(""+index)+" <string>"+XMLEncodeString(constant)+"</string>");
    }

    /**
     * Emit an immutable int array constant object.
     * @param index The index of the constant in the class's constant object pool.
     * @param constant The immutable int array constant object.
     */
    public void emitIntArrayConstant(int index, int[] constant) {
        print("     "+comment(""+index)+" <int_array>");
        for (int j = 0; j < constant.length; j++){
            print("<int>"+constant[j]+"</int>");
        }
        println("</int_array>");
    }

    /**
     * Emit an immutable short array constant object.
     * @param index The index of the constant in the class's constant object pool.
     * @param constant The immutable short array constant object.
     */
    public void emitShortArrayConstant(int index, short[] constant) {
        print("     "+comment(""+index)+" <short_array>");
        for (int j = 0; j < constant.length; j++){
            print("<short>"+constant[j]+"</short>");
        }
        println("</short_array>");
    }

    /**
     * Emit an immutable char array constant object.
     * @param index The index of the constant in the class's constant object pool.
     * @param constant The immutable char array constant object.
     */
    public void emitCharArrayConstant(int index, char[] constant) {
        print("     "+comment(""+index)+" <char_array>");
        for (int j = 0; j < constant.length; j++){
            print("<char>"+constant[j]+"</char>");
        }
        println("</char_array>");
    }

    /**
     * Emit an immutable byte array constant object.
     *
     * @param index The index of the constant in the class's constant object pool.
     * @param constant The immutable byte array constant object.
     */
    public void emitByteArrayConstant(int index, byte[] constant) {
        print("     "+comment(""+index)+" <byte_array>");
        for (int j = 0; j < constant.length; j++){
            print("<byte>"+constant[j]+"</byte>");
        }
        println("</byte_array>");
    }

    /**
     * Emit the attributes (if any) for a class.
     *
     * @param type The class to which the atributes pertain.
     * @param attributes The attributes.
     * @exception IOException if there is an IO problem.
     */
    public void emitClassAttributes(Type type, SuiteAttribute[] attributes) {
        if (attributes != null && attributes.length != 0) {
            println("      <attributes>");
            for (int i = 0; i != attributes.length; ++i) {
                SuiteAttribute attribute = attributes[i];
                if (attribute instanceof VMAccessedAttribute) {
                    VMAccessedAttribute attr = (VMAccessedAttribute)attribute;
                    println("        <attribute>");
                    println("          <name>"+attr.getAttributeName()+"</name>");
                    println("          <access>"+((attr.classAccessFlags & SquawkConstants.VMACC_EEPROM) == 0 ? "" : "<eeprom/>")+"</access>");
                    emitVMAccessedInstanceFieldsTable(type, attr);
                    emitVMAccessedStaticMethodsTable(type, attr);
                    println("        </attribute>");
                } else {
                    println("    <!-- XMLSuiteEmitter: skipped unrecognized class attribute: "+attribute.getAttributeName()+" -->");
                }
            }
            println("      </attributes>");
        }
    }

    private void emitVMAccessedInstanceFieldsTable(Type type, VMAccessedAttribute attribute) {
        println("          <instanceFieldsTable>");
        Field[] fields = type.getFields(false);
        int fieldIndex = 0;
        for (Enumeration e = attribute.getInstanceFieldsTableEntries(); e.hasMoreElements();) {
            VMAccessedAttribute.Item item = (VMAccessedAttribute.Item)e.nextElement();
            while (fields[fieldIndex].slot() != item.slot) {
                Assert.that(fieldIndex < fields.length);
                fieldIndex++;
            }
            Field field = fields[fieldIndex];
            println("            <field>"+comment(field.name()));
            println("              <slot>"+item.slot+"</slot>");
            print  ("              <access>");
            if ((item.accessFlags & SquawkConstants.VMACC_UNSIGNED) != 0) {
                print("<unsigned/>");
            }
            if ((item.accessFlags & SquawkConstants.VMACC_READ) != 0) {
                print("<read/>");
            }
            if ((item.accessFlags & SquawkConstants.VMACC_WRITE) != 0) {
                print("<write/>");
            }
            if ((item.accessFlags & SquawkConstants.VMACC_WBOPAQUE) != 0) {
                print("<wbopaque/>");
            }
            println("</access>");
            println("            </field>");
        }
        println("          </instanceFieldsTable>");
    }

    private void emitVMAccessedStaticMethodsTable(Type type, VMAccessedAttribute attribute) {
        println("          <staticMethodsTable>");
        Method[] methods = type.getMethods(true);
        int methodIndex = 0;
        for (Enumeration e = attribute.getStaticMethodsTableEntries(); e.hasMoreElements();) {
            VMAccessedAttribute.Item item = (VMAccessedAttribute.Item)e.nextElement();
            while (methods[methodIndex].slot() != item.slot) {
                Assert.that(methodIndex < methods.length);
                methodIndex++;
            }
            Method method = methods[methodIndex];
            println("            <method>"+comment(method.name()));
            println("              <slot>"+item.slot+"</slot>");
            print  ("              <access>");
            if ((item.accessFlags & SquawkConstants.VMACC_CALL) != 0) {
                print("<call/>");
            }
            println("</access>");
            println("            </method>");
        }
        println("          </staticMethodsTable>");
    }

    /**
     * Emit the method bodies for a list of methods.
     * @param methodCount The number of methods.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitMethodBodiesStart(int methodCount)   { println("  <methods>"+comment("num methods="+methodCount)); return true; }
    public void emitMethodBodiesEnd()                       { println("  </methods>");             }

    /**
     * Emit the method body for a method.
     * @param method The method.
     * @return true if the suite producer should traverse the children of this suite element.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public boolean emitMethodBodyStart(Method method) {
        println("    <method_body>"+comment(method));
        return true;
    }
    public void    emitMethodBodyEnd()                {
        println("    </method_body>");
    }

    /**
     * Emit the class of a method.
     * @param ofClass The method's class.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMethodClass(Type ofClass) {
        println("      <type>"+ofClass.suiteID()+"</type>"+comment(ofClass));
    }

    /**
     * Emit the index of a method in it's table. There is one table for static
     * methods and one for virtual methods so method indexes are not necessarily
     * unique within a class.
     * @param index The method's index.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMethodSlot(int index) {
        println("      <entry>"+index+"</entry>");
    }

    /**
     * Emit the implementation access flags for a method.
     * @param flags the access flags
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMethodImplAccessFlags(int flags) {
        emitGenericAccessFlags("      ", flags, null);
    }

    /**
     * Emit the local variable types of a method.
     * @param locals The local variables of a method.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitMethodLocals(Local[] locals) {
        println("      <locals>");
        for (int i = 0; i < locals.length; i++){
            println("     "+comment(""+i)+" <type>"+locals[i].type().suiteID()+"</type>"+comment(locals[i].toString()));
        }
        println("      </locals>");
    }

    /**
     * Emit the max stack value.
     * @param maxStack
     */
    public void emitMethodMaxStack(int maxStack) {
        println("      <stack>"+maxStack+"</stack>");
    }

    /**
     * Start emitting the exception handler table of a method.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlersStart(int numHandlers) { println("        <handlers>"+comment("handlers_count="+numHandlers));  }
    public void emitExceptionHandlersEnd()                  { println("        </handlers>"); }

    /**
     * Start emitting a single exception handler table entry of a method.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlerStart() { println("           <handler>");  }
    public void emitExceptionHandlerEnd()   { println("           </handler>"); }

    /**
     * Emit the start address of the code range protected by an exception handler table entry.
     * @param from The start address.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlerFrom(int from) {
        println("          <from>"+from+"</from>");
    }

    /**
     * Emit the end address of the code range protected by an exception handler table entry.
     * @param to The end address.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlerTo(int to) {
        println("          <to>"+to+"</to>");
    }

    /**
     * Emit the entry address of the code of an exception handler.
     * @param entry The entry address.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlerEntry(int entry) {
        println("          <entrypoint>"+entry+"</entrypoint>");
    }

    /**
     * Emit the exception type caught by an exception handler.
     * @param catchType The exception type caught by an exception handler.
     * @exception IOException if an IO error occurs in the SuiteEmitter.
     */
    public void emitExceptionHandlerCatchType(Type catchType) {
        println("          <type>"+catchType.suiteID()+"</type>"+comment(catchType));
    }

    private boolean isFirstPass;

    /**
      * Emit the bytecode of a method.
      * @param firstPass  If true, this notifys the emitter that this is pass 1 through the
      * instructions. During this pass, each instruction emitting method should not actually emit
      * any content but merely return the number of bytes it will emit when
      * invoked during pass 2. This pass is used by the SuiteProducer to calculate
      * the final code layout and hence the correct offsets for each branch.
      * During pass 2, each emitting method should just emit the content and
      * return anything as that value is ignored by the SuiteProducer.
      * @return true if the suite producer should traverse the children of this suite element.
      * @exception IOException if an IO error occurs in the SuiteEmitter.
      */
     public boolean emitBytecodeStart(boolean firstPass, int length) {
         isFirstPass = firstPass;
         if(!firstPass) {
             println("      <code>");
         }
         return true;
     }

     public void emitBytecodeEnd() {
         if(!isFirstPass) {
             println("      </code>");
         }
     }

     /*---------------------------------------------------------------------------*\
      *                Instruction emitting methods                               *
     \*---------------------------------------------------------------------------*/

     private int emitLongFloatOpcode(IContext ctx, int opcode, boolean isFloat) {
         int size = (opcode == -1) ? 0 : 1;
         if (!isFirstPass) {
             // 2nd pass
             if (opcode != -1) {
                 instructionPrefix(ctx);
                 if (isFloat) {
/*if[FLOATS]*/
                     Assert.that(opcode >= 0 && opcode < Mnemonics.FLOAT_OPCODES.length);
                     wrapPrintln(Mnemonics.FLOAT_OPCODES[opcode]);
/*end[FLOATS]*/
                 }
                 else {
                     Assert.that(opcode >= 0 && opcode < Mnemonics.LONG_OPCODES.length);
                     wrapPrintln(Mnemonics.LONG_OPCODES[opcode]);
                 }
             }
         }
         return size;
     }

    /**
     * Helper function for emitting instructions that have exactly one format:
     *
     *    <opcode>
     *
     * @param inst
     * @param opcode The opcode of the instruction.
     * @return the number of bytes emitted which will be 1.
     */
    private int emitInstructionWithNoOperand(IContext ctx, int opcode) {
        if (!isFirstPass) {
            instructionPrefix(ctx);
            wrapPrintln(Mnemonics.OPCODES[opcode]);
        }
        return 1;
    }

    /**
     * Determine if a given number of bits is sufficent to encode a given unsigned value.
     * @param bits
     * @param operand The operand value.
     * @return
     */
    private boolean fits(int bits, long operand) {
        Assert.that(operand >= 0);
        long lo = 0;
        long hi = (1 << bits) - 1;
        return (operand >= lo && operand <= hi);
    }

    private void instructionPrefix(IContext ctx) {
        if (ctx.stackState != null && isEmittingStackComments()) {
            print("        ");
            println(comment("stack: { "+Type.toSignature(ctx.stackState,
                    0,
                    ctx.stackDepth,
                    ", ")+"}"));
        }
        print("        ");
        int srcLine = ctx.inst.getSourceLineNo();
        if (isEmittingComments()) {
            int oip = ctx.inst.getOriginalIP();
            print(comment("ip="+ctx.ip+" oip="+oip+" line="+srcLine)+"  ");
        }
    }

    /**
     * Helper function for emitting instructions that take a single operand that can
     * be up to 32-bits in magnitude.
     * @param inst
     * @param opcode The opcode of the instruction
     * @param isImm4BitOpcode If true, then opcode includes an immediate 4-bit operand.
     * @param operand The value of the operand.
     * @param debug
     * @return the number of bytes required for the encoding
     */
    private int emitInstructionWithVariableSizeIndexOperand(IContext ctx, int opcode, boolean isImm4BitOpcode, long operand, Object debug) {
        Assert.that(opcode >= 0 && opcode < Mnemonics.OPCODES.length);
        String mnemonic = Mnemonics.OPCODES[opcode];
        String comment = "";
        if (isEmittingComments()) {
            if (debug instanceof Local) {
                comment = comment(debug.toString());
            }
            else if (debug instanceof Type) {
                comment = comment((Type)debug);
            }
            else if (debug instanceof Member) {
                comment = comment((Member)debug);
            }
            else if (debug instanceof String) {
                comment = comment("\""+debug+"\"");
            }
            else {
                Assert.that(debug == null);
            }
        }

        int size;
        // The *_<0..15> immediate operand format
        if (isImm4BitOpcode) {
            Assert.that(fits(4, operand), "operand="+operand);
            size = 1;
            if (!isFirstPass) {
                instructionPrefix(ctx);
                wrapPrint(mnemonic);
                println(comment);
            }
        }
        // The single byte operand format
        else if (fits(8, operand)) {
            size = 2;
            if(!isFirstPass) {
                instructionPrefix(ctx);
                wrapPrint(mnemonic);
                println("<byte>"+operand+"</byte>"+comment);
            }
        }
        // The wide_<0-15> format
        else if (fits(12, operand)) {
            size = 3;
            if(!isFirstPass) {
                int wideImm = OPC.WIDE_0 + (int)((operand >> 8) & 0xF);
                Assert.that(wideImm >= OPC.WIDE_0 && wideImm <= OPC.WIDE_15);
                instructionPrefix(ctx);
                wrapPrint(Mnemonics.OPCODES[wideImm]);
                wrapPrint(mnemonic);
                println("<byte>"+(operand & 0xFF)+"</byte>"+comment);
            }
        }
        // The wide_half format
        else if (fits(16, operand)) {
            size = 4;
            if(!isFirstPass) {
                instructionPrefix(ctx);
                wrapPrint(Mnemonics.OPCODES[OPC.WIDE_HALF]);
                wrapPrint(mnemonic);
                println("<short>"+operand+"</short>"+comment);
            }
        }
        // The wide_int or wide_float format
        else {
            Assert.that(fits(32, operand));
            size = 6;
            if(!isFirstPass) {
                instructionPrefix(ctx);
                wrapPrint(Mnemonics.OPCODES[OPC.WIDE_FULL]);
                wrapPrint(mnemonic);
                println("<int>"+operand+"</int>"+comment);
            }
        }
        return size;
    }

    /**
      * Emit an arithmetic instruction.
      * @param opcode One of the following bytecodes:
      *
      *   OPC_IADD
      *   OPC_ISUB
      *   OPC_IAND
      *   OPC_IOR
      *   OPC_IXOR
      *   OPC_ISHL
      *   OPC_ISHR
      *   OPC_IUSHR
      *   OPC_IMUL
      *   OPC_IDIV
      *   OPC_IREM
      * @return the number of bytes emitted for this instruction.
      */
     public int emitArithmeticOp(IContext ctx, int opcode, int longFloatOpcode, boolean isFloat) {
         return emitInstructionWithNoOperand(ctx, opcode) + emitLongFloatOpcode(ctx, longFloatOpcode, isFloat);
     }

    /**
     * Emit an array load instruction.
     * @param opcode One of the following bytecodes:
     *    OPC_ALOAD
     * @return the number of bytes emitted for this instruction.
     */
    public int emitArrayLoad(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }

    /**
     * Emit an array store instruction.
     * @param opcode One of the following bytecodes:
     *
     *    OPC_ASTORE
     * @return the number of bytes emitted for this instruction.
     */
    public int emitArrayStore(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }

    /**
     * Emit the array length instruction.
     * @return the number of bytes emitted for this instruction.
     */
    public int emitArrayLength(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }

    /**
     * The branch encoding table for IF and GOTO instructions.
     *
     *  Offset range  |  Encoding size
     *  --------------+----------------
     *   -(2^8)
     *
     *  [ 8, 2, 12, 3, 16 , 4 ]
     *
     * This specifies that a signed 8-bit offset can be encoded in a 2 byte instruction,
     * a signed 12-bit offset can be encoded in a 3 byte instruction and a 16-bit
     * offset can be encoded in a 4 byte instruction.
     */
    private BranchEncoding[] branchEncodingTable = new BranchEncoding[] {
        new BranchEncoding(8, 2, 2),
        new BranchEncoding(12, 3, 3),
        new BranchEncoding(16 , 4, 4)
    };

    /**
     * Emit a branch instruction.
     * @param opcode One of the following bytecodes:
     *
     *     OPC_IFEQ
     *     OPC_IFNE
     *     OPC_IFLT
     *     OPC_IFLE
     *     OPC_IFGT
     *     OPC_IFGE
     *
     *     OPC_IF_ICMPEQ
     *     OPC_IF_ICMPNE
     *     OPC_IF_ICMPLT
     *     OPC_IF_ICMPLE
     *     OPC_IF_ICMPGT
     *     opc_IF_ICMPGE
     *
     *     OPC_GOTO
     *
     * @param offset If this is pass 1, then this will be 0 and should be ignored.
     * If this is pass 2, then this is the offset to the target instruction.
     * @param bitSize This will be null during the first pass. On the second pass,
     * this will be one of the BranchEncoding values returned in the first pass.
     * @return null if this is pass 2. If this is pass 1, then
     * return a table mapping upper limits of offsets (in terms
     * their bit sizes) to the number of bytes required to encode these limits.
     * This table is used by the SuiteProducer to calculate the final code
     * layout and hence the correct offsets for each branch.
     */
    public BranchEncoding[] emitBranch(IContext ctx, int opcode, int offset, BranchEncoding enc) {
        if(isFirstPass){
            return branchEncodingTable;
        }

        // Prepend "wide" prefix (if necessary).
        instructionPrefix(ctx);
        int bitSize = enc.offsetBits;
        switch (bitSize) {
            case 8:                                            break;
            case 12: wrapPrint("wide_"+((offset >> 8) & 0xF)); break;
            case 16: wrapPrint("wide_half");                   break;
            default: println("ERROR: Unexpected bitSize for branch instruction offset: "+bitSize);
        }

        // Emit opcode
        if (opcode < 0 || opcode >= Mnemonics.OPCODES.length) {
            println("ERROR: invalid branch opcode: " + opcode);
            return null;
        }
        else {
            wrapPrint(Mnemonics.OPCODES[opcode]);
        }

        // Emit offset
        switch (bitSize) {
            case 8:  print("<byte>"+offset+"</byte>");         break;
            case 12: print("<byte>"+(offset&0xFF)+"</byte>");  break;
            case 16: print("<short>"+offset+"</short>");         break;
        }

        // Emit real offset as a comment
        int abs = ctx.ip + enc.ipAdjust + offset;
        println(comment("rel="+offset+" abs="+abs));
        return null;
    }

    /**
     * Emit the checkcast instruction.
     * @param opcode OPC_CHECKCAST
     * @return the number of bytes emitted for this instruction.
     */
    public int emitCheckcast(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }

    /**
     * Emit a primitive type conversion instruction.
     * @param opcode One of the following bytecodes:
     *
     *     OPC_I2B
     *     OPC_I2C
     *     OPC_I2S
     *     OPC_NEG
     * @return the number of bytes emitted for this instruction.
     */
    public int emitConversion(IContext ctx, int opcode, int longFloatOpcode, boolean isFloat) {
        return emitInstructionWithNoOperand(ctx, opcode) + emitLongFloatOpcode(ctx, longFloatOpcode, isFloat);
    }

    /**
     * Emit an instruction to load a class reference to the stack.
     * @param opcode One of the following bytecodes:
     *
     *     OPC_CLASS
     *     OPC_CLASS_<i>   [ 0 <= i <= 15 ]
     *
     * Note that the given opcode need not necessarily be emitted. For example,
     * if the given opcode is OPC_CLASS_12, the emitter can choose to emit
     * OPC_CLASS with the operand of 12 instead.
     * @param index The index of the class in the pool of class references for the
     * current class.  This index is in terms of the ordering in the array passed
     * to emitClassReferences.
     * @return the number of bytes emitted for this instruction.
     * @see #emitClassReferences(Type[])
     */
    public int emitLoadClass(IContext ctx, int opcode, int index) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, opcode != OPC.CLASS, index, null);
    }

    /**
     * Emit a load constant value instruction.
     * @param opcode One of the following bytecodes:
     *
     *     OPC_CONST_M1
     *     OPC_CONST_<i>    [ 0 <= i <= 15 ]
     *     OPC_CONST_BYTE
     *     OPC_CONST_SHORT
     *     OPC_CONST_CHAR
     *     OPC_CONST_INT
     *     OPC_CONST_FLOAT
     *     OPC_CONST_DOUBLE
     *
     * @param value the constant value to load.
     * @param operandBytes the number of bytes worth of value to emit.
     * @return the number of bytes emitted for this instruction.
     */
    public int emitLoadConstant(IContext ctx, int opcode, long value) {
        int size;
        if (opcode == OPC.CONST_M1 || (opcode >= OPC.CONST_0 && opcode <= OPC.CONST_15)) {
            size = 1;
        }
        else {
            switch (opcode) {
                case OPC.CONST_BYTE:   size = 2; break;
                case OPC.CONST_SHORT:  size = 3; break;
                case OPC.CONST_CHAR:   size = 3; break;
                case OPC.CONST_INT:    size = 5; break;
                case OPC.CONST_LONG:   size = 9; break;
/*if[FLOATS]*/
                case OPC.CONST_FLOAT:  size = 5; break;
                case OPC.CONST_DOUBLE: size = 9; break;
/*end[FLOATS]*/
                default:
                    throw new RuntimeException("Error: unknown load constant opcode: " + opcode);
            }
        }
        if (!isFirstPass) {
            //2nd pass
            instructionPrefix(ctx);
            if (opcode == OPC.CONST_M1 || (opcode >= OPC.CONST_0 && opcode <= OPC.CONST_15)) {
                wrapPrintln(Mnemonics.OPCODES[opcode]);
            }
            else {
                wrapPrint(Mnemonics.OPCODES[opcode]);
                switch (opcode) {
                    case OPC.CONST_BYTE:   println("<byte>"  +(byte)  value+"</byte>");           break;
                    case OPC.CONST_SHORT:  println("<short>" +(short) value+"</short>");          break;
                    case OPC.CONST_CHAR:   println("<char>"  +(int)  (value & 0xFFFF)+"</char>"); break;
                    case OPC.CONST_INT:    println("<int>"   +(int)   value+"</int>");            break;
                    case OPC.CONST_LONG:   println("<long>"  +        value+"</long>");           break;
/*if[FLOATS]*/
                    case OPC.CONST_FLOAT:  println("<float>" +(int)   value+"</float>");          break;
                    case OPC.CONST_DOUBLE: println("<double>"+        value+"</double>");         break;
/*end[FLOATS]*/
                    default:
                        throw new RuntimeException("Error: unknown load constant opcode: " + opcode);
                }
            }
        }
        return size;
    }

    /**
     * Emit a load constant null instruction.
     * @param opcode OPC_CONST_NULL
     * @return
     * @return the number of bytes emitted for this instruction.
     */
    public int emitLoadConstantNull(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }


    /**
     * Emit a load constant object instruction.
     * @param opcode One of the following bytecodes:
     *
     *     OPC_OBJECT_<n>    [ 0 <= n <= 15 ]
     *     OPC_OBJECT
     *
     * @param index The index of the constant object in the pool of constant
     * objects for the current class. This index is in terms of the ordering
     * in the array passed to emitConstants.
     * @return the number of bytes emitted for this instruction.
     * @see #emitConstants(Object[])
     */
    public int emitLoadObject(IContext ctx, int opcode, int index) {
        String constant = null;
        if (ctx.inst instanceof LoadConstantObject) {
            LoadConstantObject ldc = (LoadConstantObject)ctx.inst;
            Object value = ldc.value();
            if (value instanceof String) {
                constant = (String)value;
            }
        }
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, opcode != OPC.OBJECT, index, constant);
    }


    /**
     * Emit a get instance field instruction.
     * @param opcode One of the following bytecodes:
     *
     *     OPC_GETFIELD
     *
     * @param slot The slot number of the field.
     */
    public int emitGetField(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((LoadField)ctx.inst).field());
    }

    /**
     * Emit a get static field instruction.
     * @param opcode One of the following bytecodes:
     *
     *     OPC_GETSTATIC
     *
     * @param slot The slot number of the field.
     * @return the number of bytes emitted for this instruction.
     */
    public int emitGetStatic(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((LoadField)ctx.inst).field());
    }


    public int emitInstanceOf(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }
    public int emitIncDecLocal(IContext ctx, int opcode, int index) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, index, ((IncDecLocal)ctx.inst).local());
    }

    public int emitInvokeVirtual(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((Invoke)ctx.inst).method());
    }
    public int emitInvokeSuper(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((Invoke)ctx.inst).method());
    }
    public int emitInvokeStatic(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((Invoke)ctx.inst).method());
    }
    public int emitInvokeInit(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((Invoke)ctx.inst).method());
    }
    public int emitInvokeInterface(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((Invoke)ctx.inst).method());
    }
    public int emitLoadLocal(IContext ctx, int opcode, int index) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, opcode != OPC.LOAD, index, ((LoadLocal)ctx.inst).local());
    }
    public int emitLookup(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }
    public int emitMonitorEnter(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }
    public int emitMonitorExit(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }
    public int emitNew(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }
    public int emitNewArray(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }
    public int emitNewDimension(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }
    public int emitPop(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }
    public int emitPutField(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((StoreField)ctx.inst).field());
    }
    public int emitPutStatic(IContext ctx, int opcode, int slot) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, false, slot, ((StoreField)ctx.inst).field());
    }
    public int emitStoreLocal(IContext ctx, int opcode, int index) {
        return emitInstructionWithVariableSizeIndexOperand(ctx, opcode, opcode != OPC.STORE, index, ((StoreLocal)ctx.inst).local());
    }
    public int emitReturn(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }

    /**
     * This will have to be modified
     * @param opcode
     * @param is16Bit
     * @param defaultOffset
     * @param lowMatch
     * @param highMatch
     * @param offsets
     * @param padding
     * @return
     */
    public BranchEncoding emitTableSwitch(IContext ctx, int opcode, boolean is16Bit, int defaultOffset, int lowMatch, int highMatch, int[] offsets, int padding) {
        if (!is16Bit) {
            throw new RuntimeException("This SuiteEmitter implementation cannot emit 32-bit switch instructions");
        }
        int ipAdjust = 1 + // opcode
                       2 + // default offset
                       4 + // low
                       4;  // high
        if (isFirstPass) {
            return new BranchEncoding(16, ipAdjust+(2*(highMatch-lowMatch+1)), ipAdjust);
        }
        int size;
        if (!isFirstPass) {
            //2nd pass
            instructionPrefix(ctx);
            wrapPrintln(Mnemonics.OPCODES[opcode]);
            if (padding == 1) {
                instructionPrefix(ctx);
                println("  <byte>0</byte>");
            }
            instructionPrefix(ctx);
            int abs = ctx.ip + ipAdjust + padding + defaultOffset;
            println("  <short>"+defaultOffset+"</short>"+comment("rel="+defaultOffset+" abs="+abs));
            instructionPrefix(ctx);
            println("  <int>"+lowMatch+"</int>");
            instructionPrefix(ctx);
            println("  <int>"+highMatch+"</int>");
            for (int i = 0; i < highMatch-lowMatch+1; i++) {
                instructionPrefix(ctx);
                int rel = offsets[i];
                abs = ctx.ip +ipAdjust + padding + rel;
                println("  <short>"+rel+"</short>"+comment("rel="+rel+" abs="+abs));
            }
        }
        return null;
    }
    public int emitThrow(IContext ctx, int opcode) {
        return emitInstructionWithNoOperand(ctx, opcode);
    }

    /**
     * Emit the attributes (if any) for a method body.
     *
     * @param method The method to which the attributes pertain.
     * @param attributes The attributes.
     * @exception IOException if there is an IO problem.
     */
    public void emitMethodBodyAttributes(Method method, SuiteAttribute[] attributes) {

        if (attributes != null && attributes.length != 0) {
            println("      <attributes>");
            for (int i = 0; i != attributes.length; ++i) {
                SuiteAttribute attribute = attributes[i];
                if (attribute instanceof MethodDebugAttribute) {
                    MethodDebugAttribute debug = (MethodDebugAttribute)attribute;
                    println("        <attribute>");
                    println("          <name>"+debug.getAttributeName()+"</name>");
                    println("          <filePath>"+debug.filePath+"</filePath>");
                    println("          <signature>"+XMLEncodeString(debug.signature)+"</signature>");
                    println("          <lineNumberTable>");
                    for (Enumeration e = debug.getLineNumberTableEntries(); e.hasMoreElements();) {
                        MethodDebugAttribute.LNTItem item = (MethodDebugAttribute.LNTItem)e.nextElement();
                        println("            <item>");
                        println("              <startPc>"+item.startPc+"</startPc>");
                        println("              <lineNumber>"+item.lineNumber+"</lineNumber>");
                        println("            </item>");
                    }
                    println("          </lineNumberTable>");
                    println("        </attribute>");
                } else {
                    println("    <!-- XMLSuiteEmitter: skipped unrecognized method attribute: "+attribute.getAttributeName()+" -->");
                }
            }
            println("      </attributes>");
        }
    }
}
