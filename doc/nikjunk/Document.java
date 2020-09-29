



   Reg r = getReg();
   a.pop(r);




   void iadd(Reg dst, Reg src1, Reg src2) {
       a.add(
   }



  Reg lp = new Reg("esi");



   switch (b) {
       case IADD:
           Reg dst  = tos();
           Reg src1 = tos();
           Reg src2 = pop();
           a.add(dst, src1, src2);
           next();
           break;

       case IFEQ:
           Reg src    = tos();
           int offset = fetchByte();
           ifeq(src, 0, offset);
           next();
           break;

       case NEXT:
           Reg src    = tos();
           int offset = fetchByte();
           ifeq(src, 0, offset);
           next();
           break;

   }


   table[IADD] = ip;
   IADD();
   disp();
   table[ISUB] = ip;
   ISUB();
   disp();