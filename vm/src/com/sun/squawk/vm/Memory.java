//J2C:memory.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/abstract public class Memory extends PlatformAbstraction {

/*-----------------------------------------------------------------------*\
 *                               Buffers                                 *
\*-----------------------------------------------------------------------*/

/*IFJ*/private byte[] memory;
//IFC//private byte *memory = 0;
       private int memorySize;
       private boolean bigEndian;

//IFC//#ifdef REALMEMORY
//IFC//#define getImageOffset() ((int)memory)
//IFC//#define ptrForJni(addr)  ((byte*)addr)
//IFC//#else
//IFC//#define getImageOffset() 0
//IFC//#define ptrForJni(addr)  ((byte*)(memory+(addr)))
//IFC//#endif /* REALMEMORY*/

/*IFJ*/int getImageOffset() { return 1024; }

/*-----------------------------------------------------------------------*\
 *                            Initialization                             *
\*-----------------------------------------------------------------------*/

    /**
     * Setup the pointer to the memory area and its length
     */
    void Memory_init(byte mem[], int size, boolean isBigEndian) {
        memory = mem;
        bigEndian =  isBigEndian;
        memorySize = size;
    }

    /**
     * Return the length of the memory area
     */
    int getMemorySize() {
        return memorySize;
    }


/*-----------------------------------------------------------------------*\
 *                             Round up                                  *
\*-----------------------------------------------------------------------*/

    /**
     * Round up a given number to 4.
     * @param addr
     * @return
     */
/*MAC*/ int roundup4(int $addr) {
            return ($addr+3) & ~3;
        }

/*-----------------------------------------------------------------------*\
 *                             Memory access                             *
\*-----------------------------------------------------------------------*/


//*IFJ*/String inHex(int i) { return Integer.toHexString(i); }
/*IFJ*/String inHex(int i) { return Integer.toString(i); }


//IFC//#ifdef PRODUCTION

//IFC//#define checkAddress(addr, byteOffset)             ((addr)+byteOffset)

//IFC//#define getWord(addr, offset)                      (((int *)(addr))[offset])
//IFC//#define setWord(addr, offset, value)              ((((int *)(addr))[offset]) = (value))

//IFC//#define getByte(addr, offset)                      (((char *)(addr))[offset])
//IFC//#define getUnsignedByte(addr, offset)              (((unsigned char *)(addr))[offset])
//IFC//#define setByte(addr, offset, value)              ((((char *)(addr))[offset]) = (char)((value) & 0xFF))

//IFC//#define getHalf(addr, offset)                      (((short *)(addr))[offset])
//IFC//#define getUnsignedHalf(addr, offset)              (((unsigned short *)(addr))[offset])
//IFC//#define setHalf(addr, offset, value)              ((((short *)(addr))[offset]) = ((value) & 0xFFFF))

//IFC//#define pushWord(addr, value)                      *((int *)(addr += 4)) = (value)

//IFC///*MAC*/ void setDWord(int $addr, jlong $value)       { ((jlong *)$addr)[0] = ($value); }
//IFC///*MAC*/ jlong getDWord(int $addr)                    { return ((jlong *)$addr)[0];    }

//IFC//#define copyWords(src, dst, num)                   memmove((char*)(dst), (char*)(src), (num)*4)
//IFC//#define copyBytes(src, dst, num)                   memmove((char*)(dst), (char*)(src), (num))

//IFC//#define zeroBytes(addr, num)                       memset((void*)(addr), 0, (num))


//IFC//#else

    /**
     * Ensure that an object address is on a word boundary and that memory is
     * not being accessed out of range.
     * @param addr
     * @param byteOffset
     * @return
     */
    int checkAddress(int addr, int byteOffset) {
        if ((addr & 3) != 0) {
/*IFJ*/     fatalVMError("Badly aligned address "+inHex(addr));
//IFC//     fatalVMError("Badly aligned address");
        }
        if (addr < 0) {
/*IFJ*/     fatalVMError("Bad base < 0 -- base = "+inHex(addr));
//IFC//     fatalVMError("Bad base < 0");
        }
        if (addr+byteOffset < 0) {
/*IFJ*/     fatalVMError("Bad base+offset < 0 -- base = "+inHex(addr)+" offset ="+inHex(byteOffset));
//IFC//     fatalVMError("Bad base+offset < 0");
        }
        if (addr+byteOffset >= memorySize+getImageOffset()) {
/*IFJ*/     fatalVMError("Bad base+offset above memory -- base = "+inHex(addr)+" offset ="+inHex(byteOffset)+" memorySize ="+inHex(memorySize+getImageOffset()));
//IFC//     fatalVMError("Bad base+offset address base");
        }
        return addr+byteOffset;
    }




    /**
     * checkWrite
     */
    void checkWrite(int addr, int value) {
///*IFJ*/ int a = 853972 + (4*0);
///*IFJ*/ if (addr == a) {
///*IFJ*/ traceStream.println("Write to "+a+" value = "+value+" instructionCount="+getInstructionCount());
///*IFJ*/ Throwable t = new Throwable();
///*IFJ*/ t.printStackTrace(traceStream);
///*IFJ*/ }
    }

    /**
     * getWord
     */
    int getWord(int addr, int wordOffset) {
        int res;
        addr = checkAddress(addr, wordOffset*4);
        if (bigEndian) {
            res =  (memory[addr  ]       ) << 24 |
                   (memory[addr+1] & 0xFF) << 16 |
                   (memory[addr+2] & 0xFF) << 8  |
                   (memory[addr+3] & 0xFF);
        } else {
            res =  (memory[addr+3]       ) << 24 |
                   (memory[addr+2] & 0xFF) << 16 |
                   (memory[addr+1] & 0xFF) << 8  |
                   (memory[addr  ] & 0xFF);
        }

        return res;
    }

    /**
     * setWord
     */
    void setWord(int addr, int wordOffset, int value) {
        addr = checkAddress(addr, wordOffset*4);
        checkWrite(addr, value);
        if (bigEndian) {
            memory[addr  ] = (byte)(value >> 24);
            memory[addr+1] = (byte)(value >> 16);
            memory[addr+2] = (byte)(value >> 8 );
            memory[addr+3] = (byte)(value      );
        } else {
            memory[addr+3] = (byte)(value >> 24);
            memory[addr+2] = (byte)(value >> 16);
            memory[addr+1] = (byte)(value >> 8 );
            memory[addr  ] = (byte)(value      );
        }
    }

    /**
     * getByte
     */
    int getByte(int addr, int byteOffset) {
        addr = checkAddress(addr, byteOffset);
        return memory[addr];
    }

    /**
     * getUnsignedByte
     */
    int getUnsignedByte(int addr, int byteOffset) {
        addr = checkAddress(addr, byteOffset);
        return memory[addr] & 0xFF;
    }

    /**
     * setByte
     */
    void setByte(int addr, int byteOffset, int value) {
        addr = checkAddress(addr, byteOffset);
        checkWrite(addr, value);
        memory[addr] = (byte)value;
    }

    /**
     * getHalf
     */
    int getHalf(int addr, int halfOffset) {
        addr = checkAddress(addr, halfOffset*2);
        if (bigEndian) {
            return (memory[addr  ]       ) << 8  |
                   (memory[addr+1] & 0xFF);
        } else {
            return (memory[addr+1]       ) << 8  |
                   (memory[addr  ] & 0xFF);
        }
    }

    /**
     * getUnsignedHalf
     */
    int getUnsignedHalf(int addr, int halfOffset) {
        return getHalf(addr, halfOffset) & 0xFFFF;
    }

    /**
     * setHalf
     */
    void setHalf(int addr, int halfOffset, int value) {
        addr = checkAddress(addr, halfOffset*2);
        checkWrite(addr, value);
        if (bigEndian) {
            memory[addr  ] = (byte)(value >> 8 );
            memory[addr+1] = (byte)(value      );
        } else {
            memory[addr+1] = (byte)(value >> 8 );
            memory[addr  ] = (byte)(value      );
        }
    }

    /**
     * Copy 'num' bytes from 'src' to 'dst'. Both 'src' and 'dst' are byte
     * addresses.
     * @param src
     * @param dst
     * @param num
     */
    void copyBytes(int src, int dst, int num) {
        if (num < 0) {
/*IFJ*/     fatalVMError("Negative range " + num);
//IFJ//     fatalVMError("Negative range");
        }
        checkAddress(0, src);
        checkAddress(0, src+num-1);
        checkAddress(0, dst);
        checkAddress(0, dst+num-1);

/*IFJ*/ System.arraycopy(memory, src, memory, dst, num);
//IFC// memmove(memory+dst, memory+src, num);
    }

    /**
     * copyWords
     */
    void copyWords(int from, int to, int num) {
        copyBytes(from, to, num*4);
    }

    /**
     * Zero a segment of memory.
     * @param addr
     * @param numBytes
     */
    void zeroBytes(int addr, int numBytes) {
        int i;
        for (i = 0; i < numBytes; i++) {
            setByte(addr, i, 0);
        }
    }

//IFC//#endif /* PRODUCTION */

    /**
     * getLongAtWord
     */
    long getLongAtWord(int addr, int wordOffset) {
        int word1 = getWord(addr, wordOffset);
        int word2 = getWord(addr, wordOffset+1);
        return ((long)word1 << 32) + (word2 & 0xFFFFFFFFL);
    }

    /**
     * setLongAtWord
     */
    void setLongAtWord(int addr, int wordOffset, long val) {
        setWord(addr, wordOffset,   (int)(val >> 32));
        setWord(addr, wordOffset+1, (int)(val));
    }

    /**
     * getLong
     */
    long getLong(int addr, int longOffset) {
        return getLongAtWord(addr, longOffset*2);
    }

    /**
     * setLong
     */
    void setLong(int addr, int longOffset, long val) {
        setLongAtWord(addr, longOffset*2, val);
    }

/*MAC*/int   fetchUnsignedByte(int $addr)  { return getUnsignedByte(0, $addr); }
/*MAC*/int   fetchByte(int $addr)          { return getByte(0, $addr);         }
/*MAC*/int   fetchUnsignedShort(int $addr) { return (fetchUnsignedByte($addr) << 8) + fetchUnsignedByte($addr+1); }
/*MAC*/short fetchShort(int $addr)         { return (short)fetchUnsignedShort($addr); }
/*MAC*/int   fetchInt(int $addr)           { return (fetchUnsignedByte($addr)<<24) + (fetchUnsignedByte($addr+1)<<16) + (fetchUnsignedByte($addr+2)<<8) + fetchUnsignedByte($addr+3); }
/*IFJ*/long  fetchLong(int $addr)          { return (((long)fetchInt($addr))<<32) + (fetchInt($addr+4) & 0xFFFFFFFFL); }
//IFC//#define fetchLong(addr)                    ((((jlong)fetchInt(addr))  <<32) + (unsigned int)fetchInt(addr+4))

    /**
     * Write the memory contents to an image file.
     * @param imageFileName
     */
    protected void Memory_writeImage(String imageFileName, int mmr[]) {
        writeImage(imageFileName, memory, memorySize, mmr, bigEndian);
    }



/*IFJ*/}
