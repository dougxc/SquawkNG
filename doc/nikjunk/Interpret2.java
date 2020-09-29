    void interpret() {
        int b = getUnsignedByte(ip++);
        switch (b) {
            case OPC_CONST_0:
            case OPC_CONST_1:
            case OPC_CONST_2:
            case OPC_CONST_3:
            case OPC_CONST_4:
            case OPC_CONST_5:
            case OPC_CONST_6:
            case OPC_CONST_7:
            case OPC_CONST_8:
            case OPC_CONST_9:
            case OPC_CONST_10:
            case OPC_CONST_11:
            case OPC_CONST_12:
            case OPC_CONST_13:
            case OPC_CONST_14:
            case OPC_CONST_15:
                push(b&15);
                break;

            case OPC_CONST_BYTE:
                push(getSignedByte(ip++));
                break;

            case OPC_CONST_SHORT:
                push(getSignedShort(ip));
                ip += 2;
                break;

            case OPC_CONST_CHAR:
                push(getUnsignedShort(ip));
                ip += 2;
                break;

            case OPC_CONST_INT:
                push(getInt(ip));
                ip += 4;
                break;

            case OPC_CONST_LONG:
            case OPC_CONST_FLOAT:
            case OPC_CONST_DOUBLE:
                push(getInt(ip));
                ip += 4;
                push(getInt(ip));
                ip += 4;
                break;


            case OPC_OBJECT_0:
            case OPC_OBJECT_1
            case OPC_OBJECT_2:
            case OPC_OBJECT_3:
            case OPC_OBJECT_4:
            case OPC_OBJECT_5:
            case OPC_OBJECT_6:
            case OPC_OBJECT_7:
            case OPC_OBJECT_8:
            case OPC_OBJECT_9:
            case OPC_OBJECT_10:
            case OPC_OBJECT_11:
            case OPC_OBJECT_12:
            case OPC_OBJECT_13:
            case OPC_OBJECT_14:
            case OPC_OBJECT_15:
                push(getObjectReference(b&15));
                break;

            case OPC_OBJECT:
                push(getObjectReference(getUnsignedByte(ip++)));


            case OPC_CLASS_0:
            case OPC_CLASS_1:
            case OPC_CLASS_2:
            case OPC_CLASS_3:
            case OPC_CLASS_4:
            case OPC_CLASS_5:
            case OPC_CLASS_6:
            case OPC_CLASS_7:
            case OPC_CLASS_8:
            case OPC_CLASS_9:
            case OPC_CLASS_10:
            case OPC_CLASS_11:
            case OPC_CLASS_12:
            case OPC_CLASS_13:
            case OPC_CLASS_14:
            case OPC_CLASS_15:
                push(getClassReference(b&15));
                break;

            case OPC_CLASS:
                push(getClassReference(getUnsignedByte(ip++)));
                break;

            case OPC_LOAD_0:
            case OPC_LOAD_1:
            case OPC_LOAD_2:
            case OPC_LOAD_3:
            case OPC_LOAD_4:
            case OPC_LOAD_5:
            case OPC_LOAD_6:
            case OPC_LOAD_7:
            case OPC_LOAD_8:
            case OPC_LOAD_9:
            case OPC_LOAD_10:
            case OPC_LOAD_11:
            case OPC_LOAD_12:
            case OPC_LOAD_13:
            case OPC_LOAD_14:
            case OPC_LOAD_15:
                push(getLocal(b&15));
                break;

            case OPC_LOAD:
                push(getLocal(getUnsignedByte(ip++)));
                break;

            case OPC_STORE_0:
            case OPC_STORE_1:
            case OPC_STORE_2:
            case OPC_STORE_3:
            case OPC_STORE_4:
            case OPC_STORE_5:
            case OPC_STORE_6:
            case OPC_STORE_7:
            case OPC_STORE_8:
            case OPC_STORE_9:
            case OPC_STORE_10:
            case OPC_STORE_11:
            case OPC_STORE_12:
            case OPC_STORE_13:
            case OPC_STORE_14:
            case OPC_STORE_15:
                setLocal(b&15, pop());
                break;

            case OPC_STORE:
                if (wide == 0) {
                    setLocal(getUnsignedByte(ip++), pop());
                } else if (wide == -2) {
                    setLocal(getUnsignedShort(ip), pop());
                    ip += 2;
                } else if (wide == -4) {
                    setLocal(getInt(ip++), pop());
                    ip += 4;
                } else {
                    setLocal(wide << 8  || getUnsignedByte(ip++), pop());
                }
                wide = 0;
                break;

            case OPC_WIDE_0:
            case OPC_WIDE_1:
            case OPC_WIDE_2:
            case OPC_WIDE_3:
            case OPC_WIDE_4:
            case OPC_WIDE_5:
            case OPC_WIDE_6:
            case OPC_WIDE_7:
            case OPC_WIDE_8:
            case OPC_WIDE_9:
            case OPC_WIDE_10:
            case OPC_WIDE_11:
            case OPC_WIDE_12:
            case OPC_WIDE_13:
            case OPC_WIDE_14:
            case OPC_WIDE_15:
                wide = b&15;
                break;

            case OPC_WIDE_HALF:
                wide = -2;
                break;

            case OPC_WIDE_FULL:
                wide = -4;
                break;

            case OPC_EXTEND_0:
            case OPC_EXTEND_1:
            case OPC_EXTEND_2:
            case OPC_EXTEND_3:
            case OPC_EXTEND_4:
            case OPC_EXTEND_5:
            case OPC_EXTEND_6:
            case OPC_EXTEND_7:
            case OPC_EXTEND_8:
            case OPC_EXTEND_9:
            case OPC_EXTEND_10:
            case OPC_EXTEND_11:
            case OPC_EXTEND_12:
            case OPC_EXTEND_13:
            case OPC_EXTEND_14:
            case OPC_EXTEND_15:
                extend(b&15);
                break;

            case OPC_EXTEND:
                extend(getUnsignedByte(ip++));
                break;





            case OPC_LONGOP:
                longOp();
                break;

            case OPC_FLOATOP:
                floatOp();
                break;

            case OPC_CONST_M1:
                push(-1);
                break;

            case OPC_IADD:
                tosAdd(pop());
                break;

            case OPC_ISUB:
                tosSub(pop());
                break;

            case OPC_IAND:
                tosAnd(pop());
                break;

            case OPC_IOR:
                toOr(pop());
                break;

            case OPC_IXOR:
                tosXor(pop());
                break;

            case OPC_ISHL:
                tosShl(pop()&0x1F);
                break;

            case OPC_ISHR:
                tosShr(pop()&0x1F);
                break;

            case OPC_IUSHR:
                tosUshr(pop()&0x1F);
                break;

            case OPC_IMUL:
                tosMul(pop());
                break;

            case OPC_IDIV:
                tosDiv(pop());
                break;

            case OPC_IREM:
                tosRem(pop());
                break;

            case OPC_THROW:
            case OPC_BPT:
            case OPC_NOP:
            case OPC_POP:
            case OPC_NEG:
            case OPC_I2B:
            case OPC_I2S:
            case OPC_I2C:
            case OPC_LOOKUP:
            case OPC_STABLESWITCH:
            case OPC_MONITORENTER:
            case OPC_MONITOREXIT:
            case OPC_CLASS_MONITORENTER:
            case OPC_CLASS_MONITOREXIT:
            case OPC_ARRAYLENGTH:
            case OPC_CLINIT:
            case OPC_NEWARRAY:
            case OPC_NEWDIMENSION:
            case OPC_INSTANCEOF:
            case OPC_CHECKCAST:
            case OPC_EQ:
            case OPC_LT:
            case OPC_LE:
            case OPC_NE:
            case OPC_GT:
            case OPC_GE:

            case OPC_ALOAD:
            case OPC_ALOAD_B:
            case OPC_ALOAD_S:
            case OPC_ALOAD_C:
            case OPC_ALOAD_I2:

            case OPC_ASTORE:
            case OPC_ASTORE_B:
            case OPC_ASTORE_S:
            case OPC_ASTORE_O:
            case OPC_ASTORE_I2:


            case OPC_INC:
            case OPC_DEC:



            case OPC_INVOKEINIT:
            case OPC_INVOKEINIT1:
            case OPC_INVOKEINIT2:

            case OPC_INVOKEINTERFACE:
            case OPC_INVOKEINTERFACE1:
            case OPC_INVOKEINTERFACE2:

            case OPC_INVOKESTATIC:
            case OPC_INVOKESTATIC1:
            case OPC_INVOKESTATIC2:

            case OPC_INVOKESUPER:
            case OPC_INVOKESUPER1:
            case OPC_INVOKESUPER2:

            case OPC_INVOKEVIRTUAL:
            case OPC_INVOKEVIRTUAL1:
            case OPC_INVOKEVIRTUAL2:

            case OPC_RETURN:
            case OPC_RETURN1:
            case OPC_RETURN2:





            case OPC_GETSTATIC:
            case OPC_PUTSTATIC:

            case OPC_CLASS_GETSTATIC:
            case OPC_CLASS_PUTSTATIC:



            case OPC_THIS_GETFIELD:
                push(getLocal(0));
            case OPC_GETFIELD:
                int obj = pop();
                if (obj == 0) {
                    b = OPE_NPE;
                    break;
                }
                break;

            case OPC_THIS_GETFIELD_B:
                push(getLocal(0));
            case OPC_GETFIELD_B:
                xxxx;
                break;

            case OPC_THIS_GETFIELD_S:
                push(getLocal(0));
            case OPC_GETFIELD_S:
                xxxx;
                break;

            case OPC_THIS_GETFIELD_C:
                push(getLocal(0));
            case OPC_GETFIELD_C:
                xxxx;
                break;

            case OPC_THIS_GETFIELD_I2:
                push(getLocal(0));
            case OPC_GETFIELD_I2:
                xxxx;
                break;





            case OPC_THIS_PUTFIELD: {
                int val = pop();
                push(getLocal(0));
                push(val);
            }
            case OPC_PUTFIELD:
                xxxx;
                break;



            case OPC_IFEQ:
            case OPC_IFNE:
            case OPC_IFLT:
            case OPC_IFLE:
            case OPC_IFGT:
            case OPC_IFGE:

            case OPC_IF_ICMPEQ:
            case OPC_IF_ICMPNE:
            case OPC_IF_ICMPLT:
            case OPC_IF_ICMPLE:
            case OPC_IF_ICMPGT:
            case OPC_IF_ICMPGE:

            case OPC_GOTO:

            case OPC_NEWOBJECT:

            case OPC_STABLESWITCH_PAD:
            case OPC_LOAD_I2:
            case OPC_STORE_I2:



            case OPC_GETSTATIC_O:
            case OPC_GETSTATIC_I2:
            case OPC_CLASS_GETSTATIC_O:
            case OPC_CLASS_GETSTATIC_I2:
            case OPC_PUTSTATIC_O:
            case OPC_PUTSTATIC_I2:
            case OPC_CLASS_PUTSTATIC_O:
            case OPC_CLASS_PUTSTATIC_I2:



            case OPC_PUTFIELD_B:
            case OPC_PUTFIELD_S:
            case OPC_PUTFIELD_O:
            case OPC_PUTFIELD_I2:



            case OPC_THIS_PUTFIELD_B:
            case OPC_THIS_PUTFIELD_S:
            case OPC_THIS_PUTFIELD_O:
            case OPC_THIS_PUTFIELD_I2:
        }
    }


    void longOp() {
        switch (b) {
            case OPC_LADD:
            case OPC_LSUB:
            case OPC_LMUL:
            case OPC_LDIV:
            case OPC_LREM:
            case OPC_LAND:
            case OPC_LOR:
            case OPC_LXOR:
            case OPC_LNEG:
            case OPC_LSHL:
            case OPC_LSHR:
            case OPC_LUSHR:
            case OPC_LCMP:
            case OPC_L2I:
            case OPC_I2L:
        }
    }

    void floatOp() {
        switch (b) {
            case OPC_FADD:
            case OPC_FSUB:
            case OPC_FMUL:
            case OPC_FDIV:
            case OPC_FREM:
            case OPC_FNEG:
            case OPC_FCMPG:
            case OPC_FCMPL:
            case OPC_DADD:
            case OPC_DSUB:
            case OPC_DMUL:
            case OPC_DDIV:
            case OPC_DREM:
            case OPC_DNEG:
            case OPC_DCMPG:
            case OPC_DCMPL:
            case OPC_I2F:
            case OPC_L2F:
            case OPC_F2I:
            case OPC_F2L:
            case OPC_I2D:
            case OPC_L2D:
            case OPC_F2D:
            case OPC_D2I:
            case OPC_D2L:
            case OPC_D2F:
            default:
        }
    }