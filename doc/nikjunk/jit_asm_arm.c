    a.nullCheck(r)
    a.boundsCheck(b, o)

Bytes

    a.ldb(d, b, o)                                  // rrr rri
    a.ldub(d, b, o)                                 // rrr rri
    a.stb(s, b, o)                                  // rrr rri

Halfs

    a.ldh(d, b, o [,2]))                            // rrr rri
    a.lduh(d, b, o [,2])                            // rrr rri
    a.sth(s, b, o [,2])                             // rrr rri

Words / Doubles

    a.ld(d, b, o [,4 or 8])                         // rrr rri
    a.st(s, b, o [,4 or 8])                         // rrr rri

ALU

    a.mov(d, s)                                     // rr ri
    a.neg(d, s)                                     // rr
    a.add(d, r1, r2)                                // rrr rri
    a.and(d, r1, r2)                                // rrr rri
    a.or(d, r1, r2)                                 // rrr rri
    a.xor(d, r1, r2)                                // rrr rri
    a.sub(d, r1, r2)                                // rrr rri
    a.shl(d, r1, r2)                                // rrr rri
    a.shr(d, r1, r2)                                // rrr rri
    a.ushr(d, r1, r2)                               // rrr rri
    a.mul(d, r1, r2)                                // rrr rri
    a.div(d, r1, r2)                                // rrr rri
    a.rem(d, r1, r2)                                // rrr rri
    a.rsb(d, r1, i)                                 // rri

Compare

    a.ifeq(x, y, l)                                 // rrl ril
    a.ifne(x, y, l)                                 // rrl ril
    a.ifgt(x, y, l)                                 // rrl ril
    a.ifge(x, y, l)                                 // rrl ril
    a.iflt(x, y, l)                                 // rrl ril
    a.ifle(x, y, l)                                 // rrl ril
    a.iflo(x, y, l)                                 // rrl ril
    a.iflos(x, y, l)                                // rrl ril
    a.ifhi(x, y, l)                                 // rrl ril
    a.ifhis(x, y, l)                                // rrl ril

Call / Ret / Jump

    a.invoke(x, r)                                  // lr rr
    a.ret(r)                                        // r
    a.jump(l)                                       // l

Calling C

    a.cparm(x)                                      // r i
    a.ccall("ftn" [,r])                             // string [r]

    a.ccall([r,] "ftn" [,p1] [,p2] ... [,pn])       // [r] String [r i]

Push / Pop

    a.push(s ...)                                   // r i
    a.pop(d ...)                                    // r

    a.push(m)                                       // m
    a.pop(m)                                        // m

Binding

    l = new label(addr)

    l = new label()
    a.bind(l)

Convert

    a.i2b(d, s)                                     // rr
    a.i2s(d, s)                                     // rr
    a.i2c(d, s)                                     // rr
    a.cvt(d, s)                                     // rr












