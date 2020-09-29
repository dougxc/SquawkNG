//J2C:header.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;
/*IFJ*/abstract public class HeaderLayout extends ObjectLayout {

/*---------------------------------------------------------------------------*
 *                          Basic object header access
 *---------------------------------------------------------------------------*
 *
 * The object header for an array is 1 or 2 words. If the length of the array
 * is less than 255 (i.e. 0xFF), then the object header is:
 *
 *           <----- 8 ----> <------------------- 24 -------------------->
 *          +--------------+---------------------------------------------+
 *          | Array length |   Class pointer (least significant bit = 0) |
 *          +--------------+---------------------------------------------+
 *
 * If the length of the array is equal to or greater than 255 then the object
 * header is:
 *
 *           <--------------------------- 31 ------------------------->
 *          +----------------------------------------------------------+-+
 *          |                     Array length                         |1|
 *          +--------------+-------------------------------------------+-+
 *          |    0xFF      |   Class pointer (least significant bit = 0) |
 *          +--------------+---------------------------------------------+
 *                          <------------------- 24 -------------------->
 *
 *
 * The object header for a non-array object is:
 *
 *           <------------> <------------------- 24 -------------------->
 *          +--------------+---------------------------------------------+
 *          |  (unused)    |   Class pointer (least significant bit = 0) |
 *          +--------------+---------------------------------------------+
 *
 * As noted in the diagrams, the least significant bit in a class pointer is
 * always 0 as objects are always aligned on a 4 byte boundary. This serves
 * 2 purposes. First it can be used to differentiate between an array length
 * word and a class pointer word when iterating over the heap in copyNonRoots
 * as well as being used for the forwarding bit in objects that have already
 * been copied to the target space.
 *---------------------------------------------------------------------------*/


    /** Array checking can only be performed after re-location is done. */
        boolean arrayCheckingEnabled = false;

    private static final int

        OBJ_length                          = -2,  /** Only for large array objects **/
        OBJ_class                           = -1,

        HEADER_TYPE_MASK                    = 0x3,

        HT_CLASS_WORD                       = 0x0, /* 00 */
        HT_ARRAY_LENGTH                     = 0x1, /* 01 */
        HT_FORWARD_POINTER                  = 0x2, /* 10 */
        HT_FREE_BLOCK                       = 0x3, /* 11 */
        HT_LARGE_FREE_BLOCK_BIT             = 0x4,

        HT_ARRAY_LENGTH_SHIFT               = 2,
        HT_FREE_BLOCK_NEXT_SHIFT            = 3,

        SMALL_HEADER_SIZE                   = 4,
        LARGE_HEADER_SIZE                   = 8,

        HT_DUMMY = 999;

        /**
         * Get the offset in words from an oop to the class pointer in the object's header
         * @return offset
         */
/*MAC*/ public int Object_wordOffsetToClassPointer() {
            return OBJ_class;
        }

        /**
         * Get the number of bits in the high range of the class word that are used to
         * encode the length for short arrays
         * @return bit count
         */
/*MAC*/ /*private*/ int compactArrayBitCount() {
            return /*VAL*/8/*VM.ARRAY_HEADER_LENGTH_BITS*/;
        }

        /**
         * Get the number of bits in the low range of the class word that are used to
         * point to the class of the object
         * @return bit count
         */
/*MAC*/ private int classPointerBitCount() {
            return 32 - compactArrayBitCount();
        }

        /**
         * Return true if the system supports compact arrays
         * @return true/false
         */
/*MAC*/ private boolean hasCompactArrays() {
            return compactArrayBitCount() > 0;
        }

        /**
         * Get the largest value that can be encoded in the length field of a compact
         * array header. The real maximum value is always one less than this value because the
         * highest value is used as a flag to indicate that the object has a two word header.
         * If, for instance, 8 bits are used for the compact array length then compactArrayMaxSize()
         * will be 255. In this case the values 0 to 254 will be used for small array lengths and
         * the value 255 will flag that the object has a two word header.
         * @return the largest value supported by the compact array length field.
         */
/*MAC*/ private int compactArrayMaxSize() {
            return (1 << compactArrayBitCount()) - 1;
        }

        /**
         * Get the mask for the compact array length bits
         * @return mask
         */
/*MAC*/ private int compactArrayLengthMask() {
            return compactArrayMaxSize() << classPointerBitCount();
        }

        /**
         * Get the mask for the class pointer bits
         * @return mask
         */
/*MAC*/ private int classPointerMask() {
            return ~compactArrayLengthMask();
        }

        /**
         * Get the number of bytes required for the header of a normal (non-array) object
         * @return header length
         */
/*MAC*/ public int Object_objectHeaderSize() {
            return SMALL_HEADER_SIZE;
        }


        /**
         * Test the array element count to see if it is large enough to require a large header
         * @param array length
         * @return true if it is
         */
/*MAC*/ boolean Object_isLargeArrayHeaderLength(int $count)  {
            return $count >= compactArrayMaxSize();
        }

        /**
         * Get the number of bytes required for the header of an array with a given length.
         * @param array length
         * @return header length
         */
/*MAC*/ int Object_calculateArrayHeaderLength(int $count)  {
            return $count >= compactArrayMaxSize() ? LARGE_HEADER_SIZE : SMALL_HEADER_SIZE;
        }

        /**
         * Test a word to see if it is the class word of an object header
         * @param array length
         * @return true if the word is the class word
         */
/*MAC*/ boolean Object_isClassWord(int $word) {
            return ($word & HEADER_TYPE_MASK) == HT_CLASS_WORD;
        }

        /**
         * Test a word to see if it is an array length word of an object header
         * @param array length
         * @return true if the word is an array header word
         */
/*MAC*/ boolean Object_isArrayLengthWord(int $word) {
            return ($word & HEADER_TYPE_MASK) == HT_ARRAY_LENGTH;
        }

        /**
         * Test a word to see if it is a forward pointer
         * @param array length
         * @return true if the word is a forward pointer
         */
/*MAC*/ boolean Object_isForwardedWord(int $word) {
            return ($word & HEADER_TYPE_MASK) == HT_FORWARD_POINTER;
        }

        /**
         * Get the class pointer word
         * @param object
         * @return the word
         */
/*MAC*/ private int CLASS_WORD(int $object) {
/*IFJ*/     return getWord((assumeNonZero($object, null, 0)), OBJ_class);
//IFC//     return getWord((assumeNonZero($object, __FILE__, __LINE__)), OBJ_class);
        }

        /**
         * Get the array length word
         * @param object
         * @return the word
         */
/*MAC*/ private int LENGTH_WORD(int $object) {
/*IFJ*/     return getWord((assumeNonZero($object, null, 0)), OBJ_length);
//IFC//     return getWord((assumeNonZero($object, __FILE__, __LINE__)), OBJ_length);
        }

        /**
         * Get the array length word
         * @param object
         * @return the word
         */
/*MAC*/ int Object_getClass(int $object) {
/*IFJ*/     return assumeNonZero(CLASS_WORD($object) & classPointerMask(), null, 0) + getImageOffset();
//IFC//     return assumeNonZero(CLASS_WORD($object) & classPointerMask(), __FILE__, __LINE__) + getImageOffset();
        }

        /**
         * Determine whether or not a given object is an array.
         * @param oop
         * @return
         */
/*MAC*/ boolean Object_isArray(int $oop) {
            return Class_isArrayClass(Object_getClass($oop));
        }

        /**
         * Test to see if an object is an array
         * @param object
         */
/*MAC*/ void Object_assumeIsArray(int $object) {
            if (assuming() && arrayCheckingEnabled) {
                assume(Object_isArray($object));
            }
        }

        /**
         * Test to see if an object is not an array
         * @param object
         */
/*MAC*/ void Object_assumeNotArray(int $object) {
            if (assuming() && arrayCheckingEnabled) {
                assume(!Object_isArray($object));
            }
        }

//IFC//#ifndef PRODUCTION
        /**
         * Get the length in elements for a given array object.
         * @param object
         * @return
         */
        int Object_getArrayElementCountNotAssuming(int o) {
            return (!hasCompactArrays() || ((CLASS_WORD(o) & compactArrayLengthMask()) == compactArrayLengthMask())
                       ? (int)srl(LENGTH_WORD(o), HT_ARRAY_LENGTH_SHIFT)
                       : (int)srl(CLASS_WORD(o), classPointerBitCount()));
        }

        /**
         * Get the length in elements for a given array object.
         * @param object
         * @return
         */
        int Object_getArrayElementCount(int o) {
            Object_assumeIsArray(o);
            return Object_getArrayElementCountNotAssuming(o);
        }

//IFC//#else
//IFC//#define Object_getArrayElementCountNotAssuming(o) (                                                          \
//IFC//            (!hasCompactArrays() || ((CLASS_WORD(o) & compactArrayLengthMask()) == compactArrayLengthMask()) \
//IFC//                ? (int)srl(LENGTH_WORD(o), HT_ARRAY_LENGTH_SHIFT)                                            \
//IFC//                : (int)srl(CLASS_WORD(o), classPointerBitCount()))                                           \
//IFC//        )
//IFC//#define Object_getArrayElementCount(o) Object_getArrayElementCountNotAssuming(o)
//IFC//#endif





        /**
         * Set the class pointer for an object.
         * @param object
         * @param klass
         */
/*MAC*/ void Object_setClass(int $object, int $klass) {
            assume($object != 0);
            assume($klass != 0);
            assume((($klass - getImageOffset()) & compactArrayLengthMask()) == 0);
            setWord($object, OBJ_class, (CLASS_WORD($object) & compactArrayLengthMask()) | ($klass - getImageOffset()));
        }

        /**
         * Set the class pointer for an array and its length.
         * @param object
         * @param klass
         */
/*MAC*/ void Object_setClassAndArrayCount(int $object, int $klass, int $count) {
            Object_setClass($object, $klass);
            Object_assumeIsArray($object);
            if ($count >= compactArrayMaxSize()) {
                setWord($object, OBJ_length, ($count << HT_ARRAY_LENGTH_SHIFT) | HT_ARRAY_LENGTH);
                setWord($object, OBJ_class, compactArrayLengthMask() | getWord($object, OBJ_class));
            } else {
                setWord($object, OBJ_class, ($count << classPointerBitCount()) | getWord($object, OBJ_class));
            }
        }

/*if[LISP2.COLLECTOR]*/
        /**
         * Set encoded low bits
         * @param object
         * @param lowbits
         */
/*MAC*/ void Object_setEncodedLowBits(int $object, int $lowbits) {
            assume($object != 0);
            assume(($lowbits & compactArrayLengthMask()) == 0);
            setWord($object, OBJ_class, (CLASS_WORD($object) & compactArrayLengthMask()) | $lowbits);
        }

        /**
         * Get encoded low bits
         * @param object
         */
/*MAC*/ int Object_getEncodedLowBits(int $object) {
/*IFJ*/     return assumeNonZero(CLASS_WORD($object) & classPointerMask(), null, 0);
//IFC//     return assumeNonZero(CLASS_WORD($object) & classPointerMask(), __FILE__, __LINE__);
        }
/*end[LISP2.COLLECTOR]*/

        /**
         * Get the length of a header in bytes
         * @param addr
         * @return the length
         */
/*MAC*/ int Object_getHeaderSize(int $addr) {
            return Object_isArrayLengthWord(getWord($addr, 0)) ? LARGE_HEADER_SIZE : SMALL_HEADER_SIZE;
        }

        /**
         * Set the forwarding pointer for an object to another object
         * @param object
         * @return the word
         */
/*MAC*/ void Object_forwardToObject(int $oop, int $to) {
            Object_setClass($oop, $to | HT_FORWARD_POINTER);
        }

        /**
         * Test an oop to see if it's header contains a forward pointer
         * @param oop
         * @return true if the oop has been forwarded
         */
/*MAC*/ boolean Object_isForwarded(int $oop) {
            return Object_isForwardedWord(Object_getClass($oop));
        }

        /**
         * Retuen the address an object has been forwarded to
         * @param oop
         * @return address
         */
/*MAC*/ int Object_getForwardedObject(int $oop) {
            return Object_getClass($oop) & ~HEADER_TYPE_MASK;
        }

        /**
         * Resolve an oop and return its forwarded address if it has been forwarded.
         * @param oop
         * @return the oop or the forwarded oop
         */
/*MAC*/ int Object_getPossiblyForwardedObject(int $oop) {
            return ($oop != 0 && Object_isForwarded($oop)) ? Object_getForwardedObject($oop) : $oop;
        }

/*---------------------------------------------------------------------------*\
 *                             Length calculation                            *
\*---------------------------------------------------------------------------*/

        /**
         * Get the element size (in bytes) of an array class.
         * @param klass Pointer to a Class object representing an array
         * @return the size (in bytes) of an element of the array.
         */
/*MAC*/ int Object_getArrayElementLength(int $klass) {
            int cno = Class_getType($klass);
            assume(Class_getElementType($klass) != 0);
            switch (cno) {
/*if[FLOATS]*/
                case CNO.DOUBLE_ARRAY:
/*end[FLOATS]*/
                case CNO.LONG_ARRAY: return 8;
/*if[NEWSTRING]*/
                case CNO.STRING:
/*end[NEWSTRING]*/
                case CNO.CHAR_ARRAY:
                case CNO.SHORT_ARRAY: return 2;
/*if[NEWSTRING]*/
                case CNO.STRING_OF_BYTES:
                case CNO.STRING_OF_SYMBOLS:
/*end[NEWSTRING]*/
                case CNO.BOOLEAN_ARRAY:
                case CNO.BYTE_ARRAY: return 1;
                default: return 4; /* This is the case for all word sized arrays */
            }
        }

        /**
         * Get the length (in bytes) of an object.
         * @param oop
         * @return object length
         */
/*MAC*/ int Object_getObjectLength(int $oop) {
            int klass = Object_getClass($oop);
            int cno   = Class_getType(klass);
            if (Object_isArray($oop)) {
                int count = Object_getArrayElementCount($oop) * Object_getArrayElementLength(klass);
                return roundup4(count); /* Round up to a full word boundry */
            } else {
                return Class_getInstanceFieldsLength(klass) * 4;
            }
        }

        /**
         * arrayCopy
         */
/*MAC*/ void Object_arrayCopy(int $src, int $srcPos, int $dst, int $dstPos, int $length) {

            int itemLength = Object_getArrayElementLength(Object_getClass($src));
            assume(Object_getArrayElementLength(Object_getClass($dst)) == itemLength);

            /* Adjust offsets and length to be in terms of bytes */
            $srcPos *= itemLength;
            $dstPos *= itemLength;
            $length *= itemLength;

            copyBytes($src+$srcPos, $dst+$dstPos, $length);
        }


/*---------------------------------------------------------------------------*\
 *                             Pointer manipulation                          *
\*---------------------------------------------------------------------------*/

        /**
         * Get the start of the block succeeding a given object.
         * @param oop object address
         * @return block address
         */
/*MAC*/ int Object_nextBlock(int $oop) {
            return $oop  + Object_getObjectLength($oop);
        }

        /**
         * Convert a block address to an object address.
         * @param addr block address
         * @return object address
         */
/*MAC*/ int Object_blockToOop(int $addr) {
            return $addr + Object_getHeaderSize($addr);
        }

        /**
         * Convert an object address to a block address.
         * @param oop object address
         * @return block address
         */
/*MAC*/ int Object_oopToBlock(int $oop) {
            return $oop - (Object_isArray($oop) ? Object_calculateArrayHeaderLength(Object_getArrayElementCount($oop)) : Object_objectHeaderSize());
        }

/*---------------------------------------------------------------------------*\
 *                 Free block accessors - all in EEPROM                      *
\*---------------------------------------------------------------------------*/

        /**
         * Test an address to see if it is a free block
         * @param addr
         * @return true if there is a free block at the given address.
         */
/*MAC*/ boolean isFreeBlock(int $addr) {
            return (getWord($addr, 0) & HEADER_TYPE_MASK) == HT_FREE_BLOCK;
        }

        /**
         * Determine whether or not a free block at a specified address
         * is larger than one word.
         * @return
         */
/*MAC*/ boolean FreeBlock_isLarge(int $addr) {
            return (getWord($addr, 0) & HT_LARGE_FREE_BLOCK_BIT) != 0;
        }

        /**
         * Get the length (in bytes) of a free block at a specified address.
         * @param addr the address of a free block.
         * @return the length (in bytes) of the specified free block
         */
/*MAC*/ int FreeBlock_getSize(int $addr) {
            return FreeBlock_isLarge($addr) ? getWord($addr, 1) : bytesPerWord();

        }

        /**
         * Get the offset (in bytes) of the next free block relative to a
         * specified block.
         * @param addr The address of the specified block.
         * @return the offset (in bytes) of the next free block relative to
         * the block at addr.
         */
/*MAC*/ int FreeBlock_getNextOffset(int $addr) {
            return srl(getWord($addr, 0), HT_FREE_BLOCK_NEXT_SHIFT);
        }

        /**
         * Set the header of a free block at a specified address. The header
         * is composed of the block's size (in bytes) and the offset (in bytes)
         * to the next free block.
         * @param addr The address of the free block whose header word is
         * to be set.
         * @param size The size (in bytes) of this free block.
         * @param offset The offset (in bytes) to the successor free block
         * or 0 if this is the last free block in the list.
         */
/*MAC*/ void FreeBlock_setHeader(int $addr, int $size, int $offset) {
            int header = (int)sll($offset, HT_FREE_BLOCK_NEXT_SHIFT);
            assume($size > 0);
            assume($size % bytesPerWord() == 0);

            /* Ensure the offset's magnitude can be encoded in the available number of bits */
            assume($offset == (int)srl(header, HT_FREE_BLOCK_NEXT_SHIFT));

            if ($size == w2b(1)) {
                setPersistentWord($addr, 0, header | HT_FREE_BLOCK);
            } else {
                setPersistentWord($addr, 0, header | HT_FREE_BLOCK | HT_LARGE_FREE_BLOCK_BIT);
                setPersistentWord($addr, 1, $size);
            }
        }


/*---------------------------------------------------------------------------*\
 *                 Object heap traversal                                     *
\*---------------------------------------------------------------------------*/

        /**
         * Get the next object in the heap after a given object, skipping any
         * interceding free blocks.
         * @param oop The object for which to find the successor.
         * @param start
         * @param end
         * @return
         */
        int Object_nextObject(int oop, int start, int end) {
            int chunk = Object_nextBlock(oop);
            assume(chunk >= start);
            if (chunk == end) {
                return 0; // end
            } else {
                assume(chunk < end);
                /* Skip any interceding free blocks */
                while (isFreeBlock(chunk)) {
                    chunk += FreeBlock_getSize(chunk);
                    if (chunk == end) {
                        return 0;
                    }
                }
                return Object_blockToOop(chunk);
            }
        }












/*IFJ*/}
