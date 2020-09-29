

public class Test extends Super {

    int ip, sp, etc;

    void mainLoop() {
         for (;;) {
             switch (b) {
                 case OPC_IADD:   { int r = pop() ; int l = pop() ; push(iadd(l, r)); }  break;
                 case OPC_IADD:   { int r = pop() ; int l = pop() ; int res = iadd(l, r) ; push(res); }  break;

                 case OPC_IADD:     push(iadd(pop(), pop()));   break;
                 case OPC_LOAD:     push(load());               break
                 case OPC_STORE:    store(pop());               break

             }
         }
    }



    void putfield(Instance obj, int val) {
        int off = fetchUnsignedByte(ip++);
        if (obj == null) {
            throw new NullPointerException();
        }
        obj.atWordPut(off, val);
    }




        try {
            obj.atPut(off, val);
        } catch(NullPointerException ex) {
            x
        } catch(IllegalStoreException ex) {
        }


        if (nullCheck(ref)) break;
        assumeValidObjectField(ref, off, 4);
        storeWord(ref, off, val);
        if (storeFailed()) break;
        continue;
    }



    int iadd(int l, int r) {
        return l+r;
    }

    int load_0() {
        return locals.at(0);
    }

    int load_1() {
        return locals.at(1);
    }

    int load() {
        int slot = fetchUnsignedByte();
        return locals.at(slot);
    }

    void store(int value) {
        int slot = fetchUnsignedByte();
        locals.atPut(slot, value);
    }








    int idiv(int l, int r) {
        if (r == 0) {
            throw new ArithmeticException();
        } else if (l == 0x80000000 && r == -1) {
            return l;
        } else {
            return l / r;
        }
    }




}






    void mainLoop() {
        for (;;) {
            try {
                b = fetchUnsignedByte(ip++);
                switch (b) {
                    case OPC_IDIV: {
                        int r = pop();
                        int l = pop();
                        int res = idiv(l, r);
                        push(res);
                        break;
                    }
                    case OPC_GETFIELD_I: {
                        Instance inst = popInstance();
                        int res = getfield_i(inst);
                        push(res)
                        break;
                    }
                }
            } catch (ArithmeticException ex) {
                fn = ARITHMETICEXCEPTION;
            } catch (NullPointerException ex) {
                fn = NULLPOINTERXCEPTION;
            }
            callVMExtension(fn);
        }
    }




























/*MAC*/ int findClassState(int $cls) {
            int classHash = Class_getCno($cls) & (CLS_STATE_TABLE_SIZE - 1); /* Get hash code */
            int cstable   = getClassStateTable();                            /* Get hash table */
            int cs = getWord(cstable, classHash);                            /* Get first entry */
            if (cs != 0 && ClassState_getClass(cs) == $cls) {                /* Is is the class being searched for? */
                Object_assumeIsArray(cs);
                return cs;                                                   /* Y - return */
            } else {
                return findClassState2($cls);
            }
        }



        KlassState findClassState(Klass cls) {
            int classHash = cls.cno & (CLS_STATE_TABLE_SIZE - 1);           /* Get hash code */
            KlassState[] cstable = getClassStateTable();                    /* Get hash table */
            KlassState cs = cstable[classHash];                             /* Get first entry */
            if (cs != 0 && cs.klass == cls) {                               /* Is is the class being searched for? */
                return cs;                                                  /* Y - return */
            } else {
                return findClassState2(cls);
            }
        }











                case OPC_INVOKEVIRTUAL2: {
                    int mp;
                    int rcvr;
                    int vtbl;
                    int vstart;
                    int parm = fetchUnsignedByte(ip++);                 /* Get the "number" operand of invoke */

                    pushFrame();

                    rcvr = Frame_getLocal(lp, 0);                       /* Get the receiver */
                    if (nullCheck(rcvr)) {
                        lp = Frame_getPreviousLp(lp);
                        sp = lp + Frame_getStackOffset(lp);
                        break;
                    }

                    cp = Object_getClass(rcvr);                         /* Get the callee's class */
                    vstart = Class_getFirstVirtualMethod(cp);           /* Get the number of the first method defined */
                    for (;;) {
                        while (parm < vstart) {                         /* Is the target method there? */
                            cp  = Class_getSuperClass(cp);              /* Go to the super class */
                            assume(cp != 0);
                            vstart = Class_getFirstVirtualMethod(cp);   /* Get the number of the first method defined */
                        }                                               /* Test again... */
                        vtbl = Class_getVirtualMethods(cp);             /* Get the vtable for virtual methods */
                        assumeArrayIndexInBounds(vtbl, parm-vstart);
                        mp = getWord(vtbl, parm-vstart);                /* Get the method */
                        if (mp != 0) {
                            break;
                        } else {
                            cp  = Class_getSuperClass(cp);              /* N - Go to the super class */
                            assume(cp != 0);
                            vstart = Class_getFirstVirtualMethod(cp);   /* Get the number of the first method defined */
                        }
                    }

                    setupFrame(mp);

                    continue;
                }






                void invokevirtual{} {
                    int parm = fetchUnsignedByte(ip++);                 /* Get the "number" operand of invoke */

                    pushFrame();

                    Instance rcvr = lp.local[0];                        /* Get the receiver */

                    if (rcvr == null) {
                        lp = lp.previousLp;
                        setSp(lp, lp.stackOffset);
                        throw new NullPointerException();
                    }

                    cp = rcvr.klass;                                    /* Get the callee's class */
                    int vstart = cp.firstVirtualMethod;                 /* Get the number of the first method defined */
                    for (;;) {
                        while (parm < vstart) {                         /* Is the target method there? */
                            cp = cp.superKlass;                         /* Go to the super class */
                            assume(cp != null);
                            vstart = cp.firstVirtualMethod;             /* Get the number of the first method defined */
                        }                                               /* Test again... */
                        byte[][] vtbl = cp.virtualMethods;              /* Get the vtable for virtual methods */
                        assumeArrayIndexInBounds(vtbl, parm-vstart);
                        byte[] mp = vtbl[parm-vstart];                  /* Get the method */
                        if (mp != null) {
                            break;
                        } else {
                            cp = cp.superKlass;                         /* N - Go to the super class */
                            assume(cp != null);
                            vstart = cp.firstVirtualMethod;             /* Get the number of the first method defined */
                        }
                    }

                    setupFrame(mp);
                }





















                case OPC_INVOKESTATIC: {
                    int mp;
                    int vtbl;
                    int parm = fetchUnsignedByte(ip++);                 /* Get the "number" operand of invoke */

                    pushFrame();

                    cp = pop();                                         /* Get the callee's class */
                    vtbl = Class_getStaticMethods(cp);                  /* Get the vtable for virtual methods */
                    assumeArrayIndexInBounds(vtbl, parm);
                    mp = getWord(vtbl, parm);                           /* Get the method */
                    if (mp == 0) {
                        fatalVMError("OPC_INVOKESTATIC mp==0 (Probably a call to an unimplemented native method)");
                    }

                    setupFrame(mp);

                    continue;
                }


                void invokestatic() {
                    int parm = fetchUnsignedByte(ip++);                 /* Get the "number" operand of invoke */

                    pushFrame();

                    cp = popKlass();                                    /* Get the callee's class */
                    VTable vtbl =  cp.staticMethods;                    /* Get the vtable for virtual methods */
                    Method mp = vtbl.at(parm);                          /* Get the method */
                    if (mp == null) {
                        throw new VMError("OPC_INVOKESTATIC mp==null (Probably a call to an unimplemented native method)");
                    }

                    setupFrame(mp);
                }




























    int interpret(int chunk) {

        int ip, sp, lp, fn, cp, sl, bc;

        <Code to setup the above machine registers>

        for (;;) {
            int b = fetchUnsignedByte(ip++);

            switch (b) {

                case OPC_IDIV: {
                    int r = pop();
                    int l = pop();
                    if (r == 0) {
                        fn = ARITHMETICEXCEPTION;
                        break;
                    } else if (l == 0x80000000 && r == -1) {
                        push(l);
                    } else {
                        push(l / r);
                    }
                    continue;
                }

                case OPC_GETFIELD_I: {
                    int ref = pop();
                    int off = fetchUnsignedByte(ip++);
                    if (nullCheck(ref)) {
                        fn = NULLPOINTERXCEPTION
                        break;
                    }
                    push(getWord(ref, off));
                    continue;
                }

            }

            <Code to invoke a static function based upon fn>
        }
    }






    int idiv(int l, int r) {
        prefetch(ip);
        if (r == 0) {
            throw new ArithmeticException();
        } else if (l == 0x80000000 && r == -1) {
            return l;
        } else {
            return l / r;
        }
    }


    int getfield_i(Instance ref) {
        int off = fetchUnsignedByte(ip++);
        prefetch(ip);
        if (ref == null) {
            throw new NullPointerException();
        }
        return ref.wordAt(off);
    }


    void icmplt(int l, int r);
        int offset = fetchByte(ip++);
        if (l <  r) {
            ip += offset;
        }
        prefetch(ip);
        if (--bc == 0) {
            throw new ThreadYieldException();
        }
    }









++IR1 trace for Ljava/lang/String;::indexOf
    Instructions = 27 Locals = l5 t1 t2 *l0# l4# l3 *l1 *l2
    t1      = [offset I (fslot 1)] l0#
    t2      = [count I (fslot 2)] l0#
    l3      = t1 + t2
    l4#     = [value [C (fslot 0)] l0#
    if l2 >= const(0) goto 8
    l2      = const(0)
    goto 12
8:
    t2      = [count I (fslot 2)] l0#
    if l2 < t2 goto 12
    return const(-1)
12:
    t1      = [offset I (fslot 1)] l0#
    l5      = t1 + l2
    goto 24
16:
    t1      = l4#[l5]
    if t1 != l1 goto 22
    t2      = [offset I (fslot 1)] l0#
    t1      = l5 - t2
    return t1
22:
    l5      = l5 + const(1)
24:
    if l5 < l3 goto 16
    return const(-1)

--IR1 trace for Ljava/lang/String;::indexOf





++IR1 trace for Ljava/lang/String;::indexOf
    Instructions = 27 Locals = l5 t1 t2 *l0# l4# l3 *l1 *l2

    <native block 1>
    goto 24

16:
    <native block 2>
24:
    if l5 < l3 goto 16
    return const(-1)

--IR1 trace for Ljava/lang/String;::indexOf










    int idiv(int l, int r) {
        prefetch();
        if (r == 0) {
            throw new ArithmeticException();
        } else if (l == 0x80000000 && r == -1) {
            return l;
        } else {
            return l / r;
        }
    }


    int getfield_i(Instance ref) {
        int off = fetchUnsignedByte();
        prefetch();
        if (ref == null) {
            throw new NullPointerException();
        }
        return ref.wordAt(off);
    }


    void icmplt(int l, int r);
        int offset = fetchByte();
        if (l <  r) {
            ip += offset;
        }
        prefetch();
        if (--bc == 0) {
            throw new ThreadYieldException();
        }
    }


    void icmplt(int l, int r);
        int offset = fetchByte();
        if (l <  r) {
            incIp(offset);
        }
        prefetch();
        yield();
    }


    void icmplt(int l, int r);
        int offset = fetchByte();
        branch(l, r, OPC_LT, offset);
    }





    void ret() {
        popFrame();
    }

    int ret1(int val) {
        popFrame();
        return val;
    }

    long ret2(long val) {
        popFrame();
        return val;
    }




    void extend() {
        extend_n(fetchUnsignedByte(ip++));
    }

    void extend_0() {
        extend_n(0);
    }

    void extend_1() {
        extend_n(1);
    }

    void extend_n(int value) {
        prefetch(ip);
        ...
    }












    int idiv(int l, int r) {
        prefetch();
        div0check(r);
        if (l == 0x80000000 && r == -1) {
            return l;
        } else {
            return l / r;
        }
    }

    int getfield_i(Instance ref) {
        int off = fetchUnsignedByte();
        prefetch();
        nullCheck(ref);
        return ref.wordAt(off);
    }

    void icmplt(int l, int r);
        int offset = fetchByte();
        branch_lt(l, r, offset);
    }







    void div0check(int x) {
        if (x == 0) {
            throw new ArithmeticException();
        }
    }

    void nullCheck(Instance ref) {
        if (ref == null) {
            throw new NullPointerException();
        }
    }

    void branch_lt(int l, int r, int offset) {
        if (l < r) {
            incIp(offset);
        }
        prefetch();
        yield();
    }

    void yield() {
        if (--bc == 0) {
            throw new ThreadYieldException();
        }
    }
