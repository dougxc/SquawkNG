
    Registers
    =========

    ip: instruction pointer
    sp: stack pointer
    lp: frame pointer
    cp: class pointer
    sl: stack chunk limit



    Chunk
    =====

    next            offset 0        OOP
    prev            offset 1        OOP
    size            offset 2        Value
    lastlp          offset 3        Real address in chunk




    Frame
    =====

    p_lp            offset -5       Real address in chunk
    c_cp            offset -4       OOP
    c_mp            offset -3       OOP
    c_ip            offset -2       Real address in c_mp
    stkoff          offset -1       Offset from lp to sp




    Invoke
    ======

    int moffset = *ip++;                                // the "number" operand of invoke
    lp->c_ip = ip;                                      // save current ip
    temp = lp;                                          // copy caller's lp
    lp = lp + lp[stkoff];                               // make lp point to the start of the stack area
    lp->p_lp = temp;                                    // store the caller's lp into the new frame
    cp = getClassOf(lp[0]);                             // get the callee's class
    int mp = cp->vtable[moffset];                       // get the method
    lp->c_mp = mp;                                      // save the current method
    lp->c_cp = cp;                                      // save the current class
    ip = mp + mp[HSIZE];                                // set the ip to start executing the first bytecode


    Simple Return
    =============

    sp = lp                                             // set the sp
    lp = lp->p_lp                                       // restore the lp
    cp = lp->c_cp                                       // cp
    ip = lp->c_ip                                       // and ip


    Extend
    ======

    int restartIP = ip-1;                               // restart address
    int zp = lp + nlocals;                              // point past last local
    int ep = zp + FSIZE + nstack;                       // point past the last word used
    if (ep > sl) {                                      // overflow?
        int minsize = ep-lp+FSIZE+CHUNKHEADERSIZE;      // Work out the minimum needed
        int cc = getCurrentChunk(lp);                   // get the current chunk
        int nc = cc->next;                              // get the next chumk if there is one
        cc->next = null;                                // clear pointer
        if (nc == null || nc->size < minsize) {         // large enough?
            nc = allocChuck(minsize);                   // allocate chunk
        }
        if (nc == null) {                               // Failed?
            lp->c_ip = restartIP;                       // set to re-execute the extend
            lp->stkoff = sp - lp;                       // save the stack offset
            cc->lastlp = lp;                            // save the lp
            Heap->currentChunk = cc;                    // save the current chunk
            return;                                     // go do gc
        }
        sl = nc + nc->size;                             // set up the sl for this chunk
        nc->prev   = cc;                                // save the previous chunk address
        nc->lastlp = 0;                                 // needed?
        cc->next   = nc;                                // save the next chunk
        cc->lastlp = lp->p_lp;                          // save the last real frame
        nlp = nc + CHUNKHEADERSIZE+FSIZE;               // get the next frame
        nlp->p_lp = 0;                                  // this marks the first frame in the chunk
        nlp->c_mp = lp->c_mp;                           // copy from old frame
        nlp->c_ip = lp->c_ip;                           // ...
        nlp->c_cp = lp->c_cp;                           // ...
        int nsp = nlp;                                  // get start of param
        while (lp != sp) {                              // copy parms
            *nsp++ = *lp++;                             // ...
        }                                               // ...
        lp = nlp;                                       // get the new lp
        sp = nsp;                                       // get the new sp
        zp = lp + nlocals;                              // point past last local
    }
    while (sp != zp) {                                  // zero locals
        *sp++ = 0;                                      // ...
    }                                                   // ...
    sp += FSIZE;                                        // skip frame
    lp->stkoff = sp - lp;                               // save the stack offset


    Return after Extend
    ===================

    <pop return value>
    plp = lp->p_lp                                      // get previous lp
    if (plp != 0) {                                     // zero?
        sp = lp;                                        // n - then set sp to our lp
    } else {
        cc = getCurrentChunk(lp);                       // get current chunk
        pc = cc->prev;                                  // get previous chunk
        assume(pc != null);
        sl = pc + pc->size;                             // reset sl
        plp = pc->lastlp;                               // get previous frame
        pc->lastlp = 0;                                 // clear last frame pointer
        sp = plp + plp->stkoff;                         // setup sp
    }
    lp = plp;                                           // get the previous lp
    cp = lp->c_cp;                                      // reset cp
    ip = lp->c_ip;                                      // get next ip
    <push return value>



    Image launch
    ============

    cc = Heap->currentChunk;                            // get current chunk
    lp = cc->lastlp;                                    // get last frame
    cc->lastlp = 0;                                     // clear last frame pointer
    ip = lp->c_ip;                                      // setup cp
    cp = lp->c_cp;                                      // setup ip
    sp = lp + lp->stkoff;                               // reset sp


    newObject
    =========

    int restartIP = ip-1;                               // restart address
    if (lp[0] == 0) {                                   // is the object already allocated?
        lp[0] = allocObjectForClass(cp);                // n - try to allocate
        if (lp[0] == 0) {                               // worked?
            lp->c_ip = restartIP;                       // n - set ip to re-execute the newObject bytecode
            cc = getCurrentChunk(lp);                   // get the current chunk
            cc->lastlp = lp;                            // save lp
            Heap->currentChunk = cc;                    // save current chunk
            return;                                     // go do gc
        }
    }


    newArray
    ========

    int restartIP = ip-1;                               // restart ip
    int cls = pop();
    int siz = pop();
    int obj = allocArrayObject(cls, siz);               // try to allocate
    if (obj == 0) {                                     // worked?
        lp->c_ip = restartIP;                           // n - set ip to re-execute the newObject bytecode
        cc = getCurrentChunk(lp);                       // get the current chunk
        cc->lastlp = lp;                                // save lp
        Heap->currentChunk = cc;                        // save current chunk
        return;                                         // go do gc
    }
    push obj;                                           // save result


    newDimension
    ============

    int restartIP = ip-1;                               // restart ip
    int siz = pop();
    int array = tos();
    int cls = getElementType(getClass(array));
    int obj = allocArrayObject(cls, siz);               // try to allocate
    if (obj == 0) {                                     // worked?
        lp->c_ip = restartIP;                           // n - set ip to re-execute the newObject bytecode
        cc = getCurrentChunk(lp);                       // get the current chunk
        cc->lastlp = lp;                                // save lp
        Heap->currentChunk = cc;                        // save current chunk
        return;                                         // go do gc
    }
    <put obj in first slot that is zero in array>


    GC
    ==

    Copy chunk to new space

    void copyChunk(int xc) {
        int cc = getChunkInNewSpace(xc->size);          // allocate new chunk
        int delta = 0 - xc + cc;                        // form chunk offset delta
        memcopy(xc, cc);                                // copy the whole chunk
        cc->lastlp += delta;                            // update offset
        if (cc->next != null && cc->next->lastlp == 0) {// next chunk alive?
            cc->next = null;                            // n - delete reference
        }

        int lp = cc + CHUNKHEADERSIZE+FSIZE;            // get the first lp in chunk
        while(lp <= cc->lastlp) {                       // while not at the end ...

            if (lp->p_lp != 0) {                        // got a previous frame?
                lp->p_lp += delta;                      // y - update the offset
            }                                           // ...

            lp->c_cp = updateOop(lp->c_cp);             // update class pointer

            int relip = lp->c_ip - lp->c_mp;            // get the ip address relative to the method
            lp->c_mp = updateOop(lp->c_mp);             // update the method
            lp->c_ip = relip + lp->c_mp;                // form the absolute ip address again

            if (relip == mp[HSIZE]) {                   // before extend?
                for (all parms that are oops) {
                    lp->parm = updateOop(lp->parm);     // update parm
                }
                assume(lp == cc->lastlp);
                break;                                  // Finished
            } else {
                for (all parms and locals that are oops) {
                    lp->local = updateOop(lp->local);   // update local
                }
                lp += lp->stkoff
            }
        }

        if (cc->prev != null) {
            cc->prev = updateOop(cc->prev);             // update previous chunk pointer
        }

        if (cc->next != null) {
            cc->next = updateOop(cc->next);             // update next chunk pointer
        }
    }
