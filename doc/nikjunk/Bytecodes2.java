/*

    instruction                 suffix bytes



64

    const_n 0-15                                                                                                    8
    class_n 0-15                                                                                                        getfield_i                  1
    load_n  0-15                                                                                                        getfield_s                  1
    store_n 0-15                                                                                                        getfield_c                  1
                                                                                                                        getfield_b                  1
64                                                                                                                      putfield_i                  1
    if      cc          offset                                                                                          putfield_s                  1
    MemOp   code                                                                                                        putfield_o                  1
    AluOp   code                                                                                                        putfield_b                  1
    OtherOp code
                                                                                                                    4
                                                                                                                        getstatic_i                 1
10                                                                                                                      getstatic_o                 1
    const_byte                  1                                                                                       putstatic_i                 1
    const_char                  2                                                                                       putstatic_o                 1
    const_short                 2
    const_int                   4                                                                                   8
                                                                                                                        load0_getfield_i            1
    class                       1                                                                                       load0_getfield_s            1
    load                        1                                                                                       load0_getfield_c            1
    store                       1                                                                                       load0_getfield_b            1
                                                                                                                        load0_putfield_i            1
    class_wide                  2                                                                                       load0_putfield_s            1
    load_wide                   2                                                                                       load0_putfield_o            1
    store_wide                  2                                                                                       load0_putfield_b            1

8                                                                                                                   3
    getfield_i                  1                                                                                       invokevirtial               1
    getfield_s                  1                                                                                       invokeabsolute              1
    getfield_c                  1                                                                                       invokeinterface             1
    getfield_b                  1
    putfield_i                  1
    putfield_s                  1
    putfield_o                  1                                                                                   8
    putfield_b                  1                                                                                       getfield_i_wide             2
                                                                                                                        getfield_s_wide             2
4                                                                                                                       getfield_c_wide             2
    getstatic_i                 1                                                                                       getfield_b_wide             2
    getstatic_o                 1                                                                                       putfield_i_wide             2
    putstatic_i                 1                                                                                       putfield_s_wide             2
    putstatic_o                 1                                                                                       putfield_o_wide             2
                                                                                                                        putfield_b_wide             2
8
    load0_getfield_i            1                                                                                   4
    load0_getfield_s            1                                                                                       getstatic_i_wide            2
    load0_getfield_c            1                                                                                       getstatic_o_wide            2
    load0_getfield_b            1                                                                                       putstatic_i_wide            2
    load0_putfield_i            1                                                                                       putstatic_o_wide            2
    load0_putfield_s            1
    load0_putfield_o            1                                                                                   8
    load0_putfield_b            1                                                                                       load0_getfield_i_wide       2
                                                                                                                        load0_getfield_s_wide       2
3                                                                                                                       load0_getfield_c_wide       2
    invokevirtial               1                                                                                       load0_getfield_b_wide       2
    invokeabsolute              1                                                                                       load0_putfield_i_wide       2
    invokeinterface             1                                                                                       load0_putfield_s_wide       2
                                                                                                                        load0_putfield_o_wide       2
                                                                                                                        load0_putfield_b_wide       2

                                                                                                                    3
                                                                                                                        invokevirtial_wide          2
                                                                                                                        invokeabsolute_wide         2
                                                                                                                        invokeinterface_wide        2



    field   access      slot                    (virt/static, get/put, operand type)
    invoke              slot


    MemOp   code
    AluOp   code
    OtherOp code
    if      cc          offset







    wide

    const_b             byte
    const_s             short
    const_c             char
    const_i             int

    load                offset
    store               offset
    class               offset
    invoke              slot

*/




    class_5 field offset

    class 23 field offset

    class 23 wide field offset offset




/*

        Instruction                            with altfix


        posfix      +val
        negfix      -val
        altfix      +val
        const       imm4                       ldconst

        load        local4                     expand
        store       local4                     primitive
        goto        offset4
        ifz         offset4

        invoke      imm4  (& slot)             getstatic_i
        aluOp       code                       getstatic_o
        memOp       code                       putstatic_i
        otherOp     code                       putstatic_o

        getfield_i  imm4                       putfield_i
        getfield_s  imm4                       putfield_s
        getfield_c  imm4                       putfield_o
        getfield_b  imm4                       putfield_b




alu codes           add sub and or xor sll srl sra eq lt le ne gt ge mul
mem codes           ldi lds ldc ldb sti sts stb sto
other codes         ret0 ret1 ret2 yield bpt nop neg inc div rem i2b i2s i2c
                    tsw lsw pop menter mexit new

*/





Primitive data types
--------------------

    void
    int
    long1
    long2
    float
    double1
    double2
    boolean
    char
    short
    byte

    i2f
    i2l1
    i2l2
    i2d1
    i2d2


133 (0x85) i2l
134 (0x86) i2f
135 (0x87) i2d
136 (0x88) l2i
137 (0x89) l2f
138 (0x8a) l2d
139 (0x8b) f2i
140 (0x8c) f2l
141 (0x8d) f2d
142 (0x8e) d2i
143 (0x8f) d2l
144 (0x90) d2f




















Wide (22)
=========

    widen 0-15
    wide_short
    wide_char
    wide_int
    wide_float
    wide_long
    wide_double


Constants (18)
==============

    iconst_n 0-15               0
    iconst                      1

    iconst_m1                   0


Constant object references (17)
===============================

    object_n 0-15               0
    object                      1


Class references (17)
=====================

    class_n 0-15                0
    class                       1



Local variable access (34)
==========================

    load_n 0-15                 0
    load                        1

    store_n 0-15                0
    store                       1



Extend (17)
===========

    extend_n 0-15               0
    extend                      1



Array access (2) <8>
============

    aload
    astore

Field access (6) <40>
=================


    getfield                    1
    putfield                    1

    this_getfield               1
    this_putfield               1

    getstatic                   1
    putstatic                   1




Invoke (3)
==========

    invokevirtual               1
    invokeabsolute              1
    invokeinterface             1



If (13)
=======

    ifeq                        1
    ifne                        1
    iflt                        1
    ifle                        1
    ifgt                        1
    ifge                        1
    if_icmpeq                   1
    if_icmpne                   1
    if_icmplt                   1
    if_icmple                   1
    if_icmpgt                   1
    if_icmpge                   1
    goto                        1



ALU (26)
========

    iadd
    isub
    iand
    ior
    ixor
    ishl
    ishr
    iushr
    imul
    idiv
    irem
    return  (signature of method shows what to return)
    yield
    bpt
    nop
    pop
    neg
    inc
    i2b
    i2s
    i2c
    tableswitch
    lookupswitch
    monitorenter
    monitorexit
    new







    parm    <type>
    parm    <type>
    parm    <type>
    parm    <type>
    local   <type>
    local   <type>
    local   <type>
    local   <type>
    local   <type>
    local   <type>
    local   <type>

    load    1
    load    2
    iadd
    store   5

    etc.


-------------------



    load_4


    oconst_6
    lookup
    oconst_7
    switch











wide
class
object
load
store
extend



iconst_0
iconst_1
iconst_2
iconst_3
iconst_4
iconst_5
iconst_6
iconst_7
iconst_8
iconst_9
iconst_10
iconst_11
iconst_12
iconst_13
iconst_14
iconst_15






































































=================================================================

// Bytecodes with implicit operands

iconst_0-15
wide_0-15
class_0-15
object_0-15
load_0-15
store_0-15
extend_0-15


wide_short // Simple bytecodes
wide_char
wide_int
wide_float
wide_long
wide_double
iconst_m1
aload_b
aload_s
aload_c
aload_i
astore_b
astore_s
astore_o
astore_i
iadd
isub
iand
ior
ixor
ishl
ishr
iushr
imul
idiv
irem
return
return_1
return_2
yield
bpt
nop
pop
neg
inc
i2b
i2s
i2c
lookup
tableswitch
monitorenter
monitorexit
new
newarray
newdimension
eq
lt
le
ne
gt
ge


iconst                      // Bytecodes with succeeding operand
class
object
load
store
extend
invoke
invokevirtual
invokeinterface
ifeq
ifne
iflt
ifle
ifgt
ifge
if_icmpeq
if_icmpne
if_icmplt
if_icmple
if_icmpgt
if_icmpge
goto

getstatic
putstatic
getfield
putfield
this_getfield
this_putfield

getstatic_i
getstatic_o
putstatic_i
putstatic_o
getfield_i
getfield_s
getfield_c
getfield_b
putfield_i
putfield_s
putfield_o
putfield_b
this_getfield_i
this_getfield_s
this_getfield_c
this_getfield_b
this_putfield_i
this_putfield_s
this_putfield_o
this_putfield_b