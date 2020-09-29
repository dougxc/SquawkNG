package com.sun.squawk.translator.util;



/**
 * Verifier errors constants.
 */
public final class VEConst {
    private final String description;
    private VEConst(String description) {
        this.description = description;
    }
    public String toString() {
        return description;
    }

    public static final VEConst STACK_OVERFLOW            = new VEConst("Stack Overflow");
    public static final VEConst STACK_UNDERFLOW           = new VEConst("Stack Underflow");
    public static final VEConst STACK_EXPECT_CAT1         = new VEConst("Unexpected Long or Double on Stack");
    public static final VEConst STACK_BAD_TYPE            = new VEConst("Bad type on stack");
    public static final VEConst LOCALS_OVERFLOW           = new VEConst("Too many locals");
    public static final VEConst LOCALS_BAD_TYPE           = new VEConst("Bad type in local");
    public static final VEConst LOCALS_UNDERFLOW          = new VEConst("Locals underflow");
    public static final VEConst TARGET_BAD_TYPE           = new VEConst("Inconsistent or missing stackmap at target");
    public static final VEConst BACK_BRANCH_UNINIT        = new VEConst("Backwards branch with uninitialized object");
    public static final VEConst SEQ_BAD_TYPE              = new VEConst("Inconsistent stackmap at next instruction");
    public static final VEConst EXPECT_CLASS              = new VEConst("Expect constant pool entry of type class");
    public static final VEConst EXPECT_THROWABLE          = new VEConst("Expect subclass of java.lang.Throwable");
    public static final VEConst BAD_LOOKUPSWITCH          = new VEConst("Items in lookupswitch not sorted");
    public static final VEConst BAD_LDC                   = new VEConst("Bad constant pool for ldc");
    public static final VEConst BALOAD_BAD_TYPE           = new VEConst("baload requires byte[] or boolean[]");
    public static final VEConst AALOAD_BAD_TYPE           = new VEConst("aaload requires subtype of Object[]");
    public static final VEConst BASTORE_BAD_TYPE          = new VEConst("bastore requires byte[] or boolean[]");
    public static final VEConst AASTORE_BAD_TYPE          = new VEConst("bad array or element type for aastore");
    public static final VEConst FIELD_BAD_TYPE            = new VEConst("FIELD_BAD_TYPE");
    public static final VEConst EXPECT_METHODREF          = new VEConst("Bad constant pool type for invoker");
    public static final VEConst ARGS_NOT_ENOUGH           = new VEConst("Insufficient args on stack for method call");
    public static final VEConst ARGS_BAD_TYPE             = new VEConst("Bad arguments on stack for method call");
    public static final VEConst EXPECT_INVOKESPECIAL      = new VEConst("Bad invocation of initialization method");
    public static final VEConst EXPECT_NEW                = new VEConst("Bad stackmap reference to uninitialized object");
    public static final VEConst EXPECT_UNINIT             = new VEConst("Initializer called on already initialized object");
    public static final VEConst BAD_INSTR                 = new VEConst("Illegal byte code");
    public static final VEConst EXPECT_ARRAY              = new VEConst("arraylength on non-array");
    public static final VEConst MULTIANEWARRAY            = new VEConst("Bad dimension for multianewarray");
    public static final VEConst EXPECT_NO_RETVAL          = new VEConst("Value returned from void method");
    public static final VEConst RETVAL_BAD_TYPE           = new VEConst("Wrong value returned from method");
    public static final VEConst EXPECT_RETVAL             = new VEConst("Value not returned from method");
    public static final VEConst RETURN_UNINIT_THIS        = new VEConst("Initializer not initializing this");
    public static final VEConst BAD_STACKMAP              = new VEConst("Illegal offset for stackmap");
    public static final VEConst FALL_THROUGH              = new VEConst("Code can fall off the bottom");
    public static final VEConst EXPECT_ZERO               = new VEConst("Last byte of invokeinterface must be zero");
    public static final VEConst NARGS_MISMATCH            = new VEConst("Bad nargs field for invokeinterface");
    public static final VEConst INVOKESPECIAL             = new VEConst("Bad call to invokespecial");
    public static final VEConst BAD_INIT_CALL             = new VEConst("Bad call to <init> method");
    public static final VEConst EXPECT_FIELDREF           = new VEConst("Constant pool entry must be a field reference");
    public static final VEConst FINAL_METHOD_OVERRIDE     = new VEConst("Override of final method");
    public static final VEConst MIDDLE_OF_BYTE_CODE       = new VEConst("Code ends in middle of byte code");

}
