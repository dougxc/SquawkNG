//J2C:segacc.c **DO NOT DELETE THIS LINE**

/*IFJ*/package com.sun.squawk.vm;

/**
 * This class encapsulates the subsystem of the Squawk VM that deals with
 * accessing and setting well-known variable in each segment. The anatomy
 * of the different segments types is depicted in SquawkDesignDiagrams.ppt.
 *
 * @see "SquawkDesignDiagrams.ppt"
 * @author not attributable
 * @version 1.0
 */
/*IFJ*/abstract public class SegmentAccess extends NativeBuffers {

/*-----------------------------------------------------------------------*\
 *              Accessors to well known parts of memory                  *
\*-----------------------------------------------------------------------*/

/*MAC*/ int getSegmentMSROverhead() { return MSR_SIZE * 4; }

        /**
         * Get the start of the heap in a segment.
         */
/*MAC*/ int getHeapStart(int $segment)  { return $segment + getSegmentMSROverhead(); }

        /**
         * Get the size of the heap in a segment.
         */
/*MAC*/ int getHeapSize(int $segment, int $segmentEnd)  { return $segmentEnd - getHeapStart($segment); }

        /**
         * Get/set the address of the start of the object memory in a given segment.
         */
/*MAC*/ int  getObjectMemoryStart(int $segment)                  { return getWord($segment, MSR_gci + GCI_objectMemoryStart);                   }
/*MAC*/ void setObjectMemoryStart(int $segment, int $start)      {        setWord($segment, MSR_gci + GCI_objectMemoryStart, $start);           }

        /**
         * Get/set the size of the object memory for a given segment.
         */
/*MAC*/ int  getObjectMemorySize(int $segment)                   { return getWord($segment, MSR_gci + GCI_objectMemorySize);                    }
/*MAC*/ void setObjectMemorySize(int $segment, int $size)        {        setWord($segment, MSR_gci + GCI_objectMemorySize, $size);             }

        /**
         * Get/set the address of the end of the object memory in a given segment.
         */
/*MAC*/ int  getObjectMemoryEnd(int $segment)                  { return getObjectMemoryStart($segment) + getObjectMemorySize($segment);                   }

        /**
         * Start of the active semi space for a given segment.
         */
/*MAC*/ int  getObjectPartitionStart(int $segment)               { return getWord($segment, MSR_gci + GCI_currentObjectPartitionStart);                        }
/*MAC*/ void setObjectPartitionStart(int $segment, int $addr)    {        setWord($segment, MSR_gci + GCI_currentObjectPartitionStart, $addr);                 }

        /**
         * Allocation point in the active semi space for a given segment.
         */
/*MAC*/ int  getObjectPartitionFree(int $segment)                { return getWord($segment, MSR_gci + GCI_currentObjectPartitionFree);                 }
/*MAC*/ void setObjectPartitionFree(int $segment, int $addr)     {        setWord($segment, MSR_gci + GCI_currentObjectPartitionFree, $addr);          }

        /**
         * End of the active semi space for a given segment.
         */
/*MAC*/ int  getObjectPartitionEnd(int $segment)                 { return getWord($segment, MSR_gci + GCI_currentObjectPartitionEnd);                     }
/*MAC*/ void setObjectPartitionEnd(int $segment, int $addr)      {        setWord($segment, MSR_gci + GCI_currentObjectPartitionEnd, $addr);              }

        /**
         * Size of the last allocation that failed or zero if the last allocation
         * succeeded.
         */
/*MAC*/ int  getFailedAllocationSize(int $segment)               { return getWord($segment, MSR_gci + GCI_failedAllocationSize);                }
/*MAC*/ void setFailedAllocationSize(int $segment, int $size)    {        setWord($segment, MSR_gci + GCI_failedAllocationSize, $size);         }

        /**
         * This is a _global_[][] implementing the entries of the hashtable used
         * to store runtime state for each class.
         */
/*MAC*/ int  getClassStateTable()                                { return getWord(   RAM, MSR_roots + ROOT_classStateTable);                    }
/*MAC*/ void setClassStateTable(int $value)                      {        setOopRoot(RAM, MSR_roots + ROOT_classStateTable, $value);            }

        /**
         * This is the Suite[] which is the starting pointing for resolving
         * a class number into a Class object.
         */
/*MAC*/ int  getSuiteTableRom()                                  { return getWord(   ROM,    MSR_roots + ROOT_suiteTable);                      }
/*MAC*/ void setSuiteTableRom(int $value)                        {        setOopRoot(ROM,    MSR_roots + ROOT_suiteTable, $value);              }
/*MAC*/ int  getSuiteTableEeprom()                               { return getWord(   EEPROM, MSR_roots + ROOT_suiteTable);                      }
/*MAC*/ void setSuiteTableEeprom(int $value)                     {        setOopRoot(EEPROM, MSR_roots + ROOT_suiteTable, $value);              }


/*if[DEBUG.METHODDEBUGTABLE]*/
        /**
         * This is the Object[] containing the debug info for methods.
         */
/*MAC*/ int  getMethodDebugTable(int $segment)                   { return getWord(   $segment, MSR_roots + ROOT_methodDebugTable);                          }
/*MAC*/ void setMethodDebugTable(int $segment, int $value)       {        setOopRoot($segment, MSR_roots + ROOT_methodDebugTable, $value);                  }
/*MAC*/ void setMethodDebugTableRom(int $value)                  {        setOopRoot(ROM,      MSR_roots + ROOT_methodDebugTable, $value);                  }

/*end[DEBUG.METHODDEBUGTABLE]*/

        /**
         * The current stack chunk. The variable is used as mechanism for
         * communication between the interpreter loop and any Java support routines
         * it calls. While in the loop, the current stack chunk is stored
         * in a local variable. Just before calling a support routine, the value of
         * the local variable is written into the heap.
         */
/*MAC*/ int  getCurrentStackChunk()                              { return getWord(   RAM, MSR_roots + ROOT_currentStackChunk);                  }
/*MAC*/ void setCurrentStackChunk(int $value)                    {        setOopRoot(RAM, MSR_roots + ROOT_currentStackChunk, $value);          }


        /**
         * Return the address of a byte that contains a THROW instruction
         */
        int getAddressOfThrowBytecode() {
            int byteAddress = RAM + w2b(MSR_gci + GCI_res1);
            setByte(byteAddress, 0, OPC.THROW);
            return byteAddress;
        }

        /**
         * Get/Set the address of the class java.lang.VMExtension
         */
/*MAC*/ int  getJavaLangVMExtension()                            { return getWord(   ROM, MSR_roots + ROOT_javaLangVMExtension);                  }
/*MAC*/ void setJavaLangVMExtension(int $value)                  {        setOopRoot(ROM, MSR_roots + ROOT_javaLangVMExtension, $value);          }

        /**
         * Get/Set the pre-built out of memory exception
         */
/*MAC*/ int  getOutOfMemoryError()                               { return getWord(   RAM, MSR_roots + ROOT_outOfMemoryObject);                  }
/*MAC*/ void setOutOfMemoryError(int $value)                     {        setOopRoot(RAM, MSR_roots + ROOT_outOfMemoryObject, $value);          }

        /**
         * Get/Set the fast lock stack
         */
/*MAC*/ int  getFastLockStack()                                  { return getWord(   RAM, MSR_roots + ROOT_fastLockStack);                      }
/*MAC*/ void setFastLockStack(int $value)                        {        setOopRoot(RAM, MSR_roots + ROOT_fastLockStack, $value);              }

        /**
         * Get/set hash table of persistent memory roots.
         */
/*MAC*/ int  getPersistentMemoryTable()                          { return getWord(   EEPROM, MSR_roots + ROOT_persistentMemoryTable);           }
/*MAC*/ void setPersistentMemoryTable(int $value)                {        setOopRoot(EEPROM, MSR_roots + ROOT_persistentMemoryTable, $value);   }

        /**
         * Get/set finalization queue head
         */
/*MAC*/ int  getFinalizerQueue()                                 { return getWord(   RAM, MSR_roots + ROOT_finalizationQueue);                  }
/*MAC*/ void setFinalizerQueue(int $assn)                        {        setOopRoot(RAM, MSR_roots + ROOT_finalizationQueue, $assn);           }

        /**
         * Set the association hashtable
         */
/*MAC*/ void setAssociationHashtable(int $table) {
            assume($table != 0);
            assume(Object_getArrayElementCount($table) == ASSN_hashtableSize);
            setOopRoot(RAM, MSR_roots + ROOT_associationHashtable, $table);
        }

        /**
         * Get the association hashtable
         */
/*MAC*/ int getAssociationHashtable() {
            return getWord(RAM, MSR_roots + ROOT_associationHashtable);
        }

        /**
         * Get the association hashtable size
         */
/*MAC*/ int getAssociationHashtableSize() {
            return ASSN_hashtableSize;
        }

        /**
         * Get the hash code for an association
         */
/*MAC*/ int getAssociationHashEntry(int $object) {
            return ($object >> 2) % getAssociationHashtableSize();
        }

        /**
         * Get a queue of hashtable entires
         */
/*MAC*/ int getAssociationQueue(int $index) {
            int table = getAssociationHashtable();
            assume(table != 0);
            assume($index < getAssociationHashtableSize());
            return getWord(table, $index);
        }

        /**
         * Get the association (if any) for an object.
         */
/*MAC*/ int getAssociation(int $object) {
            int assn = getAssociationQueue(getAssociationHashEntry($object));
            while (assn != 0) {
                if (ObjectAssociation_getObject(assn) == $object) {
                    return assn;
                }
                assn = ObjectAssociation_getNext(assn);
            }
            return 0;
        }



        /**
         * Set a queue of hashtable entires
         */
/*MAC*/ void setAssociationQueue(int $index, int $assn) {
            int table = getAssociationHashtable();
            assume(table != 0);
            assume($index < getAssociationHashtableSize());
            setWord(table, $index, $assn);
        }

        /**
         * Get the pointer to the head of the linked list of free blocks in
         * EEPROM.
         */
/*MAC*/ int getEepromFreeList() {
            return getWord(EEPROM, MSR_roots + ROOT_freeList);
        }

        /**
         * Set the pointer to the head of the linked list of free blocks in
         * EEPROM.
         */
/*MAC*/ void setEepromFreeList(int $value) {
            setOopRoot(EEPROM, MSR_roots + ROOT_freeList, $value);
        }


/*if[LISP2.COLLECTOR]*/
        /**
         * Get the pointer to the head of the linked list of stack chunks
         */
/*MAC*/ int getStackChunkList() {
            return getWord(RAM, MSR_roots + ROOT_stackChunkList);
        }

        /**
         * Set the pointer to the head of the linked list of stack chunks
         */
/*MAC*/ void setStackChunkList(int $value) {
            setOopRoot(RAM, MSR_roots + ROOT_stackChunkList, $value);
        }
/*end[LISP2.COLLECTOR]*/


/*-----------------------------------------------------------------------*\
 *                            Initialization                             *
\*-----------------------------------------------------------------------*/

        /**
         * Initialise the garbage collection info for a segment.
         * @param segment The start address of the segment.
         * @param start The start address of the object memory.
         * @param start The size of the object memory.
         */
        void Segment_init(int segment, int start, int size) {
            setObjectMemoryStart(segment, start);
            setObjectMemorySize(segment, size);
            setObjectPartitionStart(segment, start);
            setObjectPartitionFree(segment, start);
            setObjectPartitionEnd(segment, start + size);
        }


/*-----------------------------------------------------------------------*\
 *                            getClassFromCNO                            *
\*-----------------------------------------------------------------------*/

        /**
         * Get the address of Class given its suite class number.
         * @param cno
         * @return
         */
/*MAC*/ int getClassFromCNO(int $cno) {
            int klass, suite, sno, klasses, suiteTable, cnoFF;
            assume($cno != 0);

           /*
            * Get the suite table
            */
            suiteTable = getSuiteTableEeprom();
            if (suiteTable == 0) {
                suiteTable = getSuiteTableRom();
            }
            assume(suiteTable != 0);

           /*
            * Split the class number into its suite index and class index
            */
            sno = ($cno >> 8);
            assume((sno & 0xFF) == sno);
            cnoFF = $cno & 0xFF;

           /*
            * Retrieve the Suite from the Suite[]
            */
            suite = getWord(suiteTable, sno);
            assume(suite != 0);

           /*
            * Get the 'classes' field from the suite
            */
            klasses = Suite_getClasses(suite);

           /*
            * Look up the class
            */
            klass = getWord(klasses, cnoFF);

            assume(klass != 0);
            assume(Class_getType(klass) == $cno);

            return klass;
        }

/*if[LISP2.COLLECTOR]*/

        /**
         * Get the highest possible relative class number
         * @return mas
         */
/*MAC*/ int getMaximumRcn() {
            int suiteTable, suiteTableLth, i;
            int max = 0;

           /*
            * Get the suite table
            */
            suiteTable = getSuiteTableEeprom();
            if (suiteTable == 0) {
                suiteTable = getSuiteTableRom();
            }
            assume(suiteTable != 0);
            suiteTableLth = Object_getArrayElementCount(suiteTable);

            for (i = 0 ; i < suiteTableLth ; i++) {
                int suite = getWord(suiteTable, i);
                if (suite != 0) {
                    int slth, klasses;
                    klasses = Suite_getClasses(suite);
                    assume(klasses != 0);
                    slth  = Object_getArrayElementCount(klasses);
                    max += slth;
                }
            }
            return max;
        }


        /**
         * Get the relative class number from a suite class number.
         * @param rcn
         * @return klass
         */
/*MAC*/ int getKlassFromRcn(int $rcn) {
            int suiteTable, i;

           /*
            * Get the suite table
            */
            assume($rcn != 0);
            suiteTable = getSuiteTableEeprom();
            if (suiteTable == 0) {
                suiteTable = getSuiteTableRom();
            }
            assume(suiteTable != 0);

            for (i = 0 ;; i++) {
                int suite = getWord(suiteTable, i);
                if (suite != 0) {
                    int klasses = Suite_getClasses(suite);
                    int slth  = Object_getArrayElementCount(klasses);
                    if ($rcn > slth) {
                        $rcn -= slth;
                    } else {
                        return getWord(klasses, $rcn);
                    }
                }
            }
        }


        /**
         * Get the relative class number from a suite class number.
         * @param cno
         * @return rcn
         */
/*MAC*/ int getRcnFromKlass(int $klass) {
            int cno = Class_getType($klass);
            int rcn, sno, suiteTable, i;

           /*
            * Get the suite table
            */
            assume(cno != 0);
            suiteTable = getSuiteTableEeprom();
            if (suiteTable == 0) {
                suiteTable = getSuiteTableRom();
            }
            assume(suiteTable != 0);

           /*
            * Split the class number into its suite index and class index
            */
            sno = (cno >> 8);
            assume((sno & 0xFF) == sno);
            rcn = cno & 0xFF;

           /*
            * Add to the rcn the count of all classes in previous suites
            */
            for (i = 0 ; i < sno ; i++) {
                int suite = getWord(suiteTable, i);
                if (suite != 0) {
                    int klasses = Suite_getClasses(suite);
                    rcn += Object_getArrayElementCount(klasses);
                }
            }

            assume(getKlassFromRcn(rcn) == $klass);
            return rcn;
        }


/*end[LISP2.COLLECTOR]*/

/*IFJ*/}
