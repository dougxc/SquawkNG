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





Constants (21)
==============

    const_m1                    0
    const_n 0-15                0
    const_byte                  1
    const_char                  2
    const_short                 2
    const_int                   4


Local variable access (36)
==========================

    load_n 0-15                 0
    load                        1
    load_wide                   2
    store_n 0-15                0
    store                       1
    store_wide                  2


Class references (18)
=====================

    class_n 0-15                0
    class                       1
    class_wide                  2


Constant object references (18)
===============================

    object_n 0-15               0
    object                      1
    object_wide                 2


Array access (8)
============

    ldi
    lds
    ldc
    ldb
    sti
    sts
    sto
    stb

Field access (40)
=================

    getfield_i                  1
    getfield_s                  1
    getfield_c                  1
    getfield_b                  1
    putfield_i                  1
    putfield_s                  1
    putfield_o                  1
    putfield_b                  1
    this_getfield_i             1
    this_getfield_s             1
    this_getfield_c             1
    this_getfield_b             1
    this_putfield_i             1
    this_putfield_s             1
    this_putfield_o             1
    this_putfield_b             1

    getfield_i_wide             2
    getfield_s_wide             2
    getfield_c_wide             2
    getfield_b_wide             2
    putfield_i_wide             2
    putfield_s_wide             2
    putfield_o_wide             2
    putfield_b_wide             2
    this_getfield_i_wide        2
    this_getfield_s_wide        2
    this_getfield_c_wide        2
    this_getfield_b_wide        2
    this_putfield_i_wide        2
    this_putfield_s_wide        2
    this_putfield_o_wide        2
    this_putfield_b_wide        2

    getstatic_i                 1
    getstatic_o                 1
    putstatic_i                 1
    putstatic_o                 1

    getstatic_i_wide            2
    getstatic_o_wide            2
    putstatic_i_wide            2
    putstatic_o_wide            2



Invoke (6)
==========

    invokevirtial               1
    invokeabsolute              1
    invokeinterface             1
    invokevirtial_wide          2
    invokeabsolute_wide         2
    invokeinterface_wide        2


If (26)
=======

    ifeq                        1
    ifne                        1
    iflt                        1
    ifle                        1
    ifgt                        1
    ifge                        1
    ifeqz                       1
    ifnez                       1
    ifltz                       1
    iflez                       1
    ifgtz                       1
    ifgez                       1
    goto                        1
    ifeq_wide                   2
    ifne_wide                   2
    iflt_wide                   2
    ifle_wide                   2
    ifgt_wide                   2
    ifge_wide                   2
    ifeqz_wide                  2
    ifnez_wide                  2
    ifltz_wide                  2
    iflez_wide                  2
    ifgtz_wide                  2
    ifgez_wide                  2
    goto_wide                   2


ALU (34)
========

    add
    sub
    and
    or
    xor
    sll
    srl
    sra
    eq
    lt
    le
    ne
    gt
    ge
    mul
    div
    rem
    return0
    return1
    return2
    yield
    bpt
    nop
    neg
    inc
    i2b
    i2s
    i2c
    tsw
    lsw
    pop
    menter
    mexit
    new
