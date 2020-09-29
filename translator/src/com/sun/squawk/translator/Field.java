package com.sun.squawk.translator;

import com.sun.squawk.translator.util.*;
import com.sun.squawk.translator.loader.*;

/**
 * This class represents a static of instance field of a class.
 */
public final class Field extends Member {

    /** Value for the field to be initialized to. */
    private Object constantValue;

    /**
     * Public constructor.
     * @param vm
     * @param parent
     * @param name
     * @param type
     * @param flags
     * @return
     */
    public static Field create(Translator vm, Type parent, String name, Type type, int flags) {
        Field field = new Field(parent, name, type, flags);
        return field;
    }

    /**
     * Load the types implied by the signature of this field. That is, load
     * the fields type and declaring class.
     */
    public void loadSignatureTypes() throws LinkageException {
        if (!areTypesLoaded()) {
            parent().load();
            type().load();
            setTypesLoaded();
        }
    }

    /**
     * Set the constantValue.
     * @param constantValue
     */
    public void setConstantValue(Object constantValue) {
        this.constantValue = constantValue;
    }

    /**
     * Get initial value.
     * @return the initial value of the field.
     */
    public Object getConstantValue() {
        return constantValue;
    }

    /**
     * Return the signature of the field in internal format. E.g. "(II)V" for a method
     * or "Ljava/io/PrintStream;" for a field.
     * @param includePackageNames If true, then use fully qualified class names.
     * @param asSourceDecl If true, then use a format corresponding with the source level
     * declaration of this method.
     * @return the signature of the member in internal format.
     */
    public String signature(boolean includePackageNames, boolean asSourceDecl) {
        return type().toSignature(includePackageNames, asSourceDecl);
    }

    /**
     * Return a String representation of this member.
     * @param includePackageNames If true, then use fully qualified class names.
     * @param asSourceDecl If true, then use a format corresponding with the source level
     * declaration of this member.
     * @param includeParent If true, include the class owning this member in the string.
     * @return a String representation of the member.
     */
    public String toString(boolean includePackageNames, boolean asSourceDecl, boolean includeParent) {
        String result = type().toSignature(includePackageNames, asSourceDecl);
        String name   = name();
        if (includeParent) {
            name = parent().toSignature(includePackageNames, asSourceDecl)+"."+name;
        }
        return result+" "+name;
    }

    /**
     * Return true if this is a field representing a primitive constant.
     * @return
     */
    public boolean isPrimitiveConstant() {
        return constantValue != null && isStatic() && isFinal() && type().isPrimitive();
    }

    /** Private constructor. */
    private Field(Type parent, String name, Type type, int flags) {
        super(parent, name, type, flags);
    }

   /* ------------------------------------------------------------------------ *\
    *                         Suite pruning                                    *
   \* ------------------------------------------------------------------------ */

    public void mark() {
        if (!isPrimitiveConstant() && !includeInSuite() && type().getLinkageError() == null) {
            vm().traceMark(this);
            vm().markDepth++;
            setFlag(JVMConst.ACC_INCLUDE);

            // Mark parent
            parent().mark();

            // Return now if the parent has a LinkageError
//            if (parent().getLinkageError() != null) {
//                vm().markDepth--;
//                return;
//            }

            // Mark field type
            type().mark();

            vm().markDepth--;
        }
    }
}
