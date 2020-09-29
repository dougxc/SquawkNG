/*
 * @(#)Bytecodes.java                   1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1;

/**
 * Specifies constants for all bytecodes used in the Virtual Machine and
 * provides utility functions to get bytecode attributes.
 *
 * @see      <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/VMSpecTOC.doc.html">The Java Virtual Machine Specification</a>
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Bytecodes {
    /**
     * A Java bytecode constant.
     */
    public static final int
            _illegal              =  -1, _nop                  =   0,
            _aconst_null          =   1, _iconst_m1            =   2,
            _iconst_0             =   3, _iconst_1             =   4,
            _iconst_2             =   5, _iconst_3             =   6,
            _iconst_4             =   7, _iconst_5             =   8,
            _lconst_0             =   9, _lconst_1             =  10,
            _fconst_0             =  11, _fconst_1             =  12,
            _fconst_2             =  13, _dconst_0             =  14,
            _dconst_1             =  15, _bipush               =  16,
            _sipush               =  17, _ldc                  =  18,
            _ldc_w                =  19, _ldc2_w               =  20,
            _iload                =  21, _lload                =  22,
            _fload                =  23, _dload                =  24,
            _aload                =  25, _iload_0              =  26,
            _iload_1              =  27, _iload_2              =  28,
            _iload_3              =  29, _lload_0              =  30,
            _lload_1              =  31, _lload_2              =  32,
            _lload_3              =  33, _fload_0              =  34,
            _fload_1              =  35, _fload_2              =  36,
            _fload_3              =  37, _dload_0              =  38,
            _dload_1              =  39, _dload_2              =  40,
            _dload_3              =  41, _aload_0              =  42,
            _aload_1              =  43, _aload_2              =  44,
            _aload_3              =  45, _iaload               =  46,
            _laload               =  47, _faload               =  48,
            _daload               =  49, _aaload               =  50,
            _baload               =  51, _caload               =  52,
            _saload               =  53, _istore               =  54,
            _lstore               =  55, _fstore               =  56,
            _dstore               =  57, _astore               =  58,
            _istore_0             =  59, _istore_1             =  60,
            _istore_2             =  61, _istore_3             =  62,
            _lstore_0             =  63, _lstore_1             =  64,
            _lstore_2             =  65, _lstore_3             =  66,
            _fstore_0             =  67, _fstore_1             =  68,
            _fstore_2             =  69, _fstore_3             =  70,
            _dstore_0             =  71, _dstore_1             =  72,
            _dstore_2             =  73, _dstore_3             =  74,
            _astore_0             =  75, _astore_1             =  76,
            _astore_2             =  77, _astore_3             =  78,
            _iastore              =  79, _lastore              =  80,
            _fastore              =  81, _dastore              =  82,
            _aastore              =  83, _bastore              =  84,
            _castore              =  85, _sastore              =  86,
            _pop                  =  87, _pop2                 =  88,
            _dup                  =  89, _dup_x1               =  90,
            _dup_x2               =  91, _dup2                 =  92,
            _dup2_x1              =  93, _dup2_x2              =  94,
            _swap                 =  95, _iadd                 =  96,
            _ladd                 =  97, _fadd                 =  98,
            _dadd                 =  99, _isub                 = 100,
            _lsub                 = 101, _fsub                 = 102,
            _dsub                 = 103, _imul                 = 104,
            _lmul                 = 105, _fmul                 = 106,
            _dmul                 = 107, _idiv                 = 108,
            _ldiv                 = 109, _fdiv                 = 110,
            _ddiv                 = 111, _irem                 = 112,
            _lrem                 = 113, _frem                 = 114,
            _drem                 = 115, _ineg                 = 116,
            _lneg                 = 117, _fneg                 = 118,
            _dneg                 = 119, _ishl                 = 120,
            _lshl                 = 121, _ishr                 = 122,
            _lshr                 = 123, _iushr                = 124,
            _lushr                = 125, _iand                 = 126,
            _land                 = 127, _ior                  = 128,
            _lor                  = 129, _ixor                 = 130,
            _lxor                 = 131, _iinc                 = 132,
            _i2l                  = 133, _i2f                  = 134,
            _i2d                  = 135, _l2i                  = 136,
            _l2f                  = 137, _l2d                  = 138,
            _f2i                  = 139, _f2l                  = 140,
            _f2d                  = 141, _d2i                  = 142,
            _d2l                  = 143, _d2f                  = 144,
            _i2b                  = 145, _i2c                  = 146,
            _i2s                  = 147, _lcmp                 = 148,
            _fcmpl                = 149, _fcmpg                = 150,
            _dcmpl                = 151, _dcmpg                = 152,
            _ifeq                 = 153, _ifne                 = 154,
            _iflt                 = 155, _ifge                 = 156,
            _ifgt                 = 157, _ifle                 = 158,
            _if_icmpeq            = 159, _if_icmpne            = 160,
            _if_icmplt            = 161, _if_icmpge            = 162,
            _if_icmpgt            = 163, _if_icmple            = 164,
            _if_acmpeq            = 165, _if_acmpne            = 166,
            _goto                 = 167, _jsr                  = 168,
            _ret                  = 169, _tableswitch          = 170,
            _lookupswitch         = 171, _ireturn              = 172,
            _lreturn              = 173, _freturn              = 174,
            _dreturn              = 175, _areturn              = 176,
            _return               = 177, _getstatic            = 178,
            _putstatic            = 179, _getfield             = 180,
            _putfield             = 181, _invokevirtual        = 182,
            _invokespecial        = 183, _invokestatic         = 184,
            _invokeinterface      = 185, // _xxxunusedxxx      = 186,
            _new                  = 187, _newarray             = 188,
            _anewarray            = 189, _arraylength          = 190,
            _athrow               = 191, _checkcast            = 192,
            _instanceof           = 193, _monitorenter         = 194,
            _monitorexit          = 195, _wide                 = 196,
            _multianewarray       = 197, _ifnull               = 198,
            _ifnonnull            = 199, _goto_w               = 200,
            _jsr_w                = 201, _breakpoint           = 202;

    /**
     * A Java Virtual Machine bytecode constant.
     */
    public static final int
            _fast_ildc            = 203, _fast_fldc            = 204,
            _fast_aldc            = 205, _fast_ildc_w          = 206,
            _fast_fldc_w          = 207, _fast_aldc_w          = 208,
            _fast_lldc_w          = 209, _fast_dldc_w          = 210,
            _fast_aload_0         = 211, _fast_iaccess_0       = 212,
            _fast_aaccess_0       = 213, _fast_linearswitch    = 214,
            _fast_binaryswitch    = 215, _fast_putstatic       = 216,
            _fast_igetstatic      = 217, _fast_lgetstatic      = 218,
            _fast_fgetstatic      = 219, _fast_dgetstatic      = 220,
            _fast_agetstatic      = 221, _fast_iputstatic      = 222,
            _fast_lputstatic      = 223, _fast_fputstatic      = 224,
            _fast_dputstatic      = 225, _fast_aputstatic      = 226,
            _fast_getfield        = 227, _fast_putfield        = 228,
            _fast_igetfield       = 229, _fast_lgetfield       = 230,
            _fast_fgetfield       = 231, _fast_dgetfield       = 232,
            _fast_agetfield       = 233, _fast_iputfield       = 234,
            _fast_lputfield       = 235, _fast_fputfield       = 236,
            _fast_dputfield       = 237, _fast_aputfield       = 238,
            _fast_invokevtable    = 239, _fast_invokevfinal    = 240,
            _fast_invokespecial   = 241, _fast_invokestatic    = 242,
            _fast_invokeinterface = 243, _fast_new             = 244;

    /**
     * The number of Java bytecodes.
     */
    public static final int NUM_JAVA_CODES = 203;

    /**
     * The total number of codes including bytecodes of the Virtual Machine.
     */
    public static final int NUM_CODES = 245;

    /**
     * The mnemonics of the bytecodes.
     */
    private static String[] name = new String[NUM_CODES];

    /**
     * The length of the bytecodes.
     */
    private static int[] length = new int[NUM_CODES];

    /**
     * The length of the bytecodes' wide forms.
     */
    private static int[] wideLen = new int[NUM_CODES];

    /**
     * The corresponding Java bytecodes.
     */
    private static int[] javaCode = new int[NUM_CODES];

    /**
     * Whether or not the bytecodes can throw exceptions.
     */
    private static boolean[] canTrap = new boolean[NUM_CODES];

    /**
     * Whether or not the bytecodes can throw asynchronous exceptions.
     */
    private static boolean[] isAsync = new boolean[NUM_CODES];

    /**
     * Defines the attributes of the bytecodes.
     */
    static {
        define(_nop                 , "nop"                 , 1, 0, false, false);
        define(_aconst_null         , "aconst_null"         , 1, 0, false, false);
        define(_iconst_m1           , "iconst_m1"           , 1, 0, false, false);
        define(_iconst_0            , "iconst_0"            , 1, 0, false, false);
        define(_iconst_1            , "iconst_1"            , 1, 0, false, false);
        define(_iconst_2            , "iconst_2"            , 1, 0, false, false);
        define(_iconst_3            , "iconst_3"            , 1, 0, false, false);
        define(_iconst_4            , "iconst_4"            , 1, 0, false, false);
        define(_iconst_5            , "iconst_5"            , 1, 0, false, false);
        define(_lconst_0            , "lconst_0"            , 1, 0, false, false);
        define(_lconst_1            , "lconst_1"            , 1, 0, false, false);
        define(_fconst_0            , "fconst_0"            , 1, 0, false, false);
        define(_fconst_1            , "fconst_1"            , 1, 0, false, false);
        define(_fconst_2            , "fconst_2"            , 1, 0, false, false);
        define(_dconst_0            , "dconst_0"            , 1, 0, false, false);
        define(_dconst_1            , "dconst_1"            , 1, 0, false, false);
        define(_bipush              , "bipush"              , 2, 0, false, false);
        define(_sipush              , "sipush"              , 3, 0, false, false);
        define(_ldc                 , "ldc"                 , 2, 0, true , false);
        define(_ldc_w               , "ldc_w"               , 3, 0, true , false);
        define(_ldc2_w              , "ldc2_w"              , 3, 0, true , false);
        define(_iload               , "iload"               , 2, 4, false, false);
        define(_lload               , "lload"               , 2, 4, false, false);
        define(_fload               , "fload"               , 2, 4, false, false);
        define(_dload               , "dload"               , 2, 4, false, false);
        define(_aload               , "aload"               , 2, 4, false, false);
        define(_iload_0             , "iload_0"             , 1, 0, false, false);
        define(_iload_1             , "iload_1"             , 1, 0, false, false);
        define(_iload_2             , "iload_2"             , 1, 0, false, false);
        define(_iload_3             , "iload_3"             , 1, 0, false, false);
        define(_lload_0             , "lload_0"             , 1, 0, false, false);
        define(_lload_1             , "lload_1"             , 1, 0, false, false);
        define(_lload_2             , "lload_2"             , 1, 0, false, false);
        define(_lload_3             , "lload_3"             , 1, 0, false, false);
        define(_fload_0             , "fload_0"             , 1, 0, false, false);
        define(_fload_1             , "fload_1"             , 1, 0, false, false);
        define(_fload_2             , "fload_2"             , 1, 0, false, false);
        define(_fload_3             , "fload_3"             , 1, 0, false, false);
        define(_dload_0             , "dload_0"             , 1, 0, false, false);
        define(_dload_1             , "dload_1"             , 1, 0, false, false);
        define(_dload_2             , "dload_2"             , 1, 0, false, false);
        define(_dload_3             , "dload_3"             , 1, 0, false, false);
        define(_aload_0             , "aload_0"             , 1, 0, false, false);
        define(_aload_1             , "aload_1"             , 1, 0, false, false);
        define(_aload_2             , "aload_2"             , 1, 0, false, false);
        define(_aload_3             , "aload_3"             , 1, 0, false, false);
        define(_iaload              , "iaload"              , 1, 0, true , false);
        define(_laload              , "laload"              , 1, 0, true , false);
        define(_faload              , "faload"              , 1, 0, true , false);
        define(_daload              , "daload"              , 1, 0, true , false);
        define(_aaload              , "aaload"              , 1, 0, true , false);
        define(_baload              , "baload"              , 1, 0, true , false);
        define(_caload              , "caload"              , 1, 0, true , false);
        define(_saload              , "saload"              , 1, 0, true , false);
        define(_istore              , "istore"              , 2, 4, false, false);
        define(_lstore              , "lstore"              , 2, 4, false, false);
        define(_fstore              , "fstore"              , 2, 4, false, false);
        define(_dstore              , "dstore"              , 2, 4, false, false);
        define(_astore              , "astore"              , 2, 4, false, false);
        define(_istore_0            , "istore_0"            , 1, 0, false, false);
        define(_istore_1            , "istore_1"            , 1, 0, false, false);
        define(_istore_2            , "istore_2"            , 1, 0, false, false);
        define(_istore_3            , "istore_3"            , 1, 0, false, false);
        define(_lstore_0            , "lstore_0"            , 1, 0, false, false);
        define(_lstore_1            , "lstore_1"            , 1, 0, false, false);
        define(_lstore_2            , "lstore_2"            , 1, 0, false, false);
        define(_lstore_3            , "lstore_3"            , 1, 0, false, false);
        define(_fstore_0            , "fstore_0"            , 1, 0, false, false);
        define(_fstore_1            , "fstore_1"            , 1, 0, false, false);
        define(_fstore_2            , "fstore_2"            , 1, 0, false, false);
        define(_fstore_3            , "fstore_3"            , 1, 0, false, false);
        define(_dstore_0            , "dstore_0"            , 1, 0, false, false);
        define(_dstore_1            , "dstore_1"            , 1, 0, false, false);
        define(_dstore_2            , "dstore_2"            , 1, 0, false, false);
        define(_dstore_3            , "dstore_3"            , 1, 0, false, false);
        define(_astore_0            , "astore_0"            , 1, 0, false, false);
        define(_astore_1            , "astore_1"            , 1, 0, false, false);
        define(_astore_2            , "astore_2"            , 1, 0, false, false);
        define(_astore_3            , "astore_3"            , 1, 0, false, false);
        define(_iastore             , "iastore"             , 1, 0, true , false);
        define(_lastore             , "lastore"             , 1, 0, true , false);
        define(_fastore             , "fastore"             , 1, 0, true , false);
        define(_dastore             , "dastore"             , 1, 0, true , false);
        define(_aastore             , "aastore"             , 1, 0, true , false);
        define(_bastore             , "bastore"             , 1, 0, true , false);
        define(_castore             , "castore"             , 1, 0, true , false);
        define(_sastore             , "sastore"             , 1, 0, true , false);
        define(_pop                 , "pop"                 , 1, 0, false, false);
        define(_pop2                , "pop2"                , 1, 0, false, false);
        define(_dup                 , "dup"                 , 1, 0, false, false);
        define(_dup_x1              , "dup_x1"              , 1, 0, false, false);
        define(_dup_x2              , "dup_x2"              , 1, 0, false, false);
        define(_dup2                , "dup2"                , 1, 0, false, false);
        define(_dup2_x1             , "dup2_x1"             , 1, 0, false, false);
        define(_dup2_x2             , "dup2_x2"             , 1, 0, false, false);
        define(_swap                , "swap"                , 1, 0, false, false);
        define(_iadd                , "iadd"                , 1, 0, false, false);
        define(_ladd                , "ladd"                , 1, 0, false, false);
        define(_fadd                , "fadd"                , 1, 0, false, false);
        define(_dadd                , "dadd"                , 1, 0, false, false);
        define(_isub                , "isub"                , 1, 0, false, false);
        define(_lsub                , "lsub"                , 1, 0, false, false);
        define(_fsub                , "fsub"                , 1, 0, false, false);
        define(_dsub                , "dsub"                , 1, 0, false, false);
        define(_imul                , "imul"                , 1, 0, false, false);
        define(_lmul                , "lmul"                , 1, 0, false, false);
        define(_fmul                , "fmul"                , 1, 0, false, false);
        define(_dmul                , "dmul"                , 1, 0, false, false);
        define(_idiv                , "idiv"                , 1, 0, true , false);
        define(_ldiv                , "ldiv"                , 1, 0, true , false);
        define(_fdiv                , "fdiv"                , 1, 0, false, false);
        define(_ddiv                , "ddiv"                , 1, 0, false, false);
        define(_irem                , "irem"                , 1, 0, true , false);
        define(_lrem                , "lrem"                , 1, 0, true , false);
        define(_frem                , "frem"                , 1, 0, false, false);
        define(_drem                , "drem"                , 1, 0, false, false);
        define(_ineg                , "ineg"                , 1, 0, false, false);
        define(_lneg                , "lneg"                , 1, 0, false, false);
        define(_fneg                , "fneg"                , 1, 0, false, false);
        define(_dneg                , "dneg"                , 1, 0, false, false);
        define(_ishl                , "ishl"                , 1, 0, false, false);
        define(_lshl                , "lshl"                , 1, 0, false, false);
        define(_ishr                , "ishr"                , 1, 0, false, false);
        define(_lshr                , "lshr"                , 1, 0, false, false);
        define(_iushr               , "iushr"               , 1, 0, false, false);
        define(_lushr               , "lushr"               , 1, 0, false, false);
        define(_iand                , "iand"                , 1, 0, false, false);
        define(_land                , "land"                , 1, 0, false, false);
        define(_ior                 , "ior"                 , 1, 0, false, false);
        define(_lor                 , "lor"                 , 1, 0, false, false);
        define(_ixor                , "ixor"                , 1, 0, false, false);
        define(_lxor                , "lxor"                , 1, 0, false, false);
        define(_iinc                , "iinc"                , 3, 6, false, false);
        define(_i2l                 , "i2l"                 , 1, 0, false, false);
        define(_i2f                 , "i2f"                 , 1, 0, false, false);
        define(_i2d                 , "i2d"                 , 1, 0, false, false);
        define(_l2i                 , "l2i"                 , 1, 0, false, false);
        define(_l2f                 , "l2f"                 , 1, 0, false, false);
        define(_l2d                 , "l2d"                 , 1, 0, false, false);
        define(_f2i                 , "f2i"                 , 1, 0, false, false);
        define(_f2l                 , "f2l"                 , 1, 0, false, false);
        define(_f2d                 , "f2d"                 , 1, 0, false, false);
        define(_d2i                 , "d2i"                 , 1, 0, false, false);
        define(_d2l                 , "d2l"                 , 1, 0, false, false);
        define(_d2f                 , "d2f"                 , 1, 0, false, false);
        define(_i2b                 , "i2b"                 , 1, 0, false, false);
        define(_i2c                 , "i2c"                 , 1, 0, false, false);
        define(_i2s                 , "i2s"                 , 1, 0, false, false);
        define(_lcmp                , "lcmp"                , 1, 0, false, false);
        define(_fcmpl               , "fcmpl"               , 1, 0, false, false);
        define(_fcmpg               , "fcmpg"               , 1, 0, false, false);
        define(_dcmpl               , "dcmpl"               , 1, 0, false, false);
        define(_dcmpg               , "dcmpg"               , 1, 0, false, false);
        define(_ifeq                , "ifeq"                , 3, 0, false, true );
        define(_ifne                , "ifne"                , 3, 0, false, true );
        define(_iflt                , "iflt"                , 3, 0, false, true );
        define(_ifge                , "ifge"                , 3, 0, false, true );
        define(_ifgt                , "ifgt"                , 3, 0, false, true );
        define(_ifle                , "ifle"                , 3, 0, false, true );
        define(_if_icmpeq           , "if_icmpeq"           , 3, 0, false, true );
        define(_if_icmpne           , "if_icmpne"           , 3, 0, false, true );
        define(_if_icmplt           , "if_icmplt"           , 3, 0, false, true );
        define(_if_icmpge           , "if_icmpge"           , 3, 0, false, true );
        define(_if_icmpgt           , "if_icmpgt"           , 3, 0, false, true );
        define(_if_icmple           , "if_icmple"           , 3, 0, false, true );
        define(_if_acmpeq           , "if_acmpeq"           , 3, 0, false, true );
        define(_if_acmpne           , "if_acmpne"           , 3, 0, false, true );
        define(_goto                , "goto"                , 3, 0, false, true );
        define(_jsr                 , "jsr"                 , 3, 0, false, true );
        define(_ret                 , "ret"                 , 2, 4, false, true );
        define(_tableswitch         , "tableswitch"         , 0, 0, false, true );
        define(_lookupswitch        , "lookupswitch"        , 0, 0, false, true );
        define(_ireturn             , "ireturn"             , 1, 0, false, true );
        define(_lreturn             , "lreturn"             , 1, 0, false, true );
        define(_freturn             , "freturn"             , 1, 0, false, true );
        define(_dreturn             , "dreturn"             , 1, 0, false, true );
        define(_areturn             , "areturn"             , 1, 0, false, true );
        define(_return              , "return"              , 1, 0, false, true );
        define(_getstatic           , "getstatic"           , 3, 0, true , false);
        define(_putstatic           , "putstatic"           , 3, 0, true , false);
        define(_getfield            , "getfield"            , 3, 0, true , false);
        define(_putfield            , "putfield"            , 3, 0, true , false);
        define(_invokevirtual       , "invokevirtual"       , 3, 0, true , false);
        define(_invokespecial       , "invokespecial"       , 3, 0, true , false);
        define(_invokestatic        , "invokestatic"        , 3, 0, true , false);
        define(_invokeinterface     , "invokeinterface"     , 5, 0, true , false);
        define(_new                 , "new"                 , 3, 0, true , false);
        define(_newarray            , "newarray"            , 2, 0, true , false);
        define(_anewarray           , "anewarray"           , 3, 0, true , false);
        define(_arraylength         , "arraylength"         , 1, 0, true , false);
        define(_athrow              , "athrow"              , 1, 0, true , false);
        define(_checkcast           , "checkcast"           , 3, 0, true , false);
        define(_instanceof          , "instanceof"          , 3, 0, true , false);
        define(_monitorenter        , "monitorenter"        , 1, 0, true , false);
        define(_monitorexit         , "monitorexit"         , 1, 0, false, false);
        define(_wide                , "wide"                , 0, 0, false, false);
        define(_multianewarray      , "multianewarray"      , 4, 0, true , false);
        define(_ifnull              , "ifnull"              , 3, 0, false, true );
        define(_ifnonnull           , "ifnonnull"           , 3, 0, false, true );
        define(_goto_w              , "goto_w"              , 5, 0, false, true );
        define(_jsr_w               , "jsr_w"               , 5, 0, false, true );
        define(_breakpoint          , "breakpoint"          , 0, 0, false, false);

        define(_fast_ildc           , "fast_ildc"           , 2, 0, _ldc            );
        define(_fast_fldc           , "fast_fldc"           , 2, 0, _ldc            );
        define(_fast_aldc           , "fast_aldc"           , 2, 0, _ldc            );
        define(_fast_ildc_w         , "fast_ildc_w"         , 3, 0, _ldc_w          );
        define(_fast_fldc_w         , "fast_fldc_w"         , 3, 0, _ldc_w          );
        define(_fast_aldc_w         , "fast_aldc_w"         , 3, 0, _ldc_w          );
        define(_fast_lldc_w         , "fast_lldc_w"         , 3, 0, _ldc2_w         );
        define(_fast_dldc_w         , "fast_dldc_w"         , 3, 0, _ldc2_w         );
        define(_fast_aload_0        , "fast_aload_0"        , 1, 0, _aload_0        );
        define(_fast_iaccess_0      , "fast_iaccess_0"      , 4, 0, _aload_0        );
        define(_fast_aaccess_0      , "fast_aaccess_0"      , 4, 0, _aload_0        );
        define(_fast_linearswitch   , "fast_linearswitch"   , 0, 0, _lookupswitch   );
        define(_fast_binaryswitch   , "fast_binaryswitch"   , 0, 0, _lookupswitch   );
        define(_fast_putstatic      , "fast_putstatic"      , 3, 0, _putstatic      );
        define(_fast_igetstatic     , "fast_igetstatic"     , 3, 0, _getstatic      );
        define(_fast_lgetstatic     , "fast_lgetstatic"     , 3, 0, _getstatic      );
        define(_fast_fgetstatic     , "fast_fgetstatic"     , 3, 0, _getstatic      );
        define(_fast_dgetstatic     , "fast_dgetstatic"     , 3, 0, _getstatic      );
        define(_fast_agetstatic     , "fast_agetstatic"     , 3, 0, _getstatic      );
        define(_fast_iputstatic     , "fast_iputstatic"     , 3, 0, _putstatic      );
        define(_fast_lputstatic     , "fast_lputstatic"     , 3, 0, _putstatic      );
        define(_fast_fputstatic     , "fast_fputstatic"     , 3, 0, _putstatic      );
        define(_fast_dputstatic     , "fast_dputstatic"     , 3, 0, _putstatic      );
        define(_fast_aputstatic     , "fast_aputstatic"     , 3, 0, _putstatic      );
        define(_fast_getfield       , "fast_getfield"       , 3, 0, _getfield       );
        define(_fast_putfield       , "fast_putfield"       , 3, 0, _putfield       );
        define(_fast_igetfield      , "fast_igetfield"      , 3, 0, _getfield       );
        define(_fast_lgetfield      , "fast_lgetfield"      , 3, 0, _getfield       );
        define(_fast_fgetfield      , "fast_fgetfield"      , 3, 0, _getfield       );
        define(_fast_dgetfield      , "fast_dgetfield"      , 3, 0, _getfield       );
        define(_fast_agetfield      , "fast_agetfield"      , 3, 0, _getfield       );
        define(_fast_iputfield      , "fast_iputfield"      , 3, 0, _putfield       );
        define(_fast_lputfield      , "fast_lputfield"      , 3, 0, _putfield       );
        define(_fast_fputfield      , "fast_fputfield"      , 3, 0, _putfield       );
        define(_fast_dputfield      , "fast_dputfield"      , 3, 0, _putfield       );
        define(_fast_aputfield      , "fast_aputfield"      , 3, 0, _putfield       );
        define(_fast_invokevtable   , "fast_invokevtable"   , 3, 0, _invokevirtual  );
        define(_fast_invokevfinal   , "fast_invokevfinal"   , 3, 0, _invokevirtual  );
        define(_fast_invokespecial  , "fast_invokespecial"  , 3, 0, _invokespecial  );
        define(_fast_invokestatic   , "fast_invokestatic"   , 3, 0, _invokestatic   );
        define(_fast_invokeinterface, "fast_invokeinterface", 5, 0, _invokeinterface);
        define(_fast_new            , "fast_new"            , 3, 0, _new            );
    }

    /**
     * Don't let anyone instantiate this class.
     */
    private Bytecodes() {}

    /**
     * Defines a bytecode and its attributes.
     *
     * @param  code        the bytecode value
     * @param  name        a mnemonic used for disassembling
     * @param  length      size of the instruction in bytes
     * @param  wideLen     size of the wide form in bytes
     * @param  javaCode    the corresponding Java bytecode
     * @param  canTrap     whether the code can throw an exception
     * @param  isAsync     whether the code can throw an asynchronous exception
     */
    private static void define(int code, String name, int length, int wideLen,
            int javaCode, boolean canTrap, boolean isAsync) {
        Bytecodes.name[code] = name;
        Bytecodes.length[code] = length;
        Bytecodes.wideLen[code] = wideLen;
        Bytecodes.javaCode[code] = javaCode;
        Bytecodes.canTrap[code] = canTrap;
        Bytecodes.isAsync[code] = isAsync;
    }

    /**
     * Defines a bytecode and its attributes.
     *
     * @see  #define(int, String, int, int, int, boolean, boolean)
     */
    private static void define(int code, String name, int length, int wideLen,
            boolean canTrap, boolean isAsync) {
        define(code, name, length, wideLen, code, canTrap, isAsync);
    }

    /**
     * Defines a bytecode and its attributes.
     *
     * @see  #define(int, String, int, int, int, boolean, boolean)
     */
    private static void define(int code, String name, int length, int wideLen,
            int javaCode) {
        define(code, name, length, wideLen, javaCode, false, false);
    }

    /**
     * Returns the name of the specified bytecode.
     *
     * @param   code  the bytecode
     * @return  name of the bytecode
     */
    public static String getName(int code) {
        return name[code];
    }

    /**
     * Returns the length of the specified bytecode instruction.
     *
     * @param   code  the bytecode
     * @return  size in bytes
     */
    public static int getLength(int code) {
        return length[code];
    }

    /**
     * Returns the length of the bytecode's wide form or 0 if the instruction
     * cannot be wide.
     *
     * @param   code  the bytecode
     * @return  size of the wide form in bytes
     */
    public static int getWideLen(int code) {
        return wideLen[code];
    }

    /**
     * Returns the corresponding Java bytecode.
     *
     * @param   code  the bytecode
     * @return  the Java bytecode
     */
    public static int getJavaCode(int code) {
        return javaCode[code];
    }

    /**
     * Returns whether or not the bytecode can throw an exception in compiled
     * code. Note that the monitorexit and the return bytecodes do not throw
     * exceptions since monitor pairing guarantees that they will succeed.
     *
     * @param   code  the bytecode
     * @return  whether or not the bytecode can throw an exception
     */
    public static boolean canTrap(int code) {
        return canTrap[code];
    }

    /**
     * Returns whether or not the bytecode can throw an asynchronous exception
     * in compiled code.
     *
     * @param   code  the bytecode
     * @return  whether or not the bytecode can throw an asynchronous exception
     */
    public static boolean isAsync(int code) {
        return isAsync[code];
    }
}
