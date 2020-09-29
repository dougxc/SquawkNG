
    Registers
    =========

    sp: stack pointer
    lp: frame pointer
    ip: instruction pointer
    cp: class pointer
    mp: method pointer
    ep: end pointer
    sl: stack chunk limit

    Chunk
    =====

    next            offset 0        OOP
    prev            offset 1        OOP
    size            offset 2        Value
    lastlp          offset 3        Real address in chunk


    Frame
    =====

    c_mp            offset -5       OOP
    c_ip            offset -4       Real address in c_mp
    p_lp            offset -3       Real address in chunk
    p_cp            offset -2       OOP
    stkoff          offset -1       Offset from lp to sp


    Invoke
    ======

    int moffset = *ip++;       // the "number" operand of invoke
    lp->c_ip = ip;             // save current ip
    temp = lp;                 //
    lp = lp + lp[stkoff];
    lp->p_lp = temp;
    lp->p_cp = cp;
    cp = getClassOf(lp[0]);
    int mp = cp->vtable[moffset];
    lp->c_mp = mp;
    ip = mp + mp[HSIZE];


    Simple Return
    =============

    sp = lp
    cp = lp->p_cp
    lp = lp->p_lp
    ip = lp->c_ip


    Extend X,Y
    ==========

    sp = lp + X + FSIZE;    // X = nlocals
    ep = sp + Y;            // Y = maxstack
    if (ep > sl) {
        int minsize = ep-lp+FSIZE+CHUNKHEADERSIZE;
        nc = cc->next;
        if (nc != null) {
            if (nc->size < minsize) {
                nc = cc->next = null; // too small
            }
        }
        if (nc == null) {
            nc = allocChuck(minsize);
        }
        if (nc == null) {
            int mp = lp->c_mp;
            lp->c_ip = mp + mp[HSIZE]; // set to start of method
            cc = getCurrentChunk(lp);
            cc->lastlp = lp;
            Heap->currentChunk = cc;
            return; // to do gc
        }
        sl = nc + nc->size;
        nc->prev   = cc;
        nc->lastlp = 0; // needed?
        cc->next   = nc;
        cc->lastlp = lp->p_lp; // last complete frame
        nlp = nc + CHUNKHEADERSIZE+FSIZE;
        nlp->c_mp = lp->c_mp;
        nlp->c_ip = lp->c_ip;
        nlp->p_lp = 0;
        nlp->p_cp = lp->c_lp;
        lp = nlp;
    }
    lp->stkoff = sp - lp;


    Return after Extend
    ===================

    cp = lp->p_cp
    plp = lp->p_lp
    if (plp != 0) {
        sp = lp
    } else {
        cc = getCurrentChunk(lp);
        pc = cc->prev;
        assume(pc != null);
        sl = pc + pc->size;
        plp = pc->lastlp;
        pc->lastlp = 0;
        sp = plp + plp->stkoff;
    }
    lp = plp
    ip = lp->c_ip


    Image launch
    ------------

    cc = Heap->currentChunk;
    lp = cc->lastlp;
    cc->lastlp = 0;
    ip = lp->c_ip;
    cp = lp->c_cp;
    sp = lp + lp->stkoff; // reset sp (may be junk, may be correct)


    <init>
    ------

    extend x, y
    newObject


    newObject
    ---------

    if (lp[0] == 0) {
        lp[0] = allocObject(cp);
        if (lp[0] == 0) {
            lp->c_ip = ip - 1; // backup so as to re-execute the newObject bytecode
            cc = getCurrentChunk(lp);
            cc->lastlp = lp;
            Heap->currentChunk = cc;
            return; // to do gc
        }
    }






    <init>
    ------

    extend x, y
    newObject   //push cp, 0 & call Native.alloc()
    store_0


    newObject
    ---------

    obj = lp[0];
    if (obj == 0) {
        push cp
        push 0
        invoke Native.alloc()
    } else {
        push obj
    }



    Native.alloc
    ------------
    allocate
    return


    allocate
    --------

    sp = lp + 2;
    cp = lp[0];
    size = lp[1];
    obj = allocObject(cp, size);
    if (obj == 0) {
        int mp = lp->c_mp;
        lp->c_ip = mp + mp[HSIZE]; // set to start of method
        cc = getCurrentChunk(lp);
        cc->lastlp = lp;
        Heap->currentChunk = cc;
        return; // to do gc
    }
    push obj




    newArray
    --------

    invoke Native.alloc()





    GC
    --

    Move chunk from xc to cc

    if (cc->lastlp != 0) {
        cc->lastlp = updateAddress(xc, cc, cc->lastlp);
        if (cc->next != null) {
            if (cc->next->lastlp == 0) {
                cc->next = null;
            } else {
                cc->next updateOop(cc->next);
            }
        }
        if (cc->prev != null) {
            cc->prev updateOop(cc->prev);
        }
        lp = cc + CHUNKHEADERSIZE+FSIZE;
        while(lp <= cc->lastlp) {
            lp->c_cp = updateOop(lp->c_cp);

            int relip = lp->c_ip - lp->c_mp;    // make rel
            lp->c_mp = updateOop(lp->c_mp);
            lp->c_ip = relip + lp->c_mp;        // make abs

            if (lp->p_lp != 0) {
                lp->p_lp = updateAddress(xc, cc, lp->p_lp);
            }

            if (relip == mp[HSIZE]) { // before extend
                for (all parms) {
                    lp->parm = updateOop(lp->parm);
                }
                assume(lp == cc->lastlp);
                break; // Finished
            } else {
                for (all locals) {
                    lp->local = updateOop(lp->local);
                }
                lp += lp->stkoff
            }
        }
    }


    updateAddress(xc, cc, value) {
        return cc + (value - xc);
    }
